package com.marketplace.rest;

import com.marketplace.entity.Inventory;
import com.marketplace.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Web Service #3: Inventory Management API
 * View and update stock levels for items.
 */
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryRestController {

    private final InventoryService inventoryService;

    public InventoryRestController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<?> getInventory(@PathVariable Long itemId) {
        return inventoryService.getByItemId(itemId)
                .map(inv -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("itemId", inv.getItemId());
                    map.put("quantity", inv.getQuantity());
                    map.put("reserved", inv.getReserved());
                    map.put("available", inv.getAvailable());
                    return ResponseEntity.ok((Object) map);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<Map<String, Object>>> getSellerInventory(@PathVariable Long sellerId) {
        List<Inventory> inventories = inventoryService.getSellerInventory(sellerId);
        List<Map<String, Object>> result = inventories.stream().map(inv -> {
            Map<String, Object> map = new HashMap<>();
            map.put("itemId", inv.getItemId());
            map.put("quantity", inv.getQuantity());
            map.put("reserved", inv.getReserved());
            map.put("available", inv.getAvailable());
            return map;
        }).toList();
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<?> updateInventory(@PathVariable Long itemId,
                                              @RequestBody Map<String, Integer> body) {
        int newQty = body.getOrDefault("quantity", 0);
        try {
            Inventory updated = inventoryService.updateQuantity(itemId, newQty);
            Map<String, Object> map = new HashMap<>();
            map.put("itemId", updated.getItemId());
            map.put("quantity", updated.getQuantity());
            map.put("available", updated.getAvailable());
            map.put("message", "Inventory updated successfully");
            return ResponseEntity.ok(map);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
