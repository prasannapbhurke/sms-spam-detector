import requests
import threading
import os
from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.textinput import TextInput
from kivy.uix.button import Button
from kivy.uix.label import Label
from kivy.uix.progressbar import ProgressBar
from kivy.uix.scrollview import ScrollView
from kivy.clock import Clock
from kivy.core.window import Window

Window.size = (540, 960)

class SMSSpamDetectorApp(App):
    # YOUR RAILWAY CONFIG
    API_URL = "https://sms-spam-detector-production.up.railway.app/predict"
    API_KEY = "SMS_GUARD_2024_SECURE"

    def build(self):
        self.title = "🤖 AI SMS SPAM GUARD"
        main_layout = BoxLayout(orientation='vertical', padding=20, spacing=15)

        # UI Elements
        main_layout.add_widget(Label(text='🤖 AI SPAM DETECTOR', font_size='24sp', bold=True, size_hint_y=0.1))
        
        self.textbox = TextInput(hint_text='Paste SMS here...', size_hint_y=0.2, font_size='16sp')
        main_layout.add_widget(self.textbox)

        self.btn = Button(text='🔍 Analyze Message', size_hint_y=0.1, background_color=(0.1, 0.5, 0.9, 1))
        self.btn.bind(on_press=self.start_analysis)
        main_layout.add_widget(self.btn)

        self.result_label = Label(text='Ready to scan', size_hint_y=0.1, font_size='18sp', bold=True)
        main_layout.add_widget(self.result_label)

        self.prob_bar = ProgressBar(max=100, value=0, size_hint_y=0.05)
        main_layout.add_widget(self.prob_bar)

        self.history = TextInput(readonly=True, size_hint_y=0.4, font_size='12sp', background_color=(0.9, 0.9, 0.9, 1))
        main_layout.add_widget(self.history)

        return main_layout

    def start_analysis(self, instance):
        msg = self.textbox.text.strip()
        if not msg: return
        
        self.btn.disabled = True
        self.btn.text = "Analyzing..."
        self.result_label.text = "Contacting AI Server..."
        
        # Run network call in background thread so UI doesn't freeze
        threading.Thread(target=self.call_api, args=(msg,), daemon=True).start()

    def call_api(self, msg):
        try:
            response = requests.post(
                self.API_URL,
                json={"message": msg},
                headers={"X-API-KEY": self.API_KEY},
                timeout=15
            )
            data = response.json()
            
            # Update UI on main thread
            Clock.schedule_once(lambda dt: self.update_ui(data, msg))
        except Exception as e:
            Clock.schedule_once(lambda dt: self.show_error(str(e)))

    def update_ui(self, data, msg):
        prediction = data.get('prediction', 'Unknown')
        confidence = data.get('confidence', 0)
        
        if prediction == "Spam":
            self.result_label.text = f"🚨 SPAM DETECTED ({confidence*100:.1f}%)"
            self.result_label.color = (1, 0, 0, 1)
            self.prob_bar.value = confidence * 100
        else:
            self.result_label.text = f"✅ SAFE MESSAGE ({(1-confidence)*100:.1f}%)"
            self.result_label.color = (0, 1, 0, 1)
            self.prob_bar.value = (1 - confidence) * 100

        self.history.text = f"Msg: {msg}\nResult: {prediction}\n\n" + self.history.text
        self.btn.disabled = False
        self.btn.text = "🔍 Analyze Message"

    def show_error(self, error):
        self.result_label.text = "Connection Error"
        self.result_label.color = (1, 0.5, 0, 1)
        self.btn.disabled = False
        self.btn.text = "Retry Analysis"

if __name__ == '__main__':
    SMSSpamDetectorApp().run()
