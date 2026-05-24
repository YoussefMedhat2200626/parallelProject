package com.marketplace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.dto.AiSearchRequest;
import com.marketplace.dto.AiSearchResponse;
import com.marketplace.entity.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiServiceClient {
    private static final Logger LOG = LoggerFactory.getLogger(AiServiceClient.class);
    private static final String AI_SERVICE_HOST = "localhost";
    private static final int AI_SERVICE_PORT = 9092; // Port 9090 is used by Spring Boot's own RestSocketServer
    private final ObjectMapper objectMapper;

    public AiServiceClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy();
        this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Sends the query and standard DB search results to the AI Service via raw socket for semantic ranking.
     * If the AI service is unreachable, it gracefully falls back to returning an empty response object.
     */
    public AiSearchResponse smartSearch(String query, List<Item> items) {
        AiSearchResponse fallbackResponse = new AiSearchResponse();
        fallbackResponse.setResults(new ArrayList<>());
        fallbackResponse.setSearch_summary("");

        if (items == null || items.isEmpty()) {
            return fallbackResponse;
        }

        try (Socket socket = new Socket(AI_SERVICE_HOST, AI_SERVICE_PORT)) {
            socket.setSoTimeout(15000); // 15 seconds timeout
            
            // Prepare payload
            AiSearchRequest request = new AiSearchRequest(query, items);
            String jsonPayload = objectMapper.writeValueAsString(request);
            
            // Construct raw HTTP POST request
            String httpRequest = "POST /ai/search HTTP/1.1\r\n" +
                                 "Host: " + AI_SERVICE_HOST + ":" + AI_SERVICE_PORT + "\r\n" +
                                 "Content-Type: application/json\r\n" +
                                 "Content-Length: " + jsonPayload.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                                 "Connection: close\r\n\r\n" +
                                 jsonPayload;
            
            // Send request
            OutputStream out = socket.getOutputStream();
            out.write(httpRequest.getBytes(StandardCharsets.UTF_8));
            out.flush();
            
            // Read response
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String line;
            boolean isBody = false;
            StringBuilder bodyBuilder = new StringBuilder();
            
            while ((line = in.readLine()) != null) {
                if (isBody) {
                    bodyBuilder.append(line).append("\n");
                } else if (line.isEmpty()) {
                    isBody = true; // headers end
                }
            }
            
            String jsonResponse = bodyBuilder.toString().trim();
            if (jsonResponse.startsWith("{")) {
                AiSearchResponse response = objectMapper.readValue(jsonResponse, AiSearchResponse.class);
                return response;
            } else {
                LOG.error("Invalid response from AI Service: " + jsonResponse);
                return fallbackResponse;
            }
            
        } catch (Exception e) {
            LOG.warn("AI Service is unreachable or failed. Falling back to default search. Error: {}", e.getMessage());
            return fallbackResponse; // Graceful fallback
        }
    }
}
