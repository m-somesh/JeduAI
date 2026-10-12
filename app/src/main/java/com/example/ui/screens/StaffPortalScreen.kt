package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuizQuestion

@Composable
fun StaffPortalScreen(
    onCreateAssessment: (String, String, List<QuizQuestion>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var questionText by remember { mutableStateOf("") }
    var optA by remember { mutableStateOf("") }
    var optB by remember { mutableStateOf("") }
    var optC by remember { mutableStateOf("") }
    var optD by remember { mutableStateOf("") }
    var explanation by remember { mutableStateOf("") }
    var showSuccessMessage by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Staff & Educator Portal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Manage classes, create custom curriculum assessments, and monitor student comprehension.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Analytics Overview Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📊 Class Learning Metrics", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("38", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            Text("Enrolled Students", style = MaterialTheme.typography.bodySmall)
                        }
                        Column {
                            Text("84.5%", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF20B486))
                            Text("Avg. Quiz Score", style = MaterialTheme.typography.bodySmall)
                        }
                        Column {
                            Text("142", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF773DE8))
                            Text("Concepts Explored", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // Assessment Creation Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("✍️ Create Verified Assessment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Quiz Title (e.g. Thermodynamics Weekly Check)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject / Department") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = questionText,
                        onValueChange = { questionText = it },
                        label = { Text("Question Prompt") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text("Options (Option A is designated as correct for this template):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(value = optA, onValueChange = { optA = it }, label = { Text("Option A (Correct Answer)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = optB, onValueChange = { optB = it }, label = { Text("Option B") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = optC, onValueChange = { optC = it }, label = { Text("Option C") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = optD, onValueChange = { optD = it }, label = { Text("Option D") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))

                    OutlinedTextField(
                        value = explanation,
                        onValueChange = { explanation = it },
                        label = { Text("Detailed Step-by-Step Educational Solution") },
                        modifier = Modifier.fillMaxWidth().height(90.dp),
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            if (title.isNotBlank() && questionText.isNotBlank() && optA.isNotBlank()) {
                                val question = QuizQuestion(
                                    question = questionText,
                                    options = listOf(optA, optB.ifBlank { "Option B" }, optC.ifBlank { "Option C" }, optD.ifBlank { "Option D" }),
                                    correctIndex = 0,
                                    explanation = explanation.ifBlank { "Verified correct answer provided by staff." },
                                    conceptTag = subject
                                )
                                onCreateAssessment(title, subject, listOf(question))
                                showSuccessMessage = true
                                title = ""
                                questionText = ""
                                optA = ""
                                optB = ""
                                optC = ""
                                optD = ""
                                explanation = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Publish, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Publish Assessment to Class", fontWeight = FontWeight.Bold)
                    }

                    if (showSuccessMessage) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFD8F3DC),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "✅ Assessment successfully published to enrolled students!",
                                color = Color(0xFF087F5B),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
