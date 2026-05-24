package com.marketplace.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketplace.entity.Inventory;
import com.marketplace.entity.Item;
import com.marketplace.entity.Item.ItemStatus;
import com.marketplace.repository.InventoryRepository;
import com.marketplace.repository.ItemRepository;

/**
 * Manages marketplace items — create, edit, remove, search.
 * Each item is tied to a seller and has associated inventory.
 */
@Service
public class ItemService {

    private static final Logger LOG = LoggerFactory.getLogger(ItemService.class);
    private final ItemRepository itemRepository;
    private final InventoryRepository inventoryRepository;

    public ItemService(ItemRepository itemRepository, InventoryRepository inventoryRepository) {
        this.itemRepository = itemRepository;
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * Create a new item listing with initial inventory.
     */
    @Transactional
    public Item createItem(Long sellerId, String name, String description, String brand,
                           String category, Long priceCents, int initialQuantity) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Item name is required");
        if (priceCents <= 0) throw new IllegalArgumentException("Price must be greater than zero");
        Item item = new Item(sellerId, name, description, brand, category, priceCents);
        item = itemRepository.save(item);

        Inventory inventory = new Inventory(item.getItemId(), sellerId, initialQuantity);
        inventoryRepository.save(inventory);

        LOG.info("Item created: {} (id={}) by seller {}", name, item.getItemId(), sellerId);
        return item;
    }

    /**
     * Update an existing item's details.
     */
    @Transactional
    public Item updateItem(Long itemId, Long sellerId, String name, String description,
                           String brand, String category, Long priceCents) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Item name is required");
        if (priceCents <= 0) throw new IllegalArgumentException("Price must be greater than zero");

        Item item = itemRepository.findByItemIdAndSellerId(itemId, sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found or unauthorized"));
        if (item.getStatus() == ItemStatus.SOLD) {
          throw new IllegalArgumentException("Cannot edit a sold item");
        }
        item.setName(name);
        item.setDescription(description);
        item.setBrand(brand);
        item.setCategory(category);
        item.setPriceCents(priceCents);
        item.setUpdatedAt(LocalDateTime.now());
        return itemRepository.save(item);
    }

    /**
     * Remove (soft-delete) an item.
     */
    @Transactional
    public void removeItem(Long itemId, Long sellerId) {
        Item item = itemRepository.findByItemIdAndSellerId(itemId, sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found or unauthorized"));
        item.setStatus(ItemStatus.REMOVED);
        itemRepository.save(item);
        inventoryRepository.findByItemId(itemId).ifPresent(inv -> {
        inv.setReserved(0);
        inventoryRepository.save(inv);});
        LOG.info("Item removed: {} (id={})", item.getName(), itemId);
    }

    public Optional<Item> findById(Long itemId) {
        if (itemId == null) return Optional.empty();
        return itemRepository.findById(itemId);
    }

    /**
     * Get all active items for a seller.
     */
    public List<Item> getSellerItems(Long sellerId) {
        return itemRepository.findBySellerIdAndStatusNot(sellerId, ItemStatus.REMOVED);
    }

    /**
     * Search items by name, brand, or category — excludes seller's own items.
     * Falls back to phonetic (SOUNDEX) fuzzy search when the exact match finds nothing,
     * enabling typo-tolerant queries such as "cheaost" matching "cheapest".
     */
    public List<Item> searchItems(String query, Long currentUserId) {
        if (query == null || query.isBlank()) {
            return itemRepository.findAllActiveExcludingSeller(currentUserId, ItemStatus.ACTIVE);
        }
        String trimmed = query.trim();
        List<Item> results = itemRepository.searchItems(trimmed, currentUserId, ItemStatus.ACTIVE);
        if (!results.isEmpty()) {
            return results;
        }
        // Fallback: phonetic + prefix fuzzy search using SOUNDEX
        String prefix = trimmed.length() > 3 ? trimmed.substring(0, 3).toLowerCase() : trimmed.toLowerCase();
        LOG.info("Exact search found no results for '{}', falling back to fuzzy search", trimmed);
        return itemRepository.fuzzySearchItems(trimmed, prefix, currentUserId, ItemStatus.ACTIVE.name());
    }

    /**
     * Get all active items for browsing (excluding current user's items).
     */
    public List<Item> browseItems(Long currentUserId) {
        return itemRepository.findAllActiveExcludingSeller(currentUserId, ItemStatus.ACTIVE);
    }

    /*
        * Save item - used for status updates (e.g. marking as SOLD)
    */

    public Item saveItem(Item item) {
    return itemRepository.save(item);
    }

    

}
