package com.user.repository;

import com.user.model.InvestorMasterCams;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvestorMasterCamsRepository extends JpaRepository<InvestorMasterCams, Integer> {

    @Query("SELECT i FROM InvestorMasterCams i " +
            "WHERE i.user_id = :user_id AND i.client_name = :client_name AND i.amc_code = :amc_code " +
            "AND i.sch_name IS NOT NULL AND i.sch_name != ''")
    List<InvestorMasterCams> findInvestorMasterCams(Integer user_id, String client_name, String amc_code);


    @Query("SELECT i FROM InvestorMasterCams i " +
            "WHERE i.user_id = :user_id AND i.client_name = :client_name AND i.product IN :product")
    List<InvestorMasterCams> findByUserIdClientNameAndProductIn(@Param("user_id") Integer userId,
                                                                @Param("client_name") String clientName,
                                                                @Param("product") String product);

    @Query("SELECT i FROM InvestorMasterCams i " +
            "WHERE i.user_id = :user_id AND i.client_name = :client_name")
    List<InvestorMasterCams> findByCamsUserIdAndClientName(@Param("user_id") Integer userId,
                                                       @Param("client_name") String clientName);


    @Query("SELECT i FROM InvestorMasterCams i WHERE i.user_id = :userId AND i.client_name = :clientName AND i.product IN :products")
    List<InvestorMasterCams> findByUserIdAndClientNameAndProductIn(
            @Param("userId") Integer userId,
            @Param("clientName") String clientName,
            @Param("products") List<String> products
    );

    @Query("SELECT i FROM InvestorMasterCams i WHERE i.user_id = :userId AND i.client_name = :clientName AND i.tax_status = :tax_status AND i.holding_na = :holding_na AND i.joint1_pan = :joint1_pan AND i.broker_cod = :broker_cod")
    List<InvestorMasterCams> findByuserIdAndClientnameAndTaxstatus(
            @Param("userId") Integer userId,
            @Param("clientName") String clientName,
            @Param("tax_status") String tax_status,
            @Param("holding_na") String holding_na,
            @Param("joint1_pan") String joint1_pan,
            @Param("broker_cod") String broker_cod
    );


}
