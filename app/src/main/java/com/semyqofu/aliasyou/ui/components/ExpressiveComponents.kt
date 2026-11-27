package com.semyqofu.aliasyou.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Modifier that applies an expressive spring bounce effect when pressed.
 */
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    scaleDown: Float = 0.94f,
    onClick: () -> Unit
): Modifier = composed {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            while (true) {
                awaitPointerEventScope {
                    awaitFirstDown(requireUnconsumed = false)
                    scope.launch {
                        scale.animateTo(
                            targetValue = scaleDown,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                    val up = waitForUpOrCancellation()
                    scope.launch {
                        scale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                    if (up != null) {
                        onClick()
                    }
                }
            }
        }
}

/**
 * Modifier that creates a breathing/pulsing scale animation.
 */
fun Modifier.pulsing(
    minScale: Float = 0.96f,
    maxScale: Float = 1.04f,
    durationMs: Int = 1200
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    this.scale(pulseScale)
}

/**
 * Custom Material-style rounded Skull icon (24dp) for the extreme phrase difficulty.
 */
val SkullIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Skull",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.EvenOdd
        ) {
            // Outer skull silhouette with integrated tooth notches along the lower jaw
            moveTo(12f, 2.2f)
            curveTo(6.9f, 2.2f, 3.2f, 5.8f, 3.2f, 10.6f)
            curveTo(3.2f, 13.0f, 4.4f, 15.0f, 6.4f, 16.1f)
            lineTo(6.4f, 19.6f)
            curveTo(6.4f, 20.8f, 7.3f, 21.6f, 8.4f, 21.6f)
            lineTo(9.9f, 21.6f)
            lineTo(9.9f, 18.8f)
            curveTo(9.9f, 18.3f, 11.1f, 18.3f, 11.1f, 18.8f)
            lineTo(11.1f, 21.6f)
            lineTo(12.9f, 21.6f)
            lineTo(12.9f, 18.8f)
            curveTo(12.9f, 18.3f, 14.1f, 18.3f, 14.1f, 18.8f)
            lineTo(14.1f, 21.6f)
            lineTo(15.6f, 21.6f)
            curveTo(16.7f, 21.6f, 17.6f, 20.8f, 17.6f, 19.6f)
            lineTo(17.6f, 16.1f)
            curveTo(19.6f, 15.0f, 20.8f, 13.0f, 20.8f, 10.6f)
            curveTo(20.8f, 5.8f, 17.1f, 2.2f, 12f, 2.2f)
            close()

            // Left eye socket (circle)
            moveTo(8.5f, 9.2f)
            arcTo(2.0f, 2.0f, 0f, isMoreThanHalf = true, isPositiveArc = true, 8.49f, 9.2f)
            close()

            // Right eye socket (circle)
            moveTo(15.5f, 9.2f)
            arcTo(2.0f, 2.0f, 0f, isMoreThanHalf = true, isPositiveArc = true, 15.49f, 9.2f)
            close()

            // Nasal cavity
            moveTo(12f, 13.1f)
            lineTo(13.3f, 15.8f)
            quadTo(12f, 16.3f, 10.7f, 15.8f)
            close()
        }
    }.build()
}

/**
 * MD3 Expressive organic shapes:
 * - 7-sided cookie (семисторонняя печенька)
 * - 4-sided cookie (четырёхсторонняя печенька)
 * - Ellipse (эллипс)
 * - 9-sided sunny scallop (девятисторонняя печенька / солнце)
 * - 3-sided cookie (трёхсторонняя печенька)
 * - Squircle (сквиркл)
 */
enum class Md3ExpressiveShape {
    COOKIE_7,
    COOKIE_4,
    ELLIPSE,
    SUNNY_9,
    COOKIE_3,
    SQUIRCLE
}

fun computeExpressiveRadius(shape: Md3ExpressiveShape, angle: Float): Float {
    return when (shape) {
        Md3ExpressiveShape.COOKIE_7 -> 1.0f + 0.17f * cos(7f * angle) - 0.035f * cos(14f * angle)
        Md3ExpressiveShape.COOKIE_4 -> 1.0f + 0.19f * cos(4f * angle) - 0.045f * cos(8f * angle)
        Md3ExpressiveShape.ELLIPSE -> 1.0f + 0.24f * cos(2f * angle)
        Md3ExpressiveShape.SUNNY_9 -> 1.0f + 0.14f * cos(9f * angle) - 0.025f * cos(18f * angle)
        Md3ExpressiveShape.COOKIE_3 -> 1.0f + 0.18f * cos(3f * angle) - 0.035f * cos(6f * angle)
        Md3ExpressiveShape.SQUIRCLE -> 1.0f + 0.09f * cos(4f * angle)
    }
}

private fun maxExpressiveRadiusFactor(shape: Md3ExpressiveShape): Float {
    return when (shape) {
        Md3ExpressiveShape.COOKIE_7 -> 1.15f
        Md3ExpressiveShape.COOKIE_4 -> 1.16f
        Md3ExpressiveShape.ELLIPSE -> 1.24f
        Md3ExpressiveShape.SUNNY_9 -> 1.13f
        Md3ExpressiveShape.COOKIE_3 -> 1.16f
        Md3ExpressiveShape.SQUIRCLE -> 1.09f
    }
}

/**
 * Compose Shape that renders any [Md3ExpressiveShape] cleanly scaled to fit its bounds.
 */
class Md3ExpressivePolygonShape(
    private val shape: Md3ExpressiveShape,
    private val rotationDegrees: Float = 0f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val maxR = maxExpressiveRadiusFactor(shape)
        val baseRadius = (size.minDimension / 2f) / maxR
        val cx = size.width / 2f
        val cy = size.height / 2f
        val rotRad = Math.toRadians(rotationDegrees.toDouble()).toFloat()
        val numPoints = 120

        val path = Path()
        for (i in 0 until numPoints) {
            val angle = (i.toFloat() / numPoints) * (2f * Math.PI.toFloat())
            val r = computeExpressiveRadius(shape, angle) * baseRadius
            val a = angle + rotRad
            val x = cx + r * cos(a)
            val y = cy + r * sin(a)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/**
 * Returns true when the window is wide landscape (e.g., tablet in landscape orientation).
 */
fun isWideLandscape(maxWidth: Dp, maxHeight: Dp): Boolean =
    maxWidth >= 700.dp && maxWidth > maxHeight
