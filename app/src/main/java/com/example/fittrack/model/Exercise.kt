package com.example.fittrack.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Exercício do catálogo global guardado na coleção Firestore: exercises.
 */
data class Exercise(
    @DocumentId
    val id: String = "",

    val name: String = "",
    val description: String = "",

    @get:PropertyName("muscle_groups")
    @set:PropertyName("muscle_groups")
    var muscleGroups: List<String> = emptyList(),

    val equipment: String = "",

    @get:PropertyName("animation_url")
    @set:PropertyName("animation_url")
    var animationUrl: String = "",

    val difficulty: String = "intermediate", // beginner, intermediate, advanced

    @get:PropertyName("icon_emoji")
    @set:PropertyName("icon_emoji")
    var iconEmoji: String = "🏋️",

    @get:PropertyName("how_to")
    @set:PropertyName("how_to")
    var howTo: List<String> = emptyList(),

    val tips: List<String> = emptyList(),

    @get:PropertyName("common_mistakes")
    @set:PropertyName("common_mistakes")
    var commonMistakes: List<String> = emptyList(),

    @get:PropertyName("home_friendly")
    @set:PropertyName("home_friendly")
    var homeFriendly: Boolean = false
)
