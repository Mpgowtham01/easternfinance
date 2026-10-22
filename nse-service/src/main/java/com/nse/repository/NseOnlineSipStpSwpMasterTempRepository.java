package com.nse.repository;


import com.nse.model.NseOnlineSipStpSwpMasterTemp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface NseOnlineSipStpSwpMasterTempRepository extends JpaRepository<NseOnlineSipStpSwpMasterTemp, Integer>
{
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM nse_online_sip_stp_swp_master_temp WHERE master_option = :master_option", nativeQuery = true)
    void deleteByMasterOption(String master_option);

    @Modifying
    @Transactional
    @Query(value = "CREATE TABLE IF NOT EXISTS nse_online_sip_stp_swp_master_temp LIKE nse_online_sip_stp_swp_master", nativeQuery = true)
    void createNseSchemeMasterTempTable();

    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE nse_online_sip_stp_swp_master_temp", nativeQuery = true)
    void truncateNseSchemeMasterTempTable();

    @Modifying
    @Transactional
    @Query(value = "RENAME TABLE nse_online_sip_stp_swp_master TO nse_online_sip_stp_swp_master_old, nse_online_sip_stp_swp_master_temp TO nse_online_sip_stp_swp_master", nativeQuery = true)
    void modifyNseSchemeMasterTempToMainTable();

    @Query(value = "SELECT COUNT(*) FROM nse_online_sip_stp_swp_master_temp WHERE LOWER(master_option) = LOWER(:master_option)", nativeQuery = true)
    long countByMasterOption(String master_option);

    // DML equivalents of the TRUNCATE/RENAME lifecycle above: the application DB user has no DROP,
    // ALTER or CREATE privilege, so the temp rows are published with DELETE/INSERT. The id column is
    // deliberately left out of the INSERT - the live table keeps the other master_options' rows and
    // their ids, so the temp ids would collide.
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM nse_online_sip_stp_swp_master WHERE LOWER(master_option) = LOWER(:master_option)", nativeQuery = true)
    int deleteFromMainByMasterOption(String master_option);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO nse_online_sip_stp_swp_master
                (amc_code, amc_name, scheme_code, scheme_name, scheme_isin, scheme_type,
                 sip_transaction_mode, sip_frequency, sip_dates, sip_minimum_gap, sip_maximum_gap,
                 sip_installment_gap, sip_status, sip_minimum_installment_amount, sip_maximum_installment_amount,
                 sip_multiplier_amount, sip_minimum_installment_numbers, sip_maximum_installment_numbers,
                 pause_flag, pause_minimum_installments, pause_maximum_installments, pause_modification_count,
                 filler_1, filler_2, filler_3, filler_4, filler_5, created_date,
                 astp_transaction_mode, astp_in_minimum_installment_amount, astp_in_maximum_installment_amount,
                 astp_in_multiplier_amount, astp_out_minimum_installment_amount, astp_out_maximum_installment_amount,
                 astp_out_multiplier_amount, astp_minimum_installment_units, astp_maximum_installment_units,
                 astp_multiplier_units, astp_minimum_installment_numbers, astp_maximum_installment_numbers,
                 astp_reg_in, astp_reg_out, astp_frequency, astp_dates, astp_minimum_gap, astp_maximum_gap,
                 astp_installment_gap, astp_status,
                 aswp_transaction_mode, aswp_minimum_installment_amount, aswp_maximum_installment_amount,
                 aswp_multiplier_amount, aswp_minimum_installment_units, aswp_maximum_installment_units,
                 aswp_multiplier_units, aswp_minimum_installment_numbers, aswp_maximum_installment_numbers,
                 aswp_frequency, aswp_dates, aswp_minimum_gap, aswp_maximum_gap, aswp_installment_gap,
                 aswp_status, master_option, scheme_amfi, scheme_category, scheme_amfi_code)
            SELECT amc_code, amc_name, scheme_code, scheme_name, scheme_isin, scheme_type,
                   sip_transaction_mode, sip_frequency, sip_dates, sip_minimum_gap, sip_maximum_gap,
                   sip_installment_gap, sip_status, sip_minimum_installment_amount, sip_maximum_installment_amount,
                   sip_multiplier_amount, sip_minimum_installment_numbers, sip_maximum_installment_numbers,
                   pause_flag, pause_minimum_installments, pause_maximum_installments, pause_modification_count,
                   filler_1, filler_2, filler_3, filler_4, filler_5, created_date,
                   astp_transaction_mode, astp_in_minimum_installment_amount, astp_in_maximum_installment_amount,
                   astp_in_multiplier_amount, astp_out_minimum_installment_amount, astp_out_maximum_installment_amount,
                   astp_out_multiplier_amount, astp_minimum_installment_units, astp_maximum_installment_units,
                   astp_multiplier_units, astp_minimum_installment_numbers, astp_maximum_installment_numbers,
                   astp_reg_in, astp_reg_out, astp_frequency, astp_dates, astp_minimum_gap, astp_maximum_gap,
                   astp_installment_gap, astp_status,
                   aswp_transaction_mode, aswp_minimum_installment_amount, aswp_maximum_installment_amount,
                   aswp_multiplier_amount, aswp_minimum_installment_units, aswp_maximum_installment_units,
                   aswp_multiplier_units, aswp_minimum_installment_numbers, aswp_maximum_installment_numbers,
                   aswp_frequency, aswp_dates, aswp_minimum_gap, aswp_maximum_gap, aswp_installment_gap,
                   aswp_status, master_option, scheme_amfi, scheme_category, scheme_amfi_code
            FROM nse_online_sip_stp_swp_master_temp
            WHERE LOWER(master_option) = LOWER(:master_option)
            """, nativeQuery = true)
    int copyTempToMainByMasterOption(String master_option);

    @Modifying
    @Transactional
    @Query(value = "DROP TABLE IF EXISTS nse_online_sip_stp_swp_master_old", nativeQuery = true)
    void dropNseSchemeMasterOldTable();
}