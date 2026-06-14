import { invoke, isTauri } from "@tauri-apps/api/core";

export type SyncStatus = {
  enabled: boolean;
  joined: boolean;
  serverUrl: string;
  deviceName: string;
  syncId: string | null;
  deviceId: string | null;
  cursor: number;
  snapshotSeq: number;
};

export type SyncCreateResult = {
  syncId: string;
  pairingCode: string;
  pairingExpiresAtMs: number;
  deviceId: string;
};

export type SyncApplyReport = {
  appliedEvents: number;
  cursor: number;
  snapshotSeq: number;
};

/// Create a new sync space on the configured server. Returns the pairing code
/// other devices use to join.
export async function createSyncSpace(
  serverUrl: string,
  deviceName: string
): Promise<SyncCreateResult> {
  return invoke<SyncCreateResult>("sync_create_space", { serverUrl, deviceName });
}

/// Join an existing sync space with a 5-character pairing code; pulls the
/// initial snapshot on success.
export async function joinSyncSpace(
  serverUrl: string,
  pairingCode: string,
  deviceName: string
): Promise<SyncApplyReport> {
  return invoke<SyncApplyReport>("sync_join_space", { serverUrl, pairingCode, deviceName });
}

/// Push pending local captures, then pull and apply remote events.
export async function syncPullNow(): Promise<SyncApplyReport> {
  return invoke<SyncApplyReport>("sync_pull_now");
}

/// Report current sync configuration and progress.
export async function fetchSyncStatus(): Promise<SyncStatus | null> {
  if (!isTauri()) {
    return null;
  }
  return invoke<SyncStatus>("sync_status");
}

/// Disable sync without discarding credentials.
export async function disableSync(): Promise<void> {
  await invoke("sync_disable");
}
