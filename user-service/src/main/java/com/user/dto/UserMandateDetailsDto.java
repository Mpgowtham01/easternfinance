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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    private String emandate_mode;
    private String emandate_otm;
    private String emandate_otm_amount;
    private Integer emandate_otm_approved;
    private String emandate_otm_rej_reason;
    private LocalDate emandate_otm_created_date;

    // NSE ACH
    private Integer nse_ach_flag;
    private String nse_ach = "";
    private String nse_ach_amount = "";
    private Integer nse_ach_approved = 0;
    private String nse_ach_rej_reason = "";
    private LocalDate nse_ach_created_date;

    private String client_name;

    private Date created_date;

    private Date nse_ach_start_date;
    private Date nse_ach_end_date;

    public static UserMandateDetailsDto fromEntity(UsersMandateDetails entity) {
        UserMandateDetailsDto dto = new UserMandateDetailsDto();
        dto.setId(entity.getId());
        dto.setUser_id(entity.getUser_id());
        dto.setOnline_id(entity.getOnline_id());
        dto.setOnline_flag(entity.getOnline_flag());
        dto.setOnline_code(entity.getOnline_code());
        dto.setBroker_code(entity.getBroker_code());
        dto.setBank_account_number(entity.getBank_account_number());

        dto.setXsip_otm_flag(entity.getXsip_otm_flag());
        dto.setXsip_otm(entity.getXsip_otm());
        dto.setXsip_otm_amount(entity.getXsip_otm_amount());
        dto.setXsip_otm_approved(entity.getXsip_otm_approved());
        dto.setXsip_otm_rej_reason(entity.getXsip_otm_rej_reason());


        dto.setEmandate_otm_flag(entity.getEmandate_otm_flag());
        dto.setEmandate_otm(entity.getEmandate_otm());
        dto.setEmandate_otm_amount(entity.getEmandate_otm_amount());
        dto.setEmandate_otm_approved(entity.getEmandate_otm_approved());
        dto.setEmandate_otm_rej_reason(entity.getEmandate_otm_rej_reason());

        dto.setNse_ach_flag(entity.getNse_ach_flag());
        dto.setNse_ach(entity.getNse_ach());
        dto.setNse_ach_amount(entity.getNse_ach_amount());
        dto.setNse_ach_approved(entity.getNse_ach_approved());
        dto.setNse_ach_rej_reason(entity.getNse_ach_rej_reason());

        dto.setClient_name(entity.getClient_name());
        dto.setCreated_date(entity.getCreated_date());
        return dto;
    }

}
