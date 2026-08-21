package com.nse.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Transaction status for a specific scheme")
public class PurchaseTransactionResponse {

    @Schema(description = "HTTP status code", example = "200")
    private Integer status;

    @Schema(description = "Status message", example = "SUCCESS")
    private String status_msg;

    @Schema(description = "NSE return message", example = "All transactions processed")
    private String message;

    @Schema(description = "Map of scheme name with its transaction status", example = "{ \"HDFC Overnight Fund - Growth\": \"Successfully Triggered\", \"ICICI Bluechip Fund - IDCW\": \"Failed\" }")
    private Map<String, String> transaction_status;

    @Schema(description = "NSE return message", example = "Purchase Transaction order Id's")
    Set<String> orderIdList;
}

