package com.marketplace.rest;

import com.marketplace.entity.Item;
import com.marketplace.service.ItemService;
import com.marketplace.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Web Service #1: Item Search API
 * Provides text-based search by name, brand, and category.
 */
@RestController
@RequestMapping("/api/v1/items")
public class ItemRestController {

    private final ItemService itemService;
    private final InventoryService inventoryService;

    public ItemRestController(ItemService itemService, InventoryService inventoryService) {
        this.itemService = itemService;
        this.inventoryService = inventoryService;
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchItems(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false, defaultValue = "0") Long excludeSeller) {

        List<Item> items = itemService.searchItems(q, excludeSeller);
        List<Map<String, Object>> results = items.stream().map(item -> {
            Map<String, Object> map = new HashMap<>();
            map.put("itemId", item.getItemId());
            map.put("name", item.getName());
            map.put("description", item.getDescription());
            map.put("brand", item.getBrand());
            map.put("category", item.getCategory());
            map.put("priceCents", item.getPriceCents());
            map.put("priceFormatted", item.getPriceFormatted());
            map.put("status", item.getStatus());
            map.put("sellerId", item.getSellerId());
            map.put("availableQty", inventoryService.getAvailableQuantity(item.getItemId()));
            return map;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("query", q);
        response.put("resultCount", results.size());
        response.put("items", results);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<?> getItem(@PathVariable Long itemId) {
        return itemService.findById(itemId)
                .map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("itemId", item.getItemId());
                    map.put("name", item.getName());
                    map.put("description", item.getDescription());
                    map.put("brand", item.getBrand());
                    map.put("category", item.getCategory());
                    map.put("priceCents", item.getPriceCents());
                    map.put("priceFormatted", item.getPriceFormatted());
                    map.put("status", item.getStatus());
                    map.put("availableQty", inventoryService.getAvailableQuantity(item.getItemId()));
                    return ResponseEntity.ok((Object) map);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
