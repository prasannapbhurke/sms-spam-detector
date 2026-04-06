import customtkinter as ctk
import pickle
import re
import nltk
import threading
import os

from gtts import gTTS
from playsound import playsound
from nltk.corpus import stopwords
from nltk.stem import PorterStemmer

# ----------------------------
# Setup
# ----------------------------
ctk.set_appearance_mode("dark")
ctk.set_default_color_theme("dark-blue")

nltk.download("stopwords")

stemmer = PorterStemmer()
stop_words = set(stopwords.words("english"))

# Load model
model = pickle.load(open("spam_model.pkl", "rb"))
vectorizer = pickle.load(open("vectorizer.pkl", "rb"))

# ----------------------------
# 🔊 SPEAK FUNCTION (gTTS)
# ----------------------------
def speak(text):
    def run():
        try:
            tts = gTTS(text=text, lang='en')
            file = "voice.mp3"
            tts.save(file)
            playsound(file)
            os.remove(file)
        except:
            pass

    threading.Thread(target=run).start()

# ----------------------------
# Preprocess
# ----------------------------
def preprocess_text(text):
    text = re.sub(r"\W", " ", str(text))
    text = text.lower()
    words = text.split()
    words = [stemmer.stem(word) for word in words if word not in stop_words]
    return " ".join(words)

# ----------------------------
# 🚨 ADVANCED RULE SYSTEM
# ----------------------------
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

# ----------------------------
# Insight
# ----------------------------
def get_insight(text):
    indicators = []

    if "http" in text or ".com" in text or ".in" in text:
        indicators.append("link detected")

    if any(x in text.lower() for x in ["verify", "login", "update"]):
        indicators.append("account action request")

    if any(x in text.lower() for x in ["win", "prize", "money"]):
        indicators.append("money/offer claim")

    return "⚠️ " + ", ".join(indicators) if indicators else "✅ Looks normal"

# ----------------------------
# Animation
# ----------------------------
def animate_bar(bar, target):
    current = bar.get()
    if current < target:
        current += 0.01
        bar.set(current)
        app.after(5, lambda: animate_bar(bar, target))
    else:
        bar.set(target)

# ----------------------------
# Prediction
# ----------------------------
def predict_sms(speak_flag=True):
    message = textbox.get("1.0", "end").strip()

    if message == "":
        return

    processed = preprocess_text(message)
    vectorized = vectorizer.transform([processed])

    proba = model.predict_proba(vectorized)[0]
    spam_prob = proba[1]

    ml_prediction = model.predict(vectorized)[0]
    rule_prediction = rule_based_check(message)

    if ml_prediction == 1 or rule_prediction:
        is_spam = True
        spam_prob = max(spam_prob, 0.9)
    else:
        is_spam = False

    animate_bar(spam_bar, spam_prob)
    animate_bar(safe_bar, 1 - spam_prob)

    if is_spam:
        result = f"🚨 SPAM DETECTED ({spam_prob*100:.1f}%)"
        result_label.configure(text=result, text_color="red")

        if speak_flag:
            speak("Warning! This message is spam")

    else:
        result = f"✅ SAFE MESSAGE ({(1-spam_prob)*100:.1f}%)"
        result_label.configure(text=result, text_color="green")

        if speak_flag:
            speak("This message is safe")

    insight_label.configure(text=get_insight(message))

    history.insert("end", f"{message}\n→ {result}\n\n")
    history.see("end")

# ----------------------------
# UI
# ----------------------------
app = ctk.CTk()
app.geometry("1000x650")
app.title("🤖 SMS SPAM DETECTION SYSTEM")

main = ctk.CTkFrame(app)
main.pack(fill="both", expand=True, padx=10, pady=10)

left = ctk.CTkFrame(main)
left.pack(side="left", expand=True, fill="both", padx=10)

right = ctk.CTkFrame(main)
right.pack(side="right", expand=True, fill="both", padx=10)

textbox = ctk.CTkTextbox(left, height=120)
textbox.pack(pady=10, fill="x")

# No voice while typing
textbox.bind("<KeyRelease>", lambda e: predict_sms(False))

# Button → voice
btn = ctk.CTkButton(left, text="Analyze Message",
                    command=lambda: predict_sms(True))
btn.pack()

result_label = ctk.CTkLabel(left, text="", font=("Arial", 18))
result_label.pack(pady=10)

insight_label = ctk.CTkLabel(left, text="", text_color="yellow")
insight_label.pack()

spam_bar = ctk.CTkProgressBar(left)
spam_bar.pack(pady=5)

safe_bar = ctk.CTkProgressBar(left)
safe_bar.pack(pady=5)

history = ctk.CTkTextbox(right)
history.pack(fill="both", expand=True)

app.mainloop()