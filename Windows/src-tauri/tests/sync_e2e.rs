//! Live end-to-end bidirectional sync test against the real sync server.
//!
//! Spawns the `clipdock-sync-server` binary, then exercises the full path a
//! Windows device takes to share a capture: create/join space, capture text,
//! mark pending, build the outbound event from `clipboard_core`, push it, and
//! confirm a second device pulls and applies it into its own history.
//!
//! The test is skipped (passes as a no-op) when the server binary has not been
//! built, so it never breaks a plain `cargo test`. Build it first with
//! `cargo build` in the `Server/` crate to enable the test.

use std::io::{BufRead, BufReader};
use std::path::PathBuf;
use std::process::{Child, Command, Stdio};
use std::time::Duration;

use clipboard_core::{
    CaptureTextRequest, ClipboardCore, ItemQuery, PageRequest, SourceConfidence,
    SyncApplyEventsRequest, SyncEventRecord, SyncLocalPendingRequest, SyncUploadedEvent,
};
use serde_json::json;

fn server_binary() -> Option<PathBuf> {
    let path = PathBuf::from(env!("CARGO_MANIFEST_DIR"))
        .join("../../Server/target/debug/clipdock-sync-server");
    path.exists().then(|| path)
}

struct ServerHandle {
    child: Child,
    base_url: String,
    _data_dir: tempfile::TempDir,
}

impl Drop for ServerHandle {
    fn drop(&mut self) {
        let _ = self.child.kill();
        let _ = self.child.wait();
    }
}

fn start_server() -> ServerHandle {
    let binary = server_binary().expect("server binary present");
    let data_dir = tempfile::tempdir().expect("tempdir");
    let mut child = Command::new(binary)
        .arg("--bind")
        .arg("127.0.0.1:0")
        .arg("--database")
        .arg(data_dir.path().join("sync.sqlite"))
        .arg("--assets")
        .arg(data_dir.path().join("assets"))
        .stdout(Stdio::piped())
        .stderr(Stdio::piped())
        .spawn()
        .expect("spawn server");

    // The server logs "listening on http://HOST:PORT" once bound.
    let stdout = child.stdout.take().expect("server stdout");
    let mut reader = BufReader::new(stdout);
    let mut base_url = String::new();
    let mut line = String::new();
    for _ in 0..50 {
        line.clear();
        if reader.read_line(&mut line).unwrap_or(0) == 0 {
            std::thread::sleep(Duration::from_millis(100));
            continue;
        }
        if let Some(idx) = line.find("http://") {
            base_url = line[idx..].trim().to_string();
            break;
        }
    }
    assert!(!base_url.is_empty(), "server did not report a listen address");

    ServerHandle {
        child,
        base_url,
        _data_dir: data_dir,
    }
}

fn capture_and_mark_pending(core: &mut ClipboardCore, sync_id: &str, text: &str) -> String {
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

    core.mark_sync_local_pending(SyncLocalPendingRequest {
        sync_id: sync_id.to_string(),
        content_hash: format!("blake3:{}", result.content_hash),
        item_id: Some(result.item_id),
        client_event_id: "win-e2e-1".to_string(),
    })
    .expect("mark pending");

    result.content_hash
}

#[tokio::test]
async fn windows_capture_syncs_to_another_device() {
    let Some(_) = server_binary() else {
        eprintln!("skipping sync_e2e: server binary not built");
        return;
    };

    let server = start_server();
    let http = reqwest::Client::new();

    // Device A creates the space.
    let create: serde_json::Value = http
        .post(format!("{}/v2/sync/create", server.base_url))
        .json(&json!({ "device_name": "device-A" }))
        .send()
        .await
        .expect("create send")
        .json()
        .await
        .expect("create json");
    let sync_id = create["data"]["sync_id"].as_str().unwrap().to_string();
    let pairing_code = create["data"]["pairing_code"].as_str().unwrap().to_string();
    let token_a = create["data"]["token"].as_str().unwrap().to_string();

    // Device B (the Windows client) joins.
    let join: serde_json::Value = http
        .post(format!("{}/v2/sync/join", server.base_url))
        .json(&json!({ "pairing_code": pairing_code, "device_name": "Windows-B" }))
        .send()
        .await
        .expect("join send")
        .json()
        .await
        .expect("join json");
    let token_b = join["data"]["token"].as_str().unwrap().to_string();

    // Device B captures text and builds an outbound event via clipboard_core.
    let dir_b = tempfile::tempdir().expect("tempdir b");
    let mut core_b = ClipboardCore::open(dir_b.path()).expect("open core b");
    capture_and_mark_pending(&mut core_b, &sync_id, "shared from Windows");

    let pending = core_b
        .list_pending_sync_events(&sync_id)
        .expect("list pending");
    assert_eq!(pending.len(), 1, "one event should be pending");
    let event = &pending[0];

    // Push it to the server (mirrors SyncClient::push_events).
    let push: serde_json::Value = http
        .post(format!("{}/v2/events", server.base_url))
        .bearer_auth(&token_b)
        .json(&json!({
            "events": [{
                "client_event_id": event.client_event_id,
                "type": "item_upsert",
                "content_hash": event.content_hash,
                "item_type": event.item_type,
                "payload": event.payload,
                "copy_count_delta": event.copy_count_delta,
            }]
        }))
        .send()
        .await
        .expect("push send")
        .json()
        .await
        .expect("push json");
    let server_seq = push["data"]["events"][0]["server_seq"].as_i64().unwrap();
    assert!(server_seq >= 1);

    core_b
        .mark_sync_events_uploaded(
            &sync_id,
            &[SyncUploadedEvent {
                content_hash: event.content_hash.clone(),
                server_seq,
            }],
        )
        .expect("mark uploaded");
    assert_eq!(
        core_b.list_pending_sync_events(&sync_id).unwrap().len(),
        0,
        "pending set should be empty after ack"
    );

    // Device A pulls and applies — the Windows capture should appear.
    let pull: serde_json::Value = http
        .get(format!("{}/v2/events?after=0&limit=200", server.base_url))
        .bearer_auth(&token_a)
        .send()
        .await
        .expect("pull send")
        .json()
        .await
        .expect("pull json");

    let events: Vec<SyncEventRecord> =
        serde_json::from_value(pull["data"]["events"].clone()).expect("decode events");
    let next_cursor = pull["data"]["next_cursor"].as_i64().unwrap();
    assert_eq!(events.len(), 1, "device A should receive one event");

    let dir_a = tempfile::tempdir().expect("tempdir a");
    let mut core_a = ClipboardCore::open(dir_a.path()).expect("open core a");
    core_a
        .apply_sync_events(SyncApplyEventsRequest {
            sync_id: sync_id.clone(),
            device_id: "device-A-local".to_string(),
            events,
            next_cursor,
        })
        .expect("apply events");

    let page = core_a
        .list_items(ItemQuery::default(), PageRequest::default())
        .expect("list items");
    assert_eq!(page.items.len(), 1, "applied item should appear on device A");
    assert_eq!(page.items[0].summary, "shared from Windows");
}
