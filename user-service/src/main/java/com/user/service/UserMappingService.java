package com.user.service;

import com.user.model.UsersMapping;
import com.user.repository.UsersMappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserMappingService
{

    @Autowired
    UsersMappingRepository usersMappingRepository;

    public List<Integer> getFamilyMembersCount(String clientName)
    {
        return usersMappingRepository.findDistinctInvestorIdsByClientName(clientName);
    }

    public List<UsersMapping> findFilteredUsersByClientName(String clientName)
    {
        return usersMappingRepository.findFilteredUsersByClientName(clientName);
    }

    public List<UsersMapping> getByUserIdAndClientName(Integer userid,String clientName)
    {
        return usersMappingRepository.findByUserIdAndClientName(userid,clientName);
    }

}
