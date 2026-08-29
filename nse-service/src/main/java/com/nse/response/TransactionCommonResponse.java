package com.nse.response;

import com.nse.pojo.CommonPojo;
import lombok.Data;

import java.util.List;

@Data
public class TransactionCommonResponse
{
    public int status;
    public String status_msg;
    public String msg;
    public List<CommonPojo> list;
}
