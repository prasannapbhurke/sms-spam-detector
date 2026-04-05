# SMS Spam Detector - Quick Deployment Steps

## 🚀 Get Your App Live in 5 Minutes!

### Step 1: Prepare Your Code
✅ Your app is ready! All files are included:
- `app.py` - Flask backend
- `templates/`, `static/` - Frontend files  
- `requirements-deploy.txt` - Dependencies
- `Procfile` - Deployment config
- `spam_model.pkl`, `vectorizer.pkl` - ML models

### Step 2: Setup Git Repository

```bash
git init
git add .
git commit -m "Initial commit: SMS Spam Detector"
```

Or just run: `init-git.bat`

### Step 3: Create GitHub Repository

1. Go to https://github.com/new
2. Create new repository (name: `sms-spam-detector`)
3. Copy the remote URL

### Step 4: Push to GitHub

```bash
git remote add origin https://github.com/YOUR_USERNAME/sms-spam-detector.git
git branch -M main
git push -u origin main
```

### Step 5: Deploy on Render

1. Visit https://render.com
2. Sign up with GitHub
3. Click "New +" → "Web Service"
4. Select your repository
5. Configure:
   - **Build Command**: `pip install -r requirements-deploy.txt`
   - **Start Command**: `gunicorn app:app`
6. Click "Deploy"

### Step 6: Access Your Live App

Your app will be available at:
```
https://sms-spam-detector.onrender.com
```

---

## 📱 Share Your App

Send this link to anyone:
- Works on desktop, tablet, and mobile
- No installation needed
- Available 24/7

---

## 🔄 Update Your App

To update after making changes:
```bash
git add .
git commit -m "Update: Your changes here"
git push
```

Render will automatically redeploy! 🎉

---

## ⚡ Pro Tips

- Use a custom domain by upgrading on Render
- Monitor performance in Render dashboard
- Check logs for any errors
- Keep models updated regularly

---

## ❌ Troubleshooting

**App won't deploy?**
- Check that all required files are in the repository
- Verify `spam_model.pkl` and `vectorizer.pkl` are included
- Check deployment logs on Render dashboard

**Slow performance?**
- This is normal on free tier with cold starts
- Consider upgrading or using multiple services

**Want a custom domain?**
- Upgrade to paid plan on Render
- Or use a domain registrar + DNS forwarding

Enjoy! 🎉

