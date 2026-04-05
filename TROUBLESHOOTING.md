# 🔧 Troubleshooting Guide

## Common Issues & Solutions

### Installation & Setup Issues

#### 1. "buildozer: command not found"

**Cause:** Buildozer not installed or not in PATH

**Solutions:**
```bash
# Install buildozer
pip install buildozer

# Or run with Python module
python -m buildozer --version

# Verify installation
pip show buildozer
```

---

#### 2. "Java not found" or "JAVA_HOME not set"

**Cause:** Java Development Kit not installed

**Windows Solution:**
1. Download JDK 11 from https://www.oracle.com/java/technologies/javase-jdk11-downloads.html
2. Install it (default: C:\Program Files\Java\jdk-11)
3. Set JAVA_HOME:
   ```
   Right-click This PC → Properties → Advanced system settings
   → Environment Variables → New:
   Variable name: JAVA_HOME
   Variable value: C:\Program Files\Java\jdk-11
   ```
4. Restart terminal/IDE
5. Verify: `java -version`

**Linux Solution:**
```bash
sudo apt-get install openjdk-11-jdk
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk
```

**Mac Solution:**
```bash
brew install openjdk@11
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
```

---

#### 3. "ANDROID_SDK_ROOT not set"

**Cause:** Android SDK not found

**Solutions:**
1. Install Android Studio from https://developer.android.com/studio
2. Set environment variable:
   ```
   Windows: ANDROID_SDK_ROOT = C:\Users\YourName\AppData\Local\Android\Sdk
   Linux: export ANDROID_SDK_ROOT=$HOME/Android/Sdk
   ```
3. Verify SDK tools:
   ```bash
   ls $ANDROID_SDK_ROOT/platforms
   ls $ANDROID_SDK_ROOT/platform-tools
   ```

---

### Build Errors

#### 4. "Build failed on Windows"

**Most Common Cause:** Windows-specific build tools missing

**Solutions:**
1. **Use WSL (Windows Subsystem for Linux)** - Recommended
   ```bash
   wsl --install
   # Inside WSL:
   sudo apt-get install python3-pip
   pip install buildozer
   ```

2. **Use Linux VM** - VirtualBox with Ubuntu

3. **Use Docker** - Easiest option
   ```bash
   docker-compose up --build
   ```

4. **Install build tools:**
   ```bash
   pip install cython setuptools
   ```

---

#### 5. "Gradle build failed"

**Cause:** Gradle configuration issue

**Solutions:**
```bash
# Clean previous build
buildozer android clean

# Increase Gradle memory
export GRADLE_OPTS="-Xmx4096m"

# Try building again
buildozer android debug
```

---

#### 6. "NDK not found"

**Cause:** Android NDK (Native Development Kit) missing

**Solutions:**
```bash
# Update buildozer.spec:
android.ndk = 25b  # Update to available version

# Or install NDK:
# Via Android Studio: SDK Manager → NDK (Side by side)
```

---

### Runtime Errors

#### 7. "ModuleNotFoundError: No module named 'kivy'"

**Cause:** Dependencies not installed

**Solution:**
```bash
pip install -r requirements.txt
```

---

#### 8. "Model not found" error on APK

**Cause:** spam_model.pkl or vectorizer.pkl not included

**Solutions:**
1. Verify files in project directory:
   ```bash
   ls spam_model.pkl
   ls vectorizer.pkl
   ```

2. Update buildozer.spec:
   ```
   source.include_exts = py,pkl,csv,png,jpg,kv,atlas
   ```

3. Rebuild APK:
   ```bash
   buildozer android clean
   buildozer android debug
   ```

---

#### 9. "Text-to-speech not working"

**Cause:** Google TTS app not installed on device

**Solutions:**
1. Install on Android: Settings → Apps → Google Play Store
2. Search "Google Text-to-Speech" and install
3. Set as default: Settings → Accessibility → Text-to-Speech options

**Or:** App works without TTS (graceful degradation)

---

### Performance Issues

#### 10. "APK takes too long to launch"

**Normal:** First launch takes 30-60 seconds
- Python runtime is being extracted
- NLTK stopwords being loaded
- Subsequent launches faster

**Not normal:** Taking > 2 minutes
- Check device storage space
- Restart phone
- Reinstall APK

---

#### 11. "App crashes on start"

**Solutions:**
1. Check device logs:
   ```bash
   adb logcat | grep smsspamdetector
   ```

2. Clear app cache:
   ```bash
   adb shell pm clear org.smsspam.smsspamdetector
   ```

3. Reinstall:
   ```bash
   adb uninstall org.smsspam.smsspamdetector
   adb install bin/smsspamdetector-1.0-debug.apk
   ```

4. Check model files are present

---

#### 12. "Out of memory" during build

**Cause:** Buildozer needs more RAM

**Solutions:**
```bash
# Increase Gradle heap
export GRADLE_OPTS="-Xmx8192m"

# Or edit buildozer.spec:
# android.gradle_options = org.gradle.jvmargs=-Xmx8192m
```

---

### Disk Space Issues

#### 13. "No space left on device"

**Cause:** Not enough disk space for build

**Solutions:**
```bash
# Check disk space
df -h

# Clean build artifacts
buildozer android clean

# Remove old builds
rm -rf .buildozer build bin

# Free up space, then retry
```

**Minimum required:** 30 GB free

---

### Docker Issues

#### 14. "Docker build fails"

**Common causes & solutions:**

```bash
# Out of memory
docker update --memory 8g $(docker ps -q)

# Permission denied
chmod -R 777 bin/

# Network issues
docker-compose build --no-cache

# Full rebuild
docker system prune -a
docker-compose up --build
```

---

#### 15. "APK not in bin/ folder after Docker build"

**Solutions:**
1. Check Docker volume mounting:
   ```bash
   docker volume ls
   ```

2. Run interactive build:
   ```bash
   docker run -it -v ${PWD}:/app sms-spam-detector /bin/bash
   # Inside: buildozer android debug
   ```

3. Copy from container:
   ```bash
   docker cp container_id:/app/bin/. ./bin/
   ```

---

### Installation Issues

#### 16. "adb: command not found"

**Cause:** Android Debug Bridge not in PATH

**Solutions:**

**Windows:**
```bash
# Add to PATH:
C:\Users\YourName\AppData\Local\Android\Sdk\platform-tools

# Or use full path:
C:\Users\YourName\AppData\Local\Android\Sdk\platform-tools\adb install bin\*.apk
```

**Linux/Mac:**
```bash
export PATH=$PATH:$ANDROID_SDK_ROOT/platform-tools
# Or create symlink:
ln -s $ANDROID_SDK_ROOT/platform-tools/adb /usr/local/bin/adb
```

---

#### 17. "adb: device not found"

**Cause:** Phone not connected or USB debugging disabled

**Solutions:**
1. Enable USB Debugging:
   - Settings → Developer options → USB Debugging (enable)
   - If no Developer options: Settings → About → Tap Build Number 7 times

2. Check connection:
   ```bash
   adb devices
   ```

3. Reconnect USB cable in different mode:
   - Try "File Transfer" mode
   - Try "Camera" mode
   - Try different USB port

---

#### 18. "APK installation fails"

**Cause:** Existing app version conflicts

**Solutions:**
```bash
# Remove old version first
adb uninstall org.smsspam.smsspamdetector

# Then install
adb install bin/smsspamdetector-1.0-debug.apk

# Or force replace
adb install -r bin/smsspamdetector-1.0-debug.apk
```

---

### Model & Algorithm Issues

#### 19. "Detection accuracy seems off"

**Check:**
1. Is spam_model.pkl the correct file?
2. Is vectorizer.pkl matching the model?
3. Try on desktop version to compare

**Solutions:**
```bash
# Verify file integrity
file spam_model.pkl
file vectorizer.pkl

# Compare with desktop (sms1.py)
python sms1.py
```

---

#### 20. "Different results between desktop and mobile"

**Possible causes:**
1. Different NLTK stopwords version
2. Model file corruption
3. Preprocessing differences

**Solutions:**
1. Rebuild both versions with same Python version
2. Copy model files from working desktop version
3. Check NLTK data version:
   ```python
   import nltk
   nltk.download('stopwords')
   ```

---

## Getting Help

### Before asking for help, check:

1. ✅ Read BUILD_INSTRUCTIONS.md
2. ✅ Check above solutions
3. ✅ Review error message carefully
4. ✅ Search similar issues online
5. ✅ Check file permissions and paths

### Useful diagnostic commands:

```bash
# Check Java
java -version
echo %JAVA_HOME%

# Check Android SDK
echo %ANDROID_SDK_ROOT%
ls %ANDROID_SDK_ROOT%\platforms

# Check Python
python --version
pip show buildozer kivy

# Check Android device
adb devices
adb shell getprop ro.build.version.release

# View app logs
adb logcat -c
adb logcat | grep smsspamdetector

# Check file sizes
ls -lh bin/
du -sh .buildozer/
```

### Report Issues With:
- Your OS (Windows/Linux/Mac)
- Python version
- Error message (full output)
- Steps to reproduce
- Output of diagnostic commands above

---

## Quick Recovery Steps

If everything breaks:

```bash
# Full clean rebuild
buildozer android clean
rm -rf bin build .buildozer
pip install --upgrade buildozer
buildozer android debug
```

---

**Still stuck?** Check the main documentation files or try Docker build method as alternative!

