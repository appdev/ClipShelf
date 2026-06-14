//! Sync orchestration: handshake (create/join), inbound event + snapshot
//! application, status reporting, and a background polling loop.
//!
//! Credentials (sync id, device id, token, server url) are persisted in the
//! shared `clipboard_core` preferences document under the `sync` section,
//! mirroring the macOS client. Inbound events/snapshots are applied through
//! `clipboard_core`, so conflict resolution and content hashing stay identical
//! across platforms.

mod client;

use std::time::Duration;

use clipboard_core::{SyncApplyEventsRequest, SyncApplySnapshotRequest, SyncUploadedEvent};
use serde::Serialize;
use tauri::{AppHandle, Manager, State};

use crate::core_state::CoreState;
use client::{OutgoingEvent, SyncClient, DEFAULT_PULL_LIMIT};

const EVENT_TYPE_ITEM_UPSERT: &str = "item_upsert";

/// Interval between background sync pulls.
const POLL_INTERVAL: Duration = Duration::from_secs(15);

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SyncStatus {
    pub enabled: bool,
    pub joined: bool,
    pub server_url: String,
    pub device_name: String,
    pub sync_id: Option<String>,
    pub device_id: Option<String>,
    pub cursor: i64,
    pub snapshot_seq: i64,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SyncCreateResult {
    pub sync_id: String,
    pub pairing_code: String,
    pub pairing_expires_at_ms: i64,
    pub device_id: String,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SyncApplyReport {
    pub applied_events: usize,
    pub cursor: i64,
    pub snapshot_seq: i64,
}

/// Snapshot of the sync credentials needed to talk to the server, read out of
/// the preferences document under a single lock.
struct SyncCredentials {
    server_url: String,
    sync_id: String,
    device_id: String,
    token: String,
}

fn read_credentials(state: &CoreState) -> Result<Option<SyncCredentials>, String> {
    state.with_core(|core| {
        let prefs = core.get_preferences().map_err(|error| error.to_string())?;
        let sync = prefs.sync;
        match (sync.sync_id, sync.device_id, sync.device_token) {
            (Some(sync_id), Some(device_id), Some(token))
                if !sync.server_url.is_empty()
                    && !sync_id.is_empty()
                    && !device_id.is_empty()
                    && !token.is_empty() =>
            {
                Ok(Some(SyncCredentials {
                    server_url: sync.server_url,
                    sync_id,
                    device_id,
                    token,
                }))
            }
            _ => Ok(None),
        }
    })
}

/// Persist sync identity into the preferences document, enabling sync.
fn store_identity(
    state: &CoreState,
    server_url: &str,
    sync_id: &str,
    device_id: &str,
    token: &str,
    device_name: &str,
) -> Result<(), String> {
    state.with_core(|core| {
        let mut prefs = core.get_preferences().map_err(|error| error.to_string())?;
        prefs.sync.enabled = true;
        prefs.sync.server_url = server_url.to_string();
        prefs.sync.sync_id = Some(sync_id.to_string());
        prefs.sync.device_id = Some(device_id.to_string());
        prefs.sync.device_token = Some(token.to_string());
        if !device_name.is_empty() {
            prefs.sync.device_name = device_name.to_string();
        }
        core.update_preferences(prefs)
            .map(|_| ())
            .map_err(|error| error.to_string())
    })
}

/// Apply a freshly pulled batch of events, advancing the device cursor.
fn apply_events(
    state: &CoreState,
    sync_id: &str,
    device_id: &str,
    events: Vec<clipboard_core::SyncEventRecord>,
    next_cursor: i64,
) -> Result<SyncApplyReport, String> {
    let count = events.len();
    state.with_core(|core| {
        let outcome = core
            .apply_sync_events(SyncApplyEventsRequest {
                sync_id: sync_id.to_string(),
                device_id: device_id.to_string(),
                events,
                next_cursor,
            })
            .map_err(|error| error.to_string())?;
        Ok(SyncApplyReport {
            applied_events: count,
            cursor: outcome.cursor,
            snapshot_seq: outcome.snapshot_seq,
        })
    })
}

/// Apply a full snapshot (used right after joining a space).
fn apply_snapshot(
    state: &CoreState,
    sync_id: &str,
    device_id: &str,
    snapshot: client::SnapshotResponse,
) -> Result<SyncApplyReport, String> {
    state.with_core(|core| {
        let outcome = core
            .apply_sync_snapshot(SyncApplySnapshotRequest {
                sync_id: sync_id.to_string(),
                device_id: device_id.to_string(),
                snapshot_seq: snapshot.snapshot_seq,
                items: snapshot.items,
                tombstones: snapshot.tombstones,
            })
            .map_err(|error| error.to_string())?;
        Ok(SyncApplyReport {
            applied_events: 0,
            cursor: outcome.cursor,
            snapshot_seq: outcome.snapshot_seq,
        })
    })
}

/// Upload any locally captured items pending sync, then mark them synced once
/// the server acknowledges them. Returns the number of events uploaded.
async fn push_pending(state: &CoreState, credentials: &SyncCredentials) -> Result<usize, String> {
    let pending = state.with_core(|core| {
        core.list_pending_sync_events(&credentials.sync_id)
            .map_err(|error| error.to_string())
    })?;

    if pending.is_empty() {
        return Ok(0);
    }

    let events: Vec<OutgoingEvent> = pending
        .iter()
        .map(|event| OutgoingEvent {
            client_event_id: event.client_event_id.clone(),
            event_type: EVENT_TYPE_ITEM_UPSERT.to_string(),
            content_hash: event.content_hash.clone(),
            item_type: Some(event.item_type.clone()),
            payload: Some(event.payload.clone()),
            copy_count_delta: Some(event.copy_count_delta),
        })
        .collect();

    let client = SyncClient::new(&credentials.server_url);
    let response = client
        .push_events(&credentials.token, events)
        .await
        .map_err(|error| error.to_message())?;

    // Map acknowledged client_event_ids back to their content hashes.
    let uploads: Vec<SyncUploadedEvent> = pending
        .iter()
        .filter_map(|event| {
            response
                .events
                .iter()
                .find(|acked| acked.client_event_id == event.client_event_id)
                .map(|acked| SyncUploadedEvent {
                    content_hash: event.content_hash.clone(),
                    server_seq: acked.server_seq,
                })
        })
        .collect();

    let count = uploads.len();
    state.with_core(|core| {
        core.mark_sync_events_uploaded(&credentials.sync_id, &uploads)
            .map(|_| ())
            .map_err(|error| error.to_string())
    })?;

    Ok(count)
}

fn current_cursor(state: &CoreState, sync_id: &str, device_id: &str) -> Result<i64, String> {
    state.with_core(|core| {
        core.get_sync_progress(sync_id, device_id)
            .map(|progress| progress.cursor)
            .map_err(|error| error.to_string())
    })
}

// ----- Tauri commands ------------------------------------------------------

/// Create a new sync space and persist the resulting credentials. Returns the
/// pairing code that other devices use to join.
#[tauri::command]
pub async fn sync_create_space(
    state: State<'_, CoreState>,
    server_url: String,
    device_name: String,
) -> Result<SyncCreateResult, String> {
    let client = SyncClient::new(&server_url);
    let response = client
        .create_space(&device_name)
        .await
        .map_err(|error| error.to_message())?;

    store_identity(
        &state,
        &server_url,
        &response.sync_id,
        &response.device_id,
        &response.token,
        &device_name,
    )?;

    Ok(SyncCreateResult {
        sync_id: response.sync_id,
        pairing_code: response.pairing_code,
        pairing_expires_at_ms: response.pairing_expires_at_ms,
        device_id: response.device_id,
    })
}

/// Join an existing sync space with a pairing code, then load the initial
/// snapshot so the local history reflects the space immediately.
#[tauri::command]
pub async fn sync_join_space(
    state: State<'_, CoreState>,
    server_url: String,
    pairing_code: String,
    device_name: String,
) -> Result<SyncApplyReport, String> {
    let client = SyncClient::new(&server_url);
    let response = client
        .join_space(&pairing_code, &device_name)
        .await
        .map_err(|error| error.to_message())?;

    store_identity(
        &state,
        &server_url,
        &response.sync_id,
        &response.device_id,
        &response.token,
        &device_name,
    )?;

    let snapshot = client
        .get_snapshot(&response.token)
        .await
        .map_err(|error| error.to_message())?;

    apply_snapshot(&state, &response.sync_id, &response.device_id, snapshot)
}

/// Push pending local items, then pull and apply remote events. This is the
/// full bidirectional sync step. Safe to call when sync is not configured
/// (returns a no-op report).
#[tauri::command]
pub async fn sync_pull_now(state: State<'_, CoreState>) -> Result<SyncApplyReport, String> {
    let Some(credentials) = read_credentials(&state)? else {
        return Ok(SyncApplyReport {
            applied_events: 0,
            cursor: 0,
            snapshot_seq: 0,
        });
    };

    // Outbound first so freshly captured items reach the server before we
    // reconcile the cursor.
    push_pending(&state, &credentials).await?;

    let cursor = current_cursor(&state, &credentials.sync_id, &credentials.device_id)?;
    let client = SyncClient::new(&credentials.server_url);
    let pulled = client
        .pull_events(&credentials.token, cursor, DEFAULT_PULL_LIMIT)
        .await
        .map_err(|error| error.to_message())?;

    apply_events(
        &state,
        &credentials.sync_id,
        &credentials.device_id,
        pulled.events,
        pulled.next_cursor,
    )
}

/// Push only: upload locally captured items pending sync. Returns the number
/// of events uploaded.
#[tauri::command]
pub async fn sync_push_now(state: State<'_, CoreState>) -> Result<usize, String> {
    let Some(credentials) = read_credentials(&state)? else {
        return Ok(0);
    };
    push_pending(&state, &credentials).await
}

/// Report current sync configuration and progress for the preferences UI.
#[tauri::command]
pub fn sync_status(state: State<'_, CoreState>) -> Result<SyncStatus, String> {
    state.with_core(|core| {
        let prefs = core.get_preferences().map_err(|error| error.to_string())?;
        let sync = prefs.sync.clone();
        let joined = sync.sync_id.is_some() && sync.device_id.is_some();
        let (cursor, snapshot_seq) = match (sync.sync_id.as_deref(), sync.device_id.as_deref()) {
            (Some(sync_id), Some(device_id)) if joined => core
                .get_sync_progress(sync_id, device_id)
                .map(|progress| (progress.cursor, progress.snapshot_seq))
                .unwrap_or((0, 0)),
            _ => (0, 0),
        };
        Ok(SyncStatus {
            enabled: sync.enabled,
            joined,
            server_url: sync.server_url,
            device_name: sync.device_name,
            sync_id: sync.sync_id,
            device_id: sync.device_id,
            cursor,
            snapshot_seq,
        })
    })
}

/// Disable sync without discarding credentials (re-enable resumes from cursor).
#[tauri::command]
pub fn sync_disable(state: State<'_, CoreState>) -> Result<(), String> {
    state.with_core(|core| {
        let mut prefs = core.get_preferences().map_err(|error| error.to_string())?;
        prefs.sync.enabled = false;
        core.update_preferences(prefs)
            .map(|_| ())
            .map_err(|error| error.to_string())
    })
}

/// Spawn the background polling loop. While sync is enabled and configured, it
/// pulls and applies events every `POLL_INTERVAL`. Errors are logged and the
/// loop continues, so transient network failures self-heal.
pub fn start_sync_poll_loop(app: AppHandle) {
    tauri::async_runtime::spawn(async move {
        loop {
            tokio::time::sleep(POLL_INTERVAL).await;

            let Some(state) = app.try_state::<CoreState>() else {
                continue;
            };

            let credentials = match read_credentials(&state) {
                Ok(Some(credentials)) => credentials,
                Ok(None) => continue,
                Err(message) => {
                    eprintln!("sync poll: failed to read credentials: {message}");
                    continue;
                }
            };

            // Respect the enabled flag.
            let enabled = state
                .with_core(|core| {
                    core.get_preferences()
                        .map(|prefs| prefs.sync.enabled)
                        .map_err(|error| error.to_string())
                })
                .unwrap_or(false);
            if !enabled {
                continue;
            }

            // Outbound: upload pending local captures.
            if let Err(message) = push_pending(&state, &credentials).await {
                eprintln!("sync poll: push failed: {message}");
            }

            let cursor = match current_cursor(&state, &credentials.sync_id, &credentials.device_id)
            {
                Ok(cursor) => cursor,
                Err(message) => {
                    eprintln!("sync poll: cursor read failed: {message}");
                    continue;
                }
            };

            let client = SyncClient::new(&credentials.server_url);
            match client
                .pull_events(&credentials.token, cursor, DEFAULT_PULL_LIMIT)
                .await
            {
                Ok(pulled) => {
                    if let Err(message) = apply_events(
                        &state,
                        &credentials.sync_id,
                        &credentials.device_id,
                        pulled.events,
                        pulled.next_cursor,
                    ) {
                        eprintln!("sync poll: apply failed: {message}");
                    }
                }
                Err(error) => {
                    eprintln!("sync poll: pull failed: {}", error.to_message());
                }
            }
        }
    });
}
