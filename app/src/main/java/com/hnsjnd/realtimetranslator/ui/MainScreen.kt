package com.hnsjnd.realtimetranslator.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hnsjnd.realtimetranslator.data.AudioSource
import com.hnsjnd.realtimetranslator.data.Language
import com.hnsjnd.realtimetranslator.data.TranscriptLine
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("实时翻译", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Clear, contentDescription = "清空")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Audio source tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.audioSource == AudioSource.MICROPHONE,
                    onClick = { if (!state.isRunning) onAudioSource(AudioSource.MICROPHONE) },
                    label = { Text("麦克风") },
                    leadingIcon = { Icon(Icons.Default.Mic, null, Modifier.size(18.dp)) },
                    enabled = !state.isRunning
                )
                FilterChip(
                    selected = state.audioSource == AudioSource.INTERNAL,
                    onClick = { if (!state.isRunning) onAudioSource(AudioSource.INTERNAL) },
                    label = { Text("内录") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.VolumeUp, null, Modifier.size(18.dp)) },
                    enabled = !state.isRunning
                )
            }

            Spacer(Modifier.height(12.dp))

            // Language pickers
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LangDropdown(
                    label = "源语言",
                    selected = state.sourceLang,
                    options = Language.entries,
                    enabled = !state.isRunning,
                    onSelect = onSourceLang,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onSwap, enabled = !state.isRunning) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "交换")
                }
                LangDropdown(
                    label = "目标语言",
                    selected = state.targetLang,
                    options = Language.entries.filter { it != Language.AUTO },
                    enabled = !state.isRunning,
                    onSelect = onTargetLang,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Start / Stop
            Button(
                onClick = { if (state.isRunning) onStop() else onStart() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isRunning)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    if (state.isRunning) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (state.isRunning) "停止翻译" else "开始翻译",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = state.statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            if (state.audioSource == AudioSource.INTERNAL && !state.isRunning) {
                Text(
                    text = "内录需授予「屏幕录制」权限（MediaProjection），用于捕获系统播放音频。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Transcript list
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                if (state.lines.isEmpty()) {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "译文将显示在这里",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.lines, key = { it.id }) { line ->
                            TranscriptItem(line)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TranscriptItem(line: TranscriptLine) {
    val time = remember(line.timestampMs) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(line.timestampMs))
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (line.isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                RoundedCornerShape(8.dp)
            )
            .padding(10.dp)
    ) {
        Text(time, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        if (line.original.isNotBlank()) {
            Text(
                line.original,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(2.dp))
        }
        Text(
            line.translated,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = if (line.isError) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LangDropdown(
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
            singleLine = true
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang.displayName) },
                    onClick = {
                        onSelect(lang)
                        expanded = false
                    }
                )
            }
        }
    }
}
