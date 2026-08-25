package com.nse.response;

import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

@Data
public class UserMandateDetailsResponse
{
        private Integer id;
    private Integer user_id;
    private Integer online_id;
    private String online_flag;
    private String online_code;
    private String broker_code;
    private String bank_account_number;

    // XSIP Mandate
    private Integer xsip_otm_flag;
    private String xsip_otm;
    private String xsip_otm_amount;
    private Integer xsip_otm_approved;
    private String xsip_otm_rej_reason;
    private LocalDate xsip_otm_created_date;

    // E-Mandate
    private Integer emandate_otm_flag;
    private String emandate_otm;
    private String emandate_otm_amount;
    private Integer emandate_otm_approved;
    private String emandate_otm_rej_reason;
    private LocalDate emandate_otm_created_date;

    // NSE ACH
    private Integer nse_ach_flag;
    private String nse_ach;
    private String nse_ach_amount;
    private Integer nse_ach_approved;
    private String nse_ach_rej_reason;
    private LocalDate nse_ach_created_date;

    // MFU Mandate
    private Integer mfu_mandate_flag;
    private String mfu_mandate;
    private String mfu_mandate_mode;
    private String mfu_mmrn_no;
    private String mfu_mandate_amount;
    private Integer mfu_mandate_approved;
    private String mfu_mandate_rej_reason;
    private LocalDate mfu_mandate_created_date;

    private String client_name;

    private Date created_date;

    private String bank_name;
    private String bank_branch;
    private String bank_address;
    private String bank_account_holder_name;
    private String bank_account_type;
    private String bank_ifsc_code;
    private String bank_micr_code;
    private String bank_proof;

    private Date nse_ach_start_date;
    private Date nse_ach_end_date;

}
