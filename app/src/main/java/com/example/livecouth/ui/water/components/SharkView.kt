package com.example.livecouth.ui.water.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.livecouth.R
import com.example.livecouth.domain.model.SharkState
import kotlin.math.roundToInt

private enum class SharkExpression {
    THIRSTY,
    NORMAL,
    HAPPY,
}

@Composable
fun SharkView(
    state: SharkState,
    waterLevel: Float,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    isReduceMotion: Boolean = false,
    celebrate: Boolean = false,
) {
    val progress = waterLevel.coerceIn(0f, 1f)
    val stage = when {
        progress < 0.20f -> 0
        progress < 0.40f -> 1
        progress < 0.70f -> 2
        else -> 3
    }
    val expression = when {
        celebrate -> SharkExpression.HAPPY
        state == SharkState.THIRSTY -> SharkExpression.THIRSTY
        else -> SharkExpression.NORMAL
    }
    val targetX = when (stage) {
        0 -> 0.30f
        1 -> 0.35f
        2 -> 0.42f
        else -> 0.54f
    }
    val targetY = when (stage) {
        0 -> 0.54f
        1 -> 0.52f
        2 -> 0.46f
        else -> 0.40f
    }
    val positionAnimation: FiniteAnimationSpec<Float> = if (isReduceMotion) {
        tween(durationMillis = 0)
    } else {
        tween(durationMillis = 900, easing = FastOutSlowInEasing)
    }
    val baseX = animateFloatAsState(targetValue = targetX, animationSpec = positionAnimation, label = "SharkPositionX")
    val baseY = animateFloatAsState(targetValue = targetY, animationSpec = positionAnimation, label = "SharkPositionY")
    val infiniteTransition = rememberInfiniteTransition(label = "SharkMotion")
    val phaseState: State<Float> = if (isReduceMotion) {
        remember { mutableFloatStateOf(0f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 5000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "SharkPhase",
        )
    }
    val celebrationRotation = remember { Animatable(0f) }
    LaunchedEffect(celebrate, isReduceMotion) {
        if (celebrate && !isReduceMotion) {
            celebrationRotation.snapTo(0f)
            celebrationRotation.animateTo(
                targetValue = 360f,
                animationSpec = tween(durationMillis = 1700, easing = FastOutSlowInEasing),
            )
        } else {
            celebrationRotation.snapTo(0f)
        }
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val sharkSize = minOf(size, maxWidth, maxHeight)
        val horizontalSpace = (maxWidth - sharkSize).coerceAtLeast(0.dp)
        val verticalSpace = (maxHeight - sharkSize).coerceAtLeast(0.dp)
        val density = LocalDensity.current
        val horizontalSpacePx = with(density) { horizontalSpace.toPx() }
        val verticalSpacePx = with(density) { verticalSpace.toPx() }
        val sharkSizePx = with(density) { sharkSize.toPx() }
        Box(
            modifier = Modifier
                .offset {
                    val motion = sharkMotion(stage, phaseState.value)
                    IntOffset(
                        x = ((baseX.value + motion.x).coerceIn(0f, 1f) * horizontalSpacePx).roundToInt(),
                        y = (baseY.value.coerceIn(0f, 1f) * verticalSpacePx).roundToInt(),
                    )
                }
                .size(sharkSize),
        ) {
            SharkIllustration(
                expression = expression,
                stage = stage,
                phaseState = phaseState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val motion = sharkMotion(stage, phaseState.value)
                        translationY = motion.bob * sharkSizePx
                        rotationZ = motion.rotation + celebrationRotation.value
                    },
            )
        }
    }
}

@Composable
private fun SharkIllustration(
    expression: SharkExpression,
    stage: Int = 0,
    phaseState: State<Float>? = null,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val phase = phaseState?.value ?: 0f
        val motion = sharkMotion(stage, phase)
        drawShark(expression, phase * 2f * Math.PI.toFloat(), motion.splash)
    }
}

private fun sharkMotion(stage: Int, phase: Float): SharkMotion {
    val radians = phase * 2f * Math.PI.toFloat()
    return when (stage) {
        0 -> {
            val hopCycle = phase
            val hop = if (hopCycle < 0.18f) {
                kotlin.math.sin((hopCycle / 0.18f) * Math.PI.toFloat())
            } else {
                0f
            }
            SharkMotion(bob = -0.08f * hop, rotation = kotlin.math.sin(radians) * hop * 5f)
        }

        1 -> {
            val surge = when {
                phase < 0.28f -> smoothStep(phase / 0.28f)
                phase < 0.42f -> 1f
                phase < 0.72f -> 1f - smoothStep((phase - 0.42f) / 0.30f)
                else -> 0f
            }
            SharkMotion(
                x = 0.22f * surge,
                bob = -0.025f * surge,
                rotation = kotlin.math.sin(radians) * surge * 5f,
                splash = surge,
            )
        }

        2 -> {
            val jumpCycle = (phase * 1.65f) % 1f
            val jump = if (jumpCycle < 0.23f) {
                kotlin.math.sin((jumpCycle / 0.23f) * Math.PI.toFloat())
            } else {
                0f
            }
            SharkMotion(
                x = kotlin.math.sin(radians) * 0.08f,
                bob = -0.14f * jump,
                rotation = kotlin.math.sin(radians) * 4f + (if (jumpCycle < 0.23f) -16f * jump else 0f),
                splash = if (jumpCycle in 0.23f..0.30f) 1f - (jumpCycle - 0.23f) / 0.07f else 0f,
            )
        }

        else -> SharkMotion(
            x = kotlin.math.sin(radians) * 0.10f,
            bob = kotlin.math.sin(radians * 1.8f) * 0.055f,
            rotation = kotlin.math.sin(radians) * 7f,
        )
    }
}

private data class SharkMotion(
    val x: Float = 0f,
    val bob: Float = 0f,
    val rotation: Float = 0f,
    val splash: Float = 0f,
)

private fun DrawScope.drawShark(
    expression: SharkExpression,
    tailPhase: Float,
    splash: Float,
) {
    val w = size.width
    val h = size.height
    val unit = minOf(w, h)
    val outline = Color(0xFF263F50)
    val body = Color(0xFF7895A6)
    val back = Color(0xFF55798D)
    val belly = Color(0xFFF4F7F5)
    val lineWidth = unit * 0.018f
    val tailWiggle = kotlin.math.sin(tailPhase) * h * 0.045f

    val tail = Path().apply {
        moveTo(w * 0.25f, h * 0.47f)
        cubicTo(w * 0.16f, h * 0.40f, w * 0.11f, h * 0.24f + tailWiggle, w * 0.035f, h * 0.15f + tailWiggle)
        cubicTo(w * 0.08f, h * 0.32f + tailWiggle * 0.5f, w * 0.10f, h * 0.43f, w * 0.19f, h * 0.50f)
        cubicTo(w * 0.10f, h * 0.58f, w * 0.09f, h * 0.72f - tailWiggle * 0.5f, w * 0.035f, h * 0.85f - tailWiggle)
        cubicTo(w * 0.16f, h * 0.77f - tailWiggle * 0.5f, w * 0.22f, h * 0.62f, w * 0.27f, h * 0.54f)
        close()
    }
    drawPath(tail, back)
    drawPath(tail, outline, style = Stroke(width = lineWidth))

    val rearFin = Path().apply {
        moveTo(w * 0.30f, h * 0.34f)
        cubicTo(w * 0.25f, h * 0.20f, w * 0.30f, h * 0.19f, w * 0.39f, h * 0.31f)
        close()
    }
    drawPath(rearFin, back)
    drawPath(rearFin, outline, style = Stroke(width = lineWidth))

    val farPectoralFin = Path().apply {
        moveTo(w * 0.53f, h * 0.55f)
        cubicTo(w * 0.59f, h * 0.49f, w * 0.70f, h * 0.48f, w * 0.78f, h * 0.57f)
        cubicTo(w * 0.68f, h * 0.63f, w * 0.60f, h * 0.64f, w * 0.53f, h * 0.55f)
        close()
    }
    drawPath(farPectoralFin, back)

    val dorsalFin = Path().apply {
        moveTo(w * 0.36f, h * 0.31f)
        cubicTo(w * 0.40f, h * 0.19f, w * 0.45f, h * 0.07f, w * 0.51f, h * 0.04f)
        cubicTo(w * 0.58f, h * 0.12f, w * 0.63f, h * 0.23f, w * 0.69f, h * 0.31f)
        close()
    }
    drawPath(dorsalFin, back)
    drawPath(dorsalFin, outline, style = Stroke(width = lineWidth))

    val bodyPath = Path().apply {
        moveTo(w * 0.95f, h * 0.43f)
        cubicTo(w * 0.90f, h * 0.31f, w * 0.78f, h * 0.24f, w * 0.59f, h * 0.24f)
        cubicTo(w * 0.42f, h * 0.20f, w * 0.25f, h * 0.30f, w * 0.18f, h * 0.43f)
        cubicTo(w * 0.15f, h * 0.49f, w * 0.17f, h * 0.55f, w * 0.23f, h * 0.60f)
        cubicTo(w * 0.38f, h * 0.75f, w * 0.65f, h * 0.76f, w * 0.82f, h * 0.61f)
        cubicTo(w * 0.91f, h * 0.54f, w * 0.95f, h * 0.48f, w * 0.95f, h * 0.43f)
        close()
    }
    drawPath(bodyPath, body)

    val backPatch = Path().apply {
        moveTo(w * 0.95f, h * 0.43f)
        cubicTo(w * 0.90f, h * 0.31f, w * 0.78f, h * 0.24f, w * 0.59f, h * 0.24f)
        cubicTo(w * 0.42f, h * 0.20f, w * 0.25f, h * 0.30f, w * 0.18f, h * 0.43f)
        cubicTo(w * 0.39f, h * 0.38f, w * 0.69f, h * 0.37f, w * 0.95f, h * 0.43f)
        close()
    }
    drawPath(backPatch, back)

    val bellyPath = Path().apply {
        moveTo(w * 0.94f, h * 0.50f)
        cubicTo(w * 0.75f, h * 0.55f, w * 0.46f, h * 0.59f, w * 0.22f, h * 0.55f)
        cubicTo(w * 0.36f, h * 0.72f, w * 0.66f, h * 0.73f, w * 0.83f, h * 0.61f)
        cubicTo(w * 0.90f, h * 0.57f, w * 0.93f, h * 0.53f, w * 0.94f, h * 0.50f)
        close()
    }
    drawPath(bellyPath, belly)

    val nearPectoralFin = Path().apply {
        moveTo(w * 0.47f, h * 0.59f)
        cubicTo(w * 0.43f, h * 0.69f, w * 0.37f, h * 0.80f, w * 0.32f, h * 0.91f)
        cubicTo(w * 0.50f, h * 0.84f, w * 0.63f, h * 0.72f, w * 0.70f, h * 0.59f)
        close()
    }
    drawPath(nearPectoralFin, back)
    drawPath(nearPectoralFin, outline, style = Stroke(width = lineWidth))
    drawPath(bodyPath, outline, style = Stroke(width = lineWidth))

    for (index in 0..2) {
        val gillX = w * (0.65f + index * 0.045f)
        val gill = Path().apply {
            moveTo(gillX, h * 0.36f)
            cubicTo(gillX - w * 0.018f, h * 0.42f, gillX - w * 0.025f, h * 0.49f, gillX - w * 0.045f, h * 0.53f)
        }
        drawPath(gill, Color(0xFF405F70), style = Stroke(width = lineWidth * 0.75f))
    }

    val eyeCenter = Offset(w * 0.78f, h * 0.36f)
    val eyeRadius = unit * 0.052f
    drawCircle(Color(0xFFFDFEFE), eyeRadius, eyeCenter)
    drawCircle(Color(0xFF153345), eyeRadius * 0.62f, Offset(eyeCenter.x + eyeRadius * 0.18f, eyeCenter.y))
    drawCircle(Color.White, eyeRadius * 0.22f, Offset(eyeCenter.x + eyeRadius * 0.34f, eyeCenter.y - eyeRadius * 0.32f))

    val mouthOpen = when (expression) {
        SharkExpression.THIRSTY -> 0.105f + kotlin.math.sin(tailPhase * 0.7f) * 0.012f
        SharkExpression.NORMAL -> 0.025f + kotlin.math.sin(tailPhase) * 0.006f
        SharkExpression.HAPPY -> 0.09f + kotlin.math.sin(tailPhase) * 0.008f
    }
    val mouthTop = h * 0.475f
    val mouthBottom = mouthTop + h * mouthOpen
    val mouth = Path().apply {
        moveTo(w * 0.79f, mouthTop)
        cubicTo(w * 0.85f, h * 0.455f, w * 0.92f, h * 0.46f, w * 0.95f, h * 0.48f)
        cubicTo(w * 0.93f, h * 0.52f + h * mouthOpen, w * 0.86f, mouthBottom, w * 0.79f, mouthBottom)
        close()
    }
    drawPath(mouth, Color(0xFF233746))

    val jaw = Path().apply {
        moveTo(w * 0.79f, mouthBottom - h * 0.01f)
        cubicTo(w * 0.83f, mouthBottom + h * 0.035f, w * 0.90f, mouthBottom + h * 0.035f, w * 0.945f, mouthBottom - h * 0.015f)
        cubicTo(w * 0.92f, h * 0.63f, w * 0.85f, h * 0.62f, w * 0.80f, h * 0.57f)
        close()
    }
    drawPath(jaw, body)
    drawPath(jaw, outline, style = Stroke(width = lineWidth * 0.75f))

    for (index in 0..4) {
        val toothX = w * (0.83f + index * 0.021f)
        val toothWidth = w * 0.022f
        val tooth = Path().apply {
            moveTo(toothX, mouthTop + h * 0.003f)
            lineTo(toothX + toothWidth * 0.5f, mouthTop + h * 0.047f)
            lineTo(toothX + toothWidth, mouthTop + h * 0.003f)
            close()
        }
        drawPath(tooth, Color.White)
    }

    when (expression) {
        SharkExpression.THIRSTY -> {
            drawLine(
                color = outline,
                start = Offset(eyeCenter.x - eyeRadius, eyeCenter.y - eyeRadius * 0.08f),
                end = Offset(eyeCenter.x + eyeRadius, eyeCenter.y + eyeRadius * 0.18f),
                strokeWidth = lineWidth,
            )
            val tongue = Path().apply {
                moveTo(w * 0.86f, mouthBottom - h * 0.006f)
                cubicTo(w * 0.88f, mouthBottom + h * 0.025f, w * 0.91f, mouthBottom + h * 0.025f, w * 0.925f, mouthBottom - h * 0.006f)
                close()
            }
            drawPath(tongue, Color(0xFFE98288))
            drawCircle(Color(0xFF72CFE0), unit * 0.018f, Offset(w * 0.71f, h * 0.28f))
        }

        SharkExpression.NORMAL -> {
            drawLine(
                color = outline,
                start = Offset(eyeCenter.x - eyeRadius, eyeCenter.y - eyeRadius * 1.15f),
                end = Offset(eyeCenter.x + eyeRadius, eyeCenter.y - eyeRadius * 1.15f),
                strokeWidth = lineWidth,
            )
        }

        SharkExpression.HAPPY -> {
            drawCircle(Color(0xFFE99A9A).copy(alpha = 0.55f), unit * 0.025f, Offset(w * 0.72f, h * 0.47f))
            drawCircle(Color(0xFFE99A9A).copy(alpha = 0.55f), unit * 0.025f, Offset(w * 0.93f, h * 0.47f))
            drawLine(
                color = outline,
                start = Offset(eyeCenter.x - eyeRadius, eyeCenter.y - eyeRadius * 1.15f),
                end = Offset(eyeCenter.x + eyeRadius, eyeCenter.y - eyeRadius * 1.15f),
                strokeWidth = lineWidth,
            )
        }
    }

    if (splash > 0f) {
        for (index in 0..5) {
            val direction = if (index % 2 == 0) -1f else 1f
            val spread = unit * (0.08f + index * 0.025f) * splash
            drawCircle(
                color = Color(0xFFD8F8FF).copy(alpha = splash * 0.8f),
                radius = unit * (0.012f + (index % 3) * 0.004f),
                center = Offset(w * 0.53f + spread * direction, h * 0.80f - splash * unit * 0.08f),
            )
        }
    }
}

private fun smoothStep(value: Float): Float {
    val clamped = value.coerceIn(0f, 1f)
    return clamped * clamped * (3f - 2f * clamped)
}

@Preview(showBackground = true, widthDp = 420, heightDp = 190)
@Composable
private fun SharkExpressionsPreview() {
    MaterialTheme {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SharkExpressionPreview(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.shark_expression_thirsty),
                expression = SharkExpression.THIRSTY,
            )
            SharkExpressionPreview(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.shark_expression_normal),
                expression = SharkExpression.NORMAL,
            )
            SharkExpressionPreview(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.shark_expression_happy),
                expression = SharkExpression.HAPPY,
            )
        }
    }
}

@Composable
private fun SharkExpressionPreview(
    modifier: Modifier,
    label: String,
    expression: SharkExpression,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        SharkIllustration(
            expression = expression,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
        )
    }
}
