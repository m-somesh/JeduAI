package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EducationLevel
import com.example.data.model.LanguageOption
import com.example.data.model.SUPPORTED_LANGUAGES
import com.example.data.model.UserProfile
import com.example.ui.components.LanguageSelectorDialog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    currentProfile: UserProfile,
    onComplete: (UserProfile) -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var selectedUiLang by remember { mutableStateOf(currentProfile.interfaceLanguage) }
    var selectedExplainLang by remember { mutableStateOf(currentProfile.explanationLanguage) }
    var selectedEduLevel by remember { mutableStateOf(currentProfile.educationLevel) }
    var selectedInterests by remember { mutableStateOf(currentProfile.interests.toSet()) }
    var selectedGoal by remember { mutableIntStateOf(currentProfile.dailyGoalMinutes) }

    var showUiLangDialog by remember { mutableStateOf(false) }
    var showExplainLangDialog by remember { mutableStateOf(false) }

    val subjectsList = listOf(
        "Computer Science", "Artificial Intelligence", "Physics",
        "Mathematics", "Cellular Biology", "Chemistry",
        "Microeconomics", "Data Structures", "Constitutional Law", "History"
    )

    if (showUiLangDialog) {
        LanguageSelectorDialog(
            title = "Choose Interface Language",
            selectedLanguage = selectedUiLang,
            onLanguageSelected = { selectedUiLang = it },
            onDismissRequest = { showUiLangDialog = false }
        )
    }

    if (showExplainLangDialog) {
        LanguageSelectorDialog(
            title = "Choose Preferred Study Language",
            selectedLanguage = selectedExplainLang,
            onLanguageSelected = { selectedExplainLang = it },
            onDismissRequest = { showExplainLangDialog = false }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 0) {
                        TextButton(
                            onClick = { step-- },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Back")
                        }
                    } else {
                        TextButton(
                            onClick = {
                                onComplete(
                                    currentProfile.copy(
                                        interfaceLanguage = selectedUiLang,
                                        explanationLanguage = selectedExplainLang,
                                        educationLevel = selectedEduLevel,
                                        interests = selectedInterests.toList(),
                                        dailyGoalMinutes = selectedGoal
                                    )
                                )
                            },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Skip")
                        }
                    }

                    Button(
                        onClick = {
                            if (step < 4) {
                                step++
                            } else {
                                onComplete(
                                    currentProfile.copy(
                                        interfaceLanguage = selectedUiLang,
                                        explanationLanguage = selectedExplainLang,
                                        educationLevel = selectedEduLevel,
                                        interests = selectedInterests.toList(),
                                        dailyGoalMinutes = selectedGoal
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .widthIn(min = 140.dp)
                    ) {
                        Text(
                            text = if (step == 4) "Start Learning" else "Continue",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step Indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                repeat(5) { idx ->
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (idx == step) 28.dp else 12.dp)
                            .clip(CircleShape)
                            .background(
                                if (idx == step) MaterialTheme.colorScheme.primary
                                else if (idx < step) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "onboarding_steps"
            ) { currentStep ->
                when (currentStep) {
                    0 -> StepWelcome()
                    1 -> StepLanguages(
                        uiLang = selectedUiLang,
                        explainLang = selectedExplainLang,
                        onOpenUiLang = { showUiLangDialog = true },
                        onOpenExplainLang = { showExplainLangDialog = true }
                    )
                    2 -> StepEducation(
                        selected = selectedEduLevel,
                        onSelect = { selectedEduLevel = it }
                    )
                    3 -> StepInterests(
                        allSubjects = subjectsList,
                        selected = selectedInterests,
                        onToggle = { sub ->
                            selectedInterests = if (selectedInterests.contains(sub)) {
                                selectedInterests - sub
                            } else {
                                selectedInterests + sub
                            }
                        }
                    )
                    4 -> StepGoal(
                        selectedGoal = selectedGoal,
                        onSelectGoal = { selectedGoal = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun StepWelcome() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF39C6F4),
                            Color(0xFF5B5FEF)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome to JeduAI",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Learn in Your Language.\nGrow Without Limits.",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🤖", fontSize = 24.sp)
                    Text("Multilingual AI Tutor for STEM & College Subjects", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📖", fontSize = 24.sp)
                    Text("Translate Textbooks with Bilingual Comparison", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🎥", fontSize = 24.sp)
                    Text("Video Lectures with Synced Regional Subtitles", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🎯", fontSize = 24.sp)
                    Text("Daily Practice Quizzes, Streaks & Flashcards", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun StepLanguages(
    uiLang: LanguageOption,
    explainLang: LanguageOption,
    onOpenUiLang: () -> Unit,
    onOpenExplainLang: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text("Language Preferences", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Choose how JeduAI speaks and explains concepts to you.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenUiLang() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(18.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("App Interface Language", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${uiLang.flag} ${uiLang.displayName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Text("Change", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenExplainLang() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(18.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Concept Explanation Language", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${explainLang.flag} ${explainLang.displayName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Text("Change", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("💡 You can switch languages anytime from the tutor chat or profile.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StepEducation(
    selected: EducationLevel,
    onSelect: (EducationLevel) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text("Your Education Level", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("This helps JeduAI adjust the depth and difficulty of explanations.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(20.dp))

        EducationLevel.entries.forEach { level ->
            val isSelected = level == selected
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onSelect(level) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        level.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepInterests(
    allSubjects: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Choose Your Subjects", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Pick topics you are currently studying or preparing for.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(24.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            allSubjects.forEach { sub ->
                val isSelected = selected.contains(sub)
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggle(sub) },
                    label = { Text(sub) },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
private fun StepGoal(
    selectedGoal: Int,
    onSelectGoal: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text("Daily Study Target", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Consistency is key! Set a realistic daily learning commitment.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(24.dp))

        val options = listOf(15, 30, 45, 60)
        options.forEach { minutes ->
            val isSelected = minutes == selectedGoal
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onSelectGoal(minutes) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(18.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("$minutes Minutes / Day", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            when (minutes) {
                                15 -> "Quick daily concept revision"
                                30 -> "Recommended for balanced progress"
                                45 -> "Deep study & practice questions"
                                else -> "Intensive exam preparation"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
