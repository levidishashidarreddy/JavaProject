@echo off
echo ========================================================
echo Starting NetSentinel Web Portal and Security Engine...
echo Access at: http://localhost:8080
echo ========================================================

if exist "%~dp0netsentinel-web\target\netsentinel-web-1.0.0-SNAPSHOT.jar" (
    echo [INFO] Running packaged application JAR...
    java -jar "%~dp0netsentinel-web\target\netsentinel-web-1.0.0-SNAPSHOT.jar"
    pause
    exit /b %ERRORLEVEL%
)

where mvn >nul 2>&1
if %ERRORLEVEL% equ 0 (
    set MVN_CMD=mvn
) else (
    if exist "%~dp0.tools\apache-maven-3.9.16\bin\mvn.cmd" (
        set MVN_CMD="%~dp0.tools\apache-maven-3.9.16\bin\mvn.cmd"
    ) else (
        echo [ERROR] Maven not found.
        pause
        exit /b 1
    )
)

cd "%~dp0netsentinel-web"
call %MVN_CMD% spring-boot:run
pause
