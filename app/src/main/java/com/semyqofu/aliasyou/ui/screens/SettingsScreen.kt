package com.semyqofu.aliasyou.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToGame: (apiKey: String, theme: String, duration: Int, teams: Int, points: Int, penalty: Int, diff: String) -> Unit
) {
    var roundDuration by remember { mutableFloatStateOf(60f) }
    var teamCount by remember { mutableFloatStateOf(2f) }
    var pointsToWin by remember { mutableFloatStateOf(40f) }
    var skipPenalty by remember { mutableFloatStateOf(1f) }
    var difficulty by remember { mutableStateOf("Средняя") }
    var theme by remember { mutableStateOf("Общая") }

    var apiKey by remember { mutableStateOf("") }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var tempApiKey by remember { mutableStateOf("") }
    var showHistoryList by remember { mutableStateOf(false) }
    var showLastResponse by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
    var usedWordsList by remember(showApiKeyDialog) {
        val jsonString = prefs.getString("used_words_list_json", null)
        val list: List<String> = if (!jsonString.isNullOrEmpty()) {
            try {
                kotlinx.serialization.json.Json.decodeFromString<List<String>>(jsonString)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            prefs.getStringSet("used_words", emptySet<String>())?.toList() ?: emptyList()
        }
        mutableStateOf(list)
    }

    val lastResponse = remember(showApiKeyDialog) {
        prefs.getString("last_gemini_response", "Ответов от Gemini еще не было") ?: "Ответов от Gemini еще не было"
    }

    // Получаем Vibrator в обход Compose Haptic API
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
        apiKey = prefs.getString("api_key", "") ?: ""
    }

    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("Настройки Gemini API") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Введите API ключ Gemini:", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = tempApiKey,
                        onValueChange = { tempApiKey = it },
                        label = { Text("API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Сыгранные слова (${usedWordsList.size}):", style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = { showHistoryList = !showHistoryList }) {
                            Text(if (showHistoryList) "Скрыть" else "Показать")
                        }
                    }

                    if (showHistoryList) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            if (usedWordsList.isEmpty()) {
                                Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text("История пуста", style = MaterialTheme.typography.bodyMedium)
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    usedWordsList.forEachIndexed { index, word ->
                                        Text("${index + 1}. $word", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            prefs.edit()
                                .remove("used_words")
                                .remove("used_words_list_json")
                                .apply()
                            usedWordsList = emptyList()
                            android.widget.Toast.makeText(context, "История слов очищена", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        enabled = usedWordsList.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Сбросить историю слов")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Последний ответ Gemini:", style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = { showLastResponse = !showLastResponse }) {
                            Text(if (showLastResponse) "Скрыть" else "Показать")
                        }
                    }

                    if (showLastResponse) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = lastResponse,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        apiKey = tempApiKey
                        prefs.edit().putString("api_key", tempApiKey).apply()
                        showApiKeyDialog = false
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showApiKeyDialog = false }
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки игры") },
                actions = {
                    IconButton(
                        onClick = {
                            tempApiKey = apiKey
                            showApiKeyDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Настройки Gemini API Key"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = theme,
                onValueChange = { theme = it },
                label = { Text("Тематика слов") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Длительность раунда: ${roundDuration.toInt()} сек")
            Slider(
                value = roundDuration,
                onValueChange = { newValue ->
                    // Вибрируем только при изменении целой секунды
                    if (newValue.toInt() != roundDuration.toInt()) {
                        triggerVibration(8) // Сверхкороткий щелчок
                    }
                    roundDuration = newValue
                },
                valueRange = 30f..180f
            )

            Text("Количество команд: ${teamCount.toInt()}")
            Slider(
                value = teamCount,
                onValueChange = { newValue ->
                    // Вибрируем при переходе на следующую команду
                    if (newValue.toInt() != teamCount.toInt()) {
                        triggerVibration(10)
                    }
                    teamCount = newValue
                },
                valueRange = 1f..6f,
                steps = 4
            )

            Text("Очки для победы: ${pointsToWin.toInt()}")
            Slider(
                value = pointsToWin,
                onValueChange = { newValue ->
                    // Вибрируем при изменении целевых очков
                    if (newValue.toInt() != pointsToWin.toInt()) {
                        triggerVibration(8)
                    }
                    pointsToWin = newValue
                },
                valueRange = 20f..200f
            )

            Text("Штраф за пропуск: ${skipPenalty.toInt()}")
            Slider(
                value = skipPenalty,
                onValueChange = { newValue ->
                    // Вибрируем при изменении штрафа
                    if (newValue.toInt() != skipPenalty.toInt()) {
                        triggerVibration(10)
                    }
                    skipPenalty = newValue
                },
                valueRange = 0f..3f,
                steps = 2
            )

            Text("Сложность:")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Семья", "Легкая", "Средняя", "Сложная").forEach { level ->
                    FilterChip(
                        selected = difficulty == level,
                        onClick = {
                            triggerVibration(20) // Короткий клик при переключении
                            difficulty = level
                        },
                        label = { Text(level) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    triggerVibration(45) // Насыщенный клик при старте игры
                    prefs.edit().putString("api_key", apiKey).apply()
                    onNavigateToGame(apiKey, theme, roundDuration.toInt(), teamCount.toInt(), pointsToWin.toInt(), skipPenalty.toInt(), difficulty)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Начать игру")
            }
        }
    }
}