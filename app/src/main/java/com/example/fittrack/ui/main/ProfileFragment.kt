package com.example.fittrack.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.fittrack.R
import com.example.fittrack.databinding.FragmentProfileBinding
import com.example.fittrack.ui.AboutActivity
import com.example.fittrack.ui.AccountSettingsActivity
import com.example.fittrack.ui.HelpActivity
import com.example.fittrack.ui.ProfileMetricsActivity
import com.example.fittrack.ui.auth.SplashLoginActivity
import com.example.fittrack.viewmodel.ProfileViewModel
import com.google.android.material.snackbar.Snackbar
import java.io.File
import com.example.fittrack.utils.UiAnimations

/**
 * Fragment do perfil. Mostra dados da conta, métricas e acessos a definições.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { copyPhotoLocallyAndSave(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        UiAnimations.reveal(binding.root)
        setupListeners()
        observeViewModel()
        viewModel.loadUser()
    }

    override fun onResume() { super.onResume(); viewModel.loadUser(); viewModel.setOnline(true) }
    override fun onPause() { super.onPause(); viewModel.setOnline(false) }

    private fun setupListeners() {
        binding.btnSettings.setOnClickListener { startActivity(Intent(requireContext(), AccountSettingsActivity::class.java)) }
        binding.cardProfileHeader.setOnClickListener { startActivity(Intent(requireContext(), ProfileMetricsActivity::class.java)) }
        binding.btnViewProfileDetails.setOnClickListener { startActivity(Intent(requireContext(), ProfileMetricsActivity::class.java)) }
        binding.ivProfilePhoto.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnChangePhoto.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnUpgradePro.setOnClickListener { if (viewModel.user.value?.isPro == true) viewModel.revokePro() else viewModel.upgradeToPro() }
        binding.btnAbout.setOnClickListener { startActivity(Intent(requireContext(), AboutActivity::class.java)) }
        binding.btnHelp.setOnClickListener { startActivity(Intent(requireContext(), HelpActivity::class.java)) }
        binding.btnReportBug.setOnClickListener { startActivity(Intent(requireContext(), com.example.fittrack.ui.BugReportActivity::class.java)) }
        binding.btnLogout.setOnClickListener {
            viewModel.logout()
            startActivity(Intent(requireContext(), SplashLoginActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
        }
    }

    private fun copyPhotoLocallyAndSave(sourceUri: Uri) {
        try {
            val file = File(requireContext().filesDir, "profile_photo_current.jpg")
            requireContext().contentResolver.openInputStream(sourceUri)?.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
            val localUri = Uri.fromFile(file).toString()
            Glide.with(this).load(file).circleCrop().skipMemoryCache(true).into(binding.ivProfilePhoto)
            viewModel.saveLocalPhoto(localUri)
        } catch (e: Exception) {
            Snackbar.make(binding.root, e.localizedMessage ?: getString(R.string.photo_update_failed), Snackbar.LENGTH_LONG).show()
        }
    }

    private fun observeViewModel() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            user ?: return@observe
            binding.tvDisplayName.text = user.displayName.ifBlank { getString(R.string.profile_title) }
            binding.tvUsername.text = if (user.username.isNotBlank()) "@${user.username.removePrefix("@")}" else ""
            binding.tvEmail.text = user.email
            binding.tvOnlineBadge.visibility = if (user.showOnline) View.VISIBLE else View.GONE
            if (user.isPro) {
                binding.tvProBadge.visibility = View.VISIBLE
                binding.btnUpgradePro.text = getString(R.string.btn_revoke_pro)
                binding.btnUpgradePro.backgroundTintList = android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.error))
            } else {
                binding.tvProBadge.visibility = View.GONE
                binding.btnUpgradePro.text = getString(R.string.btn_upgrade_pro)
                binding.btnUpgradePro.backgroundTintList = android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.pro_gold))
            }
            if (user.photoUrl.isNotBlank()) Glide.with(this).load(Uri.parse(user.photoUrl)).circleCrop().skipMemoryCache(true).into(binding.ivProfilePhoto)
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE }
        viewModel.message.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                val text = when (it) {
                    "pro_upgrade_success" -> getString(R.string.pro_upgrade_success)
                    "pro_revoked" -> getString(R.string.pro_revoked)
                    "photo_updated" -> getString(R.string.photo_updated)
                    else -> it
                }
                Snackbar.make(binding.root, text, Snackbar.LENGTH_LONG).show(); viewModel.clearMessage()
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
