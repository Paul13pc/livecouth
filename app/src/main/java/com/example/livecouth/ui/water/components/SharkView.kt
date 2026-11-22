package com.example.livecouth.ui.water.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.livecouth.domain.model.SharkState

@Composable
fun SharkView(
    state: SharkState,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    isReduceMotion: Boolean = false,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SharkAnimation")

    // Tail oscillation speed and angle depending on SharkState
    val tailDuration = when (state) {
        SharkState.STRANDED -> 2500
        SharkState.RECOVERING -> 1200
        SharkState.SWIMMING -> 800
        SharkState.HAPPY -> 500
        SharkState.CELEBRATING -> 350
    }

    val tailAngle by if (isReduceMotion) {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = -12f,
            targetValue = 12f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = tailDuration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "TailWiggle",
        )
    }

    // Celebrating flip rotation
    val celebrateRotation by if (isReduceMotion || state != SharkState.CELEBRATING) {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "CelebrateRotation",
        )
    }

    // Vertical bobbing movement
    val verticalOffset by if (isReduceMotion) {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = if (state == SharkState.STRANDED) 0f else -6f,
            targetValue = if (state == SharkState.STRANDED) 3f else 6f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = if (state == SharkState.STRANDED) 2000 else 1000,
                    easing = FastOutSlowInEasing,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "VerticalBobbing",
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                rotationZ = celebrateRotation
                translationY = verticalOffset
            },
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = this.size.width
            val h = this.size.height

            // Colors
            val bodyColor = Color(0xFF4A90E2)
            val bellyColor = Color(0xFFF0F8FF)
            val strokeColor = Color(0xFF1E3A8A)
            val eyeColor = Color(0xFF1E293B)

            val centerY = h * 0.5f

            // 1. Draw Tail Fin (oscillating)
            val tailPath = Path().apply {
                moveTo(w * 0.15f, centerY)
                val topTailY = centerY - h * 0.25f + tailAngle
                val bottomTailY = centerY + h * 0.25f - tailAngle
                lineTo(w * 0.02f, topTailY)
                quadraticTo(w * 0.08f, centerY, w * 0.02f, bottomTailY)
                close()
            }
            drawPath(path = tailPath, color = bodyColor)
            drawPath(path = tailPath, color = strokeColor, style = Stroke(width = 3f))

            // 2. Main Body Path
            val bodyPath = Path().apply {
                // Snout at (w * 0.9f, centerY)
                moveTo(w * 0.9f, centerY)
                // Top curve back to tail
                cubicTo(
                    w * 0.75f, centerY - h * 0.35f,
                    w * 0.35f, centerY - h * 0.3f,
                    w * 0.18f, centerY,
                )
                // Bottom curve from tail to snout
                cubicTo(
                    w * 0.35f, centerY + h * 0.3f,
                    w * 0.75f, centerY + h * 0.35f,
                    w * 0.9f, centerY,
                )
                close()
            }
            drawPath(path = bodyPath, color = bodyColor)

            // 3. Belly Path
            val bellyPath = Path().apply {
                moveTo(w * 0.85f, centerY)
                cubicTo(
                    w * 0.7f, centerY + h * 0.15f,
                    w * 0.35f, centerY + h * 0.25f,
                    w * 0.22f, centerY + h * 0.05f,
                )
                cubicTo(
                    w * 0.35f, centerY + h * 0.3f,
                    w * 0.75f, centerY + h * 0.35f,
                    w * 0.85f, centerY,
                )
                close()
            }
            drawPath(path = bellyPath, color = bellyColor)

            // Outline around body
            drawPath(path = bodyPath, color = strokeColor, style = Stroke(width = 4f))

            // 4. Dorsal Fin (Top)
            val dorsalFinPath = Path().apply {
                moveTo(w * 0.45f, centerY - h * 0.26f)
                lineTo(w * 0.38f, centerY - h * 0.5f)
                quadraticTo(w * 0.48f, centerY - h * 0.35f, w * 0.58f, centerY - h * 0.22f)
                close()
            }
            drawPath(path = dorsalFinPath, color = bodyColor)
            drawPath(path = dorsalFinPath, color = strokeColor, style = Stroke(width = 3f))

            // 5. Pectoral Fin (Side)
            val pectoralFinPath = Path().apply {
                moveTo(w * 0.52f, centerY + h * 0.05f)
                lineTo(w * 0.42f, centerY + h * 0.35f)
                quadraticTo(w * 0.55f, centerY + h * 0.25f, w * 0.62f, centerY + h * 0.08f)
                close()
            }
            drawPath(path = pectoralFinPath, color = bodyColor)
            drawPath(path = pectoralFinPath, color = strokeColor, style = Stroke(width = 3f))

            // 6. Gills
            for (i in 0..2) {
                val gillX = w * (0.58f + i * 0.04f)
                drawLine(
                    color = strokeColor,
                    start = Offset(gillX, centerY - h * 0.08f),
                    end = Offset(gillX - w * 0.015f, centerY + h * 0.08f),
                    strokeWidth = 3f,
                )
            }

            // 7. Eyes based on SharkState
            val eyeX = w * 0.76f
            val eyeY = centerY - h * 0.08f

            when (state) {
                SharkState.STRANDED -> {
                    // Closed/tired eyes (X X shape)
                    val r = 8f
                    drawLine(
                        color = eyeColor,
                        start = Offset(eyeX - r, eyeY - r),
                        end = Offset(eyeX + r, eyeY + r),
                        strokeWidth = 4f,
                    )
                    drawLine(
                        color = eyeColor,
                        start = Offset(eyeX - r, eyeY + r),
                        end = Offset(eyeX + r, eyeY - r),
                        strokeWidth = 4f,
                    )
                }

                SharkState.RECOVERING -> {
                    // Normal open eye
                    drawCircle(color = eyeColor, radius = 9f, center = Offset(eyeX, eyeY))
                    drawCircle(color = Color.White, radius = 3f, center = Offset(eyeX + 3f, eyeY - 3f))
                }

                SharkState.SWIMMING -> {
                    // Happy open eye
                    drawCircle(color = eyeColor, radius = 10f, center = Offset(eyeX, eyeY))
                    drawCircle(color = Color.White, radius = 4f, center = Offset(eyeX + 3f, eyeY - 3f))
                }

                SharkState.HAPPY, SharkState.CELEBRATING -> {
                    // Big cheerful eye with double highlight or star
                    drawCircle(color = eyeColor, radius = 12f, center = Offset(eyeX, eyeY))
                    drawCircle(color = Color.White, radius = 4f, center = Offset(eyeX + 3f, eyeY - 4f))
                    drawCircle(color = Color.White, radius = 2f, center = Offset(eyeX - 3f, eyeY + 3f))

                    // Smile line below eye
                    val mouthPath = Path().apply {
                        moveTo(w * 0.78f, centerY + h * 0.08f)
                        quadraticTo(w * 0.83f, centerY + h * 0.15f, w * 0.87f, centerY + h * 0.05f)
                    }
                    drawPath(path = mouthPath, color = strokeColor, style = Stroke(width = 3f))
                }
            }
        }
    }
}
