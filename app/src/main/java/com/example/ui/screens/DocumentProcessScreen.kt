package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.data.repository.DocumentExtractorHelper
import com.example.ui.components.LanguageSelectorDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentProcessScreen(
    initialDocument: DocumentBook? = null,
    defaultLanguage: LanguageOption,
    onNavigateBack: () -> Unit,
    onMindMapGenerated: (MindMap) -> Unit,
    onGenerateMindMap: suspend (
        title: String,
        content: String,
        subject: String,
        detailLevel: MindMapDetailLevel,
        targetLanguage: LanguageOption,
        sourceDocId: String?
    ) -> Result<MindMap>
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var docTitle by remember { mutableStateOf(initialDocument?.title ?: "") }
    var docSubject by remember { mutableStateOf(initialDocument?.subject ?: "General Studies") }
    var rawContent by remember {
        mutableStateOf(
            initialDocument?.sections?.joinToString("\n\n") { "${it.title}\n${it.originalText}" } ?: ""
        )
    }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf(initialDocument?.title ?: "") }
    var detectedChapters by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedChapter by remember { mutableStateOf("Full Document") }
    var detailLevel by remember { mutableStateOf(MindMapDetailLevel.BALANCED) }
    var selectedLanguage by remember { mutableStateOf(defaultLanguage) }
    var showLanguagePicker by remember { mutableStateOf(false) }

    // Generation State & Stepper
    var isProcessing by remember { mutableStateOf(false) }
    var currentStepIndex by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val processingSteps = listOf(
        "Reading Document & Metadata",
        "Extracting Clean Academic Text",
        "Identifying Foundational & Core Concepts",
        "Discovering Cross-Branch Relationships",
        "Synthesizing Hierarchical Mind Map",
        "Validating & Persisting to Offline Storage"
    )

    // Storage Access Framework Document Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            coroutineScope.launch {
                val result = DocumentExtractorHelper.extractFromUri(context, uri)
                if (result.isSuccess) {
                    val extracted = result.getOrThrow()
                    selectedFileName = extracted.fileName
                    if (docTitle.isBlank()) docTitle = extracted.fileName.replace(".pdf", "").replace(".txt", "")
                    rawContent = extracted.fullText
                    detectedChapters = extracted.detectedChapters
                    if (detectedChapters.isNotEmpty()) selectedChapter = "Full Document"
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generate AI Mind Map", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isProcessing) {
                // Processing Stepper Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(56.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 5.dp
                        )

                        Text(
                            "Analyzing & Mapping Knowledge...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            docTitle.ifBlank { "Educational Document" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Stepper Items
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            processingSteps.forEachIndexed { index, stepName ->
                                val isDone = index < currentStepIndex
                                val isCurrent = index == currentStepIndex

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isDone -> MaterialTheme.colorScheme.primary
                                                    isCurrent -> MaterialTheme.colorScheme.primaryContainer
                                                    else -> MaterialTheme.colorScheme.outlineVariant
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isDone) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Text(
                                                "${index + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Text(
                                        stepName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                isProcessing = false
                                errorMessage = "Generation canceled by user."
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel Processing")
                        }
                    }
                }
            } else {
                // Source Selection Method
                Text(
                    "1. Document Source",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                filePickerLauncher.launch("*/*")
                            }
                            .testTag("btn_pick_pdf_file"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Pick File (PDF/Text)", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                            if (selectedFileName.isNotBlank()) {
                                Text(
                                    selectedFileName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    OutlinedCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (rawContent.isBlank()) {
                                    rawContent = """
                                        # Classical Mechanics & Conservation Laws
                                        Newton's three laws of motion govern macroscopic physical dynamics:
                                        1. An object remains at rest unless acted upon by a net external force.
                                        2. F = ma: Force equals mass multiplied by acceleration.
                                        3. For every action, there is an equal and opposite reaction.
                                        
                                        Conservation of Momentum dictates that in an isolated system, total linear momentum remains invariant over time.
                                    """.trimIndent()
                                    docTitle = "Classical Mechanics & Newton's Laws"
                                    docSubject = "Physics"
                                }
                            },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text("Load Sample Notes", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                            Text("Physics & STEM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Document Metadata Fields
                OutlinedTextField(
                    value = docTitle,
                    onValueChange = { docTitle = it },
                    label = { Text("Mind Map Title") },
                    placeholder = { Text("e.g. Thermodynamics & Heat Engines") },
                    modifier = Modifier.fillMaxWidth().testTag("input_mind_map_title"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = docSubject,
                    onValueChange = { docSubject = it },
                    label = { Text("Academic Subject") },
                    placeholder = { Text("e.g. Physics, Biology, Computer Science") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Chapter Selector (if detected)
                if (detectedChapters.isNotEmpty()) {
                    Text(
                        "Section or Chapter Focus",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    var expandedChapterDropdown by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedChapterDropdown = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedChapter, style = MaterialTheme.typography.bodyMedium)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(
                            expanded = expandedChapterDropdown,
                            onDismissRequest = { expandedChapterDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Full Document") },
                                onClick = {
                                    selectedChapter = "Full Document"
                                    expandedChapterDropdown = false
                                }
                            )
                            detectedChapters.forEach { chap ->
                                DropdownMenuItem(
                                    text = { Text(chap) },
                                    onClick = {
                                        selectedChapter = chap
                                        expandedChapterDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Text / Notes Content Box
                OutlinedTextField(
                    value = rawContent,
                    onValueChange = { rawContent = it },
                    label = { Text("Extracted Document Text / Notes") },
                    placeholder = { Text("Type, paste notes, or select a document above...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .testTag("input_document_content"),
                    shape = RoundedCornerShape(12.dp)
                )

                HorizontalDivider()

                // 2. Configuration Settings
                Text(
                    "2. Generation Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Detail Level Selector
                Text("Detail Level", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MindMapDetailLevel.values().forEach { level ->
                        val isSelected = detailLevel == level
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { detailLevel = level },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    level.label,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    level.nodeCountRange,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Language Selection Bar
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLanguagePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Response Language", style = MaterialTheme.typography.labelMedium)
                            Text(
                                "${selectedLanguage.flag} ${selectedLanguage.displayName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(onClick = { showLanguagePicker = true }) {
                            Text("Change")
                        }
                    }
                }

                // Error Message Display
                errorMessage?.let { err ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Text(err, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Action Button
                Button(
                    onClick = {
                        val title = docTitle.ifBlank { "Educational Topic" }
                        val contentToMap = rawContent.ifBlank {
                            "Overview of $title: Covers foundational theories, core principles, equations, and examination takeaways."
                        }

                        isProcessing = true
                        errorMessage = null
                        currentStepIndex = 0

                        coroutineScope.launch {
                            // Stage 1: Reading document
                            delay(300)
                            currentStepIndex = 1

                            // Stage 2: Extracting text
                            delay(350)
                            currentStepIndex = 2

                            // Stage 3: Identifying concepts
                            delay(400)
                            currentStepIndex = 3

                            // Stage 4: Discovering relationships
                            delay(400)
                            currentStepIndex = 4

                            // Stage 5: Synthesizing
                            val genResult = onGenerateMindMap(
                                title,
                                contentToMap,
                                docSubject,
                                detailLevel,
                                selectedLanguage,
                                initialDocument?.id
                            )

                            currentStepIndex = 5
                            delay(300)
                            isProcessing = false

                            if (genResult.isSuccess) {
                                onMindMapGenerated(genResult.getOrThrow())
                            } else {
                                errorMessage = genResult.exceptionOrNull()?.localizedMessage ?: "Failed to generate mind map. Please check inputs."
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_start_generate_mind_map"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.AccountTree, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate AI Mind Map", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showLanguagePicker) {
        LanguageSelectorDialog(
            title = "Mind Map Response Language",
            selectedLanguage = selectedLanguage,
            onLanguageSelected = { selectedLanguage = it },
            onDismissRequest = { showLanguagePicker = false }
        )
    }
}
