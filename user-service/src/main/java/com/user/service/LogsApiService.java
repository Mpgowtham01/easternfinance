package com.user.service;

import com.user.config.TokenInterceptor;
import com.user.model.LogsApi;
import com.user.repository.LogsApiRepository;
import com.user.response.CustomClaim;
import org.hibernate.internal.util.StringHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class LogsApiService
{
    @Autowired
    LogsApiRepository logsApiRepository;

    @Value("${jwt.secret-key}")
    private String secretKey;

    public void saveApiLog(String token, String api_request, String api_request_body, Integer api_response_status, String api_failure_message, String ip, String source)
    {
        CustomClaim customClaim = TokenInterceptor.getClaimsFromToken(token, secretKey);

        if (customClaim == null)
        {
            return;
        }

        String client_name = customClaim.getClient_name();
        String origin_user_id = customClaim.getUser_id();
        String user_id = customClaim.getInvestor_id();
        String user_type_id = customClaim.getType_id();

        LogsApi logsApi = new LogsApi();

        if (StringHelper.isNotEmpty(user_type_id) && !user_type_id.equalsIgnoreCase("1") && !user_type_id.equalsIgnoreCase("3") && StringHelper.isNotEmpty(user_id))
        {
            logsApi.setOrigin_user_id(toInt(origin_user_id));
            logsApi.setUser_id(toInt(user_id));
            logsApi.setUser_type_id(toInt(user_type_id));
        }
        else
        {
            logsApi.setOrigin_user_id(toInt(user_id));
            logsApi.setUser_id(toInt(user_id));
            logsApi.setUser_type_id(toInt(user_type_id));
        }

        logsApi.setClient_name(client_name);
        logsApi.setApi_request(api_request);
        logsApi.setApi_request_body(api_request_body);
        logsApi.setApi_response_status(api_response_status);
        logsApi.setApi_failure_message(api_failure_message);
        logsApi.setApi_request_time(new Date());
        logsApi.setIp(ip);
        logsApi.setSource(source);
        logsApiRepository.save(logsApi);
    }

    private Integer toInt(String value)
    {
        try
        {
            return (value == null || value.trim().isEmpty()) ? null : Integer.valueOf(value.trim());
        }
        catch (NumberFormatException ex)
        {
            return null;
        }
    }
}
