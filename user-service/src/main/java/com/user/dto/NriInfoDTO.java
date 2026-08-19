package com.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "NRI Address Information DTO")
public class NriInfoDTO
{
    @Schema(description = "Address line 1", example = "123, Palm Street")
    private String address1 = "";

    @Schema(description = "Address line 2", example = "Near Marina Bay")
    private String address2 = "";

    @Schema(description = "Address line 3", example = "Apartment 45B")
    private String address3 = "";

    @Schema(description = "City of residence", example = "Dubai")
    private String city = "";

    @Schema(description = "State or province", example = "Dubai")
    private String state = "";

    @Schema(description = "Postal code", example = "00000")
    private String pincode = "";

    @Schema(description = "Country of residence", example = "United Arab Emirates")
    private String country = "";
}
