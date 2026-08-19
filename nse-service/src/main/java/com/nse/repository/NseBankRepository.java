package com.nse.repository;

import com.nse.model.NseBank;


import org.springframework.data.jpa.repository.JpaRepository;

public interface NseBankRepository extends JpaRepository<NseBank, Integer> {}