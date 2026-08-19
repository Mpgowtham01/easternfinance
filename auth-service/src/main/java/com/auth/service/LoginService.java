package com.auth.service;

import com.auth.model.PasswordHelper;
import com.auth.model.User;
import com.auth.repository.UserRepository;
import org.hibernate.internal.util.StringHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LoginService
{
    @Autowired
    UserRepository userRepository;

    public User validateLogin(String mobile, String password, String client_name)
    {
        User users = null;
        List<User> list = null;
        try
        {
            boolean isMobile = mobile.matches("\\d+");

            if (isMobile)
            {
                list = userRepository.findByMobile(mobile);
            } else
            {
                list = userRepository.findByPanIgnoreCase(mobile);
            }

            if (StringHelper.isNotEmpty(client_name))
            {
                list = list.stream().filter(u -> u.getClient_name().equalsIgnoreCase(client_name)).collect(Collectors.toList());
            }

            if(!list.isEmpty())
            {
                if(list.size() > 1)
                {
                    for (User users2 : list)
                    {
                        if(!users2.getUser_password().isEmpty() && PasswordHelper.checkPassword(password, users2.getUser_password()))
                        {
                            users = users2;
                            break;
                        }
                    }
                }else
                {
                    User users3 = list.get(0);

                    if(!users3.getUser_password().isEmpty() && PasswordHelper.checkPassword(password, users3.getUser_password()))
                    {
                        users = users3;
                    }
                }
            }
        }
        catch(Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date());
            ex.printStackTrace();
        }
        return users;
    }
}
