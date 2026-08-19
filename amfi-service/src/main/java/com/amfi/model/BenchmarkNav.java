package com.amfi.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Data
@Table(name = "benchmark_nav")
public class BenchmarkNav {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")

    private int id;
    private String scheme_benchmark_name;
    private String scheme_benchmark_code;
    private Double nav;
    private Date nav_date;


}
