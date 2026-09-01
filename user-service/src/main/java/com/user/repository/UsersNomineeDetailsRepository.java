package com.user.repository;

import com.user.model.UsersNomineeDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsersNomineeDetailsRepository extends JpaRepository<UsersNomineeDetails, Integer>
{
    @Query("FROM UsersNomineeDetails b WHERE b.user_id = :user_id AND b.client_name = :client_name")
    UsersNomineeDetails findByUserIdAndClientName(
            @Param("user_id") Integer user_id,
            @Param("client_name") String client_name
    );

    @Query(value = "SELECT * FROM users_nominee_details WHERE user_id = :userId AND client_name = :clientName AND online_id = :online_id AND online_flag = :online_flag", nativeQuery = true)
    Optional<UsersNomineeDetails> findByUseridAndClientName(@Param("userId") Integer userid, @Param("clientName") String clientName, @Param("online_id") String online_id, @Param("online_flag") String online_flag);

    @Query(value = "SELECT * FROM users_nominee_details WHERE online_id = :online_id AND client_name = :clientName", nativeQuery = true)
    UsersNomineeDetails getUsersNomineeDetailsByOnlineId(Integer online_id, String clientName);

    @Query(value = "SELECT * FROM users_nominee_details WHERE user_id = :userId AND client_name = :clientName AND online_code = :online_code AND broker_code = :broker_code", nativeQuery = true)
    Optional<UsersNomineeDetails> findByUseridAndClientNameAndClientCode(@Param("userId") Integer userid, @Param("clientName") String clientName, @Param("online_code") String online_code, @Param("broker_code") String broker_code);

    @Query(value = "SELECT * FROM users_nominee_details WHERE online_flag = :onlineFlag AND online_code = :clientCode AND broker_code = :brokerCode AND client_name = :clientName ", nativeQuery = true)
    UsersNomineeDetails getNomineeInfoByClientCodeAndBrokerCode(@Param("clientCode") String clientCode, @Param("brokerCode") String brokerCode, @Param("onlineFlag") String onlineFlag, @Param("clientName") String clientName);

    @Query(value = "SELECT * FROM users_nominee_details WHERE user_id = :userId AND online_flag = :online_flag AND online_id = :online_id AND online_code = :online_code", nativeQuery = true)
    Optional<UsersNomineeDetails> findByUserIds(
            @Param("userId") Integer userId,
            @Param("online_flag") String online_flag,
            @Param("online_id") String online_id,
            @Param("online_code") String online_code
    );

    @Query(value = "SELECT * FROM users_nominee_details WHERE user_id = :userId AND online_flag = :online_flag AND online_id = :online_code", nativeQuery = true)
    Optional<UsersNomineeDetails> findByUserId(
            @Param("userId") Integer userId,
            @Param("online_flag") String online_flag,
            @Param("online_code") String online_code
    );

}
