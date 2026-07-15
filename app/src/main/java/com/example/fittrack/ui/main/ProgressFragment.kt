package com.example.fittrack.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fittrack.R
import com.example.fittrack.databinding.FragmentProgressBinding
import com.example.fittrack.model.Session
import com.example.fittrack.ui.adapter.SessionHistoryAdapter
import com.example.fittrack.utils.UiAnimations
import com.example.fittrack.viewmodel.ProgressViewModel
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.snackbar.Snackbar

/**
 * Fragment de progresso. Mostra métricas e histórico; a informação completa pode depender do estado Free/Pro.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class ProgressFragment : Fragment() {

    private var _binding: FragmentProgressBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProgressViewModel by viewModels()
    private lateinit var historyAdapter: SessionHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        UiAnimations.reveal(binding.root)
        setupTabs()
        setupRecycler()
        setupListeners()
        observeData()
        viewModel.loadData()
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.progress_tab_charts))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.progress_tab_history))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.progress_tab_records))
        showTab(0)
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) = showTab(tab.position)
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) = Unit
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) = Unit
        })
    }

    private fun showTab(position: Int) {
        binding.chartContainer.visibility = if (position == 0) View.VISIBLE else View.GONE
        binding.recyclerHistory.visibility = if (position == 1) View.VISIBLE else View.GONE
        binding.tvRecords.visibility = if (position == 2) View.VISIBLE else View.GONE
    }

    private fun setupRecycler() {
        historyAdapter = SessionHistoryAdapter { session ->
            val intent = Intent(requireContext(), com.example.fittrack.ui.session.SessionDetailActivity::class.java)
            intent.putExtra("session_id", session.id)
            startActivity(intent)
        }

        binding.recyclerHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }

    /**
     * O ecrã de progresso já não é a funcionalidade paga principal.
     * Mesmo assim mostra o estado Free/Pro porque utilizadores Pro podem consultar estatísticas completas,
     * enquanto utilizadores Free ficam limitados ao período configurado no Remote Config.
     */
    private fun setupListeners() {
        binding.btnUpgradePro.setOnClickListener { viewModel.upgradeToPro() }
    }

    private fun observeData() {
        viewModel.sessions.observe(viewLifecycleOwner) { sessions ->
            historyAdapter.submitList(sessions)
            renderChart(sessions)
            renderRecords(sessions)
            binding.tvNoData.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isPro.observe(viewLifecycleOwner) { isPro ->
            binding.tvProgressPlan.text = if (isPro) getString(R.string.progress_plan_pro) else getString(R.string.progress_plan_free)
        }

        viewModel.showPaywall.observe(viewLifecycleOwner) { show ->
            binding.proPaywallCard.visibility = if (show) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.upgradeSuccess.observe(viewLifecycleOwner) { success ->
            if (success == true) Snackbar.make(binding.root, getString(R.string.pro_upgrade_success), Snackbar.LENGTH_LONG).show()
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    /**
     * Desenha um gráfico simples de volume de treino a partir das sessões disponíveis.
     * O ViewModel decide se a lista vem limitada (Free) ou completa (Pro).
     */
    private fun renderChart(sessions: List<Session>) {
        if (sessions.isEmpty()) {
            binding.lineChart.clear()
            return
        }
        val ordered = sessions.sortedBy { it.completedAt?.time ?: 0L }
        val entries = ordered.mapIndexed { index, session -> Entry(index.toFloat(), session.totalVolume.toFloat()) }
        val dataSet = LineDataSet(entries, getString(R.string.progress_volume_chart_title)).apply {
            setDrawValues(false)
            setDrawCircles(true)
            lineWidth = 2f
        }
        binding.lineChart.data = LineData(dataSet)
        binding.lineChart.description.isEnabled = false
        binding.lineChart.invalidate()
    }

    private fun renderRecords(sessions: List<Session>) {
        val prs = sessions.sumOf { it.personalRecords }
        val volume = sessions.sumOf { it.totalVolume }
        val minutes = sessions.sumOf { it.durationSeconds } / 60
        binding.tvRecords.text = getString(R.string.progress_records_summary, sessions.size, volume, minutes, prs)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
