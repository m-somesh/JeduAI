package com.example.data.model

data class AcademicTerm(
    val term: String,
    val translatedTerm: String,
    val explanation: String
)

enum class TranslationMode(val label: String, val description: String) {
    DIRECT("Direct Translation", "Preserves exact sentences and academic meaning"),
    LEARNING("Learning Translation", "Translates and explains technical terms & concept idioms")
}

data class TranslationRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sourceText: String,
    val translatedText: String,
    val sourceLang: LanguageOption,
    val targetLang: LanguageOption,
    val mode: TranslationMode = TranslationMode.LEARNING,
    val keyTerms: List<AcademicTerm> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val isSaved: Boolean = false
)
