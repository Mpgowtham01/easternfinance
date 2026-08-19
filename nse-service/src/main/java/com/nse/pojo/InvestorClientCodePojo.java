package com.nse.pojo;

import lombok.Data;

@Data
public class InvestorClientCodePojo {
    public String inv_name = "";
    public String tax_status = "";
    public String tax_status_code = "";
    public String holding_nature = "";
    public String holding_nature_code = "";
    public String broker_code = "";
    public String investor_code = "";
    public String logo = "";
    public String bse_nse_mfu_flag = "";
    public Integer reg_id = 0;
}
