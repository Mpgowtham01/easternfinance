package com.amfi.service;

import com.amfi.model.AmfiMfNav;
import com.amfi.repository.AmfiMfNavRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AmfiMfNavService
{
    @Autowired
    private AmfiMfNavRepository amfiMfNavRepository;

    // 1. Get all NAV values (Double) for a scheme, ordered by nav_date DESC
    public List<Double> getNetAssetValuesBySchemeCode(String schemeCode) {
        return amfiMfNavRepository.findNetAssetValueBySchemeCodeOrderByNavDateDesc(schemeCode);
    }

    // 2. Get the latest NAV entry for a scheme
    public AmfiMfNav getLatestNavBySchemeCode(String schemeCode) {
        return amfiMfNavRepository.findTopBySchemeCodeOrderByNavDateDesc(schemeCode);
    }
}
