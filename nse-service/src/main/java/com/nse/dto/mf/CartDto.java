package com.nse.dto.mf;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
public class CartDto
{
    @Id
    private Integer id = 0;
    private Integer user_id = 0;
    private String name = "";
    private String tax_status_code = "";
    private String tax_status_desc = "";
    private String holding_nature_code = "";
    private String holding_nature_desc = "";
    private String purchase_type = "";
    private String trnx_type = "";
    private String vendor = "";
    private String product_name = "";
    private Boolean nfo_flag;
    private String scheme_name = "";
    private String scheme_amfi_short_name = "";
    private String scheme_product_code = "";
    private String scheme_company = "";
    private String scheme_company_code = "";
    private String scheme_reinvest_tag = "";
    private String to_product_name = "";
    private String to_scheme_name = "";
    private String to_scheme_amfi_short_name = "";
    private String to_scheme_product_code = "";
    private String to_scheme_company = "";
    private String to_scheme_company_code = "";
    private String to_scheme_reinvest_tag = "";
    private String folio_no = "";
    private String amount_type = "";
    private String amount = "";
    private String total_amount = "";
    private String units = "";
    private String total_units = "";
    private String frequency = "";
    private String sip_date = "";
    private String start_date = "";
    private String end_date = "";
    private String first_date = "";
    private String second_date = "";
    private Boolean until_cancel = false;
    private String status = "";
    private Boolean active = true;
    private Date status_date = null;
    private String client_name = "";
    private String investor_code;
    private String broker_code;
    private String euin_code;
    private String payment_type;
    private String payment_mode;
    private String bank_name;
    private String bank_account_number;
    private String 	bank_ifsc;
    private String payment_id = "";
    private String bank_mandate = "";
    private String installment = "";

    private String start_day = "";
    private String start_month = "";
    private String start_year = "";
    private String end_day = "";
    private String end_month = "";
    private String end_year = "";
    private String tenure = "";
    private Boolean first_order_flag = false;

    @Transient
    private String scheme_logo = "";

    @Transient
    private String to_scheme_logo  = "";
}
