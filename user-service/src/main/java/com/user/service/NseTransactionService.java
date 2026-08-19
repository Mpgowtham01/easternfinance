package com.user.service;

import com.user.dto.NseTransactionDto;
import com.user.model.NseTransactions;
import com.user.repository.NseTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NseTransactionService {

    private final NseTransactionRepository repository;

    public void save(NseTransactionDto dto) {
        NseTransactions entity = new NseTransactions();
        // copy all fields from dto → entity
        entity.setUrl(dto.getUrl());
        entity.setNse_request(dto.getNse_request());
        entity.setNse_response(dto.getNse_response());
        entity.setReturn_msg(dto.getReturn_msg());
        entity.setService_return_code(dto.getService_return_code());
        entity.setService_msg(dto.getService_msg());
        entity.setReg_id(dto.getReg_id());
        entity.setPayment_link(dto.getPayment_link());
        entity.setPan(dto.getPan());
        entity.setName(dto.getName());
        entity.setBranch(dto.getBranch());
        entity.setRm_name(dto.getRm_name());
        entity.setSubbroker_name(dto.getSubbroker_name());
        entity.setClient_name(dto.getClient_name());
        entity.setIin_number(dto.getIin_number());
        entity.setScheme_name(dto.getScheme_name());
        entity.setScheme_code(dto.getScheme_code());
        entity.setFolio_no(dto.getFolio_no());
        entity.setAmount_units(dto.getAmount_units());
        entity.setFrequency(dto.getFrequency());
        entity.setPeriod_day(dto.getPeriod_day());
        entity.setUmrn_no(dto.getUmrn_no());
        entity.setPurchase_type(dto.getPurchase_type());
        entity.setPayment_ref_no(dto.getPayment_ref_no());
        entity.setUnique_number(dto.getUnique_number());
        entity.setAuto_trxn_no(dto.getAuto_trxn_no());
        entity.setSip_reg_no(dto.getSip_reg_no());
        entity.setPayment_mode(dto.getPayment_mode());
        entity.setTopup_amount(dto.getTopup_amount());
        entity.setBank_acc_no(dto.getBank_acc_no());
        entity.setTransaction_number(dto.getTransaction_number());
        entity.setApplication_number(dto.getApplication_number());
        entity.setTo_scheme_code(dto.getTo_scheme_code());
        entity.setTo_scheme_name(dto.getTo_scheme_name());
        entity.setTransaction_type(dto.getTransaction_type());
        entity.setTransaction_status(dto.getTransaction_status());
        entity.setPayment_status(dto.getPayment_status());
        entity.setActive_ceased_status(dto.getActive_ceased_status());
        entity.setRemarks(dto.getRemarks());
        entity.setMandate_id(dto.getMandate_id());
        entity.setMandate_status(dto.getMandate_status());
        entity.setEmandate_auth_flag(dto.getEmandate_auth_flag());
        entity.setApp_received_flag(dto.getApp_received_flag());
        entity.setTransaction_date(dto.getTransaction_date());
        entity.setUser_id(dto.getUser_id());
        entity.setRegister_source(dto.getRegister_source());
        entity.setBroker_code(dto.getBroker_code());
        entity.setEuin_number(dto.getEuin_number());
        entity.setCc_received(dto.getCc_received());
        entity.setFund_trans_to_amc(dto.getFund_trans_to_amc());
        entity.setRefund_status(dto.getRefund_status());
        entity.setRefund_amount(dto.getRefund_amount());

        repository.save(entity);
    }
}

