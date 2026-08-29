package com.nse.response;

import lombok.Data;

@Data
public class MandateMasterResponse
{
    private Integer user_id;
    private String name = "";
    private String pan = "";
    private String rm_name = "";

    private String bank_name1 = "";
    private String bank_account_number1 = "";
    private String bank_account_type1 = "";
    private String bank_name2 = "";
    private String bank_account_number2 = "";
    private String bank_account_type2 = "";
    private String bank_name3 = "";
    private String bank_account_number3 = "";
    private String bank_account_type3 = "";

    private Integer nse_ach_flag1 = 0;
    private String nse_ach1 = "";
    private String nse_ach_amount1 = "";
    private Integer nse_ach_approved1 = 0;

    private Integer nse_ach_flag2 = 0;
    private String nse_ach2 = "";
    private String nse_ach_amount2 = "";
    private Integer nse_ach_approved2 = 0;

    private Integer nse_ach_flag3 = 0;
    private String nse_ach3 = "";
    private String nse_ach_amount3 = "";
    private Integer nse_ach_approved3 = 0;

    private Integer mfu_mandate_flag1 = 0;
    private String mfu_mandate1 = "";
    private String mfu_mandate_amount1 = "";
    private Integer mfu_mandate_approved1 = 0;

    private Integer mfu_mandate_flag2 = 0;
    private String mfu_mandate2 = "";
    private String mfu_mandate_amount2 = "";
    private Integer mfu_mandate_approved2 = 0;

    private Integer mfu_mandate_flag3 = 0;
    private String mfu_mandate3 = "";
    private String mfu_mandate_amount3 = "";
    private Integer mfu_mandate_approved3 = 0;

    private Integer xsip_otm_flag1 = 0;
    private String xsip_otm1 = "";
    private Integer xsip_otm_approved1 = 0;

    private Integer xsip_otm_flag2 = 0;
    private String xsip_otm2 = "";
    private Integer xsip_otm_approved2 = 0;

    private Integer xsip_otm_flag3 = 0;
    private String xsip_otm3 = "";
    private Integer xsip_otm_approved3 = 0;

    private Integer emandate_otm_flag1 = 0;
    private String emandate_otm1 = "";
    private Integer emandate_otm_approved1 = 0;

    private Integer emandate_otm_flag2 = 0;
    private String emandate_otm2 = "";
    private Integer emandate_otm_approved2 = 0;

    private Integer emandate_otm_flag3 = 0;
    private String emandate_otm3 = "";
    private Integer emandate_otm_approved3 = 0;

    private String client_name = "";

    private String bank_name = "";
    private String bank_account_number = "";
    private String bank_account_type = "";
    private String nse_ach = "";
    private String nse_ach_amount = "";

    private String xsip_otm_amount1 = "";
    private String xsip_otm_amount2 = "";
    private String xsip_otm_amount3 = "";

    private String emandate_otm_amount1 = "";
    private String emandate_otm_amount2 = "";
    private String emandate_otm_amount3 = "";

    private String xsip_otm_rej_reason1 = "";
    private String xsip_otm_rej_reason2 = "";
    private String xsip_otm_rej_reason3 = "";

    private String emandate_otm_rej_reason1 = "";
    private String emandate_otm_rej_reason2 = "";
    private String emandate_otm_rej_reason3 = "";
}
