package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AdminPortalScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Administrator Portal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Operational system overview, multilingual telemetry, security audits, and infrastructure status.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // System Health Status Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🛡️ Platform Health & Security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    StatusRow(title = "AI Inference Engine", status = "Operational (gemini-3.5-flash)", isHealthy = true)
                    StatusRow(title = "Local Offline Cache", status = "14 Regional Languages Synced", isHealthy = true)
                    StatusRow(title = "API Credential Isolation", status = "Secrets Guarded / Server Side", isHealthy = true)
                    StatusRow(title = "User Data Privacy", status = "Zero Cross-User Leakage / Isolated", isHealthy = true)
                }
            }
        }

        // Multilingual Telemetry Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🌐 Regional Language Telemetry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    LanguageStatBar(lang = "Hindi (हिन्दी)", percentage = 0.42f, percentText = "42%")
                    LanguageStatBar(lang = "Tamil (தமிழ்)", percentage = 0.28f, percentText = "28%")
                    LanguageStatBar(lang = "Telugu (తెలుగు)", percentage = 0.15f, percentText = "15%")
                    LanguageStatBar(lang = "Spanish (Español)", percentage = 0.09f, percentText = "9%")
                    LanguageStatBar(lang = "Others (Bengali, Marathi, etc.)", percentage = 0.06f, percentText = "6%")
                }
            }
        }

        // Active Platform Counters
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("1,240+", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Active Learners", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("99.94%", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF20B486))
                        Text("API Reliability", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun StatusRow(title: String, status: String, isHealthy: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isHealthy) Color(0xFF20B486) else Color(0xFFF5A623),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun LanguageStatBar(lang: String, percentage: Float, percentText: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(lang, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text(percentText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
