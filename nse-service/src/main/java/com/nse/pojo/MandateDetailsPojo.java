package com.nse.pojo;

import lombok.Data;

@Data
public class MandateDetailsPojo
{
    public String bank_name = "";
    public String bank_account_holder_name = "";
    public String bank_account_number = "";
    public String bank_ifsc_code = "";
    public String bank_code = "";
    public String bank_micr_code = "";
    public String bank_branch = "";
    public String mandate_type = "";
    public Integer mandate_flag = 0;
    public String mandate_id = "";
    public String mandate_amount = "";
    public Integer mandate_approved =  0;
    public String default_bank = "";
    public String mmrn_number =  "";
    public String mandate_mode = "";
    public String mandate_status = "";
    public String mandate_desc = "";
    public String mandate_date = "";
    public String account_type = "";
    public String mandate_end_date = "";
}
