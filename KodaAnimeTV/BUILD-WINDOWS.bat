@echo off
cd /d "%~dp0"
where gradle >nul 2>&1
if errorlevel 1 (
 echo Instale Android Studio e abra esta pasta nele. Use Build ^> Build APK(s).
 pause
 exit /b 1
)
gradle :app:assembleDebug
if errorlevel 1 (pause & exit /b 1)
echo APK: app\build\outputs\apk\debug\app-debug.apk
pause
