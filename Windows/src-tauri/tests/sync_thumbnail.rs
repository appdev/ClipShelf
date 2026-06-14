//! Verifies inbound thumbnail handling: after applying a remote image event,
//! the thumbnail is recorded as pending download; once attached, the item
//! surfaces a preview asset and is no longer pending.

use clipboard_core::{
    ClipboardCore, ItemQuery, PageRequest, SyncApplyEventsRequest, SyncEventRecord,
};
use clipdock_sync_contract::AssetDigest;
use serde_json::json;

fn image_event(content_hash: &str, digest: &str) -> SyncEventRecord {
    serde_json::from_value(json!({
        "server_seq": 1,
        "device_id": "device-remote",
        "client_event_id": "img-1",
        "type": "item_upsert",
        "content_hash": content_hash,
        "item_type": "image",
        "payload": {
            "summary": "Remote image",
            "thumbnail_digest": digest,
            "thumbnail_mime_type": "image/webp",
            "thumbnail_byte_count": 1234,
            "thumbnail_width": 320,
            "thumbnail_height": 200
        },
        "copy_count_delta": 1,
        "created_at_ms": 1_700_000_000_000_i64
    }))
    .unwrap()
}

#[test]
fn applied_remote_image_records_then_attaches_thumbnail() {
    let dir = tempfile::tempdir().unwrap();
    let mut core = ClipboardCore::open(dir.path()).unwrap();

    let content_hash = format!("blake3:{}", "a".repeat(64));
    let digest = AssetDigest::from_bytes(b"thumb-bytes").as_str().to_string();

    core.apply_sync_events(SyncApplyEventsRequest {
        sync_id: "sync_t".to_string(),
        device_id: "device-local".to_string(),
        events: vec![image_event(&content_hash, &digest)],
        next_cursor: 1,
    })
    .unwrap();

    // The image item exists and its thumbnail is pending download.
    let page = core
        .list_items(ItemQuery::default(), PageRequest::default())
        .unwrap();
    assert_eq!(page.items.len(), 1);
    let item_id = page.items[0].id.clone();
    assert!(
        page.items[0].preview_asset_path.is_none(),
        "no preview before download"
    );

    let pending = core.list_pending_thumbnail_downloads("sync_t").unwrap();
    assert_eq!(pending.len(), 1);
    assert_eq!(pending[0].digest, digest);
    assert_eq!(pending[0].width, 320);
    assert_eq!(pending[0].height, 200);

    // Simulate downloading and attaching the thumbnail.
    std::fs::write(dir.path().join("assets/thumb-x.webp"), b"thumb-bytes").unwrap();
    core.attach_remote_thumbnail(
        &item_id,
        "assets/thumb-x.webp",
        "image/webp",
        11,
        320,
        200,
        &digest,
    )
    .unwrap();

    // Now the item exposes a preview asset and is no longer pending.
    let page = core
        .list_items(ItemQuery::default(), PageRequest::default())
        .unwrap();
    assert!(
        page.items[0]
            .preview_asset_path
            .as_deref()
            .is_some_and(|path| path.ends_with("thumb-x.webp")),
        "preview asset should resolve to the attached thumbnail: {:?}",
        page.items[0].preview_asset_path
    );
    assert_eq!(
        core.list_pending_thumbnail_downloads("sync_t").unwrap().len(),
        0,
        "thumbnail should no longer be pending after attach"
    );
}
