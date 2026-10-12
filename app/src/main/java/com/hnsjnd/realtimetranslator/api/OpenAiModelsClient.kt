package com.hnsjnd.realtimetranslator.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ModelLists(
    val allIds: List<String>,
    val chatModels: List<String>,
    val asrModels: List<String>
)

sealed class ModelsFetchResult {
    data class Success(val models: ModelLists) : ModelsFetchResult()
    data class Failure(val message: String) : ModelsFetchResult()
}

/**
 * OpenAI-compatible GET {baseUrl}/models client used by Settings「测试连接」.
 */
object OpenAiModelsClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    fun normalizeBaseUrl(raw: String): String = raw.trim().trimEnd('/')

    suspend fun fetchModels(baseUrl: String, apiKey: String): ModelsFetchResult =
        withContext(Dispatchers.IO) {
            val base = normalizeBaseUrl(baseUrl)
            if (base.isBlank()) {
                return@withContext ModelsFetchResult.Failure("请填写 API Base URL")
            }
            if (apiKey.isBlank()) {
                return@withContext ModelsFetchResult.Failure("请填写 API Key")
            }
            val url = "$base/models"
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer ${apiKey.trim()}")
                .addHeader("Accept", "application/json")
                .build()

            try {
                client.newCall(request).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        val detail = extractErrorMessage(body).ifBlank { body.take(180) }
                        return@withContext ModelsFetchResult.Failure(
                            "连接失败 HTTP ${resp.code}" +
                                if (detail.isNotBlank()) "：$detail" else ""
                        )
                    }
                    val ids = parseModelIds(body)
                    if (ids.isEmpty()) {
                        return@withContext ModelsFetchResult.Failure(
                            "连接成功，但未解析到模型列表（响应可能非 OpenAI 格式）"
                        )
                    }
                    ModelsFetchResult.Success(classifyModels(ids))
                }
            } catch (e: Exception) {
                ModelsFetchResult.Failure("网络错误：${e.message ?: e.javaClass.simpleName}")
            }
        }

    private fun parseModelIds(body: String): List<String> {
        return try {
            val root = JSONObject(body)
            val data = root.optJSONArray("data") ?: return emptyList()
            buildList {
                for (i in 0 until data.length()) {
                    val obj = data.optJSONObject(i) ?: continue
                    val id = obj.optString("id").trim()
                    if (id.isNotEmpty()) add(id)
                }
            }.distinct().sorted()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun extractErrorMessage(body: String): String {
        return try {
            val root = JSONObject(body)
            val err = root.optJSONObject("error")
            err?.optString("message")?.takeIf { it.isNotBlank() }
                ?: root.optString("message").takeIf { it.isNotBlank() }
                ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Heuristic split: whisper/audio/transcribe → ASR; exclude those from chat when possible.
     * If no clear chat set, fall back to all non-ASR (or all).
     */
    fun classifyModels(ids: List<String>): ModelLists {
        val asr = ids.filter { isAsrLike(it) }
        val chatPreferred = ids.filter { isChatLike(it) && !isAsrLike(it) }
        val chat = when {
            chatPreferred.isNotEmpty() -> chatPreferred
            else -> ids.filter { !isAsrLike(it) }.ifEmpty { ids }
        }
        return ModelLists(
            allIds = ids,
            chatModels = chat,
            asrModels = asr.ifEmpty {
                // Keep whisper-1 as suggestion even if not listed
                listOf("whisper-1")
            }
        )
    }

    private fun isAsrLike(id: String): Boolean {
        val n = id.lowercase()
        return n.contains("whisper") ||
            n.contains("transcrib") ||
            n.contains("speech") ||
            n.contains("asr") ||
            (n.contains("audio") && !n.contains("tts") && !n.contains("gpt-4o-audio"))
    }

    private fun isChatLike(id: String): Boolean {
        val n = id.lowercase()
        // Exclude obvious non-chat
        if (isAsrLike(n)) return false
        if (n.contains("embedding") || n.contains("moderation") ||
            n.contains("tts") || n.contains("dall-e") || n.contains("davinci-002") && n.contains("embed") ||
            n.contains("babbage") && n.contains("embed") ||
            n.contains("clip") || n.contains("realtime") && n.contains("preview") && n.contains("audio")
        ) return false
        // Prefer known chat families; otherwise accept remaining
        // Prefer known chat families; still accept most remaining IDs (gateways often only list chat)
        return n.contains("gpt") || n.contains("o1") || n.contains("o3") || n.contains("o4") ||
            n.contains("claude") || n.contains("gemini") || n.contains("llama") ||
            n.contains("qwen") || n.contains("deepseek") || n.contains("mistral") ||
            n.contains("command") || n.contains("chat") || n.contains("instruct") ||
            (!n.contains("embedding") && !n.contains("moderation") && !n.contains("tts"))
    }
}
