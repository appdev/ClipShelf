//! P2P coordination: register this device's endpoint with the server so peers
//! can discover it, and expose device listing.
//!
//! Direct peer-to-peer blob transfer (iroh-blobs) requires running an iroh
//! node and multi-machine testing; that remains an optimization. The
//! coordination layer here — endpoint registration and discovery — is the
//! prerequisite and is exercised against the real server in tests.

use super::client::SyncClient;
use super::SyncCredentials;
use crate::core_state::CoreState;

/// Ensure a stable endpoint id exists in preferences, generating and
/// persisting one on first use. Derived from the device id so it is stable per
/// device. Returns `None` if P2P is disabled or sync is unconfigured.
fn ensure_endpoint_id(state: &CoreState) -> Result<Option<String>, String> {
    state.with_core(|core| {
        let mut prefs = core.get_preferences().map_err(|error| error.to_string())?;
        if !prefs.sync.p2p_enabled {
            return Ok(None);
        }
        if let Some(existing) = prefs
            .sync
            .endpoint_id
            .clone()
            .filter(|value| !value.is_empty())
        {
            return Ok(Some(existing));
        }
        let Some(device_id) = prefs.sync.device_id.clone().filter(|v| !v.is_empty()) else {
            return Ok(None);
        };
        let endpoint_id = format!("ep-{}", blake3::hash(device_id.as_bytes()).to_hex());
        prefs.sync.endpoint_id = Some(endpoint_id.clone());
        core.update_preferences(prefs)
            .map_err(|error| error.to_string())?;
        Ok(Some(endpoint_id))
    })
}

/// Announce this device's P2P endpoint to the server (TTL refresh). No-op when
/// P2P is disabled. Best-effort: errors are returned for the caller to log.
pub async fn announce_endpoint(
    state: &CoreState,
    credentials: &SyncCredentials,
) -> Result<(), String> {
    let Some(endpoint_id) = ensure_endpoint_id(state)? else {
        return Ok(());
    };
    let client = SyncClient::new(&credentials.server_url);
    client
        .report_p2p_endpoint(&credentials.token, &endpoint_id)
        .await
        .map_err(|error| error.to_message())
}
