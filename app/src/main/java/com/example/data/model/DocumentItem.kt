package com.example.data.model

data class DocumentSection(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sectionNumber: Int,
    val title: String,
    val originalText: String,
    val translatedText: String,
    val simplifiedExplanation: String,
    val glossary: List<AcademicTerm> = emptyList(),
    val isCompleted: Boolean = false,
    val userNote: String = "",
    val isBookmarked: Boolean = false
)

data class DocumentBook(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val subject: String,
    val sourceLang: LanguageOption,
    val targetLang: LanguageOption,
    val sections: List<DocumentSection>,
    val progress: Float = 0f,
    val isUserUploaded: Boolean = false
)
