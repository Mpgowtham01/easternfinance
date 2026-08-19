package com.nse.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Details of a bank fetched using IFSC code from Razorpay IFSC API")
public class BankDetails {

    @JsonProperty("BANK")
    @Schema(description = "Name of the bank", example = "HDFC Bank")
    private String bank;

    @JsonProperty("IFSC")
    @Schema(description = "IFSC code of the bank branch", example = "HDFC0002850")
    private String ifsc;

    @JsonProperty("BRANCH")
    @Schema(description = "Branch name", example = "JAYNAGAR THREE, BANGALORE")
    private String branch;

    @JsonProperty("ADDRESS")
    @Schema(description = "Branch address", example = "HDFC BANK LTD. NO. 48/13, 3RD MAIN, 40TH CROSS, 8TH BLOCK JAYANAGAR, BANGALORE")
    private String address;

    @JsonProperty("CONTACT")
    @Schema(description = "Branch contact number", example = "+919945863333")
    private String contact;

    @JsonProperty("CITY")
    @Schema(description = "City of the branch", example = "BANGALORE URBAN")
    private String city;

    @JsonProperty("DISTRICT")
    @Schema(description = "District of the branch", example = "BANGALORE")
    private String district;

    @JsonProperty("STATE")
    @Schema(description = "State where the branch is located", example = "KARNATAKA")
    private String state;

    @JsonProperty("MICR")
    @Schema(description = "MICR code", example = "560240089")
    private String micr;

    @JsonProperty("SWIFT")
    @Schema(description = "SWIFT code", example = "HDFCINBB")
    private String swift;

    @JsonProperty("BANKCODE")
    @Schema(description = "Bank code", example = "HDFC")
    private String bankCode;

    @JsonProperty("NEFT")
    @Schema(description = "NEFT support", example = "true")
    private Boolean neft;

    @JsonProperty("IMPS")
    @Schema(description = "IMPS support", example = "true")
    private Boolean imps;

    @JsonProperty("RTGS")
    @Schema(description = "RTGS support", example = "true")
    private Boolean rtgs;

    @JsonProperty("UPI")
    @Schema(description = "UPI support", example = "true")
    private Boolean upi;

    @JsonProperty("CENTRE")
    @Schema(description = "Centre name", example = "BANGALORE")
    private String centre;

    @JsonProperty("ISO3166")
    @Schema(description = "ISO 3166 State code", example = "IN-KA")
    private String iso3166;

    // Getters and setters for all fields
}

