package ai;

import java.io.IOException;

/**
 * AI-Powered Smart Search Handler.
 * 
 * Instead of basic text matching (name/brand), this uses Gemini AI
 * to understand user intent and rank products by relevance.
 * 
 * Examples of smart queries it handles:
 *   - "something nice for a birthday under 500"
 *   - "cheap electronics for students"
 *   - "durable outdoor gear"
 *   - "best value headphones"
 * 
 * Input:  POST /ai/search with JSON body containing "query" and "items"
 * Output: Ranked list of items with relevance scores and explanations
 */
public class SmartSearchHandler {

    private final GeminiClient geminiClient;

    public SmartSearchHandler(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    /**
     * Handle a smart search request.
     * 
     * Expected JSON body:
     * {
     *   "query": "something for a birthday under 500",
     *   "items": [
     *     {"id": 1, "name": "...", "brand": "...", "price": 100, "description": "..."},
     *     ...
     *   ]
     * }
     * 
     * @param requestBody The raw JSON string from the HTTP request
     * @return JSON response with ranked items
     */
    public String handle(String requestBody) throws IOException {
        System.out.println("[Smart Search] Processing search request...");

        // 1) Extract the query and items from the request
        String query = extractJsonStringField(requestBody, "query");
        String itemsArray = extractJsonArray(requestBody, "items");

        if (query == null || query.isEmpty()) {
            return "{\"error\":\"Missing required field: 'query'\"}";
        }
        if (itemsArray == null || itemsArray.isEmpty() || itemsArray.equals("[]")) {
            return "{\"error\":\"Missing or empty field: 'items'\"}";
        }

        System.out.println("[Smart Search] Query: \"" + query + "\"");

        // 2) Build the AI prompt
        String prompt = buildSearchPrompt(query, itemsArray);

        // 3) Call Gemini AI
        System.out.println("[Smart Search] Calling Gemini AI...");
        String aiResponse;
        try {
            aiResponse = geminiClient.generateContent(prompt);
        } catch (IOException e) {
            System.err.println("[Smart Search] Gemini API failed: " + e.getMessage());
            // Fallback: return items as-is with a warning
            return "{\"warning\":\"AI search unavailable, returning unranked results\","
                 + "\"results\":" + itemsArray + "}";
        }

        System.out.println("[Smart Search] AI response received.");

        // 4) Clean up the AI response (strip markdown code fences if present)
        String cleanResponse = cleanJsonResponse(aiResponse);

        // 5) Validate it looks like JSON
        if (!cleanResponse.trim().startsWith("{") && !cleanResponse.trim().startsWith("[")) {
            // AI returned non-JSON, wrap it
            return "{\"results\":" + itemsArray + ","
                 + "\"ai_summary\":\"" + escapeJson(cleanResponse) + "\"}";
        }

        return cleanResponse;
    }

    /**
     * Build the prompt that instructs Gemini to rank products.
     */
    private String buildSearchPrompt(String query, String itemsJson) {
        return """
            You are a smart product search engine for an online marketplace.
            
            A customer is searching for: "%s"
            
            Here are the available products in the marketplace:
            %s
            
            Your task:
            1. Analyze the customer's search intent (what they want, budget hints, preferences)
            2. Rank the products from MOST relevant to LEAST relevant based on the query
            3. Assign a relevance score (0.0 to 1.0) to each product
            4. Provide a brief reason for each ranking
            
            Respond ONLY with valid JSON in this exact format (no markdown, no explanation):
            {
              "results": [
                {
                  "id": <product_id>,
                  "name": "<product_name>",
                  "brand": "<brand>",
                  "price": <price>,
                  "relevance_score": <0.0 to 1.0>,
                  "reason": "<brief explanation of why this matches the query>"
                }
              ],
              "search_summary": "<one sentence summary of what the customer is looking for>"
            }
            
            Important rules:
            - Only include products that are at least somewhat relevant (score >= 0.2)
            - Sort by relevance_score descending (best match first)
            - If NO products match the query at all, return an empty results array
            - Always respond with valid JSON only, no markdown code fences
            """.formatted(query, itemsJson);
    }

    /**
     * Extract a string field value from a JSON object.
     * Simple parser — handles our known JSON structures.
     */
    private String extractJsonStringField(String json, String fieldName) {
        String key = "\"" + fieldName + "\"";
        int keyIdx = json.indexOf(key);
        if (keyIdx == -1) return null;

        // Find the colon after the key
        int colonIdx = json.indexOf(':', keyIdx + key.length());
        if (colonIdx == -1) return null;

        // Find the opening quote of the value
        int valueStart = json.indexOf('"', colonIdx + 1);
        if (valueStart == -1) return null;

        // Find the closing quote (handling escapes)
        int valueEnd = findClosingQuote(json, valueStart + 1);
        if (valueEnd == -1) return null;

        return json.substring(valueStart + 1, valueEnd);
    }

    /**
     * Extract a JSON array from a JSON object by field name.
     * Returns the full array including brackets: [...]
     */
    private String extractJsonArray(String json, String fieldName) {
        String key = "\"" + fieldName + "\"";
        int keyIdx = json.indexOf(key);
        if (keyIdx == -1) return null;

        // Find the opening bracket
        int bracketStart = json.indexOf('[', keyIdx + key.length());
        if (bracketStart == -1) return null;

        // Find the matching closing bracket (handle nesting)
        int depth = 0;
        boolean inString = false;
        for (int i = bracketStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && inString) {
                i++; // skip escaped char
                continue;
            }
            if (c == '"') {
                inString = !inString;
            } else if (!inString) {
                if (c == '[') depth++;
                else if (c == ']') {
                    depth--;
                    if (depth == 0) {
                        return json.substring(bracketStart, i + 1);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Find the closing quote of a JSON string, handling escaped characters.
     */
    private int findClosingQuote(String json, int startIdx) {
        for (int i = startIdx; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    /**
     * Remove markdown code fences from AI response if present.
     * Gemini sometimes wraps JSON in ```json ... ```
     */
    private String cleanJsonResponse(String response) {
        String trimmed = response.trim();

        // Remove ```json ... ``` wrapper
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }

        return trimmed.trim();
    }

    /**
     * Escape a string for safe JSON embedding.
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
