package com.nse.services;

import com.nse.model.LogException;
import com.nse.repository.LogExceptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class LogExceptionService
{
    @Autowired
    LogExceptionRepository logExceptionRepository;

    public void save(Integer user_id, String client_name, String api_request, String exception_message, String http_method, String ip_address, String source)
    {
        LogException log = new LogException();
        log.setUser_id(user_id);
        log.setClient_name(client_name);
        log.setApi_request(api_request);
        log.setException_message(exception_message);
        log.setHttp_method(http_method);
        log.setIp_address(ip_address);
        log.setSource(source == "Mobile" ? "Mobile App" : "Website");
        log.setCreated_at(new Date());
        log.setUpdated_at(new Date());
        log.setActive(true);
        logExceptionRepository.save(log);
    }
}
