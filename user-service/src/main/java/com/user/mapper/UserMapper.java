package com.user.mapper;

import com.user.dto.UserDto;
import com.user.model.*;
import com.user.utils.UserUtils;

import java.util.Date;
import java.util.List;

public class UserMapper
{
    public static UserDto mapToUserDtoMapper(UsersOnlineRegDetails userDetails, List<UsersBankDetails> bankDetailsList, List<UsersMandateDetails> mandateDetailsList, UsersNomineeDetails nomineeDetails)
    {
        UserDto userDto = new UserDto();
        userDto.setId(userDetails.getId());
        userDto.setUser_id(userDetails.getUser_id());
        userDto.setName(UserUtils.checkParem(userDetails.getName()));
        userDto.setPan(UserUtils.checkParem(userDetails.getPan()));
        userDto.setMobile(UserUtils.checkParem(userDetails.getMobile()));
        userDto.setMobile_isd_code(UserUtils.checkParem(userDetails.getMobile_isd_code()));
        userDto.setAlter_mobile(UserUtils.checkParem(userDetails.getAlter_mobile()));
        userDto.setEmail(UserUtils.checkParem(userDetails.getEmail()));
        userDto.setAlter_email(UserUtils.checkParem(userDetails.getAlter_email()));
        userDto.setStreet_1(UserUtils.checkParem(userDetails.getStreet_1()));
        userDto.setStreet_2(UserUtils.checkParem(userDetails.getStreet_2()));
        userDto.setStreet_3(UserUtils.checkParem(userDetails.getStreet_3()));
        userDto.setCity(UserUtils.checkParem(userDetails.getCity()));
        userDto.setPincode(UserUtils.checkParem(userDetails.getPincode()));
        userDto.setState(UserUtils.checkParem(userDetails.getState()));
        userDto.setState_code(UserUtils.checkParem(userDetails.getState_code()));
        userDto.setCountry(UserUtils.checkParem(userDetails.getCountry()));
        userDto.setFather_name(UserUtils.checkParem(userDetails.getFather_name()));
        userDto.setGender(UserUtils.checkParem(userDetails.getGender()));
        userDto.setDate_of_birth(UserUtils.checkParem(userDetails.getDate_of_birth()));
        userDto.setPlace_of_birth(UserUtils.checkParem(userDetails.getPlace_of_birth()));
        userDto.setCountry_of_birth(UserUtils.checkParem(userDetails.getCountry_of_birth()));
        userDto.setCountry_birth_code(UserUtils.checkParem(userDetails.getCountry_birth_code()));
        userDto.setPhone_office(UserUtils.checkParem(userDetails.getPhone_office()));
        userDto.setPhone_residence(UserUtils.checkParem(userDetails.getPhone_residence()));
        userDto.setName(UserUtils.checkParem(userDetails.getName()));
        userDto.setInv_category(UserUtils.checkParem(userDetails.getInv_category()));
        userDto.setTax_status(UserUtils.checkParem(userDetails.getTax_status()));
        userDto.setTax_status_code(UserUtils.checkParem(userDetails.getTax_status_code()));
        userDto.setHolding_nature(UserUtils.checkParem(userDetails.getHolding_nature()));
        userDto.setHolding_nature_code(UserUtils.checkParem(userDetails.getHolding_nature_code()));
        userDto.setOccupation(UserUtils.checkParem(userDetails.getOccupation()));
        userDto.setOccupation_code(UserUtils.checkParem(userDetails.getOccupation_code()));
        userDto.setAnnual_income(UserUtils.checkParem(userDetails.getAnnual_income()));
        userDto.setAnnual_income_code(UserUtils.checkParem(userDetails.getAnnual_income_code()));
        userDto.setSource_of_wealth(UserUtils.checkParem(userDetails.getSource_of_wealth()));
        userDto.setSource_of_wealth_code(UserUtils.checkParem(userDetails.getSource_of_wealth_code()));
        userDto.setPolitical(UserUtils.checkParem(userDetails.getPolitical()));
        userDto.setPolitical_code(UserUtils.checkParem(userDetails.getPolitical_code()));
        userDto.setNetworth_amount(UserUtils.checkParem(userDetails.getNetworth_amount()));
        userDto.setNetworth_dob(UserUtils.checkParem(userDetails.getNetworth_dob()));
        userDto.setAddress_type(UserUtils.checkParem(userDetails.getAddress_type()));
        userDto.setAddress_type_code(UserUtils.checkParem(userDetails.getAddress_type_code()));
        userDto.setGuard_name(UserUtils.checkParem(userDetails.getGuard_name()));
        userDto.setGuard_middle_name(UserUtils.checkParem(userDetails.getGuard_middle_name()));
        userDto.setGuard_last_name(UserUtils.checkParem(userDetails.getGuard_last_name()));
        userDto.setGuard_pan(UserUtils.checkParem(userDetails.getGuard_pan()));
        userDto.setGuard_dob(UserUtils.checkParem(userDetails.getGuard_dob()));
        userDto.setGuard_gender(UserUtils.checkParem(userDetails.getGuard_gender()));
        userDto.setGuard_mobile(UserUtils.checkParem(userDetails.getGuard_mobile()));
        userDto.setGuard_email(UserUtils.checkParem(userDetails.getGuard_email()));
        userDto.setGuard_relationship(UserUtils.checkParem(userDetails.getGuard_relationship()));
        userDto.setGuard_relation_proof(UserUtils.checkParem(userDetails.getGuard_relation_proof()));
        userDto.setGuard_account_relation(UserUtils.checkParem(userDetails.getGuard_account_relation()));
        userDto.setJoint_holder_name1(UserUtils.checkParem(userDetails.getJoint_holder_name1()));
        userDto.setJoint_holder_pan1(UserUtils.checkParem(userDetails.getJoint_holder_pan1()));
        userDto.setJoint_holder_dob1(UserUtils.checkParem(userDetails.getJoint_holder_dob1()));
        userDto.setJoint_holder_email1(UserUtils.checkParem(userDetails.getJoint_holder_email1()));
        userDto.setJoint_holder_email_relation1(UserUtils.checkParem(userDetails.getJoint_holder_email_relation1()));
        userDto.setJoint_holder_mobile1(UserUtils.checkParem(userDetails.getJoint_holder_mobile1()));
        userDto.setJoint_holder_mobile1_isd_code(UserUtils.checkParem(userDetails.getJoint_holder_mobile1_isd_code()));
        userDto.setJoint_holder_mobile_relation1(UserUtils.checkParem(userDetails.getJoint_holder_mobile_relation1()));
        userDto.setJoint_holder_signature1(UserUtils.checkParem(userDetails.getJoint_holder_signature1()));
        userDto.setJoint_holder_place_of_birth1(UserUtils.checkParem(userDetails.getJoint_holder_place_of_birth1()));
        userDto.setJoint_holder_country_birth_code1(UserUtils.checkParem(userDetails.getJoint_holder_country_birth_code1()));
        userDto.setJoint_holder_occupation_code1(UserUtils.checkParem(userDetails.getJoint_holder_occupation_code1()));
        userDto.setJoint_holder_annual_income_code1(UserUtils.checkParem(userDetails.getJoint_holder_annual_income_code1()));
        userDto.setJoint_holder_source_of_wealth_code1(UserUtils.checkParem(userDetails.getJoint_holder_source_of_wealth_code1()));
        userDto.setJoint_holder_political_code1(UserUtils.checkParem(userDetails.getJoint_holder_political_code1()));
        userDto.setJoint_holder_address_type_code1(UserUtils.checkParem(userDetails.getJoint_holder_address_type_code1()));
        userDto.setJoint_holder_name2(UserUtils.checkParem(userDetails.getJoint_holder_name2()));
        userDto.setJoint_holder_pan2(UserUtils.checkParem(userDetails.getJoint_holder_pan2()));
        userDto.setJoint_holder_dob2(UserUtils.checkParem(userDetails.getJoint_holder_dob2()));
        userDto.setJoint_holder_email2(UserUtils.checkParem(userDetails.getJoint_holder_email2()));
        userDto.setJoint_holder_email_relation2(UserUtils.checkParem(userDetails.getJoint_holder_email_relation2()));
        userDto.setJoint_holder_mobile2(UserUtils.checkParem(userDetails.getJoint_holder_mobile2()));
        userDto.setJoint_holder_mobile2_isd_code(UserUtils.checkParem(userDetails.getJoint_holder_mobile2_isd_code()));
        userDto.setJoint_holder_mobile_relation2(UserUtils.checkParem(userDetails.getJoint_holder_mobile_relation2()));
        userDto.setJoint_holder_signature2(UserUtils.checkParem(userDetails.getJoint_holder_signature2()));
        userDto.setJoint_holder_place_of_birth2(UserUtils.checkParem(userDetails.getJoint_holder_place_of_birth2()));
        userDto.setJoint_holder_country_birth_code2(UserUtils.checkParem(userDetails.getJoint_holder_country_birth_code2()));
        userDto.setJoint_holder_occupation_code2(UserUtils.checkParem(userDetails.getJoint_holder_occupation_code2()));
        userDto.setJoint_holder_annual_income_code2(UserUtils.checkParem(userDetails.getJoint_holder_annual_income_code2()));
        userDto.setJoint_holder_source_of_wealth_code2(UserUtils.checkParem(userDetails.getJoint_holder_source_of_wealth_code2()));
        userDto.setJoint_holder_political_code2(UserUtils.checkParem(userDetails.getJoint_holder_political_code2()));
        userDto.setJoint_holder_address_type_code2(UserUtils.checkParem(userDetails.getJoint_holder_address_type_code2()));
        userDto.setNri_address1(UserUtils.checkParem(userDetails.getNri_address1()));
        userDto.setNri_address2(UserUtils.checkParem(userDetails.getNri_address2()));
        userDto.setNri_address3(UserUtils.checkParem(userDetails.getNri_address3()));
        userDto.setNri_city(UserUtils.checkParem(userDetails.getNri_city()));
        userDto.setNri_state(UserUtils.checkParem(userDetails.getNri_state()));
        userDto.setNri_pincode(UserUtils.checkParem(userDetails.getNri_pincode()));
        userDto.setNri_country(UserUtils.checkParem(userDetails.getNri_country()));
        userDto.setEmail_verified(userDetails.getEmail_verified());
        userDto.setEmail_authcode(UserUtils.checkParem(userDetails.getEmail_authcode()));
        userDto.setMobile_verified(userDetails.getMobile_verified());
        userDto.setMobile_otp(UserUtils.checkParem(userDetails.getMobile_otp()));
        userDto.setNse_customer(userDetails.getNse_customer());
        userDto.setNse_active(userDetails.getNse_active());
        userDto.setNse_iin_number(UserUtils.checkParem(userDetails.getNse_iin_number()));
        userDto.setMobile_relation(UserUtils.checkParem(userDetails.getMobile_relation()));
        userDto.setEmail_relation(UserUtils.checkParem(userDetails.getEmail_relation()));
        userDto.setRegister_source(UserUtils.checkParem(userDetails.getRegister_source()));
        userDto.setSalutation(UserUtils.checkParem(userDetails.getSalutation()));
        userDto.setClient_name(UserUtils.checkParem(userDetails.getClient_name()));
        userDto.setBroker_code(UserUtils.checkParem(userDetails.getBroker_code()));
        userDto.setEuin(UserUtils.checkParem(userDetails.getEuin()));
        userDto.setOnline_flag(UserUtils.checkParem(userDetails.getOnline_flag()));
        userDto.setFirst_name(UserUtils.checkParem(userDetails.getFirst_name()));
        userDto.setMiddle_name(UserUtils.checkParem(userDetails.getMiddle_name()));
        userDto.setLast_name(UserUtils.checkParem(userDetails.getLast_name()));
        userDto.setJoint_holder_middle_name1(UserUtils.checkParem(userDetails.getJoint_holder_middle_name1()));
        userDto.setJoint_holder_last_name1(UserUtils.checkParem(userDetails.getJoint_holder_last_name1()));
        userDto.setJoint_holder_middle_name2(UserUtils.checkParem(userDetails.getJoint_holder_middle_name2()));
        userDto.setJoint_holder_last_name2(UserUtils.checkParem(userDetails.getJoint_holder_last_name2()));
        userDto.setCreated_date(userDetails.getCreated_date());

        // Map bank details (up to 3 banks)
        if (bankDetailsList != null && !bankDetailsList.isEmpty())
        {
            if (bankDetailsList.size() >= 1)
            {
                UsersBankDetails bank1 = bankDetailsList.get(0);
                userDto.setBank_name1(UserUtils.checkParem(bank1.getBank_name()));
                userDto.setBank_branch1(UserUtils.checkParem(bank1.getBank_branch()));
                userDto.setBank_address1(UserUtils.checkParem(bank1.getBank_address()));
                userDto.setBank_account_number1(UserUtils.checkParem(bank1.getBank_account_number()));
                userDto.setBank_account_holder_name1(UserUtils.checkParem(bank1.getBank_account_holder_name()));
                userDto.setBank_account_type1(UserUtils.checkParem(bank1.getBank_account_type()));
                userDto.setBank_ifsc_code1(UserUtils.checkParem(bank1.getBank_ifsc_code()));
                userDto.setBank_micr_code1(UserUtils.checkParem(bank1.getBank_micr_code()));
                userDto.setBank_proof1(UserUtils.checkParem(bank1.getBank_proof()));
                userDto.setBank_code1(UserUtils.checkParem(bank1.getBank_code()));

                if (mandateDetailsList != null && !mandateDetailsList.isEmpty())
                {
                    UsersMandateDetails userMandate = mandateDetailsList.stream().filter(mandate -> mandate.getBank_account_number().equalsIgnoreCase(bank1.getBank_account_number())).findFirst().orElse(null);

                    if(userMandate != null)
                    {
                        userDto.setXsip_otm_flag1(userMandate.getXsip_otm_flag());
                        userDto.setXsip_otm1(UserUtils.checkParem(userMandate.getXsip_otm()));
                        userDto.setXsip_otm_approved1(userMandate.getXsip_otm_approved());
                        userDto.setXsip_otm_rej_reason1(UserUtils.checkParem(userMandate.getXsip_otm_rej_reason()));
                        userDto.setXsip_otm_created_date1(userMandate.getXsip_otm_created_date());
                        userDto.setXsip_otm_amount1(UserUtils.checkParem(userMandate.getXsip_otm_amount()));

                        userDto.setEmandate_otm_flag1(userMandate.getEmandate_otm_flag());
                        userDto.setEmandate_otm1(UserUtils.checkParem(userMandate.getEmandate_otm()));
                        userDto.setEmandate_otm_approved1(userMandate.getEmandate_otm_approved());
                        userDto.setEmandate_otm_rej_reason1(UserUtils.checkParem(userMandate.getEmandate_otm_rej_reason()));
                        userDto.setEmandate_otm_created_date1(userMandate.getEmandate_otm_created_date());
                        userDto.setEmandate_otm_amount1(UserUtils.checkParem(userMandate.getEmandate_otm_amount()));

                        userDto.setNse_ach_flag1(userMandate.getNse_ach_flag());
                        userDto.setNse_ach1(UserUtils.checkParem(userMandate.getNse_ach()));
                        userDto.setNse_ach_amount1(UserUtils.checkParem(userMandate.getNse_ach_amount()));
                        userDto.setNse_ach_approved1(userMandate.getNse_ach_approved());
                        userDto.setNse_ach_rej_reason1(UserUtils.checkParem(userMandate.getNse_ach_rej_reason()));
                        userDto.setNse_ach_created_date1(userMandate.getNse_ach_created_date());

                    }
                }
            }

            if (bankDetailsList.size() >= 2)
            {
                UsersBankDetails bank2 = bankDetailsList.get(1);
                userDto.setBank_name2(UserUtils.checkParem(bank2.getBank_name()));
                userDto.setBank_branch2(UserUtils.checkParem(bank2.getBank_branch()));
                userDto.setBank_address2(UserUtils.checkParem(bank2.getBank_address()));
                userDto.setBank_account_number2(UserUtils.checkParem(bank2.getBank_account_number()));
                userDto.setBank_account_holder_name2(UserUtils.checkParem(bank2.getBank_account_holder_name()));
                userDto.setBank_account_type2(UserUtils.checkParem(bank2.getBank_account_type()));
                userDto.setBank_ifsc_code2(UserUtils.checkParem(bank2.getBank_ifsc_code()));
                userDto.setBank_micr_code2(UserUtils.checkParem(bank2.getBank_micr_code()));
                userDto.setBank_proof2(UserUtils.checkParem(bank2.getBank_proof()));
                userDto.setBank_code2(UserUtils.checkParem(bank2.getBank_code()));

                if (mandateDetailsList != null && !mandateDetailsList.isEmpty())
                {
                    UsersMandateDetails userMandate = mandateDetailsList.stream().filter(mandate -> mandate.getBank_account_number().equalsIgnoreCase(bank2.getBank_account_number())).findFirst().orElse(null);

                    if(userMandate != null)
                    {
                        userDto.setXsip_otm_flag2(userMandate.getXsip_otm_flag());
                        userDto.setXsip_otm2(UserUtils.checkParem(userMandate.getXsip_otm()));
                        userDto.setXsip_otm_approved2(userMandate.getXsip_otm_approved());
                        userDto.setXsip_otm_rej_reason2(UserUtils.checkParem(userMandate.getXsip_otm_rej_reason()));
                        userDto.setXsip_otm_created_date2(userMandate.getXsip_otm_created_date());
                        userDto.setXsip_otm_amount2(UserUtils.checkParem(userMandate.getXsip_otm_amount()));

                        userDto.setEmandate_otm_flag2(userMandate.getEmandate_otm_flag());
                        userDto.setEmandate_otm2(UserUtils.checkParem(userMandate.getEmandate_otm()));
                        userDto.setEmandate_otm_approved2(userMandate.getEmandate_otm_approved());
                        userDto.setEmandate_otm_rej_reason2(UserUtils.checkParem(userMandate.getEmandate_otm_rej_reason()));
                        userDto.setEmandate_otm_created_date2(userMandate.getEmandate_otm_created_date());
                        userDto.setEmandate_otm_amount2(UserUtils.checkParem(userMandate.getEmandate_otm_amount()));

                        userDto.setNse_ach_flag2(userMandate.getNse_ach_flag());
                        userDto.setNse_ach2(UserUtils.checkParem(userMandate.getNse_ach()));
                        userDto.setNse_ach_amount2(UserUtils.checkParem(userMandate.getNse_ach_amount()));
                        userDto.setNse_ach_approved2(userMandate.getNse_ach_approved());
                        userDto.setNse_ach_rej_reason2(UserUtils.checkParem(userMandate.getNse_ach_rej_reason()));
                        userDto.setNse_ach_created_date2(userMandate.getNse_ach_created_date());

                    }
                }
            }

            if (bankDetailsList.size() >= 3)
            {
                UsersBankDetails bank3 = bankDetailsList.get(2);
                userDto.setBank_name3(UserUtils.checkParem(bank3.getBank_name()));
                userDto.setBank_branch3(UserUtils.checkParem(bank3.getBank_branch()));
                userDto.setBank_address3(UserUtils.checkParem(bank3.getBank_address()));
                userDto.setBank_account_number3(UserUtils.checkParem(bank3.getBank_account_number()));
                userDto.setBank_account_holder_name3(UserUtils.checkParem(bank3.getBank_account_holder_name()));
                userDto.setBank_account_type3(UserUtils.checkParem(bank3.getBank_account_type()));
                userDto.setBank_ifsc_code3(UserUtils.checkParem(bank3.getBank_ifsc_code()));
                userDto.setBank_micr_code3(UserUtils.checkParem(bank3.getBank_micr_code()));
                userDto.setBank_proof3(UserUtils.checkParem(bank3.getBank_proof()));
                userDto.setBank_code3(UserUtils.checkParem(bank3.getBank_code()));

                if (mandateDetailsList != null && !mandateDetailsList.isEmpty())
                {
                    UsersMandateDetails userMandate = mandateDetailsList.stream().filter(mandate -> mandate.getBank_account_number().equalsIgnoreCase(bank3.getBank_account_number())).findFirst().orElse(null);

                    if(userMandate != null)
                    {
                        userDto.setXsip_otm_flag3(userMandate.getXsip_otm_flag());
                        userDto.setXsip_otm3(UserUtils.checkParem(userMandate.getXsip_otm()));
                        userDto.setXsip_otm_approved3(userMandate.getXsip_otm_approved());
                        userDto.setXsip_otm_rej_reason3(UserUtils.checkParem(userMandate.getXsip_otm_rej_reason()));
                        userDto.setXsip_otm_created_date3(userMandate.getXsip_otm_created_date());
                        userDto.setXsip_otm_amount3(UserUtils.checkParem(userMandate.getXsip_otm_amount()));

                        userDto.setEmandate_otm_flag3(userMandate.getEmandate_otm_flag());
                        userDto.setEmandate_otm3(UserUtils.checkParem(userMandate.getEmandate_otm()));
                        userDto.setEmandate_otm_approved3(userMandate.getEmandate_otm_approved());
                        userDto.setEmandate_otm_rej_reason3(UserUtils.checkParem(userMandate.getEmandate_otm_rej_reason()));
                        userDto.setEmandate_otm_created_date3(userMandate.getEmandate_otm_created_date());
                        userDto.setEmandate_otm_amount3(UserUtils.checkParem(userMandate.getEmandate_otm_amount()));

                        userDto.setNse_ach_flag3(userMandate.getNse_ach_flag());
                        userDto.setNse_ach3(UserUtils.checkParem(userMandate.getNse_ach()));
                        userDto.setNse_ach_amount3(UserUtils.checkParem(userMandate.getNse_ach_amount()));
                        userDto.setNse_ach_approved3(userMandate.getNse_ach_approved());
                        userDto.setNse_ach_rej_reason3(UserUtils.checkParem(userMandate.getNse_ach_rej_reason()));
                        userDto.setNse_ach_created_date3(userMandate.getNse_ach_created_date());

                    }
                }
            }

            if (bankDetailsList.size() >= 4)
            {
                UsersBankDetails bank4 = bankDetailsList.get(3);
                userDto.setBank_name4(UserUtils.checkParem(bank4.getBank_name()));
                userDto.setBank_branch4(UserUtils.checkParem(bank4.getBank_branch()));
                userDto.setBank_address4(UserUtils.checkParem(bank4.getBank_address()));
                userDto.setBank_account_number4(UserUtils.checkParem(bank4.getBank_account_number()));
                userDto.setBank_account_holder_name4(UserUtils.checkParem(bank4.getBank_account_holder_name()));
                userDto.setBank_account_type4(UserUtils.checkParem(bank4.getBank_account_type()));
                userDto.setBank_ifsc_code4(UserUtils.checkParem(bank4.getBank_ifsc_code()));
                userDto.setBank_micr_code4(UserUtils.checkParem(bank4.getBank_micr_code()));
                userDto.setBank_proof4(UserUtils.checkParem(bank4.getBank_proof()));
                userDto.setBank_code4(UserUtils.checkParem(bank4.getBank_code()));

            }

            if (bankDetailsList.size() >= 5)
            {
                UsersBankDetails bank5 = bankDetailsList.get(4);
                userDto.setBank_name5(UserUtils.checkParem(bank5.getBank_name()));
                userDto.setBank_branch5(UserUtils.checkParem(bank5.getBank_branch()));
                userDto.setBank_address5(UserUtils.checkParem(bank5.getBank_address()));
                userDto.setBank_account_number5(UserUtils.checkParem(bank5.getBank_account_number()));
                userDto.setBank_account_holder_name5(UserUtils.checkParem(bank5.getBank_account_holder_name()));
                userDto.setBank_account_type5(UserUtils.checkParem(bank5.getBank_account_type()));
                userDto.setBank_ifsc_code5(UserUtils.checkParem(bank5.getBank_ifsc_code()));
                userDto.setBank_micr_code5(UserUtils.checkParem(bank5.getBank_micr_code()));
                userDto.setBank_proof5(UserUtils.checkParem(bank5.getBank_proof()));
                userDto.setBank_code5(UserUtils.checkParem(bank5.getBank_code()));

            }
        }
        // Map nominee details
        if (nomineeDetails != null)
        {
            userDto.setNominee_opt(nomineeDetails.getNominee_opt());
            userDto.setNumber_of_nominee(nomineeDetails.getNumber_of_nominee());

            if(nomineeDetails.getNominee_soa() != null && !nomineeDetails.getNominee_soa().isEmpty())
            {
                userDto.setNominee_soa(nomineeDetails.getNominee_soa());
            }else{
                userDto.setNominee_soa("N");
            }
            // Nominee 1
            userDto.setNominee1_type(UserUtils.checkParem(nomineeDetails.getNominee1_type()));
            userDto.setNominee1_type_desc(UserUtils.checkParem(nomineeDetails.getNominee1_type_desc()));
            userDto.setNominee1_name(UserUtils.checkParem(nomineeDetails.getNominee1_name()));
            userDto.setNominee1_middle_name(UserUtils.checkParem(nomineeDetails.getNominee1_middle_name()));
            userDto.setNominee1_last_name(UserUtils.checkParem(nomineeDetails.getNominee1_last_name()));
            userDto.setNominee1_pan(UserUtils.checkParem(nomineeDetails.getNominee1_pan()));
            userDto.setNominee1_dob(UserUtils.checkParem(nomineeDetails.getNominee1_dob()));
            userDto.setNominee1_address1(UserUtils.checkParem(nomineeDetails.getNominee1_address1()));
            userDto.setNominee1_address2(UserUtils.checkParem(nomineeDetails.getNominee1_address2()));
            userDto.setNominee1_address3(UserUtils.checkParem(nomineeDetails.getNominee1_address3()));
            userDto.setNominee1_pincode(UserUtils.checkParem(nomineeDetails.getNominee1_pincode()));
            userDto.setNominee1_city(UserUtils.checkParem(nomineeDetails.getNominee1_city()));
            userDto.setNominee1_state(UserUtils.checkParem(nomineeDetails.getNominee1_state()));
            userDto.setNominee1_state_code(UserUtils.checkParem(nomineeDetails.getNominee1_state_code()));
            userDto.setNominee1_country(UserUtils.checkParem(nomineeDetails.getNominee1_country()));
            userDto.setNominee1_mobile(UserUtils.checkParem(nomineeDetails.getNominee1_mobile()));
            userDto.setNominee1_email(UserUtils.checkParem(nomineeDetails.getNominee1_email()));
            userDto.setNominee1_id_type(UserUtils.checkParem(nomineeDetails.getNominee1_id_type()));
            userDto.setNominee1_id_no(UserUtils.checkParem(nomineeDetails.getNominee1_id_no()));
            userDto.setNominee1_relation(UserUtils.checkParem(nomineeDetails.getNominee1_relation()));
            userDto.setNominee1_percentage(UserUtils.checkParem(nomineeDetails.getNominee1_percentage()));
            userDto.setNominee1_guard_name(UserUtils.checkParem(nomineeDetails.getNominee1_guard_name()));
            userDto.setNominee1_guard_pan(UserUtils.checkParem(nomineeDetails.getNominee1_guard_pan()));
            userDto.setNominee1_guard_dob(UserUtils.checkParem(nomineeDetails.getNominee1_guard_dob()));
            userDto.setNominee1_guard_relationship(UserUtils.checkParem(nomineeDetails.getNominee1_guard_relationship()));

            // Nominee 2
            userDto.setNominee2_type(UserUtils.checkParem(nomineeDetails.getNominee2_type()));
            userDto.setNominee2_type_desc(UserUtils.checkParem(nomineeDetails.getNominee2_type_desc()));
            userDto.setNominee2_name(UserUtils.checkParem(nomineeDetails.getNominee2_name()));
            userDto.setNominee2_middle_name(UserUtils.checkParem(nomineeDetails.getNominee2_middle_name()));
            userDto.setNominee2_last_name(UserUtils.checkParem(nomineeDetails.getNominee2_last_name()));
            userDto.setNominee2_pan(UserUtils.checkParem(nomineeDetails.getNominee2_pan()));
            userDto.setNominee2_dob(UserUtils.checkParem(nomineeDetails.getNominee2_dob()));
            userDto.setNominee2_relation(UserUtils.checkParem(nomineeDetails.getNominee2_relation()));
            userDto.setNominee2_percentage(UserUtils.checkParem(nomineeDetails.getNominee2_percentage()));
            userDto.setNominee2_address1(UserUtils.checkParem(nomineeDetails.getNominee2_address1()));
            userDto.setNominee2_pincode(UserUtils.checkParem(nomineeDetails.getNominee2_pincode()));
            userDto.setNominee2_city(UserUtils.checkParem(nomineeDetails.getNominee2_city()));
            userDto.setNominee2_state(UserUtils.checkParem(nomineeDetails.getNominee2_state()));
            userDto.setNominee2_state_code(UserUtils.checkParem(nomineeDetails.getNominee2_state_code()));
            userDto.setNominee2_country(UserUtils.checkParem(nomineeDetails.getNominee2_country()));
            userDto.setNominee2_mobile(UserUtils.checkParem(nomineeDetails.getNominee2_mobile()));
            userDto.setNominee2_email(UserUtils.checkParem(nomineeDetails.getNominee2_email()));
            userDto.setNominee2_id_type(UserUtils.checkParem(nomineeDetails.getNominee2_id_type()));
            userDto.setNominee2_id_no(UserUtils.checkParem(nomineeDetails.getNominee2_id_no()));
            userDto.setNominee2_guard_name(UserUtils.checkParem(nomineeDetails.getNominee2_guard_name()));
            userDto.setNominee2_guard_pan(UserUtils.checkParem(nomineeDetails.getNominee2_guard_pan()));
            userDto.setNominee2_guard_dob(UserUtils.checkParem(nomineeDetails.getNominee2_guard_dob()));
            userDto.setNominee2_guard_relationship(UserUtils.checkParem(nomineeDetails.getNominee2_guard_relationship()));

            // Nominee 3
            userDto.setNominee3_type(UserUtils.checkParem(nomineeDetails.getNominee3_type()));
            userDto.setNominee3_type_desc(UserUtils.checkParem(nomineeDetails.getNominee3_type_desc()));
            userDto.setNominee3_name(UserUtils.checkParem(nomineeDetails.getNominee3_name()));
            userDto.setNominee3_middle_name(UserUtils.checkParem(nomineeDetails.getNominee3_middle_name()));
            userDto.setNominee3_last_name(UserUtils.checkParem(nomineeDetails.getNominee3_last_name()));
            userDto.setNominee3_pan(UserUtils.checkParem(nomineeDetails.getNominee3_pan()));
            userDto.setNominee3_dob(UserUtils.checkParem(nomineeDetails.getNominee3_dob()));
            userDto.setNominee3_relation(UserUtils.checkParem(nomineeDetails.getNominee3_relation()));
            userDto.setNominee3_percentage(UserUtils.checkParem(nomineeDetails.getNominee3_percentage()));
            userDto.setNominee3_address1(UserUtils.checkParem(nomineeDetails.getNominee3_address1()));
            userDto.setNominee3_pincode(UserUtils.checkParem(nomineeDetails.getNominee3_pincode()));
            userDto.setNominee3_city(UserUtils.checkParem(nomineeDetails.getNominee3_city()));
            userDto.setNominee3_state(UserUtils.checkParem(nomineeDetails.getNominee3_state()));
            userDto.setNominee3_state_code(UserUtils.checkParem(nomineeDetails.getNominee3_state_code()));
            userDto.setNominee3_country(UserUtils.checkParem(nomineeDetails.getNominee3_country()));
            userDto.setNominee3_mobile(UserUtils.checkParem(nomineeDetails.getNominee3_mobile()));
            userDto.setNominee3_email(UserUtils.checkParem(nomineeDetails.getNominee3_email()));
            userDto.setNominee3_id_type(UserUtils.checkParem(nomineeDetails.getNominee3_id_type()));
            userDto.setNominee3_id_no(UserUtils.checkParem(nomineeDetails.getNominee3_id_no()));
            userDto.setNominee3_guard_name(UserUtils.checkParem(nomineeDetails.getNominee3_guard_name()));
            userDto.setNominee3_guard_pan(UserUtils.checkParem(nomineeDetails.getNominee3_guard_pan()));
            userDto.setNominee3_guard_dob(UserUtils.checkParem(nomineeDetails.getNominee3_guard_dob()));
            userDto.setNominee3_guard_relationship(UserUtils.checkParem(nomineeDetails.getNominee3_guard_relationship()));
        }

        return userDto;
    }

//    public static UsersMandateDetails toEntity(UserMandateDetailsDto dto) {
//        UsersMandateDetails entity = new UsersMandateDetails();
//
//        entity.setId(dto.getId());
//        entity.setUser_id(dto.getUser_id());
//        entity.setOnline_id(dto.getOnline_id());
//        entity.setOnline_flag(dto.getOnline_flag());
//        entity.setOnline_code(dto.getOnline_code());
//        entity.setBroker_code(dto.getBroker_code());
//        entity.setBank_account_number(dto.getBank_account_number());
//
//        entity.setXsip_otm_flag(dto.getXsip_otm_flag());
//        entity.setXsip_otm(dto.getXsip_otm());
//        entity.setXsip_otm_amount(dto.getXsip_otm_amount());
//        entity.setXsip_otm_approved(dto.getXsip_otm_approved());
//        entity.setXsip_otm_rej_reason(dto.getXsip_otm_rej_reason());
//        if (dto.getXsip_otm_created_date() != null) {
//            entity.setXsip_otm_created_date(
//                    java.sql.Date.valueOf(dto.getXsip_otm_created_date())
//            );
//        }
//
//        entity.setEmandate_otm_flag(dto.getEmandate_otm_flag());
//        entity.setEmandate_otm(dto.getEmandate_otm());
//        entity.setEmandate_otm_amount(dto.getEmandate_otm_amount());
//        entity.setEmandate_otm_approved(dto.getEmandate_otm_approved());
//        entity.setEmandate_otm_rej_reason(dto.getEmandate_otm_rej_reason());
//        if (dto.getEmandate_otm_created_date() != null) {
//            entity.setEmandate_otm_created_date(
//                    java.sql.Date.valueOf(dto.getEmandate_otm_created_date())
//            );
//        }
//
//        entity.setNse_ach_flag(dto.getNse_ach_flag());
//        entity.setNse_ach(dto.getNse_ach());
//        entity.setNse_ach_amount(dto.getNse_ach_amount());
//        entity.setNse_ach_approved(dto.getNse_ach_approved());
//        entity.setNse_ach_rej_reason(dto.getNse_ach_rej_reason());
//        if (dto.getNse_ach_created_date() != null) {
//            entity.setNse_ach_created_date(
//                    java.sql.Date.valueOf(dto.getNse_ach_created_date())
//            );
//        }
//
//        entity.setMfu_mandate_flag(dto.getMfu_mandate_flag());
//        entity.setMfu_mandate(dto.getMfu_mandate());
//        entity.setMfu_mandate_mode(dto.getMfu_mandate_mode());
//        entity.setMfu_mmrn_no(dto.getMfu_mmrn_no());
//        entity.setMfu_mandate_amount(dto.getMfu_mandate_amount());
//        entity.setMfu_mandate_approved(dto.getMfu_mandate_approved());
//        entity.setMfu_mandate_rej_reason(dto.getMfu_mandate_rej_reason());
//        entity.setMfu_mandate_start_date(dto.getMfu_mandate_start_date());
//        entity.setMfu_mandate_end_date(dto.getMfu_mandate_end_date());
//        if (dto.getMfu_mandate_created_date() != null) {
//            entity.setMfu_mandate_created_date(dto.getMfu_mandate_created_date());
//        }
//
//        entity.setClient_name(dto.getClient_name());
//
//        if (dto.getCreated_date() != null) {
//            entity.setCreated_date(
//                    new Date(dto.getCreated_date().getTime())
//            );
//        }
//
//        return entity;
//    }
//
//    public static UserDto mapUserToDto(User user) {
//        UserDto dto = new UserDto();
//
//        dto.setId(user.getId());
//        dto.setName(user.getName());
//        dto.setPan(user.getPan());
//        dto.setMobile(user.getMobile());
//        dto.setEmail(user.getEmail());
//        dto.setGender(user.getGender());
//        dto.setAlter_email(user.getAlter_email());
//        dto.setAlter_mobile(user.getAlter_mobile());
//        dto.setType_id(user.getType_id());
//        dto.setBranch(user.getBranch());
//        dto.setRm_name(user.getRm_name());
//        dto.setSubbroker_name(user.getSubbroker_name());
//        dto.setSuper_subbroker_name(user.getSuper_subbroker_name());
//        dto.setPayout(user.getPayout());
//        dto.setActive(user.getActive());
//        dto.setEmail_active(user.getEmail_active());
//        dto.setStreet_1(user.getStreet_1());
//        dto.setStreet_2(user.getStreet_2());
//        dto.setStreet_3(user.getStreet_3());
//        dto.setCity(user.getCity());
//        dto.setPincode(user.getPincode());
//        dto.setState(user.getState());
//        dto.setCountry(user.getCountry());
//        dto.setDate_of_birth(user.getDate_of_birth());
//        dto.setAnniversary_date(user.getAnniversary_date());
//        dto.setBroker_code(user.getBroker_code());
//        dto.setClient_name(user.getClient_name());
//        dto.setFirst_investment_date(user.getFirst_investment_date());
//        dto.setMf_oneday_change(user.isMf_oneday_change());
//        dto.setIs_purchase_allowed(user.is_purchase_allowed());
//        dto.setIs_redeem_allowed(user.is_redeem_allowed());
//        dto.setIs_switch_allowed(user.is_switch_allowed());
//        dto.setIs_stp_allowed(user.is_stp_allowed());
//        dto.setIs_swp_allowed(user.is_swp_allowed());
//        dto.setMf_aum(user.getMf_aum());
//        // Map any additional fields as needed
//
//        return dto;
//    }

    public static UserDto mapToUserDtoMappers(UsersOnlineRegDetails userDetails, List<UsersBankDetails> bankDetailsList, List<UsersMandateDetails> mandateDetailsList, UsersNomineeDetails nomineeDetails) {
        UserDto userDto = new UserDto();
        // Map basic user details
        userDto.setId(userDetails.getId());
        userDto.setUser_id(userDetails.getUser_id());
        userDto.setName(UserUtils.checkParem(userDetails.getName()));
        userDto.setPan(UserUtils.checkParem(userDetails.getPan()));
        userDto.setMobile(UserUtils.checkParem(userDetails.getMobile()));
        userDto.setMobile_isd_code(UserUtils.checkParem(userDetails.getMobile_isd_code()));
        userDto.setAlter_mobile(UserUtils.checkParem(userDetails.getAlter_mobile()));
        userDto.setEmail(UserUtils.checkParem(userDetails.getEmail()));
        userDto.setAlter_email(UserUtils.checkParem(userDetails.getAlter_email()));
        userDto.setStreet_1(UserUtils.checkParem(userDetails.getStreet_1()));
        userDto.setStreet_2(UserUtils.checkParem(userDetails.getStreet_2()));
        userDto.setStreet_3(UserUtils.checkParem(userDetails.getStreet_3()));
        userDto.setCity(UserUtils.checkParem(userDetails.getCity()));
        userDto.setPincode(UserUtils.checkParem(userDetails.getPincode()));
        userDto.setState(UserUtils.checkParem(userDetails.getState()));
        userDto.setState_code(UserUtils.checkParem(userDetails.getState_code()));
        userDto.setCountry(UserUtils.checkParem(userDetails.getCountry()));
        userDto.setFather_name(UserUtils.checkParem(userDetails.getFather_name()));
        userDto.setGender(UserUtils.checkParem(userDetails.getGender()));
        userDto.setDate_of_birth(UserUtils.checkParem(userDetails.getDate_of_birth()));
        userDto.setPlace_of_birth(UserUtils.checkParem(userDetails.getPlace_of_birth()));
        userDto.setCountry_of_birth(UserUtils.checkParem(userDetails.getCountry_of_birth()));
        userDto.setCountry_birth_code(UserUtils.checkParem(userDetails.getCountry_birth_code()));
        userDto.setPhone_office(UserUtils.checkParem(userDetails.getPhone_office()));
        userDto.setPhone_residence(UserUtils.checkParem(userDetails.getPhone_residence()));
        userDto.setName(UserUtils.checkParem(userDetails.getName()));
        userDto.setInv_category(UserUtils.checkParem(userDetails.getInv_category()));
        userDto.setTax_status(UserUtils.checkParem(userDetails.getTax_status()));
        userDto.setTax_status_code(UserUtils.checkParem(userDetails.getTax_status_code()));
        userDto.setHolding_nature(UserUtils.checkParem(userDetails.getHolding_nature()));
        userDto.setHolding_nature_code(UserUtils.checkParem(userDetails.getHolding_nature_code()));
        userDto.setOccupation(UserUtils.checkParem(userDetails.getOccupation()));
        userDto.setOccupation_code(UserUtils.checkParem(userDetails.getOccupation_code()));
        userDto.setAnnual_income(UserUtils.checkParem(userDetails.getAnnual_income()));
        userDto.setAnnual_income_code(UserUtils.checkParem(userDetails.getAnnual_income_code()));
        userDto.setSource_of_wealth(UserUtils.checkParem(userDetails.getSource_of_wealth()));
        userDto.setSource_of_wealth_code(UserUtils.checkParem(userDetails.getSource_of_wealth_code()));
        userDto.setPolitical(UserUtils.checkParem(userDetails.getPolitical()));
        userDto.setPolitical_code(UserUtils.checkParem(userDetails.getPolitical_code()));
        userDto.setNetworth_amount(UserUtils.checkParem(userDetails.getNetworth_amount()));
        userDto.setNetworth_dob(UserUtils.checkParem(userDetails.getNetworth_dob()));
        userDto.setAddress_type(UserUtils.checkParem(userDetails.getAddress_type()));
        userDto.setAddress_type_code(UserUtils.checkParem(userDetails.getAddress_type_code()));
        userDto.setGuard_name(UserUtils.checkParem(userDetails.getGuard_name()));
        userDto.setGuard_pan(UserUtils.checkParem(userDetails.getGuard_pan()));
        userDto.setGuard_dob(UserUtils.checkParem(userDetails.getGuard_dob()));
        userDto.setGuard_mobile(UserUtils.checkParem(userDetails.getGuard_mobile()));
        userDto.setGuard_email(UserUtils.checkParem(userDetails.getGuard_email()));
        userDto.setGuard_relationship(UserUtils.checkParem(userDetails.getGuard_relationship()));
        userDto.setGuard_relation_proof(UserUtils.checkParem(userDetails.getGuard_relation_proof()));
        userDto.setGuard_account_relation(UserUtils.checkParem(userDetails.getGuard_account_relation()));
        userDto.setJoint_holder_name1(UserUtils.checkParem(userDetails.getJoint_holder_name1()));
        userDto.setJoint_holder_pan1(UserUtils.checkParem(userDetails.getJoint_holder_pan1()));
        userDto.setJoint_holder_dob1(UserUtils.checkParem(userDetails.getJoint_holder_dob1()));
        userDto.setJoint_holder_email1(UserUtils.checkParem(userDetails.getJoint_holder_email1()));
        userDto.setJoint_holder_email_relation1(UserUtils.checkParem(userDetails.getJoint_holder_email_relation1()));
        userDto.setJoint_holder_mobile1(UserUtils.checkParem(userDetails.getJoint_holder_mobile1()));
        userDto.setJoint_holder_mobile1_isd_code(UserUtils.checkParem(userDetails.getJoint_holder_mobile1_isd_code()));
        userDto.setJoint_holder_mobile_relation1(UserUtils.checkParem(userDetails.getJoint_holder_mobile_relation1()));
        userDto.setJoint_holder_signature1(UserUtils.checkParem(userDetails.getJoint_holder_signature1()));
        userDto.setJoint_holder_place_of_birth1(UserUtils.checkParem(userDetails.getJoint_holder_place_of_birth1()));
        userDto.setJoint_holder_country_birth_code1(UserUtils.checkParem(userDetails.getJoint_holder_country_birth_code1()));
        userDto.setJoint_holder_occupation_code1(UserUtils.checkParem(userDetails.getJoint_holder_occupation_code1()));
        userDto.setJoint_holder_annual_income_code1(UserUtils.checkParem(userDetails.getJoint_holder_annual_income_code1()));
        userDto.setJoint_holder_source_of_wealth_code1(UserUtils.checkParem(userDetails.getJoint_holder_source_of_wealth_code1()));
        userDto.setJoint_holder_political_code1(UserUtils.checkParem(userDetails.getJoint_holder_political_code1()));
        userDto.setJoint_holder_address_type_code1(UserUtils.checkParem(userDetails.getJoint_holder_address_type_code1()));
        userDto.setJoint_holder_name2(UserUtils.checkParem(userDetails.getJoint_holder_name2()));
        userDto.setJoint_holder_pan2(UserUtils.checkParem(userDetails.getJoint_holder_pan2()));
        userDto.setJoint_holder_dob2(UserUtils.checkParem(userDetails.getJoint_holder_dob2()));
        userDto.setJoint_holder_email2(UserUtils.checkParem(userDetails.getJoint_holder_email2()));
        userDto.setJoint_holder_email_relation2(UserUtils.checkParem(userDetails.getJoint_holder_email_relation2()));
        userDto.setJoint_holder_mobile2(UserUtils.checkParem(userDetails.getJoint_holder_mobile2()));
        userDto.setJoint_holder_mobile2_isd_code(UserUtils.checkParem(userDetails.getJoint_holder_mobile2_isd_code()));
        userDto.setJoint_holder_mobile_relation2(UserUtils.checkParem(userDetails.getJoint_holder_mobile_relation2()));
        userDto.setJoint_holder_signature2(UserUtils.checkParem(userDetails.getJoint_holder_signature2()));
        userDto.setJoint_holder_place_of_birth2(UserUtils.checkParem(userDetails.getJoint_holder_place_of_birth2()));
        userDto.setJoint_holder_country_birth_code2(UserUtils.checkParem(userDetails.getJoint_holder_country_birth_code2()));
        userDto.setJoint_holder_occupation_code2(UserUtils.checkParem(userDetails.getJoint_holder_occupation_code2()));
        userDto.setJoint_holder_annual_income_code2(UserUtils.checkParem(userDetails.getJoint_holder_annual_income_code2()));
        userDto.setJoint_holder_source_of_wealth_code2(UserUtils.checkParem(userDetails.getJoint_holder_source_of_wealth_code2()));
        userDto.setJoint_holder_political_code2(UserUtils.checkParem(userDetails.getJoint_holder_political_code2()));
        userDto.setJoint_holder_address_type_code2(UserUtils.checkParem(userDetails.getJoint_holder_address_type_code2()));
        userDto.setNri_address1(UserUtils.checkParem(userDetails.getNri_address1()));
        userDto.setNri_address2(UserUtils.checkParem(userDetails.getNri_address2()));
        userDto.setNri_address3(UserUtils.checkParem(userDetails.getNri_address3()));
        userDto.setNri_city(UserUtils.checkParem(userDetails.getNri_city()));
        userDto.setNri_state(UserUtils.checkParem(userDetails.getNri_state()));
        userDto.setNri_pincode(UserUtils.checkParem(userDetails.getNri_pincode()));
        userDto.setNri_country(UserUtils.checkParem(userDetails.getNri_country()));
        userDto.setEmail_verified(userDetails.getEmail_verified());
        userDto.setEmail_authcode(UserUtils.checkParem(userDetails.getEmail_authcode()));
        userDto.setMobile_verified(userDetails.getMobile_verified());
        userDto.setMobile_otp(UserUtils.checkParem(userDetails.getMobile_otp()));
        userDto.setNse_customer(userDetails.getNse_customer());
        userDto.setNse_active(userDetails.getNse_active());
        userDto.setNse_iin_number(UserUtils.checkParem(userDetails.getNse_iin_number()));
        userDto.setMobile_relation(UserUtils.checkParem(userDetails.getMobile_relation()));
        userDto.setEmail_relation(UserUtils.checkParem(userDetails.getEmail_relation()));
        userDto.setRegister_source(UserUtils.checkParem(userDetails.getRegister_source()));
        userDto.setSalutation(UserUtils.checkParem(userDetails.getSalutation()));
        userDto.setClient_name(UserUtils.checkParem(userDetails.getClient_name()));
        userDto.setBroker_code(UserUtils.checkParem(userDetails.getBroker_code()));
        userDto.setEuin(UserUtils.checkParem(userDetails.getEuin()));
        userDto.setOnline_flag(UserUtils.checkParem(userDetails.getOnline_flag()));
        userDto.setFirst_name(UserUtils.checkParem(userDetails.getFirst_name()));
        userDto.setMiddle_name(UserUtils.checkParem(userDetails.getMiddle_name()));
        userDto.setLast_name(UserUtils.checkParem(userDetails.getLast_name()));
        userDto.setCreated_date(userDetails.getCreated_date());

        // Map bank details (up to 3 banks)
        String bankAcc1 = "";
        String bankAcc2 = "";
        String bankAcc3 = "";
        String bankAcc4 = "";
        String bankAcc5 = "";
        if (bankDetailsList != null && !bankDetailsList.isEmpty())
        {
            if (bankDetailsList.size() >= 1) {
                UsersBankDetails bank1 = bankDetailsList.get(0);
                userDto.setBank_name1(UserUtils.checkParem(bank1.getBank_name()));
                userDto.setBank_branch1(UserUtils.checkParem(bank1.getBank_branch()));
                userDto.setBank_address1(UserUtils.checkParem(bank1.getBank_address()));
                userDto.setBank_account_number1(UserUtils.checkParem(bank1.getBank_account_number()));
                userDto.setBank_account_holder_name1(UserUtils.checkParem(bank1.getBank_account_holder_name()));
                userDto.setBank_account_type1(UserUtils.checkParem(bank1.getBank_account_type()));
                userDto.setBank_ifsc_code1(UserUtils.checkParem(bank1.getBank_ifsc_code()));
                userDto.setBank_micr_code1(UserUtils.checkParem(bank1.getBank_micr_code()));
                userDto.setBank_proof1(UserUtils.checkParem(bank1.getBank_proof()));
                userDto.setDefault_bank1(bank1.getDefault_bank());
                bankAcc1 = UserUtils.checkParem(bank1.getBank_account_number());

            }

            if (bankDetailsList.size() >= 2)
            {
                UsersBankDetails bank2 = bankDetailsList.get(1);
                userDto.setBank_name2(UserUtils.checkParem(bank2.getBank_name()));
                userDto.setBank_branch2(UserUtils.checkParem(bank2.getBank_branch()));
                userDto.setBank_address2(UserUtils.checkParem(bank2.getBank_address()));
                userDto.setBank_account_number2(UserUtils.checkParem(bank2.getBank_account_number()));
                userDto.setBank_account_holder_name2(UserUtils.checkParem(bank2.getBank_account_holder_name()));
                userDto.setBank_account_type2(UserUtils.checkParem(bank2.getBank_account_type()));
                userDto.setBank_ifsc_code2(UserUtils.checkParem(bank2.getBank_ifsc_code()));
                userDto.setBank_micr_code2(UserUtils.checkParem(bank2.getBank_micr_code()));
                userDto.setBank_proof2(UserUtils.checkParem(bank2.getBank_proof()));
                userDto.setDefault_bank2(bank2.getDefault_bank());
                bankAcc2 = UserUtils.checkParem(bank2.getBank_account_number());

            }

            if (bankDetailsList.size() >= 3)
            {
                UsersBankDetails bank3 = bankDetailsList.get(2);
                userDto.setBank_name3(UserUtils.checkParem(bank3.getBank_name()));
                userDto.setBank_branch3(UserUtils.checkParem(bank3.getBank_branch()));
                userDto.setBank_address3(UserUtils.checkParem(bank3.getBank_address()));
                userDto.setBank_account_number3(UserUtils.checkParem(bank3.getBank_account_number()));
                userDto.setBank_account_holder_name3(UserUtils.checkParem(bank3.getBank_account_holder_name()));
                userDto.setBank_account_type3(UserUtils.checkParem(bank3.getBank_account_type()));
                userDto.setBank_ifsc_code3(UserUtils.checkParem(bank3.getBank_ifsc_code()));
                userDto.setBank_micr_code3(UserUtils.checkParem(bank3.getBank_micr_code()));
                userDto.setBank_proof3(UserUtils.checkParem(bank3.getBank_proof()));
                userDto.setDefault_bank3(bank3.getDefault_bank());
                bankAcc3 = UserUtils.checkParem(bank3.getBank_account_number());

            }

            if (bankDetailsList.size() >= 4)
            {
                UsersBankDetails bank4 = bankDetailsList.get(3);
                userDto.setBank_name4(UserUtils.checkParem(bank4.getBank_name()));
                userDto.setBank_branch4(UserUtils.checkParem(bank4.getBank_branch()));
                userDto.setBank_address4(UserUtils.checkParem(bank4.getBank_address()));
                userDto.setBank_account_number4(UserUtils.checkParem(bank4.getBank_account_number()));
                userDto.setBank_account_holder_name4(UserUtils.checkParem(bank4.getBank_account_holder_name()));
                userDto.setBank_account_type4(UserUtils.checkParem(bank4.getBank_account_type()));
                userDto.setBank_ifsc_code4(UserUtils.checkParem(bank4.getBank_ifsc_code()));
                userDto.setBank_micr_code4(UserUtils.checkParem(bank4.getBank_micr_code()));
                userDto.setBank_proof4(UserUtils.checkParem(bank4.getBank_proof()));
                userDto.setDefault_bank4(bank4.getDefault_bank());
                bankAcc4 = UserUtils.checkParem(bank4.getBank_account_number());

            }

            if (bankDetailsList.size() >= 5)
            {
                UsersBankDetails bank5 = bankDetailsList.get(4);
                userDto.setBank_name5(UserUtils.checkParem(bank5.getBank_name()));
                userDto.setBank_branch5(UserUtils.checkParem(bank5.getBank_branch()));
                userDto.setBank_address5(UserUtils.checkParem(bank5.getBank_address()));
                userDto.setBank_account_number5(UserUtils.checkParem(bank5.getBank_account_number()));
                userDto.setBank_account_holder_name5(UserUtils.checkParem(bank5.getBank_account_holder_name()));
                userDto.setBank_account_type5(UserUtils.checkParem(bank5.getBank_account_type()));
                userDto.setBank_ifsc_code5(UserUtils.checkParem(bank5.getBank_ifsc_code()));
                userDto.setBank_micr_code5(UserUtils.checkParem(bank5.getBank_micr_code()));
                userDto.setBank_proof5(UserUtils.checkParem(bank5.getBank_proof()));
                userDto.setDefault_bank5(bank5.getDefault_bank());
                bankAcc5 = UserUtils.checkParem(bank5.getBank_account_number());

            }
        }

        if (mandateDetailsList != null && !mandateDetailsList.isEmpty())
        {
            if (mandateDetailsList.size() >= 1) {
                String finalBankAcc1 = bankAcc1;
                UsersMandateDetails userMandate = mandateDetailsList.stream().filter(mandate-> mandate.getBank_account_number().equalsIgnoreCase(finalBankAcc1)).findFirst().orElse(new UsersMandateDetails());
                userDto.setXsip_otm_flag1(userMandate.getXsip_otm_flag());
                userDto.setXsip_otm1(UserUtils.checkParem(userMandate.getXsip_otm()));
                userDto.setXsip_otm_approved1(userMandate.getXsip_otm_approved());
                userDto.setXsip_otm_rej_reason1(UserUtils.checkParem(userMandate.getXsip_otm_rej_reason()));
                userDto.setXsip_otm_created_date1(userMandate.getXsip_otm_created_date());
                userDto.setXsip_otm_amount1(UserUtils.checkParem(userMandate.getXsip_otm_amount()));

                userDto.setEmandate_otm_flag1(userMandate.getEmandate_otm_flag());
                userDto.setEmandate_otm1(UserUtils.checkParem(userMandate.getEmandate_otm()));
                userDto.setEmandate_otm_approved1(userMandate.getEmandate_otm_approved());
                userDto.setEmandate_otm_rej_reason1(UserUtils.checkParem(userMandate.getEmandate_otm_rej_reason()));
                userDto.setEmandate_otm_created_date1(userMandate.getEmandate_otm_created_date());
                userDto.setEmandate_otm_amount1(UserUtils.checkParem(userMandate.getEmandate_otm_amount()));

                userDto.setNse_ach_flag1(userMandate.getNse_ach_flag());
                userDto.setNse_ach1(UserUtils.checkParem(userMandate.getNse_ach()));
                userDto.setNse_ach_amount1(UserUtils.checkParem(userMandate.getNse_ach_amount()));
                userDto.setNse_ach_approved1(userMandate.getNse_ach_approved());
                userDto.setNse_ach_rej_reason1(UserUtils.checkParem(userMandate.getNse_ach_rej_reason()));
                userDto.setNse_ach_created_date1(userMandate.getNse_ach_created_date());

            }

            if (mandateDetailsList.size() >= 2) {
                String finalBankAcc2 = bankAcc2;
                UsersMandateDetails userMandate1 = mandateDetailsList.stream().filter(mandate-> mandate.getBank_account_number().equalsIgnoreCase(finalBankAcc2)).findFirst().orElse(new UsersMandateDetails());
                userDto.setXsip_otm_flag2(userMandate1.getXsip_otm_flag());
                userDto.setXsip_otm2(UserUtils.checkParem(userMandate1.getXsip_otm()));
                userDto.setXsip_otm_approved2(userMandate1.getXsip_otm_approved());
                userDto.setXsip_otm_rej_reason2(UserUtils.checkParem(userMandate1.getXsip_otm_rej_reason()));
                userDto.setXsip_otm_created_date2(userMandate1.getXsip_otm_created_date());
                userDto.setXsip_otm_amount2(UserUtils.checkParem(userMandate1.getXsip_otm_amount()));

                userDto.setEmandate_otm_flag2(userMandate1.getEmandate_otm_flag());
                userDto.setEmandate_otm2(UserUtils.checkParem(userMandate1.getEmandate_otm()));
                userDto.setEmandate_otm_approved2(userMandate1.getEmandate_otm_approved());
                userDto.setEmandate_otm_rej_reason2(UserUtils.checkParem(userMandate1.getEmandate_otm_rej_reason()));
                userDto.setEmandate_otm_created_date2(userMandate1.getEmandate_otm_created_date());
                userDto.setEmandate_otm_amount2(UserUtils.checkParem(userMandate1.getEmandate_otm_amount()));

                userDto.setNse_ach_flag2(userMandate1.getNse_ach_flag());
                userDto.setNse_ach2(UserUtils.checkParem(userMandate1.getNse_ach()));
                userDto.setNse_ach_amount2(UserUtils.checkParem(userMandate1.getNse_ach_amount()));
                userDto.setNse_ach_approved2(userMandate1.getNse_ach_approved());
                userDto.setNse_ach_rej_reason2(UserUtils.checkParem(userMandate1.getNse_ach_rej_reason()));
                userDto.setNse_ach_created_date2(userMandate1.getNse_ach_created_date());

            }

            if (mandateDetailsList.size() >= 3) {
                String finalBankAcc3 = bankAcc3;
                UsersMandateDetails userMandate3 = mandateDetailsList.stream().filter(mandate-> mandate.getBank_account_number().equalsIgnoreCase(finalBankAcc3)).findFirst().orElse(new UsersMandateDetails());
                userDto.setXsip_otm_flag3(userMandate3.getXsip_otm_flag());
                userDto.setXsip_otm3(UserUtils.checkParem(userMandate3.getXsip_otm()));
                userDto.setXsip_otm_approved3(userMandate3.getXsip_otm_approved());
                userDto.setXsip_otm_rej_reason3(UserUtils.checkParem(userMandate3.getXsip_otm_rej_reason()));
                userDto.setXsip_otm_created_date3(userMandate3.getXsip_otm_created_date());
                userDto.setXsip_otm_amount3(UserUtils.checkParem(userMandate3.getXsip_otm_amount()));

                userDto.setEmandate_otm_flag3(userMandate3.getEmandate_otm_flag());
                userDto.setEmandate_otm3(UserUtils.checkParem(userMandate3.getEmandate_otm()));
                userDto.setEmandate_otm_approved3(userMandate3.getEmandate_otm_approved());
                userDto.setEmandate_otm_rej_reason3(UserUtils.checkParem(userMandate3.getEmandate_otm_rej_reason()));
                userDto.setEmandate_otm_created_date3(userMandate3.getEmandate_otm_created_date());
                userDto.setEmandate_otm_amount3(UserUtils.checkParem(userMandate3.getEmandate_otm_amount()));

                userDto.setNse_ach_flag3(userMandate3.getNse_ach_flag());
                userDto.setNse_ach3(UserUtils.checkParem(userMandate3.getNse_ach()));
                userDto.setNse_ach_amount3(UserUtils.checkParem(userMandate3.getNse_ach_amount()));
                userDto.setNse_ach_approved3(userMandate3.getNse_ach_approved());
                userDto.setNse_ach_rej_reason3(UserUtils.checkParem(userMandate3.getNse_ach_rej_reason()));
                userDto.setNse_ach_created_date3(userMandate3.getNse_ach_created_date());

            }

        }

        // Map nominee details
        if (nomineeDetails != null)
        {
            userDto.setNumber_of_nominee(nomineeDetails.getNumber_of_nominee());
            userDto.setNominee_soa(nomineeDetails.getNominee_soa());

            // Nominee 1
            userDto.setNominee1_type(UserUtils.checkParem(nomineeDetails.getNominee1_type()));
            userDto.setNominee1_type_desc(UserUtils.checkParem(nomineeDetails.getNominee1_type_desc()));
            userDto.setNominee1_name(UserUtils.checkParem(nomineeDetails.getNominee1_name()));
            userDto.setNominee1_pan(UserUtils.checkParem(nomineeDetails.getNominee1_pan()));
            userDto.setNominee1_dob(UserUtils.checkParem(nomineeDetails.getNominee1_dob()));
            userDto.setNominee1_address1(UserUtils.checkParem(nomineeDetails.getNominee1_address1()));
            userDto.setNominee1_address2(UserUtils.checkParem(nomineeDetails.getNominee1_address2()));
            userDto.setNominee1_address3(UserUtils.checkParem(nomineeDetails.getNominee1_address3()));
            userDto.setNominee1_pincode(UserUtils.checkParem(nomineeDetails.getNominee1_pincode()));
            userDto.setNominee1_city(UserUtils.checkParem(nomineeDetails.getNominee1_city()));
            userDto.setNominee1_state(UserUtils.checkParem(nomineeDetails.getNominee1_state()));
            userDto.setNominee1_state_code(UserUtils.checkParem(nomineeDetails.getNominee1_state_code()));
            userDto.setNominee1_country(UserUtils.checkParem(nomineeDetails.getNominee1_country()));
            userDto.setNominee1_mobile(UserUtils.checkParem(nomineeDetails.getNominee1_mobile()));
            userDto.setNominee1_email(UserUtils.checkParem(nomineeDetails.getNominee1_email()));
            userDto.setNominee1_id_type(UserUtils.checkParem(nomineeDetails.getNominee1_id_type()));
            userDto.setNominee1_id_no(UserUtils.checkParem(nomineeDetails.getNominee1_id_no()));
            userDto.setNominee1_relation(UserUtils.checkParem(nomineeDetails.getNominee1_relation()));
            userDto.setNominee1_percentage(UserUtils.checkParem(nomineeDetails.getNominee1_percentage()));
            userDto.setNominee1_guard_name(UserUtils.checkParem(nomineeDetails.getNominee1_guard_name()));
            userDto.setNominee1_guard_pan(UserUtils.checkParem(nomineeDetails.getNominee1_guard_pan()));
            userDto.setNominee1_guard_dob(UserUtils.checkParem(nomineeDetails.getNominee1_guard_dob()));
            userDto.setNominee1_guard_relationship(UserUtils.checkParem(nomineeDetails.getNominee1_guard_relationship()));

            // Nominee 2
            userDto.setNominee2_type(UserUtils.checkParem(nomineeDetails.getNominee2_type()));
            userDto.setNominee2_type_desc(UserUtils.checkParem(nomineeDetails.getNominee2_type_desc()));
            userDto.setNominee2_name(UserUtils.checkParem(nomineeDetails.getNominee2_name()));
            userDto.setNominee2_pan(UserUtils.checkParem(nomineeDetails.getNominee2_pan()));
            userDto.setNominee2_dob(UserUtils.checkParem(nomineeDetails.getNominee2_dob()));
            userDto.setNominee2_relation(UserUtils.checkParem(nomineeDetails.getNominee2_relation()));
            userDto.setNominee2_percentage(UserUtils.checkParem(nomineeDetails.getNominee2_percentage()));
            userDto.setNominee2_address1(UserUtils.checkParem(nomineeDetails.getNominee2_address1()));
            userDto.setNominee2_pincode(UserUtils.checkParem(nomineeDetails.getNominee2_pincode()));
            userDto.setNominee2_city(UserUtils.checkParem(nomineeDetails.getNominee2_city()));
            userDto.setNominee2_state(UserUtils.checkParem(nomineeDetails.getNominee2_state()));
            userDto.setNominee2_state_code(UserUtils.checkParem(nomineeDetails.getNominee2_state_code()));
            userDto.setNominee2_country(UserUtils.checkParem(nomineeDetails.getNominee2_country()));
            userDto.setNominee2_mobile(UserUtils.checkParem(nomineeDetails.getNominee2_mobile()));
            userDto.setNominee2_email(UserUtils.checkParem(nomineeDetails.getNominee2_email()));
            userDto.setNominee2_id_type(UserUtils.checkParem(nomineeDetails.getNominee2_id_type()));
            userDto.setNominee2_id_no(UserUtils.checkParem(nomineeDetails.getNominee2_id_no()));
            userDto.setNominee2_guard_name(UserUtils.checkParem(nomineeDetails.getNominee2_guard_name()));
            userDto.setNominee2_guard_pan(UserUtils.checkParem(nomineeDetails.getNominee2_guard_pan()));
            userDto.setNominee2_guard_dob(UserUtils.checkParem(nomineeDetails.getNominee2_guard_dob()));
            userDto.setNominee2_guard_relationship(UserUtils.checkParem(nomineeDetails.getNominee2_guard_relationship()));

            // Nominee 3
            userDto.setNominee3_type(UserUtils.checkParem(nomineeDetails.getNominee3_type()));
            userDto.setNominee3_type_desc(UserUtils.checkParem(nomineeDetails.getNominee3_type_desc()));
            userDto.setNominee3_name(UserUtils.checkParem(nomineeDetails.getNominee3_name()));
            userDto.setNominee3_pan(UserUtils.checkParem(nomineeDetails.getNominee3_pan()));
            userDto.setNominee3_dob(UserUtils.checkParem(nomineeDetails.getNominee3_dob()));
            userDto.setNominee3_relation(UserUtils.checkParem(nomineeDetails.getNominee3_relation()));
            userDto.setNominee3_percentage(UserUtils.checkParem(nomineeDetails.getNominee3_percentage()));
            userDto.setNominee3_address1(UserUtils.checkParem(nomineeDetails.getNominee3_address1()));
            userDto.setNominee3_pincode(UserUtils.checkParem(nomineeDetails.getNominee3_pincode()));
            userDto.setNominee3_city(UserUtils.checkParem(nomineeDetails.getNominee3_city()));
            userDto.setNominee3_state(UserUtils.checkParem(nomineeDetails.getNominee3_state()));
            userDto.setNominee3_state_code(UserUtils.checkParem(nomineeDetails.getNominee3_state_code()));
            userDto.setNominee3_country(UserUtils.checkParem(nomineeDetails.getNominee3_country()));
            userDto.setNominee3_mobile(UserUtils.checkParem(nomineeDetails.getNominee3_mobile()));
            userDto.setNominee3_email(UserUtils.checkParem(nomineeDetails.getNominee3_email()));
            userDto.setNominee3_id_type(UserUtils.checkParem(nomineeDetails.getNominee3_id_type()));
            userDto.setNominee3_id_no(UserUtils.checkParem(nomineeDetails.getNominee3_id_no()));
            userDto.setNominee3_guard_name(UserUtils.checkParem(nomineeDetails.getNominee3_guard_name()));
            userDto.setNominee3_guard_pan(UserUtils.checkParem(nomineeDetails.getNominee3_guard_pan()));
            userDto.setNominee3_guard_dob(UserUtils.checkParem(nomineeDetails.getNominee3_guard_dob()));
            userDto.setNominee3_guard_relationship(UserUtils.checkParem(nomineeDetails.getNominee3_guard_relationship()));
        }

        return userDto;
    }

}
