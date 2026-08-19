package com.amfi.repository;

import com.amfi.model.InvestorPortfolioHealthCheckup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.util.List;

@Repository
public interface InvestorPortfolioHealthCheckupRepository extends JpaRepository<InvestorPortfolioHealthCheckup, Serializable>
{
    @Query("SELECT i FROM InvestorPortfolioHealthCheckup i WHERE i.scheme_amfi_code <> '' AND i.scheme_amfi_code IS NOT NULL AND i.scheme_plan_type = 'Regular'")
    List<InvestorPortfolioHealthCheckup> findAllRegularWithSchemeAmfiCode();
}
