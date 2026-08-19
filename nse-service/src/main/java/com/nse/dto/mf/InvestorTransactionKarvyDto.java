package com.nse.dto.mf;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
public class InvestorTransactionKarvyDto {

    @Id
    private int id;

    private String product_code;
    private String fund;
    private String folio_number;
    private String scheme_code;
    private String dividend_option;
    private String fund_description;
    private String transaction_head;
    private String transaction_number;
    private String switch_ref_no;
    private String instrument_number;
    private String investor_name;
    private String transaction_mode;
    private String transaction_status;
    private String branch_name;
    private String branch_transaction_no;

    @Temporal(TemporalType.DATE)
    private Date transaction_date;

    @Temporal(TemporalType.DATE)
    private Date process_date;

    private Double price;
    private Double load_percentage;
    private Double units;
    private Double amount;
    private Double load_amount;

    private String agent_code;
    private String sub_broker_code;
    private String brokerage_percentage;
    private String commission;
    private String investor_id;
    private String report_date;
    private String report_time;
    private String transaction_sub;
    private String application_number;
    private String transaction_id;
    private String transaction_description;
    private String transaction_type;
    private String purchase_date;

    @Temporal(TemporalType.DATE)
    private Date purchase_date1;

    private String purchase_amount;
    private String purchase_units;
    private String transaction_flag;
    private String switch_fund_date;
    private String instrument_date;
    private String instrument_bank;
    private String nav;
    private String purchase_transaction_no;

    private Double stt;

    private String ihno;
    private String branch_code;
    private String inward_number;
    private String remarks;
    private String pan1;
    private String nct_change_date;
    private String status;
    private String rejtrnoorgno;
    private String clientid;
    private String dpid;
    private String trcharges;
    private String toproductcode;
    private String nav_date;
    private String portdate;
    private String assettype;
    private String subtrantype;
    private String citycategory;
    private String euin;
    private String sub_broker_arn_code;
    private String pan2;
    private String pan3;

    private Double tdsamount;

    private String scheme;
    private String plan;
    private String td_trxnmode;
    private String atmcardstatus;

    @Column(length = 1000)
    private String atmcardremarks;

    private String isin;
    private String newunqno;
    private String euin_valid_indicator;
    private String euin_declaration_indicator;
    private String sip_regn_date;
    private String divper;
    private String isc_trno;
    private String brok_ent_date;
    private String guardian_pan;
    private String can;
    private String exchorg_tr6;
    private String elec_trxnf7;
    private String sip_reg_slno;
    private String cleared;
    private String inv_state;
    private String tercat;

    private Double stampduty;

    private String client_name;
    private Integer user_id;
    private String user_name;
    private String branch;
    private String rm_name;
    private String subbroker_name;
}
