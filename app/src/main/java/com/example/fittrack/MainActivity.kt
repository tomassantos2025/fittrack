package com.example.fittrack

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.fittrack.databinding.ActivityMainBinding
import com.example.fittrack.repository.SocialRepository
import com.example.fittrack.utils.LocaleHelper
import com.example.fittrack.utils.SystemBars
import com.example.fittrack.utils.UiAnimations
import com.google.firebase.firestore.ListenerRegistration

/**
 * Activity principal depois do login. Contém a navegação base da aplicação e gere os fragments principais: dashboard, planos, progresso, biblioteca, social e perfil.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val socialRepository = SocialRepository()
    private var socialBadgeListener: ListenerRegistration? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SystemBars.applyTopInset(binding.navHostFragment)
        SystemBars.applyBottomInset(binding.bottomNavigation)

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHost.navController
        setupBottomMenu()
        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateSelectedItem(destination.id)
            UiAnimations.reveal(binding.navHostFragment, 0L, 180L, 8f)
        }
        updateSelectedItem(navController.currentDestination?.id ?: R.id.dashboardFragment)

        if (intent.getBooleanExtra("open_social", false)) navigateTo(R.id.socialFragment)
    }

    private fun setupBottomMenu() {
        binding.navDashboard.setOnClickListener { navigateTo(R.id.dashboardFragment) }
        binding.navPlans.setOnClickListener { navigateTo(R.id.workoutPlansFragment) }
        binding.navExercises.setOnClickListener { navigateTo(R.id.exerciseLibraryFragment) }
        binding.navSocial.setOnClickListener { navigateTo(R.id.socialFragment) }
        binding.navProfile.setOnClickListener { navigateTo(R.id.profileFragment) }
    }

    private fun navigateTo(destinationId: Int) {
        if (navController.currentDestination?.id != destinationId) navController.navigate(destinationId)
    }

    private fun updateSelectedItem(destinationId: Int) {
        val items = listOf(
            Triple(binding.navDashboard, binding.iconDashboard, binding.textDashboard) to R.id.dashboardFragment,
            Triple(binding.navPlans, binding.iconPlans, binding.textPlans) to R.id.workoutPlansFragment,
            Triple(binding.navExercises, binding.iconExercises, binding.textExercises) to R.id.exerciseLibraryFragment,
            Triple(binding.navSocial, binding.iconSocial, binding.textSocial) to R.id.socialFragment,
            Triple(binding.navProfile, binding.iconProfile, binding.textProfile) to R.id.profileFragment
        )
        items.forEach { (views, id) ->
            val selected = id == destinationId
            views.first.isSelected = selected
            views.second.isSelected = selected
            views.third.isSelected = selected
            UiAnimations.selectNavItem(views.first, selected)
        }
    }

    override fun onStart() {
        super.onStart()
        refreshSocialBadge()
    }

    override fun onStop() {
        socialBadgeListener?.remove()
        socialBadgeListener = null
        super.onStop()
    }

    fun refreshSocialBadge() {
        socialBadgeListener?.remove()
        socialBadgeListener = socialRepository.observePendingIncomingRequestCount { count ->
            runOnUiThread { updateSocialBadge(count) }
        }
    }

    private fun updateSocialBadge(count: Int) {
        binding.socialBadge.visibility = if (count > 0) View.VISIBLE else View.GONE
        binding.socialBadge.text = if (count > 9) "9+" else count.toString()
        if (count > 0) UiAnimations.pulse(binding.socialBadge)
    }
}
