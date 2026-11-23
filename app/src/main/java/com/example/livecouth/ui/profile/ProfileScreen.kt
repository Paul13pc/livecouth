package com.example.livecouth.ui.profile

import android.app.Activity
import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
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
import com.example.livecouth.domain.model.CityLocation
import com.example.livecouth.domain.model.ReminderFrequency
import com.example.livecouth.domain.model.ReminderSettingsError
import com.example.livecouth.domain.model.ReminderSettingsValidator
import com.example.livecouth.domain.model.ValidationError
import com.example.livecouth.ui.auth.AuthViewModel

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private data class CityPreset(val location: CityLocation, val labelResId: Int)

private fun reminderFrequencyLabel(frequency: ReminderFrequency): Int = when (frequency) {
    ReminderFrequency.EVERY_30_MINUTES -> R.string.reminder_frequency_30_minutes
    ReminderFrequency.EVERY_HOUR -> R.string.reminder_frequency_1_hour
    ReminderFrequency.EVERY_90_MINUTES -> R.string.reminder_frequency_90_minutes
    ReminderFrequency.EVERY_2_HOURS -> R.string.reminder_frequency_2_hours
    ReminderFrequency.EVERY_3_HOURS -> R.string.reminder_frequency_3_hours
    ReminderFrequency.CUSTOM -> R.string.reminder_frequency_custom
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    authViewModel: AuthViewModel? = null,
    modifier: Modifier = Modifier,
    isOnboarding: Boolean = false,
    onNavigateBack: () -> Unit = {},
    onProfileSaved: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var isFrequencyMenuExpanded by remember { mutableStateOf(false) }
    var testNotificationPending by remember { mutableStateOf(false) }
    var showLocationConsent by remember { mutableStateOf(false) }
    var hasRequestedLocationPermission by rememberSaveable { mutableStateOf(false) }
    val mantaName = stringResource(id = R.string.city_manta)
    val quitoName = stringResource(id = R.string.city_quito)
    val guayaquilName = stringResource(id = R.string.city_guayaquil)
    val cuencaName = stringResource(id = R.string.city_cuenca)
    val cityPresets = listOf(
        CityPreset(CityLocation(mantaName, -0.9677f, -80.7089f), R.string.city_manta),
        CityPreset(CityLocation(quitoName, -0.1807f, -78.4678f), R.string.city_quito),
        CityPreset(CityLocation(guayaquilName, -2.1709f, -79.9224f), R.string.city_guayaquil),
        CityPreset(CityLocation(cuencaName, -2.9001f, -79.0059f), R.string.city_cuenca),
    )

    // Permission launcher for Android 13+ (POST_NOTIFICATIONS)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                if (testNotificationPending) {
                    viewModel.triggerTestNotification(context)
                } else {
                    viewModel.onReminderEnabledChange(true)
                }
            }
            testNotificationPending = false
        },
    )
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.onDeviceLocationPermissionGranted()
            } else {
                val shouldShowRationale = context.findActivity()
                    ?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) == true
                viewModel.onDeviceLocationPermissionDenied(
                    permanentlyDenied = hasRequestedLocationPermission && !shouldShowRationale,
                )
            }
        },
    )
    val appSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                viewModel.onDeviceLocationPermissionGranted()
            }
        },
    )

    fun requestDeviceLocation() {
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.onDeviceLocationPermissionGranted()
            return
        }

        val shouldShowRationale = context.findActivity()
            ?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) == true
        if (hasRequestedLocationPermission && !shouldShowRationale) {
            viewModel.onDeviceLocationPermissionDenied(permanentlyDenied = true)
        } else {
            hasRequestedLocationPermission = true
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    fun showTimePicker(initialMinute: Int, onTimeSelected: (Int) -> Unit) {
        TimePickerDialog(
            context,
            { _, hour, minute -> onTimeSelected(hour * 60 + minute) },
            initialMinute / 60,
            initialMinute % 60,
            DateFormat.is24HourFormat(context),
        ).show()
    }

    fun formatTime(minuteOfDay: Int): String =
        String.format("%02d:%02d", minuteOfDay / 60, minuteOfDay % 60)

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
                                contentDescription = stringResource(id = R.string.back),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
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

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Datos personales",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = viewModel::onNameChange,
                        label = { Text(text = stringResource(id = R.string.profile_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        isError = uiState.nameError != null,
                        supportingText = uiState.nameError?.let { error ->
                            {
                                Text(
                                    text = stringResource(
                                        id = when (error) {
                                            ValidationError.REQUIRED -> R.string.validation_name_required
                                            ValidationError.TOO_LONG -> R.string.validation_name_too_long
                                            else -> R.string.validation_name_required
                                        },
                                    ),
                                )
                            }
                        },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = uiState.weightText,
                            onValueChange = viewModel::onWeightChange,
                            label = { Text(text = stringResource(id = R.string.profile_weight)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = uiState.weightError != null,
                            supportingText = uiState.weightError?.let { error ->
                                {
                                    Text(
                                        text = stringResource(
                                            id = when (error) {
                                                ValidationError.REQUIRED -> R.string.validation_weight_required
                                                ValidationError.INVALID_FORMAT -> R.string.validation_weight_format
                                                else -> R.string.validation_weight_range
                                            },
                                        ),
                                    )
                                }
                            },
                        )
                        OutlinedTextField(
                            value = uiState.ageText,
                            onValueChange = viewModel::onAgeChange,
                            label = { Text(text = stringResource(id = R.string.profile_age)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = uiState.ageError != null,
                            supportingText = uiState.ageError?.let { error ->
                                {
                                    Text(
                                        text = stringResource(
                                            id = when (error) {
                                                ValidationError.REQUIRED -> R.string.validation_age_required
                                                ValidationError.INVALID_FORMAT -> R.string.validation_age_integer
                                                else -> R.string.validation_age_range
                                            },
                                        ),
                                    )
                                }
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                shape = RoundedCornerShape(20.dp),
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

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.climate_location_mode_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.climate_location_mode_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = uiState.usesDeviceLocationForClimate,
                            onClick = {
                                if (!uiState.usesDeviceLocationForClimate) {
                                    viewModel.clearDeviceLocationNotice()
                                    showLocationConsent = true
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(id = R.string.climate_location_device),
                                    textAlign = TextAlign.Center,
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                        FilterChip(
                            selected = !uiState.usesDeviceLocationForClimate,
                            onClick = viewModel::onManualClimateLocationSelected,
                            label = {
                                Text(
                                    text = stringResource(id = R.string.climate_location_manual),
                                    textAlign = TextAlign.Center,
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (uiState.isResolvingDeviceLocation) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                            Text(
                                text = stringResource(id = R.string.climate_location_loading),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }

            if (!uiState.usesDeviceLocationForClimate) {
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.profile_city),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            cityPresets.take(2).forEach { preset ->
                                FilterChip(
                                    selected = uiState.cityName == preset.location.name,
                                    onClick = { viewModel.onCitySelected(preset.location) },
                                    label = {
                                        Text(
                                            text = stringResource(id = preset.labelResId),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            cityPresets.drop(2).forEach { preset ->
                                FilterChip(
                                    selected = uiState.cityName == preset.location.name,
                                    onClick = { viewModel.onCitySelected(preset.location) },
                                    label = {
                                        Text(
                                            text = stringResource(id = preset.labelResId),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.reminder_settings_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
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
                            checked = uiState.reminderSettings.enabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.onReminderEnabledChange(enabled)
                                }
                            },
                        )
                    }

                    if (uiState.reminderSettings.enabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = stringResource(id = R.string.reminder_start_time))
                                OutlinedButton(
                                    onClick = {
                                        showTimePicker(uiState.reminderSettings.startMinute) {
                                            viewModel.onReminderStartMinuteChange(it)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(text = formatTime(uiState.reminderSettings.startMinute))
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = stringResource(id = R.string.reminder_end_time))
                                OutlinedButton(
                                    onClick = {
                                        showTimePicker(uiState.reminderSettings.endMinute) {
                                            viewModel.onReminderEndMinuteChange(it)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(text = formatTime(uiState.reminderSettings.endMinute))
                                }
                            }
                        }
                        Text(
                            text = stringResource(id = R.string.reminder_frequency_label),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Box {
                            OutlinedButton(
                                onClick = { isFrequencyMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = stringResource(
                                        id = reminderFrequencyLabel(uiState.reminderSettings.frequency),
                                    ),
                                )
                            }
                            DropdownMenu(
                                expanded = isFrequencyMenuExpanded,
                                onDismissRequest = { isFrequencyMenuExpanded = false },
                            ) {
                                ReminderFrequency.entries.forEach { frequency ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(text = stringResource(id = reminderFrequencyLabel(frequency)))
                                        },
                                        onClick = {
                                            viewModel.onReminderFrequencyChange(frequency)
                                            isFrequencyMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                        if (uiState.reminderSettings.frequency == ReminderFrequency.CUSTOM) {
                            OutlinedTextField(
                                value = uiState.customReminderIntervalText,
                                onValueChange = viewModel::onCustomReminderIntervalChange,
                                label = { Text(text = stringResource(id = R.string.reminder_custom_interval)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                isError = uiState.reminderSettingsError ==
                                    ReminderSettingsError.INVALID_INTERVAL,
                                supportingText = if (
                                    uiState.reminderSettingsError == ReminderSettingsError.INVALID_INTERVAL
                                ) {
                                    { Text(text = stringResource(id = R.string.reminder_interval_minimum)) }
                                } else {
                                    null
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.reminder_active_days),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        val dayLabels = listOf(
                            R.string.reminder_day_monday,
                            R.string.reminder_day_tuesday,
                            R.string.reminder_day_wednesday,
                            R.string.reminder_day_thursday,
                            R.string.reminder_day_friday,
                            R.string.reminder_day_saturday,
                            R.string.reminder_day_sunday,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            dayLabels.forEachIndexed { index, labelResId ->
                                FilterChip(
                                    selected = ReminderSettingsValidator.isDayActive(
                                        uiState.reminderSettings,
                                        index,
                                    ),
                                    onClick = { viewModel.onReminderDayToggle(index) },
                                    label = {
                                        Text(
                                            text = stringResource(id = labelResId),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(id = R.string.reminder_quiet_hours),
                                modifier = Modifier.weight(1f),
                            )
                            Switch(
                                checked = uiState.reminderSettings.quietHoursEnabled,
                                onCheckedChange = viewModel::onQuietHoursEnabledChange,
                            )
                        }
                        if (uiState.reminderSettings.quietHoursEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = stringResource(id = R.string.reminder_quiet_start))
                                    OutlinedButton(
                                        onClick = {
                                            showTimePicker(uiState.reminderSettings.quietStartMinute) {
                                                viewModel.onQuietStartMinuteChange(it)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(text = formatTime(uiState.reminderSettings.quietStartMinute))
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = stringResource(id = R.string.reminder_quiet_end))
                                    OutlinedButton(
                                        onClick = {
                                            showTimePicker(uiState.reminderSettings.quietEndMinute) {
                                                viewModel.onQuietEndMinuteChange(it)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(text = formatTime(uiState.reminderSettings.quietEndMinute))
                                    }
                                }
                            }
                        }
                        uiState.reminderSettingsError?.let { error ->
                            val errorString = when (error) {
                                ReminderSettingsError.INVALID_ACTIVE_WINDOW -> R.string.reminder_invalid_window
                                ReminderSettingsError.INVALID_INTERVAL -> R.string.reminder_interval_minimum
                                ReminderSettingsError.INVALID_QUIET_WINDOW -> R.string.reminder_invalid_quiet_window
                            }
                            Text(
                                text = stringResource(id = errorString),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                if (
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS,
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    testNotificationPending = true
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.triggerTestNotification(context)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text(text = stringResource(id = R.string.test_notification_btn))
                        }
                    }
                }
            }

            if (authViewModel != null) {
                val authUiState by authViewModel.uiState.collectAsState()
                Spacer(modifier = Modifier.height(24.dp))
                AccountSignInCard(
                    state = authUiState,
                    onSignIn = { authViewModel.signIn(context) },
                    onSyncNow = authViewModel::synchronizeNow,
                    onSignOut = { deleteLocalData ->
                        authViewModel.signOut(context, deleteLocalData)
                    },
                )
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
                    .height(56.dp),
                enabled = uiState.canSave,
                shape = RoundedCornerShape(18.dp),
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

        if (showLocationConsent) {
            AlertDialog(
                onDismissRequest = { showLocationConsent = false },
                title = {
                    Text(text = stringResource(id = R.string.climate_location_consent_title))
                },
                text = {
                    Text(text = stringResource(id = R.string.climate_location_consent_message))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLocationConsent = false
                            requestDeviceLocation()
                        },
                    ) {
                        Text(text = stringResource(id = R.string.climate_location_consent_continue))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLocationConsent = false }) {
                        Text(text = stringResource(id = R.string.custom_dialog_cancel))
                    }
                },
            )
        }

        uiState.deviceLocationNotice?.let { notice ->
            val isPermanentDenial =
                notice == DeviceLocationNotice.PERMISSION_PERMANENTLY_DENIED
            val messageResId = when (notice) {
                DeviceLocationNotice.PERMISSION_DENIED -> R.string.climate_location_permission_denied
                DeviceLocationNotice.PERMISSION_PERMANENTLY_DENIED ->
                    R.string.climate_location_permission_permanently_denied
                DeviceLocationNotice.LOCATION_DISABLED_LAST_USED ->
                    R.string.climate_location_disabled_last_used
                DeviceLocationNotice.LOCATION_DISABLED_MANUAL ->
                    R.string.climate_location_disabled_manual
                DeviceLocationNotice.LAST_KNOWN_LOCATION_USED ->
                    R.string.climate_location_last_known_used
                DeviceLocationNotice.LOCATION_UNAVAILABLE ->
                    R.string.climate_location_unavailable
                DeviceLocationNotice.LOCATION_STORAGE_ERROR ->
                    R.string.climate_location_storage_error
            }
            AlertDialog(
                onDismissRequest = viewModel::clearDeviceLocationNotice,
                title = {
                    Text(text = stringResource(id = R.string.climate_location_notice_title))
                },
                text = { Text(text = stringResource(id = messageResId)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearDeviceLocationNotice()
                            if (isPermanentDenial) {
                                appSettingsLauncher.launch(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    },
                                )
                            }
                        },
                    ) {
                        Text(
                            text = stringResource(
                                id = if (isPermanentDenial) {
                                    R.string.climate_location_open_settings
                                } else {
                                    R.string.climate_location_understood
                                },
                            ),
                        )
                    }
                },
                dismissButton = {
                    if (isPermanentDenial) {
                        TextButton(onClick = viewModel::clearDeviceLocationNotice) {
                            Text(text = stringResource(id = R.string.climate_location_manual_fallback))
                        }
                    }
                },
            )
        }
    }
}
