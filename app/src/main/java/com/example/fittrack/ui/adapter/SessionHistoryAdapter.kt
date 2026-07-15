package com.example.fittrack.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fittrack.R
import com.example.fittrack.databinding.ItemSessionHistoryBinding
import com.example.fittrack.model.Session
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter de RecyclerView. Transforma listas de dados em linhas/cards visuais e encaminha cliques para o ecrã que o usa.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class SessionHistoryAdapter(
    private val onItemClick: (Session) -> Unit
) : ListAdapter<Session, SessionHistoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSessionHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemSessionHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(session: Session) {
            binding.tvPlanName.text = session.planName.ifBlank { binding.root.context.getString(R.string.default_workout_name) }

            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            binding.tvDate.text = session.completedAt?.let { dateFormat.format(it) } ?: ""

            binding.tvDuration.text = formatDuration(session.durationSeconds)

            binding.tvVolume.text = binding.root.context.getString(
                R.string.kg_format, session.totalVolume
            )

            binding.tvSets.text = binding.root.context.getString(R.string.sets_count_format, session.totalSets)

            if (session.personalRecords > 0) {
                binding.tvPr.text = binding.root.context.getString(R.string.pr_count_format, session.personalRecords)
                binding.tvPr.visibility = android.view.View.VISIBLE
            } else {
                binding.tvPr.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener { onItemClick(session) }
        }
    }

    private fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "${h}h ${m}m ${s}s" else "${m}m ${s}s"
    }

    class DiffCallback : DiffUtil.ItemCallback<Session>() {
        override fun areItemsTheSame(oldItem: Session, newItem: Session) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Session, newItem: Session) = oldItem == newItem
    }
}