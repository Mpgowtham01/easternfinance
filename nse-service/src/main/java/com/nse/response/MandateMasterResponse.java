package com.nse.response;


public class MandateMasterResponse
{
    private Integer user_id;
    private String name = "";
    private String pan = "";
    private String rm_name = "";

    private String bank_name1 = "";
    private String bank_account_number1 = "";
    private String bank_account_type1 = "";
    private String bank_name2 = "";
    private String bank_account_number2 = "";
    private String bank_account_type2 = "";
    private String bank_name3 = "";
    private String bank_account_number3 = "";
    private String bank_account_type3 = "";

    private Integer nse_ach_flag1 = 0;
    private String nse_ach1 = "";
    private String nse_ach_amount1 = "";
    private Integer nse_ach_approved1 = 0;

    private Integer nse_ach_flag2 = 0;
    private String nse_ach2 = "";
    private String nse_ach_amount2 = "";
    private Integer nse_ach_approved2 = 0;

    private Integer nse_ach_flag3 = 0;
    private String nse_ach3 = "";
    private String nse_ach_amount3 = "";
    private Integer nse_ach_approved3 = 0;

    private Integer mfu_mandate_flag1 = 0;
    private String mfu_mandate1 = "";
    private String mfu_mandate_amount1 = "";
    private Integer mfu_mandate_approved1 = 0;

    private Integer mfu_mandate_flag2 = 0;
    private String mfu_mandate2 = "";
    private String mfu_mandate_amount2 = "";
    private Integer mfu_mandate_approved2 = 0;

    private Integer mfu_mandate_flag3 = 0;
    private String mfu_mandate3 = "";
    private String mfu_mandate_amount3 = "";
    private Integer mfu_mandate_approved3 = 0;

    private Integer xsip_otm_flag1 = 0;
    private String xsip_otm1 = "";
    private Integer xsip_otm_approved1 = 0;

    private Integer xsip_otm_flag2 = 0;
    private String xsip_otm2 = "";
    private Integer xsip_otm_approved2 = 0;

    private Integer xsip_otm_flag3 = 0;
    private String xsip_otm3 = "";
    private Integer xsip_otm_approved3 = 0;

    private Integer emandate_otm_flag1 = 0;
    private String emandate_otm1 = "";
    private Integer emandate_otm_approved1 = 0;

    private Integer emandate_otm_flag2 = 0;
    private String emandate_otm2 = "";
    private Integer emandate_otm_approved2 = 0;

    private Integer emandate_otm_flag3 = 0;
    private String emandate_otm3 = "";
    private Integer emandate_otm_approved3 = 0;

    private String client_name = "";

    private String bank_name = "";
    private String bank_account_number = "";
    private String bank_account_type = "";
    private String nse_ach = "";
    private String nse_ach_amount = "";

    private String xsip_otm_amount1 = "";
    private String xsip_otm_amount2 = "";
    private String xsip_otm_amount3 = "";

    private String emandate_otm_amount1 = "";
    private String emandate_otm_amount2 = "";
    private String emandate_otm_amount3 = "";

    private String xsip_otm_rej_reason1 = "";
    private String xsip_otm_rej_reason2 = "";
    private String xsip_otm_rej_reason3 = "";

    private String emandate_otm_rej_reason1 = "";
    private String emandate_otm_rej_reason2 = "";
    private String emandate_otm_rej_reason3 = "";

    public Integer getUser_id() {
        return user_id;
    }

    public void setUser_id(Integer user_id) {
        this.user_id = user_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
    }

    public String getRm_name() {
        return rm_name;
    }

    public void setRm_name(String rm_name) {
        this.rm_name = rm_name;
    }

    public String getBank_name1() {
        return bank_name1;
    }

    public void setBank_name1(String bank_name1) {
        this.bank_name1 = bank_name1;
    }

    public String getBank_account_number1() {
        return bank_account_number1;
    }

    public void setBank_account_number1(String bank_account_number1) {
        this.bank_account_number1 = bank_account_number1;
    }

    public String getBank_name2() {
        return bank_name2;
    }

    public void setBank_name2(String bank_name2) {
        this.bank_name2 = bank_name2;
    }

    public String getBank_account_number2() {
        return bank_account_number2;
    }

    public void setBank_account_number2(String bank_account_number2) {
        this.bank_account_number2 = bank_account_number2;
    }

    public String getBank_name3() {
        return bank_name3;
    }

    public void setBank_name3(String bank_name3) {
        this.bank_name3 = bank_name3;
    }

    public String getBank_account_number3() {
        return bank_account_number3;
    }

    public void setBank_account_number3(String bank_account_number3) {
        this.bank_account_number3 = bank_account_number3;
    }

    public Integer getNse_ach_flag1() {
        return nse_ach_flag1;
    }

    public void setNse_ach_flag1(Integer nse_ach_flag1) {
        this.nse_ach_flag1 = nse_ach_flag1;
    }

    public String getNse_ach1() {
        return nse_ach1;
    }

    public void setNse_ach1(String nse_ach1) {
        this.nse_ach1 = nse_ach1;
    }

    public String getNse_ach_amount1() {
        return nse_ach_amount1;
    }

    public void setNse_ach_amount1(String nse_ach_amount1) {
        this.nse_ach_amount1 = nse_ach_amount1;
    }

    public Integer getNse_ach_approved1() {
        return nse_ach_approved1;
    }

    public void setNse_ach_approved1(Integer nse_ach_approved1) {
        this.nse_ach_approved1 = nse_ach_approved1;
    }

    public Integer getNse_ach_flag2() {
        return nse_ach_flag2;
    }

    public void setNse_ach_flag2(Integer nse_ach_flag2) {
        this.nse_ach_flag2 = nse_ach_flag2;
    }

    public String getNse_ach2() {
        return nse_ach2;
    }

    public void setNse_ach2(String nse_ach2) {
        this.nse_ach2 = nse_ach2;
    }

    public String getNse_ach_amount2() {
        return nse_ach_amount2;
    }

    public void setNse_ach_amount2(String nse_ach_amount2) {
        this.nse_ach_amount2 = nse_ach_amount2;
    }

    public Integer getNse_ach_approved2() {
        return nse_ach_approved2;
    }

    public void setNse_ach_approved2(Integer nse_ach_approved2) {
        this.nse_ach_approved2 = nse_ach_approved2;
    }

    public Integer getNse_ach_flag3() {
        return nse_ach_flag3;
    }

    public void setNse_ach_flag3(Integer nse_ach_flag3) {
        this.nse_ach_flag3 = nse_ach_flag3;
    }

    public String getNse_ach3() {
        return nse_ach3;
    }

    public void setNse_ach3(String nse_ach3) {
        this.nse_ach3 = nse_ach3;
    }

    public String getNse_ach_amount3() {
        return nse_ach_amount3;
    }

    public void setNse_ach_amount3(String nse_ach_amount3) {
        this.nse_ach_amount3 = nse_ach_amount3;
    }

    public Integer getNse_ach_approved3() {
        return nse_ach_approved3;
    }

    public void setNse_ach_approved3(Integer nse_ach_approved3) {
        this.nse_ach_approved3 = nse_ach_approved3;
    }

    public Integer getMfu_mandate_flag1() {
        return mfu_mandate_flag1;
    }

    public void setMfu_mandate_flag1(Integer mfu_mandate_flag1) {
        this.mfu_mandate_flag1 = mfu_mandate_flag1;
    }

    public String getMfu_mandate1() {
        return mfu_mandate1;
    }

    public void setMfu_mandate1(String mfu_mandate1) {
        this.mfu_mandate1 = mfu_mandate1;
    }

    public String getMfu_mandate_amount1() {
        return mfu_mandate_amount1;
    }

    public void setMfu_mandate_amount1(String mfu_mandate_amount1) {
        this.mfu_mandate_amount1 = mfu_mandate_amount1;
    }

    public Integer getMfu_mandate_approved1() {
        return mfu_mandate_approved1;
    }

    public void setMfu_mandate_approved1(Integer mfu_mandate_approved1) {
        this.mfu_mandate_approved1 = mfu_mandate_approved1;
    }

    public Integer getMfu_mandate_flag2() {
        return mfu_mandate_flag2;
    }

    public void setMfu_mandate_flag2(Integer mfu_mandate_flag2) {
        this.mfu_mandate_flag2 = mfu_mandate_flag2;
    }

    public String getMfu_mandate2() {
        return mfu_mandate2;
    }

    public void setMfu_mandate2(String mfu_mandate2) {
        this.mfu_mandate2 = mfu_mandate2;
    }

    public String getMfu_mandate_amount2() {
        return mfu_mandate_amount2;
    }

    public void setMfu_mandate_amount2(String mfu_mandate_amount2) {
        this.mfu_mandate_amount2 = mfu_mandate_amount2;
    }

    public Integer getMfu_mandate_approved2() {
        return mfu_mandate_approved2;
    }

    public void setMfu_mandate_approved2(Integer mfu_mandate_approved2) {
        this.mfu_mandate_approved2 = mfu_mandate_approved2;
    }

    public Integer getMfu_mandate_flag3() {
        return mfu_mandate_flag3;
    }

    public void setMfu_mandate_flag3(Integer mfu_mandate_flag3) {
        this.mfu_mandate_flag3 = mfu_mandate_flag3;
    }

    public String getMfu_mandate3() {
        return mfu_mandate3;
    }

    public void setMfu_mandate3(String mfu_mandate3) {
        this.mfu_mandate3 = mfu_mandate3;
    }

    public String getMfu_mandate_amount3() {
        return mfu_mandate_amount3;
    }

    public void setMfu_mandate_amount3(String mfu_mandate_amount3) {
        this.mfu_mandate_amount3 = mfu_mandate_amount3;
    }

    public Integer getMfu_mandate_approved3() {
        return mfu_mandate_approved3;
    }

    public void setMfu_mandate_approved3(Integer mfu_mandate_approved3) {
        this.mfu_mandate_approved3 = mfu_mandate_approved3;
    }

    public Integer getXsip_otm_flag1() {
        return xsip_otm_flag1;
    }

    public void setXsip_otm_flag1(Integer xsip_otm_flag1) {
        this.xsip_otm_flag1 = xsip_otm_flag1;
    }

    public String getXsip_otm1() {
        return xsip_otm1;
    }

    public void setXsip_otm1(String xsip_otm1) {
        this.xsip_otm1 = xsip_otm1;
    }

    public Integer getXsip_otm_approved1() {
        return xsip_otm_approved1;
    }

    public void setXsip_otm_approved1(Integer xsip_otm_approved1) {
        this.xsip_otm_approved1 = xsip_otm_approved1;
    }

    public Integer getXsip_otm_flag2() {
        return xsip_otm_flag2;
    }

    public void setXsip_otm_flag2(Integer xsip_otm_flag2) {
        this.xsip_otm_flag2 = xsip_otm_flag2;
    }

    public String getXsip_otm2() {
        return xsip_otm2;
    }

    public void setXsip_otm2(String xsip_otm2) {
        this.xsip_otm2 = xsip_otm2;
    }

    public Integer getXsip_otm_approved2() {
        return xsip_otm_approved2;
    }

    public void setXsip_otm_approved2(Integer xsip_otm_approved2) {
        this.xsip_otm_approved2 = xsip_otm_approved2;
    }

    public Integer getXsip_otm_flag3() {
        return xsip_otm_flag3;
    }

    public void setXsip_otm_flag3(Integer xsip_otm_flag3) {
        this.xsip_otm_flag3 = xsip_otm_flag3;
    }

    public String getXsip_otm3() {
        return xsip_otm3;
    }

    public void setXsip_otm3(String xsip_otm3) {
        this.xsip_otm3 = xsip_otm3;
    }

    public Integer getXsip_otm_approved3() {
        return xsip_otm_approved3;
    }

    public void setXsip_otm_approved3(Integer xsip_otm_approved3) {
        this.xsip_otm_approved3 = xsip_otm_approved3;
    }

    public Integer getEmandate_otm_flag1() {
        return emandate_otm_flag1;
    }

    public void setEmandate_otm_flag1(Integer emandate_otm_flag1) {
        this.emandate_otm_flag1 = emandate_otm_flag1;
    }

    public String getEmandate_otm1() {
        return emandate_otm1;
    }

    public void setEmandate_otm1(String emandate_otm1) {
        this.emandate_otm1 = emandate_otm1;
    }

    public Integer getEmandate_otm_approved1() {
        return emandate_otm_approved1;
    }

    public void setEmandate_otm_approved1(Integer emandate_otm_approved1) {
        this.emandate_otm_approved1 = emandate_otm_approved1;
    }

    public Integer getEmandate_otm_flag2() {
        return emandate_otm_flag2;
    }

    public void setEmandate_otm_flag2(Integer emandate_otm_flag2) {
        this.emandate_otm_flag2 = emandate_otm_flag2;
    }

    public String getEmandate_otm2() {
        return emandate_otm2;
    }

    public void setEmandate_otm2(String emandate_otm2) {
        this.emandate_otm2 = emandate_otm2;
    }

    public Integer getEmandate_otm_approved2() {
        return emandate_otm_approved2;
    }

    public void setEmandate_otm_approved2(Integer emandate_otm_approved2) {
        this.emandate_otm_approved2 = emandate_otm_approved2;
    }

    public Integer getEmandate_otm_flag3() {
        return emandate_otm_flag3;
    }

    public void setEmandate_otm_flag3(Integer emandate_otm_flag3) {
        this.emandate_otm_flag3 = emandate_otm_flag3;
    }

    public String getEmandate_otm3() {
        return emandate_otm3;
    }

    public void setEmandate_otm3(String emandate_otm3) {
        this.emandate_otm3 = emandate_otm3;
    }

    public Integer getEmandate_otm_approved3() {
        return emandate_otm_approved3;
    }

    public void setEmandate_otm_approved3(Integer emandate_otm_approved3) {
        this.emandate_otm_approved3 = emandate_otm_approved3;
    }

    public String getClient_name() {
        return client_name;
    }

    public void setClient_name(String client_name) {
        this.client_name = client_name;
    }

    public String getBank_account_type1() {
        return bank_account_type1;
    }

    public void setBank_account_type1(String bank_account_type1) {
        this.bank_account_type1 = bank_account_type1;
    }

    public String getBank_account_type2() {
        return bank_account_type2;
    }

    public void setBank_account_type2(String bank_account_type2) {
        this.bank_account_type2 = bank_account_type2;
    }

    public String getBank_account_type3() {
        return bank_account_type3;
    }

    public void setBank_account_type3(String bank_account_type3) {
        this.bank_account_type3 = bank_account_type3;
    }

    public String getBank_name() {
        return bank_name;
    }

    public void setBank_name(String bank_name) {
        this.bank_name = bank_name;
    }

    public String getBank_account_number() {
        return bank_account_number;
    }

    public void setBank_account_number(String bank_account_number) {
        this.bank_account_number = bank_account_number;
    }

    public String getBank_account_type() {
        return bank_account_type;
    }

    public void setBank_account_type(String bank_account_type) {
        this.bank_account_type = bank_account_type;
    }

    public String getNse_ach() {
        return nse_ach;
    }

    public void setNse_ach(String nse_ach) {
        this.nse_ach = nse_ach;
    }

    public String getNse_ach_amount() {
        return nse_ach_amount;
    }

    public void setNse_ach_amount(String nse_ach_amount) {
        this.nse_ach_amount = nse_ach_amount;
    }

    public String getXsip_otm_amount1() {
        return xsip_otm_amount1;
    }

    public void setXsip_otm_amount1(String xsip_otm_amount1) {
        this.xsip_otm_amount1 = xsip_otm_amount1;
    }

    public String getXsip_otm_amount2() {
        return xsip_otm_amount2;
    }

    public void setXsip_otm_amount2(String xsip_otm_amount2) {
        this.xsip_otm_amount2 = xsip_otm_amount2;
    }

    public String getXsip_otm_amount3() {
        return xsip_otm_amount3;
    }

    public void setXsip_otm_amount3(String xsip_otm_amount3) {
        this.xsip_otm_amount3 = xsip_otm_amount3;
    }

    public String getEmandate_otm_amount1() {
        return emandate_otm_amount1;
    }

    public void setEmandate_otm_amount1(String emandate_otm_amount1) {
        this.emandate_otm_amount1 = emandate_otm_amount1;
    }

    public String getEmandate_otm_amount2() {
        return emandate_otm_amount2;
    }

    public void setEmandate_otm_amount2(String emandate_otm_amount2) {
        this.emandate_otm_amount2 = emandate_otm_amount2;
    }

    public String getEmandate_otm_amount3() {
        return emandate_otm_amount3;
    }

    public void setEmandate_otm_amount3(String emandate_otm_amount3) {
        this.emandate_otm_amount3 = emandate_otm_amount3;
    }

    public String getXsip_otm_rej_reason1() {
        return xsip_otm_rej_reason1;
    }

    public void setXsip_otm_rej_reason1(String xsip_otm_rej_reason1) {
        this.xsip_otm_rej_reason1 = xsip_otm_rej_reason1;
    }

    public String getXsip_otm_rej_reason2() {
        return xsip_otm_rej_reason2;
    }

    public void setXsip_otm_rej_reason2(String xsip_otm_rej_reason2) {
        this.xsip_otm_rej_reason2 = xsip_otm_rej_reason2;
    }

    public String getXsip_otm_rej_reason3() {
        return xsip_otm_rej_reason3;
    }

    public void setXsip_otm_rej_reason3(String xsip_otm_rej_reason3) {
        this.xsip_otm_rej_reason3 = xsip_otm_rej_reason3;
    }

    public String getEmandate_otm_rej_reason1() {
        return emandate_otm_rej_reason1;
    }

    public void setEmandate_otm_rej_reason1(String emandate_otm_rej_reason1) {
        this.emandate_otm_rej_reason1 = emandate_otm_rej_reason1;
    }

    public String getEmandate_otm_rej_reason2() {
        return emandate_otm_rej_reason2;
    }

    public void setEmandate_otm_rej_reason2(String emandate_otm_rej_reason2) {
        this.emandate_otm_rej_reason2 = emandate_otm_rej_reason2;
    }

    public String getEmandate_otm_rej_reason3() {
        return emandate_otm_rej_reason3;
    }

    public void setEmandate_otm_rej_reason3(String emandate_otm_rej_reason3) {
        this.emandate_otm_rej_reason3 = emandate_otm_rej_reason3;
    }
}
