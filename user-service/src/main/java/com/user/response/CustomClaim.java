package com.user.response;

import lombok.Data;

@Data
public class CustomClaim {
    private long iat;
    private long exp;
    private String user_id;
    private String type_id;
    private String investor_id;
    private String client_name;
}
