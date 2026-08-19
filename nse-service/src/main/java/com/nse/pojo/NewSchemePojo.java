package com.nse.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewSchemePojo {
    private String scheme_name;
    private String scheme;
    private String scheme_category;
    private String amc_name;
    private String amc_code;
    private String scheme_code;
    private String logo;
}
