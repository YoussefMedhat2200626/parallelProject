package com.marketplace.service;

import com.marketplace.entity.Item;
import com.marketplace.entity.Transaction;
import com.marketplace.entity.Transaction.TransactionStatus;
import com.marketplace.entity.Transaction.TransactionType;
import com.marketplace.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the purchase flow:
 * 1. Validate item availability
 * 2. Reserve inventory
 * 3. Transfer funds
 * 4. Decrement stock
 * 5. Record transaction
 */
@Service
public class TransactionService {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionService.class);
    private final TransactionRepository transactionRepository;
    private final WalletService walletService;
    private final ItemService itemService;
    private final InventoryService inventoryService;

    public TransactionService(TransactionRepository transactionRepository,
                              WalletService walletService,
                              ItemService itemService,
                              InventoryService inventoryService) {
        this.transactionRepository = transactionRepository;
        this.walletService = walletService;
        this.itemService = itemService;
        this.inventoryService = inventoryService;
    }

    /**
     * Execute a purchase: transfers money, decrements stock, records transaction.
     */
    @Transactional
    public Transaction purchaseItem(Long buyerId, Long itemId, int quantity) {
        // 1. Validate item exists and is active
        Item item = itemService.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));

        if (item.getStatus() != Item.ItemStatus.ACTIVE) {
            throw new IllegalStateException("Item is not available for purchase");
        }
        if (item.getSellerId().equals(buyerId)) {
            throw new IllegalArgumentException("Cannot purchase your own item");
        }

        long totalCents = item.getPriceCents() * quantity;

        // 2. Check buyer has sufficient funds
        Long buyerBalance = walletService.getBalance(buyerId);
        if (buyerBalance < totalCents) {
            throw new IllegalStateException(
                    String.format("Insufficient funds. Required: $%.2f, Available: $%.2f",
                            totalCents / 100.0, buyerBalance / 100.0));
        }

        // 3. Reserve inventory
        boolean reserved = inventoryService.reserveStock(itemId, quantity);
        if (!reserved) {
            throw new IllegalStateException("Insufficient stock for item: " + item.getName());
        }

        // 4. Transfer funds from buyer to seller
        String ref;
        try {
            ref = walletService.transfer(buyerId, item.getSellerId(), totalCents);
        } catch (Exception e) {
            // Rollback inventory reservation by decrementing reserved count
            LOG.error("Fund transfer failed, rolling back reservation for item {}", itemId);
            throw e;
        }

        // 5. Decrement actual stock
        inventoryService.decrementStock(itemId, quantity);

        // 6. Check if item is sold out, mark as SOLD
        int remaining = inventoryService.getAvailableQuantity(itemId);
        if (remaining <= 0) {
            item.setStatus(Item.ItemStatus.SOLD);
        }

        // 7. Record transaction
        Transaction txn = new Transaction(buyerId, item.getSellerId(), itemId, quantity,
                totalCents, TransactionType.PURCHASE);
        txn.setStatus(TransactionStatus.COMPLETED);
        txn.setReferenceCode(ref);
        txn = transactionRepository.save(txn);

        LOG.info("Purchase completed: buyer={}, item={}, qty={}, total={} cents, ref={}",
                buyerId, itemId, quantity, totalCents, ref);
        return txn;
    }

    public List<Transaction> getBuyerTransactions(Long buyerId) {
        return transactionRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);
    }

    public List<Transaction> getSellerTransactions(Long sellerId) {
        return transactionRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
    }

    public List<Transaction> getUserTransactions(Long userId) {
        return transactionRepository.findAllByUserId(userId);
    }
}
