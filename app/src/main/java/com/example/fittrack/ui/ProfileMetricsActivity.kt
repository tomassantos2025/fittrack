package com.example.fittrack.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.fittrack.R
import com.example.fittrack.databinding.ActivityProfileMetricsBinding
import com.example.fittrack.repository.AuthRepository
import com.example.fittrack.repository.UserRepository
import com.example.fittrack.utils.SystemBars
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Ecrã para editar métricas físicas do utilizador, como peso e altura, usadas para personalizar a experiência.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class ProfileMetricsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileMetricsBinding
    private val users = UserRepository()
    private val auth = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileMetricsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SystemBars.applyTopInset(binding.metricsToolbar)
        binding.btnBack.setOnClickListener { finish() }
        listOf(binding.etHeight, binding.etCurrentWeight, binding.etGoalWeight).forEach { it.doAfterTextChanged { updateBmiPreview() } }
        binding.btnSave.setOnClickListener { save() }
        load()
    }

    private fun load() = lifecycleScope.launch {
        val uid = auth.currentUser()?.uid ?: return@launch
        users.getBodyMetrics(uid).onSuccess { metrics ->
            if (metrics.age > 0) binding.etAge.setText(metrics.age.toString())
            if (metrics.heightCm > 0) binding.etHeight.setText(metrics.heightCm.toString())
            if (metrics.currentWeightKg > 0) binding.etCurrentWeight.setText(metrics.currentWeightKg.toString())
            if (metrics.goalWeightKg > 0) binding.etGoalWeight.setText(metrics.goalWeightKg.toString())
            updateBmiPreview()
        }
    }

    private fun save() = lifecycleScope.launch {
        val uid = auth.currentUser()?.uid ?: return@launch
        val age = binding.etAge.text.toString().toIntOrNull() ?: 0
        val height = number(binding.etHeight.text.toString())
        val current = number(binding.etCurrentWeight.text.toString())
        val goal = number(binding.etGoalWeight.text.toString())
        if (age !in 1..120 || height <= 0 || current <= 0 || goal <= 0) {
            show(getString(R.string.profile_metrics_invalid)); return@launch
        }
        users.updateBodyMetrics(uid, age, height, current, goal).onSuccess { show(getString(R.string.profile_metrics_saved)); finish() }.onFailure { show(it.localizedMessage) }
    }

    private fun updateBmiPreview() {
        val h = number(binding.etHeight.text.toString()) / 100.0
        val w = number(binding.etCurrentWeight.text.toString())
        binding.tvBmiPreview.text = if (h > 0 && w > 0) getString(R.string.profile_bmi_format, w / (h * h)) else getString(R.string.profile_bmi_unavailable)
        val goal = number(binding.etGoalWeight.text.toString())
        binding.tvGoalPreview.text = if (w > 0 && goal > 0) getString(R.string.profile_goal_preview_format, kotlin.math.abs(goal - w), if (goal < w) getString(R.string.profile_goal_direction_lose) else getString(R.string.profile_goal_direction_gain)) else getString(R.string.profile_goal_preview_empty)
    }
    private fun number(raw: String) = raw.replace(',', '.').toDoubleOrNull() ?: 0.0
    private fun show(text: String?) = Snackbar.make(binding.root, text ?: getString(R.string.error_generic), Snackbar.LENGTH_LONG).show()
}
