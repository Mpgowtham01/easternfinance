package com.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AuthUtils
{
    public static String checkParem(String param)
    {
        if (param == null || param.trim().equalsIgnoreCase("null") || param.trim().equalsIgnoreCase("undefined"))
        {
            return "";
        }

        return param.trim();
    }

    public static ResponseEntity<ErrorResponse> errorResponse(String returnMsg, HttpStatus statusCode) {
        ErrorResponse response = new ErrorResponse(statusCode.value(), statusCode.getReasonPhrase(), returnMsg);
        return ResponseEntity.status(statusCode).body(response);
    }

    public static ResponseEntity<SuccessResponse> successResponse(String token, String clientName, Long expiresIn, HttpStatus statusCode, String refreshToken, Long refreshTokenExpiresIn)
    {
        SuccessResponse response = new SuccessResponse(statusCode.value(), statusCode.getReasonPhrase(), token, "Bearer", refreshToken, clientName, expiresIn, refreshTokenExpiresIn);
        return ResponseEntity.ok(response);
    }
}
