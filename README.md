# 🐤 Taha Bird

A Flappy Bird–style game for Android, built entirely with **Kotlin** and **Jetpack Compose**.
All graphics are drawn in code on a Compose `Canvas` — no image assets required.

## 📥 Download

**[⬇️ Download TahaBird.apk (v1.0)](https://github.com/TAHASHAH12/Taha_Bird/releases/download/v1.0/TahaBird.apk)**

Or browse all versions on the [Releases page](https://github.com/TAHASHAH12/Taha_Bird/releases).

### Install on your phone
1. Open the download link above on your Android phone.
2. Tap the downloaded `TahaBird.apk`.
3. If prompted, allow **Install unknown apps** for your browser / file manager.
4. If Play Protect shows a warning, tap **More details → Install anyway**.
5. Find **Taha Bird** in your app drawer and play!

> Requires Android 7.0 (API 24) or newer.

## 🎮 How to Play

- **Tap** anywhere to start and to flap.
- Fly through the gaps between the pipes — each pipe passed is **+1 point**.
- Hitting a pipe or the ground ends the game.
- Your **best score** is saved on your device.

## ✨ Features

- Smooth game loop synced to the display refresh rate (`withFrameNanos`)
- Gravity physics with bird tilt and wing-flap animation
- Randomized pipes, parallax clouds and scrolling ground
- Resolution-independent — plays the same on any screen size
- Persistent high score
- Start, in-game and Game Over screens

## 🛠️ Build from Source

**Requirements:** Android Studio (latest), JDK 11+, Android SDK

```bash
git clone https://github.com/TAHASHAH12/Taha_Bird.git
cd Taha_Bird
./gradlew assembleDebug
```

The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.
Or simply open the project in Android Studio and press **Run ▶**.

### Tweak the difficulty
Edit the constants at the top of
[`TahaBirdGame.kt`](app/src/main/java/com/example/firstapp/TahaBirdGame.kt):

| Constant        | Effect                          |
|-----------------|---------------------------------|
| `PIPE_GAP`      | Size of the gap between pipes   |
| `PIPE_SPEED`    | How fast the pipes move         |
| `PIPE_SPACING`  | Distance between pipe pairs     |
| `GRAVITY`       | How fast the bird falls         |
| `FLAP_VELOCITY` | How strong each flap is         |

## 📁 Project Structure

```
app/src/main/java/com/example/firstapp/
├── MainActivity.kt     # App entry point
├── TahaBirdGame.kt     # Game engine, rendering and UI overlays
└── ui/theme/           # Compose theme
```

## 👤 Author

**Taha Shah** — [@TAHASHAH12](https://github.com/TAHASHAH12)
