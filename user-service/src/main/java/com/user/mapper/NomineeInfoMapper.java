package com.user.mapper;

import com.user.dto.NomineeInfoDTO;
import com.user.model.User;
import com.user.model.UsersNomineeDetails;
import com.user.model.UsersOnlineRegDetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NomineeInfoMapper
{
    public static UsersNomineeDetails dtoToUser(List<NomineeInfoDTO> dtoList, UsersNomineeDetails user) {
        if (dtoList == null || user == null) return null;
        System.out.println("DtoList = " + dtoList + ", user = " + user);
        for (NomineeInfoDTO dto : dtoList)
        {
            if (dto.getId() == null) continue;

            int id = dto.getId();

            if (id == 1)
            {
                user.setNominee1_name(dto.getName());
                user.setNominee1_dob(dto.getDob());
                user.setNominee1_type(dto.getType());
                user.setNominee1_type_desc(dto.getTypeDesc());
                user.setNominee1_percentage(dto.getPercentage());
                user.setNominee1_address1(dto.getAddress1());
                user.setNominee1_address2(dto.getAddress2());
                user.setNominee1_address3(dto.getAddress3());
                user.setNominee1_pincode(dto.getPincode());
                user.setNominee1_city(dto.getCity());
                user.setNominee1_state(dto.getState());
                user.setNominee1_state_code(dto.getStateCode());
                user.setNominee1_country(dto.getCountry());
                user.setNominee1_email(dto.getEmail());
                user.setNominee1_mobile(dto.getMobile());
                user.setNominee1_relation(dto.getRelation());
                user.setNominee1_id_type(dto.getIdType());
                user.setNominee1_id_no(dto.getIdNo());
                user.setNominee1_guard_name(dto.getGuardName());
                user.setNominee1_guard_pan(dto.getGuardPan());
                user.setNominee1_guard_relationship(dto.getGuardRelation());
            } else if (id == 2) {
                user.setNominee2_name(dto.getName());
                user.setNominee2_dob(dto.getDob());
                user.setNominee2_type(dto.getType());
                user.setNominee2_percentage(dto.getPercentage());
                user.setNominee2_type_desc(dto.getTypeDesc());
                user.setNominee2_address1(dto.getAddress1());
                user.setNominee2_pincode(dto.getPincode());
                user.setNominee2_city(dto.getCity());
                user.setNominee2_state(dto.getState());
                user.setNominee2_state_code(dto.getStateCode());
                user.setNominee2_country(dto.getCountry());
                user.setNominee2_email(dto.getEmail());
                user.setNominee2_mobile(dto.getMobile());
                user.setNominee2_relation(dto.getRelation());
                user.setNominee2_id_type(dto.getIdType());
                user.setNominee2_id_no(dto.getIdNo());
                user.setNominee2_guard_name(dto.getGuardName());
                user.setNominee2_guard_pan(dto.getGuardPan());
                user.setNominee2_guard_relationship(dto.getGuardRelation());
            } else if (id == 3) {
                user.setNominee3_name(dto.getName());
                user.setNominee3_dob(dto.getDob());
                user.setNominee3_type(dto.getType());
                user.setNominee3_type_desc(dto.getTypeDesc());
                user.setNominee3_percentage(dto.getPercentage());
                user.setNominee3_address1(dto.getAddress1());
                user.setNominee3_pincode(dto.getPincode());
                user.setNominee3_city(dto.getCity());
                user.setNominee3_state(dto.getState());
                user.setNominee3_state_code(dto.getStateCode());
                user.setNominee3_country(dto.getCountry());
                user.setNominee3_email(dto.getEmail());
                user.setNominee3_mobile(dto.getMobile());
                user.setNominee3_relation(dto.getRelation());
                user.setNominee3_id_type(dto.getIdType());
                user.setNominee3_id_no(dto.getIdNo());
                user.setNominee3_guard_name(dto.getGuardName());
                user.setNominee3_guard_pan(dto.getGuardPan());
                user.setNominee3_guard_relationship(dto.getGuardRelation());
            }
        }

        return user;
    }

    public static List<NomineeInfoDTO> userToDto(UsersNomineeDetails user)
    {
        if (user == null) return Collections.emptyList();

        List<NomineeInfoDTO> dtoList = new ArrayList<>();

        // Nominee 1
        NomineeInfoDTO nominee1 = new NomineeInfoDTO();
        nominee1.setId(1);
        nominee1.setName(user.getNominee1_name());
        nominee1.setDob(user.getNominee1_dob());
        nominee1.setType(user.getNominee1_type());
        nominee1.setTypeDesc(user.getNominee1_type_desc());
        nominee1.setPercentage(user.getNominee1_percentage());
        nominee1.setAddress1(user.getNominee1_address1());
        nominee1.setAddress2(user.getNominee1_address2());
        nominee1.setAddress3(user.getNominee1_address3());
        nominee1.setPincode(user.getNominee1_pincode());
        nominee1.setCity(user.getNominee1_city());
        nominee1.setState(user.getNominee1_state());
        nominee1.setStateCode(user.getNominee1_state_code());
        nominee1.setCountry(user.getNominee1_country());
        nominee1.setEmail(user.getNominee1_email());
        nominee1.setMobile(user.getNominee1_mobile());
        nominee1.setRelation(user.getNominee1_relation());
        nominee1.setIdType(user.getNominee1_id_type());
        nominee1.setIdNo(user.getNominee1_id_no());
        nominee1.setGuardName(user.getNominee1_guard_name());
        nominee1.setGuardPan(user.getNominee1_guard_pan());
        nominee1.setGuardRelation(user.getNominee1_guard_relationship());
        dtoList.add(nominee1);

        // Nominee 2
        NomineeInfoDTO nominee2 = new NomineeInfoDTO();
        nominee2.setId(2);
        nominee2.setName(user.getNominee2_name());
        nominee2.setDob(user.getNominee2_dob());
        nominee2.setType(user.getNominee2_type());
        nominee2.setTypeDesc(user.getNominee2_type_desc());
        nominee2.setPercentage(user.getNominee2_percentage());
        nominee2.setAddress1(user.getNominee2_address1());
        nominee2.setAddress2("");
        nominee2.setAddress3("");
        nominee2.setPincode(user.getNominee2_pincode());
        nominee2.setCity(user.getNominee2_city());
        nominee2.setState(user.getNominee2_state());
        nominee2.setStateCode(user.getNominee2_state_code());
        nominee2.setCountry(user.getNominee2_country());
        nominee2.setEmail(user.getNominee2_email());
        nominee2.setMobile(user.getNominee2_mobile());
        nominee2.setRelation(user.getNominee2_relation());
        nominee2.setIdType(user.getNominee2_id_type());
        nominee2.setIdNo(user.getNominee2_id_no());
        nominee2.setGuardName(user.getNominee2_guard_name());
        nominee2.setGuardPan(user.getNominee2_guard_pan());
        nominee2.setGuardRelation(user.getNominee2_guard_relationship());
        dtoList.add(nominee2);

        // Nominee 3
        NomineeInfoDTO nominee3 = new NomineeInfoDTO();
        nominee3.setId(3);
        nominee3.setName(user.getNominee3_name());
        nominee3.setDob(user.getNominee3_dob());
        nominee3.setType(user.getNominee3_type());
        nominee3.setTypeDesc(user.getNominee3_type_desc());
        nominee3.setPercentage(user.getNominee3_percentage());
        nominee3.setAddress1(user.getNominee3_address1());
        nominee3.setAddress2("");
        nominee3.setAddress3("");
        nominee3.setPincode(user.getNominee3_pincode());
        nominee3.setCity(user.getNominee3_city());
        nominee3.setState(user.getNominee3_state());
        nominee3.setStateCode(user.getNominee3_state_code());
        nominee3.setCountry(user.getNominee3_country());
        nominee3.setEmail(user.getNominee3_email());
        nominee3.setMobile(user.getNominee3_mobile());
        nominee3.setRelation(user.getNominee3_relation());
        nominee3.setIdType(user.getNominee3_id_type());
        nominee3.setIdNo(user.getNominee3_id_no());
        nominee3.setGuardName(user.getNominee3_guard_name());
        nominee3.setGuardPan(user.getNominee3_guard_pan());
        nominee3.setGuardRelation(user.getNominee3_guard_relationship());
        dtoList.add(nominee3);

        return dtoList;
    }

    public static UsersNomineeDetails dtoToUserBseNseDetails(List<NomineeInfoDTO> dtoList, UsersNomineeDetails user) {
        if (dtoList == null || user == null) return null;

        for (NomineeInfoDTO dto : dtoList)
        {
            if (dto.getId() == null) continue;

            int id = dto.getId();

            if (id == 1)
            {
                user.setNominee1_name(dto.getName());
                user.setNominee1_dob(dto.getDob());
                user.setNominee1_type(dto.getType());
                user.setNominee1_type_desc(dto.getNomineeTypeDesc());
                user.setNominee1_percentage(dto.getPercentage());
                user.setNominee1_address1(dto.getAddress1());
                user.setNominee1_address2(dto.getAddress2());
                user.setNominee1_address3(dto.getAddress3());
                user.setNominee1_pincode(dto.getPincode());
                user.setNominee1_city(dto.getCity());
                user.setNominee1_state(dto.getState());
                user.setNominee1_state_code(dto.getStateCode());
                user.setNominee1_type_desc(dto.getNomineeTypeDesc());
                user.setNominee1_country(dto.getCountry());
                user.setNominee1_email(dto.getEmail());
                user.setNominee1_mobile(dto.getMobile());
                user.setNominee1_relation(dto.getRelation());
                user.setNominee1_id_type(dto.getIdType());
                user.setNominee1_id_no(dto.getIdNo());
                user.setNominee1_guard_name(dto.getGuardName());
                user.setNominee1_guard_pan(dto.getGuardPan());
                user.setNominee1_guard_relationship(dto.getGuardRelation());
            } else if (id == 2) {
                user.setNominee2_name(dto.getName());
                user.setNominee2_dob(dto.getDob());
                user.setNominee2_type_desc(dto.getNomineeTypeDesc());
                user.setNominee2_percentage(dto.getPercentage());
                user.setNominee2_address1(dto.getAddress1());
                user.setNominee2_pincode(dto.getPincode());
                user.setNominee2_city(dto.getCity());
                user.setNominee2_state(dto.getState());
                user.setNominee2_state_code(dto.getStateCode());
                user.setNominee2_country(dto.getCountry());
                user.setNominee2_email(dto.getEmail());
                user.setNominee2_mobile(dto.getMobile());
                user.setNominee2_relation(dto.getRelation());
                user.setNominee2_type_desc(dto.getNomineeTypeDesc());
                user.setNominee2_id_type(dto.getIdType());
                user.setNominee2_id_no(dto.getIdNo());
                user.setNominee2_guard_name(dto.getGuardName());
                user.setNominee2_guard_pan(dto.getGuardPan());
                user.setNominee2_guard_relationship(dto.getGuardRelation());
            } else if (id == 3) {
                user.setNominee3_name(dto.getName());
                user.setNominee3_dob(dto.getDob());
                user.setNominee3_type(dto.getType());
                user.setNominee3_type_desc(dto.getNomineeTypeDesc());
                user.setNominee3_percentage(dto.getPercentage());
                user.setNominee3_address1(dto.getAddress1());
                user.setNominee3_pincode(dto.getPincode());
                user.setNominee3_city(dto.getCity());
                user.setNominee3_state(dto.getState());
                user.setNominee3_state_code(dto.getStateCode());
                user.setNominee3_country(dto.getCountry());
                user.setNominee3_email(dto.getEmail());
                user.setNominee3_mobile(dto.getMobile());
                user.setNominee3_relation(dto.getRelation());
                user.setNominee3_type_desc(dto.getNomineeTypeDesc());
                user.setNominee3_id_type(dto.getIdType());
                user.setNominee3_id_no(dto.getIdNo());
                user.setNominee3_guard_name(dto.getGuardName());
                user.setNominee3_guard_pan(dto.getGuardPan());
                user.setNominee3_guard_relationship(dto.getGuardRelation());
            }
        }

        return user;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static List<NomineeInfoDTO> userBseNseDetailsToDto(UsersNomineeDetails user) {
        if (user == null) return Collections.emptyList();
        System.out.println("user = " + user.getNominee1_id_no());
        List<NomineeInfoDTO> dtoList = new ArrayList<>();

        // Nominee 1
        NomineeInfoDTO nominee1 = new NomineeInfoDTO();
        nominee1.setId(1);
        nominee1.setName(safe(user.getNominee1_name()));
        nominee1.setDob(safe(user.getNominee1_dob()));
        nominee1.setType(safe(user.getNominee1_type()));
        nominee1.setTypeDesc(safe(user.getNominee1_type_desc()));
        nominee1.setPercentage(safe(user.getNominee1_percentage()));
        nominee1.setAddress1(safe(user.getNominee1_address1()));
        nominee1.setAddress2(safe(user.getNominee1_address2()));
        nominee1.setAddress3(safe(user.getNominee1_address3()));
        nominee1.setPincode(safe(user.getNominee1_pincode()));
        nominee1.setCity(safe(user.getNominee1_city()));
        nominee1.setState(safe(user.getNominee1_state()));
        nominee1.setStateCode(safe(user.getNominee1_state_code()));
        nominee1.setCountry(safe(user.getNominee1_country()));
        nominee1.setEmail(safe(user.getNominee1_email()));
        nominee1.setMobile(safe(user.getNominee1_mobile()));
        nominee1.setRelation(safe(user.getNominee1_relation()));
        nominee1.setIdType(safe(user.getNominee1_id_type()));
        nominee1.setIdNo(safe(user.getNominee1_id_no()));
        nominee1.setGuardName(safe(user.getNominee1_guard_name()));
        nominee1.setGuardPan(safe(user.getNominee1_guard_pan()));
        nominee1.setGuardRelation(safe(user.getNominee1_guard_relationship()));
        dtoList.add(nominee1);

        // Nominee 2
        NomineeInfoDTO nominee2 = new NomineeInfoDTO();
        nominee2.setId(2);
        nominee2.setName(safe(user.getNominee2_name()));
        nominee2.setDob(safe(user.getNominee2_dob()));
        nominee2.setType(safe(user.getNominee2_type()));
        nominee2.setTypeDesc(safe(user.getNominee2_type_desc()));
        nominee2.setPercentage(safe(user.getNominee2_percentage()));
        nominee2.setAddress1(safe(user.getNominee2_address1()));
        nominee2.setAddress2(safe(user.getNominee2_address2()));
        nominee2.setAddress3(safe(user.getNominee2_address3()));
        nominee2.setPincode(safe(user.getNominee2_pincode()));
        nominee2.setCity(safe(user.getNominee2_city()));
        nominee2.setState(safe(user.getNominee2_state()));
        nominee2.setStateCode(safe(user.getNominee2_state_code()));
        nominee2.setCountry(safe(user.getNominee2_country()));
        nominee2.setEmail(safe(user.getNominee2_email()));
        nominee2.setMobile(safe(user.getNominee2_mobile()));
        nominee2.setRelation(safe(user.getNominee2_relation()));
        nominee2.setIdType(safe(user.getNominee2_id_type()));
        nominee2.setIdNo(safe(user.getNominee2_id_no()));
        nominee2.setGuardName(safe(user.getNominee2_guard_name()));
        nominee2.setGuardPan(safe(user.getNominee2_guard_pan()));
        nominee2.setGuardRelation(safe(user.getNominee2_guard_relationship()));
        dtoList.add(nominee2);

        // Nominee 3
        NomineeInfoDTO nominee3 = new NomineeInfoDTO();
        nominee3.setId(3);
        nominee3.setName(safe(user.getNominee3_name()));
        nominee3.setDob(safe(user.getNominee3_dob()));
        nominee3.setType(safe(user.getNominee3_type()));
        nominee3.setTypeDesc(safe(user.getNominee3_type_desc()));
        nominee3.setPercentage(safe(user.getNominee3_percentage()));
        nominee3.setAddress1(safe(user.getNominee3_address1()));
        nominee3.setAddress2(safe(user.getNominee3_address2()));
        nominee3.setAddress3(safe(user.getNominee3_address3()));
        nominee3.setPincode(safe(user.getNominee3_pincode()));
        nominee3.setCity(safe(user.getNominee3_city()));
        nominee3.setState(safe(user.getNominee3_state()));
        nominee3.setStateCode(safe(user.getNominee3_state_code()));
        nominee3.setCountry(safe(user.getNominee3_country()));
        nominee3.setEmail(safe(user.getNominee3_email()));
        nominee3.setMobile(safe(user.getNominee3_mobile()));
        nominee3.setRelation(safe(user.getNominee3_relation()));
        nominee3.setIdType(safe(user.getNominee3_id_type()));
        nominee3.setIdNo(safe(user.getNominee3_id_no()));
        nominee3.setGuardName(safe(user.getNominee3_guard_name()));
        nominee3.setGuardPan(safe(user.getNominee3_guard_pan()));
        nominee3.setGuardRelation(safe(user.getNominee3_guard_relationship()));
        dtoList.add(nominee3);

        return dtoList;
    }
}
