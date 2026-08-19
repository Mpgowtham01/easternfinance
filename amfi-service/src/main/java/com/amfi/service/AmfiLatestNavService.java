package com.amfi.service;

import com.amfi.model.AmfiLatestNav;
import com.amfi.repository.AmfiLatestNavRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AmfiLatestNavService
{
    @Autowired
    private AmfiLatestNavRepository amfiLatestNavRepository;

    // Get NAV records by scheme code
    public List<AmfiLatestNav> getBySchemeCode(String schemeCode)
    {
        return amfiLatestNavRepository.findBySchemeCodeUsingQuery(schemeCode);
    }
}
