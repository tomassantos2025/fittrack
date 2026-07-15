package com.example.fittrack.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fittrack.R
import com.example.fittrack.repository.AuthRepository
import com.example.fittrack.repository.UserRepository
import com.example.fittrack.utils.UiMessageException
import kotlinx.coroutines.launch

/**
 * ViewModel do login. Valida credenciais e comunica com o repositório de autenticação.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class LoginViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    private val _loginResult = MutableLiveData<Result<Unit>>()
    val loginResult: LiveData<Result<Unit>> = _loginResult

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _googleSignInResult = MutableLiveData<Result<Unit>>()
    val googleSignInResult: LiveData<Result<Unit>> = _googleSignInResult

    fun isLoggedIn(): Boolean = authRepository.isLoggedIn()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginResult.value = Result.failure(UiMessageException(R.string.login_fields_required))
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            val result = authRepository.login(email, password)
            _loginResult.value = result.fold(
                onSuccess = { firebaseUser -> userRepository.getOrCreateUser(firebaseUser).map { Unit } },
                onFailure = { Result.failure(it) }
            )
            _isLoading.value = false
        }
    }

    fun loginWithGoogle(idToken: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = authRepository.loginWithGoogle(idToken)
            _googleSignInResult.value = result.fold(
                onSuccess = { firebaseUser -> userRepository.getOrCreateUser(firebaseUser).map { Unit } },
                onFailure = { Result.failure(it) }
            )
            _isLoading.value = false
        }
    }
}
