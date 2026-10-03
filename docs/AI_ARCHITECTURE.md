# SmartNote AI — AI Architecture

## Current mode: On-device Gemini Nano

SmartNote uses Google's ML Kit GenAI Prompt API over Android AICore/Gemini Nano. The app does not require an API key or a backend for AI inference.

```text
Microphone
   ↓
Local audio file
   ↓
On-device speech recognition
   ↓
Transcript
   ↓
GeminiNanoService
   ↓
Notion-style Markdown notes
   ↓
Room / local files
```

## Scale-ready boundary

The UI and domain layer must depend on `AiService`, not directly on Gemini. The first implementation is `GeminiNanoService`.

Future implementations can be added without changing the UI:

- `CloudGeminiService` — Gemini API through a backend
- `FirebaseGeminiService` — Firebase AI Logic
- `LocalModelService` — another local model

```text
AiService
 ├── GeminiNanoService       # current, on-device
 ├── CloudGeminiService      # future
 └── LocalModelService       # future
```

## Important device constraint

Gemini Nano is not universally available on every Android device. The app must check `AVAILABLE`, `DOWNLOADABLE`, and `UNAVAILABLE` at runtime and provide a graceful fallback. ML Kit GenAI Prompt API currently requires Android API 26+ and has input/output token limits and per-app inference quotas.

## Privacy

When Gemini Nano is used, prompts and generated output are processed locally through AICore; no application backend is required.

## Long transcripts

For long meetings, do not send the entire transcript in one prompt. Use a local chunk → summarize → merge pipeline:

```text
Transcript
  ↓
Local chunking
  ↓
Gemini Nano summary per chunk
  ↓
Merge summaries
  ↓
Final concise notes
```

This keeps the app compatible with model context limits and creates a clean path to future cloud/hybrid processing.
