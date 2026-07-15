package com.example.fittrack.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Modelo do perfil do utilizador. Inclui dados de conta, preferências e a flag isPro que controla as funcionalidades Premium.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
data class User(
    @DocumentId
    val uid: String = "",

    @get:PropertyName("display_name")
    @set:PropertyName("display_name")
    var displayName: String = "",

    val username: String = "",

    val email: String = "",

    @get:PropertyName("friend_code")
    @set:PropertyName("friend_code")
    var friendCode: String = "",

    @get:PropertyName("photo_url")
    @set:PropertyName("photo_url")
    var photoUrl: String = "",

    @get:PropertyName("is_pro")
    @set:PropertyName("is_pro")
    var isPro: Boolean = false,

    val language: String = "en",

    @get:PropertyName("show_online")
    @set:PropertyName("show_online")
    var showOnline: Boolean = true,

    @get:PropertyName("read_receipts")
    @set:PropertyName("read_receipts")
    var readReceipts: Boolean = true,

    @get:PropertyName("is_online")
    @set:PropertyName("is_online")
    var isOnline: Boolean = false,

    @get:PropertyName("total_sessions")
    @set:PropertyName("total_sessions")
    var totalSessions: Int = 0,

    @get:PropertyName("current_streak")
    @set:PropertyName("current_streak")
    var currentStreak: Int = 0,

    @get:PropertyName("active_day_keys")
    @set:PropertyName("active_day_keys")
    var activeDayKeys: List<String> = emptyList(),


    @get:PropertyName("last_workout_date")
    @set:PropertyName("last_workout_date")
    var lastWorkoutDate: Date? = null,

    @ServerTimestamp
    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Date? = null
)
