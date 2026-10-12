package com.hnsjnd.realtimetranslator

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hnsjnd.realtimetranslator.ui.CrashLogScreen
import com.hnsjnd.realtimetranslator.ui.MainScreen
import com.hnsjnd.realtimetranslator.util.CrashLogger
import com.hnsjnd.realtimetranslator.ui.SettingsScreen
import com.hnsjnd.realtimetranslator.ui.TranslatorViewModel
import com.hnsjnd.realtimetranslator.ui.theme.RealtimeTranslatorTheme

class MainActivity : ComponentActivity() {

    private val viewModel: TranslatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RealtimeTranslatorTheme {
                val state by viewModel.ui.collectAsStateWithLifecycle()
                var showSettings by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
                var crashLog by remember { mutableStateOf(CrashLogger.read(this@MainActivity)) }
                var showCrash by remember { mutableStateOf(crashLog != null) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    val granted = results.values.all { it }
                    if (granted) {
                        tryStartTranslation()
                    } else {
                        Toast.makeText(this, "需要麦克风等权限才能翻译", Toast.LENGTH_SHORT).show()
                    }
                }

                val mediaProjectionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                        viewModel.onMediaProjectionGranted(result.resultCode, result.data)
                    } else {
                        viewModel.onMediaProjectionDenied()
                    }
                }

                LaunchedEffect(state.needsMediaProjection) {
                    if (state.needsMediaProjection) {
                        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        mediaProjectionLauncher.launch(mpm.createScreenCaptureIntent())
                    }
                }

                if (showCrash && crashLog != null) {
                    CrashLogScreen(
                        log = crashLog!!,
                        onClear = {
                            CrashLogger.clear(this@MainActivity)
                            crashLog = null
                            showCrash = false
                        },
                        onClose = { showCrash = false }
                    )
                } else if (showSettings) {
                    SettingsScreen(
                        config = state.config,
                        onSave = { viewModel.saveConfig(it) },
                        onBack = { showSettings = false },
                        onOpenCrashLog = {
                            crashLog = CrashLogger.read(this@MainActivity) ?: "暂无崩溃日志"
                            showCrash = true
                        }
                    )
                } else {
                    MainScreen(
                        state = state,
                        onSourceLang = viewModel::setSourceLang,
                        onTargetLang = viewModel::setTargetLang,
                        onAudioSource = viewModel::setAudioSource,
                        onStart = {
                            val needed = mutableListOf(Manifest.permission.RECORD_AUDIO)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                needed += Manifest.permission.POST_NOTIFICATIONS
                            }
                            val missing = needed.filter {
                                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
                            }
                            if (missing.isEmpty()) {
                                tryStartTranslation()
                            } else {
                                permissionLauncher.launch(missing.toTypedArray())
                            }
                        },
                        onStop = viewModel::stop,
                        onSwap = viewModel::swapLanguages,
                        onClear = viewModel::clearTranscript,
                        onOpenSettings = { showSettings = true }
                    )
                }
            }
        }
    }

    private fun tryStartTranslation() {
        // requestStart returns true if MediaProjection intent will be launched via state
        viewModel.requestStart()
    }
}
