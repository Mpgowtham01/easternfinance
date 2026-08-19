package com.nse.dto.mf;

import jakarta.persistence.Transient;
import lombok.Data;

import java.util.Date;


@Data
public class InvestorSipCams49Dto
{
    private Integer id;
    private String product = "";
    private String scheme = "";
    //String scheme_amfi_short_name;
    private String folio_no = "";
    private String inv_name = "";
    private String aut_trntyp = "";
    private String auto_trno = "";
    private Double auto_amount = 0.0;
    private Double monthly_sip_amount = 0.0;
    private Date reg_date;
    private Date from_date;
    private Date to_date;
    private Date cease_date;
    private String periodicit = "";
    private String period_day = "";
    private String inv_inn = "";
    private String payment_mo = "";
    private String target_sch = "";
    //String target_scheme_amfi_short_name;
    private String subbroker = "";
    private String remarks = "";
    private String top_up_frq = "";
    private Double top_up_amt = 0.0;
    private String ac_type = "";
    private String bank = "";
    private String branch = "";
    private String instrm_no = "";
    private String cheq_micr_ = "";
    private String ac_holder_ = "";
    private String pan = "";
    private String top_up_per = "";
    private String euin = "";
    private String sub_arn_co = "";
    private String ter_location = "";
    private String scheme_code = "";
    private String target_scheme_code = "";
    private String user_code = "";
    private String zone = "";
    private String zone_branch = "";
    private String location = "";
    private String agent_code = "";
    private String agent_name = "";
    private String to_scheme = "";
    private String to_plan = "";
    private String ecs_ac_no = "";
    private String regslno = "";
    private String inv_dpid = "";
    private String inv_clientid = "";
    private String dp_invname = "";
    private String modifyflag = "";
    private String umrncode = "";
    private String sip_type = "";
    private String sip_mode = "";
    private String status = "";
    private String no_of_installments = "";
    private String amc_code = "";
    private String email = "";
    private String mobile = "";
    private String registrar = "";
    private Date created_date;
    private Date last_tran_date;
    private Integer sip_active = 0;
    private String client_name = "";
    private Integer user_id = 0;
    private String user_name = "";
    private String user_branch = "";
    private String rm_name = "";
    private String super_subbroker_name = "";
    private String subbroker_name = "";
    private Double total_units = 0.0;
    private Double current_cost = 0.0;
    private Double current_value = 0.0;
    private Double xirr = 0.0;
    private String scheme_amfi_short_name = "";
    private String scheme_common_name = "";
    private String scheme_amfi_code = "";
    private String scheme_category = "";
    private String scheme_broad_category = "";
    private String advkhoj_remarks = "";

    @Transient
    String from_date_str;
    @Transient
    String to_date_str;
    @Transient
    String reg_date_str;
    @Transient
    String monthly_sip_amount_str;
}
