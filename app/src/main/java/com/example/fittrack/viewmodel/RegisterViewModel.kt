package com.example.fittrack.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fittrack.R
import com.example.fittrack.model.User
import com.example.fittrack.repository.AuthRepository
import com.example.fittrack.repository.UserRepository
import com.example.fittrack.utils.UiMessageException
import kotlinx.coroutines.launch

/**
 * ViewModel do registo. Cria conta, documento de utilizador e valida dados inseridos.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class RegisterViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    private val _registerResult = MutableLiveData<Result<Unit>>()
    val registerResult: LiveData<Result<Unit>> = _registerResult

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun register(displayName: String, username: String, email: String, password: String, confirmPassword: String) {
        // Validation
        if (displayName.isBlank()) {
            _registerResult.value = Result.failure(UiMessageException(R.string.register_display_name_required))
            return
        }
        if (username.isBlank()) {
            _registerResult.value = Result.failure(UiMessageException(R.string.register_username_required))
            return
        }
        if (!username.matches(Regex("^[\\p{L}\\p{N}._-]{3,20}$"))) {
            _registerResult.value = Result.failure(UiMessageException(R.string.register_username_invalid))
            return
        }
        if (email.isBlank()) {
            _registerResult.value = Result.failure(UiMessageException(R.string.register_email_required))
            return
        }
        if (password.length < 6) {
            _registerResult.value = Result.failure(UiMessageException(R.string.register_password_short))
            return
        }
        if (password != confirmPassword) {
            _registerResult.value = Result.failure(UiMessageException(R.string.register_password_mismatch))
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            val authResult = authRepository.register(email, password, displayName)
            authResult.onSuccess { firebaseUser ->
                // Create Firestore user document
                val user = User(
                    uid = firebaseUser.uid,
                    displayName = displayName,
                    username = username,
                    email = email,
                    isPro = false,
                    language = "en"
                )
                val createResult = userRepository.createUserDocument(firebaseUser.uid, user)
                _registerResult.value = createResult
            }.onFailure { e ->
                _registerResult.value = Result.failure(e)
            }
            _isLoading.value = false
        }
    }
}
