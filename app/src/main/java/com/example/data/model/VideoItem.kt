package com.example.data.model

data class VideoChapter(
    val title: String,
    val timestampSeconds: Int,
    val formattedTime: String
)

data class VideoTranscriptSegment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestampSeconds: Int,
    val formattedTime: String,
    val originalText: String,
    val translatedText: String
)

data class VideoLecture(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val instructor: String,
    val duration: String,
    val videoUrl: String,
    val summary: String,
    val sourceLang: LanguageOption,
    val targetLang: LanguageOption,
    val chapters: List<VideoChapter>,
    val transcript: List<VideoTranscriptSegment>
)
