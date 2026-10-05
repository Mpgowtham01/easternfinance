package com.user.pojo;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@Data
@JsonPropertyOrder({
        "regId",
        "vendor",
        "showCard",
        "status",
        "title",
        "description",
        "button_text",
        "call_back_url",
        "mfuCanStatusFlag",
        "clientCodeStatusFlag",
        "online_code",
        "tax_status",
        "tax_status_code",
        "holding_nature",
        "holding_nature_code",
        "broker_code"
})
public class UserRegStatusPojo {
    public Integer regId = 0;
    public String vendor = "";
    public Boolean showCard = true;
    public String status = "";
    public String title = "";
    public String description = "";
    public String button_text = "";
    public String call_back_url = "";
    public Boolean mfuCanStatusFlag = false;
    public Boolean clientCodeStatusFlag = false;
    public String online_code = "";
    public String tax_status = "";
    public String tax_status_code = "";
    public String holding_nature = "";
    public String holding_nature_code = "";
    public String broker_code = "";
}
