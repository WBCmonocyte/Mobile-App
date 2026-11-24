@echo off
setlocal
echo ========================================================
echo   Starting EasySport Spring Boot Backend on Port 8080
echo ========================================================

REM Kiem tra Java
if not defined JAVA_HOME (
    if exist "C:\Program Files\Java\jdk-24" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-24"
    )
)
if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

REM Kiem tra Maven
where mvn >nul 2>nul
if %errorlevel% equ 0 (
    set "MVN_CMD=mvn"
) else if exist "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" (
    set "MVN_CMD=C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd"
) else (
    echo [ERROR] Khong tim thay Maven. Vui long cai Maven hoac them vao PATH.
    pause
    exit /b 1
)

REM Tu dong chuyen tiep port cho may ao/thiet bi Android (adb reverse)
if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
    "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" reverse tcp:8080 tcp:8080 >nul 2>&1
) else (
    where adb >nul 2>nul && adb reverse tcp:8080 tcp:8080 >nul 2>&1
)

cd /d "%~dp0backend"
echo [INFO] Dang khoi dong Spring Boot Backend...
call "%MVN_CMD%" spring-boot:run
pause
