package com.user.mapper;

import com.user.dto.JointHolderInfoDTO;
import com.user.model.User;
import com.user.model.UsersOnlineRegDetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JoinHolderInfoMapper
{
    public static
    UsersOnlineRegDetails dtoToUser(List<JointHolderInfoDTO> dtoList, UsersOnlineRegDetails user) {
        if (dtoList == null || user == null) return null;

        for (JointHolderInfoDTO dto : dtoList)
        {
            if (dto.getId() == null) continue;

            Integer id = dto.getId();

            if (id == 1)
            {
                user.setJoint_holder_name1(dto.getName());
                user.setJoint_holder_pan1(dto.getPan());
                user.setJoint_holder_dob1(dto.getDob());
                user.setJoint_holder_email1(dto.getEmail());
                user.setJoint_holder_email_relation1(dto.getEmailRelation());
                user.setJoint_holder_mobile1(dto.getMobile());
                user.setJoint_holder_mobile_relation1(dto.getMobileRelation());
                user.setJoint_holder_place_of_birth1(dto.getPlaceBirth());
                user.setJoint_holder_country_birth_code1(dto.getCountryBirth());
                user.setJoint_holder_occupation_code1(dto.getOccupation());
                user.setJoint_holder_annual_income_code1(dto.getIncome());
                user.setJoint_holder_source_of_wealth_code1(dto.getSourceWealth());
                user.setJoint_holder_address_type_code1(dto.getAddressType());
                user.setJoint_holder_political_code1(dto.getPolitical());
            }

            if (id == 2)
            {
                user.setJoint_holder_name2(dto.getName());
                user.setJoint_holder_pan2(dto.getPan());
                user.setJoint_holder_dob2(dto.getDob());
                user.setJoint_holder_email2(dto.getEmail());
                user.setJoint_holder_email_relation2(dto.getEmailRelation());
                user.setJoint_holder_mobile2(dto.getMobile());
                user.setJoint_holder_mobile_relation2(dto.getMobileRelation());
                user.setJoint_holder_place_of_birth2(dto.getPlaceBirth());
                user.setJoint_holder_country_birth_code2(dto.getCountryBirth());
                user.setJoint_holder_occupation_code2(dto.getOccupation());
                user.setJoint_holder_annual_income_code2(dto.getIncome());
                user.setJoint_holder_source_of_wealth_code2(dto.getSourceWealth());
                user.setJoint_holder_address_type_code2(dto.getAddressType());
                user.setJoint_holder_political_code2(dto.getPolitical());
            }
        }

        return user;
    }

    public static List<JointHolderInfoDTO> userToDto(
            UsersOnlineRegDetails user)
    {
        if (user == null) return Collections.emptyList();

        List<JointHolderInfoDTO> dtoList = new ArrayList<>();

        // Joint Holder 1
        JointHolderInfoDTO jh1 = new JointHolderInfoDTO();
        jh1.setId(1);
        jh1.setName(user.getJoint_holder_name1());
        jh1.setPan(user.getJoint_holder_pan1());
        jh1.setDob(user.getJoint_holder_dob1());
        jh1.setEmail(user.getJoint_holder_email1());
        jh1.setEmailRelation(user.getJoint_holder_email_relation1());
        jh1.setMobile(user.getJoint_holder_mobile1());
        jh1.setMobileRelation(user.getJoint_holder_mobile_relation1());
        jh1.setPlaceBirth(user.getJoint_holder_place_of_birth1());
        jh1.setCountryBirth(user.getJoint_holder_country_birth_code1());
        jh1.setOccupation(user.getJoint_holder_occupation_code1());
        jh1.setIncome(user.getJoint_holder_annual_income_code1());
        jh1.setSourceWealth(user.getJoint_holder_source_of_wealth_code1());
        jh1.setAddressType(user.getJoint_holder_address_type_code1());
        jh1.setPolitical(user.getJoint_holder_political_code1());
        dtoList.add(jh1);

        // Joint Holder 2
        JointHolderInfoDTO jh2 = new JointHolderInfoDTO();
        jh2.setId(2);
        jh2.setName(user.getJoint_holder_name2());
        jh2.setPan(user.getJoint_holder_pan2());
        jh2.setDob(user.getJoint_holder_dob2());
        jh2.setEmail(user.getJoint_holder_email2());
        jh2.setEmailRelation(user.getJoint_holder_email_relation2());
        jh2.setMobile(user.getJoint_holder_mobile2());
        jh2.setMobileRelation(user.getJoint_holder_mobile_relation2());
        jh2.setPlaceBirth(user.getJoint_holder_place_of_birth2());
        jh2.setCountryBirth(user.getJoint_holder_country_birth_code2());
        jh2.setOccupation(user.getJoint_holder_occupation_code2());
        jh2.setIncome(user.getJoint_holder_annual_income_code2());
        jh2.setSourceWealth(user.getJoint_holder_source_of_wealth_code2());
        jh2.setAddressType(user.getJoint_holder_address_type_code2());
        jh2.setPolitical(user.getJoint_holder_political_code2());
        dtoList.add(jh2);

        return dtoList;
    }

    public static UsersOnlineRegDetails dtoToUserBseNseDetails(List<JointHolderInfoDTO> dtoList, UsersOnlineRegDetails user) {
        if (dtoList == null || user == null) return null;

        for (JointHolderInfoDTO dto : dtoList)
        {
            if (dto.getId() == null) continue;

            Integer id = dto.getId();

            if (id == 1)
            {
                user.setJoint_holder_name1(dto.getName());
                user.setJoint_holder_pan1(dto.getPan());
                user.setJoint_holder_dob1(dto.getDob());
                user.setJoint_holder_email1(dto.getEmail());
                user.setJoint_holder_email_relation1(dto.getEmailRelation());
                user.setJoint_holder_mobile1(dto.getMobile());
                user.setJoint_holder_mobile_relation1(dto.getMobileRelation());
                user.setJoint_holder_place_of_birth1(dto.getPlaceBirth());
                user.setJoint_holder_country_birth_code1(dto.getCountryBirth());
                user.setJoint_holder_occupation_code1(dto.getOccupation());
                user.setJoint_holder_annual_income_code1(dto.getIncome());
                user.setJoint_holder_source_of_wealth_code1(dto.getSourceWealth());
                user.setJoint_holder_address_type_code1(dto.getAddressType());
                user.setJoint_holder_political_code1(dto.getPolitical());
            }

            if (id == 2)
            {
                user.setJoint_holder_name2(dto.getName());
                user.setJoint_holder_pan2(dto.getPan());
                user.setJoint_holder_dob2(dto.getDob());
                user.setJoint_holder_email2(dto.getEmail());
                user.setJoint_holder_email_relation2(dto.getEmailRelation());
                user.setJoint_holder_mobile2(dto.getMobile());
                user.setJoint_holder_mobile_relation2(dto.getMobileRelation());
                user.setJoint_holder_place_of_birth2(dto.getPlaceBirth());
                user.setJoint_holder_country_birth_code2(dto.getCountryBirth());
                user.setJoint_holder_occupation_code2(dto.getOccupation());
                user.setJoint_holder_annual_income_code2(dto.getIncome());
                user.setJoint_holder_source_of_wealth_code2(dto.getSourceWealth());
                user.setJoint_holder_address_type_code2(dto.getAddressType());
                user.setJoint_holder_political_code2(dto.getPolitical());
            }
        }

        return user;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static List<JointHolderInfoDTO> userBseNseDetailsToDto(
            UsersOnlineRegDetails user) {
        if (user == null) return Collections.emptyList();

        List<JointHolderInfoDTO> dtoList = new ArrayList<>();

        // Joint Holder 1
        JointHolderInfoDTO jh1 = new JointHolderInfoDTO();
        jh1.setId(1);
        jh1.setName(safe(user.getJoint_holder_name1()));
        jh1.setPan(safe(user.getJoint_holder_pan1()));
        jh1.setDob(safe(user.getJoint_holder_dob1()));
        jh1.setEmail(safe(user.getJoint_holder_email1()));
        jh1.setEmailRelation(safe(user.getJoint_holder_email_relation1()));
        jh1.setMobile(safe(user.getJoint_holder_mobile1()));
        jh1.setMobileRelation(safe(user.getJoint_holder_mobile_relation1()));
        jh1.setPlaceBirth(safe(user.getJoint_holder_place_of_birth1()));
        jh1.setCountryBirth(safe(user.getJoint_holder_country_birth_code1()));
        jh1.setOccupation(safe(user.getJoint_holder_occupation_code1()));
        jh1.setIncome(safe(user.getJoint_holder_annual_income_code1()));
        jh1.setSourceWealth(safe(user.getJoint_holder_source_of_wealth_code1()));
        jh1.setAddressType(safe(user.getJoint_holder_address_type_code1()));
        jh1.setPolitical(safe(user.getJoint_holder_political_code1()));
        dtoList.add(jh1);

        // Joint Holder 2
        JointHolderInfoDTO jh2 = new JointHolderInfoDTO();
        jh2.setId(2);
        jh2.setName(safe(user.getJoint_holder_name2()));
        jh2.setPan(safe(user.getJoint_holder_pan2()));
        jh2.setDob(safe(user.getJoint_holder_dob2()));
        jh2.setEmail(safe(user.getJoint_holder_email2()));
        jh2.setEmailRelation(safe(user.getJoint_holder_email_relation2()));
        jh2.setMobile(safe(user.getJoint_holder_mobile2()));
        jh2.setMobileRelation(safe(user.getJoint_holder_mobile_relation2()));
        jh2.setPlaceBirth(safe(user.getJoint_holder_place_of_birth2()));
        jh2.setCountryBirth(safe(user.getJoint_holder_country_birth_code2()));
        jh2.setOccupation(safe(user.getJoint_holder_occupation_code2()));
        jh2.setIncome(safe(user.getJoint_holder_annual_income_code2()));
        jh2.setSourceWealth(safe(user.getJoint_holder_source_of_wealth_code2()));
        jh2.setAddressType(safe(user.getJoint_holder_address_type_code2()));
        jh2.setPolitical(safe(user.getJoint_holder_political_code2()));
        dtoList.add(jh2);

        return dtoList;
    }
}
