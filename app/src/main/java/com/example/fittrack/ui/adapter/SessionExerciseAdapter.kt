package com.example.fittrack.ui.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fittrack.R
import com.example.fittrack.databinding.ItemSessionExerciseBinding
import com.example.fittrack.model.SetLog
import com.example.fittrack.viewmodel.SessionExercise

/**
 * Adapter used only while the workout is being configured.
 * Completion controls intentionally live in the focused workout screen instead of here.
 */
class SessionExerciseAdapter(
    private val onAddSet: (exerciseIndex: Int) -> Unit,
    private val onRemoveSet: (exerciseIndex: Int, setIndex: Int) -> Unit,
    private val onSetChanged: (exerciseIndex: Int, setIndex: Int, weight: Double, reps: Int) -> Unit
) : ListAdapter<SessionExercise, SessionExerciseAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSessionExerciseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position), position)

    inner class ViewHolder(private val b: ItemSessionExerciseBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: SessionExercise, exerciseIndex: Int) {
            b.tvExerciseName.text = item.entry.exerciseName
            b.tvTargetInfo.text = b.root.context.getString(
                R.string.session_target_format,
                item.setLogs.size,
                item.entry.targetReps,
                item.entry.targetWeight
            )

            if (item.animationUrl.isNotEmpty()) {
                Glide.with(b.ivExerciseAnim)
                    .asGif()
                    .load(item.animationUrl)
                    .placeholder(R.drawable.ic_exercise_placeholder)
                    .into(b.ivExerciseAnim)
            } else {
                b.ivExerciseAnim.setImageResource(R.drawable.ic_exercise_placeholder)
            }

            b.llSets.removeAllViews()
            item.setLogs.forEachIndexed { setIndex, setLog ->
                addSetRow(b.llSets, exerciseIndex, setIndex, setLog)
            }
            b.btnAddSet.setOnClickListener { onAddSet(exerciseIndex) }
        }

        private fun addSetRow(container: LinearLayout, exerciseIndex: Int, setIndex: Int, setLog: SetLog) {
            val context = container.context
            val row = LayoutInflater.from(context).inflate(R.layout.item_set_log_row, container, false)
            val tvSetNumber = row.findViewById<TextView>(R.id.tvSetNumber)
            val etWeight = row.findViewById<EditText>(R.id.etWeight)
            val etReps = row.findViewById<EditText>(R.id.etReps)
            val btnRemove = row.findViewById<ImageButton>(R.id.btnRemoveSet)
            val cbDone = row.findViewById<CheckBox>(R.id.cbDone)

            tvSetNumber.text = context.getString(R.string.set_number_label, setLog.setNumber)
            etWeight.setText(if (setLog.weight > 0) setLog.weight.toString() else "")
            etReps.setText(if (setLog.reps > 0) setLog.reps.toString() else "")

            // Completion belongs to focused workout mode, never to configuration.
            cbDone.visibility = View.GONE
            // The first three sets are the default workout structure. Extra sets can be removed.
            btnRemove.visibility = if (setIndex >= 3) View.VISIBLE else View.INVISIBLE
            btnRemove.setOnClickListener { onRemoveSet(exerciseIndex, setIndex) }

            var currentWeight = setLog.weight
            var currentReps = setLog.reps
            etWeight.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    currentWeight = s.toString().replace(',', '.').toDoubleOrNull() ?: 0.0
                    onSetChanged(exerciseIndex, setIndex, currentWeight, currentReps)
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            })
            etReps.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    currentReps = s.toString().toIntOrNull() ?: 0
                    onSetChanged(exerciseIndex, setIndex, currentWeight, currentReps)
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            })
            container.addView(row)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SessionExercise>() {
        override fun areItemsTheSame(a: SessionExercise, b: SessionExercise) =
            a.entry.id.ifBlank { "${a.entry.exerciseId}-${a.entry.order}" } ==
                b.entry.id.ifBlank { "${b.entry.exerciseId}-${b.entry.order}" }
        override fun areContentsTheSame(a: SessionExercise, b: SessionExercise) = a == b
    }
}
