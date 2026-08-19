package com.nse.services;

import com.nse.model.NseOnlineSchemeMaster;
import com.nse.repository.NseOnlineSchemeMasterRepository;
import com.nse.utils.NseUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NseOnlineSchemeMasterService
{
    @Autowired
    private NseOnlineSchemeMasterRepository nseOnlineSchemeMasterRepository;

    public List<Object[]> getLumpsumAmc(List<String> amc_list)
    {
        if(amc_list != null && amc_list.size() > 0)
        {
            return nseOnlineSchemeMasterRepository.getLumpsumAmcNamesByAmcName(amc_list);
        }else
        {
            return nseOnlineSchemeMasterRepository.getAllLumpsumAmcNames();
        }
    }

    public List<String> getLumpsumCategories(String amcCode)
    {
        System.out.println("amcCode = " + amcCode);
        if(!amcCode.equalsIgnoreCase("All"))
        {
            return nseOnlineSchemeMasterRepository.getLumpsumCategoriesByAmc(amcCode);
        }else{
            return nseOnlineSchemeMasterRepository.getAllLumpsumSchemeCategories();
        }
    }

    public List<Object[]> getLumpsumSchemeNames(String amcName, String category)
    {
        System.out.println("amc_name = " + amcName);
        if(amcName.equalsIgnoreCase("All") && category.equalsIgnoreCase("All"))
        {
            return nseOnlineSchemeMasterRepository.getAllLumpsumSchemes();
        }else if(amcName.equalsIgnoreCase("All") && !category.equalsIgnoreCase("All"))
        {
            return nseOnlineSchemeMasterRepository.getAllLumpsumSchemesByCategory(category);
        }else if(!amcName.equalsIgnoreCase("All") && category.equalsIgnoreCase("All"))
        {
            if(amcName.contains("SIF"))
            {
                String amc_name = NseUtils.getSIFAmcCode(amcName);
                System.out.println("53 = " + amc_name);
                return nseOnlineSchemeMasterRepository.getAllSIFLumpsumSchemesByAmcCode(amc_name);
            }

            return nseOnlineSchemeMasterRepository.getAllLumpsumSchemesByAmcCode(amcName);
        }else
        {
            if(amcName.contains("SIF"))
            {
                String amc_name = NseUtils.getSIFAmcCode(amcName);
                System.out.println("63 = " + amc_name);
                return nseOnlineSchemeMasterRepository.getAllSIFLumpsumSchemesByAmcCodeAndCategory(amc_name,category);
            }
            return nseOnlineSchemeMasterRepository.getAllLumpsumSchemesByAmcCodeAndCategory(amcName, category);
        }
    }

    public List<String> getSipCategories(String amcCode)
    {
        if(amcCode.equalsIgnoreCase("All"))
        {
            return nseOnlineSchemeMasterRepository.getAllSipSchemeCategories();
        }else
        {
            return nseOnlineSchemeMasterRepository.getSipSchemeCategoriesByAmc(amcCode);
        }
    }

    public List<Object[]> getSipSchemeNames(String amc_code, String category)
    {
        if(amc_code.equalsIgnoreCase("All") && category.equalsIgnoreCase("All"))
        {
            return nseOnlineSchemeMasterRepository.getAllSipSchemes();
        }else if(amc_code.equalsIgnoreCase("All") && !category.equalsIgnoreCase("All"))
        {
            return nseOnlineSchemeMasterRepository.getSipSchemesByCategory(category);
        }else if(!amc_code.equalsIgnoreCase("All") && category.equalsIgnoreCase("All"))
        {
            return nseOnlineSchemeMasterRepository.getSipSchemesByAmcCode(amc_code);
        }else
        {
            return nseOnlineSchemeMasterRepository.getSipSchemesByAmcCodeAndCategory(amc_code, category);
        }
    }
}
