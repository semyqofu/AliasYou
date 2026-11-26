package com.semyqofu.aliasyou

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.semyqofu.aliasyou.data.GeminiService
import com.semyqofu.aliasyou.data.GigaChatService
import com.semyqofu.aliasyou.data.OfflineWordsRepository
import com.semyqofu.aliasyou.ui.AppStrings
import com.semyqofu.aliasyou.ui.LocalAppStrings
import com.semyqofu.aliasyou.ui.screens.*
import com.semyqofu.aliasyou.ui.theme.AliasYouTheme
import com.semyqofu.aliasyou.viewmodel.GameSettings
import com.semyqofu.aliasyou.viewmodel.GameViewModel
import com.semyqofu.aliasyou.viewmodel.GameViewModelFactory
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        setContent {
            AliasYouTheme {
                val context = androidx.compose.ui.platform.LocalContext.current
                val prefs = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
                val offlineRepository = remember { OfflineWordsRepository(context.applicationContext) }
                val viewModel: GameViewModel = viewModel(factory = GameViewModelFactory(GeminiService(""), prefs, offlineRepository))
                val navController = rememberNavController()
                val uiState by viewModel.uiState.collectAsState()
                val scores by viewModel.teamScores.collectAsState()

                var appLanguage by remember {
                    mutableStateOf(
                        prefs.getString("app_language", null)
                            ?: if (Locale.getDefault().language == "en") "en" else "ru"
                    )
                }
                val hasCompletedOnboarding = remember {
                    prefs.getBoolean("onboarding_completed", false)
                }

                val appStrings = remember(appLanguage) { AppStrings.forLanguage(appLanguage) }

                CompositionLocalProvider(LocalAppStrings provides appStrings) {
                    val startDestination = when {
                        intent.getStringExtra("screen") == "game_over" -> "game_over_preview"
                        !hasCompletedOnboarding -> "onboarding"
                        else -> "settings"
                    }

                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) + fadeIn(animationSpec = tween(300))
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -fullWidth / 3 },
                                animationSpec = tween(300)
                            ) + fadeOut(animationSpec = tween(200))
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> -fullWidth / 3 },
                                animationSpec = tween(300)
                            ) + fadeIn(animationSpec = tween(300))
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) + fadeOut(animationSpec = tween(200))
                        }
                    ) {
                        composable("onboarding") {
                            LanguageOnboardingScreen(
                                initialLanguage = appLanguage,
                                onLanguageSelected = { lang ->
                                    appLanguage = lang
                                },
                                onContinue = { lang ->
                                    appLanguage = lang
                                    prefs.edit()
                                        .putString("app_language", lang)
                                        .putBoolean("onboarding_completed", true)
                                        .apply()
                                    navController.navigate("settings") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                appLanguage = appLanguage,
                                onLanguageChange = { newLang ->
                                    appLanguage = newLang
                                    prefs.edit().putString("app_language", newLang).apply()
                                },
                                onNavigateToGame = { provider, apiKey, model, theme, duration, teams, points, penalty, diff ->
                                    val aiService = if (provider.equals("gigachat", ignoreCase = true)) {
                                        val scope = prefs.getString("gigachat_scope", "GIGACHAT_API_PERS") ?: "GIGACHAT_API_PERS"
                                        GigaChatService(authKey = apiKey, modelName = model, scope = scope)
                                    } else {
                                        GeminiService(apiKey = apiKey, modelName = model)
                                    }
                                    viewModel.setAiService(aiService)
                                    viewModel.startGame(
                                        GameSettings(
                                            theme = theme,
                                            roundDuration = duration,
                                            teamCount = teams,
                                            pointsToWin = points,
                                            skipPenalty = penalty,
                                            difficulty = diff,
                                            language = appLanguage
                                        )
                                    )
                                    navController.navigate("start")
                                },
                                onPlayOffline = { duration, teams, points, penalty, diff ->
                                    viewModel.playOffline(
                                        GameSettings(
                                            theme = if (appLanguage == "en") "Offline" else "Офлайн",
                                            roundDuration = duration,
                                            teamCount = teams,
                                            pointsToWin = points,
                                            skipPenalty = penalty,
                                            difficulty = diff,
                                            language = appLanguage
                                        )
                                    )
                                    navController.navigate("start")
                                }
                            )
                        }
                        composable("start") {
                            AnimatedContent(
                                targetState = uiState,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(350)) + scaleIn(initialScale = 0.94f))
                                        .togetherWith(fadeOut(animationSpec = tween(250)) + scaleOut(targetScale = 1.04f))
                                },
                                label = "StartContentTransition"
                            ) { state ->
                                when (state) {
                                    is GameViewModel.UiState.Success -> {
                                        StartScreen(state.activeTeam, scores) {
                                            navController.navigate("game")
                                        }
                                    }
                                    is GameViewModel.UiState.Loading -> LoadingScreen()
                                    is GameViewModel.UiState.Error -> {
                                        ErrorScreen(
                                            message = state.message,
                                            onPlayOffline = { viewModel.playOffline() },
                                            onBackToSettings = {
                                                navController.navigate("settings") {
                                                    popUpTo("settings") { inclusive = true }
                                                }
                                            }
                                        )
                                    }
                                    else -> {
                                        ErrorScreen(
                                            onPlayOffline = { viewModel.playOffline() },
                                            onBackToSettings = {
                                                navController.navigate("settings") {
                                                    popUpTo("settings") { inclusive = true }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        composable("game") {
                            AnimatedContent(
                                targetState = uiState,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(350)) + scaleIn(initialScale = 0.94f))
                                        .togetherWith(fadeOut(animationSpec = tween(250)) + scaleOut(targetScale = 1.04f))
                                },
                                label = "GameContentTransition"
                            ) { state ->
                                when (state) {
                                    is GameViewModel.UiState.Success -> GameScreen(
                                        words = state.words,
                                        currentIndex = state.currentWordIndex,
                                        settings = state.settings,
                                        activeTeam = state.activeTeam,
                                        teamNames = scores.keys.toList(),
                                        onAddScore = { team, score -> viewModel.addScoreToTeam(team, score) },
                                        onRoundFinished = { processedCount ->
                                            viewModel.finishRound(processedCount)

                                            if (viewModel.uiState.value !is GameViewModel.UiState.GameOver) {
                                                navController.navigate("start") {
                                                    popUpTo("start") { inclusive = true }
                                                }
                                            }
                                        }
                                    )
                                    is GameViewModel.UiState.Loading -> LoadingScreen()
                                    is GameViewModel.UiState.GameOver -> {
                                        GameOverScreen(state.winner, state.finalScores) {
                                            navController.navigate("settings") { popUpTo(0) { inclusive = true } }
                                        }
                                    }
                                    is GameViewModel.UiState.Error -> {
                                        ErrorScreen(
                                            message = state.message,
                                            onPlayOffline = { viewModel.playOffline() },
                                            onBackToSettings = {
                                                navController.navigate("settings") {
                                                    popUpTo(0) { inclusive = true }
                                                }
                                            }
                                        )
                                    }
                                    else -> Text(appStrings.gameEndedFallback)
                                }
                            }
                        }
                        composable("game_over_preview") {
                            GameOverScreen(
                                winner = if (appLanguage == "en") "Quantum Foxes" else "Секреты реальности",
                                finalScores = if (appLanguage == "en") {
                                    mapOf("Quantum Foxes" to 42, "Astral Wolves" to 28)
                                } else {
                                    mapOf("Секреты реальности" to 42, "Зоны возможного" to 28)
                                },
                                onRestart = {
                                    navController.navigate("settings") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
