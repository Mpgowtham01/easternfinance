package com.user.repository;

import com.user.model.BasketDetails;
import com.user.model.MymfboxOnboarding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BasketDetailsRepository extends JpaRepository<BasketDetails, Integer>
{
    @Query("FROM BasketDetails b WHERE b.id = :id AND b.basket_name = :basket_name AND b.client_name = :client_name")
    List<BasketDetails> findLatestByUserIdAndClientName(
            @Param("id") Integer id,
            @Param("basket_name") String basket_name,
            @Param("client_name") String client_name
    );

    @Query("FROM BasketDetails b WHERE b.client_name = :client_name")
    List<BasketDetails> findLatestByClientName(@Param("client_name") String client_name);


}
