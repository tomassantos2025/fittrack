package com.example.fittrack.ui.social

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fittrack.R
import com.example.fittrack.databinding.ActivityGroupChatBinding
import com.example.fittrack.model.TrainingGroup
import com.example.fittrack.repository.AuthRepository
import com.example.fittrack.repository.SocialRepository
import com.example.fittrack.repository.UserRepository
import com.example.fittrack.ui.adapter.ChatMessageAdapter
import com.example.fittrack.utils.LocaleHelper
import com.example.fittrack.utils.SystemBars
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

/**
 * Ecrã de chat de grupo. Permite conversar em grupos criados na componente social.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class GroupChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGroupChatBinding
    private val social = SocialRepository()
    private val auth = AuthRepository()
    private val users = UserRepository()
    private var listener: ListenerRegistration? = null
    private lateinit var adapter: ChatMessageAdapter
    private var groupId = ""
    private var readReceipts = true
    private var currentGroup: TrainingGroup? = null

    override fun attachBaseContext(newBase: Context) { super.attachBaseContext(LocaleHelper.onAttach(newBase)) }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        binding = ActivityGroupChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SystemBars.applyTopInset(binding.toolbar)
        SystemBars.applyBottomInset(binding.inputRow)
        groupId = intent.getStringExtra("group_id").orEmpty()
        adapter = ChatMessageAdapter { auth.currentUser()?.uid.orEmpty() }
        binding.rvMessages.adapter = adapter
        (binding.rvMessages.layoutManager as? LinearLayoutManager)?.stackFromEnd = true
        binding.btnBack.setOnClickListener { finish() }
        binding.btnGroupOptions.setOnClickListener { showGroupOptions() }
        binding.btnSend.setOnClickListener { send() }
        binding.etMessage.setOnEditorActionListener { _, id, _ -> if (id == EditorInfo.IME_ACTION_SEND) { send(); true } else false }
        load()
    }

    override fun onResume() {
        super.onResume()
        auth.currentUser()?.uid?.let { uid -> lifecycleScope.launch { users.setOnlineStatus(uid, true) } }
    }

    override fun onPause() {
        auth.currentUser()?.uid?.let { uid -> lifecycleScope.launch { users.setOnlineStatus(uid, false) } }
        super.onPause()
    }

    private fun load() = lifecycleScope.launch {
        val myUid = auth.currentUser()?.uid.orEmpty()
        if (myUid.isBlank()) { finish(); return@launch }
        users.getUser(myUid).onSuccess { readReceipts = it.readReceipts }
        social.group(groupId).onSuccess { group ->
            currentGroup = group
            binding.tvTitle.text = group.name
            if (group.pendingMemberUids.contains(myUid)) showInvitationDialog(group)
            else startMessages()
        }.onFailure { show(it.localizedMessage); finish() }
    }

    private fun showInvitationDialog(group: TrainingGroup) {
        binding.inputRow.visibility = View.GONE
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.social_group_invitation_title)
            .setMessage(getString(R.string.social_group_invitation_message, group.name))
            .setCancelable(false)
            .setNegativeButton(R.string.social_leave_group) { _, _ ->
                lifecycleScope.launch { social.leaveGroup(groupId); finish() }
            }
            .setPositiveButton(R.string.social_stay_group) { _, _ ->
                lifecycleScope.launch {
                    social.acceptGroupInvitation(groupId)
                        .onSuccess { binding.inputRow.visibility = View.VISIBLE; startMessages() }
                        .onFailure { show(it.localizedMessage); finish() }
                }
            }.show()
    }

    private fun startMessages() {
        listener?.remove()
        listener = social.observeGroupMessages(groupId) { result ->
            result.onSuccess { items ->
                adapter.submitList(items) { if (items.isNotEmpty()) binding.rvMessages.scrollToPosition(items.lastIndex) }
                binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                val myUid = auth.currentUser()?.uid.orEmpty()
                if (readReceipts && myUid.isNotBlank()) lifecycleScope.launch {
                    social.markGroupMessagesRead(groupId, items.filter { it.senderUid != myUid && !it.readBy.contains(myUid) }.map { it.id }, myUid)
                }
            }.onFailure { show(it.localizedMessage) }
        }
    }

    private fun showGroupOptions() = lifecycleScope.launch {
        val group = social.group(groupId).getOrElse { show(it.localizedMessage); return@launch }
        currentGroup = group
        val memberUsers = social.groupMembers(groupId).getOrDefault(emptyList())
        val participants = memberUsers.joinToString("\n") { user ->
            val state = if (user.showOnline && user.isOnline) getString(R.string.social_active_now) else getString(R.string.social_offline)
            val waiting = if (group.pendingMemberUids.contains(user.uid)) " • ${getString(R.string.social_invitation_pending)}" else ""
            "• ${user.displayName.ifBlank { user.username }} — $state$waiting"
        }.ifBlank { getString(R.string.social_no_participants) }
        MaterialAlertDialogBuilder(this@GroupChatActivity)
            .setTitle(R.string.social_group_options)
            .setMessage(participants)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.social_leave_group) { _, _ -> confirmLeaveGroup() }
            .show()
    }

    private fun confirmLeaveGroup() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.social_leave_group)
            .setMessage(R.string.social_leave_group_confirm)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.social_leave_group) { _, _ ->
                lifecycleScope.launch { social.leaveGroup(groupId).onSuccess { finish() }.onFailure { show(it.localizedMessage) } }
            }.show()
    }

    private fun send() {
        val text = binding.etMessage.text?.toString().orEmpty().trim()
        if (text.isBlank()) return
        lifecycleScope.launch { social.sendGroupMessage(groupId, text).onFailure { show(it.localizedMessage) } }
        binding.etMessage.setText("")
    }

    private fun show(message: String?) = Snackbar.make(binding.root, message ?: getString(R.string.error_generic), Snackbar.LENGTH_LONG).show()
    override fun onDestroy() { listener?.remove(); super.onDestroy() }
}
