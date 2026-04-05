from flask import Flask, render_template, request, jsonify
import pickle
import re
import nltk
import os
from nltk.corpus import stopwords
from nltk.stem import PorterStemmer

app = Flask(__name__)

# Setup NLTK
try:
    nltk.data.find('corpora/stopwords')
except LookupError:
    nltk.download("stopwords")

stemmer = PorterStemmer()
stop_words = set(stopwords.words("english"))

# Load model with error handling (optional - will work without it)
try:
    import pickle
    model = pickle.load(open("spam_model.pkl", "rb"))
    vectorizer = pickle.load(open("vectorizer.pkl", "rb"))
    ML_AVAILABLE = True
except Exception as e:
    print(f"ML model not available: {e} - using rule-based detection only")
    model = None
    vectorizer = None
    ML_AVAILABLE = False

# Preprocess function
def preprocess_text(text):
    text = re.sub(r"\W", " ", str(text))
    text = text.lower()
    words = text.split()
    words = [stemmer.stem(word) for word in words if word not in stop_words]
    return " ".join(words)

# Rule-based check
def rule_based_check(text):
    text = text.lower()
    spam_keywords = [
        "win", "won", "free", "prize", "claim",
        "urgent", "offer", "earn", "money",
        "click", "verify", "account", "bank",
        "restricted", "blocked", "suspended",
        "login", "update", "wallet"
    ]
    brands = ["paytm", "bank", "upi", "amazon", "google", "phonepe"]

    if ("http" in text or "www" in text or ".com" in text or ".in" in text):
        return True
    if any(word in text for word in spam_keywords):
        return True
    if any(b in text for b in brands) and any(w in text for w in ["verify", "login", "update", "restricted"]):
        return True
    return False

# Get insight
def get_insight(text):
    indicators = []
    if "http" in text or ".com" in text or ".in" in text:
        indicators.append("link detected")
    if any(x in text.lower() for x in ["verify", "login", "update"]):
        indicators.append("account action request")
    if any(x in text.lower() for x in ["win", "prize", "money"]):
        indicators.append("money/offer claim")
    return "⚠️ " + ", ".join(indicators) if indicators else "✅ Looks normal"

@app.route('/')
def index():
    return render_template('index.html')

@app.route('/predict', methods=['POST'])
def predict():
    data = request.get_json()
    message = data.get('message', '').strip()
    if not message:
        return jsonify({'error': 'No message provided'})

    try:
        # Primary: Rule-based detection (always works)
        rule_prediction = rule_based_check(message)

        # Secondary: ML model (if available)
        spam_prob = 0.5
        ml_available = False

        if ML_AVAILABLE and model is not None and vectorizer is not None:
            try:
                processed = preprocess_text(message)
                vectorized = vectorizer.transform([processed])
                proba = model.predict_proba(vectorized)[0]
                spam_prob = float(proba[1])
                ml_prediction = int(model.predict(vectorized)[0])
                ml_available = True

                # Combine ML and rule-based
                if ml_prediction == 1 or rule_prediction:
                    is_spam = True
                    spam_prob = max(spam_prob, 0.85)
                else:
                    is_spam = False
                    spam_prob = min(spam_prob, 0.15)
            except Exception as ml_error:
                print(f"ML prediction failed: {ml_error}")
                ml_available = False

        # If no ML, use rule-based only
        if not ml_available:
            if rule_prediction:
                is_spam = True
                spam_prob = 0.9
            else:
                is_spam = False
                spam_prob = 0.1

        # Special handling for very short messages
        if len(message) <= 2:
            if rule_prediction:
                is_spam = True
                spam_prob = 0.95
            else:
                is_spam = False
                spam_prob = 0.05

        if is_spam:
            result = f"🚨 SPAM DETECTED ({spam_prob*100:.1f}%)"
            color = "red"
        else:
            result = f"✅ SAFE MESSAGE ({(1-spam_prob)*100:.1f}%)"
            color = "green"

        insight = get_insight(message)
        return jsonify({
            'result': result,
            'color': color,
            'insight': insight,
            'spam_score': spam_prob * 100,
            'safe_score': (1 - spam_prob) * 100,
            'ml_used': ml_available
        })
    except Exception as e:
        return jsonify({
            'result': "⚠️ ANALYSIS FAILED - Please try again",
            'color': "orange",
            'insight': "Error occurred during analysis",
            'spam_score': 50,
            'safe_score': 50,
            'error': str(e)
        })

if __name__ == '__main__':
    app.run(debug=False, host='0.0.0.0', port=int(os.environ.get('PORT', 5000)))
