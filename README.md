# SMS Spam Detector

An AI-powered spam detection system that uses machine learning to identify spam SMS messages in real-time. Available as desktop app, web API, and Android APK.

## Features

- **Real-time SMS Analysis** - Instant spam detection with probability scores
- **Machine Learning** - Naive Bayes classifier with TF-IDF vectorization (~97% accuracy)
- **Rule-based Detection** - Heuristic filters for common spam patterns
- **Multi-platform** - Desktop (Windows), Web API, Android APK
- **Text-to-Speech** - Audio feedback for results (desktop version)

## Project Structure

```
sms-spam-detector/
├── sms1.py           # Desktop app (CustomTkinter)
├── app.py            # Web API (Flask)
├── main.py           # Android app (Kivy)
├── spam_model.pkl    # Trained ML model
├── vectorizer.pkl   # TF-IDF vectorizer
├── spam.csv         # Training data
├── android_app/      # Android source code
├── templates/       # Web UI templates
└── bin/             # Built APKs
```

## Quick Start

### Desktop App

```bash
pip install -r requirements.txt
python sms1.py
```

### Web API

```bash
pip install -r requirements-deploy.txt
python app.py
```

The API runs on `http://localhost:5000`

#### API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Web dashboard |
| `/predict` | POST | Analyze SMS |
| `/health` | GET | Health check |

#### Predict Request

```bash
curl -X POST https://your-api.com/predict \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: SMS_GUARD_2024_SECURE" \
  -d '{"message": "Your SMS text here"}'
```

#### Response

```json
{
  "prediction": "Spam",
  "confidence": 0.975
}
```

### Android APK

See [README_ANDROID.md](README_ANDROID.md) for detailed Android build instructions.

#### Pre-built APK

Install the APK on your Android device:

```bash
adb install bin/smsspamdetector-1.0-debug.apk
```

## Machine Learning Model

- **Algorithm**: Multinomial Naive Bayes
- **Features**: TF-IDF vectorized text
- **Accuracy**: ~97%
- **Classes**: spam (1) or ham (0)

### Text Preprocessing

1. Remove non-alphanumeric characters
2. Convert to lowercase
3. Remove stopwords
4. Apply Porter Stemmer
5. TF-IDF vectorization

### Rule-based Enhancement

Additional heuristic filters:
- Money claims (win, free, prize, earn)
- Urgency indicators (urgent, immediate)
- Account actions (verify, login, update)
- Financial brands (paytm, bank, upi)
- URLs and links

## Tech Stack

| Component | Technology |
|-----------|------------|
| Desktop UI | CustomTkinter |
| Web Framework | Flask |
| Mobile UI | Kivy |
| ML Library | scikit-learn |
| NLP | NLTK |
| APK Builder | Buildozer |

## Requirements

```
pandas
numpy
scikit-learn
nltk
customtkinter
flask
flask-sqlalchemy
flask-limiter
requests
gtts
playsound
```

## Deployment

### Railway/Render

1. Connect GitHub repository
2. Set environment variables:
   - `PORT`: 5000
   - `DATABASE_URL`: PostgreSQL connection string
3. Deploy from `app.py`

### Android APK Build

```bash
# Windows
setup.bat
build.bat

# Linux/Mac
pip install buildozer cython
buildozer android debug
```

See [README_ANDROID.md](README_ANDROID.md) for full build guide.

## License

MIT License

Copyright (c) 2026 prasannapbhurke

## GitHub

https://github.com/prasannapbhurke/sms-spam-detector