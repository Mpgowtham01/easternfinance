package com.user.service;

import com.user.model.MymfboxOnboarding;
import com.user.model.User;
import com.user.model.UsersOnlineRegDetails;
import com.user.repository.MymfboxOnboardingRepository;
import com.user.repository.UserOnlineRegDetailsRespository;
import com.user.repository.UserRepository;
import com.user.utils.UserUtils;
import jakarta.transaction.Transactional;
import org.hibernate.internal.util.StringHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class OnboardingService {

    @Autowired
    private MymfboxOnboardingRepository onboardingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    UserOnlineRegDetailsRespository userOnlineRegDetailsRespository;

    public MymfboxOnboarding getOrCreateOnboarding(Integer userId, String clientName)
    {
        System.out.println("userid = " + userId);
        Optional<MymfboxOnboarding> onboardingOpt = onboardingRepository.findLatestByUserIdAndClientName(userId, clientName);
        return onboardingOpt.orElse(null);
    }

    public MymfboxOnboarding getOrCreateOnboardingbyid(Integer userId, String clientName)
    {
        List<MymfboxOnboarding> onboardingOpt = onboardingRepository.findLatestByonlineregidAndClientName(userId, clientName);
        System.out.println("onboardingOpt = " + onboardingOpt);
        if(onboardingOpt == null || onboardingOpt.size() == 0)
        {
            return null;
        }else{
            return onboardingOpt.get(0);
        }
    }

    public MymfboxOnboarding getOrCreateOnboardingbyidAndClientName(Integer userId, String clientName)
    {
        List<MymfboxOnboarding> onboardingOpt = onboardingRepository.findLatestByonlineregidAndClient(userId, clientName);
        System.out.println("onboardingOpt = " + onboardingOpt);
        if(onboardingOpt == null || onboardingOpt.size() == 0)
        {
            return null;
        }else{
            return onboardingOpt.get(0);
        }
    }

    public MymfboxOnboarding getOrCreateOnboardingbyMultireg(Integer userId, String clientName,Boolean multireg)
    {
        List<MymfboxOnboarding> onboardingOpt = onboardingRepository.findLatestByonlineregidAndClientNameMultiTrue(userId, clientName,multireg);
        System.out.println("onboardingOpt = " + onboardingOpt);
        if(onboardingOpt == null || onboardingOpt.size() == 0)
        {
            return null;
        }else{
            return onboardingOpt.get(0);
        }
    }

    public MymfboxOnboarding getOrCreateOnboardingbyidMulti(Integer userId, String clientName)
    {
        List<MymfboxOnboarding> onboardingOpt = onboardingRepository.findLatestByonlineregidAndClientNameMulti(userId, clientName);
        System.out.println("onboardingOpt = " + onboardingOpt);
        if(onboardingOpt == null || onboardingOpt.size() == 0)
        {
            return null;
        }else{
            return onboardingOpt.get(0);
        }
    }

    public List<MymfboxOnboarding> getOrCreateOnboardingbyidCheck(Integer userId, String clientName)
    {
        List<MymfboxOnboarding> onboardingOpt = onboardingRepository.findLatestByonlineregidAndClientNameCheck(userId, clientName);
        System.out.println("onboardingOpt = " + onboardingOpt);
        if(onboardingOpt == null || onboardingOpt.size() == 0)
        {
            return null;
        }else{
            return onboardingOpt;
        }
    }

    @Transactional
    public void deleteExistingOnboardings(Integer userId, String clientName) {

        int deletedCount =
                onboardingRepository.deleteIncompleteOnboardings(userId, clientName);

        System.out.println("Deleted onboarding rows: " + deletedCount);
    }



    public MymfboxOnboarding saveOnboarding(MymfboxOnboarding onboarding)
    {
        return onboardingRepository.save(onboarding);
    }

    public MymfboxOnboarding getOnboardingByUserId(Integer userId, String client_name)
    {
        MymfboxOnboarding onboard = null;
        try
        {
            List<MymfboxOnboarding> list = onboardingRepository.findMyMfboxList(userId,client_name);
            if(list != null && !list.isEmpty())
            {
                onboard = list.get(0);
            }

            if(onboard == null)
            {
                List<UsersOnlineRegDetails> userlist = userOnlineRegDetailsRespository.findInactiveNseByUserIdAndClientname1(userId,client_name);

                UsersOnlineRegDetails user = null;

                if(userlist != null && userlist.size() > 0)
                {
                    user = userlist.get(0);
                }

                if(user != null)
                {
                    MymfboxOnboarding onboarding = new MymfboxOnboarding();
                    onboarding.setUser_id(user.getId());
                    onboarding.setStatus(0);
                    onboarding.setClient_name(client_name);
                    onboarding.setVendor("");
                    onboarding.setTax_status(user.getTax_status_code());
                    onboarding.setHolding_nature(user.getHolding_nature_code());
                    onboarding.setInv_category("");
                    onboarding.setInvestor_info(false);
                    onboarding.setPersonal_info(false);
                    onboarding.setContact_info(false);
                    onboarding.setNri_info(false);
                    onboarding.setJoint_holder_info(false);
                    onboarding.setNomiee_info(false);
                    onboarding.setBank_info(false);
                    onboarding.setSignature_info(false);
                    onboarding.setHas_nominee(false);
                    onboarding.setHas_nri(false);
                    onboarding.setHas_joint_holder(false);
                    onboarding.setIs_all_steps_completed(false);
                    onboarding.setIs_registration_completed(false);
                    onboarding.setIs_multiple_registration(false);
                    onboarding.setNse_already_reg_diff_arn(false);

                    onboard = onboarding;
                }
            }else
            {
                String tax_status = onboard.getTax_status();
                String holding_nature = onboard.getHolding_nature();
                String inv_category = onboard.getInv_category();

                List<UsersOnlineRegDetails> userlist = userOnlineRegDetailsRespository.findInactiveNseByUserIdAndClientname1(userId,client_name);

                UsersOnlineRegDetails user = null;

                if(userlist != null && userlist.size() > 0)
                {
                    user = userlist.get(0);
                }

                if(user != null)
                {
                    if(StringHelper.isEmpty(tax_status))
                    {
                        onboard.setTax_status(user.getTax_status_code());
                    }

                    if(StringHelper.isEmpty(holding_nature))
                    {
                        onboard.setHolding_nature(user.getHolding_nature_code());
                    }

                    if(StringHelper.isEmpty(inv_category))
                    {
                        onboard.setInv_category("");
                    }
                }
            }
        }
        catch(Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date()); ex.printStackTrace();
        }
        return onboard;
    }
}
