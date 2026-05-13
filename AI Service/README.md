# AI Marketplace Service

AI-powered microservice for the Distributed Online Marketplace system.  
Part of CSE352s — Parallel and Distributed Systems project.

## Features

### 1. AI Smart Search (`POST /ai/search`)
Natural language product search powered by Google Gemini AI. Instead of basic name/brand text matching, users can search with natural queries like:
- *"something nice for a birthday under 500"*
- *"cheap electronics for students"*
- *"durable outdoor gear"*

The AI understands intent, budget hints, and preferences, then ranks products by relevance.

### 2. AI Product Enrichment (`POST /ai/enrich`)
Auto-generates professional product descriptions and search tags when sellers add new items. The seller only needs to provide a name and brand — the AI handles the rest.

## Prerequisites

- **Java 17+** (JDK)
- **Gemini API Key** (free at [aistudio.google.com/apikey](https://aistudio.google.com/apikey))

## Quick Start

### 1. Set your API key
Edit the `.env` file:
```
GEMINI_API_KEY=your_actual_api_key_here
```

### 2. Compile
```bash
compile.bat
```

### 3. Run
```bash
run.bat [port]    # Default: 9090
```

### 4. Test
In a separate terminal:
```bash
test.bat
```

## API Reference

### `GET /ai/health`
Health check endpoint.

**Response:**
```json
{"status": "healthy", "service": "ai-marketplace", "version": "1.0"}
```

---

### `POST /ai/search`
AI-powered smart product search.

**Request Body:**
```json
{
  "query": "something for a birthday gift under 500",
  "items": [
    {
      "id": 1,
      "name": "Wireless Headphones",
      "brand": "Sony",
      "price": 450,
      "description": "Premium wireless headphones with noise cancellation"
    },
    {
      "id": 2,
      "name": "USB-C Cable",
      "brand": "Anker",
      "price": 30,
      "description": "Fast charging cable"
    }
  ]
}
```

**Response:**
```json
{
  "results": [
    {
      "id": 1,
      "name": "Wireless Headphones",
      "brand": "Sony",
      "price": 450,
      "relevance_score": 0.92,
      "reason": "Great gift option within budget at $450"
    }
  ],
  "search_summary": "Looking for an affordable birthday gift"
}
```

---

### `POST /ai/enrich`
AI-powered product data enrichment.

**Request Body:**
```json
{
  "name": "Wireless Headphones",
  "brand": "Sony",
  "category": "Electronics",
  "price": 450
}
```

**Response:**
```json
{
  "name": "Wireless Headphones",
  "brand": "Sony",
  "description": "Experience premium sound quality with Sony Wireless Headphones. Featuring advanced Bluetooth connectivity, active noise cancellation, and up to 30 hours of battery life. Perfect for music lovers and commuters who demand crystal-clear audio.",
  "tags": ["wireless", "headphones", "bluetooth", "noise-cancelling", "sony", "audio", "gift"],
  "category_suggestion": "Electronics > Audio > Headphones"
}
```

## Integration Guide

### For the API Gateway team:
Add routing for the AI service in your API Gateway:

```java
// In your API Gateway routing logic:
if (path.startsWith("/ai/")) {
    // Forward to AI Service (port 9090)
    forwardToService("localhost", 9090, method, path, body);
}
```

### For the Item Service team:
To add AI search to your existing search flow:

```java
// After basic DB search, call AI Service for smart ranking:
Socket aiSocket = new Socket("localhost", 9090);
// Send: POST /ai/search with items array
// Receive: Ranked results from AI
```

To add auto-enrichment when adding new products:

```java
// When a seller adds a new item, call AI Service:
Socket aiSocket = new Socket("localhost", 9090);
// Send: POST /ai/enrich with product info
// Receive: Generated description and tags
// Store enriched data in Item DB
```

## Architecture

```
Client → API Gateway → AI Service (port 9090)
                            │
                            ├─→ SmartSearchHandler  → Gemini API
                            └─→ EnrichmentHandler   → Gemini API
```

- **Zero external Java dependencies** — uses only `java.net.*` and `java.util.concurrent.*`
- **Thread-per-request** concurrency model (matches team architecture)
- **Custom HTTP parser** over raw TCP sockets (matches team's REST-over-sockets pattern)
- **Graceful fallback** — if Gemini API is down, returns basic results instead of failing

## Project Structure

```
ai-service/
├── src/
│   └── ai/
│       ├── AIServer.java              # Main server (socket listener + thread pool)
│       ├── AIRequestHandler.java      # HTTP parser and request router
│       ├── GeminiClient.java          # Google Gemini API client
│       ├── SmartSearchHandler.java    # AI-powered product search
│       └── EnrichmentHandler.java     # AI product data enrichment
├── compile.bat                        # Build script
├── run.bat                            # Run script
├── test.bat                           # Test script
├── .env                               # Gemini API key
└── README.md                          # This file
```


