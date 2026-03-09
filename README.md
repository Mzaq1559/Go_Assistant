# Go Assistant – Screen Overlay

A floating bubble that sits on top of WeChat.
Tap it → it screenshots your board → Claude AI draws a gold circle on the best move.

---

## Build & Install

```
flutter pub get
flutter build apk --release
```

Install the APK on your phone (USB or copy the file).

---

## First Launch (takes 2 minutes)

1. Open **Go Assistant**
2. Tap **Grant** next to "Draw Over Other Apps" → allow it in settings → come back
3. Enter your Claude API key  
   → Get one free at **console.anthropic.com** → API Keys
4. Choose who plays next (Black or White)
5. Tap **Start Overlay**
6. Allow "Screen Capture" when the popup appears
7. A floating bubble ⚫ appears on your screen

---

## Playing

1. Open WeChat → open your Go game
2. When it's your turn, **tap the bubble**
3. Wait 2–4 seconds
4. A **gold circle** appears on the recommended move
5. Tap it in WeChat → play the move
6. Bubble has auto-switched to the other color
7. Repeat each turn

---

## Tips

- Drag the bubble anywhere on screen if it's in the way
- The notification bar shows "Overlay active" while running
- To stop: pull down notification → tap Stop, or go back to the app and tap Stop Overlay
- If the circle is slightly off, use "Board size" in settings to fix it (pick your actual board size instead of Auto)
