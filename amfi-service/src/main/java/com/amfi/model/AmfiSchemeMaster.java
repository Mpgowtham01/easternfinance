package com.amfi.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Data
@Table(name = "amfi_scheme_master")
public class AmfiSchemeMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private Integer amc_id;
    private String scheme_company;
    private String scheme_company_short_name;
    private String scheme_advisorkhoj_category;
    private String category_short_name;
    private String scheme_broad_category;
    private String scheme_sub_category;
    private String scheme_category_class;
    private String tax_category;
    private String tax_category_adv;
    private String scheme_amfi_code;
    private String scheme_amfi_growth_code;
    private Boolean erstwhile_scheme_merged;
    private String erstwhile_name;
    private String scheme_amfi;
    private String scheme_amfi_short_name;
    private String scheme_amfi_url;
    private String scheme_amfi_common;
    private String scheme_cams_productcode;
    private String scheme_karvy_productcode;
    private String nj_scheme_name;
    private String isin_no;
    private String isin_divreinvst_no;
    private String isin_sweep_no;
    private Boolean active;
    private Boolean active_close;

    @Temporal(TemporalType.DATE)
    private Date scheme_inception_date;

    @Temporal(TemporalType.DATE)
    private Date scheme_allocation_date;

    @Temporal(TemporalType.DATE)
    private Date scheme_maturity_date;

    private String open_or_closed;
    @Column(columnDefinition = "text")
    private String scheme_objective;

    @Column(columnDefinition = "text")
    private String pres_asset;

    @Column(columnDefinition = "text")
    private String category_description;

    private Integer sip_minimum_amount;
    private String sip_dates;
    private String riskometer;
    private String risk_class;
    private String riskometer_benchmark;
    private Boolean dividend_scheme;
    private String dividend_type;
    private String scheme_plan_type;
    private String url;
    private Double volatility;
    private Double sharpratio;
    private Double mean;
    private Double alpha;
    private Double beta;
    private Double R2;
    private Double downside_deviation;
    private Double treynor_ratio;
    private Double information_ratio;
    private Double upmarket_capture_ratio;
    private Double downmarket_capture_ratio;
    private Double capture_ratio;
    private Double yield_to_maturity;
    private Double average_maturity;
    private Double modified_duration;
    private Double maximum_drawdown_5_yr;
    private Double momentum_score;
    private Double mean_average;
    private Double std_average;
    private Double sharp_ratio_average;
    private Double alpha_average;
    private Double beta_average;
    private Double r2_average;
    private Double sortin_no_ratio_average;
    private String scheme_benchmark_old;
    private String scheme_benchmark;
    private String scheme_benchmark_code;
    private String scheme_benchmark_advisorkhoj;
    private String scheme_benchmark_advisorkhoj_code;
    private String scheme_benchmark_nse;
    private String scheme_benchmark_code_nse;
    private String mirae_benchmark_code;
    private String baroda_benchmark_code;
    private String scheme_benchmark_code_old;
    private String scheme_asset_date;
    private Double scheme_assets;
    private String ter_date;
    private Double ter;
    private String scheme_manager;

    @Column(columnDefinition = "text")
    private String scheme_manager_biography;

    private String asset_class;
    private Double minimum;
    private Double minimum_topup;
    private String portfolio_turnover_ratio;
    private String portfolio_turnover_ratio_date;
    private Double standard_deviation;
    private Boolean historical_nav;
    private Integer par_value;

    @Temporal(TemporalType.DATE)
    private Date par_value_change_date;

    private Double par_value_is_updated;
    private Double par_value_dividend_is_updated;
    private String schemecode_cams;
    private Boolean etf_scheme;

    @Column(length = 5000)
    private String exit1;

    private Double portfolio_duration;
    private Double ve_ytm;
    private String doubled_in;
    private Double sortino_ratio;
    private String market_cap_rank;
    private Double market_cap_largecap_percent;
    private Double market_cap_midcap_percent;
    private Double market_cap_smallcap_percent;
    private Double duration_last;
    private Double maturity_last;
    private Double ytm_last;
    private String scheme_aum_name;
    private String scheme_ssd_name;
    private String amc_common_name;
    private String mirae_scheme_name;
    private String mirae_scheme_code;
    private String scheme_etf_symbol;
    private String mirae_minimum_investment;
}