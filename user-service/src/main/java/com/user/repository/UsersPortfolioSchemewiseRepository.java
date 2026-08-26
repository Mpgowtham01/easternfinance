package com.user.repository;

import com.user.model.UsersPortfolioSchemewise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsersPortfolioSchemewiseRepository extends JpaRepository<UsersPortfolioSchemewise, Integer> {

    @Query("SELECT DISTINCT u.scheme_code FROM UsersPortfolioSchemewise u WHERE u.user_id = :user_id AND u.client_name = :client_name AND u.amc_code = :amc_code AND u.registrar != 'Manual'")
    List<String> findDistinctSchemeCode(Integer user_id, String client_name, String amc_code);

    @Query("SELECT DISTINCT u.broker_code FROM UsersPortfolioSchemewise u WHERE u.user_id = :user_id AND u.client_name = :client_name AND u.folio_no = :folio_no")
    List<String> findDistinctBrokerCodeByUserIdAndClientNameAndFolioNo(@Param("user_id") Integer userId, @Param("client_name") String clientName, @Param("folio_no") String folioNo);

    @Query("FROM UsersPortfolioSchemewise u WHERE u.user_id = :user_id AND u.total_units > 0 AND u.client_name = :client_name GROUP BY u.amc_code ORDER BY u.amc_name ASC")
    List<UsersPortfolioSchemewise> findByUserIdAndClientNameWithPositiveUnitsGroupByAmcCode(
            @Param("user_id") Integer userid,
            @Param("client_name") String clientName);

    @Query("SELECT DISTINCT u.scheme_code FROM UsersPortfolioSchemewise u WHERE u.user_id = :user_id AND u.client_name = :client_name AND u.amc_code = :amc_code AND u.registrar != 'Manual'")
    List<String> findDistinctSchemeCodeByUserIdAndClientNameAndAmcCodeAndRegistrarNotManual(
            @Param("user_id") Integer userId,
            @Param("client_name") String clientName,
            @Param("amc_code") String amcCode
    );

    @Query("SELECT DISTINCT u.scheme_amfi_code FROM UsersPortfolioSchemewise u " +
            "WHERE u.scheme_amfi_code <> '' AND u.total_units > 0 " +
            "AND u.amc_code = :amc_code AND u.user_id = :user_id " +
            "AND u.client_name = :client_name AND u.scheme_code IN :scheme_codes")
    List<String> findDistinctAmfiCodesByConditions(
            @Param("amc_code") String amcCode,
            @Param("user_id") Integer userId,
            @Param("client_name") String clientName,
            @Param("scheme_codes") List<String> schemeCodes
    );

    @Query("FROM UsersPortfolioSchemewise u WHERE u.user_id = :user_id AND u.client_name = :client_name AND u.scheme_name = :scheme_name AND u.folio_no = :folio_no AND u.registrar <> 'Manual'")
    List<UsersPortfolioSchemewise> findByUserIdAndClientNameAndSchemeNameAndFolioNoAndRegistrarNotManual(
            @Param("user_id") Integer userId,
            @Param("client_name") String clientName,
            @Param("scheme_name") String schemeName,
            @Param("folio_no") String folioNo
    );

    @Query("FROM UsersPortfolioSchemewise u WHERE u.user_id = :user_id AND u.client_name = :client_name AND u.scheme_name = :scheme_name AND u.registrar <> 'Manual'")
    List<UsersPortfolioSchemewise> findByUserIdAndClientNameAndSchemeNameAndRegistrarNotManual(
            @Param("user_id") Integer user_id,
            @Param("client_name") String client_name,
            @Param("scheme_name") String scheme_name
    );

    @Query("FROM UsersPortfolioSchemewise u WHERE u.scheme_name = :scheme_name")
    Optional<UsersPortfolioSchemewise> findFirstBySchemeName(@Param("scheme_name") String schemeName);

    @Query("SELECT DISTINCT u.scheme_amfi_code FROM UsersPortfolioSchemewise u " +
            "WHERE u.total_units > 0 " +
            "AND u.amc_name = :amcCode " +
            "AND u.user_id = :userId " +
            "AND u.client_name = :clientName")
    List<String> findDistinctSchemeAmfiCodeByUserAndAmc(
            @Param("amcCode") String amcCode,
            @Param("userId") int userId,
            @Param("clientName") String clientName
    );

    @Query("FROM UsersPortfolioSchemewise u " +
            "WHERE u.user_id = :userId " +
            "AND u.client_name = :clientName " +
            "AND u.total_units > 0 " +
            "ORDER BY u.scheme_name ASC")
    List<UsersPortfolioSchemewise> findActiveSchemesByUserAndClient(
            @Param("userId") Integer userId,
            @Param("clientName") String clientName);

    @Query("FROM UsersPortfolioSchemewise u " +
            "WHERE u.user_id = :userId " +
            "AND u.client_name = :clientName " +
            "AND (u.scheme_name = :schemeName OR u.scheme_amfi_code = :schemeName) " +
            "AND u.folio_no = :folioNo " +
            "AND u.registrar <> 'Manual'")
    List<UsersPortfolioSchemewise> findByUserIdAndClientNameAndSchemeNameOrAmfiCodeAndFolioNoAndRegistrarNotManual(
            @Param("userId") Integer userId,
            @Param("clientName") String clientName,
            @Param("schemeName") String schemeName,
            @Param("folioNo") String folioNo
    );

    @Query("FROM UsersPortfolioSchemewise u " +
            "WHERE u.client_name = :clientName " +
            "AND u.registrar <> 'Manual' " +
            "AND u.folio_no IN :folioNos " +
            "ORDER BY u.folio_no, u.scheme_code ASC")
    List<UsersPortfolioSchemewise> findByClientNameAndFolioNosAndRegistrarNotManualOrderByFolioNoAndSchemeCode(
            @Param("clientName") String clientName,
            @Param("folioNos") List<String> folioNos
    );

    @Query(value = """
       SELECT registrar
       FROM users_portfolio_schemewise
       WHERE amc_code = :amc_code
         AND registrar IS NOT NULL
         AND registrar <> ''
       LIMIT 1
       """, nativeQuery = true)
    String findRegisterByAmcCode(
            @Param("amc_code") String amc_code
    );

    @Query(value = """
       SELECT amc_code
       FROM users_portfolio_schemewise
       WHERE amc_name = :amc_name
         AND amc_code IS NOT NULL
         AND amc_code <> ''
       LIMIT 1
       """, nativeQuery = true)
    String findAmcCodeByAmcName(
            @Param("amc_name") String amc_name
    );

}
