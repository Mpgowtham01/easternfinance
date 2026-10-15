package com.nse.services;

import com.nse.model.NseOnlineSipStpSwpMasterTemp;
import com.nse.repository.NseOnlineSipStpSwpMasterTempRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NseSxpSchemeMasterTempService
{
    @Autowired
    NseOnlineSipStpSwpMasterTempRepository repository;

    public void createNseSchemeMasterTempTable()
    {
        repository.createNseSchemeMasterTempTable();
        repository.truncateNseSchemeMasterTempTable();
    }

    public void modifyNseSchemeMasterTempToMainTable()
    {
        repository.modifyNseSchemeMasterTempToMainTable();
    }

    public void dropNseSchemeMasterOldTable()
    {
        repository.dropNseSchemeMasterOldTable();
    }

    public void saveAllAndFlushAllSchemeMaster(List<NseOnlineSipStpSwpMasterTemp> masterList)
    {
        repository.saveAllAndFlush(masterList);
    }

    public void deleteByMasterOption(String option)
    {
        repository.deleteByMasterOption(option);
    }
}
