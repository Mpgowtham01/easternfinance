package com.nse.repository;


import com.nse.model.NseOnlineSipStpSwpMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface NseOnlineSipStpSwpMasterRepository extends JpaRepository<NseOnlineSipStpSwpMaster, Integer> {

    @Query("SELECT DISTINCT s FROM NseOnlineSipStpSwpMaster s " +
            "WHERE s.master_option = 'SIP' AND s.sip_status = '1' AND s.scheme_amfi = :schemeAmfi GROUP BY s.sip_frequency")
    List<NseOnlineSipStpSwpMaster> findGroupedSipBySchemeAmfi(@Param("schemeAmfi") String schemeAmfi);

    @Query("FROM NseOnlineSipStpSwpMaster s " +
            "WHERE s.master_option = 'SIP' AND s.sip_status = '1' " +
            "AND s.amc_name = :amcName " +
            "AND s.scheme_code = :schemeCode " +
            "AND s.sip_frequency = :sipFrequency")
    List<NseOnlineSipStpSwpMaster> findByAmcNameAndSchemeCodeAndFrequency(
            @Param("amcName") String amcName,
            @Param("schemeCode") String schemeCode,
            @Param("sipFrequency") String sipFrequency);

    @Query("SELECT s FROM NseOnlineSipStpSwpMaster s " +
            "WHERE s.amc_name = :amcName " +
            "AND s.master_option = 'STP' " +
            "AND s.astp_status = 'Y' " +
            "AND s.scheme_amfi = :schemeName GROUP BY astp_frequency")
    List<NseOnlineSipStpSwpMaster> findDistinctAstpFrequenciesByAmcNameAndSchemeName(
            @Param("amcName") String amc_name,
            @Param("schemeName") String scheme_name
    );


    @Query("SELECT s " +
            "FROM NseOnlineSipStpSwpMaster s " +
            "WHERE s.master_option = 'SWP' " +
            "AND s.aswp_status = 'Y' " +
            "AND s.scheme_amfi = :schemeName " +
            "GROUP BY s.aswp_frequency")
    List<NseOnlineSipStpSwpMaster> findDistinctAswpFrequenciesByAmcNameAndSchemeName(
            @Param("schemeName") String schemeName
    );

    @Query("FROM NseOnlineSipStpSwpMaster s " +
            "WHERE s.master_option = 'STP' " +
            "AND s.astp_status = 'Y' " +
            "AND s.amc_name = :amcName " +
            "AND s.scheme_code = :schemeCode")
    List<NseOnlineSipStpSwpMaster> findFirstByAmcNameAndSchemeCodeForStp(
            @Param("amcName") String amcName,
            @Param("schemeCode") String schemeCode);

    @Modifying
    @Transactional
    @Query("DELETE NseOnlineSipStpSwpMaster s WHERE s.master_option = :master_option ")
    void deleteByMasterOption(String master_option);

}