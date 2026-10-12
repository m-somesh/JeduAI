package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.GeminiTutorService
import com.example.data.repository.TextToSpeechHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScreen(
    documents: List<DocumentBook>,
    onAddDocument: (String, String, String, LanguageOption) -> Unit,
    onMarkCompleted: (String, String, Boolean) -> Unit,
    onSaveNote: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val tutorService = remember { GeminiTutorService() }
    val ttsHelper = remember { TextToSpeechHelper(context) }

    DisposableEffect(Unit) {
        onDispose { ttsHelper.shutdown() }
    }

    var selectedDocId by remember { mutableStateOf(documents.firstOrNull()?.id ?: "") }
    val currentDoc = documents.find { it.id == selectedDocId } ?: documents.firstOrNull()

    var currentSectionIndex by remember { mutableIntStateOf(0) }
    val currentSection = currentDoc?.sections?.getOrNull(currentSectionIndex)

    var viewMode by remember { mutableStateOf("BILINGUAL") } // "ORIGINAL", "TRANSLATED", "BILINGUAL"
    var showUploadDialog by remember { mutableStateOf(false) }
    var simplifiedExplanation by remember { mutableStateOf("") }
    var isSimplifying by remember { mutableStateOf(false) }
    var userNoteText by remember(currentSection?.id) { mutableStateOf(currentSection?.userNote ?: "") }

    // Custom Document Upload Dialog
    if (showUploadDialog) {
        var uploadTitle by remember { mutableStateOf("") }
        var uploadSubject by remember { mutableStateOf("") }
        var uploadContent by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showUploadDialog = false },
            title = { Text("Import Academic Notes / Text") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = uploadTitle,
                        onValueChange = { uploadTitle = it },
                        label = { Text("Chapter Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uploadSubject,
                        onValueChange = { uploadSubject = it },
                        label = { Text("Subject (e.g. Chemistry, CS)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uploadContent,
                        onValueChange = { uploadContent = it },
                        label = { Text("Text Content (Separate sections with blank lines)") },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        maxLines = 8
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (uploadTitle.isNotBlank() && uploadContent.isNotBlank()) {
                            onAddDocument(uploadTitle, uploadSubject, uploadContent, SUPPORTED_LANGUAGES[0])
                            showUploadDialog = false
                        }
                    }
                ) {
                    Text("Import & Translate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUploadDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("E-Books & Textbooks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        FilledTonalButton(onClick = { showUploadDialog = true }) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Text")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Document Selector Pills
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(documents) { doc ->
                            val isSelected = doc.id == currentDoc?.id
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedDocId = doc.id
                                    currentSectionIndex = 0
                                    simplifiedExplanation = ""
                                },
                                label = { Text(doc.title.take(24) + "...") },
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (currentDoc == null || currentSection == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No documents loaded yet.")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Book Title & Progress Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(currentDoc.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${currentDoc.subject} • ${currentDoc.sourceLang.name} → ${currentDoc.targetLang.displayName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Reading Progress", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            Text("${(currentDoc.progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        LinearProgressIndicator(
                            progress = { currentDoc.progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                        )
                    }
                }

                // Section Navigation Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(currentSection.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { if (currentSectionIndex > 0) currentSectionIndex-- },
                            enabled = currentSectionIndex > 0
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Section")
                        }
                        Text(
                            "${currentSectionIndex + 1} / ${currentDoc.sections.size}",
                            modifier = Modifier.align(Alignment.CenterVertically),
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { if (currentSectionIndex < currentDoc.sections.size - 1) currentSectionIndex++ },
                            enabled = currentSectionIndex < currentDoc.sections.size - 1
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Section")
                        }
                    }
                }

                // View Mode Tabs: Original, Translated, Bilingual
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("BILINGUAL" to "Dual View", "ORIGINAL" to "Original", "TRANSLATED" to "Translated").forEach { (mode, label) ->
                        FilterChip(
                            selected = viewMode == mode,
                            onClick = { viewMode = mode },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Section Content Cards
                if (viewMode == "ORIGINAL" || viewMode == "BILINGUAL") {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Original (${currentDoc.sourceLang.name})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                IconButton(onClick = { ttsHelper.speak(currentSection.originalText, currentDoc.sourceLang.code) }) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen Original")
                                }
                            }
                            Text(currentSection.originalText, style = MaterialTheme.typography.bodyMedium, lineHeight = 24.sp)
                        }
                    }
                }

                if (viewMode == "TRANSLATED" || viewMode == "BILINGUAL") {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Translated (${currentDoc.targetLang.displayName})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                IconButton(onClick = { ttsHelper.speak(currentSection.translatedText, currentDoc.targetLang.code) }) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen Translated")
                                }
                            }
                            Text(currentSection.translatedText, style = MaterialTheme.typography.bodyMedium, lineHeight = 24.sp)
                        }
                    }
                }

                // AI Simplify Action Button
                FilledTonalButton(
                    onClick = {
                        isSimplifying = true
                        coroutineScope.launch {
                            val res = tutorService.simplifySection(currentSection.originalText, currentDoc.targetLang)
                            simplifiedExplanation = res.getOrDefault("Concept simplified.")
                            isSimplifying = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isSimplifying) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("JeduAI is breaking this down...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Explain This Section in Simple Words", fontWeight = FontWeight.Bold)
                    }
                }

                // Simplified Explanation Result
                if (simplifiedExplanation.isNotBlank()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F7FF))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("💡 Simplified Concept Explanation", fontWeight = FontWeight.Bold, color = Color(0xFF0077B6))
                            Text(simplifiedExplanation, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp, color = Color(0xFF03045E))
                        }
                    }
                }

                // Glossary Section
                if (currentSection.glossary.isNotEmpty()) {
                    Text("🔑 Section Vocabulary & Academic Concepts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    currentSection.glossary.forEach { term ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(term.term, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(term.translatedTerm, fontWeight = FontWeight.Bold)
                                }
                                Text(term.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Personal Notes
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📝 My Study Notes for This Section", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = userNoteText,
                            onValueChange = {
                                userNoteText = it
                                onSaveNote(currentDoc.id, currentSection.id, it)
                            },
                            placeholder = { Text("Write personal exam notes, mnemonics, or formulas...") },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Mark Completed Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onMarkCompleted(currentDoc.id, currentSection.id, !currentSection.isCompleted)
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Checkbox(
                        checked = currentSection.isCompleted,
                        onCheckedChange = { onMarkCompleted(currentDoc.id, currentSection.id, it) }
                    )
                    Text("Mark section as completed (+20 XP)", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
