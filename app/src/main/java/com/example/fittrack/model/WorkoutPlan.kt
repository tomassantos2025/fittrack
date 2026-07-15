package com.example.fittrack.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Modelo de um plano de treino. Pode ser privado do utilizador ou público para aparecer na comunidade.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
data class WorkoutPlan(
    @DocumentId
    val id: String = "",

    val name: String = "",
    val description: String = "",

    @get:PropertyName("owner_uid")
    @set:PropertyName("owner_uid")
    var ownerUid: String = "",

    @get:PropertyName("owner_name")
    @set:PropertyName("owner_name")
    var ownerName: String = "",

    @get:PropertyName("owner_username")
    @set:PropertyName("owner_username")
    var ownerUsername: String = "",

    @get:PropertyName("source_plan_id")
    @set:PropertyName("source_plan_id")
    var sourcePlanId: String = "",

    @get:PropertyName("source_owner_name")
    @set:PropertyName("source_owner_name")
    var sourceOwnerName: String = "",

    @get:PropertyName("source_owner_username")
    @set:PropertyName("source_owner_username")
    var sourceOwnerUsername: String = "",

    @get:PropertyName("exercise_names")
    @set:PropertyName("exercise_names")
    var exerciseNames: List<String> = emptyList(),

    @get:PropertyName("muscle_groups")
    @set:PropertyName("muscle_groups")
    var muscleGroups: List<String> = emptyList(),

    @get:PropertyName("exercise_count")
    @set:PropertyName("exercise_count")
    var exerciseCount: Int = 0,

    @get:PropertyName("is_public")
    @set:PropertyName("is_public")
    var isPublic: Boolean = false,

    @get:PropertyName("import_count")
    @set:PropertyName("import_count")
    var importCount: Int = 0,

    @ServerTimestamp
    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Date? = null,

    @ServerTimestamp
    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    var updatedAt: Date? = null
)
