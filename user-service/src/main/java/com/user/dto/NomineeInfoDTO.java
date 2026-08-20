package com.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Nominee Information DTO")
public class NomineeInfoDTO
{
    @Schema(description = "Unique nominee ID", example = "1")
    public Integer id = 0;

    @Schema(description = "Number Of Nominee", example = "1")
    public String number_of_nominee = "";

    @Schema(description = "Nominee type code", example = "01")
    public String type = "";

    @Schema(description = "Nominee type description", example = "Primary")
    public String typeDesc = "";

    @Schema(description = "Full name of the nominee", example = "Alice Doe")
    public String name = "";

    @Schema(description = "Middle name of the nominee", example = "Alice Doe")
    public String middle_name = "";

    @Schema(description = "Last name of the nominee", example = "Alice Doe")
    public String last_name = "";

    @Schema(description = "Date of birth in YYYY-MM-DD format", example = "2005-05-10")
    public String dob = "";

    @Schema(description = "Address line 1", example = "123 Garden Road")
    public String address1 = "";

    @Schema(description = "Address line 2", example = "Block A")
    public String address2 = "";

    @Schema(description = "Address line 3", example = "Near Central Park")
    public String address3 = "";

    @Schema(description = "PIN/ZIP code", example = "560001")
    public String pincode = "";

    @Schema(description = "City name", example = "Bangalore")
    public String city = "";

    @Schema(description = "State name", example = "Karnataka")
    public String state = "";

    @Schema(description = "State code", example = "KA")
    public String stateCode = "";

    @Schema(description = "Country name", example = "India")
    public String country = "";

    @Schema(description = "Identification type", example = "PAN")
    public String idType = "";

    @Schema(description = "Identification number", example = "ABCDE1234F")
    public String idNo = "";

    @Schema(description = "Email ID of the nominee", example = "alice@example.com")
    public String email = "";

    @Schema(description = "Mobile number of the nominee", example = "9876543210")
    public String mobile = "";

    @Schema(description = "Relationship with the investor", example = "Daughter")
    public String relation = "";

    @Schema(description = "Guardian's name (if nominee is a minor)", example = "John Doe")
    public String guardName = "";

    @Schema(description = "Guardian's name (if nominee is a minor)", example = "John Doe")
    public String guard_middle_name = "";

    @Schema(description = "Guardian's name (if nominee is a minor)", example = "John Doe")
    public String guard_last_name = "";

    @Schema(description = "Guardian's PAN", example = "PQRSX6789K")
    public String guardPan = "";

    @Schema(description = "Relationship of guardian with nominee", example = "Father")
    public String guardRelation = "";

    @Schema(description = "DOB of guardian with nominee", example = "DOB")
    public String guardDob = "";

    @Schema(description = "Share percentage allocated to this nominee", example = "100")
    public String percentage = "0";

    @Schema(description = "Nominee Type Description", example = "Service")
    public String nomineeTypeDesc = "0";

    @Schema(description = "Nominee Soa Description", example = "Service")
    public String nominee_soa = "";


}
