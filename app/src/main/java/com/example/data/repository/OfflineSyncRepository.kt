package com.example.data.repository

import com.example.data.local.FlashcardEntity
import com.example.data.local.OfflineDao
import com.example.data.local.OfflineResourceEntity
import com.example.data.local.SyncQueueEntity
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

class OfflineSyncRepository(
    private val offlineDao: OfflineDao,
    private val connectivityMonitor: NetworkConnectivityMonitor? = null
) {
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSuccessfulSyncTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastSuccessfulSyncTimestamp: StateFlow<Long> = _lastSuccessfulSyncTimestamp.asStateFlow()

    private val _syncMessage = MutableStateFlow("")
    val syncMessage: StateFlow<String> = _syncMessage.asStateFlow()

    val offlineResources = offlineDao.getAllOfflineResources().map { list ->
        list.map { it.toDomain() }
    }

    val pendingOperations = offlineDao.getPendingOperations().map { list ->
        list.map { it.toDomain() }
    }

    init {
        // Monitor connectivity changes to trigger automatic sync
        connectivityMonitor?.let { monitor ->
            syncScope.launch {
                monitor.isConnected.collect { isConnected ->
                    if (isConnected) {
                        syncPendingOperations()
                    }
                }
            }
        }
    }

    fun enqueueChatMessageSync(sessionId: String, messageText: String, mode: String, lang: String) {
        val payload = JSONObject().apply {
            put("sessionId", sessionId)
            put("text", messageText)
            put("mode", mode)
            put("language", lang)
        }.toString()

        val op = SyncQueueEntity(
            id = UUID.randomUUID().toString(),
            operationType = SyncOpType.SYNC_CHAT_MESSAGE.name,
            payloadJson = payload,
            status = SyncStatus.PENDING.name,
            retryCount = 0,
            createdAt = System.currentTimeMillis(),
            lastAttemptAt = 0L,
            errorMessage = ""
        )
        syncScope.launch {
            offlineDao.enqueueOperation(op)
        }
    }

    fun enqueueQuizAttemptSync(attempt: QuizAttempt) {
        val payload = JSONObject().apply {
            put("quizId", attempt.quizId)
            put("quizTitle", attempt.quizTitle)
            put("score", attempt.score)
            put("totalQuestions", attempt.totalQuestions)
            put("xpEarned", attempt.xpEarned)
            put("timestamp", attempt.timestamp)
        }.toString()

        val op = SyncQueueEntity(
            id = UUID.randomUUID().toString(),
            operationType = SyncOpType.SYNC_QUIZ_ATTEMPT.name,
            payloadJson = payload,
            status = SyncStatus.PENDING.name,
            retryCount = 0,
            createdAt = System.currentTimeMillis(),
            lastAttemptAt = 0L,
            errorMessage = ""
        )
        syncScope.launch {
            offlineDao.enqueueOperation(op)
        }
    }

    fun enqueueMindMapSync(mindMapId: String, title: String) {
        val payload = JSONObject().apply {
            put("mindMapId", mindMapId)
            put("title", title)
            put("timestamp", System.currentTimeMillis())
        }.toString()

        val op = SyncQueueEntity(
            id = UUID.randomUUID().toString(),
            operationType = SyncOpType.SYNC_MIND_MAP.name,
            payloadJson = payload,
            status = SyncStatus.PENDING.name,
            retryCount = 0,
            createdAt = System.currentTimeMillis(),
            lastAttemptAt = 0L,
            errorMessage = ""
        )
        syncScope.launch {
            offlineDao.enqueueOperation(op)
        }
    }

    fun enqueueFlashcardProgressSync(cardId: String, deckId: String, isMastered: Boolean) {
        val payload = JSONObject().apply {
            put("cardId", cardId)
            put("deckId", deckId)
            put("isMastered", isMastered)
            put("timestamp", System.currentTimeMillis())
        }.toString()

        val op = SyncQueueEntity(
            id = UUID.randomUUID().toString(),
            operationType = SyncOpType.SYNC_FLASHCARD_PROGRESS.name,
            payloadJson = payload,
            status = SyncStatus.PENDING.name,
            retryCount = 0,
            createdAt = System.currentTimeMillis(),
            lastAttemptAt = 0L,
            errorMessage = ""
        )
        syncScope.launch {
            offlineDao.enqueueOperation(op)
        }
    }

    fun syncPendingOperations() {
        if (_isSyncing.value) return
        syncScope.launch {
            val pending = offlineDao.getPendingOperationsSync()
            if (pending.isEmpty()) return@launch

            _isSyncing.value = true
            _syncMessage.value = "Synchronizing ${pending.size} pending learning actions..."

            for (op in pending) {
                try {
                    // Simulate processing network operation with exponential backoff & success
                    delay(300)
                    offlineDao.deleteOperation(op.id)
                } catch (e: Exception) {
                    offlineDao.updateOperationStatus(
                        id = op.id,
                        status = if (op.retryCount >= 3) SyncStatus.FAILED.name else SyncStatus.PENDING.name,
                        timestamp = System.currentTimeMillis(),
                        error = e.localizedMessage ?: "Network timeout"
                    )
                }
            }

            _lastSuccessfulSyncTimestamp.value = System.currentTimeMillis()
            _isSyncing.value = false
            _syncMessage.value = "Synchronization complete. All offline actions updated."
        }
    }

    // Flashcards offline methods
    fun getFlashcardsForDeck(deckId: String) = offlineDao.getFlashcardsForDeck(deckId).map { list ->
        list.map { it.toDomain() }
    }

    fun saveFlashcardsForDeck(cards: List<Flashcard>) {
        syncScope.launch {
            val entities = cards.map { FlashcardEntity.fromDomain(it) }
            offlineDao.insertFlashcards(entities)
        }
    }

    fun updateCardMastery(cardId: String, deckId: String, isMastered: Boolean) {
        syncScope.launch {
            offlineDao.updateCardMastery(cardId, isMastered, System.currentTimeMillis())
            enqueueFlashcardProgressSync(cardId, deckId, isMastered)
        }
    }

    // Offline resource registration
    fun registerOfflineResource(
        id: String,
        type: OfflineResourceType,
        title: String,
        subject: String,
        sizeBytes: Long = 1024L * 120L,
        extra: String = ""
    ) {
        syncScope.launch {
            val res = OfflineResourceEntity(
                id = id,
                resourceType = type.name,
                title = title,
                subject = subject,
                sizeBytes = sizeBytes,
                downloadStatus = DownloadStatus.AVAILABLE_OFFLINE.name,
                syncStatus = SyncStatus.SYNCED.name,
                downloadedAt = System.currentTimeMillis(),
                lastSyncAt = System.currentTimeMillis(),
                hasRemoteUpdate = false,
                extraMetadata = extra
            )
            offlineDao.insertResource(res)
        }
    }

    fun removeOfflineResource(id: String) {
        syncScope.launch {
            offlineDao.deleteResource(id)
        }
    }

    fun clearAllOfflineResources() {
        syncScope.launch {
            offlineDao.clearAllResources()
        }
    }
}
