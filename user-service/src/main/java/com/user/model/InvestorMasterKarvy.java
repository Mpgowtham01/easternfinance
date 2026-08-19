package com.user.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "investor_master_karvy")
@Data
public class InvestorMasterKarvy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String product_code;
    private String fund;
    private String folio;
    private String dividend_option;
    private String fund_description;
    private String investor_name;
    private String joint_name1;
    private String joint_name2;
    private String address1;
    private String address2;
    private String address3;
    private String city;
    private String pincode;
    private String state;
    private String country;
    private String tpin;
    private String date_of_birth;
    private String f_name;
    private String m_name;
    private String phone_residence;
    private String phone_res1;
    private String phone_res2;
    private String phone_office;
    private String phone_off1;
    private String phone_off2;
    private String fax_residence;
    private String fax_office;
    private String tax_status;
    private String occ_code;
    private String email;
    private String bankaccno;
    private String bank_name;
    private String account_type;
    private String branch;
    private String bank_address1;
    private String bank_address2;
    private String bank_address3;
    private String bank_city;
    private String bank_phone;
    private String bank_state;
    private String bank_country;
    private String investor_id;
    private String broker_code;
    private String pan_number;
    private String mobile;
    private String report_date;
    private String report_time;
    private String occupation_description;
    private String mode_of_holding;
    private String mode_of_holding_description;
    private String mapin_id;
    private String category;
    private String pan2;
    private String pan3;
    private String guard_name;
    private String nominee;
    private String client_id;
    private String dp_id;
    private String category_d1;
    private String status_desc;
    private String ifsc;
    private String nominee2;
    private String nominee3;
    private String kyc1_flag;
    private String kyc2_flag;
    private String kyc3_flag;
    private String guardian_pan;
    private String last_update;
    private String can;
    private String nominee_rel;
    private String nominee2_r3;
    private String nominee3_r4;
    private String nominee_ra5;
    private String nominee2_r6;
    private String nominee3_r7;
    private String adrh1_info;
    private String adrh2_info;
    private String adrh3_info;
    private String adrg_info;
    private String client_name;
    private Integer user_id;
    private String branch_name;
    private String rm_name;
    private String super_subbroker_name;
    private String subbroker_name;
    private Double total_units;
    private Double current_value;
}