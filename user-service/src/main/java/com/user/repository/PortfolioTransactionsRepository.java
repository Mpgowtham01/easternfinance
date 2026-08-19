package com.user.repository;

import com.user.model.PortfolioTransactions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioTransactionsRepository extends JpaRepository<PortfolioTransactions, Long> {

    @Query("""
        FROM PortfolioTransactions p
        WHERE p.folio_no = :folioNo
          AND p.scheme_code = :schemeCode
          AND p.user_id = :userId
          AND p.client_name = :clientName
        ORDER BY p.traddate ASC, p.units DESC
    """)
    List<PortfolioTransactions> findByFolioSchemeAndUser(
            @Param("folioNo") String folioNo,
            @Param("schemeCode") String schemeCode,
            @Param("userId") Integer userId,
            @Param("clientName") String clientName
    );

    // Converted group by query
    @Query("""
        SELECT p
        FROM PortfolioTransactions p
        WHERE p.user_id = :userId
          AND p.client_name = :clientName
        GROUP BY p.scheme_code, p.folio_no
        ORDER BY p.scheme ASC, p.traddate ASC
    """)
    List<PortfolioTransactions> findGroupedBySchemeAndFolio(
            @Param("userId") Integer userId,
            @Param("clientName") String clientName
    );
}
