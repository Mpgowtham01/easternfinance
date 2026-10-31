package com.user.repository;

import com.user.model.LogsApi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogsApiRepository extends JpaRepository<LogsApi, Integer>
{

}
