package com.user.service;

import com.user.model.MymfboxOnboarding;
import com.user.model.User;
import com.user.repository.MymfboxOnboardingRepository;
import com.user.repository.UserRepository;
import com.user.utils.UserUtils;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OnboardingService {

    @Autowired
    private MymfboxOnboardingRepository onboardingRepository;

    @Autowired
    private UserRepository userRepository;

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
}
