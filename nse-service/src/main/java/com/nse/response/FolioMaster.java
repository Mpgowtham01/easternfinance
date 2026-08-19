package com.nse.response;

import lombok.Data;

@Data
public class FolioMaster
{
    private String folio_number;
    private String scheme;
    private String scheme_amfi_short_name;
    private Double units;
    private Double current_value;
    private String broker_code;
    private String product_code;
    private Integer user_id;

    private String holdername;
    private String holderpan;

    private String firstjointholdername;
    private String firstjointholderpan;
    private String secondjointholdername;
    private String secondjointholderpan;

    private String dob;
    private String mod_of_holding;
    private String address;
    private String city;
    private String pincode;
    private String mobile;
    private String phone_off;
    private String phone_res;
    private String email;

    private String bank;
    private String branch;
    private String account_type;
    private String bankCity;
    private String acnumber;
    private String ifsc;
    private String guard_name;
    private String guard_pan;
    private String tax_status;
    private String occupation;
    private String updated_date;

    private String nom1_name;
    private String nom1_relation;
    private String nom1_addr1;
    private String nom1_addr2;
    private String nom1_addr3;
    private String nom1_city;
    private String nom1_state;
    private String nom1_pincod;
    private String nom1_ph_off;
    private String nom1_ph_res;
    private String nom1_email;
    private String nom1_percen;

    private String nom2_name;
    private String nom2_relation;
    private String nom2_addr1;
    private String nom2_addr2;
    private String nom2_addr3;
    private String nom2_city;
    private String nom2_state;
    private String nom2_pincod;
    private String nom2_ph_off;
    private String nom2_ph_res;
    private String nom2_email;
    private String nom2_percen;

    private String nom3_name;
    private String nom3_relation;
    private String nom3_addr1;
    private String nom3_addr2;
    private String nom3_addr3;
    private String nom3_city;
    private String nom3_state;
    private String nom3_pincod;
    private String nom3_ph_off;
    private String nom3_ph_res;
    private String nom3_email;
    private String nom3_percen;
}
