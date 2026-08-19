package com.nse.dto.amfi;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.util.Date;

@Data
public class AmfiMfNavDto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String mf_company;
    private String scheme_nature;
    private String scheme_code;
    private String scheme_name;
    private Double net_asset_value;
    private Date nav_date;
    private Double rebased_nav;
}
