package com.amfi.utils;

import com.amfi.response.CommonResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class AmfiUtils
{
    public static String getIpAddr(HttpServletRequest request)
    {
        //is client behind something?
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null)
        {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    public static String checkParem(String param)
    {
        if (param == null || param.trim().equalsIgnoreCase("null") || param.trim().equalsIgnoreCase("undefined"))
        {
            return "";
        }

        return param.trim();
    }

    public static ResponseEntity<Object> commonResponse(String message, HttpStatus status)
    {
        // Use existing constructor with 3 args
        CommonResponse commonResponse = new CommonResponse(status.value(), message, "");
        return new ResponseEntity<>(commonResponse, status);
    }
}
