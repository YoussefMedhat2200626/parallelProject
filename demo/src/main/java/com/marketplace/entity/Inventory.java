package com.marketplace.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id")
    private Long inventoryId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    @Column(name = "reserved", nullable = false)
    private Integer reserved = 0;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Inventory() {}

    public Inventory(Long itemId, Long sellerId, Integer quantity) {
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.quantity = quantity;
        this.reserved = 0;
        this.updatedAt = LocalDateTime.now();
    }

    public int getAvailable() {
        return quantity - reserved;
    }

    // Getters and Setters
    public Long getInventoryId() { return inventoryId; }
    public void setInventoryId(Long inventoryId) { this.inventoryId = inventoryId; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getReserved() { return reserved; }
    public void setReserved(Integer reserved) { this.reserved = reserved; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
