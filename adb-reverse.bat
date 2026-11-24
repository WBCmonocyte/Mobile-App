@echo off
setlocal
echo Dang thiet lap adb reverse port 8080 cho Android...
if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
    "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" reverse tcp:8080 tcp:8080
    "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" reverse --list
) else (
    adb reverse tcp:8080 tcp:8080
    adb reverse --list
)
echo Hoan tat! App da co the ket noi thang toi Backend tren cong 8080.
pause
