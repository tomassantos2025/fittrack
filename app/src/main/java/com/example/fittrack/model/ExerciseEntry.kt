package com.example.fittrack.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * An exercise entry within a WorkoutPlan.
 * Stored as a sub-collection: plans/{planId}/exercise_entries/{entryId}
 */
data class ExerciseEntry(
    @DocumentId
    val id: String = "",

    @get:PropertyName("exercise_id")
    @set:PropertyName("exercise_id")
    var exerciseId: String = "",

    @get:PropertyName("exercise_name")
    @set:PropertyName("exercise_name")
    var exerciseName: String = "",

    @get:PropertyName("target_sets")
    @set:PropertyName("target_sets")
    var targetSets: Int = 3,

    @get:PropertyName("target_reps")
    @set:PropertyName("target_reps")
    var targetReps: Int = 10,

    @get:PropertyName("target_weight")
    @set:PropertyName("target_weight")
    var targetWeight: Double = 0.0,

    val order: Int = 0,

    val notes: String = ""
)
