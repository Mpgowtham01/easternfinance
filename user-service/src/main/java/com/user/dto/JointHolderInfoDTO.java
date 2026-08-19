package com.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Joint Holder Information DTO")
public class JointHolderInfoDTO
{
    @Schema(description = "Unique joint holder ID", example = "1")
    public Integer id = 0;

    @Schema(description = "Full name of the joint holder", example = "Jane Doe")
    private String name = "";

    @Schema(description = "PAN number of the joint holder", example = "ABCDE1234F")
    private String pan = "";

    @Schema(description = "Date of birth (YYYY-MM-DD)", example = "1985-06-15")
    private String dob = "";

    @Schema(description = "Email address of the joint holder", example = "jane.doe@example.com")
    private String email = "";

    @Schema(description = "Relation code for the email", example = "01")
    private String emailRelation = "";

    @Schema(description = "Mobile number of the joint holder", example = "9876543210")
    private String mobile = "";

    @Schema(description = "Relation code for the mobile number", example = "01")
    private String mobileRelation = "";

    @Schema(description = "Place of birth", example = "Mumbai")
    private String placeBirth = "";

    @Schema(description = "Country of birth", example = "India")
    private String countryBirth = "";

    @Schema(description = "Occupation of the joint holder", example = "Software Engineer")
    private String occupation = "";

    @Schema(description = "Income range", example = "5-10 LPA")
    private String income = "";

    @Schema(description = "Source of wealth", example = "Employment")
    private String sourceWealth = "";

    @Schema(description = "Type of address (e.g., Residential, Office)", example = "Residential")
    private String addressType = "";

    @Schema(description = "Political exposure status", example = "Not Politically Exposed")
    private String political = "";
}
