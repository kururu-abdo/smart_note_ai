# SmartNote AI

Android app for recording meetings and lectures, turning them into transcripts, and generating concise Notion-style smart notes.

## Current MVP

- Kotlin + Jetpack Compose + Material 3
- Clean, Notion-inspired notes UI
- Local audio recording to app storage
- Runtime microphone permission
- Recording timer and waveform UI
- Recording metadata: title, description, type
- Notes library with sample content
- Smart Notes presentation: summary, key points, action items, decisions, open questions
- AI/transcription provider boundary planned for the next integration layer

## Architecture direction

`UI -> ViewModel -> Use Cases -> Repository -> Local Storage / AI Provider`

The repository is intentionally provider-agnostic so transcription can later be connected to an on-device engine or a cloud provider without coupling the UI to a vendor.

## Build

Open the project in Android Studio and sync Gradle. Use JDK 17 and an Android SDK with API 35.

## Roadmap

1. Persist recordings and notes with Room.
2. Add transcript entity and transcription repository.
3. Connect a real speech-to-text provider.
4. Add AI summarization with structured JSON output.
5. Render Markdown/Notion-style blocks from the structured result.
6. Add full-text and semantic search.
7. Add playback, export, sharing, and Ask AI over saved meetings.

> No API keys or secrets are stored in this repository.
