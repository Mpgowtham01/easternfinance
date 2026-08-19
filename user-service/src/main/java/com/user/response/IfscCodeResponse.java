package com.user.response;

import com.user.pojo.IfscCodePojo;

public class IfscCodeResponse
{
    public int status;
    public String status_msg;
    public String msg;
    public IfscCodePojo result;
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
    public IfscCodePojo getResult() {
        return result;
    }
    public void setResult(IfscCodePojo result) {
        this.result = result;
    }
}
