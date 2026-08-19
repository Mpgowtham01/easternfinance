package com.nse.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "nse_online_step_up_scheme_master")
public class NseOnlineStepUpSchemeMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String amc_code;
    private String amc_name;
    private String scheme_code;
    private String scheme_name;
    private String sip_transaction_mode;
    private String sip_frequency;
    private String sip_dates;
    private String sip_minimum_gap;
    private String sip_maximum_gap;
    private String sip_installment_gap;
    private String sip_status;
    private Double sip_minimum_installment_amount;
    private Double sip_maximum_installment_amount;
    private Double sip_multiplier_amount;
    private String sip_minimum_installment_numbers;
    private String sip_maximum_installment_numbers;
    private String scheme_isin;
    private String scheme_type;
    private String pause_flag;
    private String pause_minimum_installments;
    private String pause_maximum_installments;
    private String pause_modification_count;
    private String stepup_flag;
    private String amc_name_amfi;
    private String scheme_category;
    private String scheme_amfi_code;
    private String scheme_amfi;
}