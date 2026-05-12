package com.marketplace.service;

import com.marketplace.entity.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV Import service for bulk product creation.
 * Expected CSV format: name,description,brand,category,price,quantity
 * First line is treated as header and skipped.
 */
@Service
public class CsvImportService {

    private static final Logger LOG = LoggerFactory.getLogger(CsvImportService.class);
    private final ItemService itemService;

    public CsvImportService(ItemService itemService) {
        this.itemService = itemService;
    }

    public record CsvImportResult(int successCount, int failCount, List<String> errors) {}

    public CsvImportResult importItems(MultipartFile file, Long sellerId) {
        List<String> errors = new ArrayList<>();
        int success = 0;
        int fail = 0;
        int lineNum = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                if (line.trim().isEmpty()) continue;

                try {
                    String[] parts = parseCsvLine(line);
                    if (parts.length < 6) {
                        errors.add("Line " + lineNum + ": Expected 6 columns (name,description,brand,category,price,quantity)");
                        fail++;
                        continue;
                    }

                    String name = parts[0].trim();
                    String description = parts[1].trim();
                    String brand = parts[2].trim();
                    String category = parts[3].trim();
                    long priceCents = Math.round(Double.parseDouble(parts[4].trim()) * 100);
                    int quantity = Integer.parseInt(parts[5].trim());

                    if (name.isEmpty()) {
                        errors.add("Line " + lineNum + ": Name is required");
                        fail++;
                        continue;
                    }
                    if (priceCents <= 0) {
                        errors.add("Line " + lineNum + ": Price must be positive");
                        fail++;
                        continue;
                    }

                    itemService.createItem(sellerId, name, description, brand, category, priceCents, quantity);
                    success++;
                } catch (NumberFormatException e) {
                    errors.add("Line " + lineNum + ": Invalid number format - " + e.getMessage());
                    fail++;
                } catch (Exception e) {
                    errors.add("Line " + lineNum + ": " + e.getMessage());
                    fail++;
                }
            }
        } catch (Exception e) {
            errors.add("Failed to read CSV file: " + e.getMessage());
        }

        LOG.info("CSV import complete: {} success, {} failed", success, fail);
        return new CsvImportResult(success, fail, errors);
    }

    /**
     * Simple CSV line parser that handles quoted fields.
     */
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}
