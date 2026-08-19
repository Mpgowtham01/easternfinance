package com.user.response;

import lombok.Data;

@Data
public class PanKYCResponse {
    private Integer status;
    private String status_msg;
    private String msg;
    private Boolean kyc_status = false;
    private String inv_name;

}
