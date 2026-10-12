package com.hnsjnd.realtimetranslator

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.*
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.test.core.app.ApplicationProvider
import com.hnsjnd.realtimetranslator.data.AiProviderConfig
import com.hnsjnd.realtimetranslator.ui.SettingsScreen
import com.hnsjnd.realtimetranslator.ui.theme.RealtimeTranslatorTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsScreenCrashTest {
    @get:Rule val rule = createComposeRule()

    private val ids = listOf(
        "Qwen/Qwen3-8B", "deepseek-ai/DeepSeek-V3", "deepseek-ai/DeepSeek-R1", "deepseek-ai/DeepSeek-OCR",
        "FunAudioLLM/SenseVoiceSmall", "FunAudioLLM/CosyVoice2-0.5B", "Wan-AI/Wan2.2-I2V-A14B",
        "Wan-AI/Wan2.2-T2V-A14B", "XingChenAGI/Xing4.0-29B", "THUDM/GLM-4-9B-0414", "TeleAI/TeleSpeechASR"
    )

    private fun open(seed: Boolean) {
        val app = ApplicationProvider.getApplicationContext<RealtimeTranslatorApp>()
        val base = "https://api.siliconflow.cn/v1"
        if (seed) app.settingsRepository.saveModelCache(base, ids)
        rule.setContent {
            RealtimeTranslatorTheme {
                SettingsScreen(
                    config = AiProviderConfig(apiBaseUrl = base, apiKey = "sk-x", chatModel = "Qwen/Qwen3-8B", asrModel = "whisper-1"),
                    onSave = {}, onBack = {}
                )
            }
        }
    }

    @Test fun opensWithoutCache() { open(false); rule.waitForIdle() }

    @Test fun opensWithCacheExpandAndFilter() {
        open(true)
        rule.onAllNodesWithContentDescription("展开模型列表").onFirst().performScrollTo().performClick()
        rule.waitForIdle()
        rule.onAllNodes(hasSetTextAction().and(hasText("Qwen/Qwen3-8B"))).onFirst().performTextReplacement("fun")
        rule.waitForIdle()
        rule.onNodeWithText("匹配", substring = true).assertExists()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ArrowExpandTest {
    @get:Rule val rule = createComposeRule()
    @Test fun arrowOpensList() {
        val app = ApplicationProvider.getApplicationContext<RealtimeTranslatorApp>()
        val base = "https://api.siliconflow.cn/v1"
        app.settingsRepository.saveModelCache(base, listOf("Qwen/Qwen3-8B", "FunAudioLLM/SenseVoiceSmall"))
        rule.setContent { RealtimeTranslatorTheme { SettingsScreen(
            config = AiProviderConfig(apiBaseUrl = base, apiKey = "k", chatModel = "Qwen/Qwen3-8B", asrModel = "x"),
            onSave = {}, onBack = {}) } }
        rule.onAllNodesWithContentDescription("展开模型列表")[1].performScrollTo().performClick()
        rule.waitForIdle()
        org.robolectric.shadows.ShadowLog.stream = System.out
        rule.onRoot().printToLog("TREE")
        rule.onNodeWithText("共 1 个模型").assertExists()
    }
}
