package com.example.fittrack.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Modelo de uma mensagem de chat. É usado nas conversas de grupo da área social.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
data class ChatMessage(
    @DocumentId
    val id: String = "",

    @get:PropertyName("sender_uid")
    @set:PropertyName("sender_uid")
    var senderUid: String = "",

    @get:PropertyName("sender_name")
    @set:PropertyName("sender_name")
    var senderName: String = "",

    val text: String = "",

    @get:PropertyName("read_by")
    @set:PropertyName("read_by")
    var readBy: List<String> = emptyList(),

    @ServerTimestamp
    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Date? = null
)
