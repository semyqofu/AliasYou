package com.semyqofu.aliasyou.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semyqofu.aliasyou.data.AiPromptHelper
import com.semyqofu.aliasyou.ui.LocalAppStrings
import com.semyqofu.aliasyou.ui.components.Md3ExpressivePolygonShape
import com.semyqofu.aliasyou.ui.components.Md3ExpressiveShape
import com.semyqofu.aliasyou.ui.components.SkullIcon
import com.semyqofu.aliasyou.ui.components.bouncyClickable
import com.semyqofu.aliasyou.ui.theme.SuccessGreen

data class ModelOption(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val tint: androidx.compose.ui.graphics.Color
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    appLanguage: String = "ru",
    onLanguageChange: (String) -> Unit = {},
    onNavigateToGame: (
        provider: String,
        apiKey: String,
        model: String,
        theme: String,
        duration: Int,
        teams: Int,
        points: Int,
        penalty: Int,
        diff: String
    ) -> Unit,
    onPlayOffline: (
        duration: Int,
        teams: Int,
        points: Int,
        penalty: Int,
        difficulty: String
    ) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }

    // Game settings
    var roundDuration by remember { mutableFloatStateOf(60f) }
    var teamCount by remember { mutableFloatStateOf(2f) }
    var pointsToWin by remember { mutableFloatStateOf(40f) }
    var skipPenalty by remember { mutableFloatStateOf(1f) }
    var difficulty by remember { mutableStateOf(AiPromptHelper.DIFFICULTY_MEDIUM) }
    var theme by remember { mutableStateOf(strings.defaultTheme) }

    LaunchedEffect(appLanguage) {
        if (appLanguage == "en" && (theme == "Общая" || theme.isBlank())) {
            theme = "General"
        } else if (appLanguage == "ru" && (theme == "General" || theme.isBlank())) {
            theme = "Общая"
        }
    }

    // Offline mode toggle: true = use 5,000 preloaded words, false = use selected AI
    var isOfflineMode by remember {
        mutableStateOf(prefs.getBoolean("is_offline_mode", false))
    }

    // AI Provider selection: "gemini" or "gigachat"
    var selectedProvider by remember {
        mutableStateOf(prefs.getString("ai_provider", "gemini") ?: "gemini")
    }

    // Gemini settings
    var geminiApiKey by remember {
        mutableStateOf(prefs.getString("gemini_api_key", prefs.getString("api_key", "") ?: "") ?: "")
    }
    var geminiModel by remember {
        mutableStateOf(prefs.getString("gemini_model", "gemini-flash-latest") ?: "gemini-flash-latest")
    }

    // GigaChat settings
    var gigachatAuthKey by remember {
        mutableStateOf(prefs.getString("gigachat_auth_key", "") ?: "")
    }
    var gigachatModel by remember {
        mutableStateOf(prefs.getString("gigachat_model", "GigaChat") ?: "GigaChat")
    }
    var gigachatScope by remember {
        mutableStateOf(prefs.getString("gigachat_scope", "GIGACHAT_API_PERS") ?: "GIGACHAT_API_PERS")
    }

    // Dialog state
    var showApiDialog by remember { mutableStateOf(false) }
    var dialogTab by remember { mutableIntStateOf(0) } // 0: Gemini, 1: GigaChat, 2: History

    var tempGeminiKey by remember { mutableStateOf("") }
    var tempGigaChatKey by remember { mutableStateOf("") }
    var tempGigaChatScope by remember { mutableStateOf("GIGACHAT_API_PERS") }

    var showHistoryList by remember { mutableStateOf(false) }
    var showLastResponse by remember { mutableStateOf(false) }

    var usedWordsList by remember(showApiDialog) {
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

    val lastResponse = remember(showApiDialog, strings.languageCode) {
        prefs.getString("last_ai_response", prefs.getString("last_gemini_response", strings.noAiResponseYet) ?: "")
            ?: strings.noAiResponseYet
    }

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

    val presetThemes = strings.presetThemes

    val geminiModels = listOf(
        ModelOption("gemini-flash-latest", "Flash (latest)", Icons.Rounded.ElectricBolt, MaterialTheme.colorScheme.primary),
        ModelOption("gemini-flash-lite-latest", "Flash Lite (latest)", Icons.Rounded.RocketLaunch, MaterialTheme.colorScheme.tertiary),
        ModelOption("gemini-pro-latest", "Pro (latest)", Icons.Rounded.Psychology, MaterialTheme.colorScheme.secondary)
    )

    val gigachatModels = listOf(
        ModelOption("GigaChat", strings.gigachatBaseModelLabel, Icons.Rounded.SmartToy, SuccessGreen),
        ModelOption("GigaChat-Plus", "GigaChat-Plus", Icons.Rounded.Star, MaterialTheme.colorScheme.primary),
        ModelOption("GigaChat-Pro", "GigaChat-Pro", Icons.Rounded.Psychology, MaterialTheme.colorScheme.secondary),
        ModelOption("GigaChat-Max", "GigaChat-Max", Icons.Rounded.WorkspacePremium, MaterialTheme.colorScheme.tertiary)
    )

    // Current API Key status for the active provider
    val isCurrentProviderConfigured = if (selectedProvider == "gemini") {
        geminiApiKey.isNotBlank()
    } else {
        gigachatAuthKey.isNotBlank()
    }

    // Dialog for API Keys and Settings
    if (showApiDialog) {
        AlertDialog(
            onDismissRequest = { showApiDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    strings.apiDialogTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Quick Interface & Word Language Selector
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = strings.languageSettingTitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = appLanguage == "ru",
                                onClick = {
                                    triggerVibration(15)
                                    onLanguageChange("ru")
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text(strings.russianLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = appLanguage == "en",
                                onClick = {
                                    triggerVibration(15)
                                    onLanguageChange("en")
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text(strings.englishLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Dialog Tabs
                    PrimaryTabRow(
                        selectedTabIndex = dialogTab,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = dialogTab == 0,
                            onClick = { dialogTab = 0 },
                            text = { Text(strings.tabGemini, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = dialogTab == 1,
                            onClick = { dialogTab = 1 },
                            text = { Text(strings.tabGigaChat, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = dialogTab == 2,
                            onClick = { dialogTab = 2 },
                            text = { Text(strings.tabHistory, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    when (dialogTab) {
                        0 -> {
                            // Gemini tab
                            Text(
                                strings.geminiApiTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                strings.geminiApiDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = tempGeminiKey,
                                onValueChange = { tempGeminiKey = it },
                                label = { Text(strings.geminiKeyLabel) },
                                placeholder = { Text("AIzaSy...") },
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        1 -> {
                            // GigaChat tab
                            Text(
                                strings.gigachatApiTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                strings.gigachatApiDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = tempGigaChatKey,
                                onValueChange = { tempGigaChatKey = it },
                                label = { Text(strings.gigachatKeyLabel) },
                                placeholder = { Text("ODQ2...") },
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                strings.gigachatScopeTitle,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = tempGigaChatScope == "GIGACHAT_API_PERS",
                                    onClick = { tempGigaChatScope = "GIGACHAT_API_PERS" },
                                    label = { Text(strings.gigachatScopePers, fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = tempGigaChatScope == "GIGACHAT_API_CORP",
                                    onClick = { tempGigaChatScope = "GIGACHAT_API_CORP" },
                                    label = { Text(strings.gigachatScopeCorp, fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        2 -> {
                            // History & Logs tab
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    strings.playedWordsCount(usedWordsList.size),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                TextButton(onClick = { showHistoryList = !showHistoryList }) {
                                    Text(if (showHistoryList) strings.hide else strings.show)
                                }
                            }

                            if (showHistoryList) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 140.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    if (usedWordsList.isEmpty()) {
                                        Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                            Text(strings.historyEmpty, style = MaterialTheme.typography.bodyMedium)
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
                                    Toast.makeText(context, strings.historyClearedToast, Toast.LENGTH_SHORT).show()
                                },
                                enabled = usedWordsList.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.clearHistory)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings.lastAiResponseTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                TextButton(onClick = { showLastResponse = !showLastResponse }) {
                                    Text(if (showLastResponse) strings.hide else strings.show)
                                }
                            }

                            if (showLastResponse) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 140.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        geminiApiKey = tempGeminiKey
                        gigachatAuthKey = tempGigaChatKey
                        gigachatScope = tempGigaChatScope

                        prefs.edit()
                            .putString("api_key", tempGeminiKey)
                            .putString("gemini_api_key", tempGeminiKey)
                            .putString("gigachat_auth_key", tempGigaChatKey)
                            .putString("gigachat_scope", tempGigaChatScope)
                            .apply()

                        showApiDialog = false
                        Toast.makeText(context, strings.settingsSavedToast, Toast.LENGTH_SHORT).show()
                    },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiDialog = false }) {
                    Text(strings.cancel)
                }
            },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    }

    Scaffold(
        topBar = {
            val diceTransition = rememberInfiniteTransition(label = "diceCookieSpin")
            val diceCookieRotation by diceTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(16000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "diceCookieRotation"
            )

            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Alias You",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = Md3ExpressivePolygonShape(Md3ExpressiveShape.COOKIE_7),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { rotationZ = diceCookieRotation }
                            ) {}
                            Icon(
                                imageVector = Icons.Rounded.Casino,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            triggerVibration(15)
                            tempGeminiKey = geminiApiKey
                            tempGigaChatKey = gigachatAuthKey
                            tempGigaChatScope = gigachatScope
                            dialogTab = if (selectedProvider == "gemini") 0 else 1
                            showApiDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = strings.apiSettingsCd
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            val handleStartGame = {
                triggerVibration(45)

                if (isOfflineMode) {
                    prefs.edit().putBoolean("is_offline_mode", true).apply()
                    onPlayOffline(
                        roundDuration.toInt(),
                        teamCount.toInt(),
                        pointsToWin.toInt(),
                        skipPenalty.toInt(),
                        difficulty
                    )
                } else {
                    val currentKey = if (selectedProvider == "gemini") geminiApiKey else gigachatAuthKey
                    val currentModel = if (selectedProvider == "gemini") geminiModel else gigachatModel

                    if (currentKey.isBlank()) {
                        tempGeminiKey = geminiApiKey
                        tempGigaChatKey = gigachatAuthKey
                        tempGigaChatScope = gigachatScope
                        dialogTab = if (selectedProvider == "gemini") 0 else 1
                        showApiDialog = true
                        Toast.makeText(
                            context,
                            strings.missingKeyToast(if (selectedProvider == "gemini") "Gemini" else "GigaChat"),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        prefs.edit()
                            .putBoolean("is_offline_mode", false)
                            .putString("ai_provider", selectedProvider)
                            .putString("gemini_api_key", geminiApiKey)
                            .putString("gemini_model", geminiModel)
                            .putString("gigachat_auth_key", gigachatAuthKey)
                            .putString("gigachat_model", gigachatModel)
                            .putString("gigachat_scope", gigachatScope)
                            .apply()

                        onNavigateToGame(
                            selectedProvider,
                            currentKey,
                            currentModel,
                            theme,
                            roundDuration.toInt(),
                            teamCount.toInt(),
                            pointsToWin.toInt(),
                            skipPenalty.toInt(),
                            difficulty
                        )
                    }
                }
            }

            ExtendedFloatingActionButton(
                onClick = handleStartGame,
                icon = {
                    Icon(
                        imageVector = if (isOfflineMode) Icons.Rounded.PlayArrow else Icons.Rounded.RocketLaunch,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                },
                text = {
                    Text(
                        strings.startGameButton,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(56.dp)
                    .bouncyClickable {
                        handleStartGame()
                    },
                shape = MaterialTheme.shapes.extraLarge,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 2.dp
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card 1: AI Provider & Model Selection
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Psychology,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.aiCardTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Status pill (always crisp single line)
                        Surface(
                            shape = CircleShape,
                            color = if (isOfflineMode) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else if (isCurrentProviderConfigured) {
                                SuccessGreen.copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            },
                            modifier = Modifier.clickable {
                                if (!isOfflineMode) {
                                    tempGeminiKey = geminiApiKey
                                    tempGigaChatKey = gigachatAuthKey
                                    tempGigaChatScope = gigachatScope
                                    dialogTab = if (selectedProvider == "gemini") 0 else 1
                                    showApiDialog = true
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isOfflineMode) MaterialTheme.colorScheme.secondary
                                            else if (isCurrentProviderConfigured) SuccessGreen
                                            else MaterialTheme.colorScheme.error
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isOfflineMode) strings.statusOffline else if (isCurrentProviderConfigured) strings.statusReady else strings.statusNoKey,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    color = if (isOfflineMode) MaterialTheme.colorScheme.onSecondaryContainer
                                    else if (isCurrentProviderConfigured) SuccessGreen
                                    else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // Provider Segmented Choice
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (isOfflineMode) 0.55f else 1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Gemini Button
                        val isGemini = selectedProvider == "gemini"
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable {
                                    triggerVibration(15)
                                    selectedProvider = "gemini"
                                },
                            shape = MaterialTheme.shapes.medium,
                            color = if (isGemini) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = if (isGemini) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (isGemini) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Gemini",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (isGemini) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isGemini) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // GigaChat Button
                        val isGiga = selectedProvider == "gigachat"
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable {
                                    triggerVibration(15)
                                    selectedProvider = "gigachat"
                                },
                            shape = MaterialTheme.shapes.medium,
                            color = if (isGiga) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = if (isGiga) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.secondary) else null
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SmartToy,
                                    contentDescription = null,
                                    tint = if (isGiga) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "GigaChat",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (isGiga) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isGiga) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Model Selection for the active provider
                    Column(
                        modifier = Modifier.alpha(if (isOfflineMode) 0.55f else 1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (selectedProvider == "gemini") strings.geminiModelLabel else strings.gigachatModelLabel,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (selectedProvider == "gemini") {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(geminiModels) { model ->
                                    val isSelected = geminiModel == model.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            triggerVibration(15)
                                            geminiModel = model.id
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = model.icon,
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else model.tint,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = model.label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        shape = MaterialTheme.shapes.small
                                    )
                                }
                            }
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(gigachatModels) { model ->
                                    val isSelected = gigachatModel == model.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            triggerVibration(15)
                                            gigachatModel = model.id
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = model.icon,
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.secondary else model.tint,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = model.label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        shape = MaterialTheme.shapes.small
                                    )
                                }
                            }
                        }
                    }

                    // Offline Mode Switch inside AI card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable {
                                triggerVibration(15)
                                isOfflineMode = !isOfflineMode
                                prefs.edit().putBoolean("is_offline_mode", isOfflineMode).apply()
                            },
                        shape = MaterialTheme.shapes.medium,
                        color = if (isOfflineMode) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WifiOff,
                                    contentDescription = null,
                                    tint = if (isOfflineMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = strings.playOfflineTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOfflineMode) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isOfflineMode) strings.offlineSubtitleActive else strings.offlineSubtitleInactive,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isOfflineMode) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = isOfflineMode,
                                onCheckedChange = { checked ->
                                    triggerVibration(15)
                                    isOfflineMode = checked
                                    prefs.edit().putBoolean("is_offline_mode", checked).apply()
                                },
                                thumbContent = if (isOfflineMode) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
            }

            // Card 2: Theme & Difficulty
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isOfflineMode) Icons.Rounded.Speed else Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isOfflineMode) strings.difficultyCardTitleOffline else strings.difficultyCardTitleAi,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Only show theme input and presets when offline mode is false
                    AnimatedVisibility(
                        visible = !isOfflineMode,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = theme,
                                onValueChange = { theme = it },
                                label = { Text(strings.themeFieldLabel) },
                                placeholder = { Text(strings.themePlaceholder) },
                                singleLine = true,
                                trailingIcon = {
                                    if (theme.isNotEmpty()) {
                                        IconButton(onClick = { theme = "" }) {
                                            Icon(Icons.Rounded.Close, contentDescription = strings.clearCd, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                },
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Preset Chips
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(presetThemes) { item ->
                                    val isSelected = theme == item
                                    SuggestionChip(
                                        onClick = {
                                            triggerVibration(15)
                                            theme = item
                                        },
                                        label = { Text(item) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                                        ),
                                        shape = MaterialTheme.shapes.small
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = strings.difficultyLevelLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val difficultyItems = listOf(
                        Triple(AiPromptHelper.DIFFICULTY_FAMILY, Icons.Rounded.FamilyRestroom, MaterialTheme.colorScheme.primary),
                        Triple(AiPromptHelper.DIFFICULTY_EASY, Icons.Rounded.SentimentSatisfied, SuccessGreen),
                        Triple(AiPromptHelper.DIFFICULTY_MEDIUM, Icons.Rounded.Speed, MaterialTheme.colorScheme.tertiary),
                        Triple(AiPromptHelper.DIFFICULTY_HARD, Icons.Rounded.LocalFireDepartment, MaterialTheme.colorScheme.error),
                        Triple(AiPromptHelper.DIFFICULTY_PHRASES, SkullIcon, MaterialTheme.colorScheme.error)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        difficultyItems.forEach { (level, icon, tintColor) ->
                            val isSelected = difficulty == level
                            val isSkullMode = level == AiPromptHelper.DIFFICULTY_PHRASES
                            val displayLabel = strings.difficultyLabels[level] ?: level
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    triggerVibration(20)
                                    difficulty = level
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = when {
                                            isSkullMode -> MaterialTheme.colorScheme.error
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            else -> tintColor
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = displayLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = if (isSkullMode) {
                                    FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                                        selectedLeadingIconColor = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    FilterChipDefaults.filterChipColors()
                                },
                                shape = MaterialTheme.shapes.small
                            )
                        }
                    }

                    val difficultyHint = strings.difficultyHints[difficulty].orEmpty()
                    if (difficultyHint.isNotEmpty()) {
                        Text(
                            text = difficultyHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (difficulty == AiPromptHelper.DIFFICULTY_PHRASES) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            // Card 3: Game Settings (Sliders)
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.roundSettingsTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Duration Slider
                    SettingSliderItem(
                        icon = Icons.Rounded.Timer,
                        title = strings.roundDurationTitle,
                        valueText = strings.roundDurationValue(roundDuration.toInt()),
                        value = roundDuration,
                        onValueChange = { newValue ->
                            if (newValue.toInt() != roundDuration.toInt()) {
                                triggerVibration(8)
                            }
                            roundDuration = newValue
                        },
                        valueRange = 30f..180f
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Team Count Slider
                    SettingSliderItem(
                        icon = Icons.Rounded.Groups,
                        title = strings.teamCountTitle,
                        valueText = "${teamCount.toInt()}",
                        value = teamCount,
                        onValueChange = { newValue ->
                            if (newValue.toInt() != teamCount.toInt()) {
                                triggerVibration(10)
                            }
                            teamCount = newValue
                        },
                        valueRange = 1f..6f,
                        steps = 4
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Points to Win Slider
                    SettingSliderItem(
                        icon = Icons.Rounded.EmojiEvents,
                        title = strings.pointsToWinTitle,
                        valueText = "${pointsToWin.toInt()}",
                        value = pointsToWin,
                        onValueChange = { newValue ->
                            if (newValue.toInt() != pointsToWin.toInt()) {
                                triggerVibration(8)
                            }
                            pointsToWin = newValue
                        },
                        valueRange = 20f..200f
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Skip Penalty Slider
                    SettingSliderItem(
                        icon = Icons.Rounded.RemoveCircleOutline,
                        title = strings.skipPenaltyTitle,
                        valueText = "${skipPenalty.toInt()}",
                        value = skipPenalty,
                        onValueChange = { newValue ->
                            if (newValue.toInt() != skipPenalty.toInt()) {
                                triggerVibration(10)
                            }
                            skipPenalty = newValue
                        },
                        valueRange = 0f..3f,
                        steps = 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}

@Composable
fun SettingSliderItem(
    icon: ImageVector,
    title: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

