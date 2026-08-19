package com.nse.services;

import com.nse.dto.mf.UserDto;
import com.nse.model.NseLogModel;
import com.nse.repository.NseLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class NseLogService
{
    @Autowired
    NseLogRepository nseLogRepository;

    public void saveLog(String title, String description, String logMsg, String ipAddr, String source, String clientName, UserDto user)
    {
        NseLogModel log = new NseLogModel();
        log.setUserid(user.getId());
        log.setUsername(user.getFirst_name());
        log.setMobile(user.getMobile());
        log.setTitle(title);
        log.setDescription(description);
        log.setContent(logMsg.toString());
        log.setLogtime(new Date());
        log.setIp(ipAddr);
        log.setSource(source.equalsIgnoreCase("Mobile") ? "Mobile App" : "Website");
        log.setClientName(clientName);
        nseLogRepository.save(log);
    }
}
