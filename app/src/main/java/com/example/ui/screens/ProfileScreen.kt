package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.LanguageSelectorDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profile: UserProfile,
    onUpdateProfile: (UserProfile) -> Unit,
    onReplayOnboarding: () -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var email by remember { mutableStateOf(profile.email) }
    var selectedRole by remember { mutableStateOf(profile.role) }
    var selectedEduLevel by remember { mutableStateOf(profile.educationLevel) }
    var selectedUiLang by remember { mutableStateOf(profile.interfaceLanguage) }
    var selectedExplainLang by remember { mutableStateOf(profile.explanationLanguage) }
    var dailyGoal by remember { mutableFloatStateOf(profile.dailyGoalMinutes.toFloat()) }
    var isDarkMode by remember { mutableStateOf(profile.isDarkMode) }

    var showUiLangPicker by remember { mutableStateOf(false) }
    var showExplainLangPicker by remember { mutableStateOf(false) }
    var showEduDialog by remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf(false) }

    if (showUiLangPicker) {
        LanguageSelectorDialog(
            title = "App Interface Language",
            selectedLanguage = selectedUiLang,
            onLanguageSelected = {
                selectedUiLang = it
                onUpdateProfile(profile.copy(interfaceLanguage = it))
            },
            onDismissRequest = { showUiLangPicker = false }
        )
    }

    if (showExplainLangPicker) {
        LanguageSelectorDialog(
            title = "Tutor Explanation Language",
            selectedLanguage = selectedExplainLang,
            onLanguageSelected = {
                selectedExplainLang = it
                onUpdateProfile(profile.copy(explanationLanguage = it))
            },
            onDismissRequest = { showExplainLangPicker = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Profile Header
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(18.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.name.take(1),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(profile.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(profile.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "ROLE: ${profile.role.name}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Role Switcher Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Portal Perspective (Switch Role)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Experience JeduAI as a student, educator, or platform administrator.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UserRole.entries.forEach { role ->
                            val isSelected = role == profile.role
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedRole = role
                                    onUpdateProfile(profile.copy(role = role))
                                },
                                label = { Text(role.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Language & Learning Preferences Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Language & Education Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    // Interface Lang Row
                    SettingRow(
                        title = "App Interface Language",
                        value = "${profile.interfaceLanguage.flag} ${profile.interfaceLanguage.name}",
                        onClick = { showUiLangPicker = true }
                    )

                    HorizontalDivider()

                    // Explanation Lang Row
                    SettingRow(
                        title = "Tutor Explanation Language",
                        value = "${profile.explanationLanguage.flag} ${profile.explanationLanguage.displayName}",
                        onClick = { showExplainLangPicker = true }
                    )

                    HorizontalDivider()

                    // Education Level
                    SettingRow(
                        title = "Education Level",
                        value = profile.educationLevel.label,
                        onClick = {
                            val next = when (profile.educationLevel) {
                                EducationLevel.MIDDLE_SCHOOL -> EducationLevel.HIGH_SCHOOL
                                EducationLevel.HIGH_SCHOOL -> EducationLevel.COLLEGE_UNDERGRAD
                                EducationLevel.COLLEGE_UNDERGRAD -> EducationLevel.POSTGRADUATE
                                EducationLevel.POSTGRADUATE -> EducationLevel.COMPETITIVE_EXAMS
                                EducationLevel.COMPETITIVE_EXAMS -> EducationLevel.MIDDLE_SCHOOL
                            }
                            onUpdateProfile(profile.copy(educationLevel = next))
                        }
                    )
                }
            }
        }

        // Study Target & Theme Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Personal Goals & Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Text("Daily Target: ${dailyGoal.toInt()} minutes / day", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Slider(
                        value = dailyGoal,
                        onValueChange = {
                            dailyGoal = it
                            onUpdateProfile(profile.copy(dailyGoalMinutes = it.toInt()))
                        },
                        valueRange = 15f..60f,
                        steps = 2
                    )

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dark Theme", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = {
                                isDarkMode = it
                                onUpdateProfile(profile.copy(isDarkMode = it))
                            }
                        )
                    }
                }
            }
        }

        // Onboarding Replay / Reset
        item {
            OutlinedButton(
                onClick = onReplayOnboarding,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Revisit Welcome & Onboarding Guide")
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SettingRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
