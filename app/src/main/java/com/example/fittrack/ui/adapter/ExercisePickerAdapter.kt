package com.example.fittrack.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fittrack.R
import com.example.fittrack.databinding.ItemExercisePickerCardBinding
import com.example.fittrack.model.Exercise
import com.example.fittrack.ui.ExerciseDetailDialog
import com.example.fittrack.utils.ExerciseUiHelper

/**
 * Adapter de RecyclerView. Transforma listas de dados em linhas/cards visuais e encaminha cliques para o ecrã que o usa.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class ExercisePickerAdapter(
    private val showAddButton: Boolean = true,
    private val onAdd: ((Exercise) -> Unit)? = null
) : ListAdapter<Exercise, ExercisePickerAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExercisePickerCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val b: ItemExercisePickerCardBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(exercise: Exercise) {
            val context = b.root.context
            b.tvName.text = ExerciseUiHelper.name(context, exercise)
            b.tvMeta.text = listOf(
                exercise.muscleGroups.joinToString(" / ") { ExerciseUiHelper.muscle(context, it) },
                ExerciseUiHelper.equipment(context, exercise.equipment),
                ExerciseUiHelper.difficulty(context, exercise.difficulty),
                if (exercise.homeFriendly) context.getString(R.string.exercise_home_friendly) else ""
            ).filter { it.isNotBlank() }.joinToString(" • ")
            b.tvDescription.text = ExerciseUiHelper.description(context, exercise).ifBlank {
                context.getString(R.string.exercise_default_description)
            }

            if (exercise.animationUrl.isNotBlank()) {
                Glide.with(b.ivExercise)
                    .asGif()
                    .load(exercise.animationUrl)
                    .placeholder(R.drawable.ic_exercise_placeholder)
                    .error(R.drawable.ic_exercise_placeholder)
                    .into(b.ivExercise)
            } else {
                b.ivExercise.setImageResource(R.drawable.ic_exercise_placeholder)
            }

            b.btnAdd.visibility = if (showAddButton) View.VISIBLE else View.GONE
            b.btnAdd.setOnClickListener { onAdd?.invoke(exercise) }
            b.btnLearnMore.setOnClickListener {
                ExerciseDetailDialog.show(context, exercise, showAddButton, onAdd)
            }
            b.root.setOnClickListener {
                ExerciseDetailDialog.show(context, exercise, showAddButton, onAdd)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Exercise>() {
        override fun areItemsTheSame(a: Exercise, b: Exercise) = a.id == b.id
        override fun areContentsTheSame(a: Exercise, b: Exercise) = a == b
    }
}
