package com.user.service;

import com.user.model.UsersMandateDetails;
import com.user.repository.UsersMandateDetailsRespository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserMandateDetailsService
{
    @Autowired
    private UsersMandateDetailsRespository userMandateDetailsRespository;

    public Optional<UsersMandateDetails> getNseUserMandateDetailsByUmrn(Integer userId, String onlineCode, String nseAch, String clientName)
    {
        return userMandateDetailsRespository.getNseUserMandateDetailsByUmrn(userId, onlineCode, nseAch, clientName);
    }

    public List<UsersMandateDetails> getByAllFields(Integer userId, String onlineFlag, String onlineCode, String bankAccountNumber, String clientName)
    {
        return userMandateDetailsRespository.findByAllFields(userId,onlineFlag, onlineCode, bankAccountNumber, clientName);

    }


}
