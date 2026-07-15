package com.example.fittrack.model

import com.google.firebase.firestore.PropertyName
import java.util.Date

/**
 * Modelo de dados das métricas físicas privadas do utilizador, guardadas no documento privado do perfil.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
data class BodyMetrics(
    val age: Int = 0,
    @get:PropertyName("height_cm") @set:PropertyName("height_cm") var heightCm: Double = 0.0,
    @get:PropertyName("current_weight_kg") @set:PropertyName("current_weight_kg") var currentWeightKg: Double = 0.0,
    @get:PropertyName("goal_weight_kg") @set:PropertyName("goal_weight_kg") var goalWeightKg: Double = 0.0,
    @get:PropertyName("updated_at") @set:PropertyName("updated_at") var updatedAt: Date? = Date()
) {
    fun bmi(): Double? {
        val heightM = heightCm / 100.0
        return if (heightM > 0 && currentWeightKg > 0) {
            currentWeightKg / (heightM * heightM)
        } else {
            null
        }
    }
}
