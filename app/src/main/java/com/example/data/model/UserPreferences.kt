package com.example.data.model

data class LanguageOption(
    val code: String,
    val name: String,
    val nativeName: String,
    val flag: String = ""
) {
    val displayName: String get() = "$name ($nativeName)"
}

val SUPPORTED_LANGUAGES = listOf(
    LanguageOption("en", "English", "English", "🇺🇸"),
    LanguageOption("hi", "Hindi", "हिन्दी", "🇮🇳"),
    LanguageOption("ta", "Tamil", "தமிழ்", "🇮🇳"),
    LanguageOption("te", "Telugu", "తెలుగు", "🇮🇳"),
    LanguageOption("es", "Spanish", "Español", "🇪🇸"),
    LanguageOption("fr", "French", "Français", "🇫🇷"),
    LanguageOption("bn", "Bengali", "বাংলা", "🇮🇳"),
    LanguageOption("mr", "Marathi", "मराठी", "🇮🇳"),
    LanguageOption("gu", "Gujarati", "ગુજરાતી", "🇮🇳"),
    LanguageOption("kn", "Kannada", "ಕನ್ನಡ", "🇮🇳"),
    LanguageOption("ml", "Malayalam", "മലയാളം", "🇮🇳"),
    LanguageOption("de", "German", "Deutsch", "🇩🇪"),
    LanguageOption("ja", "Japanese", "日本語", "🇯🇵"),
    LanguageOption("ar", "Arabic", "العربية", "🇸🇦")
)

enum class UserRole {
    STUDENT,
    STAFF,
    ADMIN
}

enum class EducationLevel(val label: String) {
    MIDDLE_SCHOOL("Middle School (Grades 6-8)"),
    HIGH_SCHOOL("High School (Grades 9-12)"),
    COLLEGE_UNDERGRAD("Undergraduate / College"),
    POSTGRADUATE("Postgraduate / Masters"),
    COMPETITIVE_EXAMS("Competitive Exams (JEE, NEET, GATE, UPSC)")
}

data class UserProfile(
    val name: String = "Somesh Kumar",
    val email: String = "somesh@jeduai.app",
    val interfaceLanguage: LanguageOption = SUPPORTED_LANGUAGES[0], // English
    val explanationLanguage: LanguageOption = SUPPORTED_LANGUAGES[0], // English (Default)
    val educationLevel: EducationLevel = EducationLevel.COLLEGE_UNDERGRAD,
    val role: UserRole = UserRole.STUDENT,
    val interests: List<String> = listOf("Computer Science", "Artificial Intelligence", "Physics"),
    val dailyGoalMinutes: Int = 30,
    val currentStreak: Int = 5,
    val xpPoints: Int = 840,
    val isDarkMode: Boolean = false,
    val fontSizeScale: Float = 1.0f
)
