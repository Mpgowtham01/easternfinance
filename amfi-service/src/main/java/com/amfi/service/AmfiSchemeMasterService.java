package com.amfi.service;

import com.amfi.model.AmfiSchemeMaster;
import com.amfi.repository.AmfiSchemeMasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AmfiSchemeMasterService
{
    @Autowired
    private AmfiSchemeMasterRepository amfiSchemeMasterRepository;

    // 1. Get CAMS Product Codes by Company
    public List<String> getCamsProductCodesByCompany(String schemeCompany)
    {
        return amfiSchemeMasterRepository.findSchemeCamsProductCodesByCompany(schemeCompany);
    }

    // 2. Get Active Schemes by scheme_amfi
    public List<AmfiSchemeMaster> getActiveSchemesByAmfi(String schemeAmfi)
    {
        return amfiSchemeMasterRepository.findBySchemeAmfiAndActive(schemeAmfi);
    }

    // 3. Get First Matching Record by scheme_cams_productcode
    public AmfiSchemeMaster getFirstByCamsProductCode(String schemeCamsProductcode)
    {
        List<AmfiSchemeMaster> result = amfiSchemeMasterRepository.findFirstBySchemeCamsProductcode(schemeCamsProductcode);
        return result.isEmpty() ? null : result.get(0);
    }

    // 4. Get Active Schemes by scheme_amfi_code
    public List<AmfiSchemeMaster> getActiveSchemesByAmfiCode(String schemeAmfiCode)
    {
        return amfiSchemeMasterRepository.findBySchemeAmfiCodeAndActive(schemeAmfiCode);
    }

    // 5. Get First Matching Record by scheme_karvy_productcode
    public AmfiSchemeMaster getFirstByKarvyProductCode(String schemeKarvyProductcode)
    {
        List<AmfiSchemeMaster> result = amfiSchemeMasterRepository.findFirstBySchemeKarvyProductcode(schemeKarvyProductcode);
        return result.isEmpty() ? null : result.get(0);
    }

    public List<AmfiSchemeMaster> getActiveWithIsinNo()
    {
        return amfiSchemeMasterRepository.findActiveWithIsinNo();
    }

    public List<AmfiSchemeMaster> findSchemeAmfiMaster(String scheme_amfi)
    {
        return amfiSchemeMasterRepository.findSchemeAmfiMaster(scheme_amfi);
    }

    public List<Object[]> autoSuggestAllMfSchemes(String keyword,String category, String amc)
    {
        return amfiSchemeMasterRepository.autoSuggestAllMfSchemes(keyword,category,amc);
    }

}