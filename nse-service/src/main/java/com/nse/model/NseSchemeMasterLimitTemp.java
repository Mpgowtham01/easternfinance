package com.nse.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "nse_scheme_master_limit_temp")
public class NseSchemeMasterLimitTemp {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    private String registrar_id;
    private String amc_code;
    private String product_code;
    private String product_short_name;
    private String product_long_name;
    private String trxn_type;
    private String sub_trxn_type;
    private String sub_trxn_type_desc;
    private String minimum_amount;
    private String maximum_amount;
    private String min_units;
    private String multiples;
    private String min_installment_months;
    private String last_modified_date;
}
