package com.nse.dto.mf;

import lombok.Data;

import java.util.Date;

@Data
public class BankDto
{
    private Integer id;

    private Integer user_id;
    private String online_flag;
    private Integer online_id;
    private String online_code;
    private String broker_code;
    private String client_name;
    private String bank_name;
    private String bank_branch;
    private String bank_address;
    private String bank_account_number;
    private String bank_account_holder_name;
    private String bank_account_type;
    private String bank_ifsc_code;
    private String bank_micr_code;
    private String default_bank;
    private String bank_proof;
    private String default_flag;

    private Date created_date;
}
