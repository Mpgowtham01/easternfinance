package com.nse.dto.mf;

import lombok.Data;

@Data
public class BasketDetailsDto
{
    private Integer id;
    private String basket_name;
    private String scheme_amfi;
    private String scheme_amfi_code;
    private Integer amount;
    private String client_name;
}
