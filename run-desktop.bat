@echo off
echo ========================================================
echo Starting NetSentinel JavaFX Desktop Security Console...
echo (Ensure NetSentinel Web is running on http://localhost:8080)
echo ========================================================

if exist "%~dp0netsentinel-desktop\target\netsentinel-desktop-1.0.0-SNAPSHOT.jar" (
    echo [INFO] Running packaged JavaFX shaded JAR...
    java -jar "%~dp0netsentinel-desktop\target\netsentinel-desktop-1.0.0-SNAPSHOT.jar"
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

cd "%~dp0netsentinel-desktop"
call %MVN_CMD% javafx:run
pause
