@echo off
REM ChatNexus Docker Quick Test Script for Windows
REM Run this to test Docker locally in 2 minutes

echo.
echo 🐳 ChatNexus Docker Test
echo ========================
echo.

REM Check if Docker is installed
docker --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker is not installed. Please install Docker Desktop first.
    pause
    exit /b 1
)

echo ✅ Docker found
echo.

REM Build Docker image
echo 🔨 Building Docker image...
docker build -t chatnexus:latest .

if errorlevel 1 (
    echo ❌ Docker build failed!
    pause
    exit /b 1
)

echo ✅ Docker image built successfully
echo.

REM Show image info
echo 📊 Image Information:
docker images chatnexus:latest

echo.
echo ✅ Docker setup is working!
echo.
echo Next steps:
echo 1. Deploy to Railway: Follow README.md section '🚂 Deploy to Railway'
echo 2. Or run locally with: docker run -d -p 8080:8080 chatnexus:latest
echo.
pause

