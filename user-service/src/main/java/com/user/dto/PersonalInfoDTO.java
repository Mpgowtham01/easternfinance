package com.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Personal Information DTO")
public class PersonalInfoDTO
{
    @Schema(description = "Name of the investor", example = "John Doe")
    private String name = "";

    @Schema(description = "Father's name of the investor", example = "Robert Doe")
    private String fatherName = "";

    @Schema(description = "PAN number of the investor", example = "ABCDE1234F")
    private String pan = "";

    @Schema(description = "Date of birth in YYYY-MM-DD format", example = "1990-01-01")
    private String dob = "";

    @Schema(description = "Gender of the investor", example = "Male")
    private String gender = "";

    @Schema(description = "Email address", example = "john.doe@example.com")
    private String email = "";

    @Schema(description = "Code representing relationship of the email", example = "01")
    private String emailRelationCode = "";

    @Schema(description = "Mobile number", example = "9876543210")
    private String mobile = "";

    @Schema(description = "Code representing relationship of the mobile number", example = "01")
    private String mobileRelationCode = "";

    @Schema(description = "Broker code assigned to the investor", example = "ARN-77441")
    private String brokerCode = "";

    @Schema(description = "Unique investor code", example = "INV1001")
    private String investorCode = "";

    @Schema(description = "Tax status code", example = "01")
    private String taxStatusCode = "";

    @Schema(description = "Tax status description", example = "Individual")
    private String taxStatusDesc = "";

    @Schema(description = "Holding nature code", example = "01")
    private String holdingNatureCode = "";

    @Schema(description = "Holding nature description", example = "Single")
    private String holdingNatureDesc = "";

    @Schema(description = "Place of birth", example = "Chennai")
    private String placeBirth = "";

    @Schema(description = "Country of birth description", example = "India")
    private String countryBirthDesc = "";

    @Schema(description = "Country of birth code", example = "IN")
    private String countryBirthCode = "";

    @Schema(description = "Occupation description", example = "Engineer")
    private String occupationDesc = "";

    @Schema(description = "Occupation code", example = "O1")
    private String occupationCode = "";

    @Schema(description = "Income description", example = "5-10 LPA")
    private String incomeDesc = "";

    @Schema(description = "Income code", example = "03")
    private String incomeCode = "";

    @Schema(description = "Source of wealth description", example = "Salary")
    private String sourceWealthDesc = "";

    @Schema(description = "Source of wealth code", example = "01")
    private String sourceWealthCode = "";

    @Schema(description = "Political status description", example = "Not Politically Exposed")
    private String politicalStatusDesc = "";

    @Schema(description = "Political status code", example = "N")
    private String politicalStatusCode = "";

    @Schema(description = "Guardian's name (if applicable)", example = "Jane Doe")
    private String guardName = "";

    @Schema(description = "Guardian's PAN", example = "PQRSX6789K")
    private String guardPan = "";

    @Schema(description = "Guardian's date of birth", example = "1970-05-10")
    private String guardDob = "";

    @Schema(description = "Guardian relationship to investor", example = "Mother")
    private String guardRelation = "";

    @Schema(description = "Guardian account relation", example = "Joint Holder")
    private String guardAccountRelation = "";

    @Schema(description = "Address type code", example = "01")
    private String addressTypeCode = "";

    @Schema(description = "Address type description", example = "Permanent")
    private String addressTypeDesc = "";

    @Schema(description = "Net worth date (as of)", example = "2023-03-31")
    private String networthDob = "";

    @Schema(description = "Net worth amount", example = "1500000")
    private String networthAmount = "";

    @Schema(description = "Data source", example = "Self-declared")
    private String source = "";

    @Schema(description = "Flag indicating multiple accounts (1 = Yes, 0 = No)", example = "1")
    private Integer multiple = 0;
}
