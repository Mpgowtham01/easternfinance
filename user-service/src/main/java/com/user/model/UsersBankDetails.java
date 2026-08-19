package com.user.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Table(name = "users_bank_details")
@Data
public class UsersBankDetails
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
    private String bank_name;
    private String bank_code;
    private String bank_mode;
    private String bank_branch;
    private String bank_address;
    private String bank_account_number;
    private String bank_account_holder_name;
    private String bank_account_type;
    private String bank_ifsc_code;
    private String bank_micr_code;
    private String default_bank;
    private String bank_proof;
    private Date created_date;
    private Date updated_date;
}
