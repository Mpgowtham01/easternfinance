package com.auth.repository;

import com.auth.model.BseNseKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BseNseKeyRepository extends JpaRepository<BseNseKey, Integer>
{
    @Query(
            value = "SELECT * FROM bse_nse_key WHERE broker_code = :broker_code",
            nativeQuery = true
    )
    Optional<BseNseKey> findClientNameByBrokerCode(@Param("broker_code") String broker_code);
}
