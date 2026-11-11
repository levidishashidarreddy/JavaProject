@echo off
echo ========================================================
echo NetSentinel — Master Project Build
echo Java 21 + Spring Boot 3 + JavaFX 21 + SQLite
echo ========================================================

REM Check if maven is in PATH or use bundled tools
where mvn >nul 2>&1
if %ERRORLEVEL% equ 0 (
    set MVN_CMD=mvn
) else (
    if exist "%~dp0.tools\apache-maven-3.9.16\bin\mvn.cmd" (
        set MVN_CMD="%~dp0.tools\apache-maven-3.9.16\bin\mvn.cmd"
    ) else (
        echo [ERROR] Maven not found in PATH or .tools directory.
        pause
        exit /b 1
    )
)

echo Building NetSentinel multi-module platform...
call %MVN_CMD% clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Build failed! Check compiler messages above.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ========================================================
echo [SUCCESS] NetSentinel build completed successfully!
echo Run 'run-web.bat' to start the Spring Boot web application.
echo Run 'run-desktop.bat' to start the JavaFX desktop console.
echo ========================================================
pause

