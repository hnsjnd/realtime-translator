package com.hnsjnd.realtimetranslator.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hnsjnd.realtimetranslator.data.AudioSource
import com.hnsjnd.realtimetranslator.data.Language
import com.hnsjnd.realtimetranslator.data.TranscriptLine
import com.hnsjnd.realtimetranslator.ui.theme.Navy800
import com.hnsjnd.realtimetranslator.ui.theme.Navy850
import com.hnsjnd.realtimetranslator.ui.theme.Teal400
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: UiState,
    onSourceLang: (Language) -> Unit,
    onTargetLang: (Language) -> Unit,
    onAudioSource: (AudioSource) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onSwap: () -> Unit,
    onClear: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.lines.size) {
        if (state.lines.isNotEmpty()) {
            listState.animateScrollToItem(state.lines.lastIndex)
        }
    }

    val bg = MaterialTheme.colorScheme.background

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "实时翻译",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Realtime Translator",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onClear, enabled = state.lines.isNotEmpty()) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "清空记录",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "设置",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bg,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            StatusPill(
                isRunning = state.isRunning,
                message = state.statusMessage
            )

            Spacer(Modifier.height(16.dp))

            AudioSourceSegment(
                selected = state.audioSource,
                enabled = !state.isRunning,
                onSelect = onAudioSource
            )

            if (state.audioSource == AudioSource.INTERNAL && !state.isRunning) {
                Text(
                    text = "内录需授予「屏幕录制」权限，用于捕获系统播放音频。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            LanguageRow(
                source = state.sourceLang,
                target = state.targetLang,
                enabled = !state.isRunning,
                onSource = onSourceLang,
                onTarget = onTargetLang,
                onSwap = onSwap
            )

            Spacer(Modifier.height(20.dp))

            StartStopButton(
                isRunning = state.isRunning,
                onClick = { if (state.isRunning) onStop() else onStart() }
            )

            Spacer(Modifier.height(20.dp))

            Text(
                "实时字幕",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                shadowElevation = 0.dp
            ) {
                if (state.lines.isEmpty()) {
                    EmptyTranscript()
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.lines, key = { it.id }) { line ->
                            TranscriptCard(line)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(isRunning: Boolean, message: String) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (isRunning) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotScale"
    )
    val dotColor by animateColorAsState(
        targetValue = when {
            isRunning -> Teal400
            message.contains("错误") || message.contains("失败") -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        },
        label = "dotColor"
    )
    val statusLabel = when {
        isRunning && (message.contains("捕获") || message.contains("启动")) -> "聆听中"
        isRunning -> "翻译中"
        else -> "空闲"
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .scale(if (isRunning) scale else 1f)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            statusLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "  ·  $message",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AudioSourceSegment(
    selected: AudioSource,
    enabled: Boolean,
    onSelect: (AudioSource) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Navy850)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SegmentItem(
            selected = selected == AudioSource.MICROPHONE,
            enabled = enabled,
            icon = Icons.Default.Mic,
            label = "麦克风",
            subtitle = "外部环境声",
            modifier = Modifier.weight(1f),
            onClick = { onSelect(AudioSource.MICROPHONE) }
        )
        SegmentItem(
            selected = selected == AudioSource.INTERNAL,
            enabled = enabled,
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            label = "内录",
            subtitle = "系统播放声",
            modifier = Modifier.weight(1f),
            onClick = { onSelect(AudioSource.INTERNAL) }
        )
    }
}

@Composable
private fun SegmentItem(
    selected: Boolean,
    enabled: Boolean,
    icon: ImageVector,
    label: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "segBg"
    )
    val content by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "segFg"
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = content)
        Text(
            subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = content.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun LanguageRow(
    source: Language,
    target: Language,
    enabled: Boolean,
    onSource: (Language) -> Unit,
    onTarget: (Language) -> Unit,
    onSwap: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LangPickerCard(
            label = "源语言",
            selected = source,
            options = Language.entries,
            enabled = enabled,
            onSelect = onSource,
            modifier = Modifier.weight(1f)
        )
        Surface(
            onClick = onSwap,
            enabled = enabled,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = "交换语言",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        LangPickerCard(
            label = "目标语言",
            selected = target,
            options = Language.entries.filter { it != Language.AUTO },
            enabled = enabled,
            onSelect = onTarget,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LangPickerCard(
    label: String,
    selected: Language,
    options: List<Language>,
    enabled: Boolean,
    onSelect: (Language) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier
    ) {
        TextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            enabled = enabled,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Navy800,
                unfocusedContainerColor = Navy800,
                disabledContainerColor = Navy800.copy(alpha = 0.6f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            options.forEach { lang ->
                DropdownMenuItem(
                    text = {
                        Text(
                            lang.displayName,
                            fontWeight = if (lang == selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (lang == selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onSelect(lang)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun StartStopButton(isRunning: Boolean, onClick: () -> Unit) {
    val brush = if (isRunning) {
        Brush.horizontalGradient(
            listOf(Color(0xFFFB7185), Color(0xFFE11D48))
        )
    } else {
        Brush.horizontalGradient(
            listOf(Color(0xFF2DD4BF), Color(0xFF14B8A6), Color(0xFF0D9488))
        )
    }
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(brush),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = if (isRunning) Color.White else Color(0xFF0B1220)
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
    ) {
        Icon(
            if (isRunning) Icons.Default.Stop else Icons.Default.GraphicEq,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            if (isRunning) "停止翻译" else "开始翻译",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyTranscript() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Hearing,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "等待语音输入",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "配置 AI 模型后点击「开始翻译」\n原文与译文将实时显示在这里",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TranscriptCard(line: TranscriptLine) {
    val time = remember(line.timestampMs) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(line.timestampMs))
    }
    val borderColor = if (line.isError)
        MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
    else
        MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (line.isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                else Navy850
            )
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (line.isError) Icons.Default.Translate else Icons.Default.Translate,
                contentDescription = null,
                tint = if (line.isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (line.original.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                line.original,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            line.translated,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (line.isError) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface
        )
    }
}
