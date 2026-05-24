package ai;

import java.io.IOException;

/**
 * AI Product Data Enrichment Handler.
 * 
 * When a seller adds a new product with minimal info (name, brand, price),
 * this handler uses Gemini AI to automatically generate:
 *   - A professional, detailed product description
 *   - Relevant search tags/keywords for better discoverability
 * 
 * Input:  POST /ai/enrich with JSON body containing product info
 * Output: Enriched product data with AI-generated description and tags
 */
public class EnrichmentHandler {

    private final GeminiClient geminiClient;

    public EnrichmentHandler(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    /**
     * Handle a product enrichment request.
     * 
     * Expected JSON body:
     * {
     *   "name": "Wireless Headphones",
     *   "brand": "Sony",
     *   "category": "Electronics",     (optional)
     *   "price": 450                   (optional)
     * }
     * 
     * @param requestBody The raw JSON string from the HTTP request
     * @return JSON response with enriched product data
     */
    public String handle(String requestBody) throws IOException {
        System.out.println("[Enrichment] Processing product enrichment request...");

        // 1) Extract product fields from the request
        String name     = extractJsonStringField(requestBody, "name");
        String brand    = extractJsonStringField(requestBody, "brand");
        String category = extractJsonStringField(requestBody, "category");
        String priceStr = extractJsonNumberField(requestBody, "price");

        if (name == null || name.isEmpty()) {
            return "{\"error\":\"Missing required field: 'name'\"}";
        }

        System.out.println("[Enrichment] Product: " + name + " (" + brand + ")");

        // 2) Build the AI prompt
        String prompt = buildEnrichmentPrompt(name, brand, category, priceStr);

        // 3) Call Gemini AI
        System.out.println("[Enrichment] Calling Gemini AI...");
        String aiResponse;
        try {
            aiResponse = geminiClient.generateContent(prompt);
        } catch (IOException e) {
            System.err.println("[Enrichment] Gemini API failed: " + e.getMessage());
            // Fallback: generate a basic description without AI
            return buildFallbackResponse(name, brand, category, priceStr);
        }

        System.out.println("[Enrichment] AI response received.");

        // 4) Clean up the response
        String cleanResponse = cleanJsonResponse(aiResponse);

        // 5) Validate it looks like JSON
        if (!cleanResponse.trim().startsWith("{")) {
            // AI returned non-JSON, wrap the text as a description
            return buildManualResponse(name, brand, cleanResponse);
        }

        return cleanResponse;
    }

    /**
     * Build the prompt that instructs Gemini to enrich the product data.
     */
    private String buildEnrichmentPrompt(String name, String brand, String category, String price) {
        StringBuilder productInfo = new StringBuilder();
        productInfo.append("Product Name: ").append(name).append("\n");
        if (brand != null && !brand.isEmpty()) {
            productInfo.append("Brand: ").append(brand).append("\n");
        }
        if (category != null && !category.isEmpty()) {
            productInfo.append("Category: ").append(category).append("\n");
        }
        if (price != null && !price.isEmpty()) {
            productInfo.append("Price: $").append(price).append("\n");
        }

        return """
            You are a professional product copywriter for an online marketplace.
            
            A seller has listed a new product with these basic details:
            %s
            
            Your task:
            1. Write a compelling, professional product description (2-3 sentences)
               - Highlight key features and benefits
               - Use persuasive but honest language
               - Make it sound premium and appealing
            2. Generate 5-8 relevant search tags/keywords that buyers might use to find this product
            
            Respond ONLY with valid JSON in this exact format (no markdown, no explanation):
            {
              "name": "<original product name>",
              "brand": "<original brand or 'Generic'>",
              "description": "<your generated 2-3 sentence product description>",
              "tags": ["tag1", "tag2", "tag3", "tag4", "tag5"],
              "category_suggestion": "<suggested category if not provided>"
            }
            
            Important rules:
            - Description should be professional and marketplace-ready
            - Tags should be lowercase, single words or short phrases
            - Always respond with valid JSON only, no markdown code fences
            """.formatted(productInfo.toString());
    }

    /**
     * Build a fallback response when the AI is unavailable.
     */
    private String buildFallbackResponse(String name, String brand, String category, String price) {
        String safeName = escapeJson(name);
        String safeBrand = (brand != null && !brand.isEmpty()) ? escapeJson(brand) : "Generic";
        String safeCategory = (category != null && !category.isEmpty()) ? escapeJson(category) : "General";
        
        String description = "Quality " + safeName + " by " + safeBrand + ". "
            + "Available now at our marketplace.";

        return "{"
            + "\"name\":\"" + safeName + "\","
            + "\"brand\":\"" + safeBrand + "\","
            + "\"description\":\"" + escapeJson(description) + "\","
            + "\"tags\":[\"" + safeName.toLowerCase() + "\",\"" + safeBrand.toLowerCase() + "\",\"" + safeCategory.toLowerCase() + "\"],"
            + "\"warning\":\"AI enrichment unavailable, basic description generated\""
            + "}";
    }

    /**
     * Build a response when AI returns non-JSON text.
     */
    private String buildManualResponse(String name, String brand, String aiText) {
        String safeName = escapeJson(name);
        String safeBrand = (brand != null) ? escapeJson(brand) : "Generic";
        String safeDesc = escapeJson(aiText.length() > 500 ? aiText.substring(0, 500) : aiText);

        return "{"
            + "\"name\":\"" + safeName + "\","
            + "\"brand\":\"" + safeBrand + "\","
            + "\"description\":\"" + safeDesc + "\","
            + "\"tags\":[]"
            + "}";
    }

    // ───────────────────── JSON Utility Methods ─────────────────────

    /**
     * Extract a string field from JSON.
     */
    private String extractJsonStringField(String json, String fieldName) {
        String key = "\"" + fieldName + "\"";
        int keyIdx = json.indexOf(key);
        if (keyIdx == -1) return null;

        int colonIdx = json.indexOf(':', keyIdx + key.length());
        if (colonIdx == -1) return null;

        // Skip whitespace after colon
        int pos = colonIdx + 1;
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) pos++;

        if (pos >= json.length() || json.charAt(pos) != '"') return null;

        int valueStart = pos;
        int valueEnd = findClosingQuote(json, valueStart + 1);
        if (valueEnd == -1) return null;

        return json.substring(valueStart + 1, valueEnd);
    }

    /**
     * Extract a numeric field from JSON as a string.
     */
    private String extractJsonNumberField(String json, String fieldName) {
        String key = "\"" + fieldName + "\"";
        int keyIdx = json.indexOf(key);
        if (keyIdx == -1) return null;

        int colonIdx = json.indexOf(':', keyIdx + key.length());
        if (colonIdx == -1) return null;

        // Skip whitespace after colon
        int pos = colonIdx + 1;
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) pos++;

        // Read the number (digits, dots, minus)
        StringBuilder num = new StringBuilder();
        while (pos < json.length()) {
            char c = json.charAt(pos);
            if (Character.isDigit(c) || c == '.' || c == '-') {
                num.append(c);
                pos++;
            } else {
                break;
            }
        }

        return num.length() > 0 ? num.toString() : null;
    }

    /**
     * Find closing quote handling escaped characters.
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
     * Strip markdown code fences from AI response.
     */
    private String cleanJsonResponse(String response) {
        String trimmed = response.trim();
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
     * Escape a string for JSON.
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
