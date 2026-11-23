package com.example.livecouth.ui.water.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.livecouth.R
import com.example.livecouth.domain.model.SharkState
import kotlin.math.PI
import kotlin.math.sin

private val confettiColors = arrayOf(
    Color(0xFFFFD166),
    Color(0xFF06D6A0),
    Color(0xFFEF476F),
    Color(0xFF118AB2),
    Color(0xFFF4F1DE),
)

@Composable
fun WaterHabitatBackground(
    progress: Float,
    isSplashTriggered: Boolean,
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier,
    isReduceMotion: Boolean = false,
    isCelebrationTriggered: Boolean = false,
) {
    val isDark = isSystemInDarkTheme()
    val animatedProgress = animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (isReduceMotion) {
            tween(durationMillis = 0)
        } else {
            tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        },
        label = "WaterLevelAnimation",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "HabitatAnimations")
    val phaseState: State<Float> = if (isReduceMotion) {
        remember { mutableFloatStateOf(0f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 9000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "HabitatPhase",
        )
    }

    val splashProgress = remember { Animatable(1f) }
    val latestOnSplashFinished by rememberUpdatedState(onSplashFinished)
    LaunchedEffect(isSplashTriggered, isReduceMotion) {
        if (!isSplashTriggered) {
            splashProgress.snapTo(1f)
            return@LaunchedEffect
        }
        if (isReduceMotion) {
            splashProgress.snapTo(1f)
            latestOnSplashFinished()
            return@LaunchedEffect
        }
        splashProgress.snapTo(0f)
        splashProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing),
        )
        latestOnSplashFinished()
    }

    val celebrationProgress = remember { Animatable(0f) }
    LaunchedEffect(isCelebrationTriggered, isReduceMotion) {
        if (isCelebrationTriggered && !isReduceMotion) {
            celebrationProgress.snapTo(0f)
            celebrationProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1800, easing = LinearEasing),
            )
        } else {
            celebrationProgress.snapTo(0f)
        }
    }

    val skyColor = if (isDark) Color(0xFF082F49) else Color(0xFFB7E4F6)
    val horizonColor = if (isDark) Color(0xFF164E63) else Color(0xFFD8F1F8)
    val sandColor = if (isDark) Color(0xFF554327) else Color(0xFFE9C46A)
    val waterColor = if (isDark) Color(0xFF0284C7) else Color(0xFF168DB5)
    val deepWaterColor = if (isDark) Color(0xFF075985) else Color(0xFF146C8E)

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val sceneProgress = animatedProgress.value.coerceIn(0f, 1f)
        val phase = phaseState.value
        val radians = phase * (2f * PI.toFloat())
        val phaseCycle = phase
        val splash = splashProgress.value
        val celebration = celebrationProgress.value
        val sandTop = height * 0.72f
        val isWaveStage = sceneProgress >= 0.20f && sceneProgress < 0.40f

        drawRect(skyColor)
        drawRect(
            color = horizonColor.copy(alpha = 0.45f),
            topLeft = Offset(0f, height * 0.70f),
            size = Size(width, height * 0.17f),
        )

        val sand = Path().apply {
            moveTo(0f, height)
            lineTo(0f, height * 0.66f)
            cubicTo(width * 0.28f, height * 0.66f, width * 0.66f, height * 0.79f, width, height * 0.88f)
            lineTo(width, height)
            close()
        }
        drawPath(sand, sandColor)

        for (index in 0..35) {
            val xRatio = ((index * 0.618f) % 1f)
            val yRatio = 0.89f + ((index * 0.371f) % 0.09f)
            val radius = width * (0.0015f + ((index % 3) * 0.0007f))
            drawCircle(
                color = Color(0xFF9B773C).copy(alpha = 0.22f),
                radius = radius,
                center = Offset(width * xRatio, height * yRatio),
            )
        }

        val waveCycle = (phaseCycle * 1.5f) % 1f
        val surge = when {
            waveCycle < 0.28f -> smoothStep(waveCycle / 0.28f)
            waveCycle < 0.42f -> 1f
            waveCycle < 0.72f -> 1f - smoothStep((waveCycle - 0.42f) / 0.30f)
            else -> 0f
        }
        val stageProgress = when {
            sceneProgress < 0.20f -> 0f
            sceneProgress < 0.40f -> (sceneProgress - 0.20f) / 0.20f
            sceneProgress < 0.70f -> (sceneProgress - 0.40f) / 0.30f
            else -> (sceneProgress - 0.70f) / 0.30f
        }.coerceIn(0f, 1f)
        val baseCoverage = when {
            sceneProgress < 0.20f -> 0f
            sceneProgress < 0.40f -> 0.06f + stageProgress * 0.28f
            sceneProgress < 0.70f -> 0.34f + stageProgress * 0.34f
            else -> 0.68f + stageProgress * 0.29f
        }
        val waterCoverage = (baseCoverage + if (isWaveStage) surge * 0.18f else 0f).coerceIn(0f, 0.98f)
        val waveFrontX = width * (1f - waterCoverage)
        val waterSurfaceLeft = when {
            sceneProgress < 0.40f -> height * 0.72f
            sceneProgress < 0.70f -> height * (0.72f - stageProgress * 0.14f)
            else -> height * (0.58f - stageProgress * 0.19f)
        }
        fun waterSurfaceAt(xRatio: Float): Float =
            waterSurfaceLeft + height * 0.18f * xRatio.coerceIn(0f, 1f)

        val surfaceY = waterSurfaceAt(waveFrontX / width)
        val surfaceAmplitude = if (isWaveStage) height * 0.009f else height * 0.005f

        if (sceneProgress < 0.20f) {
            val puddle = Path().apply {
                moveTo(width * 0.71f, height * 0.87f)
                cubicTo(width * 0.74f, height * 0.84f, width * 0.83f, height * 0.84f, width * 0.87f, height * 0.87f)
                cubicTo(width * 0.84f, height * 0.90f, width * 0.75f, height * 0.90f, width * 0.71f, height * 0.87f)
                close()
            }
            drawPath(puddle, waterColor.copy(alpha = 0.76f))
            drawPath(puddle, Color(0xFFCBF5F5).copy(alpha = 0.72f), style = Stroke(width = height * 0.004f))
        } else {
            val water = Path().apply {
                moveTo(waveFrontX, surfaceY + sin(radians) * surfaceAmplitude)
                cubicTo(
                    waveFrontX - width * 0.025f,
                    surfaceY + height * 0.045f,
                    waveFrontX + width * 0.035f,
                    surfaceY + height * 0.13f,
                    waveFrontX + width * 0.015f,
                    height,
                )
                lineTo(width, height)
                for (step in 36 downTo 0) {
                    val x = waveFrontX + (width - waveFrontX) * step / 36f
                    val ratio = x / width
                    val y = waterSurfaceAt(ratio) + sin((ratio * 4f * PI.toFloat()) + radians) * surfaceAmplitude
                    lineTo(x, y)
                }
                close()
            }
            drawPath(water, waterColor.copy(alpha = 0.90f))

            val deepWater = Path().apply {
                for (step in 0..32) {
                    val x = waveFrontX + (width - waveFrontX) * step / 32f
                    val ratio = x / width
                    val surface = waterSurfaceAt(ratio)
                    val deepY = surface + (height - surface) * 0.57f
                    if (step == 0) moveTo(x, deepY) else lineTo(x, deepY)
                }
                lineTo(width, height)
                lineTo(waveFrontX, height)
                close()
            }
            drawPath(deepWater, deepWaterColor.copy(alpha = 0.48f))

            val surfaceLine = Path().apply {
                for (step in 0..36) {
                    val x = waveFrontX + (width - waveFrontX) * step / 36f
                    val ratio = x / width
                    val y = waterSurfaceAt(ratio) + sin((ratio * 4f * PI.toFloat()) + radians) * surfaceAmplitude
                    if (step == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(
                surfaceLine,
                Color(0xFFCBF5F5).copy(alpha = if (isWaveStage) 0.96f else 0.72f),
                style = Stroke(width = height * if (isWaveStage) 0.009f else 0.004f),
            )

            if (isWaveStage) {
                val foamEdge = Path().apply {
                    moveTo(waveFrontX, surfaceY)
                    cubicTo(
                        waveFrontX - width * 0.025f,
                        surfaceY + height * 0.045f,
                        waveFrontX + width * 0.035f,
                        surfaceY + height * 0.13f,
                        waveFrontX + width * 0.015f,
                        height,
                    )
                }
                drawPath(
                    foamEdge,
                    Color.White.copy(alpha = 0.90f * (0.55f + surge * 0.45f)),
                    style = Stroke(width = height * 0.012f),
                )
                for (index in 0..6) {
                    val yRatio = index / 6f
                    val x = waveFrontX + width * (0.012f + sin(index * 2.4f + radians) * 0.012f)
                    val y = surfaceY + (height - surfaceY) * yRatio
                    drawCircle(
                        Color.White.copy(alpha = 0.75f * (1f - yRatio * 0.35f)),
                        radius = height * (0.008f + (index % 2) * 0.003f),
                        center = Offset(x.coerceIn(0f, width), y),
                    )
                }
            } else if (sceneProgress >= 0.40f) {
                for (index in 0..10) {
                    val rise = (phaseCycle * 0.38f + index * 0.13f) % 1f
                    val xRatio = waveFrontX / width + (index * 0.173f + 0.08f) % (waterCoverage * 0.86f).coerceAtLeast(0.01f)
                    val y = waterSurfaceAt(xRatio) + (height - waterSurfaceAt(xRatio)) * (1f - rise)
                    val radius = width * (0.003f + (index % 3) * 0.0015f)
                    drawCircle(
                        Color.White.copy(alpha = 0.26f + rise * 0.18f),
                        radius,
                        Offset(width * xRatio.coerceIn(0f, 1f), y),
                    )
                    drawCircle(
                        Color.White.copy(alpha = 0.45f),
                        radius,
                        Offset(width * xRatio.coerceIn(0f, 1f), y),
                        style = Stroke(width = height * 0.002f),
                    )
                }
            }
        }

        if (sceneProgress >= 0.70f) {
            val fishY = waterSurfaceAt(0.5f) + height * 0.16f
            val fishTravel = (phaseCycle * 0.8f) % 1f
            drawFish(
                center = Offset(width * (0.16f + fishTravel * 0.72f), fishY),
                scale = width * 0.045f,
                color = Color(0xFFFFD166),
                facingRight = true,
            )
            drawFish(
                center = Offset(width * (0.84f - fishTravel * 0.68f), fishY + height * 0.11f),
                scale = width * 0.030f,
                color = Color(0xFFFF8C69),
                facingRight = false,
            )
            drawFish(
                center = Offset(width * (0.34f + ((fishTravel + 0.42f) % 1f) * 0.50f), fishY + height * 0.20f),
                scale = width * 0.024f,
                color = Color(0xFFF4F1DE),
                facingRight = true,
            )
            drawCoral(
                base = Offset(width * 0.09f, height * 0.96f),
                height = height * 0.15f,
                color = Color(0xFFEF6F6C),
                strokeWidth = height * 0.012f,
            )
            drawCoral(
                base = Offset(width * 0.88f, height * 0.98f),
                height = height * 0.19f,
                color = Color(0xFFFF9F68),
                strokeWidth = height * 0.014f,
            )
        }

        if (splash < 1f && !isReduceMotion) {
            val splashX = width * 0.52f
            val splashY = if (sceneProgress >= 0.20f) surfaceY else sandTop
            for (index in 0..17) {
                val angle = PI.toFloat() * (0.10f + index / 19f)
                val distance = height * (0.04f + (index % 6) * 0.012f) * splash
                val x = splashX + sin(angle) * distance * 1.4f
                val y = splashY - kotlin.math.cos(angle) * distance + height * 0.12f * splash * splash
                val radius = height * (0.004f + (index % 4) * 0.001f) * (1f - splash * 0.45f)
                drawCircle(
                    Color(0xFFDAFAFF).copy(alpha = 1f - splash),
                    radius,
                    Offset(x, y),
                )
            }
        }

        if (celebration > 0f && !isReduceMotion) {
            for (index in 0 until 34) {
                val seedX = ((index * 0.618f) % 1f)
                val fall = ((celebration * (1.35f + (index % 5) * 0.08f) + index * 0.031f) % 1.45f)
                val x = width * seedX + sin(radians * 2f + index) * width * 0.018f
                val y = height * (fall - 0.18f)
                if (y in -height * 0.04f..height) {
                    val particleSize = width * (0.008f + (index % 3) * 0.003f)
                    drawRect(
                        color = confettiColors[index % confettiColors.size],
                        topLeft = Offset(x, y),
                        size = Size(particleSize, particleSize * 1.6f),
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawFish(
    center: Offset,
    scale: Float,
    color: Color,
    facingRight: Boolean,
) {
    val direction = if (facingRight) 1f else -1f
    val body = Path().apply {
        moveTo(center.x - direction * scale * 0.55f, center.y)
        cubicTo(
            center.x - direction * scale * 0.30f,
            center.y - scale * 0.48f,
            center.x + direction * scale * 0.34f,
            center.y - scale * 0.42f,
            center.x + direction * scale * 0.55f,
            center.y,
        )
        cubicTo(
            center.x + direction * scale * 0.30f,
            center.y + scale * 0.40f,
            center.x - direction * scale * 0.30f,
            center.y + scale * 0.40f,
            center.x - direction * scale * 0.55f,
            center.y,
        )
        close()
    }
    drawPath(body, color)

    val tail = Path().apply {
        moveTo(center.x - direction * scale * 0.48f, center.y)
        lineTo(center.x - direction * scale * 0.88f, center.y - scale * 0.30f)
        lineTo(center.x - direction * scale * 0.82f, center.y + scale * 0.30f)
        close()
    }
    drawPath(tail, color)
    drawCircle(
        Color(0xFF244554),
        scale * 0.055f,
        Offset(center.x + direction * scale * 0.31f, center.y - scale * 0.10f),
    )
}

private fun DrawScope.drawCoral(
    base: Offset,
    height: Float,
    color: Color,
    strokeWidth: Float,
) {
    val coral = Path().apply {
        moveTo(base.x, base.y)
        cubicTo(base.x - height * 0.12f, base.y - height * 0.28f, base.x + height * 0.10f, base.y - height * 0.58f, base.x, base.y - height)
        moveTo(base.x - height * 0.03f, base.y - height * 0.42f)
        cubicTo(base.x - height * 0.22f, base.y - height * 0.52f, base.x - height * 0.24f, base.y - height * 0.65f, base.x - height * 0.28f, base.y - height * 0.72f)
        moveTo(base.x + height * 0.02f, base.y - height * 0.60f)
        cubicTo(base.x + height * 0.20f, base.y - height * 0.67f, base.x + height * 0.23f, base.y - height * 0.83f, base.x + height * 0.25f, base.y - height * 0.88f)
    }
    drawPath(
        coral,
        color,
        style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round),
    )
}

private fun smoothStep(value: Float): Float {
    val clamped = value.coerceIn(0f, 1f)
    return clamped * clamped * (3f - 2f * clamped)
}

@Preview(showBackground = true, widthDp = 400, heightDp = 560)
@Composable
private fun WaterHabitatProgressPreview() {
    var percentage by remember { mutableFloatStateOf(0f) }
    val progress = percentage / 100f
    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(R.string.water_scene_preview_progress, percentage.toInt()),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleSmall,
            )
            Slider(
                value = percentage,
                onValueChange = { percentage = it },
                valueRange = 0f..100f,
                steps = 99,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                WaterHabitatBackground(
                    progress = progress,
                    isSplashTriggered = false,
                    onSplashFinished = {},
                    isCelebrationTriggered = percentage >= 100f,
                    modifier = Modifier.fillMaxSize(),
                )
                SharkView(
                    state = SharkState.calculateStateFromPercentage(progress),
                    waterLevel = progress,
                    isReduceMotion = false,
                    celebrate = percentage >= 100f,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
