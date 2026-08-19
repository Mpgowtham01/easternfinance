package com.user.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Table(name = "users_nominee_details")
@Data
public class UsersNomineeDetails
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer user_id;
    private Integer online_id;
    private String online_flag;
    private String online_code;
    private String broker_code;
    private String client_name;
    private Integer client_id;
    private String registered_source;
    private String number_of_nominee;
    private String nominee_opt = "";
    private String nominee_soa;

    // Nominee 1
    private String nominee1_type;
    private String nominee1_type_desc;
    private String nominee1_name;
    private String nominee1_middle_name;
    private String nominee1_last_name;
    private String nominee1_pan;
    private String nominee1_dob;
    private String nominee1_address1;
    private String nominee1_address2;
    private String nominee1_address3;
    private String nominee1_pincode;
    private String nominee1_city;
    private String nominee1_state;
    private String nominee1_state_code;
    private String nominee1_country;
    private String nominee1_id_type;
    private String nominee1_id_no;
    private String nominee1_email;
    private String nominee1_mobile;
    private String nominee1_relation;
    private String nominee1_guard_name;
    private String nominee1_guard_middle_name;
    private String nominee1_guard_last_name;
    private String nominee1_guard_pan;
    private String nominee1_guard_dob;
    private String nominee1_guard_relationship;
    private String nominee1_percentage;

    // Nominee 2
    private String nominee2_type;
    private String nominee2_type_desc;
    private String nominee2_name;
    private String nominee2_middle_name;
    private String nominee2_last_name;
    private String nominee2_pan;
    private String nominee2_dob;
    private String nominee2_address1;
    private String nominee2_address2;
    private String nominee2_address3;
    private String nominee2_pincode;
    private String nominee2_city;
    private String nominee2_state;
    private String nominee2_state_code;
    private String nominee2_country;
    private String nominee2_id_type;
    private String nominee2_id_no;
    private String nominee2_email;
    private String nominee2_mobile;
    private String nominee2_relation;
    private String nominee2_guard_name;
    private String nominee2_guard_middle_name;
    private String nominee2_guard_last_name;
    private String nominee2_guard_pan;
    private String nominee2_guard_dob;
    private String nominee2_guard_relationship;
    private String nominee2_percentage;

    // Nominee 3
    private String nominee3_type;
    private String nominee3_type_desc;
    private String nominee3_name;
    private String nominee3_middle_name;
    private String nominee3_last_name;
    private String nominee3_pan;
    private String nominee3_dob;
    private String nominee3_address1;
    private String nominee3_address2;
    private String nominee3_address3;
    private String nominee3_pincode;
    private String nominee3_city;
    private String nominee3_state;
    private String nominee3_state_code;
    private String nominee3_country;
    private String nominee3_id_type;
    private String nominee3_id_no;
    private String nominee3_email;
    private String nominee3_mobile;
    private String nominee3_relation;
    private String nominee3_guard_name;
    private String nominee3_guard_middle_name;
    private String nominee3_guard_last_name;
    private String nominee3_guard_pan;
    private String nominee3_guard_dob;
    private String nominee3_guard_relationship;
    private String nominee3_percentage;

    private Date created_date;
    private Date updated_date;
}
