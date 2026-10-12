package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineDao {

    // Offline Resources
    @Query("SELECT * FROM offline_resources ORDER BY downloadedAt DESC")
    fun getAllOfflineResources(): Flow<List<OfflineResourceEntity>>

    @Query("SELECT * FROM offline_resources WHERE id = :id LIMIT 1")
    suspend fun getOfflineResourceById(id: String): OfflineResourceEntity?

    @Query("SELECT COUNT(*) FROM offline_resources")
    suspend fun getResourceCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResource(resource: OfflineResourceEntity)

    @Query("UPDATE offline_resources SET downloadStatus = :status, lastSyncAt = :timestamp WHERE id = :id")
    suspend fun updateDownloadStatus(id: String, status: String, timestamp: Long)

    @Query("DELETE FROM offline_resources WHERE id = :id")
    suspend fun deleteResource(id: String)

    @Query("DELETE FROM offline_resources")
    suspend fun clearAllResources()

    // Sync Queue
    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    fun getPendingOperations(): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingOperationsSync(): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueOperation(op: SyncQueueEntity)

    @Query("UPDATE sync_queue SET status = :status, lastAttemptAt = :timestamp, retryCount = retryCount + 1, errorMessage = :error WHERE id = :id")
    suspend fun updateOperationStatus(id: String, status: String, timestamp: Long, error: String = "")

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteOperation(id: String)

    // Flashcards
    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY front ASC")
    fun getFlashcardsForDeck(deckId: String): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY front ASC")
    suspend fun getFlashcardsForDeckSync(deckId: String): List<FlashcardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(cards: List<FlashcardEntity>)

    @Query("UPDATE flashcards SET isMastered = :isMastered, reviewCount = reviewCount + 1, lastReviewedAt = :timestamp WHERE id = :id")
    suspend fun updateCardMastery(id: String, isMastered: Boolean, timestamp: Long)

    @Query("DELETE FROM flashcards WHERE deckId = :deckId")
    suspend fun deleteFlashcardsForDeck(deckId: String)
}
