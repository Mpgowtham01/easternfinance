package com.user.service;

import com.user.dto.UsersNseRegReportDto;
import com.user.model.UsersNseRegReport;
import com.user.repository.UsersNseRegReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsersNseRegReportService {

    private final UsersNseRegReportRepository repository;

    public void save(UsersNseRegReportDto dto) {
        UsersNseRegReport entity = new UsersNseRegReport();
        entity.setUser_id(dto.getUser_id());
        entity.setName(dto.getName());
        entity.setPan(dto.getPan());
        entity.setBranch(dto.getBranch());
        entity.setRm_name(dto.getRm_name());
        entity.setSubbroker_name(dto.getSubbroker_name());
        entity.setIin_number(dto.getIin_number());
        entity.setIin_created_date(dto.getIin_created_date());
        entity.setForm_updated_date(dto.getForm_updated_date());
        entity.setCheque_updated_date(dto.getCheque_updated_date());
        entity.setIin_status(dto.getIin_status());
        entity.setIin_active(dto.getIin_active());
        entity.setMandate_active(dto.getMandate_active());
        entity.setTransaction_date(dto.getTransaction_date());
        entity.setMultiple_reg(dto.getMultiple_reg());
        entity.setClient_name(dto.getClient_name());

        repository.save(entity);
    }

    public UsersNseRegReport saveorupdateRegReport(UsersNseRegReport user) {
        return repository.save(user);
    }

}
