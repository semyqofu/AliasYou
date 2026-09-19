package com.semyqofu.aliasyou.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.semyqofu.aliasyou.viewmodel.GameSettings
import kotlinx.coroutines.launch

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
    var roundScore by remember { mutableIntStateOf(0) }
    var wordsProcessed by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(settings.roundDuration) }
    var isRoundActive by remember { mutableStateOf(true) }
    var showTeamDialog by remember { mutableStateOf(false) }
    var pendingScore by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()
    val offsetY = remember { androidx.compose.animation.core.Animatable(0f) }

    // Получаем контекст для доступа к системным службам
    val context = LocalContext.current

    // Безопасное получение сервиса вибрации для любых версий Android
    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // Функция-помощник для запуска вибрации заданной длительности (в миллисекундах)
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
        android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
            .startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
    }

    if (showTeamDialog) {
        AlertDialog(
            onDismissRequest = { showTeamDialog = false },
            title = { Text("Выберите команду") },
            text = {
                Column {
                    teamNames.forEach { team ->
                        TextButton(onClick = {
                            // Ощутимая вибрация при тапе по кнопке (45 миллисекунд)
                            triggerVibration(45)
                            onAddScore(team, pendingScore)
                            showTeamDialog = false
                            onRoundFinished(wordsProcessed + 1)
                        }) { Text(team, style = MaterialTheme.typography.titleMedium) }
                    }
                }
            },
            confirmButton = {}
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header (Шапка игры)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = MaterialTheme.shapes.large
            ) {
                Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ход команды", style = MaterialTheme.typography.labelLarge)
                        Text(activeTeam, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2)
                    }
                    Text("$timeLeft", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Light)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Hints (Подсказка "Дальше" сверху)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.width(160.dp)) {
                    Text("ДАЛЬШЕ", modifier = Modifier.padding(vertical = 12.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            // ПЕРВЫЙ резиновый интервал — толкает карточку вниз к центру
            Spacer(modifier = Modifier.weight(1f))

            // Word Card (Интерактивная карточка со словом)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .zIndex(1f)
                    .graphicsLayer { translationY = offsetY.value }
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            scope.launch { offsetY.snapTo(offsetY.value + delta) }
                        },
                        onDragStopped = {
                            scope.launch {
                                val finalY = offsetY.value
                                if (finalY < -100f) { // Свайп Вверх (Слово отгадано)
                                    // Быстрый вибро-щелчок при успешном свайпе (25 миллисекунд)
                                    triggerVibration(25)

                                    if (!isRoundActive) { // Логика последнего слова по истечении таймера
                                        pendingScore = 1
                                        showTeamDialog = true
                                    } else {
                                        onAddScore(activeTeam, 1)
                                        wordsProcessed++
                                    }
                                } else if (finalY > 100f) { // Свайп Вниз (Пропуск слова)
                                    // Такой же щелчок при пропуске (25 миллисекунд)
                                    triggerVibration(25)

                                    onAddScore(activeTeam, -settings.skipPenalty)
                                    wordsProcessed++

                                    // Если таймер уже вышел — сразу завершаем ход
                                    if (!isRoundActive) {
                                        onRoundFinished(wordsProcessed)
                                    }
                                }
                                offsetY.animateTo(0f)
                            }
                        }
                    ),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(32.dp)) {
                    Text(
                        text = words.getOrNull(currentIndex + wordsProcessed)?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Слова закончились",
                        style = MaterialTheme.typography.displaySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // ВТОРОЙ резиновый интервал — толкает карточку вверх к центру.
            Spacer(modifier = Modifier.weight(1f))

            // Skip button (Кнопка-подсказка "Пропустить")
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.width(160.dp).padding(bottom = 32.dp)
            ) {
                Text(
                    text = "ПРОПУСТИТЬ",
                    modifier = Modifier.padding(vertical = 12.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}