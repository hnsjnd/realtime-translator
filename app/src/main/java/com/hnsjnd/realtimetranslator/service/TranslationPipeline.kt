package com.hnsjnd.realtimetranslator.service

import android.util.Log
import com.hnsjnd.realtimetranslator.asr.AsrService
import com.hnsjnd.realtimetranslator.audio.AudioCapture
import com.hnsjnd.realtimetranslator.audio.WavEncoder
import com.hnsjnd.realtimetranslator.data.TranscriptLine
import com.hnsjnd.realtimetranslator.translate.TranslationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Orchestrates: AudioCapture → WAV encode → ASR → Translation → transcript events.
 */
class TranslationPipeline(
    private val scope: CoroutineScope,
    private val asr: AsrService,
    private val translator: TranslationService,
    private val sampleRateHz: Int = 16_000
) {
    private val _lines = MutableSharedFlow<TranscriptLine>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val lines: SharedFlow<TranscriptLine> = _lines.asSharedFlow()

    private val _status = MutableSharedFlow<String>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val status: SharedFlow<String> = _status.asSharedFlow()

    private val running = AtomicBoolean(false)
    private var job: Job? = null
    private var capture: AudioCapture? = null

    fun start(
        audioCapture: AudioCapture,
        sourceLang: String,
        targetLang: String,
        chunkDurationMs: Int = 2500
    ) {
        if (!running.compareAndSet(false, true)) return
        capture = audioCapture
        job = scope.launch {
            _status.emit("正在捕获音频…")
            try {
                audioCapture.start(chunkDurationMs).collect { pcm ->
                    try {
                        val wav = WavEncoder.pcmToWav(pcm, sampleRateHz)
                        val langHint = sourceLang.takeIf { it != "auto" }
                        val original = asr.transcribe(wav, langHint)
                        if (original.isBlank()) return@collect

                        // Skip if source == target and no auto (still show original)
                        val translated = if (sourceLang != "auto" && sourceLang == targetLang) {
                            original
                        } else {
                            translator.translate(original, sourceLang, targetLang)
                        }

                        _lines.emit(
                            TranscriptLine(
                                id = UUID.randomUUID().toString(),
                                original = original,
                                translated = translated,
                                sourceLang = sourceLang,
                                targetLang = targetLang,
                                timestampMs = System.currentTimeMillis()
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "chunk pipeline error", e)
                        _lines.emit(
                            TranscriptLine(
                                id = UUID.randomUUID().toString(),
                                original = "",
                                translated = e.message ?: "处理失败",
                                sourceLang = sourceLang,
                                targetLang = targetLang,
                                timestampMs = System.currentTimeMillis(),
                                isError = true
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "capture flow error", e)
                _status.emit("错误: ${e.message}")
            } finally {
                running.set(false)
                _status.emit("已停止")
            }
        }
    }

    fun stop() {
        running.set(false)
        capture?.stop()
        capture = null
        job?.cancel()
        job = null
    }

    fun isRunning(): Boolean = running.get()

    companion object {
        private const val TAG = "TranslationPipeline"
    }
}
