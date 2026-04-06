@echo off
REM Initialize Git repository for deployment
git init
git add .
git commit -m "Initial commit: SMS Spam Detector webapp"
echo.
echo Git repository initialized!
echo.
echo Next steps:
echo 1. Create a repository on GitHub (https://github.com/new)
echo 2. Run: git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git
echo 3. Run: git branch -M main
echo 4. Run: git push -u origin main
echo 5. Go to https://render.com and deploy!
pause
