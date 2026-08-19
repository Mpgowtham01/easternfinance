package com.amfi.service;

import com.amfi.model.InvestorPortfolioHealthCheckup;
import com.amfi.repository.InvestorPortfolioHealthCheckupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InvestorPortfolioHealthCheckupService
{
    @Autowired
    private InvestorPortfolioHealthCheckupRepository repository;

    public List<InvestorPortfolioHealthCheckup> getRatingList()
    {
        return repository.findAllRegularWithSchemeAmfiCode();
    }
}
