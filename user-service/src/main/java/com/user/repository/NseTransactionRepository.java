package com.user.repository;

import com.user.model.NseTransactions;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NseTransactionRepository extends JpaRepository<NseTransactions, Long> {
}