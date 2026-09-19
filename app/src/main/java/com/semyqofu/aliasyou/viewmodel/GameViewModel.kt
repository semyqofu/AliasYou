package com.semyqofu.aliasyou.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.semyqofu.aliasyou.data.GameData
import com.semyqofu.aliasyou.data.GeminiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class GameSettings(
    val theme: String,
    val roundDuration: Int,
    val teamCount: Int,
    val pointsToWin: Int,
    val skipPenalty: Int,
    val difficulty: String
)

class GameViewModel(
    private var geminiService: GeminiService,
    private val prefs: SharedPreferences? = null
) : ViewModel() {

    fun setGeminiService(service: GeminiService) {
        this.geminiService = service
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    private val _teamScores = MutableStateFlow<Map<String, Int>>(emptyMap())
    val teamScores: StateFlow<Map<String, Int>> = _teamScores

    private var _gameSettings: GameSettings? = null
    private var _activeTeamIndex = 0
    private var _currentWordIndex = 0
    private var _allWords = listOf<String>()

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val words: List<String>, val currentWordIndex: Int, val settings: GameSettings, val activeTeam: String) : UiState()
        data class Error(val message: String) : UiState()
        data class GameOver(val winner: String, val finalScores: Map<String, Int>) : UiState()
    }

    fun startGame(settings: GameSettings) {
        _gameSettings = settings
        _activeTeamIndex = 0
        _currentWordIndex = 0
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // Вычисляем необходимое количество слов (команды * очки * 2)
                val wordCount = settings.teamCount * settings.pointsToWin * 2

                // Читаем упорядоченный список ранее сыгранных слов
                val jsonString = prefs?.getString("used_words_list_json", null)
                val usedWordsList: List<String> = if (!jsonString.isNullOrEmpty()) {
                    try {
                        Json.decodeFromString<List<String>>(jsonString)
                    } catch (e: Exception) {
                        emptyList()
                    }
                } else {
                    prefs?.getStringSet("used_words", emptySet())?.toList() ?: emptyList()
                }

                // Передаем параметры в GeminiService
                val result = geminiService.generateGameData(
                    theme = settings.theme,
                    difficulty = settings.difficulty,
                    wordCount = wordCount,
                    teamCount = settings.teamCount,
                    excludedWords = usedWordsList
                )

                // Сохраняем сырой ответ Gemini для просмотра в настройках
                prefs?.edit()?.putString("last_gemini_response", result.rawJson)?.apply()

                _allWords = result.gameData.words
                _teamScores.value = result.gameData.teams.associateWith { 0 }
                _uiState.value = UiState.Success(_allWords, _currentWordIndex, settings, result.gameData.teams.first())
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Ошибка парсинга или сети")
            }
        }
    }

    private fun savePlayedWordsToHistory(playedWords: List<String>) {
        if (prefs == null || playedWords.isEmpty()) return
        val jsonString = prefs.getString("used_words_list_json", null)
        val existingList: List<String> = if (!jsonString.isNullOrEmpty()) {
            try {
                Json.decodeFromString<List<String>>(jsonString)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            prefs.getStringSet("used_words", emptySet())?.toList() ?: emptyList()
        }

        val newPlayed = playedWords.map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
        val updatedList = (newPlayed + existingList.filterNot { old -> newPlayed.any { n -> n.equals(old, ignoreCase = true) } }).take(500)
        val newJson = Json.encodeToString(updatedList)

        prefs.edit()
            .putString("used_words_list_json", newJson)
            .putStringSet("used_words", updatedList.toSet())
            .apply()
    }

    fun addScoreToTeam(teamName: String, score: Int) {
        val currentScores = _teamScores.value.toMutableMap()
        currentScores[teamName] = (currentScores[teamName] ?: 0) + score
        _teamScores.value = currentScores
    }

    // Теперь метод принимает количество слов, обработанных за раунд
    fun finishRound(wordsCount: Int) {
        if (wordsCount > 0 && _allWords.isNotEmpty()) {
            val startIndex = _currentWordIndex
            val endIndex = minOf(startIndex + wordsCount, _allWords.size)
            if (startIndex < endIndex) {
                val playedInRound = _allWords.subList(startIndex, endIndex)
                savePlayedWordsToHistory(playedInRound)
            }
        }

        // Прибавляем отыгранные слова к общему индексу
        _currentWordIndex += wordsCount

        _activeTeamIndex++
        if (_activeTeamIndex >= (_gameSettings?.teamCount ?: 1)) {
            _activeTeamIndex = 0
        }

        val nextTeam = _teamScores.value.keys.toList()[_activeTeamIndex]

        val winner = _teamScores.value.entries
            .filter { it.value >= (_gameSettings?.pointsToWin ?: Int.MAX_VALUE) }
            .maxByOrNull { it.value }

        if (winner != null && _activeTeamIndex == 0) {
            _uiState.value = UiState.GameOver(winner.key, _teamScores.value)
        } else {
            val currentState = _uiState.value
            // Обновляем UiState, передавая измененный activeTeam и новый _currentWordIndex
            if (currentState is UiState.Success) {
                _uiState.value = currentState.copy(
                    activeTeam = nextTeam,
                    currentWordIndex = _currentWordIndex
                )
            }
        }
    }
}

class GameViewModelFactory(
    private val geminiService: GeminiService,
    private val prefs: SharedPreferences? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(geminiService, prefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

