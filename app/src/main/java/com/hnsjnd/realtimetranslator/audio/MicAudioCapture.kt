package com.hnsjnd.realtimetranslator.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Captures external sound via [AudioRecord] (VOICE_RECOGNITION source).
 */
class MicAudioCapture(
    override val sampleRateHz: Int = 16_000
) : AudioCapture {

    override val channelCount: Int = 1

    private val running = AtomicBoolean(false)
    @Volatile private var recorder: AudioRecord? = null

    @SuppressLint("MissingPermission")
    override fun start(chunkDurationMs: Int): Flow<ByteArray> = flow {
        if (!running.compareAndSet(false, true)) {
            Log.w(TAG, "already running")
            return@flow
        }
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBuf = AudioRecord.getMinBufferSize(sampleRateHz, channelConfig, encoding)
        val bufferSize = (minBuf * 2).coerceAtLeast(sampleRateHz * 2) // ~1s worth
        val bytesPerChunk = sampleRateHz * 2 /*16-bit*/ * chunkDurationMs / 1000

        val ar = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRateHz,
            channelConfig,
            encoding,
            bufferSize
        )
        recorder = ar
        if (ar.state != AudioRecord.STATE_INITIALIZED) {
            running.set(false)
            throw IllegalStateException("AudioRecord init failed (state=${ar.state})")
        }
        ar.startRecording()
        Log.i(TAG, "mic capture started @ ${sampleRateHz}Hz, chunk=${chunkDurationMs}ms")

        val readBuf = ByteArray(bufferSize)
        val chunkBuf = ByteArrayOutput(bytesPerChunk)

        try {
            while (currentCoroutineContext().isActive && running.get()) {
                val n = ar.read(readBuf, 0, readBuf.size)
                if (n > 0) {
                    chunkBuf.write(readBuf, 0, n)
                    while (chunkBuf.size() >= bytesPerChunk) {
                        val chunk = chunkBuf.take(bytesPerChunk)
                        if (hasSignal(chunk)) {
                            emit(chunk)
                        }
                    }
                } else if (n < 0) {
                    Log.e(TAG, "AudioRecord.read error: $n")
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
                if (state == AudioRecord.STATE_INITIALIZED) {
                    try { stop() } catch (_: Exception) {}
                }
                release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "stop error", e)
        } finally {
            recorder = null
            running.set(false)
            Log.i(TAG, "mic capture stopped")
        }
    }

    /** Skip near-silent chunks to save ASR calls. */
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

    /** Simple growable byte buffer with take(). */
    private class ByteArrayOutput(initial: Int) {
        private var buf = ByteArray(initial.coerceAtLeast(4096))
        private var len = 0
        fun size() = len
        fun write(src: ByteArray, off: Int, count: Int) {
            ensure(len + count)
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
        private fun ensure(need: Int) {
            if (need <= buf.size) return
            var ns = buf.size
            while (ns < need) ns *= 2
            buf = buf.copyOf(ns)
        }
    }

    companion object {
        private const val TAG = "MicAudioCapture"
    }
}
