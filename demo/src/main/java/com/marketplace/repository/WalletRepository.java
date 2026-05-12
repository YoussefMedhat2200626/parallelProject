package com.marketplace.repository;

import com.marketplace.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserId(Long userId);

    @Modifying
    @Query("UPDATE Wallet w SET w.balanceCents = w.balanceCents + :amount WHERE w.userId = :userId AND w.balanceCents + :amount >= 0")
    int updateBalance(@Param("userId") Long userId, @Param("amount") Long amount);
}
