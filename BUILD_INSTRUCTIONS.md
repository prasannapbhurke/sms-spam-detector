# SMS Spam Detector - Android APK Build Guide

## Overview
This project has been converted from a desktop Python app (CustomTkinter) to a mobile app using **Kivy** and **Buildozer**. This guide will help you build and create the APK.

## Prerequisites

### 1. Install Python (if not already installed)
- Download Python 3.9+ from https://www.python.org/
- Make sure to add Python to PATH

### 2. Install Java Development Kit (JDK)
- Download JDK 11 or later from https://www.oracle.com/java/technologies/javase-downloads.html
- Set JAVA_HOME environment variable

### 3. Install Android SDK
- Download Android Studio from https://developer.android.com/studio
- Or install Android SDK Command-line Tools
- Set ANDROID_SDK_ROOT environment variable

## Step-by-Step Build Instructions

### Step 1: Install Buildozer
```bash
pip install buildozer
```

### Step 2: Install Build Dependencies (Windows)
```bash
pip install cython
pip install buildozer pyjnius plyer
```

### Step 3: Install Required Python Packages
```bash
pip install -r requirements.txt
```

### Step 4: Verify Files
Make sure you have these files in your project directory:
- `main.py` - Main Kivy application
- `buildozer.spec` - Build configuration
- `spam_model.pkl` - Pre-trained model
- `vectorizer.pkl` - TF-IDF vectorizer
- `requirements.txt` - Python dependencies

### Step 5: Build the APK

Navigate to your project directory and run:

```bash
buildozer android debug
```

**Note:** On Windows, building Android APKs can be complex. Alternative approaches:

#### Option A: Use WSL (Windows Subsystem for Linux)
1. Install WSL 2 with Ubuntu
2. Install build tools in WSL
3. Run buildozer from WSL

#### Option B: Use Docker
Build inside a Docker container with pre-configured Android environment

#### Option C: Build on Linux/Mac
More straightforward if you have access to a Linux machine

### Step 6: Release Build (After Testing)
```bash
buildozer android release
```

## Output
- **Debug APK:** `bin/smsspamdetector-1.0-debug.apk`
- **Release APK:** `bin/smsspamdetector-1.0-release.apk`

## Installation on Android Device

### Via USB Cable:
```bash
adb install bin/smsspamdetector-1.0-debug.apk
```

### Via File Transfer:
1. Transfer APK to phone
2. Open file manager and tap the APK
3. Follow installation prompts

## Features

✅ Real-time SMS spam detection  
✅ ML model (Naive Bayes) with TF-IDF vectorization  
✅ Rule-based spam indicators  
✅ Mobile-optimized UI (Kivy)  
✅ Text-to-speech feedback (optional)  
✅ Detection history  
✅ Spam probability scores  

## Troubleshooting

### "buildozer command not found"
- Ensure buildozer is installed: `pip install buildozer`
- Use full path if needed: `python -m buildozer`

### Build fails on Windows
- Use WSL or Linux for building (recommended)
- Or use an online APK builder service

### Model not found error on phone
- Ensure `spam_model.pkl` and `vectorizer.pkl` are copied to app storage
- Buildozer should include them automatically via `source.include_exts`

### Text-to-speech not working
- Install Google Text-to-Speech on your Android device
- App gracefully handles missing audio libraries

## File Structure

```
project/
├── main.py                    # Kivy app (replaces sms1.py)
├── buildozer.spec            # Build configuration
├── requirements.txt          # Python dependencies
├── spam_model.pkl            # ML model (pre-trained)
├── vectorizer.pkl            # TF-IDF vectorizer
└── spam.csv                  # Training data (optional)
```

## Supported Android Versions
- Minimum: Android 5.0 (API 21)
- Target: Android 12+ (API 31+)

## Notes

1. **File Size:** The APK will be around 80-120 MB due to Python runtime and libraries
2. **First Launch:** App may take time to extract and initialize on first run
3. **Model Performance:** Same ML model as desktop version for consistency
4. **Battery Usage:** Text-to-speech may consume battery; can be disabled

## Alternative: Using Python-to-Android (p4a)

If buildozer doesn't work:
```bash
pip install python-for-android
p4a apk --requirements python3,kivy,nltk,scikit-learn
```

## Next Steps

1. **Test locally** before building APK:
   ```bash
   python main.py
   ```

2. **Customize** if needed (change title, colors, layout in main.py)

3. **Sign release APK** for Google Play Store distribution

## Support

For issues:
- Check Kivy documentation: https://kivy.org/doc/stable/
- Buildozer troubleshooting: https://buildozer.readthedocs.io/
- Python-for-Android: https://python-for-android.readthedocs.io/

---

**Author:** Converted from Desktop to Mobile  
**Last Updated:** 2026  

