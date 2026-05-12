package com.marketplace.repository;

import com.marketplace.entity.ExternalStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ExternalStoreRepository extends JpaRepository<ExternalStore, Long> {
    Optional<ExternalStore> findByApiKey(String apiKey);
}
