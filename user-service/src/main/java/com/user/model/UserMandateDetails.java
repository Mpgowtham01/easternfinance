package com.user.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "user_mandate_details")
public class UserMandateDetails
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer user_id;
    private String online_flag;
    private String online_code;
    private String bank_account_number;
    private Integer xsip_otm_flag;
    private String xsip_otm;
    private String xsip_otm_amount;
    private Integer xsip_otm_approved;
    private String xsip_otm_rej_reason;
    private Date xsip_otm_created_date;
    private Integer emandate_otm_flag;
    private String emandate_otm;
    private String emandate_otm_amount;
    private Integer emandate_otm_approved;
    private String emandate_otm_rej_reason;
    private Date emandate_otm_created_date;
    private Integer nse_ach_flag;
    private String nse_ach;
    private String nse_ach_amount;
    private Integer nse_ach_approved;
    private String nse_ach_rej_reason;
    private Date nse_ach_created_date;
    private Integer mfu_mandate_flag;
    private String mfu_mandate;
    private String mfu_mandate_mode;
    private String mfu_mmrn_no;
    private String mfu_mandate_amount;
    private Integer mfu_mandate_approved;
    private String mfu_mandate_rej_reason;
    private Date mfu_mandate_created_date;
    private String client_name;
    private Date created_date;
}
