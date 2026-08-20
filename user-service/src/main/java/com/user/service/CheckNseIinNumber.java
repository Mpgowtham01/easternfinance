package com.user.service;

import com.user.model.UsersOnlineRegDetails;
import com.user.repository.UserOnlineRegDetailsRespository;
import com.user.repository.UserRepository;
import com.user.utils.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class CheckNseIinNumber {

    private final UserRepository userRepository;

    @Autowired
    UserOnlineRegDetailsRespository userOnlineRegDetailsRespository;

    public class UniqueIDProvider {

        private static final String DIGITS = "0123456789";
        private static final SecureRandom random = new SecureRandom();

        public static String generateUniquePin(int length) {
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                sb.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
            }
            return sb.toString();
        }
    }

    public String CheckNseIinNumbers(String clientName) {
        String nseIinNumberNew = "";

        try {
            for (int k = 1; k <= 10; k++) {
                String uniqueID = UniqueIDProvider.generateUniquePin(6);

                if (clientName.length() >= 4) {
                    nseIinNumberNew = clientName.substring(0, 4) + uniqueID;
                } else {
                    nseIinNumberNew = clientName + uniqueID;
                }

                boolean exists = userOnlineRegDetailsRespository.findByClientNameAndNseIinNumber(clientName, nseIinNumberNew).isPresent();

                if (!exists) {
                    break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return nseIinNumberNew;
    }

    public String checkNseMultipleRegistrationIinNumbers(String clientName, String arnCode)
    {
        String nseIinNumberNew = "";

        try
        {
            for (int k = 1; k <= 10; k++)
            {
                nseIinNumberNew = UserUtils.generateNseIinNumber(arnCode);

                boolean exists = userOnlineRegDetailsRespository.findByClientNameAndNseIinNumber(clientName, nseIinNumberNew).isPresent();

                if (!exists) {
                    break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return nseIinNumberNew;
    }

    public UsersOnlineRegDetails CheckNewIinNumber(String clientName, String iin_number, String arn_number) {
        UsersOnlineRegDetails exists = null;
        try {
            exists = userOnlineRegDetailsRespository
                    .findByClientNameAndNseIinNumberAndActive(clientName, iin_number, arn_number);


        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return exists;
    }

    public boolean CheckNewIinNumbers(String clientName,String iin_number) {
        boolean exists = false;
        try {
            exists = userOnlineRegDetailsRespository
                    .findByClientNameAndNseIinNumber(clientName, iin_number)
                    .isPresent();


        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return exists;
    }
}
