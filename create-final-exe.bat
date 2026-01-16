@echo off
chcp 65001 >nul
title Create Final AimTrainer.exe
color 0A

echo ===========================================
echo    Creating Final AimTrainer.exe
echo ===========================================
echo.

REM Check Launch4j
set LAUNCH4J="C:\Program Files (x86)\Launch4j\launch4j.exe"
if not exist %LAUNCH4J% (
    echo ❌ Launch4j not found!
    echo Please install from: http://launch4j.sourceforge.net/
    pause
    exit /b 1
)

REM Package distribution first
echo [1] Packaging distribution...
call package-distribution.bat

REM Navigate to distribution folder
cd AimTrainer_Final

echo [2] Creating .exe with Launch4j...
%LAUNCH4J% launch4j-config.xml

if exist "AimTrainer.exe" (
    echo.
    echo ✅ SUCCESS! AimTrainer.exe created!
    echo 📍 Location: %CD%\AimTrainer.exe
    echo 📦 File size: %~z0 bytes
    
    REM Test if it works
    echo.
    echo [3] Testing .exe...
    start "" "AimTrainer.exe"
    
    echo.
    echo ===========================================
    echo 🎉 READY TO DISTRIBUTE!
    echo ===========================================
    echo.
    echo You can now distribute the ENTIRE folder:
    echo   %CD%\
    echo.
    echo Or just these essential files:
    echo   - AimTrainer.exe
    echo   - jre\ folder
    echo   - resources\ folder
    echo.
    echo Users can simply double-click AimTrainer.exe
    echo No Java installation required!
) else (
    echo ❌ Failed to create .exe
    echo Check Launch4j configuration
)

cd ..
echo.
pause