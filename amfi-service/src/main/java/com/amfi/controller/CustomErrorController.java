package com.amfi.controller;

import com.amfi.response.StatusMessage;
import com.amfi.utils.AmfiUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
public class CustomErrorController implements ErrorController
{

    private final ErrorAttributes errorAttributes;

    public CustomErrorController(ErrorAttributes errorAttributes)
    {
        this.errorAttributes = errorAttributes;
    }

    @RequestMapping("/error")
    public ResponseEntity<?> handleError(HttpServletRequest request)
    {
        try
        {
            ServletWebRequest webRequest = new ServletWebRequest(request);

            Map<String, Object> errorDetails = errorAttributes.getErrorAttributes(webRequest, ErrorAttributeOptions.of(ErrorAttributeOptions.Include.MESSAGE, ErrorAttributeOptions.Include.BINDING_ERRORS));

            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", errorDetails.getOrDefault("message", "API Not Found, Please check the URL"));
            response.put("path", errorDetails.getOrDefault("path", request.getRequestURI()));
            response.put("error", errorDetails.getOrDefault("error", ""));
            response.put("timestamp", errorDetails.getOrDefault("timestamp", ""));
            response.put("statusCode", errorDetails.getOrDefault("status", 404));

            HttpStatus status = HttpStatus.resolve((int) errorDetails.getOrDefault("status", 404));
            return new ResponseEntity<>(response, status != null ? status : HttpStatus.NOT_FOUND);
        }catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return AmfiUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
