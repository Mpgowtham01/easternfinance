package com.user.model;

import jakarta.persistence.*;
import lombok.Data;


@Data
@Entity
@Table(name = "bse_nse_online_access")
public class BseNseOnlineAccess {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "id")

    private Integer id;
    private String online_type;

    @Column(name = "client_name")
    private String clientName;

    @Column(name = "broker_code")
    private String brokerCode;

    private String nse_userid;
    private String nse_memberid;
    private String nse_secret_key;
    private String nse_license_key;


}

