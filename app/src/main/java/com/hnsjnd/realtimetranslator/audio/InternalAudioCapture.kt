package com.hnsjnd.realtimetranslator.audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Captures system / app playback audio via [AudioPlaybackCaptureConfiguration]
 * (Android 10 / API 29+). Requires a valid [MediaProjection] obtained from
 * MediaProjectionManager.createScreenCaptureIntent() — the user must grant
 * the screen-capture permission dialog.
 *
 * Note: Some OEMs / apps opt out of capture; media marked USAGE_UNKNOWN may
 * still be captured depending on the matching rules below.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class InternalAudioCapture(
    private val mediaProjection: MediaProjection,
    override val sampleRateHz: Int = 16_000
) : AudioCapture {

    override val channelCount: Int = 1

    private val running = AtomicBoolean(false)
    @Volatile private var recorder: AudioRecord? = null

    @SuppressLint("MissingPermission")
    override fun start(chunkDurationMs: Int): Flow<ByteArray> = flow {
        if (!running.compareAndSet(false, true)) return@flow

        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRateHz)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()

        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()

        val minBuf = AudioRecord.getMinBufferSize(
            sampleRateHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = (minBuf * 2).coerceAtLeast(sampleRateHz * 2)
        val bytesPerChunk = sampleRateHz * 2 * chunkDurationMs / 1000

        val ar = AudioRecord.Builder()
            .setAudioFormat(format)
            .setBufferSizeInBytes(bufferSize)
            .setAudioPlaybackCaptureConfig(config)
            .build()

        recorder = ar
        if (ar.state != AudioRecord.STATE_INITIALIZED) {
            running.set(false)
            throw IllegalStateException("Internal AudioRecord init failed (state=${ar.state})")
        }
        ar.startRecording()
        Log.i(TAG, "internal audio capture started @ ${sampleRateHz}Hz")

        val readBuf = ByteArray(bufferSize)
        val chunkBuf = GrowableBytes(bytesPerChunk)

        try {
            while (currentCoroutineContext().isActive && running.get()) {
                val n = ar.read(readBuf, 0, readBuf.size)
                if (n > 0) {
                    chunkBuf.write(readBuf, 0, n)
                    while (chunkBuf.size() >= bytesPerChunk) {
                        val chunk = chunkBuf.take(bytesPerChunk)
                        if (hasSignal(chunk)) emit(chunk)
                    }
                } else if (n < 0) {
                    Log.e(TAG, "read error: $n")
                    break
                }
            }
        } finally {
            stopInternal()
        }
    }.flowOn(Dispatchers.IO)

    override fun stop() {
        running.set(false)
        stopInternal()
    }

    private fun stopInternal() {
        try {
            recorder?.run {
                try { stop() } catch (_: Exception) {}
                release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "stop error", e)
        } finally {
            recorder = null
            running.set(false)
            Log.i(TAG, "internal audio capture stopped")
        }
    }

    private fun hasSignal(pcm: ByteArray, threshold: Int = 180): Boolean {
        var i = 0
        while (i + 1 < pcm.size) {
            val sample = (pcm[i].toInt() and 0xff) or (pcm[i + 1].toInt() shl 8)
            val s = if (sample > 32767) sample - 65536 else sample
            if (kotlin.math.abs(s) > threshold) return true
            i += 2
        }
        return false
    }

    private class GrowableBytes(initial: Int) {
        private var buf = ByteArray(initial.coerceAtLeast(4096))
        private var len = 0
        fun size() = len
        fun write(src: ByteArray, off: Int, count: Int) {
            if (len + count > buf.size) {
                var ns = buf.size
                while (ns < len + count) ns *= 2
                buf = buf.copyOf(ns)
            }
            System.arraycopy(src, off, buf, len, count)
            len += count
        }
        fun take(n: Int): ByteArray {
            val out = buf.copyOfRange(0, n)
            val remain = len - n
            if (remain > 0) System.arraycopy(buf, n, buf, 0, remain)
            len = remain
            return out
        }
    }

    companion object {
        private const val TAG = "InternalAudioCapture"

        fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }
}
