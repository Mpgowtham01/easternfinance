package com.nse.response;

import com.nse.dto.mf.InvestorTransactionDto;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class InvestorSchemeWisePortfolioResponse implements Cloneable
{
    private String scheme;
    private String scheme_amfi_short_name;
    private String scheme_class;
    private String scheme_code;
    private String scheme_registrar;
    private String scheme_amfi_common;
    private String amc_code;
    private String amc_name;
    private String foliono;
    private String amc_logo;
    private Double scheme_weight;
    private Double scheme_rating;
    private Double scheme_score;
    private String scheme_review;
    private String scheme_benchmark_name;
    private String scheme_benchmark_code;
    private String scheme_risk_profile;
    private String notes;
    private String broker_code;
    private String multiple_broker_code;
    private String euin;

    private Date investmentStartDate;
    private Date investmentReStartDate;
    private Date lastInvestmentDate;
    private Date lastTransactionDate;
    private double investmentStartNav;
    private double investmentStartValue;

    private double totalInflow;
    private double totalOutflow;
    private double partialInflow;
    private double partialOutflow;
    private double totalInvestedAmount;
    private double totalOriginalInvestedAmount;
    private double totalSwitchInAmount;
    private double totalSwitchOutAmount;
    private double totalRedemptionAmount;
    private double totalUnits;
    private double currentCostOfInvestment;
    private double purchaseNav;
    private double dividendReinvestment;
    private double dividendPaid;
    private double latestNav;
    private Date latestNavDate;
    private double totalCurrentValue;
    private double unrealisedProfitLoss;
    private double realisedProfitLoss;
    private double realisedGainLoss;
    private double cagr;
    private double total_cagr;
    private double absolute_return;
    private double total_absolute_return;
    private Integer average_days;

    private Boolean isSipScheme = false;
    private Boolean isActiveSipScheme = false;
    private Boolean isDividendScheme = false;
    private Boolean isDividendDeclared = false;
    private Boolean isDividendPayout = false;
    private Boolean isDividendReinvest = false;
    private Boolean isNegativeTransaction = false;
    private Boolean isDeamtAccount = false;
    private String goal_name;

    private String scheme_company;
    private String scheme_advisorkhoj_category;
    private String isin_no;
    private String scheme_amfi_code;
    private String tax_category;

    private double sip_amount;
    private String sip_amount_str;
    private Double total_sip_amount;
    private String total_sip_amount_str;
    private String sip_frequency;

    private double total_dividend_paid;
    private double total_dividend_reinvest;
    private double total_dividend;
    private double total_dividend_paid_current_fy;
    private double total_dividend_reinvest_current_fy;

    private Boolean isStpInScheme = false;
    private Boolean isStpOutScheme = false;
    private Boolean isActiveStpScheme = false;
    private double stp_amount;
    private Boolean isSwpScheme = false;
    private Boolean isActiveSwpScheme = false;
    private double swp_amount;

    private String user_id;
    private String investorName;
    private String investorPan;
    private String investor_branch;
    private String investor_rm_name;
    private String investor_subbroker_name;
    private Boolean isManualEntry = false;
    private String acc_holder_name;
    private String bank_name;
    private String bank_branch_name;

    private String invCostStartDate;
    private String invCostEndDate;
    private String currValueStartDate;
    private String currValueEndDate;
    private Integer sip_row_merged_count;

    private Double total_current_cost_start_date;
    private Double total_current_value_start_date;
    private Double total_current_cost_end_date;
    private Double total_current_value_end_date;
    private Double total_purcahse;
    private Double total_redemption;
    private Double total_div_invest;
    private Double total_div_paid;
    private Double total_gain_loss;
    private Double total_abs_return;

    private List<Date> scheme_dates_array;
    private List<Double> scheme_amount_array;

    private String sensex;
    private Double short_term_units;
    private Double short_term_amount;
    private Double long_term_units;
    private Double long_term_amount;
    private Double notional_units;
    private Double notional_amount;

    private Double prev_latestNav;
    private Double day_change_value;
    private Double day_change_percentage;
    private Double nav_1year_high;
    private Double nav_change_percentage_1year;
    private Double nav_inception_high;
    private Double nav_change_percentage_inception;

    private Double loadFreeUnits;

    private String joint1_name;
    private String joint1_pan;
    private String joint2_name;
    private String joint2_pan;
    private String tax_status;
    private String tax_status_code;
    private String holding_nature;
    private String nominee1_name;
    private String nominee1_relation;
    private String nominee1_percentage;
    private String nominee2_name;
    private String nominee2_relation;
    private String nominee2_percentage;
    private String nominee3_name;
    private String nominee3_relation;
    private String nominee3_percentage;

    private List<XirrResponse> xirr_list;
    private List<FolioMaster> folio_list;

    private double currentNav;
    private Date currentNavDate;

    private String scheme_pan;
    private String arn_number;

    private String scheme_benchmark_flag;
    private String benchmark_name;
    private String benchmark_code;
    private Double benchmark_currentCostOfInvestment;
    private Double benchmark_totalCurrentValue;
    private Double benchmark_absolute_return;
    private Double benchmark_cagr;

    private String totalOriginalInvestedAmount_str = "0";
    private String totalSwitchInAmount_str = "0";
    private String totalSwitchOutAmount_str = "0";
    private String totalRedemptionAmount_str = "0";
    private String currentCostOfInvestment_str = "0";
    private String totalCurrentValue_str = "0";
    private String unrealisedProfitLoss_str = "0";
    private String realisedProfitLoss_str = "0";
    private String realisedGainLoss_str = "0";
    private String total_dividend_reinvest_str = "0";
    private String total_dividend_paid_str = "0";
    private String total_dividend_str = "0";
    private String totalUnits_str = "0";
    private String purchaseNav_str = "0";
    private String latestNav_str = "0";
    private String investmentStartDate_str = "";
    private String investmentReStartDate_str = "";
    private String latestNavDate_str = "";
    private String currentNav_str = "0";
    private String long_term_units_str = "0";
    private String long_term_amount_str = "0";

    private String totalInflow_str;
    private String totalOutflow_str;

    private String invCostStartDate_str;
    private String invCostEndDate_str;
    private String currValueStartDate_str;
    private String currValueEndDate_str;

    private String benchmark_totalCurrentValue_str;

    private String scheme_bank_details;
    private String tag_name;

    private List<InvestorSchemeWiseTransactionResponse> investorSchemeWiseTransactionResponses;
    private List<List<InvestorSchemeWiseTransactionResponse>> familyInvestorSchemeWiseTransactionResponses;
    private List<SIPTransactionResponse> transaction_list;
    private List<InvestorTransactionDto> inv_transaction_list;

    private List<XirrResponse> scheme_xirr_list;

    private String bse_client_code;
    private Double monthly_amt;

    private String  monthly_amt_str;

    private String stp_amount_str;

    private String swp_amount_str;

    private Date invCostStart_Date;
    private Date invCostEnd_Date;

    private String totaldividend_paid_str = "0";
    private Boolean optionEnable = false;
}
