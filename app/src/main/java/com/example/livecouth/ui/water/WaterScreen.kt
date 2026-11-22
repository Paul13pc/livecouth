package com.example.livecouth.ui.water

import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.livecouth.R
import com.example.livecouth.domain.model.SharkState
import com.example.livecouth.ui.water.components.CustomAmountDialog
import com.example.livecouth.ui.water.components.SharkView
import com.example.livecouth.ui.water.components.WaterHabitatBackground
import com.example.livecouth.ui.water.components.WaterLogItem

import androidx.compose.ui.res.stringResource

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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.water_tracker_title),
                        style = MaterialTheme.typography.titleLarge,
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
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // 1. Habitat Background (Sand & Animated Water Surface)
            WaterHabitatBackground(
                progress = uiState.progress,
                sharkState = uiState.sharkState,
                isSplashTriggered = uiState.isWaterAddedAnimationTriggered,
                onSplashFinished = viewModel::onSplashAnimationFinished,
                isReduceMotion = isReduceMotion,
                modifier = Modifier.fillMaxSize(),
            )

            // 2. Foreground Overlay Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Header Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(
                                id = R.string.current_progress,
                                uiState.currentMl,
                                uiState.goalMl,
                            ),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { uiState.progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Humorous Coach Phrase
                        Text(
                            text = stringResource(id = uiState.sharkState.phraseResId),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Shark Habitat Area with SharkView
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = when (uiState.sharkState) {
                        SharkState.STRANDED -> Alignment.BottomCenter
                        SharkState.RECOVERING -> Alignment.Center
                        SharkState.SWIMMING -> Alignment.Center
                        SharkState.HAPPY -> Alignment.Center
                        SharkState.CELEBRATING -> Alignment.TopCenter
                    },
                ) {
                    SharkView(
                        state = uiState.sharkState,
                        isReduceMotion = isReduceMotion,
                    )
                }

                // Quick Add Controls
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { viewModel.addWater(150) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = stringResource(id = R.string.add_150))
                        }

                        Button(
                            onClick = { viewModel.addWater(250) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = stringResource(id = R.string.add_250))
                        }

                        Button(
                            onClick = { viewModel.addWater(500) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = stringResource(id = R.string.add_500))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { viewModel.showCustomDialog(true) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(id = R.string.add_custom))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Today's Water Logs Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.today_logs_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (uiState.todayLogs.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(id = R.string.empty_logs),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                items(
                                    items = uiState.todayLogs,
                                    key = { it.id },
                                ) { log ->
                                    WaterLogItem(
                                        waterLog = log,
                                        onDelete = viewModel::deleteWaterLog,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Custom Amount Dialog
            if (uiState.showCustomAmountDialog) {
                CustomAmountDialog(
                    onDismiss = { viewModel.showCustomDialog(false) },
                    onConfirm = { amount ->
                        viewModel.addWater(amount)
                        viewModel.showCustomDialog(false)
                    },
                )
            }
        }
    }
}
