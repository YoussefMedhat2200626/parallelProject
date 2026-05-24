@echo off
REM ═══════════════════════════════════════════════
REM  Start AI Service from the demo directory
REM  Usage: start-ai.bat [port]   (default: 9092)
REM ═══════════════════════════════════════════════

set PORT=%1
if "%PORT%"=="" set PORT=9092

REM Kill any existing process on the port
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%PORT% " 2^>nul') do (
    echo [AI] Freeing port %PORT% (PID %%a)...
    taskkill /PID %%a /F >nul 2>&1
)

REM Compile if needed
if not exist "ai-service\out\ai\AIServer.class" (
    echo [AI] Compiling AI Service...
    javac -d ai-service\out -encoding UTF-8 ai-service\src\ai\*.java
    if %ERRORLEVEL% NEQ 0 (
        echo [AI] Compilation FAILED!
        pause
        exit /b 1
    )
)

echo [AI] Starting AI Service on port %PORT%...
java -cp ai-service\out ai.AIServer %PORT%
