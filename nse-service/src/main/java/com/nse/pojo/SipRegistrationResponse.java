package com.nse.pojo;

import lombok.Data;

@Data
public class SipRegistrationResponse
{
    private String activeStatus;
    private String clientCode;
    private String schemeName;
    private String schemeCode;
    private String folioNo;
    private String amount;
    private String startDate;
    private String endDate;
    private String registrationNo;
    private String nseMandateId;
    private String uniqueId;
    private String transactionDate;
    private String frequencyType;
    private String euin;

    private String fromSchemeName;
    private String toSchemeName;
    private String transferUnits;
    private String transferAmount;

    private String toSchemeCode;
    private String fromSchemeCode;
}
