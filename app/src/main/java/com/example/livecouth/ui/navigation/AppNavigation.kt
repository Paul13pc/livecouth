package com.example.livecouth.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.livecouth.domain.repository.UserPreferencesRepository
import com.example.livecouth.ui.history.HistoryScreen
import com.example.livecouth.ui.history.HistoryViewModel
import com.example.livecouth.ui.auth.AuthViewModel
import com.example.livecouth.ui.profile.ProfileScreen
import com.example.livecouth.ui.profile.ProfileViewModel
import com.example.livecouth.ui.water.WaterScreen
import com.example.livecouth.ui.water.WaterViewModel

object Routes {
    const val ONBOARDING = "onboarding"
    const val WATER_TRACKER = "water_tracker"
    const val PROFILE = "profile"
    const val HISTORY = "history"
}

@Composable
fun AppNavigation(
    userPreferencesRepository: UserPreferencesRepository,
    navController: NavHostController = rememberNavController(),
) {
    val userProfileState by userPreferencesRepository.userProfile.collectAsState(initial = null)
    val authViewModel: AuthViewModel = hiltViewModel()

    val profile = userProfileState

    if (profile == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = if (profile.isOnboardingCompleted) {
        Routes.WATER_TRACKER
    } else {
        Routes.ONBOARDING
    }

    LaunchedEffect(profile.isOnboardingCompleted) {
        if (
            profile.isOnboardingCompleted &&
            navController.currentDestination?.route == Routes.ONBOARDING
        ) {
            navController.navigate(Routes.WATER_TRACKER) {
                popUpTo(Routes.ONBOARDING) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Routes.ONBOARDING) {
            val profileViewModel: ProfileViewModel = hiltViewModel()
            ProfileScreen(
                viewModel = profileViewModel,
                authViewModel = authViewModel,
                isOnboarding = true,
                onProfileSaved = {
                    navController.navigate(Routes.WATER_TRACKER) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.WATER_TRACKER) {
            val waterViewModel: WaterViewModel = hiltViewModel()
            WaterScreen(
                viewModel = waterViewModel,
                onNavigateToProfile = {
                    navController.navigate(Routes.PROFILE)
                },
                onNavigateToHistory = {
                    navController.navigate(Routes.HISTORY)
                },
            )
        }

        composable(Routes.PROFILE) {
            val profileViewModel: ProfileViewModel = hiltViewModel()
            ProfileScreen(
                viewModel = profileViewModel,
                authViewModel = authViewModel,
                isOnboarding = false,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onProfileSaved = {
                    navController.popBackStack()
                },
            )
        }

        composable(Routes.HISTORY) {
            val historyViewModel: HistoryViewModel = hiltViewModel()
            HistoryScreen(
                viewModel = historyViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }
    }
}
