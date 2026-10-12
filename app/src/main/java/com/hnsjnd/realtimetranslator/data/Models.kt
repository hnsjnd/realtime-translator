package com.hnsjnd.realtimetranslator.data

enum class AudioSource {
    MICROPHONE,
    INTERNAL
}

enum class Language(val code: String, val displayName: String) {
    AUTO("auto", "自动检测"),
    ZH("zh", "中文"),
    EN("en", "English"),
    JA("ja", "日本語"),
    KO("ko", "한국어"),
    FR("fr", "Français"),
    DE("de", "Deutsch"),
    ES("es", "Español"),
    RU("ru", "Русский");

    companion object {
        fun fromCode(code: String): Language =
            entries.find { it.code == code } ?: AUTO
    }
}

data class TranscriptLine(
    val id: String,
    val original: String,
    val translated: String,
    val sourceLang: String,
    val targetLang: String,
    val timestampMs: Long,
    val isError: Boolean = false
)

data class AiProviderConfig(
    val apiBaseUrl: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val chatModel: String = "gpt-4o-mini",
    val asrEndpoint: String = "", // empty = {baseUrl}/audio/transcriptions
    val asrModel: String = "whisper-1"
) {
    fun resolvedAsrUrl(): String {
        val custom = asrEndpoint.trim()
        if (custom.isNotEmpty()) return custom.trimEnd('/')
        return apiBaseUrl.trimEnd('/') + "/audio/transcriptions"
    }

    fun resolvedChatUrl(): String = apiBaseUrl.trimEnd('/') + "/chat/completions"

    fun isConfigured(): Boolean = apiKey.isNotBlank() && apiBaseUrl.isNotBlank()
}
