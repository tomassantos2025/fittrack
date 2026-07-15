package com.example.fittrack.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * A single set log within a session exercise.
 * Stored as a sub-collection: users/{uid}/sessions/{sessionId}/set_logs/{logId}
 */
data class SetLog(
    @DocumentId
    val id: String = "",

    @get:PropertyName("exercise_id")
    @set:PropertyName("exercise_id")
    var exerciseId: String = "",

    @get:PropertyName("exercise_name")
    @set:PropertyName("exercise_name")
    var exerciseName: String = "",

    @get:PropertyName("set_number")
    @set:PropertyName("set_number")
    var setNumber: Int = 1,

    var weight: Double = 0.0,

    var reps: Int = 0,

    @get:PropertyName("is_completed")
    @set:PropertyName("is_completed")
    var isCompleted: Boolean = false,

    @get:PropertyName("is_personal_record")
    @set:PropertyName("is_personal_record")
    var isPersonalRecord: Boolean = false,

    @get:PropertyName("exercise_order")
    @set:PropertyName("exercise_order")
    var exerciseOrder: Int = 0
)
