package com.user.mapper;

import com.user.dto.BankDto;
import com.user.dto.BankInfoDTO;
import com.user.model.User;
import com.user.model.UsersBankDetails;
import com.user.model.UsersOnlineRegDetails;

import java.util.List;
import java.util.stream.Collectors;

public class BankInfoMapper
{
    public static UsersBankDetails dtoToUser(BankInfoDTO dto, UsersBankDetails user)
    {
        if (dto == null || user == null) return null;

        user.setBank_ifsc_code(dto.getIfscCode());
        user.setBank_micr_code(dto.getMicrCode());
        user.setBank_code(dto.getBankCode());
        user.setBank_name(dto.getBankName());
        user.setBank_address(dto.getBankAddress());
        user.setBank_branch(dto.getBranchName());
        user.setBank_account_number(dto.getAccountNumber());
        user.setBank_account_holder_name(dto.getAccountHolderName());
        user.setBank_account_type(dto.getAccountType());
//        user.setBank_proof1("");
        user.setDefault_bank("Y"); // hardcoded as default

        return user;
    }

    public static UsersBankDetails toEntity(BankDto dto)
    {
        UsersBankDetails entity = new UsersBankDetails();
        entity.setId(dto.getId());
        entity.setUser_id(dto.getUser_id());
        entity.setOnline_flag(dto.getOnline_flag());
        entity.setOnline_id(dto.getOnline_id());
        entity.setOnline_code(dto.getOnline_code());
        entity.setBroker_code(dto.getBroker_code());
        entity.setClient_name(dto.getClient_name());
        entity.setBank_name(dto.getBank_name());
        entity.setBank_branch(dto.getBank_branch());
        entity.setBank_address(dto.getBank_address());
        entity.setBank_code(dto.getBank_code());
        entity.setBank_account_number(dto.getBank_account_number());
        entity.setBank_account_holder_name(dto.getBank_account_holder_name());
        entity.setBank_account_type(dto.getBank_account_type());
        entity.setBank_ifsc_code(dto.getBank_ifsc_code());
        entity.setBank_micr_code(dto.getBank_micr_code());
        if (dto.getCreated_date() != null)
        {
            entity.setCreated_date(dto.getCreated_date());
        }
        return entity;
    }

//    public static BankInfoDTO userToDto(User user)
//    {
//        if (user == null) return null;
//
//        BankInfoDTO dto = new BankInfoDTO();
//        dto.setIfscCode(user.getBank_ifsc_code1());
//        dto.setMicrCode(user.getBank_micr_code1());
//        dto.setBankCode(user.getBank_code1());
//        dto.setBankName(user.getBank_name1());
//        dto.setBankAddress(user.getBank_address1());
//        dto.setBranchName(user.getBank_branch1());
//        dto.setAccountNumber(user.getBank_account_number1());
//        dto.setAccountHolderName(user.getBank_account_holder_name1());
//        dto.setAccountType(user.getBank_account_type1());
//        return dto;
//    }

    public static UsersBankDetails dtoToUserBseNseDetails(BankInfoDTO dto, UsersBankDetails user)
    {
        if (dto == null || user == null) return null;

        user.setBank_ifsc_code(dto.getIfscCode());
        user.setBank_micr_code(dto.getMicrCode());
        user.setBank_code(dto.getBankCode());
        user.setBank_name(dto.getBankName());
        user.setBank_address(dto.getBankAddress());
        user.setBank_branch(dto.getBranchName());
        user.setBank_account_number(dto.getAccountNumber());
        user.setBank_account_holder_name(dto.getAccountHolderName());
        user.setBank_account_type(dto.getAccountType());
//        user.setBank_proof1("");
        user.setDefault_bank("Y"); // hardcoded as default

        return user;
    }
    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static BankInfoDTO userBseNseDetailsToDto(UsersBankDetails user) {
        BankInfoDTO dto = new BankInfoDTO();
        dto.setIfscCode(safe(user.getBank_ifsc_code()));
        dto.setMicrCode(safe(user.getBank_micr_code()));
        dto.setBankCode(safe(user.getBank_code()));
        dto.setBankName(safe(user.getBank_name()));
        dto.setBankAddress(safe(user.getBank_address()));
        dto.setBranchName(safe(user.getBank_branch()));
        dto.setAccountNumber(safe(user.getBank_account_number()));
        dto.setAccountHolderName(safe(user.getBank_account_holder_name()));
        dto.setAccountType(safe(user.getBank_account_type()));
        return dto;
    }

    public static List<BankInfoDTO> userBseNseDetailsToDto(List<UsersBankDetails> users)
    {
        return users.stream().map(BankInfoMapper::userBseNseDetailsToDto).collect(Collectors.toList());
    }

    public static BankDto toDto(UsersBankDetails entity) {
        BankDto dto = new BankDto();
        dto.setId(entity.getId());
        dto.setUser_id(entity.getUser_id());
        dto.setOnline_id(entity.getOnline_id());
        dto.setOnline_flag(entity.getOnline_flag());
        dto.setOnline_code(entity.getOnline_code());
        dto.setBroker_code(entity.getBroker_code());
        dto.setClient_name(entity.getClient_name());
        dto.setBank_name(entity.getBank_name());
        dto.setBank_branch(entity.getBank_branch());
        dto.setBank_address(entity.getBank_address());
        dto.setBank_account_number(entity.getBank_account_number());
        dto.setBank_account_holder_name(entity.getBank_account_holder_name());
        dto.setBank_account_type(entity.getBank_account_type());
        dto.setBank_ifsc_code(entity.getBank_ifsc_code());
        dto.setBank_micr_code(entity.getBank_micr_code());
        if (dto.getCreated_date() != null) {
            entity.setCreated_date(dto.getCreated_date());
        }
        return dto;
    }

}
