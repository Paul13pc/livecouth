package com.example.livecouth.ui.water

import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.livecouth.R
import com.example.livecouth.data.local.entity.WaterLog
import com.example.livecouth.domain.model.ManualClimate
import com.example.livecouth.ui.water.components.CustomAmountDialog
import com.example.livecouth.ui.water.components.QuickAddButtonsDialog
import com.example.livecouth.ui.water.components.SharkView
import com.example.livecouth.ui.water.components.WaterHabitatBackground
import com.example.livecouth.ui.water.components.WaterLogItem
import kotlinx.coroutines.delay

@Composable
fun rememberIsReduceMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f,
            )
            scale == 0f
        } catch (_: Exception) {
            false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterScreen(
    viewModel: WaterViewModel,
    modifier: Modifier = Modifier,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val isReduceMotion = rememberIsReduceMotionEnabled()
    var showClimateDetails by rememberSaveable { mutableStateOf(false) }
    var showAllLogs by rememberSaveable { mutableStateOf(false) }
    var showStatusMessage by remember { mutableStateOf(true) }
    val goalReached = uiState.isGoalReady && uiState.progress >= 1f

    LaunchedEffect(uiState.currentMl, uiState.sharkState, goalReached) {
        showStatusMessage = true
        delay(4_000)
        showStatusMessage = false
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.water_tracker_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = stringResource(id = R.string.history_icon_desc),
                        )
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = stringResource(id = R.string.profile_icon_desc),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val sceneMinimumHeight = if (showClimateDetails || showAllLogs) {
                0.dp
            } else {
                maxHeight * 0.55f
            }
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(
                                id = R.string.home_progress_summary,
                                uiState.currentMl,
                                uiState.goalMl,
                                (uiState.progress * 100).toInt(),
                            ),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        IconButton(
                            onClick = { showClimateDetails = !showClimateDetails },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = if (showClimateDetails) {
                                    Icons.Default.KeyboardArrowUp
                                } else {
                                    Icons.Default.KeyboardArrowDown
                                },
                                contentDescription = stringResource(id = R.string.home_weather_toggle),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { uiState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    if (uiState.currentMl > 6000) {
                        Text(
                            text = stringResource(id = R.string.water_total_warning),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else if (showStatusMessage) {
                        Text(
                            text = stringResource(
                                id = if (goalReached) {
                                    R.string.water_goal_congratulations
                                } else {
                                    uiState.sharkState.phraseResId
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (showClimateDetails) {
                    ClimateDetailsPanel(
                        uiState = uiState,
                        onManualClimateChange = viewModel::onManualClimateChange,
                    )
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .heightIn(min = sceneMinimumHeight)
                        .clipToBounds(),
                    contentAlignment = Alignment.Center,
                ) {
                    WaterHabitatBackground(
                        progress = uiState.progress,
                        isSplashTriggered = uiState.isWaterAddedAnimationTriggered,
                        onSplashFinished = viewModel::onSplashAnimationFinished,
                        isReduceMotion = isReduceMotion,
                        isCelebrationTriggered = uiState.isCelebrationTriggered,
                        modifier = Modifier.fillMaxSize(),
                    )
                    val sharkSize = maxOf(120.dp, minOf(140.dp, maxHeight / 1.44f, maxWidth / 1.44f))
                    SharkView(
                        state = uiState.sharkState,
                        waterLevel = uiState.progress,
                        size = sharkSize,
                        isReduceMotion = isReduceMotion,
                        celebrate = uiState.isCelebrationTriggered,
                    )
                }

                WaterActionsPanel(
                    uiState = uiState,
                    showAllLogs = showAllLogs,
                    onToggleLogs = { showAllLogs = !showAllLogs },
                    onAddQuickAmount = viewModel::addQuickAmount,
                    onEditButtons = { viewModel.showQuickButtonsEditor(true) },
                    onAddCustomAmount = { viewModel.showCustomDialog(true) },
                    onDeleteLog = viewModel::deleteWaterLog,
                )
            }

            if (uiState.showCustomAmountDialog) {
                CustomAmountDialog(
                    onDismiss = { viewModel.showCustomDialog(false) },
                    onConfirm = { amount, drinkType, factor ->
                        viewModel.addDrink(amount, drinkType, factor)
                        viewModel.showCustomDialog(false)
                    },
                )
            }

            if (uiState.showQuickButtonsEditor) {
                QuickAddButtonsDialog(
                    buttons = uiState.quickAddButtons,
                    onDismiss = { viewModel.showQuickButtonsEditor(false) },
                    onSave = viewModel::saveQuickAddButton,
                    onDelete = viewModel::deleteQuickAddButton,
                    onMove = viewModel::moveQuickAddButton,
                )
            }
        }
    }
}

@Composable
private fun ClimateDetailsPanel(
    uiState: WaterUiState,
    onManualClimateChange: (ManualClimate) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 6.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when {
                uiState.isWeatherLoading -> Text(
                    text = stringResource(id = R.string.home_weather_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                uiState.weather != null -> {
                    Text(
                        text = stringResource(
                            id = R.string.home_weather_summary,
                            uiState.weather.cityName,
                            uiState.weather.maxTemperatureCelsius,
                            uiState.weather.maxHumidityPercent,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (uiState.isWeatherUnavailable) {
                        Text(
                            text = stringResource(
                                id = R.string.home_weather_cached,
                                uiState.weather.cityName,
                                uiState.weather.date,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                else -> Text(
                    text = stringResource(id = R.string.home_weather_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(
                    id = R.string.home_goal_breakdown,
                    uiState.baseGoalMl,
                    uiState.climateAdjustmentMl,
                    uiState.goalMl,
                ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (uiState.isWeatherUnavailable && uiState.weather == null) {
                Text(
                    text = stringResource(id = R.string.manual_weather_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ManualClimate.entries.forEach { climate ->
                        FilterChip(
                            selected = uiState.manualClimate == climate,
                            onClick = { onManualClimateChange(climate) },
                            label = {
                                Text(
                                    text = stringResource(id = climate.labelResId),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WaterActionsPanel(
    uiState: WaterUiState,
    showAllLogs: Boolean,
    onToggleLogs: () -> Unit,
    onAddQuickAmount: (Long) -> Unit,
    onEditButtons: () -> Unit,
    onAddCustomAmount: () -> Unit,
    onDeleteLog: (WaterLog) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = uiState.quickAddButtons,
                    key = { it.id },
                ) { quickButton ->
                    Button(
                        onClick = { onAddQuickAmount(quickButton.id) },
                        modifier = Modifier.height(44.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(text = quickButton.name, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onEditButtons,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(text = stringResource(id = R.string.quick_buttons_manage))
                }
                Button(
                    onClick = onAddCustomAmount,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.add_custom),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleLogs)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.today_logs_count, uiState.todayLogs.size),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    imageVector = if (showAllLogs) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = stringResource(id = R.string.water_logs_toggle),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            if (showAllLogs) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 180.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(uiState.todayLogs, key = { it.id }) { log ->
                        WaterLogItem(waterLog = log, onDelete = onDeleteLog)
                    }
                }
            } else {
                val latestLog = uiState.todayLogs.firstOrNull()
                Text(
                    text = if (latestLog == null) {
                        stringResource(id = R.string.empty_logs)
                    } else {
                        stringResource(
                            id = R.string.water_log_amount,
                            stringResource(id = latestLog.drinkType.labelResId),
                            latestLog.cantidadMl,
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
