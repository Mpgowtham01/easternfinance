package com.nse.dto.mf;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
public class UsersPortfolioSchemewiseDto {

    private String name;
    private String pan;
    private int user_id;
    private String branch;
    private String rm_name;
    private String super_subbroker_name;
    private String subbroker_name;
    private String mobile;
    private int type_id;
    private String amc_code;
    private String amc_name;
    private String folio_no;
    private String scheme_category;
    private String scheme_sub_category;
    private String scheme_broad_category;
    private String scheme_code;
    private String dividend_option;
    private String scheme_option_code;
    private String scheme_amfi_code;

    @Column(length = 1000)
    private String scheme_name;

    private String scheme_amfi_short_name;
    private String scheme_common;

    private Double avg_nav;
    private Double total_units;
    private Double load_free_units;
    private Double purchase_amount;
    private Double invested_amount;
    private Double current_value;
    private Double xirr;
    private Double latest_nav;

    @Temporal(TemporalType.DATE)
    private Date latest_nav_date;

    private Double dividend_paid;
    private Double dividend_reinvest;
    private Double day_change_value;
    private Double day_change_percentage;

    @Temporal(TemporalType.DATE)
    private Date start_date;

    private String salutation;
    private String pincode;
    private String ter_location;
    private String sip_reg_date;

    private int sip_flag;
    private Double sip_amount;

    @Temporal(TemporalType.DATE)
    private Date last_transaction_Date;

    private int average_days;
    private String isin;
    private String registrar;
    private String client_name;
    private String broker_code;
    private String euin;

    @Temporal(TemporalType.DATE)
    private Date created_date;
}
