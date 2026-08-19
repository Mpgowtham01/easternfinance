package com.nse.response;

import lombok.Data;

import java.util.Date;

@Data
public class XirrResponse
{
    public String trxn_type;
    public Date trxn_date;
    public Double nav;
    public Double units;
    public Double amount;
    public Double stamp_duty;
    public Double tr_charges;
    public Double stt;
    public Double check_units;
    public Double total_units;
}
