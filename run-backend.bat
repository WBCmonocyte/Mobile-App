@echo off
echo ========================================================
echo   Starting EasySport Spring Boot Backend on Port 8080
echo ========================================================
set "JAVA_HOME=D:\Java\jdk-25"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "%~dp0backend"
"C:\Users\HP\Downloads\apache-maven-3.10.0-bin\apache-maven-3.10.0\bin\mvn.cmd" spring-boot:run
pause
