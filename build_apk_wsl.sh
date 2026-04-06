# SMS Spam Detector APK Build Script for WSL
# Run this in WSL terminal to set up and build the APK

echo "========================================="
echo "SMS Spam Detector - APK Build Script"
echo "For WSL 2 (Windows Subsystem for Linux)"
echo "========================================="
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[!]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

# Step 1: Update system
echo ""
print_warning "Step 1: Updating system packages..."
sudo apt-get update && sudo apt-get upgrade -y
if [ $? -eq 0 ]; then
    print_status "System updated"
else
    print_error "Failed to update system"
    exit 1
fi

# Step 2: Install dependencies
echo ""
print_warning "Step 2: Installing build dependencies..."
# We need Java 17 for modern Android tools and python3-venv for virtual environments
sudo apt-get install -y python3-pip python3-dev python3-venv python3-full openjdk-17-jdk git curl wget unzip rsync cmake autoconf automake libtool libffi-dev pkg-config libsdl2-dev libsdl2-image-dev libsdl2-ttf-dev libsdl2-mixer-dev
if [ $? -eq 0 ]; then
    print_status "Dependencies installed"
else
    print_error "Failed to install dependencies"
    exit 1
fi

# --- Build Environment Setup ---

# Define project source on Windows and a build directory inside Linux
SOURCE_DIR=$(pwd)
BUILD_DIR=~/sms_spam_detector_build

echo ""
print_warning "Step 3: Preparing Linux build environment..."

# Force a full clean of Buildozer caches to resolve stubborn dependency issues
print_status "Cleaning up global Buildozer cache..."
rm -rf ~/.buildozer/cache

# Create a clean build directory in the Linux filesystem to avoid NTFS issues
print_status "Cleaning up previous build directory..."
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"

print_status "Copying project files to build directory..."
rsync -a --exclude='.git' --exclude='.venv' --exclude='bin' --exclude='build' --exclude='.buildozer' "$SOURCE_DIR/" "$BUILD_DIR/"

# Move into the build directory
cd "$BUILD_DIR"
if [ $? -ne 0 ]; then
    print_error "Failed to navigate to build directory: $BUILD_DIR"
    exit 1
fi

# Step 4: Set up Python Virtual Environment and Install Packages
echo ""
print_warning "Step 4: Setting up Python virtual environment..."

# Create a new virtual environment to avoid conflicts with system packages (PEP 668)
print_status "Creating new Python virtual environment..."
python3.11 -m venv .venv
if [ $? -ne 0 ]; then
    print_error "Failed to create Python virtual environment. Ensure python3.11-full is installed."
    exit 1
fi

# Activate the virtual environment and install packages
print_status "Activating virtual environment and installing packages..."
source .venv/bin/activate
pip install --upgrade pip setuptools wheel cython
pip install -r requirements.txt
pip install buildozer
if [ $? -eq 0 ]; then
    print_status "Python packages installed"
else
    print_error "Failed to install Python packages"
    exit 1
fi

# Step 5: Set up Android SDK
echo ""
print_warning "Step 5: Setting up Android SDK..."

# Create directory
ANDROID_HOME=~/Android/Sdk
mkdir -p "$ANDROID_HOME"

# Download Android SDK
if [ ! -f "$ANDROID_HOME/commandlinetools-linux.zip" ]; then
    print_status "Downloading Android SDK..."
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O "$ANDROID_HOME/commandlinetools-linux.zip"
    if [ $? -eq 0 ]; then
        print_status "Android SDK downloaded"
    else
        print_error "Failed to download Android SDK"
        exit 1
    fi
fi

# Extract
print_status "Extracting Android SDK..."
unzip -o -q "$ANDROID_HOME/commandlinetools-linux.zip" -d "$ANDROID_HOME"
mkdir -p "$ANDROID_HOME/cmdline-tools/latest"
mv "$ANDROID_HOME/cmdline-tools"/* "$ANDROID_HOME/cmdline-tools/latest/" 2>/dev/null || true

# Add to PATH
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools

# Make permanent
if ! grep -q "ANDROID_SDK_ROOT" ~/.bashrc; then
    echo "" >> ~/.bashrc
    echo "# Android SDK" >> ~/.bashrc
    echo "export ANDROID_SDK_ROOT=$ANDROID_HOME" >> ~/.bashrc
    echo "export PATH=\$PATH:\$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:\$ANDROID_SDK_ROOT/platform-tools" >> ~/.bashrc
fi

print_status "Android SDK set up"

# Step 6: Install SDK components
echo ""
print_warning "Step 6: Installing Android SDK components (this may take a few minutes)..."
yes | sdkmanager --sdk_root=$ANDROID_SDK_ROOT "platforms;android-31" "build-tools;31.0.0" "ndk;25.2.9519653"
if [ $? -eq 0 ]; then
    print_status "Android SDK components installed"
else
    print_error "Failed to install SDK components"
    exit 1
fi

# Step 7: Build the APK
echo ""
print_warning "Step 7: Building APK..."

if [ ! -f "$BUILD_DIR/main.py" ]; then
    print_error "main.py not found in project directory"
    print_error "Current directory: $(pwd)"
    exit 1
fi

# Clean previous builds
print_status "Cleaning previous builds..."
buildozer android clean > /dev/null 2>&1

# Build
print_status "Starting APK build (this may take 15-30 minutes)..."
# The virtual environment is active, so the 'buildozer' command will be found
buildozer android debug

if [ $? -eq 0 ]; then
    echo ""
    print_status "APK build completed successfully!"
    echo ""
    # Copy the final APK back to the original source directory on Windows
    print_status "Copying APK back to your Windows project folder..."
    mkdir -p "$SOURCE_DIR/bin"
    cp -f bin/*.apk "$SOURCE_DIR/bin/"

    echo "========================================="
    echo "Output:"
    APK_NAME=$(basename bin/*.apk)
    echo "APK location: $SOURCE_DIR/bin/$APK_NAME"
    echo "File size: $(ls -lh "$SOURCE_DIR/bin/$APK_NAME" 2>/dev/null | awk '{print $5}')"
    echo "========================================="
    echo ""
    print_status "Next step: Transfer APK to Android device and install"
    echo "Windows path: C:\\Users\\PRASANNA\\Desktop\\New folder\\bin\\$APK_NAME"
    echo ""
else
    print_error "APK build failed. Check the output above for errors."
    exit 1
fi
