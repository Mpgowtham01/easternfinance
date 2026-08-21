package com.user.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;


@Entity
@Table(name = "users_online_reg_details")
@Data
public class UsersOnlineRegDetails
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer user_id;
    private String name;
    private String pan;

    @Column(insertable = false, updatable = false)
    private String pan_nonempty;

    private String mobile;
    private String mobile_isd_code;
    private String alter_mobile;
    private String email;
    private String alter_email;
    private String street_1;
    private String street_2;
    private String street_3;
    private String city;
    private String pincode;
    private String state;
    private String state_code;
    private String country;
    private String father_name;
    private String gender;
    private String date_of_birth;
    private String place_of_birth;
    private String country_of_birth;
    private String country_birth_code;
    private String phone_office;
    private String phone_residence;
    private String resident_status;
    private String inv_category;
    private String tax_status;
    private String tax_status_code;
    private String holding_nature;
    private String holding_nature_code;
    private String occupation;
    private String occupation_code;
    private String annual_income;
    private String annual_income_code;
    private String source_of_wealth;
    private String source_of_wealth_code;
    private String political;
    private String political_code;
    private String networth_amount;
    private String networth_dob;
    private String address_type;
    private String address_type_code;

    private String guard_name;
    private String guard_middle_name;
    private String guard_last_name;
    private String guard_pan;
    private String guard_dob;
    private String guard_mobile;
    private String guard_email;
    private String guard_gender;
    private String guard_relationship;
    private String guard_relation_proof;
    private String guard_account_relation;

    private String joint_holder_name1;
    private String joint_holder_middle_name1 = "";
    private String joint_holder_last_name1 = "";
    private String joint_holder_pan1;
    private String joint_holder_dob1;
    private String joint_holder_gender1;
    private String joint_holder_father_name1;
    private String joint_holder_email1;
    private String joint_holder_email_relation1;
    private String joint_holder_mobile1;
    private String joint_holder_mobile1_isdCode;
    private String joint_holder_mobile1_isd_code;
    private String joint_holder_mobile_relation1;
    private String joint_holder_place_of_birth1;
    private String joint_holder_country_birth_code1;
    private String joint_holder_occupation_code1;
    private String joint_holder_occupation_other1;
    private String joint_holder_annual_income_code1;
    private String joint_holder_source_of_wealth_code1;
    private String joint_holder_political_code1;
    private String joint_holder_address_type_code1;
    private String joint_holder_tax_residency1;
    private String joint_holder_tax_country1;
    private String joint_holder_tax_no1;
    @Lob
    private String joint_holder_signature1;
    private String joint_holder_network_dob1;
    private String joint_holder_network_amount1;

    private String joint_holder_name2;
    private String joint_holder_middle_name2 = "";
    private String joint_holder_last_name2 = "";
    private String joint_holder_pan2;
    private String joint_holder_dob2;
    private String joint_holder_gender2;
    private String joint_holder_father_name2;
    private String joint_holder_email2;
    private String joint_holder_email_relation2;
    private String joint_holder_mobile2;
    private String joint_holder_mobile2_isd_code;
    private String joint_holder_mobile_relation2;
    private String joint_holder_place_of_birth2;
    private String joint_holder_country_birth_code2;
    private String joint_holder_occupation_code2;
    private String joint_holder_occupation_other2;
    private String joint_holder_annual_income_code2;
    private String joint_holder_source_of_wealth_code2;
    private String joint_holder_political_code2;
    private String joint_holder_address_type_code2;
    private String joint_holder_tax_residency2;
    private String joint_holder_tax_country2;
    private String joint_holder_tax_no2;
    @Lob
    private String joint_holder_signature2;
    private String joint_holder_network_dob2;
    private String joint_holder_network_amount2;

    private String nri_address1;
    private String nri_address2;
    private String nri_address3;
    private String nri_city;
    private String nri_state;
    private String nri_pincode;
    private String nri_country;
    private String nri_tax_residency;
    private String nri_tax_country;
    private String nri_tax_no;

    private Integer nse_customer;
    private Integer nse_active;
    private String nse_iin_number;

    // DB generated column: nullif(trim(nse_iin_number), '') — read-only
    @Column(insertable = false, updatable = false)
    private String nse_iin_number_nonempty;

    private String nse_iin_number_rej_reason;

    private String mobile_relation;
    private String email_relation;
    private String register_source;
    private String salutation;
    private String client_name;
    private Integer client_id;
    private String broker_code;
    private String euin;
    private String online_flag;
    private String first_name;
    private String middle_name;
    private String last_name;

    private Integer email_verified;
    private String email_authcode;
    private Integer mobile_verified;
    private String mobile_otp;

    private Date created_date;

}
