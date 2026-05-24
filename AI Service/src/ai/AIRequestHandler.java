package ai;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * Handles incoming HTTP-like requests on the AI service socket.
 * 
 * Matches the team's architecture: custom HTTP parser over raw TCP sockets
 * that mimics RESTful services from scratch.
 * 
 * Supported Endpoints:
 *   POST /ai/search  - AI-powered smart product search
 *   POST /ai/enrich  - AI product data enrichment  
 *   GET  /ai/health  - Health check
 */
public class AIRequestHandler implements Runnable {

    private final Socket clientSocket;
    private final GeminiClient geminiClient;

    // Lazily initialized handlers (shared GeminiClient)
    private final SmartSearchHandler searchHandler;
    private final EnrichmentHandler enrichmentHandler;

    public AIRequestHandler(Socket clientSocket, GeminiClient geminiClient) {
        this.clientSocket = clientSocket;
        this.geminiClient = geminiClient;
        this.searchHandler = new SmartSearchHandler(geminiClient);
        this.enrichmentHandler = new EnrichmentHandler(geminiClient);
    }

    @Override
    public void run() {
        try (
            InputStream rawIn = clientSocket.getInputStream();
            OutputStream rawOut = clientSocket.getOutputStream()
        ) {
            // Parse the incoming HTTP request
            String[] requestParts = parseHttpRequest(rawIn);
            String method = requestParts[0];   // e.g., "POST"
            String path   = requestParts[1];   // e.g., "/ai/search"
            String body   = requestParts[2];   // JSON body (if any)

            System.out.println("[AI Handler] " + method + " " + path);

            // Route the request to the appropriate handler
            String responseBody;
            int statusCode;

            try {
                switch (path) {
                    case "/ai/search":
                        if (!"POST".equals(method)) {
                            statusCode = 405;
                            responseBody = errorJson("Method not allowed. Use POST.");
                        } else {
                            responseBody = searchHandler.handle(body);
                            statusCode = 200;
                        }
                        break;

                    case "/ai/enrich":
                        if (!"POST".equals(method)) {
                            statusCode = 405;
                            responseBody = errorJson("Method not allowed. Use POST.");
                        } else {
                            responseBody = enrichmentHandler.handle(body);
                            statusCode = 200;
                        }
                        break;

                    case "/ai/health":
                        statusCode = 200;
                        responseBody = "{\"status\":\"healthy\",\"service\":\"ai-marketplace\",\"version\":\"1.0\"}";
                        break;

                    default:
                        statusCode = 404;
                        responseBody = errorJson("Endpoint not found: " + path
                            + ". Available: POST /ai/search, POST /ai/enrich, GET /ai/health");
                        break;
                }
            } catch (Exception e) {
                System.err.println("[AI Handler] Error processing request: " + e.getMessage());
                e.printStackTrace();
                statusCode = 500;
                responseBody = errorJson("Internal server error: " + e.getMessage());
            }

            // Send the HTTP response
            sendHttpResponse(rawOut, statusCode, responseBody);

            System.out.println("[AI Handler] → " + statusCode + " (" + responseBody.length() + " bytes)");

        } catch (IOException e) {
            System.err.println("[AI Handler] Connection error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignore close errors
            }
        }
    }

    /**
     * Parse an HTTP request from the input stream.
     * 
     * Returns: [method, path, body]
     * 
     * Handles the team's custom HTTP-like protocol:
     *   POST /ai/search HTTP/1.1
     *   Content-Type: application/json
     *   Content-Length: 123
     *   
     *   {"query": "..."}
     */
    private String[] parseHttpRequest(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

        // 1) Read the request line: "METHOD /path HTTP/1.x"
        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.isEmpty()) {
            return new String[]{"GET", "/ai/health", ""};
        }

        String[] parts = requestLine.split("\\s+");
        String method = parts.length > 0 ? parts[0].toUpperCase() : "GET";
        String path   = parts.length > 1 ? parts[1] : "/";

        // 2) Read headers until blank line
        int contentLength = 0;
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            if (line.toLowerCase().startsWith("content-length:")) {
                try {
                    contentLength = Integer.parseInt(line.substring(15).trim());
                } catch (NumberFormatException e) {
                    // Ignore malformed content-length
                }
            }
        }

        // 3) Read the body based on Content-Length
        String body = "";
        if (contentLength > 0) {
            char[] bodyChars = new char[contentLength];
            int totalRead = 0;
            while (totalRead < contentLength) {
                int read = reader.read(bodyChars, totalRead, contentLength - totalRead);
                if (read == -1) break;
                totalRead += read;
            }
            body = new String(bodyChars, 0, totalRead);
        } else if ("POST".equals(method) || "PUT".equals(method)) {
            // Fallback: try to read available data if no Content-Length
            StringBuilder sb = new StringBuilder();
            while (reader.ready()) {
                int c = reader.read();
                if (c == -1) break;
                sb.append((char) c);
            }
            body = sb.toString();
        }

        return new String[]{method, path, body};
    }

    /**
     * Send an HTTP response back through the socket.
     */
    private void sendHttpResponse(OutputStream out, int statusCode, String body) throws IOException {
        String statusText = switch (statusCode) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 500 -> "Internal Server Error";
            default  -> "Unknown";
        };

        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        // Build response with CORS headers for browser/frontend compatibility
        String response = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n"
            + "Content-Type: application/json; charset=UTF-8\r\n"
            + "Content-Length: " + bodyBytes.length + "\r\n"
            + "Access-Control-Allow-Origin: *\r\n"
            + "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n"
            + "Access-Control-Allow-Headers: Content-Type, Authorization\r\n"
            + "Connection: close\r\n"
            + "\r\n";

        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.write(bodyBytes);
        out.flush();
    }

    /**
     * Build a simple error JSON response.
     */
    private String errorJson(String message) {
        // Escape any quotes in the message
        String escaped = message.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"error\":\"" + escaped + "\"}";
    }
}
