package com.nse.response;

import lombok.Data;

@Data
public class SipRegistrationResponseWrapper
{
    private String registration_no;
    private String folio_no;
    private String scheme_name;
    private String scheme_code;
    private String amount;
    private String start_date;
    private String end_date;
    private String frequency;
    private String frequency_code;
    private String euin_number;
    private String transaction_status;
    private String payment_status;
    private String register_source;
    private String umrn_no;
    private String unique_number;
    private String first_order_flag;
    private String installment;
    private String transaction_date;
    private String to_scheme_name;
    private String to_scheme_code;
    private String amc_code;
    private String ext_unique_ref_no;
    private String unique_ref_no;
    private String trxn_ref_no;
    private String group_order_no;
    private String sip_reg_no;
    private String mandate_id;

    private String logo;
}
