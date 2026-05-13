@echo off
REM ═══════════════════════════════════════════════════
REM   AI Marketplace Service - Run Script
REM   Starts the AI service on the specified port
REM ═══════════════════════════════════════════════════

set PORT=%1
if "%PORT%"=="" set PORT=9090

echo.
echo [RUN] Starting AI Marketplace Service on port %PORT%...
echo [RUN] Press Ctrl+C to stop.
echo.

REM Check if compiled
if not exist "out\ai\AIServer.class" (
    echo [RUN] ✗ Not compiled yet! Running compile.bat first...
    call compile.bat
    if %ERRORLEVEL% NEQ 0 exit /b 1
)

java -cp out ai.AIServer %PORT%
