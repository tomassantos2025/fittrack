package com.example.fittrack.repository

import com.google.firebase.remoteconfig.FirebaseRemoteConfig

/**
 * Repositório para configurações remotas globais. Não decide se um utilizador é Pro; essa decisão vem do perfil no Firestore.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class RemoteConfigRepository {

    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    fun isProUser(): Boolean {
        return remoteConfig.getBoolean("is_pro_user")
    }

    fun historyDaysLimit(): Int {
        return remoteConfig.getLong("history_days_limit").toInt()
    }

    fun isAiSuggestionsEnabled(): Boolean {
        return remoteConfig.getBoolean("ai_suggestions_enabled")
    }

    suspend fun fetchAndActivate(): Result<Boolean> {
        return try {
            val result = remoteConfig.fetchAndActivate()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
