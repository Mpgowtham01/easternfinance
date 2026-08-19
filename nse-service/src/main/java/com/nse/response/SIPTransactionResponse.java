package com.nse.response;

import lombok.Data;

import java.util.Date;

@Data
public class SIPTransactionResponse
{
    String scheme_name;
    String scheme_amfi_short_name;
    String scheme_code;
    String folio_no;
    String category;
    String tax_category;
    String transaction_date;
    String transaction_type;
    String transaction_nature;
    Double purchase_price;
    Double purchase_units;
    Double purchase_amount;
    Double total_units;
    String current_date;
    Double current_price;
    Double current_value;
    Integer days;
    Double capital_gain;
    Double abs_return;
    Double cagr_return;
    String sensex;
    String index_cost;
    String broker_code;
    Double grandfathered_units;
    Double grandfathered_amount;
    Double grandfathered_nav;
    Double stg_gain;
    Double ltg_gain;
    Double indexed_nav;
    Date purchaseDate;
    String purchase_date;
    Date redemptionDate;
    String redemption_date;
    Double redemption_price;
    Double redemption_units;
    Double redemption_amount;
    String logo;
    Double stamp_duty;
    String bank_name;
    String account_number;
    Double tds_amount;
    Double stt_amount;
    Double load_amount;
}
