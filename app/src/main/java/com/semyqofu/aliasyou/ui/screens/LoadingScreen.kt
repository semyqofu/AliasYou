package com.semyqofu.aliasyou.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semyqofu.aliasyou.ui.LocalAppStrings
import com.semyqofu.aliasyou.ui.components.Md3ExpressiveShape
import com.semyqofu.aliasyou.ui.components.computeExpressiveRadius
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Md3MorphingShape(
    modifier: Modifier = Modifier
) {
    val shapes = remember {
        listOf(
            Md3ExpressiveShape.COOKIE_7,
            Md3ExpressiveShape.COOKIE_4,
            Md3ExpressiveShape.ELLIPSE,
            Md3ExpressiveShape.SUNNY_9,
            Md3ExpressiveShape.COOKIE_3,
            Md3ExpressiveShape.SQUIRCLE
        )
    }

    var currentShapeIdx by remember { mutableIntStateOf(0) }
    val morphProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            // Hold shape for 700ms so user clearly distinguishes it
            delay(700)
            // Smoothly morph to next shape over 850ms
            morphProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
            )
            currentShapeIdx = (currentShapeIdx + 1) % shapes.size
            morphProgress.snapTo(0f)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "morphAnimation")

    // Slow organic continuous rotation
    val continuousRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "continuousRotate"
    )

    // Gentle breathing scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val containerColor = MaterialTheme.colorScheme.primaryContainer

    val shape1 = shapes[currentShapeIdx]
    val shape2 = shapes[(currentShapeIdx + 1) % shapes.size]
    val progress = morphProgress.value

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
            },
        contentAlignment = Alignment.Center
    ) {
        // Rotating Morphing Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(continuousRotation)
        ) {
            val numPoints = 180
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseRadius = (size.minDimension / 2f) * 0.76f

            val path = Path()
            for (i in 0 until numPoints) {
                val angle = (i.toFloat() / numPoints) * (2f * Math.PI.toFloat())
                val r1 = computeExpressiveRadius(shape1, angle)
                val r2 = computeExpressiveRadius(shape2, angle)
                val r = (1f - progress) * r1 + progress * r2

                val x = cx + r * baseRadius * cos(angle)
                val y = cy + r * baseRadius * sin(angle)

                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            path.close()

            // Subtle tonal shadow
            drawPath(
                path = path,
                color = primaryColor.copy(alpha = 0.12f)
            )

            // Main MD3 Primary Container Fill
            drawPath(
                path = path,
                color = containerColor
            )

            // Refined expressive border outline
            drawPath(
                path = path,
                color = primaryColor.copy(alpha = 0.35f),
                style = Stroke(width = 2.5.dp.toPx())
            )
        }

        // Centered AutoAwesome icon
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(34.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun LoadingScreen(message: String? = null) {
    val strings = LocalAppStrings.current
    val hints = remember(strings) { strings.loadingHints }

    var currentHintIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2400)
            currentHintIndex = (currentHintIndex + 1) % hints.size
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Material 3 Expressive Shape Morphing Component (compact size)
            Md3MorphingShape(
                modifier = Modifier.size(108.dp)
            )

            Spacer(modifier = Modifier.height(26.dp))

            Text(
                text = "Alias You",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Animated Cycling Hint Text
            AnimatedContent(
                targetState = hints[currentHintIndex],
                transitionSpec = {
                    (fadeIn(animationSpec = tween(400)) + slideInVertically { 20 })
                        .togetherWith(fadeOut(animationSpec = tween(300)) + slideOutVertically { -20 })
                },
                label = "loadingHintAnimation"
            ) { hint ->
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .width(160.dp)
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
