package com.amfi.repository;

import com.amfi.model.AmfiMfNav;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface AmfiMfNavRepository extends JpaRepository<AmfiMfNav, Integer> {

    @Query("SELECT a.net_asset_value FROM AmfiMfNav a WHERE a.scheme_code = :schemeCode ORDER BY a.nav_date DESC")
    List<Double> findNetAssetValueBySchemeCodeOrderByNavDateDesc(String schemeCode);

    @Query("SELECT a FROM AmfiMfNav a WHERE a.scheme_code = :schemeCode ORDER BY a.nav_date DESC")
    AmfiMfNav findTopBySchemeCodeOrderByNavDateDesc(@org.springframework.data.repository.query.Param("schemeCode") String schemeCode);

}
