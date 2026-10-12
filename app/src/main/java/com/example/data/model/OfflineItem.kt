package com.example.data.model

import java.util.UUID

enum class OfflineResourceType(val label: String, val iconEmoji: String) {
    MIND_MAP("AI Mind Map", "🗺️"),
    DOCUMENT("E-Book / Notes", "📚"),
    QUIZ("Practice Quiz", "📝"),
    FLASHCARD_DECK("Flashcard Deck", "🗂️"),
    CHAT_SESSION("Tutoring Chat", "💬"),
    REVISION_NOTE("Revision Notes", "📋")
}

enum class DownloadStatus(val label: String) {
    ONLINE_ONLY("Online Only"),
    DOWNLOADING("Downloading..."),
    AVAILABLE_OFFLINE("Available Offline"),
    FAILED("Download Failed")
}

enum class SyncStatus(val label: String) {
    SYNCED("Synced"),
    PENDING("Waiting to Sync"),
    SYNCING("Syncing..."),
    FAILED("Sync Failed")
}

enum class SyncOpType {
    SYNC_CHAT_MESSAGE,
    SYNC_QUIZ_ATTEMPT,
    SYNC_MIND_MAP,
    SYNC_FLASHCARD_PROGRESS,
    SYNC_NOTE
}

data class OfflineResourceItem(
    val id: String = UUID.randomUUID().toString(),
    val resourceType: OfflineResourceType,
    val title: String,
    val subject: String,
    val sizeBytes: Long = 1024L * 150L, // ~150 KB default
    val downloadStatus: DownloadStatus = DownloadStatus.AVAILABLE_OFFLINE,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val downloadedAt: Long = System.currentTimeMillis(),
    val lastSyncAt: Long = System.currentTimeMillis(),
    val hasRemoteUpdate: Boolean = false,
    val extraMetadata: String = ""
)

data class Flashcard(
    val id: String = UUID.randomUUID().toString(),
    val deckId: String,
    val front: String,
    val back: String,
    val hint: String = "",
    val conceptTag: String = "",
    val isMastered: Boolean = false,
    val reviewCount: Int = 0,
    val lastReviewedAt: Long = 0L
)

data class PendingSyncOperation(
    val id: String = UUID.randomUUID().toString(),
    val operationType: SyncOpType,
    val payloadJson: String,
    val status: SyncStatus = SyncStatus.PENDING,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAttemptAt: Long = 0L,
    val errorMessage: String = ""
)
