package com.user.dto;

import lombok.Data;

import java.util.Date;

@Data
public class UserDto {
    private Integer id;
    private Integer user_id;
    private String name = "";
    private String pan = "";
    private String mobile = "";
    private String mobile_isd_code = "";
    private String email= "";
    private String alter_email = "";
    private String alter_mobile = "";
    private Integer type_id = 1;
    private String branch = "";
    private String rm_name = "";
    private String subbroker_name = "";
    private String super_subbroker_name = "";
    private String payout = "";
    private Boolean login_status = true;
    private Integer active = 1;
    private Integer email_active = 1;
    private String user_password = "";
    private String user_pass = "";
    private String street_1 = "";
    private String street_2 = "";
    private String street_3 = "";
    private String city = "";
    private String pincode = "";
    private String state = "";
    private String state_code = "";
    private String country = "";
    private String father_name = "";
    private String gender = "";
    private String marital_status = "";
    private String date_of_birth = "";
    private String anniversary_date = "";
    private String place_of_birth = "";
    private String country_of_birth = "";
    private String country_birth_code = "";
    private String phone_office = "";
    private String phone_residence = "";
    private String resident_status = "";
    private String inv_category = "";
    private String tax_status = "";
    private String tax_status_code = "";
    private String holding_nature = "";
    private String holding_nature_code = "";
    private String occupation = "";
    private String occupation_code = "";
    private String annual_income = "";
    private String annual_income_code = "";
    private String source_of_wealth = "";
    private String source_of_wealth_code = "";
    private String political = "";
    private String political_code = "";
    private String networth_amount = "";
    private String networth_dob = "";
    private String address_type = "";
    private String address_type_code = "";

    private String guard_name = "";
    private String guard_middle_name = "";
    private String guard_last_name = "";
    private String guard_pan = "";
    private String guard_dob = "";
    private String guard_mobile = "";
    private String guard_email = "";
    private String guard_gender = "";
    private String guard_relationship = "";
    private String guard_relation_proof = "";
    private String guard_account_relation = "";

    private String joint_holder_name1 = "";
    private String joint_holder_middle_name1 = "";
    private String joint_holder_last_name1 = "";
    private String joint_holder_pan1 = "";
    private String joint_holder_dob1 = "";
    private String joint_holder_email1 = "";
    private String joint_holder_email_relation1 = "";
    private String joint_holder_mobile1 = "";
    private String joint_holder_mobile1_isd_code = "";
    private String joint_holder_mobile_relation1 = "";
    private String joint_holder_signature1 = "";
    private String joint_holder_place_of_birth1 = "";
    private String joint_holder_country_birth_code1 = "";
    private String joint_holder_occupation_code1 = "";
    //private String joint_holder_occupation_other1 = "";
    private String joint_holder_annual_income_code1 = "";
    private String joint_holder_source_of_wealth_code1 = "";
    //private String joint_holder_source_of_wealth_other1 = "";
    private String joint_holder_political_code1 = "";
    private String joint_holder_address_type_code1 = "";

    private String joint_holder_name2 = "";
    private String joint_holder_middle_name2 = "";
    private String joint_holder_last_name2 = "";
    private String joint_holder_pan2 = "";
    private String joint_holder_dob2 = "";
    private String joint_holder_email2 = "";
    private String joint_holder_email_relation2 = "";
    private String joint_holder_mobile2 = "";
    private String joint_holder_mobile2_isd_code = "";
    private String joint_holder_mobile_relation2 = "";
    private String joint_holder_signature2 = "";
    private String joint_holder_place_of_birth2 = "";
    private String joint_holder_country_birth_code2 = "";
    private String joint_holder_occupation_code2 = "";
    //private String joint_holder_occupation_other2 = "";
    private String joint_holder_annual_income_code2 = "";
    private String joint_holder_source_of_wealth_code2 = "";
    //private String joint_holder_source_of_wealth_other2 = "";
    private String joint_holder_political_code2 = "";
    private String joint_holder_address_type_code2 = "";

    private String nri_address1 = "";
    private String nri_address2 = "";
    private String nri_address3 = "";
    private String nri_city = "";
    private String nri_state = "";
    private String nri_pincode = "";
    private String nri_country = "";

    private String number_of_nominee = "";
    private String nominee_opt = "";
    private String nominee_soa = "";
    private String nominee1_type = "";
    private String nominee1_type_desc = "";
    private String nominee1_name = "";
    private String nominee1_middle_name = "";
    private String nominee1_last_name = "";
    private String nominee1_dob = "";
    private String nominee1_address1 = "";
    private String nominee1_address2 = "";
    private String nominee1_address3 = "";
    private String nominee1_pincode = "";
    private String nominee1_city = "";
    private String nominee1_state = "";
    private String nominee1_state_code = "";
    private String nominee1_country = "";
    private String nominee1_mobile = "";
    private String nominee1_email = "";
    private String nominee1_id_type = "";
    private String nominee1_id_no = "";
    private String nominee1_relation = "";
    private String nominee1_percentage = "";
    private String nominee1_guard_name = "";
    private String nominee1_guard_pan = "";
    private String nominee1_guard_relationship = "";

    private String nominee1_pan = "";
    private String nominee2_pan = "";
    private String nominee3_pan = "";

    private String nominee2_type = "";
    private String nominee2_type_desc = "";
    private String nominee2_name = "";
    private String nominee2_middle_name = "";
    private String nominee2_last_name = "";
    private String nominee2_dob = "";
    private String nominee2_relation = "";
    private String nominee2_percentage = "";
    private String nominee2_address1 = "";
    //private String nominee2_address2 = "";
    //private String nominee2_address3 = "";
    private String nominee2_pincode = "";
    private String nominee2_city = "";
    private String nominee2_state = "";
    private String nominee2_state_code = "";
    private String nominee2_country = "";
    private String nominee2_mobile = "";
    private String nominee2_email = "";
    private String nominee2_id_type = "";
    private String nominee2_id_no = "";
    private String nominee2_guard_name = "";
    private String nominee2_guard_pan = "";
    private String nominee2_guard_relationship = "";

    private String nominee3_type = "";
    private String nominee3_type_desc = "";
    private String nominee3_name = "";
    private String nominee3_middle_name = "";
    private String nominee3_last_name = "";
    private String nominee3_dob = "";
    private String nominee3_relation = "";
    private String nominee3_percentage = "";
    private String nominee3_address1 = "";
    //private String nominee3_address2 = "";
    //private String nominee3_address3 = "";
    private String nominee3_pincode = "";
    private String nominee3_city = "";
    private String nominee3_state = "";
    private String nominee3_state_code = "";
    private String nominee3_country = "";
    private String nominee3_mobile = "";
    private String nominee3_email = "";
    private String nominee3_id_type = "";
    private String nominee3_id_no = "";
    private String nominee3_guard_name = "";
    private String nominee3_guard_pan = "";
    private String nominee3_guard_relationship = "";

    private String bank_name1 = "";
    private String bank_code1 = "";
    private String bank_mode1 = "";
    private String bank_branch1 = "";
    private String bank_address1 = "";
    private String bank_account_number1 = "";
    private String bank_account_holder_name1 = "";
    private String bank_account_type1 = "";
    private String bank_ifsc_code1 = "";
    private String bank_micr_code1 = "";
    private String default_bank1 = "N";
    private String bank_proof1 = "";

    private String bank_name2 = "";
    private String bank_code2 = "";
    private String bank_mode2 = "";
    private String bank_branch2 = "";
    private String bank_address2 = "";
    private String bank_account_number2 = "";
    private String bank_account_holder_name2 = "";
    private String bank_account_type2 = "";
    private String bank_ifsc_code2 = "";
    private String bank_micr_code2 = "";
    private String default_bank2 = "N";
    private String bank_proof2 = "";

    private String bank_name3 = "";
    private String bank_code3 = "";
    private String bank_mode3 = "";
    private String bank_branch3 = "";
    private String bank_address3 = "";
    private String bank_account_number3 = "";
    private String bank_account_holder_name3 = "";
    private String bank_account_type3 = "";
    private String bank_ifsc_code3 = "";
    private String bank_micr_code3 = "";
    private String default_bank3 = "N";
    private String bank_proof3 = "";

    private String bank_name4 = "";
    private String bank_code4 = "";
    private String bank_mode4 = "";
    private String bank_branch4 = "";
    private String bank_address4 = "";
    private String bank_account_number4 = "";
    private String bank_account_holder_name4 = "";
    private String bank_account_type4 = "";
    private String bank_ifsc_code4 = "";
    private String bank_micr_code4 = "";
    private String default_bank4 = "N";
    private String bank_proof4 = "";

    private String bank_name5 = "";
    private String bank_code5 = "";
    private String bank_mode5 = "";
    private String bank_branch5 = "";
    private String bank_address5 = "";
    private String bank_account_number5 = "";
    private String bank_account_holder_name5 = "";
    private String bank_account_type5 = "";
    private String bank_ifsc_code5 = "";
    private String bank_micr_code5 = "";
    private String default_bank5 = "N";
    private String bank_proof5 = "";

    private Integer xsip_otm_flag1 = 0;
    private String xsip_otm1 = "";
    private Integer xsip_otm_approved1 = 0;
    private String xsip_otm_amount1 = "";
    private String xsip_otm_rej_reason1 = "";
    private Date xsip_otm_created_date1;

    private Integer xsip_otm_flag2 = 0;
    private String xsip_otm2 = "";
    private Integer xsip_otm_approved2 = 0;
    private String xsip_otm_amount2 = "";
    private String xsip_otm_rej_reason2 = "";
    private Date xsip_otm_created_date2;

    private Integer xsip_otm_flag3 = 0;
    private String xsip_otm3 = "";
    private Integer xsip_otm_approved3 = 0;
    private String xsip_otm_amount3 = "";
    private String xsip_otm_rej_reason3 = "";
    private Date xsip_otm_created_date3;

    private Integer emandate_otm_flag1 = 0;
    private String emandate_otm1 = "";
    private Integer emandate_otm_approved1 = 0;
    private String emandate_otm_amount1 = "";
    private String emandate_otm_rej_reason1 = "";
    private Date emandate_otm_created_date1;

    private Integer emandate_otm_flag2 = 0;
    private String emandate_otm2 = "";
    private Integer emandate_otm_approved2 = 0;
    private String emandate_otm_amount2 = "";
    private String emandate_otm_rej_reason2 = "";
    private Date emandate_otm_created_date2;

    private Integer emandate_otm_flag3 = 0;
    private String emandate_otm3 = "";
    private Integer emandate_otm_approved3 = 0;
    private String emandate_otm_amount3 = "";
    private String emandate_otm_rej_reason3 = "";
    private Date emandate_otm_created_date3;

    private Integer nse_ach_flag1 = 0;
    private String nse_ach1 = "";
    private String nse_ach_amount1 = "";
    private Integer nse_ach_approved1 = 0;
    private String nse_ach_rej_reason1 = "";
    private Date nse_ach_created_date1;

    private Integer mfu_mandate_flag1 = 0;
    private String mfu_mandate1 = "";
    private String mfu_mandate_amount1 = "";
    private Integer mfu_mandate_approved1 = 0;
    private String mfu_mandate_rej_reason1 = "";
    private String mfu_mandate_mode1 = "";
    private String mfu_mmrn_no1 = "";
    private Date mfu_mandate_created_date1;

    private Integer nse_ach_flag2 = 0;
    private String nse_ach2 = "";
    private String nse_ach_amount2 = "";
    private Integer nse_ach_approved2 = 0;
    private String nse_ach_rej_reason2 = "";
    private Date nse_ach_created_date2;

    private Integer mfu_mandate_flag2 = 0;
    private String mfu_mandate2 = "";
    private String mfu_mandate_amount2 = "";
    private Integer mfu_mandate_approved2 = 0;
    private String mfu_mandate_rej_reason2 = "";
    private String mfu_mandate_mode2 = "";
    private String mfu_mmrn_no2 = "";
    private Date mfu_mandate_created_date2;

    private Integer nse_ach_flag3 = 0;
    private String nse_ach3 = "";
    private String nse_ach_amount3 = "";
    private Integer nse_ach_approved3 = 0;
    private String nse_ach_rej_reason3 = "";
    private Date nse_ach_created_date3;

    private Integer mfu_mandate_flag3 = 0;
    private String mfu_mandate3 = "";
    private String mfu_mandate_amount3 = "";
    private Integer mfu_mandate_approved3 = 0;
    private String mfu_mandate_rej_reason3 = "";
    private String mfu_mandate_mode3 = "";
    private String mfu_mmrn_no3 = "";
    private Date mfu_mandate_created_date3;

    private Integer email_verified = 0;
    private String email_authcode = "";
    private String email_relation = "";
    private Integer mobile_verified = 0;
    private String mobile_otp = "";
    private String mobile_relation = "";
    private Integer bse_customer = 0;
    private Integer bse_active = 0;
    private String bse_client_code = "";
    private Integer bse_steps_completed = 0;
    private Integer bse_investor_info = 0;
    private Integer bse_contact_info = 0;
    private Integer bse_bank_info = 0;
    private Integer bse_nominee_info = 0;
    private Integer bse_signature_info = 0;
    private String bse_signature = "";
    private String bse_chequeleaf = "";
    private String bse_client_type = "";

    private Integer nse_customer = 0;
    private Integer nse_active = 0;
    private String nse_iin_number = "";

    private Integer mfu_customer = 0;
    private Integer mfu_active = 0;
    private String mfu_can_number = "";

    private String broker_code = "";
    private Date created_date;
    private String language = "";
    private String referral_partner_code = "";
    private String register_source = "";
    private String aadhaar = "";
    private String passport = "";

    private String emp_code = "";
    private String user_risk = "";
    private String alias = "";

    private String loginid = "";
    private String client_name = "";
    private String salutation = "";
    private Integer hni_clients = 0;
    private Integer huf_clients = 0;

    private Integer portfolio_created = 1;
    private boolean is_purchase_allowed = true;
    private boolean is_redeem_allowed = true;
    private boolean is_switch_allowed = true;
    private boolean is_stp_allowed = true;
    private boolean is_swp_allowed = true;
    private boolean mf_oneday_change = true;
    private String acquired_by = "";
    private String freshchat_restoreid = "";

    private String gst_no = "";
    private String portfolio_sent_date = "";
    private String login_sent_date = "";
    private String birthday_sent_date = "";
    private Double mf_aum = 0.0;
    private String maker_approver = "";
    private String investor_photo = "";
    private String online_flag = "";

    private String nominee1_guard_dob = "";
    private String nominee2_guard_dob = "";
    private String nominee3_guard_dob = "";
    private String euin;

    private Date first_investment_date;
    private String note;
    private Date mobile_last_login;

    private String one_signal_subscription_id;
    private String one_signal_mobile_model;
    private String one_signal_mobile_brand;
    private String one_signal_mobile_os;

    private Integer online_kyc_flag;

    private String first_name = "";
    private String middle_name = "";
    private String last_name = "";

    private String date_of_birth_greeting = "";
}
