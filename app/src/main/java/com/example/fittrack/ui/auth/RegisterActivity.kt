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
import com.example.fittrack.R
import com.example.fittrack.databinding.ActivityRegisterBinding
import com.example.fittrack.utils.LocaleHelper
import com.example.fittrack.utils.UiMessageException
import com.example.fittrack.viewmodel.RegisterViewModel
import com.example.fittrack.viewmodel.LoginViewModel
import com.example.fittrack.utils.SystemBars
import com.google.android.material.snackbar.Snackbar

/**
 * Ecrã de registo. Cria a conta no Firebase Authentication e cria o perfil correspondente no Firestore.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()
    private val loginViewModel: LoginViewModel by viewModels()
    private lateinit var credentialManager: CredentialManager

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SystemBars.applyTopInset(binding.root)
        credentialManager = CredentialManager.create(this)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnRegister.setOnClickListener {
            val displayName = binding.etDisplayName.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()
            viewModel.register(displayName, username, email, password, confirmPassword)
        }

        binding.btnGoogleRegister.setOnClickListener { startGoogleSignIn() }
        binding.tvLoginLink.setOnClickListener { finish() }
    }

    private fun startGoogleSignIn() {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        lifecycleScope.launch {
            try {
                val credential = credentialManager.getCredential(this@RegisterActivity, request).credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    loginViewModel.loginWithGoogle(GoogleIdTokenCredential.createFrom(credential.data).idToken)
                } else Snackbar.make(binding.root, getString(R.string.google_signin_invalid), Snackbar.LENGTH_LONG).show()
            } catch (e: Exception) {
                Snackbar.make(binding.root, e.localizedMessage ?: getString(R.string.login_failed), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnRegister.isEnabled = !isLoading
            binding.btnRegister.text = if (isLoading) "" else getString(R.string.btn_register)
        }

        loginViewModel.googleSignInResult.observe(this) { result ->
            result.onSuccess {
                startActivity(Intent(this, PremiumInfoActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
                finish()
            }.onFailure { e -> Snackbar.make(binding.root, e.localizedMessage ?: getString(R.string.login_failed), Snackbar.LENGTH_LONG).show() }
        }

        viewModel.registerResult.observe(this) { result ->
            result.onSuccess {
                startActivity(Intent(this, PremiumInfoActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }.onFailure { e ->
                Snackbar.make(binding.root, if (e is UiMessageException) getString(e.messageRes) else (e.localizedMessage ?: getString(R.string.registration_failed)), Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
