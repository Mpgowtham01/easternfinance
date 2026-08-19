package com.user.repository;

import com.user.model.MyMFBoxLogActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MyMfBoxLogRepository extends JpaRepository<MyMFBoxLogActivity, Integer> {

    // Derived query based on the 'id' field
    List<MyMFBoxLogActivity> findById(int id);
}
