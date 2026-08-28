package com.user.dto;

import com.user.model.UsersMandateDetails;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

@Data
public class UserMandateDetailsDto
{
    @Id
    private Integer id;
    private Integer user_id = 0;
    private Integer online_id = 0;
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
    private Date nse_ach_start_date;
    private Date nse_ach_end_date;
    private String nse_ach_rej_reason = "";
    private Date nse_ach_created_date;

    private String client_name = "";
    private String broker_code = "";
    private Date created_date;
}
