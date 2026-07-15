package com.example.fittrack.repository

import android.net.Uri
import com.example.fittrack.model.User
import com.example.fittrack.model.BodyMetrics
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Repositório do perfil do utilizador. Centraliza leitura/escrita do documento users/{uid} e das definições privadas.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class UserRepository {

    private val socialRepository by lazy { SocialRepository() }
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    suspend fun getUser(uid: String): Result<User> {
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            val user = snapshot.toObject(User::class.java)
                ?: return Result.failure(Exception("User document not found"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Repara contas que existem no Firebase Authentication mas ainda não têm
     * um documento de perfil correspondente no Firestore. Isto pode acontecer com utilizadores antigos de teste
     * ou contas criadas antes de existir a etapa de criação do perfil.
     */
    suspend fun getOrCreateUser(firebaseUser: FirebaseUser, preferredLanguage: String = "en"): Result<User> {
        return try {
            val ref = usersCollection.document(firebaseUser.uid)
            val snapshot = ref.get().await()
            val existing = snapshot.toObject(User::class.java)

            val email = firebaseUser.email.orEmpty()
            val fallbackName = firebaseUser.displayName
                ?.takeIf { it.isNotBlank() }
                ?: email.substringBefore("@").ifBlank { "FitTrack User" }
            val fallbackUsername = email.substringBefore("@")
                .replace(Regex("[^\\p{L}\\p{N}._-]"), "")
                .take(20)
                .ifBlank { "user${firebaseUser.uid.take(6)}" }

            if (existing != null) {
                val repaired = existing.copy(
                    displayName = existing.displayName.ifBlank { fallbackName },
                    username = existing.username.ifBlank { fallbackUsername },
                    email = existing.email.ifBlank { email },
                    language = existing.language.ifBlank { preferredLanguage }
                )
                if (repaired != existing) ref.set(repaired, SetOptions.merge()).await()
                return socialRepository.ensureFriendCode(repaired)
            }

            val newUser = User(
                uid = firebaseUser.uid,
                displayName = fallbackName,
                username = fallbackUsername,
                email = email,
                language = preferredLanguage,
                friendCode = socialRepository.makeFriendCode(firebaseUser.uid)
            )
            ref.set(newUser).await()
            Result.success(newUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createUserDocument(uid: String, user: User): Result<Unit> {
        return try {
            val completedUser = if (user.friendCode.isBlank()) user.copy(friendCode = socialRepository.makeFriendCode(uid)) else user
            usersCollection.document(uid).set(completedUser).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * É usado set com merge em vez de update para que um perfil antigo ou parcialmente
     * incompleto possa ser recuperado sem bloquear a interface.
     */
    suspend fun updateUser(uid: String, fields: Map<String, Any>): Result<Unit> {
        return try {
            usersCollection.document(uid).set(fields, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * O upload remoto de fotografia exigiria Firebase Storage. A app atual guarda um URI local
     * como alternativa simples, por isso o upload remoto fica desativado nesta versão.
     */
    suspend fun uploadProfilePhoto(uid: String, imageUri: Uri): Result<String> {
        return Result.failure(Exception("Photo upload not available"))
    }

    /** Lê a flag de subscrição por utilizador, usada pelo Smart Planner e pelo AI Coach. */
    suspend fun isCurrentUserPro(uid: String): Boolean {
        return getUser(uid).getOrNull()?.isPro == true
    }

    /** Escreve a flag de subscrição no Firestore. */
    suspend fun setProStatus(uid: String, isPro: Boolean): Result<Unit> =
        updateUser(uid, mapOf("is_pro" to isPro))

    suspend fun updateLanguage(uid: String, language: String): Result<Unit> =
        updateUser(uid, mapOf("language" to language))

    suspend fun updateAccount(uid: String, displayName: String, username: String, email: String): Result<Unit> {
        return updateUser(uid, mapOf(
            "display_name" to displayName,
            "username" to username,
            "email" to email
        ))
    }

    suspend fun getBodyMetrics(uid: String): Result<BodyMetrics> {
        return try {
            val snapshot = usersCollection.document(uid).collection("private").document("body_metrics").get().await()
            Result.success(snapshot.toObject(BodyMetrics::class.java) ?: BodyMetrics())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBodyMetrics(uid: String, age: Int, heightCm: Double, currentWeightKg: Double, goalWeightKg: Double): Result<Unit> {
        return try {
            usersCollection.document(uid).collection("private").document("body_metrics")
                .set(BodyMetrics(age, heightCm, currentWeightKg, goalWeightKg)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePrivacy(uid: String, showOnline: Boolean, readReceipts: Boolean): Result<Unit> {
        return updateUser(uid, mapOf(
            "show_online" to showOnline,
            "read_receipts" to readReceipts
        ))
    }

    suspend fun setOnlineStatus(uid: String, isOnline: Boolean): Result<Unit> =
        updateUser(uid, mapOf("is_online" to isOnline))

    suspend fun markTodayActive(uid: String): Result<ActivityStats> {
        return try {
            val ref = usersCollection.document(uid)
            val snapshot = ref.get().await()
            val existing = (snapshot.get("active_day_keys") as? List<*>)?.filterIsInstance<String>().orEmpty().toMutableSet()
            val today = java.time.LocalDate.now(java.time.ZoneId.systemDefault())
            existing.add(today.toString())
            val retained = existing.mapNotNull { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
                .filter { !it.isBefore(today.minusDays(400)) }
                .distinct().sorted()
            val weekStart = today.with(java.time.DayOfWeek.MONDAY)
            val activeThisWeek = retained.count { !it.isBefore(weekStart) && !it.isAfter(today) }
            var streak = 0
            var cursor = today
            val set = retained.toSet()
            while (cursor in set) { streak++; cursor = cursor.minusDays(1) }
            ref.set(mapOf("active_day_keys" to retained.map { it.toString() }, "current_streak" to streak), SetOptions.merge()).await()
            Result.success(ActivityStats(activeThisWeek, streak))
        } catch (e: Exception) { Result.failure(e) }
    }

    data class ActivityStats(val activeDaysThisWeek: Int, val currentStreak: Int)

    suspend fun deleteUserDocument(uid: String): Result<Unit> {
        return try {
            usersCollection.document(uid).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
