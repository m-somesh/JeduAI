package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.*
import com.example.data.repository.GeminiTutorService
import com.example.data.repository.SpeechToTextHelper
import com.example.data.repository.TextToSpeechHelper
import com.example.ui.components.LanguageSelectorDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorScreen(
    currentSession: ChatSession?,
    allSessions: List<ChatSession>,
    defaultLanguage: LanguageOption,
    onSendMessage: (ChatMessage) -> Unit,
    onNewChat: (String, String) -> Unit,
    onSelectChat: (String) -> Unit,
    onDeleteChat: (String) -> Unit,
    onLanguageChanged: (LanguageOption) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val tutorService = remember { GeminiTutorService() }
    val ttsHelper = remember { TextToSpeechHelper(context) }

    var selectedMode by remember { mutableStateOf(TutorMode.BEGINNER) }
    var selectedLanguage by remember(defaultLanguage) { mutableStateOf(defaultLanguage) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    var partialSpeechText by remember { mutableStateOf("") }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showSessionsDrawer by remember { mutableStateOf(false) }

    // Re-explain message language picker dialog state
    var reExplainMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showReExplainDialog by remember { mutableStateOf(false) }

    // Language switched notification indicator
    var languageSwitchNotification by remember { mutableStateOf("") }

    // Speech Intent Fallback Launcher
    val speechIntentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenMatches?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                inputText = if (inputText.isBlank()) spokenText else "$inputText $spokenText"
            }
        }
    }

    // Initialize SpeechToTextHelper
    val sttHelper = remember {
        SpeechToTextHelper(
            context = context,
            onResult = { text ->
                isListening = false
                partialSpeechText = ""
                inputText = if (inputText.isBlank()) text else "$inputText $text"
            },
            onPartialResult = { partial ->
                partialSpeechText = partial
            },
            onListeningStateChanged = { listening ->
                isListening = listening
                if (!listening) {
                    partialSpeechText = ""
                }
            },
            onError = { errorMsg ->
                isListening = false
                partialSpeechText = ""
                // Graceful fallback to system RecognizerIntent if SpeechRecognizer failed
                try {
                    val intent = SpeechToTextHelper(context, {}, {}, {}, {}).createSpeechIntent(selectedLanguage.code)
                    speechIntentLauncher.launch(intent)
                } catch (_: Exception) {
                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Microphone Permission Launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            sttHelper.startListening(selectedLanguage.code)
        } else {
            Toast.makeText(context, "Microphone permission is required to dictate your questions.", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ttsHelper.shutdown()
            sttHelper.destroy()
        }
    }

    val quickPrompts = listOf(
        "Explain Quantum Superposition simply",
        "Step-by-step Photosynthesis & ATP cycle",
        "High-yield exam formulas for Physics",
        "Binary Search Tree traversal method",
        "Quick revision notes for Microeconomics"
    )

    if (showLanguagePicker) {
        LanguageSelectorDialog(
            title = "Switch Tutor Response Language",
            selectedLanguage = selectedLanguage,
            onLanguageSelected = { lang ->
                selectedLanguage = lang
                onLanguageChanged(lang)
                languageSwitchNotification = "Tutor will now explain in ${lang.flag} ${lang.displayName}"
            },
            onDismissRequest = { showLanguagePicker = false }
        )
    }

    if (showReExplainDialog && reExplainMessage != null) {
        LanguageSelectorDialog(
            title = "Re-explain This Answer In:",
            selectedLanguage = selectedLanguage,
            onLanguageSelected = { targetLang ->
                val originalMsg = reExplainMessage
                showReExplainDialog = false
                reExplainMessage = null

                if (originalMsg != null) {
                    isLoading = true
                    coroutineScope.launch {
                        val result = tutorService.askTutor(
                            question = "Please explain the following in ${targetLang.name} (${targetLang.nativeName}):\n\n${originalMsg.text}",
                            mode = selectedMode,
                            targetLanguage = targetLang,
                            subject = currentSession?.subject ?: "General"
                        )
                        val text = result.getOrDefault("Could not re-explain. Please try again.")
                        val newAiMsg = ChatMessage(
                            sender = MessageSender.AI,
                            text = text,
                            mode = selectedMode,
                            languageCode = targetLang.code
                        )
                        onSendMessage(newAiMsg)
                        isLoading = false
                        listState.animateScrollToItem((currentSession?.messages?.size ?: 0) + 1)
                    }
                }
            },
            onDismissRequest = {
                showReExplainDialog = false
                reExplainMessage = null
            }
        )
    }

    // Chat Sessions Sheet
    if (showSessionsDrawer) {
        ModalBottomSheet(onDismissRequest = { showSessionsDrawer = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Saved Chat Sessions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = {
                            onNewChat("Study Session ${allSessions.size + 1}", "General")
                            showSessionsDrawer = false
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Chat")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(allSessions) { session ->
                        val isCurrent = session.id == currentSession?.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    onSelectChat(session.id)
                                    showSessionsDrawer = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(session.title, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text(session.lastMessage, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { onDeleteChat(session.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // Title and Current Language Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { showSessionsDrawer = true }) {
                                Icon(Icons.Default.History, contentDescription = "Chat History")
                            }
                            Column {
                                Text(
                                    currentSession?.title ?: "JeduAI Tutor",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    "Mode: ${selectedMode.badge}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Prominent Language Selector Button
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.clickable { showLanguagePicker = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(selectedLanguage.flag)
                                Text(
                                    selectedLanguage.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Change Language",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Quick Language Switcher Row (Single-tap to switch among languages)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "🌐 Language: ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(SUPPORTED_LANGUAGES) { lang ->
                                val isSelected = lang.code == selectedLanguage.code
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedLanguage = lang
                                        onLanguageChanged(lang)
                                        languageSwitchNotification = "Tutor will now explain in ${lang.flag} ${lang.displayName}"
                                    },
                                    label = {
                                        Text("${lang.flag} ${lang.nativeName}")
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                        IconButton(
                            onClick = { showLanguagePicker = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Translate,
                                contentDescription = "All Languages",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Mode Selection Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        items(TutorMode.entries) { mode ->
                            val isSelected = mode == selectedMode
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedMode = mode },
                                label = { Text(mode.badge) },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Language Switch Banner Feedback
                    AnimatedVisibility(
                        visible = languageSwitchNotification.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    languageSwitchNotification,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = { languageSwitchNotification = "" },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Speech Dictation Active Banner
                    AnimatedVisibility(
                        visible = isListening,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 0.95f,
                            targetValue = 1.05f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse_scale"
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .scale(pulseScale)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.error)
                                    )
                                    Column {
                                        Text(
                                            "🎙️ Listening in ${selectedLanguage.flag} ${selectedLanguage.name}...",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        if (partialSpeechText.isNotBlank()) {
                                            Text(
                                                "\"$partialSpeechText\"",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                maxLines = 1
                                            )
                                        } else {
                                            Text(
                                                "Dictate your academic question now...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }

                                TextButton(
                                    onClick = { sttHelper.stopListening() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Done", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Current Language Indicator Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clickable { showLanguagePicker = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("🌐", fontSize = 14.sp)
                            Text(
                                "Answering in: ${selectedLanguage.flag} ${selectedLanguage.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            "Tap to switch ⇄",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Quick Prompts when few messages
                    if ((currentSession?.messages?.size ?: 0) < 3) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            items(quickPrompts) { prompt ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.clickable {
                                        inputText = prompt
                                    }
                                ) {
                                    Text(
                                        text = prompt,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Input Row with Text Field, Microphone Dictation Button, and Send Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Ask or tap mic to speak...") },
                            leadingIcon = {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clickable { showLanguagePicker = true }
                                        .padding(start = 6.dp)
                                ) {
                                    Text(
                                        "${selectedLanguage.flag} ${selectedLanguage.code.uppercase()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4
                        )

                        // Speech Dictation Microphone Button
                        IconButton(
                            onClick = {
                                if (isListening) {
                                    sttHelper.stopListening()
                                } else {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        sttHelper.startListening(selectedLanguage.code)
                                    } else {
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isListening) MaterialTheme.colorScheme.errorContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        ) {
                            Icon(
                                if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = if (isListening) "Stop Dictation" else "Dictate Question with Voice",
                                tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Send Message Button
                        FilledIconButton(
                            onClick = {
                                if (inputText.isNotBlank() && !isLoading) {
                                    val userMsgText = inputText.trim()
                                    inputText = ""
                                    val userMsg = ChatMessage(
                                        sender = MessageSender.USER,
                                        text = userMsgText,
                                        mode = selectedMode,
                                        languageCode = selectedLanguage.code
                                    )
                                    onSendMessage(userMsg)

                                    isLoading = true
                                    coroutineScope.launch {
                                        val result = tutorService.askTutor(
                                            question = userMsgText,
                                            mode = selectedMode,
                                            targetLanguage = selectedLanguage,
                                            subject = currentSession?.subject ?: "General"
                                        )

                                        val aiResponse = result.getOrDefault("I encountered an issue. Let's try again!")
                                        val aiMsg = ChatMessage(
                                            sender = MessageSender.AI,
                                            text = aiResponse,
                                            mode = selectedMode,
                                            languageCode = selectedLanguage.code
                                        )
                                        onSendMessage(aiMsg)
                                        isLoading = false
                                        listState.animateScrollToItem((currentSession?.messages?.size ?: 0) + 1)
                                    }
                                }
                            },
                            enabled = inputText.isNotBlank() && !isLoading,
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val messages = currentSession?.messages ?: emptyList()

        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🤖", fontSize = 54.sp)
                    Text("What are we learning today?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Type or dictate your question with the microphone. JeduAI explains step-by-step in ${selectedLanguage.name} (${selectedLanguage.nativeName}).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    OutlinedButton(
                        onClick = { showLanguagePicker = true },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Current Language: ${selectedLanguage.flag} ${selectedLanguage.displayName}")
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(
                        message = msg,
                        onSpeak = { text, lang -> ttsHelper.speak(text, lang) },
                        onCopy = { text ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("JeduAI Explanation", text)
                            clipboard.setPrimaryClip(clip)
                        },
                        onReExplainInOtherLanguage = {
                            reExplainMessage = msg
                            showReExplainDialog = true
                        }
                    )
                }

                if (isLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("JeduAI is reasoning in ${selectedLanguage.name} (${selectedLanguage.nativeName})...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    onSpeak: (String, String) -> Unit,
    onCopy: (String) -> Unit,
    onReExplainInOtherLanguage: () -> Unit
) {
    val isUser = message.sender == MessageSender.USER
    var hasLiked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Text(message.mode.badge, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                val langOption = SUPPORTED_LANGUAGES.find { it.code == message.languageCode }
                if (langOption != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            "${langOption.flag} ${langOption.nativeName}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = if (isUser) 0.dp else 2.dp,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )

                if (!isUser) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Re-explain in another language button
                        TextButton(
                            onClick = onReExplainInOtherLanguage,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("🌐 Translate", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onSpeak(message.text, message.languageCode) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Read Aloud", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onCopy(message.text) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { hasLiked = !hasLiked }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    if (hasLiked) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                                    contentDescription = "Like",
                                    tint = if (hasLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
