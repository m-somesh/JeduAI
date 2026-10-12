package com.example.data.model

enum class TutorMode(val title: String, val badge: String, val description: String) {
    BEGINNER("Explain Like I'm a Beginner", "💡 Beginner", "Simple analogies, everyday examples, zero jargon"),
    EXAM_PREP("Exam Preparation", "🎯 High-Yield", "Formulas, definitions, common mistakes, mark-scoring points"),
    PROBLEM_SOLVING("Step-by-Step Solver", "🔢 Step-by-Step", "Numbered logical breakdowns for equations, code, or proofs"),
    QUICK_REVISION("Quick Revision", "⚡ Revision", "Bite-sized summaries, mnemonics, quick formulas"),
    PRACTICE_QUIZ("Practice Quiz", "📝 Quiz Me", "Instant test questions with interactive hints"),
    LANGUAGE_LEARNING("Language Tutor", "🌐 Bilingual", "Sentence breakdowns, vocabulary drills, academic terminology")
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val mode: TutorMode = TutorMode.BEGINNER,
    val languageCode: String = "en",
    val isBookmarked: Boolean = false,
    val userRating: Int = 0 // 1 for thumbs up, -1 for thumbs down
)

enum class MessageSender {
    USER,
    AI
}

data class ChatSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val subject: String,
    val lastMessage: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val messages: List<ChatMessage> = emptyList()
)
