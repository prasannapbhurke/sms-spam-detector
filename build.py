#!/usr/bin/env python3
"""
SMS Spam Detector - Android APK Builder
Simplified build script for cross-platform support
"""

import subprocess
import sys
import os
import shutil
from pathlib import Path

def run_command(cmd, description):
    """Run a command and handle errors"""
    print(f"\n{'='*50}")
    print(f"[*] {description}")
    print(f"{'='*50}")

    try:
        result = subprocess.run(cmd, shell=True, check=True)
        return True
    except subprocess.CalledProcessError as e:
        print(f"\n[!] Error: {description} failed")
        print(f"[!] Return code: {e.returncode}")
        return False

def check_requirements():
    """Check if required tools are installed"""
    print("[*] Checking requirements...")

    # Check Python
    try:
        import kivy
        print("[✓] Kivy installed")
    except ImportError:
        print("[✗] Kivy not installed. Run: pip install -r requirements.txt")
        return False

    # Check buildozer
    try:
        subprocess.run(["buildozer", "--version"], capture_output=True, check=True)
        print("[✓] Buildozer installed")
    except FileNotFoundError:
        print("[✗] Buildozer not installed. Run: pip install buildozer")
        return False

    # Check Java
    try:
        subprocess.run(["java", "-version"], capture_output=True, check=True)
        print("[✓] Java installed")
    except FileNotFoundError:
        print("[!] Java not found. Please install JDK 11+")

    # Check Android SDK
    if not os.getenv("ANDROID_SDK_ROOT"):
        print("[!] ANDROID_SDK_ROOT not set. Build may fail.")
    else:
        print(f"[✓] ANDROID_SDK_ROOT: {os.getenv('ANDROID_SDK_ROOT')}")

    return True

def verify_project_files():
    """Verify all necessary project files exist"""
    print("\n[*] Verifying project files...")

    required_files = [
        "main.py",
        "buildozer.spec",
        "spam_model.pkl",
        "vectorizer.pkl",
        "requirements.txt"
    ]

    missing = []
    for file in required_files:
        if not os.path.exists(file):
            missing.append(file)
            print(f"[✗] Missing: {file}")
        else:
            print(f"[✓] Found: {file}")

    if missing:
        print(f"\n[!] Missing files: {', '.join(missing)}")
        return False

    return True

def clean_build():
    """Clean previous build artifacts"""
    print("\n[*] Cleaning previous builds...")

    dirs_to_clean = [".buildozer", "build", "bin"]
    for dir_name in dirs_to_clean:
        if os.path.exists(dir_name):
            try:
                shutil.rmtree(dir_name)
                print(f"[✓] Removed {dir_name}/")
            except Exception as e:
                print(f"[!] Could not remove {dir_name}: {e}")

def build_apk(release=False):
    """Build APK using Buildozer"""
    build_type = "release" if release else "debug"

    if not run_command(
        f"buildozer android {build_type}",
        f"Building {build_type.upper()} APK"
    ):
        return False

    # Find and report APK location
    bin_dir = Path("bin")
    if bin_dir.exists():
        apks = list(bin_dir.glob("*.apk"))
        if apks:
            latest_apk = max(apks, key=os.path.getctime)
            print(f"\n[✓] APK created successfully!")
            print(f"[✓] Location: {latest_apk}")
            print(f"[✓] Size: {latest_apk.stat().st_size / (1024*1024):.2f} MB")
            return True

    return False

def main():
    """Main build process"""
    print("\n" + "="*50)
    print("SMS SPAM DETECTOR - ANDROID APK BUILDER")
    print("="*50)

    # Parse arguments
    clean = "--clean" in sys.argv
    release = "--release" in sys.argv
    skip_checks = "--skip-checks" in sys.argv

    # Verify requirements
    if not skip_checks:
        if not check_requirements():
            print("\n[!] Requirements not met. Please install missing packages.")
            print("[*] Run: python setup.py install")
            sys.exit(1)

        if not verify_project_files():
            print("\n[!] Project files incomplete.")
            sys.exit(1)

    # Clean if requested
    if clean:
        clean_build()

    # Build APK
    if build_apk(release):
        print("\n[✓] Build completed successfully!")

        if release:
            print("\n[!] Next step: Sign the release APK for Play Store distribution")
        else:
            print("\n[*] Install on device: adb install bin/*.apk")

        sys.exit(0)
    else:
        print("\n[✗] Build failed!")
        print("\n[*] Troubleshooting tips:")
        print("    - Ensure Android SDK and JDK are installed")
        print("    - Set ANDROID_SDK_ROOT and JAVA_HOME environment variables")
        print("    - Try building on WSL or Linux for better compatibility")
        sys.exit(1)

if __name__ == "__main__":
    main()

