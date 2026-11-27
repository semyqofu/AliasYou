package com.semyqofu.aliasyou.ui.screens

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.semyqofu.aliasyou.ui.LocalAppStrings
import com.semyqofu.aliasyou.ui.components.bouncyClickable
import com.semyqofu.aliasyou.ui.components.isWideLandscape
import com.semyqofu.aliasyou.ui.components.pulsing
import com.semyqofu.aliasyou.ui.theme.OnSuccessGreenContainer
import com.semyqofu.aliasyou.ui.theme.SuccessGreen
import com.semyqofu.aliasyou.ui.theme.SuccessGreenContainer
import com.semyqofu.aliasyou.viewmodel.GameSettings
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun GameScreen(
    words: List<String>,
    currentIndex: Int,
    settings: GameSettings,
    activeTeam: String,
    teamNames: List<String>,
    onAddScore: (String, Int) -> Unit,
    onRoundFinished: (Int) -> Unit
) {
    val strings = LocalAppStrings.current
    var roundScore by remember { mutableIntStateOf(0) }
    var wordsProcessed by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(settings.roundDuration) }
    var isRoundActive by remember { mutableStateOf(true) }
    var showTeamDialog by remember { mutableStateOf(false) }
    var pendingScore by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }
    val cardAlpha = remember { Animatable(1f) }

    val context = LocalContext.current

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

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            kotlinx.coroutines.delay(1000)
            timeLeft--
        }
        isRoundActive = false
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager?.ringerMode != AudioManager.RINGER_MODE_SILENT) {
            triggerVibration(120)
        }
        playRoundEndTone(context)
    }

    val isUrgent = timeLeft in 1..10
    val progress = (timeLeft.toFloat() / settings.roundDuration).coerceIn(0f, 1f)

    if (showTeamDialog) {
        AlertDialog(
            onDismissRequest = { showTeamDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    strings.lastWordTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        strings.lastWordSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    teamNames.forEach { team ->
                        FilledTonalButton(
                            onClick = {
                                triggerVibration(45)
                                onAddScore(team, pendingScore)
                                showTeamDialog = false
                                onRoundFinished(wordsProcessed + 1)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .bouncyClickable {
                                    triggerVibration(45)
                                    onAddScore(team, pendingScore)
                                    showTeamDialog = false
                                    onRoundFinished(wordsProcessed + 1)
                                },
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                team,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        showTeamDialog = false
                        onRoundFinished(wordsProcessed + 1)
                    }
                ) {
                    Text(strings.nobodyGuessed)
                }
            },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    val dragY = offsetY.value
    val isDraggingUp = dragY < -30f
    val isDraggingDown = dragY > 30f
    val dragFraction = (abs(dragY) / 140f).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val headerCard: @Composable () -> Unit = {
            // Header: Team name + Round score + Timer
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.teamTurnLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = activeTeam,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }

                        // Score ticker
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (roundScore >= 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.scorePrefix,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (roundScore >= 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                                AnimatedContent(
                                    targetState = roundScore,
                                    transitionSpec = {
                                        if (targetState > initialState) {
                                            slideInVertically { height -> height } + fadeIn() togetherWith
                                                    slideOutVertically { height -> -height } + fadeOut()
                                        } else {
                                            slideInVertically { height -> -height } + fadeIn() togetherWith
                                                    slideOutVertically { height -> height } + fadeOut()
                                        }
                                    },
                                    label = "scoreTicker"
                                ) { score ->
                                    Text(
                                        text = "$score",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (score >= 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        // Timer Badge
                        Box(
                            modifier = Modifier
                                .then(if (isUrgent) Modifier.pulsing(minScale = 0.92f, maxScale = 1.08f, durationMs = 600) else Modifier)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isUrgent) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isUrgent) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$timeLeft",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isUrgent) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = when {
                            timeLeft <= 10 -> MaterialTheme.colorScheme.error
                            timeLeft <= 20 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        val upHintPill: @Composable () -> Unit = {
            // Upper Hint Pill (ОТГАДАНО)
            val upScale = if (isDraggingUp) 1f + (dragFraction * 0.12f) else 1f
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDraggingUp) SuccessGreenContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .scale(upScale)
                    .width(180.dp),
                tonalElevation = if (isDraggingUp) 4.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowUpward,
                        contentDescription = null,
                        tint = if (isDraggingUp) OnSuccessGreenContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.guessedPill,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDraggingUp) OnSuccessGreenContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Two-Card Stack (Deck Pattern)
        val currentWord = words.getOrNull(currentIndex + wordsProcessed)
            ?.let(::formatCardText)
            ?: strings.outOfWords

        val nextWord = words.getOrNull(currentIndex + wordsProcessed + 1)
            ?.let(::formatCardText)

        val cardColor = MaterialTheme.colorScheme.primary
        val onCardColor = MaterialTheme.colorScheme.onPrimary

        val wordDeck: @Composable (Dp) -> Unit = { deckCardHeight ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(deckCardHeight + 10.dp),
                contentAlignment = Alignment.Center
            ) {
                // Card 2: Background Next Card (revealed smoothly underneath)
                if (nextWord != null) {
                    val bgScale = 0.92f + (0.08f * dragFraction)
                    val bgAlpha = 0.45f + (0.55f * dragFraction)
                    val bgOffsetY = (12.dp * (1f - dragFraction)).value

                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.96f)
                            .height(deckCardHeight)
                            .graphicsLayer {
                                scaleX = bgScale
                                scaleY = bgScale
                                alpha = bgAlpha
                                translationY = bgOffsetY
                            },
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(
                            containerColor = cardColor
                        ),
                        border = null,
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        WordCardInnerContent(
                            numberLabel = strings.cardNumber(wordsProcessed + 2),
                            wordText = nextWord,
                            onCardColor = onCardColor,
                            isForeground = false
                        )
                    }
                }

                // Card 1: Foreground Top Card (Draggable with physical fling)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(deckCardHeight)
                        .zIndex(2f)
                        .graphicsLayer {
                            translationY = dragY
                            rotationZ = (dragY / 22f).coerceIn(-12f, 12f)
                            alpha = cardAlpha.value
                        }
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta ->
                                scope.launch { offsetY.snapTo(offsetY.value + delta) }
                            },
                            onDragStopped = {
                                scope.launch {
                                    val finalY = offsetY.value
                                    if (finalY < -100f) {
                                        // Swipe UP (Отгадано)
                                        triggerVibration(30)
                                        // Continuous fling out
                                        launch { cardAlpha.animateTo(0f, tween(160, easing = FastOutLinearInEasing)) }
                                        offsetY.animateTo(-1200f, tween(160, easing = FastOutLinearInEasing))

                                        if (!isRoundActive) {
                                            pendingScore = 1
                                            showTeamDialog = true
                                        } else {
                                            roundScore++
                                            onAddScore(activeTeam, 1)
                                            wordsProcessed++
                                        }

                                        // Reset position seamlessly
                                        offsetY.snapTo(0f)
                                        cardAlpha.snapTo(1f)
                                    } else if (finalY > 100f) {
                                        // Swipe DOWN (Пропуск)
                                        triggerVibration(30)
                                        // Continuous fling out
                                        launch { cardAlpha.animateTo(0f, tween(160, easing = FastOutLinearInEasing)) }
                                        offsetY.animateTo(1200f, tween(160, easing = FastOutLinearInEasing))

                                        roundScore -= settings.skipPenalty
                                        onAddScore(activeTeam, -settings.skipPenalty)
                                        wordsProcessed++

                                        if (!isRoundActive) {
                                            onRoundFinished(wordsProcessed)
                                        }

                                        // Reset position seamlessly
                                        offsetY.snapTo(0f)
                                        cardAlpha.snapTo(1f)
                                    } else {
                                        // Spring smoothly back to center
                                        offsetY.animateTo(
                                            0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                }
                            }
                        ),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = cardColor
                    ),
                    border = null,
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (abs(dragY) > 20f) 14.dp else 4.dp
                    )
                ) {
                    WordCardInnerContent(
                        numberLabel = strings.cardNumber(wordsProcessed + 1),
                        wordText = currentWord,
                        onCardColor = onCardColor,
                        isForeground = true
                    )
                }
            }
        }

        val downHintPill: @Composable () -> Unit = {
            // Lower Hint Pill (ПРОПУСК)
            val downScale = if (isDraggingDown) 1f + (dragFraction * 0.12f) else 1f
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDraggingDown) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .scale(downScale)
                    .width(180.dp),
                tonalElevation = if (isDraggingDown) 4.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowDownward,
                        contentDescription = null,
                        tint = if (isDraggingDown) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.skipPill(settings.skipPenalty),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDraggingDown) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            if (isWideLandscape(maxWidth, maxHeight)) {
                val deckHeight = (maxHeight * 0.52f).coerceIn(240.dp, 320.dp)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 36.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(36.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(0.85f),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.widthIn(max = 420.dp)) {
                            headerCard()
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1.15f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        upHintPill()
                        Box(modifier = Modifier.widthIn(max = 460.dp)) {
                            wordDeck(deckHeight)
                        }
                        downHintPill()
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 540.dp)
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        headerCard()
                        Spacer(modifier = Modifier.height(16.dp))
                        upHintPill()
                        Spacer(modifier = Modifier.weight(1f))
                        wordDeck(290.dp)
                        Spacer(modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.padding(bottom = 16.dp)) {
                            downHintPill()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WordCardInnerContent(
    numberLabel: String,
    wordText: String,
    onCardColor: androidx.compose.ui.graphics.Color,
    isForeground: Boolean
) {
    val (textStyle, lineHeight) = when {
        wordText.length > 28 -> MaterialTheme.typography.titleLarge to 27.sp
        wordText.length > 16 -> MaterialTheme.typography.headlineMedium to 32.sp
        else -> MaterialTheme.typography.headlineLarge to 38.sp
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = onCardColor.copy(alpha = if (isForeground) 0.18f else 0.15f),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = numberLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isForeground) onCardColor else onCardColor.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Text(
                text = wordText,
                style = textStyle,
                fontWeight = if (isForeground) FontWeight.ExtraBold else FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (isForeground) onCardColor else onCardColor.copy(alpha = 0.7f),
                lineHeight = lineHeight
            )
        }
    }
}

private fun formatCardText(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return trimmed
    // Preserve intentional internal capitalization (e.g., proper nouns in phrases) unless ALL CAPS
    val hasLowercase = trimmed.any { it.isLowerCase() }
    val normalized = if (hasLowercase) trimmed else trimmed.lowercase()
    return normalized.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

/**
 * Checks whether system ringer mode, Do Not Disturb filter, and media volume permit playing
 * the round-end audio cue.
 */
internal fun isSoundPlaybackAllowed(
    ringerMode: Int,
    interruptionFilter: Int?,
    musicVolume: Int,
    isMusicMuted: Boolean
): Boolean {
    if (ringerMode != AudioManager.RINGER_MODE_NORMAL) return false
    if (interruptionFilter != null && interruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
        return false
    }
    return musicVolume > 0 && !isMusicMuted
}

private suspend fun playRoundEndTone(context: Context) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    val allowed = isSoundPlaybackAllowed(
        ringerMode = audioManager.ringerMode,
        interruptionFilter = notificationManager?.currentInterruptionFilter,
        musicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC),
        isMusicMuted = audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
    )
    if (!allowed) return

    var toneGenerator: ToneGenerator? = null
    try {
        toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
        kotlinx.coroutines.delay(550)
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        try {
            toneGenerator?.release()
        } catch (_: Exception) {
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    com.semyqofu.aliasyou.ui.theme.AliasYouTheme {
        GameScreen(
            words = listOf("Космонавт", "Телескоп", "Галактика"),
            currentIndex = 0,
            settings = GameSettings(
                theme = "Космос",
                roundDuration = 60,
                teamCount = 2,
                pointsToWin = 40,
                skipPenalty = 1,
                difficulty = "Средняя"
            ),
            activeTeam = "Звездные волки",
            teamNames = listOf("Звездные волки", "Океан понимания"),
            onAddScore = { _, _ -> },
            onRoundFinished = {}
        )
    }
}

