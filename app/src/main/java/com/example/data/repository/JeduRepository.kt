package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class JeduRepository(
    private val chatDao: ChatDao? = null,
    private val mindMapDao: MindMapDao? = null,
    private val offlineDao: OfflineDao? = null,
    private val connectivityMonitor: NetworkConnectivityMonitor? = null
) {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val mindMapGeneratorService = MindMapGeneratorService()
    val offlineSyncRepository = offlineDao?.let { OfflineSyncRepository(it, connectivityMonitor) }

    val isConnected: StateFlow<Boolean> =
        connectivityMonitor?.isConnected ?: MutableStateFlow(true).asStateFlow()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _chatSessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val chatSessions: StateFlow<List<ChatSession>> = _chatSessions.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String>("")
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    private val _translations = MutableStateFlow<List<TranslationRecord>>(emptyList())
    val translations: StateFlow<List<TranslationRecord>> = _translations.asStateFlow()

    private val _documents = MutableStateFlow<List<DocumentBook>>(emptyList())
    val documents: StateFlow<List<DocumentBook>> = _documents.asStateFlow()

    private val _videos = MutableStateFlow<List<VideoLecture>>(emptyList())
    val videos: StateFlow<List<VideoLecture>> = _videos.asStateFlow()

    private val _quizzes = MutableStateFlow<List<Quiz>>(emptyList())
    val quizzes: StateFlow<List<Quiz>> = _quizzes.asStateFlow()

    private val _quizAttempts = MutableStateFlow<List<QuizAttempt>>(emptyList())
    val quizAttempts: StateFlow<List<QuizAttempt>> = _quizAttempts.asStateFlow()

    private val _achievements = MutableStateFlow<List<AchievementBadge>>(emptyList())
    val achievements: StateFlow<List<AchievementBadge>> = _achievements.asStateFlow()

    // Mind Maps
    private val _mindMaps = MutableStateFlow<List<MindMap>>(emptyList())
    val mindMaps: StateFlow<List<MindMap>> = _mindMaps.asStateFlow()

    private val _currentMindMapId = MutableStateFlow<String>("")
    val currentMindMapId: StateFlow<String> = _currentMindMapId.asStateFlow()

    // Offline Resources & Sync Queue
    private val _offlineResources = MutableStateFlow<List<OfflineResourceItem>>(emptyList())
    val offlineResources: StateFlow<List<OfflineResourceItem>> = _offlineResources.asStateFlow()

    private val _pendingSyncOperations = MutableStateFlow<List<PendingSyncOperation>>(emptyList())
    val pendingSyncOperations: StateFlow<List<PendingSyncOperation>> = _pendingSyncOperations.asStateFlow()

    private val _deckFlashcards = MutableStateFlow<Map<String, List<Flashcard>>>(emptyMap())
    val deckFlashcards: StateFlow<Map<String, List<Flashcard>>> = _deckFlashcards.asStateFlow()

    init {
        seedInitialData()
        if (chatDao != null) {
            observeChatDatabase()
        }
        if (mindMapDao != null) {
            observeMindMapDatabase()
        }
        if (offlineSyncRepository != null) {
            observeOfflineDatabase()
        }
    }

    private fun observeChatDatabase() {
        val dao = chatDao ?: return
        repositoryScope.launch {
            if (dao.getSessionCount() == 0) {
                val seed = _chatSessions.value.firstOrNull()
                if (seed != null) {
                    dao.insertSession(ChatSessionEntity.fromDomain(seed))
                    seed.messages.forEach { msg ->
                        dao.insertMessage(ChatMessageEntity.fromDomain(seed.id, msg))
                    }
                }
            }

            combine(dao.getAllSessions(), dao.getAllMessages()) { sessionEntities, messageEntities ->
                val messagesBySession = messageEntities.groupBy { it.sessionId }
                sessionEntities.map { sessionEntity ->
                    val msgs = messagesBySession[sessionEntity.id]?.map { it.toDomain() } ?: emptyList()
                    sessionEntity.toDomain(msgs)
                }
            }.collect { domainSessions ->
                if (domainSessions.isNotEmpty()) {
                    _chatSessions.value = domainSessions
                    if (_currentSessionId.value.isBlank() || domainSessions.none { it.id == _currentSessionId.value }) {
                        _currentSessionId.value = domainSessions.first().id
                    }
                }
            }
        }
    }

    private fun observeMindMapDatabase() {
        val dao = mindMapDao ?: return
        repositoryScope.launch {
            if (dao.getMindMapCount() == 0) {
                // Seed initial maps
                val seedMaps = _mindMaps.value
                seedMaps.forEach { map ->
                    dao.insertCompleteMindMap(
                        map = MindMapEntity.fromDomain(map),
                        nodes = map.nodes.map { MindMapNodeEntity.fromDomain(it) },
                        relations = map.relations.map { MindMapRelationEntity.fromDomain(it) }
                    )
                }
            }

            dao.getAllMindMaps().collect { entities ->
                val fullMaps = entities.map { entity ->
                    val nodes = dao.getNodesForMapSync(entity.id).map { it.toDomain() }
                    val relations = dao.getRelationsForMapSync(entity.id).map { it.toDomain() }
                    entity.toDomain(nodes, relations)
                }
                if (fullMaps.isNotEmpty()) {
                    _mindMaps.value = fullMaps
                    if (_currentMindMapId.value.isBlank() || fullMaps.none { it.id == _currentMindMapId.value }) {
                        _currentMindMapId.value = fullMaps.first().id
                    }
                }
            }
        }
    }

    private fun observeOfflineDatabase() {
        val syncRepo = offlineSyncRepository ?: return
        repositoryScope.launch {
            syncRepo.offlineResources.collect { list ->
                _offlineResources.value = list
            }
        }
        repositoryScope.launch {
            syncRepo.pendingOperations.collect { list ->
                _pendingSyncOperations.value = list
            }
        }
    }

    private fun seedInitialData() {
        // Initial Chat Sessions
        val defaultChat = ChatSession(
            id = "chat_quantum_intro",
            title = "Quantum Physics Basics & Superposition",
            subject = "Physics",
            lastMessage = "A qubit can exist in a superposition of |0⟩ and |1⟩ basis states simultaneously!",
            updatedAt = System.currentTimeMillis() - 3600000,
            messages = listOf(
                ChatMessage(
                    sender = MessageSender.USER,
                    text = "Can you explain what quantum superposition is in simple terms?",
                    mode = TutorMode.BEGINNER,
                    languageCode = "en"
                ),
                ChatMessage(
                    sender = MessageSender.AI,
                    text = """
                        💡 Understanding Quantum Superposition with a Simple Analogy:
                        
                        Imagine you have a regular coin on a table. When it rests flat, it can only show either 'Heads' (0) or 'Tails' (1). That is how a classical computer bit works.
                        
                        Now imagine spinning that coin vigorously on the table. While it is spinning, it is a blur of both Heads and Tails simultaneously! This spinning state represents a **Superposition** in a quantum bit (qubit).
                        
                        Key Takeaways:
                        1. A Qubit can exist in a linear combination of both |0⟩ and |1⟩ states simultaneously.
                        2. Only when we measure or observe the qubit does it collapse into a definite state (0 or 1).
                    """.trimIndent(),
                    mode = TutorMode.BEGINNER,
                    languageCode = "en"
                )
            )
        )
        _chatSessions.value = listOf(defaultChat)
        _currentSessionId.value = defaultChat.id

        // Initial Saved Translations
        _translations.value = listOf(
            TranslationRecord(
                id = "trans_1",
                sourceText = "Mitochondria are membrane-bound cell organelles that generate most of the chemical energy needed to power the cell's biochemical reactions.",
                translatedText = "Mitochondria are membrane-bound cell organelles that generate ATP (chemical energy) to power biochemical reactions.",
                sourceLang = SUPPORTED_LANGUAGES[0],
                targetLang = SUPPORTED_LANGUAGES[0],
                mode = TranslationMode.LEARNING,
                keyTerms = listOf(
                    AcademicTerm("Mitochondria", "Powerhouse organelle", "Double-membraned organelle generating ATP"),
                    AcademicTerm("ATP", "Adenosine Triphosphate", "Primary molecular energy currency of living cells")
                ),
                isSaved = true
            )
        )

        // Seed Curated Mind Maps
        val quantumMap = createSeedMindMap(
            id = "map_quantum_arch",
            title = "Quantum Computing & Superposition",
            subject = "Physics & Computer Science",
            sourceDocTitle = "Introduction to Quantum Information Systems",
            rootTitle = "Quantum Computing Architecture",
            branches = listOf(
                SeedBranch(
                    title = "Quantum Bits (Qubits)",
                    summary = "The fundamental two-state quantum mechanical unit of information.",
                    explanation = "Unlike classical bits that hold either 0 or 1, a qubit represents a vector in a 2-dimensional complex Hilbert space.",
                    takeaways = listOf("Represented on the Bloch Sphere", "State vectors: |ψ⟩ = α|0⟩ + β|1⟩", "|α|² + |β|² = 1"),
                    examples = listOf("Superconducting transmon qubits", "Trapped ion quantum gates"),
                    subtopics = listOf("Bloch Sphere Representation", "State Normalization")
                ),
                SeedBranch(
                    title = "Quantum Superposition",
                    summary = "Coexistence of multiple orthogonal states until measured.",
                    explanation = "Allows quantum algorithms like Grover's and Shor's to explore vast combinatorial problem spaces simultaneously.",
                    takeaways = listOf("Measurement forces wave-function collapse", "Interference enhances correct answers"),
                    examples = listOf("Hadamard Gate initialization", "Dual-slit quantum photon experiment"),
                    subtopics = listOf("Wavefunction Collapse", "Quantum Interference")
                ),
                SeedBranch(
                    title = "Quantum Entanglement",
                    summary = "Non-local physical phenomenon where quantum states cannot be factored independently.",
                    explanation = "Measuring one entangled qubit instantaneously determines the state of the other, regardless of distance.",
                    takeaways = listOf("Bell States represent maximally entangled pairs", "Essential for quantum teleportation & cryptography"),
                    examples = listOf("EPR Paradox experiment", "Quantum Key Distribution (BB84 protocol)"),
                    subtopics = listOf("Bell States & Tests", "Quantum Teleportation")
                ),
                SeedBranch(
                    title = "Quantum Algorithms & Gates",
                    summary = "Unitary matrix operations and practical algorithmic speedups.",
                    explanation = "Quantum logic gates must be reversible unitary operations (U†U = I) preserving quantum probability amplitudes.",
                    takeaways = listOf("Pauli-X, Y, Z, Phase, and CNOT gates", "Shor's algorithm offers exponential speedup for factoring"),
                    examples = listOf("Shor's Factoring Algorithm", "Grover's Database Search (quadratic speedup)"),
                    subtopics = listOf("Unitary Gate Reversibility", "Shor's & Grover's Speedups")
                )
            )
        )

        val dataStructuresMap = createSeedMindMap(
            id = "map_data_structures",
            title = "Data Structures & Asymptotics",
            subject = "Computer Science",
            sourceDocTitle = "Algorithms & Complexity Theory Notes",
            rootTitle = "Core Data Structures & Complexity",
            branches = listOf(
                SeedBranch(
                    title = "Linear Structures",
                    summary = "Contiguous arrays, linked lists, stacks, and queues with sequential access.",
                    explanation = "Provides foundational memory layout paradigms. Fixed-size arrays allow O(1) random access; linked lists allow O(1) dynamic insertions.",
                    takeaways = listOf("Array random access is O(1)", "Stack is LIFO; Queue is FIFO"),
                    examples = listOf("Call stack execution", "Circular buffer queues"),
                    subtopics = listOf("Dynamic Arrays", "Singly & Doubly Linked Lists")
                ),
                SeedBranch(
                    title = "Hierarchical Trees",
                    summary = "Binary Search Trees, AVL balance, and Priority Heaps.",
                    explanation = "Trees structure hierarchical relationships and log-time search properties. Balanced trees prevent worst-case linear degradation.",
                    takeaways = listOf("Balanced BST search: O(log n)", "Binary Min/Max Heap insertion: O(log n)"),
                    examples = listOf("Database indexing B-trees", "Dijkstra's Priority Queue"),
                    subtopics = listOf("Self-Balancing AVL Trees", "Binary Min/Max Heaps")
                ),
                SeedBranch(
                    title = "Graph Traversal & DAGs",
                    summary = "Vertices, directed/undirected edges, BFS, DFS, and topological sort.",
                    explanation = "Models networks, dependency resolution, shortest paths, and state-space exploration.",
                    takeaways = listOf("BFS uses Queue (O(V+E))", "DFS uses Stack/Recursion", "Topological sort requires DAG"),
                    examples = listOf("Social network shortest hops", "Build system compilation order"),
                    subtopics = listOf("Breadth-First Search (BFS)", "Depth-First Search & Cycles")
                ),
                SeedBranch(
                    title = "Asymptotic Complexity",
                    summary = "Big-O, Big-Omega, Big-Theta, and space-time trade-offs.",
                    explanation = "Formal mathematical characterization of algorithm resource growth as input size approaches infinity.",
                    takeaways = listOf("Big-O defines upper bound", "Big-Theta defines tight bound", "Amortized analysis averages multi-step costs"),
                    examples = listOf("QuickSort worst O(n²) vs average O(n log n)", "Hash table O(1) amortized insertion"),
                    subtopics = listOf("Big-O vs Big-Theta", "Amortized Analysis")
                )
            )
        )

        _mindMaps.value = listOf(quantumMap, dataStructuresMap)
        _currentMindMapId.value = quantumMap.id

        // Seed initial Offline Library resources
        _offlineResources.value = listOf(
            OfflineResourceItem(
                id = quantumMap.id,
                resourceType = OfflineResourceType.MIND_MAP,
                title = quantumMap.title,
                subject = quantumMap.subject,
                sizeBytes = 184 * 1024L,
                downloadStatus = DownloadStatus.AVAILABLE_OFFLINE,
                syncStatus = SyncStatus.SYNCED
            ),
            OfflineResourceItem(
                id = dataStructuresMap.id,
                resourceType = OfflineResourceType.MIND_MAP,
                title = dataStructuresMap.title,
                subject = dataStructuresMap.subject,
                sizeBytes = 210 * 1024L,
                downloadStatus = DownloadStatus.AVAILABLE_OFFLINE,
                syncStatus = SyncStatus.SYNCED
            ),
            OfflineResourceItem(
                id = defaultChat.id,
                resourceType = OfflineResourceType.CHAT_SESSION,
                title = defaultChat.title,
                subject = defaultChat.subject,
                sizeBytes = 64 * 1024L,
                downloadStatus = DownloadStatus.AVAILABLE_OFFLINE,
                syncStatus = SyncStatus.SYNCED
            )
        )

        // Seed Documents
        _documents.value = listOf(
            DocumentBook(
                id = "doc_quantum_intro",
                title = "Introduction to Quantum Information Systems",
                subject = "Physics & STEM",
                sourceLang = SUPPORTED_LANGUAGES[0],
                targetLang = SUPPORTED_LANGUAGES[0],
                sections = listOf(
                    DocumentSection(
                        sectionNumber = 1,
                        title = "Quantum State Vectors and Probability",
                        originalText = "In quantum mechanics, the state of a physical system is represented by a unit vector in a complex Hilbert space. The state is traditionally denoted using Dirac bra-ket notation |ψ⟩.",
                        translatedText = "In quantum mechanics, the state of a physical system is represented by a unit vector in a complex Hilbert space, denoted using Dirac notation |ψ⟩.",
                        simplifiedExplanation = "Unlike a regular bit that is only 0 or 1, a qubit represents a vector that can be in multiple states at once.",
                        glossary = listOf(
                            AcademicTerm("Hilbert Space", "Complex vector space", "An inner product space that is complete with respect to distance metric"),
                            AcademicTerm("Bra-ket Notation", "Dirac notation", "Standard notation for describing quantum states")
                        ),
                        isCompleted = true
                    ),
                    DocumentSection(
                        sectionNumber = 2,
                        title = "Linear Superposition and Unitary Evolution",
                        originalText = "The time evolution of a closed quantum system is described by a unitary operator U. Unitary operators preserve the norm of state vectors, guaranteeing that total probability remains 1.",
                        translatedText = "The time evolution of a closed quantum system is described by a unitary operator U, preserving total probability of 1.",
                        simplifiedExplanation = "Every quantum operation is reversible and preserves 100% total probability.",
                        glossary = listOf(
                            AcademicTerm("Unitary Operator", "Reversible operator", "A bounded linear operator on Hilbert space satisfying U†U = I")
                        )
                    )
                ),
                progress = 0.5f
            ),
            DocumentBook(
                id = "doc_ds_algorithms",
                title = "Data Structures & Computational Complexity",
                subject = "Computer Science",
                sourceLang = SUPPORTED_LANGUAGES[0],
                targetLang = SUPPORTED_LANGUAGES[0],
                sections = listOf(
                    DocumentSection(
                        sectionNumber = 1,
                        title = "Asymptotic Analysis and Big-O",
                        originalText = "Asymptotic analysis evaluates how the execution time or space requirement of an algorithm scales as input size n tends to infinity. Big-O gives the upper bound.",
                        translatedText = "Asymptotic analysis evaluates how runtime or memory scales as input size n grows to infinity.",
                        simplifiedExplanation = "Big-O tells you how fast an algorithm runs when inputs get huge.",
                        glossary = listOf(
                            AcademicTerm("Big-O Notation", "Upper bound complexity", "Describes the worst-case growth rate of an algorithm")
                        ),
                        isCompleted = true
                    )
                ),
                progress = 1.0f
            )
        )

        // Seed Quizzes
        _quizzes.value = listOf(
            Quiz(
                id = "quiz_quantum_foundations",
                title = "Quantum Information & Superposition",
                subject = "Physics & STEM",
                difficulty = "Medium",
                questions = listOf(
                    QuizQuestion(
                        question = "What is the key principle that allows a quantum computer qubit to represent 0 and 1 simultaneously?",
                        options = listOf(
                            "Quantum Tunneling",
                            "Quantum Superposition",
                            "Photoelectric Effect",
                            "Thermodynamic Equilibrium"
                        ),
                        correctIndex = 1,
                        explanation = "Quantum Superposition allows a quantum system to exist in a linear combination of states until measured.",
                        conceptTag = "Superposition"
                    ),
                    QuizQuestion(
                        question = "Which asymptotic notation represents the tight asymptotic bound of an algorithm?",
                        options = listOf(
                            "Big-O (O)",
                            "Big-Omega (Ω)",
                            "Big-Theta (Θ)",
                            "Small-o (o)"
                        ),
                        correctIndex = 2,
                        explanation = "Big-Theta (Θ) gives both upper and lower bounds, indicating the exact order of growth.",
                        conceptTag = "Asymptotics"
                    )
                ),
                xpReward = 60
            )
        )

        // Achievements
        _achievements.value = listOf(
            AchievementBadge(
                id = "badge_polyglot",
                title = "Multilingual Scholar",
                description = "Learn concepts in 3 or more regional languages",
                iconEmoji = "🌐",
                isUnlocked = true,
                currentProgress = 3,
                targetProgress = 3
            ),
            AchievementBadge(
                id = "badge_mind_mapper",
                title = "Master Mind Mapper",
                description = "Generate and explore 3 AI hierarchical mind maps",
                iconEmoji = "🗺️",
                isUnlocked = true,
                currentProgress = 2,
                targetProgress = 3
            ),
            AchievementBadge(
                id = "badge_offline_scholar",
                title = "Offline Learner",
                description = "Download and review 3 learning resources offline",
                iconEmoji = "⚡",
                isUnlocked = true,
                currentProgress = 3,
                targetProgress = 3
            )
        )
    }

    private fun createSeedMindMap(
        id: String,
        title: String,
        subject: String,
        sourceDocTitle: String,
        rootTitle: String,
        branches: List<SeedBranch>
    ): MindMap {
        val rootId = "root_${id}"
        val rootNode = MindMapNode(
            id = rootId,
            mindMapId = id,
            parentNodeId = null,
            title = rootTitle,
            summary = "Central conceptual root node for $title.",
            detailedExplanation = "Comprehensive hierarchical mind map integrating foundational principles, core mechanisms, mathematical formulations, and practical exam takeaways.",
            nodeType = MindMapNodeType.ROOT,
            sourcePage = 1,
            sourceSection = "Overview",
            sortOrder = 0,
            keyTakeaways = listOf("Unified conceptual framework", "High-yield examination topics")
        )

        val allNodes = mutableListOf(rootNode)
        val allRelations = mutableListOf<MindMapRelation>()

        branches.forEachIndexed { bIndex, branch ->
            val bId = "b_${id}_$bIndex"
            val branchNode = MindMapNode(
                id = bId,
                mindMapId = id,
                parentNodeId = rootId,
                title = branch.title,
                summary = branch.summary,
                detailedExplanation = branch.explanation,
                nodeType = MindMapNodeType.MAIN_TOPIC,
                sourcePage = bIndex + 1,
                sourceSection = "Chapter ${bIndex + 1}",
                sortOrder = bIndex + 1,
                keyTakeaways = branch.takeaways,
                examples = branch.examples
            )
            allNodes.add(branchNode)

            allRelations.add(
                MindMapRelation(
                    mindMapId = id,
                    sourceNodeId = bId,
                    targetNodeId = rootId,
                    relationType = MindMapRelationType.PART_OF,
                    description = "Major thematic branch connecting to central topic"
                )
            )

            branch.subtopics.forEachIndexed { sIndex, subTitle ->
                val sId = "sub_${id}_${bIndex}_$sIndex"
                val subNode = MindMapNode(
                    id = sId,
                    mindMapId = id,
                    parentNodeId = bId,
                    title = subTitle,
                    summary = "Subtopic analyzing $subTitle within ${branch.title}.",
                    detailedExplanation = "Detailed breakdown of $subTitle with relevant formulas, principles, and applications.",
                    nodeType = if (sIndex % 2 == 0) MindMapNodeType.CONCEPT else MindMapNodeType.EXAMPLE,
                    sourcePage = bIndex + 1,
                    sourceSection = "Section ${bIndex + 1}.${sIndex + 1}",
                    sortOrder = (bIndex + 1) * 10 + sIndex,
                    keyTakeaways = listOf("Core exam principle: $subTitle")
                )
                allNodes.add(subNode)
            }
        }

        // Add cross branch relation
        if (branches.size >= 2) {
            allRelations.add(
                MindMapRelation(
                    mindMapId = id,
                    sourceNodeId = "b_${id}_0",
                    targetNodeId = "b_${id}_1",
                    relationType = MindMapRelationType.PREREQUISITE,
                    description = "Foundations are prerequisites to understanding active mechanisms"
                )
            )
        }

        return MindMap(
            id = id,
            title = title,
            subject = subject,
            sourceDocumentTitle = sourceDocTitle,
            rootNodeId = rootId,
            responseLanguage = SUPPORTED_LANGUAGES[0],
            detailLevel = MindMapDetailLevel.BALANCED,
            nodes = allNodes,
            relations = allRelations,
            isAvailableOffline = true
        )
    }

    private data class SeedBranch(
        val title: String,
        val summary: String,
        val explanation: String,
        val takeaways: List<String>,
        val examples: List<String>,
        val subtopics: List<String>
    )

    // Mind Map Operations
    fun selectMindMap(id: String) {
        _currentMindMapId.value = id
    }

    fun saveMindMap(map: MindMap) {
        _mindMaps.update { list ->
            listOf(map) + list.filter { it.id != map.id }
        }
        _currentMindMapId.value = map.id
        addXp(40)

        // Register to offline resources & database
        offlineSyncRepository?.registerOfflineResource(
            id = map.id,
            type = OfflineResourceType.MIND_MAP,
            title = map.title,
            subject = map.subject,
            sizeBytes = (map.nodes.size * 12 * 1024L)
        )

        mindMapDao?.let { dao ->
            repositoryScope.launch {
                dao.insertCompleteMindMap(
                    map = MindMapEntity.fromDomain(map),
                    nodes = map.nodes.map { MindMapNodeEntity.fromDomain(it) },
                    relations = map.relations.map { MindMapRelationEntity.fromDomain(it) }
                )
            }
        }
    }

    fun renameMindMap(id: String, newTitle: String) {
        _mindMaps.update { list ->
            list.map { if (it.id == id) it.copy(title = newTitle, updatedAt = System.currentTimeMillis()) else it }
        }
        mindMapDao?.let { dao ->
            repositoryScope.launch {
                dao.renameMindMap(id, newTitle, System.currentTimeMillis())
            }
        }
    }

    fun deleteMindMap(id: String) {
        _mindMaps.update { it.filter { map -> map.id != id } }
        if (_currentMindMapId.value == id) {
            _currentMindMapId.value = _mindMaps.value.firstOrNull()?.id ?: ""
        }
        offlineSyncRepository?.removeOfflineResource(id)

        mindMapDao?.let { dao ->
            repositoryScope.launch {
                dao.deleteCompleteMindMap(id)
            }
        }
    }

    suspend fun createMindMapFromContent(
        title: String,
        content: String,
        subject: String,
        detailLevel: MindMapDetailLevel,
        targetLanguage: LanguageOption,
        sourceDocId: String? = null
    ): Result<MindMap> {
        val result = mindMapGeneratorService.generateMindMap(
            documentTitle = title,
            content = content,
            subject = subject,
            detailLevel = detailLevel,
            targetLanguage = targetLanguage,
            sourceDocumentId = sourceDocId
        )
        if (result.isSuccess) {
            val map = result.getOrThrow()
            saveMindMap(map)
        }
        return result
    }

    // Flashcard Deck operations
    fun getFlashcardsForDeck(deckId: String): List<Flashcard> {
        return _deckFlashcards.value[deckId] ?: emptyList()
    }

    fun saveFlashcardsForDeck(deckId: String, cards: List<Flashcard>) {
        _deckFlashcards.update { map ->
            map + (deckId to cards)
        }
        offlineSyncRepository?.saveFlashcardsForDeck(cards)
        offlineSyncRepository?.registerOfflineResource(
            id = deckId,
            type = OfflineResourceType.FLASHCARD_DECK,
            title = "Flashcards: $deckId",
            subject = "Revision Deck",
            sizeBytes = cards.size * 2 * 1024L
        )
    }

    fun toggleFlashcardMastery(cardId: String, deckId: String) {
        _deckFlashcards.update { map ->
            val list = map[deckId] ?: emptyList()
            val updated = list.map { card ->
                if (card.id == cardId) card.copy(isMastered = !card.isMastered, reviewCount = card.reviewCount + 1)
                else card
            }
            map + (deckId to updated)
        }
        val isNowMastered = _deckFlashcards.value[deckId]?.find { it.id == cardId }?.isMastered ?: false
        offlineSyncRepository?.updateCardMastery(cardId, deckId, isNowMastered)
        if (isNowMastered) addXp(15)
    }

    // Offline Management
    fun markResourceOffline(id: String, type: OfflineResourceType, title: String, subject: String) {
        offlineSyncRepository?.registerOfflineResource(id, type, title, subject)
    }

    fun removeOfflineCopy(id: String) {
        offlineSyncRepository?.removeOfflineResource(id)
    }

    fun retryPendingSync() {
        offlineSyncRepository?.syncPendingOperations()
    }

    // User Profile Actions
    fun updateProfile(profile: UserProfile) {
        _userProfile.value = profile
    }

    fun setInterfaceLanguage(lang: LanguageOption) {
        _userProfile.update { it.copy(interfaceLanguage = lang) }
    }

    fun setExplanationLanguage(lang: LanguageOption) {
        _userProfile.update { it.copy(explanationLanguage = lang) }
    }

    fun setEducationLevel(level: EducationLevel) {
        _userProfile.update { it.copy(educationLevel = level) }
    }

    fun setUserRole(role: UserRole) {
        _userProfile.update { it.copy(role = role) }
    }

    fun addXp(amount: Int) {
        _userProfile.update { it.copy(xpPoints = it.xpPoints + amount) }
    }

    // Chat Actions (with Room Persistence & Offline Queue)
    fun createNewChat(title: String, subject: String): ChatSession {
        val newSession = ChatSession(
            title = title,
            subject = subject,
            lastMessage = "Started a new study session"
        )
        _chatSessions.update { listOf(newSession) + it }
        _currentSessionId.value = newSession.id

        chatDao?.let { dao ->
            repositoryScope.launch {
                dao.insertSession(ChatSessionEntity.fromDomain(newSession))
            }
        }
        return newSession
    }

    fun selectChatSession(id: String) {
        _currentSessionId.value = id
    }

    fun addMessageToCurrentSession(message: ChatMessage) {
        var currentId = _currentSessionId.value
        if (currentId.isBlank() || _chatSessions.value.none { it.id == currentId }) {
            val newSession = createNewChat("Study Session", "General")
            currentId = newSession.id
        }
        val targetId = currentId
        _chatSessions.update { list ->
            list.map { session ->
                if (session.id == targetId) {
                    session.copy(
                        lastMessage = message.text.take(80),
                        updatedAt = System.currentTimeMillis(),
                        messages = session.messages + message
                    )
                } else {
                    session
                }
            }
        }
        addXp(10)

        // If offline and it's a user message, enqueue sync
        if (!isConnected.value && message.sender == MessageSender.USER) {
            offlineSyncRepository?.enqueueChatMessageSync(
                sessionId = targetId,
                messageText = message.text,
                mode = message.mode.name,
                lang = message.languageCode
            )
        }

        chatDao?.let { dao ->
            repositoryScope.launch {
                dao.insertMessage(ChatMessageEntity.fromDomain(targetId, message))
                dao.updateSessionLastMessage(
                    sessionId = targetId,
                    lastMessage = message.text.take(80),
                    updatedAt = System.currentTimeMillis()
                )
            }
        }
    }

    fun deleteChatSession(id: String) {
        _chatSessions.update { it.filter { session -> session.id != id } }
        if (_currentSessionId.value == id) {
            _currentSessionId.value = _chatSessions.value.firstOrNull()?.id ?: ""
        }

        chatDao?.let { dao ->
            repositoryScope.launch {
                dao.deleteSessionAndMessages(id)
            }
        }
    }

    // Translation Actions
    fun saveTranslation(record: TranslationRecord) {
        _translations.update { listOf(record) + it }
        addXp(15)
    }

    fun toggleSaveTranslation(id: String) {
        _translations.update { list ->
            list.map { if (it.id == id) it.copy(isSaved = !it.isSaved) else it }
        }
    }

    fun deleteTranslation(id: String) {
        _translations.update { it.filter { item -> item.id != id } }
    }

    // Document Actions
    fun addCustomDocument(title: String, subject: String, content: String, targetLang: LanguageOption) {
        val sections = content.split("\n\n").filter { it.isNotBlank() }.mapIndexed { index, paragraph ->
            DocumentSection(
                sectionNumber = index + 1,
                title = "Section ${index + 1}",
                originalText = paragraph,
                translatedText = "Translation: $paragraph",
                simplifiedExplanation = "Concept summary for section ${index + 1}",
                glossary = emptyList()
            )
        }
        val newDoc = DocumentBook(
            title = title,
            subject = subject,
            sourceLang = SUPPORTED_LANGUAGES[0],
            targetLang = targetLang,
            sections = sections,
            progress = 0f,
            isUserUploaded = true
        )
        _documents.update { listOf(newDoc) + it }
        addXp(25)
    }

    fun markSectionCompleted(docId: String, sectionId: String, completed: Boolean) {
        _documents.update { list ->
            list.map { doc ->
                if (doc.id == docId) {
                    val updatedSections = doc.sections.map { sec ->
                        if (sec.id == sectionId) sec.copy(isCompleted = completed) else sec
                    }
                    val completedCount = updatedSections.count { it.isCompleted }
                    val progress = if (updatedSections.isNotEmpty()) completedCount.toFloat() / updatedSections.size else 0f
                    doc.copy(sections = updatedSections, progress = progress)
                } else {
                    doc
                }
            }
        }
        if (completed) addXp(20)
    }

    fun updateSectionNote(docId: String, sectionId: String, note: String) {
        _documents.update { list ->
            list.map { doc ->
                if (doc.id == docId) {
                    val updatedSections = doc.sections.map { sec ->
                        if (sec.id == sectionId) sec.copy(userNote = note) else sec
                    }
                    doc.copy(sections = updatedSections)
                } else {
                    doc
                }
            }
        }
    }

    // Quiz Actions
    fun recordQuizAttempt(attempt: QuizAttempt) {
        _quizAttempts.update { listOf(attempt) + it }
        addXp(attempt.xpEarned)

        offlineSyncRepository?.enqueueQuizAttemptSync(attempt)
    }

    fun createStaffQuiz(title: String, subject: String, questions: List<QuizQuestion>) {
        val newQuiz = Quiz(
            title = title,
            subject = subject,
            difficulty = "Teacher Verified",
            questions = questions,
            xpReward = 75
        )
        _quizzes.update { listOf(newQuiz) + it }
    }
}
