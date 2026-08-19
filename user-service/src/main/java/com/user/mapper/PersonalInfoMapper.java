package com.user.mapper;

import com.user.dto.PersonalInfoDTO;
import com.user.model.User;
import com.user.model.UsersOnlineRegDetails;

public class PersonalInfoMapper
{
    public static UsersOnlineRegDetails dtoToUser(PersonalInfoDTO dto, UsersOnlineRegDetails user)
    {
        if (dto == null) return null;

        if(dto.getSource().equalsIgnoreCase("Website"))
        {
            user.setPan(dto.getPan());
            user.setBroker_code(dto.getBrokerCode());
            user.setNse_iin_number(dto.getInvestorCode());
            user.setTax_status_code(dto.getTaxStatusCode());
            user.setTax_status(dto.getTaxStatusDesc());
            user.setHolding_nature_code(dto.getHoldingNatureCode());
            user.setHolding_nature(dto.getHoldingNatureDesc());
        }

        user.setName(dto.getName());
        user.setDate_of_birth(dto.getDob());
        user.setGender(dto.getGender());
        user.setEmail(dto.getEmail());
        user.setFather_name(dto.getFatherName());
        user.setEmail_relation(dto.getEmailRelationCode());
        user.setMobile(dto.getMobile());
        user.setMobile_relation(dto.getMobileRelationCode());
        user.setPlace_of_birth(dto.getPlaceBirth());
        user.setCountry_of_birth(dto.getCountryBirthDesc());
        user.setCountry_birth_code(dto.getCountryBirthCode());
        user.setOccupation(dto.getOccupationDesc());
        user.setOccupation_code(dto.getOccupationCode());
        user.setAnnual_income(dto.getIncomeDesc());
        user.setAnnual_income_code(dto.getIncomeCode());
        user.setSource_of_wealth(dto.getSourceWealthDesc());
        user.setSource_of_wealth_code(dto.getSourceWealthCode());
        user.setPolitical(dto.getPoliticalStatusDesc());
        user.setPolitical_code(dto.getPoliticalStatusCode());
        user.setGuard_name(dto.getGuardName());
        user.setGuard_pan(dto.getGuardPan());
        user.setGuard_dob(dto.getGuardDob());
        user.setGuard_relationship(dto.getGuardRelation());
        user.setGuard_account_relation(dto.getGuardAccountRelation());
        user.setAddress_type_code(dto.getAddressTypeCode());
        user.setAddress_type(dto.getAddressTypeDesc());
        user.setNetworth_dob(dto.getNetworthDob());
        user.setNetworth_amount(dto.getNetworthAmount());
        return user;
    }

    public static PersonalInfoDTO userToDto(UsersOnlineRegDetails user) {
        if (user == null) return null;

        PersonalInfoDTO dto = new PersonalInfoDTO();
        dto.setName(user.getName());
        dto.setPan(user.getPan());
        dto.setFatherName(user.getFather_name());
        dto.setDob(user.getDate_of_birth());
        dto.setGender(user.getGender());
        dto.setEmail(user.getEmail());
        dto.setEmailRelationCode(user.getEmail_relation());
        dto.setMobile(user.getMobile());
        dto.setMobileRelationCode(user.getMobile_relation());
        dto.setBrokerCode(user.getBroker_code());
        dto.setTaxStatusCode(user.getTax_status_code());
        dto.setTaxStatusDesc(user.getTax_status());
        dto.setHoldingNatureCode(user.getHolding_nature_code());
        dto.setHoldingNatureDesc(user.getHolding_nature());
        dto.setPlaceBirth(user.getPlace_of_birth());
        dto.setCountryBirthDesc(user.getCountry_of_birth());
        dto.setCountryBirthCode(user.getCountry_birth_code());
        dto.setOccupationDesc(user.getOccupation());
        dto.setOccupationCode(user.getOccupation_code());
        dto.setIncomeDesc(user.getAnnual_income());
        dto.setIncomeCode(user.getAnnual_income_code());
        dto.setSourceWealthDesc(user.getSource_of_wealth());
        dto.setSourceWealthCode(user.getSource_of_wealth_code());
        dto.setPoliticalStatusDesc(user.getPolitical());
        dto.setPoliticalStatusCode(user.getPolitical_code());
        dto.setGuardName(user.getGuard_name());
        dto.setGuardPan(user.getGuard_pan());
        dto.setGuardDob(user.getGuard_dob());
        dto.setGuardRelation(user.getGuard_relationship());
        dto.setGuardAccountRelation(user.getGuard_account_relation());
        dto.setAddressTypeCode(user.getAddress_type_code());
        dto.setAddressTypeDesc(user.getAddress_type());
        dto.setNetworthDob(user.getNetworth_dob());
        dto.setNetworthAmount(user.getNetworth_amount());

        return dto;
    }

    public static UsersOnlineRegDetails dtoToUserBseNseDetails(PersonalInfoDTO dto, UsersOnlineRegDetails user)
    {
        if (dto == null) return null;
        user.setName(dto.getName());
        user.setDate_of_birth(dto.getDob());
        user.setGender(dto.getGender());
        user.setEmail(dto.getEmail());
        user.setFather_name(dto.getFatherName());
        user.setEmail_relation(dto.getEmailRelationCode());
        user.setMobile(dto.getMobile());
        user.setMobile_relation(dto.getMobileRelationCode());
        user.setPlace_of_birth(dto.getPlaceBirth());
        user.setCountry_of_birth(dto.getCountryBirthDesc());
        user.setCountry_birth_code(dto.getCountryBirthCode());
        user.setOccupation(dto.getOccupationDesc());
        user.setOccupation_code(dto.getOccupationCode());
        user.setAnnual_income(dto.getIncomeDesc());
        user.setAnnual_income_code(dto.getIncomeCode());
        user.setSource_of_wealth(dto.getSourceWealthDesc());
        user.setSource_of_wealth_code(dto.getSourceWealthCode());
        user.setPolitical(dto.getPoliticalStatusDesc());
        user.setPolitical_code(dto.getPoliticalStatusCode());
        user.setGuard_name(dto.getGuardName());
        user.setGuard_pan(dto.getGuardPan());
        user.setGuard_dob(dto.getGuardDob());
        user.setGuard_relationship(dto.getGuardRelation());
        user.setGuard_account_relation(dto.getGuardAccountRelation());
        user.setAddress_type_code(dto.getAddressTypeCode());
        user.setAddress_type(dto.getAddressTypeDesc());
        user.setNetworth_dob(dto.getNetworthDob());
        user.setNetworth_amount(dto.getNetworthAmount());
        return user;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static PersonalInfoDTO userBseNseDetailsToDto(UsersOnlineRegDetails user) {
        if (user == null) return null;

        PersonalInfoDTO dto = new PersonalInfoDTO();
        dto.setName(safe(user.getName()));
        dto.setPan(safe(user.getPan()));
        dto.setDob(safe(user.getDate_of_birth()));
        dto.setGender(safe(user.getGender()));
        dto.setEmail(safe(user.getEmail()));
        dto.setFatherName(safe(user.getFather_name()));
        dto.setEmailRelationCode(safe(user.getEmail_relation()));
        dto.setMobile(safe(user.getMobile()));
        dto.setMobileRelationCode(safe(user.getMobile_relation()));
        dto.setBrokerCode(safe(user.getBroker_code()));
        dto.setTaxStatusCode(safe(user.getTax_status_code()));
        dto.setTaxStatusDesc(safe(user.getTax_status()));
        dto.setHoldingNatureCode(safe(user.getHolding_nature_code()));
        dto.setHoldingNatureDesc(safe(user.getHolding_nature()));
        dto.setPlaceBirth(safe(user.getPlace_of_birth()));
        dto.setCountryBirthDesc(safe(user.getCountry_of_birth()));
        dto.setCountryBirthCode(safe(user.getCountry_birth_code()));
        dto.setOccupationDesc(safe(user.getOccupation()));
        dto.setOccupationCode(safe(user.getOccupation_code()));
        dto.setIncomeDesc(safe(user.getAnnual_income()));
        dto.setIncomeCode(safe(user.getAnnual_income_code()));
        dto.setSourceWealthDesc(safe(user.getSource_of_wealth()));
        dto.setSourceWealthCode(safe(user.getSource_of_wealth_code()));
        dto.setPoliticalStatusDesc(safe(user.getPolitical()));
        dto.setPoliticalStatusCode(safe(user.getPolitical_code()));
        dto.setGuardName(safe(user.getGuard_name()));
        dto.setGuardPan(safe(user.getGuard_pan()));
        dto.setGuardDob(safe(user.getGuard_dob()));
        dto.setGuardRelation(safe(user.getGuard_relationship()));
        dto.setGuardAccountRelation(safe(user.getGuard_account_relation()));
        dto.setAddressTypeCode(safe(user.getAddress_type_code()));
        dto.setAddressTypeDesc(safe(user.getAddress_type()));
        dto.setNetworthDob(safe(user.getNetworth_dob()));
        dto.setNetworthAmount(safe(user.getNetworth_amount()));

        return dto;
    }
}

