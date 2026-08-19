package com.user.response;

import com.user.pojo.MandateDetailsPojo;

import java.util.List;

public class BankMandateResponse
{
    public int status;
    public String status_msg;
    public String msg;
    public List<MandateDetailsPojo> list;
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
    public List<MandateDetailsPojo> getList() {
        return list;
    }
    public void setList(List<MandateDetailsPojo> list) {
        this.list = list;
    }

}
