import pickle
import json
import numpy as np

# Load the vectorizer
with open('vectorizer.pkl', 'rb') as f:
    vectorizer = pickle.load(f)

# Get vocabulary (word to index mapping)
vocab = vectorizer.vocabulary_

# Convert numpy int64 to standard python int for JSON serialization
cleaned_vocab = {k: int(v) if isinstance(v, (np.integer, np.int64)) else v for k, v in vocab.items()}

# Save as JSON for Android to read
with open('vocab.json', 'w') as f:
    json.dump(cleaned_vocab, f)

print("✅ Vocabulary exported to vocab.json successfully!")
