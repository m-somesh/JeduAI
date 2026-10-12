package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        MindMapEntity::class,
        MindMapNodeEntity::class,
        MindMapRelationEntity::class,
        OfflineResourceEntity::class,
        SyncQueueEntity::class,
        FlashcardEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class JeduDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao
    abstract fun mindMapDao(): MindMapDao
    abstract fun offlineDao(): OfflineDao

    companion object {
        @Volatile
        private var INSTANCE: JeduDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `mind_maps` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `sourceDocumentTitle` TEXT NOT NULL,
                        `sourceDocumentId` TEXT,
                        `rootNodeId` TEXT NOT NULL,
                        `responseLanguageCode` TEXT NOT NULL,
                        `detailLevel` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `isAvailableOffline` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mind_maps_title` ON `mind_maps` (`title`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mind_maps_updatedAt` ON `mind_maps` (`updatedAt`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `mind_map_nodes` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `mindMapId` TEXT NOT NULL,
                        `parentNodeId` TEXT,
                        `title` TEXT NOT NULL,
                        `summary` TEXT NOT NULL,
                        `detailedExplanation` TEXT NOT NULL,
                        `nodeType` TEXT NOT NULL,
                        `sourcePage` INTEGER NOT NULL,
                        `sourceSection` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `isExpanded` INTEGER NOT NULL,
                        `prerequisitesJson` TEXT NOT NULL,
                        `examplesJson` TEXT NOT NULL,
                        `keyTakeawaysJson` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mind_map_nodes_mindMapId` ON `mind_map_nodes` (`mindMapId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mind_map_nodes_parentNodeId` ON `mind_map_nodes` (`parentNodeId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mind_map_nodes_sortOrder` ON `mind_map_nodes` (`sortOrder`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `mind_map_relations` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `mindMapId` TEXT NOT NULL,
                        `sourceNodeId` TEXT NOT NULL,
                        `targetNodeId` TEXT NOT NULL,
                        `relationType` TEXT NOT NULL,
                        `description` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mind_map_relations_mindMapId` ON `mind_map_relations` (`mindMapId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mind_map_relations_sourceNodeId` ON `mind_map_relations` (`sourceNodeId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mind_map_relations_targetNodeId` ON `mind_map_relations` (`targetNodeId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `offline_resources` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `resourceType` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `sizeBytes` INTEGER NOT NULL,
                        `downloadStatus` TEXT NOT NULL,
                        `syncStatus` TEXT NOT NULL,
                        `downloadedAt` INTEGER NOT NULL,
                        `lastSyncAt` INTEGER NOT NULL,
                        `hasRemoteUpdate` INTEGER NOT NULL,
                        `extraMetadata` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_offline_resources_resourceType` ON `offline_resources` (`resourceType`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_offline_resources_downloadStatus` ON `offline_resources` (`downloadStatus`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sync_queue` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `operationType` TEXT NOT NULL,
                        `payloadJson` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `retryCount` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `lastAttemptAt` INTEGER NOT NULL,
                        `errorMessage` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_queue_status` ON `sync_queue` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_queue_createdAt` ON `sync_queue` (`createdAt`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `flashcards` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `deckId` TEXT NOT NULL,
                        `front` TEXT NOT NULL,
                        `back` TEXT NOT NULL,
                        `hint` TEXT NOT NULL,
                        `conceptTag` TEXT NOT NULL,
                        `isMastered` INTEGER NOT NULL,
                        `reviewCount` INTEGER NOT NULL,
                        `lastReviewedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcards_deckId` ON `flashcards` (`deckId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcards_isMastered` ON `flashcards` (`isMastered`)")
            }
        }

        fun getInstance(context: Context): JeduDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    JeduDatabase::class.java,
                    "jedu_learning.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
