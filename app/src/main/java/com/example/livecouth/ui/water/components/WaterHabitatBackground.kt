package com.example.livecouth.ui.water.components

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.livecouth.domain.model.SharkState
import kotlin.math.sin
import kotlin.random.Random

private data class Bubble(
    var xRatio: Float,
    var yRatio: Float,
    val radiusRatio: Float,
    val speed: Float,
)

private data class SplashParticle(
    val startX: Float,
    val startY: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    var progress: Float = 0f,
)

private data class ConfettiParticle(
    var xRatio: Float,
    var yRatio: Float,
    val sizeRatio: Float,
    val speed: Float,
    val color: Color,
)

@Composable
fun WaterHabitatBackground(
    progress: Float,
    sharkState: SharkState,
    isSplashTriggered: Boolean,
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier,
    isReduceMotion: Boolean = false,
) {
    val isDark = isSystemInDarkTheme()

    // Smooth transition of water level
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (isReduceMotion) tween(0) else tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "WaterLevelAnimation",
    )

    // Wave animation offset
    val infiniteTransition = rememberInfiniteTransition(label = "HabitatAnimations")
    val waveOffset by if (isReduceMotion) {
        remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "WaveSurface",
        )
    }

    // Bubbles
    val bubbles = remember {
        mutableStateListOf<Bubble>().apply {
            repeat(12) {
                add(
                    Bubble(
                        xRatio = Random.nextFloat(),
                        yRatio = Random.nextFloat(),
                        radiusRatio = Random.nextFloat() * 0.015f + 0.005f,
                        speed = Random.nextFloat() * 0.002f + 0.001f,
                    ),
                )
            }
        }
    }

    // Confetti for CELEBRATING state
    val confettiList = remember {
        mutableStateListOf<ConfettiParticle>().apply {
            val colors = listOf(
                Color(0xFFFF5722), Color(0xFFFFEB3B), Color(0xFF4CAF50),
                Color(0xFF2196F3), Color(0xFFE91E63), Color(0xFF9C27B0),
            )
            repeat(30) {
                add(
                    ConfettiParticle(
                        xRatio = Random.nextFloat(),
                        yRatio = Random.nextFloat(),
                        sizeRatio = Random.nextFloat() * 0.012f + 0.006f,
                        speed = Random.nextFloat() * 0.004f + 0.002f,
                        color = colors[Random.nextInt(colors.size)],
                    ),
                )
            }
        }
    }

    // Splash particles
    val splashList = remember { mutableStateListOf<SplashParticle>() }

    LaunchedEffect(isSplashTriggered) {
        if (isSplashTriggered && !isReduceMotion) {
            splashList.clear()
            repeat(20) {
                val angle = Random.nextDouble(-Math.PI, 0.0)
                val speed = Random.nextFloat() * 12f + 4f
                splashList.add(
                    SplashParticle(
                        startX = Random.nextFloat(),
                        startY = 0.5f,
                        vx = (Math.cos(angle) * speed).toFloat(),
                        vy = (Math.sin(angle) * speed).toFloat(),
                        color = Color(0xFFBEE3F8),
                    ),
                )
            }
            onSplashFinished()
        }
    }

    // Colors according to theme
    val skyColor = if (isDark) Color(0xFF0F172A) else Color(0xFFE0F2FE)
    val sandColor = if (isDark) Color(0xFF45361A) else Color(0xFFE9C46A)
    val waterColor = if (isDark) Color(0xFF0284C7).copy(alpha = 0.75f) else Color(0xFF0077B6).copy(alpha = 0.65f)
    val deepWaterColor = if (isDark) Color(0xFF0369A1).copy(alpha = 0.85f) else Color(0xFF023E8A).copy(alpha = 0.8f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. Draw Sky / Background
        drawRect(color = skyColor, size = size)

        // 2. Draw Sand at bottom (fixed 12% height)
        val sandHeight = height * 0.12f
        val sandTopY = height - sandHeight

        val sandPath = Path().apply {
            moveTo(0f, height)
            lineTo(0f, sandTopY)
            quadraticTo(width * 0.5f, sandTopY - 15f, width, sandTopY + 10f)
            lineTo(width, height)
            close()
        }
        drawPath(path = sandPath, color = sandColor)

        // 3. Draw Water Layer
        // Water top position based on progress: 0% -> sand top; 100% -> near top of screen (10% from top)
        val maxWaterHeight = height - sandHeight - (height * 0.10f)
        val currentWaterHeight = sandHeight + (maxWaterHeight * animatedProgress)
        val waterTopY = height - currentWaterHeight

        if (animatedProgress > 0.02f) {
            val waterPath = Path().apply {
                moveTo(0f, height)
                lineTo(0f, waterTopY)

                // Sine wave surface
                val waveAmplitude = if (isReduceMotion) 0f else 12f
                val waveFrequency = 2
                val steps = 40
                for (i in 0..steps) {
                    val x = width * (i.toFloat() / steps)
                    val sinVal = sin((x / width * waveFrequency * 2 * Math.PI) + waveOffset).toFloat()
                    val y = waterTopY + sinVal * waveAmplitude
                    lineTo(x, y)
                }

                lineTo(width, height)
                close()
            }

            drawPath(path = waterPath, color = waterColor)

            // Deep water gradient/layer at bottom
            val deepWaterPath = Path().apply {
                moveTo(0f, height)
                lineTo(0f, waterTopY + currentWaterHeight * 0.4f)
                lineTo(width, waterTopY + currentWaterHeight * 0.4f)
                lineTo(width, height)
                close()
            }
            drawPath(path = deepWaterPath, color = deepWaterColor)

            // 4. Draw Rising Bubbles (when in water)
            if (!isReduceMotion) {
                bubbles.forEach { bubble ->
                    bubble.yRatio -= bubble.speed
                    if (bubble.yRatio < 0f) {
                        bubble.yRatio = 1f
                        bubble.xRatio = Random.nextFloat()
                    }

                    val bx = width * bubble.xRatio
                    val by = waterTopY + (currentWaterHeight * bubble.yRatio)
                    val br = width * bubble.radiusRatio

                    drawCircle(
                        color = Color.White.copy(alpha = 0.4f),
                        radius = br,
                        center = Offset(bx, by),
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.7f),
                        radius = br,
                        center = Offset(bx, by),
                        style = Stroke(width = 1.5f),
                    )
                }
            }
        }

        // 5. Draw Confetti when CELEBRATING
        if (sharkState == SharkState.CELEBRATING && !isReduceMotion) {
            confettiList.forEach { confetti ->
                confetti.yRatio += confetti.speed
                if (confetti.yRatio > 1f) {
                    confetti.yRatio = 0f
                    confetti.xRatio = Random.nextFloat()
                }

                val cx = width * confetti.xRatio
                val cy = height * confetti.yRatio
                val cSize = width * confetti.sizeRatio

                drawRect(
                    color = confetti.color,
                    topLeft = Offset(cx, cy),
                    size = Size(cSize, cSize * 1.5f),
                )
            }
        }

        // 6. Draw Splash Particles
        if (splashList.isNotEmpty() && !isReduceMotion) {
            val iterator = splashList.iterator()
            while (iterator.hasNext()) {
                val p = iterator.next()
                p.progress += 0.05f
                if (p.progress >= 1f) {
                    iterator.remove()
                    continue
                }

                val px = width * p.startX + p.vx * p.progress * 15f
                val py = waterTopY + p.vy * p.progress * 15f + 0.5f * 9.8f * p.progress * p.progress * 20f

                drawCircle(
                    color = p.color.copy(alpha = 1f - p.progress),
                    radius = 6f * (1f - p.progress * 0.5f),
                    center = Offset(px, py),
                )
            }
        }
    }
}
