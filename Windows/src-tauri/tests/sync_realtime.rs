//! Live test of the realtime WebSocket path against the real server: a device
//! subscribed to `/v2/ws` must receive an `event_batch` shortly after another
//! device pushes an event over HTTP. Skipped when the server binary is absent.

use std::path::PathBuf;
use std::process::{Child, Command, Stdio};
use std::time::Duration;

use futures_util::StreamExt;
use std::io::{BufRead, BufReader};
use tokio_tungstenite::tungstenite::client::IntoClientRequest;
use tokio_tungstenite::tungstenite::Message;

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

#[tokio::test]
async fn websocket_delivers_realtime_event_batch() {
    if server_binary().is_none() {
        eprintln!("skipping sync_realtime: server binary not built");
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
    let pairing_code = create["data"]["pairing_code"].as_str().unwrap().to_string();
    let token_a = create["data"]["token"].as_str().unwrap().to_string();

    let join: serde_json::Value = http
        .post(format!("{}/v2/sync/join", server.base_url))
        .json(&serde_json::json!({ "pairing_code": pairing_code, "device_name": "Windows-B" }))
        .send()
        .await
        .unwrap()
        .json()
        .await
        .unwrap();
    let token_b = join["data"]["token"].as_str().unwrap().to_string();

    // Device B subscribes over WebSocket (mirrors realtime::run_connection).
    let ws_url = server.base_url.replacen("http://", "ws://", 1);
    let mut request = format!("{ws_url}/v2/ws?cursor=0&protocol_version=2")
        .into_client_request()
        .unwrap();
    request
        .headers_mut()
        .insert("Authorization", format!("Bearer {token_b}").parse().unwrap());
    let (mut stream, _resp) = tokio_tungstenite::connect_async(request)
        .await
        .expect("ws connect");

    // First message should be `hello`.
    let hello = next_text(&mut stream).await.expect("hello message");
    assert_eq!(hello["type"], "hello");

    // Device A pushes an event; device B should receive an event_batch.
    let hash = format!("blake3:{}", "d".repeat(64));
    http.post(format!("{}/v2/events", server.base_url))
        .bearer_auth(&token_a)
        .json(&serde_json::json!({
            "events": [{
                "client_event_id": "rt-1",
                "type": "item_upsert",
                "content_hash": hash,
                "item_type": "text",
                "payload": { "text": "realtime hello" },
                "copy_count_delta": 1
            }]
        }))
        .send()
        .await
        .unwrap();

    // Read messages until an event_batch arrives (ignore catchup_required etc).
    let mut found = false;
    for _ in 0..10 {
        let Some(value) = next_text_timeout(&mut stream, Duration::from_secs(5)).await else {
            break;
        };
        if value["type"] == "event_batch" {
            let events = value["events"].as_array().unwrap();
            assert!(events
                .iter()
                .any(|event| event["payload"]["text"] == "realtime hello"));
            found = true;
            break;
        }
    }
    assert!(found, "expected a realtime event_batch carrying the push");
}

async fn next_text<S>(stream: &mut S) -> Option<serde_json::Value>
where
    S: StreamExt<Item = Result<Message, tokio_tungstenite::tungstenite::Error>> + Unpin,
{
    while let Some(message) = stream.next().await {
        if let Ok(Message::Text(text)) = message {
            return serde_json::from_str(&text).ok();
        }
    }
    None
}

async fn next_text_timeout<S>(stream: &mut S, timeout: Duration) -> Option<serde_json::Value>
where
    S: StreamExt<Item = Result<Message, tokio_tungstenite::tungstenite::Error>> + Unpin,
{
    tokio::time::timeout(timeout, next_text(stream))
        .await
        .ok()
        .flatten()
}
