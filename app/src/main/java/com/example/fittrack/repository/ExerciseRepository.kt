package com.example.fittrack.repository

import com.example.fittrack.model.Exercise
import com.example.fittrack.utils.ExerciseSeeder
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repositório do catálogo de exercícios. Lê os exercícios guardados remotamente no Firebase e fornece-os aos ecrãs da app.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class ExerciseRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val exercisesCollection = db.collection("exercises")

    suspend fun getExercises(): Result<List<Exercise>> {
        return try {
            val snapshot = exercisesCollection.orderBy("name").get().await()
            val exercises = snapshot.toObjects(Exercise::class.java)
            if (exercises.isEmpty()) {
                Result.success(ExerciseSeeder.localExercises.sortedBy { it.name })
            } else {
                Result.success(exercises)
            }
        } catch (e: Exception) {
            // Keep the UX usable for demos even without Firebase indexes, internet, or seeded data.
            Result.success(ExerciseSeeder.localExercises.sortedBy { it.name })
        }
    }

    suspend fun searchExercises(query: String): Result<List<Exercise>> {
        return getExercises().map { exercises ->
            val normalized = query.trim()
            if (normalized.isBlank()) {
                exercises
            } else {
                exercises.filter { exercise ->
                    exercise.name.contains(normalized, ignoreCase = true) ||
                        exercise.description.contains(normalized, ignoreCase = true) ||
                        exercise.equipment.contains(normalized, ignoreCase = true) ||
                        exercise.muscleGroups.any { it.contains(normalized, ignoreCase = true) }
                }
            }
        }
    }

    suspend fun getExerciseById(id: String): Result<Exercise> {
        return try {
            val snapshot = exercisesCollection.document(id).get().await()
            val exercise = snapshot.toObject(Exercise::class.java)
                ?: ExerciseSeeder.localExercises.firstOrNull { it.id == id }
                ?: return Result.failure(Exception("Exercise not found"))
            Result.success(exercise)
        } catch (e: Exception) {
            ExerciseSeeder.localExercises.firstOrNull { it.id == id }
                ?.let { Result.success(it) }
                ?: Result.failure(e)
        }
    }

    suspend fun getExercisesByMuscleGroup(muscleGroup: String): Result<List<Exercise>> {
        return getExercises().map { exercises ->
            exercises.filter { exercise ->
                exercise.muscleGroups.any { it.equals(muscleGroup, ignoreCase = true) }
            }
        }
    }
}
