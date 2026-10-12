package com.hnsjnd.realtimetranslator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import com.hnsjnd.realtimetranslator.RealtimeTranslatorApp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.hnsjnd.realtimetranslator.api.ModelsFetchResult
import com.hnsjnd.realtimetranslator.api.OpenAiModelsClient
import com.hnsjnd.realtimetranslator.data.AiProviderConfig
import com.hnsjnd.realtimetranslator.ui.theme.Navy800
import com.hnsjnd.realtimetranslator.ui.theme.Teal400
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    config: AiProviderConfig,
    onSave: (AiProviderConfig) -> Unit,
    onBack: () -> Unit,
    onOpenCrashLog: () -> Unit = {}
) {
    val context = LocalContext.current
    val settingsRepo = remember { (context.applicationContext as RealtimeTranslatorApp).settingsRepository }

    // Keyed on Unit (not config) so a save never overwrites what the user is editing/selected
    var baseUrl by rememberSaveable { mutableStateOf(config.apiBaseUrl) }
    var apiKey by rememberSaveable { mutableStateOf(config.apiKey) }
    var chatModel by rememberSaveable { mutableStateOf(config.chatModel) }
    var asrEndpoint by rememberSaveable { mutableStateOf(config.asrEndpoint) }
    var asrModel by rememberSaveable { mutableStateOf(config.asrModel) }
    var showKey by remember { mutableStateOf(false) }
    var showAllModels by rememberSaveable { mutableStateOf(false) }

    var allModelIds by remember { mutableStateOf(settingsRepo.loadModelCache(config.apiBaseUrl)) }
    val classified = remember(allModelIds) { OpenAiModelsClient.classifyModels(allModelIds) }
    val allSorted = remember(allModelIds) { OpenAiModelsClient.sortChat(allModelIds) }
    val chatOptions = if (showAllModels) allSorted else classified.chatModels
    val asrOptions = if (showAllModels) allSorted else classified.asrModels
    var testing by remember { mutableStateOf(false) }
    var testError by remember { mutableStateOf<String?>(null) }
    var testOk by remember { mutableStateOf(false) }
    val modelsFetched = allModelIds.isNotEmpty()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val bg = MaterialTheme.colorScheme.background

    fun runTestConnection(fromAuto: Boolean = false) {
        if (testing) return
        if (baseUrl.isBlank() || apiKey.isBlank()) {
            if (!fromAuto) {
                scope.launch { snackbar.showSnackbar("请先填写 Base URL 与 API Key") }
            }
            return
        }
        testing = true
        testError = null
        testOk = false
        scope.launch {
            when (val result = OpenAiModelsClient.fetchModels(baseUrl, apiKey)) {
                is ModelsFetchResult.Success -> {
                    val lists = result.models
                    allModelIds = lists.allIds
                    settingsRepo.saveModelCache(baseUrl, lists.allIds)
                    testOk = true
                    // Only fill when the current value is empty or clearly not offered by this provider
                    if (chatModel.isBlank() || chatModel !in lists.allIds) {
                        pickPreferredChat(lists.chatModels)?.let { chatModel = it }
                    }
                    if (asrModel.isBlank() || asrModel !in lists.allIds) {
                        pickPreferredAsr(lists.asrModels)?.let { asrModel = it }
                    }
                    snackbar.showSnackbar(
                        "连接成功，发现 ${lists.allIds.size} 个模型" +
                            "（聊天 ${lists.chatModels.size} / ASR ${lists.asrModels.size}）"
                    )
                }
                is ModelsFetchResult.Failure -> {
                    testError = result.message
                    testOk = false
                    snackbar.showSnackbar(result.message)
                }
            }
            testing = false
        }
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "AI 模型设置",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "OpenAI 兼容接口",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bg,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "填写 Base URL 与 API Key 后点「测试连接」，自动拉取可用模型列表。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            SettingsSection(
                title = "连接",
                icon = Icons.Default.Link
            ) {
                SettingsField(
                    value = baseUrl,
                    onValueChange = {
                        baseUrl = it
                        testOk = false
                        testError = null
                        allModelIds = settingsRepo.loadModelCache(it)
                    },
                    label = "API Base URL",
                    placeholder = "https://api.openai.com/v1"
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        testOk = false
                        testError = null
                    },
                    label = { Text("API Key") },
                    placeholder = { Text("sk-…") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focus ->
                            // Optional: auto-fetch when leaving key field with both filled
                            if (!focus.isFocused && baseUrl.isNotBlank() && apiKey.isNotBlank() &&
                                !modelsFetched && !testing
                            ) {
                                runTestConnection(fromAuto = true)
                            }
                        },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    visualTransformation = if (showKey) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showKey) "隐藏密钥" else "显示密钥",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = fieldColors()
                )

                Spacer(Modifier.height(14.dp))

                OutlinedButton(
                    onClick = { runTestConnection(fromAuto = false) },
                    enabled = !testing && baseUrl.isNotBlank() && apiKey.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (testing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("正在测试连接…")
                    } else {
                        Icon(
                            Icons.Outlined.WifiTethering,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("测试连接", fontWeight = FontWeight.SemiBold)
                    }
                }

                if (testOk) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Teal400,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "连接正常，可从下方列表选择模型",
                            style = MaterialTheme.typography.bodySmall,
                            color = Teal400
                        )
                    }
                }
                testError?.let { err ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            SettingsSection(
                title = "模型",
                icon = Icons.Default.ModelTraining
            ) {
                if (modelsFetched) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "显示全部模型（含图像/语音合成等）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(checked = showAllModels, onCheckedChange = { showAllModels = it })
                    }
                    Spacer(Modifier.height(8.dp))
                }
                ModelPickerField(
                    label = "翻译模型 (Chat)",
                    value = chatModel,
                    options = chatOptions,
                    enabled = !testing,
                    onValueChange = { chatModel = it },
                    placeholder = "gpt-4o-mini 或点测试连接拉取"
                )
                Spacer(Modifier.height(12.dp))
                ModelPickerField(
                    label = "语音识别模型 (ASR)",
                    value = asrModel,
                    options = asrOptions,
                    enabled = !testing,
                    onValueChange = { asrModel = it },
                    placeholder = "whisper-1"
                )
                Spacer(Modifier.height(12.dp))
                SettingsField(
                    value = asrEndpoint,
                    onValueChange = { asrEndpoint = it },
                    label = "ASR 端点（可选）",
                    placeholder = "留空 = {Base URL}/audio/transcriptions"
                )
                if (modelsFetched) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "共 ${allModelIds.size} 个模型（聊天 ${classified.chatModels.size} / 语音识别 ${classified.asrModels.size}）。输入可实时筛选，也可填写自定义模型名。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "密钥使用 EncryptedSharedPreferences 加密存于本机。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    onSave(
                        AiProviderConfig(
                            apiBaseUrl = OpenAiModelsClient.normalizeBaseUrl(baseUrl),
                            apiKey = apiKey.trim(),
                            chatModel = chatModel.trim(),
                            asrEndpoint = asrEndpoint.trim(),
                            asrModel = asrModel.trim()
                        )
                    )
                    scope.launch { snackbar.showSnackbar("配置已保存") }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Teal400,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("保存配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "快速示例",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "• OpenAI：https://api.openai.com/v1\n" +
                            "• 本地 Ollama：http://192.168.x.x:11434/v1\n" +
                            "• 测试连接会请求 GET {Base URL}/models",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.TextButton(
                onClick = onOpenCrashLog,
                modifier = Modifier.fillMaxWidth()
            ) { Text("查看崩溃日志", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(Modifier.height(28.dp))
        }
    }
}

private fun pickPreferredChat(options: List<String>): String? {
    if (options.isEmpty()) return null
    val preferred = listOf(
        "gpt-4o-mini", "gpt-4o", "gpt-4.1-mini", "gpt-4.1", "gpt-3.5-turbo",
        "deepseek-chat", "qwen-plus", "claude-3-5-sonnet"
    )
    preferred.forEach { p ->
        options.find { it.equals(p, ignoreCase = true) || it.contains(p, ignoreCase = true) }
            ?.let { return it }
    }
    return options.firstOrNull { it.contains("mini", ignoreCase = true) }
        ?: options.firstOrNull()
}

private fun pickPreferredAsr(options: List<String>): String? {
    if (options.isEmpty()) return null
    return options.find { it.contains("whisper-1", ignoreCase = true) }
        ?: options.find { it.contains("whisper", ignoreCase = true) }
        ?: options.firstOrNull()
}

@Composable
private fun ModelPickerField(
    label: String,
    value: String,
    options: List<String>,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var expanded by remember { mutableStateOf(false) }
    // When true the text is a filter query; after picking an item we show the full list again
    var filtering by remember { mutableStateOf(false) }
    var hadFocus by remember { mutableStateOf(false) }
    val hasOptions = options.isNotEmpty()
    val query = value.trim()
    val filtered = remember(options, query, filtering) {
        if (!filtering || query.isEmpty()) options
        else options.filter { it.contains(query, ignoreCase = true) }
    }

    fun close() {
        expanded = false
        filtering = false
        keyboard?.hide()
        focusManager.clearFocus()
    }

    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                filtering = true
                if (hasOptions) expanded = true
            },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    // Collapse only when focus is really lost, not on the initial unfocused callback
                    if (it.isFocused) hadFocus = true
                    else if (hadFocus) { hadFocus = false; expanded = false; filtering = false }
                },
            enabled = enabled,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { close() }),
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (value.isNotEmpty()) {
                        IconButton(onClick = {
                            onValueChange("")
                            filtering = true
                            if (hasOptions) expanded = true
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "清空", modifier = Modifier.size(18.dp))
                        }
                    }
                    if (hasOptions) {
                        IconButton(onClick = {
                            filtering = false
                            expanded = !expanded
                        }) {
                            Icon(
                                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (expanded) "收起" else "展开模型列表"
                            )
                        }
                    }
                }
            },
            colors = fieldColors()
        )

        if (expanded && hasOptions) {
            Spacer(Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Navy800,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        if (filtered.isEmpty()) "无匹配模型，将使用自定义值「$query」"
                        else if (filtering && query.isNotEmpty()) "匹配 ${filtered.size} / ${options.size}"
                        else "共 ${options.size} 个模型",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (filtered.isEmpty()) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                    if (filtered.isNotEmpty()) {
                        LazyColumn(Modifier.heightIn(max = 260.dp)) {
                            items(filtered, key = { it }) { model ->
                                val selected = model == value
                                Text(
                                    text = highlight(model, if (filtering) query else "", Teal400),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected) Teal400 else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onValueChange(model)
                                            close()
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun highlight(text: String, query: String, color: androidx.compose.ui.graphics.Color): AnnotatedString {
    if (query.isEmpty()) return AnnotatedString(text)
    val idx = text.indexOf(query, ignoreCase = true)
    if (idx < 0 || idx + query.length > text.length) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text.substring(0, idx))
        withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) {
            append(text.substring(idx, idx + query.length))
        }
        append(text.substring(idx + query.length))
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SettingsField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = fieldColors()
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Navy800,
    unfocusedContainerColor = Navy800,
    focusedBorderColor = Teal400.copy(alpha = 0.7f),
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedLabelColor = Teal400,
    cursorColor = Teal400
)
