package com.nse.dto.mf;

import jakarta.persistence.Id;
import lombok.Data;

import java.util.Date;

@Data
public class UserMandateDetailsDto
{
    @Id
    private Integer id;
    private Integer user_id = 0;
    private String online_flag = "";
    private String online_code = "";
    private String bank_account_number = "";

    private Integer xsip_otm_flag = 0;
    private String xsip_otm = "";
    private Integer xsip_otm_approved = 0;
    private String xsip_otm_rej_reason = "";
    private Date xsip_otm_created_date;
    private String xsip_otm_amount = "";

    private Integer emandate_otm_flag = 0;
    private String emandate_otm = "";
    private Integer emandate_otm_approved = 0;
    private String emandate_otm_rej_reason = "";
    private Date emandate_otm_created_date;
    private String emandate_otm_amount = "";

    private Integer nse_ach_flag = 0;
    private String nse_ach = "";
    private String nse_ach_amount = "";
    private Integer nse_ach_approved = 0;
    private String nse_ach_rej_reason = "";
    private Date nse_ach_created_date;

    private Integer mfu_mandate_flag = 0;
    private String mfu_mandate = "";
    private String mfu_mandate_amount = "";
    private Integer mfu_mandate_approved = 0;
    private String mfu_mandate_rej_reason = "";
    private String mfu_mandate_mode = "";
    private String mfu_mmrn_no = "";
    private Date mfu_mandate_created_date;

    private String client_name = "";
    private String broker_code = "";
    private Date created_date;
}
