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

# New detailed insight function
def get_detailed_insight(text):
    text_lower = text.lower()
    insights = []
    
    # Finding 1: Presence of a link
    if re.search(r'https?://[^\s]+', text_lower) or re.search(r'www\.[^\s]+', text_lower):
        insights.append({
            "emoji": "🔗",
            "reason": "Contains a Link",
            "description": "Messages with links should be treated with caution. Don't click unless you trust the sender."
        })

    # Finding 2: Urgency keywords
    urgency_keywords = ["urgent", "act now", "limited time", "expires", "immediately"]
    if any(word in text_lower for word in urgency_keywords):
        insights.append({
            "emoji": "⏳",
            "reason": "Sense of Urgency",
            "description": "Scammers often create a false sense of urgency to pressure you into acting without thinking."
        })

    # Finding 3: Financial/Prize keywords
    money_keywords = ["win", "won", "prize", "claim", "free", "earn money", "cash", "lottery"]
    if any(word in text_lower for word in money_keywords):
        insights.append({
            "emoji": "💰",
            "reason": "Potential Prize/Money Scam",
            "description": "Be wary of unexpected offers of money or prizes. If it sounds too good to be true, it probably is."
        })

    # Finding 4: Account action keywords
    account_keywords = ["verify", "login", "update", "suspended", "locked", "restricted"]
    if any(word in text_lower for word in account_keywords):
        insights.append({
            "emoji": "🔒",
            "reason": "Account Security Alert",
            "description": "This message asks you to take action on an account. Never use links in an SMS to log in. Go directly to the official website or app."
        })
        
    # Finding 5: Character abuse
    if re.search(r'(!\s*!|£\s*£|\$\s*\$)', text_lower):
        insights.append({
            "emoji": "‼️",
            "reason": "Unusual Formatting",
            "description": "Excessive use of special characters can be a sign of an unprofessional or automated spam message."
        })

    if not insights:
        insights.append({
            "emoji": "✅",
            "reason": "Looks Normal",
            "description": "Our automated checks didn't find any common spam indicators, but always remain cautious."
        })
        
    return insights

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

        # Get the new detailed insights
        detailed_insights = get_detailed_insight(message)
        
        # If the message is safe, we should only show the "Looks Normal" insight.
        if not is_spam:
            detailed_insights = [{
                "emoji": "✅",
                "reason": "Looks Normal",
                "description": "Our automated checks didn't find any common spam indicators, but always remain cautious."
            }]

        return jsonify({
            'result': result,
            'color': color,
            'detailed_insight': detailed_insights, # New key
            'spam_score': spam_prob * 100,
            'safe_score': (1 - spam_prob) * 100,
            'ml_used': ml_available
        })
    except Exception as e:
        return jsonify({
            'result': "⚠️ ANALYSIS FAILED - Please try again",
            'color': "orange",
            'detailed_insight': [{
                "emoji": "⚙️",
                "reason": "Analysis Error",
                "description": f"An unexpected error occurred: {e}"
            }],
            'spam_score': 50,
            'safe_score': 50,
            'error': str(e)
        })

if __name__ == '__main__':
    app.run(debug=False, host='0.0.0.0', port=int(os.environ.get('PORT', 5000)))
