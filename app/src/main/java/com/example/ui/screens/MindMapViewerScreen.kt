package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.MindMapGeneratorService
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindMapViewerScreen(
    mindMap: MindMap,
    onNavigateBack: () -> Unit,
    onAskTutorAboutConcept: (String, String) -> Unit,
    onSaveBranchFlashcards: (String, List<Flashcard>) -> Unit,
    onRecordQuizAttempt: (QuizAttempt) -> Unit,
    generatorService: MindMapGeneratorService
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Transform State (Pan & Zoom)
    var scale by remember { mutableFloatStateOf(1.0f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Selected & Expanded Nodes
    var selectedNode by remember { mutableStateOf<MindMapNode?>(mindMap.rootNode) }
    val expandedNodeIds = remember { mutableStateMapOf<String, Boolean>() }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchBox by remember { mutableStateOf(false) }

    // Dialog & Sheet States
    var showDetailsSheet by remember { mutableStateOf(false) }
    var showBranchFlashcardsDialog by remember { mutableStateOf<Pair<MindMapNode, List<Flashcard>>?>(null) }
    var showBranchQuizDialog by remember { mutableStateOf<Pair<MindMapNode, Quiz>?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // Initialize expanded states (root and main branches expanded by default)
    LaunchedEffect(mindMap) {
        mindMap.rootNode?.let { root ->
            expandedNodeIds[root.id] = true
            mindMap.getChildrenOf(root.id).forEach { branch ->
                expandedNodeIds[branch.id] = true
            }
        }
    }

    val ancestorPath = remember(selectedNode, mindMap) {
        selectedNode?.let { mindMap.getAncestorPath(it.id) } ?: emptyList()
    }

    // Graph Layout Computation (Hierarchical Tree Coordinates)
    val layoutCoordinates = remember(mindMap, expandedNodeIds.toMap()) {
        computeHierarchicalLayout(mindMap, expandedNodeIds)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            mindMap.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${mindMap.subject} • ${mindMap.nodes.size} Concepts",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSearchBox = !showSearchBox }) {
                        Icon(Icons.Default.Search, contentDescription = "Search nodes")
                    }
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Export and share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            // Interactive Graph Canvas Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.4f, 2.8f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    }
            ) {
                // Background Grid Lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gridSpacing = 40.dp.toPx() * scale
                    val startX = (offsetX % gridSpacing)
                    val startY = (offsetY % gridSpacing)
                    var curX = startX
                    while (curX < size.width) {
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.2f),
                            start = Offset(curX, 0f),
                            end = Offset(curX, size.height),
                            strokeWidth = 1f
                        )
                        curX += gridSpacing
                    }
                    var curY = startY
                    while (curY < size.height) {
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.2f),
                            start = Offset(0f, curY),
                            end = Offset(size.width, curY),
                            strokeWidth = 1f
                        )
                        curY += gridSpacing
                    }
                }

                // Render Hierarchical Connecting Edges
                Canvas(modifier = Modifier.fillMaxSize()) {
                    layoutCoordinates.forEach { (nodeId, coord) ->
                        val node = mindMap.nodes.find { it.id == nodeId } ?: return@forEach
                        val pId = node.parentNodeId
                        if (pId != null && layoutCoordinates.containsKey(pId)) {
                            val parentCoord = layoutCoordinates[pId]!!
                            val isHighlighted = ancestorPath.contains(nodeId) && ancestorPath.contains(pId)

                            val startX = parentCoord.x * scale + offsetX
                            val startY = parentCoord.y * scale + offsetY
                            val endX = coord.x * scale + offsetX
                            val endY = coord.y * scale + offsetY

                            val path = Path().apply {
                                moveTo(startX, startY)
                                val midY = (startY + endY) / 2f
                                cubicTo(startX, midY, endX, midY, endX, endY)
                            }

                            drawPath(
                                path = path,
                                color = if (isHighlighted) Color(0xFF6750A4) else Color(0xFFB0B0C0),
                                style = Stroke(
                                    width = if (isHighlighted) 4.5f * scale else 2.2f * scale,
                                    cap = StrokeCap.Round
                                )
                            )
                        }
                    }

                    // Render Cross-Branch Relationships (Curved Dash Lines)
                    mindMap.relations.forEach { relation ->
                        val srcCoord = layoutCoordinates[relation.sourceNodeId]
                        val tgtCoord = layoutCoordinates[relation.targetNodeId]
                        if (srcCoord != null && tgtCoord != null) {
                            val startX = srcCoord.x * scale + offsetX
                            val startY = srcCoord.y * scale + offsetY
                            val endX = tgtCoord.x * scale + offsetX
                            val endY = tgtCoord.y * scale + offsetY

                            val relPath = Path().apply {
                                moveTo(startX, startY)
                                val controlX = (startX + endX) / 2f - 60f * scale
                                val controlY = (startY + endY) / 2f
                                quadraticTo(controlX, controlY, endX, endY)
                            }

                            drawPath(
                                path = relPath,
                                color = Color(0xFFE8702A),
                                style = Stroke(
                                    width = 2.0f * scale,
                                    cap = StrokeCap.Round
                                )
                            )
                        }
                    }
                }

                // Render Interactive Node Elements
                layoutCoordinates.forEach { (nodeId, coord) ->
                    val node = mindMap.nodes.find { it.id == nodeId } ?: return@forEach
                    val isSelected = selectedNode?.id == nodeId
                    val isInAncestorPath = ancestorPath.contains(nodeId)
                    val isSearchMatch = searchQuery.isNotBlank() &&
                            (node.title.contains(searchQuery, ignoreCase = true) || node.summary.contains(searchQuery, ignoreCase = true))

                    val nodeX = (coord.x * scale + offsetX).roundToInt()
                    val nodeY = (coord.y * scale + offsetY).roundToInt()
                    val children = mindMap.getChildrenOf(node.id)
                    val isExpanded = expandedNodeIds[node.id] != false

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(nodeX - 100, nodeY - 35) }
                            .width(200.dp)
                            .testTag("node_${node.id}")
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedNode = node
                                    showDetailsSheet = true
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.primaryContainer
                                isSearchMatch -> MaterialTheme.colorScheme.tertiaryContainer
                                isInAncestorPath -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surface
                            },
                            shadowElevation = if (isSelected) 8.dp else 3.dp,
                            border = when {
                                isSelected -> androidx.compose.foundation.BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary)
                                isInAncestorPath -> androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
                                isSearchMatch -> androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary)
                                else -> androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        node.nodeType.badge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(node.nodeType.colorHex),
                                        fontWeight = FontWeight.Bold
                                    )

                                    if (children.isNotEmpty()) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clickable {
                                                    expandedNodeIds[node.id] = !isExpanded
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    if (isExpanded) Icons.Default.Remove else Icons.Default.Add,
                                                    contentDescription = "Expand toggle",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    node.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (node.summary.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        node.summary,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Top Floating Search Bar (if activated)
            AnimatedVisibility(
                visible = showSearchBox,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter concepts in this mind map...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = {
                                searchQuery = ""
                                showSearchBox = false
                            }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Floating Navigation & Zoom Control Panel
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    IconButton(onClick = { scale = (scale * 1.25f).coerceAtMost(2.8f) }) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In")
                    }
                    IconButton(onClick = { scale = (scale / 1.25f).coerceAtLeast(0.4f) }) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
                    }
                    IconButton(
                        onClick = {
                            // Fit to Screen & Center Root
                            scale = 1.0f
                            offsetX = 400f
                            offsetY = 160f
                        }
                    ) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Center Root")
                    }
                    IconButton(
                        onClick = {
                            val allExpanded = expandedNodeIds.values.all { it }
                            mindMap.nodes.forEach { n ->
                                expandedNodeIds[n.id] = !allExpanded
                            }
                        }
                    ) {
                        Icon(Icons.Default.UnfoldMore, contentDescription = "Expand/Collapse All")
                    }
                }
            }

            // Quick Floating Bottom Bar for Currently Selected Node
            selectedNode?.let { node ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .fillMaxWidth(0.68f)
                        .clickable { showDetailsSheet = true },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(node.nodeType.colorHex)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(node.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("Tap for deep dive & quiz", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    // Node Details Modal Bottom Sheet
    if (showDetailsSheet && selectedNode != null) {
        val node = selectedNode!!
        val children = mindMap.getChildrenOf(node.id)

        ModalBottomSheet(
            onDismissRequest = { showDetailsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header & Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(node.nodeType.colorHex).copy(alpha = 0.15f)
                    ) {
                        Text(
                            "${node.nodeType.badge} • ${node.nodeType.label}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(node.nodeType.colorHex)
                        )
                    }

                    if (node.sourceSection.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                "📖 ${node.sourceSection}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    node.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Concise Definition
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Core Definition:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(node.summary, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Comprehensive Detailed Explanation
                if (node.detailedExplanation.isNotBlank()) {
                    Text("Deep Dive Explanation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(node.detailedExplanation, style = MaterialTheme.typography.bodyMedium)
                }

                // Key Takeaways
                if (node.keyTakeaways.isNotEmpty()) {
                    Text("High-Yield Takeaways", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    node.keyTakeaways.forEach { takeaway ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("⚡", style = MaterialTheme.typography.bodyMedium)
                            Text(takeaway, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Real-World Examples
                if (node.examples.isNotEmpty()) {
                    Text("Applications & Examples", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    node.examples.forEach { example ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("🔬", style = MaterialTheme.typography.bodyMedium)
                            Text(example, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Related Concepts / Cross-Branch Links
                val relationsForThisNode = mindMap.relations.filter { it.sourceNodeId == node.id || it.targetNodeId == node.id }
                if (relationsForThisNode.isNotEmpty()) {
                    Text("Related Concepts & Links", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(relationsForThisNode) { rel ->
                            val otherId = if (rel.sourceNodeId == node.id) rel.targetNodeId else rel.sourceNodeId
                            val otherNode = mindMap.nodes.find { it.id == otherId }
                            otherNode?.let { linkedNode ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable {
                                        selectedNode = linkedNode
                                    }
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                        Text(rel.relationType.badge, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        Text(linkedNode.title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Primary Learning Action Buttons
                Text("Branch Learning Tools", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val cardsResult = generatorService.generateBranchFlashcards(
                                    branchRootNode = node,
                                    childNodes = children,
                                    targetLanguage = mindMap.responseLanguage
                                )
                                if (cardsResult.isSuccess) {
                                    val cards = cardsResult.getOrThrow()
                                    onSaveBranchFlashcards("deck_${node.id}", cards)
                                    showBranchFlashcardsDialog = Pair(node, cards)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("btn_branch_flashcards"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Flashcards")
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val quizResult = generatorService.generateBranchQuiz(
                                    branchRootNode = node,
                                    childNodes = children,
                                    targetLanguage = mindMap.responseLanguage
                                )
                                if (quizResult.isSuccess) {
                                    val quiz = quizResult.getOrThrow()
                                    showBranchQuizDialog = Pair(node, quiz)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("btn_branch_quiz"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Practice Quiz")
                    }
                }

                OutlinedButton(
                    onClick = {
                        showDetailsSheet = false
                        onAskTutorAboutConcept(
                            "Can you explain the concept of '${node.title}' in depth with examples?",
                            mindMap.subject
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ask AI Tutor About This Concept")
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Branch Flashcards Viewer Dialog
    showBranchFlashcardsDialog?.let { (branch, cards) ->
        var currentCardIndex by remember { mutableIntStateOf(0) }
        var isFlipped by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showBranchFlashcardsDialog = null },
            title = {
                Text(
                    "🗂️ Flashcards: ${branch.title}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                if (cards.isEmpty()) {
                    Text("No flashcards available for this branch.")
                } else {
                    val currentCard = cards[currentCardIndex]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Card ${currentCardIndex + 1} of ${cards.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Interactive Flip Card
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clickable { isFlipped = !isFlipped },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isFlipped) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shadowElevation = 4.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        if (isFlipped) "ANSWER:" else "QUESTION:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFlipped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        if (isFlipped) currentCard.back else currentCard.front,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "(Tap to flip)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        // Navigation Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = {
                                    if (currentCardIndex > 0) {
                                        currentCardIndex--
                                        isFlipped = false
                                    }
                                },
                                enabled = currentCardIndex > 0
                            ) {
                                Text("Previous")
                            }

                            TextButton(
                                onClick = {
                                    if (currentCardIndex < cards.size - 1) {
                                        currentCardIndex++
                                        isFlipped = false
                                    }
                                },
                                enabled = currentCardIndex < cards.size - 1
                            ) {
                                Text("Next")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBranchFlashcardsDialog = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Branch Practice Quiz Dialog
    showBranchQuizDialog?.let { (branch, quiz) ->
        var currentQIndex by remember { mutableIntStateOf(0) }
        var selectedAnswerIndex by remember { mutableStateOf<Int?>(null) }
        var isAnswerSubmitted by remember { mutableStateOf(false) }
        var score by remember { mutableIntStateOf(0) }
        var isQuizCompleted by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showBranchQuizDialog = null },
            title = {
                Text(
                    "📝 Quiz: ${branch.title}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                if (isQuizCompleted) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("🎉 Quiz Complete!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Your Score: $score / ${quiz.questions.size}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                        Text("+${score * 15} XP Earned!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                } else if (quiz.questions.isNotEmpty()) {
                    val q = quiz.questions[currentQIndex]
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Question ${currentQIndex + 1} of ${quiz.questions.size}", style = MaterialTheme.typography.labelSmall)
                        Text(q.question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

                        q.options.forEachIndexed { optIndex, optionText ->
                            val isSelected = selectedAnswerIndex == optIndex
                            val isCorrect = optIndex == q.correctIndex

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isAnswerSubmitted) {
                                        selectedAnswerIndex = optIndex
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = when {
                                    isAnswerSubmitted && isCorrect -> Color(0xFFC8E6C9)
                                    isAnswerSubmitted && isSelected && !isCorrect -> Color(0xFFFFCDD2)
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Text(
                                    optionText,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        if (isAnswerSubmitted) {
                            Text(
                                q.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (isQuizCompleted) {
                    TextButton(onClick = { showBranchQuizDialog = null }) {
                        Text("Finish")
                    }
                } else {
                    Button(
                        onClick = {
                            if (!isAnswerSubmitted) {
                                if (selectedAnswerIndex != null) {
                                    isAnswerSubmitted = true
                                    if (selectedAnswerIndex == quiz.questions[currentQIndex].correctIndex) {
                                        score++
                                    }
                                }
                            } else {
                                if (currentQIndex < quiz.questions.size - 1) {
                                    currentQIndex++
                                    selectedAnswerIndex = null
                                    isAnswerSubmitted = false
                                } else {
                                    isQuizCompleted = true
                                    onRecordQuizAttempt(
                                        QuizAttempt(
                                            quizId = quiz.id,
                                            quizTitle = quiz.title,
                                            score = score,
                                            totalQuestions = quiz.questions.size,
                                            xpEarned = score * 15
                                        )
                                    )
                                }
                            }
                        },
                        enabled = selectedAnswerIndex != null
                    ) {
                        Text(if (!isAnswerSubmitted) "Check Answer" else if (currentQIndex < quiz.questions.size - 1) "Next Question" else "See Results")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showBranchQuizDialog = null }) {
                    Text("Exit")
                }
            }
        )
    }

    // Export & Share Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export & Share Revision Notes") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Export full knowledge graph notes with hierarchy, formulas, and relationship definitions.")
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Title: ${mindMap.title}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Text("Concepts: ${mindMap.nodes.size}", style = MaterialTheme.typography.labelSmall)
                            Text("Format: Structured Markdown & Text", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val markdownNotes = generatorService.generateRevisionNotesMarkdown(mindMap)
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TITLE, mindMap.title)
                            putExtra(Intent.EXTRA_TEXT, markdownNotes)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share Mind Map Revision Notes")
                        context.startActivity(shareIntent)
                        showExportDialog = false
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Notes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Tree Coordinates Computation
private fun computeHierarchicalLayout(
    mindMap: MindMap,
    expandedMap: Map<String, Boolean>
): Map<String, Offset> {
    val result = mutableMapOf<String, Offset>()
    val root = mindMap.rootNode ?: return result

    val rootPos = Offset(500f, 100f)
    result[root.id] = rootPos

    val mainBranches = mindMap.getChildrenOf(root.id)
    val branchCount = mainBranches.size
    val totalWidth = maxOf(800f, branchCount * 260f)
    val startX = rootPos.x - (totalWidth / 2f) + 130f

    mainBranches.forEachIndexed { bIndex, branch ->
        val branchX = startX + (bIndex * (totalWidth / maxOf(1, branchCount)))
        val branchY = rootPos.y + 180f
        result[branch.id] = Offset(branchX, branchY)

        val isBranchExpanded = expandedMap[branch.id] != false
        if (isBranchExpanded) {
            val subNodes = mindMap.getChildrenOf(branch.id)
            val subCount = subNodes.size
            val subWidth = maxOf(200f, subCount * 120f)
            val subStartX = branchX - (subWidth / 2f) + 60f

            subNodes.forEachIndexed { sIndex, subNode ->
                val subX = subStartX + (sIndex * 140f)
                val subY = branchY + 160f
                result[subNode.id] = Offset(subX, subY)
            }
        }
    }

    return result
}
