package com.hnsjnd.realtimetranslator.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.hnsjnd.realtimetranslator.data.AiProviderConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    config: AiProviderConfig,
    onSave: (AiProviderConfig) -> Unit,
    onBack: () -> Unit
) {
    var baseUrl by remember(config) { mutableStateOf(config.apiBaseUrl) }
    var apiKey by remember(config) { mutableStateOf(config.apiKey) }
    var chatModel by remember(config) { mutableStateOf(config.chatModel) }
    var asrEndpoint by remember(config) { mutableStateOf(config.asrEndpoint) }
    var asrModel by remember(config) { mutableStateOf(config.asrModel) }
    var showKey by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 模型设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "配置任意 OpenAI 兼容 API（官方 OpenAI、Azure、本地 Ollama/vLLM、第三方中转等）。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("API Base URL") },
                placeholder = { Text("https://api.openai.com/v1") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                placeholder = { Text("sk-…") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None
                else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Text(if (showKey) "隐" else "显", style = MaterialTheme.typography.labelMedium)
                    }
                }
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = chatModel,
                onValueChange = { chatModel = it },
                label = { Text("翻译模型 (Chat)") },
                placeholder = { Text("gpt-4o-mini") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = asrModel,
                onValueChange = { asrModel = it },
                label = { Text("语音识别模型 (ASR)") },
                placeholder = { Text("whisper-1") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = asrEndpoint,
                onValueChange = { asrEndpoint = it },
                label = { Text("ASR 端点（可选）") },
                placeholder = { Text("留空则使用 {Base URL}/audio/transcriptions") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "密钥使用 EncryptedSharedPreferences 加密存储在本机，不会上传到本项目服务器。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    onSave(
                        AiProviderConfig(
                            apiBaseUrl = baseUrl.trim(),
                            apiKey = apiKey.trim(),
                            chatModel = chatModel.trim(),
                            asrEndpoint = asrEndpoint.trim(),
                            asrModel = asrModel.trim()
                        )
                    )
                    scope.launch { snackbar.showSnackbar("已保存") }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("保存配置")
            }

            Spacer(Modifier.height(24.dp))
            Text("快速示例", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "• OpenAI：Base=https://api.openai.com/v1 ，模型 gpt-4o-mini / whisper-1\n" +
                    "• 本地 Ollama（需兼容层）：Base=http://192.168.x.x:11434/v1\n" +
                    "• 其他中转：填入提供商给出的 Base URL 与 Key 即可",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }
    }
}
