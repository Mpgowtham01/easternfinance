package com.nse.response;

import com.nse.pojo.BasketDetailsPojo;
import lombok.Data;

import java.util.List;

@Data
public class BasketDetailsResponse
{
    public int status;
    public String status_msg;
    public String msg;
    public List<BasketDetailsPojo> result;
}
