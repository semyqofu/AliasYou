package com.semyqofu.aliasyou.data

data class GameResult(
    val gameData: GameData,
    val rawJson: String
)

interface AiService {
    suspend fun generateGameData(
        theme: String,
        difficulty: String,
        wordCount: Int,
        teamCount: Int,
        excludedWords: List<String> = emptyList(),
        language: String = "ru"
    ): GameResult
}
