package com.marketplace.socket;

import com.marketplace.entity.*;
import com.marketplace.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * REST Web Services implemented using raw Java Sockets.
 * Listens on port 9090 and manually parses HTTP requests,
 * routes them, and writes HTTP responses with JSON bodies.
 *
 * Endpoints:
 *   GET  /api/v1/items/search?q=...&excludeSeller=...
 *   GET  /api/v1/items/{itemId}
 *   GET  /api/v1/accounts/{userId}
 *   GET  /api/v1/inventory/{itemId}
 *   GET  /api/v1/inventory/seller/{sellerId}
 *   PUT  /api/v1/inventory/{itemId}   body: {"quantity": N}
 */
@Component
public class RestSocketServer {

    private static final Logger LOG = LoggerFactory.getLogger(RestSocketServer.class);
    private static final int PORT = 9090;

    private final ItemService itemService;
    private final InventoryService inventoryService;
    private final UserService userService;
    private final WalletService walletService;
    private final TransactionService transactionService;

    private ServerSocket serverSocket;
    private volatile boolean running = true;

    public RestSocketServer(ItemService itemService, InventoryService inventoryService,
                            UserService userService, WalletService walletService,
                            TransactionService transactionService) {
        this.itemService = itemService;
        this.inventoryService = inventoryService;
        this.userService = userService;
        this.walletService = walletService;
        this.transactionService = transactionService;
    }

    @PostConstruct
    public void start() {
        Thread serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                LOG.info("REST Socket Server started on port {}", PORT);
                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        // Handle each connection in a new thread
                        Thread handler = new Thread(() -> handleClient(clientSocket));
                        handler.setDaemon(true);
                        handler.start();
                    } catch (IOException e) {
                        if (running) LOG.error("Error accepting connection", e);
                    }
                }
            } catch (IOException e) {
                LOG.error("Failed to start REST Socket Server on port {}", PORT, e);
            }
        }, "rest-socket-server");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    @PreDestroy
    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            LOG.error("Error closing server socket", e);
        }
    }

    private void handleClient(Socket socket) {
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             OutputStream out = socket.getOutputStream()) {

            // ---- 1. Parse the HTTP request line ----
            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            String[] parts = requestLine.split(" ");
            if (parts.length < 2) return;
            String method = parts[0];     // GET, PUT, POST, etc.
            String fullPath = parts[1];   // /api/v1/items/search?q=test

            // ---- 2. Parse headers ----
            Map<String, String> headers = new HashMap<>();
            String headerLine;
            int contentLength = 0;
            while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
                int colon = headerLine.indexOf(':');
                if (colon > 0) {
                    String key = headerLine.substring(0, colon).trim().toLowerCase();
                    String value = headerLine.substring(colon + 1).trim();
                    headers.put(key, value);
                    if (key.equals("content-length")) {
                        contentLength = Integer.parseInt(value);
                    }
                }
            }

            // ---- 3. Read body (for PUT/POST) ----
            String body = "";
            if (contentLength > 0) {
                char[] buf = new char[contentLength];
                int read = in.read(buf, 0, contentLength);
                body = new String(buf, 0, read);
            }

            // ---- 4. Separate path and query string ----
            String path = fullPath;
            Map<String, String> queryParams = new HashMap<>();
            int qMark = fullPath.indexOf('?');
            if (qMark >= 0) {
                path = fullPath.substring(0, qMark);
                String queryStr = fullPath.substring(qMark + 1);
                for (String pair : queryStr.split("&")) {
                    String[] kv = pair.split("=", 2);
                    String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                    String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
                    queryParams.put(key, value);
                }
            }

            LOG.debug("REST Socket: {} {} (body={})", method, path, body.length());

            // ---- 5. Route the request ----
            String responseJson;
            int statusCode;

            try {
                if (method.equals("GET") && path.equals("/api/v1/items/search")) {
                    responseJson = handleItemSearch(queryParams);
                    statusCode = 200;
                } else if (method.equals("GET") && path.matches("/api/v1/items/\\d+")) {
                    Long itemId = Long.parseLong(path.substring("/api/v1/items/".length()));
                    responseJson = handleGetItem(itemId);
                    statusCode = responseJson.contains("\"error\"") ? 404 : 200;
                } else if (method.equals("GET") && path.matches("/api/v1/accounts/\\d+")) {
                    Long userId = Long.parseLong(path.substring("/api/v1/accounts/".length()));
                    responseJson = handleGetAccount(userId);
                    statusCode = responseJson.contains("\"error\"") ? 404 : 200;
                } else if (method.equals("GET") && path.matches("/api/v1/inventory/seller/\\d+")) {
                    Long sellerId = Long.parseLong(path.substring("/api/v1/inventory/seller/".length()));
                    responseJson = handleGetSellerInventory(sellerId);
                    statusCode = 200;
                } else if (method.equals("GET") && path.matches("/api/v1/inventory/\\d+")) {
                    Long itemId = Long.parseLong(path.substring("/api/v1/inventory/".length()));
                    responseJson = handleGetInventory(itemId);
                    statusCode = responseJson.contains("\"error\"") ? 404 : 200;
                } else if (method.equals("PUT") && path.matches("/api/v1/inventory/\\d+")) {
                    Long itemId = Long.parseLong(path.substring("/api/v1/inventory/".length()));
                    responseJson = handleUpdateInventory(itemId, body);
                    statusCode = responseJson.contains("\"error\"") ? 400 : 200;
                } else {
                    responseJson = "{\"error\": \"Not Found\", \"path\": \"" + escapeJson(path) + "\"}";
                    statusCode = 404;
                }
            } catch (Exception e) {
                responseJson = "{\"error\": \"" + escapeJson(e.getMessage()) + "\"}";
                statusCode = 500;
            }

            // ---- 6. Write the HTTP response ----
            sendHttpResponse(out, statusCode, "application/json", responseJson);

        } catch (Exception e) {
            LOG.error("Error handling REST socket client", e);
        }
    }

    // ===================== REST Handlers =====================

    /** REST Service 1: Item Search */
    private String handleItemSearch(Map<String, String> params) {
        String q = params.getOrDefault("q", "");
        long excludeSeller = Long.parseLong(params.getOrDefault("excludeSeller", "0"));

        List<Item> items = itemService.searchItems(q, excludeSeller);
        StringBuilder sb = new StringBuilder();
        sb.append("{\"query\":\"").append(escapeJson(q)).append("\",");
        sb.append("\"resultCount\":").append(items.size()).append(",");
        sb.append("\"items\":[");
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append("\"itemId\":").append(item.getItemId()).append(",");
            sb.append("\"name\":\"").append(escapeJson(item.getName())).append("\",");
            sb.append("\"description\":\"").append(escapeJson(item.getDescription())).append("\",");
            sb.append("\"brand\":\"").append(escapeJson(item.getBrand())).append("\",");
            sb.append("\"category\":\"").append(escapeJson(item.getCategory())).append("\",");
            sb.append("\"priceCents\":").append(item.getPriceCents()).append(",");
            sb.append("\"priceFormatted\":\"").append(item.getPriceFormatted()).append("\",");
            sb.append("\"status\":\"").append(item.getStatus()).append("\",");
            sb.append("\"sellerId\":").append(item.getSellerId()).append(",");
            sb.append("\"availableQty\":").append(inventoryService.getAvailableQuantity(item.getItemId()));
            sb.append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    /** REST Service 1b: Get Single Item */
    private String handleGetItem(Long itemId) {
        return itemService.findById(itemId).map(item -> {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"itemId\":").append(item.getItemId()).append(",");
            sb.append("\"name\":\"").append(escapeJson(item.getName())).append("\",");
            sb.append("\"description\":\"").append(escapeJson(item.getDescription())).append("\",");
            sb.append("\"brand\":\"").append(escapeJson(item.getBrand())).append("\",");
            sb.append("\"category\":\"").append(escapeJson(item.getCategory())).append("\",");
            sb.append("\"priceCents\":").append(item.getPriceCents()).append(",");
            sb.append("\"priceFormatted\":\"").append(item.getPriceFormatted()).append("\",");
            sb.append("\"status\":\"").append(item.getStatus()).append("\",");
            sb.append("\"availableQty\":").append(inventoryService.getAvailableQuantity(item.getItemId()));
            sb.append("}");
            return sb.toString();
        }).orElse("{\"error\":\"Item not found\"}");
    }

    /** REST Service 2: Account Info */
    private String handleGetAccount(Long userId) {
        return userService.findById(userId).map(user -> {
            Wallet wallet = walletService.getWallet(userId);
            List<Item> itemsForSale = itemService.getSellerItems(userId);
            List<Transaction> purchases = transactionService.getBuyerTransactions(userId);
            List<Transaction> sales = transactionService.getSellerTransactions(userId);

            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"userId\":").append(user.getUserId()).append(",");
            sb.append("\"username\":\"").append(escapeJson(user.getUsername())).append("\",");
            sb.append("\"fullName\":\"").append(escapeJson(user.getFullName())).append("\",");
            sb.append("\"email\":\"").append(escapeJson(user.getEmail())).append("\",");
            sb.append("\"balanceCents\":").append(wallet.getBalanceCents()).append(",");
            sb.append("\"balanceFormatted\":\"").append(wallet.getBalanceFormatted()).append("\",");
            sb.append("\"itemsForSale\":").append(itemsForSale.size()).append(",");
            sb.append("\"totalPurchases\":").append(purchases.size()).append(",");
            sb.append("\"totalSales\":").append(sales.size());
            sb.append("}");
            return sb.toString();
        }).orElse("{\"error\":\"User not found\"}");
    }

    /** REST Service 3: Get Inventory */
    private String handleGetInventory(Long itemId) {
        return inventoryService.getByItemId(itemId).map(inv -> {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"itemId\":").append(inv.getItemId()).append(",");
            sb.append("\"quantity\":").append(inv.getQuantity()).append(",");
            sb.append("\"reserved\":").append(inv.getReserved()).append(",");
            sb.append("\"available\":").append(inv.getAvailable());
            sb.append("}");
            return sb.toString();
        }).orElse("{\"error\":\"Inventory not found\"}");
    }

    /** REST Service 3b: Get Seller Inventory */
    private String handleGetSellerInventory(Long sellerId) {
        List<Inventory> inventories = inventoryService.getSellerInventory(sellerId);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < inventories.size(); i++) {
            Inventory inv = inventories.get(i);
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append("\"itemId\":").append(inv.getItemId()).append(",");
            sb.append("\"quantity\":").append(inv.getQuantity()).append(",");
            sb.append("\"reserved\":").append(inv.getReserved()).append(",");
            sb.append("\"available\":").append(inv.getAvailable());
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    /** REST Service 3c: Update Inventory (PUT) */
    private String handleUpdateInventory(Long itemId, String body) {
        // Manually parse JSON body: {"quantity": 10}
        int qty = 0;
        int idx = body.indexOf("\"quantity\"");
        if (idx >= 0) {
            String after = body.substring(idx + "\"quantity\"".length());
            after = after.replaceFirst("[^0-9-]*", ""); // skip ':' and whitespace
            StringBuilder numStr = new StringBuilder();
            for (char c : after.toCharArray()) {
                if (Character.isDigit(c) || c == '-') numStr.append(c);
                else if (numStr.length() > 0) break;
            }
            qty = Integer.parseInt(numStr.toString());
        }

        try {
            Inventory updated = inventoryService.updateQuantity(itemId, qty);
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"itemId\":").append(updated.getItemId()).append(",");
            sb.append("\"quantity\":").append(updated.getQuantity()).append(",");
            sb.append("\"available\":").append(updated.getAvailable()).append(",");
            sb.append("\"message\":\"Inventory updated successfully\"");
            sb.append("}");
            return sb.toString();
        } catch (Exception e) {
            return "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    // ===================== HTTP Helpers =====================

    private void sendHttpResponse(OutputStream out, int statusCode, String contentType, String body)
            throws IOException {
        String statusText = switch (statusCode) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            default -> "OK";
        };

        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        String response = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                "Content-Type: " + contentType + "; charset=utf-8\r\n" +
                "Content-Length: " + bodyBytes.length + "\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n" +
                "\r\n";
        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.write(bodyBytes);
        out.flush();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
