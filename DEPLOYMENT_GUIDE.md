# SMS Spam Detector - Deployment Guide

## 🚀 Make Your App Live Online

Your SMS Spam Detector webapp can now be deployed to the cloud! Here are the easiest options:

---

## Option 1: Deploy on Render (Recommended - Easiest)

### Steps:
1. **Create a GitHub account** (if you don't have one): https://github.com/signup
2. **Upload your code to GitHub**:
   - Create a new repository
   - Push all files to GitHub (including `app.py`, `templates/`, `static/`, `spam_model.pkl`, `vectorizer.pkl`, `requirements-deploy.txt`, `Procfile`)

3. **Deploy on Render**:
   - Go to https://render.com
   - Sign up with GitHub
   - Click "New +" → "Web Service"
   - Connect your GitHub repository
   - Fill in:
     - **Name**: `sms-spam-detector`
     - **Build Command**: `pip install -r requirements-deploy.txt`
     - **Start Command**: `gunicorn app:app`
     - **Plan**: Free (generous free tier!)
   - Click "Deploy"
   - Your app will be live at: `https://sms-spam-detector.onrender.com`

---

## Option 2: Deploy on Railway

### Steps:
1. Go to https://railway.app
2. Sign up with GitHub
3. Create a new project
4. Select "Deploy from GitHub"
5. Connect your repository
6. Railway will auto-detect the Python app
7. Your app will be live!

---

## Option 3: Deploy on Heroku

### Steps:
1. Go to https://heroku.com
2. Sign up and verify email
3. Install Heroku CLI
4. Run in your project folder:
   ```bash
   heroku login
   heroku create sms-spam-detector
   git push heroku main
   ```

---

## Option 4: Deploy on PythonAnywhere

### Steps:
1. Go to https://www.pythonanywhere.com
2. Sign up (free account available)
3. Upload your files via web interface
4. Create a web app with Flask
5. Point to your `app.py`
6. Your app will be live at: `https://yourusername.pythonanywhere.com`

---

## 📋 Files Needed for Deployment

Make sure all these files are in your GitHub repository:
- ✅ `app.py` (your Flask app)
- ✅ `templates/index.html`
- ✅ `static/css/styles.css`
- ✅ `static/js/script.js`
- ✅ `spam_model.pkl` (your trained model)
- ✅ `vectorizer.pkl` (your vectorizer)
- ✅ `requirements-deploy.txt` (dependencies)
- ✅ `Procfile` (deployment config)

---

## 🔒 Security Notes

- Never commit API keys or sensitive data to GitHub
- Use environment variables for secrets
- Keep your models updated

---

## ✨ After Deployment

Once deployed:
- Share the live URL with anyone
- Works on desktop and mobile browsers
- No need for users to install anything
- 24/7 online access

---

## 📞 Need Help?

- **Render Support**: https://render.com/docs
- **Railway Support**: https://railway.app/docs
- **Heroku Support**: https://devcenter.heroku.com
- **PythonAnywhere Support**: https://help.pythonanywhere.com

Enjoy your live SMS Spam Detector! 🎉

