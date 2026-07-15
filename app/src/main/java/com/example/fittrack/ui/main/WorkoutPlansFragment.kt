package com.example.fittrack.ui.main

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.fittrack.R
import com.example.fittrack.databinding.FragmentWorkoutPlansBinding
import com.example.fittrack.model.WorkoutPlan
import com.example.fittrack.ui.adapter.PlanCardAdapter
import com.example.fittrack.ui.plan.CreateEditPlanActivity
import com.example.fittrack.ui.PremiumInfoActivity
import com.example.fittrack.ui.plan.PlanDetailActivity
import com.example.fittrack.ui.social.FriendProfileActivity
import com.example.fittrack.ui.session.ActiveSessionActivity
import com.example.fittrack.viewmodel.SmartPlanGoal
import com.example.fittrack.viewmodel.WorkoutPlansViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.example.fittrack.utils.UiAnimations

/**
 * Mostra os planos pessoais e os planos da comunidade.
 * O botão Smart Planner aparece apenas na aba dos planos pessoais,
 * porque os planos gerados pertencem ao fluxo privado de planeamento do utilizador.
 */
class WorkoutPlansFragment : Fragment() {

    // Binding gerado a partir de fragment_workout_plans.xml.
    // É guardado como nullable para ser limpo em onDestroyView e evitar memory leaks em fragments.
    private var _binding: FragmentWorkoutPlansBinding? = null
    private val binding get() = _binding!!

    // ViewModel responsável por dados, regras Free/Pro e comunicação com Firebase.
    private val viewModel: WorkoutPlansViewModel by viewModels()

    // Adapter dos planos pessoais: permite abrir, iniciar, editar e apagar planos.
    private lateinit var myPlansAdapter: PlanCardAdapter

    // Adapter dos planos da comunidade: permite importar e ver o perfil do autor.
    private lateinit var communityAdapter: PlanCardAdapter

    // Estado local dos filtros. Estes valores são aplicados sempre que a lista muda.
    private var searchQuery = ""
    private var selectedMuscle = "All"

    /** Infla o layout do fragment e prepara o binding. */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWorkoutPlansBinding.inflate(inflater, container, false)
        return binding.root
    }

    /** Configura tabs, listas, filtros, botões e observadores depois da view existir. */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        UiAnimations.reveal(binding.root)
        setupTabs()
        setupAdapters()
        setupFilters()
        setupListeners()
        observeViewModel()
        viewModel.loadPlans()
    }

    /** Cria as duas abas principais: planos pessoais e comunidade. */
    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.plans_my_plans))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.plans_community))
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showMyPlans()
                    1 -> showCommunityPlans()
                }
                applyFilters()
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    /** Configura os adapters e define o que acontece quando o utilizador clica em cada card. */
    private fun setupAdapters() {
        // A mesma ação de abrir detalhe é usada tanto nos planos pessoais como nos da comunidade.
        val openDetail: (WorkoutPlan) -> Unit = { plan ->
            startActivity(Intent(requireContext(), PlanDetailActivity::class.java).putExtra("plan_id", plan.id))
        }
        // Só os planos pessoais podem ser iniciados diretamente como treino.
        val startPlan: (WorkoutPlan) -> Unit = { plan ->
            startActivity(Intent(requireContext(), ActiveSessionActivity::class.java).putExtra("plan_id", plan.id))
        }

        // Adapter da aba "Os meus planos". Tem ações completas porque os planos pertencem ao utilizador.
        myPlansAdapter = PlanCardAdapter(
            currentUserUid = viewModel.currentUid(),
            onPlanClick = openDetail,
            onStartClick = startPlan,
            onEditClick = { plan -> startActivity(Intent(requireContext(), CreateEditPlanActivity::class.java).putExtra("plan_id", plan.id)) },
            onDeleteClick = { plan -> confirmDeletePlan(plan) },
            onImportClick = null,
            onOwnerClick = null,
            myPlanIds = { viewModel.myPlanIds() },
            importedSourcePlanIds = { viewModel.importedSourcePlanIds() },
            communityMode = false
        )
        // Adapter da comunidade. Aqui não existe Smart Planner nem edição; apenas consulta/importação.
        communityAdapter = PlanCardAdapter(
            currentUserUid = viewModel.currentUid(),
            onPlanClick = openDetail,
            onStartClick = null,
            onEditClick = null,
            onDeleteClick = null,
            onImportClick = { plan -> viewModel.importPlan(plan.id) },
            onOwnerClick = { plan ->
                if (plan.ownerUid.isNotBlank()) startActivity(Intent(requireContext(), FriendProfileActivity::class.java).putExtra("user_uid", plan.ownerUid))
            },
            myPlanIds = { viewModel.myPlanIds() },
            importedSourcePlanIds = { viewModel.importedSourcePlanIds() },
            communityMode = true
        )
        binding.rvPlans.adapter = myPlansAdapter
    }

    /** Liga a pesquisa de texto. Sempre que o texto muda, a lista é filtrada de novo. */
    private fun setupFilters() {
        binding.etSearchPlans.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { searchQuery = s?.toString().orEmpty(); applyFilters() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })
    }

    /** Reconstrói os chips de filtros com base nos grupos musculares/exercícios existentes. */
    private fun buildChips() {
        val groups = mutableListOf("All", "Trending")
        (viewModel.myPlans.value.orEmpty() + viewModel.communityPlans.value.orEmpty())
            .flatMap { it.muscleGroups + it.exerciseNames }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .forEach { groups.add(it) }
        binding.chipGroupPlanMuscles.removeAllViews()
        groups.forEachIndexed { index, group ->
            val chip = Chip(requireContext()).apply {
                id = View.generateViewId()
                text = when (group) {
                    "All" -> getString(R.string.filter_all)
                    "Trending" -> getString(R.string.plan_filter_trending)
                    else -> group
                }
                isCheckable = true
                isChecked = group == selectedMuscle || (index == 0 && selectedMuscle == "All")
                setOnClickListener { selectedMuscle = group; applyFilters() }
            }
            binding.chipGroupPlanMuscles.addView(chip)
        }
    }

    /** Mostra a aba de planos pessoais e o cartão de planeamento Free/Pro. */
    private fun showMyPlans() {
        binding.rvPlans.adapter = myPlansAdapter
        binding.cardPlanLimit.visibility = View.VISIBLE
        binding.fabCreatePlan.show()
        updatePlannerCard()
    }

    /** Mostra apenas os planos públicos da comunidade. O Smart Planner fica escondido aqui de propósito. */
    private fun showCommunityPlans() {
        binding.rvPlans.adapter = communityAdapter
        binding.cardPlanLimit.visibility = View.GONE
        binding.fabCreatePlan.hide()
    }

    /** Liga os botões principais: criar plano e abrir/usar Smart Planner. */
    private fun setupListeners() {
        // No Free, este botão pode abrir o diálogo de limite em vez do formulário.
        binding.fabCreatePlan.setOnClickListener {
            if (viewModel.canAddMorePlans()) {
                startActivity(Intent(requireContext(), CreateEditPlanActivity::class.java))
            } else {
                showPlanLimitDialog()
            }
        }
        // O Smart Planner é Pro: Free é encaminhado para a comparação Free vs Pro.
        binding.btnSmartPlanner.setOnClickListener {
            if (viewModel.isPro.value == true) showSmartPlannerDialog() else startActivity(Intent(requireContext(), PremiumInfoActivity::class.java))
        }
    }

    /** Observa dados do ViewModel e atualiza a interface sempre que algo muda. */
    private fun observeViewModel() {
        viewModel.myPlans.observe(viewLifecycleOwner) { buildChips(); applyFilters(); updatePlannerCard() }
        viewModel.communityPlans.observe(viewLifecycleOwner) { buildChips(); applyFilters() }
        viewModel.isPro.observe(viewLifecycleOwner) { updatePlannerCard() }
        viewModel.isLoading.observe(viewLifecycleOwner) { loading -> binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE }
        viewModel.error.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                val text = when (it) {
                    "plan_imported" -> getString(R.string.plan_imported_success)
                    "plan_deleted" -> getString(R.string.plan_deleted_success)
                    "plan_limit_reached" -> getString(R.string.plan_limit_reached)
                    "smart_planner_locked" -> getString(R.string.smart_planner_locked)
                    "smart_plan_created" -> getString(R.string.smart_plan_created)
                    else -> it
                }
                Snackbar.make(binding.root, text, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    /**
     * Atualiza o cartão de planeamento consoante o tipo de conta.
     * Free vê quantos planos já usou e Pro vê a entrada para gerar planos automáticos.
     */
    private fun updatePlannerCard() {
        val isPro = viewModel.isPro.value == true
        val count = viewModel.myPlans.value.orEmpty().size
        binding.tvPlanLimitTitle.text = if (isPro) getString(R.string.smart_planner_title_pro) else getString(R.string.smart_planner_title_free)
        binding.tvPlanLimitBody.text = if (isPro) {
            getString(R.string.smart_planner_body_pro)
        } else {
            getString(R.string.smart_planner_body_free, count, WorkoutPlansViewModel.FREE_PLAN_LIMIT)
        }
        binding.btnSmartPlanner.text = if (isPro) getString(R.string.smart_planner_generate) else getString(R.string.smart_planner_locked_button)
    }

    /** Mostra o bloqueio quando o utilizador Free tenta ultrapassar o limite de planos. */
    private fun showPlanLimitDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.plan_limit_title)
            .setMessage(R.string.plan_limit_message)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_upgrade_pro) { _, _ -> startActivity(Intent(requireContext(), PremiumInfoActivity::class.java)) }
            .show()
    }

    /** Diálogo alternativo para explicar que o Smart Planner é uma funcionalidade Pro. */
    private fun showSmartPlannerLockedDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.smart_planner_locked_title)
            .setMessage(R.string.smart_planner_locked_message)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_upgrade_pro) { _, _ -> startActivity(Intent(requireContext(), PremiumInfoActivity::class.java)) }
            .show()
    }

    /** Mostra os objetivos disponíveis e envia a escolha para o ViewModel gerar o plano. */
    private fun showSmartPlannerDialog() {
        val goals = arrayOf(
            getString(R.string.smart_goal_strength),
            getString(R.string.smart_goal_hypertrophy),
            getString(R.string.smart_goal_fat_loss),
            getString(R.string.smart_goal_home)
        )
        val values = arrayOf(SmartPlanGoal.STRENGTH, SmartPlanGoal.HYPERTROPHY, SmartPlanGoal.FAT_LOSS, SmartPlanGoal.HOME)
        var selected = 0
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.smart_planner_dialog_title)
            .setSingleChoiceItems(goals, selected) { _, which -> selected = which }
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.smart_planner_generate) { _, _ -> viewModel.generateSmartPlan(values[selected]) }
            .show()
    }

    /**
     * Aplica pesquisa e filtro por chip à lista atualmente visível.
     * A fonte muda conforme a aba selecionada: planos pessoais ou comunidade.
     */
    private fun applyFilters() {
        val source = if (binding.tabLayout.selectedTabPosition == 0) viewModel.myPlans.value.orEmpty() else viewModel.communityPlans.value.orEmpty()
        val filtered = source.filter { plan ->
            val queryOk = searchQuery.isBlank() ||
                plan.name.contains(searchQuery, true) ||
                plan.description.contains(searchQuery, true) ||
                plan.ownerName.contains(searchQuery, true) ||
                plan.ownerUsername.contains(searchQuery, true) ||
                plan.exerciseNames.any { it.contains(searchQuery, true) }
            val filterOk = selectedMuscle == "All" || selectedMuscle == "Trending" ||
                plan.muscleGroups.any { it.equals(selectedMuscle, true) } ||
                plan.exerciseNames.any { it.equals(selectedMuscle, true) }
            queryOk && filterOk
        }.let { list ->
            if (selectedMuscle == "Trending" || binding.tabLayout.selectedTabPosition == 1) list.sortedByDescending { it.importCount } else list
        }
        if (binding.tabLayout.selectedTabPosition == 0) myPlansAdapter.submitList(filtered) else communityAdapter.submitList(filtered)
        binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    /** Confirma antes de apagar um plano, para evitar eliminações acidentais. */
    private fun confirmDeletePlan(plan: WorkoutPlan) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.plan_delete)
            .setMessage(getString(R.string.plan_delete_confirm, plan.name))
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.plan_delete) { _, _ -> viewModel.deletePlan(plan.id) }
            .show()
    }

    /** Ao regressar de outro ecrã, recarrega os planos para refletir alterações recentes. */
    override fun onResume() {
        super.onResume()
        viewModel.loadPlans()
    }

    /** Limpa o binding quando a view do fragment é destruída. */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
