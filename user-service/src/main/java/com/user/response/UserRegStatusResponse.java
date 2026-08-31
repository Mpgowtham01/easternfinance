package com.user.response;

import com.user.pojo.UserRegStatusPojo;
import lombok.Data;

@Data
public class UserRegStatusResponse
{
    public int status;
    public String status_msg;
    public String msg;
    public UserRegStatusPojo result;
}
