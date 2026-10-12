package com.hnsjnd.realtimetranslator.audio

import kotlinx.coroutines.flow.Flow

/**
 * Common interface for microphone and internal (AudioPlaybackCapture) sources.
 * Emits PCM 16-bit mono little-endian chunks at [sampleRateHz].
 */
interface AudioCapture {
    val sampleRateHz: Int
    val channelCount: Int

    /** Start capturing; returns a cold Flow of PCM byte arrays (each ~chunkDurationMs). */
    fun start(chunkDurationMs: Int = 2500): Flow<ByteArray>

    fun stop()
}
