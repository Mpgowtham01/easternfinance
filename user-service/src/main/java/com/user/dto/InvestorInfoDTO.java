package com.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Investor Information DTO")
public class InvestorInfoDTO {

    @Schema(description = "PAN number of the investor", example = "ABCDE1234F")
    private String pan = "";

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
}

