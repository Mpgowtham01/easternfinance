package com.nse.services;

import com.nse.model.NseOnlineSchemeMaster;
import com.nse.model.NseOnlineSchemeMasterTemp;
import com.nse.repository.NseOnlineSchemeMasterRepository;
import com.nse.repository.NseOnlineSchemeMasterTempRepository;
import com.nse.utils.NseUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NseOnlineSchemeMasterService
{
    @Autowired
    private NseOnlineSchemeMasterRepository nseOnlineSchemeMasterRepository;

    @Autowired
    private NseOnlineSchemeMasterTempRepository nseOnlineSchemeMasterTempRepository;

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
        if(!amcCode.equalsIgnoreCase("All"))
        {
            return nseOnlineSchemeMasterRepository.getLumpsumCategoriesByAmc(amcCode);
        }else{
            return nseOnlineSchemeMasterRepository.getAllLumpsumSchemeCategories();
        }
    }

    public List<Object[]> getLumpsumSchemeNames(String amcName, String category)
    {
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
                return nseOnlineSchemeMasterRepository.getAllSIFLumpsumSchemesByAmcCode(amc_name);
            }

            return nseOnlineSchemeMasterRepository.getAllLumpsumSchemesByAmcCode(amcName);
        }else
        {
            if(amcName.contains("SIF"))
            {
                String amc_name = NseUtils.getSIFAmcCode(amcName);
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

    public void saveAllAndFlushAllSchemeMaster(List<NseOnlineSchemeMasterTemp> masterList)
    {
        nseOnlineSchemeMasterTempRepository.saveAllAndFlush(masterList);
    }

    public void clearNseSchemeMasterTempTable()
    {
        nseOnlineSchemeMasterTempRepository.deleteAllTempRecords();
    }

    public long countTempRecords()
    {
        return nseOnlineSchemeMasterTempRepository.countTempRecords();
    }

    /**
     * Publishes the temp table as the live table. Both statements share one transaction, so readers
     * keep seeing the previous scheme master until the copy commits, and a failed copy rolls the
     * delete back instead of leaving the live table empty.
     */
    @Transactional
    public int publishTempToMainTable()
    {
        nseOnlineSchemeMasterTempRepository.deleteAllMainRecords();
        return nseOnlineSchemeMasterTempRepository.copyTempToMainTable();
    }
}
