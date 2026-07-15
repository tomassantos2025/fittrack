package com.example.fittrack.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.fittrack.databinding.FragmentDashboardBinding
import com.example.fittrack.ui.adapter.SessionHistoryAdapter
import com.example.fittrack.ui.session.ActiveSessionActivity
import com.example.fittrack.ui.PremiumInfoActivity
import com.example.fittrack.viewmodel.DashboardViewModel
import com.google.android.material.snackbar.Snackbar
import com.example.fittrack.utils.UiAnimations

/**
 * Fragment da página inicial. Resume a atividade semanal e dá acesso rápido aos principais fluxos da app.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var sessionAdapter: SessionHistoryAdapter
    private var isProUser: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        UiAnimations.reveal(binding.root)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
        viewModel.loadData()
    }

    private fun setupRecyclerView() {
        sessionAdapter = SessionHistoryAdapter { session ->
            val intent = Intent(requireContext(),
                com.example.fittrack.ui.session.SessionDetailActivity::class.java)
            intent.putExtra("session_id", session.id)
            startActivity(intent)
        }
        binding.rvRecentSessions.adapter = sessionAdapter
    }

    private fun setupListeners() {
        binding.btnStartWorkout.setOnClickListener {
            startActivity(Intent(requireContext(), ActiveSessionActivity::class.java))
        }
        binding.btnPremiumCta.setOnClickListener {
            if (isProUser) {
                findNavController().navigate(com.example.fittrack.R.id.progressFragment)
            } else {
                startActivity(Intent(requireContext(), PremiumInfoActivity::class.java))
            }
        }
    }

    private fun observeViewModel() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.tvGreeting.text = getString(
                    com.example.fittrack.R.string.dashboard_greeting, it.displayName
                )
                binding.tvStreak.text = it.currentStreak.toString()
                isProUser = it.isPro
                binding.tvPremiumStatusTitle.text = if (it.isPro) "Smart Planner enabled" else "Training plan slots"
                binding.tvPremiumStatusBody.text = if (it.isPro) {
                    "Create unlimited plans and generate workout plans based on your goal."
                } else {
                    "Free includes 2 custom plans. Pro adds unlimited plans and Smart Planner."
                }
                binding.btnPremiumCta.text = if (it.isPro) "Open progress" else "View Pro"
            }
        }

        viewModel.weeklyStats.observe(viewLifecycleOwner) { stats ->
            stats?.let {
                binding.tvWeeklySessions.text = it.totalSessions.toString()
                binding.tvWeeklyVolume.text = getString(
                    com.example.fittrack.R.string.kg_format, it.totalVolume
                )
                binding.tvActiveDays.text = it.activeDays.toString()
                binding.tvStreak.text = it.currentStreak.toString()
            }
        }

        viewModel.recentSessions.observe(viewLifecycleOwner) { sessions ->
            sessionAdapter.submitList(sessions)
            binding.tvNoSessions.visibility =
                if (sessions.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    override fun onResume() { super.onResume(); viewModel.loadData() }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
