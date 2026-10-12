package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MindMapDao {

    @Query("SELECT * FROM mind_maps ORDER BY updatedAt DESC")
    fun getAllMindMaps(): Flow<List<MindMapEntity>>

    @Query("SELECT * FROM mind_maps WHERE id = :id LIMIT 1")
    suspend fun getMindMapById(id: String): MindMapEntity?

    @Query("SELECT COUNT(*) FROM mind_maps")
    suspend fun getMindMapCount(): Int

    @Query("SELECT * FROM mind_map_nodes WHERE mindMapId = :mindMapId ORDER BY sortOrder ASC")
    fun getNodesForMap(mindMapId: String): Flow<List<MindMapNodeEntity>>

    @Query("SELECT * FROM mind_map_nodes WHERE mindMapId = :mindMapId ORDER BY sortOrder ASC")
    suspend fun getNodesForMapSync(mindMapId: String): List<MindMapNodeEntity>

    @Query("SELECT * FROM mind_map_relations WHERE mindMapId = :mindMapId")
    fun getRelationsForMap(mindMapId: String): Flow<List<MindMapRelationEntity>>

    @Query("SELECT * FROM mind_map_relations WHERE mindMapId = :mindMapId")
    suspend fun getRelationsForMapSync(mindMapId: String): List<MindMapRelationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMindMap(map: MindMapEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<MindMapNodeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelations(relations: List<MindMapRelationEntity>)

    @Query("UPDATE mind_maps SET title = :newTitle, updatedAt = :updatedAt WHERE id = :id")
    suspend fun renameMindMap(id: String, newTitle: String, updatedAt: Long)

    @Query("DELETE FROM mind_maps WHERE id = :mindMapId")
    suspend fun deleteMindMapEntity(mindMapId: String)

    @Query("DELETE FROM mind_map_nodes WHERE mindMapId = :mindMapId")
    suspend fun deleteNodesForMap(mindMapId: String)

    @Query("DELETE FROM mind_map_relations WHERE mindMapId = :mindMapId")
    suspend fun deleteRelationsForMap(mindMapId: String)

    @Transaction
    suspend fun insertCompleteMindMap(
        map: MindMapEntity,
        nodes: List<MindMapNodeEntity>,
        relations: List<MindMapRelationEntity>
    ) {
        deleteNodesForMap(map.id)
        deleteRelationsForMap(map.id)
        insertMindMap(map)
        if (nodes.isNotEmpty()) insertNodes(nodes)
        if (relations.isNotEmpty()) insertRelations(relations)
    }

    @Transaction
    suspend fun deleteCompleteMindMap(mindMapId: String) {
        deleteRelationsForMap(mindMapId)
        deleteNodesForMap(mindMapId)
        deleteMindMapEntity(mindMapId)
    }
}
