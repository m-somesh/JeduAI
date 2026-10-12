package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.repository.JeduRepository
import com.example.data.repository.MindMapGeneratorService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun testAppNameResource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("JeduAI", appName)
    }

    @Test
    fun testRepositoryInitialDataAndActions() {
        val repository = JeduRepository()

        // Test User Profile
        val profile = repository.userProfile.value
        assertEquals("Somesh Kumar", profile.name)
        assertEquals(EducationLevel.COLLEGE_UNDERGRAD, profile.educationLevel)

        // Test Language change
        val tamil = SUPPORTED_LANGUAGES.first { it.code == "ta" }
        repository.setExplanationLanguage(tamil)
        assertEquals("ta", repository.userProfile.value.explanationLanguage.code)

        // Test Document Library
        val docs = repository.documents.value
        assertTrue("Documents should be preloaded", docs.isNotEmpty())
        val firstDoc = docs.first()
        assertTrue("Document should have sections", firstDoc.sections.isNotEmpty())

        // Test Section completion and XP increase
        val initialXp = repository.userProfile.value.xpPoints
        val firstSection = firstDoc.sections.first()
        repository.markSectionCompleted(firstDoc.id, firstSection.id, true)
        val updatedDoc = repository.documents.value.first { it.id == firstDoc.id }
        assertTrue(updatedDoc.sections.first().isCompleted)
        assertTrue(repository.userProfile.value.xpPoints > initialXp)

        // Test Chat Creation
        val chat = repository.createNewChat("Physics Review", "Physics")
        assertEquals("Physics Review", chat.title)
        repository.addMessageToCurrentSession(
            ChatMessage(
                sender = MessageSender.USER,
                text = "Explain quantum physics",
                mode = TutorMode.BEGINNER,
                languageCode = "en"
            )
        )
        val updatedChat = repository.chatSessions.value.first { it.id == chat.id }
        assertEquals(1, updatedChat.messages.size)

        // Test Mind Maps initial seeding
        val maps = repository.mindMaps.value
        assertTrue("Mind maps should be preloaded", maps.isNotEmpty())
        val firstMap = maps.first()
        assertNotNull(firstMap.rootNode)
        assertTrue(firstMap.nodes.size >= 4)

        // Test Offline Resources initial seeding
        val offlineRes = repository.offlineResources.value
        assertTrue("Offline resources should be preloaded", offlineRes.isNotEmpty())
    }

    @Test
    fun testRoomDatabasePersistence() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, JeduDatabase::class.java).build()
        val dao: ChatDao = db.chatDao()

        val session = ChatSessionEntity(
            id = "sess_thermo",
            title = "Thermodynamics Q&A",
            subject = "Physics",
            lastMessage = "Entropy always increases in an isolated system.",
            updatedAt = System.currentTimeMillis()
        )
        dao.insertSession(session)
        assertEquals(1, dao.getSessionCount())

        val msg = ChatMessageEntity(
            id = "msg_1",
            sessionId = "sess_thermo",
            sender = "USER",
            text = "State the second law of thermodynamics",
            timestamp = System.currentTimeMillis(),
            mode = "EXAM_PREP",
            languageCode = "en",
            isBookmarked = false,
            userRating = 1
        )
        dao.insertMessage(msg)

        val messages = dao.getMessagesForSession("sess_thermo").first()
        assertEquals(1, messages.size)
        assertEquals("State the second law of thermodynamics", messages[0].text)

        // Delete test
        dao.deleteSessionAndMessages("sess_thermo")
        assertEquals(0, dao.getSessionCount())

        db.close()
    }

    @Test
    fun testMindMapRoomPersistence() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, JeduDatabase::class.java).build()
        val mindMapDao = db.mindMapDao()

        val mapEntity = MindMapEntity(
            id = "map_ai_nlp",
            title = "Natural Language Processing",
            subject = "Computer Science",
            sourceDocumentTitle = "NLP Course Notes",
            sourceDocumentId = null,
            rootNodeId = "node_nlp_root",
            responseLanguageCode = "en",
            detailLevel = "BALANCED",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            status = "COMPLETED",
            isAvailableOffline = true
        )

        val rootNode = MindMapNodeEntity(
            id = "node_nlp_root",
            mindMapId = "map_ai_nlp",
            parentNodeId = null,
            title = "Natural Language Processing",
            summary = "Computational linguistics and machine learning on human language.",
            detailedExplanation = "Deep learning and statistical models parsing syntax and semantics.",
            nodeType = "ROOT",
            sourcePage = 1,
            sourceSection = "Intro",
            sortOrder = 0,
            isExpanded = true,
            prerequisitesJson = "",
            examplesJson = "",
            keyTakeawaysJson = "Tokens and vectors"
        )

        val branch1 = MindMapNodeEntity(
            id = "node_nlp_transformers",
            mindMapId = "map_ai_nlp",
            parentNodeId = "node_nlp_root",
            title = "Transformer Architecture",
            summary = "Self-attention mechanism processing sequences in parallel.",
            detailedExplanation = "Self-attention Q, K, V matrices and multi-head attention.",
            nodeType = "MAIN_TOPIC",
            sourcePage = 2,
            sourceSection = "Chapter 2",
            sortOrder = 1,
            isExpanded = true,
            prerequisitesJson = "",
            examplesJson = "",
            keyTakeawaysJson = "O(n^2) attention complexity"
        )

        val relation = MindMapRelationEntity(
            id = "rel_1",
            mindMapId = "map_ai_nlp",
            sourceNodeId = "node_nlp_transformers",
            targetNodeId = "node_nlp_root",
            relationType = "PART_OF",
            description = "Transformers form the backbone of modern NLP"
        )

        mindMapDao.insertCompleteMindMap(
            map = mapEntity,
            nodes = listOf(rootNode, branch1),
            relations = listOf(relation)
        )

        assertEquals(1, mindMapDao.getMindMapCount())

        val fetchedNodes = mindMapDao.getNodesForMapSync("map_ai_nlp")
        assertEquals(2, fetchedNodes.size)
        assertEquals("node_nlp_root", fetchedNodes[0].id)
        assertNull(fetchedNodes[0].parentNodeId)

        val fetchedRelations = mindMapDao.getRelationsForMapSync("map_ai_nlp")
        assertEquals(1, fetchedRelations.size)
        assertEquals("PART_OF", fetchedRelations[0].relationType)

        // Rename test
        mindMapDao.renameMindMap("map_ai_nlp", "Modern NLP & LLMs", System.currentTimeMillis())
        val updatedMap = mindMapDao.getMindMapById("map_ai_nlp")
        assertEquals("Modern NLP & LLMs", updatedMap?.title)

        // Cascade delete test
        mindMapDao.deleteCompleteMindMap("map_ai_nlp")
        assertEquals(0, mindMapDao.getMindMapCount())
        assertEquals(0, mindMapDao.getNodesForMapSync("map_ai_nlp").size)

        db.close()
    }

    @Test
    fun testOfflineDaoAndSyncQueue() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, JeduDatabase::class.java).build()
        val offlineDao = db.offlineDao()

        val resEntity = OfflineResourceEntity(
            id = "res_101",
            resourceType = "MIND_MAP",
            title = "Quantum Information Notes",
            subject = "Physics",
            sizeBytes = 1024 * 120L,
            downloadStatus = "AVAILABLE_OFFLINE",
            syncStatus = "SYNCED",
            downloadedAt = System.currentTimeMillis(),
            lastSyncAt = System.currentTimeMillis(),
            hasRemoteUpdate = false,
            extraMetadata = ""
        )
        offlineDao.insertResource(resEntity)
        assertEquals(1, offlineDao.getResourceCount())

        // Sync queue enqueuing
        val syncOp = SyncQueueEntity(
            id = "op_chat_1",
            operationType = "SYNC_CHAT_MESSAGE",
            payloadJson = "{\"text\": \"Offline question\"}",
            status = "PENDING",
            retryCount = 0,
            createdAt = System.currentTimeMillis(),
            lastAttemptAt = 0L,
            errorMessage = ""
        )
        offlineDao.enqueueOperation(syncOp)

        val pending = offlineDao.getPendingOperationsSync()
        assertEquals(1, pending.size)
        assertEquals("SYNC_CHAT_MESSAGE", pending[0].operationType)

        offlineDao.deleteOperation("op_chat_1")
        assertEquals(0, offlineDao.getPendingOperationsSync().size)

        db.close()
    }

    @Test
    fun testMindMapGeneratorServiceAndBranchLearning() = runTest {
        val service = MindMapGeneratorService()

        val generatedMapResult = service.generateMindMap(
            documentTitle = "Thermodynamics & Statistical Mechanics",
            content = "First law states conservation of energy. Second law states entropy of an isolated system always increases.",
            subject = "Physics",
            detailLevel = MindMapDetailLevel.CONCISE,
            targetLanguage = SUPPORTED_LANGUAGES[0]
        )
        assertTrue(generatedMapResult.isSuccess)
        val map = generatedMapResult.getOrThrow()

        assertNotNull(map.rootNode)
        assertTrue(map.nodes.size >= 5)

        // Verify hierarchy: all child nodes have valid parent reference
        val nodeIds = map.nodes.map { it.id }.toSet()
        map.nodes.forEach { node ->
            if (node.id != map.rootNodeId) {
                assertNotNull(node.parentNodeId)
                assertTrue("Parent ID must exist in graph", nodeIds.contains(node.parentNodeId))
            }
        }

        // Test Branch Flashcards Generation
        val firstBranch = map.getChildrenOf(map.rootNodeId).first()
        val children = map.getChildrenOf(firstBranch.id)
        val flashcardsResult = service.generateBranchFlashcards(firstBranch, children, SUPPORTED_LANGUAGES[0])
        assertTrue(flashcardsResult.isSuccess)
        val cards = flashcardsResult.getOrThrow()
        assertTrue("Flashcards must be created", cards.isNotEmpty())

        // Test Branch Quiz Generation
        val quizResult = service.generateBranchQuiz(firstBranch, children, SUPPORTED_LANGUAGES[0])
        assertTrue(quizResult.isSuccess)
        val quiz = quizResult.getOrThrow()
        assertTrue("Quiz questions must be generated", quiz.questions.isNotEmpty())
        assertTrue("Questions must have 4 options", quiz.questions.first().options.size == 4)

        // Test Markdown Revision Notes
        val markdown = service.generateRevisionNotesMarkdown(map)
        assertTrue(markdown.contains("Revision Notes"))
        assertTrue(markdown.contains(firstBranch.title))
    }

    @Test
    fun testSpeechToTextLanguageTagResolution() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sttHelper = com.example.data.repository.SpeechToTextHelper(context, {}, {}, {}, {})
        assertEquals("en-US", sttHelper.getLanguageTag("en"))
        assertEquals("hi-IN", sttHelper.getLanguageTag("hi"))
        assertEquals("ta-IN", sttHelper.getLanguageTag("ta"))
        assertEquals("te-IN", sttHelper.getLanguageTag("te"))
        assertEquals("es-ES", sttHelper.getLanguageTag("es"))

        val intent = sttHelper.createSpeechIntent("hi")
        assertEquals(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH, intent.action)
        assertEquals("hi-IN", intent.getStringExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE))
        sttHelper.destroy()
    }
}
