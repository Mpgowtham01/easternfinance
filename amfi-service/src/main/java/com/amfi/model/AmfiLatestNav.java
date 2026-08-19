package com.amfi.model;


import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "amfi_latest_nav")
public class AmfiLatestNav {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String mf_company;
    private String scheme_nature;
    private String scheme_code;
    private String scheme_name;
    private String scheme_amfi_short_name;
    private String scheme_url;
    private String isin_div_payout_growth_no;
    private String isin_div_reinvestment_no;
    private Double net_asset_value;

    @Temporal(TemporalType.DATE)
    private Date nav_date;

    private String category;
    private String category_short_name;

    @Temporal(TemporalType.DATE)
    private Date create_date;

    private Double prev_net_asset_value;

    @Temporal(TemporalType.DATE)
    private Date prev_nav_date;

    private Double change_percent;
    private String scheme_plan_type;
}

