package com.hnsjnd.realtimetranslator.asr

import android.util.Log
import com.hnsjnd.realtimetranslator.data.AiProviderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * OpenAI-compatible Whisper multipart client.
 * POST {asrEndpoint or baseUrl/audio/transcriptions}
 *   file=@audio.wav  model=whisper-1  [language=xx]  response_format=json
 */
class OpenAiWhisperAsr(
    private val configProvider: () -> AiProviderConfig,
    private val client: OkHttpClient = defaultClient()
) : AsrService {

    override suspend fun transcribe(wavBytes: ByteArray, languageHint: String?): String =
        withContext(Dispatchers.IO) {
            val cfg = configProvider()
            if (!cfg.isConfigured()) {
                throw IllegalStateException("请先在设置中配置 API Key 与 Base URL")
            }
            val url = cfg.resolvedAsrUrl()
            val fileBody = wavBytes.toRequestBody("audio/wav".toMediaType())
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "chunk.wav", fileBody)
                .addFormDataPart("model", cfg.asrModel.ifBlank { "whisper-1" })
                .addFormDataPart("response_format", "json")
                .apply {
                    val lang = languageHint?.takeIf { it.isNotBlank() && it != "auto" }
                    if (lang != null) addFormDataPart("language", lang)
                }
                .build()

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer ${cfg.apiKey}")
                .post(multipart)
                .build()

            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    Log.e(TAG, "ASR HTTP ${resp.code}: $body")
                    throw RuntimeException("ASR 失败 (${resp.code}): ${body.take(200)}")
                }
                val text = try {
                    JSONObject(body).optString("text", "").trim()
                } catch (_: Exception) {
                    body.trim()
                }
                Log.d(TAG, "ASR ok: ${text.take(80)}")
                text
            }
        }

    companion object {
        private const val TAG = "OpenAiWhisperAsr"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}
