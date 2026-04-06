import pickle
import numpy as np
import tensorflow as tf
from sklearn.feature_extraction.text import TfidfVectorizer

# 1. Load your existing model and vectorizer
with open('spam_model.pkl', 'rb') as f:
    model = pickle.load(f)
with open('vectorizer.pkl', 'rb') as f:
    vectorizer = pickle.load(f)

# 2. Extract weights from scikit-learn (Assuming it's a LogisticRegression or LinearSVC)
# Note: Complex models like Random Forest require specialized tools like 'm2cgen' or 'sklearn-onnx'
# This example assumes a linear model which is standard for SMS spam.

try:
    weights = model.coef_.flatten().astype(np.float32)
    bias = model.intercept_.astype(np.float32)

    # 3. Create a simple Keras model that mirrors the logic
    input_dim = len(vectorizer.get_feature_names_out())
    
    keras_model = tf.keras.Sequential([
        tf.keras.layers.InputLayer(input_shape=(input_dim,)),
        tf.keras.layers.Dense(1, activation='sigmoid')
    ])

    # Set the weights
    keras_model.layers[0].set_weights([weights.reshape(input_dim, 1), bias])

    # 4. Convert to TFLite
    converter = tf.lite.TFLiteConverter.from_keras_model(keras_model)
    tflite_model = converter.convert()

    # 5. Save the model
    with open('spam_model.tflite', 'wb') as f:
        f.write(tflite_model)
    
    print("✅ Model converted to spam_model.tflite successfully!")

except Exception as e:
    print(f"❌ Conversion Error: {e}")
    print("If your model is not Linear (e.g. RandomForest), you should use 'onnx' instead.")
