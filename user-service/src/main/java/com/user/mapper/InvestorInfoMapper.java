package com.user.mapper;

import com.user.dto.InvestorInfoDTO;
import com.user.model.User;
import com.user.model.UsersOnlineRegDetails;
import com.user.utils.UserUtils;

public class InvestorInfoMapper
{
    public static UsersOnlineRegDetails mapDtoToUser(InvestorInfoDTO dto, UsersOnlineRegDetails user)
    {
        if (dto == null) return null;
        user.setPan(UserUtils.checkParameter(dto.getPan()));
        user.setBroker_code(UserUtils.checkParameter(dto.getBrokerCode()));
        user.setNse_iin_number(UserUtils.checkParameter(dto.getInvestorCode()));
        user.setTax_status_code(UserUtils.checkParameter(dto.getTaxStatusCode()));
        user.setTax_status(UserUtils.checkParameter(dto.getTaxStatusDesc()));
        user.setHolding_nature_code(UserUtils.checkParameter(dto.getHoldingNatureCode()));
        user.setHolding_nature(UserUtils.checkParameter(dto.getHoldingNatureDesc()));
        return user;
    }

    public static InvestorInfoDTO mapUserToDto(User user)
    {
        if (user == null) return null;
        InvestorInfoDTO dto = new InvestorInfoDTO();
        dto.setPan(UserUtils.checkParameter(user.getPan()));
        dto.setBrokerCode(UserUtils.checkParameter(user.getBroker_code()));
        dto.setInvestorCode(UserUtils.checkParameter(user.getNse_iin_number()));
        dto.setTaxStatusCode(UserUtils.checkParameter(user.getTax_status_code()));
        dto.setTaxStatusDesc(UserUtils.checkParameter(user.getTax_status()));
        dto.setHoldingNatureCode(UserUtils.checkParameter(user.getHolding_nature_code()));
        dto.setHoldingNatureDesc(UserUtils.checkParameter(user.getHolding_nature()));
        return dto;
    }

    public static UsersOnlineRegDetails mapDtoToUserBseNseDetails(InvestorInfoDTO dto, UsersOnlineRegDetails user)
    {
        if (dto == null) return null;
        user.setPan(UserUtils.checkParameter(dto.getPan()));
        user.setBroker_code(UserUtils.checkParameter(dto.getBrokerCode()));
        user.setNse_iin_number(UserUtils.checkParameter(dto.getInvestorCode()));
        user.setTax_status_code(UserUtils.checkParameter(dto.getTaxStatusCode()));
        user.setTax_status(UserUtils.checkParameter(dto.getTaxStatusDesc()));
        user.setHolding_nature_code(UserUtils.checkParameter(dto.getHoldingNatureCode()));
        user.setHolding_nature(UserUtils.checkParameter(dto.getHoldingNatureDesc()));
        user.setName(UserUtils.checkParem(user.getName()));
        user.setMobile(UserUtils.checkParem(user.getMobile()));
        return user;
    }

    public static InvestorInfoDTO mapBseNseDetailsToDto(UserBseNseDetails user)
    {
        if (user == null) return null;
        InvestorInfoDTO dto = new InvestorInfoDTO();
        dto.setPan(UserUtils.checkParameter(user.getPan()));
        dto.setBrokerCode(UserUtils.checkParameter(user.getBroker_code()));
        dto.setInvestorCode(UserUtils.checkParameter(user.getNse_iin_number()));
        dto.setTaxStatusCode(UserUtils.checkParameter(user.getTax_status_code()));
        dto.setTaxStatusDesc(UserUtils.checkParameter(user.getTax_status()));
        dto.setHoldingNatureCode(UserUtils.checkParameter(user.getHolding_nature_code()));
        dto.setHoldingNatureDesc(UserUtils.checkParameter(user.getHolding_nature()));
        return dto;
    }
}
