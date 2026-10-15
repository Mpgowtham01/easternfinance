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

    @Modifying
    @Transactional
    @Query(value = "DROP TABLE IF EXISTS nse_online_sip_stp_swp_master_old", nativeQuery = true)
    void dropNseSchemeMasterOldTable();
}