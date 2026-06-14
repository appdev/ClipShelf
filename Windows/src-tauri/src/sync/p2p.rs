//! P2P coordination: register this device's endpoint with the server so peers
//! can discover it, and expose device listing.
//!
//! Direct peer-to-peer blob transfer (iroh-blobs) requires running an iroh
//! node and multi-machine testing; that remains an optimization. The
//! coordination layer here — endpoint registration and discovery — is the
//! prerequisite and is exercised against the real server in tests.

use super::client::SyncClient;
use super::{p2p_transport, SyncCredentials};
use crate::core_state::CoreState;

/// Whether P2P is enabled in preferences.
fn p2p_enabled(state: &CoreState) -> Result<bool, String> {
    state.with_core(|core| {
        core.get_preferences()
            .map(|prefs| prefs.sync.p2p_enabled)
            .map_err(|error| error.to_string())
    })
}

/// Persist the real iroh endpoint id into preferences for reference.
fn store_endpoint_id(state: &CoreState, endpoint_id: &str) {
    let _ = state.with_core(|core| {
        let mut prefs = core.get_preferences().map_err(|error| error.to_string())?;
        prefs.sync.endpoint_id = Some(endpoint_id.to_string());
        core.update_preferences(prefs)
            .map(|_| ())
            .map_err(|error| error.to_string())
    });
}

/// Announce this device's real iroh P2P endpoint (id + reachable addresses) to
/// the server so peers can connect for blob transfer. No-op when P2P is
/// disabled. Best-effort.
pub async fn announce_endpoint(
    state: &CoreState,
    credentials: &SyncCredentials,
) -> Result<(), String> {
    if !p2p_enabled(state)? {
        return Ok(());
    }
    let Some(node) = p2p_transport::start_node(state.root_dir().clone()).await else {
        return Ok(());
    };
    store_endpoint_id(state, &node.endpoint_id);

    let client = SyncClient::new(&credentials.server_url);
    client
        .report_p2p_endpoint(
            &credentials.token,
            &node.endpoint_id,
            node.relay_url.as_deref(),
            node.direct_addresses,
        )
        .await
        .map_err(|error| error.to_message())
}
