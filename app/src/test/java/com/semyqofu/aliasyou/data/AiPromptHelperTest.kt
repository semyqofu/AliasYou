package com.semyqofu.aliasyou.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPromptHelperTest {

    @Test
    fun singleWordMode_rejectsPhrasesAndCleansPrefixes() {
        val rawResponse = """
            Конечно! Вот ваш JSON:
            ```json
            {
              "words": [
                "1. Космонавт",
                "Белая ворона",
                "Телескоп (оптический)",
                "Звезда — небесное тело",
                "Кот в мешке",
                "Галактика",
                "Планета",
                "Комета",
              ],
              "teams": ["Альфа", "Бета"]
            }
            ```
            Надеюсь, игра понравится!
        """.trimIndent()

        val result = AiPromptHelper.parseAndValidate(
            rawText = rawResponse,
            difficulty = AiPromptHelper.DIFFICULTY_MEDIUM,
            requestedWordCount = 5,
            teamCount = 2,
            excludedWords = listOf("комета")
        )

        val words = result.gameData.words
        assertEquals(5, words.size)
        assertTrue(words.containsAll(listOf("Космонавт", "Телескоп", "Звезда", "Галактика", "Планета")))
        assertFalse(words.any { it.contains(" ") })
        assertFalse(words.any { it.equals("Комета", ignoreCase = true) })
        assertEquals(listOf("Альфа", "Бета"), result.gameData.teams)
    }

    @Test
    fun repairsTruncatedJsonInsideTeamsArray() {
        val truncatedInTeams = """
            {
              "words": ["Сигнал", "Орбита", "Космос", "Ракета", "Спутник"],
              "teams": ["Комета", "Метео
        """.trimIndent()

        val result = AiPromptHelper.parseAndValidate(
            rawText = truncatedInTeams,
            difficulty = AiPromptHelper.DIFFICULTY_MEDIUM,
            requestedWordCount = 5,
            teamCount = 2,
            excludedWords = emptyList()
        )

        assertEquals(5, result.gameData.words.size)
        assertEquals(2, result.gameData.teams.size)
        assertTrue(result.gameData.teams.contains("Комета"))
    }

    @Test(expected = FormatValidationException::class)
    fun singleWordMode_throwsWhenMostlyPhrasesReturned() {
        val rawResponse = """
            {
              "words": [
                "Белая ворона",
                "Кот в мешке",
                "Синдром самозванца",
                "Эффект бабочки",
                "Троянский конь",
                "Космос"
              ],
              "teams": ["Команда 1", "Команда 2"]
            }
        """.trimIndent()

        AiPromptHelper.parseAndValidate(
            rawText = rawResponse,
            difficulty = AiPromptHelper.DIFFICULTY_HARD,
            requestedWordCount = 20,
            teamCount = 2,
            excludedWords = emptyList()
        )
    }

    @Test
    fun phrasesMode_keepsMultiWordAndDropsSingleWord() {
        val rawResponse = """
            {
              "words": [
                "1. Эффект бабочки",
                "Яблоко",
                "Кот Шрёдингера",
                "Синдром самозванца",
                "Звезда",
                "Бритва Оккама",
                "Черная дыра"
              ]
            }
        """.trimIndent()

        val result = AiPromptHelper.parseAndValidate(
            rawText = rawResponse,
            difficulty = AiPromptHelper.DIFFICULTY_PHRASES,
            requestedWordCount = 4,
            teamCount = 3,
            excludedWords = emptyList()
        )

        val words = result.gameData.words
        assertEquals(4, words.size)
        assertTrue(words.all { it.contains(" ") })
        assertFalse(words.contains("Яблоко"))
        assertFalse(words.contains("Звезда"))
        // Missing teams field should safely fall back to creative teams
        assertEquals(3, result.gameData.teams.size)
    }

    @Test
    fun repairsTruncatedJsonWithPartialWordList() {
        val truncated = """
            {
              "words": ["Атом", "Молекула", "Нейтрон", "Кварк", "Фотон", "Прот
        """.trimIndent()

        val result = AiPromptHelper.parseAndValidate(
            rawText = truncated,
            difficulty = AiPromptHelper.DIFFICULTY_HARD,
            requestedWordCount = 5,
            teamCount = 2,
            excludedWords = emptyList()
        )

        assertEquals(5, result.gameData.words.size)
        assertTrue(result.gameData.words.containsAll(listOf("Атом", "Молекула", "Нейтрон", "Кварк", "Фотон")))
    }

    @Test
    fun promptsClearlyEnforceWordVsPhraseRules() {
        val hardSystem = AiPromptHelper.buildSystemInstruction(AiPromptHelper.DIFFICULTY_HARD)
        val hardUser = AiPromptHelper.buildUserPrompt("Наука", AiPromptHelper.DIFFICULTY_HARD, 50, 2, emptyList(), 42)
        assertTrue(hardSystem.contains("БЕЗ ПРОБЕЛОВ"))
        assertTrue(hardUser.contains("НИКАКИХ словосочетаний"))

        val phraseSystem = AiPromptHelper.buildSystemInstruction(AiPromptHelper.DIFFICULTY_PHRASES)
        val phraseUser = AiPromptHelper.buildUserPrompt("Наука", AiPromptHelper.DIFFICULTY_PHRASES, 50, 2, emptyList(), 42)
        assertTrue(phraseSystem.contains("словосочетанием"))
        assertTrue(phraseUser.contains("Одиночные слова запрещены"))
    }

    @Test
    fun englishPromptsAndTeamFallbackWorkCleanly() {
        val enSystem = AiPromptHelper.buildSystemInstruction(AiPromptHelper.DIFFICULTY_MEDIUM, language = "en")
        val enUser = AiPromptHelper.buildUserPrompt("Science", AiPromptHelper.DIFFICULTY_MEDIUM, 40, 2, emptyList(), 7, language = "en")
        assertTrue(enSystem.contains("SINGLE ENGLISH WORD"))
        assertTrue(enUser.contains("ENGLISH", ignoreCase = true))

        val enRaw = """
            {
              "words": ["Nebula", "Dark matter", "Gravity", "Quasar", "Photon", "Black hole", "Compass", "Prism"]
            }
        """.trimIndent()
        val parsed = AiPromptHelper.parseAndValidate(
            rawText = enRaw,
            difficulty = AiPromptHelper.DIFFICULTY_MEDIUM,
            requestedWordCount = 5,
            teamCount = 2,
            excludedWords = emptyList(),
            language = "en"
        )
        assertEquals(5, parsed.gameData.words.size)
        assertFalse(parsed.gameData.words.any { it.contains(" ") })
        assertEquals(2, parsed.gameData.teams.size)
        assertTrue(parsed.gameData.teams.all { it in AiPromptHelper.CREATIVE_TEAMS_POOL_EN })
    }
}
