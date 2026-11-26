package com.semyqofu.aliasyou.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.semyqofu.aliasyou.data.AiService
import com.semyqofu.aliasyou.data.GameData
import com.semyqofu.aliasyou.data.GeminiService
import com.semyqofu.aliasyou.data.OfflineWordsRepository
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
    val difficulty: String,
    val language: String = "ru"
)

class GameViewModel(
    private var aiService: AiService,
    private val prefs: SharedPreferences? = null,
    private val offlineWordsRepository: OfflineWordsRepository? = null
) : ViewModel() {

    fun setAiService(service: AiService) {
        this.aiService = service
    }

    fun setGeminiService(service: GeminiService) {
        this.aiService = service
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

    val currentGameSettings: GameSettings?
        get() = _gameSettings

    private val fallbackWords = listOf(
        "Синхрофазотрон", "Космический корабль", "Искусственный интеллект", "Печенье",
        "Электросамокат", "Капучино", "Нейросеть", "Парашют", "Телепортация",
        "Голограмма", "Марсоход", "Квантовый компьютер", "Астронавт", "Лабиринт",
        "Супергерой", "Дирижабль", "Калейдоскоп", "Вулкан", "Северное сияние",
        "Фейерверк", "Матрица", "Киберпанк", "Альпака", "Хамелеон", "Батискаф",
        "Метеорит", "Айсберг", "Цунами", "Тайфун", "Микроскоп", "Бинокль",
        "Стратосфера", "Антигравитация", "Гравитация", "Телескоп", "Экспедиция",
        "Фламинго", "Эверест", "Скейтборд", "Парусник", "Атлантида", "Пирамида",
        "Пингвин", "Детектив", "Компас", "Навигатор", "Робототехника", "Лазер"
    )

    private val fallbackTeams = listOf("Квантовые Лисы", "Космические Еноты", "Нейро-Пираты", "Астральные Волки")

    private val fallbackWordsEn = listOf(
        "Spaceship", "Algorithm", "Cookie", "Scooter", "Cappuccino", "Parachute",
        "Hologram", "Astronaut", "Labyrinth", "Superhero", "Blimp", "Kaleidoscope",
        "Volcano", "Fireworks", "Matrix", "Cyberpunk", "Alpaca", "Chameleon",
        "Submarine", "Meteorite", "Iceberg", "Tsunami", "Typhoon", "Microscope",
        "Binoculars", "Stratosphere", "Gravity", "Telescope", "Expedition", "Flamingo",
        "Everest", "Skateboard", "Sailboat", "Atlantis", "Pyramid", "Penguin",
        "Detective", "Compass", "Navigator", "Robotics", "Laser", "Aurora"
    )

    private val fallbackTeamsEn = listOf("Quantum Foxes", "Cosmic Raccoons", "Neuro Pirates", "Astral Wolves")

    fun playOffline(settings: GameSettings? = null) {
        val currentLang = settings?.language
            ?: _gameSettings?.language
            ?: prefs?.getString("app_language", "ru")
            ?: "ru"
        val s = settings ?: _gameSettings ?: GameSettings(
            theme = if (currentLang == "en") "Offline" else "Офлайн",
            roundDuration = 60,
            teamCount = 2,
            pointsToWin = 30,
            skipPenalty = 1,
            difficulty = "Средняя",
            language = currentLang
        )
        _gameSettings = s
        _activeTeamIndex = 0
        _currentWordIndex = 0
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            kotlinx.coroutines.delay(1200) // Показывает морф-анимацию MD3

            // Читаем исключенные слова из истории
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

            val requiredWordCount = (s.teamCount * s.pointsToWin * 2).coerceAtLeast(80)

            val defaultFallbackWords = if (s.language == "en") fallbackWordsEn else fallbackWords
            val defaultFallbackTeams = if (s.language == "en") fallbackTeamsEn else fallbackTeams

            val words = offlineWordsRepository?.getWords(
                difficulty = s.difficulty,
                count = requiredWordCount,
                excludedWords = usedWordsList,
                language = s.language
            ) ?: defaultFallbackWords.shuffled()

            val teams = offlineWordsRepository?.getCreativeTeams(s.teamCount.coerceAtLeast(2), language = s.language)
                ?: defaultFallbackTeams.take(s.teamCount.coerceAtLeast(2))

            _allWords = words
            _teamScores.value = teams.associateWith { 0 }
            _uiState.value = UiState.Success(_allWords, _currentWordIndex, s, teams.first())
        }
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

                // Передаем параметры в AiService (Gemini или GigaChat)
                val result = aiService.generateGameData(
                    theme = settings.theme,
                    difficulty = settings.difficulty,
                    wordCount = wordCount,
                    teamCount = settings.teamCount,
                    excludedWords = usedWordsList,
                    language = settings.language
                )

                // Сохраняем сырой ответ AI для просмотра в настройках
                prefs?.edit()
                    ?.putString("last_gemini_response", result.rawJson)
                    ?.putString("last_ai_response", result.rawJson)
                    ?.apply()

                _allWords = result.gameData.words
                _teamScores.value = result.gameData.teams.associateWith { 0 }
                _uiState.value = UiState.Success(_allWords, _currentWordIndex, settings, result.gameData.teams.first())
            } catch (e: Exception) {
                val defaultErr = if (settings.language == "en") "Parsing or network error" else "Ошибка парсинга или сети"
                _uiState.value = UiState.Error(e.message ?: defaultErr)
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
    private val aiService: AiService,
    private val prefs: SharedPreferences? = null,
    private val offlineWordsRepository: OfflineWordsRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(aiService, prefs, offlineWordsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

