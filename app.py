from flask import Flask, request, jsonify
import pickle
import re
from nltk.corpus import stopwords
from nltk.stem import PorterStemmer
import os

# --- 1. Initialize Flask Application ---
app = Flask(__name__)

# --- 2. Load ML Models and Preprocessing Objects ---
# These are loaded once when the application starts to ensure fast responses.
try:
    # Use absolute paths for reliability, especially in production environments
    # Assuming the files are in the same directory as app.py
    base_dir = os.path.dirname(os.path.abspath(__file__))
    model_path = os.path.join(base_dir, 'spam_model.pkl')
    vectorizer_path = os.path.join(base_dir, 'vectorizer.pkl')

    with open(model_path, 'rb') as model_file:
        model = pickle.load(model_file)
    with open(vectorizer_path, 'rb') as vectorizer_file:
        vectorizer = pickle.load(vectorizer_file)
    
    # Initialize NLTK components
    # (No need to download every time, but good to have the objects ready)
    stemmer = PorterStemmer()
    stop_words = set(stopwords.words("english"))
    
    print("✅ Models and NLTK components loaded successfully.")

except FileNotFoundError as e:
    print(f"❌ Error loading model files: {e}")
    print("API cannot make predictions. Please ensure 'spam_model.pkl' and 'vectorizer.pkl' are present.")
    model = None
    vectorizer = None
except Exception as e:
    print(f"❌ An unexpected error occurred during initialization: {e}")
    model = None
    vectorizer = None


# --- 3. Preprocessing Function ---
# This function cleans the input text to match the format used for training the model.
def preprocess_text(text):
    """Cleans and prepares text for the model."""
    # Remove non-alphabetic characters and convert to lowercase
    text = re.sub(r"\W", " ", str(text)).lower()
    
    # Tokenize and apply stemming, removing stopwords
    words = text.split()
    words = [stemmer.stem(word) for word in words if word not in stop_words]
    
    return " ".join(words)


# --- 4. Create the API Endpoint ---
@app.route('/predict', methods=['POST'])
def predict():
    """
    Receives a JSON request with a 'message' and returns a spam prediction.
    """
    # --- Error Handling for Model Loading ---
    if not model or not vectorizer:
        return jsonify({"error": "Server-side model not loaded. Cannot make predictions."}), 503 # Service Unavailable

    # --- Input Validation ---
    if not request.is_json:
        return jsonify({"error": "Invalid input: request must be in JSON format."}), 400

    data = request.get_json()
    message = data.get('message')

    if not message or not isinstance(message, str) or not message.strip():
        return jsonify({"error": "Invalid input: 'message' key is missing, empty, or not a string."}), 400

    try:
        # --- Prediction Logic ---
        # 1. Preprocess the input message
        processed_message = preprocess_text(message)

        # 2. Vectorize the processed message using the loaded TF-IDF vectorizer
        vectorized_message = vectorizer.transform([processed_message])

        # 3. Predict using the loaded model
        prediction_code = model.predict(vectorized_message)[0]
        
        # 4. Interpret the prediction
        result = "Spam" if prediction_code == 1 else "Not Spam"

        # --- Return the successful response ---
        return jsonify({"prediction": result})

    except Exception as e:
        # Catch any other unexpected errors during the process
        print(f"An error occurred during prediction: {e}")
        return jsonify({"error": "An internal error occurred during prediction."}), 500


# --- 5. Health Check Endpoint (Optional but Recommended) ---
@app.route('/', methods=['GET'])
def health_check():
    """Provides a simple health check to confirm the API is running."""
    return "SMS Spam Detection API is running."


# --- 6. Main Execution ---
if __name__ == '__main__':
    # Use Gunicorn or another production server instead of app.run() for deployment.
    # The following is for local development testing.
    # The port is read from the PORT environment variable, common in cloud platforms.
    port = int(os.environ.get('PORT', 5000))
    app.run(host='0.0.0.0', port=port, debug=False)
