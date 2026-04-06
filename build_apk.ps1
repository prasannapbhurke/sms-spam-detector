# SMS Spam Detector - Windows Build Helper
# This script helps you build the APK on Windows via WSL 2

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "SMS Spam Detector - APK Build Helper" -ForegroundColor Cyan
Write-Host "Windows Subsystem for Linux (WSL 2)" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Check if WSL is installed
Write-Host "Checking if WSL 2 is installed..." -ForegroundColor Yellow
$wslCheck = wsl --version 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "[✓] WSL 2 is installed" -ForegroundColor Green
    Write-Host $wslCheck
} else {
    Write-Host "[✗] WSL 2 is not installed" -ForegroundColor Red
    Write-Host ""
    Write-Host "Installing WSL 2..." -ForegroundColor Yellow
    Write-Host ""

    # Install WSL
    wsl --install

    Write-Host ""
    Write-Host "=========================================" -ForegroundColor Yellow
    Write-Host "WSL installation initiated!" -ForegroundColor Green
    Write-Host "=========================================" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "After installation completes:" -ForegroundColor Cyan
    Write-Host "1. Restart your computer" -ForegroundColor White
    Write-Host "2. Ubuntu terminal will open automatically" -ForegroundColor White
    Write-Host "3. Create a username and password" -ForegroundColor White
    Write-Host "4. Then run this script again" -ForegroundColor White
    Write-Host ""

    Read-Host "Press Enter after WSL is installed and you've created an account"
}

# Check if build script exists
$buildScript = "build_apk_wsl.sh"
if (!(Test-Path $buildScript)) {
    Write-Host ""
    Write-Host "Note: Build script not found. Creating it..." -ForegroundColor Yellow
    # The script should already exist from previous creation
}

# Show menu
Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Choose an option:" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Build APK (Full setup + build)" -ForegroundColor Yellow
Write-Host "2. Quick build (if already set up)" -ForegroundColor Yellow
Write-Host "3. Show instructions" -ForegroundColor Yellow
Write-Host "4. Exit" -ForegroundColor Yellow
Write-Host ""

$choice = Read-Host "Enter your choice (1-4)"

switch ($choice) {
    "1" {
        Write-Host ""
        Write-Host "Starting full build process in WSL..." -ForegroundColor Green
        Write-Host ""

        # Convert Windows path to WSL path
        $projectPath = Get-Location
        $wslPath = $projectPath -replace '\\', '/' -replace '^C:', '/mnt/c'

        # Copy build script to WSL and run
        wsl bash "$($wslPath)/build_apk_wsl.sh"
    }

    "2" {
        Write-Host ""
        Write-Host "Starting quick build in WSL..." -ForegroundColor Green
        Write-Host ""

        wsl bash -c "
            cd /mnt/c/Users/PRASANNA/Desktop/'New folder'
            echo 'Cleaning previous builds...'
            buildozer android clean > /dev/null 2>&1
            echo 'Building APK...'
            buildozer android debug
        "
    }

    "3" {
        Write-Host ""
        Write-Host "=========================================" -ForegroundColor Green
        Write-Host "Build Instructions" -ForegroundColor Green
        Write-Host "=========================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "METHOD 1: Quick Build (Recommended)" -ForegroundColor Yellow
        Write-Host "1. Press Windows key, type 'Ubuntu'" -ForegroundColor White
        Write-Host "2. Open Ubuntu terminal" -ForegroundColor White
        Write-Host "3. Copy and paste the commands below:" -ForegroundColor White
        Write-Host ""
        Write-Host "cd /mnt/c/Users/PRASANNA/Desktop/'New folder'" -ForegroundColor Cyan
        Write-Host "bash build_apk_wsl.sh" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "Or for quick rebuild if already set up:" -ForegroundColor Yellow
        Write-Host "buildozer android debug" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "METHOD 2: Manual Setup" -ForegroundColor Yellow
        Write-Host "See BUILD_APK_WINDOWS.md for detailed instructions" -ForegroundColor White
        Write-Host ""
    }

    default {
        Write-Host "Exiting..." -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Green
Write-Host "For more information, see:" -ForegroundColor Green
Write-Host "- BUILD_APK_WINDOWS.md" -ForegroundColor White
Write-Host "- BUILD_INSTRUCTIONS.md" -ForegroundColor White
Write-Host "- TROUBLESHOOTING.md" -ForegroundColor White
Write-Host "=========================================" -ForegroundColor Green

