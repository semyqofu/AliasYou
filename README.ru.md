# Alias You 🎲

[🇬🇧 English](README.md) · [🇷🇺 Русская версия](README.ru.md)

**Alias You** — современная мобильная игра для компании (Alias / «Элиас») для Android, созданная на **Kotlin**, **Jetpack Compose** и **Material Design 3 Expressive** (с поддержкой динамических цветов Material You), которая сочетает генерацию слов с помощью нейросетей (**Google Gemini** и **Сбер GigaChat**) и большой встроенный офлайн-словарь.

---

## ✨ Основные возможности

- **Два языка (Русский и English)**
  - При первом запуске открывается экран выбора языка в стиле стартового экрана Google Pixel с медленно вращающимися органическими формами MD3 Expressive.
  - Выбранный язык применяется ко всему интерфейсу, встроенным офлайн-словарям, системным промптам нейросетей, генерируемым словам и названиям команд.
  - Язык можно сменить в любой момент в диалоге настроек.

- **Генерация карточек через две нейросети**
  - **Google Gemini API**: поддержка `gemini-flash-latest`, `gemini-flash-lite-latest` и `gemini-pro-latest` с `systemInstruction`, `responseMimeType = "application/json"` и `responseSchema` для строгого вывода JSON.
  - **Сбер GigaChat API**: поддержка `GigaChat`, `GigaChat-Plus`, `GigaChat-Pro` и `GigaChat-Max` (учётные записи для физ. и юр. лиц).
  - **Жёсткий контроль формата и слов**: автоочистка и восстановление JSON, строгий фильтр одиночных слов на обычных сложностях, исключение уже сыгранных слов из истории и автоматическая повторная попытка при сбое формата.

- **Полноценный офлайн-режим (более 10 000 карточек суммарно)**
  - Переключатель **«Играть офлайн»** прямо в карточке нейросетей — при его включении блок тематики плавно скрывается, оставляя выбор сложности и параметров раунда.
  - **Русская база**: 5 000 одиночных существительных (2 000 Семья/Лёгкая, 2 000 Средняя, 1 000 Сложная) + 882 словосочетания.
  - **Английская база**: более 5 300 слов и словосочетаний по тем же уровням сложности.

- **5 уровней сложности**
  - **Семья** — добрые и понятные одиночные слова для детей и взрослых.
  - **Лёгкая** — популярные повседневные существительные (строго по 1 слову).
  - **Средняя** — интересные понятия и явления (строго по 1 слову).
  - **Сложная** — редкие, научные и абстрактные слова (строго по 1 слову).
  - **Словосочетания** 💀 *(иконка черепа)* — экстремальный режим с устойчивыми фразами, идиомами и крылатыми выражениями (2–4 слова).

- **Выразительный интерфейс Material Design 3 Expressive**
  - Стопка карточек с вертикальными свайпами, пружинной физикой и тактильной отдачей (**Вверх** — отгадано `+1`, **Вниз** — пропуск `-N`).
  - Морф-анимация загрузки со сменой MD3-форм (`COOKIE_7`, `COOKIE_4`, `ELLIPSE`, `SUNNY_9`, `COOKIE_3`, `SQUIRCLE`) и декоративные формы в интерфейсе.
  - Анимированный счётчик очков, пульсация таймера в конце раунда, диалог «Последнее слово!» по истечении времени и просмотр истории сыгранных слов и последнего JSON-ответа AI.

---

## 🛠 Технологии

- **Язык:** Kotlin
- **UI:** Jetpack Compose, Material 3, Material Icons Extended, Navigation Compose
- **Архитектура:** MVVM (`ViewModel` + `StateFlow`)
- **AI и сеть:**
  - Google Generative AI Android SDK (`com.google.ai.client.generativeai`)
  - OkHttp 4 (OAuth2 и Chat Completions REST API для Сбер GigaChat)
  - Kotlinx Serialization JSON
- **Сборка:** Gradle Kotlin DSL (`compileSdk 36`, `minSdk 24`, Java 11)

---

## 🚀 Сборка и запуск

1. **Клонируйте репозиторий:**
   ```bash
   git clone https://github.com/semyqofu/AliasYou.git
   cd AliasYou
   ```
2. **Соберите и установите APK:**
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
3. **Запустите модульные тесты:**
   ```bash
   ./gradlew testDebugUnitTest
   ```
4. **Настройте API-ключ (опционально — офлайн-режим работает сразу):**
   - Откройте настройки (иконка шестерёнки в правом верхнем углу).
   - Вставьте ключ **Google AI Studio** для Gemini или авторизационные данные **Сбер Developers** для GigaChat.

---

## 📂 Структура проекта

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
