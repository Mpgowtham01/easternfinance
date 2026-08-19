package com.nse.repository;

import com.nse.model.NseOnlineStepUpSchemeMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NseOnlineStepUpSchemeMasterRepository extends JpaRepository<NseOnlineStepUpSchemeMaster, Integer>
{
    @Query(value = """
    SELECT *
    FROM nse_online_step_up_scheme_master
    WHERE scheme_amfi = :schemeName
      AND sip_minimum_installment_amount < 200000
      AND stepup_flag = 'Y'
    LIMIT 1
""", nativeQuery = true)
    Optional<NseOnlineStepUpSchemeMaster> findBySchemeNameAndStepUpFlag(
            @Param("schemeName") String schemeName
    );
}
