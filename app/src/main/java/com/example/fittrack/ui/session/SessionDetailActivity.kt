package com.example.fittrack.ui.session

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fittrack.R
import com.example.fittrack.databinding.ActivitySessionDetailBinding
import com.example.fittrack.model.SetLog
import com.example.fittrack.utils.LocaleHelper
import com.example.fittrack.viewmodel.SessionDetailViewModel
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Activity que mostra os detalhes de uma sessão já registada no histórico.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class SessionDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySessionDetailBinding
    private val viewModel: SessionDetailViewModel by viewModels()
    private lateinit var setLogAdapter: SetLogDetailAdapter

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySessionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()

        val sessionId = intent.getStringExtra("session_id") ?: run { finish(); return }
        viewModel.load(sessionId)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        setLogAdapter = SetLogDetailAdapter()
        binding.rvSetLogs.apply {
            adapter = setLogAdapter
            layoutManager = LinearLayoutManager(this@SessionDetailActivity)
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.session.observe(this) { session ->
            session ?: return@observe
            supportActionBar?.title = session.planName.ifEmpty { getString(R.string.session_detail_title) }

            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            binding.tvDate.text = session.completedAt?.let { dateFormat.format(it) } ?: "—"

            val mins = session.durationSeconds / 60
            val secs = session.durationSeconds % 60
            binding.tvDuration.text = "%d:%02d".format(mins, secs)

            binding.tvVolume.text = getString(R.string.kg_format, session.totalVolume)
            binding.tvSets.text = session.totalSets.toString()
            binding.tvExercises.text = session.exercisesCompleted.toString()
        }

        viewModel.setLogs.observe(this) { logs ->
            setLogAdapter.submitList(logs)
            binding.tvEmptyLogs.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { msg ->
            msg?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }
}

// ─── Inline adapter for set logs in detail view ───────────────────────────────

class SetLogDetailAdapter : ListAdapter<SetLog, SetLogDetailAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_set_detail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvExercise: TextView = view.findViewById(R.id.tvExerciseName)
        private val tvSet: TextView = view.findViewById(R.id.tvSetNumber)
        private val tvWeight: TextView = view.findViewById(R.id.tvWeight)
        private val tvReps: TextView = view.findViewById(R.id.tvReps)
        private val tvPr: TextView = view.findViewById(R.id.tvPr)

        fun bind(log: SetLog) {
            tvExercise.text = log.exerciseName
            tvSet.text = itemView.context.getString(R.string.set_number_detail_format, log.setNumber)
            tvWeight.text = itemView.context.getString(R.string.kg_format, log.weight)
            tvReps.text = itemView.context.getString(R.string.reps_count_format, log.reps)
            tvPr.visibility = if (log.isPersonalRecord) View.VISIBLE else View.GONE
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SetLog>() {
        override fun areItemsTheSame(a: SetLog, b: SetLog) = a.id == b.id
        override fun areContentsTheSame(a: SetLog, b: SetLog) = a == b
    }
}