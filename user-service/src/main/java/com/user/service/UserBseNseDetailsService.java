package com.user.service;

import com.user.model.UsersOnlineRegDetails;
import com.user.repository.UserOnlineRegDetailsRespository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserBseNseDetailsService
{
    @Autowired
    UserOnlineRegDetailsRespository userOnlineRegDetailsRespository;

    public UsersOnlineRegDetails saveOrUpdateUserOnlineReg(UsersOnlineRegDetails user)
    {
        return userOnlineRegDetailsRespository.save(user);
    }

    // 🔁 Save or update based on user ID
    public UsersOnlineRegDetails saveOrUpdateUsersOnlineRegDetails(UsersOnlineRegDetails user) {
        return userOnlineRegDetailsRespository.save(user);
    }


    public Optional<UsersOnlineRegDetails> getUsersOnlineRegDetailsByAllFields(
            Integer userId,
            String clientName,
            String taxStatusCode,
            String holdingNatureCode,
            String jointHolderPan1,
            String jointHolderPan2
    ) {
        return userOnlineRegDetailsRespository.getUserBseNseDetailsByAllFields(
                userId, clientName, taxStatusCode, holdingNatureCode, jointHolderPan1, jointHolderPan2
        );
    }

    public List<UsersOnlineRegDetails> getUserBseNseDetailsByUserIdAndClientname(Integer userId, String clientName)
    {
        return userOnlineRegDetailsRespository.findInactiveNseByUserIdAndClientname1(userId, clientName );
    }

    public List<UsersOnlineRegDetails> findByUserIdAndBseClientCodeAndClientName(Integer userId, String bseClientCode,String clientName)
    {
        return userOnlineRegDetailsRespository.findByUserIdAndBseClientCodeAndClientName(userId, bseClientCode,clientName );
    }

    public List<UsersOnlineRegDetails> findByUserIdAndBseClientCodeAndBrokerCodeAndClientName(Integer userId, String bseClientCode,String brokerCode,String clientName)
    {
        return userOnlineRegDetailsRespository.findByUserIdAndBseClientCodeAndBrokerCodeAndClientName(userId, bseClientCode,brokerCode,clientName );
    }

    public List<UsersOnlineRegDetails> findActiveBseByBseClientCodeAndClientName(String bseClientCode,String clientName)
    {
        return userOnlineRegDetailsRespository.findActiveBseByBseClientCodeAndClientName(bseClientCode,clientName );
    }
    public Optional<UsersOnlineRegDetails> getUserRegDetailsByOnlineId(Integer online_id, String client_name)
    {
        return userOnlineRegDetailsRespository.getUserRegDetailsByOnlineId(online_id, client_name);
    }

}
