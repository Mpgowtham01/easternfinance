package com.amfi.repository;

import com.amfi.model.BenchmarkNavOld;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BenchmarkNavOldRepository extends JpaRepository<BenchmarkNavOld, Integer> {

    @Query("FROM BenchmarkNavOld b WHERE b.scheme_benchmark_code = :schemeBenchmarkCode ORDER BY b.nav_date DESC")
    List<BenchmarkNavOld> findByOldSchemeBenchmarkCodeOrderByNavDateDesc(String schemeBenchmarkCode);
}
