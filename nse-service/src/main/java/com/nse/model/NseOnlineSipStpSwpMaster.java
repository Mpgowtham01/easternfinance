package com.nse.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "nse_online_sip_stp_swp_master")
public class NseOnlineSipStpSwpMaster {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "id")

    private Integer id;
    //common
    private String amc_code;
    private String amc_name;
    private String scheme_code;
    private String scheme_name;
    private String scheme_isin;
    private String scheme_type;

    //SIP.
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
    private Double sip_minimum_installment_numbers;
    private Double sip_maximum_installment_numbers;

    private String pause_flag;
    private String pause_minimum_installments;
    private String pause_maximum_installments;
    private String pause_modification_count;
    private String filler_1;
    private String filler_2;
    private String filler_3;
    private String filler_4;
    private String filler_5;
    private Date created_date;

    //STP.
    private String astp_transaction_mode;

    private Double astp_in_minimum_installment_amount;
    private Double astp_in_maximum_installment_amount;
    private Double astp_in_multiplier_amount;
    private Double astp_out_minimum_installment_amount;
    private Double astp_out_maximum_installment_amount;
    private Double astp_out_multiplier_amount;

    private String astp_minimum_installment_units;
    private String astp_maximum_installment_units;
    private String astp_multiplier_units;
    private String astp_minimum_installment_numbers;
    private String astp_maximum_installment_numbers;
    private String astp_reg_in;
    private String astp_reg_out;
    private String astp_frequency;
    private String astp_dates;
    private String astp_minimum_gap;
    private String astp_maximum_gap;
    private String astp_installment_gap;
    private String astp_status;

    //SWP.
    private String aswp_transaction_mode;

    private Double aswp_minimum_installment_amount;
    private Double aswp_maximum_installment_amount;
    private Double aswp_multiplier_amount;

    private String aswp_minimum_installment_units;
    private String aswp_maximum_installment_units;
    private String aswp_multiplier_units;
    private String aswp_minimum_installment_numbers;
    private String aswp_maximum_installment_numbers;
    private String aswp_frequency;
    private String aswp_dates;
    private String aswp_minimum_gap;
    private String aswp_maximum_gap;
    private String aswp_installment_gap;
    private String aswp_status;

    private String master_option;

    private String scheme_amfi;
    private String scheme_category;
    private String scheme_amfi_code;


}
