package com.user.dao;

import com.user.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class UserDAO
{
    @PersistenceContext
    private EntityManager session;

    public List<User> getUserListByDynamicQuery(String dynamicQuery)
    {
        Query query = null;
        List<User> userList = null;
        try
        {
            query = session.createQuery(dynamicQuery);
            userList = query.getResultList();

        }catch (Exception ex)
        {
            ex.printStackTrace();
            throw new RuntimeException("Error while getting user list by dynamic query");
        }
        return userList;
    }
}
