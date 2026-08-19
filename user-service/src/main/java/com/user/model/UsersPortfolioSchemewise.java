package com.user.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "users_portfolio_schemewise")
public class UsersPortfolioSchemewise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private String pan;
    private String mobile;
    private Integer user_id;
    private String branch;
    private String rm_name;
    private String super_subbroker_name;
    private String subbroker_name;
    private Integer type_id;
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
    private String scheme_name;
    private String scheme_amfi_short_name;
    private String scheme_common;
    private Double total_units;
    private Double avg_nav;
    private Double load_free_units;
    private Double purchase_amount;
    private Double invested_amount;
    private Double current_value;
    private Double xirr;
    private Double latest_nav;
    private Double dividend_paid;
    private Double dividend_reinvest;
    private Double day_change_value;
    private Double day_change_percentage;
    private Date start_date;
    private String salutation;
    private String pincode;
    private String ter_location;
    private String sip_reg_date;
    private Integer sip_flag;
    private Double sip_amount;
    private Date last_transaction_Date;
    private Integer average_days;
    private String isin;
    private String registrar;
    private String client_name;
    private String broker_code;
    private String euin;
    private Date created_date;
}
