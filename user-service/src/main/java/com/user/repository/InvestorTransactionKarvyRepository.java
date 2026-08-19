package com.user.repository;


import com.user.model.InvestorTransactionKarvy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvestorTransactionKarvyRepository extends JpaRepository<InvestorTransactionKarvy, Integer> {


        @Query("FROM InvestorTransactionKarvy t " +
                "WHERE t.user_id = :userId " +
                "AND t.client_name = :clientName " +
                "AND CONCAT(t.fund, '', t.scheme_code) IN :prodCodes " +
                "AND t.folio_number = :folioNumber " +
                "ORDER BY t.transaction_date ASC, t.units DESC")
        List<InvestorTransactionKarvy> findByUserIdClientNameAndProductCodesAndFolio(
                @Param("userId") Integer userId,
                @Param("clientName") String clientName,
                @Param("prodCodes") List<String> prodCodes,
                @Param("folioNumber") String folioNumber
        );

                @Query("""
            SELECT t 
            FROM InvestorTransactionKarvy t
            WHERE t.user_id = :userId
            AND t.client_name = :clientName
            GROUP BY t.fund, t.scheme_code, t.folio_number
            ORDER BY t.fund_description ASC, t.transaction_date ASC
        """)
                List<InvestorTransactionKarvy> findGroupedTransactions(
                        @Param("userId") Integer userId,
                        @Param("clientName") String clientName);


                @Query("""
            SELECT t 
            FROM InvestorTransactionKarvy t
            WHERE t.folio_number = :folioNumber
            AND t.fund = :fund
            AND t.scheme_code = :schemeCode
            AND t.user_id = :userId
            AND t.client_name = :clientName
            ORDER BY t.transaction_date ASC, t.units DESC
        """)
                List<InvestorTransactionKarvy> findTransactionsByFolioAndFund(
                        @Param("folioNumber") String folioNumber,
                        @Param("fund") String fund,
                        @Param("schemeCode") String schemeCode,
                        @Param("userId") Integer userId,
                        @Param("clientName") String clientName);



}
