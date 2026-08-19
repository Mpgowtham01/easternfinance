package com.nse.dto.mf;


import lombok.Data;


@Data
public class BseNseOnlineAccessDto {

    private Integer id;
    private String online_type;
    private String client_name;
    private String broker_code;
    private String nse_userid;
    private String nse_memberid;
    private String nse_secret_key;
    private String nse_license_key;
}
