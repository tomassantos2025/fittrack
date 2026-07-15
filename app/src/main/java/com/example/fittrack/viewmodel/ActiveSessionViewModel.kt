package com.example.fittrack.viewmodel

import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fittrack.model.ExerciseEntry
import com.example.fittrack.model.Session
import com.example.fittrack.model.SetLog
import com.example.fittrack.model.Exercise
import com.example.fittrack.repository.ExerciseRepository
import com.example.fittrack.repository.SessionRepository
import com.example.fittrack.repository.WorkoutPlanRepository
import kotlinx.coroutines.launch

/**
 * ViewModel da sessão ativa. Guarda estado temporário da sessão e coordena gravação no repositório.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
data class SessionExercise(
    val entry: ExerciseEntry,
    val animationUrl: String = "",
    val setLogs: MutableList<SetLog> = mutableListOf()
)

class ActiveSessionViewModel : ViewModel() {

    private val sessionRepository = SessionRepository()
    private val planRepository = WorkoutPlanRepository()
    private val exerciseRepository = ExerciseRepository()

    private val _exercises = MutableLiveData<List<SessionExercise>>(emptyList())
    val exercises: LiveData<List<SessionExercise>> = _exercises

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _sessionSaved = MutableLiveData<Session?>()
    val sessionSaved: LiveData<Session?> = _sessionSaved

    private val _allExercises = MutableLiveData<List<Exercise>>(emptyList())
    val allExercises: LiveData<List<Exercise>> = _allExercises

    var planId: String = ""
    var planName: String = ""

    // Chronometer base (set once on start)
    var chronometerBase: Long = SystemClock.elapsedRealtime()

    fun loadPlan(planId: String) {
        if (planId.isEmpty() || _exercises.value?.isNotEmpty() == true) return
        this.planId = planId
        _isLoading.value = true

        viewModelScope.launch {
            val planResult = planRepository.getPlan(planId)
            if (planResult.isFailure) {
                _error.value = planResult.exceptionOrNull()?.localizedMessage
                _isLoading.value = false
                return@launch
            }
            planName = planResult.getOrNull()?.name.orEmpty()

            val entriesResult = planRepository.getExerciseEntries(planId)
            if (entriesResult.isFailure) {
                _error.value = entriesResult.exceptionOrNull()?.localizedMessage
                _isLoading.value = false
                return@launch
            }

            val catalogue = exerciseRepository.getExercises().getOrDefault(emptyList()).associateBy { it.id }
            _allExercises.value = catalogue.values.toList()
            val sessionExercises = entriesResult.getOrDefault(emptyList()).sortedBy { it.order }.map { entry ->
                val initialSets = (1..entry.targetSets).map { setNum ->
                    SetLog(
                        exerciseId = entry.exerciseId,
                        exerciseName = entry.exerciseName,
                        setNumber = setNum,
                        weight = entry.targetWeight,
                        reps = entry.targetReps,
                        exerciseOrder = entry.order
                    )
                }
                SessionExercise(
                    entry = entry,
                    animationUrl = catalogue[entry.exerciseId]?.animationUrl.orEmpty(),
                    setLogs = initialSets.toMutableList()
                )
            }
            _exercises.value = sessionExercises
            _isLoading.value = false
        }
    }

    fun startEmptySession() {
        // Quick-start with no plan
        planName = ""
        _exercises.value = emptyList()
        loadExercises()
    }

    fun loadExercises() {
        if (_allExercises.value?.isNotEmpty() == true) return
        viewModelScope.launch {
            exerciseRepository.getExercises()
                .onSuccess { _allExercises.value = it }
                .onFailure { _error.value = it.localizedMessage }
        }
    }

    fun addQuickExercise(exercise: Exercise) {
        val current = _exercises.value?.toMutableList() ?: mutableListOf()
        val entry = ExerciseEntry(
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            targetSets = 3,
            targetReps = 10,
            targetWeight = 0.0,
            order = current.size
        )
        val initialSets = (1..entry.targetSets).map { setNum ->
            SetLog(
                exerciseId = entry.exerciseId,
                exerciseName = entry.exerciseName,
                setNumber = setNum,
                weight = entry.targetWeight,
                reps = entry.targetReps,
                exerciseOrder = entry.order
            )
        }
        current.add(SessionExercise(entry = entry, animationUrl = exercise.animationUrl, setLogs = initialSets.toMutableList()))
        _exercises.value = current
    }

    fun moveExercise(fromPosition: Int, toPosition: Int) {
        val current = _exercises.value?.toMutableList() ?: return
        if (fromPosition !in current.indices || toPosition !in current.indices || fromPosition == toPosition) return
        val moved = current.removeAt(fromPosition)
        current.add(toPosition, moved)
        val reordered = current.mapIndexed { index, sessionExercise ->
            val updatedEntry = sessionExercise.entry.copy(order = index)
            val updatedSets = sessionExercise.setLogs.map { it.copy(exerciseOrder = index) }.toMutableList()
            sessionExercise.copy(entry = updatedEntry, setLogs = updatedSets)
        }
        _exercises.value = reordered
    }

    fun updateSet(exerciseIndex: Int, setIndex: Int, weight: Double, reps: Int) {
        val current = _exercises.value?.toMutableList() ?: return
        val exercise = current[exerciseIndex]
        val sets = exercise.setLogs.toMutableList()
        if (setIndex < sets.size) {
            sets[setIndex] = sets[setIndex].copy(weight = weight, reps = reps)
        }
        current[exerciseIndex] = exercise.copy(setLogs = sets)
        _exercises.value = current
    }

    fun toggleSetDone(exerciseIndex: Int, setIndex: Int) {
        val current = _exercises.value?.toMutableList() ?: return
        val exercise = current[exerciseIndex]
        val sets = exercise.setLogs.toMutableList()
        if (setIndex < sets.size) {
            sets[setIndex] = sets[setIndex].copy(isCompleted = !sets[setIndex].isCompleted)
        }
        current[exerciseIndex] = exercise.copy(setLogs = sets)
        _exercises.value = current
    }

    fun addSet(exerciseIndex: Int) {
        val current = _exercises.value?.toMutableList() ?: return
        val exercise = current[exerciseIndex]
        val sets = exercise.setLogs.toMutableList()
        val lastSet = sets.lastOrNull()
        sets.add(
            SetLog(
                exerciseId = exercise.entry.exerciseId,
                exerciseName = exercise.entry.exerciseName,
                setNumber = sets.size + 1,
                weight = lastSet?.weight ?: 0.0,
                reps = lastSet?.reps ?: exercise.entry.targetReps,
                exerciseOrder = exercise.entry.order
            )
        )
        current[exerciseIndex] = exercise.copy(setLogs = sets)
        _exercises.value = current
    }

    fun removeSet(exerciseIndex: Int, setIndex: Int) {
        val current = _exercises.value?.toMutableList() ?: return
        val exercise = current[exerciseIndex]
        val sets = exercise.setLogs.toMutableList()
        if (sets.size > 3 && setIndex in 3 until sets.size) {
            sets.removeAt(setIndex)
            // Re-number
            sets.forEachIndexed { i, s -> sets[i] = s.copy(setNumber = i + 1) }
        }
        current[exerciseIndex] = exercise.copy(setLogs = sets)
        _exercises.value = current
    }

    fun finishSession(durationSeconds: Long) {
        val exerciseList = _exercises.value ?: return
        val allLogs = exerciseList.flatMap { it.setLogs.filter { s -> s.isCompleted } }

        // Allow finishing even with no completed sets (quick workout / empty session)

        val totalVolume = allLogs.sumOf { it.weight * it.reps }
        val exerciseById = _allExercises.value.orEmpty().associateBy { it.id }
        val muscleGroups = exerciseList.flatMap { exercise ->
            exerciseById[exercise.entry.exerciseId]?.muscleGroups ?: emptyList()
        }.distinct()

        val session = Session(
            planId = planId,
            planName = planName,
            durationSeconds = durationSeconds,
            totalVolume = totalVolume,
            totalSets = allLogs.size,
            totalReps = allLogs.sumOf { it.reps },
            exercisesCompleted = exerciseList.count { ex -> ex.setLogs.any { it.isCompleted } },
            muscleGroups = muscleGroups,
            completedAt = java.util.Date()
        )

        viewModelScope.launch {
            sessionRepository.saveSession(session, allLogs).onSuccess { savedId ->
                _sessionSaved.value = session.copy(id = savedId)
            }.onFailure {
                _error.value = it.localizedMessage
            }
        }
    }

    fun clearError() { _error.value = null }
}