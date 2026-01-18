@echo off 
echo Starting AimTrainer... 
echo. 
echo IMPORTANT: You need Java 21 or higher installed. 
echo If you have Java installed, the game will start. 
echo If not, download from: https://adoptium.net/ 
echo. 
timeout /t 3 /nobreak 
java -Xmx2G -jar AimTrainer.jar 
pause 
