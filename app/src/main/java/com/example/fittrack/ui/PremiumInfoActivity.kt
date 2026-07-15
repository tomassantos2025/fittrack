package com.example.fittrack.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fittrack.MainActivity
import com.example.fittrack.R
import com.example.fittrack.databinding.ActivityPremiumInfoBinding
import com.example.fittrack.repository.AuthRepository
import com.example.fittrack.repository.UserRepository
import com.example.fittrack.utils.LocaleHelper
import com.example.fittrack.utils.SystemBars
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Free vs Pro comparison screen shown after login/registration and from upgrade buttons.
 * Nesta versão académica, o botão de ativação Pro simula a subscrição de 1€/mês guardando
 * users/{uid}/is_pro = true no Firestore.
 */
class PremiumInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumInfoBinding
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SystemBars.applyTopInset(binding.root)

        binding.btnContinueFree.setOnClickListener { goToMain() }
        binding.btnActivatePro.setOnClickListener { activateProDemo() }
    }

    /** Guarda o estado Pro no documento Firestore do utilizador atual. */
    private fun activateProDemo() {
        val uid = authRepository.currentUser()?.uid ?: return goToMain()
        binding.btnActivatePro.isEnabled = false
        lifecycleScope.launch {
            userRepository.setProStatus(uid, true)
                .onSuccess {
                    Snackbar.make(binding.root, getString(R.string.pro_upgrade_success), Snackbar.LENGTH_SHORT).show()
                    goToMain()
                }
                .onFailure {
                    binding.btnActivatePro.isEnabled = true
                    Snackbar.make(binding.root, it.localizedMessage ?: getString(R.string.error_generic), Snackbar.LENGTH_LONG).show()
                }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
