package com.example.fittrack.ui.social

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.fittrack.R
import com.example.fittrack.databinding.ActivityFriendProfileBinding
import com.example.fittrack.repository.UserRepository
import com.example.fittrack.utils.LocaleHelper
import com.example.fittrack.utils.SystemBars
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Ecrã de perfil de outro utilizador. É usado quando se abre um autor de plano ou um amigo.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class FriendProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFriendProfileBinding
    private val users = UserRepository()

    override fun attachBaseContext(newBase: Context) { super.attachBaseContext(LocaleHelper.onAttach(newBase)) }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        binding = ActivityFriendProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SystemBars.applyTopInset(binding.toolbar)
        binding.btnBack.setOnClickListener { finish() }
        load()
    }

    private fun load() {
        val uid = intent.getStringExtra("user_uid").orEmpty()
        lifecycleScope.launch {
            users.getUser(uid).onSuccess { user ->
                binding.tvName.text = user.displayName
                binding.tvUsername.text = if (user.username.isBlank()) "" else "@${user.username.removePrefix("@")}" 
                binding.tvCode.text = user.friendCode
                binding.tvStats.text = getString(R.string.social_profile_stats, user.totalSessions, user.currentStreak)
                binding.tvStatus.text = if (user.showOnline && user.isOnline) getString(R.string.social_active_now) else getString(R.string.social_offline)
                if (user.photoUrl.isNotBlank()) Glide.with(this@FriendProfileActivity).load(Uri.parse(user.photoUrl)).circleCrop().into(binding.ivProfilePhoto)
            }.onFailure { Snackbar.make(binding.root, it.localizedMessage ?: getString(R.string.error_generic), Snackbar.LENGTH_LONG).show() }
        }
    }
}
