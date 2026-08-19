package com.user.repository;

import com.user.model.UsersNomineeDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UsersNomineeDetailsRepository extends JpaRepository<UsersNomineeDetails, Integer>
{
    @Query("FROM UsersNomineeDetails b WHERE b.user_id = :user_id AND b.client_name = :client_name")
    List<UsersNomineeDetails> findByUserIdAndClientName(
            @Param("user_id") Integer user_id,
            @Param("client_name") String client_name
    );

}
