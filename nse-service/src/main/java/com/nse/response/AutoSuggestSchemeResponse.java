package com.nse.response;

import com.nse.pojo.AutoSuggestSchemePojo;
import lombok.Data;

import java.util.List;

@Data
public class AutoSuggestSchemeResponse
{
    public Integer status;
    public String status_msg;
    public String msg;
    public List<AutoSuggestSchemePojo> list;
}
