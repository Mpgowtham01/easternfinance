package com.user.response;

import lombok.Data;

@Data
public class MyMFBoxApiValidityResponse
{
    public boolean valid;
    public String valid_msg;
    public String client_name;
}
