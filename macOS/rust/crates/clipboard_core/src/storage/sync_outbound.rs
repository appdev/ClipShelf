//! Outbound sync: enumerate locally captured items that are pending upload,
//! shaped into the wire payloads other devices expect, and mark them synced
//! once the server acknowledges them.
//!
//! This is the encode-side counterpart to `sync_apply::map_remote_item`. The
//! payload field names here must match what that decoder reads (`text`,
//! `url`/`text`, `hex`, `plain_text`). Asset-backed types (image, file) are
//! intentionally skipped until asset transfer is implemented — they cannot be
//! reconstructed on the remote without the payload asset.

use crate::domain::{SyncPendingEvent, SyncPendingImage, SyncUploadedEvent};
use crate::error::Result;
use crate::time::now_ms;
use clipdock_sync_contract::BLAKE3_PREFIX;
use rusqlite::{params, OptionalExtension};
use serde_json::json;

use super::ClipboardCore;

/// `sync_item_state.content_hash` is stored in bare (prefix-stripped) form,
/// while the wire protocol uses the `blake3:` prefix. Convert at the boundary.
fn to_wire_hash(local_hash: &str) -> String {
    if local_hash.starts_with(BLAKE3_PREFIX) {
        local_hash.to_string()
    } else {
        format!("{BLAKE3_PREFIX}{local_hash}")
    }
}

fn to_local_hash(wire_hash: &str) -> String {
    wire_hash
        .strip_prefix(BLAKE3_PREFIX)
        .unwrap_or(wire_hash)
        .to_string()
}

impl ClipboardCore {
    /// List items marked `local_pending_upload` for the given sync space, in
    /// capture order, shaped into upload-ready events. Asset-backed types are
    /// skipped (they require asset transfer first).
    pub fn list_pending_sync_events(
        &mut self,
        sync_id: impl AsRef<str>,
    ) -> Result<Vec<SyncPendingEvent>> {
        let sync_id = sync_id.as_ref().trim().to_string();
        if sync_id.is_empty() {
            return Ok(Vec::new());
        }

        let transaction = self.connection.transaction()?;
        let mut events = Vec::new();
        {
            let mut statement = transaction.prepare(
                r#"
                SELECT
                    state.content_hash,
                    state.local_pending_event_id,
                    item.id,
                    item.type,
                    item.primary_text,
                    item.summary,
                    item.copy_count
                FROM sync_item_state AS state
                INNER JOIN clipboard_items AS item ON item.id = state.item_id
                WHERE state.sync_id = ?1
                    AND state.provenance = 'local_pending_upload'
                    AND item.deleted_at_ms IS NULL
                ORDER BY item.last_copied_at_ms ASC
                "#,
            )?;

            let rows = statement.query_map(params![sync_id], |row| {
                Ok(PendingRow {
                    content_hash: row.get(0)?,
                    client_event_id: row.get::<_, Option<String>>(1)?,
                    item_id: row.get(2)?,
                    item_type: row.get(3)?,
                    primary_text: row.get::<_, Option<String>>(4)?,
                    summary: row.get(5)?,
                    copy_count: row.get(6)?,
                })
            })?;

            for row in rows {
                let row = row?;
                if let Some(event) = build_pending_event(&transaction, row)? {
                    events.push(event);
                }
            }
        }
        transaction.commit()?;
        Ok(events)
    }

    /// List image items marked `local_pending_upload` together with their
    /// payload asset location, so the caller can generate and upload a
    /// thumbnail before constructing the image upsert event.
    pub fn list_pending_image_sync_events(
        &mut self,
        sync_id: impl AsRef<str>,
    ) -> Result<Vec<SyncPendingImage>> {
        let sync_id = sync_id.as_ref().trim().to_string();
        if sync_id.is_empty() {
            return Ok(Vec::new());
        }

        let transaction = self.connection.transaction()?;
        let mut images = Vec::new();
        {
            let mut statement = transaction.prepare(
                r#"
                SELECT
                    state.content_hash,
                    state.local_pending_event_id,
                    item.id,
                    item.summary,
                    asset.relative_path,
                    asset.width,
                    asset.height,
                    asset.byte_count,
                    asset.mime_type
                FROM sync_item_state AS state
                INNER JOIN clipboard_items AS item ON item.id = state.item_id
                INNER JOIN clipboard_assets AS asset
                    ON asset.item_id = item.id AND asset.kind = 'payload'
                WHERE state.sync_id = ?1
                    AND state.provenance = 'local_pending_upload'
                    AND item.type = 'image'
                    AND item.deleted_at_ms IS NULL
                ORDER BY item.last_copied_at_ms ASC
                "#,
            )?;

            let rows = statement.query_map(params![sync_id], |row| {
                let content_hash: String = row.get(0)?;
                let client_event_id: Option<String> = row.get(1)?;
                Ok(SyncPendingImage {
                    content_hash: to_wire_hash(&content_hash),
                    item_id: row.get(2)?,
                    summary: row.get(3)?,
                    payload_relative_path: row.get(4)?,
                    width: row.get::<_, Option<i64>>(5)?.unwrap_or(0),
                    height: row.get::<_, Option<i64>>(6)?.unwrap_or(0),
                    byte_count: row.get(7)?,
                    mime_type: row
                        .get::<_, Option<String>>(8)?
                        .unwrap_or_else(|| "image/png".to_string()),
                    client_event_id: client_event_id
                        .filter(|value| !value.trim().is_empty())
                        .unwrap_or_else(|| format!("local-{content_hash}")),
                })
            })?;

            for row in rows {
                images.push(row?);
            }
        }
        transaction.commit()?;
        Ok(images)
    }

    /// Transition acknowledged events from `local_pending_upload` to
    /// `synced_local`, recording the assigned server sequence. Returns the
    /// number of rows updated.
    pub fn mark_sync_events_uploaded(
        &mut self,
        sync_id: impl AsRef<str>,
        uploads: &[SyncUploadedEvent],
    ) -> Result<crate::domain::ItemManagementResult> {
        let sync_id = sync_id.as_ref().trim().to_string();
        if sync_id.is_empty() || uploads.is_empty() {
            return Ok(crate::domain::ItemManagementResult { affected_count: 0 });
        }

        let now = now_ms();
        let transaction = self.connection.transaction()?;
        let mut affected = 0_i64;
        for upload in uploads {
            affected += transaction.execute(
                r#"
                UPDATE sync_item_state
                SET provenance = 'synced_local',
                    local_status = 'uploaded',
                    last_server_seq = MAX(last_server_seq, ?3),
                    local_pending_event_id = NULL,
                    updated_at_ms = ?4
                WHERE sync_id = ?1
                    AND content_hash = ?2
                    AND provenance = 'local_pending_upload'
                "#,
                params![sync_id, to_local_hash(&upload.content_hash), upload.server_seq, now],
            )? as i64;
        }
        transaction.commit()?;
        Ok(crate::domain::ItemManagementResult {
            affected_count: affected,
        })
    }
}

struct PendingRow {
    content_hash: String,
    client_event_id: Option<String>,
    item_id: String,
    item_type: String,
    primary_text: Option<String>,
    summary: String,
    copy_count: i64,
}

fn build_pending_event(
    transaction: &rusqlite::Transaction<'_>,
    row: PendingRow,
) -> Result<Option<SyncPendingEvent>> {
    let client_event_id = row
        .client_event_id
        .filter(|value| !value.trim().is_empty())
        .unwrap_or_else(|| format!("local-{}", row.content_hash));

    let payload = match row.item_type.as_str() {
        "text" => {
            let text = row.primary_text.unwrap_or_else(|| row.summary.clone());
            json!({ "text": text, "summary": row.summary })
        }
        "color" => {
            let hex = row.primary_text.unwrap_or_else(|| row.summary.clone());
            json!({ "hex": hex })
        }
        "rich_text" => {
            let plain = row.primary_text.unwrap_or_default();
            json!({ "plain_text": plain, "summary": row.summary })
        }
        "link" => {
            let link = transaction
                .query_row(
                    r#"
                    SELECT canonical_url, original_text, display_url, host, title, site_name
                    FROM link_metadata
                    WHERE item_id = ?1
                    "#,
                    params![row.item_id],
                    |link_row| {
                        Ok(LinkRow {
                            canonical_url: link_row.get(0)?,
                            original_text: link_row.get(1)?,
                            display_url: link_row.get(2)?,
                            host: link_row.get(3)?,
                            title: link_row.get::<_, Option<String>>(4)?,
                            site_name: link_row.get::<_, Option<String>>(5)?,
                        })
                    },
                )
                .optional()?;

            let Some(link) = link else {
                // Missing link metadata: fall back to a text event so content
                // still propagates rather than dropping the item.
                let text = row.primary_text.unwrap_or_else(|| row.summary.clone());
                return Ok(Some(SyncPendingEvent {
                    content_hash: to_wire_hash(&row.content_hash),
                    item_type: "text".to_string(),
                    payload: json!({ "text": text, "summary": row.summary }),
                    copy_count_delta: row.copy_count.max(1),
                    client_event_id,
                }));
            };

            json!({
                "url": link.canonical_url,
                "text": link.original_text,
                "display_url": link.display_url,
                "host": link.host,
                "title": link.title,
                "site_name": link.site_name,
            })
        }
        // image / file / unknown require asset transfer; skip for now.
        _ => return Ok(None),
    };

    Ok(Some(SyncPendingEvent {
        content_hash: to_wire_hash(&row.content_hash),
        item_type: row.item_type,
        payload,
        copy_count_delta: row.copy_count.max(1),
        client_event_id,
    }))
}

struct LinkRow {
    canonical_url: String,
    original_text: String,
    display_url: String,
    host: String,
    title: Option<String>,
    site_name: Option<String>,
}
