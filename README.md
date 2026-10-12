# 实时翻译 / Realtime Translator

Android 手机实时翻译应用（Kotlin + Jetpack Compose）。支持麦克风外录与系统内录音频捕获，通过可配置的 OpenAI 兼容大模型完成语音识别（ASR）与文本翻译。

> **包名：** `com.hnsjnd.realtimetranslator`  
> **最低系统：** Android 10（API 29，内录依赖 AudioPlaybackCapture）  
> **目标系统：** API 34

---

## 功能

- **麦克风实时翻译**：`AudioRecord` 捕获环境声 → 分片 → ASR → 翻译
- **系统内录（内录）**：`MediaProjection` + `AudioPlaybackCaptureConfiguration` 捕获手机播放的音频（视频、通话外放、其他 App 声音等，视 OEM/App 策略而定）
- **可配置 AI 大模型**：任意 OpenAI 兼容 API（官方 OpenAI、中转、本地 Ollama/vLLM 等）
  - API Base URL
  - API Key（`EncryptedSharedPreferences` 加密存储于本机）
  - Chat 翻译模型名
  - ASR 模型名 / 可选独立 ASR 端点
- **语言**：源语言（含「自动检测」）与目标语言：中 / 英 / 日 / 韩 / 法等
- **前台服务**：翻译过程中以通知栏前台服务持续运行
- **实时字幕列表**：原文 + 译文逐条展示

---

## 配置 AI 模型

1. 打开 App → 右上角 **设置**
2. 填写：
   - **API Base URL**：例如 `https://api.openai.com/v1`
   - **API Key**：你的密钥
   - **翻译模型**：例如 `gpt-4o-mini`
   - **语音识别模型**：例如 `whisper-1`
   - **ASR 端点（可选）**：留空则使用 `{Base URL}/audio/transcriptions`
3. 点击 **保存配置**

密钥仅保存在本机加密存储中，不会上传到本项目的任何服务器。

### 兼容说明

| 能力 | 默认路径 | 说明 |
|------|----------|------|
| ASR | `POST {base}/audio/transcriptions` | Whisper 风格 multipart（`file` + `model`） |
| 翻译 | `POST {base}/chat/completions` | Chat Completions + 实时字幕风格 system prompt |

只要服务端实现上述 OpenAI 兼容接口即可接入。

---

## 权限说明

| 权限 | 用途 |
|------|------|
| `RECORD_AUDIO` | 麦克风录音 |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MICROPHONE` / `FOREGROUND_SERVICE_MEDIA_PROJECTION` | 前台翻译服务 |
| `POST_NOTIFICATIONS`（Android 13+） | 显示前台通知 |
| **屏幕录制（MediaProjection）** | **内录**时由系统弹窗授予，用于 `AudioPlaybackCapture` |

内录**必须**由用户在系统对话框中同意屏幕捕获；拒绝则无法启动内录模式。

---

## 用 Android Studio 打开 / 构建

### 环境要求

- Android Studio Hedgehog 或更新版本（AGP 8.2+）
- JDK 17+
- Android SDK Platform 34、Build-Tools 34

### 步骤

```bash
git clone https://github.com/hnsjnd/realtime-translator.git
cd realtime-translator
# 用 Android Studio 打开本目录，等待 Gradle Sync
# 或命令行：
./gradlew assembleDebug
```

调试包输出路径：

```
app/build/outputs/apk/debug/app-debug.apk
```

安装到设备：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

命令行构建前请确认 `local.properties` 中有：

```properties
sdk.dir=/path/to/your/Android/Sdk
```

（克隆后该文件通常不在仓库中，Android Studio 会自动生成。）

---

## 项目结构

```
app/src/main/java/com/hnsjnd/realtimetranslator/
├── ui/           # Compose 主界面、设置页、ViewModel、主题
├── audio/        # MicAudioCapture、InternalAudioCapture、WavEncoder
├── asr/          # AsrService + OpenAiWhisperAsr
├── translate/    # TranslationService + OpenAiChatTranslator
├── settings/     # EncryptedSharedPreferences 配置仓库
├── service/      # TranslationForegroundService + Pipeline
└── data/         # 数据模型
```

---

## 已知限制

- **仅 Android**，不含 iOS
- **内录**依赖用户授予 MediaProjection；部分应用/厂商可能禁止被捕获
- ASR 按约 2.5 秒分片上传，非真正流式 WebSocket（接口可扩展）
- 需要可用的网络与已配置的 ASR/Chat API；无 API Key 时无法开始翻译
- API Key 只存本机，卸载 App 会清除

---

## License

MIT © hnsjnd
