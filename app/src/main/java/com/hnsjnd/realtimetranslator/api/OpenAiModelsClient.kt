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

/** OpenAI-compatible GET {baseUrl}/models client used by Settings「测试连接」. */
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
            if (base.isBlank()) return@withContext ModelsFetchResult.Failure("请填写 API Base URL")
            if (apiKey.isBlank()) return@withContext ModelsFetchResult.Failure("请填写 API Key")
            val request = Request.Builder()
                .url("$base/models")
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
                            "连接失败 HTTP ${resp.code}" + if (detail.isNotBlank()) "：$detail" else ""
                        )
                    }
                    val ids = parseModelIds(body)
                    if (ids.isEmpty()) {
                        return@withContext ModelsFetchResult.Failure("连接成功，但未解析到模型列表（响应可能非 OpenAI 格式）")
                    }
                    ModelsFetchResult.Success(classifyModels(ids))
                }
            } catch (e: Exception) {
                ModelsFetchResult.Failure("网络错误：${e.message ?: e.javaClass.simpleName}")
            }
        }

    private fun parseModelIds(body: String): List<String> = try {
        val data = JSONObject(body).optJSONArray("data")
        if (data == null) emptyList() else buildList {
            for (i in 0 until data.length()) {
                val id = data.optJSONObject(i)?.optString("id")?.trim().orEmpty()
                if (id.isNotEmpty()) add(id)
            }
        }.distinct()
    } catch (_: Exception) {
        emptyList()
    }

    private fun extractErrorMessage(body: String): String = try {
        val root = JSONObject(body)
        root.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
            ?: root.optString("message").takeIf { it.isNotBlank() } ?: ""
    } catch (_: Exception) {
        ""
    }

    // ---- Classification ----

    private val ASR_KEYS = listOf(
        "whisper", "sensevoice", "paraformer", "telespeech", "transcrib",
        "speech-to-text", "speech2text", "stt", "asr", "funasr"
    )

    /** Not usable for chat translation and not ASR either (TTS, image, video, embeddings…). */
    private val NON_CHAT_KEYS = listOf(
        "tts", "cosyvoice", "fish-speech", "fishaudio", "gpt-sovits", "indextts", "moss-ttsd", "speech",
        "wan-ai", "wan2", "i2v", "t2v", "t2i", "i2i", "flux", "stable-diffusion", "sdxl", "sd3",
        "kolors", "hunyuanvideo", "ltx-video", "video", "image", "qwen-image", "dall-e", "imagen",
        "ocr", "embedding", "embed", "bge-", "rerank", "moderation", "clip", "audio"
    )

    private val CHAT_PRIORITY = listOf(
        "qwen3", "qwen2.5", "qwen", "deepseek-v3", "deepseek-chat", "deepseek",
        "glm-4", "glm", "kimi", "moonshot", "gpt-4o", "gpt-4.1", "gpt-5", "gpt",
        "claude", "gemini", "minimax", "hunyuan", "doubao", "ernie", "yi-", "internlm",
        "llama", "mistral"
    )

    fun isAsrLike(id: String): Boolean {
        val n = id.lowercase()
        if (n.contains("cosyvoice") || n.contains("tts")) return false
        return ASR_KEYS.any { k ->
            if (k == "stt" || k == "asr") Regex("(^|[^a-z])$k([^a-z]|$)").containsMatchIn(n) else n.contains(k)
        }
    }

    fun isChatLike(id: String): Boolean {
        val n = id.lowercase()
        if (isAsrLike(n)) return false
        return NON_CHAT_KEYS.none { n.contains(it) }
    }

    private fun chatRank(id: String): Int {
        val n = id.lowercase()
        val idx = CHAT_PRIORITY.indexOfFirst { n.contains(it) }
        return if (idx < 0) CHAT_PRIORITY.size else idx
    }

    fun sortChat(ids: List<String>): List<String> =
        ids.sortedWith(compareBy<String>({ chatRank(it) }, { it.lowercase() }))

    fun sortAsr(ids: List<String>): List<String> =
        ids.sortedWith(compareBy<String>({ if (it.lowercase().contains("sensevoice") || it.lowercase().contains("whisper")) 0 else 1 }, { it.lowercase() }))

    fun classifyModels(ids: List<String>): ModelLists {
        val all = ids.distinct().sortedBy { it.lowercase() }
        return ModelLists(
            allIds = all,
            chatModels = sortChat(all.filter { isChatLike(it) }),
            asrModels = sortAsr(all.filter { isAsrLike(it) })
        )
    }
}
