package com.example.fittrack.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fittrack.model.Session
import com.example.fittrack.repository.AuthRepository
import com.example.fittrack.repository.RemoteConfigRepository
import com.example.fittrack.repository.SessionRepository
import com.example.fittrack.repository.UserRepository
import kotlinx.coroutines.launch

/**
 * ViewModel do progresso. Prepara histórico, estatísticas e aplica as regras de visibilidade Free/Pro.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class ProgressViewModel : ViewModel() {

    private val sessionRepository = SessionRepository()
    private val remoteConfigRepository = RemoteConfigRepository()
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    private val _sessions = MutableLiveData<List<Session>>(emptyList())
    val sessions: LiveData<List<Session>> = _sessions

    private val _isPro = MutableLiveData(false)
    val isPro: LiveData<Boolean> = _isPro

    private val _showPaywall = MutableLiveData(false)
    val showPaywall: LiveData<Boolean> = _showPaywall

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _upgradeSuccess = MutableLiveData<Boolean>()
    val upgradeSuccess: LiveData<Boolean> = _upgradeSuccess

    /**
     * Carrega os dados de progresso de acordo com o estado da conta atual.
     * - Utilizadores Free recebem apenas os últimos N dias, definidos no Firebase Remote Config.
     * - Utilizadores Pro recebem o histórico completo de sessões.
     */
    fun loadData() {
        _isLoading.value = true
        viewModelScope.launch {
            val uid = authRepository.currentUser()?.uid
            val isProUser = uid?.let { userRepository.isCurrentUserPro(it) } == true
            val limitDays = remoteConfigRepository.historyDaysLimit().takeIf { it > 0 } ?: 7

            _isPro.value = isProUser
            _showPaywall.value = !isProUser

            val result = if (isProUser) {
                sessionRepository.getAllSessions()
            } else {
                sessionRepository.getSessionsSince(limitDays)
            }

            result
                .onSuccess { _sessions.value = it }
                .onFailure { _error.value = it.localizedMessage }

            _isLoading.value = false
        }
    }

    /**
     * Ativação Pro simplificada usada nesta versão.
     * Numa versão publicada na loja, antes de guardar is_pro=true seria necessário validar pagamento com Google Play Billing.
     */
    fun upgradeToPro() {
        val uid = authRepository.currentUser()?.uid ?: return
        viewModelScope.launch {
            userRepository.setProStatus(uid, true)
                .onSuccess {
                    _upgradeSuccess.value = true
                    loadData()
                }
                .onFailure { _error.value = it.localizedMessage }
        }
    }

    fun clearError() { _error.value = null }
}
