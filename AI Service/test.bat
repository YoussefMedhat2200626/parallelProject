@echo off
REM ═══════════════════════════════════════════════════
REM   AI Marketplace Service - Quick Test Script
REM   Tests both AI endpoints with sample data
REM ═══════════════════════════════════════════════════

set PORT=%1
if "%PORT%"=="" set PORT=9090

echo.
echo ══════════════════════════════════════════════════
echo   Testing AI Marketplace Service (port %PORT%)
echo ══════════════════════════════════════════════════

REM ─── Test 1: Health Check ───
echo.
echo [TEST 1] Health Check...
echo   GET /ai/health
curl -s http://localhost:%PORT%/ai/health 2>nul
echo.

REM ─── Test 2: AI Smart Search ───
echo.
echo [TEST 2] AI Smart Search...
echo   POST /ai/search
echo   Query: "something for a birthday gift under 500"
curl -s -X POST http://localhost:%PORT%/ai/search ^
  -H "Content-Type: application/json" ^
  -d "{\"query\": \"something for a birthday gift under 500\", \"items\": [{\"id\": 1, \"name\": \"Wireless Headphones\", \"brand\": \"Sony\", \"price\": 450, \"description\": \"Premium wireless headphones with noise cancellation\"}, {\"id\": 2, \"name\": \"USB-C Cable\", \"brand\": \"Anker\", \"price\": 30, \"description\": \"Fast charging USB cable\"}, {\"id\": 3, \"name\": \"Smartwatch\", \"brand\": \"Samsung\", \"price\": 380, \"description\": \"Galaxy smartwatch with health tracking\"}, {\"id\": 4, \"name\": \"Laptop Stand\", \"brand\": \"Rain Design\", \"price\": 200, \"description\": \"Aluminum laptop stand for better ergonomics\"}, {\"id\": 5, \"name\": \"Mechanical Keyboard\", \"brand\": \"Keychron\", \"price\": 300, \"description\": \"Wireless mechanical keyboard with RGB lighting\"}]}" 2>nul
echo.

REM ─── Test 3: AI Product Enrichment ───
echo.
echo [TEST 3] AI Product Enrichment...
echo   POST /ai/enrich
echo   Product: "Wireless Headphones" by Sony
curl -s -X POST http://localhost:%PORT%/ai/enrich ^
  -H "Content-Type: application/json" ^
  -d "{\"name\": \"Wireless Headphones\", \"brand\": \"Sony\", \"category\": \"Electronics\", \"price\": 450}" 2>nul
echo.

echo.
echo ══════════════════════════════════════════════════
echo   Tests complete!
echo ══════════════════════════════════════════════════
echo.
