package com.nse.pojo;

import lombok.Data;

@Data
public class SipCartRequestPojo {

    private String scheme_name = "";
    private String scheme_reinvest_tag = "";
    private String amount = "";
    private String sip_date = "";
    private String start_date = "";
    private String end_date = "";
    private String installment = "";
    private String sip_tenure = "";
}
