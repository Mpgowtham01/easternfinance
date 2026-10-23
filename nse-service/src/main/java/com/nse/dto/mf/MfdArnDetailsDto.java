package com.nse.dto.mf;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
public class MfdArnDetailsDto
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name= "client_name")
    private String clientName;

    private String broker_code;
    private String euin;
    private String euin_name;
    private String cams_mailback_email;
    private String karvy_mailback_email;
    private String karvy_member_id;
    private String karvy_password;
    private String fundsnet_user_id;
    private String fundsnet_password;
    private String fundsnet_security_answer;
    private String amc_names;
}
