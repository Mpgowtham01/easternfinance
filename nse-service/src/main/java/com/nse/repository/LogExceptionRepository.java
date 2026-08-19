package com.nse.repository;

import com.nse.model.LogException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.io.Serializable;

@Repository
public interface LogExceptionRepository extends JpaRepository<LogException, Serializable>
{

}
