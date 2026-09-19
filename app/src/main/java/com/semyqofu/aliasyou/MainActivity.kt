package com.semyqofu.aliasyou

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.semyqofu.aliasyou.data.GeminiService
import com.semyqofu.aliasyou.ui.screens.*
import com.semyqofu.aliasyou.ui.theme.AliasYouTheme
import com.semyqofu.aliasyou.viewmodel.GameSettings
import com.semyqofu.aliasyou.viewmodel.GameViewModel
import com.semyqofu.aliasyou.viewmodel.GameViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        setContent {
            AliasYouTheme {
                val context = androidx.compose.ui.platform.LocalContext.current
                val prefs = androidx.compose.runtime.remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
                val viewModel: GameViewModel = viewModel(factory = GameViewModelFactory(GeminiService(""), prefs))
                val navController = rememberNavController()
                val uiState by viewModel.uiState.collectAsState()
                val scores by viewModel.teamScores.collectAsState()

                NavHost(navController = navController, startDestination = "settings") {
                    composable("settings") {
                        SettingsScreen(onNavigateToGame = { apiKey, theme, duration, teams, points, penalty, diff ->
                            val geminiService = GeminiService(apiKey)
                            viewModel.setGeminiService(geminiService)
                            viewModel.startGame(GameSettings(theme, duration, teams, points, penalty, diff))
                            navController.navigate("start")
                        })
                    }
                    composable("start") {
                        when (val state = uiState) {
                            is GameViewModel.UiState.Success -> {
                                StartScreen(state.activeTeam, scores) {
                                    navController.navigate("game")
                                }
                            }
                            is GameViewModel.UiState.Loading -> LoadingScreen()
                            is GameViewModel.UiState.Error -> {
                                ErrorScreen(
                                    onBackToSettings = {
                                        navController.navigate("settings") {
                                            popUpTo("settings") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            else -> {
                                ErrorScreen(
                                    onBackToSettings = {
                                        navController.navigate("settings") {
                                            popUpTo("settings") { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    }
                    composable("game") {
                        when (val state = uiState) {
                            is GameViewModel.UiState.Success -> GameScreen(
                                words = state.words,
                                currentIndex = state.currentWordIndex,
                                settings = state.settings,
                                activeTeam = state.activeTeam,
                                teamNames = scores.keys.toList(),
                                onAddScore = { team, score -> viewModel.addScoreToTeam(team, score) },
                                onRoundFinished = { processedCount ->
                                    viewModel.finishRound(processedCount)

                                    // Проверяем состояние по правильному типу GameViewModel.UiState.GameOver
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
                                    onBackToSettings = {
                                        navController.navigate("settings") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }
                            else -> Text("Игра завершена")
                        }
                    }
                }
            }
        }
    }
}