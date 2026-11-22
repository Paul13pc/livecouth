package com.example.livecouth.ui.profile

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.livecouth.R
import com.example.livecouth.domain.model.ActivityLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    modifier: Modifier = Modifier,
    isOnboarding: Boolean = false,
    onNavigateBack: () -> Unit = {},
    onProfileSaved: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Permission launcher for Android 13+ (POST_NOTIFICATIONS)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.onReminderEnabledChange(true)
            }
        },
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isOnboarding) {
                            stringResource(id = R.string.onboarding_title)
                        } else {
                            stringResource(id = R.string.profile_title)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    if (!isOnboarding) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Atrás",
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isOnboarding) {
                Text(
                    text = stringResource(id = R.string.onboarding_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Name input
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(text = stringResource(id = R.string.profile_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Weight input
            OutlinedTextField(
                value = uiState.weightText,
                onValueChange = viewModel::onWeightChange,
                label = { Text(text = stringResource(id = R.string.profile_weight)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Age input
            OutlinedTextField(
                value = uiState.ageText,
                onValueChange = viewModel::onAgeChange,
                label = { Text(text = stringResource(id = R.string.profile_age)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Activity level selector
            Text(
                text = stringResource(id = R.string.profile_activity),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = uiState.activityLevel == ActivityLevel.BAJO,
                    onClick = { viewModel.onActivityLevelChange(ActivityLevel.BAJO) },
                    label = { Text(text = stringResource(id = R.string.activity_low)) },
                    modifier = Modifier.weight(1f),
                )

                FilterChip(
                    selected = uiState.activityLevel == ActivityLevel.MEDIO,
                    onClick = { viewModel.onActivityLevelChange(ActivityLevel.MEDIO) },
                    label = { Text(text = stringResource(id = R.string.activity_medium)) },
                    modifier = Modifier.weight(1f),
                )

                FilterChip(
                    selected = uiState.activityLevel == ActivityLevel.ALTO,
                    onClick = { viewModel.onActivityLevelChange(ActivityLevel.ALTO) },
                    label = { Text(text = stringResource(id = R.string.activity_high)) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Calculated goal preview card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(
                            id = R.string.calculated_goal_preview,
                            uiState.calculatedGoalMl,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // Reminder Settings Section
            Text(
                text = stringResource(id = R.string.reminder_settings_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(id = R.string.reminder_enable_switch),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )

                Switch(
                    checked = uiState.reminderEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.onReminderEnabledChange(enabled)
                        }
                    },
                )
            }

            if (uiState.reminderEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                // Start Hour Slider
                Text(
                    text = "Hora de inicio: ${uiState.reminderStartHour}:00",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Slider(
                    value = uiState.reminderStartHour.toFloat(),
                    onValueChange = { viewModel.onReminderStartHourChange(it.toInt()) },
                    valueRange = 0f..23f,
                    steps = 22,
                )

                // End Hour Slider
                Text(
                    text = "Hora de fin: ${uiState.reminderEndHour}:00",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Slider(
                    value = uiState.reminderEndHour.toFloat(),
                    onValueChange = { viewModel.onReminderEndHourChange(it.toInt()) },
                    valueRange = 0f..23f,
                    steps = 22,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Immediate Test Button
                OutlinedButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        viewModel.triggerTestNotification(context)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(id = R.string.test_notification_btn))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Save button
            Button(
                onClick = {
                    viewModel.saveProfile(
                        context = context,
                        onSuccess = onProfileSaved,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                Text(
                    text = if (isOnboarding) {
                        stringResource(id = R.string.btn_save_onboarding)
                    } else {
                        stringResource(id = R.string.btn_save_profile)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
