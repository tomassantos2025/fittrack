package com.example.fittrack.utils

import android.view.View
import android.view.animation.OvershootInterpolator

/**
 * Utilitário com animações pequenas da interface para tornar a experiência mais fluida.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
object UiAnimations {
    fun reveal(view: View, delay: Long = 0L, duration: Long = 260L, distanceDp: Float = 14f) {
        val density = view.resources.displayMetrics.density
        view.animate().cancel()
        view.alpha = 0f
        view.translationY = distanceDp * density
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(delay)
            .setDuration(duration)
            .setInterpolator(OvershootInterpolator(0.7f))
            .start()
    }

    fun selectNavItem(view: View, selected: Boolean) {
        view.animate().cancel()
        view.animate()
            .scaleX(if (selected) 1.06f else 1f)
            .scaleY(if (selected) 1.06f else 1f)
            .translationY(if (selected) -3f * view.resources.displayMetrics.density else 0f)
            .setDuration(180L)
            .start()
    }

    fun pulse(view: View) {
        view.animate().cancel()
        view.scaleX = 0.8f
        view.scaleY = 0.8f
        view.animate().scaleX(1f).scaleY(1f).setDuration(220L).setInterpolator(OvershootInterpolator()).start()
    }
}
