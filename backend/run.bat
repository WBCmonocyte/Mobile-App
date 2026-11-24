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

cd /d "%~dp0"
echo [INFO] Dang khoi dong Spring Boot Backend...
call "%MVN_CMD%" spring-boot:run
pause
