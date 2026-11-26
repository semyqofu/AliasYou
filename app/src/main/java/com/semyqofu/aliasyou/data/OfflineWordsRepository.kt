package com.semyqofu.aliasyou.data

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

class OfflineWordsRepository(private val context: Context) {

    private val easyWordsRu: List<String> by lazy { loadAssetLines("words_easy.txt") }
    private val mediumWordsRu: List<String> by lazy { loadAssetLines("words_medium.txt") }
    private val hardWordsRu: List<String> by lazy { loadAssetLines("words_hard.txt") }
    private val phraseWordsRu: List<String> by lazy { loadAssetLines("words_phrases.txt") }

    private val easyWordsEn: List<String> by lazy { loadAssetLines("words_en_easy.txt") }
    private val mediumWordsEn: List<String> by lazy { loadAssetLines("words_en_medium.txt") }
    private val hardWordsEn: List<String> by lazy { loadAssetLines("words_en_hard.txt") }
    private val phraseWordsEn: List<String> by lazy { loadAssetLines("words_en_phrases.txt") }

    private fun loadAssetLines(fileName: String): List<String> {
        return try {
            context.assets.open(fileName).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).useLines { lines ->
                    lines.map { it.trim() }.filter { it.isNotEmpty() }.toList()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getWords(
        difficulty: String,
        count: Int,
        excludedWords: List<String> = emptyList(),
        language: String = "ru"
    ): List<String> {
        val canonicalDiff = AiPromptHelper.normalizeDifficulty(difficulty)
        val pool = if (language == "en") {
            when (canonicalDiff) {
                AiPromptHelper.DIFFICULTY_FAMILY, AiPromptHelper.DIFFICULTY_EASY -> easyWordsEn.ifEmpty { mediumWordsEn }
                AiPromptHelper.DIFFICULTY_HARD -> hardWordsEn.ifEmpty { mediumWordsEn }
                AiPromptHelper.DIFFICULTY_PHRASES -> phraseWordsEn.ifEmpty { hardWordsEn }
                else -> mediumWordsEn.ifEmpty { easyWordsEn }
            }
        } else {
            when (canonicalDiff) {
                AiPromptHelper.DIFFICULTY_FAMILY, AiPromptHelper.DIFFICULTY_EASY -> easyWordsRu.ifEmpty { mediumWordsRu }
                AiPromptHelper.DIFFICULTY_HARD -> hardWordsRu.ifEmpty { mediumWordsRu }
                AiPromptHelper.DIFFICULTY_PHRASES -> phraseWordsRu.ifEmpty { hardWordsRu }
                else -> mediumWordsRu.ifEmpty { easyWordsRu }
            }
        }

        if (pool.isEmpty()) {
            return if (language == "en") {
                listOf("Word", "Game", "Team", "Victory", "Time")
            } else {
                listOf("Слово", "Игра", "Команда", "Победа", "Время")
            }
        }

        val excludedSet = excludedWords.map { it.lowercase() }.toSet()
        val available = pool.filterNot { it.lowercase() in excludedSet }

        val resultPool = if (available.size >= count) available else pool
        return resultPool.shuffled().take(count)
    }

    fun getCreativeTeams(count: Int, language: String = "ru"): List<String> =
        AiPromptHelper.getCreativeTeams(count, language = language)
}
