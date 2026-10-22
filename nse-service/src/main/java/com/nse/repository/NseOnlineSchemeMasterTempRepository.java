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

    @Query(value = "SELECT COUNT(*) FROM nse_online_scheme_master_temp", nativeQuery = true)
    long countTempRecords();

    // DML equivalents of the TRUNCATE/RENAME lifecycle below: the application DB user has no DROP,
    // ALTER or CREATE privilege, so the temp table is cleared and published with DELETE/INSERT.
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM nse_online_scheme_master_temp", nativeQuery = true)
    int deleteAllTempRecords();

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM nse_online_scheme_master", nativeQuery = true)
    int deleteAllMainRecords();

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO nse_online_scheme_master
                (unique_sr_no, scheme_code, rta_scheme_code, amc_scheme_code, isin, amc_code, amc_name,
                 scheme_type, scheme_category, plan_type, scheme_amfi_code, scheme_name, scheme_amfi_short_name,
                 scheme, purchase_allowed, purchase_transaction_mode, new_purchase_min_amount,
                 additional_purchase_min_amount, additional_purchase_max_amount, purchase_amount_multiplier,
                 purchase_cutoff_time, redemption_allowed, redemption_transaction_mode, redemption_min_qty,
                 redemption_qty_multiplier, redemption_max_qty, redemption_min_amount, redemption_max_amount,
                 redemption_amount_multiplier, redemption_cutoff_time, rta_agent_code, amc_active_flag,
                 div_reinvest_flag, sip_allowed, stp_enabled, swp_enabled, switch_allowed, settlement_type,
                 amc_ind, face_value, scheme_start_date, maturity_date, exit_load_flag, exit_load,
                 lock_in_period_flag, lock_in_period, channel_partner_code, reopening_date,
                 open_close_ended_scheme, created_date)
            SELECT unique_sr_no, scheme_code, rta_scheme_code, amc_scheme_code, isin, amc_code, amc_name,
                   scheme_type, scheme_category, plan_type, scheme_amfi_code, scheme_name, scheme_amfi_short_name,
                   scheme, purchase_allowed, purchase_transaction_mode, new_purchase_min_amount,
                   additional_purchase_min_amount, additional_purchase_max_amount, purchase_amount_multiplier,
                   purchase_cutoff_time, redemption_allowed, redemption_transaction_mode, redemption_min_qty,
                   redemption_qty_multiplier, redemption_max_qty, redemption_min_amount, redemption_max_amount,
                   redemption_amount_multiplier, redemption_cutoff_time, rta_agent_code, amc_active_flag,
                   div_reinvest_flag, sip_allowed, stp_enabled, swp_enabled, switch_allowed, settlement_type,
                   amc_ind, face_value, scheme_start_date, maturity_date, exit_load_flag, exit_load,
                   lock_in_period_flag, lock_in_period, channel_partner_code, reopening_date,
                   open_close_ended_scheme, created_date
            FROM nse_online_scheme_master_temp
            """, nativeQuery = true)
    int copyTempToMainTable();

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