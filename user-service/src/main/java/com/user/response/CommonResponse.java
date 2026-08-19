package com.user.response;

import jakarta.persistence.Transient;
import lombok.Data;

@Data
public class CommonResponse {

    public Integer status;
    public String status_msg;
    public String msg;

    @Transient
    public Integer user_id = 0;
}
