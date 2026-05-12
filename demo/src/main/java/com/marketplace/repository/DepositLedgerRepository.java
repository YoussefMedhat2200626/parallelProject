package com.marketplace.repository;

import com.marketplace.entity.DepositLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DepositLedgerRepository extends JpaRepository<DepositLedger, Long> {
    List<DepositLedger> findByUserIdOrderByCreatedAtDesc(Long userId);
}
