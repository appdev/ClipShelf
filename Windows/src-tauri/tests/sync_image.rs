//! Live test of outbound image sync: a captured image generates a WebP
//! thumbnail that uploads to the server, and the image upsert event (carrying
//! thumbnail metadata) is received and applied by another device. Skipped when
//! the server binary is absent.

use std::io::{BufRead, BufReader};
use std::path::PathBuf;
use std::process::{Child, Command, Stdio};
use std::time::Duration;

use clipboard_core::{
    CaptureImageRequest, ClipboardCore, ItemQuery, PageRequest, SourceConfidence,
    SyncApplyEventsRequest, SyncEventRecord, SyncLocalPendingRequest,
};
use clipdock_sync_contract::{
    AssetDigest, ASSET_KIND_THUMBNAIL, SYNC_THUMBNAIL_MIME_TYPE, THUMBNAIL_DETAIL_TARGET_BYTES,
    THUMBNAIL_MAX_BYTES, THUMBNAIL_NORMAL_TARGET_BYTES,
};
use clipdock_thumbnail_codec::encode_adaptive_thumbnail_webp_rgba;

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
        .args([
            "--bind",
            "127.0.0.1:0",
            "--database",
            data_dir.path().join("sync.sqlite").to_str().unwrap(),
            "--assets",
            data_dir.path().join("assets").to_str().unwrap(),
        ])
        .stdout(Stdio::piped())
        .stderr(Stdio::piped())
        .spawn()
        .expect("spawn server");

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
    assert!(!base_url.is_empty());
    ServerHandle {
        child,
        base_url,
        _data_dir: data_dir,
    }
}

/// Write a deterministic PNG into the core's assets dir and return the relative
/// path, mirroring how the clipboard bridge stages captured images.
fn write_test_png(root: &std::path::Path, width: u32, height: u32) -> String {
    let relative = "assets/clipboard-image-itest.png";
    let mut img = image::RgbaImage::new(width, height);
    for (x, y, pixel) in img.enumerate_pixels_mut() {
        *pixel = image::Rgba([(x % 256) as u8, (y % 256) as u8, 128, 255]);
    }
    img.save(root.join(relative)).expect("save png");
    relative.to_string()
}

#[tokio::test]
async fn image_capture_syncs_thumbnail_to_another_device() {
    if server_binary().is_none() {
        eprintln!("skipping sync_image: server binary not built");
        return;
    }

    let server = start_server();
    let http = reqwest::Client::new();

    let create: serde_json::Value = http
        .post(format!("{}/v2/sync/create", server.base_url))
        .json(&serde_json::json!({ "device_name": "device-A" }))
        .send()
        .await
        .unwrap()
        .json()
        .await
        .unwrap();
    let sync_id = create["data"]["sync_id"].as_str().unwrap().to_string();
    let pairing = create["data"]["pairing_code"].as_str().unwrap().to_string();
    let token_a = create["data"]["token"].as_str().unwrap().to_string();

    let join: serde_json::Value = http
        .post(format!("{}/v2/sync/join", server.base_url))
        .json(&serde_json::json!({ "pairing_code": pairing, "device_name": "Windows-B" }))
        .send()
        .await
        .unwrap()
        .json()
        .await
        .unwrap();
    let token_b = join["data"]["token"].as_str().unwrap().to_string();

    // Device B captures an image and marks it pending.
    let dir_b = tempfile::tempdir().unwrap();
    let mut core_b = ClipboardCore::open(dir_b.path()).unwrap();
    let relative = write_test_png(dir_b.path(), 640, 400);
    let captured = core_b
        .capture_image(CaptureImageRequest {
            payload_relative_path: relative.clone(),
            preview_relative_path: None,
            mime_type: Some("image/png".to_string()),
            width: 640,
            height: 400,
            byte_count: 0,
            source_bundle_id: None,
            source_app_name: None,
            source_bundle_path: None,
            source_icon_relative_path: None,
            source_confidence: SourceConfidence::Unknown,
            pasteboard_change_count: 0,
            self_write_token: None,
        })
        .unwrap();
    core_b
        .mark_sync_local_pending(SyncLocalPendingRequest {
            sync_id: sync_id.clone(),
            content_hash: format!("blake3:{}", captured.content_hash),
            item_id: Some(captured.item_id),
            client_event_id: "img-evt-1".to_string(),
        })
        .unwrap();

    // The image must surface via the dedicated pending-image query.
    let pending = core_b.list_pending_image_sync_events(&sync_id).unwrap();
    assert_eq!(pending.len(), 1);
    let image = &pending[0];

    // Generate a thumbnail (mirrors sync::assets::generate_thumbnail).
    let payload_path = dir_b.path().join(&image.payload_relative_path);
    let decoded = image::ImageReader::open(&payload_path)
        .unwrap()
        .decode()
        .unwrap()
        .to_rgba8();
    let (tw, th) = (decoded.width(), decoded.height());
    let thumb = encode_adaptive_thumbnail_webp_rgba(
        &decoded.into_raw(),
        tw,
        th,
        THUMBNAIL_NORMAL_TARGET_BYTES,
        THUMBNAIL_DETAIL_TARGET_BYTES,
        THUMBNAIL_MAX_BYTES,
    )
    .unwrap()
    .expect("thumbnail generated");
    let digest = AssetDigest::from_bytes(&thumb.bytes);

    // Upload the thumbnail to the server.
    let upload = http
        .put(format!("{}/v2/assets/{}", server.base_url, digest.as_str()))
        .bearer_auth(&token_b)
        .header("content-type", SYNC_THUMBNAIL_MIME_TYPE)
        .header("x-clipdock-asset-kind", ASSET_KIND_THUMBNAIL)
        .header("x-clipdock-asset-width", thumb.width.to_string())
        .header("x-clipdock-asset-height", thumb.height.to_string())
        .body(thumb.bytes.clone())
        .send()
        .await
        .unwrap();
    assert!(upload.status().is_success(), "thumbnail upload failed: {}", upload.status());

    // Push the image upsert event carrying thumbnail metadata.
    http.post(format!("{}/v2/events", server.base_url))
        .bearer_auth(&token_b)
        .json(&serde_json::json!({
            "events": [{
                "client_event_id": image.client_event_id,
                "type": "item_upsert",
                "content_hash": image.content_hash,
                "item_type": "image",
                "payload": {
                    "summary": image.summary,
                    "thumbnail_digest": digest.as_str(),
                    "thumbnail_mime_type": SYNC_THUMBNAIL_MIME_TYPE,
                    "thumbnail_byte_count": thumb.bytes.len(),
                    "thumbnail_width": thumb.width,
                    "thumbnail_height": thumb.height,
                },
                "copy_count_delta": 1
            }]
        }))
        .send()
        .await
        .unwrap();

    // Device A pulls and applies — the image item should appear.
    let pull: serde_json::Value = http
        .get(format!("{}/v2/events?after=0&limit=200", server.base_url))
        .bearer_auth(&token_a)
        .send()
        .await
        .unwrap()
        .json()
        .await
        .unwrap();
    let events: Vec<SyncEventRecord> =
        serde_json::from_value(pull["data"]["events"].clone()).unwrap();
    let next_cursor = pull["data"]["next_cursor"].as_i64().unwrap();
    assert_eq!(events.len(), 1);

    let dir_a = tempfile::tempdir().unwrap();
    let mut core_a = ClipboardCore::open(dir_a.path()).unwrap();
    core_a
        .apply_sync_events(SyncApplyEventsRequest {
            sync_id,
            device_id: "device-A".to_string(),
            events,
            next_cursor,
        })
        .unwrap();
    let page = core_a
        .list_items(ItemQuery::default(), PageRequest::default())
        .unwrap();
    assert_eq!(page.items.len(), 1);
    assert_eq!(
        page.items[0].item_type,
        clipboard_core::ClipboardItemType::Image
    );

    // The uploaded thumbnail must be downloadable from the server.
    let download = http
        .get(format!("{}/v2/assets/{}", server.base_url, digest.as_str()))
        .bearer_auth(&token_a)
        .send()
        .await
        .unwrap();
    assert!(download.status().is_success());
    let bytes = download.bytes().await.unwrap();
    assert_eq!(bytes.len(), thumb.bytes.len(), "thumbnail bytes round-trip");
}
