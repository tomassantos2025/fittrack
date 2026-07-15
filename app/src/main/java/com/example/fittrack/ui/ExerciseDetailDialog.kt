package com.example.fittrack.ui

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import com.bumptech.glide.Glide
import com.example.fittrack.R
import com.example.fittrack.databinding.DialogExerciseDetailBinding
import com.example.fittrack.model.Exercise
import com.example.fittrack.utils.ExerciseUiHelper
import java.net.URLEncoder

/**
 * Diálogo com detalhes de um exercício. Apresenta informação complementar sem sair do ecrã atual.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
object ExerciseDetailDialog {
    fun show(
        context: Context,
        exercise: Exercise,
        showAddButton: Boolean,
        onAdd: ((Exercise) -> Unit)? = null
    ) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DialogExerciseDetailBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        binding.tvName.text = ExerciseUiHelper.name(context, exercise)
        binding.tvMeta.text = listOf(
            exercise.muscleGroups.joinToString(" / ") { ExerciseUiHelper.muscle(context, it) },
            ExerciseUiHelper.equipment(context, exercise.equipment),
            ExerciseUiHelper.difficulty(context, exercise.difficulty),
            if (exercise.homeFriendly) context.getString(R.string.exercise_home_friendly) else ""
        ).filter { it.isNotBlank() }.joinToString(" • ")
        binding.tvDescription.text = ExerciseUiHelper.description(context, exercise).ifBlank {
            context.getString(R.string.exercise_default_description)
        }
        binding.tvHowTo.text = bullets(ExerciseUiHelper.howTo(context, exercise).ifEmpty {
            listOf(context.getString(R.string.exercise_detail_no_steps))
        })
        binding.tvTips.text = bullets(ExerciseUiHelper.tips(context, exercise).ifEmpty {
            listOf(context.getString(R.string.exercise_detail_default_tip))
        })
        binding.tvMistakes.text = bullets(ExerciseUiHelper.mistakes(context, exercise).ifEmpty {
            listOf(context.getString(R.string.exercise_detail_default_mistake))
        })

        if (exercise.animationUrl.isNotBlank()) {
            Glide.with(binding.ivExercise)
                .asGif()
                .load(exercise.animationUrl)
                .placeholder(R.drawable.ic_exercise_placeholder)
                    .error(R.drawable.ic_exercise_placeholder)
                .into(binding.ivExercise)
        } else {
            binding.ivExercise.setImageResource(R.drawable.ic_exercise_placeholder)
        }

        binding.btnBack.setOnClickListener { dialog.dismiss() }

        binding.btnYoutube.setOnClickListener {
            val encoded = URLEncoder.encode(ExerciseUiHelper.youtubeSearchQuery(exercise), "UTF-8")
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded")))
        }

        binding.btnAdd.visibility = if (showAddButton) View.VISIBLE else View.GONE
        binding.btnAdd.setOnClickListener {
            onAdd?.invoke(exercise)
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.94f).toInt(),
            (context.resources.displayMetrics.heightPixels * 0.88f).toInt()
        )
    }

    private fun bullets(items: List<String>): String = items.joinToString("\n") { "• $it" }
}
