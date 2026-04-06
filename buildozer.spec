[app]

# Title of your application
title = SMS Spam Detector

# Package name
package.name = smsspamdetector

# Package domain
package.domain = org.smsspam

# Source directory
source.dir = .

# Source files to include
source.include_exts = py,png,jpg,kv,atlas,pkl,csv

# Version
version = 1.0

# Requirements
requirements = python3,kivy,pyjnius,nltk,scikit-learn,numpy==1.21.6,gTTS,pillow

# Permissions
android.permissions = INTERNET,WRITE_EXTERNAL_STORAGE,READ_EXTERNAL_STORAGE,RECORD_AUDIO

# Orientation
orientation = portrait

# Fullscreen
fullscreen = 0

# Android API level
android.api = 31
android.minapi = 24
android.ndk = 25b

# Gradle options
android.gradle_options = org.gradle.jvmargs=-Xmx4096m

# Architecture
android.archs = arm64-v8a,armeabi-v7a

# Services to add
android.features = android.hardware.microphone

# Force buildozer to use the latest development branch of python-for-android
p4a.branch = develop
android.p4a_args = --use-prebuilt-version-for=numpy

[buildozer]

# Log level (0 = error only, 1 = info, 2 = debug)
log_level = 2

# Display warning on partial build
warn_on_root = 1
