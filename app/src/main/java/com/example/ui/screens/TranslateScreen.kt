package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.ui.components.LanguageSelectorDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateScreen(
    savedTranslations: List<TranslationRecord>,
    defaultSource: LanguageOption = SUPPORTED_LANGUAGES[0], // English
    defaultTarget: LanguageOption = SUPPORTED_LANGUAGES[0], // English (Default)
    onSaveTranslation: (TranslationRecord) -> Unit,
    onDeleteTranslation: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val tutorService = remember { GeminiTutorService() }
    val ttsHelper = remember { TextToSpeechHelper(context) }

    DisposableEffect(Unit) {
        onDispose { ttsHelper.shutdown() }
    }

    var sourceLang by remember { mutableStateOf(defaultSource) }
    var targetLang by remember { mutableStateOf(defaultTarget) }
    var isLearningMode by remember { mutableStateOf(true) }

    var inputText by remember { mutableStateOf("") }
    var translatedResult by remember { mutableStateOf("") }
    var extractedGlossary by remember { mutableStateOf<List<AcademicTerm>>(emptyList()) }
    var isTranslating by remember { mutableStateOf(false) }

    var showSourcePicker by remember { mutableStateOf(false) }
    var showTargetPicker by remember { mutableStateOf(false) }

    val sampleAcademicTexts = listOf(
        "Photosynthesis converts light energy into glucose molecules.",
        "Newton's First Law states an object remains in uniform motion unless acted on by external force.",
        "A binary search tree has O(log n) average time complexity for searching.",
        "The mitochondria generate ATP required for cellular metabolic processes."
    )

    if (showSourcePicker) {
        LanguageSelectorDialog(
            title = "Source Language",
            selectedLanguage = sourceLang,
            onLanguageSelected = { sourceLang = it },
            onDismissRequest = { showSourcePicker = false }
        )
    }

    if (showTargetPicker) {
        LanguageSelectorDialog(
            title = "Target Language",
            selectedLanguage = targetLang,
            onLanguageSelected = { targetLang = it },
            onDismissRequest = { showTargetPicker = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Language Selector Row with Swap
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Source Language
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showSourcePicker = true }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(sourceLang.flag, fontSize = 20.sp)
                    Column {
                        Text("From", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(sourceLang.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }

                // Swap Button
                IconButton(
                    onClick = {
                        val temp = sourceLang
                        sourceLang = targetLang
                        targetLang = temp
                    }
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Swap Languages", tint = MaterialTheme.colorScheme.primary)
                }

                // Target Language
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showTargetPicker = true }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(targetLang.flag, fontSize = 20.sp)
                    Column {
                        Text("To", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(targetLang.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Mode Switcher Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterChip(
                selected = isLearningMode,
                onClick = { isLearningMode = true },
                label = { Text("🎓 Learning Mode (With Glossary)") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            FilterChip(
                selected = !isLearningMode,
                onClick = { isLearningMode = false },
                label = { Text("⚡ Direct Translation") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Input Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Type or paste academic textbook text here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 110.dp),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (inputText.isNotEmpty()) {
                            IconButton(onClick = { inputText = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick suggestions
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(sampleAcademicTexts) { sample ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { inputText = sample }
                        ) {
                            Text(
                                sample.take(28) + "...",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (inputText.isNotBlank() && !isTranslating) {
                            isTranslating = true
                            coroutineScope.launch {
                                val result = tutorService.translateText(
                                    text = inputText,
                                    sourceLang = sourceLang,
                                    targetLang = targetLang,
                                    isLearningMode = isLearningMode
                                )
                                val (trans, gloss) = result.getOrDefault(Pair("Translation unavailable.", emptyList()))
                                translatedResult = trans
                                extractedGlossary = gloss
                                isTranslating = false

                                // Automatically record translation
                                onSaveTranslation(
                                    TranslationRecord(
                                        sourceText = inputText,
                                        translatedText = trans,
                                        sourceLang = sourceLang,
                                        targetLang = targetLang,
                                        mode = if (isLearningMode) TranslationMode.LEARNING else TranslationMode.DIRECT,
                                        keyTerms = gloss,
                                        isSaved = true
                                    )
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = inputText.isNotBlank() && !isTranslating
                ) {
                    if (isTranslating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Translating to ${targetLang.name}...")
                    } else {
                        Icon(Icons.Default.Translate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Translate & Analyze", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Translation Result Section
        if (translatedResult.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${targetLang.name} (${targetLang.nativeName})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row {
                            IconButton(onClick = { ttsHelper.speak(translatedResult, targetLang.code) }) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen")
                            }
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Translation", translatedResult))
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            }
                        }
                    }

                    Text(
                        text = translatedResult,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp
                    )

                    // Extracted Glossary
                    if (extractedGlossary.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("🔑 Key Academic Concepts & Glossary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                        extractedGlossary.forEach { term ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(term.term, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(term.translatedTerm, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(term.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Saved Translations
        if (savedTranslations.isNotEmpty()) {
            Text("Recent Saved Translations", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            savedTranslations.take(4).forEach { rec ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${rec.sourceLang.name} → ${rec.targetLang.name}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { onDeleteTranslation(rec.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        Text(rec.sourceText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        Text(rec.translatedText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
