package com.user.repository;

import com.user.model.InvestorTransactionCams;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvestorTransactionCamsRepository extends JpaRepository<InvestorTransactionCams, Integer> {

        @Query("FROM InvestorTransactionCams i " +
                "WHERE i.user_id = :userId " +
                "AND i.client_name = :clientName " +
                "AND i.prodcode IN :prodcode " +
                "AND i.folio_no = :folioNo " +
                "ORDER BY i.traddate ASC, i.units DESC")
        List<InvestorTransactionCams> findByUserIdClientNameProdcodeAndFolioNo(
                @Param("userId") Integer userId,
                @Param("clientName") String clientName,
                @Param("prodcode") List<String> prodcode,
                @Param("folioNo") String folioNo
        );

        @Query("FROM InvestorTransactionCams i " +
                "WHERE i.user_id = :userId AND i.client_name = :clientName " +
                "GROUP BY i.prodcode, i.folio_no " +
                "ORDER BY i.scheme ASC, i.traddate ASC")
        List<InvestorTransactionCams> findGroupedByProdcodeAndFolioNo(
                @Param("userId") Integer userId,
                @Param("clientName") String clientName
        );

    @Query("FROM InvestorTransactionCams i " +
            "WHERE i.user_id = :userId AND i.client_name = :clientName " +
            "GROUP BY i.prodcode, i.folio_no " +
            "ORDER BY i.scheme ASC, i.traddate ASC")
    List<InvestorTransactionCams> findGroupedByProdcodeAndFolioNoBytaxStatus(
            @Param("userId") Integer userId,
            @Param("clientName") String clientName
    );

        @Query("FROM InvestorTransactionCams i " +
                "WHERE i.folio_no = :folioNo " +
                "AND i.prodcode = :prodcode " +
                "AND i.user_id = :userId " +
                "AND i.client_name = :clientName " +
                "ORDER BY i.traddate ASC, i.units DESC")
        List<InvestorTransactionCams> findByFolioNoAndProdcodeAndUserIdAndClientName(
                @Param("folioNo") String folioNo,
                @Param("prodcode") String prodcode,
                @Param("userId") Integer userId,
                @Param("clientName") String clientName
        );

}
