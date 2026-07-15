package com.example.fittrack.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * Utilitário de aparência. Aplica opções visuais, como tema claro/escuro, de forma centralizada.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
object AppearanceHelper {
    const val MODE_SYSTEM = "system"
    const val MODE_DARK = "dark"
    const val MODE_LIGHT = "light"
    const val MODE_AMOLED = "amoled"
    private const val PREFS = "appearance_prefs"
    private const val KEY_MODE = "appearance_mode"

    fun getMode(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_MODE, MODE_DARK) ?: MODE_DARK

    fun setMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_MODE, mode).apply()
        apply(mode)
    }

    fun applySaved(context: Context) = apply(getMode(context))

    private fun apply(mode: String) {
        AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                MODE_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                else -> AppCompatDelegate.MODE_NIGHT_YES
            }
        )
    }
}
