package com.user.repository;

import com.user.model.InvestorMasterCams;
import com.user.model.InvestorMasterKarvy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface InvestorMasterKarvyRepository extends JpaRepository<InvestorMasterKarvy, Integer> {

    @Query("SELECT i FROM InvestorMasterKarvy i " +
            "WHERE i.user_id = :user_id AND i.client_name = :client_name AND i.fund = :fund")
    List<InvestorMasterKarvy> findByUserIdAndClientNameAndFund(
            @Param("user_id") Integer userId,
            @Param("client_name") String clientName,
            @Param("fund") String amc_code
    );

    @Query("FROM InvestorMasterKarvy i " +
            "WHERE i.user_id = :user_id AND i.client_name = :client_name AND i.fund = :fund")
    List<InvestorMasterKarvy> findByUserIdClientNameAndFund(
            @Param("user_id") Integer userId,
            @Param("client_name") String clientName,
            @Param("fund") String fund
    );

    @Query("FROM InvestorMasterKarvy i WHERE i.user_id = :user_id AND i.client_name = :client_name AND i.product_code LIKE CONCAT(:scheme_code, '%')")
    List<InvestorMasterKarvy> findByUserIdAndClientNameAndProductCodeStartsWith(
            @Param("user_id") Integer userId,
            @Param("client_name") String clientName,
            @Param("scheme_code") String schemeCode
    );
    @Query("FROM InvestorMasterKarvy i WHERE i.user_id = :user_id AND i.client_name = :client_name AND i.product_code = :product_code")
    List<InvestorMasterKarvy> findByUserIdAndClientNameAndProductCode(
            @Param("user_id") Integer user_id,
            @Param("client_name") String client_name,
            @Param("product_code") String scheme_name
    );

    @Query("SELECT i FROM InvestorMasterKarvy i " +
            "WHERE i.user_id = :user_id AND i.client_name = :client_name")
    List<InvestorMasterKarvy> findByKarvyUserIdAndClientName(@Param("user_id") Integer userId,
                                                       @Param("client_name") String clientName);


    @Query("SELECT i FROM InvestorMasterKarvy i WHERE i.user_id = :user_id AND i.client_name = :client_name AND i.tax_status IN :tax_status AND i.mode_of_holding_description = :holding_na AND i.broker_code = :broker_code")
    List<InvestorMasterKarvy> findByKarvyUserIdAndClientNameAndTaxstatus(@Param("user_id") Integer userId,
                                                                         @Param("client_name") String clientName,
                                                                         @Param("tax_status") List<String> taxStatus,
                                                                         @Param("holding_na") String holding_na,
                                                                         @Param("broker_code") String broker_cod);


}
