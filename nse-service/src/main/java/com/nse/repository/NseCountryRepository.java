package com.nse.repository;

import com.nse.model.NseCountry;



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NseCountryRepository extends JpaRepository<NseCountry, Integer> {
    @Query(value = "SELECT country_code FROM nse_country WHERE country_name = :countryName", nativeQuery = true)
    Optional<String> findCountryCodeByCountryName(@Param("countryName") String countryName);
}