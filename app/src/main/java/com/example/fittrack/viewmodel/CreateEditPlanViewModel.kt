package com.example.fittrack.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fittrack.model.Exercise
import com.example.fittrack.model.ExerciseEntry
import com.example.fittrack.model.WorkoutPlan
import com.example.fittrack.repository.ExerciseRepository
import com.example.fittrack.repository.WorkoutPlanRepository
import kotlinx.coroutines.launch

/**
 * ViewModel de criação/edição de planos. Prepara dados do formulário e guarda alterações no Firebase.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class CreateEditPlanViewModel : ViewModel() {
    private val planRepository = WorkoutPlanRepository()
    private val exerciseRepository = ExerciseRepository()
    private val _plan = MutableLiveData<WorkoutPlan?>()
    val plan: LiveData<WorkoutPlan?> = _plan
    private val _entries = MutableLiveData<List<ExerciseEntry>>(emptyList())
    val entries: LiveData<List<ExerciseEntry>> = _entries
    private val _allExercises = MutableLiveData<List<Exercise>>(emptyList())
    val allExercises: LiveData<List<Exercise>> = _allExercises
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    private val _saveSuccess = MutableLiveData(false)
    val saveSuccess: LiveData<Boolean> = _saveSuccess
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    private var editingPlanId = ""

    fun loadPlan(planId: String) {
        editingPlanId = planId
        _isLoading.value = true
        viewModelScope.launch {
            planRepository.getPlan(planId).onSuccess { _plan.value = it }.onFailure { _error.value = it.localizedMessage }
            planRepository.getExerciseEntries(planId).onSuccess { _entries.value = it }.onFailure { _error.value = it.localizedMessage }
            _isLoading.value = false
        }
    }

    fun loadExercises() {
        if (_allExercises.value?.isNotEmpty() == true) return
        viewModelScope.launch { exerciseRepository.getExercises().onSuccess { _allExercises.value = it }.onFailure { _error.value = it.localizedMessage } }
    }

    fun addExercise(exercise: Exercise) {
        val current = _entries.value?.toMutableList() ?: mutableListOf()
        current.add(ExerciseEntry(exerciseId = exercise.id, exerciseName = exercise.name, targetSets = 3, targetReps = 10, targetWeight = 0.0, order = current.size))
        _entries.value = current
    }

    fun removeEntry(position: Int) {
        val current = _entries.value?.toMutableList() ?: return
        if (position < current.size) { current.removeAt(position); current.forEachIndexed { i, e -> current[i] = e.copy(order = i) }; _entries.value = current }
    }

    fun moveEntry(fromPosition: Int, toPosition: Int) {
        val current = _entries.value?.toMutableList() ?: return
        if (fromPosition !in current.indices || toPosition !in current.indices || fromPosition == toPosition) return
        val moved = current.removeAt(fromPosition); current.add(toPosition, moved); current.forEachIndexed { i, e -> current[i] = e.copy(order = i) }; _entries.value = current
    }

    fun updateEntry(position: Int, entry: ExerciseEntry) {
        val current = _entries.value?.toMutableList() ?: return
        if (position < current.size) { current[position] = entry; _entries.value = current }
    }

    fun savePlan(name: String, description: String, isPublic: Boolean) {
        if (name.isBlank()) { _error.value = "Plan name cannot be empty."; return }
        _isLoading.value = true
        val exerciseById = _allExercises.value.orEmpty().associateBy { it.id }
        val muscleGroups = _entries.value?.flatMap { exerciseById[it.exerciseId]?.muscleGroups ?: emptyList() }?.distinct() ?: emptyList()
        val existing = _plan.value
        val plan = (existing ?: WorkoutPlan()).copy(
            id = editingPlanId,
            name = name,
            description = description,
            isPublic = if (existing?.sourcePlanId?.isNotBlank() == true) false else isPublic,
            muscleGroups = muscleGroups
        )
        viewModelScope.launch {
            planRepository.savePlan(plan, _entries.value ?: emptyList()).onSuccess { _saveSuccess.value = true }.onFailure { _error.value = it.localizedMessage }
            _isLoading.value = false
        }
    }

    fun clearError() { _error.value = null }
}
