@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

echo =====================================================
echo   KenPhoto AI v1.2.0 - Build APK Debug cho Windows
 echo =====================================================

echo [1/5] Tim Android SDK...
if not "%ANDROID_SDK_ROOT%"=="" set "ANDROID_HOME=%ANDROID_SDK_ROOT%"
if "%ANDROID_HOME%"=="" if exist "%LOCALAPPDATA%\Android\Sdk" set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
if "%ANDROID_HOME%"=="" (
  echo [LOI] Chua tim thay Android SDK.
  echo Mo Android Studio ^> More Actions/Tools ^> SDK Manager va cai:
  echo   - Android SDK Platform 36
  echo   - Android SDK Build-Tools 36.0.0
  echo Sau do chay lai file nay.
  pause
  exit /b 1
)

if not exist "%ANDROID_HOME%\platforms\android-36\android.jar" (
  echo [LOI] Thieu Android SDK Platform 36 tai:
  echo %ANDROID_HOME%\platforms\android-36
  echo Hay cai Platform 36 trong Android Studio SDK Manager.
  pause
  exit /b 1
)
if not exist "%ANDROID_HOME%\build-tools\36.0.0\aapt2.exe" (
  echo [LOI] Thieu Android SDK Build-Tools 36.0.0.
  echo Hay cai Build-Tools 36.0.0 trong Android Studio SDK Manager.
  pause
  exit /b 1
)

echo [2/5] Tim Java...
where java >nul 2>nul
if errorlevel 1 (
  if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" (
    set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
  ) else if exist "%ProgramFiles%\Android\Android Studio\jre\bin\java.exe" (
    set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jre"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
  ) else (
    echo [LOI] Chua tim thay Java. Android Studio thuong da kem JDK.
    echo Hay mo project bang Android Studio mot lan roi chay lai.
    pause
    exit /b 1
  )
)
java -version

echo [3/5] Chuan bi Gradle 9.5.0...
set "GRADLE_DIR=%CD%\.gradle-local\gradle-9.5.0"
set "GRADLE_ZIP=%CD%\.gradle-local\gradle-9.5.0-bin.zip"
if not exist "%GRADLE_DIR%\bin\gradle.bat" (
  if not exist "%CD%\.gradle-local" mkdir "%CD%\.gradle-local"
  echo Dang tai Gradle 9.5.0 tu services.gradle.org...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -UseBasicParsing -Uri 'https://services.gradle.org/distributions/gradle-9.5.0-bin.zip' -OutFile '%GRADLE_ZIP%'"
  if errorlevel 1 goto :download_error
  echo Dang giai nen Gradle...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%GRADLE_ZIP%' -DestinationPath '%CD%\.gradle-local' -Force"
  if errorlevel 1 goto :download_error
)

echo [4/5] Build APK...
set "ANDROID_SDK_ROOT=%ANDROID_HOME%"
call "%GRADLE_DIR%\bin\gradle.bat" --no-daemon --stacktrace assembleDebug
if errorlevel 1 goto :build_error

set "APK=%CD%\app\build\outputs\apk\debug\app-debug.apk"
if not exist "%APK%" goto :build_error
copy /Y "%APK%" "%CD%\KenPhotoAI-v1.2-debug.apk" >nul

echo [5/5] HOAN TAT.
echo.
echo APK de cai dien thoai:
echo %CD%\KenPhotoAI-v1.2-debug.apk
echo.
explorer /select,"%CD%\KenPhotoAI-v1.2-debug.apk" >nul 2>nul
pause
exit /b 0

:download_error
echo [LOI] Khong tai/giai nen duoc Gradle. Kiem tra Internet roi chay lai.
pause
exit /b 1

:build_error
echo [LOI] Build APK that bai.
echo File log chi tiet nam trong cua so nay. Co the mo project bang Android Studio de Sync/Build lai.
pause
exit /b 1
