package com.user.response;

import com.user.pojo.InvestorClientCodePojo;

import java.util.List;

public class InvestorClientCodeResponse
{
    public int status;
    public String status_msg;
    public String msg;
    public List<InvestorClientCodePojo> client_code_list;
    public int getStatus() {
        return status;
    }
    public void setStatus(int status) {
        this.status = status;
    }
    public String getStatus_msg() {
        return status_msg;
    }
    public void setStatus_msg(String status_msg) {
        this.status_msg = status_msg;
    }
    public String getMsg() {
        return msg;
    }
    public void setMsg(String msg) {
        this.msg = msg;
    }
    public List<InvestorClientCodePojo> getClient_code_list() {
        return client_code_list;
    }
    public void setClient_code_list(List<InvestorClientCodePojo> client_code_list) {
        this.client_code_list = client_code_list;
    }


}

