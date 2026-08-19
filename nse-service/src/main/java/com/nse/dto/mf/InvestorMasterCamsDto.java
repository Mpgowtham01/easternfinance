package com.nse.dto.mf;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
public class InvestorMasterCamsDto {

    private Integer id;

    private String foliochk;
    private String inv_name;
    private String address1;
    private String address2;
    private String address3;
    private String city;
    private String pincode;
    private String product;
    private String sch_name;

    @Temporal(TemporalType.DATE)
    private Date rep_date;

    private Double clos_bal;
    private Double rupee_bal;
    private String jnt_name1;
    private String jnt_name2;
    private String phone_off;
    private String phone_res;
    private String email;
    private String holding_na;
    private String uin_no;
    private String pan_no;
    private String joint1_pan;
    private String joint2_pan;
    private String guard_pan;
    private String tax_status;
    private String broker_cod;
    private String subbroker;
    private String reinv_flag;
    private String bank_name;
    private String branch;
    private String ac_type;
    private String ac_no;
    private String b_address1;
    private String b_address2;
    private String b_address3;
    private String b_city;
    private String b_pincode;

    @Temporal(TemporalType.DATE)
    private Date inv_dob;

    private String mobile_no;
    private String occupation;
    private String inv_iin;
    private String nom_name;
    private String relation;
    private String nom_addr1;
    private String nom_addr2;
    private String nom_addr3;
    private String nom_city;
    private String nom_state;
    private String nom_pincod;
    private String nom_ph_off;
    private String nom_ph_res;
    private String nom_email;
    private Double nom_percen;
    private String nom2_name;
    private String nom2_relat;
    private String nom2_addr1;
    private String nom2_addr2;
    private String nom2_addr3;
    private String nom2_city;
    private String nom2_state;
    private String nom2_pinco;
    private String nom2_ph_of;
    private String nom2_ph_re;
    private String nom2_email;
    private Double nom2_perce;
    private String nom3_name;
    private String nom3_relat;
    private String nom3_addr1;
    private String nom3_addr2;
    private String nom3_addr3;
    private String nom3_city;
    private String nom3_state;
    private String nom3_pinco;
    private String nom3_ph_of;
    private String nom3_ph_re;
    private String nom3_email;
    private Double nom3_perce;
    private String ifsc_code;
    private String dp_id;
    private String demat;
    private String guard_name;
    private String brokcode;

    @Temporal(TemporalType.DATE)
    private Date folio_date;

    private String aadhaar;
    private String tpa_linked;
    private String fh_ckyc_no;
    private String jh1_ckyc;
    private String jh2_ckyc;
    private String g_ckyc_no;

    @Temporal(TemporalType.DATE)
    private Date jh1_dob;

    @Temporal(TemporalType.DATE)
    private Date jh2_dob;

    @Temporal(TemporalType.DATE)
    private Date guardian_d;

    private String amc_code;
    private String gst_state_;
    private String client_name;
    private Integer user_id;
    private String branch_name;
    private String rm_name;
    private String super_subbroker_name;
    private String subbroker_name;
    private Double total_units;
    private Double current_value;
}
