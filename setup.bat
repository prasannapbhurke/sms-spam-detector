@echo off
REM SMS Spam Detector - Android APK Build Setup Script
REM Run this script to install all dependencies on Windows

echo ====================================
echo SMS Spam Detector - Setup Script
echo ====================================
echo.

REM Check if Python is installed
python --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Python is not installed or not in PATH
    echo Please install Python 3.9+ from https://www.python.org/
    pause
    exit /b 1
)

echo [1/4] Upgrading pip...
python -m pip install --upgrade pip
if errorlevel 1 echo WARNING: pip upgrade failed

echo.
echo [2/4] Installing Buildozer and dependencies...
pip install buildozer cython pyjnius plyer
if errorlevel 1 (
    echo ERROR: Failed to install buildozer
    pause
    exit /b 1
)

echo.
echo [3/4] Installing Python requirements...
pip install -r requirements.txt
if errorlevel 1 (
    echo ERROR: Failed to install requirements
    pause
    exit /b 1
)

echo.
echo [4/4] Verifying installation...
python -c "import kivy; import nltk; import sklearn; print('✓ All packages installed successfully!')"
if errorlevel 1 (
    echo ERROR: Verification failed
    pause
    exit /b 1
)

echo.
echo ====================================
echo Setup Complete!
echo ====================================
echo.
echo Next steps:
echo 1. Ensure JAVA_HOME and ANDROID_SDK_ROOT are set
echo 2. Run: buildozer android debug
echo.
echo For detailed instructions, see BUILD_INSTRUCTIONS.md
echo.
pause

