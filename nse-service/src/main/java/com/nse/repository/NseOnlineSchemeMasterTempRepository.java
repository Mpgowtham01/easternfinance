package com.nse.repository;

import com.nse.model.NseOnlineSchemeMasterTemp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface NseOnlineSchemeMasterTempRepository extends JpaRepository<NseOnlineSchemeMasterTemp, Integer>
{
    @Modifying
    @Transactional
    @Query(value = "CREATE TABLE IF NOT EXISTS nse_online_scheme_master_temp LIKE nse_online_scheme_master", nativeQuery = true)
    void createNseSchemeMasterTempTable();

    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE nse_online_scheme_master_temp", nativeQuery = true)
    void truncateNseSchemeMasterTempTable();

    @Modifying
    @Transactional
    @Query(value = "RENAME TABLE nse_online_scheme_master TO nse_online_scheme_master_old, nse_online_scheme_master_temp TO nse_online_scheme_master", nativeQuery = true)
    void modifyNseSchemeMasterTempToMainTable();

    @Modifying
    @Transactional
    @Query(value = "DROP TABLE IF EXISTS nse_online_scheme_master_old", nativeQuery = true)
    void dropNseSchemeMasterOldTable();


    @Modifying
    @Transactional
    @Query(value = "UPDATE nse_scheme_master_temp SET scheme_amfi = product_name WHERE scheme_amfi = '' OR scheme_amfi IS NULL", nativeQuery = true)
    int updateEmptySchemeAmfi();

    @Modifying
    @Transactional
    @Query(value = "UPDATE nse_scheme_master_temp SET product_category = 'OE' WHERE product_category LIKE 'Open%'", nativeQuery = true)
    int updateOpenEndedCategory();

    @Modifying
    @Transactional
    @Query(value = "UPDATE nse_scheme_master_temp SET product_category = 'CE' WHERE product_category LIKE 'Close%'", nativeQuery = true)
    int updateCloseEndedCategory();

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE nse_scheme_master_temp
            SET active_flag = 'N'
            WHERE product_name LIKE '%ETF%'
              AND product_name NOT LIKE '%Fund of Fund%'
              AND product_name NOT LIKE '%FOF%'
            """, nativeQuery = true)
    int deactivateEtfSchemes();

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM nse_scheme_master_temp WHERE isin = 'INF209K01AD1'", nativeQuery = true)
    int deleteInvalidIsin();

    @Modifying
    @Transactional
    @Query(value = "ALTER TABLE nse_scheme_master_temp RENAME TO nse_scheme_master", nativeQuery = true)
    void renameTempToMainTable();

    @Modifying
    @Transactional
    @Query(value = "CREATE TABLE nse_scheme_master_temp LIKE nse_scheme_master", nativeQuery = true)
    void createTempTableLikeMain();
}