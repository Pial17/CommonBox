package com.example.data.sync

enum class SyncState(val label: String, val badgeColorHex: Long) {
    SYNCED("Synced", 0xFF059669),       // 🟢 Emerald green
    OFFLINE("Offline", 0xFFD97706),     // 🟠 Amber/Orange
    SYNCING("Syncing...", 0xFF2563EB),  // 🔵 Blue
    SYNC_ERROR("Sync failed", 0xFFDC2626) // 🔴 Red
}

data class SyncReport(
    val state: SyncState,
    val pendingCount: Int = 0,
    val lastSyncedTime: Long = System.currentTimeMillis(),
    val statusMessage: String = "Local cash records saved safely"
)
