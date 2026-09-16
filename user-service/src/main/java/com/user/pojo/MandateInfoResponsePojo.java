package com.user.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Mobile response envelope for /getMandateInfo.
 */
@Data
public class MandateInfoResponsePojo
{
    public Integer status = 200;
    public String status_msg = "Success";
    public String msg = "Success";
    public List<BankMandateInfoPojo> bank_list = new ArrayList<BankMandateInfoPojo>();
}
