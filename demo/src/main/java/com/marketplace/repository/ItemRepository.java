package com.marketplace.repository;

import com.marketplace.entity.Item;
import com.marketplace.entity.Item.ItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findBySellerIdAndStatusNot(Long sellerId, ItemStatus status);

    List<Item> findBySellerId(Long sellerId);

    List<Item> findByStatus(ItemStatus status);

    @Query("SELECT i FROM Item i WHERE i.status = :status AND i.sellerId <> :userId " +
           "AND (LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(i.brand) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(i.category) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Item> searchItems(@Param("query") String query, @Param("userId") Long userId, @Param("status") ItemStatus status);

    @Query("SELECT i FROM Item i WHERE i.status = :status AND i.sellerId <> :userId")
    List<Item> findAllActiveExcludingSeller(@Param("userId") Long userId, @Param("status") ItemStatus status);

    @Query("SELECT i FROM Item i WHERE i.itemId = :itemId AND i.sellerId = :sellerId")
    Optional<Item> findByItemIdAndSellerId(@Param("itemId") Long itemId, @Param("sellerId") Long sellerId);
}
