package com.marketplace.service;

import com.marketplace.entity.Inventory;
import com.marketplace.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Manages inventory / stock levels for items.
 */
@Service
public class InventoryService {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryService.class);
    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public Optional<Inventory> getByItemId(Long itemId) {
        return inventoryRepository.findByItemId(itemId);
    }

    public List<Inventory> getSellerInventory(Long sellerId) {
        return inventoryRepository.findBySellerId(sellerId);
    }

    @Transactional
    public Inventory updateQuantity(Long itemId, int newQuantity) {
        Inventory inv = inventoryRepository.findByItemId(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found for item: " + itemId));
        inv.setQuantity(newQuantity);
        return inventoryRepository.save(inv);
    }

    /**
     * Reserve stock for a pending purchase. Returns true if successful.
     */
    @Transactional
    public boolean reserveStock(Long itemId, int quantity) {
        int updated = inventoryRepository.reserveStock(itemId, quantity);
        if (updated > 0) {
            LOG.info("Reserved {} units of item {}", quantity, itemId);
            return true;
        }
        return false;
    }

    /**
     * Decrement stock after a completed purchase.
     */
    @Transactional
    public boolean decrementStock(Long itemId, int quantity) {
        int updated = inventoryRepository.decrementQuantity(itemId, quantity);
        return updated > 0;
    }

    public int getAvailableQuantity(Long itemId) {
        return inventoryRepository.findByItemId(itemId)
                .map(Inventory::getAvailable)
                .orElse(0);
    }
}
