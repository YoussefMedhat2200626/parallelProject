package ai;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * Client for the Google Gemini API.
 * 
 * Uses java.net.HttpURLConnection (zero external dependencies).
 * Calls the Gemini 2.0 Flash model for fast, free AI text generation.
 * 
 * API Reference:
 *   https://ai.google.dev/gemini-api/docs/text-generation
 */
public class GeminiClient {

    private static final String GEMINI_API_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    
    // Timeouts
    private static final int CONNECT_TIMEOUT_MS = 15_000;  // 15 seconds
    private static final int READ_TIMEOUT_MS    = 60_000;  // 60 seconds (AI generation can take time)

    private final String apiKey;

    public GeminiClient(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Send a prompt to Gemini and return the generated text response.
     *
     * @param prompt The text prompt to send to the model
     * @return The generated text response from Gemini
     * @throws IOException if the API call fails
     */
    public String generateContent(String prompt) throws IOException {
        URL url = new URL(GEMINI_API_URL + "?key=" + apiKey);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            // Configure the connection
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);

            // Build the request body (manual JSON to avoid external dependency)
            String requestBody = buildRequestJson(prompt);

            // Send the request
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input);
                os.flush();
            }

            // Read the response
            int statusCode = conn.getResponseCode();
            String responseBody = readStream(
                statusCode >= 200 && statusCode < 300
                    ? conn.getInputStream()
                    : conn.getErrorStream()
            );

            // Handle errors
            if (statusCode < 200 || statusCode >= 300) {
                throw new IOException(
                    "Gemini API error (HTTP " + statusCode + "): " + responseBody
                );
            }

            // Extract the generated text from the response
            return extractGeneratedText(responseBody);

        } finally {
            conn.disconnect();
        }
    }

    /**
     * Build the Gemini API request JSON.
     * 
     * Structure:
     * {
     *   "contents": [{
     *     "parts": [{ "text": "prompt" }]
     *   }],
     *   "generationConfig": {
     *     "temperature": 0.7,
     *     "maxOutputTokens": 2048
     *   }
     * }
     */
    private String buildRequestJson(String prompt) {
        // Escape the prompt for safe JSON embedding
        String escapedPrompt = escapeJson(prompt);
        
        return "{"
            + "\"contents\":[{"
            +   "\"parts\":[{\"text\":\"" + escapedPrompt + "\"}]"
            + "}],"
            + "\"generationConfig\":{"
            +   "\"temperature\":0.7,"
            +   "\"maxOutputTokens\":2048"
            + "}"
            + "}";
    }

    /**
     * Extract the generated text from the Gemini API response.
     * 
     * Response structure:
     * {
     *   "candidates": [{
     *     "content": {
     *       "parts": [{ "text": "generated text here" }]
     *     }
     *   }]
     * }
     */
    private String extractGeneratedText(String responseJson) throws IOException {
        // Find the text field inside candidates[0].content.parts[0].text
        // We navigate the JSON structure manually to avoid external dependencies
        
        String marker = "\"text\"";
        
        // Find the candidates array, then navigate to the text field
        int candidatesIdx = responseJson.indexOf("\"candidates\"");
        if (candidatesIdx == -1) {
            throw new IOException("Invalid Gemini response: no 'candidates' field found");
        }
        
        // Find the "parts" array within candidates
        int partsIdx = responseJson.indexOf("\"parts\"", candidatesIdx);
        if (partsIdx == -1) {
            throw new IOException("Invalid Gemini response: no 'parts' field found");
        }
        
        // Find the "text" field within parts
        int textKeyIdx = responseJson.indexOf(marker, partsIdx);
        if (textKeyIdx == -1) {
            throw new IOException("Invalid Gemini response: no 'text' field found in parts");
        }
        
        // Find the colon after "text"
        int colonIdx = responseJson.indexOf(':', textKeyIdx + marker.length());
        if (colonIdx == -1) {
            throw new IOException("Invalid Gemini response: malformed 'text' field");
        }
        
        // Find the opening quote of the value
        int valueStart = responseJson.indexOf('"', colonIdx + 1);
        if (valueStart == -1) {
            throw new IOException("Invalid Gemini response: no value for 'text' field");
        }
        
        // Find the closing quote (handling escaped quotes)
        int valueEnd = findClosingQuote(responseJson, valueStart + 1);
        if (valueEnd == -1) {
            throw new IOException("Invalid Gemini response: unterminated text value");
        }
        
        String rawText = responseJson.substring(valueStart + 1, valueEnd);
        return unescapeJson(rawText);
    }

    /**
     * Find the closing quote of a JSON string, handling escaped characters.
     */
    private int findClosingQuote(String json, int startIdx) {
        for (int i = startIdx; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\') {
                i++; // Skip the escaped character
            } else if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    /**
     * Escape a string for safe embedding in JSON.
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                case '\b': sb.append("\\b");  break;
                case '\f': sb.append("\\f");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * Unescape a JSON string value.
     */
    private String unescapeJson(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                switch (next) {
                    case '"':  sb.append('"');  i++; break;
                    case '\\': sb.append('\\'); i++; break;
                    case 'n':  sb.append('\n'); i++; break;
                    case 'r':  sb.append('\r'); i++; break;
                    case 't':  sb.append('\t'); i++; break;
                    case 'b':  sb.append('\b'); i++; break;
                    case 'f':  sb.append('\f'); i++; break;
                    case 'u':
                        if (i + 5 < text.length()) {
                            String hex = text.substring(i + 2, i + 6);
                            sb.append((char) Integer.parseInt(hex, 16));
                            i += 5;
                        }
                        break;
                    default:
                        sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Read an InputStream fully into a String.
     */
    private String readStream(InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            char[] buffer = new char[4096];
            int charsRead;
            while ((charsRead = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, charsRead);
            }
        }
        return sb.toString();
    }
}
