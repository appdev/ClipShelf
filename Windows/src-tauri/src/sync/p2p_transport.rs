//! Thin async wrappers around the shared `clipdock_p2p` iroh-blobs transport.
//!
//! The underlying functions manage a global iroh node and an internal tokio
//! runtime, so they must run on a blocking thread (via `spawn_blocking`) to
//! avoid nesting runtimes. Paths are resolved relative to the ClipDock data
//! root, matching how the node is initialized.

use std::path::PathBuf;

/// Result of providing a file over P2P.
pub struct Provided {
    pub blob_hash: String,
    pub blob_ticket: String,
}

const TIMEOUT_MS: i64 = 30_000;

/// The local iroh node's reachable identity.
pub struct NodeInfo {
    pub endpoint_id: String,
    pub relay_url: Option<String>,
    pub direct_addresses: Vec<String>,
}

/// Start (or reuse) the local iroh node and return its reachable identity.
pub async fn start_node(root_dir: PathBuf) -> Option<NodeInfo> {
    let root = root_dir.to_string_lossy().to_string();
    let outcome = tokio::task::spawn_blocking(move || clipdock_p2p::start_node(root, TIMEOUT_MS))
        .await
        .ok()?;
    if !outcome.ok {
        eprintln!("p2p start_node failed: {}", outcome.error_code);
        return None;
    }
    let direct_addresses: Vec<String> =
        serde_json::from_str(&outcome.direct_addresses_json).unwrap_or_default();
    Some(NodeInfo {
        endpoint_id: outcome.endpoint_id,
        relay_url: (!outcome.relay_url.is_empty()).then_some(outcome.relay_url),
        direct_addresses,
    })
}

/// Provide a file so peers can fetch it; returns the blob hash and a ticket
/// (which encodes this node's address + the hash). `file_relative` is relative
/// to `root_dir`.
pub async fn provide_file(root_dir: PathBuf, file_relative: String) -> Option<Provided> {
    let root = root_dir.to_string_lossy().to_string();
    let outcome =
        tokio::task::spawn_blocking(move || clipdock_p2p::provide_file(root, file_relative, TIMEOUT_MS))
            .await
            .ok()?;
    if !outcome.ok {
        eprintln!("p2p provide failed: {}", outcome.error_code);
        return None;
    }
    Some(Provided {
        blob_hash: outcome.blob_hash,
        blob_ticket: outcome.blob_ticket,
    })
}

/// Download a blob by ticket into `output_relative` (relative to `root_dir`).
/// Returns the number of bytes written on success.
pub async fn download_file(
    root_dir: PathBuf,
    blob_ticket: String,
    output_relative: String,
) -> Option<i64> {
    let root = root_dir.to_string_lossy().to_string();
    let outcome = tokio::task::spawn_blocking(move || {
        clipdock_p2p::download_file(root, blob_ticket, output_relative, TIMEOUT_MS)
    })
    .await
    .ok()?;
    if !outcome.ok {
        eprintln!("p2p download failed: {}", outcome.error_code);
        return None;
    }
    Some(outcome.local_bytes.max(outcome.downloaded_bytes))
}
