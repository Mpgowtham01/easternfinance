package com.user.service;

import com.user.dto.UserDto;
import com.user.model.User;
import com.user.model.UsersOnlineRegDetails;
import com.user.repository.UserOnlineRegDetailsRespository;
import com.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.List;

@Service
public class UserService {

    @Autowired
    UserOnlineRegDetailsRespository userOnlineRegDetailsRespository;

    // 🔁 Save or update based on user ID
    public UsersOnlineRegDetails saveOrUpdateUser(UsersOnlineRegDetails user)
    {
        return userOnlineRegDetailsRespository.save(user);
    }



    public Optional<UsersOnlineRegDetails> getUserById(Integer id) {
        return userOnlineRegDetailsRespository.findById(id);
    }

    public List<UsersOnlineRegDetails> getAllUsers() {
        return userOnlineRegDetailsRespository.findAll();
    }

    public void deleteUserById(Integer id) {
        userOnlineRegDetailsRespository.deleteById(id);
    }
}

