package com.hnsjnd.realtimetranslator.asr

/**
 * Speech-to-text interface. Implementations talk to OpenAI-compatible
 * /audio/transcriptions (Whisper-style) or any HTTP ASR endpoint.
 */
interface AsrService {
    /**
     * @param wavBytes WAV-wrapped PCM audio
     * @param languageHint ISO-639-1 code or "auto"/null for detection
     * @return transcribed text (may be empty for silence / no speech)
     */
    suspend fun transcribe(wavBytes: ByteArray, languageHint: String? = null): String
}
