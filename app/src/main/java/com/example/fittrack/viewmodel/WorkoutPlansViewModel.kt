package com.example.fittrack.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fittrack.model.Exercise
import com.example.fittrack.model.ExerciseEntry
import com.example.fittrack.model.WorkoutPlan
import com.example.fittrack.repository.AuthRepository
import com.example.fittrack.repository.ExerciseRepository
import com.example.fittrack.repository.UserRepository
import com.example.fittrack.repository.WorkoutPlanRepository
import kotlinx.coroutines.launch

/**
 * Controla o ecrã de planos de treino e contém a funcionalidade Premium principal.
 *
 * Modelo de negócio usado na app:
 * - Utilizadores Free podem criar/importar no máximo FREE_PLAN_LIMIT planos pessoais.
 * - Utilizadores Pro têm planos ilimitados e podem gerar planos automaticamente com o Smart Planner.
 */
class WorkoutPlansViewModel : ViewModel() {
    companion object {
        // O limite deixa o utilizador experimentar a app, mas cria uma razão real para pagar se usar planos com frequência.
        const val FREE_PLAN_LIMIT = 2
    }

    // Repositório de autenticação: usado apenas para saber qual é o utilizador atual.
    private val authRepository = AuthRepository()

    // Repositório dos planos: concentra toda a leitura/escrita de planos no Firebase.
    private val planRepository = WorkoutPlanRepository()

    // Repositório do utilizador: usado para saber se o perfil atual tem is_pro = true.
    private val userRepository = UserRepository()

    // Repositório dos exercícios: fornece os exercícios que o Smart Planner usa para montar planos.
    private val exerciseRepository = ExerciseRepository()

    // Lista privada do utilizador autenticado. Só deve ser alterada dentro do ViewModel.
    private val _myPlans = MutableLiveData<List<WorkoutPlan>>(emptyList())
    val myPlans: LiveData<List<WorkoutPlan>> = _myPlans

    // Lista pública de planos da comunidade, apresentada numa aba separada.
    private val _communityPlans = MutableLiveData<List<WorkoutPlan>>(emptyList())
    val communityPlans: LiveData<List<WorkoutPlan>> = _communityPlans

    // Controla o spinner/progress bar da interface durante operações assíncronas.
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Estado Premium do utilizador atual. A UI observa este valor para mostrar/bloquear funcionalidades.
    private val _isPro = MutableLiveData(false)
    val isPro: LiveData<Boolean> = _isPro

    // Canal simples de mensagens para a UI: erros, sucesso de importação, bloqueio Premium, etc.
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    /** Devolve o uid do utilizador autenticado. Se não houver sessão, devolve string vazia. */
    fun currentUid() = authRepository.currentUser()?.uid ?: ""

    /** IDs dos planos pessoais, usado para marcar na comunidade quais já pertencem ao utilizador. */
    fun myPlanIds(): Set<String> = _myPlans.value.orEmpty().map { it.id }.toSet()

    /** IDs originais de planos importados, usado para impedir importações repetidas. */
    fun importedSourcePlanIds(): Set<String> = _myPlans.value.orEmpty().mapNotNull { it.sourcePlanId.takeIf { sourceId -> sourceId.isNotBlank() } }.toSet()

    /** Regra principal Free/Pro: Free só pode ter FREE_PLAN_LIMIT planos; Pro não tem limite. */
    fun canAddMorePlans(): Boolean = _isPro.value == true || _myPlans.value.orEmpty().size < FREE_PLAN_LIMIT

    /** Número de planos que o utilizador Free ainda pode criar/importar antes do bloqueio. */
    fun remainingFreePlans(): Int = (FREE_PLAN_LIMIT - _myPlans.value.orEmpty().size).coerceAtLeast(0)

    /**
     * Lê os planos do utilizador autenticado, os planos públicos da comunidade e o estado Pro.
     * O estado Pro é guardado por utilizador no Firestore: users/{uid}/is_pro.
     */
    fun loadPlans() {
        _isLoading.value = true
        viewModelScope.launch {
            val uid = currentUid()

            // Primeiro lemos o estado Pro, porque ele influencia a forma como a UI mostra os botões/cartões.
            if (uid.isNotBlank()) {
                _isPro.value = userRepository.isCurrentUserPro(uid)
            }

            // Planos pessoais: são privados do utilizador e contam para o limite Free.
            planRepository.getMyPlans().onSuccess { _myPlans.value = it }.onFailure { _error.value = it.localizedMessage }

            // Planos da comunidade: são planos públicos criados por outros utilizadores ou pelo próprio.
            planRepository.getCommunityPlans().onSuccess { _communityPlans.value = it }.onFailure { _error.value = it.localizedMessage }
            _isLoading.value = false
        }
    }

    /**
     * Importa um plano público da comunidade para a lista pessoal do utilizador.
     * A importação também conta para o limite Free, para a limitação ter impacto real.
     */
    fun importPlan(planId: String) = viewModelScope.launch {
        if (!canAddMorePlans()) {
            _error.value = "plan_limit_reached"
            return@launch
        }
        planRepository.importPlan(planId).onSuccess { _error.value = "plan_imported"; loadPlans() }.onFailure { _error.value = it.localizedMessage }
    }

    /**
     * O Smart Planner é a funcionalidade Pro principal.
     * Cria um plano privado e editável a partir dos exercícios já existentes na aplicação.
     */
    fun generateSmartPlan(goal: SmartPlanGoal) = viewModelScope.launch {
        if (_isPro.value != true) {
            _error.value = "smart_planner_locked"
            return@launch
        }
        _isLoading.value = true
        try {
            val exercises = exerciseRepository.getExercises().getOrThrow()
            val selected = selectExercisesForGoal(exercises, goal)
            if (selected.isEmpty()) {
                _error.value = "No exercises available to generate a plan."
                return@launch
            }
            val entries = selected.mapIndexed { index, exercise ->
                ExerciseEntry(
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    targetSets = if (goal == SmartPlanGoal.STRENGTH) 5 else 3,
                    targetReps = when (goal) {
                        SmartPlanGoal.STRENGTH -> 5
                        SmartPlanGoal.FAT_LOSS -> 14
                        SmartPlanGoal.HOME -> 12
                        SmartPlanGoal.HYPERTROPHY -> 10
                    },
                    targetWeight = 0.0,
                    order = index,
                    notes = "Gerado pelo Smart Planner"
                )
            }
            val plan = WorkoutPlan(
                name = goal.planName,
                description = goal.description,
                isPublic = false,
                muscleGroups = selected.flatMap { it.muscleGroups }.distinct(),
                exerciseNames = selected.map { it.name }.distinct(),
                exerciseCount = entries.size
            )
            planRepository.savePlan(plan, entries).onSuccess {
                _error.value = "smart_plan_created"
                loadPlans()
            }.onFailure { _error.value = it.localizedMessage }
        } catch (e: Exception) {
            _error.value = e.localizedMessage ?: "Não foi possível gerar o plano automático."
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Gerador simples baseado em regras.
     * Escolhe exercícios por grupo muscular/equipamento de acordo com o objetivo selecionado.
     */
    private fun selectExercisesForGoal(exercises: List<Exercise>, goal: SmartPlanGoal): List<Exercise> {
        // Cada objetivo privilegia grupos musculares diferentes.
        // Esta abordagem é simples, mas suficiente para demonstrar personalização automática.
        val preferredGroups = when (goal) {
            SmartPlanGoal.STRENGTH -> listOf("Chest", "Back", "Legs", "Shoulders")
            SmartPlanGoal.HYPERTROPHY -> listOf("Chest", "Back", "Biceps", "Triceps", "Legs")
            SmartPlanGoal.FAT_LOSS -> listOf("Legs", "Core", "Full Body", "Cardio")
            SmartPlanGoal.HOME -> listOf("Core", "Chest", "Legs", "Shoulders")
        }
        // Filtra exercícios que fazem sentido para o objetivo escolhido.
        // No objetivo HOME também verifica se o exercício é adequado para treino em casa.
        val filtered = exercises.filter { exercise ->
            val groupMatch = exercise.muscleGroups.any { group -> preferredGroups.any { pref -> group.contains(pref, ignoreCase = true) || pref.contains(group, ignoreCase = true) } }
            val homeMatch = goal != SmartPlanGoal.HOME || exercise.homeFriendly || exercise.equipment.contains("body", true) || exercise.equipment.isBlank()
            groupMatch && homeMatch
        }
        // Se o filtro não encontrar nada, usa o catálogo geral como fallback para evitar ecrã vazio.
        // distinctBy evita exercícios repetidos e take(6) mantém o plano curto e apresentável.
        return (filtered.ifEmpty { exercises }).distinctBy { it.id.ifBlank { it.name } }.take(6)
    }

    /** Apaga um plano pessoal e recarrega a lista para a interface ficar atualizada. */
    fun deletePlan(planId: String) = viewModelScope.launch {
        planRepository.deletePlan(planId).onSuccess { _error.value = "plan_deleted"; loadPlans() }.onFailure { _error.value = it.localizedMessage }
    }

    /** Limpa a última mensagem depois de a UI a mostrar ao utilizador. */
    fun clearError() { _error.value = null }
}

/**
 * Objetivos disponíveis no Smart Planner.
 * Cada opção define o nome e a descrição do plano criado automaticamente.
 */
enum class SmartPlanGoal(val planName: String, val description: String) {
    STRENGTH("Smart Strength Plan", "Plano Pro gerado para trabalhar força, com séries mais pesadas e progressão."),
    HYPERTROPHY("Smart Muscle Growth Plan", "Plano Pro gerado para hipertrofia, com volume equilibrado por grupo muscular."),
    FAT_LOSS("Smart Conditioning Plan", "Plano Pro gerado para condicionamento, com mais repetições e gasto calórico."),
    HOME("Smart Home Plan", "Plano Pro gerado com exercícios adequados para treino em casa.")
}
