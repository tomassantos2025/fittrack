package com.example.fittrack.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fittrack.R
import com.example.fittrack.databinding.FragmentExerciseLibraryBinding
import com.example.fittrack.ui.adapter.ExercisePickerAdapter
import com.example.fittrack.viewmodel.ExerciseLibraryViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.example.fittrack.utils.UiAnimations

/**
 * Fragment da biblioteca de exercícios. Permite pesquisar e consultar exercícios disponíveis.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class ExerciseLibraryFragment : Fragment() {
    private var _binding: FragmentExerciseLibraryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ExerciseLibraryViewModel by viewModels()
    private lateinit var adapter: ExercisePickerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExerciseLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        UiAnimations.reveal(binding.root)
        adapter = ExercisePickerAdapter(showAddButton = false)
        binding.rvExercises.layoutManager = LinearLayoutManager(requireContext())
        binding.rvExercises.adapter = adapter

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = viewModel.setQuery(s?.toString().orEmpty())
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        binding.btnSuggestExercise.setOnClickListener { openSuggestionEmail(false) }

        viewModel.filteredExercises.observe(viewLifecycleOwner) { exercises ->
            adapter.submitList(exercises)
            binding.tvEmpty.visibility = if (exercises.isEmpty()) View.VISIBLE else View.GONE
            binding.tvCount.text = resources.getQuantityString(R.plurals.exercise_count, exercises.size, exercises.size)
        }
        viewModel.exercises.observe(viewLifecycleOwner) { buildChips() }
        viewModel.isLoading.observe(viewLifecycleOwner) { binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE }
        viewModel.error.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.loadExercises()
    }

    private fun buildChips() {
        binding.chipGroupMuscles.removeAllViews()
        viewModel.muscleGroups().forEachIndexed { index, group ->
            val chip = Chip(requireContext()).apply {
                id = View.generateViewId()
                text = if (group == "All") getString(R.string.filter_all) else group
                isCheckable = true
                isChecked = index == 0
                setOnClickListener { viewModel.setMuscleGroup(group) }
            }
            binding.chipGroupMuscles.addView(chip)
        }
    }

    private fun openSuggestionEmail(isBug: Boolean) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("tomassantos142@outlook.pt"))
            putExtra(Intent.EXTRA_SUBJECT, getString(if (isBug) R.string.bug_report_email_subject else R.string.exercise_suggestion_email_subject))
            putExtra(Intent.EXTRA_TEXT, getString(if (isBug) R.string.bug_report_email_body else R.string.exercise_suggestion_email_body))
        }
        startActivity(Intent.createChooser(intent, getString(if (isBug) R.string.btn_report_bug else R.string.exercise_suggestion_title)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
