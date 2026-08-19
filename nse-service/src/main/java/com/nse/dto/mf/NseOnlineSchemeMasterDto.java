package com.nse.dto.mf;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
public class NseOnlineSchemeMasterDto {

    @Id
    private int id;

    @Column(name = "unique_sr_no")
    private String uniqueSrNo;

    @Column(name = "scheme_code")
    private String schemeCode;

    @Column(name = "rta_scheme_code")
    private String rtaSchemeCode;

    @Column(name = "amc_scheme_code")
    private String amcSchemeCode;

    @Column(name = "isin")
    private String isin;

    @Column(name = "amc_code")
    private String amcCode;

    @Column(name = "amc_name")
    private String amcName;

    @Column(name = "scheme_type")
    private String schemeType;

    @Column(name = "scheme_category")
    private String schemeCategory;

    @Column(name = "plan_type")
    private String planType;

    @Column(name = "scheme_amfi_code")
    private String schemeAmfiCode;

    @Column(name = "scheme_name")
    private String schemeName;

    @Column(name = "scheme_amfi_short_name")
    private String schemeAmfiShortName;

    @Column(name = "scheme")
    private String scheme;

    @Column(name = "purchase_allowed")
    private String purchaseAllowed;

    @Column(name = "purchase_transaction_mode")
    private String purchaseTransactionMode;

    @Column(name = "new_purchase_min_amount")
    private Double newPurchaseMinAmount;

    @Column(name = "additional_purchase_min_amount")
    private Double additionalPurchaseMinAmount;

    @Column(name = "additional_purchase_max_amount")
    private Double additionalPurchaseMaxAmount;

    @Column(name = "purchase_amount_multiplier")
    private Double purchaseAmountMultiplier;

    @Column(name = "purchase_cutoff_time")
    private String purchaseCutoffTime;

    @Column(name = "redemption_allowed")
    private String redemptionAllowed;

    @Column(name = "redemption_transaction_mode")
    private String redemptionTransactionMode;

    @Column(name = "redemption_min_qty")
    private Double redemptionMinQty;

    @Column(name = "redemption_qty_multiplier")
    private Double redemptionQtyMultiplier;

    @Column(name = "redemption_max_qty")
    private Double redemptionMaxQty;

    @Column(name = "redemption_min_amount")
    private Double redemptionMinAmount;

    @Column(name = "redemption_max_amount")
    private Double redemptionMaxAmount;

    @Column(name = "redemption_amount_multiplier")
    private Double redemptionAmountMultiplier;

    @Column(name = "redemption_cutoff_time")
    private String redemptionCutoffTime;

    @Column(name = "rta_agent_code")
    private String rtaAgentCode;

    @Column(name = "amc_active_flag")
    private String amcActiveFlag;

    @Column(name = "div_reinvest_flag")
    private String divReinvestFlag;

    @Column(name = "sip_allowed")
    private String sipAllowed;

    @Column(name = "stp_enabled")
    private String stpEnabled;

    @Column(name = "swp_enabled")
    private String swpEnabled;

    @Column(name = "switch_allowed")
    private String switchAllowed;

    @Column(name = "settlement_type")
    private String settlementType;

    @Column(name = "amc_ind")
    private String amcInd;

    @Column(name = "face_value")
    private String faceValue;

    @Column(name = "scheme_start_date")
    private String schemeStartDate;

    @Column(name = "maturity_date")
    private String maturityDate;

    @Column(name = "exit_load_flag")
    private String exitLoadFlag;

    @Column(name = "exit_load")
    private String exitLoad;

    @Column(name = "lock_in_period_flag")
    private String lockInPeriodFlag;

    @Column(name = "lock_in_period")
    private String lockInPeriod;

    @Column(name = "channel_partner_code")
    private String channelPartnerCode;

    @Column(name = "reopening_date")
    private String reopeningDate;

    @Column(name = "open_close_ended_scheme")
    private String openCloseEndedScheme;

    @Column(name = "created_date")
    @Temporal(TemporalType.DATE)
    private Date createdDate;
}
