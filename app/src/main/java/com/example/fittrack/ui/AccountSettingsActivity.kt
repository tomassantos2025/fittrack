package com.example.fittrack.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fittrack.R
import com.example.fittrack.databinding.ActivityAccountSettingsBinding
import com.example.fittrack.repository.AuthRepository
import com.example.fittrack.repository.UserRepository
import com.example.fittrack.ui.auth.SplashLoginActivity
import com.example.fittrack.utils.AppearanceHelper
import com.example.fittrack.utils.LocaleHelper
import com.example.fittrack.utils.SystemBars
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

/**
 * Ecrã de definições da conta. Permite editar dados do utilizador, idioma, privacidade e ações de conta.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class AccountSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountSettingsBinding
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()
    private val workoutPlanRepository = com.example.fittrack.repository.WorkoutPlanRepository()
    private var originalEmail: String = ""
    private var isBindingUser = false

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SystemBars.applyTopInset(binding.root)
        setupSpinners()
        setupListeners()
        loadUser()
    }

    override fun onResume() {
        super.onResume()
        authRepository.currentUser()?.uid?.let { uid ->
            lifecycleScope.launch { userRepository.setOnlineStatus(uid, true) }
        }
    }

    override fun onPause() {
        super.onPause()
        authRepository.currentUser()?.uid?.let { uid ->
            lifecycleScope.launch { userRepository.setOnlineStatus(uid, false) }
        }
    }

    private fun setupSpinners() {
        val languages = listOf(
            getString(R.string.profile_language_en),
            getString(R.string.profile_language_pt),
            getString(R.string.profile_language_es)
        )
        val languageAdapter = ArrayAdapter(this, R.layout.item_spinner, languages)
        languageAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        binding.spinnerLanguage.adapter = languageAdapter
        binding.spinnerLanguage.setSelection(listOf("en", "pt", "es").indexOf(LocaleHelper.getLanguage(this)).coerceAtLeast(0))

        val modes = listOf(
            getString(R.string.appearance_system),
            getString(R.string.appearance_dark),
            getString(R.string.appearance_light),
            getString(R.string.appearance_amoled)
        )
        val appearanceAdapter = ArrayAdapter(this, R.layout.item_spinner, modes)
        appearanceAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        binding.spinnerAppearance.adapter = appearanceAdapter
        val modeCodes = listOf(
            AppearanceHelper.MODE_SYSTEM,
            AppearanceHelper.MODE_DARK,
            AppearanceHelper.MODE_LIGHT,
            AppearanceHelper.MODE_AMOLED
        )
        binding.spinnerAppearance.setSelection(modeCodes.indexOf(AppearanceHelper.getMode(this)).coerceAtLeast(0))
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSaveAccount.setOnClickListener { saveAccount() }
        binding.btnChangePassword.setOnClickListener { changePassword() }
        binding.btnDeleteAccount.setOnClickListener { confirmDeleteAccount() }

        binding.spinnerLanguage.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, pos: Int, id: Long) {
                val selected = listOf("en", "pt", "es")[pos]
                if (selected != LocaleHelper.getLanguage(this@AccountSettingsActivity)) {
                    authRepository.currentUser()?.uid?.let { uid -> lifecycleScope.launch { userRepository.updateLanguage(uid, selected) } }
                    LocaleHelper.setLocale(this@AccountSettingsActivity, selected)
                    recreate()
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }

        binding.spinnerAppearance.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, pos: Int, id: Long) {
                val selected = listOf(
                    AppearanceHelper.MODE_SYSTEM,
                    AppearanceHelper.MODE_DARK,
                    AppearanceHelper.MODE_LIGHT,
                    AppearanceHelper.MODE_AMOLED
                )[pos]
                if (selected != AppearanceHelper.getMode(this@AccountSettingsActivity)) {
                    AppearanceHelper.setMode(this@AccountSettingsActivity, selected)
                    Snackbar.make(binding.root, getString(R.string.appearance_applied), Snackbar.LENGTH_SHORT).show()
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }

        binding.switchShowOnline.setOnCheckedChangeListener { _, checked ->
            if (!isBindingUser) savePrivacy(showOnline = checked, readReceipts = binding.switchReadReceipts.isChecked)
        }
        binding.switchReadReceipts.setOnCheckedChangeListener { _, checked ->
            if (!isBindingUser) savePrivacy(showOnline = binding.switchShowOnline.isChecked, readReceipts = checked)
        }
    }

    private fun loadUser() {
        val firebaseUser = authRepository.currentUser() ?: return
        lifecycleScope.launch {
            userRepository.getOrCreateUser(firebaseUser, LocaleHelper.getLanguage(this@AccountSettingsActivity))
                .onSuccess { user ->
                    isBindingUser = true
                    binding.etDisplayName.setText(user.displayName)
                    binding.etUsername.setText(user.username)
                    binding.etEmail.setText(user.email.ifBlank { firebaseUser.email.orEmpty() })
                    originalEmail = binding.etEmail.text?.toString().orEmpty()
                    binding.switchShowOnline.isChecked = user.showOnline
                    binding.switchReadReceipts.isChecked = user.readReceipts
                    isBindingUser = false
                }
                .onFailure { showMessage(it.localizedMessage ?: getString(R.string.error_generic)) }
        }
    }

    private fun saveAccount() {
        val uid = authRepository.currentUser()?.uid ?: return
        val displayName = binding.etDisplayName.text?.toString()?.trim().orEmpty()
        val username = binding.etUsername.text?.toString()?.trim().orEmpty().removePrefix("@")
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        if (displayName.isBlank() || username.isBlank() || email.isBlank()) {
            showMessage(getString(R.string.settings_required_fields))
            return
        }
        lifecycleScope.launch {
            authRepository.updateDisplayName(displayName)
            if (email != originalEmail) {
                authRepository.updateEmail(email).onFailure {
                    showMessage(getString(R.string.settings_reauth_needed))
                    return@launch
                }
            }
            userRepository.updateAccount(uid, displayName, username, email).onSuccess {
                originalEmail = email
                showMessage(getString(R.string.settings_saved))
            }.onFailure { showMessage(it.localizedMessage ?: getString(R.string.error_generic)) }
        }
    }

    private fun changePassword() {
        val newPassword = binding.etNewPassword.text?.toString().orEmpty()
        if (newPassword.length < 6) {
            showMessage(getString(R.string.password_too_short))
            return
        }
        lifecycleScope.launch {
            authRepository.updatePassword(newPassword).onSuccess {
                binding.etNewPassword.setText("")
                showMessage(getString(R.string.password_updated))
            }.onFailure { showMessage(getString(R.string.settings_reauth_needed)) }
        }
    }

    private fun savePrivacy(showOnline: Boolean, readReceipts: Boolean) {
        val uid = authRepository.currentUser()?.uid ?: return
        lifecycleScope.launch { userRepository.updatePrivacy(uid, showOnline, readReceipts) }
    }

    private fun confirmDeleteAccount() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_account_title)
            .setMessage(R.string.delete_account_message)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_delete_account) { _, _ ->
                if (authRepository.usesPasswordProvider()) showDeletePasswordDialog() else deleteAccountNow(null)
            }
            .show()
    }

    private fun showDeletePasswordDialog() {
        val inputLayout = TextInputLayout(this).apply {
            setPadding(32, 8, 32, 0)
            hint = getString(R.string.settings_current_password)
        }
        val passwordInput = TextInputEditText(inputLayout.context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        inputLayout.addView(passwordInput)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_account_confirm_identity)
            .setMessage(R.string.delete_account_password_message)
            .setView(inputLayout)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_confirm) { _, _ ->
                val password = passwordInput.text?.toString().orEmpty()
                if (password.isBlank()) {
                    showMessage(getString(R.string.settings_password_required))
                } else {
                    deleteAccountNow(password)
                }
            }
            .show()
    }

    /**
     * Antes de apagar dados no Firestore é necessário reautenticar o utilizador. A versão anterior
     * removed the profile before checking the recent-login requirement. If
     * Firebase Authentication still rejects deletion unexpectedly, recreate
     * the lightweight profile document so the user is not left with a broken
     * account.
     */
    private fun deleteAccountNow(password: String?) {
        val firebaseUser = authRepository.currentUser() ?: return
        val uid = firebaseUser.uid
        lifecycleScope.launch {
            if (!password.isNullOrBlank()) {
                authRepository.reauthenticateWithPassword(password).onFailure {
                    showMessage(getString(R.string.settings_invalid_password))
                    return@launch
                }
            }
            workoutPlanRepository.deletePlansOwnedBy(uid)
            userRepository.deleteUserDocument(uid).onFailure {
                showMessage(it.localizedMessage ?: getString(R.string.error_generic))
                return@launch
            }
            authRepository.deleteAccount().onSuccess {
                startActivity(Intent(this@AccountSettingsActivity, SplashLoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }.onFailure {
                authRepository.currentUser()?.let { currentUser ->
                    userRepository.getOrCreateUser(currentUser, LocaleHelper.getLanguage(this@AccountSettingsActivity))
                }
                showMessage(getString(R.string.settings_reauth_needed))
            }
        }
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}
