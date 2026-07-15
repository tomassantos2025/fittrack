package com.example.fittrack.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fittrack.R
import com.example.fittrack.databinding.ItemPlanCardBinding
import com.example.fittrack.model.WorkoutPlan

/**
 * Adapter de RecyclerView. Transforma listas de dados em linhas/cards visuais e encaminha cliques para o ecrã que o usa.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class PlanCardAdapter(
    private val currentUserUid: String,
    private val onPlanClick: (WorkoutPlan) -> Unit,
    private val onStartClick: ((WorkoutPlan) -> Unit)? = null,
    private val onEditClick: ((WorkoutPlan) -> Unit)? = null,
    private val onDeleteClick: ((WorkoutPlan) -> Unit)? = null,
    private val onImportClick: ((WorkoutPlan) -> Unit)? = null,
    private val onOwnerClick: ((WorkoutPlan) -> Unit)? = null,
    private val myPlanIds: () -> Set<String> = { emptySet() },
    private val importedSourcePlanIds: () -> Set<String> = { emptySet() },
    private val communityMode: Boolean = false
) : ListAdapter<WorkoutPlan, PlanCardAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemPlanCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemPlanCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(plan: WorkoutPlan) {
            val context = binding.root.context
            val isOwner = plan.ownerUid == currentUserUid
            val isImportedCopy = plan.sourcePlanId.isNotBlank()
            val isAlreadyImported = importedSourcePlanIds().contains(plan.id)
            val isAlreadyMine = isOwner || myPlanIds().contains(plan.id) || isAlreadyImported

            binding.tvPlanName.text = plan.name
            val username = plan.ownerUsername.takeIf { it.isNotBlank() }?.let { " (@${it.removePrefix("@")})" }.orEmpty()
            binding.tvOwner.text = context.getString(R.string.plan_created_by, plan.ownerName.ifBlank { context.getString(R.string.unknown_creator) } + username)
            binding.tvDescription.text = plan.description.ifBlank { plan.muscleGroups.joinToString(", ") }
            binding.tvExerciseCount.text = context.getString(R.string.plan_exercises_count, plan.exerciseCount)
            binding.tvMuscleGroups.text = context.getString(R.string.plan_muscles_format, plan.muscleGroups.joinToString(", ").ifBlank { "—" })
            binding.tvExerciseNames.text = context.getString(R.string.plan_contains_format, plan.exerciseNames.take(4).joinToString(", ").ifBlank { "—" })
            binding.tvPublicBadge.visibility = if (plan.isPublic) View.VISIBLE else View.GONE

            binding.tvOwnershipBadge.visibility = View.GONE
            when {
                communityMode && isOwner -> {
                    binding.tvOwnershipBadge.visibility = View.VISIBLE
                    binding.tvOwnershipBadge.text = context.getString(R.string.plan_your_public_plan)
                }
                communityMode && isAlreadyImported -> {
                    binding.tvOwnershipBadge.visibility = View.VISIBLE
                    binding.tvOwnershipBadge.text = context.getString(R.string.plan_imported_to_my_plans)
                }
                !communityMode && isImportedCopy -> {
                    binding.tvOwnershipBadge.visibility = View.VISIBLE
                    val source = plan.sourceOwnerUsername.ifBlank { plan.sourceOwnerName }
                    binding.tvOwnershipBadge.text = context.getString(R.string.plan_imported_from, source.ifBlank { context.getString(R.string.unknown_creator) })
                }
                !communityMode && plan.isPublic -> {
                    binding.tvOwnershipBadge.visibility = View.VISIBLE
                    binding.tvOwnershipBadge.text = context.getString(R.string.plan_public_badge)
                }
            }

            binding.btnStart.visibility = if (!communityMode && onStartClick != null) View.VISIBLE else View.GONE
            binding.btnStart.setOnClickListener { onStartClick?.invoke(plan) }
            binding.btnEdit.visibility = if (!communityMode && onEditClick != null && isOwner) View.VISIBLE else View.GONE
            binding.btnEdit.setOnClickListener { onEditClick?.invoke(plan) }
            binding.btnDelete.visibility = if (!communityMode && onDeleteClick != null && isOwner) View.VISIBLE else View.GONE
            binding.btnDelete.setOnClickListener { onDeleteClick?.invoke(plan) }
            binding.btnImport.visibility = if (communityMode && onImportClick != null && !isAlreadyMine) View.VISIBLE else View.GONE
            binding.btnImport.setOnClickListener { onImportClick?.invoke(plan) }

            binding.tvImportCount.visibility = if (plan.isPublic) View.VISIBLE else View.GONE
            binding.tvImportCount.text = context.getString(R.string.plan_import_count, plan.importCount)
            binding.tvOwner.isClickable = communityMode && plan.ownerUid.isNotBlank()
            binding.tvOwner.setOnClickListener { if (communityMode) onOwnerClick?.invoke(plan) }
            binding.root.setOnClickListener { onPlanClick(plan) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<WorkoutPlan>() {
        override fun areItemsTheSame(a: WorkoutPlan, b: WorkoutPlan) = a.id == b.id
        override fun areContentsTheSame(a: WorkoutPlan, b: WorkoutPlan) = a == b
    }
}
