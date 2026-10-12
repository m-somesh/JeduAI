# JeduAI (Multilingual AI Tutor & Mind Map Learning Platform)

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-blue.svg)](https://developer.android.com/jetpack/compose)
[![Room](https://img.shields.io/badge/Room%20DB-Offline--First-orange.svg)](https://developer.android.com/training/data-storage/room)
[![Gemini API](https://img.shields.io/badge/Gemini%20API-Flash%202.5%2F3.5-brightgreen.svg)](https://ai.google.dev/)

**JeduAI** is a state-of-the-art educational companion built with **Kotlin** and **Jetpack Compose (Material 3)**. Tailored for multilingual students, JeduAI transforms textbooks, lecture notes, and classroom materials into interactive visual mind maps, flashcards, practice quizzes, and real-time conversational AI tutoring in over 10 languages (including Hindi, Tamil, Telugu, Kannada, Malayalam, Bengali, Marathi, Gujarati, Spanish, French, German, and English).

---

## 🌟 Key Features

### 1. 🧠 Interactive AI Mind Maps & Knowledge Graphs
* **Automated Notes-to-Mind-Map Generation**: Upload PDFs, text documents, or paste notes to automatically generate structured concept trees.
* **Granular Detail Modes**: Choose from *Concise* (6–8 core concepts), *Standard* (12–16 concepts), or *Deep-Dive Comprehensive* (25+ granular concepts across multiple hierarchy levels).
* **Smooth Interactive Canvas**: Smooth pinch-to-zoom (0.3x to 3.0x), two-finger rotate, pan gesture support, and dynamic Bezier curved connecting lines (`cubicTo`) with depth color coding.
* **Cross-Cutting Relationship Badges**: Automatically visualizes interdisciplinary connections across disparate branches with interactive pill tags.
* **Deep Branch Study**: Tap any node to open the interactive Branch Study sheet:
  * 🔊 Multilingual Speech Explanation
  * 🎴 Auto-generated Flashcards with flip animation & mastery tracking
  * 📝 Practice Quizzes with instant scoring & rationale
  * 📥 Offline branch caching
  * 💬 Instant "Ask JeduAI about this concept" tutoring shortcut
* **High-Resolution Visual & Text Export**: Share mind maps as PNG diagrams or structured Markdown study revision summaries via standard Android shares.

### 2. 🤖 Multilingual AI Tutor (Conversational Learning)
* **Real-Time Dynamic Language Switching**: Switch explanations between English, Tamil, Hindi, Telugu, Kannada, Malayalam, Bengali, Marathi, French, German, Spanish, etc., with zero loss of conversation context.
* **Voice Speech-to-Text & Text-to-Speech**: Integrated Android `SpeechRecognizer` for hands-free voice questioning and dynamic TTS pronunciation.
* **Persona & Style Customization**: Choose tutoring modes:
  * 👩‍🏫 *Encouraging Teacher* (Socratic explanations with analogies)
  * 🔬 *Academic Professor* (Rigorous, formal definitions and proofs)
  * ⚡ *Quick Exam Prep* (Bullet-pointed key facts, formulas, and mnemonics)
* **Persistent Chat Sessions**: Automatically organized into Room database sessions with full search, replay, and offline access.

### 3. 🌐 Offline-First Learning & Sync Queue
* **Room SQLite Persistence**: Complete local database backing (`JeduDatabase`) with DAOs for sessions, messages, mind maps, offline resources, and pending sync actions.
* **Live Connectivity Monitor**: Dynamic network state banners reflecting connected/offline status in real time.
* **Two-Way Synchronization**: Enqueues offline questions, quiz submissions, and mind-map edits; automatically syncs when internet connectivity is restored.
* **Offline Resource Hub**: Unified manager displaying cached e-books, flashcard decks, offline quizzes, and mind maps with instant storage calculations (KB/MB).

### 4. 📚 Smart Document Hub & Translation
* **Document Stepper**: 6-stage document processing flow (Upload → Chapter Selection → Detail Config → AI Synthesis → Diagram Generation → Interactive Study).
* **Academic Translation**: Translate textbook excerpts, formulas, and questions while preserving technical formatting.
* **E-Books & Video Lectures**: Curated library of subject materials and video lessons with progress tracking.

### 5. 🎯 Gamification & Student Analytics
* **Daily Learning Streaks**: Tracks continuous study habits and milestones.
* **Mastery Progress**: Subject-wise mastery meters across Science, Mathematics, Languages, and Humanities.
* **Quiz History & Detailed Reviews**: In-depth explanations for correct and incorrect answers.

---

## 🏗️ Architecture & Technology Stack

```
com.example/
├── MainActivity.kt                      # Root host activity, navigation router, and system bars
├── data/
│   ├── local/                           # Room Database Layer
│   │   ├── JeduDatabase.kt              # Room database definition with schema migrations
│   │   ├── ChatEntity.kt & ChatDao.kt   # Chat session & message persistence
│   │   ├── MindMapEntity.kt & MindMapDao.kt # Mind map nodes, relations, and decks
│   │   └── OfflineEntity.kt & OfflineDao.kt # Offline resources and sync queue
│   ├── model/                           # Domain Data Models
│   │   ├── MindMapItem.kt               # MindMap, Node, Relation, Flashcard models
│   │   ├── OfflineItem.kt               # OfflineResource, SyncOperation models
│   │   ├── TutorChat.kt                 # ChatSession, ChatMessage, TutorPersona
│   │   ├── QuizItem.kt                  # QuizQuestion, QuizAttempt models
│   │   └── DocumentItem.kt              # Books, Chapters, ExtractedText models
│   └── repository/                      # Business Logic & Services
│       ├── JeduRepository.kt            # Core unified data repository
│       ├── GeminiTutorService.kt        # Gemini API client with educational prompting
│       ├── MindMapGeneratorService.kt   # AI & fallback structured mind map generator
│       ├── OfflineSyncRepository.kt     # Offline sync queue manager
│       ├── NetworkConnectivityMonitor.kt# Real-time Android ConnectivityManager flow
│       └── DocumentExtractorHelper.kt   # File extraction helper (PDF / Text / Notes)
└── ui/
    ├── components/                      # Shared UI Components
    │   └── JeduBottomNavBar.kt          # Material 3 Bottom Navigation & Route Enum
    └── screens/                         # Jetpack Compose Screens
        ├── HomeScreen.kt                # Student dashboard & quick learning hub
        ├── MindMapHomeScreen.kt         # Mind map library, filters, & creation entry
        ├── DocumentProcessScreen.kt     # 6-stage document-to-mindmap pipeline
        ├── MindMapViewerScreen.kt       # Zoomable/pannable canvas & branch study tools
        ├── TutorScreen.kt               # Real-time multilingual conversational tutor
        ├── OfflineLibraryScreen.kt      # Offline resource & sync queue manager
        ├── DocumentsScreen.kt           # E-books & textbook library
        ├── TranslateScreen.kt           # Multilingual academic translator
        ├── QuizScreen.kt                # Interactive quizzes with scoring
        ├── VideoScreen.kt               # Video lectures hub
        └── ProfileScreen.kt             # User profile, language settings, & streak
```

### Key Libraries Used
* **Jetpack Compose (Material 3)**: Modern declarative UI design.
* **Room Database & KSP**: High-performance local SQL data persistence.
* **Kotlin Coroutines & StateFlow**: Reactive, lifecycle-aware state management.
* **OkHttp & Kotlinx Serialization**: Resilient networking and type-safe JSON serialization.
* **Android Speech & Media APIs**: Voice recognition and text-to-speech engine.

---

## 🚀 Getting Started

### Prerequisites
* Android Studio Ladybug (2024.2+) or newer
* Android SDK 35 (compileSdk 35, minSdk 26)
* JDK 17 or JDK 21

### Building the Project
1. Clone the repository:
   ```bash
   git clone https://github.com/m-somesh/JeduAI.git
   cd JeduAI
   ```
2. Build debug APK using Gradle:
   ```bash
   gradle assembleDebug
   ```
3. Run local unit & Robolectric tests:
   ```bash
   gradle :app:testDebugUnitTest
   ```

---

## 🔒 Environment & Configuration
* AI Tutoring and Mind Map generation support server-side or client-side Google Gemini models (`gemini-2.5-flash` / `gemini-3.5-flash`).
* When using an external Gemini API key, configure it securely in `.env` or the Secrets panel:
  ```properties
  GEMINI_API_KEY=your_api_key_here
  ```
* JeduAI includes an intelligent **on-device educational fallback engine**, ensuring seamless operation even without an internet connection or API key.

---

## 📄 License
This project is licensed under the Apache License 2.0.
