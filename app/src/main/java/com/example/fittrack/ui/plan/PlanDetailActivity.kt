package com.example.fittrack.ui.plan

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fittrack.R
import com.example.fittrack.databinding.ActivityPlanDetailBinding
import com.example.fittrack.ui.adapter.ExerciseEntryAdapter
import com.example.fittrack.ui.session.ActiveSessionActivity
import com.example.fittrack.viewmodel.PlanDetailViewModel
import com.google.android.material.snackbar.Snackbar

/**
 * Activity de detalhe de plano. Mostra exercícios, informação do plano e ações como iniciar treino ou editar.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class PlanDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlanDetailBinding
    private val viewModel: PlanDetailViewModel by viewModels()
    private lateinit var entryAdapter: ExerciseEntryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlanDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val planId = intent.getStringExtra("plan_id") ?: run { finish(); return }

        setupToolbar()
        setupRecyclerView()
        setupListeners(planId)
        observeViewModel()
        viewModel.loadPlan(planId)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        entryAdapter = ExerciseEntryAdapter()
        binding.rvExercises.adapter = entryAdapter
        binding.rvExercises.layoutManager = LinearLayoutManager(this)
    }

    private fun setupListeners(planId: String) {
        binding.btnStartPlan.setOnClickListener {
            val intent = Intent(this, ActiveSessionActivity::class.java)
            intent.putExtra("plan_id", planId)
            startActivity(intent)
        }

        binding.btnEdit.setOnClickListener {
            val intent = Intent(this, CreateEditPlanActivity::class.java)
            intent.putExtra("plan_id", planId)
            startActivity(intent)
        }

        binding.btnImport.setOnClickListener {
            viewModel.importPlan(planId)
        }
    }

    private fun observeViewModel() {
        viewModel.plan.observe(this) { plan ->
            plan ?: return@observe
            supportActionBar?.title = plan.name
            binding.tvDescription.text = plan.description
            binding.tvMuscleGroups.text = plan.muscleGroups.joinToString(", ")
            if (plan.sourcePlanId.isNotBlank()) {
                binding.tvImportedFrom.visibility = View.VISIBLE
                val source = plan.sourceOwnerUsername.ifBlank { plan.sourceOwnerName }
                binding.tvImportedFrom.text = getString(R.string.plan_imported_from, source.ifBlank { getString(R.string.unknown_creator) })
            } else {
                binding.tvImportedFrom.visibility = View.GONE
            }

            val isOwner = viewModel.isOwner()
            binding.btnEdit.visibility = if (isOwner) View.VISIBLE else View.GONE
            binding.btnImport.visibility = if (!isOwner) View.VISIBLE else View.GONE
            binding.btnStartPlan.visibility = if (isOwner) View.VISIBLE else View.GONE
        }

        viewModel.entries.observe(this) { entries ->
            entryAdapter.submitList(entries)
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { msg ->
            msg?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.importSuccess.observe(this) { success ->
            if (success) {
                Snackbar.make(binding.root, getString(R.string.plan_imported_success), Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}