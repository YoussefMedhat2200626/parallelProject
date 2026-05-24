@echo off
REM ═══════════════════════════════════════════════════
REM   AI Marketplace Service - Compile Script
REM   Compiles all Java source files into the out/ dir
REM ═══════════════════════════════════════════════════

echo.
echo [BUILD] Compiling AI Marketplace Service...
echo.

REM Create output directory
if not exist "out" mkdir out

REM Compile all Java files
javac -d out -encoding UTF-8 src\ai\*.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [BUILD] ✗ Compilation FAILED! Fix the errors above.
    exit /b 1
)

echo [BUILD] ✓ Compilation successful!
echo [BUILD] Output: out\
echo.
echo [BUILD] To run: run.bat
echo.
