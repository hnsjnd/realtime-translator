package com.hnsjnd.realtimetranslator.audio

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavEncoder {
    /**
     * Wrap raw PCM 16-bit LE mono samples in a minimal WAV container
     * suitable for OpenAI Whisper multipart upload.
     */
    fun pcmToWav(pcm: ByteArray, sampleRate: Int, channels: Int = 1): ByteArray {
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()
        val dataSize = pcm.size
        val out = ByteArrayOutputStream(44 + dataSize)
        // RIFF header
        out.write("RIFF".toByteArray())
        out.write(intLE(36 + dataSize))
        out.write("WAVE".toByteArray())
        // fmt chunk
        out.write("fmt ".toByteArray())
        out.write(intLE(16)) // PCM chunk size
        out.write(shortLE(1)) // audio format = PCM
        out.write(shortLE(channels.toShort()))
        out.write(intLE(sampleRate))
        out.write(intLE(byteRate))
        out.write(shortLE(blockAlign))
        out.write(shortLE(bitsPerSample.toShort()))
        // data chunk
        out.write("data".toByteArray())
        out.write(intLE(dataSize))
        out.write(pcm)
        return out.toByteArray()
    }

    private fun intLE(v: Int): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array()

    private fun shortLE(v: Short): ByteArray =
        ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(v).array()
}
