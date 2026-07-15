package com.example.fittrack.repository

import com.example.fittrack.model.ExerciseEntry
import com.example.fittrack.model.WorkoutPlan
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repositório dos planos de treino. Gere planos pessoais, planos da comunidade, importação e gravação de planos gerados.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class WorkoutPlanRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val plansCollection = db.collection("plans")

    suspend fun getMyPlans(): Result<List<WorkoutPlan>> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            val snapshot = plansCollection
                .whereEqualTo("owner_uid", uid)
                .get()
                .await()
            val plans = snapshot.toObjects(WorkoutPlan::class.java)
                .sortedByDescending { it.updatedAt }
            Result.success(plans)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCommunityPlans(): Result<List<WorkoutPlan>> {
        return try {
            val snapshot = plansCollection
                .whereEqualTo("is_public", true)
                .limit(50)
                .get()
                .await()
            val plans = snapshot.toObjects(WorkoutPlan::class.java)
            val activeOwnerIds = plans.map { it.ownerUid }.filter { it.isNotBlank() }.distinct().filter { ownerId ->
                db.collection("users").document(ownerId).get().await().exists()
            }.toSet()
            Result.success(plans.filter { it.ownerUid in activeOwnerIds }.sortedByDescending { it.importCount })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlan(planId: String): Result<WorkoutPlan> {
        return try {
            val snapshot = plansCollection.document(planId).get().await()
            val plan = snapshot.toObject(WorkoutPlan::class.java)
                ?: return Result.failure(Exception("Plan not found"))
            Result.success(plan)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun savePlan(plan: WorkoutPlan, entries: List<ExerciseEntry>): Result<String> {
        return try {
            val docRef = if (plan.id.isNotEmpty()) {
                plansCollection.document(plan.id)
            } else {
                plansCollection.document()
            }

            val uid = auth.currentUser?.uid ?: ""
            val userSnapshot = if (uid.isNotBlank()) db.collection("users").document(uid).get().await() else null
            val ownerName = userSnapshot?.getString("display_name") ?: auth.currentUser?.displayName.orEmpty()
            val ownerUsername = userSnapshot?.getString("username") ?: ""
            val planToSave = plan.copy(
                id = docRef.id,
                exerciseCount = entries.size,
                ownerUid = uid,
                ownerName = ownerName,
                ownerUsername = ownerUsername,
                isPublic = if (plan.sourcePlanId.isNotBlank()) false else plan.isPublic,
                exerciseNames = entries.map { it.exerciseName }.filter { it.isNotBlank() }.distinct()
            )
            docRef.set(planToSave).await()

            // Clear existing entries and save new ones
            val existingEntries = docRef.collection("exercise_entries").get().await()
            for (doc in existingEntries.documents) {
                doc.reference.delete().await()
            }
            for ((index, entry) in entries.withIndex()) {
                val entryToSave = entry.copy(order = index)
                docRef.collection("exercise_entries").add(entryToSave).await()
            }

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getExerciseEntries(planId: String): Result<List<ExerciseEntry>> {
        return try {
            val snapshot = plansCollection.document(planId)
                .collection("exercise_entries")
                .orderBy("order")
                .get()
                .await()
            val entries = snapshot.toObjects(ExerciseEntry::class.java)
            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePlan(planId: String): Result<Unit> {
        return try {
            // Delete exercise entries sub-collection first
            val entries = plansCollection.document(planId)
                .collection("exercise_entries").get().await()
            for (doc in entries.documents) {
                doc.reference.delete().await()
            }
            plansCollection.document(planId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePlansOwnedBy(uid: String): Result<Unit> = try {
        val owned = plansCollection.whereEqualTo("owner_uid", uid).get().await()
        for (doc in owned.documents) deletePlan(doc.id).getOrThrow()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun importPlan(planId: String): Result<String> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            val originalPlan = getPlan(planId).getOrThrow()
            val originalEntries = getExerciseEntries(planId).getOrThrow()

            val userSnapshot = db.collection("users").document(uid).get().await()
            val importedPlan = originalPlan.copy(
                id = "",
                ownerUid = uid,
                ownerName = userSnapshot.getString("display_name") ?: auth.currentUser?.displayName.orEmpty(),
                ownerUsername = userSnapshot.getString("username") ?: "",
                sourcePlanId = originalPlan.id,
                sourceOwnerName = originalPlan.ownerName,
                sourceOwnerUsername = originalPlan.ownerUsername,
                isPublic = false,
                importCount = 0
            )
            val newPlanId = savePlan(importedPlan, originalEntries).getOrThrow()

            // Increment import count on original plan
            plansCollection.document(planId)
                .update("import_count", com.google.firebase.firestore.FieldValue.increment(1))
                .await()

            Result.success(newPlanId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
