package com.example.neurozen_front.presentation.worker

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import com.example.neurozen_front.presentation.components.nzNavItemColors
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.neurozen_front.domain.logic.BotSuggestion
import com.example.neurozen_front.domain.logic.RecommendationAction
import com.example.neurozen_front.domain.repository.SessionRepository
import com.example.neurozen_front.notifications.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Inicio", Icons.Outlined.Home),
    Tab("wellness", "Bienestar", Icons.Outlined.SelfImprovement),
    Tab("professionals", "Psicólogos", Icons.Outlined.Psychology),
    Tab("appointments", "Mis citas", Icons.Outlined.CalendarMonth),
    Tab("profile", "Perfil", Icons.Outlined.Person)
)

@HiltViewModel
class WorkerShellViewModel @Inject constructor(private val sessions: SessionRepository) : ViewModel() {
    suspend fun consumeOnboarding(): Boolean = sessions.consumeOnboardingFlag()
}

@Composable
fun WorkerNavHost(onLogout: () -> Unit, shell: WorkerShellViewModel = hiltViewModel()) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (shell.consumeOnboarding()) nav.navigate("onboarding")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationHelper.canNotify(context)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        bottomBar = {
            if (tabs.any { it.route == currentRoute }) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { nav.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                            colors = nzNavItemColors(usePrimary = true)
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") { HomeScreen(onAction = { nav.navigateAction(it) }, onNavigate = { nav.navigate(it) }) }
            composable("onboarding") { ProfileEditScreen(isOnboarding = true, onDone = { nav.navigate("assessment") { popUpTo("home") } }, onBack = { nav.popBackStack() }) }
            composable("triggers") { TriggersScreen(onBack = { nav.popBackStack() }) }
            composable("assessment") { AssessmentScreen(onBack = { nav.popBackStack() }, onAction = { nav.navigateAction(it) }) }
            composable("symptoms") { SymptomsScreen(onBack = { nav.popBackStack() }) }
            composable("wellness") { WellnessHubScreen(onNavigate = { nav.navigate(it) }) }
            composable("breathing") { BreathingScreen(onBack = { nav.popBackStack() }) }
            composable("breaks") { RemindersScreen(onBack = { nav.popBackStack() }) }
            composable("workload") { WorkloadScreen(onBack = { nav.popBackStack() }, onReminders = { nav.navigate("breaks") }) }
            composable("challenges") { ChallengesScreen(onBack = { nav.popBackStack() }) }
            composable("library") { LibraryScreen(onBack = { nav.popBackStack() }, onOpen = { nav.navigate("library/$it") }) }
            composable("library/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                LibraryDetailScreen(onBack = { nav.popBackStack() })
            }
            composable("professionals") { ProfessionalsScreen(onOpen = { nav.navigate("professional/$it") }) }
            composable("professional/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                ProfessionalDetailScreen(onBack = { nav.popBackStack() }, onBook = { nav.navigate("booking/$id") })
            }
            composable("booking/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                BookingScreen(onBack = { nav.popBackStack() }, onBooked = { nav.switchTab("appointments") })
            }
            composable("appointments") { MyAppointmentsScreen(onFindProfessional = { nav.switchTab("professionals") }) }
            composable("zenbot") { ZenBotScreen(onBack = { nav.popBackStack() }, onSuggestion = { nav.navigateSuggestion(it) }) }
            composable("profile") { ProfileScreen(onNavigate = { nav.navigate(it) }, onLogout = onLogout) }
            composable("profile_edit") { ProfileEditScreen(isOnboarding = false, onDone = { nav.popBackStack() }, onBack = { nav.popBackStack() }) }
            composable("change_password") { ChangePasswordScreen(onBack = { nav.popBackStack() }) }
            composable("subscription") { SubscriptionScreen(onBack = { nav.popBackStack() }) }
        }
    }
}

private fun NavHostController.switchTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

private fun NavHostController.navigateAction(action: RecommendationAction) = navigate(
    when (action) {
        RecommendationAction.BREATHING -> "breathing"
        RecommendationAction.BREAKS -> "breaks"
        RecommendationAction.LIBRARY -> "library"
        RecommendationAction.PROFESSIONALS -> "professionals"
        RecommendationAction.ASSESSMENT -> "assessment"
        RecommendationAction.WORKLOAD -> "workload"
        RecommendationAction.TRIGGERS -> "triggers"
    }
)

private fun NavHostController.navigateSuggestion(suggestion: BotSuggestion) = navigate(
    when (suggestion) {
        BotSuggestion.BREATHING -> "breathing"
        BotSuggestion.TRIGGERS -> "triggers"
        BotSuggestion.ASSESSMENT -> "assessment"
        BotSuggestion.PROFESSIONALS -> "professionals"
        BotSuggestion.BREAKS -> "breaks"
        BotSuggestion.LIBRARY -> "library"
    }
)
