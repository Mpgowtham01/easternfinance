package com.nse.repository;

import com.nse.model.NseTransactions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NseTransactionRepository extends JpaRepository<NseTransactions, Integer> {

    @Query("FROM NseTransactions nt WHERE nt.service_return_code = '0' AND nt.user_id = :userId AND nt.client_name = :clientName AND nt.transaction_type IN :transactionTypes AND nt.transaction_status NOT IN :transactionStatuses")
    List<NseTransactions> findValidTransactionsWithExclusion(
            @Param("userId") Integer userId,
            @Param("clientName") String clientName,
            @Param("transactionTypes") List<String> transactionTypes,
            @Param("transactionStatuses") List<String> transactionStatuses
    );

    @Query("FROM NseTransactions nt WHERE nt.service_return_code = '0' AND nt.user_id = :userId AND nt.client_name = :clientName AND nt.transaction_type = :transactionType")
    List<NseTransactions> findByUserClientAndType(
            @Param("userId") Integer userId,
            @Param("clientName") String clientName,
            @Param("transactionType") String transactionType
    );

    @Query("FROM NseTransactions nt WHERE nt.iin_number = :iinNumber AND nt.client_name = :clientName AND nt.transaction_type = :transactionType AND (nt.purchase_type = 'Physical' OR nt.purchase_type = 'P') AND nt.service_return_code = '0'")
    List<NseTransactions> findPhysicalTransactionsByIinAndClient(
            @Param("iinNumber") String iinNumber,
            @Param("clientName") String clientName,
            @Param("transactionType") String transactionType
    );

    @Query("FROM NseTransactions nt WHERE nt.user_id = :userId AND nt.client_name = :clientName AND nt.transaction_type NOT IN ('UCC Request','Fatca Request','Joint Holder1 Fatca Request','Joint Holder2 Fatca Request','ACH Mandate Request','Add Bank Details','Delete Bank Details') ORDER BY nt.transaction_date DESC")
    List<NseTransactions> findNonRequestTransactionsOrderedByDate(
            @Param("userId") Integer userId,
            @Param("clientName") String clientName
    );

    @Query("FROM NseTransactions n WHERE n.unique_number = :unique_number AND n.client_name = :client_name")
    Optional<NseTransactions> getNseTransactionDetails(@Param("unique_number") String uniqueNumber, @Param("client_name") String clientName);

    @Query("FROM NseTransactions nt WHERE nt.iin_number = :iinNumber AND nt.payment_ref_no = :paymentRefNo")
    List<NseTransactions> findByIinNumberAndPaymentRefNo(
            @Param("iinNumber") String iinNumber,
            @Param("paymentRefNo") String paymentRefNo
    );

    @Query("FROM NseTransactions nt WHERE nt.user_id = :userId AND nt.client_name = :clientName ORDER BY nt.transaction_date DESC")
    Page<NseTransactions> findByUserIdAndClientNameOrderByTxnDateDesc(
            @Param("userId") Integer userId,
            @Param("clientName") String clientName,
            Pageable pageable
    );

    @Query("""
    SELECT DISTINCT nt.iin_number
    FROM NseTransactions nt
    WHERE nt.user_id = :userId
      AND nt.client_name = :clientName
      AND nt.scheme_name = :schemeName
""")
    String findIinByUserClientAndScheme(
            @Param("userId") Integer userId,
            @Param("clientName") String clientName,
            @Param("schemeName") String schemeName
    );


}
