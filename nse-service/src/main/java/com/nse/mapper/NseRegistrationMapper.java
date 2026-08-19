package com.nse.mapper;

import com.nse.dto.mf.UserDto;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class NseRegistrationMapper {

    public JSONObject buildRegistrationJson(UserDto userDto,
                                            String primary_holder_kyc_type,
                                            String primary_holder_ckyc_number,
                                            String second_holder_kyc_type,
                                            String second_holder_ckyc_number,
                                            String third_holder_kyc_type,
                                            String third_holder_ckyc_number,
                                            String guardian_kyc_type,
                                            String guardian_ckyc_number,
                                            String nomination_opt,
                                            String nomination_authentication) {


            JSONArray regDetailsArray = new JSONArray();
            JSONObject regObject = new JSONObject();

            regObject.put("client_code", StringUtils.defaultString(userDto.getNse_iin_number()));
            regObject.put("primary_holder_first_name", StringUtils.defaultString(userDto.getName()));
            regObject.put("primary_holder_middle_name", "");
            regObject.put("primary_holder_last_name", "");
            regObject.put("tax_status", StringUtils.defaultString(userDto.getTax_status_code()));
            String tax_status = StringUtils.defaultString(userDto.getTax_status_code());

            List<String> taxStatusList = Arrays.asList ("03", "04", "06", "07", "08", "13", "10", "47");
            if(taxStatusList.contains(tax_status)){
                regObject.put("gender", "O");
            }else{
                regObject.put("gender", StringUtils.defaultString(userDto.getGender()));
            }

            regObject.put("primary_holder_dob_incorporation", StringUtils.defaultString(userDto.getDate_of_birth()));
            regObject.put("occupation_code", StringUtils.defaultString(userDto.getOccupation_code()));
            regObject.put("holding_nature", StringUtils.defaultString(userDto.getHolding_nature_code()));

            regObject.put("second_holder_first_name", StringUtils.defaultString(userDto.getJoint_holder_name1()));
            regObject.put("second_holder_middle_name", "");
            regObject.put("second_holder_last_name", "");
            regObject.put("third_holder_first_name", StringUtils.defaultString(userDto.getJoint_holder_name2()));
            regObject.put("third_holder_middle_name", "");
            regObject.put("third_holder_last_name", "");
            regObject.put("second_holder_dob", StringUtils.defaultString(userDto.getJoint_holder_dob1()));
            regObject.put("third_holder_dob", StringUtils.defaultString(userDto.getJoint_holder_dob2()));

            regObject.put("guardian_first_name", StringUtils.defaultString(userDto.getGuard_name()));
            regObject.put("guardian_middle_name", "");
            regObject.put("guardian_last_name", "");
            regObject.put("guardian_dob", StringUtils.defaultString(userDto.getGuard_dob()));
            System.out.println("Tax_status_code: " +userDto.getTax_status_code());
        /*if(userDto.getTax_status_code().equalsIgnoreCase("02")){
            regObject.put("primary_holder_pan_exempt", "Y");
        }else{
            regObject.put("primary_holder_pan_exempt", "");
        }*/
            regObject.put("primary_holder_pan_exempt", "");
            regObject.put("second_holder_pan_exempt", "");
            regObject.put("third_holder_pan_exempt", "");
            regObject.put("guardian_pan_exempt", "");
            if(userDto.getTax_status_code().equalsIgnoreCase("02")){
                regObject.put("primary_holder_pan", StringUtils.defaultString(userDto.getGuard_pan()));
            }else{
                regObject.put("primary_holder_pan", StringUtils.defaultString(userDto.getPan()));
            }
            regObject.put("second_holder_pan", StringUtils.defaultString(userDto.getJoint_holder_pan1()));
            regObject.put("third_holder_pan", StringUtils.defaultString(userDto.getJoint_holder_pan2()));
            regObject.put("guardian_pan", StringUtils.defaultString(userDto.getGuard_pan()));

        /*if(userDto.getTax_status_code().equalsIgnoreCase("02")){
            regObject.put("primary_holder_exempt_category", "Y");
        }else{
            regObject.put("primary_holder_exempt_category", "");
        }*/
            regObject.put("primary_holder_exempt_category", "");
            regObject.put("second_holder_exempt_category", "");
            regObject.put("third_holder_exempt_category", "");
            regObject.put("guardian_exempt_category", "");

            regObject.put("client_type", "P");
            regObject.put("pms", "");
            regObject.put("default_dp", "");
            regObject.put("cdsl_dpid", "");
            regObject.put("cdslcltid", "");
            regObject.put("cmbp_id", "");
            regObject.put("nsdldpid", "");
            regObject.put("nsdlcltid", "");

            regObject.put("account_type_1", StringUtils.defaultString(userDto.getBank_account_type1()));
            regObject.put("account_no_1", StringUtils.defaultString(userDto.getBank_account_number1()));
            regObject.put("micr_no_1", StringUtils.defaultString(userDto.getBank_micr_code1()));
            regObject.put("ifsc_code_1", StringUtils.defaultString(userDto.getBank_ifsc_code1()));
            regObject.put("default_bank_flag_1", "Y");

// Remaining banks blank
            regObject.put("account_type_2", "");
            regObject.put("account_no_2", "");
            regObject.put("micr_no_2", "");
            regObject.put("ifsc_code_2", "");
            regObject.put("default_bank_flag_2", "");
// ... same for 3,4,5
            regObject.put("account_type_3", "");
            regObject.put("account_no_3", "");
            regObject.put("micr_no_3", "");
            regObject.put("ifsc_code_3", "");
            regObject.put("default_bank_flag_3", "");
            regObject.put("account_type_4", "");
            regObject.put("account_no_4", "");
            regObject.put("micr_no_4", "");
            regObject.put("ifsc_code_4", "");
            regObject.put("default_bank_flag_4", "");
            regObject.put("account_type_5", "");
            regObject.put("account_no_5", "");
            regObject.put("micr_no_5", "");
            regObject.put("ifsc_code_5", "");
            regObject.put("default_bank_flag_5", "");

            regObject.put("cheque_name", "");
            regObject.put("div_pay_mode", "02");

            regObject.put("address_1", StringUtils.defaultString(userDto.getStreet_1()));
            regObject.put("address_2", StringUtils.defaultString(userDto.getStreet_2()));
            regObject.put("address_3", StringUtils.defaultString(userDto.getStreet_3()));
            regObject.put("city", StringUtils.defaultString(userDto.getCity()));
            regObject.put("state", StringUtils.defaultString(userDto.getState_code()));
            regObject.put("pincode", StringUtils.defaultString(userDto.getPincode()));
            regObject.put("country", StringUtils.defaultString(userDto.getCountry()));

            regObject.put("resi_phone", "");
            regObject.put("resi_fax", "");
            regObject.put("office_phone", "");
            regObject.put("office_fax", "");

            regObject.put("email", StringUtils.defaultString(userDto.getEmail()));
            regObject.put("communication_mode", "E");

            if(userDto.getBank_account_type1().equalsIgnoreCase("NE") || userDto.getBank_account_type1().equalsIgnoreCase("NO")){
                regObject.put("foreign_address_1", StringUtils.defaultString(userDto.getNri_address1()));
                regObject.put("foreign_address_2", StringUtils.defaultString(userDto.getNri_address2()));
                regObject.put("foreign_address_3", StringUtils.defaultString(userDto.getNri_address3()));
                regObject.put("foreign_address_city", StringUtils.defaultString(userDto.getNri_city()));
                regObject.put("foreign_address_pincode", StringUtils.defaultString(userDto.getNri_pincode()));
                regObject.put("foreign_address_state", StringUtils.defaultString(userDto.getNri_state()));
                regObject.put("foreign_address_country", StringUtils.defaultString(userDto.getNri_country())); //AE
                regObject.put("foreign_address_resi_phone", "");
                regObject.put("foreign_address_fax", "");
                regObject.put("foreign_address_off_phone", "");
                regObject.put("foreign_address_off_fax", "");
            }else{
                regObject.put("foreign_address_1", "");
                regObject.put("foreign_address_2", "");
                regObject.put("foreign_address_3", "");
                regObject.put("foreign_address_city", "");
                regObject.put("foreign_address_pincode", "");
                regObject.put("foreign_address_state", "");
                regObject.put("foreign_address_country", "");
                regObject.put("foreign_address_resi_phone", "");
                regObject.put("foreign_address_fax", "");
                regObject.put("foreign_address_off_phone", "");
                regObject.put("foreign_address_off_fax", "");
            }

            regObject.put("indian_mobile_no", StringUtils.defaultString(userDto.getMobile()));

// prepared kyc variables
            regObject.put("primary_holder_kyc_type", primary_holder_kyc_type);
            regObject.put("primary_holder_ckyc_number", primary_holder_ckyc_number);
            regObject.put("second_holder_kyc_type", second_holder_kyc_type);
            regObject.put("second_holder_ckyc_number", second_holder_ckyc_number);
            regObject.put("third_holder_kyc_type", third_holder_kyc_type);
            regObject.put("third_holder_ckyc_number", third_holder_ckyc_number);
            regObject.put("guardian_kyc_type", guardian_kyc_type);
            regObject.put("guardian_ckyc_number", guardian_ckyc_number);

            regObject.put("primary_holder_kra_exempt_ref_no", "");
            regObject.put("second_holder_kra_exempt_ref_no", "");
            regObject.put("third_holder_kra_exempt_ref_no", "");
            regObject.put("guardian_exempt_ref_no", "");

            regObject.put("aadhaar_updated", "");
            regObject.put("mapin_id", "");
            regObject.put("paperless_flag", "Z");
            regObject.put("lei_no", "");
            regObject.put("lei_validity", "");

            regObject.put("mobile_declaration_flag", StringUtils.defaultString(userDto.getMobile_relation()));
            regObject.put("email_declaration_flag", StringUtils.defaultString(userDto.getEmail_relation()));

            regObject.put("second_holder_email", StringUtils.defaultString(userDto.getJoint_holder_email1()));
            regObject.put("second_holder_email_declaration", StringUtils.defaultString(userDto.getJoint_holder_email_relation1()));
            regObject.put("second_holder_mobile", StringUtils.defaultString(userDto.getJoint_holder_mobile1()));
            regObject.put("second_holder_mobile_declaration", StringUtils.defaultString(userDto.getJoint_holder_mobile_relation1()));

            regObject.put("third_holder_email", StringUtils.defaultString(userDto.getJoint_holder_email2()));
            regObject.put("third_holder_email_declaration", StringUtils.defaultString(userDto.getJoint_holder_email_relation2()));
            regObject.put("third_holder_mobile", StringUtils.defaultString(userDto.getJoint_holder_mobile2()));
            regObject.put("third_holder_mobile_declaration", StringUtils.defaultString(userDto.getJoint_holder_mobile_relation2()));

            regObject.put("guardian_relation", StringUtils.defaultString(userDto.getGuard_relationship()));

            List<String> nominationOptTaxStatusList = Arrays.asList ("02","03", "04", "06", "07", "08", "13", "10", "47", "26", "28");
            if(nominationOptTaxStatusList.contains(tax_status))
            {
                regObject.put("nomination_opt", "N");
                regObject.put("nomination_authentication", "");

                // nominee 1
                regObject.put("nominee_1_name", "");
                regObject.put("nominee_1_relationship", "");
                regObject.put("nominee_1_applicable", "");
                regObject.put("nominee_1_minor_flag", "");
                regObject.put("nominee_1_dob", "");
                regObject.put("nominee_1_guardian", "");
                regObject.put("nominee_1_guardian_pan", "");
                regObject.put("nominee_1_identity_type", "");
                regObject.put("nominee_1_identity_number", "");
                regObject.put("nominee_1_email", "");
                regObject.put("nominee_1_mobile", "");
                regObject.put("nominee_1_address1", "");
                regObject.put("nominee_1_address2", "");
                regObject.put("nominee_1_address3", "");
                regObject.put("nominee_1_city", "");
                regObject.put("nominee_1_pin", "");
                regObject.put("nominee_1_country", "");

                // nominee 2
                regObject.put("nominee_2_name", "");
                regObject.put("nominee_2_relationship", "");
                regObject.put("nominee_2_applicable", "");
                regObject.put("nominee_2_dob", "");
                regObject.put("nominee_2_minor_flag", "");
                regObject.put("nominee_2_guardian", "");
                regObject.put("nominee_2_guardian_pan", "");
                regObject.put("nominee_2_identity_type", "");
                regObject.put("nominee_2_identity_number", "");
                regObject.put("nominee_2_email", "");
                regObject.put("nominee_2_mobile", "");
                regObject.put("nominee_2_address1", "");
                regObject.put("nominee_2_city", "");
                regObject.put("nominee_2_pin", "");
                regObject.put("nominee_2_country", "");

                // nominee 3
                regObject.put("nominee_3_name", "");
                regObject.put("nominee_3_relationship", "");
                regObject.put("nominee_3_applicable", "");
                regObject.put("nominee_3_dob", "");
                regObject.put("nominee_3_minor_flag", "");
                regObject.put("nominee_3_guardian", "");
                regObject.put("nominee_3_guardian_pan", "");
                regObject.put("nominee_3_identity_type", "");
                regObject.put("nominee_3_identity_number", "");
                regObject.put("nominee_3_email", "");
                regObject.put("nominee_3_mobile", "");
                regObject.put("nominee_3_address1", "");
                regObject.put("nominee_3_city", "");
                regObject.put("nominee_3_pin", "");
                regObject.put("nominee_3_country", "");

                regObject.put("nominee_soa", "");
            }else
            {
                regObject.put("nomination_opt", nomination_opt);
                regObject.put("nomination_authentication", nomination_authentication);

                // nominee 1
                regObject.put("nominee_1_name", StringUtils.defaultString(userDto.getNominee1_name()));
                regObject.put("nominee_1_relationship", StringUtils.defaultString(userDto.getNominee1_relation()));
                regObject.put("nominee_1_applicable", StringUtils.defaultString(userDto.getNominee1_percentage()));
                regObject.put("nominee_1_minor_flag", StringUtils.defaultString(userDto.getNominee1_type()));
                regObject.put("nominee_1_dob", StringUtils.defaultString(userDto.getNominee1_dob()));
                regObject.put("nominee_1_guardian", StringUtils.defaultString(userDto.getNominee1_guard_name()));
                regObject.put("nominee_1_guardian_pan", StringUtils.defaultString(userDto.getNominee1_guard_pan()));
                regObject.put("nominee_1_identity_type", StringUtils.defaultString(userDto.getNominee1_id_type()));
                regObject.put("nominee_1_identity_number", StringUtils.defaultString(userDto.getNominee1_id_no()));
                regObject.put("nominee_1_email", StringUtils.defaultString(userDto.getNominee1_email()));
                regObject.put("nominee_1_mobile", StringUtils.defaultString(userDto.getNominee1_mobile()));
                regObject.put("nominee_1_address1", StringUtils.defaultString(userDto.getNominee1_address1()));
                regObject.put("nominee_1_address2", StringUtils.defaultString(userDto.getNominee1_address2()));
                regObject.put("nominee_1_address3", StringUtils.defaultString(userDto.getNominee1_address3()));
                regObject.put("nominee_1_city", StringUtils.defaultString(userDto.getNominee1_city()));
                regObject.put("nominee_1_pin", StringUtils.defaultString(userDto.getNominee1_pincode()));
                regObject.put("nominee_1_country", StringUtils.defaultString(userDto.getNominee1_country()));

                // nominee 2
                regObject.put("nominee_2_name", StringUtils.defaultString(userDto.getNominee2_name()));
                regObject.put("nominee_2_relationship", StringUtils.defaultString(userDto.getNominee2_relation()));
                regObject.put("nominee_2_applicable", StringUtils.defaultString(userDto.getNominee2_percentage()));
                regObject.put("nominee_2_dob", StringUtils.defaultString(userDto.getNominee2_dob()));
                regObject.put("nominee_2_minor_flag", StringUtils.defaultString(userDto.getNominee2_type()));
                regObject.put("nominee_2_guardian", StringUtils.defaultString(userDto.getNominee2_guard_name()));
                regObject.put("nominee_2_guardian_pan", StringUtils.defaultString(userDto.getNominee2_guard_pan()));
                regObject.put("nominee_2_identity_type", StringUtils.defaultString(userDto.getNominee2_id_type()));
                regObject.put("nominee_2_identity_number", StringUtils.defaultString(userDto.getNominee2_id_no()));
                regObject.put("nominee_2_email", StringUtils.defaultString(userDto.getNominee2_email()));
                regObject.put("nominee_2_mobile", StringUtils.defaultString(userDto.getNominee2_mobile()));
                regObject.put("nominee_2_address1", StringUtils.defaultString(userDto.getNominee2_address1()));
                //regObject.put("nominee_2_address2", StringUtils.defaultString(userDto.getNominee1_address2()));
                // regObject.put("nominee_2_address3", StringUtils.defaultString(userDto.getNominee2_address3()));
                regObject.put("nominee_2_city", StringUtils.defaultString(userDto.getNominee2_city()));
                regObject.put("nominee_2_pin", StringUtils.defaultString(userDto.getNominee2_pincode()));
                regObject.put("nominee_2_country", StringUtils.defaultString(userDto.getNominee2_country()));

                // nominee 3
                regObject.put("nominee_3_name", StringUtils.defaultString(userDto.getNominee3_name()));
                regObject.put("nominee_3_relationship", StringUtils.defaultString(userDto.getNominee3_relation()));
                regObject.put("nominee_3_applicable", StringUtils.defaultString(userDto.getNominee3_percentage()));
                regObject.put("nominee_3_dob", StringUtils.defaultString(userDto.getNominee3_dob()));
                regObject.put("nominee_3_minor_flag", StringUtils.defaultString(userDto.getNominee3_type()));
                regObject.put("nominee_3_guardian", StringUtils.defaultString(userDto.getNominee3_guard_name()));
                regObject.put("nominee_3_guardian_pan", StringUtils.defaultString(userDto.getNominee3_guard_pan()));
                regObject.put("nominee_3_identity_type", StringUtils.defaultString(userDto.getNominee3_id_type()));
                regObject.put("nominee_3_identity_number", StringUtils.defaultString(userDto.getNominee3_id_no()));
                regObject.put("nominee_3_email", StringUtils.defaultString(userDto.getNominee3_email()));
                regObject.put("nominee_3_mobile", StringUtils.defaultString(userDto.getNominee3_mobile()));
                regObject.put("nominee_3_address1", StringUtils.defaultString(userDto.getNominee3_address1()));
                // regObject.put("nominee_3_address2", StringUtils.defaultString(userDto.getNominee3_address2()));
                //regObject.put("nominee_3_address3", StringUtils.defaultString(userDto.getNominee3_address3()));
                regObject.put("nominee_3_city", StringUtils.defaultString(userDto.getNominee3_city()));
                regObject.put("nominee_3_pin", StringUtils.defaultString(userDto.getNominee3_pincode()));
                regObject.put("nominee_3_country", StringUtils.defaultString(userDto.getNominee3_country()));

                regObject.put("nominee_soa", "N");
            }


            regObject.put("reg_id", "");
            regObject.put("reg_status", "");
            regObject.put("reg_remark", "");
            regDetailsArray.put(regObject);

            JSONObject requestBody = new JSONObject();
            requestBody.put("reg_details", regDetailsArray);

            return requestBody;

    }
}