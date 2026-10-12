package com.hnsjnd.realtimetranslator.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.hnsjnd.realtimetranslator.MainActivity
import com.hnsjnd.realtimetranslator.R
import com.hnsjnd.realtimetranslator.RealtimeTranslatorApp
import com.hnsjnd.realtimetranslator.asr.OpenAiWhisperAsr
import com.hnsjnd.realtimetranslator.audio.InternalAudioCapture
import com.hnsjnd.realtimetranslator.audio.MicAudioCapture
import com.hnsjnd.realtimetranslator.data.AudioSource
import com.hnsjnd.realtimetranslator.data.TranscriptLine
import com.hnsjnd.realtimetranslator.translate.OpenAiChatTranslator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Foreground service that owns the audio capture + ASR + translate pipeline
 * so translation continues while the app is in the background.
 */
class TranslationForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pipeline: TranslationPipeline? = null
    private var mediaProjection: MediaProjection? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val source = intent.getStringExtra(EXTRA_AUDIO_SOURCE) ?: AudioSource.MICROPHONE.name
                val sourceLang = intent.getStringExtra(EXTRA_SOURCE_LANG) ?: "auto"
                val targetLang = intent.getStringExtra(EXTRA_TARGET_LANG) ?: "zh"
                val resultCode = intent.getIntExtra(EXTRA_MP_RESULT_CODE, 0)
                val mpData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_MP_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_MP_DATA)
                }
                startForeground(NOTIFICATION_ID, buildNotification())
                beginPipeline(AudioSource.valueOf(source), sourceLang, targetLang, resultCode, mpData)
            }
            ACTION_STOP -> {
                stopPipeline()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun beginPipeline(
        audioSource: AudioSource,
        sourceLang: String,
        targetLang: String,
        mpResultCode: Int,
        mpData: Intent?
    ) {
        stopPipeline()
        val app = application as RealtimeTranslatorApp
        val settings = app.settingsRepository
        val asr = OpenAiWhisperAsr(configProvider = { settings.loadConfig() })
        val translator = OpenAiChatTranslator(configProvider = { settings.loadConfig() })
        val pipe = TranslationPipeline(scope, asr, translator)
        pipeline = pipe

        scope.launch {
            pipe.lines.collect { line ->
                _events.emit(line)
            }
        }
        scope.launch {
            pipe.status.collect { msg ->
                _status.emit(msg)
            }
        }

        try {
            val capture = when (audioSource) {
                AudioSource.MICROPHONE -> MicAudioCapture()
                AudioSource.INTERNAL -> {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        throw IllegalStateException("内录需要 Android 10+")
                    }
                    if (mpData == null) {
                        throw IllegalStateException("缺少 MediaProjection 授权，请重新授予屏幕录制权限")
                    }
                    val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    val mp = mpm.getMediaProjection(mpResultCode, mpData)
                        ?: throw IllegalStateException("无法创建 MediaProjection")
                    mediaProjection = mp
                    mp.registerCallback(object : MediaProjection.Callback() {
                        override fun onStop() {
                            Log.w(TAG, "MediaProjection stopped by system")
                            stopPipeline()
                            stopSelf()
                        }
                    }, null)
                    InternalAudioCapture(mp)
                }
            }
            pipe.start(capture, sourceLang, targetLang)
            _status.tryEmit("翻译中…")
        } catch (e: Exception) {
            Log.e(TAG, "failed to start pipeline", e)
            _status.tryEmit("启动失败: ${e.message}")
            _events.tryEmit(
                TranscriptLine(
                    id = "err",
                    original = "",
                    translated = e.message ?: "启动失败",
                    sourceLang = sourceLang,
                    targetLang = targetLang,
                    timestampMs = System.currentTimeMillis(),
                    isError = true
                )
            )
            stopSelf()
        }
    }

    private fun stopPipeline() {
        pipeline?.stop()
        pipeline = null
        try {
            mediaProjection?.stop()
        } catch (_: Exception) {
        }
        mediaProjection = null
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, TranslationForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, RealtimeTranslatorApp.CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(open)
            .addAction(0, "停止", stop)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onDestroy() {
        stopPipeline()
        scope.cancel()
        instance = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "TranslationFgService"
        const val ACTION_START = "com.hnsjnd.realtimetranslator.START"
        const val ACTION_STOP = "com.hnsjnd.realtimetranslator.STOP"
        const val EXTRA_AUDIO_SOURCE = "audio_source"
        const val EXTRA_SOURCE_LANG = "source_lang"
        const val EXTRA_TARGET_LANG = "target_lang"
        const val EXTRA_MP_RESULT_CODE = "mp_result_code"
        const val EXTRA_MP_DATA = "mp_data"
        private const val NOTIFICATION_ID = 1001

        @Volatile
        private var instance: TranslationForegroundService? = null

        private val _events = MutableSharedFlow<TranscriptLine>(extraBufferCapacity = 64)
        val events: SharedFlow<TranscriptLine> = _events.asSharedFlow()

        private val _status = MutableSharedFlow<String>(extraBufferCapacity = 8)
        val status: SharedFlow<String> = _status.asSharedFlow()

        fun isRunning(): Boolean = instance?.pipeline?.isRunning() == true

        fun start(
            context: Context,
            audioSource: AudioSource,
            sourceLang: String,
            targetLang: String,
            mpResultCode: Int = 0,
            mpData: Intent? = null
        ) {
            val intent = Intent(context, TranslationForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_AUDIO_SOURCE, audioSource.name)
                putExtra(EXTRA_SOURCE_LANG, sourceLang)
                putExtra(EXTRA_TARGET_LANG, targetLang)
                putExtra(EXTRA_MP_RESULT_CODE, mpResultCode)
                if (mpData != null) putExtra(EXTRA_MP_DATA, mpData)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TranslationForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
