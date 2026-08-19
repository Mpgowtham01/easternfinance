package com.user.repository;

import com.user.model.UsersMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsersMappingRepository extends JpaRepository<UsersMapping, Integer> {

    @Query(value = "SELECT DISTINCT investor_id FROM users_mapping WHERE client_name = :client_name", nativeQuery = true)
    List<Integer> findDistinctInvestorIdsByClientName(@Param("client_name") String clientName);

    @Query("SELECT u FROM UsersMapping u WHERE u.client_name = :client_name AND u.mapping_name <> u.investor_name ORDER BY u.user_id, u.investor_name ASC")
    List<UsersMapping> findFilteredUsersByClientName(@Param("client_name") String clientName);

    @Query("SELECT u FROM UsersMapping u WHERE u.user_id = :user_id AND u.client_name = :client_name")
    List<UsersMapping> findByUserIdAndClientName(@Param("user_id") Integer userId, @Param("client_name") String clientName);


}
