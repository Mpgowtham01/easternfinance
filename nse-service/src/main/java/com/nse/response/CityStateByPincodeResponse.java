package com.nse.response;

import com.nse.pojo.CityStatePojo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "City / State By Pincode Response")
public class CityStateByPincodeResponse
{
    @Schema(description = "HTTP status code", example = "200")
    private Integer status;

    @Schema(description = "Short status keyword", example = "Success")
    private String status_msg;

    @Schema(description = "Detailed message", example = "Success")
    private String msg;

    @Schema(description = "City and state details for the requested pincode")
    private CityStatePojo result;
}
