package com.example.fittrack.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * A completed workout session.
 * Coleção Firestore: users/{uid}/sessions/{sessionId}. Cada utilizador tem o seu próprio histórico privado.
 */
data class Session(
    @DocumentId
    val id: String = "",

    @get:PropertyName("plan_id")
    @set:PropertyName("plan_id")
    var planId: String = "",

    @get:PropertyName("plan_name")
    @set:PropertyName("plan_name")
    var planName: String = "",

    @get:PropertyName("duration_seconds")
    @set:PropertyName("duration_seconds")
    var durationSeconds: Long = 0,

    @get:PropertyName("total_volume")
    @set:PropertyName("total_volume")
    var totalVolume: Double = 0.0,

    @get:PropertyName("total_sets")
    @set:PropertyName("total_sets")
    var totalSets: Int = 0,

    @get:PropertyName("total_reps")
    @set:PropertyName("total_reps")
    var totalReps: Int = 0,

    @get:PropertyName("exercises_completed")
    @set:PropertyName("exercises_completed")
    var exercisesCompleted: Int = 0,

    @get:PropertyName("muscle_groups")
    @set:PropertyName("muscle_groups")
    var muscleGroups: List<String> = emptyList(),

    @get:PropertyName("personal_records")
    @set:PropertyName("personal_records")
    var personalRecords: Int = 0,

    @get:PropertyName("ai_suggestion")
    @set:PropertyName("ai_suggestion")
    var aiSuggestion: String = "",

    @ServerTimestamp
    @get:PropertyName("completed_at")
    @set:PropertyName("completed_at")
    var completedAt: Date? = null
)
