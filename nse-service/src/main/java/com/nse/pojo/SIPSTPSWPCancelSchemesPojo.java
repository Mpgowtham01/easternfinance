package com.nse.pojo;

import com.nse.response.SipRegistrationResponseWrapper;
import lombok.Data;

import java.util.List;

@Data
public class SIPSTPSWPCancelSchemesPojo
{
    private Integer user_id;
    private String inv_name;
    private String tax_status;
    private Integer tax_status_desc;
    private String holding_nature;
    private String holding_nature_desc;
    private String broker_code;
    private String investor_code;
    private String logo;
    private List<SipRegistrationResponseWrapper> scheme_list;
}
