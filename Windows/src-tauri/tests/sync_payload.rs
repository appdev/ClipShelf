//! Verifies inbound full-payload handling: a synced remote image carrying a
//! payload asset id + blob ticket surfaces as a pending P2P download; once the
//! payload is attached, the item is ready and no longer pending.

use clipboard_core::{
    ClipboardCore, ItemQuery, PageRequest, SyncApplyEventsRequest, SyncEventRecord,
};
use clipdock_sync_contract::AssetDigest;
use serde_json::json;

#[test]
fn applied_remote_image_records_then_attaches_payload() {
    let dir = tempfile::tempdir().unwrap();
    let mut core = ClipboardCore::open(dir.path()).unwrap();

    let content_hash = format!("blake3:{}", "a".repeat(64));
    let thumb_digest = AssetDigest::from_bytes(b"thumb").as_str().to_string();
    let payload_digest = AssetDigest::from_bytes(b"payload").as_str().to_string();
    let ticket = "blobabc123ticketstring";

    let event: SyncEventRecord = serde_json::from_value(json!({
        "server_seq": 1,
        "device_id": "device-remote",
        "client_event_id": "img-1",
        "type": "item_upsert",
        "content_hash": content_hash,
        "item_type": "image",
        "payload": {
            "summary": "Remote image",
            "thumbnail_digest": thumb_digest,
            "thumbnail_mime_type": "image/webp",
            "thumbnail_byte_count": 100,
            "thumbnail_width": 320,
            "thumbnail_height": 200,
            "payload_asset_id": payload_digest,
            "payload_blob_ticket": ticket,
            "mime_type": "image/png"
        },
        "copy_count_delta": 1,
        "created_at_ms": 1_700_000_000_000_i64
    }))
    .unwrap();

    core.apply_sync_events(SyncApplyEventsRequest {
        sync_id: "sync_p".to_string(),
        device_id: "device-local".to_string(),
        events: vec![event],
        next_cursor: 1,
    })
    .unwrap();

    // The full payload should be pending download, with the ticket recoverable.
    let pending = core.list_pending_payload_downloads("sync_p").unwrap();
    assert_eq!(pending.len(), 1);
    assert_eq!(pending[0].asset_id, payload_digest);
    let object: serde_json::Value =
        serde_json::from_str(&pending[0].source_payload_json).unwrap();
    assert_eq!(
        object.get("payload_blob_ticket").and_then(|v| v.as_str()),
        Some(ticket),
        "blob ticket must be recoverable from the stored payload json"
    );

    // Simulate the P2P download completing and attaching the payload.
    std::fs::write(dir.path().join("assets/payload-x.bin"), b"full-image-bytes").unwrap();
    core.attach_remote_payload(
        &pending[0].item_id,
        "assets/payload-x.bin",
        "image/png",
        16,
        0,
        0,
        &payload_digest,
    )
    .unwrap();

    // No longer pending, and the item exposes a payload asset path.
    assert_eq!(
        core.list_pending_payload_downloads("sync_p").unwrap().len(),
        0
    );
    let page = core
        .list_items(ItemQuery::default(), PageRequest::default())
        .unwrap();
    assert!(
        page.items[0]
            .payload_asset_path
            .as_deref()
            .is_some_and(|path| path.ends_with("payload-x.bin")),
        "payload asset should resolve after attach: {:?}",
        page.items[0].payload_asset_path
    );
}
