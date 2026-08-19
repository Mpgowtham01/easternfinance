package com.amfi.repository;


import com.amfi.model.AmfiSchemeMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AmfiSchemeMasterRepository extends JpaRepository<AmfiSchemeMaster, Integer> {

    @Query("SELECT a.scheme_cams_productcode FROM AmfiSchemeMaster a " +
            "WHERE a.scheme_company = :scheme_company AND a.scheme_cams_productcode <> ''")
    List<String> findSchemeCamsProductCodesByCompany(String scheme_company);

    @Query("FROM AmfiSchemeMaster a " +
            "WHERE a.active = true AND a.scheme_amfi = :scheme_amfi")
    List<AmfiSchemeMaster> findBySchemeAmfiAndActive(
            @org.springframework.data.repository.query.Param("scheme_amfi") String schemeAmfi);


    @Query(value = "SELECT * FROM amfi_scheme_master " +
            "WHERE FIND_IN_SET(:schemeCamsProductcode, scheme_cams_productcode) <> 0 " +
            "LIMIT 1",
            nativeQuery = true)
    List<AmfiSchemeMaster> findFirstBySchemeCamsProductcode(
            @Param("schemeCamsProductcode") String schemeCamsProductcode
    );


    @Query("FROM AmfiSchemeMaster a " +
            "WHERE a.active = true AND a.scheme_amfi_code = :schemeAmfiCode")
    List<AmfiSchemeMaster> findBySchemeAmfiCodeAndActive(
            @Param("schemeAmfiCode") String schemeAmfiCode
    );

    // ✅ Converted: FIND_IN_SET for scheme_karvy_productcode
    @Query(value = "SELECT * FROM amfi_scheme_master " +
            "WHERE FIND_IN_SET(:schemeKarvyProductcode, scheme_karvy_productcode) <> 0 " +
            "LIMIT 1",
            nativeQuery = true)
    List<AmfiSchemeMaster> findFirstBySchemeKarvyProductcode(
            @Param("schemeKarvyProductcode") String schemeKarvyProductcode
    );

    @Query("FROM AmfiSchemeMaster a WHERE a.active = true AND a.isin_no IS NOT NULL AND a.isin_no <> ''")
    List<AmfiSchemeMaster> findActiveWithIsinNo();

    @Query("FROM AmfiSchemeMaster " +
            "WHERE (scheme_amfi_url = :scheme_amfi " +
            "OR scheme_amfi = :scheme_amfi " +
            "OR scheme_amfi_short_name = :scheme_amfi " +
            "OR scheme_amfi_code = :scheme_amfi) " +
            "AND active = true " +
            "AND scheme_amfi_url <> ''")
    List<AmfiSchemeMaster> findSchemeAmfiMaster(@Param("scheme_amfi") String scheme_amfi);

    @Query("SELECT DISTINCT a.scheme_amfi, a.scheme_amfi_short_name, a.scheme_amfi_code, a.scheme_company " +
            "FROM AmfiSchemeMaster a " +
            "WHERE a.active = true " +
            "AND a.dividend_scheme = false " +
            "AND a.open_or_closed LIKE 'Open%' " +
            "AND (:category = 'All' OR a.scheme_advisorkhoj_category = :category) " +
            "AND (:amc = 'All' OR a.scheme_company = :amc) " +
            "AND (LOWER(a.scheme_amfi) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR LOWER(a.scheme_amfi_short_name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR LOWER(a.scheme_amfi_code) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND a.scheme_amfi NOT LIKE '%Direc%' " +
            "AND a.scheme_amfi NOT LIKE '%Institutiona%' " +
            "AND a.scheme_amfi NOT LIKE '%bonus%'")
    List<Object[]> autoSuggestAllMfSchemes(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("amc") String amc
    );


}

