package com.user.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Success Response")
public class OnboardingSuccessResponse
{
    @Schema(description = "HTTP status code with reason", example = "200")
    private Integer status;

    @Schema(description = "Success Message", example = "Success")
    private String success;

    @Schema(description = "Investor Code", example = "ABCDE1234G")
    private String investor_code;

    @Schema(description = "Detailed success message", example = "Investor information saved successfully.")
    private String message;
}