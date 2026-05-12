package com.marketplace.repository;

import com.marketplace.entity.OtpCode;
import com.marketplace.entity.OtpCode.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<OtpCode, Long> {

    @Query("SELECT o FROM OtpCode o WHERE o.userId = :userId AND o.code = :code " +
           "AND o.purpose = :purpose AND o.used = false AND o.expiresAt > CURRENT_TIMESTAMP")
    Optional<OtpCode> findValidOtp(@Param("userId") Long userId,
                                    @Param("code") String code,
                                    @Param("purpose") OtpPurpose purpose);
}
