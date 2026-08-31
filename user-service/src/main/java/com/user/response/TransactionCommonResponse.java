package com.user.response;

import com.user.pojo.CommonPojo;
import lombok.Data;

import java.util.List;

@Data
public class TransactionCommonResponse {
    public int status;
    public String status_msg;
    public String msg;
    public List<CommonPojo> list;
}
