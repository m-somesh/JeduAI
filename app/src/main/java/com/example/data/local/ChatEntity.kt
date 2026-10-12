package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.MessageSender
import com.example.data.model.TutorMode

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subject: String,
    val lastMessage: String,
    val updatedAt: Long
) {
    fun toDomain(messages: List<ChatMessage> = emptyList()): ChatSession {
        return ChatSession(
            id = id,
            title = title,
            subject = subject,
            lastMessage = lastMessage,
            updatedAt = updatedAt,
            messages = messages
        )
    }

    companion object {
        fun fromDomain(session: ChatSession): ChatSessionEntity {
            return ChatSessionEntity(
                id = session.id,
                title = session.title,
                subject = session.subject,
                lastMessage = session.lastMessage,
                updatedAt = session.updatedAt
            )
        }
    }
}

@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["sessionId"])]
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val sender: String, // "USER" or "AI"
    val text: String,
    val timestamp: Long,
    val mode: String,
    val languageCode: String,
    val isBookmarked: Boolean,
    val userRating: Int
) {
    fun toDomain(): ChatMessage {
        val senderEnum = try {
            MessageSender.valueOf(sender)
        } catch (_: Exception) {
            MessageSender.USER
        }
        val modeEnum = try {
            TutorMode.valueOf(mode)
        } catch (_: Exception) {
            TutorMode.BEGINNER
        }
        return ChatMessage(
            id = id,
            sender = senderEnum,
            text = text,
            timestamp = timestamp,
            mode = modeEnum,
            languageCode = languageCode,
            isBookmarked = isBookmarked,
            userRating = userRating
        )
    }

    companion object {
        fun fromDomain(sessionId: String, msg: ChatMessage): ChatMessageEntity {
            return ChatMessageEntity(
                id = msg.id,
                sessionId = sessionId,
                sender = msg.sender.name,
                text = msg.text,
                timestamp = msg.timestamp,
                mode = msg.mode.name,
                languageCode = msg.languageCode,
                isBookmarked = msg.isBookmarked,
                userRating = msg.userRating
            )
        }
    }
}
