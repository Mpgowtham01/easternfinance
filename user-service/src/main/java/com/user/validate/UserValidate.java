package com.user.validate;

import com.user.dto.*;
import org.apache.commons.lang.StringUtils;
import org.hibernate.internal.util.StringHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserValidate
{
    public static String validateInvestorInfo(InvestorInfoDTO dto)
    {
        if (dto.getTaxStatusCode().equals("02")
                || dto.getTaxStatusCode().equals("26")
                || dto.getTaxStatusCode().equals("28")) {
            // Skip PAN check
        } else {
            if (StringUtils.isEmpty(dto.getPan())) return "Pan cannot be empty";
        }

        if (StringUtils.isEmpty(dto.getBrokerCode())) return "Broker code cannot be empty";
        if (StringUtils.isEmpty(dto.getTaxStatusCode())) return "Tax status code cannot be empty";
        if (StringUtils.isEmpty(dto.getTaxStatusDesc())) return "Tax status description cannot be empty";
        if (StringUtils.isEmpty(dto.getHoldingNatureCode())) return "Holding nature code cannot be empty";
        if (StringUtils.isEmpty(dto.getHoldingNatureDesc())) return "Holding nature description cannot be empty";
        return null;
    }

    public static String validatePersonalInfo(PersonalInfoDTO dto)
    {
        if(dto.getSource().equalsIgnoreCase("Website"))
        {
            if (StringUtils.isEmpty(dto.getBrokerCode())) return "Broker code cannot be empty";
            if (StringUtils.isEmpty(dto.getTaxStatusCode())) return "Tax status code cannot be empty";
            if (StringUtils.isEmpty(dto.getTaxStatusDesc())) return "Tax status description cannot be empty";
            if (StringUtils.isEmpty(dto.getHoldingNatureCode())) return "Holding nature code cannot be empty";
            if (StringUtils.isEmpty(dto.getHoldingNatureDesc())) return "Holding nature description cannot be empty";
        }

        if(dto.getTaxStatusCode().equalsIgnoreCase("01") || dto.getTaxStatusCode().equalsIgnoreCase("02") || dto.getTaxStatusCode().equalsIgnoreCase("24") ||dto.getTaxStatusCode().equalsIgnoreCase("11") || dto.getTaxStatusCode().equalsIgnoreCase("21") || dto.getTaxStatusCode().equalsIgnoreCase("26") || dto.getTaxStatusCode().equalsIgnoreCase("28"))
        {
            if (StringUtils.isEmpty(dto.getGender())) return "Gender cannot be empty";
        }

        if (StringUtils.isEmpty(dto.getEmail())) return "Email cannot be empty";
        if (StringUtils.isEmpty(dto.getEmailRelationCode())) return "Email relation code cannot be empty";
        if (StringUtils.isEmpty(dto.getMobile())) return "Mobile number cannot be empty";
        if (StringUtils.isEmpty(dto.getMobileRelationCode())) return "Mobile relation code cannot be empty";
        if (StringUtils.isEmpty(dto.getAddressTypeCode())) return "Address type code cannot be empty";
        if (StringUtils.isEmpty(dto.getAddressTypeDesc())) return "Address type description cannot be empty";

        System.out.println("Tax Status Code : "+dto.getTaxStatusCode());

        if(dto.getTaxStatusCode().equalsIgnoreCase("02") || dto.getTaxStatusCode().equalsIgnoreCase("26") || dto.getTaxStatusCode().equalsIgnoreCase("28"))
        {
            if (StringUtils.isEmpty(dto.getName())) return "Name cannot be empty";
            if(dto.getSource().equalsIgnoreCase("Website"))
            {
                if (StringUtils.isEmpty(dto.getPan())) return "PAN cannot be empty";
            }
            if (StringUtils.isEmpty(dto.getDob())) return "Date of birth cannot be empty";
            if (StringUtils.isEmpty(dto.getFatherName())) return "Father name cannot be empty";
            if (StringUtils.isEmpty(dto.getGuardName())) return "Guardian name cannot be empty";
            if (StringUtils.isEmpty(dto.getGuardPan())) return "Guardian PAN cannot be empty";
            if (StringUtils.isEmpty(dto.getGuardDob())) return "Guardian DOB cannot be empty";
            if (StringUtils.isEmpty(dto.getGuardRelation())) return "Guardian relation cannot be empty";
            if (StringUtils.isEmpty(dto.getGuardAccountRelation())) return "Guardian account relation cannot be empty";
        }else
        {
            if (StringUtils.isEmpty(dto.getName())) return "Name cannot be empty";
            if(dto.getSource().equalsIgnoreCase("Website"))
            {
                if (StringUtils.isEmpty(dto.getPan())) return "PAN cannot be empty";
            }
            if(dto.getTaxStatusCode().equalsIgnoreCase("01") || dto.getTaxStatusCode().equalsIgnoreCase("24") ||dto.getTaxStatusCode().equalsIgnoreCase("11") || dto.getTaxStatusCode().equalsIgnoreCase("21") || dto.getTaxStatusCode().equalsIgnoreCase("02") || dto.getTaxStatusCode().equalsIgnoreCase("26")  || dto.getTaxStatusCode().equalsIgnoreCase("28") || dto.getTaxStatusCode().equalsIgnoreCase("61") || dto.getTaxStatusCode().equalsIgnoreCase("62"))
            {
                if (StringUtils.isEmpty(dto.getDob())) return "Date of birth cannot be empty";
            }else
            {
                if (StringUtils.isEmpty(dto.getNetworthDob())) return "Net worth declaration date cannot be empty";
                if (StringUtils.isEmpty(dto.getNetworthAmount())) return "Net worth amount cannot be empty";
            }
        }

        if (StringUtils.isEmpty(dto.getPlaceBirth())) return "Place of birth cannot be empty";
        if (StringUtils.isEmpty(dto.getCountryBirthDesc())) return "Country of birth description cannot be empty";
        if (StringUtils.isEmpty(dto.getCountryBirthCode())) return "Country of birth code cannot be empty";
        if (StringUtils.isEmpty(dto.getOccupationDesc())) return "Occupation description cannot be empty";
        if (StringUtils.isEmpty(dto.getOccupationCode())) return "Occupation code cannot be empty";
        if (StringUtils.isEmpty(dto.getIncomeDesc())) return "Income description cannot be empty";
        if (StringUtils.isEmpty(dto.getIncomeCode())) return "Income code cannot be empty";
        if (StringUtils.isEmpty(dto.getSourceWealthDesc())) return "Source of wealth description cannot be empty";
        if (StringUtils.isEmpty(dto.getSourceWealthCode())) return "Source of wealth code cannot be empty";
        if (StringUtils.isEmpty(dto.getPoliticalStatusDesc())) return "Political status description cannot be empty";
        if (StringUtils.isEmpty(dto.getPoliticalStatusCode())) return "Political status code cannot be empty";
        return null; // No validation errors
    }

    public static String validateNriInfo(NriInfoDTO dto)
    {
        if (StringUtils.isEmpty(dto.getAddress1())) return "Address Line 1 cannot be empty";
        if (StringUtils.isEmpty(dto.getCity())) return "City cannot be empty";
        if (StringUtils.isEmpty(dto.getState())) return "State cannot be empty";
        if (StringUtils.isEmpty(dto.getPincode())) return "Pincode cannot be empty";
        if (StringUtils.isEmpty(dto.getCountry())) return "Country cannot be empty";
        return null;
    }

    public static String validateContactInfo(ContactInfoDTO dto)
    {
        if (StringUtils.isEmpty(dto.getAddress1())) return "Address Line 1 cannot be empty";
        if (StringUtils.isEmpty(dto.getCity())) return "City cannot be empty";
        if (StringUtils.isEmpty(dto.getState())) return "State cannot be empty";
        if (StringUtils.isEmpty(dto.getPincode())) return "Pincode cannot be empty";
        if (StringUtils.isEmpty(dto.getCountry())) return "Country cannot be empty";
        return null;
    }

    public static String validateNomineeInfo(List<NomineeInfoDTO> dtoList)
    {
        if (dtoList == null || dtoList.isEmpty()) return "Nominee list cannot be empty";

        Map<Integer, NomineeInfoDTO> nomineeMap = new HashMap<>();

        for (NomineeInfoDTO dto : dtoList)
        {
            if (dto.getId() != null)
            {
                nomineeMap.put(dto.getId(), dto);
            }
        }

        for (Integer i = 1; i <= 3; i++)
        {
            NomineeInfoDTO nominee = nomineeMap.get(i);

            if (nominee == null) continue; // Skip if nominee not present

            String prefix = "Nominee " + i;

            if(i.equals(1))
            {
                if (StringUtils.isEmpty(nominee.getName())) return prefix + " name cannot be empty";
                if (StringUtils.isEmpty(nominee.getIdType())) return prefix + " ID type cannot be empty";
                if (StringUtils.isEmpty(nominee.getIdNo())) return prefix + " ID number cannot be empty";

                if (StringUtils.isNotEmpty(nominee.getIdType())
                        && nominee.getIdType().equalsIgnoreCase("1")
                        && StringUtils.isNotEmpty(nominee.getIdNo())
                        && nominee.getIdNo().length() > 10) {

                    return "Nominee " + i + " ID Number should be maximum 10 digits!";
                }

                if (StringUtils.isNotEmpty(nominee.getIdType())
                        && nominee.getIdType().equalsIgnoreCase("2")
                        && StringUtils.isNotEmpty(nominee.getIdNo())
                        && nominee.getIdNo().length() > 4) {

                    return "Nominee " + i + " ID Number should be maximum 4 digits!";
                }

                if (StringUtils.isNotEmpty(nominee.getIdType())
                        && nominee.getIdType().equalsIgnoreCase("3")
                        && StringUtils.isNotEmpty(nominee.getIdNo())
                        && nominee.getIdNo().length() > 16) {

                    return "Nominee " + i + " ID Number should be maximum 16 digits!";
                }

                if (StringUtils.isNotEmpty(nominee.getIdType())
                        && nominee.getIdType().equalsIgnoreCase("4")
                        && StringUtils.isNotEmpty(nominee.getIdNo())
                        && nominee.getIdNo().length() > 8) {

                    return "Nominee " + i + " ID Number should be maximum 8 digits!";
                }

                if (StringUtils.isEmpty(nominee.getType())) return prefix + " type cannot be empty";
                if (StringUtils.isEmpty(nominee.getTypeDesc())) return prefix + " type description cannot be empty";
                if (StringUtils.isEmpty(nominee.getRelation())) return prefix + " relation cannot be empty";

                if (StringUtils.isEmpty(nominee.getAddress1())) return prefix + " address1 cannot be empty";
                if (StringUtils.isEmpty(nominee.getPincode())) return prefix + " pincode cannot be empty";
                if (StringUtils.isEmpty(nominee.getCity())) return prefix + " city cannot be empty";
                if (StringUtils.isEmpty(nominee.getState())) return prefix + " state cannot be empty";
                if (StringUtils.isEmpty(nominee.getStateCode())) return prefix + " state code cannot be empty";
                if (StringUtils.isEmpty(nominee.getCountry())) return prefix + " country cannot be empty";
                if (StringUtils.isEmpty(nominee.getPercentage())) return prefix + " percentage cannot be empty";

                if(StringUtils.isNotEmpty(nominee.getType()) &&  nominee.getType().equalsIgnoreCase("Y"))
                {
                    if (StringUtils.isEmpty(nominee.getDob())) return prefix + " DOB cannot be empty";
                    if (StringUtils.isEmpty(nominee.getGuardName())) return prefix + " guardian name cannot be empty";
                    if (StringUtils.isEmpty(nominee.getGuardRelation())) return prefix + " guardian relation cannot be empty";
                }
            }

            if(i.equals(2))
            {
                if(StringHelper.isNotEmpty(nominee.getName()))
                {
                    if (StringUtils.isEmpty(nominee.getName())) return prefix + " name cannot be empty";
                    if (StringUtils.isEmpty(nominee.getIdType())) return prefix + " ID type cannot be empty";
                    if (StringUtils.isEmpty(nominee.getIdNo())) return prefix + " ID number cannot be empty";

                    if (StringUtils.isNotEmpty(nominee.getIdType())
                            && nominee.getIdType().equalsIgnoreCase("1")
                            && StringUtils.isNotEmpty(nominee.getIdNo())
                            && nominee.getIdNo().length() > 10) {

                        return "Nominee " + i + " ID Number should be maximum 10 digits!";
                    }

                    if (StringUtils.isNotEmpty(nominee.getIdType())
                            && nominee.getIdType().equalsIgnoreCase("2")
                            && StringUtils.isNotEmpty(nominee.getIdNo())
                            && nominee.getIdNo().length() > 4) {

                        return "Nominee " + i + " ID Number should be maximum 4 digits!";
                    }

                    if (StringUtils.isNotEmpty(nominee.getIdType())
                            && nominee.getIdType().equalsIgnoreCase("3")
                            && StringUtils.isNotEmpty(nominee.getIdNo())
                            && nominee.getIdNo().length() > 16) {

                        return "Nominee " + i + " ID Number should be maximum 16 digits!";
                    }

                    if (StringUtils.isNotEmpty(nominee.getIdType())
                            && nominee.getIdType().equalsIgnoreCase("4")
                            && StringUtils.isNotEmpty(nominee.getIdNo())
                            && nominee.getIdNo().length() > 8) {

                        return "Nominee " + i + " ID Number should be maximum 8 digits!";
                    }

                    if (StringUtils.isEmpty(nominee.getType())) return prefix + " type cannot be empty";
                    if (StringUtils.isEmpty(nominee.getTypeDesc())) return prefix + " type description cannot be empty";
                    if (StringUtils.isEmpty(nominee.getRelation())) return prefix + " relation cannot be empty";

                    if (StringUtils.isEmpty(nominee.getAddress1())) return prefix + " address1 cannot be empty";
                    if (StringUtils.isEmpty(nominee.getPincode())) return prefix + " pincode cannot be empty";
                    if (StringUtils.isEmpty(nominee.getCity())) return prefix + " city cannot be empty";
                    if (StringUtils.isEmpty(nominee.getState())) return prefix + " state cannot be empty";
                    if (StringUtils.isEmpty(nominee.getStateCode())) return prefix + " state code cannot be empty";
                    if (StringUtils.isEmpty(nominee.getCountry())) return prefix + " country cannot be empty";
                    if (StringUtils.isEmpty(nominee.getPercentage())) return prefix + " percentage cannot be empty";

                    if(StringUtils.isNotEmpty(nominee.getType()) &&  nominee.getType().equalsIgnoreCase("Y"))
                    {
                        if (StringUtils.isEmpty(nominee.getDob())) return prefix + " DOB cannot be empty";
                        if (StringUtils.isEmpty(nominee.getGuardName())) return prefix + " guardian name cannot be empty";
                        if (StringUtils.isEmpty(nominee.getGuardRelation())) return prefix + " guardian relation cannot be empty";
                    }
                }
            }

            if(i.equals(3))
            {
                if(StringHelper.isNotEmpty(nominee.getName()))
                {
                    if (StringUtils.isEmpty(nominee.getName())) return prefix + " name cannot be empty";
                    if (StringUtils.isEmpty(nominee.getIdType())) return prefix + " ID type cannot be empty";
                    if (StringUtils.isEmpty(nominee.getIdNo())) return prefix + " ID number cannot be empty";

                    if (StringUtils.isNotEmpty(nominee.getIdType())
                            && nominee.getIdType().equalsIgnoreCase("1")
                            && StringUtils.isNotEmpty(nominee.getIdNo())
                            && nominee.getIdNo().length() > 10) {

                        return "Nominee " + i + " ID Number should be maximum 10 digits!";
                    }

                    if (StringUtils.isNotEmpty(nominee.getIdType())
                            && nominee.getIdType().equalsIgnoreCase("2")
                            && StringUtils.isNotEmpty(nominee.getIdNo())
                            && nominee.getIdNo().length() > 4) {

                        return "Nominee " + i + " ID Number should be maximum 4 digits!";
                    }

                    if (StringUtils.isNotEmpty(nominee.getIdType())
                            && nominee.getIdType().equalsIgnoreCase("3")
                            && StringUtils.isNotEmpty(nominee.getIdNo())
                            && nominee.getIdNo().length() > 16) {

                        return "Nominee " + i + " ID Number should be maximum 16 digits!";
                    }

                    if (StringUtils.isNotEmpty(nominee.getIdType())
                            && nominee.getIdType().equalsIgnoreCase("4")
                            && StringUtils.isNotEmpty(nominee.getIdNo())
                            && nominee.getIdNo().length() > 8) {

                        return "Nominee " + i + " ID Number should be maximum 8 digits!";
                    }

                    if (StringUtils.isEmpty(nominee.getType())) return prefix + " type cannot be empty";
                    if (StringUtils.isEmpty(nominee.getTypeDesc())) return prefix + " type description cannot be empty";
                    if (StringUtils.isEmpty(nominee.getRelation())) return prefix + " relation cannot be empty";

                    if (StringUtils.isEmpty(nominee.getAddress1())) return prefix + " address1 cannot be empty";
                    if (StringUtils.isEmpty(nominee.getPincode())) return prefix + " pincode cannot be empty";
                    if (StringUtils.isEmpty(nominee.getCity())) return prefix + " city cannot be empty";
                    if (StringUtils.isEmpty(nominee.getState())) return prefix + " state cannot be empty";
                    if (StringUtils.isEmpty(nominee.getStateCode())) return prefix + " state code cannot be empty";
                    if (StringUtils.isEmpty(nominee.getCountry())) return prefix + " country cannot be empty";
                    if (StringUtils.isEmpty(nominee.getPercentage())) return prefix + " percentage cannot be empty";

                    if(StringUtils.isNotEmpty(nominee.getType()) &&  nominee.getType().equalsIgnoreCase("Y"))
                    {
                        if (StringUtils.isEmpty(nominee.getDob())) return prefix + " DOB cannot be empty";
                        if (StringUtils.isEmpty(nominee.getGuardName())) return prefix + " guardian name cannot be empty";
                        if (StringUtils.isEmpty(nominee.getGuardRelation())) return prefix + " guardian relation cannot be empty";
                    }
                }
            }
        }
        return null;
    }

    public static String validateJointHolderInfo(List<JointHolderInfoDTO> dtoList)
    {
        if (dtoList == null || dtoList.isEmpty()) return "Nominee list cannot be empty";

        Map<Integer, JointHolderInfoDTO> jointHolderMap = new HashMap<>();

        for (JointHolderInfoDTO dto : dtoList)
        {
            if (dto.getId() != null)
            {
                jointHolderMap.put(dto.getId(), dto);
            }
        }

        for (Integer i = 1; i <= 2; i++)
        {
            JointHolderInfoDTO jointHolder = jointHolderMap.get(i);

            if (jointHolder == null) continue;

            if(i.equals(1))
            {
                String prefix = "Joint Holder " + i;

                if (StringUtils.isEmpty(jointHolder.getName()))
                {
                    return prefix + " name cannot be empty!";
                }

                if (StringUtils.isEmpty(jointHolder.getDob()))
                {
                    return prefix + " date of birth cannot be empty!";
                }

                if (StringUtils.isEmpty(jointHolder.getPlaceBirth()))
                {
                    return prefix + " place of birth cannot be empty!";
                }

                if (StringUtils.isEmpty(jointHolder.getCountryBirth()))
                {
                    return prefix + " country of birth cannot be empty!";
                }

                if (StringUtils.isEmpty(jointHolder.getOccupation()))
                {
                    return prefix + " occupation cannot be empty!";
                }

                if (StringUtils.isEmpty(jointHolder.getIncome()))
                {
                    return prefix + " income cannot be empty!";
                }

                if (StringUtils.isEmpty(jointHolder.getSourceWealth()))
                {
                    return prefix + " source of wealth cannot be empty!";
                }

                if (StringUtils.isEmpty(jointHolder.getAddressType()))
                {
                    return prefix + " address type cannot be empty!";
                }

                if (StringUtils.isEmpty(jointHolder.getPolitical()))
                {
                    return prefix + " political exposure cannot be empty!";
                }

                if (StringUtils.isEmpty(jointHolder.getEmail()))
                {
                    return prefix + " email cannot be empty!";
                }

                if (StringUtils.isEmpty(jointHolder.getEmailRelation()))
                {
                    return prefix + " email relation cannot be empty!";
                }

                if (StringUtils.isEmpty(jointHolder.getMobile()))
                {
                    return prefix + " mobile number cannot be empty!";
                }

                if (StringUtils.isEmpty(jointHolder.getMobileRelation()))
                {
                    return prefix + " mobile relation cannot be empty!";
                }

                if (StringUtils.isEmpty(jointHolder.getPan()) || !jointHolder.getPan().matches("[A-Z]{5}[0-9]{4}[A-Z]{1}"))
                {
                    return prefix + " PAN is not valid!";
                }
            }

            if(i.equals(2))
            {
                String prefix = "Joint Holder " + i;

                if(StringHelper.isNotEmpty(jointHolder.getName()) && StringHelper.isNotEmpty(jointHolder.getPan()))
                {
                    if (StringUtils.isEmpty(jointHolder.getName()))
                    {
                        return prefix + " name cannot be empty!";
                    }

                    if (StringUtils.isEmpty(jointHolder.getDob()))
                    {
                        return prefix + " date of birth cannot be empty!";
                    }

                    if (StringUtils.isEmpty(jointHolder.getPlaceBirth()))
                    {
                        return prefix + " place of birth cannot be empty!";
                    }

                    if (StringUtils.isEmpty(jointHolder.getCountryBirth()))
                    {
                        return prefix + " country of birth cannot be empty!";
                    }

                    if (StringUtils.isEmpty(jointHolder.getOccupation()))
                    {
                        return prefix + " occupation cannot be empty!";
                    }

                    if (StringUtils.isEmpty(jointHolder.getIncome()))
                    {
                        return prefix + " income cannot be empty!";
                    }

                    if (StringUtils.isEmpty(jointHolder.getSourceWealth()))
                    {
                        return prefix + " source of wealth cannot be empty!";
                    }

                    if (StringUtils.isEmpty(jointHolder.getAddressType()))
                    {
                        return prefix + " address type cannot be empty!";
                    }

                    if (StringUtils.isEmpty(jointHolder.getPolitical()))
                    {
                        return prefix + " political exposure cannot be empty!";
                    }

                    if (StringUtils.isEmpty(jointHolder.getEmail()))
                    {
                        return prefix + " email cannot be empty!";
                    }

                    if (StringUtils.isEmpty(jointHolder.getEmailRelation()))
                    {
                        return prefix + " email relation cannot be empty!";
                    }

                    if (StringUtils.isEmpty(jointHolder.getMobile()))
                    {
                        return prefix + " mobile number cannot be empty!";
                    }

                    if (StringUtils.isEmpty(jointHolder.getMobileRelation()))
                    {
                        return prefix + " mobile relation cannot be empty!";
                    }

                    if (StringUtils.isEmpty(jointHolder.getPan()) || !jointHolder.getPan().matches("[A-Z]{5}[0-9]{4}[A-Z]{1}"))
                    {
                        return prefix + " PAN is not valid!";
                    }
                }
            }
        }

        return null;
    }

    public static String validateBankInfo(BankInfoDTO dto)
    {
        if (dto == null) return "Bank information is required";

        if (StringUtils.isEmpty(dto.getIfscCode())) return "IFSC Code cannot be empty";
        if (StringUtils.isNotEmpty(dto.getIfscCode()) && dto.getIfscCode().length() != 11) return "Provide valid bank IFSC code";
        if (StringUtils.isEmpty(dto.getBankCode())) return "Bank Code cannot be empty";
        if (StringUtils.isEmpty(dto.getBankName())) return "Bank Name cannot be empty";
        if (StringUtils.isEmpty(dto.getAccountNumber())) return "Account Number cannot be empty";
        if (StringUtils.isEmpty(dto.getAccountHolderName())) return "Account Holder Name cannot be empty";
        if (StringUtils.isEmpty(dto.getAccountType())) return "Account Type cannot be empty";
        return null;
    }

}
