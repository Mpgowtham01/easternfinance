package com.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Success Response")
public class SuccessResponse
{
    @Schema(description = "HTTP status code with reason", example = "200")
    private Integer status;

    @Schema(description = "Detailed success message", example = "Authentication successful")
    private String message;

    @Schema(description = "Authentication Token", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMDg5IiwiaWF0IjoxNzUyMjk4ODYzLCJleHAiOjE3NTIzMDI0NjN9.oTpneUQbHDWXVmIKKlFVQLlCrgh-HYWjAC24BJ6-E2E")
    private String token;

    @Schema(description = "Authentication Token Type", example = "Bearer")
    private String tokenType;

    @Schema(description = "Once token is expired, use this token to get new access token", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMDg5IiwiaWF0IjoxNzUyMjk4ODYzLCJleHAiOjE3NTIzMDI0NjN9.oTpneUQbHDWXVmIKKlFVQLlCrgh-HYWjAC24BJ6-E2E")
    private String refreshToken;

    @Schema(description = "Authentication ClientName", example = "reachyourgoals")
    private String clientName;

    @Schema(description = "Token Expiry Time", example = "3600")
    private Long expiresIn;

    @Schema(description = "Refresh Token Expiry Time", example = "604800")
    private Long refreshTokenExpiresIn;
}
