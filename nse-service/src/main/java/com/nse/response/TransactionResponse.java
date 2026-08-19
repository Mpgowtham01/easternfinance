package com.nse.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Transaction status for a specific scheme")
public class TransactionResponse {

    @Schema(description = "HTTP status code", example = "200")
    private Integer status;

    @Schema(description = "Status message", example = "SUCCESS")
    private String status_msg;

    @Schema(description = "NSE return message", example = "All transactions processed")
    private String return_msg;

    @Schema(description = "Map of scheme name with its transaction status", example = "{ \"HDFC Overnight Fund - Growth\": \"Successfully Triggered\", \"ICICI Bluechip Fund - IDCW\": \"Failed\" }")
    private Map<String, String> transaction_status;
}

