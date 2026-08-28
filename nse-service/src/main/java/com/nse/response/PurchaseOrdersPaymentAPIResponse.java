package com.nse.response;

import lombok.Data;

@Data
public class PurchaseOrdersPaymentAPIResponse {

    private String payment_mode;
    private String client_code;
    private String order_ids;
    private String mandate_id;
    private String status;
    private String order_amount;
    private String remark;
    private String short_url;
    private String basket_id;
    private String bank_account_no;
    private String ifsc;
    private String cheque_no;
    private String cheque_date;
    private String vpa;
    private String callback_url;
    private String neft_rtgs_utr_no;
}
