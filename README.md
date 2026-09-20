# Go Assistant

**AI-powered real-time Go board analysis for Android.**

Go Assistant is an Android application that analyzes an active Go board directly from the screen and provides AI-generated move suggestions through a lightweight floating overlay. It combines **Flutter**, **Android screen capture**, **Claude Vision**, and a custom Android overlay system to deliver analysis without requiring users to manually enter the board position.

> **Status:** Active development
> **Platform:** Android
> **Frontend:** Flutter
> **AI:** Anthropic Claude Vision
> **Language:** Kotlin + Dart

---

## Overview

Go Assistant is designed to provide contextual analysis of a Go board while the board is visible on an Android device.

Instead of requiring users to manually recreate a position inside a separate analysis application, Go Assistant can capture the current screen, send the relevant visual information to an AI vision model, interpret the returned analysis, and present the result using an Android floating overlay.

The application is built around three main components:

```text
┌─────────────────────────────┐
│        Go Board / App       │
└──────────────┬──────────────┘
               │
               ▼
      Android Screen Capture
               │
               ▼
       Screenshot Processing
               │
               ▼
        Claude Vision API
               │
               ▼
        Structured Analysis
               │
               ▼
       Floating Overlay UI
               │
               ▼
     Suggested Move + Stats
```

---

## Key Features

### AI-Powered Board Analysis

Go Assistant uses a vision-capable Claude model to analyze screenshots containing a Go board.

The model is instructed to identify the board position and return structured analysis including:

* Suggested move
* Board size
* Move coordinates
* Estimated win rate
* Score estimate
* Reasoning
* Pass/resign decisions

---

### Floating Android Overlay

The analysis is displayed through an Android system overlay rather than requiring the user to leave the application currently showing the board.

The overlay can display:

* Recommended move location
* Win-rate estimate
* Score information
* AI reasoning
* Current player turn
* Visual move indicator

The move indicator is rendered directly over the screen using a custom Android `View`.

---

### Screen Capture Integration

The application uses Android's **MediaProjection API** to capture the current screen.

This allows the application to analyze boards displayed by other applications without requiring the board application itself to provide an integration API.

The capture flow is:

```text
User grants screen-capture permission
                ↓
       MediaProjection starts
                ↓
       Screen frame captured
                ↓
       Bitmap extracted
                ↓
       Image sent for analysis
                ↓
       AI response received
```

---

### Android System Overlay

Go Assistant uses Android's `SYSTEM_ALERT_WINDOW` capability to display its analysis above other applications.

This makes it possible to create a persistent analysis interface consisting of:

* Floating control bubble
* Analysis overlay
* Suggested move indicator
* Information card

---

### Structured AI Responses

The AI response is expected to follow a structured JSON format rather than returning completely free-form text.

A typical response contains fields conceptually similar to:

```json
{
  "move": "D4",
  "board_size": 19,
  "col_frac": 0.158,
  "row_frac": 0.158,
  "winrate": 0.57,
  "score": 2.5,
  "reasoning": "The move strengthens the surrounding group..."
}
```

The application then converts these values into the visual overlay.

---

## Architecture

Go Assistant consists of two primary layers.

### Flutter Layer

The Flutter application is responsible for:

* Application UI
* User configuration
* API-key input
* Overlay controls
* Android method-channel communication
* Starting and stopping the analysis service

Communication between Flutter and Android is implemented using:

```text
Flutter
   │
   │ MethodChannel
   ▼
MainActivity
   │
   ▼
OverlayService
```

---

### Native Android Layer

The Android layer handles functionality that requires native platform APIs.

Main components include:

```text
MainActivity.kt
        │
        ├── Overlay permission
        ├── MediaProjection permission
        └── OverlayService control
                 │
                 ├── Screen capture
                 ├── Floating bubble
                 ├── Overlay canvas
                 ├── Claude API requests
                 └── Analysis state
                         │
                         ▼
                OverlayCanvasView.kt
```

---

## Project Structure

```text
Go_Assistant/
│
├── android/
│   └── app/
│       └── src/
│           └── main/
│               ├── kotlin/
│               │   └── com/
│               │       └── goassistant/
│               │           ├── MainActivity.kt
│               │           ├── OverlayService.kt
│               │           └── OverlayCanvasView.kt
│               │
│               └── AndroidManifest.xml
│
├── lib/
│   ├── ...
│   └── theme/
│       └── theme.dart
│
├── test/
│
├── pubspec.yaml
│
└── README.md
```

---

# Core Components

## `MainActivity.kt`

`MainActivity` acts as the bridge between Flutter and the native Android implementation.

It exposes a Flutter `MethodChannel`:

```text
com.goassistant/overlay
```

Through this channel, Flutter can request operations such as:

```text
startOverlay
stopOverlay
isOverlayRunning
requestOverlayPermission
requestScreenCapture
```

The activity also handles Android permission flows for:

* Overlay permission
* MediaProjection screen capture

---

## `OverlayService.kt`

`OverlayService` is the core Android service responsible for running the analysis system outside the normal Flutter activity lifecycle.

Its responsibilities include:

1. Starting the foreground service
2. Maintaining the screen-capture session
3. Capturing screen frames
4. Creating the floating overlay
5. Sending screenshots to the AI service
6. Processing the AI response
7. Updating the analysis UI
8. Managing the floating control bubble

The service runs as a:

```text
Foreground Service
        +
MediaProjection
        +
System Overlay
```

This architecture allows the analysis interface to remain active while another application is in the foreground.

---

## `OverlayCanvasView.kt`

`OverlayCanvasView` is a custom Android `View` responsible for rendering the AI analysis.

It draws elements such as:

* Move marker
* Highlight ring
* Crosshair
* Win-rate information
* Score
* Reasoning card
* Pass/resign state

The view also validates and clamps model-generated coordinates before rendering them.

This prevents malformed AI output from producing invalid drawing coordinates.

---

# AI Analysis Pipeline

The analysis pipeline can be summarized as:

```text
                    Android Screen
                          │
                          ▼
                  MediaProjection
                          │
                          ▼
                     Screenshot
                          │
                          ▼
                  Image Preparation
                          │
                          ▼
                  Claude Vision API
                          │
                          ▼
                  Structured Response
                          │
             ┌────────────┼────────────┐
             ▼            ▼            ▼
           Move        Win Rate       Score
             │            │            │
             └────────────┼────────────┘
                          ▼
                 OverlayCanvasView
                          │
                          ▼
                  Visual Feedback
```

---

# Android Permissions

The application requires several Android permissions.

### Overlay Permission

```xml
<uses-permission
    android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

Required to display the floating analysis interface above other applications.

### Internet Permission

```xml
<uses-permission
    android:name="android.permission.INTERNET" />
```

Required for communication with the AI API.

### Foreground Service

```xml
<uses-permission
    android:name="android.permission.FOREGROUND_SERVICE" />
```

Allows the application to maintain its analysis service.

### Media Projection Foreground Service

```xml
<uses-permission
    android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
```

Used for Android versions requiring the appropriate foreground-service type for screen capture.

---

# Foreground Service Architecture

Go Assistant explicitly declares the service as a MediaProjection foreground service:

```xml
android:foregroundServiceType="mediaProjection"
```

On supported Android versions, the service is also started with:

```kotlin
ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
```

This makes the screen-capture service compatible with modern Android foreground-service requirements.

---

# Service Lifecycle

The service is designed to avoid accidentally creating duplicate overlay views.

When the service receives another start request, it checks whether its views have already been created.

Conceptually:

```text
Service Start
     │
     ▼
Views already exist?
   /       \
 No        Yes
 │          │
 ▼          ▼
Create     Update
Views      State
```

The service also handles an explicit:

```text
STOP
```

action from the notification, allowing the user to terminate the analysis service cleanly.

---

# Error Handling

The Flutter-to-Android communication layer performs validation before starting the service.

Examples include:

* Missing API key
* Overlay permission not granted
* Screen capture permission not granted
* Duplicate screen-capture requests
* Service startup failures

Rather than silently failing, the native layer returns explicit status values to Flutter.

Example:

```text
INVALID_KEY
SERVICE_START_FAILED
IN_PROGRESS
```

---

# AI Output Validation

AI-generated coordinates cannot be blindly trusted as rendering input.

Go Assistant therefore validates important values before drawing.

For example:

```text
Column fraction → 0.0 ... 1.0
Row fraction    → 0.0 ... 1.0
Win rate        → 0.0 ... 1.0
Board size      → 9 ... 19
```

This prevents malformed model responses from causing invalid UI state.

---

# Supported Go Board Sizes

The application is designed around standard Go board sizes:

* 9 × 9
* 13 × 13
* 19 × 19

The native rendering layer additionally validates the received board size to prevent unreasonable values from affecting the overlay.

---

# Technology Stack

| Technology                     | Purpose                         |
| ------------------------------ | ------------------------------- |
| **Dart**                       | Flutter application layer       |
| **Flutter**                    | Cross-platform UI               |
| **Kotlin**                     | Native Android implementation   |
| **Android MediaProjection**    | Screen capture                  |
| **Android Foreground Service** | Background analysis lifecycle   |
| **Android WindowManager**      | Floating overlay                |
| **Claude Vision**              | Visual Go-board analysis        |
| **MethodChannel**              | Flutter ↔ Android communication |
| **Custom Android View**        | Analysis rendering              |

---

# Getting Started

## Prerequisites

Install:

* Flutter SDK
* Android SDK
* Android Studio
* Android device or emulator
* A valid Anthropic API key

A physical Android device is recommended because the application depends heavily on:

* Screen capture
* Floating overlays
* Android permission flows

---

## Clone the Repository

```bash
git clone https://github.com/Mzaq1559/Go_Assistant.git

cd Go_Assistant
```

---

## Install Dependencies

```bash
flutter pub get
```

---

## Connect an Android Device

Verify that Flutter can detect the device:

```bash
flutter devices
```

Then run:

```bash
flutter run
```

---

# Configuration

Go Assistant requires an Anthropic API key for AI analysis.

The application passes the configured API key from the Flutter layer to the native Android service when the analysis service starts.

> **Security note:** API keys should never be committed to GitHub. For a production release, the key-management architecture should be improved so that long-lived secrets are not unnecessarily exposed in the application layer.

---

# Usage

1. Launch Go Assistant.
2. Configure the required API key.
3. Grant overlay permission.
4. Grant Android screen-capture permission.
5. Open a Go board.
6. Start the analysis overlay.
7. Go Assistant captures the board.
8. The screenshot is analyzed by the vision model.
9. The returned move and statistics are rendered over the screen.
10. Use the floating interface to control or stop the service.

---

# Important Android Considerations

Because Go Assistant uses system-level Android functionality, Android may display security and privacy warnings when enabling:

* Screen capture
* Display-over-other-apps permission
* Foreground services

These permissions are fundamental to the application's architecture.

The application should clearly explain why each permission is required before requesting it.

---

# Current Limitations

Go Assistant is an experimental AI-assisted Go analysis project and has several limitations.

### Vision-Based Board Recognition

The application relies on visual interpretation of the board rather than receiving an authoritative board-state representation.

Recognition accuracy can therefore depend on:

* Board layout
* Screen resolution
* UI theme
* Board coordinates
* Stones overlapping UI elements
* Image quality

---

### AI Accuracy

The suggested move and numerical analysis are generated by an AI vision model.

They should therefore be treated as model-generated analysis rather than guaranteed game-theoretic results.

For professional-strength Go analysis, a dedicated Go engine such as KataGo would generally provide a fundamentally different analysis architecture.

---

### Device Compatibility

Android's screen-capture and overlay APIs vary across Android versions and manufacturers.

Behavior can therefore differ between:

* Android versions
* OEM Android distributions
* Permission implementations
* Battery optimization policies

---

### Performance

Screen capture, image processing, network requests, and overlay rendering can consume significant device resources.

Future versions can improve performance through:

* Region-of-interest cropping
* Lower-resolution analysis frames
* Frame throttling
* Response caching
* Local board detection
* Background processing
* Dedicated Go-engine integration

---

# Roadmap

Potential future improvements include:

* [ ] Automatic Go-board detection
* [ ] Automatic board-size detection
* [ ] Better stone/color recognition
* [ ] Board-coordinate calibration
* [ ] Multiple candidate moves
* [ ] Move history
* [ ] Variation analysis
* [ ] KataGo integration
* [ ] Local/offline analysis
* [ ] Analysis confidence indicators
* [ ] Screenshot region selection
* [ ] Improved overlay controls
* [ ] Analysis history
* [ ] Performance optimization
* [ ] Improved Android version compatibility
* [ ] Secure API-key storage

---

# Architecture Evolution

A possible future architecture is:

```text
                   Screen
                     │
                     ▼
              Board Detection
                     │
                     ▼
             Position Extraction
                     │
            ┌────────┴────────┐
            │                 │
            ▼                 ▼
       Claude Vision       KataGo
       Interpretation      Analysis
            │                 │
            └────────┬────────┘
                     ▼
              Analysis Engine
                     │
                     ▼
              Flutter / Native
                 Overlay
```

This would separate **visual understanding** from **actual Go-engine analysis**, potentially making the system substantially more robust.

---

# Development Notes

The project intentionally combines Flutter with native Android code because several required capabilities are platform-specific.

Flutter is responsible for the application-facing experience, while Kotlin handles Android functionality that cannot be implemented cleanly using Flutter alone.

This makes the project a practical example of:

* Flutter ↔ native Android integration
* Android service architecture
* MediaProjection
* System overlays
* AI API integration
* Computer vision workflows
* Structured model output
* Real-time UI updates

---

# Contributing

Contributions are welcome.

A typical contribution workflow is:

```bash
git checkout -b feature/your-feature

# Make changes

git add .
git commit -m "Add your feature"

git push origin feature/your-feature
```

Then open a Pull Request describing:

* What changed
* Why it changed
* How it was tested
* Any Android-specific considerations

---

# Disclaimer

Go Assistant is an experimental software project intended for research, learning, and AI-assisted analysis.

AI-generated analysis may contain errors. The application does not guarantee the correctness of move suggestions, win-rate estimates, scores, or other generated information.

Users are responsible for complying with the rules and policies of any platform on which they use the application.

---

# License

Add the project's license information here.

If this project is intended to be open source, a standard license such as MIT can be added to the repository.

---

## Author

**Muhammad Zulqarnain Abdullah**

Computer Science Undergraduate · UET Taxila

* GitHub: [@Mzaq1559](https://github.com/Mzaq1559)
* Portfolio: [mzaq1559.github.io/PortFolio](https://mzaq1559.github.io/PortFolio)

---

## Project

**Go Assistant**

AI-powered visual Go-board analysis with Android screen capture and floating overlays.

[View the repository](https://github.com/Mzaq1559/Go_Assistant)
