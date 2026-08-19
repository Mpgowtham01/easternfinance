package com.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Bank Account Information DTO")
public class BankInfoDTO
{
    @Schema(description = "IFSC code of the bank", example = "HDFC0001234")
    private String ifscCode = "";

    @Schema(description = "MICR code of the bank branch", example = "110240123")
    private String micrCode = "";

    @Schema(description = "Internal bank code used by the system", example = "HDFC")
    private String bankCode = "";

    @Schema(description = "Name of the bank", example = "HDFC Bank")
    private String bankName = "";

    @Schema(description = "Full address of the bank branch", example = "HDFC Towers, MG Road, Bengaluru")
    private String bankAddress = "";

    @Schema(description = "Branch name", example = "MG Road Branch")
    private String branchName = "";

    @Schema(description = "Bank account number", example = "123456789012")
    private String accountNumber = "";

    @Schema(description = "Name of the bank account holder", example = "John Doe")
    private String accountHolderName = "";

    @Schema(description = "Type of bank account", example = "Savings")
    private String accountType = "";

    @Schema(description = "Description of the account type", example = "Savings Bank Account")
    private String accountDesc = "";
}
