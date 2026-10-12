package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.*

@Entity(
    tableName = "mind_maps",
    indices = [Index(value = ["title"]), Index(value = ["updatedAt"])]
)
data class MindMapEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subject: String,
    val sourceDocumentTitle: String,
    val sourceDocumentId: String?,
    val rootNodeId: String,
    val responseLanguageCode: String,
    val detailLevel: String,
    val createdAt: Long,
    val updatedAt: Long,
    val status: String,
    val isAvailableOffline: Boolean
) {
    fun toDomain(
        nodes: List<MindMapNode> = emptyList(),
        relations: List<MindMapRelation> = emptyList()
    ): MindMap {
        val lang = SUPPORTED_LANGUAGES.find { it.code == responseLanguageCode } ?: SUPPORTED_LANGUAGES[0]
        val detail = try {
            MindMapDetailLevel.valueOf(detailLevel)
        } catch (_: Exception) {
            MindMapDetailLevel.BALANCED
        }
        val stat = try {
            MindMapStatus.valueOf(status)
        } catch (_: Exception) {
            MindMapStatus.COMPLETED
        }
        return MindMap(
            id = id,
            title = title,
            subject = subject,
            sourceDocumentTitle = sourceDocumentTitle,
            sourceDocumentId = sourceDocumentId,
            rootNodeId = rootNodeId,
            responseLanguage = lang,
            detailLevel = detail,
            createdAt = createdAt,
            updatedAt = updatedAt,
            status = stat,
            nodes = nodes,
            relations = relations,
            isAvailableOffline = isAvailableOffline
        )
    }

    companion object {
        fun fromDomain(map: MindMap): MindMapEntity {
            return MindMapEntity(
                id = map.id,
                title = map.title,
                subject = map.subject,
                sourceDocumentTitle = map.sourceDocumentTitle,
                sourceDocumentId = map.sourceDocumentId,
                rootNodeId = map.rootNodeId,
                responseLanguageCode = map.responseLanguage.code,
                detailLevel = map.detailLevel.name,
                createdAt = map.createdAt,
                updatedAt = map.updatedAt,
                status = map.status.name,
                isAvailableOffline = map.isAvailableOffline
            )
        }
    }
}

@Entity(
    tableName = "mind_map_nodes",
    indices = [
        Index(value = ["mindMapId"]),
        Index(value = ["parentNodeId"]),
        Index(value = ["sortOrder"])
    ]
)
data class MindMapNodeEntity(
    @PrimaryKey val id: String,
    val mindMapId: String,
    val parentNodeId: String?,
    val title: String,
    val summary: String,
    val detailedExplanation: String,
    val nodeType: String,
    val sourcePage: Int,
    val sourceSection: String,
    val sortOrder: Int,
    val isExpanded: Boolean,
    val prerequisitesJson: String,
    val examplesJson: String,
    val keyTakeawaysJson: String
) {
    fun toDomain(): MindMapNode {
        val type = try {
            MindMapNodeType.valueOf(nodeType)
        } catch (_: Exception) {
            MindMapNodeType.CONCEPT
        }
        return MindMapNode(
            id = id,
            mindMapId = mindMapId,
            parentNodeId = parentNodeId,
            title = title,
            summary = summary,
            detailedExplanation = detailedExplanation,
            nodeType = type,
            sourcePage = sourcePage,
            sourceSection = sourceSection,
            sortOrder = sortOrder,
            isExpanded = isExpanded,
            prerequisites = parseStringList(prerequisitesJson),
            examples = parseStringList(examplesJson),
            keyTakeaways = parseStringList(keyTakeawaysJson)
        )
    }

    companion object {
        fun fromDomain(node: MindMapNode): MindMapNodeEntity {
            return MindMapNodeEntity(
                id = node.id,
                mindMapId = node.mindMapId,
                parentNodeId = node.parentNodeId,
                title = node.title,
                summary = node.summary,
                detailedExplanation = node.detailedExplanation,
                nodeType = node.nodeType.name,
                sourcePage = node.sourcePage,
                sourceSection = node.sourceSection,
                sortOrder = node.sortOrder,
                isExpanded = node.isExpanded,
                prerequisitesJson = encodeStringList(node.prerequisites),
                examplesJson = encodeStringList(node.examples),
                keyTakeawaysJson = encodeStringList(node.keyTakeaways)
            )
        }

        private fun encodeStringList(list: List<String>): String = list.joinToString("|||")
        private fun parseStringList(raw: String): List<String> =
            if (raw.isBlank()) emptyList() else raw.split("|||").filter { it.isNotBlank() }
    }
}

@Entity(
    tableName = "mind_map_relations",
    indices = [
        Index(value = ["mindMapId"]),
        Index(value = ["sourceNodeId"]),
        Index(value = ["targetNodeId"])
    ]
)
data class MindMapRelationEntity(
    @PrimaryKey val id: String,
    val mindMapId: String,
    val sourceNodeId: String,
    val targetNodeId: String,
    val relationType: String,
    val description: String
) {
    fun toDomain(): MindMapRelation {
        val relType = try {
            MindMapRelationType.valueOf(relationType)
        } catch (_: Exception) {
            MindMapRelationType.ASSOCIATION
        }
        return MindMapRelation(
            id = id,
            mindMapId = mindMapId,
            sourceNodeId = sourceNodeId,
            targetNodeId = targetNodeId,
            relationType = relType,
            description = description
        )
    }

    companion object {
        fun fromDomain(relation: MindMapRelation): MindMapRelationEntity {
            return MindMapRelationEntity(
                id = relation.id,
                mindMapId = relation.mindMapId,
                sourceNodeId = relation.sourceNodeId,
                targetNodeId = relation.targetNodeId,
                relationType = relation.relationType.name,
                description = relation.description
            )
        }
    }
}
