package com.user.response;

import com.user.pojo.CartCountPojo;
import lombok.Data;

@Data
public class CartCountResponse {
    public int status;
    public String status_msg;
    public String msg;
    public CartCountPojo result;
}
