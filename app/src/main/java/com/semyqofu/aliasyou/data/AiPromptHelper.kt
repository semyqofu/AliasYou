package com.semyqofu.aliasyou.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class FormatValidationException(message: String) : Exception(message)

object AiPromptHelper {

    const val DIFFICULTY_FAMILY = "Семья"
    const val DIFFICULTY_EASY = "Легкая"
    const val DIFFICULTY_MEDIUM = "Средняя"
    const val DIFFICULTY_HARD = "Сложная"
    const val DIFFICULTY_PHRASES = "Словосочетания"

    val CREATIVE_TEAMS_POOL_RU = listOf(
        "Квантовые Лисы", "Космические Еноты", "Нейро-Пираты", "Астральные Волки",
        "Огненные Драконы", "Мастера Слов", "Искатели Смысла", "Острые Умы",
        "Фантазёры", "Ловцы Ассоциаций", "Великие Мыслители", "Бунтари Смысла",
        "Шифровальщики", "Тайные Агенты", "Гроссмейстеры", "Повелители Букв",
        "Знатоки Игры", "Властелины Времени", "Генераторы Идей", "Словесные Снайперы",
        "Эрудиты", "Интеллектуалы", "Звёздные Странники", "Магистры Речи",
        "Капитаны Очевидность", "Рыцари Логики", "Искатели Приключений", "Солнечные Зайцы",
        "Громовержцы", "Снежные Барсы", "Северные Сияния", "Короли Пантомимы"
    )

    val CREATIVE_TEAMS_POOL_EN = listOf(
        "Quantum Foxes", "Cosmic Raccoons", "Neuro Pirates", "Astral Wolves",
        "Blazing Dragons", "Wordsmiths", "Mind Seekers", "Sharp Minds",
        "Dream Weavers", "Clue Hunters", "Grand Thinkers", "Meaning Rebels",
        "Code Breakers", "Secret Agents", "Grandmasters", "Letter Lords",
        "Trivia Titans", "Time Lords", "Idea Sparks", "Word Snipers",
        "Brainiacs", "Smart Alecks", "Star Wanderers", "Speech Wizards",
        "Captain Obvious", "Logic Knights", "Quest Seekers", "Neon Owls",
        "Thunderbolts", "Snow Leopards", "Northern Lights", "Pantomime Kings"
    )

    private val lenientJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val prettyJson = Json {
        prettyPrint = true
    }

    fun normalizeDifficulty(difficulty: String): String {
        return when (difficulty.trim().lowercase()) {
            "семья", "family" -> DIFFICULTY_FAMILY
            "легкая", "лёгкая", "easy" -> DIFFICULTY_EASY
            "средняя", "medium" -> DIFFICULTY_MEDIUM
            "сложная", "hard" -> DIFFICULTY_HARD
            "словосочетания", "phrases" -> DIFFICULTY_PHRASES
            else -> DIFFICULTY_MEDIUM
        }
    }

    fun getCreativeTeams(
        count: Int,
        existing: List<String> = emptyList(),
        language: String = "ru"
    ): List<String> {
        val targetCount = count.coerceAtLeast(1)
        val cleanedExisting = existing
            .map { it.trim().trim('"', '\'', '«', '»') }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }

        if (cleanedExisting.size >= targetCount) {
            return cleanedExisting.take(targetCount)
        }

        val pool = if (language == "en") CREATIVE_TEAMS_POOL_EN else CREATIVE_TEAMS_POOL_RU
        val defaultPrefix = if (language == "en") "Team" else "Команда"

        val usedLower = cleanedExisting.map { it.lowercase() }.toMutableSet()
        val extras = pool.shuffled().filter { it.lowercase() !in usedLower }
        val result = (cleanedExisting + extras).take(targetCount)
        return if (result.size >= targetCount) {
            result
        } else {
            result + ((result.size + 1)..targetCount).map { "$defaultPrefix $it" }
        }
    }

    fun computeRequestCount(wordCount: Int): Int {
        return (wordCount * 1.35).toInt().coerceIn(wordCount + 15, 240)
    }

    fun buildSystemInstruction(difficulty: String, language: String = "ru"): String {
        val canonicalDiff = normalizeDifficulty(difficulty)
        val isPhraseMode = canonicalDiff == DIFFICULTY_PHRASES

        if (language == "en") {
            return if (isPhraseMode) {
                """
                You are a strict party-game card generator for the extreme mode of Alias in English.
                Your ONLY task is to return a single strictly valid JSON object and NOTHING else.

                STRICT RULES FOR THE "words" ARRAY:
                1. EVERY element in "words" MUST be an idiom, catchphrase, or compound expression of 2 to 4 English words separated by spaces (e.g. "Butterfly effect", "Silver lining", "Trojan horse", "Couch potato").
                2. Single 1-word entries are STRICTLY FORBIDDEN.
                3. Do NOT include numbering ("1. "), bullet points, hints in parentheses, or dash explanations.
                4. Capitalize the first letter of each entry. All entries must be unique.
                5. Generate creative English team names in "teams".

                STRICT RESPONSE FORMAT:
                - Return ONLY a valid JSON object: {"words": ["Phrase 1", "Phrase 2"], "teams": ["Team 1", "Team 2"]}
                - NO greeting, NO commentary, NO markdown code fences (```json). Only raw valid JSON.
                """.trimIndent()
            } else {
                """
                You are a strict party-game card generator for Alias in English.
                Your ONLY task is to return a single strictly valid JSON object and NOTHING else.

                STRICT RULES FOR THE "words" ARRAY:
                1. EVERY element in "words" MUST be STRICTLY ONE SINGLE ENGLISH WORD (exactly 1 word, ZERO spaces!).
                2. Multi-word phrases, idioms, two-word expressions, prepositions, parenthetical explanations, numbering ("1. "), and joined words are STRICTLY FORBIDDEN.
                3. Prefer singular nouns in their base form capitalized (or natural plurals like "Scissors", "Glasses").
                4. NO duplicates, consecutive cognates, or obvious consecutive antonym pairs (do NOT put "Cat" next to "Dog", "Sun" next to "Moon", "Hot" next to "Cold").
                5. Generate creative English team names in "teams".

                STRICT RESPONSE FORMAT:
                - Return ONLY a valid JSON object: {"words": ["Word1", "Word2"], "teams": ["Team 1", "Team 2"]}
                - NO greeting, NO commentary, NO markdown code fences (```json). Only raw valid JSON.
                """.trimIndent()
            }
        }

        return if (isPhraseMode) {
            """
            Ты — строгий генератор игровых карточек для экстремального режима игры Alias на русском языке.
            Твоя ЕДИНСТВЕННАЯ задача — вернуть один строго валидный JSON-объект и НИЧЕГО больше.

            ЖЕСТКИЕ ПРАВИЛА ДЛЯ МАССИВА "words":
            1. КАЖДЫЙ элемент в "words" ДОЛЖЕН быть словосочетанием, идиомой, крылатым выражением или составным термином из 2–4 слов через пробел (например: "Кот в мешке", "Эффект бабочки", "Синдром самозванца").
            2. Одиночные слова (из 1 слова без пробела) СТРОГО ЗАПРЕЩЕНЫ.
            3. Не добавляй нумерацию ("1. "), буллиты, пояснения в скобках или тире с расшифровкой.
            4. Каждый элемент пиши с заглавной первой буквы. Все элементы должны быть уникальными.

            ЖЕСТКИЙ ФОРМАТ ОТВЕТА:
            - Верни СТРОГО валидный JSON-объект формата: {"words": ["Фраза 1", "Фраза 2"], "teams": ["Команда 1", "Команда 2"]}
            - НИКАКОГО вводного текста, комментариев, объяснений и markdown-блоков (```json). Только чистый JSON.
            """.trimIndent()
        } else {
            """
            Ты — строгий генератор игровых карточек для настольной игры Alias на русском языке.
            Твоя ЕДИНСТВЕННАЯ задача — вернуть один строго валидный JSON-объект и НИЧЕГО больше.

            ЖЕСТКИЕ ПРАВИЛА ДЛЯ МАССИВА "words":
            1. КАЖДЫЙ элемент в "words" ДОЛЖЕН быть СТРОГО ОДНИМ-ЕДИНСТВЕННЫМ русским словом (ровно 1 слово, АБСОЛЮТНО БЕЗ ПРОБЕЛОВ!).
            2. КАТЕГОРИЧЕСКИ ЗАПРЕЩЕНЫ любые словосочетания, пары слов, идиомы, предлоги, уточнения в скобках, комментарии, нумерация ("1. ") и склейки слов.
            3. Предпочтительная часть речи — существительное в именительном падеже единственного числа (или естественном множественном, например "Ножницы", "Очки") с заглавной буквы.
            4. ЗАПРЕЩЕНЫ повторы, однокоренные слова подряд и очевидные пары-антонимы подряд (НЕ ставь рядом "кошка" и "собака", "солнце" и "луна", "день" и "ночь", "огонь" и "вода").

            ЖЕСТКИЙ ФОРМАТ ОТВЕТА:
            - Верни СТРОГО валидный JSON-объект формата: {"words": ["Слово1", "Слово2"], "teams": ["Команда 1", "Команда 2"]}
            - НИКАКОГО вводного текста, комментариев, объяснений и markdown-блоков (```json). Только чистый JSON.
            """.trimIndent()
        }
    }

    fun buildUserPrompt(
        theme: String,
        difficulty: String,
        wordCount: Int,
        teamCount: Int,
        excludedWords: List<String>,
        seed: Int,
        strictRetryNote: Boolean = false,
        language: String = "ru"
    ): String {
        val requestCount = computeRequestCount(wordCount)
        val canonicalDiff = normalizeDifficulty(difficulty)

        if (language == "en") {
            val effectiveTheme = theme.trim().ifEmpty { "General" }
            val difficultyInstructions = when (canonicalDiff) {
                DIFFICULTY_FAMILY -> "ONLY simple, friendly, kid-safe single English words (strictly 1 word without spaces): animals, food, nature, toys, everyday household items. Exclude rude, scary, adult, or obscure technical jargon."
                DIFFICULTY_EASY -> "ONLY popular, widely known single English nouns from daily life and culture (strictly 1 word without spaces, no phrases)."
                DIFFICULTY_MEDIUM -> "ONLY engaging medium-difficulty single English words (strictly 1 word without spaces!): phenomena, professions, concepts, art, science, objects. PHRASES ARE FORBIDDEN!"
                DIFFICULTY_HARD -> "ONLY rare, intellectual, scientific, philosophical, or unexpected single English words (strictly 1 word without spaces!). NO multi-word phrases or idioms!"
                DIFFICULTY_PHRASES -> "ONLY well-known idioms, catchphrases, compound expressions, and multi-word concepts of 2–4 English words separated by spaces (e.g. \"Butterfly effect\", \"Silver lining\", \"Elephant in the room\"). Single words are forbidden!"
                else -> "ONLY medium-difficulty single English words (strictly 1 word without spaces, NO phrases)."
            }

            val exclusionText = if (excludedWords.isNotEmpty()) {
                val recentExclusions = excludedWords.take(150).joinToString(", ")
                "\nFORBIDDEN WORDS (already played, DO NOT repeat them): [$recentExclusions]."
            } else ""

            val retryPrefix = if (strictRetryNote) {
                val extraRule = if (canonicalDiff == DIFFICULTY_PHRASES) {
                    "EVERY item in \"words\" MUST contain at least 2 words separated by a space!"
                } else {
                    "EVERY item in \"words\" MUST be STRICTLY ONE WORD WITH ZERO SPACES! Remove all multi-word phrases!"
                }
                "WARNING! PREVIOUS ATTEMPT FAILED VALIDATION! RETURN PURE VALID JSON ONLY WITH NO PROSE OR MARKDOWN. $extraRule\n\n"
            } else ""

            val elementRule = if (canonicalDiff == DIFFICULTY_PHRASES) {
                "each item must be 2–4 English words separated by spaces"
            } else {
                "strictly 1 English word with zero spaces per item"
            }

            return """
                ${retryPrefix}Seed: $seed.
                Generate exactly $requestCount UNIQUE English items for the "words" array ($elementRule) and $teamCount creative English team names for the "teams" array.

                Theme: "$effectiveTheme".
                Difficulty: "$canonicalDiff".
                Requirements: $difficultyInstructions$exclusionText

                Mandatory output format (strictly valid JSON with no extra text and no ```):
                {"words": [...], "teams": [...]}
            """.trimIndent()
        }

        val effectiveTheme = theme.trim().ifEmpty { "Общая" }

        val difficultyInstructions = when (canonicalDiff) {
            DIFFICULTY_FAMILY -> "ТОЛЬКО простые, добрые и понятные даже ребёнку 6 лет одиночные слова (строго 1 слово без пробелов): животные, еда, природа, игрушки, быт. Исключи грубые, пугающие, взрослые или узкоспециальные термины."
            DIFFICULTY_EASY -> "ТОЛЬКО простые общеизвестные одиночные существительные из повседневной жизни и культуры (строго 1 слово без пробелов, никаких фраз)."
            DIFFICULTY_MEDIUM -> "ТОЛЬКО интересные одиночные слова средней сложности (строго 1 слово без пробелов!): явления, профессии, понятия, предметы и события. СЛОВОСОЧЕТАНИЯ ЗАПРЕЩЕНЫ!"
            DIFFICULTY_HARD -> "ТОЛЬКО редкие, интеллектуальные, научные, философские или нетривиальные одиночные слова (строго 1 слово без пробелов!). НИКАКИХ словосочетаний, идиом и многословных выражений!"
            DIFFICULTY_PHRASES -> "ТОЛЬКО устойчивые словосочетания, крылатые фразы, идиомы и составные понятия из 2–4 слов через пробел (например: \"Эффект плацебо\", \"Белая ворона\", \"Пятое колесо\"). Одиночные слова запрещены!"
            else -> "ТОЛЬКО одиночные слова средней сложности (строго 1 слово без пробелов, БЕЗ словосочетаний)."
        }

        val exclusionText = if (excludedWords.isNotEmpty()) {
            val recentExclusions = excludedWords.take(150).joinToString(", ")
            "\nЗАПРЕЩЁННЫЕ СЛОВА (они уже играли, НЕ повторяй их): [$recentExclusions]."
        } else ""

        val retryPrefix = if (strictRetryNote) {
            val extraRule = if (canonicalDiff == DIFFICULTY_PHRASES) {
                "КАЖДЫЙ элемент в \"words\" должен состоять минимум из 2 слов через пробел!"
            } else {
                "КАЖДЫЙ элемент в \"words\" должен быть СТРОГО ОДНИМ СЛОВОМ БЕЗ ПРОБЕЛОВ! Удали все фразы из 2+ слов!"
            }
            "ВНИМАНИЕ! ПРЕДЫДУЩАЯ ПОПЫТКА НЕ ПРОШЛА ПРОВЕРКУ! ВЕРНИ ИСКЛЮЧИТЕЛЬНО ЧИСТЫЙ JSON БЕЗ ПОЯСНЕНИЙ И МАРКДАУНА. $extraRule\n\n"
        } else ""

        val elementRule = if (canonicalDiff == DIFFICULTY_PHRASES) {
            "каждый элемент из 2–4 слов через пробел"
        } else {
            "строго 1 слово без пробелов в каждом элементе"
        }

        return """
            ${retryPrefix}Seed: $seed.
            Сгенерируй ровно $requestCount УНИКАЛЬНЫХ позиций для массива "words" ($elementRule) и $teamCount креативных названий команд для массива "teams".

            Тема: "$effectiveTheme".
            Сложность: "$canonicalDiff".
            Требования: $difficultyInstructions$exclusionText

            Обязательный формат ответа (строго валидный JSON без пояснений и без ```):
            {"words": [...], "teams": [...]}
        """.trimIndent()
    }

    fun extractValidJsonObject(rawText: String): String {
        val stripped = rawText
            .replace(Regex("""```json\s*""", RegexOption.IGNORE_CASE), "")
            .replace("```", "")
            .trim()

        val firstBrace = stripped.indexOf('{')
        if (firstBrace == -1) {
            throw FormatValidationException("AI response does not contain a JSON object")
        }

        var depth = 0
        var inString = false
        var escape = false
        var endIndex = -1

        for (i in firstBrace until stripped.length) {
            val ch = stripped[i]
            if (escape) {
                escape = false
                continue
            }
            if (ch == '\\' && inString) {
                escape = true
                continue
            }
            if (ch == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (ch == '{') {
                    depth++
                } else if (ch == '}') {
                    depth--
                    if (depth == 0) {
                        endIndex = i
                        break
                    }
                }
            }
        }

        val candidate = if (endIndex > firstBrace) {
            stripped.substring(firstBrace, endIndex + 1)
        } else {
            repairTruncatedJson(stripped.substring(firstBrace))
        }

        return removeTrailingCommas(candidate)
    }

    private fun repairTruncatedJson(partial: String): String {
        val wordsKeyIndex = partial.indexOf("\"words\"")
        if (wordsKeyIndex == -1) return partial
        val bracketStart = partial.indexOf('[', wordsKeyIndex)
        if (bracketStart == -1) return partial

        val lastQuote = partial.lastIndexOf('"')
        val lastComma = partial.lastIndexOf(',')
        val safeEnd = when {
            lastComma > bracketStart && (lastQuote < lastComma || partial.indexOf('"', lastComma) == -1) -> lastComma
            lastQuote > bracketStart -> {
                var quoteCount = 0
                var escaped = false
                for (i in (bracketStart + 1) until partial.length) {
                    val c = partial[i]
                    if (escaped) {
                        escaped = false
                        continue
                    }
                    if (c == '\\') {
                        escaped = true
                        continue
                    }
                    if (c == '"') quoteCount++
                }
                if (quoteCount % 2 == 0) lastQuote + 1 else lastComma.coerceAtLeast(bracketStart + 1)
            }
            else -> partial.length
        }

        val prefix = partial.substring(0, safeEnd).trimEnd().trimEnd(',')
        return "$prefix]}"
    }

    private fun removeTrailingCommas(json: String): String {
        val sb = StringBuilder(json.length)
        var inString = false
        var escape = false
        var i = 0
        while (i < json.length) {
            val ch = json[i]
            if (escape) {
                sb.append(ch)
                escape = false
                i++
                continue
            }
            if (ch == '\\' && inString) {
                sb.append(ch)
                escape = true
                i++
                continue
            }
            if (ch == '"') {
                inString = !inString
                sb.append(ch)
                i++
                continue
            }
            if (!inString && ch == ',') {
                var j = i + 1
                while (j < json.length && json[j].isWhitespace()) {
                    j++
                }
                if (j < json.length && (json[j] == ']' || json[j] == '}')) {
                    i++
                    continue
                }
            }
            sb.append(ch)
            i++
        }
        return sb.toString()
    }

    fun parseAndValidate(
        rawText: String,
        difficulty: String,
        requestedWordCount: Int,
        teamCount: Int,
        excludedWords: List<String>,
        language: String = "ru"
    ): GameResult {
        val validJson = extractValidJsonObject(rawText)
        val rootElement = try {
            lenientJson.parseToJsonElement(validJson)
        } catch (e: Exception) {
            throw FormatValidationException("Invalid JSON from AI: ${e.message}")
        }

        val rootObject = rootElement as? JsonObject
            ?: throw FormatValidationException("Root element is not a JSON object")

        val wordsArray = (rootObject["words"]
            ?: rootObject["cards"]
            ?: rootObject["items"]
            ?: rootObject["слова"]) as? JsonArray
            ?: throw FormatValidationException("Missing \"words\" array in JSON response")

        val rawWords = wordsArray.mapNotNull { el ->
            when (el) {
                is JsonPrimitive -> el.content
                else -> null
            }
        }

        if (rawWords.isEmpty()) {
            throw FormatValidationException("AI returned an empty words array")
        }

        val canonicalDiff = normalizeDifficulty(difficulty)
        val allowPhrases = canonicalDiff == DIFFICULTY_PHRASES
        val cleanedCandidates = rawWords.mapNotNull { cleanCandidate(it, allowPhrases) }

        val excludedSet = excludedWords.map { it.trim().lowercase() }.toSet()
        val uniqueAllValid = cleanedCandidates.distinctBy { it.lowercase() }
        val nonExcluded = uniqueAllValid.filterNot { it.lowercase() in excludedSet }

        val targetCap = if (allowPhrases) 20 else 25
        val minRequiredThreshold = minOf(
            requestedWordCount,
            (requestedWordCount * 0.4).toInt().coerceIn(minOf(requestedWordCount, 5), targetCap)
        )

        if (nonExcluded.size < minRequiredThreshold && uniqueAllValid.size < minRequiredThreshold) {
            val detail = if (allowPhrases) {
                "too few valid multi-word phrases (${uniqueAllValid.size})"
            } else {
                "too few valid single words (${uniqueAllValid.size})"
            }
            throw FormatValidationException("Generation rules violated: $detail")
        }

        val selectedWords = if (nonExcluded.size >= requestedWordCount) {
            nonExcluded.take(requestedWordCount)
        } else if (nonExcluded.size >= minRequiredThreshold) {
            nonExcluded
        } else {
            uniqueAllValid.take(requestedWordCount)
        }.shuffled()

        val teamsArray = (rootObject["teams"] ?: rootObject["команды"]) as? JsonArray
        val parsedTeams = teamsArray?.mapNotNull { (it as? JsonPrimitive)?.content } ?: emptyList()
        val finalTeams = getCreativeTeams(teamCount, parsedTeams, language = language)

        val normalizedData = GameData(words = selectedWords, teams = finalTeams)
        val formattedJson = try {
            prettyJson.encodeToString(normalizedData)
        } catch (_: Exception) {
            validJson
        }

        return GameResult(
            gameData = normalizedData,
            rawJson = formattedJson
        )
    }

    internal fun cleanCandidate(raw: String, allowPhrases: Boolean): String? {
        var text = raw
            .replace(Regex("""^\s*(?:\d+[.):]|[-–—•*])\s*"""), "")
            .replace(Regex("""\s*\([^)]*\)"""), "")
            .trim()
            .trim('"', '\'', '«', '»', '`', '.', ',', ';', ':')

        val dashExplanationIdx = listOf(" — ", " – ", " - ", ": ").mapNotNull { sep ->
            text.indexOf(sep).takeIf { it > 0 }
        }.minOrNull()
        if (dashExplanationIdx != null) {
            text = text.substring(0, dashExplanationIdx).trim()
        }

        text = text.replace(Regex("""\s+"""), " ").trim()
        if (text.isEmpty()) return null

        return if (allowPhrases) {
            val tokens = text.split(' ').filter { it.isNotBlank() }
            if (tokens.size !in 2..5) return null
            if (text.length !in 4..55 || text.count { it.isLetter() } < 4) return null
            text.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else {
            if (text.any { it.isWhitespace() }) return null
            if (text.count { it == '-' } > 1) return null
            text = text.trim { !it.isLetterOrDigit() && it != '-' }
            if (text.startsWith('-') || text.endsWith('-')) return null
            if (text.length !in 2..32 || text.count { it.isLetter() } < 2) return null
            text.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
