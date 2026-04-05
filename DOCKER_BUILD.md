# Build APK Using Docker

## Quick Start with Docker

If you don't want to install all the Android build tools locally, you can use Docker to build your APK in a containerized environment.

### Prerequisites
- Docker Desktop installed: https://www.docker.com/products/docker-desktop
- docker-compose (usually included with Docker Desktop)

### Build APK with Docker

#### Option 1: Using docker-compose (Easiest)

```bash
# Build the Docker image and create APK
docker-compose build apk-builder
docker-compose run apk-builder

# Or in one command:
docker-compose up --build
```

#### Option 2: Using Docker directly

```bash
# Build the image
docker build -t sms-spam-detector .

# Run the build
docker run -v ${PWD}:/app -v ${PWD}/bin:/app/bin sms-spam-detector buildozer android debug
```

#### Option 3: Interactive Docker build

```bash
# Start container with interactive shell
docker run -it -v ${PWD}:/app sms-spam-detector /bin/bash

# Inside the container, run:
buildozer android debug
```

### Build Output

After successful build, your APK will be in:
```
bin/smsspamdetector-1.0-debug.apk
```

### Install APK on Android Device

```bash
adb install bin/smsspamdetector-1.0-debug.apk
```

### Docker Troubleshooting

**Out of Memory:**
```bash
docker update --memory 8g $(docker ps -q)
```

**Build permission issues:**
Make sure files have correct permissions
```bash
chmod -R 777 bin/
```

**Clean and rebuild:**
```bash
docker-compose down
docker image rm sms-spam-detector-apk-builder
docker-compose up --build
```

### System Requirements for Docker

- **RAM:** Minimum 4GB (8GB recommended)
- **Disk Space:** At least 30GB free
- **Docker Resources:** Configure in Docker Desktop settings
  - Memory: 6-8GB
  - CPUs: 4+

### Advantages of Docker

✅ No local installation needed  
✅ Same build environment every time  
✅ Isolated from your system  
✅ Easy to share with team  
✅ Works on Windows, Mac, Linux  

### Using Docker on Windows

1. Install Docker Desktop for Windows
2. Enable WSL 2 (Windows Subsystem for Linux)
3. Run from PowerShell:

```powershell
docker-compose up --build
```

### Advanced Docker Usage

#### Build with custom parameters

```bash
docker run -e ANDROID_API_LEVEL=31 -v ${PWD}:/app sms-spam-detector buildozer android release
```

#### Push to Docker Hub

```bash
docker tag sms-spam-detector:latest yourusername/sms-spam-detector:latest
docker push yourusername/sms-spam-detector:latest
```

### Docker Notes

- First build will take 10-20 minutes (downloading SDKs, etc.)
- Subsequent builds will be faster (cached layers)
- Output APK will be in your local `bin/` folder
- All Python packages are pre-installed in the container

---

For non-Docker builds, see BUILD_INSTRUCTIONS.md

