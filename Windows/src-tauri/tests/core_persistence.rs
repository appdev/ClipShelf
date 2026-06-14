//! Integration coverage for the storage round-trip that the Windows Tauri
//! commands rely on. These exercise `clipboard_core` exactly as the command
//! layer does (capture -> list), and verify the central "history survives a
//! restart" guarantee by reopening the database against the same directory.

use clipboard_core::{
    CaptureImageRequest, CaptureTextRequest, ClipboardCore, ClipboardItemType, ItemQuery,
    PageRequest, SourceConfidence,
};
use std::fs;

fn text_request(text: &str) -> CaptureTextRequest {
    CaptureTextRequest {
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
    }
}

#[test]
fn capture_text_round_trips_through_list() {
    let dir = tempfile::tempdir().expect("tempdir");
    let mut core = ClipboardCore::open(dir.path()).expect("open core");

    core.capture_text(text_request("hello clipdock"))
        .expect("capture text");

    let page = core
        .list_items(ItemQuery::default(), PageRequest::default())
        .expect("list items");

    assert_eq!(page.items.len(), 1);
    assert_eq!(page.items[0].item_type, ClipboardItemType::Text);
    assert_eq!(page.items[0].summary, "hello clipdock");
}

#[test]
fn recapturing_same_text_bumps_copy_count_without_duplicating() {
    let dir = tempfile::tempdir().expect("tempdir");
    let mut core = ClipboardCore::open(dir.path()).expect("open core");

    core.capture_text(text_request("repeated")).expect("first");
    core.capture_text(text_request("repeated")).expect("second");

    let page = core
        .list_items(ItemQuery::default(), PageRequest::default())
        .expect("list items");

    assert_eq!(page.items.len(), 1, "duplicate content must not create a row");
    assert_eq!(page.items[0].copy_count, 2);
}

#[test]
fn history_survives_reopening_the_database() {
    let dir = tempfile::tempdir().expect("tempdir");

    {
        let mut core = ClipboardCore::open(dir.path()).expect("open core");
        core.capture_text(text_request("persisted entry"))
            .expect("capture text");
    }

    // Reopen against the same directory, mirroring an app restart.
    let core = ClipboardCore::open(dir.path()).expect("reopen core");
    let page = core
        .list_items(ItemQuery::default(), PageRequest::default())
        .expect("list items");

    assert_eq!(page.items.len(), 1);
    assert_eq!(page.items[0].summary, "persisted entry");
}

#[test]
fn capture_image_records_payload_relative_to_root() {
    let dir = tempfile::tempdir().expect("tempdir");
    let mut core = ClipboardCore::open(dir.path()).expect("open core");

    // `ClipboardCore::open` provisions an `assets/` directory under the root;
    // the Windows capture path writes the PNG there before recording it.
    let relative_path = "assets/clipboard-image-test.png";
    let payload_path = dir.path().join(relative_path);
    fs::write(&payload_path, b"fake-png-bytes").expect("write payload");

    let request = CaptureImageRequest {
        payload_relative_path: relative_path.to_string(),
        preview_relative_path: None,
        mime_type: Some("image/png".to_string()),
        width: 320,
        height: 200,
        byte_count: 0,
        source_bundle_id: None,
        source_app_name: None,
        source_bundle_path: None,
        source_icon_relative_path: None,
        source_confidence: SourceConfidence::Unknown,
        pasteboard_change_count: 0,
        self_write_token: None,
    };

    core.capture_image(request).expect("capture image");

    let page = core
        .list_items(ItemQuery::default(), PageRequest::default())
        .expect("list items");

    assert_eq!(page.items.len(), 1);
    assert_eq!(page.items[0].item_type, ClipboardItemType::Image);
}
