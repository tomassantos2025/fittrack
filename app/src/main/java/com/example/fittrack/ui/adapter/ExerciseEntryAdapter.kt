package com.example.fittrack.ui.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fittrack.databinding.ItemExerciseEntryBinding
import com.example.fittrack.model.ExerciseEntry

/**
 * Adapter de RecyclerView. Transforma listas de dados em linhas/cards visuais e encaminha cliques para o ecrã que o usa.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class ExerciseEntryAdapter(
    private val editable: Boolean = false,
    private val onRemove: ((Int) -> Unit)? = null,
    private val onChanged: ((Int, ExerciseEntry) -> Unit)? = null
) : ListAdapter<ExerciseEntry, ExerciseEntryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExerciseEntryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(private val binding: ItemExerciseEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: ExerciseEntry, position: Int) {
            binding.tvIcon.text = iconForName(entry.exerciseName)
            binding.tvExerciseName.text = entry.exerciseName
            binding.etSets.setText(entry.targetSets.toString())
            binding.etReps.setText(entry.targetReps.toString())
            binding.etWeight.setText(entry.targetWeight.toString())

            if (editable) {
                binding.btnRemove.visibility = android.view.View.VISIBLE
                binding.etSets.isEnabled = true
                binding.etReps.isEnabled = true
                binding.etWeight.isEnabled = true

                binding.btnRemove.setOnClickListener { onRemove?.invoke(position) }

                binding.etSets.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        val updated = entry.copy(targetSets = s.toString().toIntOrNull() ?: entry.targetSets)
                        onChanged?.invoke(position, updated)
                    }
                    override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                    override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                })

                binding.etReps.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        val updated = entry.copy(targetReps = s.toString().toIntOrNull() ?: entry.targetReps)
                        onChanged?.invoke(position, updated)
                    }
                    override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                    override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                })

                binding.etWeight.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        val updated = entry.copy(targetWeight = s.toString().toDoubleOrNull() ?: entry.targetWeight)
                        onChanged?.invoke(position, updated)
                    }
                    override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                    override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                })
            } else {
                binding.btnRemove.visibility = android.view.View.GONE
                binding.etSets.isEnabled = false
                binding.etReps.isEnabled = false
                binding.etWeight.isEnabled = false
            }
        }
    }

    private fun iconForName(name: String): String {
        val n = name.lowercase()
        return when {
            "squat" in n || "leg" in n || "lunge" in n || "calf" in n || "deadlift" in n || "thrust" in n -> "🦵"
            "row" in n || "pull" in n || "lat" in n -> "🪽"
            "press" in n || "fly" in n || "push" in n || "pec" in n -> "💪"
            "curl" in n || "tricep" in n -> "💪"
            "plank" in n || "raise" in n || "core" in n -> "🔥"
            "burpee" in n || "climber" in n -> "⚡"
            else -> "🏋️"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ExerciseEntry>() {
        override fun areItemsTheSame(a: ExerciseEntry, b: ExerciseEntry) = a.id == b.id
        override fun areContentsTheSame(a: ExerciseEntry, b: ExerciseEntry) = a == b
    }
}