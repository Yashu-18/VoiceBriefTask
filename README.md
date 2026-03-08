# 🎙️ VoiceBrief

> An intelligent, background-capable Android voice recording and meeting assistant app. Built with Kotlin, Jetpack Compose, Room, WorkManager, and the Google Generative AI SDK to automatically transcribe audio and generate structured meeting summaries on-device.

---

## 📱 Description

VoiceBrief is a smart Android application designed to capture and instantly summarize meetings, lectures, and personal notes. It features a robust Foreground Service that allows recording to continue seamlessly even when the app is closed or the screen is locked. VoiceBrief intelligently chunks audio recordings to process them efficiently, utilizing the highly capable **Gemini 2.5 Flash** model to provide accurate transcriptions. After transcription, it automatically generates a structured breakdown containing a title, a short summary, actionable items, and key points—all saved locally using Room Database for an offline-first storage experience.

---

## ✨ Features

- 🎙️ **Smart Audio Recording**  
  - Background recording capability via Android Foreground Services
  - Smart auto-pause during incoming phone calls or audio focus loss
  - High-quality 16-bit PCM WAV audio capture

- 🧠 **AI Transcription & Summarization**  
  - Automatically transcribes audio precisely using Gemini 2.5 Flash
  - Generates structured summaries: Title, Summary, Action Items, and Key Points
  - Automatically chunks long audio files to support extended recordings seamlessly

- 🔁 **Resilient Background Processing**  
  - Offline-first caching with automatic retries if the network drops out
  - Powered by Android WorkManager and CoroutineWorker for reliable API queuing
  - Exponential backoff strategy to prevent rate limiting 

- ⚡ **Performance and UX**  
  - Stunning, edge-to-edge Dark Mode UI built natively with Jetpack Compose
  - MVVM Architecture for maintainable, reactive UI state management
  - Local persistence using Room Database and Kotlin StateFlow

---

## 🧪 Tech Stack

| Technology           | Role                                           |
|---------------------|------------------------------------------------|
| Kotlin              | Core application language                       |
| Jetpack Compose     | Fully declarative modern UI toolkit            |
| Room Database       | Local persistence and caching of meetings and summaries |
| Google Generative AI| Official SDK for communicating with Gemini 2.5 Flash |
| MVVM                | Architectural pattern for separation of concerns |
| Coroutines & Flows  | Asynchronous task management and reactive UI state |
| WorkManager         | Guaranteed background execution for transcription queuing |
| Dagger Hilt         | Dependency Injection                           |

---

## Screenshots

<table>
  <tr>
    <td><img src="YOUR_IMAGE_LINK_1" alt="Dashboard" width="220" height="450" /><br/>Dashboard</td>
    <td><img src="YOUR_IMAGE_LINK_2" alt="Recording Mode" width="220" height="450" /><br/>Recording Mode</td>
    <td><img src="YOUR_IMAGE_LINK_3" alt="AI Meeting Summary" width="220" height="450" /><br/>AI Meeting Summary</td>
  </tr>
</table>

*(Note: Replace `YOUR_IMAGE_LINK_...` with your actual image links once you upload them to GitHub by dragging and dropping them into a GitHub issue or PR comment box to get the CDN links)*

---

## Demo Video

[Click here to watch the demo video](YOUR_GOOGLE_DRIVE_LINK_HERE)

---

### 🚀 Setup Instructions

1. Clone the repository and open in Android Studio inside the `VoiceBrief` folder.
2. Get your free API key from [Google AI Studio](https://aistudio.google.com/app/apikey).
3. Open [gradle.properties](cci:7://file:///d:/VoiceBrief/gradle.properties:0:0-0:0) in the project root and add:
   ```properties
   GEMINI_API_KEY=your_key_here
