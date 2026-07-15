package com.example.fittrack.ui.adapter

import android.content.res.ColorStateList
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fittrack.R
import com.example.fittrack.databinding.ItemChatMessageBinding
import com.example.fittrack.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter de RecyclerView. Transforma listas de dados em linhas/cards visuais e encaminha cliques para o ecrã que o usa.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class ChatMessageAdapter(
    private val currentUid: () -> String
) : ListAdapter<ChatMessage, ChatMessageAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatMessage) {
            val mine = item.senderUid == currentUid()
            binding.messageRow.gravity = if (mine) Gravity.END else Gravity.START
            binding.cardMessage.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(binding.root.context, if (mine) R.color.chat_bubble_mine else R.color.chat_bubble_other)
            )
            binding.tvSender.text = if (mine) binding.root.context.getString(R.string.chat_you) else item.senderName
            binding.tvMessage.text = item.text
            val time = item.createdAt?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it) }.orEmpty()
            binding.tvTime.text = if (mine && item.readBy.size > 1) "$time  ✓✓" else time
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage) = oldItem == newItem
    }
}
