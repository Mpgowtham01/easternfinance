package com.user.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * One bank account together with every mandate registered against it. Used by the
 * Mobile flavour of /getMandateInfo, which groups mandates under their bank instead
 * of returning a flat list.
 */
@Data
public class BankMandateInfoPojo
{
    public String bank_name = "";
    public String bank_code = "";
    public String bank_mode = "";
    public String bank_branch = "";
    public String bank_account_number = "";
    public String bank_account_holder_name = "";
    public String bank_account_type = "";
    public String bank_ifsc_code = "";
    public String bank_micr_code = "";
    public String default_bank = "";
    public List<MandateDetailsPojo> mandate_list = new ArrayList<MandateDetailsPojo>();
}
