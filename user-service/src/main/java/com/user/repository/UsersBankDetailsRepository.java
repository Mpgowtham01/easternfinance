package com.user.repository;

import com.user.model.UsersBankDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsersBankDetailsRepository extends JpaRepository<UsersBankDetails, Integer>
{
    @Query("FROM UsersBankDetails b WHERE b.online_code = :online_code AND b.client_name = :client_name")
    List<UsersBankDetails> findLatestByUserIdAndClientName(
            @Param("online_code") String online_code,
            @Param("client_name") String client_name
    );

    @Query("FROM UsersBankDetails b WHERE b.online_id = :online_id ORDER BY b.id ASC")
    List<UsersBankDetails> findByOnlineId(@Param("online_id") Integer online_id);

    @Query("FROM UsersBankDetails b WHERE b.user_id = :user_id AND b.client_name = :client_name")
    List<UsersBankDetails> findByUserIdAndClientName(
            @Param("user_id") Integer user_id,
            @Param("client_name") String client_name
    );

    @Query(value = "SELECT * FROM users_bank_details WHERE user_id = :userId AND client_name = :clientName AND online_id = :online_id AND online_flag='NSE'", nativeQuery = true)
    List<UsersBankDetails> findByUseridAndClientName(@Param("userId") Integer userid, @Param("clientName") String clientName,@Param("online_id") String online_id);

    @Query(value = "SELECT * FROM users_bank_details WHERE online_id = :online_id AND client_name = :clientName", nativeQuery = true)
    UsersBankDetails getUsersBankDetailsByOnlineId(Integer online_id, String clientName);

    @Query(value = "SELECT * FROM users_bank_details WHERE user_id = :userId AND client_name = :clientName AND online_code = :online_code AND broker_code=:broker_code", nativeQuery = true)
    List<UsersBankDetails> findByUseridAndClientNameAndClientCode(@Param("userId") Integer userid, @Param("clientName") String clientName,@Param("online_code") String online_code,@Param("broker_code") String broker_code);

    @Query(value = "SELECT * FROM users_bank_details WHERE user_id = :userId AND client_name = :clientName AND online_id = :online_id AND online_flag=:online_flag", nativeQuery = true)
    List<UsersBankDetails> findByUseridAndClientNameAndOnlineFlag(@Param("userId") Integer userid, @Param("clientName") String clientName,@Param("online_id") String online_id,@Param("online_flag")  String online_flag);


}
