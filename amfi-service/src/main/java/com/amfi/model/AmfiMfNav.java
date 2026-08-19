package com.amfi.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Table(name = "amfi_mf_nav")
@Data
public class AmfiMfNav {

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

