
package com.user.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")

    private Integer id;
    private String name = "";
    private String pan = "";
    private String mobile = "";
    private String email = "";
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
    @Column(name = "user_password")
    private String userPassword;
    @Column(name = "user_pass")
    private String userPass;
    private String street_1 = "";
    private String street_2 = "";
    private String street_3 = "";
    private String city = "";
    private String pincode = "";
    private String state = "";
    private String country = "";
    private String father_name = "";
    private String gender = "";
    private String marital_status = "";
    private String date_of_birth = "";
    private String anniversary_date = "";
    private String phone_office = "";
    private String phone_residence = "";

    private Integer email_verified = 0;
    private String email_authcode = "";
    private Integer mobile_verified = 0;
    private String mobile_otp = "";

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
    private String euin;

    @Transient
    private String aum;
    @Transient
    private String age;
    @Transient
    private String total_curr_value;
    @Transient
    private String total_profit;
    @Transient
    private String eq_curr_value;
    @Transient
    private String debt_curr_value;
    @Transient
    private String eq_allocation;
    @Transient
    private String debt_allocation;
    @Transient
    private String ekyc_createdate;
    private Date first_investment_date;
    private String note;

    private Date mobile_last_login;

    private String one_signal_subscription_id;
    private String one_signal_mobile_model;
    private String one_signal_mobile_brand;
    private String one_signal_mobile_os;

    private Integer online_kyc_flag;

    private String date_of_birth_greeting = "";

    private String guard_pan;
    private String guard_name;
    private String occupation;
}
