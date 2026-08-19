package com.nse.services;

import com.nse.model.NseTransactions;
import com.nse.repository.NseTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NseTransactionService
{
    @Autowired
    NseTransactionRepository nseTransactionRepository;

    public void save(NseTransactions nse)
    {
        nseTransactionRepository.save(nse);
    }
}

