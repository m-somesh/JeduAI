package com.example.data.model

import java.util.UUID

enum class MindMapDetailLevel(val label: String, val nodeCountRange: String, val description: String) {
    CONCISE("Concise", "6–8 Concepts", "Core concepts, main definitions, and primary branches for fast review"),
    BALANCED("Balanced", "10–14 Concepts", "In-depth overview with subtopics, examples, and key relationships"),
    DETAILED("Detailed", "16–22 Concepts", "Comprehensive breakdown including formulas, nuances, and revision notes")
}

enum class MindMapStatus {
    IDLE,
    PROCESSING,
    COMPLETED,
    FAILED
}

enum class MindMapNodeType(val label: String, val badge: String, val colorHex: Long) {
    ROOT("Central Topic", "🎯 Root", 0xFF6750A4),
    MAIN_TOPIC("Main Branch", "🌿 Branch", 0xFF00668B),
    SUBTOPIC("Subtopic", "📌 Subtopic", 0xFF006D44),
    CONCEPT("Key Concept", "💡 Concept", 0xFF8F4C00),
    EXAMPLE("Application / Example", "🔬 Example", 0xFF7D5260),
    FORMULA("Formula / Rule", "📐 Formula", 0xFF5B53A4),
    REVISION("Revision Takeaway", "⚡ Revision", 0xFF3B6939)
}

enum class MindMapRelationType(val label: String, val badge: String) {
    PREREQUISITE("Prerequisite for", "⚠️ Prerequisite"),
    CAUSE_EFFECT("Leads to / Causes", "➡️ Cause & Effect"),
    SIMILARITY_DIFFERENCE("Contrasts with / Similar to", "⚖️ Contrast"),
    PART_OF("Part of / Subsystem of", "🧩 Component"),
    ASSOCIATION("Associated with", "🔗 Related"),
    EXAMPLE_OF("Concrete example of", "🔍 Example"),
    APPLICATION("Practical application of", "🚀 Application")
}

data class MindMapNode(
    val id: String = UUID.randomUUID().toString(),
    val mindMapId: String,
    val parentNodeId: String? = null,
    val title: String,
    val summary: String,
    val detailedExplanation: String = "",
    val nodeType: MindMapNodeType = MindMapNodeType.CONCEPT,
    val sourcePage: Int = 1,
    val sourceSection: String = "",
    val sortOrder: Int = 0,
    val isExpanded: Boolean = true,
    val prerequisites: List<String> = emptyList(),
    val examples: List<String> = emptyList(),
    val keyTakeaways: List<String> = emptyList()
)

data class MindMapRelation(
    val id: String = UUID.randomUUID().toString(),
    val mindMapId: String,
    val sourceNodeId: String,
    val targetNodeId: String,
    val relationType: MindMapRelationType = MindMapRelationType.ASSOCIATION,
    val description: String = ""
)

data class MindMap(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subject: String,
    val sourceDocumentTitle: String,
    val sourceDocumentId: String? = null,
    val rootNodeId: String,
    val responseLanguage: LanguageOption = SUPPORTED_LANGUAGES[0],
    val detailLevel: MindMapDetailLevel = MindMapDetailLevel.BALANCED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: MindMapStatus = MindMapStatus.COMPLETED,
    val nodes: List<MindMapNode> = emptyList(),
    val relations: List<MindMapRelation> = emptyList(),
    val isAvailableOffline: Boolean = true
) {
    val rootNode: MindMapNode? get() = nodes.find { it.id == rootNodeId } ?: nodes.find { it.parentNodeId == null } ?: nodes.firstOrNull()

    fun getChildrenOf(nodeId: String): List<MindMapNode> =
        nodes.filter { it.parentNodeId == nodeId }.sortedBy { it.sortOrder }

    fun getAncestorPath(targetNodeId: String): List<String> {
        val path = mutableListOf<String>()
        var curr = nodes.find { it.id == targetNodeId }
        while (curr != null) {
            path.add(curr.id)
            curr = curr.parentNodeId?.let { pId -> nodes.find { it.id == pId } }
        }
        return path
    }

    fun getAllDescendantIds(nodeId: String): Set<String> {
        val result = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(nodeId)
        while (queue.isNotEmpty()) {
            val curr = queue.removeFirst()
            val children = getChildrenOf(curr)
            children.forEach {
                result.add(it.id)
                queue.add(it.id)
            }
        }
        return result
    }
}
