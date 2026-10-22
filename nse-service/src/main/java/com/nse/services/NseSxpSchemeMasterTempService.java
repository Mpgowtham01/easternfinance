package com.nse.services;

import com.nse.model.NseOnlineSipStpSwpMasterTemp;
import com.nse.repository.NseOnlineSipStpSwpMasterTempRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public long countByMasterOption(String option)
    {
        return repository.countByMasterOption(option);
    }

    /**
     * Publishes the temp rows for one master_option as the live rows for that option. Both statements
     * share one transaction, so readers keep seeing the previous rows until the copy commits, and a
     * failed copy rolls the delete back instead of leaving that option missing from the live table.
     */
    @Transactional
    public int publishTempToMainTable(String option)
    {
        repository.deleteFromMainByMasterOption(option);
        return repository.copyTempToMainByMasterOption(option);
    }
}
