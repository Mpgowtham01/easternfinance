package com.user.controller;

import com.user.service.LogsApiService;
import io.swagger.v3.oas.annotations.Hidden;
import org.hibernate.internal.util.StringHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Hidden
@RestController
public class LogController
{
    @Autowired
    LogsApiService logsApiService;

    @PostMapping("/saveApiLog")
    public void saveApiLog(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String api_request_url,
            @RequestParam(required = false) String api_request_body,
            @RequestParam(required = false) Integer api_status,
            @RequestParam(required = false) String api_failure_message,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String source)
    {
        try
        {
            if (StringHelper.isNotEmpty(api_request_body))
            {
                api_request_body = URLDecoder.decode(api_request_body, StandardCharsets.UTF_8);
            }

            if (StringHelper.isNotEmpty(api_failure_message))
            {
                api_failure_message = URLDecoder.decode(api_failure_message, StandardCharsets.UTF_8);
            }

            if (StringHelper.isNotEmpty(source))
            {
                source = URLDecoder.decode(source, StandardCharsets.UTF_8);
            }

            logsApiService.saveApiLog(token, api_request_url, api_request_body, api_status, api_failure_message, ip, source);

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
