package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizQuestion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    quizzes: List<Quiz>,
    recentAttempts: List<QuizAttempt>,
    onRecordAttempt: (QuizAttempt) -> Unit
) {
    var selectedQuizId by remember { mutableStateOf(quizzes.firstOrNull()?.id ?: "") }
    val currentQuiz = quizzes.find { it.id == selectedQuizId } ?: quizzes.firstOrNull()

    var questionIndex by remember { mutableIntStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var hasAnswered by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var isQuizCompleted by remember { mutableStateOf(false) }

    val currentQuestion = currentQuiz?.questions?.getOrNull(questionIndex)

    fun resetQuiz() {
        questionIndex = 0
        selectedOptionIndex = null
        hasAnswered = false
        score = 0
        isQuizCompleted = false
    }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Practice Quizzes & Assessments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quizzes) { quiz ->
                            val isSelected = quiz.id == currentQuiz?.id
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedQuizId = quiz.id
                                    resetQuiz()
                                },
                                label = { Text(quiz.title.take(24) + "...") },
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (currentQuiz == null || currentQuestion == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No quizzes available.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!isQuizCompleted) {
                    // Question Progress Bar
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Question ${questionIndex + 1} of ${currentQuiz.questions.size}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Score: $score",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { (questionIndex + 1).toFloat() / currentQuiz.questions.size },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                )
                            }
                        }
                    }

                    // Question Card
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    text = currentQuestion.question,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 24.sp
                                )

                                // Options List
                                currentQuestion.options.forEachIndexed { idx, opt ->
                                    val isSelected = selectedOptionIndex == idx
                                    val isCorrect = idx == currentQuestion.correctIndex

                                    val bgColor = when {
                                        !hasAnswered && isSelected -> MaterialTheme.colorScheme.primaryContainer
                                        hasAnswered && isCorrect -> Color(0xFFD8F3DC) // Green
                                        hasAnswered && isSelected && !isCorrect -> Color(0xFFFFD6D6) // Red
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    }

                                    val textColor = when {
                                        hasAnswered && isCorrect -> Color(0xFF087F5B)
                                        hasAnswered && isSelected && !isCorrect -> Color(0xFFC92A2A)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = bgColor,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = !hasAnswered) {
                                                selectedOptionIndex = idx
                                                hasAnswered = true
                                                if (idx == currentQuestion.correctIndex) {
                                                    score++
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.surface),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(('A' + idx).toString(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                            Text(
                                                text = opt,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = textColor,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (hasAnswered && isCorrect) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF087F5B))
                                            }
                                            if (hasAnswered && isSelected && !isCorrect) {
                                                Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFC92A2A))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Explanation Card after answering
                    if (hasAnswered) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedOptionIndex == currentQuestion.correctIndex)
                                        Color(0xFFE6F4EA)
                                    else
                                        Color(0xFFFCE8E6)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = if (selectedOptionIndex == currentQuestion.correctIndex) "✅ Correct! Great job." else "❌ Incorrect. Here's why:",
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedOptionIndex == currentQuestion.correctIndex) Color(0xFF137333) else Color(0xFFC5221F)
                                    )
                                    Text(
                                        text = currentQuestion.explanation,
                                        style = MaterialTheme.typography.bodyMedium,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }

                        item {
                            Button(
                                onClick = {
                                    if (questionIndex < currentQuiz.questions.size - 1) {
                                        questionIndex++
                                        selectedOptionIndex = null
                                        hasAnswered = false
                                    } else {
                                        isQuizCompleted = true
                                        onRecordAttempt(
                                            QuizAttempt(
                                                quizId = currentQuiz.id,
                                                quizTitle = currentQuiz.title,
                                                score = score,
                                                totalQuestions = currentQuiz.questions.size,
                                                xpEarned = score * 20
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    if (questionIndex < currentQuiz.questions.size - 1) "Next Question →" else "Finish Quiz & Claim XP 🎉",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // Quiz Completed Celebration Card
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("🏆", fontSize = 54.sp)
                                Text("Quiz Completed!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    "You scored $score out of ${currentQuiz.questions.size} questions correctly.",
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        "+${score * 20} XP Earned!",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }

                                Button(
                                    onClick = { resetQuiz() },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("Retake Quiz", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Recent Quiz History
                if (recentAttempts.isNotEmpty()) {
                    item {
                        Text("Recent Quiz Attempts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    items(recentAttempts.take(3)) { attempt ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(attempt.quizTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text("Score: ${attempt.score} / ${attempt.totalQuestions}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text("+${attempt.xpEarned} XP", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp)
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
    }
}
