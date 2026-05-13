package ai;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * Test client for the AI Marketplace Service.
 * Sends test requests to all endpoints and displays the responses.
 * 
 * Usage: java ai.TestClient [port]
 */
public class TestClient {

    private static final String HOST = "localhost";
    
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9090;
        
        System.out.println("══════════════════════════════════════════════════");
        System.out.println("  Testing AI Marketplace Service (port " + port + ")");
        System.out.println("══════════════════════════════════════════════════");
        
        // Test 1: Health check
        System.out.println("\n[TEST 1] GET /ai/health");
        String healthResponse = sendRequest(port, "GET", "/ai/health", null);
        System.out.println("  Response: " + healthResponse);
        
        // Test 2: AI Smart Search
        System.out.println("\n[TEST 2] POST /ai/search");
        String searchBody = """
            {
                "query": "something for a birthday gift under 500",
                "items": [
                    {"id": 1, "name": "Wireless Headphones", "brand": "Sony", "price": 450, "description": "Premium wireless headphones with noise cancellation"},
                    {"id": 2, "name": "USB-C Cable", "brand": "Anker", "price": 30, "description": "Fast charging USB cable"},
                    {"id": 3, "name": "Smartwatch", "brand": "Samsung", "price": 380, "description": "Galaxy smartwatch with health tracking"},
                    {"id": 4, "name": "Laptop Stand", "brand": "Rain Design", "price": 200, "description": "Aluminum laptop stand for ergonomics"},
                    {"id": 5, "name": "Mechanical Keyboard", "brand": "Keychron", "price": 300, "description": "Wireless mechanical keyboard with RGB"}
                ]
            }
            """;
        System.out.println("  Query: \"something for a birthday gift under 500\"");
        System.out.println("  Items: 5 products");
        System.out.println("  Waiting for AI response...");
        String searchResponse = sendRequest(port, "POST", "/ai/search", searchBody);
        System.out.println("  Response: " + searchResponse);
        
        // Test 3: AI Product Enrichment
        System.out.println("\n[TEST 3] POST /ai/enrich");
        String enrichBody = """
            {
                "name": "Wireless Headphones",
                "brand": "Sony",
                "category": "Electronics",
                "price": 450
            }
            """;
        System.out.println("  Product: Wireless Headphones by Sony");
        System.out.println("  Waiting for AI response...");
        String enrichResponse = sendRequest(port, "POST", "/ai/enrich", enrichBody);
        System.out.println("  Response: " + enrichResponse);
        
        // Test 4: 404 endpoint
        System.out.println("\n[TEST 4] GET /ai/nonexistent");
        String notFoundResponse = sendRequest(port, "GET", "/ai/nonexistent", null);
        System.out.println("  Response: " + notFoundResponse);
        
        System.out.println("\n══════════════════════════════════════════════════");
        System.out.println("  All tests complete!");
        System.out.println("══════════════════════════════════════════════════");
    }
    
    /**
     * Send an HTTP request to the AI service via raw socket
     * (matching the team's communication pattern).
     */
    private static String sendRequest(int port, String method, String path, String body) throws Exception {
        try (Socket socket = new Socket(HOST, port)) {
            socket.setSoTimeout(120_000); // 2 min timeout for AI calls
            
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            
            // Build HTTP request
            StringBuilder request = new StringBuilder();
            request.append(method).append(" ").append(path).append(" HTTP/1.1\r\n");
            request.append("Host: ").append(HOST).append(":").append(port).append("\r\n");
            request.append("Connection: close\r\n");
            
            if (body != null && !body.isEmpty()) {
                byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
                request.append("Content-Type: application/json\r\n");
                request.append("Content-Length: ").append(bodyBytes.length).append("\r\n");
                request.append("\r\n");
                
                out.write(request.toString().getBytes(StandardCharsets.UTF_8));
                out.write(bodyBytes);
            } else {
                request.append("\r\n");
                out.write(request.toString().getBytes(StandardCharsets.UTF_8));
            }
            out.flush();
            
            // Read response
            StringBuilder response = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            
            // Skip HTTP headers
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) break; // End of headers
            }
            
            // Read body
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                response.append(buffer, 0, read);
            }
            
            return response.toString();
        }
    }
}
