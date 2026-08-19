package com.nse.response;

import com.nse.pojo.CartPojo;

import java.util.List;

public class CartResponse
{
    public int status;
    public String status_msg;
    public String msg;
    public List<CartPojo> result;

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
    public List<CartPojo> getResult() {
        return result;
    }
    public void setResult(List<CartPojo> result) {
        this.result = result;
    }

}