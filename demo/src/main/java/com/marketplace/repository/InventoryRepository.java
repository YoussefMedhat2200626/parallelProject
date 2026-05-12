package com.marketplace.repository;

import com.marketplace.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByItemId(Long itemId);
    List<Inventory> findBySellerId(Long sellerId);

    @Modifying
    @Query("UPDATE Inventory inv SET inv.quantity = inv.quantity - :qty, inv.reserved = inv.reserved - :qty " +
           "WHERE inv.itemId = :itemId AND inv.quantity >= :qty")
    int decrementQuantity(@Param("itemId") Long itemId, @Param("qty") int qty);

    @Modifying
    @Query("UPDATE Inventory inv SET inv.reserved = inv.reserved + :qty " +
           "WHERE inv.itemId = :itemId AND (inv.quantity - inv.reserved) >= :qty")
    int reserveStock(@Param("itemId") Long itemId, @Param("qty") int qty);
}
