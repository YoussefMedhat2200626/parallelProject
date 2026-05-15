package com.marketplace.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Service
public class CsvImportService {

    private static final Logger LOG = LoggerFactory.getLogger(CsvImportService.class);
    private static final List<String> REQUIRED_HEADER = List.of(
            "name", "description", "brand", "category", "price", "quantity");

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

        if (file == null || file.isEmpty()) {
            errors.add("Please upload a non-empty CSV file");
            return new CsvImportResult(0, 1, errors);
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            errors.add("Please upload a .csv file");
            return new CsvImportResult(0, 1, errors);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (isHeader) {
                    isHeader = false;
                    String[] header = parseCsvLine(stripBom(line));
                    if (!isValidHeader(header)) {
                        errors.add("Line 1: Expected header name,description,brand,category,price,quantity");
                        return new CsvImportResult(0, 1, errors);
                    }
                    continue;
                }
                if (line.trim().isEmpty()) continue;

                try {
                    String[] parts = parseCsvLine(line);
                    if (parts.length != 6) {
                        errors.add("Line " + lineNum + ": Expected 6 columns (name,description,brand,category,price,quantity)");
                        fail++;
                        continue;
                    }

                    String name = parts[0].trim();
                    String description = parts[1].trim();
                    String brand = parts[2].trim();
                    String category = parts[3].trim();
                    long priceCents = parsePriceCents(parts[4].trim());
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
                    if (quantity < 0) {
                        errors.add("Line " + lineNum + ": Quantity must be zero or positive");
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

            if (lineNum == 0) {
                errors.add("Please upload a non-empty CSV file");
                fail++;
            }
        } catch (Exception e) {
            errors.add("Failed to read CSV file: " + e.getMessage());
            fail++;
        }

        LOG.info("CSV import complete: {} success, {} failed", success, fail);
        return new CsvImportResult(success, fail, errors);
    }

    private boolean isValidHeader(String[] header) {
        if (header.length != REQUIRED_HEADER.size()) {
            return false;
        }
        return Arrays.stream(header)
                .map(String::trim)
                .map(String::toLowerCase)
                .toList()
                .equals(REQUIRED_HEADER);
    }

    private long parsePriceCents(String value) {
        BigDecimal price = new BigDecimal(value);
        return price.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private String stripBom(String line) {
        if (line != null && !line.isEmpty() && line.charAt(0) == '\uFEFF') {
            return line.substring(1);
        }
        return line;
    }


    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
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
