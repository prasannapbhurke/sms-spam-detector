[app]

# Title of your application
title = AI Spam Guard

# Package name
package.name = aispamguard

# Package domain
package.domain = org.aispam

# Source directory
source.dir = .

# Source files to include
source.include_exts = py,png,jpg,kv,atlas

# Version
version = 1.1

# Requirements
# REMOVED scikit-learn and numpy because they fail to build on Android.
# ADDED requests to communicate with your Railway API.
requirements = python3,kivy,requests,urllib3,idna,chardet,certifi

# Permissions
android.permissions = INTERNET,RECEIVE_SMS

# Orientation
orientation = portrait

# Fullscreen
fullscreen = 0

# Android API level
android.api = 33
android.minapi = 24
android.ndk = 25b

# Gradle options
android.gradle_options = org.gradle.jvmargs=-Xmx4096m

# Architecture
android.archs = arm64-v8a,armeabi-v7a

# Force buildozer to use the latest development branch
p4a.branch = develop

[buildozer]
log_level = 2
warn_on_root = 1
