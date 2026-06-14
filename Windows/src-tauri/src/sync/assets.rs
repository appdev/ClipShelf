//! Thumbnail generation for image sync. Full image payloads transfer over P2P;
//! the server only relays the lightweight WebP thumbnail used for previews.

use std::path::Path;

use clipdock_sync_contract::{
    AssetDigest, SYNC_THUMBNAIL_MIME_TYPE, THUMBNAIL_DETAIL_TARGET_BYTES,
    THUMBNAIL_NORMAL_TARGET_BYTES, THUMBNAIL_MAX_BYTES,
};
use clipdock_thumbnail_codec::encode_adaptive_thumbnail_webp_rgba;

/// A WebP thumbnail ready to upload: the bytes, their `blake3:` digest, and the
/// scaled dimensions.
pub struct GeneratedThumbnail {
    pub bytes: Vec<u8>,
    pub digest: String,
    pub mime_type: &'static str,
    pub width: i64,
    pub height: i64,
}

/// Decode the image at `payload_path` and produce an adaptive WebP thumbnail.
/// Returns `None` when the image cannot be decoded or is too small to encode.
pub fn generate_thumbnail(payload_path: &Path) -> Result<Option<GeneratedThumbnail>, String> {
    let decoded = image::ImageReader::open(payload_path)
        .map_err(|error| error.to_string())?
        .decode()
        .map_err(|error| error.to_string())?
        .to_rgba8();
    let width = decoded.width();
    let height = decoded.height();
    let rgba = decoded.into_raw();

    let thumbnail = encode_adaptive_thumbnail_webp_rgba(
        &rgba,
        width,
        height,
        THUMBNAIL_NORMAL_TARGET_BYTES,
        THUMBNAIL_DETAIL_TARGET_BYTES,
        THUMBNAIL_MAX_BYTES,
    )
    .map_err(|error| error.to_string())?;

    let Some(thumbnail) = thumbnail else {
        return Ok(None);
    };

    let digest = AssetDigest::from_bytes(&thumbnail.bytes);
    Ok(Some(GeneratedThumbnail {
        digest: digest.as_str().to_string(),
        mime_type: SYNC_THUMBNAIL_MIME_TYPE,
        width: thumbnail.width as i64,
        height: thumbnail.height as i64,
        bytes: thumbnail.bytes,
    }))
}
