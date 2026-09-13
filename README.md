# Nexus AI Assistant 🤖✨

An advanced, futuristic, privacy-first Android AI Personal Assistant powered by Jetpack Compose, Kotlin, and Multi-Provider Cloud & Local Intelligence (Google Gemini, OpenAI, Claude).

---

## 🌟 Highlights & Key Features

### 1. 🎙️ Core Multimodal Interfaces
- **Executive Home Hub**: Centralized agent command dashboard featuring real-time telemetry, companion mode toggles, and personalized assistant greetings ("Welcome back, Shahin").
- **Conversational AI Chat**: Interactive chat interface with real-time response streaming, code block formatting, and local message history persistence.
- **Voice Core Engine**: Natural voice interaction powered by Android Speech-To-Text (STT) and Neural Text-To-Speech (TTS) with interactive acoustic waveform visualizers.
- **Vision AI Studio**: Real-time camera & gallery image processing for multi-modal scene analysis, OCR text extraction, and visual query answering via Gemini Vision.
- **2D Agent Town**: Interactive virtual office minimap visualizing specialized autonomous agents operating across Executive, Operations, and Labs sectors.

---

### 2. 🧠 Multi-Provider AI Routing & Security
- **Multi-Model Support**: Seamlessly route queries between **Google Gemini 2.5 Flash / Pro**, **OpenAI GPT-4o**, **Anthropic Claude 3.5 Sonnet**, and **Custom OpenAI-Compatible Endpoints**.
- **Local / Offline Fallback**: Architecture designed for graceful degradation to on-device SLMs when disconnected from network access.
- **Hardware-Backed KeyStore Encryption**: Zero plain-text storage of API credentials. All keys are encrypted using Android `KeyStore` with `AES/GCM/NoPadding`.

---

### 3. 🛠️ Android Capability & System Tool Bridge
- **Device Telemetry**: Real-time inspection of battery health, RAM allocation, storage capacity, and network connectivity.
- **Hardware Controls**: Voice & automated command execution for device features like Flashlight, Audio Standby, and System Settings.
- **Contacts & Phone Tools**: Direct integration for contact search, automated SMS preparation, and voice call dispatch.
- **Calendar & Planning**: Read upcoming schedule events and organize operator tasks.
- **Permission Center**: Unified permission management for Microphone, Camera, Location, Contacts, and Overlay capabilities.

---

### 4. ⚡ Background Automation & Routine Engine
- **Trigger-Condition-Action Pipeline**: Build custom automation routines triggered by time schedules, device states, or location changes.
- **Background Execution**: Integrated with Android `AlarmManager` and `WorkManager` for scheduled background execution.
- **Automation Execution Logs**: Full step-by-step history tracking for executed routines and task completion metrics.

---

### 5. 🎨 Design System & Visual Polish
- **Material Design 3 (M3) Cyber Aesthetic**: Dark-themed futuristic UI with glassmorphic containers, neon glow highlights, and custom canvas telemetry widgets.
- **Dynamic Theme Customizer**: Choose between multiple visual palettes including *Nexus Cyan*, *Emerald Matrix*, *Cyber Violet*, *Crimson Command*, and *Arctic Frost*.
- **Adaptive Layouts**: Full support for edge-to-edge rendering, responsive navigation, and touch target accessibility standards.

---

## 📱 Navigation Structure

```
Bottom Navigation
  ├── 1. 🏠 Home          (Executive Dashboard & Personal Assistant)
  ├── 2. 💬 Chat          (Streaming Conversational AI)
  ├── 3. 🎙️ Voice         (Hands-Free Neural Voice Core)
  ├── 4. 👁️ Vision        (Camera & Multimodal Visual Intelligence)
  └── 5. 🏢 Agent Town    (2D Virtual Workspace & Agent Activity)
```

---

## ⚙️ Architecture Overview

- **Language**: 100% Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM + Clean Architecture with Coroutines & StateFlow
- **Data Persistence**: Room SQLite Database for local context & conversation logs
- **Network & API**: OkHttpClient + kotlinx.serialization for REST endpoints

---

## 🔒 Privacy & Permissions

Nexus AI operates on a **least-privilege principle**. All hardware permissions (Microphone, Camera, Contacts, Location) are explicitly requested at runtime only when corresponding capabilities are invoked by the operator.
