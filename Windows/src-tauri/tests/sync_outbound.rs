//! Covers the outbound sync path: a locally captured item marked pending must
//! surface as an upload-ready event with the correct wire payload, and once
//! acknowledged it must transition out of the pending set.

use clipboard_core::{
    CaptureTextRequest, ClipboardCore, SourceConfidence, SyncLocalPendingRequest, SyncUploadedEvent,
};

const SYNC_ID: &str = "sync_out";

fn capture(core: &mut ClipboardCore, text: &str) -> (String, String) {
    let result = core
        .capture_text(CaptureTextRequest {
            text: text.to_string(),
            detected_link: None,
            display_rtf_relative_path: None,
            display_rtf_mime_type: None,
            display_rtf_byte_count: 0,
            source_bundle_id: None,
            source_app_name: None,
            source_bundle_path: None,
            source_icon_relative_path: None,
            source_confidence: SourceConfidence::Unknown,
            pasteboard_change_count: 0,
            self_write_token: None,
        })
        .expect("capture text");
    (result.content_hash, result.item_id)
}

#[test]
fn captured_text_marked_pending_becomes_upload_ready_event() {
    let dir = tempfile::tempdir().expect("tempdir");
    let mut core = ClipboardCore::open(dir.path()).expect("open core");

    let (content_hash, item_id) = capture(&mut core, "sync me to other devices");
    let wire_hash = format!("blake3:{content_hash}");
    core.mark_sync_local_pending(SyncLocalPendingRequest {
        sync_id: SYNC_ID.to_string(),
        content_hash: wire_hash.clone(),
        item_id: Some(item_id),
        client_event_id: "win-evt-1".to_string(),
    })
    .expect("mark pending");

    let pending = core
        .list_pending_sync_events(SYNC_ID)
        .expect("list pending");

    assert_eq!(pending.len(), 1);
    let event = &pending[0];
    assert_eq!(event.content_hash, wire_hash);
    assert_eq!(event.item_type, "text");
    assert_eq!(event.client_event_id, "win-evt-1");
    assert_eq!(
        event.payload.get("text").and_then(|value| value.as_str()),
        Some("sync me to other devices")
    );
    assert!(event.copy_count_delta >= 1);
}

#[test]
fn acknowledged_event_leaves_the_pending_set() {
    let dir = tempfile::tempdir().expect("tempdir");
    let mut core = ClipboardCore::open(dir.path()).expect("open core");

    let (content_hash, item_id) = capture(&mut core, "ack me");
    let wire_hash = format!("blake3:{content_hash}");
    core.mark_sync_local_pending(SyncLocalPendingRequest {
        sync_id: SYNC_ID.to_string(),
        content_hash: wire_hash.clone(),
        item_id: Some(item_id),
        client_event_id: "win-evt-2".to_string(),
    })
    .expect("mark pending");

    assert_eq!(core.list_pending_sync_events(SYNC_ID).unwrap().len(), 1);

    let result = core
        .mark_sync_events_uploaded(
            SYNC_ID,
            &[SyncUploadedEvent {
                content_hash: wire_hash,
                server_seq: 42,
            }],
        )
        .expect("mark uploaded");
    assert_eq!(result.affected_count, 1);

    assert_eq!(
        core.list_pending_sync_events(SYNC_ID).unwrap().len(),
        0,
        "uploaded item must no longer be pending"
    );
}

#[test]
fn copy_count_delta_does_not_drift_on_recopy() {
    let dir = tempfile::tempdir().expect("tempdir");
    let mut core = ClipboardCore::open(dir.path()).expect("open core");

    // First capture + upload: delta should be the full count (1).
    let (hash, item_id) = capture(&mut core, "drift check");
    let wire = format!("blake3:{hash}");
    core.mark_sync_local_pending(SyncLocalPendingRequest {
        sync_id: SYNC_ID.to_string(),
        content_hash: wire.clone(),
        item_id: Some(item_id),
        client_event_id: "d-1".to_string(),
    })
    .expect("mark pending");
    let first = core.list_pending_sync_events(SYNC_ID).expect("list");
    assert_eq!(first[0].copy_count_delta, 1);
    core.mark_sync_events_uploaded(
        SYNC_ID,
        &[SyncUploadedEvent {
            content_hash: wire.clone(),
            server_seq: 1,
        }],
    )
    .expect("ack");

    // Re-copy the same content (copy_count -> 2), re-mark pending.
    let (hash2, item_id2) = capture(&mut core, "drift check");
    assert_eq!(hash2, hash, "same content => same hash");
    core.mark_sync_local_pending(SyncLocalPendingRequest {
        sync_id: SYNC_ID.to_string(),
        content_hash: wire.clone(),
        item_id: Some(item_id2),
        client_event_id: "d-2".to_string(),
    })
    .expect("mark pending again");

    // Delta must be the increment (2 - 1 = 1), not the full count (2).
    let second = core.list_pending_sync_events(SYNC_ID).expect("list");
    assert_eq!(
        second[0].copy_count_delta, 1,
        "re-copy should send the increment, not the full count"
    );
}

#[test]
fn image_captures_are_not_listed_for_outbound() {
    // Images require asset transfer, so they must not appear as pending text
    // events even if marked pending.
    let dir = tempfile::tempdir().expect("tempdir");
    let mut core = ClipboardCore::open(dir.path()).expect("open core");

    let relative_path = "assets/clipboard-image-out.png";
    std::fs::write(dir.path().join(relative_path), b"fake-png").expect("write payload");
    let result = core
        .capture_image(clipboard_core::CaptureImageRequest {
            payload_relative_path: relative_path.to_string(),
            preview_relative_path: None,
            mime_type: Some("image/png".to_string()),
            width: 10,
            height: 10,
            byte_count: 0,
            source_bundle_id: None,
            source_app_name: None,
            source_bundle_path: None,
            source_icon_relative_path: None,
            source_confidence: SourceConfidence::Unknown,
            pasteboard_change_count: 0,
            self_write_token: None,
        })
        .expect("capture image");

    core.mark_sync_local_pending(SyncLocalPendingRequest {
        sync_id: SYNC_ID.to_string(),
        content_hash: format!("blake3:{}", result.content_hash),
        item_id: Some(result.item_id),
        client_event_id: "win-img-1".to_string(),
    })
    .expect("mark pending");

    assert_eq!(
        core.list_pending_sync_events(SYNC_ID).unwrap().len(),
        0,
        "image items must be excluded from outbound text events"
    );
}
