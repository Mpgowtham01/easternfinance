package com.user.repository;

import com.user.model.MymfboxOnboarding;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MymfboxOnboardingRepository extends JpaRepository<MymfboxOnboarding, Integer> {
    @Query("SELECT m FROM MymfboxOnboarding m WHERE m.user_id = :userId AND m.client_name = :clientName AND m.is_multiple_registration = false ORDER BY m.id DESC")
    Optional<MymfboxOnboarding> findLatestByUserIdAndClientName(@Param("userId") Integer userId, @Param("clientName") String clientName);

    @Query("SELECT m FROM MymfboxOnboarding m WHERE m.user_id = :online_reg_id AND m.client_name = :clientName AND m.is_registration_completed = true AND m.is_multiple_registration = false ORDER BY m.id DESC")
    List<MymfboxOnboarding> findLatestByonlineregidAndClientName(@Param("online_reg_id") Integer online_reg_id, @Param("clientName") String clientName);

    @Query("SELECT m FROM MymfboxOnboarding m WHERE m.user_id = :online_reg_id AND m.client_name = :clientName AND m.is_multiple_registration = :multireg ORDER BY m.id DESC")
    List<MymfboxOnboarding> findLatestByonlineregidAndClientNameMultiTrue(@Param("online_reg_id") Integer online_reg_id, @Param("clientName") String clientName,@Param("multireg") Boolean multireg);

    @Query("SELECT m FROM MymfboxOnboarding m WHERE m.user_id = :online_reg_id AND m.client_name = :clientName AND m.is_multiple_registration = true ORDER BY m.id DESC")
    List<MymfboxOnboarding> findLatestByonlineregidAndClientNameMulti(@Param("online_reg_id") Integer online_reg_id, @Param("clientName") String clientName);

    @Query("SELECT m FROM MymfboxOnboarding m WHERE m.user_id = :online_reg_id AND m.client_name = :clientName AND m.is_registration_completed = true ORDER BY m.id DESC")
    List<MymfboxOnboarding> findLatestByonlineregidAndClientNameCheck(@Param("online_reg_id") Integer online_reg_id, @Param("clientName") String clientName);

    @Query("SELECT m FROM MymfboxOnboarding m WHERE m.user_id = :online_reg_id AND m.client_name = :clientName AND m.is_registration_completed = false AND m.is_multiple_registration = false ORDER BY m.id DESC")
    List<MymfboxOnboarding> findLatestByonlineregidAndClient(@Param("online_reg_id") Integer online_reg_id, @Param("clientName") String clientName);


    @Modifying
    @Transactional
    @Query("""
    DELETE FROM MymfboxOnboarding m
    WHERE m.user_id = :userId
      AND m.client_name = :clientName
      AND m.is_registration_completed = true
""")
    int deleteIncompleteOnboardings(
            @Param("userId") Integer userId,
            @Param("clientName") String clientName
    );

    @Query("FROM MymfboxOnboarding where user_id=:user_id AND client_name = :client_name order by id desc")
    List<MymfboxOnboarding> findMyMfboxList(@Param("user_id") Integer user_id, @Param("client_name") String client_name);


}