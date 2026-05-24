package ai;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * AI Microservice Server for the Distributed Online Marketplace.
 * 
 * This service provides AI-powered capabilities:
 *   - Smart Search: Natural language product search using Gemini AI
 *   - Product Enrichment: Auto-generate descriptions and tags for products
 * 
 * Runs as a standalone Java Socket server, matching the team's
 * distributed microservices architecture (pure Java Sockets + TCP).
 * 
 * Usage:
 *   java ai.AIServer [port]
 *   Default port: 9090
 */
public class AIServer {

    private static final int DEFAULT_PORT = 9090;
    private static final String BANNER = """
    
    ╔══════════════════════════════════════════════╗
    ║        AI Marketplace Service v1.0           ║
    ║   Smart Search  ·  Product Enrichment        ║
    ╚══════════════════════════════════════════════╝
    """;

    private final int port;
    private final ExecutorService threadPool;
    private final GeminiClient geminiClient;

    public AIServer(int port, String apiKey) {
        this.port = port;
        // CachedThreadPool: creates threads on demand, reuses idle ones
        // Matches the team's concurrency model (thread-per-request)
        this.threadPool = Executors.newCachedThreadPool();
        this.geminiClient = new GeminiClient(apiKey);
    }

    /**
     * Start the AI service and listen for incoming connections.
     */
    public void start() {
        System.out.println(BANNER);
        System.out.println("[AI Service] Starting on port " + port + "...");
        System.out.println("[AI Service] Endpoints:");
        System.out.println("  POST /ai/search   - AI-powered smart search");
        System.out.println("  POST /ai/enrich   - Product data enrichment");
        System.out.println("  GET  /ai/health   - Health check");
        System.out.println();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[AI Service] ✓ Server is ready and listening on port " + port);
            System.out.println();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                String clientAddr = clientSocket.getInetAddress().getHostAddress();
                System.out.println("[AI Service] New connection from " + clientAddr);

                // Handle each request in its own thread
                threadPool.submit(new AIRequestHandler(clientSocket, geminiClient));
            }
        } catch (IOException e) {
            System.err.println("[AI Service] FATAL: Could not start server on port " + port);
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    /**
     * Entry point. Loads the Gemini API key and starts the server.
     */
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("[AI Service] Invalid port: " + args[0] + ". Using default " + DEFAULT_PORT);
            }
        }

        String apiKey = loadApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("[AI Service] ERROR: GEMINI_API_KEY is not set.");
            System.err.println("[AI Service] Set it in the .env file in the ai-service directory:");
            System.err.println("             GEMINI_API_KEY=your_key_here");
            System.err.println();
            System.err.println("[AI Service] Get a free API key at: https://aistudio.google.com/apikey");
            System.exit(1);
        }

        System.out.println("[AI Service] ✓ Gemini API key loaded successfully");
        new AIServer(port, apiKey).start();
    }

    /**
     * Load the Gemini API key from environment variable or .env file.
     */
    private static String loadApiKey() {
        // 1) Try environment variable first
        String key = System.getenv("GEMINI_API_KEY");
        if (key != null && !key.trim().isEmpty()) {
            return key.trim();
        }

        // 2) Try .env file in current directory
        File envFile = new File(".env");
        if (envFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    // Skip comments and empty lines
                    if (line.isEmpty() || line.startsWith("#")) continue;

                    if (line.startsWith("GEMINI_API_KEY=")) {
                        String value = line.substring("GEMINI_API_KEY=".length()).trim();
                        // Strip surrounding quotes if present
                        if ((value.startsWith("\"") && value.endsWith("\"")) ||
                            (value.startsWith("'") && value.endsWith("'"))) {
                            value = value.substring(1, value.length() - 1);
                        }
                        return value;
                    }
                }
            } catch (IOException e) {
                System.err.println("[AI Service] Warning: Could not read .env file: " + e.getMessage());
            }
        }

        return null;
    }
}
