package com.user.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Table(name = "users_mandate_details")
@Data
public class UsersMandateDetails
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

    private Integer nse_ach_flag;
    private String nse_ach;
    private String nse_umrn_no;
    private String nse_ach_amount;
    private Integer nse_ach_approved;
    private String nse_ach_rej_reason;
    private Date nse_ach_created_date;
    private Date nse_ach_start_date;
    private Date nse_ach_end_date;
    private String nse_ach_type;

    private String client_name;
    private Integer client_id;
    private String registered_source;

    private Date created_date;
    private Date updated_date;

}
