package com.semyqofu.aliasyou.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.random.Random

class GigaChatService(
    private val authKey: String,
    val modelName: String = "GigaChat-2",
    private val scope: String = "GIGACHAT_API_PERS"
) : AiService {

    // Custom OkHttpClient with trust manager to support Sber's Russian Root CA without system cert installation
    private val client: OkHttpClient by lazy {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())

            OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        } catch (e: Exception) {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
        }
    }

    private suspend fun getAccessToken(): String = withContext(Dispatchers.IO) {
        val trimmedKey = authKey.trim()
        if (trimmedKey.startsWith("eyJ")) {
            return@withContext trimmedKey
        }

        val url = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth"
        val requestBody = FormBody.Builder()
            .add("scope", if (scope.isNotBlank()) scope else "GIGACHAT_API_PERS")
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Basic $trimmedKey")
            .addHeader("RqUID", UUID.randomUUID().toString())
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw Exception("Ошибка авторизации GigaChat (HTTP ${response.code}): $responseBody")
        }

        val json = JSONObject(responseBody)
        if (!json.has("access_token")) {
            throw Exception("В ответе авторизации GigaChat отсутствует access_token: $responseBody")
        }
        json.getString("access_token")
    }

    private fun requestCompletionContent(
        token: String,
        systemInstruction: String,
        userPrompt: String,
        temperature: Double
    ): String {
        val chatUrl = "https://api.giga.chat/v1/chat/completions"

        val messagesArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemInstruction)
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", userPrompt)
            })
        }

        val requestJson = JSONObject().apply {
            put("model", if (modelName.isBlank()) "GigaChat-2" else modelName)
            put("messages", messagesArray)
            put("temperature", temperature)
            put("stream", false)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(chatUrl)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw Exception("Ошибка GigaChat API (HTTP ${response.code}): $responseBody")
        }

        val responseJsonObj = JSONObject(responseBody)
        val choices = responseJsonObj.optJSONArray("choices")
            ?: throw Exception("В ответе GigaChat нет поля choices: $responseBody")

        if (choices.length() == 0) {
            throw FormatValidationException("GigaChat вернул пустой список вариантов")
        }

        val messageObj = choices.getJSONObject(0).optJSONObject("message")
            ?: throw FormatValidationException("Неверная структура ответа GigaChat message")

        val content = messageObj.optString("content", "")
        if (content.isBlank()) {
            throw FormatValidationException("Пустой текст ответа от GigaChat")
        }
        return content
    }

    override suspend fun generateGameData(
        theme: String,
        difficulty: String,
        wordCount: Int,
        teamCount: Int,
        excludedWords: List<String>,
        language: String
    ): GameResult = withContext(Dispatchers.IO) {
        val token = getAccessToken()
        val systemInstruction = AiPromptHelper.buildSystemInstruction(difficulty, language)

        var lastFormatError: FormatValidationException? = null

        // Up to 2 attempts: initial generation + strict low-temperature retry if format/word rules fail
        for (attempt in 0..1) {
            val temperature = if (attempt == 0) 0.75 else 0.3
            val userPrompt = AiPromptHelper.buildUserPrompt(
                theme = theme,
                difficulty = difficulty,
                wordCount = wordCount,
                teamCount = teamCount,
                excludedWords = excludedWords,
                seed = Random.nextInt(1_000_000),
                strictRetryNote = (attempt > 0),
                language = language
            )

            try {
                val content = requestCompletionContent(
                    token = token,
                    systemInstruction = systemInstruction,
                    userPrompt = userPrompt,
                    temperature = temperature
                )
                return@withContext AiPromptHelper.parseAndValidate(
                    rawText = content,
                    difficulty = difficulty,
                    requestedWordCount = wordCount,
                    teamCount = teamCount,
                    excludedWords = excludedWords,
                    language = language
                )
            } catch (e: FormatValidationException) {
                lastFormatError = e
            }
        }

        throw lastFormatError ?: Exception("Не удалось получить корректный JSON от GigaChat")
    }
}
