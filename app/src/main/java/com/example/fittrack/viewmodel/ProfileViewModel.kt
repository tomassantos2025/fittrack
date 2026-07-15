package com.example.fittrack.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fittrack.model.User
import com.example.fittrack.repository.AuthRepository
import com.example.fittrack.repository.UserRepository
import kotlinx.coroutines.launch

/**
 * ViewModel do perfil. Carrega dados do utilizador e métricas associadas.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class ProfileViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    fun loadUser() {
        val firebaseUser = authRepository.currentUser() ?: return
        _isLoading.value = true
        viewModelScope.launch {
            userRepository.getOrCreateUser(firebaseUser)
                .onSuccess { _user.value = it }
                .onFailure { _message.value = it.localizedMessage }
            _isLoading.value = false
        }
    }

    fun uploadPhoto(uri: Uri) {
        val uid = authRepository.currentUser()?.uid ?: return
        _isLoading.value = true
        viewModelScope.launch {
            userRepository.uploadProfilePhoto(uid, uri)
                .onSuccess { url ->
                    _user.value = _user.value?.copy(photoUrl = url)
                }
                .onFailure { _message.value = it.localizedMessage }
            _isLoading.value = false
        }
    }

    fun saveLocalPhoto(localUri: String) {
        val uid = authRepository.currentUser()?.uid ?: return
        _isLoading.value = true
        viewModelScope.launch {
            userRepository.updateUser(uid, mapOf("photo_url" to localUri))
                .onSuccess {
                    _user.value = _user.value?.copy(photoUrl = localUri)
                    _message.value = "photo_updated"
                }
                .onFailure { _message.value = it.localizedMessage }
            _isLoading.value = false
        }
    }

    fun upgradeToPro() {
        val uid = authRepository.currentUser()?.uid ?: return
        _isLoading.value = true
        viewModelScope.launch {
            userRepository.setProStatus(uid, true)
                .onSuccess {
                    _user.value = _user.value?.copy(isPro = true)
                    _message.value = "pro_upgrade_success"
                }
                .onFailure { _message.value = it.localizedMessage }
            _isLoading.value = false
        }
    }

    fun revokePro() {
        val uid = authRepository.currentUser()?.uid ?: return
        _isLoading.value = true
        viewModelScope.launch {
            userRepository.setProStatus(uid, false)
                .onSuccess {
                    _user.value = _user.value?.copy(isPro = false)
                    _message.value = "pro_revoked"
                }
                .onFailure { _message.value = it.localizedMessage }
            _isLoading.value = false
        }
    }

    fun changeLanguage(languageCode: String) {
        val uid = authRepository.currentUser()?.uid ?: return
        viewModelScope.launch { userRepository.updateLanguage(uid, languageCode) }
    }

    fun setOnline(isOnline: Boolean) {
        val uid = authRepository.currentUser()?.uid ?: return
        viewModelScope.launch { userRepository.setOnlineStatus(uid, isOnline) }
    }

    fun logout() = authRepository.logout()

    fun clearMessage() { _message.value = null }
}
