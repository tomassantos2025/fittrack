package com.example.fittrack.ui.session

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.fittrack.R
import com.example.fittrack.databinding.DialogSessionSummaryBinding
import com.example.fittrack.model.Session
import com.example.fittrack.repository.AiSuggestionRepository
import com.example.fittrack.repository.AuthRepository
import com.example.fittrack.repository.UserRepository
import kotlinx.coroutines.launch

/**
 * Diálogo apresentado quando uma sessão de treino termina.
 * Mostra totais da sessão a todos os utilizadores e feedback AI Coach apenas a utilizadores Pro.
 */
class SessionSummaryDialogFragment : DialogFragment() {

    private var _binding: DialogSessionSummaryBinding? = null
    private val binding get() = _binding!!

    private val aiRepository = AiSuggestionRepository()
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    companion object {
        private const val ARG_SESSION_ID       = "session_id"
        private const val ARG_DURATION         = "duration"
        private const val ARG_VOLUME           = "volume"
        private const val ARG_SETS             = "sets"
        private const val ARG_PRS              = "prs"
        private const val ARG_PLAN_NAME        = "plan_name"
        private const val ARG_TOTAL_REPS       = "total_reps"
        private const val ARG_EXERCISES        = "exercises"

        fun newInstance(session: Session) = SessionSummaryDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_SESSION_ID,  session.id)
                putLong(ARG_DURATION,      session.durationSeconds)
                putDouble(ARG_VOLUME,      session.totalVolume)
                putInt(ARG_SETS,           session.totalSets)
                putInt(ARG_PRS,            session.personalRecords)
                putString(ARG_PLAN_NAME,   session.planName)
                putInt(ARG_TOTAL_REPS,     session.totalReps)
                putInt(ARG_EXERCISES,      session.exercisesCompleted)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSessionSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: android.os.Bundle?): android.app.Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false

        val args = requireArguments()
        val durationSeconds = args.getLong(ARG_DURATION)
        val volume          = args.getDouble(ARG_VOLUME)
        val sets            = args.getInt(ARG_SETS)
        val prs             = args.getInt(ARG_PRS)

        // Estatísticas principais do treino terminado.
        val mins = durationSeconds / 60
        val secs = durationSeconds % 60
        binding.tvDurationValue.text = "%d:%02d".format(mins, secs)
        binding.tvVolumeValue.text   = getString(R.string.kg_format, volume)
        binding.tvSetsValue.text     = sets.toString()
        binding.tvPrsValue.text      = prs.toString()

        // Sugestão AI: o estado Pro é lido no perfil Firestore do utilizador atual.
        binding.cardAiSuggestion.visibility = View.VISIBLE
        binding.tvAiSuggestion.text = getString(R.string.loading)

        val session = Session(
            id                 = args.getString(ARG_SESSION_ID, ""),
            durationSeconds    = durationSeconds,
            totalVolume        = volume,
            totalSets          = sets,
            totalReps          = args.getInt(ARG_TOTAL_REPS),
            exercisesCompleted = args.getInt(ARG_EXERCISES),
            personalRecords    = prs,
            planName           = args.getString(ARG_PLAN_NAME, "")
        )

        lifecycleScope.launch {
            val uid = authRepository.currentUser()?.uid
            val isProUser = uid?.let { userRepository.isCurrentUserPro(it) } == true

            if (!isProUser) {
                binding.tvAiSuggestion.text = getString(R.string.summary_ai_pro_only)
            } else {
                aiRepository.getSuggestion(session)
                    .onSuccess { suggestion -> binding.tvAiSuggestion.text = suggestion }
                    .onFailure { binding.tvAiSuggestion.text = getString(R.string.error_generic) }
            }
        }

        binding.btnDone.setOnClickListener {
            requireActivity().finishAffinity()
            startActivity(
                Intent(requireContext(), com.example.fittrack.MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}