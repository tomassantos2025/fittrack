package com.example.fittrack

import android.app.Application
import com.example.fittrack.utils.AppearanceHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

/**
 * Classe Application global. É executada quando a app arranca e centraliza inicializações que devem existir antes dos ecrãs serem abertos.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class FitTrackApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        AppearanceHelper.applySaved(this)

        // Initialize Firebase (safe even without google-services.json — will just log a warning)
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            // Firebase not configured yet — app will still run
            e.printStackTrace()
        }

        // Configure Remote Config with defaults
        try {
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600) // 1 hour in production
                .build()
            remoteConfig.setConfigSettingsAsync(configSettings)
            remoteConfig.setDefaultsAsync(mapOf(
                "is_pro_user" to false,
                "history_days_limit" to 7L,
                "ai_suggestions_enabled" to false
            ))
            remoteConfig.fetchAndActivate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
