package com.example.data.model

data class QuizQuestion(
    val id: String = java.util.UUID.randomUUID().toString(),
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val conceptTag: String = ""
)

data class Quiz(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val subject: String,
    val difficulty: String = "Intermediate",
    val questions: List<QuizQuestion>,
    val xpReward: Int = 50
)

data class QuizAttempt(
    val id: String = java.util.UUID.randomUUID().toString(),
    val quizId: String,
    val quizTitle: String,
    val score: Int,
    val totalQuestions: Int,
    val xpEarned: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class AchievementBadge(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val isUnlocked: Boolean,
    val currentProgress: Int,
    val targetProgress: Int
)
