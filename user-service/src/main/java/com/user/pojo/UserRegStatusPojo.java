package com.user.pojo;

import lombok.Data;

@Data
public class UserRegStatusPojo {
    public String vendor = "";
    public Boolean showCard = true;
    public String status = "";
    public String title = "";
    public String description = "";
    public String button_text = "";
    public String call_back_url = "";
}
