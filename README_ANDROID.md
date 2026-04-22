# SMS Spam Detector - Android APK

A mobile AI-powered spam detection application that uses machine learning to identify spam SMS messages in real-time. Built with Kivy framework and deployed as a standalone Android APK.

## Table of Contents

1. [Overview](#overview)
2. [Features](#features)
3. [Technical Architecture](#technical-architecture)
4. [Machine Learning Model](#machine-learning-model)
5. [Installation](#installation)
6. [Usage Guide](#usage-guide)
7. [API Integration](#api-integration)
8. [Building from Source](#building-from-source)
9. [Permissions](#permissions)
10. [Troubleshooting](#troubleshooting)
11. [Security](#security)
12. [Version History](#version-history)

---

## Overview

SMS Spam Detector is an Android application that analyzes incoming SMS messages using a combination of:

- **Naive Bayes Classifier** - Machine learning model trained on thousands of spam/ham messages
- **TF-IDF Vectorization** - Text feature extraction for accurate classification
- **Rule-based Detection** - Heuristic filters for common spam patterns

The app connects to a cloud-based API for inference, making it lightweight on the device while leveraging powerful ML capabilities.

### Key Statistics

| Metric | Value |
|--------|-------|
| ML Model Accuracy | ~97% |
| API Response Time | <2 seconds |
| APK Size | ~80-120 MB |
| Min Android Version | 5.0 (API 21) |
| Target Android Version | 14 (API 34) |

---

## Features

### Core Features

1. **Real-time SMS Analysis**
   - Paste any message text for instant analysis
   - Displays spam probability as a percentage
   - Color-coded results (Red = Spam, Green = Safe)

2. **Spam Detection Engine**
   - Machine Learning classification (Naive Bayes)
   - Rule-based pattern matching
   - URL and link detection
   - Phishing indicator detection
   - Financial brand impersonation detection

3. **History Tracking**
   - Keeps recent analysis results in memory
   - Shows message, prediction, and confidence
   - Scrollable history view

4. **Visual Feedback**
   - Progress bar showing spam probability
   - Clear text labels for results

### UI Components

| Component | Description |
|-----------|-------------|
| Title Bar | "AI SPAM DETECTOR" with 24sp bold font |
| Text Input | Multi-line text box for pasting SMS |
| Analyze Button | Trigger analysis with one tap |
| Result Label | Shows prediction with confidence percentage |
| Progress Bar | Visual representation of spam probability |
| History Panel | Scrollable list of recent analyses |

---

## Technical Architecture

### System Architecture

```
+-------------------------------------------------------------+
|                    Android Device                            |
|  +-----------------------------------------------------+   |
|  |              SMS Spam Detector APK                   |   |
|  |  +-----------+    +-----------+    +-----------+ |   |
|  |  |   Kivy   |--> |  Requests |--> |   JSON   | |   |
|  |  |   UI     |    |  Library |    |  Parser | |   |
|  |  +-----------+    +-----------+    +-----------+ |   |
|  |       |                                    |       |   |
|  |       v                                    v       |   |
|  |  +----------------------------------------------+  |   |
|  |  |        Local UI Thread (Main)              |  |   |
|  |  +----------------------------------------------+  |   |
|  +-----------------------------------------------------+   |
|                           |                               |
|                           | HTTP POST                   |
|                           v                               |
+-------------------------------------------------------------+
                            |
                            v
+-------------------------------------------------------------+
|                    Cloud API Server                         |
|  +-----------------------------------------------------+   |
|  |         Railway/Render Cloud Platform             |   |
|  |  +-----------+    +-----------+    +-----------+ |   |
|  |  |  Flask   |--> |  ML Core  |--> |  Pickle  | |   |
|  |  |  Server  |    | (NB+TFIDF)|    |  Models  | |   |
|  |  +-----------+    +-----------+    +-----------+ |   |
|  |       |                |                        |   |
|  |       v                v                        |   |
|  |  +----------------------------------------------+  |   |
|  |  |     SQLite/PostgreSQL Database            |  |   |
|  |  |   (Prediction History Storage)          |  |   |
|  |  +----------------------------------------------+  |   |
|  +-----------------------------------------------------+   |
+-------------------------------------------------------------+
```

### Technology Stack

#### Android App (Client)

| Technology | Version | Purpose |
|------------|---------|---------|
| Python | 3.9+ | Runtime language |
| Kivy | 2.2.x | UI Framework |
| requests | Latest | HTTP client |
| Buildozer | Latest | APK builder |

#### Backend API (Server)

| Technology | Version | Purpose |
|------------|---------|---------|
| Flask | 2.3.x | Web framework |
| scikit-learn | 1.3.x | ML library |
| NLTK | 3.8.x | NLP processing |
| SQLAlchemy | 2.0.x | Database ORM |
| gTTS | Latest | Text-to-speech |

---

## Machine Learning Model

### Model Details

The spam detection model consists of two components:

#### 1. Naive Bayes Classifier

- **Algorithm**: Multinomial Naive Bayes
- **Training Data**: SMS Spam Collection (spam.csv)
- **Features**: TF-IDF vectorized text
- **Accuracy**: ~97%
- **Classes**: "spam" (1) or "ham" (0)

#### 2. TF-IDF Vectorizer

- **Max Features**: 5000
- **N-gram Range**: (1, 2)
- **Min DF**: 2
- **Max DF**: 95%

### Text Preprocessing Pipeline

```
Raw SMS Text
     |
     v
+-------------------------------------------------+
|  1. Remove non-alphanumeric (regex \W)          |
|  2. Convert to lowercase                      |
|  3. Tokenize into words                   |
|  4. Remove stopwords (NLTK English)         |
|  5. Apply Porter Stemmer                   |
+-------------------------------------------------+
     |
     v
Preprocessed Text
     |
     v
+-------------------------------------------------+
|  TF-IDF Vectorization                          |
+-------------------------------------------------+
     |
     v
Feature Vector (sparse)
     |
     v
+-------------------------------------------------+
|  Naive Bayes predict_proba()                  |
+-------------------------------------------------+
     |
     v
+-------------------------------------------------+
|  Probability: [ham_prob, spam_prob]          |
+-------------------------------------------------+
```

### Rule-based Enhancement

The model is enhanced with heuristic rules for common spam patterns:

| Pattern Category | Keywords/Indicators |
|------------------|---------------------|
| Money Claims | win, won, free, prize, claim, earn, money |
| Urgency | urgent, immediate, limited, act now |
| Account Actions | verify, login, update, restricted, suspended |
| Financial | paytm, bank, upi, amazon, phonepe, google pay |
| Links | http, https, www, .com, .in, .org |
| Phone Numbers | +91, 10-digit patterns |

---

## Installation

### Pre-built APK

#### Option 1: ADB Installation (Recommended)

```bash
# Connect device via USB with USB debugging enabled
adb install smsspamdetector-1.0-debug.apk

# Or install over network
adb connect 192.168.1.100:5555
adb install smsspamdetector-1.0-debug.apk
```

#### Option 2: Manual Installation

1. Transfer `smsspamdetector-1.0-debug.apk` to your phone
2. Open a file manager app
3. Navigate to the APK file
4. Tap to install
5. Grant "Install from unknown sources" permission if prompted

#### Option 3: Direct Download

> Note: For security, always download APKs from trusted sources. Contact the developer for the latest APK.

### Required Apps

| App | Purpose |
|-----|---------|
| Android 5.0+ | Operating system |
| Google Play Services | For Play Store (if distributing) |

---

## Usage Guide

### First Launch

1. Open the app from the app drawer
2. Wait for Python runtime extraction (~30-60 seconds first time)
3. The main interface will load

### Analyzing a Message

#### Step 1: Input Message

- Paste or type any SMS message into the text input field

#### Step 2: Tap Analyze

The button will change to "Analyzing..." and become disabled during the API call.

#### Step 3: View Results

- Red result: SPAM DETECTED (XX%)
- Green result: SAFE MESSAGE (XX%)

### Interpreting Results

| Result | Color | Meaning |
|--------|-------|---------|
| SPAM DETECTED (XX%) | Red | High probability of spam |
| SAFE MESSAGE (XX%) | Green | Low probability of spam |

### Understanding Confidence

- **>90%**: Very likely spam/ham
- **70-90%**: Probable spam/ham
- **<70%**: Uncertain - use caution

---

## API Integration

### API Endpoint

```
Production URL:  https://sms-spam-detector-production.up.railway.app/predict

Method:         POST
Content-Type:   application/json
Headers:       X-API-KEY: SMS_GUARD_2024_SECURE
```

### Request Format

```json
{
  "message": "Your SMS text here"
}
```

### Response Format

```json
{
  "prediction": "Spam",
  "confidence": 0.975
}
```

### Error Responses

| Status Code | Meaning |
|------------|---------|
| 400 | Invalid input |
| 401 | Unauthorized (invalid API key) |
| 429 | Rate limit exceeded |
| 500 | Server error |

---

## Building from Source

### Prerequisites

#### Windows

| Software | Version | Download |
|----------|---------|----------|
| Python | 3.9+ | python.org |
| JDK | 11+ | oracle.com/java |
| Android SDK | Latest | developer.android.com |
| Buildozer | Latest | pip install buildozer |

#### Linux/Mac

```bash
# Install system dependencies (Ubuntu/Debian)
sudo apt-get install python3-dev python3-pip
sudo apt-get install openjdk-11-jdk
sudo apt-get install android-sdk

# Install Python dependencies
pip install buildozer cython
pip install -r requirements.txt
```

### Build Steps

#### 1. Clone Repository

```bash
git clone https://github.com/prasannapbhurke/sms-spam-detector.git
cd sms-spam-detector
```

#### 2. Install Dependencies

```bash
# Create virtual environment (optional but recommended)
python -m venv venv
source venv/bin/activate  # Linux/Mac
venv\Scripts\activate     # Windows

# Install requirements
pip install -r requirements.txt
```

#### 3. Configure Environment

Set these environment variables:

**Windows (PowerShell)**
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-11"
$env:ANDROID_SDK_ROOT = "C:\Users\YourName\AppData\Local\Android\Sdk"
```

**Linux/Mac**
```bash
export JAVA_HOME=/usr/lib/jvm/java-11
export ANDROID_SDK_ROOT=$HOME/Android/Sdk
```

#### 4. Build Debug APK

```bash
buildozer android debug
```

The APK will be generated in:
```
bin/smsspamdetector-1.0-debug.apk
```

#### 5. Build Release APK

```bash
buildozer android release
```

> Note: For production release, you'll need to configure signing keys.

### Build Configuration

The `buildozer.spec` file contains all build settings:

```
[app]
title = SMS Spam Detector
package.name = smsspamdetector
package.domain = org.smsspam
version = 1.0

requirements = python3,kivy,requests
android.permissions = INTERNET,ACCESS_NETWORK_STATE
android.minapi = 21
android.targetapi = 34
```

---

## Permissions

### Required Permissions

| Permission | Reason |
|------------|-------|
| INTERNET | Connect to API server for analysis |
| ACCESS_NETWORK_STATE | Check connectivity status |

### Privacy Considerations

- Messages are sent to the cloud API for analysis
- No messages are stored locally on the device
- API key authentication required for each request
- Rate limiting prevents abuse

---

## Troubleshooting

### Common Issues

#### 1. "Connection Error" Message

**Cause**: No internet connection or API server down

**Solution**:
- Check internet connectivity
- Verify API URL is correct
- Try again in a few minutes

#### 2. Slow First Launch

**Cause**: Python runtime extraction

**Solution**:
- Wait 30-60 seconds
- Subsequent launches will be faster

#### 3. APK Won't Install

**Cause**: Unknown sources blocked

**Solution**:
- Go to Settings > Security > Unknown Sources
- Enable "Allow from this source"

#### 4. Build Fails on Windows

**Cause**: Missing Linux tools

**Solution**:
- Use WSL (Windows Subsystem for Linux)
- Or use Android Studio to build

### Debug Logs

To view debug output:

```bash
adb logcat | grep -i "smsspam"
```

---

## Security

### API Key Management

- API key is hardcoded in the app for demo purposes
- In production, use secure key storage
- Key rotation recommended every 90 days

### Best Practices

1. **Never commit API keys to version control**
2. **Use environment variables for sensitive data**
3. **Implement key rotation**
4. **Enable rate limiting**
5. **Use HTTPS only**

### Data Privacy

| Data | Storage | Retention |
|------|---------|----------|
| SMS Text | API Server | Until request complete |
| Prediction | Database | 30 days |
| API Logs | Railway/Render | 7 days |

---

## Version History

| Version | Date | Changes |
|---------|------|--------|
| 1.0 | 2026-04-22 | Initial release with Kivy UI |
| 0.9 | 2026-01-15 | Beta testing |
| 0.5 | 2025-10-01 | Alpha prototype |

---

## License

MIT License

Copyright (c) 2026 prasannapbhurke

---

## Contact

- **GitHub**: https://github.com/prasannapbhurke/sms-spam-detector
- **Issues**: Report bugs and feature requests via GitHub Issues

---

## Acknowledgments

- NLTK for text processing
- scikit-learn for machine learning
- Kivy for cross-platform UI
- Railway/Render for cloud hosting
- Buildozer for APK packaging