package com.nse.repository;

import com.nse.model.NseSchemeMasterLimitTemp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface NseOnlineSchemeMasterLimitTempRepository extends JpaRepository<NseSchemeMasterLimitTemp, Integer>
{

    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE nse_scheme_master_limit_temp", nativeQuery = true)
    void truncateNseSchemeMasterTable();

    @Modifying
    @Transactional
    @Query(value = "ALTER TABLE nse_scheme_master_limit_temp RENAME TO nse_scheme_master_limit", nativeQuery = true)
    void renameTempToMainTable();

    @Modifying
    @Transactional
    @Query(value = "CREATE TABLE nse_scheme_master_limit_temp LIKE nse_scheme_master_limit", nativeQuery = true)
    void createTempTableLikeMain();
}