package com.semyqofu.aliasyou.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlin.random.Random

class GeminiService(
    private val apiKey: String,
    val modelName: String = "gemini-flash-latest"
) : AiService {

    private val effectiveModelName = if (modelName.isBlank()) "gemini-flash-latest" else modelName

    private val gameDataSchema = Schema.obj(
        name = "GameData",
        description = "Карточки и названия команд для игры Alias",
        Schema.arr(
            name = "words",
            description = "Список уникальных слов (или словосочетаний для режима Словосочетания)",
            items = Schema.str(name = "word", description = "Карточка для объяснения")
        ),
        Schema.arr(
            name = "teams",
            description = "Список креативных названий команд",
            items = Schema.str(name = "team", description = "Название команды")
        )
    )

    private fun createModel(
        systemText: String,
        temperatureValue: Float,
        useSchema: Boolean,
        useJsonMimeType: Boolean
    ): GenerativeModel {
        return GenerativeModel(
            modelName = effectiveModelName,
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = temperatureValue
                if (useJsonMimeType) {
                    responseMimeType = "application/json"
                    if (useSchema) {
                        responseSchema = gameDataSchema
                    }
                }
            },
            systemInstruction = content { text(systemText) }
        )
    }

    override suspend fun generateGameData(
        theme: String,
        difficulty: String,
        wordCount: Int,
        teamCount: Int,
        excludedWords: List<String>,
        language: String
    ): GameResult {
        val systemText = AiPromptHelper.buildSystemInstruction(difficulty, language)
        val firstSeed = Random.nextInt(1_000_000)

        val firstPrompt = AiPromptHelper.buildUserPrompt(
            theme = theme,
            difficulty = difficulty,
            wordCount = wordCount,
            teamCount = teamCount,
            excludedWords = excludedWords,
            seed = firstSeed,
            strictRetryNote = false,
            language = language
        )

        var lastError: Exception? = null
        var useSchemaNext = true
        var useMimeNext = true

        // Up to 2 attempts: initial generation + strict retry on format/validation failure
        for (attempt in 0..1) {
            val temperatureValue = if (attempt == 0) 0.85f else 0.35f
            val promptToSend = if (attempt == 0) {
                firstPrompt
            } else {
                AiPromptHelper.buildUserPrompt(
                    theme = theme,
                    difficulty = difficulty,
                    wordCount = wordCount,
                    teamCount = teamCount,
                    excludedWords = excludedWords,
                    seed = Random.nextInt(1_000_000),
                    strictRetryNote = true,
                    language = language
                )
            }

            try {
                val model = createModel(
                    systemText = systemText,
                    temperatureValue = temperatureValue,
                    useSchema = useSchemaNext,
                    useJsonMimeType = useMimeNext
                )
                val response = model.generateContent(promptToSend)
                val text = response.text ?: throw FormatValidationException("Empty response from Gemini")
                return AiPromptHelper.parseAndValidate(
                    rawText = text,
                    difficulty = difficulty,
                    requestedWordCount = wordCount,
                    teamCount = teamCount,
                    excludedWords = excludedWords,
                    language = language
                )
            } catch (e: FormatValidationException) {
                lastError = e
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                val isSchemaOrConfigRejection =
                    msg.contains("schema", ignoreCase = true) ||
                        msg.contains("mime", ignoreCase = true) ||
                        msg.contains("system_instruction", ignoreCase = true)
                if (attempt == 0 && isSchemaOrConfigRejection) {
                    useSchemaNext = false
                    useMimeNext = !msg.contains("mime", ignoreCase = true)
                    lastError = e
                } else {
                    throw e
                }
            }
        }

        throw lastError ?: Exception("Не удалось получить корректный ответ от Gemini")
    }
}
