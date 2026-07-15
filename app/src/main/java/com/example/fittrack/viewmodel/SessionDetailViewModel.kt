package com.example.fittrack.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fittrack.model.Session
import com.example.fittrack.model.SetLog
import com.example.fittrack.repository.SessionRepository
import kotlinx.coroutines.launch

/**
 * ViewModel do detalhe de sessão. Lê uma sessão específica do histórico.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class SessionDetailViewModel : ViewModel() {

    private val sessionRepository = SessionRepository()

    private val _session = MutableLiveData<Session?>()
    val session: LiveData<Session?> = _session

    private val _setLogs = MutableLiveData<List<SetLog>>(emptyList())
    val setLogs: LiveData<List<SetLog>> = _setLogs

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun load(sessionId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            sessionRepository.getSession(sessionId).onSuccess { session ->
                _session.value = session
            }.onFailure { _error.value = it.localizedMessage }

            sessionRepository.getSetLogs(sessionId).onSuccess { logs ->
                _setLogs.value = logs
            }.onFailure { _error.value = it.localizedMessage }

            _isLoading.value = false
        }
    }

    fun clearError() { _error.value = null }
}