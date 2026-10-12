package com.hnsjnd.realtimetranslator.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hnsjnd.realtimetranslator.RealtimeTranslatorApp
import com.hnsjnd.realtimetranslator.data.AiProviderConfig
import com.hnsjnd.realtimetranslator.data.AudioSource
import com.hnsjnd.realtimetranslator.data.Language
import com.hnsjnd.realtimetranslator.data.TranscriptLine
import com.hnsjnd.realtimetranslator.service.TranslationForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val isRunning: Boolean = false,
    val statusMessage: String = "就绪",
    val sourceLang: Language = Language.AUTO,
    val targetLang: Language = Language.ZH,
    val audioSource: AudioSource = AudioSource.MICROPHONE,
    val lines: List<TranscriptLine> = emptyList(),
    val config: AiProviderConfig = AiProviderConfig(),
    val needsMediaProjection: Boolean = false
)

class TranslatorViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = (application as RealtimeTranslatorApp).settingsRepository

    private val _ui = MutableStateFlow(
        UiState(
            sourceLang = settings.sourceLang.value,
            targetLang = settings.targetLang.value,
            audioSource = settings.audioSource.value,
            config = settings.config.value
        )
    )
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            settings.sourceLang.collect { lang -> _ui.update { it.copy(sourceLang = lang) } }
        }
        viewModelScope.launch {
            settings.targetLang.collect { lang -> _ui.update { it.copy(targetLang = lang) } }
        }
        viewModelScope.launch {
            settings.audioSource.collect { src -> _ui.update { it.copy(audioSource = src) } }
        }
        viewModelScope.launch {
            settings.config.collect { cfg -> _ui.update { it.copy(config = cfg) } }
        }
        viewModelScope.launch {
            TranslationForegroundService.events.collect { line ->
                _ui.update { state ->
                    state.copy(lines = (state.lines + line).takeLast(200))
                }
            }
        }
        viewModelScope.launch {
            TranslationForegroundService.status.collect { msg ->
                _ui.update {
                    it.copy(
                        statusMessage = msg,
                        isRunning = msg.contains("翻译") || msg.contains("捕获")
                    )
                }
            }
        }
    }

    fun setSourceLang(lang: Language) = settings.setSourceLang(lang)
    fun setTargetLang(lang: Language) = settings.setTargetLang(lang)

    fun setAudioSource(source: AudioSource) {
        settings.setAudioSource(source)
    }

    fun saveConfig(config: AiProviderConfig) = settings.saveConfig(config)

    fun clearTranscript() {
        _ui.update { it.copy(lines = emptyList()) }
    }

    /**
     * @return true if caller should launch MediaProjection permission intent first.
     */
    fun requestStart(): Boolean {
        val state = _ui.value
        if (!state.config.isConfigured()) {
            _ui.update { it.copy(statusMessage = "请先在设置中配置 API Key") }
            return false
        }
        if (state.audioSource == AudioSource.INTERNAL) {
            _ui.update { it.copy(needsMediaProjection = true) }
            return true
        }
        startNow(0, null)
        return false
    }

    fun onMediaProjectionGranted(resultCode: Int, data: Intent?) {
        _ui.update { it.copy(needsMediaProjection = false) }
        startNow(resultCode, data)
    }

    fun onMediaProjectionDenied() {
        _ui.update {
            it.copy(
                needsMediaProjection = false,
                statusMessage = "内录需要屏幕录制权限，已取消"
            )
        }
    }

    private fun startNow(mpResultCode: Int, mpData: Intent?) {
        val state = _ui.value
        TranslationForegroundService.start(
            getApplication(),
            state.audioSource,
            state.sourceLang.code,
            state.targetLang.code,
            mpResultCode,
            mpData
        )
        _ui.update { it.copy(isRunning = true, statusMessage = "启动中…") }
    }

    fun stop() {
        TranslationForegroundService.stop(getApplication())
        _ui.update { it.copy(isRunning = false, statusMessage = "已停止") }
    }

    fun swapLanguages() {
        val s = _ui.value
        if (s.sourceLang == Language.AUTO) return
        settings.setSourceLang(s.targetLang)
        settings.setTargetLang(s.sourceLang)
    }
}
