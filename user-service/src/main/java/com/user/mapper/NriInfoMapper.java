package com.user.mapper;

import com.user.dto.NriInfoDTO;
import com.user.model.User;
import com.user.model.UsersOnlineRegDetails;

public class NriInfoMapper
{
    public static UsersOnlineRegDetails dtoToUser(NriInfoDTO dto, UsersOnlineRegDetails user)
    {
        if (dto == null) return null;
        user.setNri_address1(dto.getAddress1());
        user.setNri_address2(dto.getAddress2());
        user.setNri_address3(dto.getAddress3());
        user.setNri_city(dto.getCity());
        user.setNri_state(dto.getState());
        user.setNri_pincode(dto.getPincode());
        user.setNri_country(dto.getCountry());
        return user;
    }

    public static NriInfoDTO userToDto(UsersOnlineRegDetails user)
    {
        if (user == null) return null;

        NriInfoDTO dto = new NriInfoDTO();
        dto.setAddress1(user.getNri_address1());
        dto.setAddress2(user.getNri_address2());
        dto.setAddress3(user.getNri_address3());
        dto.setCity(user.getNri_city());
        dto.setState(user.getNri_state());
        dto.setPincode(user.getNri_pincode());
        dto.setCountry(user.getNri_country());

        return dto;
    }

    public static UsersOnlineRegDetails dtoToUserBseNseDetails(NriInfoDTO dto, UsersOnlineRegDetails user)
    {
        if (dto == null) return null;
        user.setNri_address1(dto.getAddress1());
        user.setNri_address2(dto.getAddress2());
        user.setNri_address3(dto.getAddress3());
        user.setNri_city(dto.getCity());
        user.setNri_state(dto.getState());
        user.setNri_pincode(dto.getPincode());
        user.setNri_country(dto.getCountry());
        return user;
    }

    public static NriInfoDTO userBseNseDetailsToDto(UsersOnlineRegDetails user)
    {
        if (user == null) return null;

        NriInfoDTO dto = new NriInfoDTO();
        dto.setAddress1(user.getNri_address1());
        dto.setAddress2(user.getNri_address2());
        dto.setAddress3(user.getNri_address3());
        dto.setCity(user.getNri_city());
        dto.setState(user.getNri_state());
        dto.setPincode(user.getNri_pincode());
        dto.setCountry(user.getNri_country());

        return dto;
    }
}
