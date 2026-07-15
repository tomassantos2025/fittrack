package com.example.fittrack.ui.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fittrack.databinding.ItemPlanExerciseBinding
import com.example.fittrack.model.Exercise

/**
 * Adapter for the exercise list in CreateEditPlanActivity.
 * Supports drag-to-reorder via ItemTouchHelper and swipe-to-remove.
 */
class PlanExerciseAdapter(
    private val onRemove: (Int) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : ListAdapter<Exercise, PlanExerciseAdapter.ViewHolder>(DiffCallback()) {

    // Mutable backing list for reordering support
    private val items = mutableListOf<Exercise>()

    override fun submitList(list: List<Exercise>?) {
        items.clear()
        list?.let { items.addAll(it) }
        super.submitList(list)
    }

    fun getItems(): List<Exercise> = items.toList()

    /** Called by ItemTouchHelper when the user drags an item */
    fun moveItem(from: Int, to: Int) {
        val moved = items.removeAt(from)
        items.add(to, moved)
        notifyItemMoved(from, to)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPlanExerciseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemPlanExerciseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(exercise: Exercise, position: Int) {
            binding.tvExerciseName.text = exercise.name
            binding.tvMuscleGroups.text = exercise.muscleGroups.joinToString(" · ")
            binding.tvEquipment.text = exercise.equipment

            binding.root.setOnClickListener { onRemove(position) }

            // Drag handle — trigger drag on touch
            binding.root.setOnLongClickListener {
                onStartDrag(this)
                true
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Exercise>() {
        override fun areItemsTheSame(a: Exercise, b: Exercise) = a.id == b.id
        override fun areContentsTheSame(a: Exercise, b: Exercise) = a == b
    }

    /**
     * ItemTouchHelper.Callback for drag-and-drop reordering.
     * Attach to the RecyclerView in the Activity:
     *
     *   val touchHelper = ItemTouchHelper(PlanExerciseAdapter.DragCallback(adapter))
     *   touchHelper.attachToRecyclerView(binding.rvEntries)
     */
    class DragCallback(private val adapter: PlanExerciseAdapter) : ItemTouchHelper.Callback() {

        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            val drag = ItemTouchHelper.UP or ItemTouchHelper.DOWN
            val swipe = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            return makeMovementFlags(drag, swipe)
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            adapter.moveItem(viewHolder.adapterPosition, target.adapterPosition)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            adapter.onRemove(viewHolder.adapterPosition)
        }

        override fun isLongPressDragEnabled() = true
        override fun isItemViewSwipeEnabled() = true
    }
}
