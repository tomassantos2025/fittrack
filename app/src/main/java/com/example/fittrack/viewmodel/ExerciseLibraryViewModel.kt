package com.example.fittrack.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fittrack.model.Exercise
import com.example.fittrack.repository.ExerciseRepository
import kotlinx.coroutines.launch

/**
 * ViewModel da biblioteca. Carrega exercícios e aplica pesquisa/filtros para a UI.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class ExerciseLibraryViewModel : ViewModel() {
    private val repository = ExerciseRepository()

    private val _exercises = MutableLiveData<List<Exercise>>(emptyList())
    val exercises: LiveData<List<Exercise>> = _exercises

    private val _filteredExercises = MutableLiveData<List<Exercise>>(emptyList())
    val filteredExercises: LiveData<List<Exercise>> = _filteredExercises

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var lastQuery = ""
    private var lastMuscle = "All"

    fun loadExercises() {
        _isLoading.value = true
        viewModelScope.launch {
            repository.getExercises()
                .onSuccess {
                    _exercises.value = it
                    applyFilters()
                }
                .onFailure { _error.value = it.localizedMessage }
            _isLoading.value = false
        }
    }

    fun setQuery(query: String) {
        lastQuery = query.trim()
        applyFilters()
    }

    fun setMuscleGroup(group: String) {
        lastMuscle = group
        applyFilters()
    }

    fun muscleGroups(): List<String> = listOf("All") + _exercises.value.orEmpty()
        .flatMap { it.muscleGroups }
        .distinct()
        .sorted()

    private fun applyFilters() {
        val source = _exercises.value.orEmpty()
        val query = lastQuery.lowercase()
        _filteredExercises.value = source.filter { exercise ->
            val matchesMuscle = lastMuscle == "All" || exercise.muscleGroups.any { it.equals(lastMuscle, ignoreCase = true) }
            val matchesQuery = query.isBlank() ||
                exercise.name.lowercase().contains(query) ||
                exercise.description.lowercase().contains(query) ||
                exercise.equipment.lowercase().contains(query) ||
                exercise.difficulty.lowercase().contains(query) ||
                exercise.muscleGroups.any { it.lowercase().contains(query) }
            matchesMuscle && matchesQuery
        }
    }

    fun clearError() { _error.value = null }
}
