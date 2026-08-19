package com.amfi.repository;


import com.amfi.model.AmfiLatestNav;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AmfiLatestNavRepository extends JpaRepository<AmfiLatestNav, Integer>
{
    @Query("FROM AmfiLatestNav WHERE scheme_code = :schemeCode")
    List<AmfiLatestNav> findBySchemeCodeUsingQuery(String schemeCode);
}