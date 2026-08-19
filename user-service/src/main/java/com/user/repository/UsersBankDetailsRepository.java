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

}
