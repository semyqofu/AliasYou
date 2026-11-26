# Alias You 🎲

[🇬🇧 English](README.md) · [🇷🇺 Русская версия](README.ru.md)

**Alias You** is a modern Android party word-guessing game (Alias) built with **Kotlin**, **Jetpack Compose**, and **Material Design 3 Expressive** (including Material You Dynamic Color), powered by AI word generation (**Google Gemini** & **Sber GigaChat**) alongside an extensive built-in offline dictionary.

---

## ✨ Key Features

- **Bilingual Experience (English & Russian)**
  - First-launch Pixel-inspired onboarding screen with slowly rotating MD3 Expressive scalloped shapes to pick **English** or **Russian**.
  - Switching language adapts the entire interface, built-in offline word packs, AI prompts, generated words, and creative team names.
  - Switch languages anytime from the Settings dialog.

- **Dual AI Word Generation**
  - **Google Gemini API**: supports `gemini-flash-latest`, `gemini-flash-lite-latest`, and `gemini-pro-latest`, using `systemInstruction`, `responseMimeType = "application/json"`, and `responseSchema` for strictly structured JSON output.
  - **Sber GigaChat API**: supports `GigaChat`, `GigaChat-Plus`, `GigaChat-Pro`, and `GigaChat-Max` (both Personal and Corporate OAuth scopes).
  - **Strict Format & Word Enforcement**: automatic JSON cleanup/repair, strict single-word validation for standard difficulties, deduplication against played-word history, and automatic retry on malformed output.

- **Rich Offline Mode (10,000+ Cards Total)**
  - Toggle **Play offline** directly inside the AI card — theme selection automatically hides, leaving difficulty and round controls.
  - **Russian pack**: 5,000+ single nouns (2,000 Easy/Family, 2,000 Medium, 1,000 Hard) + 882 multi-word phrases.
  - **English pack**: 5,300+ curated cards across Family/Easy, Medium, Hard, and multi-word Phrases.

- **5 Difficulty Levels**
  - **Family** — friendly, kid-safe single words.
  - **Easy** — popular everyday nouns (strictly 1 word).
  - **Medium** — engaging concepts and phenomena (strictly 1 word).
  - **Hard** — rare, scientific, and abstract vocabulary (strictly 1 word).
  - **Phrases** 💀 *(Skull difficulty)* — extreme idioms, catchphrases, and multi-word expressions (2–4 words).

- **Material Design 3 Expressive UI & Gameplay**
  - Vertical swipe card stack with spring physics and haptic feedback (**Swipe Up** = Guessed `+1`, **Swipe Down** = Skip `-N`).
  - Morphing MD3 Expressive loading shapes (`COOKIE_7`, `COOKIE_4`, `ELLIPSE`, `SUNNY_9`, `COOKIE_3`, `SQUIRCLE`).
  - Animated score tickers, urgent countdown pulse, "Last Word" attribution dialog when the timer ends, and full played-word history viewer.

---

## 🛠 Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose, Material 3, Material Icons Extended, Navigation Compose
- **Architecture:** MVVM (`ViewModel` + `StateFlow`)
- **AI & Networking:**
  - Google Generative AI Android SDK (`com.google.ai.client.generativeai`)
  - OkHttp 4 (for Sber GigaChat OAuth2 & Chat Completions REST API)
  - Kotlinx Serialization JSON
- **Build System:** Gradle Kotlin DSL (`compileSdk 36`, `minSdk 24`, Java 11)

---

## 🚀 Getting Started

1. **Clone the repository:**
   ```bash
   git clone https://github.com/semyqofu/AliasYou.git
   cd AliasYou
   ```
2. **Build & install debug APK:**
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
3. **Run unit tests:**
   ```bash
   ./gradlew testDebugUnitTest
   ```
4. **Configure an AI key (optional — Offline Mode works out of the box):**
   - Open Settings (gear icon) inside the app.
   - Paste your **Google AI Studio** key for Gemini or **Sber Developers** authorization credentials for GigaChat.

---

## 📂 Project Structure

```text
app/src/main/
├── assets/
│   ├── words_easy.txt / words_medium.txt / words_hard.txt / words_phrases.txt
│   └── words_en_easy.txt / words_en_medium.txt / words_en_hard.txt / words_en_phrases.txt
└── java/com/semyqofu/aliasyou/
    ├── MainActivity.kt
    ├── data/
    │   ├── AiService.kt, AiPromptHelper.kt
    │   ├── GeminiService.kt, GigaChatService.kt
    │   ├── OfflineWordsRepository.kt, GameData.kt
    ├── ui/
    │   ├── AppStrings.kt
    │   ├── components/ExpressiveComponents.kt
    │   ├── screens/
    │   │   ├── LanguageOnboardingScreen.kt
    │   │   ├── SettingsScreen.kt
    │   │   ├── LoadingScreen.kt
    │   │   ├── StartScreen.kt
    │   │   ├── GameScreen.kt
    │   │   ├── GameOverScreen.kt
    │   │   └── ErrorScreen.kt
    │   └── theme/ (Color.kt, Theme.kt, Type.kt)
    └── viewmodel/GameViewModel.kt
```
