package com.example.fittrack.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.fittrack.MainActivity
import com.example.fittrack.ui.PremiumInfoActivity
import com.example.fittrack.databinding.ActivitySplashLoginBinding
import com.example.fittrack.utils.LocaleHelper
import com.example.fittrack.utils.UiMessageException
import com.example.fittrack.utils.SystemBars
import com.example.fittrack.viewmodel.LoginViewModel
import com.google.android.material.snackbar.Snackbar

/**
 * Ecrã inicial de autenticação. Mostra a entrada da app, valida sessão existente e encaminha para login ou app principal.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class SplashLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private lateinit var credentialManager: CredentialManager

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Auto-navigate if already logged in
        if (viewModel.isLoggedIn()) {
            navigateToMain()
            return
        }

        binding = ActivitySplashLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SystemBars.applyTopInset(binding.root)

        credentialManager = CredentialManager.create(this)
        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            viewModel.login(email, password)
        }

        binding.tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.btnGoogleSignIn.setOnClickListener { startGoogleSignIn() }
    }

    private fun startGoogleSignIn() {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(getString(com.example.fittrack.R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        lifecycleScope.launch {
            try {
                val credential = credentialManager.getCredential(this@SplashLoginActivity, request).credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    viewModel.loginWithGoogle(GoogleIdTokenCredential.createFrom(credential.data).idToken)
                } else Snackbar.make(binding.root, getString(com.example.fittrack.R.string.google_signin_invalid), Snackbar.LENGTH_LONG).show()
            } catch (e: Exception) {
                Snackbar.make(binding.root, e.localizedMessage ?: getString(com.example.fittrack.R.string.login_failed), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnLogin.isEnabled = !isLoading
            binding.btnLogin.text = if (isLoading) "" else getString(com.example.fittrack.R.string.btn_login)
        }

        viewModel.loginResult.observe(this) { result ->
            result.onSuccess {
                navigateToPremiumInfo()
            }.onFailure { e ->
                Snackbar.make(binding.root, if (e is UiMessageException) getString(e.messageRes) else (e.localizedMessage ?: getString(com.example.fittrack.R.string.login_failed)), Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.googleSignInResult.observe(this) { result ->
            result.onSuccess {
                navigateToPremiumInfo()
            }.onFailure { e ->
                Snackbar.make(binding.root, if (e is UiMessageException) getString(e.messageRes) else (e.localizedMessage ?: getString(com.example.fittrack.R.string.login_failed)), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun navigateToPremiumInfo() {
        startActivity(Intent(this, PremiumInfoActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
