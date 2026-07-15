package com.example.fittrack.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fittrack.model.ExerciseEntry
import com.example.fittrack.model.WorkoutPlan
import com.example.fittrack.repository.AuthRepository
import com.example.fittrack.repository.WorkoutPlanRepository
import kotlinx.coroutines.launch

/**
 * ViewModel do detalhe de plano. Carrega o plano selecionado e os respetivos exercícios.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class PlanDetailViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val planRepository = WorkoutPlanRepository()

    private val _plan = MutableLiveData<WorkoutPlan?>()
    val plan: LiveData<WorkoutPlan?> = _plan

    private val _entries = MutableLiveData<List<ExerciseEntry>>(emptyList())
    val entries: LiveData<List<ExerciseEntry>> = _entries

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _importSuccess = MutableLiveData(false)
    val importSuccess: LiveData<Boolean> = _importSuccess

    fun loadPlan(planId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            planRepository.getPlan(planId)
                .onSuccess { _plan.value = it }
                .onFailure { _error.value = it.localizedMessage }

            planRepository.getExerciseEntries(planId)
                .onSuccess { _entries.value = it }
                .onFailure { _error.value = it.localizedMessage }

            _isLoading.value = false
        }
    }

    fun importPlan(planId: String) {
        viewModelScope.launch {
            planRepository.importPlan(planId)
                .onSuccess { _importSuccess.value = true }
                .onFailure { _error.value = it.localizedMessage }
        }
    }

    fun isOwner(): Boolean {
        val uid = authRepository.currentUser()?.uid ?: return false
        return _plan.value?.ownerUid == uid
    }

    fun clearError() { _error.value = null }
}