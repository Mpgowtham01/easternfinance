package com.nse.response;

import com.nse.dto.mf.InvestorSipCams49Dto;
import lombok.Data;

import java.util.List;

@Data
public class InvestorPortfolioResponse
{
    private List<InvestorSchemeWisePortfolioResponse> investorSchemeWisePortfolioResponses;
    private List<List<InvestorSchemeWisePortfolioResponse>> tagged_scheme_list;
    private List<InvestorSipCams49Dto> stp_list;

    private Integer id;
    private Integer type_id;
    private String investorName;
    private String investorPan;
    private String family_status;
    private String client_name;
    private String mobile;
    private String email;
    private String address1;
    private String address2;
    private String address3;
    private String city;
    private String pincode;
    private String state;
    private String phone_office;
    private String phone_residence;
    private String date_of_birth;
    private String guardian_name;
    private String guardian_pan;
    private String broker_code;
    private String occupation;
    private Integer bse_active;
    private String bse_client_code;
    private Integer nse_active;
    private String nse_iin_number;
    private Integer portfolio_score;

    private String bank_name;
    private String bank_branch;
    private String acc_type;
    private String acc_no;
    private String bank_address1;
    private String bank_address2;
    private String bank_address3;
    private String bank_city;
    private String bank_pincode;
    private String ifsc_code;
    private String demat;

    private String nom_name;
    private String relation;
    private String nom_addr1;
    private String nom_addr2;
    private String nom_addr3;
    private String nom_city;
    private String nom_state;
    private String nom_pincod;
    private String nom_ph_off;
    private String nom_ph_res;
    private String nom_email;

    private Double totalInvestedAmount = 0.0;
    private Double totalOriginalInvestedAmount = 0.0;
    private Double totalSwitchInAmount = 0.0;
    private Double totalSwitchOutAmount = 0.0;
    private Double totalRedemptionAmount = 0.0;
    private Double totalInflow = 0.0;
    private Double totalOutflow = 0.0;
    private Double totalNetInvestment = 0.0;
    private Double totalCurrentcost = 0.0;
    private Double totalCurrentValue = 0.0;
    private Double totalUnReliasedGain = 0.0;
    private Double totalReliasedGain = 0.0;
    private Double totalReliasedGainLoss = 0.0;
    private Double totalCAGR = 0.0;
    private Double totalAbsoluteReturn = 0.0;
    private Double sipGrandTotalAmount = 0.0;

    private Double totalDividendReinvestment = 0.0;
    private Double totalDividendPaid = 0.0;
    private Double totalDividend = 0.0;
    private Double totalDividendPaidCurrentFY = 0.0;
    private Double totalDividendReinvestCurrentFY = 0.0;
    private Double totalDividendCurrentFY = 0.0;

    private Double equity_total_current_cost = 0.0;
    private Double equity_total_current_value = 0.0;
    private Double equity_total_unReliasedGain = 0.0;
    private Double equity_total_ReliasedGain = 0.0;
    private Double equity_total_Redeemed_ReliasedGain = 0.0;
    private Double equity_total_CAGR = 0.0;
    private Double equity_total_AbsoluteReturn = 0.0;
    private Double equity_total_dividendReinvestment = 0.0;
    private Double equity_total_dividendPaid = 0.0;
    private Double equity_total_dividend = 0.0;
    private Double equity_total_inflow = 0.0;
    private Double equity_total_invested_amount = 0.0;

    private Double debt_total_current_cost = 0.0;
    private Double debt_total_current_value = 0.0;
    private Double debt_total_unReliasedGain = 0.0;
    private Double debt_total_ReliasedGain = 0.0;
    private Double debt_total_Redeemed_ReliasedGain = 0.0;
    private Double debt_total_CAGR = 0.0;
    private Double debt_total_AbsoluteReturn = 0.0;
    private Double debt_total_dividendReinvestment = 0.0;
    private Double debt_total_dividendPaid = 0.0;
    private Double debt_total_dividend = 0.0;
    private Double debt_total_inflow = 0.0;
    private Double debt_total_invested_amount = 0.0;

    private Double hybrid_total_current_cost = 0.0;
    private Double hybrid_total_current_value = 0.0;
    private Double hybrid_total_unReliasedGain = 0.0;
    private Double hybrid_total_ReliasedGain = 0.0;
    private Double hybrid_total_Redeemed_ReliasedGain = 0.0;
    private Double hybrid_total_CAGR = 0.0;
    private Double hybrid_total_AbsoluteReturn = 0.0;
    private Double hybrid_total_dividendReinvestment = 0.0;
    private Double hybrid_total_dividendPaid = 0.0;
    private Double hybrid_total_dividend = 0.0;
    private Double hybrid_total_inflow = 0.0;
    private Double hybrid_total_invested_amount = 0.0;

    private Double solution_total_current_cost = 0.0;
    private Double solution_total_current_value = 0.0;
    private Double solution_total_unReliasedGain = 0.0;
    private Double solution_total_ReliasedGain = 0.0;
    private Double solution_total_Redeemed_ReliasedGain = 0.0;
    private Double solution_total_CAGR = 0.0;
    private Double solution_total_AbsoluteReturn = 0.0;
    private Double solution_total_dividendReinvestment = 0.0;
    private Double solution_total_dividendPaid = 0.0;
    private Double solution_total_dividend = 0.0;
    private Double solution_total_inflow = 0.0;
    private Double solution_total_invested_amount = 0.0;

    private Double other_total_current_cost = 0.0;
    private Double other_total_current_value = 0.0;
    private Double other_total_unReliasedGain = 0.0;
    private Double other_total_ReliasedGain = 0.0;
    private Double other_total_Redeemed_ReliasedGain = 0.0;
    private Double other_total_CAGR = 0.0;
    private Double other_total_AbsoluteReturn = 0.0;
    private Double other_total_dividendReinvestment = 0.0;
    private Double other_total_dividendPaid = 0.0;
    private Double other_total_dividend = 0.0;
    private Double other_total_inflow = 0.0;
    private Double other_total_invested_amount = 0.0;

    private String equity_total_current_cost_str = "0";
    private String equity_total_current_value_str = "0";
    private String equity_total_unReliasedGain_str = "0";
    private String equity_total_ReliasedGain_str = "0";
    private String equity_total_dividendReinvestment_str = "0";
    private String equity_total_dividendPaid_str = "0";

    private String debt_total_current_cost_str = "0";
    private String debt_total_current_value_str = "0";
    private String debt_total_unReliasedGain_str = "0";
    private String debt_total_ReliasedGain_str = "0";
    private String debt_total_dividendReinvestment_str = "0";
    private String debt_total_dividendPaid_str = "0";

    private String hybrid_total_current_cost_str = "0";
    private String hybrid_total_current_value_str = "0";
    private String hybrid_total_unReliasedGain_str = "0";
    private String hybrid_total_ReliasedGain_str = "0";
    private String hybrid_total_dividendReinvestment_str = "0";
    private String hybrid_total_dividendPaid_str = "0";

    private String solution_total_current_cost_str = "0";
    private String solution_total_current_value_str = "0";
    private String solution_total_unReliasedGain_str = "0";
    private String solution_total_ReliasedGain_str = "0";
    private String solution_total_dividendReinvestment_str = "0";
    private String solution_total_dividendPaid_str = "0";

    private String other_total_current_cost_str = "0";
    private String other_total_current_value_str = "0";
    private String other_total_unReliasedGain_str = "0";
    private String other_total_ReliasedGain_str = "0";
    private String other_total_dividendReinvestment_str = "0";
    private String other_total_dividendPaid_str = "0";

    private String salutation;
    private List<XirrResponse> cagr_list;
    private List<XirrResponse> equity_cagr_list;
    private List<XirrResponse> debt_cagr_list;
    private List<XirrResponse> hybrid_cagr_list;
    private List<XirrResponse> solution_cagr_list;
    private List<XirrResponse> other_cagr_list;
    private Double family_portfolio_return = 0.0;

    private Double portfolio_day_change_value;
    private Double portfolio_day_change_percentage;

    private String totalOriginalInvestedAmount_str = "0";
    private String totalSwitchInAmount_str = "0";
    private String totalSwitchOutAmount_str = "0";
    private String totalRedemptionAmount_str = "0";

    private String totalCurrentcost_str = "0";
    private String totalCurrentValue_str = "0";
    private String totalDividendReinvestment_str = "0";
    private String totalDividendPaid_str = "0";
    private String totalDividend_str = "0";
    private String totalUnReliasedGain_str = "0";
    private String totalReliasedGain_str = "0";
    private String totalReliasedGainLoss_str ="0";

    private String totalInvestedAmount_str = "0";
    private String sipGrandTotalAmount_str = "0";
    private String total_stp_amout_str = "0";
    private String total_swp_amout_str = "0";
    private String stpGrandTotalAmount_str = "0";
    private String swpGrandTotalAmount_str = "0";
    private List<InvestorSchemeWisePortfolioResponse> equity_list;
    private List<InvestorSchemeWisePortfolioResponse> debt_list;
    private List<InvestorSchemeWisePortfolioResponse> hybrid_list;
    private List<InvestorSchemeWisePortfolioResponse> solution_list;
    private List<InvestorSchemeWisePortfolioResponse> other_list;

    private boolean mf_oneday_change;

    private String notes;

    private String benchmarkStartNav ="0";
    private String benchmarkEndNav ="0";
}
