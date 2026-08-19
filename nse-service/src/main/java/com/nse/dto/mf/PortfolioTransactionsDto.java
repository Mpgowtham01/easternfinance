package com.nse.dto.mf;

import lombok.Data;

import java.util.Date;


@Data
public class PortfolioTransactionsDto
{
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
    private String trxn_type_ = "";
    private String trxn_suffi = "";
    private boolean dividend_trxn;
    private String dividend_type = "";
    private String example = "";
    private Double dividend_per_unit = 0.0;
    private String frequency = "";
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
    private Integer user_id;
    private String branch = "";
    private String rm_name = "";
    private String subbroker_name = "";
    private String client_name = "";
    private Date created_date;

    private Double stt = 0.0;
    private Double tds = 0.0;
    private Double stamp_duty = 0.0;
    private Double trxn_charges = 0.0;
    private String trxn_no = "";
    private String post_date = "";

    private String register_source = "";
}
