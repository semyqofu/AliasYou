// Измененный GeminiService.kt
package com.semyqofu.aliasyou.data

import com.semyqofu.aliasyou.data.GameData
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.serialization.json.Json

data class GameResult(
    val gameData: GameData,
    val rawJson: String
)

class GeminiService(apiKey: String) {
    private val model = GenerativeModel(
        modelName = "gemini-3.5-flash-lite",
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.95f
        }
    )

    private val jsonParser = Json { ignoreUnknownKeys = true }

    suspend fun generateGameData(
        theme: String,
        difficulty: String,
        wordCount: Int,
        teamCount: Int,
        excludedWords: List<String> = emptyList()
    ): GameResult {
        val randomSeed = (0..1000000).random()
        // Запрашиваем с запасом для компенсации случайных дубликатов
        val requestCount = wordCount + 10

        // Описываем правила для Gemini
        val difficultyInstructions = when(difficulty) {
            "Семья" -> "очень простые, понятные даже маленьким детям слова, исключи пошлые, грубые или узкоспециализированные термины."
            "Легкая" -> "простые общеизвестные существительные (окружающие предметы, базовые действия, животные)."
            "Сложная" -> "редкие термины, абстрактные понятия, научные концепции, идиомы или сложные словосочетания."
            else -> "слова средней сложности, абстрактные понятия средней тяжести и интересные словосочетания." // Для "Средняя"
        }

        val exclusionText = if (excludedWords.isNotEmpty()) {
            val recentExclusions = excludedWords.take(200).joinToString(", ")
            " ВАЖНО: Категорически ЗАПРЕЩЕНО использовать следующие слова, которые уже играли: [$recentExclusions]."
        } else ""

        val prompt = "Создай JSON (Seed: $randomSeed) со списком из $requestCount УНИКАЛЬНЫХ, совершенно новых, НЕ повторяющихся между собой слов для игры Alias. " +
                "Все слова в списке ДОЛЖНЫ БЫТЬ УНИКАЛЬНЫМИ. " +
                "КРАЙНЕ ВАЖНО: Избегай идущих подряд банальных пар, прямых антонимов и очевидных ассоциаций (например, НЕ ставь рядом 'кошка' и 'собака', 'солнце' и 'луна', 'снег' и 'дождь'). Слова должны быть максимально разнородными. " +
                "Тематика слов: '$theme'. " +
                "Уровень сложности: '$difficulty'. Слова должны быть: $difficultyInstructions$exclusionText " +
                "Также сгенерируй $teamCount креативных названий команд. " +
                "Формат JSON: {\"words\": [\"...\"], \"teams\": [\"...\"]}. ОТВЕТЬ ТОЛЬКО ЧИСТЫМ JSON БЕЗ MARKDOWN."

        val response = model.generateContent(prompt)
        val text = response.text ?: throw Exception("Пустой ответ от Gemini")
        val cleanedJson = text.replace("```json", "").replace("```", "").trim()
        val parsedData = jsonParser.decodeFromString<GameData>(cleanedJson)

        // Строгое удаление дубликатов внутри одной игры и от ранее сыгранных слов
        val excludedSet = excludedWords.map { it.trim().lowercase() }.toSet()
        val uniqueFilteredWords = parsedData.words
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() } // Дедупликация внутри генерации
            .filterNot { it.lowercase() in excludedSet } // Исключение сыгранных ранее

        val finalWords = if (uniqueFilteredWords.size >= wordCount) {
            uniqueFilteredWords.take(wordCount)
        } else {
            // Если после очистки слов чуть не хватает, берем дедуплицированный список от ответа
            parsedData.words.map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
        }

        // Перемешиваем список, чтобы разбить любые случайные смысловые цепочки
        val shuffledWords = finalWords.shuffled()

        return GameResult(
            gameData = parsedData.copy(words = shuffledWords),
            rawJson = cleanedJson
        )
    }
}