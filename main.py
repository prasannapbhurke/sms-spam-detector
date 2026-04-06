import pickle
import re
import nltk
import threading
import os
from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.gridlayout import GridLayout
from kivy.uix.textinput import TextInput
from kivy.uix.button import Button
from kivy.uix.label import Label
from kivy.uix.progressbar import ProgressBar
from kivy.uix.scrollview import ScrollView
from kivy.garden.matplotlib.backend_kivyagg import FigureCanvasKivyAgg
from kivy.core.audio import SoundLoader
from kivy.core.window import Window
from nltk.corpus import stopwords
from nltk.stem import PorterStemmer

# Set window size for better mobile experience
Window.size = (540, 960)

try:
    from gtts import gTTS
    # We will use Kivy's SoundLoader instead of playsound for mobile compatibility
    try:
        from playsound import playsound
    except ImportError:
        playsound = None # Define it as None if not available
    AUDIO_AVAILABLE = True
except ImportError:
    AUDIO_AVAILABLE = False

# ----------------------------
# Setup
# ----------------------------
try:
    nltk.data.find('corpora/stopwords')
except LookupError:
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
    if not AUDIO_AVAILABLE:
        return

    def run():
        try:
            tts = gTTS(text=text, lang='en')
            file = "voice.mp3"
            # Ensure the file path is writable on Android
            from kivy.app import App
            file_path = os.path.join(App.get_running_app().user_data_dir, file)
            tts.save(file_path)
            
            # Use Kivy's SoundLoader for robust audio playback
            sound = SoundLoader.load(file_path)
            if sound:
                def remove_file_after_play(*args):
                    if os.path.exists(file_path):
                        os.remove(file_path)
                sound.bind(on_stop=remove_file_after_play)
                sound.play()
            else: # Fallback for desktop if playsound is available
                if playsound:
                    playsound(file_path)
                    if os.path.exists(file_path): os.remove(file_path)
        except Exception as e:
            print(f"Error in speak function: {e}")

    threading.Thread(target=run, daemon=True).start()

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
# MAIN APP
# ----------------------------
class SMSSpamDetectorApp(App):
    def build(self):
        self.title = "🤖 SMS SPAM DETECTOR"

        # Main layout
        main_layout = BoxLayout(orientation='vertical', padding=10, spacing=10)

        # Title
        title_label = Label(text='🤖 SMS SPAM DETECTOR', size_hint_y=0.1, font_size='18sp', bold=True)
        main_layout.add_widget(title_label)

        # Input section
        input_label = Label(text='Enter SMS Message:', size_hint_y=0.05, font_size='14sp')
        main_layout.add_widget(input_label)

        self.textbox = TextInput(
            multiline=True,
            size_hint_y=0.2,
            hint_text='Paste or type the SMS message here...',
            font_size='12sp'
        )
        self.textbox.bind(text=self.on_text_change)
        main_layout.add_widget(self.textbox)

        # Analyze button
        analyze_btn = Button(
            text='🔍 Analyze Message',
            size_hint_y=0.08,
            background_color=(0.1, 0.4, 0.8, 1)
        )
        analyze_btn.bind(on_press=self.predict_sms_with_audio)
        main_layout.add_widget(analyze_btn)

        # Result label
        self.result_label = Label(
            text='',
            size_hint_y=0.08,
            font_size='16sp',
            bold=True
        )
        main_layout.add_widget(self.result_label)

        # Insight label
        self.insight_label = Label(
            text='',
            size_hint_y=0.06,
            font_size='12sp',
            color=(1, 1, 0, 1)  # Yellow
        )
        main_layout.add_widget(self.insight_label)

        # Progress bars
        spam_label = Label(text='Spam Score:', size_hint_y=0.04, font_size='11sp')
        main_layout.add_widget(spam_label)

        self.spam_bar = ProgressBar(max=100, value=0, size_hint_y=0.05)
        main_layout.add_widget(self.spam_bar)

        safe_label = Label(text='Safe Score:', size_hint_y=0.04, font_size='11sp')
        main_layout.add_widget(safe_label)

        self.safe_bar = ProgressBar(max=100, value=0, size_hint_y=0.05)
        main_layout.add_widget(self.safe_bar)

        # History section
        history_label = Label(text='📋 History:', size_hint_y=0.04, font_size='12sp', bold=True)
        main_layout.add_widget(history_label)

        scroll_view = ScrollView(size_hint_y=0.3)
        self.history = TextInput(
            multiline=True,
            readonly=True,
            font_size='10sp'
        )
        scroll_view.add_widget(self.history)
        main_layout.add_widget(scroll_view)

        # Clear history button
        clear_btn = Button(
            text='🗑️ Clear History',
            size_hint_y=0.06,
            background_color=(0.8, 0.1, 0.1, 1)
        )
        clear_btn.bind(on_press=self.clear_history)
        main_layout.add_widget(clear_btn)

        return main_layout

    def on_text_change(self, instance, value):
        """Called when text changes - silent prediction"""
        self.predict_sms(speak_flag=False)

    def predict_sms_with_audio(self, instance):
        """Called when button pressed - with audio"""
        self.predict_sms(speak_flag=True)

    def predict_sms(self, speak_flag=True):
        message = self.textbox.text.strip()

        if message == "":
            self.result_label.text = ""
            self.insight_label.text = ""
            self.spam_bar.value = 0
            self.safe_bar.value = 0
            return

        try:
            processed = preprocess_text(message)
            vectorized = vectorizer.transform([processed])

            proba = model.predict_proba(vectorized)[0]
            spam_prob = float(proba[1])

            ml_prediction = model.predict(vectorized)[0]
            rule_prediction = rule_based_check(message)

            if ml_prediction == 1 or rule_prediction:
                is_spam = True
                spam_prob = max(spam_prob, 0.9)
            else:
                is_spam = False

            # Update progress bars
            self.spam_bar.value = spam_prob * 100
            self.safe_bar.value = (1 - spam_prob) * 100

            if is_spam:
                result = f"🚨 SPAM DETECTED ({spam_prob*100:.1f}%)"
                self.result_label.text = result
                self.result_label.color = (1, 0, 0, 1)  # Red

                if speak_flag:
                    speak("Warning! This message is spam")
            else:
                result = f"✅ SAFE MESSAGE ({(1-spam_prob)*100:.1f}%)"
                self.result_label.text = result
                self.result_label.color = (0, 1, 0, 1)  # Green

                if speak_flag:
                    speak("This message is safe")

            self.insight_label.text = get_insight(message)

            # Add to history
            history_entry = f"{message}\n→ {result}\n\n"
            self.history.text += history_entry

        except Exception as e:
            self.result_label.text = f"Error: {str(e)}"
            self.result_label.color = (1, 0.5, 0, 1)  # Orange

    def clear_history(self, instance):
        self.history.text = ""

if __name__ == '__main__':
    SMSSpamDetectorApp().run()
