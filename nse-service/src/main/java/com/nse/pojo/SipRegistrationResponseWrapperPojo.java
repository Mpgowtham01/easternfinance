package com.nse.pojo;


import lombok.Data;
import java.util.List;

@Data
public class SipRegistrationResponseWrapperPojo {

    private int status;
    private String statusMsg;
    private String msg;
    private List<SIPSTPSWPCancelSchemesPojo> result;
}
