package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.JeduDatabase
import com.example.data.model.DocumentBook
import com.example.data.model.UserRole
import com.example.data.repository.JeduRepository
import com.example.data.repository.NetworkConnectivityMonitor
import com.example.ui.components.JeduBottomNavBar
import com.example.ui.components.JeduScreen
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private lateinit var repository: JeduRepository
    private lateinit var connectivityMonitor: NetworkConnectivityMonitor

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = JeduDatabase.getInstance(applicationContext)
        connectivityMonitor = NetworkConnectivityMonitor(applicationContext)
        repository = JeduRepository(
            chatDao = database.chatDao(),
            mindMapDao = database.mindMapDao(),
            offlineDao = database.offlineDao(),
            connectivityMonitor = connectivityMonitor
        )

        setContent {
            val userProfile by repository.userProfile.collectAsState()
            val chatSessions by repository.chatSessions.collectAsState()
            val currentSessionId by repository.currentSessionId.collectAsState()
            val translations by repository.translations.collectAsState()
            val documents by repository.documents.collectAsState()
            val videos by repository.videos.collectAsState()
            val quizzes by repository.quizzes.collectAsState()
            val quizAttempts by repository.quizAttempts.collectAsState()
            val achievements by repository.achievements.collectAsState()

            // Mind Maps & Offline
            val mindMaps by repository.mindMaps.collectAsState()
            val currentMindMapId by repository.currentMindMapId.collectAsState()
            val isConnected by repository.isConnected.collectAsState()
            val offlineResources by repository.offlineResources.collectAsState()
            val pendingOperations by repository.pendingSyncOperations.collectAsState()
            val isSyncing by (repository.offlineSyncRepository?.isSyncing ?: MutableStateFlow(false)).collectAsState()

            var currentScreen by remember { mutableStateOf(JeduScreen.HOME) }
            var processDocumentTarget by remember { mutableStateOf<DocumentBook?>(null) }
            val activeChatSession = chatSessions.find { it.id == currentSessionId } ?: chatSessions.firstOrNull()
            val activeMindMap = mindMaps.find { it.id == currentMindMapId } ?: mindMaps.firstOrNull()

            // Predictive / Hardware BackHandler
            BackHandler(enabled = currentScreen != JeduScreen.HOME && currentScreen != JeduScreen.ONBOARDING) {
                if (currentScreen == JeduScreen.MIND_MAP_VIEWER || currentScreen == JeduScreen.MIND_MAP_PROCESS) {
                    currentScreen = JeduScreen.MIND_MAPS
                } else {
                    currentScreen = JeduScreen.HOME
                }
            }

            MyApplicationTheme(darkTheme = userProfile.isDarkMode) {
                if (currentScreen == JeduScreen.ONBOARDING) {
                    OnboardingScreen(
                        currentProfile = userProfile,
                        onComplete = { updated ->
                            repository.updateProfile(updated)
                            currentScreen = JeduScreen.HOME
                        }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.statusBars),
                        topBar = {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 3.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                    // Offline Notice Banner
                                    if (!isConnected) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { currentScreen = JeduScreen.OFFLINE }
                                                .padding(bottom = 6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.WifiOff,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    "Offline Learning Active • Studying from local cache",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // App Branding
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.clickable { currentScreen = JeduScreen.HOME }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("J", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                                            }
                                            Column {
                                                Text(
                                                    "JeduAI",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    "Multilingual Learning",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // Role Indicator / Portal Tag
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.clickable {
                                                when (userProfile.role) {
                                                    UserRole.STUDENT -> currentScreen = JeduScreen.PROFILE
                                                    UserRole.STAFF -> currentScreen = JeduScreen.STAFF
                                                    UserRole.ADMIN -> currentScreen = JeduScreen.ADMIN
                                                }
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    when (userProfile.role) {
                                                        UserRole.STUDENT -> Icons.Default.School
                                                        UserRole.STAFF -> Icons.Default.SupervisorAccount
                                                        UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                                                    },
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    userProfile.role.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    // Secondary Feature Navigation Chips
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val extraScreens = mutableListOf(
                                            JeduScreen.MIND_MAPS,
                                            JeduScreen.OFFLINE,
                                            JeduScreen.VIDEOS,
                                            JeduScreen.QUIZZES,
                                            JeduScreen.LIBRARY
                                        )
                                        if (userProfile.role == UserRole.STAFF || userProfile.role == UserRole.ADMIN) {
                                            extraScreens.add(JeduScreen.STAFF)
                                        }
                                        if (userProfile.role == UserRole.ADMIN) {
                                            extraScreens.add(JeduScreen.ADMIN)
                                        }

                                        items(extraScreens) { screen ->
                                            val isSelected = currentScreen == screen
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { currentScreen = screen },
                                                label = { Text(screen.title) },
                                                leadingIcon = {
                                                    Icon(screen.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                                },
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        bottomBar = {
                            JeduBottomNavBar(
                                currentScreen = currentScreen,
                                onScreenSelected = { currentScreen = it }
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            AnimatedContent(
                                targetState = currentScreen,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "screen_navigation"
                            ) { screen ->
                                when (screen) {
                                    JeduScreen.HOME -> HomeScreen(
                                        profile = userProfile,
                                        recentDocuments = documents,
                                        recentChats = chatSessions,
                                        recentVideos = videos,
                                        onNavigateToTutor = { currentScreen = JeduScreen.TUTOR },
                                        onNavigateToTranslate = { currentScreen = JeduScreen.TRANSLATE },
                                        onNavigateToDocuments = { currentScreen = JeduScreen.DOCUMENTS },
                                        onNavigateToVideos = { currentScreen = JeduScreen.VIDEOS },
                                        onNavigateToQuizzes = { currentScreen = JeduScreen.QUIZZES },
                                        onNavigateToLibrary = { currentScreen = JeduScreen.LIBRARY },
                                        onNavigateToProfile = { currentScreen = JeduScreen.PROFILE },
                                        onNavigateToMindMaps = { currentScreen = JeduScreen.MIND_MAPS },
                                        onNavigateToOffline = { currentScreen = JeduScreen.OFFLINE }
                                    )

                                    JeduScreen.TUTOR -> TutorScreen(
                                        currentSession = activeChatSession,
                                        allSessions = chatSessions,
                                        defaultLanguage = userProfile.explanationLanguage,
                                        onSendMessage = { msg -> repository.addMessageToCurrentSession(msg) },
                                        onNewChat = { title, subj ->
                                            repository.createNewChat(title, subj)
                                        },
                                        onSelectChat = { id -> repository.selectChatSession(id) },
                                        onDeleteChat = { id -> repository.deleteChatSession(id) },
                                        onLanguageChanged = { lang -> repository.setExplanationLanguage(lang) }
                                    )

                                    JeduScreen.MIND_MAPS -> MindMapHomeScreen(
                                        mindMaps = mindMaps,
                                        documents = documents,
                                        isConnected = isConnected,
                                        onOpenMindMap = { id ->
                                            repository.selectMindMap(id)
                                            currentScreen = JeduScreen.MIND_MAP_VIEWER
                                        },
                                        onStartNewMindMap = {
                                            processDocumentTarget = null
                                            currentScreen = JeduScreen.MIND_MAP_PROCESS
                                        },
                                        onSelectDocumentForMap = { doc ->
                                            processDocumentTarget = doc
                                            currentScreen = JeduScreen.MIND_MAP_PROCESS
                                        },
                                        onDeleteMindMap = { id -> repository.deleteMindMap(id) },
                                        onRenameMindMap = { id, title -> repository.renameMindMap(id, title) },
                                        onDuplicateMindMap = { map ->
                                            val copy = map.copy(
                                                id = java.util.UUID.randomUUID().toString(),
                                                title = "${map.title} (Copy)",
                                                createdAt = System.currentTimeMillis(),
                                                updatedAt = System.currentTimeMillis()
                                            )
                                            repository.saveMindMap(copy)
                                        }
                                    )

                                    JeduScreen.MIND_MAP_PROCESS -> DocumentProcessScreen(
                                        initialDocument = processDocumentTarget,
                                        defaultLanguage = userProfile.explanationLanguage,
                                        onNavigateBack = { currentScreen = JeduScreen.MIND_MAPS },
                                        onMindMapGenerated = { map ->
                                            repository.saveMindMap(map)
                                            repository.selectMindMap(map.id)
                                            currentScreen = JeduScreen.MIND_MAP_VIEWER
                                        },
                                        onGenerateMindMap = { title, content, subj, detail, lang, docId ->
                                            repository.createMindMapFromContent(title, content, subj, detail, lang, docId)
                                        }
                                    )

                                    JeduScreen.MIND_MAP_VIEWER -> {
                                        if (activeMindMap != null) {
                                            MindMapViewerScreen(
                                                mindMap = activeMindMap,
                                                onNavigateBack = { currentScreen = JeduScreen.MIND_MAPS },
                                                onAskTutorAboutConcept = { query, subj ->
                                                    val newChat = repository.createNewChat("Mind Map Concept", subj)
                                                    repository.addMessageToCurrentSession(
                                                        com.example.data.model.ChatMessage(
                                                            sender = com.example.data.model.MessageSender.USER,
                                                            text = query,
                                                            mode = com.example.data.model.TutorMode.BEGINNER,
                                                            languageCode = userProfile.explanationLanguage.code
                                                        )
                                                    )
                                                    currentScreen = JeduScreen.TUTOR
                                                },
                                                onSaveBranchFlashcards = { deckId, cards ->
                                                    repository.saveFlashcardsForDeck(deckId, cards)
                                                },
                                                onRecordQuizAttempt = { attempt ->
                                                    repository.recordQuizAttempt(attempt)
                                                },
                                                generatorService = repository.mindMapGeneratorService
                                            )
                                        } else {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text("No mind map selected")
                                            }
                                        }
                                    }

                                    JeduScreen.OFFLINE -> OfflineLibraryScreen(
                                        resources = offlineResources,
                                        pendingOperations = pendingOperations,
                                        isConnected = isConnected,
                                        isSyncing = isSyncing,
                                        onOpenResource = { item ->
                                            when (item.resourceType) {
                                                com.example.data.model.OfflineResourceType.MIND_MAP -> {
                                                    repository.selectMindMap(item.id)
                                                    currentScreen = JeduScreen.MIND_MAP_VIEWER
                                                }
                                                com.example.data.model.OfflineResourceType.DOCUMENT -> {
                                                    currentScreen = JeduScreen.DOCUMENTS
                                                }
                                                com.example.data.model.OfflineResourceType.CHAT_SESSION -> {
                                                    repository.selectChatSession(item.id)
                                                    currentScreen = JeduScreen.TUTOR
                                                }
                                                com.example.data.model.OfflineResourceType.QUIZ -> {
                                                    currentScreen = JeduScreen.QUIZZES
                                                }
                                                else -> {}
                                            }
                                        },
                                        onRemoveOfflineResource = { id -> repository.removeOfflineCopy(id) },
                                        onRetrySync = { repository.retryPendingSync() },
                                        onClearAllCache = { repository.offlineSyncRepository?.clearAllOfflineResources() }
                                    )

                                    JeduScreen.TRANSLATE -> TranslateScreen(
                                        savedTranslations = translations,
                                        defaultSource = userProfile.interfaceLanguage,
                                        defaultTarget = userProfile.explanationLanguage,
                                        onSaveTranslation = { rec -> repository.saveTranslation(rec) },
                                        onDeleteTranslation = { id -> repository.deleteTranslation(id) }
                                    )

                                    JeduScreen.DOCUMENTS -> DocumentScreen(
                                        documents = documents,
                                        onAddDocument = { title, subj, content, targetLang ->
                                            repository.addCustomDocument(title, subj, content, targetLang)
                                        },
                                        onMarkCompleted = { docId, secId, completed ->
                                            repository.markSectionCompleted(docId, secId, completed)
                                        },
                                        onSaveNote = { docId, secId, note ->
                                            repository.updateSectionNote(docId, secId, note)
                                        }
                                    )

                                    JeduScreen.VIDEOS -> VideoScreen(
                                        videos = videos,
                                        onAskTutorAboutVideo = { _ ->
                                            repository.createNewChat("Video Q&A", "Video Study")
                                            currentScreen = JeduScreen.TUTOR
                                        }
                                    )

                                    JeduScreen.QUIZZES -> QuizScreen(
                                        quizzes = quizzes,
                                        recentAttempts = quizAttempts,
                                        onRecordAttempt = { attempt -> repository.recordQuizAttempt(attempt) }
                                    )

                                    JeduScreen.LIBRARY -> LibraryScreen(
                                        documents = documents,
                                        translations = translations,
                                        chats = chatSessions,
                                        achievements = achievements,
                                        onOpenDocument = { currentScreen = JeduScreen.DOCUMENTS },
                                        onOpenChat = { id ->
                                            repository.selectChatSession(id)
                                            currentScreen = JeduScreen.TUTOR
                                        }
                                    )

                                    JeduScreen.STAFF -> StaffPortalScreen(
                                        onCreateAssessment = { title, subj, questions ->
                                            repository.createStaffQuiz(title, subj, questions)
                                        }
                                    )

                                    JeduScreen.ADMIN -> AdminPortalScreen()

                                    JeduScreen.PROFILE -> ProfileScreen(
                                        profile = userProfile,
                                        onUpdateProfile = { updated -> repository.updateProfile(updated) },
                                        onReplayOnboarding = { currentScreen = JeduScreen.ONBOARDING }
                                    )

                                    JeduScreen.ONBOARDING -> {
                                        // Handled above
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::connectivityMonitor.isInitialized) {
            connectivityMonitor.unregister()
        }
    }
}
