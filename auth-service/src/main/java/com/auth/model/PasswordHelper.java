package com.auth.model;

import org.springframework.security.crypto.bcrypt.BCrypt;

public class PasswordHelper
{
    public static String encryptPassword(String password)
    {
        String hashed = BCrypt.hashpw(password, BCrypt.gensalt(9));
        return hashed;
    }

    public static boolean checkPassword(String password,String hashed)
    {
        return BCrypt.checkpw(password, hashed);
    }
}
