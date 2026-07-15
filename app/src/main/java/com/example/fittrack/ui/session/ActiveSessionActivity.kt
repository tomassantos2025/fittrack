package com.example.fittrack.ui.session

import android.app.Dialog
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.Window
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fittrack.R
import com.example.fittrack.databinding.ActivityActiveSessionBinding
import com.example.fittrack.ui.adapter.ExercisePickerAdapter
import com.example.fittrack.ui.adapter.SessionExerciseAdapter
import com.example.fittrack.utils.LocaleHelper
import com.example.fittrack.utils.SystemBars
import com.example.fittrack.viewmodel.ActiveSessionViewModel
import com.google.android.material.snackbar.Snackbar

/**
 * Activity da sessão de treino ativa. Permite registar séries, peso, repetições e terminar o treino.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class ActiveSessionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityActiveSessionBinding
    private val viewModel: ActiveSessionViewModel by viewModels()
    private lateinit var exerciseAdapter: SessionExerciseAdapter
    private var workoutStarted = false
    private var isResting = false
    private var activeExerciseIndex = 0
    private var activeSetIndex = 0
    private var restTimer: CountDownTimer? = null
    private var countdownTimer: CountDownTimer? = null
    private var restDurationSeconds = 60

    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(LocaleHelper.onAttach(newBase))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActiveSessionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SystemBars.applyTopInset(binding.toolbar)
        setupToolbar(); setupRecyclerView(); setupChronometer(); setupListeners(); observeViewModel(); loadSession(); handleBackPress()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { confirmExit() }
    }

    private fun setupChronometer() {
        viewModel.chronometerBase = SystemClock.elapsedRealtime()
        binding.chronometer.base = viewModel.chronometerBase
        binding.chronometer.stop()
        binding.chronometerSet.stop()
    }

    private fun setupRecyclerView() {
        exerciseAdapter = SessionExerciseAdapter(
            onAddSet = viewModel::addSet,
            onRemoveSet = viewModel::removeSet,
            onSetChanged = viewModel::updateSet
        )
        binding.rvExercises.apply { adapter = exerciseAdapter; layoutManager = LinearLayoutManager(this@ActiveSessionActivity); itemAnimator = null }
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                viewModel.moveExercise(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition); return true
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
            override fun isLongPressDragEnabled() = !workoutStarted
        }).attachToRecyclerView(binding.rvExercises)
    }

    private fun loadSession() {
        val planId = intent.getStringExtra("plan_id").orEmpty()
        if (planId.isNotEmpty()) viewModel.loadPlan(planId) else viewModel.startEmptySession()
    }

    private fun setupListeners() {
        binding.btnStartWorkout.setOnClickListener {
            if (viewModel.exercises.value.orEmpty().isEmpty()) Snackbar.make(binding.root, R.string.session_add_exercise_before_start, Snackbar.LENGTH_LONG).show()
            else startCountdown()
        }
        binding.btnFinishWorkout.setOnClickListener { confirmFinish() }
        binding.btnAddQuickExercise.setOnClickListener { showExercisePicker() }
        binding.btnRestDuration.setOnClickListener { chooseRestDuration() }
        binding.btnCompleteSet.setOnClickListener { completeCurrentSet() }
        binding.btnSkipRest.setOnClickListener { finishRestAndAdvance() }
    }

    private fun observeViewModel() {
        viewModel.exercises.observe(this) { exercises ->
            exerciseAdapter.submitList(exercises.toList())
            if (!workoutStarted) {
                binding.tvEmptyState.visibility = if (exercises.isEmpty()) View.VISIBLE else View.GONE
                binding.btnStartWorkout.isEnabled = exercises.isNotEmpty()
            }
        }
        viewModel.isLoading.observe(this) { binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE }
        viewModel.error.observe(this) { msg -> msg?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show(); viewModel.clearError() } }
        viewModel.sessionSaved.observe(this) { session -> session ?: return@observe; SessionSummaryDialogFragment.newInstance(session).show(supportFragmentManager, "session_summary") }
    }

    private fun chooseRestDuration() {
        val options = intArrayOf(45, 60, 90, 120)
        val labels = options.map { getString(R.string.rest_duration_seconds_format, it) }.toTypedArray()
        AlertDialog.Builder(this).setTitle(R.string.rest_duration_title)
            .setSingleChoiceItems(labels, options.indexOf(restDurationSeconds).coerceAtLeast(0)) { dialog, which ->
                restDurationSeconds = options[which]
                binding.btnRestDuration.text = getString(R.string.rest_duration_seconds_format, restDurationSeconds)
                dialog.dismiss()
            }.setNegativeButton(R.string.btn_cancel, null).show()
    }

    private fun showExercisePicker() {
        val exercises = viewModel.allExercises.value.orEmpty()
        if (exercises.isEmpty()) { Snackbar.make(binding.root, R.string.exercise_picker_empty, Snackbar.LENGTH_LONG).show(); viewModel.loadExercises(); return }
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val picker = com.example.fittrack.databinding.DialogAddExerciseBinding.inflate(layoutInflater)
        dialog.setContentView(picker.root)
        val adapter = ExercisePickerAdapter(showAddButton = true) { exercise ->
            viewModel.addQuickExercise(exercise)
            dialog.dismiss()
            showSmallAddLoading()
        }
        picker.rvExercises.layoutManager = LinearLayoutManager(this)
        picker.rvExercises.adapter = adapter
        adapter.submitList(exercises)
        picker.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString().orEmpty().trim().lowercase()
                adapter.submitList(if (query.isBlank()) exercises else exercises.filter { exercise ->
                    exercise.name.lowercase().contains(query) || exercise.description.lowercase().contains(query) || exercise.equipment.lowercase().contains(query) || exercise.muscleGroups.any { it.lowercase().contains(query) }
                })
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })
        picker.btnSuggestExercise.visibility = View.GONE
        picker.btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * .94f).toInt(), (resources.displayMetrics.heightPixels * .82f).toInt())
    }

    private fun showSmallAddLoading() {
        binding.progressAddExercise.visibility = View.VISIBLE
        binding.progressAddExercise.alpha = 0f
        binding.progressAddExercise.animate().alpha(1f).setDuration(120).withEndAction {
            binding.progressAddExercise.postDelayed({ binding.progressAddExercise.animate().alpha(0f).setDuration(160).withEndAction { binding.progressAddExercise.visibility = View.GONE }.start() }, 320)
        }.start()
    }

    private fun startCountdown() {
        binding.countdownOverlay.visibility = View.VISIBLE
        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(3200, 1000) {
            var count = 3
            override fun onTick(millisUntilFinished: Long) { binding.tvCountdown.text = count.toString(); binding.tvCountdown.scaleX = .72f; binding.tvCountdown.scaleY = .72f; binding.tvCountdown.animate().scaleX(1f).scaleY(1f).setDuration(260).start(); count-- }
            override fun onFinish() { binding.countdownOverlay.visibility = View.GONE; enterFocusedWorkout() }
        }.start()
    }

    private fun enterFocusedWorkout() {
        workoutStarted = true; activeExerciseIndex = 0; activeSetIndex = 0
        viewModel.chronometerBase = SystemClock.elapsedRealtime()
        binding.chronometer.base = viewModel.chronometerBase; binding.chronometer.start()
        binding.rvExercises.visibility = View.GONE; binding.tvConfigureHint.visibility = View.GONE; binding.tvEmptyState.visibility = View.GONE
        binding.btnAddQuickExercise.visibility = View.GONE; binding.progressAddExercise.visibility = View.GONE; binding.btnStartWorkout.visibility = View.GONE
        binding.btnRestDuration.visibility = View.GONE; binding.cardFocusedWorkout.visibility = View.VISIBLE; binding.btnFinishWorkout.visibility = View.VISIBLE
        renderCurrentSet()
    }

    private fun renderCurrentSet() {
        val exercises = viewModel.exercises.value.orEmpty()
        if (activeExerciseIndex !in exercises.indices) { renderWorkoutComplete(); return }
        val exercise = exercises[activeExerciseIndex]
        if (activeSetIndex !in exercise.setLogs.indices) { advanceToNextSet(); return }
        val set = exercise.setLogs[activeSetIndex]
        binding.tvActiveExercisePosition.text = getString(R.string.session_exercise_position, activeExerciseIndex + 1, exercises.size)
        binding.tvActiveExerciseName.text = exercise.entry.exerciseName
        binding.tvActiveSetProgress.text = getString(R.string.session_active_set_progress, activeSetIndex + 1, exercise.setLogs.size)
        binding.etActiveWeight.setText(if (set.weight > 0) set.weight.toString() else "")
        binding.etActiveReps.setText(if (set.reps > 0) set.reps.toString() else "")
        if (exercise.animationUrl.isNotBlank()) Glide.with(binding.ivActiveExerciseAnim).asGif().load(exercise.animationUrl).placeholder(R.drawable.ic_exercise_placeholder).into(binding.ivActiveExerciseAnim)
        else binding.ivActiveExerciseAnim.setImageResource(R.drawable.ic_exercise_placeholder)
        binding.btnCompleteSet.visibility = View.VISIBLE; binding.btnCompleteSet.isEnabled = true; binding.cardFocusedWorkout.alpha = 1f
        binding.chronometerSet.base = SystemClock.elapsedRealtime(); binding.chronometerSet.start()
    }

    private fun completeCurrentSet() {
        if (!workoutStarted || isResting) return
        val exercises = viewModel.exercises.value.orEmpty(); val exercise = exercises.getOrNull(activeExerciseIndex) ?: return
        if (activeSetIndex !in exercise.setLogs.indices) return
        val weight = binding.etActiveWeight.text.toString().replace(',', '.').toDoubleOrNull() ?: 0.0
        val reps = binding.etActiveReps.text.toString().toIntOrNull() ?: 0
        viewModel.updateSet(activeExerciseIndex, activeSetIndex, weight, reps)
        if (!exercise.setLogs[activeSetIndex].isCompleted) viewModel.toggleSetDone(activeExerciseIndex, activeSetIndex)
        binding.chronometerSet.stop()
        if (hasNextSet()) startRestTimer(restDurationSeconds) else renderWorkoutComplete()
    }

    private fun hasNextSet(): Boolean {
        val exercises = viewModel.exercises.value.orEmpty(); val current = exercises.getOrNull(activeExerciseIndex) ?: return false
        return activeSetIndex + 1 < current.setLogs.size || activeExerciseIndex + 1 < exercises.size
    }

    private fun advanceToNextSet() {
        val exercises = viewModel.exercises.value.orEmpty(); val current = exercises.getOrNull(activeExerciseIndex) ?: return renderWorkoutComplete()
        if (activeSetIndex + 1 < current.setLogs.size) activeSetIndex++ else { activeExerciseIndex++; activeSetIndex = 0 }
        if (activeExerciseIndex < exercises.size) renderCurrentSet() else renderWorkoutComplete()
    }

    private fun startRestTimer(seconds: Int) {
        isResting = true; restTimer?.cancel(); binding.cardRestTimer.visibility = View.VISIBLE; binding.cardRestTimer.bringToFront(); binding.cardFocusedWorkout.alpha = .28f; binding.btnCompleteSet.isEnabled = false
        restTimer = object : CountDownTimer(seconds * 1000L, 1000L) {
            override fun onTick(ms: Long) { val remain = (ms / 1000L).toInt().coerceAtLeast(0); binding.tvRestTimer.text = String.format("%02d:%02d", remain / 60, remain % 60); binding.progressRest.progress = (((seconds - remain) * 100f) / seconds).toInt() }
            override fun onFinish() { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75).startTone(ToneGenerator.TONE_PROP_BEEP, 450); finishRestAndAdvance() }
        }.start()
    }

    private fun finishRestAndAdvance() { if (!isResting) return; restTimer?.cancel(); isResting = false; binding.cardRestTimer.visibility = View.GONE; binding.progressRest.progress = 0; advanceToNextSet() }

    private fun renderWorkoutComplete() {
        isResting = false; restTimer?.cancel(); binding.cardRestTimer.visibility = View.GONE; binding.chronometerSet.stop()
        binding.tvActiveExercisePosition.text = getString(R.string.session_workout_complete_kicker)
        binding.tvActiveExerciseName.text = getString(R.string.session_workout_complete_title)
        binding.tvActiveSetProgress.text = getString(R.string.session_workout_complete_body)
        binding.ivActiveExerciseAnim.setImageResource(R.drawable.ic_plans)
        binding.activeWeightLayout.visibility = View.GONE; binding.activeRepsLayout.visibility = View.GONE; binding.btnCompleteSet.visibility = View.GONE
    }

    private fun confirmFinish() {
        AlertDialog.Builder(this).setTitle(R.string.btn_finish_session).setMessage(R.string.session_confirm_finish)
            .setPositiveButton(R.string.btn_confirm) { _, _ -> val elapsed = (SystemClock.elapsedRealtime() - binding.chronometer.base) / 1000; binding.chronometer.stop(); binding.chronometerSet.stop(); restTimer?.cancel(); viewModel.finishSession(elapsed) }
            .setNegativeButton(R.string.btn_cancel, null).show()
    }

    private fun confirmExit() { AlertDialog.Builder(this).setTitle(R.string.session_discard_title).setMessage(R.string.session_discard_message).setPositiveButton(R.string.session_discard_confirm) { _, _ -> finish() }.setNegativeButton(R.string.btn_cancel, null).show() }
    private fun handleBackPress() { onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) { override fun handleOnBackPressed() = confirmExit() }) }
    override fun onPause() { super.onPause(); if (workoutStarted) { binding.chronometer.stop(); binding.chronometerSet.stop() } }
    override fun onResume() { super.onResume(); if (workoutStarted && !isResting) { binding.chronometer.start(); binding.chronometerSet.start() } }
    override fun onDestroy() { super.onDestroy(); restTimer?.cancel(); countdownTimer?.cancel() }
}
