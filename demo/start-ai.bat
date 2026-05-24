@echo off
REM ═══════════════════════════════════════════════
REM  Start AI Service from the demo directory
REM  Usage: start-ai.bat [port]   (default: 9092)
REM ═══════════════════════════════════════════════

set PORT=%1
if "%PORT%"=="" set PORT=9092

REM Skip auto port kill to avoid batch parse errors
echo [AI] Using port %PORT%...

REM Compile if needed
set "AI_DIR=..\AI Service"

echo [AI] Compiling AI Service...
javac -d "%AI_DIR%\out" -encoding UTF-8 "%AI_DIR%\src\ai\*.java"
if errorlevel 1 (
    echo [AI] Compilation FAILED!
    pause
    exit /b 1
)

echo [AI] Starting AI Service on port %PORT%...
java -cp "%AI_DIR%\out" ai.AIServer %PORT%
