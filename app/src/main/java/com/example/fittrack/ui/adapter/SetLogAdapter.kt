package com.example.fittrack.ui.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fittrack.databinding.ItemSetLogRowBinding
import com.example.fittrack.model.SetLog

/**
 * Adapter for displaying and editing individual set log rows
 * inside the ActiveSessionActivity exercise cards.
 */
class SetLogAdapter(
    private val exerciseIndex: Int,
    private val onWeightChanged: (exerciseIndex: Int, setIndex: Int, weight: Double) -> Unit,
    private val onRepsChanged: (exerciseIndex: Int, setIndex: Int, reps: Int) -> Unit,
    private val onDoneToggled: (exerciseIndex: Int, setIndex: Int, done: Boolean) -> Unit
) : ListAdapter<SetLog, SetLogAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSetLogRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(private val binding: ItemSetLogRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var weightWatcher: TextWatcher? = null
        private var repsWatcher: TextWatcher? = null

        fun bind(setLog: SetLog, setIndex: Int) {
            binding.tvSetNumber.text = binding.root.context.getString(
                com.example.fittrack.R.string.set_number_label, setLog.setNumber
            )

            // Remove old watchers before updating text to avoid feedback loops
            weightWatcher?.let { binding.etWeight.removeTextChangedListener(it) }
            repsWatcher?.let { binding.etReps.removeTextChangedListener(it) }

            binding.etWeight.setText(if (setLog.weight > 0) setLog.weight.toString() else "")
            binding.etReps.setText(if (setLog.reps > 0) setLog.reps.toString() else "")
            binding.cbDone.isChecked = setLog.isCompleted

            // Dim completed sets
            binding.root.alpha = if (setLog.isCompleted) 0.6f else 1.0f

            // Attach new watchers
            weightWatcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val w = s.toString().toDoubleOrNull() ?: 0.0
                    onWeightChanged(exerciseIndex, setIndex, w)
                }
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            }
            repsWatcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val r = s.toString().toIntOrNull() ?: 0
                    onRepsChanged(exerciseIndex, setIndex, r)
                }
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            }

            binding.etWeight.addTextChangedListener(weightWatcher)
            binding.etReps.addTextChangedListener(repsWatcher)

            binding.cbDone.setOnCheckedChangeListener { _, isChecked ->
                onDoneToggled(exerciseIndex, setIndex, isChecked)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SetLog>() {
        override fun areItemsTheSame(a: SetLog, b: SetLog) =
            a.setNumber == b.setNumber && a.exerciseId == b.exerciseId
        override fun areContentsTheSame(a: SetLog, b: SetLog) = a == b
    }
}
