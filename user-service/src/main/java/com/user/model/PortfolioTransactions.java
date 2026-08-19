package com.user.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Data
@Table(name = "portfolio_transactions")
public class PortfolioTransactions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    private String pan = "";
    private String name = "";
    private String amc_name = "";
    private String folio_no = "";
    private String scheme_broad_category = "";
    private String scheme_category = "";
    private String scheme_code = "";
    private String scheme = "";
    private String scheme_amfi_short_name = "";
    private Date traddate;
    private Double purprice;
    private Double units;
    private Double bal_units;
    private Double amount;
    private Double stt;
    private Double tds;
    private Double stamp_duty;
    private Double trxn_charges;
    private String trxn_no = "";
    private String post_date = "";
    private String trxn_type_ = "";
    private String trxn_suffi = "";
    private String frequency = "";
    private boolean dividend_trxn;
    private String dividend_type = "";
    private Double dividend_per_unit = 0.0;
    private boolean is_sip;
    private Integer no_of_installment;
    private Date start_date;
    private Date end_date;
    private Integer total_installment;
    private boolean is_swp;
    private boolean is_stp;
    private boolean tran_completed;
    private String stp_scheme_code = "";
    private String isin = "";
    private String rta_scheme_code = "";
    private String registrar = "";
    private String broker_code = "";
    private String scheme_pan = "";
    private String upload_type = "";
    private String notes = "";
    private String client_name = "";
    private Integer user_id;
    private String branch = "";
    private String rm_name = "";
    private String subbroker_name = "";
}
