package com.nse.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "City and state details resolved from a pincode")
public class CityStatePojo
{
    @Schema(description = "City name", example = "Perungalur")
    private String city;

    @Schema(description = "State name", example = "Tamil Nadu")
    private String state;

    @Schema(description = "NSE state code", example = "TN")
    private String state_code;
}
