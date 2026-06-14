//! Async HTTP client for the ClipDock sync server (protocol v2).
//!
//! Response bodies are wrapped in a `{ protocol_version, data }` envelope. The
//! pulled-event and snapshot payloads are deserialized directly into the
//! `clipboard_core` sync record types, which are field-compatible with the
//! server's wire format — this keeps Windows byte-for-byte aligned with the
//! macOS client and the server contract.

use clipboard_core::{SyncEventRecord, SyncSnapshotItemRecord, SyncSnapshotTombstoneRecord};
use serde::{Deserialize, Serialize};

/// Default page size when pulling events.
pub const DEFAULT_PULL_LIMIT: i64 = 200;

#[derive(Debug)]
pub enum SyncError {
    Transport(String),
    Server { status: u16, message: String },
    Decode(String),
}

impl std::fmt::Display for SyncError {
    fn fmt(&self, formatter: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::Transport(message) => write!(formatter, "transport error: {message}"),
            Self::Server { status, message } => {
                write!(formatter, "server error {status}: {message}")
            }
            Self::Decode(message) => write!(formatter, "decode error: {message}"),
        }
    }
}

impl SyncError {
    pub fn to_message(&self) -> String {
        self.to_string()
    }
}

#[derive(Deserialize)]
struct Envelope<T> {
    #[allow(dead_code)]
    protocol_version: u8,
    data: T,
}

#[derive(Serialize)]
struct CreateSyncBody<'a> {
    device_name: &'a str,
}

#[derive(Serialize)]
struct JoinSyncBody<'a> {
    pairing_code: &'a str,
    device_name: &'a str,
}

#[derive(Debug, Clone, Deserialize)]
pub struct CreateSyncResponse {
    pub sync_id: String,
    pub pairing_code: String,
    pub pairing_expires_at_ms: i64,
    pub device_id: String,
    pub token: String,
}

#[derive(Debug, Clone, Deserialize)]
pub struct JoinSyncResponse {
    pub sync_id: String,
    pub device_id: String,
    pub token: String,
}

#[derive(Debug, Clone, Deserialize)]
pub struct PullEventsResponse {
    pub events: Vec<SyncEventRecord>,
    pub next_cursor: i64,
}

#[derive(Debug, Clone, Deserialize)]
pub struct SnapshotResponse {
    pub snapshot_seq: i64,
    pub items: Vec<SyncSnapshotItemRecord>,
    pub tombstones: Vec<SyncSnapshotTombstoneRecord>,
}

/// A single outbound event to push to the server.
#[derive(Debug, Clone, Serialize)]
pub struct OutgoingEvent {
    pub client_event_id: String,
    #[serde(rename = "type")]
    pub event_type: String,
    pub content_hash: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub item_type: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub payload: Option<serde_json::Value>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copy_count_delta: Option<i64>,
}

#[derive(Serialize)]
struct PushEventsBody {
    events: Vec<OutgoingEvent>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct PushedEvent {
    pub client_event_id: String,
    pub server_seq: i64,
    #[allow(dead_code)]
    pub duplicate: bool,
}

#[derive(Debug, Clone, Deserialize)]
pub struct PushEventsResponse {
    pub events: Vec<PushedEvent>,
    #[allow(dead_code)]
    pub next_cursor: i64,
}

pub struct SyncClient {
    base_url: String,
    http: reqwest::Client,
}

impl SyncClient {
    pub fn new(base_url: impl Into<String>) -> Self {
        let base_url = base_url.into().trim_end_matches('/').to_string();
        Self {
            base_url,
            http: reqwest::Client::new(),
        }
    }

    fn url(&self, path: &str) -> String {
        format!("{}{}", self.base_url, path)
    }

    /// Create a brand-new sync space, registering this device as the first
    /// member and returning a pairing code other devices can join with.
    pub async fn create_space(&self, device_name: &str) -> Result<CreateSyncResponse, SyncError> {
        let response = self
            .http
            .post(self.url("/v2/sync/create"))
            .json(&CreateSyncBody { device_name })
            .send()
            .await
            .map_err(|error| SyncError::Transport(error.to_string()))?;
        decode_envelope(response).await
    }

    /// Join an existing sync space using a pairing code.
    pub async fn join_space(
        &self,
        pairing_code: &str,
        device_name: &str,
    ) -> Result<JoinSyncResponse, SyncError> {
        let response = self
            .http
            .post(self.url("/v2/sync/join"))
            .json(&JoinSyncBody {
                pairing_code,
                device_name,
            })
            .send()
            .await
            .map_err(|error| SyncError::Transport(error.to_string()))?;
        decode_envelope(response).await
    }

    /// Pull events newer than `after_seq` (the device's current cursor).
    pub async fn pull_events(
        &self,
        token: &str,
        after_seq: i64,
        limit: i64,
    ) -> Result<PullEventsResponse, SyncError> {
        let response = self
            .http
            .get(self.url("/v2/events"))
            .query(&[("after", after_seq.to_string()), ("limit", limit.to_string())])
            .bearer_auth(token)
            .send()
            .await
            .map_err(|error| SyncError::Transport(error.to_string()))?;
        decode_envelope(response).await
    }

    /// Push locally generated events to the server.
    pub async fn push_events(
        &self,
        token: &str,
        events: Vec<OutgoingEvent>,
    ) -> Result<PushEventsResponse, SyncError> {
        let response = self
            .http
            .post(self.url("/v2/events"))
            .bearer_auth(token)
            .json(&PushEventsBody { events })
            .send()
            .await
            .map_err(|error| SyncError::Transport(error.to_string()))?;
        decode_envelope(response).await
    }

    /// Fetch a full snapshot of the sync space (used on first join / recovery).
    pub async fn get_snapshot(&self, token: &str) -> Result<SnapshotResponse, SyncError> {
        let response = self
            .http
            .get(self.url("/v2/snapshot"))
            .bearer_auth(token)
            .send()
            .await
            .map_err(|error| SyncError::Transport(error.to_string()))?;
        decode_envelope(response).await
    }
}

async fn decode_envelope<T>(response: reqwest::Response) -> Result<T, SyncError>
where
    T: for<'de> Deserialize<'de>,
{
    let status = response.status();
    let bytes = response
        .bytes()
        .await
        .map_err(|error| SyncError::Transport(error.to_string()))?;

    if !status.is_success() {
        let message = String::from_utf8_lossy(&bytes).to_string();
        return Err(SyncError::Server {
            status: status.as_u16(),
            message,
        });
    }

    let envelope: Envelope<T> = serde_json::from_slice(&bytes)
        .map_err(|error| SyncError::Decode(error.to_string()))?;
    Ok(envelope.data)
}
