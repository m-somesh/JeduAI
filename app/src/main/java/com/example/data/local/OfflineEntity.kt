package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.*

@Entity(
    tableName = "offline_resources",
    indices = [Index(value = ["resourceType"]), Index(value = ["downloadStatus"])]
)
data class OfflineResourceEntity(
    @PrimaryKey val id: String,
    val resourceType: String,
    val title: String,
    val subject: String,
    val sizeBytes: Long,
    val downloadStatus: String,
    val syncStatus: String,
    val downloadedAt: Long,
    val lastSyncAt: Long,
    val hasRemoteUpdate: Boolean,
    val extraMetadata: String
) {
    fun toDomain(): OfflineResourceItem {
        val rType = try {
            OfflineResourceType.valueOf(resourceType)
        } catch (_: Exception) {
            OfflineResourceType.MIND_MAP
        }
        val dStatus = try {
            DownloadStatus.valueOf(downloadStatus)
        } catch (_: Exception) {
            DownloadStatus.AVAILABLE_OFFLINE
        }
        val sStatus = try {
            SyncStatus.valueOf(syncStatus)
        } catch (_: Exception) {
            SyncStatus.SYNCED
        }
        return OfflineResourceItem(
            id = id,
            resourceType = rType,
            title = title,
            subject = subject,
            sizeBytes = sizeBytes,
            downloadStatus = dStatus,
            syncStatus = sStatus,
            downloadedAt = downloadedAt,
            lastSyncAt = lastSyncAt,
            hasRemoteUpdate = hasRemoteUpdate,
            extraMetadata = extraMetadata
        )
    }

    companion object {
        fun fromDomain(item: OfflineResourceItem): OfflineResourceEntity {
            return OfflineResourceEntity(
                id = item.id,
                resourceType = item.resourceType.name,
                title = item.title,
                subject = item.subject,
                sizeBytes = item.sizeBytes,
                downloadStatus = item.downloadStatus.name,
                syncStatus = item.syncStatus.name,
                downloadedAt = item.downloadedAt,
                lastSyncAt = item.lastSyncAt,
                hasRemoteUpdate = item.hasRemoteUpdate,
                extraMetadata = item.extraMetadata
            )
        }
    }
}

@Entity(
    tableName = "sync_queue",
    indices = [Index(value = ["status"]), Index(value = ["createdAt"])]
)
data class SyncQueueEntity(
    @PrimaryKey val id: String,
    val operationType: String,
    val payloadJson: String,
    val status: String,
    val retryCount: Int,
    val createdAt: Long,
    val lastAttemptAt: Long,
    val errorMessage: String
) {
    fun toDomain(): PendingSyncOperation {
        val op = try {
            SyncOpType.valueOf(operationType)
        } catch (_: Exception) {
            SyncOpType.SYNC_CHAT_MESSAGE
        }
        val st = try {
            SyncStatus.valueOf(status)
        } catch (_: Exception) {
            SyncStatus.PENDING
        }
        return PendingSyncOperation(
            id = id,
            operationType = op,
            payloadJson = payloadJson,
            status = st,
            retryCount = retryCount,
            createdAt = createdAt,
            lastAttemptAt = lastAttemptAt,
            errorMessage = errorMessage
        )
    }

    companion object {
        fun fromDomain(op: PendingSyncOperation): SyncQueueEntity {
            return SyncQueueEntity(
                id = op.id,
                operationType = op.operationType.name,
                payloadJson = op.payloadJson,
                status = op.status.name,
                retryCount = op.retryCount,
                createdAt = op.createdAt,
                lastAttemptAt = op.lastAttemptAt,
                errorMessage = op.errorMessage
            )
        }
    }
}

@Entity(
    tableName = "flashcards",
    indices = [Index(value = ["deckId"]), Index(value = ["isMastered"])]
)
data class FlashcardEntity(
    @PrimaryKey val id: String,
    val deckId: String,
    val front: String,
    val back: String,
    val hint: String,
    val conceptTag: String,
    val isMastered: Boolean,
    val reviewCount: Int,
    val lastReviewedAt: Long
) {
    fun toDomain(): Flashcard {
        return Flashcard(
            id = id,
            deckId = deckId,
            front = front,
            back = back,
            hint = hint,
            conceptTag = conceptTag,
            isMastered = isMastered,
            reviewCount = reviewCount,
            lastReviewedAt = lastReviewedAt
        )
    }

    companion object {
        fun fromDomain(card: Flashcard): FlashcardEntity {
            return FlashcardEntity(
                id = card.id,
                deckId = card.deckId,
                front = card.front,
                back = card.back,
                hint = card.hint,
                conceptTag = card.conceptTag,
                isMastered = card.isMastered,
                reviewCount = card.reviewCount,
                lastReviewedAt = card.lastReviewedAt
            )
        }
    }
}
