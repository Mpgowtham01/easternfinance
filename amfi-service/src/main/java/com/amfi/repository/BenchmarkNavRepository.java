package com.amfi.repository;

import com.amfi.model.BenchmarkNav;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BenchmarkNavRepository extends JpaRepository<BenchmarkNav, Integer> {

    @Query("FROM BenchmarkNav b WHERE b.scheme_benchmark_code = :schemeBenchmarkCode ORDER BY b.nav_date DESC")
    List<BenchmarkNav> findBySchemeBenchmarkCodeOrderByNavDateDesc(String schemeBenchmarkCode);
}

