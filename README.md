# SmartNote AI

Android app for recording meetings and lectures, capturing an on-device transcript, and generating concise Notion-style smart notes.

## Product flow

`Record -> On-device Speech Recognition -> Transcript -> Gemini Nano -> Smart Notes -> Room`

No backend and no Gemini API key are required for the on-device AI path.

## Current architecture

```text
Presentation (Jetpack Compose)
        |
        v
      MVP View
        |
        v
    MainPresenter
        |
        +--> GenerateSmartNotesUseCase
        |          |
        |          v
        |   AiNoteGenerator
        |          |
        |   ChunkedAiNoteGenerator
        |          |
        |   GeminiNanoNoteGenerator
        |
        +--> NoteRepository
                   |
                   v
             RoomNoteRepository
                   |
                   v
                 Room
```

### Manual DI

`AppContainer` is the composition root. There is intentionally no Hilt/Dagger dependency at this stage. All infrastructure dependencies are created in one place and injected through interfaces.

### MVP

The presentation boundary uses `MainContract.View` and `MainPresenter`. Compose is treated as the View; business orchestration remains in the Presenter and domain use cases.

### AI provider abstraction

```kotlin
interface AiNoteGenerator {
    suspend fun generate(transcript: String): String
}
```

Gemini Nano is the current implementation. A cloud Gemini implementation can be added later without changing the domain layer.

## On-device AI

The app uses Google's ML Kit Prompt API for Gemini Nano. The current dependency is `com.google.mlkit:genai-prompt:1.0.0-beta4`. Gemini Nano is accessed through Android AICore and is only usable when the device reports the feature as available. The application does not contain an API key.

Gemini Nano has an input limit, so long transcripts are split into chunks locally and summarized hierarchically before the final note is generated.

## On-device transcription

`AndroidSpeechTranscriber` prefers Android's on-device speech recognizer when the device provides one and requests offline recognition. Devices without an on-device recognizer may fall back to the platform recognizer; the product can later expose this as an explicit privacy setting.

## Local persistence

Room stores note metadata, transcript text, and the path to the local audio file. The repository boundary keeps the domain independent of Room.

## Project structure

```text
app/src/main/java/com/kururu/smartnoteai/
├── data/
│   ├── ai/
│   ├── local/
│   ├── repository/
│   └── speech/
├── di/
├── domain/
│   ├── ai/
│   ├── model/
│   ├── repository/
│   ├── speech/
│   └── usecase/
├── presentation/
└── MainActivity.kt
```

## Build

Open the project in Android Studio, sync Gradle, and use JDK 17 with Android SDK 35.

Gemini Nano/AICore support is device-dependent. A device that reports Gemini Nano as unavailable must be handled as an unsupported on-device AI configuration.

## Roadmap

- [x] Local audio recording
- [x] On-device speech recognition boundary
- [x] Room persistence
- [x] Manual dependency injection
- [x] MVP presentation boundary
- [x] Gemini Nano abstraction
- [x] Long-transcript chunking
- [ ] Structured Smart Notes model with typed output
- [ ] Audio playback and timeline-linked transcript
- [ ] Search and filtering
- [ ] Markdown/PDF export
- [ ] Ask AI over saved notes
- [ ] Optional cloud Gemini provider
- [ ] Sync/auth/backend when product scale requires it

> No API keys or secrets are stored in this repository.
