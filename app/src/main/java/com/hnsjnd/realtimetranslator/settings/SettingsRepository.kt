package com.hnsjnd.realtimetranslator.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.hnsjnd.realtimetranslator.data.AiProviderConfig
import com.hnsjnd.realtimetranslator.data.AudioSource
import com.hnsjnd.realtimetranslator.data.Language
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stores AI provider secrets in EncryptedSharedPreferences.
 * Non-sensitive UI prefs (languages, audio source) live in a regular SharedPreferences file.
 */
class SettingsRepository(context: Context) {

    private val appContext = context.applicationContext

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            "encrypted_ai_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val uiPrefs: SharedPreferences =
        appContext.getSharedPreferences("ui_prefs", Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<AiProviderConfig> = _config.asStateFlow()

    private val _sourceLang = MutableStateFlow(
        Language.fromCode(uiPrefs.getString(KEY_SOURCE_LANG, Language.AUTO.code)!!)
    )
    val sourceLang: StateFlow<Language> = _sourceLang.asStateFlow()

    private val _targetLang = MutableStateFlow(
        Language.fromCode(uiPrefs.getString(KEY_TARGET_LANG, Language.ZH.code)!!)
    )
    val targetLang: StateFlow<Language> = _targetLang.asStateFlow()

    private val _audioSource = MutableStateFlow(
        try {
            AudioSource.valueOf(uiPrefs.getString(KEY_AUDIO_SOURCE, AudioSource.MICROPHONE.name)!!)
        } catch (_: Exception) {
            AudioSource.MICROPHONE
        }
    )
    val audioSource: StateFlow<AudioSource> = _audioSource.asStateFlow()

    fun loadConfig(): AiProviderConfig = AiProviderConfig(
        apiBaseUrl = encryptedPrefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL,
        apiKey = encryptedPrefs.getString(KEY_API_KEY, "") ?: "",
        chatModel = encryptedPrefs.getString(KEY_CHAT_MODEL, DEFAULT_CHAT_MODEL) ?: DEFAULT_CHAT_MODEL,
        asrEndpoint = encryptedPrefs.getString(KEY_ASR_ENDPOINT, "") ?: "",
        asrModel = encryptedPrefs.getString(KEY_ASR_MODEL, DEFAULT_ASR_MODEL) ?: DEFAULT_ASR_MODEL
    )

    fun saveConfig(config: AiProviderConfig) {
        encryptedPrefs.edit()
            .putString(KEY_BASE_URL, config.apiBaseUrl.trim())
            .putString(KEY_API_KEY, config.apiKey.trim())
            .putString(KEY_CHAT_MODEL, config.chatModel.trim())
            .putString(KEY_ASR_ENDPOINT, config.asrEndpoint.trim())
            .putString(KEY_ASR_MODEL, config.asrModel.trim())
            .apply()
        _config.value = config.copy(
            apiBaseUrl = config.apiBaseUrl.trim(),
            apiKey = config.apiKey.trim(),
            chatModel = config.chatModel.trim(),
            asrEndpoint = config.asrEndpoint.trim(),
            asrModel = config.asrModel.trim()
        )
    }

    fun setSourceLang(lang: Language) {
        uiPrefs.edit().putString(KEY_SOURCE_LANG, lang.code).apply()
        _sourceLang.value = lang
    }

    fun setTargetLang(lang: Language) {
        uiPrefs.edit().putString(KEY_TARGET_LANG, lang.code).apply()
        _targetLang.value = lang
    }

    fun setAudioSource(source: AudioSource) {
        uiPrefs.edit().putString(KEY_AUDIO_SOURCE, source.name).apply()
        _audioSource.value = source
    }

    companion object {
        private const val KEY_BASE_URL = "api_base_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_CHAT_MODEL = "chat_model"
        private const val KEY_ASR_ENDPOINT = "asr_endpoint"
        private const val KEY_ASR_MODEL = "asr_model"
        private const val KEY_SOURCE_LANG = "source_lang"
        private const val KEY_TARGET_LANG = "target_lang"
        private const val KEY_AUDIO_SOURCE = "audio_source"

        const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
        const val DEFAULT_CHAT_MODEL = "gpt-4o-mini"
        const val DEFAULT_ASR_MODEL = "whisper-1"
    }
}
