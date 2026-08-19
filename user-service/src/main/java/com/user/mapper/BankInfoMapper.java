package com.user.mapper;

import com.user.dto.BankInfoDTO;
import com.user.model.User;

import java.util.List;
import java.util.stream.Collectors;

public class BankInfoMapper
{
    public static User dtoToUser(BankInfoDTO dto, User user)
    {
        if (dto == null || user == null) return null;

        user.setBank_ifsc_code1(dto.getIfscCode());
        user.setBank_micr_code1(dto.getMicrCode());
        user.setBank_code1(dto.getBankCode());
        user.setBank_name1(dto.getBankName());
        user.setBank_address1(dto.getBankAddress());
        user.setBank_branch1(dto.getBranchName());
        user.setBank_account_number1(dto.getAccountNumber());
        user.setBank_account_holder_name1(dto.getAccountHolderName());
        user.setBank_account_type1(dto.getAccountType());
//        user.setBank_proof1("");
        user.setDefault_bank1("Y"); // hardcoded as default

        return user;
    }

    public static BankInfoDTO userToDto(User user)
    {
        if (user == null) return null;

        BankInfoDTO dto = new BankInfoDTO();
        dto.setIfscCode(user.getBank_ifsc_code1());
        dto.setMicrCode(user.getBank_micr_code1());
        dto.setBankCode(user.getBank_code1());
        dto.setBankName(user.getBank_name1());
        dto.setBankAddress(user.getBank_address1());
        dto.setBranchName(user.getBank_branch1());
        dto.setAccountNumber(user.getBank_account_number1());
        dto.setAccountHolderName(user.getBank_account_holder_name1());
        dto.setAccountType(user.getBank_account_type1());
        return dto;
    }

    public static UserBseNseDetails dtoToUserBseNseDetails(BankInfoDTO dto, UserBseNseDetails user)
    {
        if (dto == null || user == null) return null;

        user.setBank_ifsc_code1(dto.getIfscCode());
        user.setBank_micr_code1(dto.getMicrCode());
        user.setBank_code1(dto.getBankCode());
        user.setBank_name1(dto.getBankName());
        user.setBank_address1(dto.getBankAddress());
        user.setBank_branch1(dto.getBranchName());
        user.setBank_account_number1(dto.getAccountNumber());
        user.setBank_account_holder_name1(dto.getAccountHolderName());
        user.setBank_account_type1(dto.getAccountType());
//        user.setBank_proof1("");
        user.setDefault_bank1("Y"); // hardcoded as default

        return user;
    }
    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static BankInfoDTO userBseNseDetailsToDto(UserBseNseDetails user) {
        BankInfoDTO dto = new BankInfoDTO();
        dto.setIfscCode(safe(user.getBank_ifsc_code1()));
        dto.setMicrCode(safe(user.getBank_micr_code1()));
        dto.setBankCode(safe(user.getBank_code1()));
        dto.setBankName(safe(user.getBank_name1()));
        dto.setBankAddress(safe(user.getBank_address1()));
        dto.setBranchName(safe(user.getBank_branch1()));
        dto.setAccountNumber(safe(user.getBank_account_number1()));
        dto.setAccountHolderName(safe(user.getBank_account_holder_name1()));
        dto.setAccountType(safe(user.getBank_account_type1()));
        return dto;
    }

    public static List<BankInfoDTO> userBseNseDetailsToDto(List<UserBseNseDetails> users) {
        return users.stream()
                .map(BankInfoMapper::userBseNseDetailsToDto)
                .collect(Collectors.toList());
    }


}
