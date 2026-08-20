package com.user.mapper;

import com.user.dto.ContactInfoDTO;
import com.user.model.User;
import com.user.model.UsersOnlineRegDetails;

public class ContactInfoMapper
{
    public static UsersOnlineRegDetails dtoToUser(ContactInfoDTO dto, UsersOnlineRegDetails user)
    {
        if (dto == null) return null;
        user.setStreet_1(dto.getAddress1());
        user.setStreet_2(dto.getAddress2());
        user.setStreet_3(dto.getAddress3());
        user.setCity(dto.getCity());
        user.setState(dto.getState());
        user.setState_code(dto.getStateCode());
        user.setPincode(dto.getPincode());
        user.setCountry(dto.getCountry());
        return user;
    }

    public static ContactInfoDTO userToDto(User user)
    {
        if (user == null) return null;

        ContactInfoDTO dto = new ContactInfoDTO();
        dto.setAddress1(user.getStreet_1());
        dto.setAddress2(user.getStreet_2());
        dto.setAddress3(user.getStreet_3());
        dto.setCity(user.getCity());
        dto.setState(user.getState());
        dto.setPincode(user.getPincode());
        dto.setCountry(user.getCountry());
        return dto;
    }

    public static UsersOnlineRegDetails dtoToUserBseNseDetails(ContactInfoDTO dto, UsersOnlineRegDetails user)
    {
        if (dto == null) return null;
        user.setStreet_1(dto.getAddress1());
        user.setStreet_2(dto.getAddress2());
        user.setStreet_3(dto.getAddress3());
        user.setCity(dto.getCity());
        user.setState(dto.getState());
        user.setState_code(dto.getStateCode());
        user.setPincode(dto.getPincode());
        user.setCountry(dto.getCountry());
        return user;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static ContactInfoDTO userBseNseDetailsToDto(UsersOnlineRegDetails user)
    {
        if (user == null) return null;

        ContactInfoDTO dto = new ContactInfoDTO();
        dto.setAddress1(safe(user.getStreet_1()));
        dto.setAddress2(safe(user.getStreet_2()));
        dto.setAddress3(safe(user.getStreet_3()));
        dto.setCity(safe(user.getCity()));
        dto.setState(safe(user.getState()));
        dto.setStateCode(safe(user.getState_code()));
        dto.setPincode(safe(user.getPincode()));
        dto.setCountry(safe(user.getCountry()));
        return dto;
    }
}
