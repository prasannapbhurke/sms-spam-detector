# 🔨 APK Build on Windows - Complete Guide

## Summary

Building Android APKs on Windows directly is complex. I've created helper tools to make it easier.

---

## ⚡ Quick Start (Choose ONE method)

### **Method 1: Using WSL 2 (BEST for Windows Users) ✅ RECOMMENDED**

#### Step 1: Install WSL 2 (First time only)
Open **PowerShell as Administrator** and run:
```powershell
wsl --install
```
- Restart your computer
- Ubuntu will launch automatically
- Create a username and password

#### Step 2: Build the APK
1. Open **Ubuntu terminal** (search "Ubuntu" in Start menu)
2. Navigate to the project:
```bash
cd /mnt/c/Users/PRASANNA/Desktop/"New folder"
```

3. Run the build script:
```bash
bash build_apk_wsl.sh
```

**⏱️ Time: 20-40 minutes (first time includes SDK download)**

**Output:** `bin/smsspamdetector-1.0-debug.apk`

---

### **Method 2: Using PowerShell Helper Script (Interactive)**

Run this in PowerShell:
```powershell
cd "C:\Users\PRASANNA\Desktop\New folder"
./build_apk.ps1
```

This script will:
- Check if WSL 2 is installed
- Guide you through setup if needed
- Let you choose between full build or quick rebuild

---

### **Method 3: Online APK Builder (Easiest, No Setup)**

Use an online service to build APK:
1. Visit: https://buildozer.cloud/ or similar online builder
2. Upload your project files
3. Click "Build APK"
4. Download the APK when ready

**Pros:** ✅ No local setup  
**Cons:** ⚠️ Code goes to internet, size/time limits

---

## 📋 What Each Script Does

### `build_apk_wsl.sh`
- Comprehensive build script for WSL
- Handles all setup automatically
- Downloads Android SDK, JDK, build tools
- Builds the APK
- Ideal for fresh WSL installation

**Usage in WSL:**
```bash
bash build_apk_wsl.sh
```

### `build_apk.ps1`
- Windows PowerShell helper script
- Interactive menu-driven
- Can detect WSL and guide setup
- Easier for Windows users

**Usage in PowerShell:**
```powershell
./build_apk.ps1
```

### `BUILD_APK_WINDOWS.md`
- Detailed documentation
- All methods explained
- Troubleshooting tips
- Advanced options

---

## 🎯 Step-by-Step: WSL Method (RECOMMENDED)

### Prerequisites Check
- [ ] Windows 10 version 2004+ or Windows 11
- [ ] At least 30 GB free disk space
- [ ] 8 GB RAM minimum (16 GB recommended)

### Installation Steps

**Step 1: Enable WSL 2**
```powershell
# Run as Administrator
wsl --install
# Then restart your computer
```

**Step 2: Launch Ubuntu**
- Search "Ubuntu" in Windows Start menu
- First launch will take 2-3 minutes
- Create username and password (can be anything)

**Step 3: Build APK**
```bash
cd /mnt/c/Users/PRASANNA/Desktop/"New folder"
bash build_apk_wsl.sh
```

The script will:
1. ✓ Install build tools (buildozer, JDK, etc.)
2. ✓ Download Android SDK
3. ✓ Install SDK components
4. ✓ Build the APK
5. ✓ Show output location

**Step 4: Wait for Build**
- Build takes 15-30 minutes first time
- Faster on subsequent builds (cached)

**Step 5: Get Your APK**
After successful build, APK is at:
```
C:\Users\PRASANNA\Desktop\New folder\bin\smsspamdetector-1.0-debug.apk
```

---

## 📱 Installing APK on Android Phone

### Method 1: USB Cable (Requires ADB)
```bash
adb install -r bin/smsspamdetector-1.0-debug.apk
```

### Method 2: File Transfer (Easiest)
1. Copy `smsspamdetector-1.0-debug.apk` to your phone
2. Open file manager on phone
3. Tap the APK file
4. Follow installation prompts

### Method 3: Bluetooth/Email
Send APK file to your phone via Bluetooth, email, or file sync

---

## ⚠️ Troubleshooting

### WSL Installation Issues
```bash
# Reinstall WSL
wsl --uninstall
wsl --install

# Update WSL
wsl --update
```

### Build Fails - Out of Memory
```bash
# Increase build memory in WSL
# Edit buildozer.spec and change:
android.gradle_options = org.gradle.jvmargs=-Xmx8192m
```

### Missing Model Files
```bash
# Verify files exist before building
ls spam_model.pkl
ls vectorizer.pkl
```

### APK Not Generated
Check the build log for errors:
```bash
buildozer android debug
# Read the error messages carefully
# See TROUBLESHOOTING.md for solutions
```

---

## 📊 Comparison of Methods

| Method | Ease | Time | Reliability | Files Needed |
|--------|------|------|-------------|-------------|
| WSL 2 | ⭐⭐⭐ | 20-40 min | ✅ High | None (auto-download) |
| PowerShell | ⭐⭐⭐ | 20-40 min | ✅ High | None (auto-download) |
| Online | ⭐⭐⭐⭐⭐ | 5-10 min | ⚠️ Medium | Project files |
| Docker | ⭐⭐⭐ | 30 min | ✅ High | Docker installed |
| Windows Native | ⭐ | 2+ hours | ⚠️ Low | JDK, SDK, NDK |

---

## 🔧 Advanced: Quick Rebuild After First Build

Once everything is set up in WSL, for quick rebuilds:

```bash
cd /mnt/c/Users/PRASANNA/Desktop/"New folder"
buildozer android clean
buildozer android debug
```

Takes only 3-5 minutes on subsequent builds.

---

## 🆘 Getting Help

Check these files in order:
1. **This file** - General guide
2. **BUILD_APK_WINDOWS.md** - Detailed Windows-specific guide
3. **BUILD_INSTRUCTIONS.md** - Original build instructions
4. **TROUBLESHOOTING.md** - Common issues and solutions

---

## 📝 Project Files Summary

- **main.py** - Kivy app source code
- **buildozer.spec** - APK build configuration
- **requirements.txt** - Python dependencies
- **spam_model.pkl** - ML model (pre-trained)
- **vectorizer.pkl** - TF-IDF vectorizer
- **build_apk_wsl.sh** - WSL build automation script
- **build_apk.ps1** - Windows PowerShell helper

---

## ✅ You're Ready!

Your project is ready to build. Choose a method above and follow the steps.

**Recommended:** Use `build_apk_wsl.sh` in WSL for most reliable results.

---

**Last Updated:** April 2026  
**Project:** SMS Spam Detector Mobile App  
**Target:** Android 5.0+ (API 21+)  

