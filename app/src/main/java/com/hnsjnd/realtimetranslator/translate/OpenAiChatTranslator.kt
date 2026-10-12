package com.hnsjnd.realtimetranslator.translate

import android.util.Log
import com.hnsjnd.realtimetranslator.data.AiProviderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * OpenAI-compatible chat completions translator.
 * Uses a tight system prompt optimized for real-time subtitle-style output.
 */
class OpenAiChatTranslator(
    private val configProvider: () -> AiProviderConfig,
    private val client: OkHttpClient = defaultClient()
) : TranslationService {

    override suspend fun translate(text: String, sourceLang: String, targetLang: String): String =
        withContext(Dispatchers.IO) {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return@withContext ""

            val cfg = configProvider()
            if (!cfg.isConfigured()) {
                throw IllegalStateException("请先在设置中配置 API Key 与 Base URL")
            }

            val srcLabel = langLabel(sourceLang)
            val tgtLabel = langLabel(targetLang)
            val system = buildString {
                append("You are a real-time translation engine for live subtitles. ")
                append("Translate the user's utterance")
                if (sourceLang != "auto") append(" from $srcLabel")
                append(" into $tgtLabel. ")
                append("Rules: output ONLY the translation, no quotes, no explanations, ")
                append("no romanization, preserve proper nouns when appropriate, ")
                append("keep it concise for on-screen display.")
            }

            val payload = JSONObject().apply {
                put("model", cfg.chatModel.ifBlank { "gpt-4o-mini" })
                put("temperature", 0.2)
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", system))
                    put(JSONObject().put("role", "user").put("content", trimmed))
                })
            }

            val body = payload.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(cfg.resolvedChatUrl())
                .addHeader("Authorization", "Bearer ${cfg.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            client.newCall(request).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    Log.e(TAG, "Translate HTTP ${resp.code}: $raw")
                    throw RuntimeException("翻译失败 (${resp.code}): ${raw.take(200)}")
                }
                val result = try {
                    JSONObject(raw)
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                        .trim()
                } catch (e: Exception) {
                    Log.e(TAG, "parse error: $raw", e)
                    throw RuntimeException("翻译响应解析失败")
                }
                Log.d(TAG, "translate ok: ${result.take(80)}")
                result
            }
        }

    private fun langLabel(code: String): String = when (code) {
        "zh" -> "Chinese"
        "en" -> "English"
        "ja" -> "Japanese"
        "ko" -> "Korean"
        "fr" -> "French"
        "de" -> "German"
        "es" -> "Spanish"
        "ru" -> "Russian"
        "auto" -> "the source language (auto-detect)"
        else -> code
    }

    companion object {
        private const val TAG = "OpenAiChatTranslator"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
