package com.semyqofu.aliasyou.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semyqofu.aliasyou.ui.AppStrings
import com.semyqofu.aliasyou.ui.components.Md3ExpressiveShape
import com.semyqofu.aliasyou.ui.components.bouncyClickable
import com.semyqofu.aliasyou.ui.components.computeExpressiveRadius
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LanguageOnboardingScreen(
    initialLanguage: String = "ru",
    onLanguageSelected: (String) -> Unit,
    onContinue: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedLanguage by remember { mutableStateOf(initialLanguage) }
    val strings = remember(selectedLanguage) { AppStrings.forLanguage(selectedLanguage) }

    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    val triggerVibration = { duration: Long ->
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pixelOnboardingShapes")
    val topBlobRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "topBlobRotation"
    )
    val bottomBlobRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(36000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bottomBlobRotation"
    )

    val topBlobColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
    val bottomBlobColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Pixel-style slowly rotating scalloped organic background shapes
            Canvas(modifier = Modifier.fillMaxSize()) {
                val topRadius = size.minDimension * 0.44f
                val topCx = size.width * 0.88f
                val topCy = size.height * 0.06f
                drawScallopedBlob(
                    cx = topCx,
                    cy = topCy,
                    baseRadius = topRadius,
                    rotationDegrees = topBlobRotation,
                    shape = Md3ExpressiveShape.COOKIE_7,
                    color = topBlobColor
                )

                val bottomRadius = size.minDimension * 0.78f
                val bottomCx = size.width * 0.42f
                val bottomCy = size.height * 0.90f
                drawScallopedBlob(
                    cx = bottomCx,
                    cy = bottomCy,
                    baseRadius = bottomRadius,
                    rotationDegrees = bottomBlobRotation,
                    shape = Md3ExpressiveShape.COOKIE_7,
                    color = bottomBlobColor
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top-left circular Pixel badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 2.dp,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Casino,
                                contentDescription = "Alias You",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                // Bottom-center Greeting, Language Picker & Pixel Navigation Pills
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Animated Greeting ("Привет!" / "Hello!")
                    AnimatedContent(
                        targetState = strings,
                        transitionSpec = {
                            (fadeIn(tween(300)) + slideInVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) { 24 } + scaleIn(initialScale = 0.94f))
                                .togetherWith(fadeOut(tween(200)) + slideOutVertically { -20 } + scaleOut(targetScale = 0.96f))
                        },
                        label = "greetingTransition"
                    ) { s ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = s.onboardingGreeting,
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = s.onboardingSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }

                    // Language Choice Pills (Russian / English)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LanguageChoicePill(
                            label = "Русский",
                            subLabel = "5 000+ слов",
                            selected = selectedLanguage == "ru",
                            onClick = {
                                triggerVibration(20)
                                selectedLanguage = "ru"
                                onLanguageSelected("ru")
                            },
                            modifier = Modifier.weight(1f)
                        )
                        LanguageChoicePill(
                            label = "English",
                            subLabel = "5,300+ words",
                            selected = selectedLanguage == "en",
                            onClick = {
                                triggerVibration(20)
                                selectedLanguage = "en"
                                onLanguageSelected("en")
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Pixel-style Bottom Action Row (Language toggle pill + Primary Arrow pill)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = {
                                triggerVibration(15)
                                val next = if (selectedLanguage == "ru") "en" else "ru"
                                selectedLanguage = next
                                onLanguageSelected(next)
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 2.dp,
                            modifier = Modifier
                                .width(84.dp)
                                .height(58.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Translate,
                                    contentDescription = strings.onboardingSwitchLanguageCd,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                triggerVibration(45)
                                onContinue(selectedLanguage)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                                .bouncyClickable {
                                    triggerVibration(45)
                                    onContinue(selectedLanguage)
                                },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = strings.onboardingContinue,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageChoicePill(
    label: String,
    subLabel: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.surfaceContainerLowest
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)
        },
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        tonalElevation = if (selected) 3.dp else 0.dp,
        modifier = modifier
            .height(68.dp)
            .clip(MaterialTheme.shapes.large)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subLabel,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawScallopedBlob(
    cx: Float,
    cy: Float,
    baseRadius: Float,
    rotationDegrees: Float,
    shape: Md3ExpressiveShape,
    color: androidx.compose.ui.graphics.Color
) {
    val numPoints = 180
    val rotRad = Math.toRadians(rotationDegrees.toDouble()).toFloat()
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
    drawPath(path = path, color = color)
}
