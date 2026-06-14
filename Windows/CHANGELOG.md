# ClipDock Windows Panel — Changelog

## 0.2.0

The Windows panel graduates from a local-only UI shell into a persistent,
syncing clipboard client built on the shared `clipboard_core` engine.

### Added
- **Local persistence** — captured clipboard items (text and images) are
  stored in SQLite (schema v15, identical to macOS) and survive restarts.
- **Bidirectional sync** — create or join a sync space from Preferences; text
  captures push to the server and remote changes apply locally. Outbound and
  inbound both verified end to end against the real sync server.
- **Realtime delivery** — a `/v2/ws` WebSocket connection applies remote events
  within seconds, backed by a 15s HTTP poll loop as a safety net.
- **Image thumbnail sync** — captured images generate an adaptive WebP
  thumbnail that uploads to the server and propagates as an image event.
- **P2P discovery** — the device registers its endpoint with the server and can
  list peers in the sync space (direct iroh blob transfer is a later step).
- **System tray menu** — show/hide panel, sync now, preferences, copy
  diagnostics, and quit.

### Changed
- Backend now depends directly on `clipboard_core` rather than a bespoke
  storage schema, guaranteeing cross-platform parity.
- Preferences (including sync credentials) persist in the core database.

### Known limitations
- Full-resolution image payloads still require direct P2P transfer
  (multi-machine testing pending); only thumbnails sync via the server.
- Inbound thumbnail display wiring and copy-count delta de-duplication on
  re-copy are follow-ups.
