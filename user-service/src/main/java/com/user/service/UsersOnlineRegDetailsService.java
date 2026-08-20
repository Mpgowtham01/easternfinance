package com.user.service;

import com.user.model.UsersBankDetails;
import com.user.model.UsersNomineeDetails;
import com.user.model.UsersOnlineRegDetails;
import com.user.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsersOnlineRegDetailsService
{

    @Autowired
    UserOnlineRegDetailsRespository userOnlineRegDetailsRepository;

    @Autowired
    UsersBankDetailsRepository userBankDetailsRepository;

    @Autowired
    UsersNomineeDetailsRepository   userNomineeDetailsRepository;

    public UsersOnlineRegDetails saveOrUpdateUserOnlineReg(UsersOnlineRegDetails user)
    {
        System.out.println("saveOrUpdateUserOnlineRegd = " + user);
        return userOnlineRegDetailsRepository.save(user);
    }

//    public UsersOnlineRegDetails saveOrUpdatesUserOnlineReg(UsersOnlineRegDetails user) {
//
//        Optional<UsersOnlineRegDetails> existingOpt =
//                userOnlineRegDetailsRepository
//                        .getOptionalBseDetailsByClientCodeAndOnlineFlag(
//                                user.getUser_id(),
//                                user.getClient_name(),
//                                user.getBse_client_code(),
//                                user.getBroker_code(),"BSE"
//                        );
//
//        if (existingOpt.isPresent())
//        {
//            UsersOnlineRegDetails existing = existingOpt.get();
//            user.setId(existing.getId());
//            user.setCreated_date(existing.getCreated_date());
//
//            return userOnlineRegDetailsRepository.save(user);
//        } else
//        {
//            return userOnlineRegDetailsRepository.save(user);
//        }
//    }
//
//    public UsersOnlineRegDetails saveOrUpdateUserDetails(UsersOnlineRegDetails userDetails) {
//
//        List<UsersOnlineRegDetails> existingUsers =
//                userOnlineRegDetailsRepository.findByUserIdAndClientName(userDetails.getUser_id(), userDetails.getClient_name());
//
//        if (!existingUsers.isEmpty()) {
//            for (UsersOnlineRegDetails existingUser : existingUsers) {
//                existingUser.setName(userDetails.getName());
//                existingUser.setPan(userDetails.getPan());
//                existingUser.setEmail(userDetails.getEmail());
//                existingUser.setMobile(userDetails.getMobile());
//                existingUser.setBroker_code(userDetails.getBroker_code());
//                existingUser.setTax_status_code(userDetails.getTax_status_code());
//                existingUser.setTax_status(userDetails.getTax_status());
//                existingUser.setHolding_nature_code(userDetails.getHolding_nature_code());
//                existingUser.setHolding_nature(userDetails.getHolding_nature());
//                existingUser.setInv_category(userDetails.getInv_category());
//                userOnlineRegDetailsRepository.save(existingUser);
//            }
//            return existingUsers.get(0);
//        } else {
//            return userOnlineRegDetailsRepository.save(userDetails);
//        }
//
//    }
//
//    public UsersBankDetails saveOrUpdateUserBank(UsersBankDetails user)
//    {
//        return userBankDetailsRepository.save(user);
//    }
//
//    public UsersNomineeDetails saveOrUpdateUserNominee(UsersNomineeDetails user)
//    {
//        return userNomineeDetailsRepository.save(user);
//    }
//
//
//    public List<String> getAllIINNumbersByClientNameAndUserId(String clientName, String userId) {
//        return userOnlineRegDetailsRepository.getAllIINNumbersByClientNameAndUserId(clientName, userId);
//    }
//
//    public UsersOnlineRegDetails getBSEClientCodeRegDetails(String bse_client_code, String broker_code, String client_name)
//    {
//        List<UsersOnlineRegDetails> list = null;
//        UsersOnlineRegDetails user = null;
//
//        try
//        {
//            list = userOnlineRegDetailsRepository.findActiveBseUsersByClientAndBroker(bse_client_code, broker_code, client_name);
//
//            if(list != null && list.size() > 0)
//            {
//                user = list.get(0);
//            }
//        }
//        catch(Exception ex)
//        {
//            ex.printStackTrace();
//        }
//        return user;
//    }
}
