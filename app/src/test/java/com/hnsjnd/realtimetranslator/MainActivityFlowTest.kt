package com.hnsjnd.realtimetranslator

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.hnsjnd.realtimetranslator.data.AiProviderConfig
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun fullSettingsFlowIncludingRecreate() {
        val app = ApplicationProvider.getApplicationContext<RealtimeTranslatorApp>()
        val base = "https://api.siliconflow.cn/v1"
        app.settingsRepository.saveConfig(AiProviderConfig(apiBaseUrl = base, apiKey = "sk-x", chatModel = "Qwen/Qwen3-8B", asrModel = "FunAudioLLM/CosyVoice2-0.5B"))
        app.settingsRepository.saveModelCache(base, (1..150).map { "Org$it/Model-$it" } + listOf("FunAudioLLM/SenseVoiceSmall", "FunAudioLLM/CosyVoice2-0.5B", "Qwen/Qwen3-8B"))
        rule.onNodeWithContentDescription("设置").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("显示全部模型", substring = true).assertExists()
        val arrows = rule.onAllNodesWithContentDescription("展开模型列表")
        arrows[0].performScrollTo().performClick(); rule.waitForIdle()
        rule.onAllNodes(hasSetTextAction().and(hasText("Qwen/Qwen3-8B"))).onFirst().performTextReplacement("fun")
        rule.waitForIdle()
        rule.onAllNodes(hasSetTextAction().and(hasText("fun"))).onFirst().performImeAction()
        rule.waitForIdle()
        rule.onAllNodesWithContentDescription("展开模型列表")[1].performScrollTo().performClick(); rule.waitForIdle()
        rule.onAllNodesWithText("SenseVoiceSmall", substring = true).onLast().performScrollTo().performClick(); rule.waitForIdle()
        rule.onNode(isToggleable()).performScrollTo().performClick(); rule.waitForIdle()
        rule.onAllNodesWithContentDescription("展开模型列表")[0].performScrollTo().performClick(); rule.waitForIdle()
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        rule.onNodeWithText("保存配置").performScrollTo().performClick()
        rule.waitForIdle()
    }
}
