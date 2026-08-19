package com.user.repository;

import com.user.model.UserMandateDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserMandateDetailsRespository extends JpaRepository<UserMandateDetails, Integer>
{
    @Query("FROM UserMandateDetails u WHERE u.user_id = :user_id AND u.online_flag = 'NSE' AND u.online_code = :online_code AND u.nse_ach = :nse_ach AND u.client_name = :client_name")
    Optional<UserMandateDetails> getNseUserMandateDetailsByUmrn(
            @Param("user_id") Integer userId,
            @Param("online_code") String onlineCode,
            @Param("nse_ach") String nseAch,
            @Param("client_name") String clientName
    );

    @Query("FROM UserMandateDetails u WHERE u.user_id = :userId AND u.online_flag = :onlineFlag AND u.online_code = :onlineCode AND u.bank_account_number = :bankAccountNumber AND u.client_name = :clientName")
    List<UserMandateDetails> findByAllFields(
            @Param("userId") Integer userId,
            @Param("onlineFlag") String onlineFlag,
            @Param("onlineCode") String onlineCode,
            @Param("bankAccountNumber") String bankAccountNumber,
            @Param("clientName") String clientName
    );

    @Query(
            value = "SELECT * FROM users_mandate_details " +
                    "WHERE user_id = :user_id " +
                    "AND client_name = :client_name " +
                    "AND online_code = :online_code " +
                    "AND online_flag = :online_flag " +
                    "AND broker_code = :broker_code",
            nativeQuery = true
    )
    List<UserMandateDetails> findMandateDetails(
            @Param("user_id") Integer user_id,
            @Param("client_name") String client_name,
            @Param("online_code") String online_code,
            @Param("online_flag") String online_flag,
            @Param("broker_code") String broker_code
    );

}
