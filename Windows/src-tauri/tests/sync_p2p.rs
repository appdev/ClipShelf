//! Live test of the P2P coordination layer: a device that reports its endpoint
//! must appear in the sync space's device listing. Skipped when the server
//! binary is absent.

use std::io::{BufRead, BufReader};
use std::path::PathBuf;
use std::process::{Child, Command, Stdio};
use std::time::Duration;

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

#[tokio::test]
async fn reported_endpoint_appears_in_device_listing() {
    if server_binary().is_none() {
        eprintln!("skipping sync_p2p: server binary not built");
        return;
    }

    let server = start_server();
    let http = reqwest::Client::new();

    let create: serde_json::Value = http
        .post(format!("{}/v2/sync/create", server.base_url))
        .json(&serde_json::json!({ "device_name": "Windows-Endpoint" }))
        .send()
        .await
        .unwrap()
        .json()
        .await
        .unwrap();
    let token = create["data"]["token"].as_str().unwrap().to_string();
    let device_id = create["data"]["device_id"].as_str().unwrap().to_string();

    // Report this device's P2P endpoint (mirrors SyncClient::report_p2p_endpoint).
    let endpoint_id = format!("ep-{}", blake3::hash(device_id.as_bytes()).to_hex());
    let report = http
        .put(format!("{}/v2/p2p/endpoint", server.base_url))
        .bearer_auth(&token)
        .json(&serde_json::json!({
            "endpoint_id": endpoint_id,
            "direct_addresses": []
        }))
        .send()
        .await
        .unwrap();
    assert!(report.status().is_success(), "endpoint report failed: {}", report.status());

    // The device should now be discoverable via the device listing.
    let devices: serde_json::Value = http
        .get(format!("{}/v2/p2p/devices", server.base_url))
        .bearer_auth(&token)
        .send()
        .await
        .unwrap()
        .json()
        .await
        .unwrap();
    let list = devices["data"]["devices"].as_array().unwrap();
    assert!(
        list.iter().any(|device| device["device_id"] == device_id
            && device["endpoint"]["endpoint_id"] == endpoint_id),
        "reported endpoint should be listed: {list:?}"
    );
}
