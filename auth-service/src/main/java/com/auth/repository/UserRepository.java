package com.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.auth.model.User;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByMobile(String mobile);

    List<User> findByPanIgnoreCase(String pan);

	List<User> findByPanIgnoreCaseOrMobile(String pan, String mobile);

    @Query(value = "SELECT * FROM users WHERE id = :id", nativeQuery = true)
    Optional<User> findByIds(Integer id);

}
