# SMS Spam Detector

Android app for detecting spam, phishing, and suspicious SMS with a hybrid detection flow.

## What It Does

- Scans incoming SMS and inbox history
- Classifies messages as `Spam`, `Safe`, or `Needs Review`
- Shows a recent activity timeline with confidence and risk hints
- Lets users mark messages as `Safe` or `Spam` so future matching messages follow that learned label
- Supports fast manual inbox rescans and grouped activity history

## How Detection Works

- On-device inference with TensorFlow Lite via `LocalPredictor`
- Rule-based risk analysis via `RiskAnalyzer`
- Manual feedback memory stored locally and reused for future messages
- Real-time SMS interception through `SmsReceiver`

Note:
- The current app is not fully offline-only. Some classification paths may still use the remote API helper fallback when local checks do not make a final decision.

## Main Project Structure

- `android_app/` Android Studio project
- `android_app/app/src/main/java/com/example/spamdetector/MainActivity.java` main inbox UI
- `android_app/app/src/main/java/com/example/spamdetector/SmsReceiver.java` incoming SMS handling
- `android_app/app/src/main/java/com/example/spamdetector/LocalPredictor.java` local ML inference
- `android_app/app/src/main/java/com/example/spamdetector/RiskAnalyzer.java` rule-based analysis
- `android_app/app/src/main/java/com/example/spamdetector/MessageStore.java` encrypted local storage and feedback memory

## Build

Requirements:

- Android Studio
- Android SDK 34
- Java/Gradle environment supported by the included project

From the project root:

```powershell
cd android_app
./gradlew.bat assembleDebug
```

Debug APK output:

- `android_app/app/build/outputs/apk/debug/app-debug.apk`

## Current UI

- Drawer-based layout with hamburger menu
- Top stats for spam and safe messages
- Clean inbox-first recent activity screen
- Sidebar controls for auto-scan, filters, and scan scope
- Swipe actions and manual feedback controls

## Permissions

- `RECEIVE_SMS`
- `READ_SMS`
- `POST_NOTIFICATIONS`

## Status

This repository now focuses on the Android application. Older server/deployment files have been removed to keep the repo aligned with the current product.
