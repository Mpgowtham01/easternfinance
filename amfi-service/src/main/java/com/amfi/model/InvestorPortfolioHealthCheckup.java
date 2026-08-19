package com.amfi.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "amfi_mf_nav")
public class InvestorPortfolioHealthCheckup
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    private String scheme_company;
    private String scheme_name;
    private String scheme_amfi_short_name;
    private String scheme_amfi_code;
    private String rating;
    private Integer rating_value;
    private String scheme_amfi_common;
    private String scheme_plan_type;
    private String dividend_scheme;
    private String riskometer;
    private String scheme_broad_category;
    private String scheme_advisorkhoj_category;
    private String returns_cmp_3year;
    private String returns_cmp_3year_rank;
    private String returns_cmp_3year_total_rank;
    private Date returns_as_on_date;
    private String scheme_sub_category;
}
