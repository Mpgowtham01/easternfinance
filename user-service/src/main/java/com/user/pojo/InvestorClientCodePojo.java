package com.user.pojo;

import lombok.Data;

@Data
public class InvestorClientCodePojo {
    public String pan = "";
    public String inv_name = "";
    public String tax_status = "";
    public String tax_status_code = "";
    public String holding_nature = "";
    public String holding_nature_code = "";
    public String broker_code = "";
    public String investor_code = "";
    public String logo = "";
    public String bse_nse_mfu_flag = "";


    public String getTax_status() {
        return tax_status;
    }
    public void setTax_status(String tax_status) {
        this.tax_status = tax_status;
    }
    public String getHolding_nature() {
        return holding_nature;
    }
    public void setHolding_nature(String holding_nature) {
        this.holding_nature = holding_nature;
    }
    public String getBroker_code() {
        return broker_code;
    }
    public void setBroker_code(String broker_code) {
        this.broker_code = broker_code;
    }
    public String getInvestor_code() {
        return investor_code;
    }
    public void setInvestor_code(String investor_code) {
        this.investor_code = investor_code;
    }
    public String getLogo() {
        return logo;
    }
    public void setLogo(String logo) {
        this.logo = logo;
    }
    public String getBse_nse_mfu_flag() {
        return bse_nse_mfu_flag;
    }
    public void setBse_nse_mfu_flag(String bse_nse_mfu_flag) {
        this.bse_nse_mfu_flag = bse_nse_mfu_flag;
    }
    public String getInv_name() {
        return inv_name;
    }
    public void setInv_name(String inv_name) {
        this.inv_name = inv_name;
    }
    public String getTax_status_code() {
        return tax_status_code;
    }
    public void setTax_status_code(String tax_status_code) {
        this.tax_status_code = tax_status_code;
    }
    public String getHolding_nature_code() {
        return holding_nature_code;
    }
    public void setHolding_nature_code(String holding_nature_code) {
        this.holding_nature_code = holding_nature_code;
    }



}
