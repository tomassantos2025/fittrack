package com.example.fittrack.ui.main

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.fittrack.MainActivity
import com.example.fittrack.R
import com.example.fittrack.databinding.FragmentSocialBinding
import com.example.fittrack.model.FriendRequest
import com.example.fittrack.model.TrainingGroup
import com.example.fittrack.model.User
import com.example.fittrack.repository.AuthRepository
import com.example.fittrack.repository.SocialRepository
import com.example.fittrack.repository.UserRepository
import com.example.fittrack.ui.social.FriendProfileActivity
import com.example.fittrack.ui.social.GroupChatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import com.example.fittrack.utils.UiAnimations

/**
 * Fragment da área social. Mostra amigos, grupos e funcionalidades partilhadas entre utilizadores.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class SocialFragment : Fragment() {
    private var _binding: FragmentSocialBinding? = null
    private val binding get() = _binding!!
    private val social = SocialRepository()
    private val auth = AuthRepository()
    private val users = UserRepository()
    private var foundUser: User? = null
    private var currentFriends: List<User> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentSocialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        UiAnimations.reveal(binding.root)
        binding.btnSearchFriend.setOnClickListener { searchFriend() }
        binding.btnAddFriend.setOnClickListener { foundUser?.let { sendRequest(it) } }
        binding.btnOpenFoundProfile.setOnClickListener { foundUser?.let { openProfile(it.uid) } }
        binding.btnCreateGroup.setOnClickListener { showCreateGroup() }
        loadAll()
    }

    override fun onResume() { super.onResume(); loadAll() }

    private fun loadAll() {
        val firebaseUser = auth.currentUser() ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            users.getOrCreateUser(firebaseUser)
                .mapCatching { social.ensureFriendCode(it).getOrThrow() }
                .onSuccess { binding.tvMyFriendCode.text = it.friendCode }
            renderRequests(social.incomingRequests().getOrDefault(emptyList()))
            currentFriends = social.friends().getOrDefault(emptyList())
            renderFriends(currentFriends)
            renderGroups(social.myGroups().getOrDefault(emptyList()))
        }
    }

    private fun searchFriend() = viewLifecycleOwner.lifecycleScope.launch {
        binding.cardSearchResult.visibility = View.GONE
        social.findByFriendCode(binding.etFriendCode.text?.toString().orEmpty()).onSuccess { user ->
            foundUser = user
            binding.tvFoundName.text = user.displayName
            binding.tvFoundStats.text = getString(R.string.social_profile_stats, user.totalSessions, user.currentStreak)
            binding.cardSearchResult.visibility = View.VISIBLE
        }.onFailure { show(it.localizedMessage) }
    }

    private fun sendRequest(user: User) = viewLifecycleOwner.lifecycleScope.launch {
        social.sendFriendRequest(user).onSuccess {
            show(getString(R.string.social_request_sent))
            binding.cardSearchResult.visibility = View.GONE
        }.onFailure { show(it.localizedMessage) }
    }

    private fun renderRequests(items: List<FriendRequest>) {
        binding.listRequests.removeAllViews()
        binding.tvNoRequests.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        items.forEach { request ->
            val btn = actionButton(getString(R.string.social_accept_request_from, request.fromName))
            btn.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    social.acceptRequest(request).onSuccess {
                        loadAll(); (activity as? MainActivity)?.refreshSocialBadge()
                    }.onFailure { show(it.localizedMessage) }
                }
            }
            binding.listRequests.addView(btn)
        }
    }

    private fun renderFriends(items: List<User>) {
        binding.listFriends.removeAllViews()
        binding.tvNoFriends.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        items.forEach { friend ->
            val online = if (friend.showOnline && friend.isOnline) getString(R.string.social_active_now) else getString(R.string.social_offline)
            val btn = actionButton("${friend.displayName}  •  $online")
            btn.setOnClickListener { openProfile(friend.uid) }
            binding.listFriends.addView(btn)
        }
    }

    private fun renderGroups(items: List<TrainingGroup>) {
        binding.listGroups.removeAllViews()
        binding.tvNoGroups.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        val myUid = auth.currentUser()?.uid.orEmpty()
        items.forEach { group ->
            val pending = group.pendingMemberUids.contains(myUid)
            val suffix = if (pending) " • ${getString(R.string.social_invitation_pending)}" else ""
            val btn = actionButton("${group.name}  •  ${getString(R.string.social_members_count, group.memberUids.size)}$suffix")
            btn.setOnClickListener { startActivity(Intent(requireContext(), GroupChatActivity::class.java).putExtra("group_id", group.id)) }
            binding.listGroups.addView(btn)
        }
    }

    private fun openProfile(uid: String) {
        startActivity(Intent(requireContext(), FriendProfileActivity::class.java).putExtra("user_uid", uid))
    }

    private fun actionButton(text: String) = MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
        this.text = text
        isAllCaps = false
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        cornerRadius = (14 * resources.displayMetrics.density).toInt()
        insetTop = 0
        insetBottom = 0
        setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface))
        strokeColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.divider))
        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.card_background))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = (8 * resources.displayMetrics.density).toInt() }
        UiAnimations.reveal(this, 0L, 180L, 6f)
    }

    private fun showCreateGroup() {
        if (currentFriends.isEmpty()) {
            show(getString(R.string.social_group_requires_friends))
            return
        }
        val names = currentFriends.map { it.displayName.ifBlank { it.username } }.toTypedArray()
        val checked = BooleanArray(currentFriends.size)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.social_create_group)
            .setMessage(R.string.social_group_select_friends)
            .setMultiChoiceItems(names, checked) { _, which, selected -> checked[which] = selected }
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.social_next) { _, _ ->
                val selectedUids = currentFriends.filterIndexed { index, _ -> checked[index] }.map { it.uid }
                askGroupName(selectedUids)
            }.show()
    }

    private fun askGroupName(selectedUids: List<String>) {
        if (selectedUids.isEmpty()) { show(getString(R.string.social_group_requires_friend_selection)); return }
        val field = android.widget.EditText(requireContext()).apply { hint = getString(R.string.social_group_name_hint) }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.social_group_name_title)
            .setView(field)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.social_create_group) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    social.createGroup(field.text.toString(), selectedUids)
                        .onSuccess { show(getString(R.string.social_group_created)); loadAll() }
                        .onFailure { show(it.localizedMessage) }
                }
            }.show()
    }

    private fun show(message: String?) = Snackbar.make(binding.root, message ?: getString(R.string.error_generic), Snackbar.LENGTH_LONG).show()
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
