package com.nse.repository;

import com.nse.model.NseOnlineSchemeMaster;
import com.nse.pojo.CommonPojo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NseOnlineSchemeMasterRepository extends JpaRepository<NseOnlineSchemeMaster, Integer>
{

    // GET LUMP SUM AMC NAMES
    @Query(value = "SELECT distinct amc_code,amc_name FROM nse_online_scheme_master WHERE amc_code IN (:amc_name) AND plan_type = 'NORMAL' AND purchase_allowed = 'Y' AND amc_active_flag = 'Y' ORDER BY amc_code ASC", nativeQuery = true)
    List<Object[]> getLumpsumAmcNamesByAmcName(@Param("amc_name") List<String> amcList);

    @Query(value = "SELECT distinct amc_code,amc_name FROM nse_online_scheme_master WHERE amc_code IS NOT NULL AND amc_name!='' AND plan_type='NORMAL' AND purchase_allowed='Y' AND amc_active_flag ='Y' ORDER BY amc_code ASC", nativeQuery = true)
    List<Object[]> getAllLumpsumAmcNames();

    // GET LUMP SUM CATEGORIES
    @Query(value = "SELECT DISTINCT scheme_category FROM nse_online_scheme_master WHERE scheme_category IS NOT NULL AND scheme_category != '' AND scheme_category != 'ETFs' AND scheme_amfi_code IS NOT NULL AND scheme_amfi_code != '' AND plan_type = 'NORMAL' AND purchase_allowed = 'Y' AND purchase_transaction_mode IN ('P','DP') AND settlement_type NOT IN ('L1','L0') AND scheme NOT LIKE '%INSURED%' ORDER BY scheme_category ASC", nativeQuery = true)
    List<String> getAllLumpsumSchemeCategories();

    @Query(value = "SELECT DISTINCT scheme_category FROM nse_online_scheme_master WHERE amc_code = :amcName AND scheme_category != 'ETFs' AND scheme_category IS NOT NULL AND scheme_category != '' AND scheme_amfi_code IS NOT NULL AND scheme_amfi_code != '' AND plan_type = 'NORMAL' AND purchase_allowed = 'Y' AND purchase_transaction_mode IN ('P','DP') AND settlement_type NOT IN ('L1','L0') AND scheme NOT LIKE '%INSURED%' ORDER BY scheme_category ASC", nativeQuery = true)
    List<String> getLumpsumCategoriesByAmc(@Param("amcName") String amcName);

    // GET LUMP SUM SCHEMES
    @Query(value = "SELECT scheme_name,scheme,scheme_category,amc_code,amc_name FROM nse_online_scheme_master WHERE scheme_name IS NOT NULL AND scheme_name != '' AND scheme_amfi_code IS NOT NULL AND scheme_amfi_code != '' AND plan_type = 'NORMAL' AND purchase_allowed = 'Y' AND purchase_transaction_mode IN ('P','DP') AND settlement_type NOT IN ('L1','L0') AND scheme NOT LIKE '%INSURED%' AND scheme_category != 'ETFs' group by scheme_amfi_code", nativeQuery = true)
    List<Object[]> getAllLumpsumSchemes();

    @Query(value = "SELECT scheme_name,scheme,scheme_category,amc_code,amc_name FROM nse_online_scheme_master WHERE amc_code = :amcCode AND scheme_name IS NOT NULL AND scheme_name != '' AND scheme_amfi_code IS NOT NULL AND scheme_amfi_code != '' AND plan_type = 'NORMAL' AND purchase_allowed = 'Y' AND purchase_transaction_mode IN ('P','DP') AND settlement_type NOT IN ('L1','L0') AND scheme NOT LIKE '%INSURED%' AND scheme_category != 'ETFs' group by scheme_amfi_code", nativeQuery = true)
    List<Object[]> getAllLumpsumSchemesByAmcCode(@Param("amcCode") String amcCode);

    @Query(value = "SELECT scheme_name,scheme,scheme_category,amc_code,amc_name FROM nse_online_scheme_master WHERE amc_name = :amc_name AND scheme_name IS NOT NULL AND scheme_name != '' AND plan_type = 'NORMAL' AND purchase_allowed = 'Y' AND purchase_transaction_mode IN ('P','DP') AND settlement_type NOT IN ('L1','L0') AND scheme NOT LIKE '%INSURED%' AND scheme_category != 'ETFs' group by scheme_amfi_code", nativeQuery = true)
    List<Object[]> getAllSIFLumpsumSchemesByAmcCode(@Param("amc_name") String amc_name);

    @Query(value = "SELECT scheme_name,scheme,scheme_category,amc_code,amc_name FROM nse_online_scheme_master WHERE scheme_category = :category AND scheme_name IS NOT NULL AND scheme_name != '' AND scheme_amfi_code IS NOT NULL AND scheme_amfi_code != '' AND plan_type = 'NORMAL' AND purchase_allowed = 'Y' AND purchase_transaction_mode IN ('P','DP') AND settlement_type NOT IN ('L1','L0') AND scheme NOT LIKE '%INSURED%' AND scheme_category != 'ETFs' group by scheme_amfi_code", nativeQuery = true)
    List<Object[]> getAllLumpsumSchemesByCategory(@Param("category") String category);

    @Query(value = "SELECT scheme_name,scheme,scheme_category,amc_code,amc_name FROM nse_online_scheme_master WHERE amc_code = :amcCode AND scheme_category = :category AND scheme_name IS NOT NULL AND scheme_name != '' AND scheme_amfi_code IS NOT NULL AND scheme_amfi_code != '' AND plan_type = 'NORMAL' AND purchase_allowed = 'Y' AND purchase_transaction_mode IN ('P','DP') AND settlement_type NOT IN ('L1','L0') AND scheme NOT LIKE '%INSURED%' AND scheme_category != 'ETFs' group by scheme_amfi_code", nativeQuery = true)
    List<Object[]> getAllLumpsumSchemesByAmcCodeAndCategory(@Param("amcCode") String amcCode, @Param("category") String category);

    @Query(value = "SELECT scheme_name,scheme,scheme_category,amc_code,amc_name FROM nse_online_scheme_master WHERE amc_name = :amcCode AND scheme_category = :category AND scheme_name IS NOT NULL AND scheme_name != '' AND scheme_amfi_code IS NOT NULL AND scheme_amfi_code != '' AND plan_type = 'NORMAL' AND purchase_allowed = 'Y' AND purchase_transaction_mode IN ('P','DP') AND settlement_type NOT IN ('L1','L0') AND scheme NOT LIKE '%INSURED%' AND scheme_category != 'ETFs' group by scheme_amfi_code", nativeQuery = true)
    List<Object[]> getAllSIFLumpsumSchemesByAmcCodeAndCategory(@Param("amcCode") String amcCode, @Param("category") String category);

    // GET SIP CATEGORIES
    @Query(value = "SELECT DISTINCT scheme_category FROM nse_online_scheme_master WHERE scheme_category IS NOT NULL AND scheme_category != '' AND plan_type = 'NORMAL' AND sip_allowed = 'Y' AND amc_active_flag = 'Y' ORDER BY scheme_category ASC", nativeQuery = true)
    List<String> getAllSipSchemeCategories();

    @Query(value = "SELECT DISTINCT scheme_category FROM nse_online_scheme_master WHERE amc_code = :amcCode AND scheme_category IS NOT NULL AND scheme_category != '' AND plan_type = 'NORMAL' AND sip_allowed = 'Y' AND amc_active_flag = 'Y' ORDER BY scheme_category ASC", nativeQuery = true)
    List<String> getSipSchemeCategoriesByAmc(@Param("amcCode") String amcCode);

    // GET SIP SCHEMES
    @Query(value = "SELECT DISTINCT scheme_name,scheme_category,amc_code,amc_name,scheme_code FROM nse_online_scheme_master WHERE scheme_name IS NOT NULL AND scheme_name != '' AND scheme_amfi_code IS NOT NULL AND scheme_amfi_code != '' AND settlement_type NOT IN ('L1','L0') AND  plan_type = 'NORMAL' AND sip_allowed = 'Y' AND amc_active_flag = 'Y'", nativeQuery = true)
    List<Object[]> getAllSipSchemes();

    @Query(value = "SELECT DISTINCT scheme_name,scheme_category,amc_code,amc_name,scheme_code FROM nse_online_scheme_master WHERE amc_code = :amcCode AND scheme_name IS NOT NULL AND scheme_name != '' AND scheme_amfi_code IS NOT NULL AND settlement_type NOT IN ('L1','L0') AND scheme_amfi_code != '' AND plan_type = 'NORMAL' AND sip_allowed = 'Y' AND amc_active_flag = 'Y'", nativeQuery = true)
    List<Object[]> getSipSchemesByAmcCode(@Param("amcCode") String amcCode);

    @Query(value = "SELECT DISTINCT scheme_name,scheme_category,amc_code,amc_name,scheme_code FROM nse_online_scheme_master WHERE scheme_category = :category AND scheme_name IS NOT NULL AND scheme_name != '' AND scheme_amfi_code IS NOT NULL AND settlement_type NOT IN ('L1','L0') AND scheme_amfi_code != '' AND plan_type = 'NORMAL' AND sip_allowed = 'Y' AND amc_active_flag = 'Y'", nativeQuery = true)
    List<Object[]> getSipSchemesByCategory(@Param("category") String category);

    @Query(value = "SELECT DISTINCT scheme_name,scheme_category,amc_code,amc_name,scheme_code FROM nse_online_scheme_master WHERE amc_code = :amcCode AND scheme_category = :category AND scheme_name IS NOT NULL AND scheme_name != '' AND settlement_type NOT IN ('L1','L0') AND scheme_amfi_code IS NOT NULL AND scheme_amfi_code != '' AND plan_type = 'NORMAL' AND sip_allowed = 'Y' AND amc_active_flag = 'Y'", nativeQuery = true)
    List<Object[]> getSipSchemesByAmcCodeAndCategory(@Param("amcCode") String amcCode, @Param("category") String category);

    @Query("SELECT DISTINCT n.divReinvestFlag FROM NseOnlineSchemeMaster n WHERE " +
            "n.schemeName = :schemeName " +
            "AND n.schemeName IS NOT NULL AND n.schemeName <> '' " +
            "AND n.schemeCode IS NOT NULL AND n.schemeCode <> '' " +
            "AND n.planType = 'NORMAL' AND n.purchaseAllowed = 'Y' " +
            "AND n.purchaseTransactionMode IN ('P', 'DP') " +
            "AND n.settlementType NOT IN ('L1', 'L0') " +
            "AND n.scheme NOT LIKE '%INSURED%'")
    List<String> findDistinctDivReinvestFlagBySchemeName(@Param("schemeName") String schemeName);


    @Query("FROM NseOnlineSchemeMaster s " +
            "WHERE s.schemeCode = :schemeCode " +
            "AND s.schemeName IS NOT NULL AND s.schemeName != '' " +
            "AND s.planType = 'NORMAL' " +
            "AND s.purchaseAllowed = 'Y' " +
            "AND s.settlementType NOT IN ('L1', 'L0') " +
            "AND s.scheme NOT LIKE '%INSURED%'")
    List<NseOnlineSchemeMaster> findValidSchemesBySchemeCode(@Param("schemeCode") String schemeCode);

    @Query("SELECT DISTINCT n.divReinvestFlag FROM NseOnlineSchemeMaster n " +
            "WHERE n.schemeName = :schemeName " +
            "AND n.amcActiveFlag = 'Y' " +
            "AND n.planType = 'NORMAL' " +
            "AND n.sipAllowed = 'Y'")
    List<String> findDistinctDivReinvestFlagForSip(@Param("schemeName") String schemeName);

    @Query("FROM NseOnlineSchemeMaster n " +
            "WHERE n.schemeName = :schemeName " +
            "AND n.divReinvestFlag = :reinvestTag " +
            "AND n.amcActiveFlag = 'Y' " +
            "AND n.planType = 'NORMAL' " +
            "AND n.purchaseTransactionMode IN ('P', 'DP') " +
            "AND n.schemeCode NOT LIKE '%-L1' " +
            "AND n.schemeCode NOT LIKE '%-L0' " +
            "AND n.scheme NOT LIKE '%INSURED%'" +
            "AND n.schemeCategory != 'ETFs'" +
            "AND n.sipAllowed = 'Y'")
    List<NseOnlineSchemeMaster> findBySchemeNameAndReinvestFlag(
            @Param("schemeName") String schemeName,
            @Param("reinvestTag") String reinvestTag
    );

    @Query("FROM NseOnlineSchemeMaster n " +
            "WHERE n.schemeName = :schemeName " +
            "AND n.amcCode = :amcCode " +
            "AND n.divReinvestFlag = :reinvestTag " +
            "AND n.amcActiveFlag = 'Y' " +
            "AND n.planType = 'NORMAL' " +
            "AND n.purchaseTransactionMode IN ('P', 'DP') " +
            "AND n.settlementType NOT IN ('L0', 'L1') " +
            "AND n.scheme NOT LIKE '%INSURED%'" +
            "AND n.schemeCategory != 'ETFs'" +
            "AND n.sipAllowed = 'Y'")
    List<NseOnlineSchemeMaster> findBySchemeNameAndAmcNameAndReinvestFlag(
            @Param("schemeName") String schemeName,
            @Param("amcCode") String amcName,
            @Param("reinvestTag") String reinvestTag
    );

    @Query("FROM NseOnlineSchemeMaster s " +
            "WHERE s.schemeCode IN :schemeCodeList " +
            "AND s.schemeName IS NOT NULL AND s.schemeName != '' " +
            "AND s.schemeCode IS NOT NULL AND s.schemeCode != ''")
    List<NseOnlineSchemeMaster> getSchemeBySchemeCode(@Param("schemeCodeList") List<String> schemeCodeList);

    @Query("SELECT s FROM NseOnlineSchemeMaster s " +
            "WHERE (s.schemeName = :schemeName OR s.schemeAmfiShortName = :schemeName) " +
            "AND s.schemeName IS NOT NULL AND s.schemeName != '' " +
            "AND s.planType = 'NORMAL' AND s.purchaseAllowed = 'Y' " +
            "AND s.purchaseTransactionMode IN ('P', 'DP') " +
            "AND s.scheme NOT LIKE '%INSURED%' " +
            "AND s.settlementType IN ('L0', 'L1') " +
            "AND s.divReinvestFlag = :dividendReinvestmentFlag " +
            "AND s.schemeAmfiCode IS NOT NULL")
    List<NseOnlineSchemeMaster> findWithoutAmountCondition(
            @Param("schemeName") String schemeName,
            @Param("dividendReinvestmentFlag") String dividendReinvestmentFlag
    );


    @Query("SELECT s FROM NseOnlineSchemeMaster s " +
            "WHERE (s.schemeName = :schemeName OR s.schemeAmfiShortName = :schemeName) " +
            "AND s.schemeName IS NOT NULL AND s.schemeName != '' " +
            "AND s.planType = 'NORMAL' AND s.purchaseAllowed = 'Y' " +
            "AND s.purchaseTransactionMode IN ('P', 'DP') " +
            "AND s.scheme NOT LIKE '%INSURED%' " +
            "AND s.divReinvestFlag = :dividendReinvestmentFlag " +
            "AND s.schemeAmfiCode IS NOT NULL")
    List<NseOnlineSchemeMaster> findWithMinAmountGTE(
            @Param("schemeName") String schemeName,
            @Param("dividendReinvestmentFlag") String dividendReinvestmentFlag
    );

    @Query("SELECT s FROM NseOnlineSchemeMaster s " +
            "WHERE (s.schemeName = :schemeName OR s.schemeAmfiShortName = :schemeName) " +
            "AND s.schemeName IS NOT NULL AND s.schemeName != '' " +
            "AND s.planType = 'NORMAL' AND s.purchaseAllowed = 'Y' " +
            "AND s.purchaseTransactionMode IN ('P', 'DP') " +
            "AND s.scheme NOT LIKE '%INSURED%' " +
            "AND s.divReinvestFlag = :dividendReinvestmentFlag " +
            "AND s.settlementType NOT IN ('L1','L0') ")
    List<NseOnlineSchemeMaster> findWithMinAmountLT(
            @Param("schemeName") String schemeName,
            @Param("dividendReinvestmentFlag") String dividendReinvestmentFlag
    );

    @Query("SELECT DISTINCT n.schemeName,n.schemeCategory, n.amcCode, n.amcName, n.schemeCode FROM NseOnlineSchemeMaster n " +
            "WHERE n.schemeAmfiCode IN :schemeAmfiCodes " +
            "AND n.amcActiveFlag = 'Y' " +
            "AND n.planType = 'NORMAL' " +
            "AND n.redemptionAllowed = 'Y'")
    List<Object[]> findDistinctSchemeNamesForRedemption(
            @Param("schemeAmfiCodes") List<String> list1
    );

    @Query("SELECT DISTINCT n.divReinvestFlag FROM NseOnlineSchemeMaster n " +
            "WHERE n.schemeName = :schemeName " +
            "AND n.amcActiveFlag = 'Y' " +
            "AND n.planType = 'NORMAL' " +
            "AND n.redemptionAllowed = 'Y'")
    List<String> findDistinctDivReinvestFlagBySchemeNameForRedemption(
            @Param("schemeName") String schemeName);


    @Query("SELECT n.schemeName, n.schemeCategory, n.amcCode, n.amcName, n.schemeCode FROM NseOnlineSchemeMaster n " +
            "WHERE n.amcCode = :amcCode " +
            "AND n.newPurchaseMinAmount <= 10000 " +
            "AND n.schemeName IS NOT NULL AND n.schemeName <> '' " +
            "AND n.planType = 'NORMAL' " +
            "AND n.purchaseAllowed = 'Y' " +
            "AND n.settlementType NOT IN ('L1', 'L0') " +
            "AND n.scheme NOT LIKE '%INSURED%'")
    List<Object[]> findDistinctSchemeNameByAmcCodeAndMinAmount(
            @Param("amcCode") String amcCode
    );

    @Query("SELECT DISTINCT n.divReinvestFlag FROM NseOnlineSchemeMaster n " +
            "WHERE n.schemeName = :schemeAmfi " +
            "AND n.schemeName IS NOT NULL AND n.schemeName <> '' " +
            "AND n.planType = 'NORMAL' " +
            "AND n.switchAllowed = 'Y' " +
            "AND n.purchaseTransactionMode IN ('P', 'DP') " +
            "AND n.settlementType NOT IN ('L1', 'L0') " +
            "AND n.scheme NOT LIKE '%INSURED%'")
    List<String> findDivReinvestFlagsForSwitchAllowed(
            @Param("schemeAmfi") String scheme
    );

    @Query("SELECT DISTINCT n.schemeName FROM NseOnlineSchemeMaster n " +
            "WHERE n.amcCode = :amcCode " +
            "AND n.schemeName IS NOT NULL AND n.schemeName <> '' " +
            "AND n.schemeAmfiCode IS NOT NULL AND n.schemeAmfiCode <> '' " +
            "AND n.planType = 'NORMAL' " +
            "AND n.stpEnabled = 'Y' " +
            "AND n.amcActiveFlag = 'Y'")
    List<String> findDistinctSchemeNamesForStpByAmcCode(@Param("amcCode") String amc_code);

    @Query("SELECT DISTINCT n.schemeName FROM NseOnlineSchemeMaster n " +
            "WHERE n.amcCode = :amcCode " +
            "AND n.schemeCategory = :category " +
            "AND n.schemeName IS NOT NULL AND n.schemeName <> '' " +
            "AND n.schemeAmfiCode IS NOT NULL AND n.schemeAmfiCode <> '' " +
            "AND n.planType = 'NORMAL' " +
            "AND n.stpEnabled = 'Y' " +
            "AND n.amcActiveFlag = 'Y'")
    List<String> findDistinctSchemeNamesForStpByAmcCodeAndCategory(
            @Param("amcCode") String amc_code,
            @Param("category") String category
    );

    @Query("SELECT DISTINCT n.divReinvestFlag FROM NseOnlineSchemeMaster n " +
            "WHERE n.schemeName = :schemeName " +
            "AND n.amcActiveFlag = 'Y' " +
            "AND n.planType = 'NORMAL' " +
            "AND (n.stpEnabled IS NULL OR n.stpEnabled <> 'N')")
    List<String> findDistinctDivReinvestFlagForStp(@Param("schemeName") String scheme);

    @Query("SELECT DISTINCT n.divReinvestFlag FROM NseOnlineSchemeMaster n " +
            "WHERE n.schemeName = :schemeName " +
            "AND n.amcActiveFlag = 'Y' " +
            "AND n.planType = 'NORMAL' " +
            "AND n.swpEnabled = 'Y'")
    List<String> findDistinctDivReinvestFlagForSwp(@Param("schemeName") String scheme);

    @Query("SELECT n FROM NseOnlineSchemeMaster n " +
            "WHERE n.amcCode = :amcCode " +
            "AND n.schemeName = :schemeName " +
            "AND n.scheme NOT LIKE '%INSURED%' " +
            "AND n.divReinvestFlag = :divReinvestFlag " +
            "AND n.redemptionAllowed = 'Y'")
    List<NseOnlineSchemeMaster> findEligibleSchemesForSwitchAndRedemption(
            @Param("amcCode") String amcCode,
            @Param("schemeName") String schemeName,
            @Param("divReinvestFlag") String divReinvestFlag);

    @Query("SELECT n FROM NseOnlineSchemeMaster n " +
            "WHERE n.schemeCode = :schemeName " +
            "AND n.scheme NOT LIKE '%INSURED%' " +
            "AND n.divReinvestFlag = :divReinvestFlag " +
            "AND n.switchAllowed = 'Y'")
    List<NseOnlineSchemeMaster> findEligibleSchemesForSwitchAndRedemptions(
            @Param("schemeName") String schemeName,
            @Param("divReinvestFlag") String divReinvestFlag);

    @Query("SELECT n FROM NseOnlineSchemeMaster n " +
            "WHERE n.schemeName = :schemeName " +
            "AND n.scheme NOT LIKE '%INSURED%' " +
            "AND n.divReinvestFlag = :divReinvestFlag " +
            "AND n.switchAllowed = 'Y'")
    List<NseOnlineSchemeMaster> findEligibleSchemeNameForSwitchAndRedemptions(
            @Param("schemeName") String schemeName,
            @Param("divReinvestFlag") String divReinvestFlag);

    @Query("SELECT n FROM NseOnlineSchemeMaster n " +
            "WHERE n.schemeName = :schemeName " +
            "AND n.scheme NOT LIKE '%INSURED%' " +
            "AND n.divReinvestFlag = :divReinvestFlag " +
            "AND n.switchAllowed = 'Y'")
    List<NseOnlineSchemeMaster> findEligibleSchemesForSwitchAndRedemption(
            @Param("schemeName") String schemeName,
            @Param("divReinvestFlag") String divReinvestFlag);

    @Query("SELECT n FROM NseOnlineSchemeMaster n WHERE n.schemeCode = :scheme_code AND n.amcCode = :amc_code AND n.switchAllowed = 'Y'")
    List<NseOnlineSchemeMaster> findBySchemeAndAmcAndTransactionMode(
            @Param("scheme_code") String schemeCode,
            @Param("amc_code") String amcCode
    );

    @Query(value = "SELECT * FROM nse_online_scheme_master " +
            "WHERE amc_code IN (:amcNames) " +
            "AND plan_type = 'NORMAL' " +
            "AND purchase_allowed = 'Y' " +
            "AND settlement_type= 'MF' " +
            "AND scheme NOT LIKE '%INSURED%' AND scheme_category != 'ETFs'"+
            "AND STR_TO_DATE(scheme_start_date, '%d-%m-%Y') <= Date(now())  " +
            "AND STR_TO_DATE(maturity_date, '%d-%m-%Y') >= DATE(NOW()) " +
            "ORDER BY scheme_name ASC", nativeQuery = true)
    List<NseOnlineSchemeMaster> findSchemesByAmcNameAndStartDateNative(@Param("amcNames") List<String> amcNames);

    @Query(value = "SELECT * FROM nse_online_scheme_master " +
            "WHERE plan_type = 'NORMAL' " +
            "AND purchase_allowed = 'Y' " +
            "AND STR_TO_DATE(scheme_start_date, '%d-%m-%Y') <= Date(now())  " +
            "AND STR_TO_DATE(maturity_date, '%d-%m-%Y') >= Date(now())  " +
            "AND scheme_category != 'ETFs' " +
            "AND settlement_type = 'MF' " +
            "ORDER BY scheme_name ASC", nativeQuery = true)
    List<NseOnlineSchemeMaster> findSchemesWithStartDateTodayOrLater();

    @Query(value = "SELECT * FROM nse_online_scheme_master " +
            "WHERE amc_code IN (:amc_names) " +
            "AND plan_type = 'NORMAL' " +
            "AND purchase_allowed = 'Y' " +
            "AND settlement_type = 'MF'" +
            "AND sip_allowed = 'Y' " +
            "AND scheme NOT LIKE '%INSURED%' AND scheme_category != 'ETFs'"+
            "AND STR_TO_DATE(scheme_start_date, '%d-%m-%Y') <= Date(now()) " +
            "AND STR_TO_DATE(maturity_date, '%d-%m-%Y') >= DATE(NOW()) " +
            "ORDER BY scheme_name ASC", nativeQuery = true)
    List<NseOnlineSchemeMaster> findSIPSchemesByAmcNameAndStartDateNative(@Param("amc_names") List<String> amc_names);

    @Query(value = "SELECT * FROM nse_online_scheme_master " +
            "WHERE plan_type = 'NORMAL' " +
            "AND sip_allowed = 'Y' " +
            "AND STR_TO_DATE(scheme_start_date, '%d-%m-%Y') <= Date(now())  " +
            "AND STR_TO_DATE(maturity_date, '%d-%m-%Y') >= Date(now())  " +
            "AND scheme_category != 'ETFs' " +
            "AND settlement_type = 'MF' " +
            "ORDER BY scheme_name ASC", nativeQuery = true)
    List<NseOnlineSchemeMaster> findSIPSchemesWithStartDateTodayOrLater();

    @Query("SELECT n FROM NseOnlineSchemeMaster n " +
            "WHERE n.schemeName = :schemeName " +
            "AND n.divReinvestFlag = :reinvestTag " +
            "AND n.amcActiveFlag = 'Y' " +
            "AND n.planType = 'NORMAL' " +
            "AND n.purchaseAllowed = 'Y'")
    List<NseOnlineSchemeMaster> findBySchemeNameAndDivReinvestFlag(
            @Param("schemeName") String schemeName,
            @Param("reinvestTag") String reinvestTag
    );

    @Query("SELECT n FROM NseOnlineSchemeMaster n " +
            "WHERE n.schemeName = :schemeName " +
            "AND n.divReinvestFlag = :reinvestTag " +
            "AND n.amcActiveFlag = 'Y' " +
            "AND n.planType = 'NORMAL' " +
            "AND n.purchaseAllowed = 'Y' " +
            "AND n.settlementType NOT IN ('L1', 'L0') " +
            "AND n.sipAllowed = 'Y'")
    List<NseOnlineSchemeMaster> findEligibleSipSchemes(
            @Param("schemeName") String schemeName,
            @Param("reinvestTag") String reinvestTag
    );

    @Query("SELECT n FROM NseOnlineSchemeMaster n " +
            "WHERE n.schemeCode = :schemeCode " +
            "AND n.schemeName = :schemeName")
    List<NseOnlineSchemeMaster> findBySchemeCodeAndSchemeName(
            @Param("schemeCode") String schemeCode,
            @Param("schemeName") String schemeName
    );

    @Query("FROM NseOnlineSchemeMaster n " +
            "WHERE n.schemeAmfiCode IN :schemeAmfiCodes " +
            "AND n.schemeAmfiCode IS NOT NULL " +
            "AND n.schemeAmfiCode <> '' " +
            "AND n.amcActiveFlag = 'Y' " +
            "AND n.planType = 'NORMAL' " +
            "AND n.purchaseAllowed = 'Y'")
    List<NseOnlineSchemeMaster> findValidSchemesByAmfiCodes(@Param("schemeAmfiCodes") List<String> schemeAmfiCodes);

    @Query("FROM NseOnlineSchemeMaster n WHERE n.schemeName = :schemeName AND n.amcActiveFlag = 'Y' AND n.planType = 'NORMAL' AND n.purchaseAllowed = 'Y'")
    List<NseOnlineSchemeMaster> findBySchemeNameIfActiveAndNormalPlanAndPurchaseAllowed(@Param("schemeName") String schemeName);

    @Query("FROM NseOnlineSchemeMaster n " +
            "WHERE n.schemeName = :schemeName " +
            "AND n.amcActiveFlag = 'Y' " +
            "AND n.planType = 'NORMAL' " +
            "AND n.purchaseAllowed = 'Y' " +
            "AND n.sipAllowed = 'Y'")
    List<NseOnlineSchemeMaster> findBySchemeNameForSipPurchaseAllowedAndActive(
            @Param("schemeName") String schemeName
    );

    @Query("FROM NseOnlineSchemeMaster n " +
            "WHERE n.schemeName = :schemeName " +
            "AND n.scheme NOT LIKE '%INSURED%' " +
            "AND n.stpEnabled = 'Y' " +
            "AND n.divReinvestFlag = :divReinvestFlag " +
            "AND n.amcActiveFlag = 'Y' " +
            "AND n.settlementType not in ('L1','L0') " +
            "AND n.planType = 'NORMAL'")
    List<NseOnlineSchemeMaster> findSTPEnabledSchemes(
            @Param("schemeName") String schemeName,
            @Param("divReinvestFlag") String divReinvestFlag
    );

    @Query("FROM NseOnlineSchemeMaster n " +
            "WHERE n.schemeName = :schemeName " +
            "AND n.scheme NOT LIKE '%INSURED%' " +
            "AND n.swpEnabled = 'Y' " +
            "AND n.divReinvestFlag = :divReinvestFlag " +
            "AND n.amcActiveFlag = 'Y' " +
            "AND n.planType = 'NORMAL'")
    List<NseOnlineSchemeMaster> findSWPEnabledSchemes(
            @Param("schemeName") String schemeName,
            @Param("divReinvestFlag") String divReinvestFlag
    );

    @Query(value = "SELECT scheme_name, scheme_category, amc_code, amc_name, scheme_code " +
            "FROM nse_online_scheme_master " +
            "WHERE amc_code = :amcCode " +
            "AND plan_type = 'NORMAL' " +
            "AND purchase_allowed = 'Y' " +
            "AND STR_TO_DATE(scheme_start_date, '%d-%m-%Y') <= DATE(NOW()) " +
            "AND STR_TO_DATE(maturity_date, '%d-%m-%Y') >= DATE(NOW()) " +
            "AND scheme_category != 'ETFs' " +
            "AND settlement_type = 'MF' " +
            "ORDER BY scheme_name ASC",
            nativeQuery = true)
    List<Object[]> findSchemesByAmcCodeWithDateRangeAndSettlementMF(@Param("amcCode") String amcCode);

    @Query(value = "SELECT reinvest_tag FROM nse_online_scheme_master " +
            "WHERE amc_name = :amcName AND scheme_code = :schemeCode " +
            "AND plan_type = 'NORMAL' AND purchase_allowed = 'Y' " +
            "AND amc_active_flag = 'Y' " +
            "AND STR_TO_DATE(scheme_start_date, '%d-%b-%Y') >= DATE(NOW()) " +
            "ORDER BY scheme_name ASC", nativeQuery = true)
    String findReinvestTagByAmcNameAndSchemeCode(@Param("amcName") String amcName, @Param("schemeCode") String schemeCode);

    @Query("FROM NseOnlineSchemeMaster s " +
            "WHERE s.schemeCode IN :schemeCodeList " +
            "AND s.schemeName IS NOT NULL AND s.schemeName != '' " +
            "AND s.schemeAmfiCode IS NOT NULL")
    List<NseOnlineSchemeMaster> getNFOSchemeBySchemeCode(@Param("schemeCodeList") List<String> schemeCodeList);

    @Query("FROM NseOnlineSchemeMaster n " +
            "WHERE (n.schemeAmfiCode = :schemeAmfiCode OR n.schemeAmfiShortName = :schemeAmfi) " +
            "AND n.divReinvestFlag = :reinvestTag " +
            "AND n.amcActiveFlag = 'Y' " +
            "AND n.planType = 'REGULAR' " +
            "AND n.purchaseAllowed = 'Y' " +
            "AND n.schemeAmfiCode <> ''")
    List<NseOnlineSchemeMaster> findBySchemeAmfiOrShortNameAndReinvestTag(
            @Param("schemeAmfiCode") String schemeAmfi,
            @Param("reinvestTag") String reinvestTag
    );

    @Query("FROM NseOnlineSchemeMaster n " +
            "WHERE (n.schemeAmfiCode = :schemeAmfiCode OR n.schemeAmfiShortName = :schemeAmfi) " +
            "AND n.divReinvestFlag = :reinvestTag " +
            "AND n.amcActiveFlag = 'Y' " +
            "AND n.planType = 'REGULAR' " +
            "AND n.sipAllowed = 'Y' " +
            "AND n.schemeAmfiCode <> ''")
    List<NseOnlineSchemeMaster> findBySchemeAmfiOrShortNameAndReinvestTagSip(
            @Param("schemeAmfiCode") String schemeAmfi,
            @Param("reinvestTag") String reinvestTag
    );

    @Query("FROM NseOnlineSchemeMaster n " +
            "WHERE n.schemeName = :schemeName " +
            "AND n.scheme NOT LIKE '%INSURED%' " +
            "AND n.stpEnabled = 'Y' " +
            "AND n.divReinvestFlag = :divReinvestFlag " +
            "AND n.amcActiveFlag = 'Y' " +
            "AND n.planType = 'NORMAL'")
    List<NseOnlineSchemeMaster> findSTPEnabledSchemesForMobile(
            @Param("schemeName") String schemeName,
            @Param("divReinvestFlag") String divReinvestFlag
    );

    @Query("FROM NseOnlineSchemeMaster n " +
            "WHERE n.schemeName = :schemeName " +
            "AND n.scheme NOT LIKE '%INSURED%' " +
            "AND n.swpEnabled = 'Y' " +
            "AND n.divReinvestFlag = :divReinvestFlag " +
            "AND n.amcActiveFlag = 'Y' " +
            "AND n.planType = 'NORMAL'")
    NseOnlineSchemeMaster findSWPEnabledSchemesForMobile(
            @Param("schemeName") String schemeName,
            @Param("divReinvestFlag") String divReinvestFlag
    );

    //new query

    @Query(value = "SELECT scheme_name,scheme,scheme_category,amc_code,amc_name FROM nse_online_scheme_master WHERE amc_code IN (:amc_names) AND amc_code like '%SIF%' AND div_reinvest_flag = 'Z' AND scheme_name IS NOT NULL AND scheme_name != '' AND scheme_amfi_code IS NOT NULL AND scheme_amfi_code != '' AND plan_type = 'NORMAL' AND purchase_allowed = 'Y' AND purchase_transaction_mode IN ('P','DP') AND settlement_type NOT IN ('L1','L0') AND scheme NOT LIKE '%INSURED%' AND scheme_category != 'ETFs' group by scheme_amfi_code", nativeQuery = true)
    List<Object[]> getAllLumpsumSchemesBySifWithAmc(@Param("amc_names") List<String> amc_names);

    @Query(value = "SELECT scheme_name,scheme,scheme_category,amc_code,amc_name FROM nse_online_scheme_master WHERE amc_code IN (:amc_names) AND div_reinvest_flag = :option AND scheme_name IS NOT NULL AND scheme_name != '' AND scheme_amfi_code IS NOT NULL AND scheme_amfi_code != '' AND plan_type = 'NORMAL' AND purchase_allowed = 'Y' AND purchase_transaction_mode IN ('P','DP') AND settlement_type NOT IN ('L1','L0') AND scheme NOT LIKE '%INSURED%' AND scheme_category != 'ETFs' group by scheme_amfi_code", nativeQuery = true)
    List<Object[]> getAllLumpsumSchemesByOptionWithAmc(@Param("option") String option, @Param("amc_names") List<String> amc_names);

    @Query(value = "SELECT scheme_name,scheme,scheme_category,amc_code,amc_name FROM nse_online_scheme_master WHERE amc_code like '%SIF%' AND div_reinvest_flag = 'Z' AND scheme_name IS NOT NULL AND scheme_name != '' AND scheme_amfi_code IS NOT NULL AND scheme_amfi_code != '' AND plan_type = 'NORMAL' AND purchase_allowed = 'Y' AND purchase_transaction_mode IN ('P','DP') AND settlement_type NOT IN ('L1','L0') AND scheme NOT LIKE '%INSURED%' AND scheme_category != 'ETFs' group by scheme_amfi_code", nativeQuery = true)
    List<Object[]> getAllLumpsumSchemesBySif();

    @Query(value = "SELECT scheme_name,scheme,scheme_category,amc_code,amc_name FROM nse_online_scheme_master WHERE div_reinvest_flag = :option AND scheme_name IS NOT NULL AND scheme_name != '' AND scheme_amfi_code IS NOT NULL AND scheme_amfi_code != '' AND plan_type = 'NORMAL' AND purchase_allowed = 'Y' AND purchase_transaction_mode IN ('P','DP') AND settlement_type NOT IN ('L1','L0') AND scheme NOT LIKE '%INSURED%' AND scheme_category != 'ETFs' group by scheme_amfi_code", nativeQuery = true)
    List<Object[]> getAllLumpsumSchemesByOption(@Param("option") String option);

    @Query(value = "SELECT DISTINCT scheme_name,scheme_category,amc_code,amc_name FROM nse_online_scheme_master WHERE amc_code IN (:amc_names) AND amc_code like '%SIF%' AND div_reinvest_flag = 'Z' AND scheme_name IS NOT NULL AND scheme_name != '' AND scheme_amfi_code IS NOT NULL AND scheme_amfi_code != '' AND plan_type = 'NORMAL' AND sip_allowed = 'Y' AND amc_active_flag = 'Y' AND purchase_transaction_mode IN ('P','DP') AND settlement_type not in ('L1','L0') AND scheme NOT LIKE '%INSURED%' AND scheme_category != 'ETFs' group by scheme_amfi_code", nativeQuery = true)
    List<Object[]> getSipSifSchemesWithAmc(@Param("amc_names") List<String> amc_names);

    @Query(value = "SELECT DISTINCT scheme_name,scheme_category,amc_code,amc_name FROM nse_online_scheme_master WHERE amc_code IN (:amc_names) AND div_reinvest_flag = :option AND scheme_name IS NOT NULL AND scheme_name != '' AND scheme_amfi_code IS NOT NULL AND scheme_amfi_code != '' AND plan_type = 'NORMAL' AND sip_allowed = 'Y' AND amc_active_flag = 'Y' AND purchase_transaction_mode IN ('P','DP') AND settlement_type not in ('L1','L0') AND scheme NOT LIKE '%INSURED%' AND scheme_category != 'ETFs' group by scheme_amfi_code", nativeQuery = true)
    List<Object[]> getSipSchemesByOptionWithAmc(@Param("option") String option, @Param("amc_names") List<String> amc_names);

    @Query(value = "SELECT DISTINCT scheme_name,scheme_category,amc_code,amc_name FROM nse_online_scheme_master WHERE amc_code like '%SIF%' AND div_reinvest_flag = 'Z' AND scheme_name IS NOT NULL AND scheme_name != '' AND scheme_amfi_code IS NOT NULL AND scheme_amfi_code != '' AND plan_type = 'NORMAL' AND sip_allowed = 'Y' AND amc_active_flag = 'Y' AND purchase_transaction_mode IN ('P','DP') AND settlement_type not in ('L1','L0') AND scheme NOT LIKE '%INSURED%' AND scheme_category != 'ETFs' group by scheme_amfi_code", nativeQuery = true)
    List<Object[]> getSipSifSchemes();

    @Query(value = "SELECT DISTINCT scheme_name,scheme_category,amc_code,amc_name FROM nse_online_scheme_master WHERE div_reinvest_flag = :option AND scheme_name IS NOT NULL AND scheme_name != '' AND scheme_amfi_code IS NOT NULL AND scheme_amfi_code != '' AND plan_type = 'NORMAL' AND sip_allowed = 'Y' AND amc_active_flag = 'Y' AND purchase_transaction_mode IN ('P','DP') AND settlement_type not in ('L1','L0') AND scheme NOT LIKE '%INSURED%' AND scheme_category != 'ETFs' group by scheme_amfi_code", nativeQuery = true)
    List<Object[]> getSipSchemesByOption(@Param("option") String option);

    @Query("SELECT DISTINCT n.schemeName,n.schemeCategory, n.amcCode, n.amcName, n.schemeCode FROM NseOnlineSchemeMaster n " +
            "WHERE n.schemeAmfiCode IN :schemeAmfiCodes " +
            "AND n.amcActiveFlag = 'Y' " +
            "AND n.planType = 'NORMAL' " +
            "AND n.stpEnabled = 'Y' GROUP BY schemeAmfiCode")
    List<Object[]> findDistinctSchemeNamesForStp(
            @Param("schemeAmfiCodes") List<String> list1
    );

    @Query("SELECT n.amcCode FROM NseOnlineSchemeMaster n " +
            "WHERE n.schemeName = :schemeName " +
            "GROUP BY n.schemeAmfiCode")
    List<String> findDistinctSchemeNameByAmcCodeAndMinAmountschemeName(
            @Param("schemeName") String schemeName
    );

    @Query("SELECT n FROM NseOnlineSchemeMaster n " +
            "WHERE n.schemeName = :schemeName " +
            "AND n.scheme NOT LIKE '%INSURED%' " +
            "AND n.divReinvestFlag = :divReinvestFlag " +
            "AND n.redemptionAllowed = 'Y'")
    List<NseOnlineSchemeMaster> findEligibleSchemeForSwitchAndRedemption(
            @Param("schemeName") String schemeName,
            @Param("divReinvestFlag") String divReinvestFlag);

    @Query("""
    SELECT n 
    FROM NseOnlineSchemeMaster n
    WHERE n.schemeName = :schemeName
      AND n.scheme NOT LIKE '%INSURED%'
""")
    List<NseOnlineSchemeMaster> findBySchemeNameExcludeInsured(
            @Param("schemeName") String schemeName
    );

    @Query(value = """
    SELECT * 
    FROM nse_online_scheme_master
    WHERE amc_name = :amc_name
      AND plan_type = 'NORMAL'
      AND switch_allowed = 'Y'
      AND STR_TO_DATE(NULLIF(scheme_start_date,''), '%d-%m-%Y') <= CURDATE()
      AND scheme_category <> 'ETFs'
      AND settlement_type = 'MF'
    ORDER BY scheme_name ASC
    """, nativeQuery = true)
    List<NseOnlineSchemeMaster> findSchemesByAmc(@Param("amc_name") String amc_name);

    @Query(value = """
    SELECT * 
    FROM nse_online_scheme_master
    WHERE plan_type = 'NORMAL'
      AND switch_allowed = 'Y'
      AND STR_TO_DATE(NULLIF(scheme_start_date,''), '%d-%m-%Y') <= CURDATE()
      AND scheme_category <> 'ETFs'
      AND settlement_type = 'MF'
    ORDER BY scheme_name ASC
    """, nativeQuery = true)
    List<NseOnlineSchemeMaster> findSchemesByAmcCodeAndStartDateWithSettlementCheckNew();

}
