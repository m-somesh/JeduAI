package com.example.ui.screens

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*

@Composable
fun HomeScreen(
    profile: UserProfile,
    recentDocuments: List<DocumentBook>,
    recentChats: List<ChatSession>,
    recentVideos: List<VideoLecture>,
    onNavigateToTutor: () -> Unit,
    onNavigateToTranslate: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToVideos: () -> Unit,
    onNavigateToQuizzes: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToMindMaps: () -> Unit = {},
    onNavigateToOffline: () -> Unit = {}
) {
    var isConceptInHindi by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top Header: Greeting & Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Welcome back,",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${profile.name} 👋",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Streak Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFECE5),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🔥", fontSize = 16.sp)
                        Text(
                            "${profile.currentStreak}d",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE85D04),
                            fontSize = 14.sp
                        )
                    }
                }

                // XP Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("⚡", fontSize = 16.sp)
                        Text(
                            "${profile.xpPoints}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Daily Goal Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text("Daily Learning Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text("20 / ${profile.dailyGoalMinutes} mins", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }

                LinearProgressIndicator(
                    progress = { 20f / profile.dailyGoalMinutes.coerceAtLeast(1) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Text(
                    "You're only 10 minutes away from completing your daily goal!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Learning Hub
        Text("Learning Hub", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // AI Mind Maps Action Card
            HubCard(
                title = "AI Mind Maps",
                subtitle = "Notes-to-Graph Generator",
                emoji = "🗺️",
                gradient = Brush.linearGradient(listOf(Color(0xFF6750A4), Color(0xFF9C27B0))),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToMindMaps
            )

            // Offline Learning Library
            HubCard(
                title = "Offline Hub",
                subtitle = "Study without Internet",
                emoji = "⚡",
                gradient = Brush.linearGradient(listOf(Color(0xFF006D44), Color(0xFF2E7D32))),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToOffline
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // AI Tutor Action Card
            HubCard(
                title = "AI Tutor",
                subtitle = "Ask any doubt in ${profile.explanationLanguage.name}",
                emoji = "🤖",
                gradient = Brush.linearGradient(listOf(Color(0xFF5B5FEF), Color(0xFF773DE8))),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToTutor
            )

            // Multilingual Translator Card
            HubCard(
                title = "Translate",
                subtitle = "Academic & Concept Mode",
                emoji = "🌐",
                gradient = Brush.linearGradient(listOf(Color(0xFF39C6F4), Color(0xFF20B486))),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToTranslate
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Document Reader
            HubCard(
                title = "E-Books",
                subtitle = "Bilingual Study Viewer",
                emoji = "📖",
                gradient = Brush.linearGradient(listOf(Color(0xFFF5A623), Color(0xFFE85D75))),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToDocuments
            )

            // Video Hub
            HubCard(
                title = "Video Hub",
                subtitle = "Lectures & Subtitles",
                emoji = "🎥",
                gradient = Brush.linearGradient(listOf(Color(0xFF773DE8), Color(0xFF39C6F4))),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToVideos
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Quizzes
            HubCard(
                title = "Quizzes",
                subtitle = "AI Practice Assessments",
                emoji = "🎯",
                gradient = Brush.linearGradient(listOf(Color(0xFF20B486), Color(0xFF5B5FEF))),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToQuizzes
            )

            // Library
            HubCard(
                title = "My Library",
                subtitle = "Saved Notes & Words",
                emoji = "📚",
                gradient = Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToLibrary
            )
        }

        // Daily High-Yield Concept Spotlight
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("💡", fontSize = 18.sp)
                        Text("Concept Spotlight", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    TextButton(onClick = { isConceptInHindi = !isConceptInHindi }) {
                        Text(if (isConceptInHindi) "View in English" else "हिंदी में देखें", fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = if (isConceptInHindi)
                        "ऊर्जा संरक्षण का नियम (Conservation of Energy): ऊर्जा न तो उत्पन्न की जा सकती है और न ही नष्ट की जा सकती है; यह केवल एक रूप से दूसरे रूप में परिवर्तित होती है।"
                    else
                        "Law of Conservation of Energy: Energy cannot be created or destroyed; it can only be transformed from one form to another (e.g. potential energy into kinetic energy).",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            }
        }

        // Continue Learning
        if (recentDocuments.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Continue Reading", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onNavigateToDocuments) {
                    Text("See All")
                }
            }

            recentDocuments.take(2).forEach { doc ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToDocuments() }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📄", fontSize = 24.sp)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(doc.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(doc.subject, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { doc.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                            )
                        }

                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun HubCard(
    title: String,
    subtitle: String,
    emoji: String,
    gradient: Brush,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .height(130.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(emoji, fontSize = 28.sp)
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.85f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
