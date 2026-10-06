# HayRus

An Android application for voice translation between Russian and Armenian. A conversation assistant for real-life face-to-face communication: one person speaks Russian, the other speaks Armenian, and the translation is displayed and played back immediately after the speaker releases the recording button.

Works on a push-to-talk basis (press and hold — speak — release).

## Features

* Speech recognition in Russian and Armenian
* Text translation between languages using either voice input or manually entered text
* Translation playback using a neural voice, with the option to replay at normal or slow speed
* "Face-to-face" mode: two independent conversations are displayed on the screen (the upper one can be rotated 180° for the person sitting opposite), each with its own recording button.

## Technology Stack

* **Kotlin** + **Jetpack Compose** — UI and application logic
* **Azure AI Speech SDK** — speech recognition, translation, and speech synthesis (Speech-to-Text, Speech Translation, Text-to-Speech)
* **Azure Translator Text API** — translation of manually entered text
* **Kotlin coroutines** — asynchronous calls to Azure without blocking the UI

## Requirements

* Android 12 (API 31) or higher
* Azure resources (Microsoft Azure Portal): **Speech** and **Translator** — the free F0 tier is sufficient for development

## Project Setup

1. Create a **Speech** resource in the [Azure Portal](https://portal.azure.com) (publisher — Microsoft).

2. Create a **Translator** resource there as well (type — Global).

3. In the **Keys and Endpoint** section of each created resource, copy the keys and region.

4. Add the following to the `local.properties` file in the project root:

   ```properties
   AZURE_SPEECH_KEY=your_speech_key
   AZURE_SPEECH_REGION=your_region
   AZURE_TRANSLATOR_KEY=your_translator_key
   ```

5. Sync and build the project (**Build → Rebuild Project**) — the values will be passed to `BuildConfig`.
