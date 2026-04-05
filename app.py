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

# Load model with error handling
try:
    model = pickle.load(open("spam_model.pkl", "rb"))
    vectorizer = pickle.load(open("vectorizer.pkl", "rb"))
except Exception as e:
    print(f"Warning: Error loading model - {e}")
    # Fallback: use rule-based detection only
    model = None
    vectorizer = None

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
        # Use rule-based detection as primary if model fails
        rule_prediction = rule_based_check(message)

        # Try to use ML model if available
        spam_prob = 0.5
        try:
            if model is not None and vectorizer is not None:
                processed = preprocess_text(message)
                vectorized = vectorizer.transform([processed])
                proba = model.predict_proba(vectorized)[0]
                spam_prob = float(proba[1])
                ml_prediction = model.predict(vectorized)[0]

                if ml_prediction == 1 or rule_prediction:
                    is_spam = True
                    spam_prob = max(spam_prob, 0.9)
                else:
                    is_spam = False
            else:
                # Fallback to rule-based only
                is_spam = rule_prediction
                spam_prob = 0.85 if is_spam else 0.15
        except Exception as ml_error:
            # If ML fails, use rule-based detection
            print(f"ML prediction failed: {ml_error}, using rule-based detection")
            is_spam = rule_prediction
            spam_prob = 0.85 if is_spam else 0.15

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
            'safe_score': (1 - spam_prob) * 100
        })
    except Exception as e:
        return jsonify({'error': str(e)})

if __name__ == '__main__':
    app.run(debug=False, host='0.0.0.0', port=int(os.environ.get('PORT', 5000)))
