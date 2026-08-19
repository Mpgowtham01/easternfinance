package com.user.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.user.dto.*;
import com.user.pojo.CommonPojo;
import com.user.pojo.IfscCodePojo;
import com.user.response.*;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.internal.util.StringHelper;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserUtils
{

    public static int investorTypeId = 1;
    public static int rmTypeId = 2;
    public static int fhTypeId = 3;
    public static int subTypeId = 4;
    public static int adminTypeId = 5;
    public static int backOfficeTypeId = 6;
    public static int branchManagerTypeId = 7;
    public static int agentManager = 8;
    public static int AdvisorkhojSuperAdmin = 9;
    public static String expection = "Expection";

    public static String checkParem(String param)
    {
        if (param == null || param.trim().equalsIgnoreCase("null") || param.trim().equalsIgnoreCase("undefined"))
        {
            return "";
        }

        return param.trim();
    }

    public static ResponseEntity<OnboardingSuccessResponse> successResponseForOnboarding(String returnMsg, HttpStatus statusCode, String investor_code) {
        OnboardingSuccessResponse response = new OnboardingSuccessResponse(statusCode.value(), statusCode.getReasonPhrase(), investor_code,returnMsg);
        return ResponseEntity.ok(response);
    }

    public static ResponseEntity<SuccessResponse> successResponse(String returnMsg, HttpStatus statusCode) {
        SuccessResponse response = new SuccessResponse(statusCode.value(), statusCode.getReasonPhrase(), returnMsg);
        return ResponseEntity.ok(response);
    }

    public static ResponseEntity<ErrorResponse> errorResponse(String returnMsg, HttpStatus statusCode) {
        ErrorResponse response = new ErrorResponse(statusCode.value(), statusCode.getReasonPhrase(), returnMsg);
        return ResponseEntity.status(statusCode).body(response);
    }

    public static ResponseEntity<UserResponse> userSuccessResponse(String returnMsg, HttpStatus statusCode, InvestorInfoDTO invest_info, PersonalInfoDTO personal_info, NriInfoDTO nri_info, List<NomineeInfoDTO> nominee_info, List<JointHolderInfoDTO> joint_holder_info, ContactInfoDTO contact_info, BankInfoDTO bank_info) {
        UserResponse response = new UserResponse(statusCode.value(), statusCode.getReasonPhrase(), returnMsg,invest_info,personal_info,nri_info, contact_info, nominee_info, joint_holder_info, bank_info);
        return ResponseEntity.ok(response);
    }

    public static boolean validatePan(String pan)
    {
        String panPattern = "[A-Z]{3}[PCFTGHLABJ]{1}[A-Z]{1}[0-9]{4}[A-Z]{1}";
        Pattern pattern = Pattern.compile(panPattern);
        Matcher matcher = pattern.matcher(pan);
        return matcher.matches();
    }
    public static String checkParameter(String param)
    {
        if (param == null)
        {
            return "";
        }
        return param.trim();
    }


    public static String getUserTypeName(Integer typerId)
    {
        String name = "";

        if(typerId.equals(adminTypeId))
        {
            name = "Admin";
        }else if(typerId.equals(branchManagerTypeId))
        {
            name = "Branch";
        }else if(typerId.equals(rmTypeId))
        {
            name = "RM";
        }else if(typerId.equals(subTypeId))
        {
            name = "SubBroker";
        }else if(typerId.equals(investorTypeId))
        {
            name = "Investor";
        }else if(typerId.equals(fhTypeId))
        {
            name = "Family Head";
        }else
        {
            name = "";
        }

        return name;
    }

    public static PanKYCResponse checkPanKycStatus(String pan) throws Throwable {
        PanKYCResponse commonResponse = new PanKYCResponse();
        try {
            pan = checkParameter(pan);

            if (StringHelper.isEmpty(pan)) {
                commonResponse.setMsg("Please Provide the PAN");
                commonResponse.setKyc_status(true);
                return commonResponse;
            }

            JSONObject requestBody = new JSONObject();
            requestBody.put("pan_no", pan);
            System.out.println("requestBody = " + requestBody.toString());

            String loginUserId = "MFS175151";
            String apiSecret = "3632A753099D7C9CE0635C28A8C07AE3";
            String licenseKey = "3632A753099C7C9CE0635C28A8C07AE3";
            String memberCode = "1001177";
            String url = "https://www.nseinvest.com";

            String base64Encoded = AESEncryptionUtilV2.base64EncodedAuth(apiSecret, licenseKey, loginUserId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("memberId", memberCode);
            headers.set("Authorization", "Basic " + base64Encoded);
            headers.set("User-Agent", "PostmanRuntime/7.43.3");
            headers.set("Accept-Encoding", "gzip, deflate, br");
            headers.set("Accept-Language", "en-US");
            headers.set("Connection", "keep-alive");
            headers.set("Referer", "");

            RestTemplate restTemplate = RestTemplateFactory.createRestTemplate();
            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
            String requestUrl = url + "/nsemfdesk/api/v2/utility/KYC_CHECK";

            String responseBody = null;
            int maxAttempts = 5;

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    ResponseEntity<String> response = restTemplate.postForEntity(requestUrl, entity, String.class);
                    if (response.getStatusCode() == HttpStatus.OK) {
                        responseBody = response.getBody();
                        System.out.println("Attempt " + attempt + " response: " + responseBody);

                        if (responseBody != null &&
                                responseBody.contains("kyc_status") &&
                                !responseBody.contains("Timeout") &&
                                !responseBody.contains("Service Down") &&
                                !responseBody.contains("Connection")) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Attempt " + attempt + " failed: " + e.getMessage());
                }
            }

            System.out.println("Final responseBody = " + responseBody);

            if (responseBody == null || responseBody.isEmpty()) {
                commonResponse.setInv_name("");
                commonResponse.setStatus(HttpStatus.BAD_REQUEST.value());
                commonResponse.setStatus_msg(HttpStatus.BAD_REQUEST.getReasonPhrase());
                commonResponse.setMsg("KYC API did not respond properly after retries.");
                commonResponse.setKyc_status(false);
                return commonResponse;
            }

            JSONObject json;
            try {
                json = new JSONObject(responseBody);
            } catch (Exception e) {
                commonResponse.setInv_name("");
                commonResponse.setStatus(HttpStatus.BAD_REQUEST.value());
                commonResponse.setStatus_msg(HttpStatus.BAD_REQUEST.getReasonPhrase());
                commonResponse.setMsg("Invalid JSON response from KYC API");
                commonResponse.setKyc_status(false);
                return commonResponse;
            }

            String invName = json.optString("name", "");
            String kycStatus = json.optString("kyc_status", "");
            String kycRemark = json.optString("kyc_status_remark", "");

            if ("S".equalsIgnoreCase(kycStatus) && !kycRemark.equalsIgnoreCase("UNDER_PROCESS")) {
                commonResponse.setInv_name(invName);
                commonResponse.setStatus(HttpStatus.OK.value());
                commonResponse.setStatus_msg(HttpStatus.OK.getReasonPhrase());
                commonResponse.setMsg("Congratulations! You are Mutual Fund KYC Compliant.");
                commonResponse.setKyc_status(true);
            } else if (kycStatus.isEmpty()) {
                commonResponse.setInv_name("");
                commonResponse.setStatus(HttpStatus.BAD_REQUEST.value());
                commonResponse.setStatus_msg(HttpStatus.BAD_REQUEST.getReasonPhrase());
                commonResponse.setMsg("KYC API returned incomplete data after retries.");
                commonResponse.setKyc_status(false);
            } else {
                commonResponse.setInv_name(invName);
                commonResponse.setStatus(HttpStatus.BAD_REQUEST.value());
                commonResponse.setStatus_msg(HttpStatus.BAD_REQUEST.getReasonPhrase());
                commonResponse.setMsg("The PAN is not KYC Compliant.");
                commonResponse.setKyc_status(false);
            }

            return commonResponse;

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date());
            ex.printStackTrace();
            commonResponse.setInv_name("");
            commonResponse.setStatus(HttpStatus.BAD_REQUEST.value());
            commonResponse.setStatus_msg(HttpStatus.BAD_REQUEST.getReasonPhrase());
            commonResponse.setMsg(ex.getMessage());
            commonResponse.setKyc_status(false);
            return commonResponse;
        }
    }

    public static String getVendorImage(String vendor)
    {
        String path = "";

        if(vendor.equalsIgnoreCase("NSE"))
        {
            path = "nse.png";
        }else if(vendor.equalsIgnoreCase("BSE"))
        {
            path =  "bse.png";
        }else if(vendor.equalsIgnoreCase("MFU"))
        {
            path = "mfu.png";
        }else
        {
            path = "";
        }

        return path;
    }

    public static String getIpAddr(HttpServletRequest request)
    {
        //is client behind something?
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null)
        {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    public static String generateNseIinNumber(String arnCode)
    {
        return "M1" + arnCode.replace("ARN-", "") + new java.util.Random().ints(2, 0, 36)
                .mapToObj(i -> "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".charAt(i) + "")
                .reduce("", String::concat);
    }

    public static CommonPojo createCommonData(String code, String status)
    {
        return new CommonPojo(status, code);
    }

    public static List<CommonPojo> getEmailOrMobileRelationList() {
        List<CommonPojo> list = new ArrayList<>();
        list.add(createCommonData("SE", "Self"));
        list.add(createCommonData("SP", "Spouse"));
        list.add(createCommonData("DC", "Dependent Children"));
        list.add(createCommonData("DS", "Dependent Siblings"));
        list.add(createCommonData("DP", "Dependent Parents"));
        list.add(createCommonData("GD", "Guardian"));
        list.add(createCommonData("PM", "PMS"));
        list.add(createCommonData("CD", "Custodian"));
        list.add(createCommonData("PO", "POA"));
        return list;
    }


    private static String getIfscCodeString(JsonObject jsonObject, String key)
    {
        JsonElement element = jsonObject.get(key);
        return (element != null && !element.isJsonNull()) ? element.getAsString() : "";
    }

    public static IfscCodeResponse getBankDetailsByIfsc(String ifsc, String bank_name) throws UnirestException
    {
        HttpResponse<String> ifscResponse = Unirest.get("https://ifsc.razorpay.com/"+ifsc+"").asString();

        Integer ifscResponseStatus = ifscResponse.getStatus();

        if(ifscResponseStatus.equals(200))
        {
            String ifscResponseBody = ifscResponse.getBody();

            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(ifscResponseBody, JsonObject.class);

            String IFSC = getIfscCodeString(jsonObject, "IFSC");
            String BRANCH = getIfscCodeString(jsonObject, "BRANCH");
            String BANK = getIfscCodeString(jsonObject, "BANK");
            String CITY = getIfscCodeString(jsonObject, "CITY");
            String CENTRE = getIfscCodeString(jsonObject, "CENTRE");
            String DISTRICT = getIfscCodeString(jsonObject, "DISTRICT");
            String MICR = getIfscCodeString(jsonObject, "MICR");
            String ADDRESS = getIfscCodeString(jsonObject, "ADDRESS");

            IfscCodePojo ifscPojo = new IfscCodePojo();
            ifscPojo.setIfsc_code(IFSC);
            ifscPojo.setBranch(BRANCH);
            ifscPojo.setBank(BANK);
            ifscPojo.setCity(CITY);
            ifscPojo.setCentre(CENTRE);
            ifscPojo.setDistrict(DISTRICT);
            ifscPojo.setMicr_code(MICR);
            ifscPojo.setAddress(ADDRESS);

            IfscCodeResponse apiResponse = new IfscCodeResponse();
            apiResponse.setStatus(StatusMessage.SuccessCode);
            apiResponse.setStatus_msg(StatusMessage.SuccessMessage);
            apiResponse.setMsg(StatusMessage.SuccessMessage);
            apiResponse.setResult(ifscPojo);
            return apiResponse;

        }else
        {
            IfscCodeResponse apiResponse = new IfscCodeResponse();
            apiResponse.setStatus(StatusMessage.FailureCode);
            apiResponse.setStatus_msg(StatusMessage.FailureMessage);
            apiResponse.setMsg(StatusMessage.FailureMessage);
            apiResponse.setResult(null);
            return apiResponse;
        }
    }

    public static String getEasternfinanceEuin(String rm_name)
    {
        HashMap<String,String> euin_map = new HashMap<String,String>();
        euin_map.put("AMBRISH AGARWAL [700001]","E013599");
        euin_map.put("ABHISHEK AGARWAL [88888]","E013599");
        euin_map.put("ATUL GUPTA [700019]","E013599");
        euin_map.put("DHARMESH YAGNIK [118207]","E086034");
        euin_map.put("INDRAJIT SARKAR [11111]","E013601");
        euin_map.put("MADAN MURARI MUNDHRA [20000038]","E086336");
        euin_map.put("SAMIR JHA [20000015]","E088118");
        euin_map.put("SUDEEP PODDAR [700165]","E013599");
        euin_map.put("SUNIL AGARWAL [804001]","E013597");
        euin_map.put("SURAJIT MUKHERJEE [700081]","E088006");
        euin_map.put("SURESH DUGAR [20000055]","E080333");
        euin_map.put("TAPAS DAS [20000049]","E093969");
        euin_map.put("NEERAJ GUPTA [20000001]","E013598");
        euin_map.put("KRISHENDU MAHANTO [20000041]","E088103");
        euin_map.put("SUDESHNA CHAKRABORTY [20000037]","E088104");
        euin_map.put("SANJAY KUMAR BAJORIA [20000087]","E088104");
        euin_map.put("RAVI RANJAN [20202020]","E088104");
        euin_map.put("PRABHAT KUMAR [30303030]","E088104");
        euin_map.put("SHRAWAN KUMAR [20000027]","E176432");

        String euin = euin_map.get(rm_name);

        return euin;
    }
}
