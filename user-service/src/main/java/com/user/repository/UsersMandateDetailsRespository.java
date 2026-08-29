package com.user.repository;

import com.user.model.UsersMandateDetails;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsersMandateDetailsRespository extends JpaRepository<UsersMandateDetails, Integer>
{
    @Query("FROM UsersMandateDetails u WHERE u.user_id = :user_id AND u.online_flag = 'NSE' AND u.online_code = :online_code AND u.nse_ach = :nse_ach AND u.client_name = :client_name")
    Optional<UsersMandateDetails> getNseUserMandateDetailsByUmrn(
            @Param("user_id") Integer userId,
            @Param("online_code") String onlineCode,
            @Param("nse_ach") String nseAch,
            @Param("client_name") String clientName
    );

    @Query("FROM UsersMandateDetails u WHERE u.user_id = :userId AND u.online_flag = :onlineFlag AND u.online_code = :onlineCode AND u.bank_account_number = :bankAccountNumber AND u.client_name = :clientName")
    List<UsersMandateDetails> findByAllFields(
            @Param("userId") Integer userId,
            @Param("onlineFlag") String onlineFlag,
            @Param("onlineCode") String onlineCode,
            @Param("bankAccountNumber") String bankAccountNumber,
            @Param("clientName") String clientName
    );

    @Query("FROM UsersMandateDetails m WHERE m.online_id = :online_id ORDER BY m.id ASC")
    List<UsersMandateDetails> findByOnlineId(@Param("online_id") Integer online_id);

    @Query(
            value = "SELECT * FROM users_mandate_details " +
                    "WHERE user_id = :user_id " +
                    "AND client_name = :client_name " +
                    "AND online_code = :online_code " +
                    "AND online_flag = :online_flag " +
                    "AND broker_code = :broker_code",
            nativeQuery = true
    )
    List<UsersMandateDetails> findMandateDetails(
            @Param("user_id") Integer user_id,
            @Param("client_name") String client_name,
            @Param("online_code") String online_code,
            @Param("online_flag") String online_flag,
            @Param("broker_code") String broker_code
    );

    @Query(
            value = "SELECT * FROM users_mandate_details " +
                    "WHERE client_name = :client_name " +
                    "AND online_code = :online_code " +
                    "AND online_flag = :online_flag",
            nativeQuery = true
    )
    List<UsersMandateDetails> findMandateDetailsByClientCode(
            @Param("client_name") String client_name,
            @Param("online_code") String online_code,
            @Param("online_flag") String online_flag
    );

    @Query(value = "SELECT * FROM users_mandate_details WHERE user_id = :userId AND client_name = :clientName AND online_id = :online_id AND online_flag='NSE'", nativeQuery = true)
    List<UsersMandateDetails> findByUseridAndClientName(@Param("userId") Integer userid, @Param("clientName") String clientName, @Param("online_id") String online_id);

    @Query(value = "SELECT * FROM users_mandate_details WHERE user_id = :userId AND client_name = :clientName AND online_id = :online_id AND online_flag=:online_flag", nativeQuery = true)
    List<UsersMandateDetails> findByUseridAndClientNameAndOnlineFlag(@Param("userId") Integer userid, @Param("clientName") String clientName, @Param("online_id") String online_id, @Param("online_flag") String online_flag);

    @Query("FROM UsersMandateDetails u WHERE u.user_id = :user_id AND u.online_flag = 'NSE' AND u.online_code = :online_code AND u.client_name = :client_name AND u.broker_code = :broker_code")
    List<UsersMandateDetails> getMandateDetailsByClientCode(
            @Param("user_id") Integer userId,
            @Param("client_name") String clientName,
            @Param("online_code") String onlineCode,
            @Param("broker_code") String broker_code
    );

    @Query(
            value = "SELECT * FROM users_mandate_details " +
                    "WHERE user_id = :user_id " +
                    "AND client_name = :client_name " +
                    "AND online_code = :online_code " +
                    "AND online_flag = :online_flag " +
                    "AND broker_code = :broker_code " +
                    "AND nse_ach != '' " +
                    "AND (nse_ach_approved = 1 " +
                    "     OR (nse_ach_approved = 0 AND " +
                    "         (nse_ach_rej_reason = '' " +
                    "          OR nse_ach_rej_reason LIKE '%EXCHANGE%' " +
                    "          OR nse_ach_rej_reason LIKE '%UNDER PROCESS%' " +
                    "          OR nse_ach_rej_reason LIKE '%PENDING%' " +
                    "          OR nse_ach_rej_reason LIKE '%AGENCY%')))",
            nativeQuery = true
    )
    List<UsersMandateDetails> findMandateDetailsAch(
            @Param("user_id") Integer user_id,
            @Param("client_name") String client_name,
            @Param("online_code") String online_code,
            @Param("online_flag") String online_flag,
            @Param("broker_code") String broker_code
    );

    @Modifying
    @Transactional
    @Query("DELETE FROM UsersMandateDetails u WHERE u.user_id = :userId AND u.client_name = :clientName AND u.online_id = :online_code AND u.bank_account_number = :bank_account_number AND u.online_flag = :online_flag AND u.nse_ach = :nse_ach")
    int deleteByClientNameUserIdAndonlinecode(
            @Param("clientName") String clientName,
            @Param("userId") Integer userId,
            @Param("online_code") String online_code,
            @Param("bank_account_number") String bank_account_number,
            @Param("online_flag") String online_flag,
            @Param("nse_ach") String nse_ach
    );

}
