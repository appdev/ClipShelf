//! Realtime inbound sync over the server's `/v2/ws` WebSocket.
//!
//! Complements the periodic HTTP poll loop (which still owns outbound push and
//! acts as a safety net): this gives near-instant delivery of remote events.
//! On connect the server sends `hello`; if the local cursor is behind it sends
//! `catchup_required`, which we satisfy with an HTTP pull. Live `event_batch`
//! messages are applied directly. The connection auto-reconnects with backoff.

use std::time::Duration;

use clipboard_core::SyncEventRecord;
use futures_util::{SinkExt, StreamExt};
use serde::Deserialize;
use tauri::{AppHandle, Manager};
use tokio_tungstenite::tungstenite::client::IntoClientRequest;
use tokio_tungstenite::tungstenite::Message;

use super::client::{SyncClient, DEFAULT_PULL_LIMIT};
use super::{apply_events, current_cursor, read_credentials, SyncCredentials};
use crate::core_state::CoreState;

const RECONNECT_MIN: Duration = Duration::from_secs(3);
const RECONNECT_MAX: Duration = Duration::from_secs(60);
const IDLE_RETRY: Duration = Duration::from_secs(10);

#[derive(Debug, Deserialize)]
#[serde(tag = "type")]
enum ServerMessage {
    #[serde(rename = "hello")]
    Hello {
        latest_seq: i64,
        #[allow(dead_code)]
        cursor: i64,
    },
    #[serde(rename = "catchup_required")]
    CatchupRequired { latest_seq: i64 },
    #[serde(rename = "event_batch")]
    EventBatch {
        #[allow(dead_code)]
        from_seq: i64,
        to_seq: i64,
        events: Vec<SyncEventRecord>,
    },
    #[serde(rename = "error")]
    Error {
        code: String,
        #[serde(default)]
        message: String,
    },
}

/// Convert an `http(s)://` server URL to its `ws(s)://` equivalent.
fn to_ws_url(server_url: &str) -> String {
    let trimmed = server_url.trim_end_matches('/');
    if let Some(rest) = trimmed.strip_prefix("https://") {
        format!("wss://{rest}")
    } else if let Some(rest) = trimmed.strip_prefix("http://") {
        format!("ws://{rest}")
    } else {
        trimmed.to_string()
    }
}

/// Spawn the realtime WebSocket loop. It idles while sync is unconfigured and
/// reconnects with exponential backoff on failure.
pub fn start_realtime_loop(app: AppHandle) {
    tauri::async_runtime::spawn(async move {
        let mut backoff = RECONNECT_MIN;
        loop {
            let Some(state) = app.try_state::<CoreState>() else {
                tokio::time::sleep(IDLE_RETRY).await;
                continue;
            };

            let credentials = match read_credentials(&state) {
                Ok(Some(credentials)) if sync_enabled(&state) => credentials,
                _ => {
                    tokio::time::sleep(IDLE_RETRY).await;
                    continue;
                }
            };

            match run_connection(&app, &state, &credentials).await {
                Ok(()) => {
                    // Clean close: reset backoff and retry promptly.
                    backoff = RECONNECT_MIN;
                }
                Err(message) => {
                    eprintln!("sync realtime: {message}");
                    tokio::time::sleep(backoff).await;
                    backoff = (backoff * 2).min(RECONNECT_MAX);
                    continue;
                }
            }
            tokio::time::sleep(RECONNECT_MIN).await;
        }
    });
}

fn sync_enabled(state: &CoreState) -> bool {
    state
        .with_core(|core| {
            core.get_preferences()
                .map(|prefs| prefs.sync.enabled)
                .map_err(|error| error.to_string())
        })
        .unwrap_or(false)
}

async fn run_connection(
    app: &AppHandle,
    state: &CoreState,
    credentials: &SyncCredentials,
) -> Result<(), String> {
    let cursor = current_cursor(state, &credentials.sync_id, &credentials.device_id)?;
    let ws_url = to_ws_url(&credentials.server_url);
    let url = format!("{ws_url}/v2/ws?cursor={cursor}&protocol_version=2");

    let mut request = url
        .into_client_request()
        .map_err(|error| format!("invalid ws url: {error}"))?;
    request.headers_mut().insert(
        "Authorization",
        format!("Bearer {}", credentials.token)
            .parse()
            .map_err(|_| "invalid auth header".to_string())?,
    );

    let (stream, _response) = tokio_tungstenite::connect_async(request)
        .await
        .map_err(|error| format!("connect failed: {error}"))?;
    let (mut write, mut read) = stream.split();

    while let Some(message) = read.next().await {
        let message = message.map_err(|error| format!("stream error: {error}"))?;
        match message {
            Message::Text(text) => {
                let parsed: ServerMessage = match serde_json::from_str(&text) {
                    Ok(parsed) => parsed,
                    Err(_) => continue,
                };
                match parsed {
                    ServerMessage::Hello { latest_seq, .. }
                    | ServerMessage::CatchupRequired { latest_seq } => {
                        catch_up(app, state, credentials, latest_seq).await?;
                    }
                    ServerMessage::EventBatch { to_seq, events, .. } => {
                        apply_batch(app, state, credentials, events, to_seq).await?;
                    }
                    ServerMessage::Error { code, message } => {
                        return Err(format!("server error {code}: {message}"));
                    }
                }
            }
            Message::Ping(payload) => {
                write
                    .send(Message::Pong(payload))
                    .await
                    .map_err(|error| format!("pong failed: {error}"))?;
            }
            Message::Close(_) => return Ok(()),
            _ => {}
        }
    }
    Ok(())
}

/// Apply a live event batch if it advances past the local cursor.
async fn apply_batch(
    app: &AppHandle,
    state: &CoreState,
    credentials: &SyncCredentials,
    events: Vec<SyncEventRecord>,
    to_seq: i64,
) -> Result<(), String> {
    let cursor = current_cursor(state, &credentials.sync_id, &credentials.device_id)?;
    if to_seq <= cursor {
        return Ok(());
    }
    let report = apply_events(
        state,
        &credentials.sync_id,
        &credentials.device_id,
        events,
        to_seq,
    )?;
    if report.applied_events > 0 {
        super::download_remote_assets(state, credentials).await;
        super::notify_sync_applied(app);
    }
    Ok(())
}

/// Catch up to `latest_seq` by pulling over HTTP when the cursor is behind.
async fn catch_up(
    app: &AppHandle,
    state: &CoreState,
    credentials: &SyncCredentials,
    latest_seq: i64,
) -> Result<(), String> {
    let mut cursor = current_cursor(state, &credentials.sync_id, &credentials.device_id)?;
    if cursor >= latest_seq {
        return Ok(());
    }

    let client = SyncClient::new(&credentials.server_url);
    let mut applied_any = false;
    loop {
        let pulled = client
            .pull_events(&credentials.token, cursor, DEFAULT_PULL_LIMIT)
            .await
            .map_err(|error| error.to_message())?;
        let next_cursor = pulled.next_cursor;
        let empty = pulled.events.is_empty();
        let report = apply_events(
            state,
            &credentials.sync_id,
            &credentials.device_id,
            pulled.events,
            next_cursor,
        )?;
        applied_any = applied_any || report.applied_events > 0;
        cursor = next_cursor;
        if empty || cursor >= latest_seq {
            break;
        }
    }
    if applied_any {
        super::download_remote_assets(state, credentials).await;
        super::notify_sync_applied(app);
    }
    Ok(())
}
