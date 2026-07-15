package com.example.fittrack.ui.plan

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.Window
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fittrack.R
import com.example.fittrack.databinding.ActivityCreateEditPlanBinding
import com.example.fittrack.ui.adapter.ExerciseEntryAdapter
import com.example.fittrack.ui.adapter.ExercisePickerAdapter
import com.example.fittrack.viewmodel.CreateEditPlanViewModel
import com.google.android.material.snackbar.Snackbar

/**
 * Activity para criar ou editar um plano pessoal. Recebe um id opcional quando o utilizador está a editar um plano existente.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class CreateEditPlanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateEditPlanBinding
    private val viewModel: CreateEditPlanViewModel by viewModels()
    private lateinit var entryAdapter: ExerciseEntryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateEditPlanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val planId = intent.getStringExtra("plan_id")

        setupToolbar(planId)
        setupAdapters()
        setupListeners()
        observeViewModel()

        if (planId != null) {
            viewModel.loadPlan(planId)
        }
    }

    private fun setupToolbar(planId: String?) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (planId != null)
            getString(R.string.edit_plan_title) else getString(R.string.create_plan_title)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupAdapters() {
        entryAdapter = ExerciseEntryAdapter(
            editable = true,
            onRemove = { position -> viewModel.removeEntry(position) },
            onChanged = { position, entry -> viewModel.updateEntry(position, entry) }
        )
        binding.rvEntries.adapter = entryAdapter
        binding.rvEntries.layoutManager = LinearLayoutManager(this)
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                viewModel.moveEntry(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
                return true
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
            override fun isLongPressDragEnabled(): Boolean = true
        }).attachToRecyclerView(binding.rvEntries)
    }

    private fun setupListeners() {
        binding.btnAddExercise.setOnClickListener {
            showExercisePicker()
        }

        binding.btnSavePlan.setOnClickListener {
            val name = binding.etPlanName.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            val isPublic = binding.switchPublic.isChecked
            viewModel.savePlan(name, description, isPublic)
        }
    }

    private fun showExercisePicker() {
        val exercises = viewModel.allExercises.value.orEmpty()
        if (exercises.isEmpty()) {
            Snackbar.make(binding.root, R.string.exercise_picker_empty, Snackbar.LENGTH_LONG).show()
            viewModel.loadExercises()
            return
        }

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val pickerBinding = com.example.fittrack.databinding.DialogAddExerciseBinding.inflate(layoutInflater)
        dialog.setContentView(pickerBinding.root)

        val adapter = ExercisePickerAdapter(showAddButton = true, onAdd = { exercise ->
            viewModel.addExercise(exercise)
            Snackbar.make(binding.root, getString(R.string.exercise_added_format, exercise.name), Snackbar.LENGTH_SHORT).show()
        })
        pickerBinding.rvExercises.layoutManager = LinearLayoutManager(this)
        pickerBinding.rvExercises.adapter = adapter
        adapter.submitList(exercises)

        pickerBinding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString().orEmpty().trim().lowercase()
                val filtered = if (query.isBlank()) exercises else exercises.filter { exercise ->
                    exercise.name.lowercase().contains(query) ||
                        exercise.description.lowercase().contains(query) ||
                        exercise.equipment.lowercase().contains(query) ||
                        exercise.muscleGroups.any { it.lowercase().contains(query) }
                }
                adapter.submitList(filtered)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })
        pickerBinding.btnSuggestExercise.setOnClickListener { openSuggestionEmail() }
        pickerBinding.btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94f).toInt(),
            (resources.displayMetrics.heightPixels * 0.82f).toInt()
        )
    }

    private fun openSuggestionEmail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("tomassantos142@outlook.pt"))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.exercise_suggestion_email_subject))
            putExtra(Intent.EXTRA_TEXT, getString(R.string.exercise_suggestion_email_body))
        }
        startActivity(Intent.createChooser(intent, getString(R.string.exercise_suggestion_title)))
    }

    private fun observeViewModel() {
        viewModel.plan.observe(this) { plan ->
            plan ?: return@observe
            binding.etPlanName.setText(plan.name)
            binding.etDescription.setText(plan.description)
            val importedCopy = plan.sourcePlanId.isNotBlank()
            binding.switchPublic.isChecked = plan.isPublic && !importedCopy
            binding.switchPublic.isEnabled = !importedCopy
            binding.tvImportedRestriction.visibility = if (importedCopy) View.VISIBLE else View.GONE
        }

        viewModel.entries.observe(this) { entries ->
            entryAdapter.submitList(entries.toList())
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnSavePlan.isEnabled = !loading
        }

        viewModel.saveSuccess.observe(this) { success ->
            if (success) finish()
        }

        viewModel.error.observe(this) { msg ->
            msg?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.loadExercises()
    }
}