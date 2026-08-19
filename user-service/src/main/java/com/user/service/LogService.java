package com.user.service;

import com.user.model.Log;
import com.user.repository.LogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class LogService
{
    @Autowired
    private LogRepository logRepository;

    public void saveLog(String client_name, Integer userid, String username, String mobile, String title, String description, String content, String ip)
    {
        Date today = new Date();
        Log log = new Log();
        log.setUserid(userid);
        log.setUsername(username);
        log.setMobile(mobile);
        log.setTitle(title);
        log.setDescription(description);
        log.setContent(content);
        log.setLogtime(today);
        log.setIp(ip);
        log.setClient_name(client_name);
        logRepository.save(log);
    }
}
