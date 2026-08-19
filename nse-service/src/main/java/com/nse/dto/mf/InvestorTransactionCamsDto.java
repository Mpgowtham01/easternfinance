package com.nse.dto.mf;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
public class InvestorTransactionCamsDto {

    @Id
    private int id;

    private String amc_code;
    private String folio_no;
    private String prodcode;
    private String scheme;
    private String inv_name;
    private String trxntype;
    private Integer trxnno;
    private String trxnmode;
    private String trxnstat;
    private String usercode;
    private String usrtrxno;

    @Temporal(TemporalType.DATE)
    private Date traddate;

    @Temporal(TemporalType.DATE)
    private Date postdate;

    private Double purprice;
    private Double units;
    private Double amount;
    private String brokcode;
    private String subbrok;
    private Double brokperc;
    private Double brokcomm;
    private Integer altfolio;

    @Temporal(TemporalType.DATE)
    private Date rep_date;

    private String time1;
    private String trxnsubtyp;
    private String applicatio;
    private String trxn_natur;
    private Double tax;
    private Double total_tax;
    private String te_15h;
    private String micr_no;
    private String remarks;
    private String swflag;
    private String old_folio;
    private String seq_no;
    private String reinvest_f;
    private String mult_brok;
    private Double stt;
    private String location;
    private String scheme_typ;
    private String tax_status;
    private Double loads;
    private String scanrefno;
    private String pan;
    private Integer inv_iin;
    private String targ_src_s;
    private String trxn_type_;
    private String ticob_trty;
    private String ticob_trno;
    private String ticob_post;
    private String dp_id;
    private Double trxn_charg;
    private Double eligib_amt;
    private String src_of_txn;
    private String trxn_suffi;
    private Double siptrxnno;
    private String ter_locati;
    private String euin;
    private String euin_valid;
    private String euin_opted;
    private String sub_brk_ar;
    private String exch_dc_fl;
    private String src_brk_co;
    private String sys_regn_d;
    private String ac_no;
    private String bank_name;
    private String reversal_c;
    private String exchange_f;
    private String ca_initiat;
    private String gst_state;
    private String igst_amoun;
    private String cgst_amoun;
    private String sgst_amoun;
    private String rev_remark;
    private String original_t;
    private Double stamp_duty;
    private String client_name;
    private Integer user_id;
    private String user_name;
    private String branch;
    private String rm_name;
    private String subbroker_name;
    private String file_type;
}
