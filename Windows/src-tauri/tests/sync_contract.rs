//! Verifies the inbound sync wire format end to end: server-shaped JSON
//! (matching `Server::events::EventOut` / `SnapshotItem` / `SnapshotTombstone`)
//! must deserialize into `clipboard_core` sync records and apply cleanly. This
//! guards the cross-platform contract without needing a live server.

use clipboard_core::{
    ClipboardCore, ItemQuery, PageRequest, SyncApplyEventsRequest, SyncApplySnapshotRequest,
    SyncEventRecord, SyncSnapshotItemRecord, SyncSnapshotTombstoneRecord,
};
use serde::Deserialize;

const SYNC_ID: &str = "sync_test";
const LOCAL_DEVICE: &str = "device_local";
const REMOTE_DEVICE: &str = "device_remote";

#[derive(Deserialize)]
struct PullEventsResponse {
    events: Vec<SyncEventRecord>,
    next_cursor: i64,
}

#[derive(Deserialize)]
struct SnapshotResponse {
    snapshot_seq: i64,
    items: Vec<SyncSnapshotItemRecord>,
    tombstones: Vec<SyncSnapshotTombstoneRecord>,
}

#[test]
fn server_event_json_deserializes_and_applies() {
    // Shaped exactly like the server's pull-events `data` payload.
    let body = serde_json::json!({
        "events": [
            {
                "server_seq": 1,
                "device_id": REMOTE_DEVICE,
                "client_event_id": "evt-1",
                "type": "item_upsert",
                "content_hash": "blake3:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "item_type": "text",
                "payload": { "text": "hello from macOS" },
                "copy_count_delta": 1,
                "created_at_ms": 1_700_000_000_000_i64
            }
        ],
        "next_cursor": 1
    });

    let parsed: PullEventsResponse = serde_json::from_value(body).expect("deserialize events");
    assert_eq!(parsed.events.len(), 1);
    assert_eq!(parsed.next_cursor, 1);
    assert_eq!(parsed.events[0].event_type, "item_upsert");

    let dir = tempfile::tempdir().expect("tempdir");
    let mut core = ClipboardCore::open(dir.path()).expect("open core");

    let outcome = core
        .apply_sync_events(SyncApplyEventsRequest {
            sync_id: SYNC_ID.to_string(),
            device_id: LOCAL_DEVICE.to_string(),
            events: parsed.events,
            next_cursor: parsed.next_cursor,
        })
        .expect("apply events");

    assert_eq!(outcome.cursor, 1);
    assert_eq!(outcome.changed_item_ids.len(), 1);

    let page = core
        .list_items(ItemQuery::default(), PageRequest::default())
        .expect("list items");
    assert_eq!(page.items.len(), 1, "remote item should appear locally");
}

#[test]
fn server_snapshot_json_deserializes_and_applies() {
    let body = serde_json::json!({
        "snapshot_seq": 7,
        "items": [
            {
                "content_hash": "blake3:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                "item_type": "text",
                "payload": { "text": "snapshot entry" },
                "copy_count": 3,
                "updated_at_ms": 1_700_000_000_000_i64,
                "last_server_seq": 5
            }
        ],
        "tombstones": [
            {
                "content_hash": "blake3:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc",
                "deleted_at_ms": 1_700_000_000_001_i64,
                "last_server_seq": 6
            }
        ]
    });

    let parsed: SnapshotResponse = serde_json::from_value(body).expect("deserialize snapshot");
    assert_eq!(parsed.snapshot_seq, 7);
    assert_eq!(parsed.items.len(), 1);
    assert_eq!(parsed.tombstones.len(), 1);

    let dir = tempfile::tempdir().expect("tempdir");
    let mut core = ClipboardCore::open(dir.path()).expect("open core");

    let outcome = core
        .apply_sync_snapshot(SyncApplySnapshotRequest {
            sync_id: SYNC_ID.to_string(),
            device_id: LOCAL_DEVICE.to_string(),
            snapshot_seq: parsed.snapshot_seq,
            items: parsed.items,
            tombstones: parsed.tombstones,
        })
        .expect("apply snapshot");

    assert_eq!(outcome.snapshot_seq, 7);

    let page = core
        .list_items(ItemQuery::default(), PageRequest::default())
        .expect("list items");
    assert_eq!(page.items.len(), 1, "snapshot item should appear locally");
    assert_eq!(page.items[0].copy_count, 3);
}
