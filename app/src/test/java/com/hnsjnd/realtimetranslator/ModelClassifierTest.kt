package com.hnsjnd.realtimetranslator

import androidx.test.core.app.ApplicationProvider
import com.hnsjnd.realtimetranslator.api.OpenAiModelsClient
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ModelClassifierTest {
    @Test fun classify() {
        val r = OpenAiModelsClient.classifyModels(listOf(
            "FunAudioLLM/CosyVoice2-0.5B", "FunAudioLLM/SenseVoiceSmall", "Wan-AI/Wan2.2-I2V-A14B",
            "Qwen/Qwen3-8B", "deepseek-ai/DeepSeek-OCR", "deepseek-ai/DeepSeek-V3", "BAAI/bge-m3", "whisper-1", "Qwen/Qwen3-8B"
        ))
        assertEquals(listOf("FunAudioLLM/SenseVoiceSmall", "whisper-1"), r.asrModels)
        assertEquals(listOf("Qwen/Qwen3-8B", "deepseek-ai/DeepSeek-V3"), r.chatModels)
        assertEquals(8, r.allIds.size)
    }

    @Test fun corruptCacheIsIgnored() {
        val app = ApplicationProvider.getApplicationContext<RealtimeTranslatorApp>()
        app.getSharedPreferences("ui_prefs", 0).edit()
            .putString("model_cache_url", "https://x/v1").putString("model_cache_ids", "{not json").commit()
        assertTrue(app.settingsRepository.loadModelCache("https://x/v1").isEmpty())
        app.getSharedPreferences("ui_prefs", 0).edit().putString("model_cache_ids", null).commit()
        assertTrue(app.settingsRepository.loadModelCache("https://x/v1/").isEmpty())
    }
}
