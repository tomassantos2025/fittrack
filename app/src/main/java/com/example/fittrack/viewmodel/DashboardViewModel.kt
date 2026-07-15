package com.example.fittrack.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fittrack.model.Session
import com.example.fittrack.model.User
import com.example.fittrack.repository.AuthRepository
import com.example.fittrack.repository.SessionRepository
import com.example.fittrack.repository.UserRepository
import kotlinx.coroutines.launch

/**
 * ViewModel da dashboard. Calcula resumos semanais a partir das sessões e do estado do utilizador.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class DashboardViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()
    private val sessionRepository = SessionRepository()

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _weeklyStats = MutableLiveData<SessionRepository.WeeklyStats?>()
    val weeklyStats: LiveData<SessionRepository.WeeklyStats?> = _weeklyStats

    private val _recentSessions = MutableLiveData<List<Session>>(emptyList())
    val recentSessions: LiveData<List<Session>> = _recentSessions

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadData() {
        _isLoading.value = true
        viewModelScope.launch {
            val firebaseUser = authRepository.currentUser()
            if (firebaseUser == null) {
                _error.value = "Not logged in"
                _isLoading.value = false
                return@launch
            }

            userRepository.getOrCreateUser(firebaseUser).onSuccess { _user.value = it }
                .onFailure { _error.value = it.localizedMessage }

            val activityStats = userRepository.markTodayActive(firebaseUser.uid).getOrNull()
            sessionRepository.getWeeklyStats().onSuccess { sessionStats ->
                _weeklyStats.value = if (activityStats == null) sessionStats else sessionStats.copy(
                    activeDays = activityStats.activeDaysThisWeek,
                    currentStreak = activityStats.currentStreak
                )
            }.onFailure { _error.value = it.localizedMessage }

            sessionRepository.getRecentSessions(20).onSuccess { _recentSessions.value = it }
                .onFailure { _error.value = it.localizedMessage }

            _isLoading.value = false
        }
    }

    fun clearError() { _error.value = null }
}
