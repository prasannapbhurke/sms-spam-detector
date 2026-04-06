@echo off
REM SMS Spam Detector - Android APK Build Script
REM Run this after setup.bat to build the APK

echo ====================================
echo SMS Spam Detector - Build Script
echo ====================================
echo.

REM Check if buildozer is installed
buildozer --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Buildozer is not installed
    echo Please run setup.bat first
    pause
    exit /b 1
)

REM Check if Java is installed
java -version >nul 2>&1
if errorlevel 1 (
    echo WARNING: Java is not installed or not in PATH
    echo Please install JDK 11+ and set JAVA_HOME environment variable
    pause
    exit /b 1
)

REM Check if Android SDK is installed
if not defined ANDROID_SDK_ROOT (
    echo WARNING: ANDROID_SDK_ROOT is not set
    echo Please set ANDROID_SDK_ROOT environment variable to your Android SDK path
    pause
    exit /b 1
)

echo.
echo Building Debug APK...
echo This may take 10-20 minutes on first build
echo.

buildozer android debug

if errorlevel 1 (
    echo.
    echo ERROR: Build failed!
    echo.
    echo Troubleshooting tips:
    echo - Ensure Java and Android SDK are properly installed
    echo - Set JAVA_HOME and ANDROID_SDK_ROOT environment variables
    echo - Try building on WSL or Linux for better compatibility
    echo.
    pause
    exit /b 1
)

echo.
echo ====================================
echo Build Complete!
echo ====================================
echo.
echo APK Location: bin\smsspamdetector-1.0-debug.apk
echo.
echo Next steps:
echo - Install on Android device: adb install bin\smsspamdetector-1.0-debug.apk
echo - Or transfer the APK file to your phone and install manually
echo.
pause

