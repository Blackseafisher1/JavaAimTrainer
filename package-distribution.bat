@echo off
chcp 65001 >nul
title Simple AimTrainer Package
color 0A

echo ===========================================
echo    Simple AimTrainer Package Creator
echo ===========================================
echo.

REM 1. Build JAR
echo [1/4] Building JAR...
call gradlew.bat fatJar

REM 2. Create folder
echo [2/4] Creating folder...
if exist "AimTrainer_Final" rmdir /s /q "AimTrainer_Final"
mkdir "AimTrainer_Final"

REM 3. Copy files
echo [3/4] Copying files...
copy "build\libs\AimTrainer-fat.jar" "AimTrainer_Final\AimTrainer.jar"

if exist "src\main\resources" (
    xcopy /E /I /Y "src\main\resources" "AimTrainer_Final\resources\"
)

REM 4. Create scripts
echo [4/4] Creating launcher...
echo @echo off > "AimTrainer_Final\run.bat"
echo echo Starting AimTrainer... >> "AimTrainer_Final\run.bat"
echo echo. >> "AimTrainer_Final\run.bat"
echo echo IMPORTANT: You need Java 21 or higher installed. >> "AimTrainer_Final\run.bat"
echo echo If you have Java installed, the game will start. >> "AimTrainer_Final\run.bat"
echo echo If not, download from: https://adoptium.net/ >> "AimTrainer_Final\run.bat"
echo echo. >> "AimTrainer_Final\run.bat"
echo timeout /t 3 /nobreak >nul >> "AimTrainer_Final\run.bat"
echo java -Xmx2G -jar AimTrainer.jar >> "AimTrainer_Final\run.bat"
echo pause >> "AimTrainer_Final\run.bat"

echo.
echo ===========================================
echo ✅ BASIC PACKAGE CREATED!
echo ===========================================
echo.
echo Folder: AimTrainer_Final
echo.
echo MANUAL STEP REQUIRED:
echo 1. Download JRE 21 from:
echo    https://adoptium.net/temurin/releases/?version=21
echo 2. Choose: JRE (not JDK) -> Windows x64
echo 3. Extract ZIP to: AimTrainer_Final\jre\
echo.
echo Then use Launch4j to create .exe
echo.
pause