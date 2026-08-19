package com.nse.dto.mf;

import lombok.Data;

import java.util.Date;


@Data
public class InvestorTransactionDto
{
    private Integer id;
    private Integer user_id;
    private String user_name;
    private String pan;
    private String branch;
    private String rm_name;
    private String subbroker_name;
    private String amc_code;
    private String amc_name;
    private String folio_no;
    private String scheme;
    private String prodcode;
    private String inv_name;
    private String trxntype;
    private String trxnno;
    private Date traddate;
    private Double purprice;
    private Double units;
    private Double amount;
    private String trxn_nature;
    private Double total_tax;
    private Double stt;
    private Double stamp_duty;
    private String siptrxnno;
    private String registrar;
    private String broker_code;
    private String client_name;
}
