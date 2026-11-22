package com.example.livecouth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.livecouth.domain.repository.UserPreferencesRepository
import com.example.livecouth.ui.navigation.AppNavigation
import com.example.livecouth.ui.theme.LivecouthTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LivecouthTheme {
                AppNavigation(userPreferencesRepository = userPreferencesRepository)
            }
        }
    }
}
