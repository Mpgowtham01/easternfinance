package com.nse.dto.mf;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.util.Date;

@Data
public class BseNseKeyDto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String nse_bse;
    @Column(name = "client_name")
    private String clientName;
    private String domain;
    private String bse_userid;
    private String bse_memberid;
    private String bse_password;
    private String bse_passkey;
    private String nse_appln_id;
    private String nse_password;
    private String nse_userid;
    private String nse_memberid;
    private String nse_secret_key;
    private String nse_license_key;
    @Column(name = "broker_code")
    private String brokerCode;
    private String broker_code_other;
    private String euin;
    private String website_url;
    private String website_name;
    private String domain_url;
    private String logo;
    private String favicon;
    private String company_name;
    private String company_address1;
    private String company_address2;
    private String company_phone;
    private String company_email;
    private Integer multi_asset;
    private String bse_client_code;
    private String mail_support_name;
    private String mail_support_email;
    private String api_key;
    private String android_server_key;
    private String amc_names;
    private String stockal_auth_key;
    private String cams_mailback_email;
    private String karvy_mailback_email;
    private String karvy_member_id;
    private String karvy_password;
    private String fundsnet_user_id;
    private String fundsnet_password;
    private String fundsnet_security_answer;
    private String volt_money_partner_code;
    private String zoho_auth_code;
    private String zoho_access_token;
    private String zoho_refresh_token;
    private String play_store_link;
    private String app_store_link;
    private String default_branch;
    private String default_rm;
    private String one_signal_app_id;
    private String one_signal_api_key;
    private Integer sanchay_crm_flag;
}
