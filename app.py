import os
import re
import pickle
import nltk
from flask import Flask, request, jsonify, render_template, abort
from flask_sqlalchemy import SQLAlchemy
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address
from datetime import datetime
from functools import lru_cache, wraps
from nltk.corpus import stopwords
from nltk.stem import PorterStemmer

# --- 1. CONFIGURATION & SECURITY ---
app = Flask(__name__)

# Production API Key - Sync this with your Android app
API_KEY = "SG_Secure_6f9a2b8c3d1e4f7g8h9i0j1k2l3m4n"

# Rate Limiting
limiter = Limiter(
    get_remote_address,
    app=app,
    default_limits=["200 per day", "50 per hour"],
    storage_uri="memory://"
)

# Database Setup
DB_URL = os.environ.get('DATABASE_URL')
if DB_URL and DB_URL.startswith("postgres://"):
    DB_URL = DB_URL.replace("postgres://", "postgresql://", 1)
app.config['SQLALCHEMY_DATABASE_URI'] = DB_URL or 'sqlite:///spam_history.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

db = SQLAlchemy(app)

# --- 2. SECURITY MIDDLEWARE ---
def require_api_key(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if request.headers.get('X-API-KEY') == API_KEY:
            return f(*args, **kwargs)
        else:
            return jsonify({"error": "Unauthorized"}), 401
    return decorated_function

# Database Model
class Prediction(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    message = db.Column(db.Text, nullable=False)
    prediction = db.Column(db.String(50), nullable=False)
    confidence = db.Column(db.Float, nullable=False)
    timestamp = db.Column(db.DateTime, default=datetime.utcnow)

    def to_dict(self):
        return {
            "message": self.message[:50] + "...", 
            "prediction": self.prediction,
            "confidence": self.confidence,
            "time": self.timestamp.strftime('%H:%M')
        }

with app.app_context():
    db.create_all()

# --- 3. RESOURCE PRELOADING ---
try:
    nltk.data.find('corpora/stopwords')
except (LookupError, Exception):
    nltk.download('stopwords', quiet=True)

MODEL = None
VECTORIZER = None
STOPWORDS = None
STEMMER = None

def load_resources():
    global MODEL, VECTORIZER, STOPWORDS, STEMMER
    try:
        base_dir = os.path.dirname(os.path.abspath(__file__))
        with open(os.path.join(base_dir, 'spam_model.pkl'), 'rb') as f:
            MODEL = pickle.load(f)
        with open(os.path.join(base_dir, 'vectorizer.pkl'), 'rb') as f:
            VECTORIZER = pickle.load(f)
        STOPWORDS = set(stopwords.words("english"))
        STEMMER = PorterStemmer()
        print("🚀 Production-ready ML core loaded.")
    except Exception as e:
        print(f"❌ Initialization Error: {e}")

load_resources()

@lru_cache(maxsize=1024)
def preprocess(text):
    if not text: return ""
    text = re.sub(r"\W", " ", str(text)).lower()
    return " ".join([STEMMER.stem(w) for w in text.split() if w not in STOPWORDS])

# --- 4. SECURE ROUTES ---

@app.route('/')
def dashboard():
    return render_template('index.html')

@app.route('/predict', methods=['POST'])
@limiter.limit("10 per minute")
@require_api_key
def predict():
    data = request.get_json(silent=True)
    message = data.get('message', '').strip() if data else None

    if not message or len(message) > 1000:
        return jsonify({"error": "Invalid input"}), 400

    try:
        processed = preprocess(message)
        vectorized = VECTORIZER.transform([processed])
        prediction_id = MODEL.predict(vectorized)[0]
        label = "Spam" if prediction_id == 1 else "Not Spam"
        
        try:
            probs = MODEL.predict_proba(vectorized)[0]
            confidence = round(float(max(probs)), 2)
        except:
            confidence = 1.0

        record = Prediction(message=message, prediction=label, confidence=confidence)
        db.session.add(record)
        db.session.commit()

        return jsonify({"prediction": label, "confidence": confidence})

    except Exception as e:
        db.session.rollback()
        return jsonify({"error": "Prediction failed"}), 500

@app.route('/history', methods=['GET'])
@require_api_key
def get_history():
    data = Prediction.query.order_by(Prediction.timestamp.desc()).limit(20).all()
    return jsonify([p.to_dict() for p in data])

@app.errorhandler(429)
def ratelimit_handler(e):
    return jsonify({"error": "Too many requests"}), 429

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=int(os.environ.get('PORT', 5000)))
