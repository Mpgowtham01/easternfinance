package com.nse.controller;

import com.google.gson.Gson;
import com.nse.client.UserServiceClient;
import com.nse.config.TokenInterceptor;
import com.nse.dto.mf.*;
import com.nse.mapper.NseRegistrationMapper;
import com.nse.model.NsePincode;
import com.nse.model.NseTransactions;
import com.nse.repository.NseCountryRepository;
import com.nse.response.CommonResponse;
import com.nse.response.StatusMessage;
import com.nse.response.SuccessResponse;
import com.nse.services.LogExceptionService;
import com.nse.services.NseLogService;
import com.nse.services.NsePincodeService;
import com.nse.services.NseTransactionService;
import com.nse.utils.*;
import feign.FeignException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.hibernate.internal.util.StringHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;


@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "NSE Onboarding Controller",
        description = "APIs related to NSE onboarding steps and registration process."
)
public class NseOnboardingController {

    private static final Logger logger = LoggerFactory.getLogger(NseOnboardingController.class);

    final static String nseUrl = "https://www.nseinvest.com";

    @Value("${jwt.secret-key}")
    private String secretKey;

    private final UserServiceClient userServiceClient;

    public NseOnboardingController(UserServiceClient userServiceClient) {
        this.userServiceClient = userServiceClient;
    }

    @Autowired
    NsePincodeService nsePincodeService;

    @Autowired
    private NseRegistrationMapper nseRegistrationMapper;

    @Autowired
    private NseTransactionService nseTransactionService;

    @Autowired
    LogExceptionService logExceptionService;

    @Autowired
    NseLogService nseLogService;

    @Autowired
    NseCountryRepository countryRepository;

    @Operation(
            summary = "Register with NSE",
            description = "Registers a user with NSE by collecting and transforming all mandatory user details, including nominee information, FATCA compliance, occupation details, holding nature, and joint holder relationships. The service fetches user data based on the 'multiple_reg' flag, enriches it with state codes from nominee pincodes, assigns default relation codes, and ensures FATCA fields are correctly populated before forwarding the data to NSE."
    )

    @ApiResponses(value =
            {
                    @ApiResponse(responseCode = "200", description = "Success Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SuccessResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Failure Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommonResponse.class))),
            })
    @PostMapping("/registerWithNse")
    public ResponseEntity<?> registerWithNse(
            HttpServletRequest request,
            @RequestHeader("Authorization") String token,
            @RequestParam String multiple_reg,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String ip_address,
            @RequestParam(required = false) String origin_user_id,
            @RequestParam(required = false) String origin_first_name,
            @RequestParam(required = false) String onlineId
    )
    {
        SimpleDateFormat df1 = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat df0 = new SimpleDateFormat("dd-MM-yyyy");
        String userid = "";
        String client_name = "";
        try
        {
            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);

            if (!source.equalsIgnoreCase("mobile")
                    && (onlineId == null || onlineId.isEmpty() || !onlineId.matches("-?\\d+")))
            {
                return NseUtils.commonResponse("Online Id is Not Valid", HttpStatus.BAD_REQUEST);
            }

            Object userData;

            if(source.equalsIgnoreCase("Mobile"))
            {
                try {
                    userData = userServiceClient.getMobileAppUserDetailsByOnlineId(Integer.valueOf(userid), token);
                }catch (FeignException e)
                {
                    return FeignErrorHandler.handle(e, "User Service", "User not found");
                }
            }else
            {
                userData = userServiceClient.getUserByOnlineIdAndActive(Integer.valueOf(onlineId), Integer.valueOf(userid), token);
            }

            if(userData == null)
            {
                return NseUtils.commonResponse("User not found, Please try again", HttpStatus.BAD_REQUEST);
            }

            if (userData instanceof UserDto userDto)
            {
                // Step 1: Fetch nominee state codes using nominee pincode fields
                String nominee1_pincode = userDto.getNominee1_pincode();
                String nominee2_pincode = userDto.getNominee2_pincode();
                String nominee3_pincode = userDto.getNominee3_pincode();

                if (StringHelper.isNotEmpty(nominee1_pincode))
                {
                    Optional<NsePincode> pin = nsePincodeService.getPincodeDetails(nominee1_pincode);
                    if (pin.isPresent()) userDto.setNominee1_state(pin.get().getState_code());
                }

                if (StringHelper.isNotEmpty(nominee2_pincode))
                {
                    Optional<NsePincode> pin = nsePincodeService.getPincodeDetails(nominee2_pincode);
                    if (pin.isPresent()) userDto.setNominee2_state(pin.get().getState_code());
                }

                if (StringHelper.isNotEmpty(nominee3_pincode))
                {
                    Optional<NsePincode> pin = nsePincodeService.getPincodeDetails(nominee3_pincode);
                    if (pin.isPresent()) userDto.setNominee3_state(pin.get().getState_code());
                }

                // Step 2: Handle relation fields
                if (userDto.getJoint_holder_email_relation1() == null) userDto.setJoint_holder_email_relation1("");
                if (userDto.getJoint_holder_email_relation2() == null) userDto.setJoint_holder_email_relation2("");
                if (userDto.getJoint_holder_mobile_relation1() == null) userDto.setJoint_holder_mobile_relation1("");
                if (userDto.getJoint_holder_mobile_relation2() == null) userDto.setJoint_holder_mobile_relation2("");

                String mobile_relation = StringUtils.defaultString(userDto.getMobile_relation()).trim();
                String email_relation = StringUtils.defaultString(userDto.getEmail_relation()).trim();

                String client_taxstatus = userDto.getTax_status_code();

//                if (client_taxstatus != null && (
//                        client_taxstatus.equalsIgnoreCase("01") || client_taxstatus.equalsIgnoreCase("02") ||
//                                client_taxstatus.equalsIgnoreCase("11") || client_taxstatus.equalsIgnoreCase("21") ||
//                                client_taxstatus.equalsIgnoreCase("61") || client_taxstatus.equalsIgnoreCase("62") ||
//                                client_taxstatus.equalsIgnoreCase("26") || client_taxstatus.equalsIgnoreCase("28")))
//                {
//                    if (mobile_relation.isEmpty()) mobile_relation = "SE";
//                    if (email_relation.isEmpty()) email_relation = "SE";
//                } else
//                {
//                    mobile_relation = "";
//                    email_relation = "";
//                }
                if (mobile_relation.isEmpty()) mobile_relation = "SE";
                if (email_relation.isEmpty()) email_relation = "SE";

                String client_occupation_type;
                String client_occupation_code;
                String client_date_of_birth = "";
                client_occupation_type = userDto.getOccupation();
                client_occupation_code = userDto.getOccupation_code();

                String holding_nature = "";
                holding_nature = userDto.getHolding_nature_code().trim();

                userDto.setMobile_relation(mobile_relation);
                userDto.setEmail_relation(email_relation);

                // Step 3: Extra defaults
                String nomination_authentication = "O";
//                String nomination_opt = StringHelper.isNotEmpty(userDto.getNominee1_name()) ? "Y" : "N";

                String primary_holder_kyc_type = "K";
                String primary_holder_ckyc_number = "";

                String second_holder_kyc_type = "";
                String second_holder_ckyc_number = "";
                if (StringHelper.isNotEmpty(userDto.getJoint_holder_name1())) {
                    second_holder_kyc_type = "K";
                    second_holder_ckyc_number = "";
                }

                String third_holder_kyc_type = "";
                String third_holder_ckyc_number = "";
                if (StringHelper.isNotEmpty(userDto.getJoint_holder_name2())) {
                    third_holder_kyc_type = "K";
                    third_holder_ckyc_number = "";
                }

                String guardian_kyc_type = "";
                String guardian_ckyc_number = "";
                if (StringHelper.isNotEmpty(userDto.getGuard_name())) {
                    guardian_kyc_type = "K";
                    guardian_ckyc_number = "";
                }

                // Step 4: Fetch mail & host data
                client_name = userDto.getClient_name();
                String euin = "";

                BseNseKeyDto nsekey = null;

                try
                {
                    nsekey = userServiceClient.getByClientName(client_name,token);
                }catch (FeignException.NotFound ex)
                {
                    return NseUtils.commonResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
                }

                JSONObject regRequestBody = null;
                if (nsekey != null)
                {
                    //mail_support_name = nsekey.getMail_support_name();
                    //mail_support_email = nsekey.getMail_support_email();
                    //host = nsekey.getDomain_url();

                    String user_broker_code = userDto.getBroker_code();

                    if(StringHelper.isEmpty(user_broker_code))
                    {
                        user_broker_code = nsekey.getBrokerCode();
                    }

                    if (euin != null && !euin.isEmpty())
                    {
                        euin = euin.split(",")[0];
                    }

                    if(StringHelper.isNotEmpty(userDto.getNri_country()))
                    {
                        Optional<String> codeOpt = countryRepository.findCountryCodeByCountryName(userDto.getNri_country());

                        if(codeOpt.isPresent())
                        {
                            userDto.setNri_country(codeOpt.get());
                        }
                    }
                    System.out.println("userDto = " + userDto);
                    regRequestBody = nseRegistrationMapper.buildRegistrationJson(userDto, primary_holder_kyc_type, primary_holder_ckyc_number, second_holder_kyc_type, second_holder_ckyc_number, third_holder_kyc_type, third_holder_ckyc_number, guardian_kyc_type, guardian_ckyc_number, nomination_authentication);

                    BseNseOnlineAccessDto online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, user_broker_code,token);

                    if(online_access == null)
                    {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "NSE Online Credentials Not available. Please contact your RM"));
                    }

                    String nse_userid = NseUtils.trimOrEmpty(online_access.getNse_userid());
                    String nse_memberid = NseUtils.trimOrEmpty(online_access.getNse_memberid());
                    String nse_secret_key = NseUtils.trimOrEmpty(online_access.getNse_secret_key());
                    String nse_license_key = NseUtils.trimOrEmpty(online_access.getNse_license_key());


                    if(StringHelper.isEmpty(nse_userid) || StringHelper.isEmpty(nse_memberid) || StringHelper.isEmpty(nse_secret_key) || StringHelper.isEmpty(nse_license_key))
                    {
                        return NseUtils.commonResponse("NSE Online Credentials Not available. Please contact your RM", HttpStatus.BAD_REQUEST);
                    }
                    String base64Encoded = AESEncryptionUtilV2.base64EncodedAuth(nse_secret_key, nse_license_key, nse_userid);

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.set("memberId", nse_memberid);
                    headers.set("Authorization", "Basic "+base64Encoded);
                    headers.set("User-Agent", "PostmanRuntime/7.43.3");
                    headers.set("Accept-Encoding", "gzip, deflate, br");
                    headers.set("Accept-Language", "en-US");
                    headers.set("Connection", "keep-alive");
                    headers.set("Referer", "");
                    ///System.out.println("headers = " + headers);
                    RestTemplate restTemplate = RestTemplateFactory.createRestTemplate();
                    ///System.out.println("regRequestBody.toString() = " + regRequestBody.toString());
                    HttpEntity<String> entity = new HttpEntity<>(regRequestBody.toString(), headers);

                    String requestUrl = NseApiUrls.CREATECUSTOMER;
                    ///System.out.println("requestUrl = " + requestUrl);
                    try
                    {
                        ResponseEntity<String> response = null;
                        Integer statusCodeVal = 0;
                        String responseBody = "";
                        try
                        {
                            response = RestTemplateFactory.createRestTemplate().postForEntity(requestUrl, entity, String.class);
                            ///System.out.println("response = " + response);
                            statusCodeVal = response.getStatusCode().value();
                            responseBody = response.getBody();
                        } catch (HttpClientErrorException e)
                        {
                            statusCodeVal = e.getStatusCode().value();
                            responseBody = e.getResponseBodyAsString();
                        } catch (Exception ex)
                        {
                            responseBody = ex.getMessage();
                            ex.printStackTrace();
                        }

                        System.out.println("NSE API REQUEST URL = " + requestUrl);
                        System.out.println("NSE API REQUEST HEADER = " + new Gson().toJson(headers));
                        System.out.println("NSE API REQUEST DATA = " + regRequestBody.toString());
                        System.out.println("NSE API RESPONSE STATUS = " + statusCodeVal);
                        System.out.println("NSE API RESPONSE BODY = " + responseBody);

                        ///System.out.println("statusCodeVal = " + statusCodeVal);
                        if(statusCodeVal.equals(200))
                        {
                            JSONObject jsonObject = new JSONObject(responseBody);
                            JSONArray jsonRegArray = jsonObject.getJSONArray("reg_details");
                            System.out.println("jsonRegArray = " + jsonRegArray);
                            String reg_id = "";
                            String reg_status = "";
                            String reg_remark = "";

                            for (int i = 0; i < jsonRegArray.length(); i++)
                            {
                                JSONObject regDetail = jsonRegArray.getJSONObject(i);
                                reg_id = regDetail.optString("reg_id");
                                reg_status = regDetail.optString("reg_status");
                                reg_remark = regDetail.optString("reg_remark");
                            }

                            NseTransactions nsetrans = new NseTransactions();
                            nsetrans.setUrl(requestUrl);
                            nsetrans.setNse_request(regRequestBody.toString());
                            nsetrans.setNse_response(responseBody);
                            nsetrans.setReturn_msg(reg_status);
                            nsetrans.setService_return_code(String.valueOf(statusCodeVal));
                            nsetrans.setService_msg(reg_status);
                            nsetrans.setReg_id(reg_id);
                            nsetrans.setPayment_link("");
                            nsetrans.setPan(userDto.getPan());
                            nsetrans.setName(userDto.getName());
                            nsetrans.setBranch(userDto.getBranch());
                            nsetrans.setRm_name(userDto.getRm_name());
                            nsetrans.setSubbroker_name(userDto.getSubbroker_name());
                            nsetrans.setClient_name(client_name);
                            nsetrans.setIin_number(userDto.getNse_iin_number());
                            nsetrans.setScheme_name("");
                            nsetrans.setScheme_code("");
                            nsetrans.setFolio_no("");
                            nsetrans.setAmount_units("");
                            nsetrans.setFrequency("");
                            nsetrans.setPeriod_day("");
                            nsetrans.setUmrn_no("");
                            nsetrans.setPurchase_type("");
                            nsetrans.setPayment_ref_no("");
                            nsetrans.setUnique_number("");
                            nsetrans.setAuto_trxn_no("");
                            nsetrans.setSip_reg_no("");
                            nsetrans.setPayment_mode("");
                            nsetrans.setTopup_amount(0.0);
                            nsetrans.setBank_acc_no("");
                            nsetrans.setTransaction_number("");
                            nsetrans.setApplication_number("");
                            nsetrans.setTo_scheme_code("");
                            nsetrans.setTo_scheme_name("");
                            nsetrans.setTransaction_type("UCC Request");
                            nsetrans.setTransaction_status("");
                            nsetrans.setPayment_status("");
                            nsetrans.setActive_ceased_status("");
                            nsetrans.setRemarks(reg_remark);
                            nsetrans.setMandate_id("");
                            nsetrans.setMandate_status("");
                            nsetrans.setEmandate_auth_flag("");
                            nsetrans.setApp_received_flag("");
                            nsetrans.setTransaction_date(new Date());
                            nsetrans.setUser_id(userDto.getUser_id());

                            if(source.equalsIgnoreCase("Mobile"))
                            {
                                nsetrans.setRegister_source("Mobile App");
                            }else {
                                nsetrans.setRegister_source("Website");
                            }

                            nsetrans.setBroker_code(user_broker_code);
                            nsetrans.setEuin_number(euin);
                            nsetrans.setCc_received("");
                            nsetrans.setFund_trans_to_amc("");
                            nsetrans.setRefund_status("");
                            nsetrans.setRefund_amount("");
                            nseTransactionService.save(nsetrans);

                            if ("REG_FAILED".equalsIgnoreCase(reg_status))
                            {
                                return NseUtils.commonResponse(reg_remark, HttpStatus.BAD_REQUEST);
                            }

                            String fatca_reg_id = "";
                            String fatca_reg_status = "";
                            String fatca_reg_remark = "";

                            if(reg_status.equalsIgnoreCase("REG_SUCCESS"))
                            {
                                String data_src = "E";
                                String nffe_catg = "";
                                String id1_type = "C";
                                String ubo_add1 = "";
                                String ubo_add2 = "";
                                String ubo_add3 = "";
                                String ubo_city = "";
                                String ubo_pin = "";
                                String ubo_state = "";
                                String ubo_cntry = "";
                                String ubo_appl = "N";
                                String ubo_count = "";
                                String ubo_nation = "";
                                String ubo_df = "N";
                                String ubo_add_ty = "";
                                String ubo_ctr = "";
                                String ubo_tin = "";
                                String ubo_id_ty = "";
                                String ubo_cob = "";
                                String ubo_dob = "";
                                String ubo_gender = "";
                                String ubo_fr_nam = "";
                                String ubo_name = "";
                                String ubo_pan = "";
                                String ubo_occ = "";
                                String ubo_occ_ty = "";
                                String ubo_tel = "";
                                String ubo_mobile = "";
                                String ubo_code = "";
                                String ubo_hol_pc = "";
                                String tax_res1 = "IN";
                                String corp_servs = "04";
                                String ffi_drnfe = "NA";
                                String giin_no = "NA";
                                String giin_na = "NO";
                                String ubo_email = "";
                                String ubo_categ = "";

                                if(client_occupation_code.equalsIgnoreCase("01"))
                                {
                                    client_occupation_type = "B";
                                }
                                else if(client_occupation_code.equalsIgnoreCase("02") || client_occupation_code.equalsIgnoreCase("03") || client_occupation_code.equalsIgnoreCase("04"))
                                {
                                    client_occupation_type = "S";
                                }
                                else
                                {
                                    client_occupation_type = "O";
                                }

                                String pan = userDto.getPan();
                                String panType = "";

                                if (pan != null && pan.length() >= 4) {
                                    panType = pan.substring(3, 4).toUpperCase(Locale.ROOT);
                                }

                                if(!panType.isEmpty() && !panType.equalsIgnoreCase("P"))
                                {
                                    data_src = "P";
                                    nffe_catg = "NA";
                                    ubo_appl = "Y";
                                    ubo_count = "1";
                                    ubo_nation = "IN";
                                    ubo_name = userDto.getName();
                                    ubo_pan = userDto.getPan();

                                    ubo_add1 = userDto.getStreet_1();
                                    ubo_add2 = userDto.getStreet_2();

                                    if(ubo_add2 == null || ubo_add2.isEmpty()){
                                        ubo_add2 = "Street 2";
                                    }
                                    ubo_add3 = userDto.getStreet_3();
                                    if(ubo_add3 == null || ubo_add3.isEmpty()){
                                        ubo_add3 = "Street 3";
                                    }

                                    ubo_city = userDto.getCity();
                                    ubo_state = userDto.getState_code();
                                    ubo_pin = userDto.getPincode();
                                    ubo_cntry = "IN";
                                    ubo_add_ty = userDto.getAddress_type_code();
                                    ubo_ctr = "IN";
                                    ubo_tin = ubo_pan;
                                    ubo_id_ty = "C";
                                    ubo_cob = "IN";
                                    ubo_dob = userDto.getDate_of_birth();
                                    ubo_gender = userDto.getGender();
                                    if(ubo_gender.isEmpty())
                                    {
                                        ubo_gender = "O";
                                    }
                                    ubo_fr_nam = userDto.getFather_name();
                                    if(ubo_fr_nam.isEmpty()){
                                        ubo_fr_nam = "Not Provided";
                                    }
                                    ubo_occ = userDto.getOccupation_code();
                                    ubo_occ_ty = client_occupation_type;
                                    ubo_mobile = userDto.getMobile();
                                    ubo_email = userDto.getEmail();
                                    ubo_hol_pc = "100";
                                    ubo_code = "C14";
                                    ubo_df = "Y";
                                    ubo_categ = "UBO";
                                }

                                if(userDto.getDate_of_birth() != null && !userDto.getDate_of_birth().isEmpty())
                                {
                                    Date dob = df0.parse(userDto.getDate_of_birth());
                                    client_date_of_birth = df1.format(dob);
                                    ubo_dob = df1.format(dob);
                                }

                                JSONArray fatcaReqArray = new JSONArray();
                                JSONObject fatcaReqObject = new JSONObject();

                                Set<String> minorTaxCodeList = Set.of("02", "26", "28");
                                if (minorTaxCodeList.contains(userDto.getTax_status_code())) {
                                    fatcaReqObject.put("pan_rp", userDto.getGuard_pan());
                                }else{
                                    fatcaReqObject.put("pan_rp", userDto.getPan());
                                }
                                fatcaReqObject.put("pekrn", "");
                                fatcaReqObject.put("inv_name", userDto.getName());
                                fatcaReqObject.put("dob", client_date_of_birth);
                                fatcaReqObject.put("fr_name", "");
                                fatcaReqObject.put("sp_name", "");
                                fatcaReqObject.put("tax_status", client_taxstatus);
                                fatcaReqObject.put("data_src", data_src);
                                fatcaReqObject.put("addr_type", userDto.getAddress_type_code());
                                fatcaReqObject.put("po_bir_inc", userDto.getPlace_of_birth());
                                fatcaReqObject.put("co_bir_inc", "IN");
                                fatcaReqObject.put("tax_res1", tax_res1);

                                if (minorTaxCodeList.contains(userDto.getTax_status_code())) {

                                    fatcaReqObject.put("tpin1", userDto.getGuard_pan());
                                }else{
                                    fatcaReqObject.put("tpin1", userDto.getPan());
                                }
                                fatcaReqObject.put("id1_type", id1_type);
                                fatcaReqObject.put("tax_res2", "");
                                fatcaReqObject.put("tpin2", "");
                                fatcaReqObject.put("id2_type", "");
                                fatcaReqObject.put("tax_res3", "");
                                fatcaReqObject.put("tpin3", "");
                                fatcaReqObject.put("id3_type", "");
                                fatcaReqObject.put("tax_res4", "");
                                fatcaReqObject.put("tpin4", "");
                                fatcaReqObject.put("id4_type", "");
                                fatcaReqObject.put("srce_wealt", userDto.getSource_of_wealth_code());
                                fatcaReqObject.put("corp_servs", corp_servs);
                                fatcaReqObject.put("inc_slab", userDto.getAnnual_income_code());
                                fatcaReqObject.put("net_worth", userDto.getNetworth_amount());

                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                                SimpleDateFormat sdf2 = new SimpleDateFormat("dd-MM-yyyy");
                                String formattedDate ="";
                                if(userDto.getNetworth_dob()!= null && !userDto.getNetworth_dob().isEmpty()){
                                    formattedDate = sdf.format(sdf2.parse(userDto.getNetworth_dob()));
                                }
                                fatcaReqObject.put("nw_date", formattedDate);
                                fatcaReqObject.put("pep_flag", userDto.getPolitical_code());
                                fatcaReqObject.put("occ_code", userDto.getOccupation_code().trim());
                                fatcaReqObject.put("occ_type", NseUtils.getOccupationTypeByCode(userDto.getOccupation_code().trim())); //OCC_TYPE M Character 1  S - Service; B - Business, O - Others;X - Not Categorized

                                if (client_taxstatus.equalsIgnoreCase("01") || client_taxstatus.equalsIgnoreCase("02") ||
                                        client_taxstatus.equalsIgnoreCase("24") || client_taxstatus.equalsIgnoreCase("21") ||
                                        client_taxstatus.equalsIgnoreCase("61") || client_taxstatus.equalsIgnoreCase("62") ||
                                        client_taxstatus.equalsIgnoreCase("26") || client_taxstatus.equalsIgnoreCase("28"))
                                {
                                    fatcaReqObject.put("exemp_code", "");
                                    fatcaReqObject.put("sdf_flag", "");

                                }else{
                                    fatcaReqObject.put("exemp_code", "N");
                                    fatcaReqObject.put("sdf_flag", "Y");
                                }
                                fatcaReqObject.put("ffi_drnfe", ffi_drnfe);
                                fatcaReqObject.put("giin_no", giin_no);
                                fatcaReqObject.put("spr_entity", "");
                                fatcaReqObject.put("giin_na", giin_na);
                                fatcaReqObject.put("giin_exemc", "");
                                fatcaReqObject.put("nffe_catg", nffe_catg);
                                fatcaReqObject.put("act_nfe_sc", "");
                                fatcaReqObject.put("nature_bus", "");
                                fatcaReqObject.put("rel_listed", "");
                                fatcaReqObject.put("exch_name", "O");
                                fatcaReqObject.put("ubo_appl", ubo_appl);
                                fatcaReqObject.put("ubo_count", ubo_count);
                                //fatcaReqObject.put("sdf_flag", "");
                                fatcaReqObject.put("ubo_df", ubo_df);
                                fatcaReqObject.put("aadhaar_rp", "");
                                fatcaReqObject.put("new_change", "");

                                String logName = userDto.getName();
                                if (logName != null && logName.length() > 30) {
                                    logName = logName.substring(0, 25);
                                }
                                fatcaReqObject.put("log_name", logName);

                                fatcaReqObject.put("ubo_exch", "");
                                fatcaReqObject.put("ubo_isin", "");
                                fatcaReqObject.put("ubo_rel_li", "");
                                fatcaReqObject.put("npo_form", "N");
                                fatcaReqObject.put("npo_dcl", "N");
                                fatcaReqObject.put("npo_rgno", "");

                                JSONArray uboReqArray = new JSONArray();
                                JSONObject uboReqObject = new JSONObject();

                                uboReqObject.put("ubo_name", ubo_name);
                                uboReqObject.put("ubo_pan", ubo_pan);
                                uboReqObject.put("ubo_nation", ubo_nation);
                                uboReqObject.put("ubo_add1", ubo_add1);
                                uboReqObject.put("ubo_add2", ubo_add2);
                                uboReqObject.put("ubo_add3", ubo_add3);
                                uboReqObject.put("ubo_city", ubo_city);
                                uboReqObject.put("ubo_pin", ubo_pin);
                                uboReqObject.put("ubo_state", NseUtils.getFatcaCode(ubo_state));
                                uboReqObject.put("ubo_cntry", ubo_cntry);
                                uboReqObject.put("ubo_add_ty", ubo_add_ty);
                                uboReqObject.put("ubo_ctr", ubo_ctr);
                                uboReqObject.put("ubo_tin", ubo_tin);
                                uboReqObject.put("ubo_id_ty", ubo_id_ty);
                                uboReqObject.put("ubo_cob", ubo_cob);
                                uboReqObject.put("ubo_dob", ubo_dob);
                                uboReqObject.put("ubo_gender", ubo_gender);
                                uboReqObject.put("ubo_fr_nam", ubo_fr_nam);
                                uboReqObject.put("ubo_occ", ubo_occ);
                                uboReqObject.put("ubo_occ_ty", ubo_occ_ty);
                                uboReqObject.put("ubo_tel", ubo_tel);
                                uboReqObject.put("ubo_mobile", ubo_mobile);
                                uboReqObject.put("ubo_code", ubo_code);
                                uboReqObject.put("ubo_hol_pc", ubo_hol_pc);
                                uboReqObject.put("ubo_categ", ubo_categ);
                                uboReqObject.put("ubo_pep_fl", userDto.getPolitical_code());
                                uboReqObject.put("ubo_email", ubo_email);
                                uboReqObject.put("ubo_smo_de", "");
                                uboReqArray.put(uboReqObject);

                                fatcaReqObject.put("ubo_details", uboReqArray);
                                fatcaReqArray.put(fatcaReqObject);

                                JSONObject fatcaReqBody = new JSONObject();
                                fatcaReqBody.put("reg_details", fatcaReqArray);

                                //System.out.println("fatcaReqArray = " + fatcaReqArray);
                                //System.out.println("uboReqArray = " + uboReqArray);
                                //System.out.println("fatcaReqBody = " + fatcaReqBody);

                                HttpEntity<String> fatcaEntity = new HttpEntity<>(fatcaReqBody.toString(), headers);
                                String fatcaRequestUrl = nseUrl+"/nsemfdesk/api/v2/registration/FATCA_COMMON";

                                ResponseEntity<String> fatcaResult = restTemplate.postForEntity(fatcaRequestUrl, fatcaEntity, String.class);
                                String fatcaStatusCode = fatcaResult.getStatusCode().toString();
                                String fatcaResponse = fatcaResult.getBody();
                                System.out.println("primary holder fatcaStatusCode = " + fatcaStatusCode);
                                System.out.println("primary holder fatcaResponse = " + fatcaResponse);

                                JSONObject fatcaObj = new JSONObject(fatcaResponse);
                                JSONArray fatcaArr = fatcaObj.getJSONArray("reg_details");

                                for (int i = 0; i < fatcaArr.length(); i++)
                                {
                                    JSONObject regDetail = fatcaArr.getJSONObject(i);
                                    fatca_reg_id = regDetail.optString("reg_id");
                                    fatca_reg_status = regDetail.optString("reg_status");
                                    fatca_reg_remark = regDetail.optString("reg_remark");
                                }

                                NseTransactions nsetrans1 = new NseTransactions();
                                nsetrans1.setUrl(fatcaRequestUrl);
                                nsetrans1.setNse_request(fatcaReqBody.toString());
                                nsetrans1.setNse_response(fatcaResponse);
                                nsetrans1.setReturn_msg(fatca_reg_status);
                                nsetrans1.setService_return_code(fatcaStatusCode);
                                nsetrans1.setService_msg(fatca_reg_status);
                                nsetrans1.setReg_id(fatca_reg_id);
                                nsetrans1.setPayment_link("");
                                nsetrans1.setPan(userDto.getPan());
                                nsetrans1.setName(userDto.getName());
                                nsetrans1.setBranch(userDto.getBranch());
                                nsetrans1.setRm_name(userDto.getRm_name());
                                nsetrans1.setSubbroker_name(userDto.getSubbroker_name());
                                nsetrans1.setClient_name(client_name);
                                nsetrans1.setIin_number(userDto.getNse_iin_number());
                                nsetrans1.setScheme_name("");
                                nsetrans1.setScheme_code("");
                                nsetrans1.setFolio_no("");
                                nsetrans1.setAmount_units("");
                                nsetrans1.setFrequency("");
                                nsetrans1.setPeriod_day("");
                                nsetrans1.setUmrn_no("");
                                nsetrans1.setPurchase_type("");
                                nsetrans1.setPayment_ref_no("");
                                nsetrans1.setUnique_number("");
                                nsetrans1.setAuto_trxn_no("");
                                nsetrans1.setSip_reg_no("");
                                nsetrans1.setPayment_mode("");
                                nsetrans1.setTopup_amount(0.0);
                                nsetrans1.setBank_acc_no("");
                                nsetrans1.setTransaction_number("");
                                nsetrans1.setApplication_number("");
                                nsetrans1.setTo_scheme_code("");
                                nsetrans1.setTo_scheme_name("");
                                nsetrans1.setTransaction_type("Fatca Request");
                                nsetrans1.setTransaction_status("");
                                nsetrans1.setPayment_status("");
                                nsetrans1.setActive_ceased_status("");
                                nsetrans1.setRemarks(fatca_reg_remark);
                                nsetrans1.setMandate_id("");
                                nsetrans1.setMandate_status("");
                                nsetrans1.setEmandate_auth_flag("");
                                nsetrans1.setApp_received_flag("");
                                nsetrans1.setTransaction_date(new Date());
                                nsetrans1.setUser_id(userDto.getUser_id());
                                if(source.equalsIgnoreCase("Mobile")){
                                    nsetrans1.setRegister_source("Mobile App");
                                }else {
                                    nsetrans1.setRegister_source("Website");
                                }
                                nsetrans1.setBroker_code(user_broker_code);
                                nsetrans1.setEuin_number(euin);
                                nsetrans1.setCc_received("");
                                nsetrans1.setFund_trans_to_amc("");
                                nsetrans1.setRefund_status("");
                                nsetrans1.setRefund_amount("");
                                nseTransactionService.save(nsetrans1);

                                //Joint Holder FATCA start here
                                if(holding_nature.equalsIgnoreCase("JO") || holding_nature.equalsIgnoreCase("AS") || holding_nature.equalsIgnoreCase("ES"))
                                {
                                    String joint_name = "";
                                    String joint_pan = "";
                                    String joint1_dob = "";
                                    String joint_place_birth = "";
                                    //String joint_country_birth_code = "";
                                    String joint_occupation_code = "";
                                    String joint_annual_income_code = "";
                                    String joint_source_wealth_code = "";
                                    String joint_political_code = "";
                                    String joint_address_type_code = "";

                                    joint_name = userDto.getJoint_holder_name1();
                                    joint_pan = userDto.getJoint_holder_pan1();
                                    //joint_email = userDto.getJoint_holder_email1();
                                    joint1_dob = userDto.getJoint_holder_dob1();
                                    joint_place_birth = userDto.getJoint_holder_place_of_birth1();
                                    //joint_country_birth_code = userDto.getJoint_holder_country_birth_code1();
                                    joint_occupation_code = userDto.getJoint_holder_occupation_code1();
                                    joint_annual_income_code = userDto.getJoint_holder_annual_income_code1();
                                    joint_source_wealth_code = userDto.getJoint_holder_source_of_wealth_code1();
                                    joint_political_code = userDto.getJoint_holder_political_code1();
                                    joint_address_type_code = userDto.getJoint_holder_address_type_code1();

                                    if(userDto.getJoint_holder_dob1() != null && !userDto.getJoint_holder_dob1().isEmpty())
                                    {
                                        Date dob = df0.parse(userDto.getJoint_holder_dob1());
                                        joint1_dob = df1.format(dob);
                                    }

                                    String joint_occupation_type= "S";
                                    if(joint_occupation_code.equalsIgnoreCase("01"))
                                    {
                                        joint_occupation_type = "B";
                                    }
                                    else if (joint_occupation_code.equalsIgnoreCase("02") || joint_occupation_code.equalsIgnoreCase("03") || joint_occupation_code.equalsIgnoreCase("04"))
                                    {
                                        joint_occupation_type = "S";
                                    }
                                    else
                                    {
                                        joint_occupation_type = "O";
                                    }

                                    fatcaReqArray = new JSONArray();
                                    fatcaReqObject = new JSONObject();

                                    fatcaReqObject.put("pan_rp", userDto.getJoint_holder_pan1());
                                    fatcaReqObject.put("pekrn", "");
                                    fatcaReqObject.put("inv_name", userDto.getJoint_holder_name1());
                                    fatcaReqObject.put("dob", joint1_dob);
                                    fatcaReqObject.put("fr_name", "");
                                    fatcaReqObject.put("sp_name", "");
                                    fatcaReqObject.put("tax_status", client_taxstatus);
                                    fatcaReqObject.put("data_src", data_src);
                                    fatcaReqObject.put("addr_type", joint_address_type_code);
                                    fatcaReqObject.put("po_bir_inc", joint_place_birth);
                                    fatcaReqObject.put("co_bir_inc", "IN");
                                    fatcaReqObject.put("tax_res1", tax_res1);
                                    fatcaReqObject.put("tpin1", joint_pan);
                                    fatcaReqObject.put("id1_type", id1_type);
                                    fatcaReqObject.put("tax_res2", "");
                                    fatcaReqObject.put("tpin2", "");
                                    fatcaReqObject.put("id2_type", "");
                                    fatcaReqObject.put("tax_res3", "");
                                    fatcaReqObject.put("tpin3", "");
                                    fatcaReqObject.put("id3_type", "");
                                    fatcaReqObject.put("tax_res4", "");
                                    fatcaReqObject.put("tpin4", "");
                                    fatcaReqObject.put("id4_type", "");
                                    fatcaReqObject.put("srce_wealt", joint_source_wealth_code);
                                    fatcaReqObject.put("corp_servs", "");
                                    fatcaReqObject.put("inc_slab", joint_annual_income_code);
                                    fatcaReqObject.put("net_worth", "");
                                    fatcaReqObject.put("nw_date", "");
                                    fatcaReqObject.put("pep_flag", joint_political_code);
                                    fatcaReqObject.put("occ_code", joint_occupation_code);
                                    fatcaReqObject.put("occ_type", joint_occupation_type);
                                    fatcaReqObject.put("exemp_code", "");
                                    fatcaReqObject.put("ffi_drnfe", "");
                                    fatcaReqObject.put("giin_no", "");
                                    fatcaReqObject.put("spr_entity", "");
                                    fatcaReqObject.put("giin_na", "");
                                    fatcaReqObject.put("giin_exemc", "");
                                    fatcaReqObject.put("nffe_catg", "");
                                    fatcaReqObject.put("act_nfe_sc", "");
                                    fatcaReqObject.put("nature_bus", "");
                                    fatcaReqObject.put("rel_listed", "");
                                    fatcaReqObject.put("exch_name", "O");
                                    fatcaReqObject.put("ubo_appl", ubo_appl);
                                    fatcaReqObject.put("ubo_count", "");
                                    fatcaReqObject.put("sdf_flag", "");
                                    fatcaReqObject.put("ubo_df", ubo_df);
                                    fatcaReqObject.put("aadhaar_rp", "");
                                    fatcaReqObject.put("new_change", "");


                                    if (joint_name != null && joint_name.length() > 30) {
                                        joint_name = joint_name.substring(0, 25);
                                    }

                                    fatcaReqObject.put("log_name", joint_name);
                                    fatcaReqObject.put("ubo_exch", "");
                                    fatcaReqObject.put("ubo_isin", "");
                                    fatcaReqObject.put("ubo_rel_li", "");
                                    fatcaReqObject.put("npo_form", "N");
                                    fatcaReqObject.put("npo_dcl", "N");
                                    fatcaReqObject.put("npo_rgno", "");

                                    uboReqArray = new JSONArray();
                                    uboReqObject = new JSONObject();
                                    uboReqObject.put("ubo_name", "");
                                    uboReqObject.put("ubo_pan", "");
                                    uboReqObject.put("ubo_nation", "");
                                    uboReqObject.put("ubo_add1", "");
                                    uboReqObject.put("ubo_add2", "");
                                    uboReqObject.put("ubo_add3", "");
                                    uboReqObject.put("ubo_city", "");
                                    uboReqObject.put("ubo_pin", "");
                                    uboReqObject.put("ubo_state", "");
                                    uboReqObject.put("ubo_cntry", "");
                                    uboReqObject.put("ubo_add_ty", "");
                                    uboReqObject.put("ubo_ctr", "");
                                    uboReqObject.put("ubo_tin", "");
                                    uboReqObject.put("ubo_id_ty", "");
                                    uboReqObject.put("ubo_cob", "");
                                    uboReqObject.put("ubo_dob", "");
                                    uboReqObject.put("ubo_gender", "");
                                    uboReqObject.put("ubo_fr_nam", "");
                                    uboReqObject.put("ubo_occ", "");
                                    uboReqObject.put("ubo_occ_ty", "");
                                    uboReqObject.put("ubo_tel", "");
                                    uboReqObject.put("ubo_mobile", "");
                                    uboReqObject.put("ubo_code", "");
                                    uboReqObject.put("ubo_hol_pc", "");
                                    uboReqObject.put("ubo_categ", "");
                                    uboReqObject.put("ubo_pep_fl", "");
                                    uboReqObject.put("ubo_email", "");
                                    uboReqObject.put("ubo_smo_de", "");
                                    uboReqArray.put(uboReqObject);

                                    fatcaReqObject.put("ubo_details", uboReqArray);
                                    fatcaReqArray.put(fatcaReqObject);

                                    fatcaReqBody = new JSONObject();
                                    fatcaReqBody.put("reg_details", fatcaReqArray);
                                    System.out.println("Joint Holder 1 fatcaReqBody = " + fatcaReqBody);

                                    fatcaEntity = new HttpEntity<>(fatcaReqBody.toString(), headers);
                                    fatcaRequestUrl = nseUrl+"/nsemfdesk/api/v2/registration/FATCA_COMMON";

                                    fatcaResult = restTemplate.postForEntity(fatcaRequestUrl, fatcaEntity, String.class);
                                    fatcaStatusCode = fatcaResult.getStatusCode().toString();
                                    fatcaResponse = fatcaResult.getBody();
                                    System.out.println("joint holder 1 fatcaStatusCode = " + fatcaStatusCode);
                                    System.out.println("joint holder 1 fatcaResponse = " + fatcaResponse);

                                    fatcaObj = new JSONObject(fatcaResponse);
                                    fatcaArr = fatcaObj.getJSONArray("reg_details");

                                    fatca_reg_id = "";
                                    fatca_reg_status = "";
                                    fatca_reg_remark = "";

                                    for (int i = 0; i < fatcaArr.length(); i++)
                                    {
                                        JSONObject regDetail = fatcaArr.getJSONObject(i);
                                        fatca_reg_id = regDetail.optString("reg_id");
                                        fatca_reg_status = regDetail.optString("reg_status");
                                        fatca_reg_remark = regDetail.optString("reg_remark");
                                    }

                                    NseTransactions nsetrans2 = new NseTransactions();
                                    nsetrans2.setUrl(fatcaRequestUrl);
                                    nsetrans2.setNse_request(fatcaReqBody.toString());
                                    nsetrans2.setNse_response(fatcaResponse);
                                    nsetrans2.setReturn_msg(fatca_reg_status);
                                    nsetrans2.setService_return_code(fatcaStatusCode);
                                    nsetrans2.setService_msg(fatca_reg_status);
                                    nsetrans2.setReg_id(fatca_reg_id);
                                    nsetrans2.setPayment_link("");
                                    nsetrans2.setPan(userDto.getJoint_holder_pan1());
                                    nsetrans2.setName(userDto.getJoint_holder_name1());
                                    nsetrans2.setBranch(userDto.getBranch());
                                    nsetrans2.setRm_name(userDto.getRm_name());
                                    nsetrans2.setSubbroker_name(userDto.getSubbroker_name());
                                    nsetrans2.setClient_name(client_name);
                                    nsetrans2.setIin_number(userDto.getNse_iin_number());
                                    nsetrans2.setScheme_name("");
                                    nsetrans2.setScheme_code("");
                                    nsetrans2.setFolio_no("");
                                    nsetrans2.setAmount_units("");
                                    nsetrans2.setFrequency("");
                                    nsetrans2.setPeriod_day("");
                                    nsetrans2.setUmrn_no("");
                                    nsetrans2.setPurchase_type("");
                                    nsetrans2.setPayment_ref_no("");
                                    nsetrans2.setUnique_number("");
                                    nsetrans2.setAuto_trxn_no("");
                                    nsetrans2.setSip_reg_no("");
                                    nsetrans2.setPayment_mode("");
                                    nsetrans2.setTopup_amount(0.0);
                                    nsetrans2.setBank_acc_no("");
                                    nsetrans2.setTransaction_number("");
                                    nsetrans2.setApplication_number("");
                                    nsetrans2.setTo_scheme_code("");
                                    nsetrans2.setTo_scheme_name("");
                                    nsetrans2.setTransaction_type("Joint Holder1 Fatca Request");
                                    nsetrans2.setTransaction_status("");
                                    nsetrans2.setPayment_status("");
                                    nsetrans2.setActive_ceased_status("");
                                    nsetrans2.setRemarks(fatca_reg_remark);
                                    nsetrans2.setMandate_id("");
                                    nsetrans2.setMandate_status("");
                                    nsetrans2.setEmandate_auth_flag("");
                                    nsetrans2.setApp_received_flag("");
                                    nsetrans2.setTransaction_date(new Date());
                                    nsetrans2.setUser_id(userDto.getUser_id());
                                    if(source.equalsIgnoreCase("Mobile")){
                                        nsetrans1.setRegister_source("Mobile App");
                                    }else {
                                        nsetrans1.setRegister_source("Website");
                                    }
                                    nsetrans2.setBroker_code(user_broker_code);
                                    nsetrans2.setEuin_number(euin);
                                    nsetrans2.setCc_received("");
                                    nsetrans2.setFund_trans_to_amc("");
                                    nsetrans2.setRefund_status("");
                                    nsetrans2.setRefund_amount("");
                                    nseTransactionService.save(nsetrans2);

                                    if(StringHelper.isNotEmpty(userDto.getJoint_holder_name2()))
                                    {
                                        joint_name = "";
                                        joint_pan = "";
                                        //joint_email = "";
                                        joint1_dob = "";
                                        joint_place_birth = "";
                                        //joint_country_birth_code = "";
                                        joint_occupation_code = "";
                                        joint_annual_income_code = "";
                                        joint_source_wealth_code = "";
                                        joint_political_code = "";
                                        joint_address_type_code = "";

                                        joint_name = userDto.getJoint_holder_name2();
                                        joint_pan = userDto.getJoint_holder_pan2();
                                        //joint_email = userDto.getJoint_holder_email2();
                                        joint1_dob = userDto.getJoint_holder_dob2();
                                        joint_place_birth = userDto.getJoint_holder_place_of_birth2();
                                        //joint_country_birth_code = userDto.getJoint_holder_country_birth_code2();
                                        joint_occupation_code = userDto.getJoint_holder_occupation_code2();
                                        joint_annual_income_code = userDto.getJoint_holder_annual_income_code2();
                                        joint_source_wealth_code = userDto.getJoint_holder_source_of_wealth_code2();
                                        joint_political_code = userDto.getJoint_holder_political_code2();
                                        joint_address_type_code = userDto.getJoint_holder_address_type_code2();

                                        if(joint1_dob != null && !joint1_dob.isEmpty())
                                        {
                                            Date dob = df0.parse(joint1_dob);
                                            joint1_dob = df1.format(dob);
                                        }

                                        joint_occupation_type = "S";
                                        if(joint_occupation_code.equalsIgnoreCase("01"))
                                        {
                                            joint_occupation_type = "B";
                                        }
                                        else if (joint_occupation_code.equalsIgnoreCase("02") || joint_occupation_code.equalsIgnoreCase("03") || joint_occupation_code.equalsIgnoreCase("04"))
                                        {
                                            joint_occupation_type = "S";
                                        }
                                        else
                                        {
                                            joint_occupation_type = "O";
                                        }

                                        fatcaReqArray = new JSONArray();
                                        fatcaReqObject = new JSONObject();
                                        fatcaReqObject.put("pan_rp", joint_pan);
                                        fatcaReqObject.put("pekrn", "");
                                        fatcaReqObject.put("inv_name", joint_name);
                                        fatcaReqObject.put("dob", joint1_dob);
                                        fatcaReqObject.put("fr_name", "");
                                        fatcaReqObject.put("sp_name", "");
                                        fatcaReqObject.put("tax_status", client_taxstatus);
                                        fatcaReqObject.put("data_src", data_src);
                                        fatcaReqObject.put("addr_type", joint_address_type_code);
                                        fatcaReqObject.put("po_bir_inc", joint_place_birth);
                                        fatcaReqObject.put("co_bir_inc", "IN");
                                        fatcaReqObject.put("tax_res1", tax_res1);
                                        fatcaReqObject.put("tpin1", joint_pan);
                                        fatcaReqObject.put("id1_type", id1_type);
                                        fatcaReqObject.put("tax_res2", "");
                                        fatcaReqObject.put("tpin2", "");
                                        fatcaReqObject.put("id2_type", "");
                                        fatcaReqObject.put("tax_res3", "");
                                        fatcaReqObject.put("tpin3", "");
                                        fatcaReqObject.put("id3_type", "");
                                        fatcaReqObject.put("tax_res4", "");
                                        fatcaReqObject.put("tpin4", "");
                                        fatcaReqObject.put("id4_type", "");
                                        fatcaReqObject.put("srce_wealt", joint_source_wealth_code);
                                        fatcaReqObject.put("corp_servs", "");
                                        fatcaReqObject.put("inc_slab", joint_annual_income_code);
                                        fatcaReqObject.put("net_worth", "");
                                        fatcaReqObject.put("nw_date", "");
                                        fatcaReqObject.put("pep_flag", joint_political_code);
                                        fatcaReqObject.put("occ_code", joint_occupation_code);
                                        fatcaReqObject.put("occ_type", joint_occupation_type);
                                        fatcaReqObject.put("exemp_code", "");
                                        fatcaReqObject.put("ffi_drnfe", "");
                                        fatcaReqObject.put("giin_no", "");
                                        fatcaReqObject.put("spr_entity", "");
                                        fatcaReqObject.put("giin_na", "");
                                        fatcaReqObject.put("giin_exemc", "");
                                        fatcaReqObject.put("nffe_catg", "");
                                        fatcaReqObject.put("act_nfe_sc", "");
                                        fatcaReqObject.put("nature_bus", "");
                                        fatcaReqObject.put("rel_listed", "");
                                        fatcaReqObject.put("exch_name", "O");
                                        fatcaReqObject.put("ubo_appl", ubo_appl);
                                        fatcaReqObject.put("ubo_count", "");
                                        fatcaReqObject.put("sdf_flag", "");
                                        fatcaReqObject.put("ubo_df", ubo_df);
                                        fatcaReqObject.put("aadhaar_rp", "");
                                        fatcaReqObject.put("new_change", "");

                                        if (joint_name != null && joint_name.length() > 30) {
                                            joint_name = joint_name.substring(0, 25);
                                        }

                                        fatcaReqObject.put("log_name", joint_name);
                                        fatcaReqObject.put("ubo_exch", "");
                                        fatcaReqObject.put("ubo_isin", "");
                                        fatcaReqObject.put("ubo_rel_li", "");
                                        fatcaReqObject.put("npo_form", "N");
                                        fatcaReqObject.put("npo_dcl", "N");
                                        fatcaReqObject.put("npo_rgno", "");

                                        uboReqArray = new JSONArray();
                                        uboReqObject = new JSONObject();
                                        uboReqObject.put("ubo_name", "");
                                        uboReqObject.put("ubo_pan", "");
                                        uboReqObject.put("ubo_nation", "");
                                        uboReqObject.put("ubo_add1", "");
                                        uboReqObject.put("ubo_add2", "");
                                        uboReqObject.put("ubo_add3", "");
                                        uboReqObject.put("ubo_city", "");
                                        uboReqObject.put("ubo_pin", "");
                                        uboReqObject.put("ubo_state", "");
                                        uboReqObject.put("ubo_cntry", "");
                                        uboReqObject.put("ubo_add_ty", "");
                                        uboReqObject.put("ubo_ctr", "");
                                        uboReqObject.put("ubo_tin", "");
                                        uboReqObject.put("ubo_id_ty", "");
                                        uboReqObject.put("ubo_cob", "");
                                        uboReqObject.put("ubo_dob", "");
                                        uboReqObject.put("ubo_gender", "");
                                        uboReqObject.put("ubo_fr_nam", "");
                                        uboReqObject.put("ubo_occ", "");
                                        uboReqObject.put("ubo_occ_ty", "");
                                        uboReqObject.put("ubo_tel", "");
                                        uboReqObject.put("ubo_mobile", "");
                                        uboReqObject.put("ubo_code", "");
                                        uboReqObject.put("ubo_hol_pc", "");
                                        uboReqObject.put("ubo_categ", "");
                                        uboReqObject.put("ubo_pep_fl", "");
                                        uboReqObject.put("ubo_email", "");
                                        uboReqObject.put("ubo_smo_de", "");
                                        uboReqArray.put(uboReqObject);

                                        fatcaReqObject.put("ubo_details", uboReqArray);
                                        fatcaReqArray.put(fatcaReqObject);

                                        fatcaReqBody = new JSONObject();
                                        fatcaReqBody.put("reg_details", fatcaReqArray);

                                        System.out.println("Joint Holder 2 fatcaReqBody = " + fatcaReqBody);

                                        fatcaEntity = new HttpEntity<>(fatcaReqBody.toString(), headers);
                                        fatcaRequestUrl = nseUrl+"/nsemfdesk/api/v2/registration/FATCA_COMMON";

                                        fatcaResult = restTemplate.postForEntity(fatcaRequestUrl, fatcaEntity, String.class);
                                        fatcaStatusCode = fatcaResult.getStatusCode().toString();
                                        System.out.println("joint holder 2 fatcaStatusCode = " + fatcaStatusCode);
                                        System.out.println("joint holder 2 fatcaResponse = " + fatcaResponse);

                                        fatcaObj = new JSONObject(fatcaResponse);
                                        fatcaArr = fatcaObj.getJSONArray("reg_details");

                                        fatca_reg_id = "";
                                        fatca_reg_status = "";
                                        fatca_reg_remark = "";


                                        for (int i = 0; i < fatcaArr.length(); i++)
                                        {
                                            JSONObject regDetail = fatcaArr.getJSONObject(i);
                                            fatca_reg_id = regDetail.optString("reg_id");
                                            fatca_reg_status = regDetail.optString("reg_status");
                                            fatca_reg_remark = regDetail.optString("reg_remark");
                                        }

                                        NseTransactions nsetrans3 = new NseTransactions();
                                        nsetrans3.setUrl(fatcaRequestUrl);
                                        nsetrans3.setNse_request(fatcaReqBody.toString());
                                        nsetrans3.setNse_response(fatcaResponse);
                                        nsetrans3.setReturn_msg(fatca_reg_status);
                                        nsetrans3.setService_return_code(fatcaStatusCode);
                                        nsetrans3.setService_msg(fatca_reg_status);
                                        nsetrans3.setReg_id(fatca_reg_id);
                                        nsetrans3.setPayment_link("");
                                        nsetrans3.setPan(userDto.getJoint_holder_pan2());
                                        nsetrans3.setName(userDto.getJoint_holder_name2());
                                        nsetrans3.setBranch(userDto.getBranch());
                                        nsetrans3.setRm_name(userDto.getRm_name());
                                        nsetrans3.setSubbroker_name(userDto.getSubbroker_name());
                                        nsetrans3.setClient_name(client_name);
                                        nsetrans3.setIin_number(userDto.getNse_iin_number());
                                        nsetrans3.setScheme_name("");
                                        nsetrans3.setScheme_code("");
                                        nsetrans3.setFolio_no("");
                                        nsetrans3.setAmount_units("");
                                        nsetrans3.setFrequency("");
                                        nsetrans3.setPeriod_day("");
                                        nsetrans3.setUmrn_no("");
                                        nsetrans3.setPurchase_type("");
                                        nsetrans3.setPayment_ref_no("");
                                        nsetrans3.setUnique_number("");
                                        nsetrans3.setAuto_trxn_no("");
                                        nsetrans3.setSip_reg_no("");
                                        nsetrans3.setPayment_mode("");
                                        nsetrans3.setTopup_amount(0.0);
                                        nsetrans3.setBank_acc_no("");
                                        nsetrans3.setTransaction_number("");
                                        nsetrans3.setApplication_number("");
                                        nsetrans3.setTo_scheme_code("");
                                        nsetrans3.setTo_scheme_name("");
                                        nsetrans3.setTransaction_type("Joint Holder2 Fatca Request");
                                        nsetrans3.setTransaction_status("");
                                        nsetrans3.setPayment_status("");
                                        nsetrans3.setActive_ceased_status("");
                                        nsetrans3.setRemarks(fatca_reg_remark);
                                        nsetrans3.setMandate_id("");
                                        nsetrans3.setMandate_status("");
                                        nsetrans3.setEmandate_auth_flag("");
                                        nsetrans3.setApp_received_flag("");
                                        nsetrans3.setTransaction_date(new Date());
                                        nsetrans3.setUser_id(userDto.getUser_id());
                                        if(source.equalsIgnoreCase("Mobile")){
                                            nsetrans1.setRegister_source("Mobile App");
                                        }else {
                                            nsetrans1.setRegister_source("Website");
                                        }
                                        nsetrans3.setBroker_code(user_broker_code);
                                        nsetrans3.setEuin_number(euin);
                                        nsetrans3.setCc_received("");
                                        nsetrans3.setFund_trans_to_amc("");
                                        nsetrans3.setRefund_status("");
                                        nsetrans3.setRefund_amount("");
                                        nseTransactionService.save(nsetrans3);

                                    }
                                }

                            }
                            if (reg_status.equalsIgnoreCase("REG_SUCCESS"))
                            {
                                if (fatca_reg_status.equalsIgnoreCase("REG_FAILED"))
                                {
                                    return NseUtils.commonResponse(fatca_reg_remark+ " FATCA registration failed. Please contact your RM.", HttpStatus.BAD_REQUEST);
                                } else
                                {
                                    userDto.setNse_customer(1);
                                    userDto.setNse_iin_number(userDto.getNse_iin_number());
                                    userDto.setBroker_code(user_broker_code);
                                    userDto.setEuin(euin);
                                    userDto.setNse_active(1);
                                    userDto.setOnline_flag("NSE");
                                    userDto.setUser_id(Integer.parseInt(userid));
                                    userDto.setClient_name(client_name);
                                    try {
                                        userServiceClient.saveUserNseSuccessResponse(userDto, token);
                                    }catch (FeignException e) {
                                        String errorMessage = e.contentUTF8();

                                        if (errorMessage == null || errorMessage.isEmpty()) {
                                            errorMessage = "Unexpected error occurred.";
                                        }
                                        if (e.status() == 409)
                                        {
                                            return NseUtils.commonResponse("Duplicate entry for this user.", HttpStatus.CONFLICT);
                                        } else if (e.status() == 400) {
                                            return NseUtils.commonResponse(errorMessage, HttpStatus.BAD_REQUEST);
                                        } else {
                                            return NseUtils.commonResponse("Unexpected error from User Service: " + errorMessage,
                                                    HttpStatus.INTERNAL_SERVER_ERROR);
                                        }
                                    }

                                    if(source.equalsIgnoreCase("Mobile"))
                                    {
                                        userServiceClient.saveUserRegStatus(token);
                                    }

                                    return NseUtils.commonResponse("Registration successful", HttpStatus.OK);
                                }
                            } else
                            {
                                return NseUtils.commonResponse(fatca_reg_remark, HttpStatus.BAD_REQUEST);
                            }
                        }else
                        {
                            System.out.println("responseBody = " + responseBody);

                            String jsonPart = responseBody.substring(responseBody.indexOf("{"));

                            JSONObject json = new JSONObject(jsonPart);

                            String message = json.getString("message");

                            return NseUtils.commonResponse(message, HttpStatus.BAD_REQUEST);

                        }
                    } catch (Exception ex)
                    {
                        System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
                        ex.printStackTrace();
                        return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }

            } else if (userData != null)
            {
                return ResponseEntity.ok(userData);
            } else
            {
                return NseUtils.commonResponse("User details not available. Please contact admin.", HttpStatus.BAD_REQUEST);
            }

            return NseUtils.commonResponse("Registration successful", HttpStatus.OK);

        } catch (Exception ex)
        {
            //logExceptionService.save(Integer.parseInt(userid), client_name, NseUtils.getFullRequestUrl(request), ex.getMessage(), request.getMethod(), NseUtils.getIpAddr(request), source);
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Modify with NSE",
            description = "Registers a user with NSE by collecting and transforming all mandatory user details, including nominee information, FATCA compliance, occupation details, holding nature, and joint holder relationships. The service fetches user data based on the 'multiple_reg' flag, enriches it with state codes from nominee pincodes, assigns default relation codes, and ensures FATCA fields are correctly populated before forwarding the data to NSE."
    )

    @ApiResponses(value =
            {
                    @ApiResponse(responseCode = "200", description = "Success Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SuccessResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Failure Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommonResponse.class))),
            })
    @PostMapping("/updateUserDetailsByIIN")
    public ResponseEntity<?> modifyWithNse(
            HttpServletRequest request,
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String iin_number,
            @RequestParam(required = false) String pan,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) String email_relation,
            @RequestParam(required = false) String mobile_relation,
            @RequestParam(required = false) String dob,
            @RequestParam(required = false) String tax_status,
            @RequestParam(required = false) String tax_status_des,
            @RequestParam(required = false) String holding_nature,
            @RequestParam(required = false) String holding_nature_desc,
            @RequestParam(required = false) String guard_name,
            @RequestParam(required = false) String guard_pan,
            @RequestParam(required = false) String guard_dob,
            @RequestParam(required = false) String guard_mobile,
            @RequestParam(required = false) String guard_email,
            @RequestParam(required = false) String guard_relation,
            @RequestParam(required = false) String guard_account_relation,
            @RequestParam(required = false) String father_name,
            //@RequestParam(required = false) String place_birth,
            @RequestParam(required = false) String country_birth,
            @RequestParam(required = false) String country_birth_code,
            @RequestParam(required = false) String occupation,
            @RequestParam(required = false) String occupation_code,
//            @RequestParam(required = false) String income,
//            @RequestParam(required = false) String income_code,
//            @RequestParam(required = false) String source_wealth,
//            @RequestParam(required = false) String source_wealth_code,
//            @RequestParam(required = false) String political_status,
            @RequestParam(required = false) String address1,
            @RequestParam(required = false) String address2,
            @RequestParam(required = false) String address3,
            @RequestParam(required = false) String pincode,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String state_code,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String ifsc_code,
            @RequestParam(required = false) String micr_code,
            @RequestParam(required = false) String bank_name,
            @RequestParam(required = false) String bank_code,
            @RequestParam(required = false) String branch_name,
            @RequestParam(required = false) String bank_address,
            @RequestParam(required = false) String account_number,
            @RequestParam(required = false) String account_holder_name,
            @RequestParam(required = false) String account_type,
            @RequestParam(required = false) String account_desc,
            @RequestParam(required = false) String ifsc_code2,
            @RequestParam(required = false) String micr_code2,
            @RequestParam(required = false) String bank_name2,
            @RequestParam(required = false) String bank_code2,
            @RequestParam(required = false) String branch_name2,
            @RequestParam(required = false) String bank_address2,
            @RequestParam(required = false) String account_number2,
            @RequestParam(required = false) String account_holder_name2,
            @RequestParam(required = false) String account_type2,
            @RequestParam(required = false) String account_desc2,
            @RequestParam(required = false) String ifsc_code3,
            @RequestParam(required = false) String micr_code3,
            @RequestParam(required = false) String bank_name3,
            @RequestParam(required = false) String bank_code3,
            @RequestParam(required = false) String branch_name3,
            @RequestParam(required = false) String bank_address3,
            @RequestParam(required = false) String account_number3,
            @RequestParam(required = false) String account_holder_name3,
            @RequestParam(required = false) String account_type3,
            @RequestParam(required = false) String account_desc3,
            @RequestParam(required = false) String ifsc_code4,
            @RequestParam(required = false) String micr_code4,
            @RequestParam(required = false) String bank_name4,
            @RequestParam(required = false) String bank_code4,
            @RequestParam(required = false) String branch_name4,
            @RequestParam(required = false) String bank_address4,
            @RequestParam(required = false) String account_number4,
            @RequestParam(required = false) String account_holder_name4,
            @RequestParam(required = false) String account_type4,
            @RequestParam(required = false) String account_desc4,
            @RequestParam(required = false) String ifsc_code5,
            @RequestParam(required = false) String micr_code5,
            @RequestParam(required = false) String bank_name5,
            @RequestParam(required = false) String bank_code5,
            @RequestParam(required = false) String branch_name5,
            @RequestParam(required = false) String bank_address5,
            @RequestParam(required = false) String account_number5,
            @RequestParam(required = false) String account_holder_name5,
            @RequestParam(required = false) String account_type5,
            @RequestParam(required = false) String account_desc5,
            @RequestParam(required = false) String joint_holder_name,
            @RequestParam(required = false) String joint_holder_pan,
            @RequestParam(required = false) String joint_holder_email,
            @RequestParam(required = false) String joint_holder_mobile,
            @RequestParam(required = false) String joint_holder_dob,
            @RequestParam(required = false) String joint_holder_name1,
            @RequestParam(required = false) String joint_holder_pan1,
            @RequestParam(required = false) String joint_holder_dob1,
            @RequestParam(required = false) String joint_holder_email1,
            @RequestParam(required = false) String joint_holder_mobile1,
            @RequestParam(required = false) String joint_holder_email_relation,
            @RequestParam(required = false) String joint_holder_email_relation1,
            @RequestParam(required = false) String joint_holder_mobile_relation,
            @RequestParam(required = false) String joint_holder_mobile_relation1,
            @RequestParam(required = false) String nri_address1,
            @RequestParam(required = false) String nri_address2,
            @RequestParam(required = false) String nri_address3,
            @RequestParam(required = false) String nri_city,
            @RequestParam(required = false) String nri_state,
            @RequestParam(required = false) String nri_pincode,
            @RequestParam(required = false) String nri_country,
            @RequestParam(required = false) String address_type,
            @RequestParam(required = false) String address_type_desc,
            //@RequestParam(required = false) String joint_holder_place_birth,
            @RequestParam(required = false) String joint_holder_country_birth,
            @RequestParam(required = false) String joint_holder_occupation,
            @RequestParam(required = false) String joint_holder_income,
            @RequestParam(required = false) String joint_holder_source_wealth,
            @RequestParam(required = false) String joint_holder_address_type,
            @RequestParam(required = false) String joint_holder_political,
            //@RequestParam(required = false) String joint_holder_place_birth1,
            @RequestParam(required = false) String joint_holder_country_birth1,
            @RequestParam(required = false) String joint_holder_occupation1,
            @RequestParam(required = false) String joint_holder_income1,
            @RequestParam(required = false) String joint_holder_source_wealth1,
            @RequestParam(required = false) String joint_holder_address_type1,
            @RequestParam(required = false) String joint_holder_political1,
            @RequestParam(required = false) String number_of_nominee,
            @RequestParam(required = false) String number_of_nominee_desc,
            @RequestParam(required = false) String nominee_type,
            @RequestParam(required = false) String nominee_type_desc,
            @RequestParam(required = false) String nominee_soa,
            @RequestParam(required = false) String nominee1_name,
            @RequestParam(required = false) String nominee1_dob,
            @RequestParam(required = false) String nominee1_address1,
            @RequestParam(required = false) String nominee1_address2,
            @RequestParam(required = false) String nominee1_address3,
            @RequestParam(required = false) String nominee1_pincode,
            @RequestParam(required = false) String nominee1_city,
            @RequestParam(required = false) String nominee1_state,
            @RequestParam(required = false) String nominee1_state_code,
            @RequestParam(required = false) String nominee1_country,
            @RequestParam(required = false) String nominee1_id_type,
            @RequestParam(required = false) String nominee1_id_no,
            @RequestParam(required = false) String nominee1_email,
            @RequestParam(required = false) String nominee1_mobile,
            @RequestParam(required = false) String nominee1_relation,
            @RequestParam(required = false) String nominee1_guard_name,
            @RequestParam(required = false) String nominee1_guard_pan,
            @RequestParam(required = false) String nominee1_guard_relationship,
            @RequestParam(required = false) String nominee1_percentage,
            @RequestParam(required = false) String nominee2_type,
            @RequestParam(required = false) String nominee2_type_desc,
            @RequestParam(required = false) String nominee2_name,
            @RequestParam(required = false) String nominee2_dob,
            @RequestParam(required = false) String nominee2_relation,
            @RequestParam(required = false) String nominee2_percentage,
            @RequestParam(required = false) String nominee2_address1,
            @RequestParam(required = false) String nominee2_address2,
            @RequestParam(required = false) String nominee2_address3,
            @RequestParam(required = false) String nominee2_pincode,
            @RequestParam(required = false) String nominee2_city,
            @RequestParam(required = false) String nominee2_state,
            @RequestParam(required = false) String nominee2_state_code,
            @RequestParam(required = false) String nominee2_country,
            @RequestParam(required = false) String nominee2_id_type,
            @RequestParam(required = false) String nominee2_id_no,
            @RequestParam(required = false) String nominee2_email,
            @RequestParam(required = false) String nominee2_mobile,
            @RequestParam(required = false) String nominee2_guard_name,
            @RequestParam(required = false) String nominee2_guard_pan,
            @RequestParam(required = false) String nominee2_guard_relationship,
            @RequestParam(required = false) String nominee3_type,
            @RequestParam(required = false) String nominee3_type_desc,
            @RequestParam(required = false) String nominee3_name,
            @RequestParam(required = false) String nominee3_dob,
            @RequestParam(required = false) String nominee3_relation,
            @RequestParam(required = false) String nominee3_percentage,
            @RequestParam(required = false) String nominee3_address1,
            @RequestParam(required = false) String nominee3_address2,
            @RequestParam(required = false) String nominee3_address3,
            @RequestParam(required = false) String nominee3_pincode,
            @RequestParam(required = false) String nominee3_city,
            @RequestParam(required = false) String nominee3_state,
            @RequestParam(required = false) String nominee3_state_code,
            @RequestParam(required = false) String nominee3_country,
            @RequestParam(required = false) String nominee3_id_type,
            @RequestParam(required = false) String nominee3_id_no,
            @RequestParam(required = false) String nominee3_email,
            @RequestParam(required = false) String nominee3_mobile,
            @RequestParam(required = false) String nominee3_guard_name,
            @RequestParam(required = false) String nominee3_guard_pan,
            @RequestParam(required = false) String nominee3_guard_relationship,
            @RequestParam(required = false) String networth_dob,
            @RequestParam(required = false) String networth_amount,
            @RequestParam(required = false) String occupation_other,
            @RequestParam(required = false) String source_wealth_other,
            @RequestParam(required = false) String joint_holder_occupation_other,
            @RequestParam(required = false) String joint_source_wealth_other,
            @RequestParam(required = false) String joint_holder_occupation_other1,
            @RequestParam(required = false) String joint_source_wealth_other1,
            @RequestParam(required = false) String alter_mobile,
            @RequestParam(required = false) String alter_email,
            @RequestParam(required = false) String inv_category,
            @RequestParam(required = false) String gaurd_relation_proof,
            @RequestParam(required = false) String residence_phone,
            @RequestParam(required = false) String office_phone,
            @RequestParam(required = false) String bank_proof,
            @RequestParam(required = false) String nominee1_guard_dob,
            @RequestParam(required = false) String nominee2_guard_dob,
            @RequestParam(required = false) String nominee3_guard_dob,
            @RequestParam(required = false) String mobile_isd_code,
            @RequestParam(required = false) String joint_holder_mobile1_isd_code,
            @RequestParam(required = false) String joint_holder_mobile2_isd_code,
            @RequestParam(required = false) String arn_number,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String source
    ){

        try {
            String userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);

            System.out.println("-----------------------------------");
            System.out.println("nominee1_id_no = " + nominee1_id_no);
            System.out.println("nominee1_id_type = " + nominee1_id_type);
            System.out.println("nominee1_email = " + nominee1_email);
            System.out.println("nominee1_mobile = " + nominee1_mobile);
            System.out.println("-----------------------------------");

            UserDto users =null;
            try {
                users = userServiceClient.getUserById(Integer.valueOf(userid), token);
            } catch (FeignException e)
            {
                return FeignErrorHandler.handle(e, "User Service", "User not found");
            }
            String client_name = users.getClient_name();

            // Add this block after all @RequestParam declarations inside the method
            iin_number = NseUtils.checkParem(iin_number);
            pan = NseUtils.checkParem(pan);
            name = NseUtils.checkParem(name);
            email = NseUtils.checkParem(email);
            mobile = NseUtils.checkParem(mobile);
            email_relation = NseUtils.checkParem(email_relation);
            mobile_relation = NseUtils.checkParem(mobile_relation);
            dob = NseUtils.checkParem(dob);
            tax_status = NseUtils.checkParem(tax_status);
            tax_status_des = NseUtils.checkParem(tax_status_des);
            holding_nature = NseUtils.checkParem(holding_nature);
            holding_nature_desc = NseUtils.checkParem(holding_nature_desc);
            guard_name = NseUtils.checkParem(guard_name);
            guard_pan = NseUtils.checkParem(guard_pan);
            guard_dob = NseUtils.checkParem(guard_dob);
            guard_mobile = NseUtils.checkParem(guard_mobile);
            guard_email = NseUtils.checkParem(guard_email);
            guard_relation = NseUtils.checkParem(guard_relation);
            guard_account_relation = NseUtils.checkParem(guard_account_relation);
            father_name = NseUtils.checkParem(father_name);
            //place_birth = NseUtils.checkParem(place_birth);
            country_birth = NseUtils.checkParem(country_birth);
            country_birth_code = NseUtils.checkParem(country_birth_code);
            occupation = NseUtils.checkParem(occupation);
            occupation_code = NseUtils.checkParem(occupation_code);
//            income = NseUtils.checkParem(income);
//            income_code = NseUtils.checkParem(income_code);
//            source_wealth = NseUtils.checkParem(source_wealth);
//            source_wealth_code = NseUtils.checkParem(source_wealth_code);
//            political_status = NseUtils.checkParem(political_status);
            address1 = NseUtils.checkParem(address1);
            address2 = NseUtils.checkParem(address2);
            address3 = NseUtils.checkParem(address3);
            pincode = NseUtils.checkParem(pincode);
            city = NseUtils.checkParem(city);
            state = NseUtils.checkParem(state);
            state_code = NseUtils.checkParem(state_code);
            country = NseUtils.checkParem(country);
            ifsc_code = NseUtils.checkParem(ifsc_code);
            micr_code = NseUtils.checkParem(micr_code);
            bank_name = NseUtils.checkParem(bank_name);
            branch_name = NseUtils.checkParem(branch_name);
            bank_address = NseUtils.checkParem(bank_address);
            account_number = NseUtils.checkParem(account_number);
            account_holder_name = NseUtils.checkParem(account_holder_name);
            account_type = NseUtils.checkParem(account_type);

            ifsc_code2 = NseUtils.checkParem(ifsc_code2);
            micr_code2 = NseUtils.checkParem(micr_code2);
            bank_name2 = NseUtils.checkParem(bank_name2);
            branch_name2 = NseUtils.checkParem(branch_name2);
            bank_address2 = NseUtils.checkParem(bank_address2);
            account_number2 = NseUtils.checkParem(account_number2);
            account_holder_name2 = NseUtils.checkParem(account_holder_name2);
            account_type2 = NseUtils.checkParem(account_type2);

            ifsc_code3 = NseUtils.checkParem(ifsc_code3);
            micr_code3 = NseUtils.checkParem(micr_code3);
            bank_name3 = NseUtils.checkParem(bank_name3);
            branch_name3 = NseUtils.checkParem(branch_name3);
            bank_address3 = NseUtils.checkParem(bank_address3);
            account_number3 = NseUtils.checkParem(account_number3);
            account_holder_name3 = NseUtils.checkParem(account_holder_name3);
            account_type3 = NseUtils.checkParem(account_type3);

            ifsc_code4 = NseUtils.checkParem(ifsc_code4);
            micr_code4 = NseUtils.checkParem(micr_code4);
            bank_name4 = NseUtils.checkParem(bank_name4);
            branch_name4 = NseUtils.checkParem(branch_name4);
            bank_address4 = NseUtils.checkParem(bank_address4);
            account_number4 = NseUtils.checkParem(account_number4);
            account_holder_name4 = NseUtils.checkParem(account_holder_name4);
            account_type4 = NseUtils.checkParem(account_type4);

            ifsc_code5 = NseUtils.checkParem(ifsc_code5);
            micr_code5 = NseUtils.checkParem(micr_code5);
            bank_name5 = NseUtils.checkParem(bank_name5);
//            bank_code5 = NseUtils.checkParem(bank_code5);
            branch_name5 = NseUtils.checkParem(branch_name5);
            bank_address5 = NseUtils.checkParem(bank_address5);
            account_number5 = NseUtils.checkParem(account_number5);
            account_holder_name5 = NseUtils.checkParem(account_holder_name5);
            account_type5 = NseUtils.checkParem(account_type5);

            joint_holder_name = NseUtils.checkParem(joint_holder_name);
            joint_holder_pan = NseUtils.checkParem(joint_holder_pan);
            joint_holder_email = NseUtils.checkParem(joint_holder_email);
            joint_holder_mobile = NseUtils.checkParem(joint_holder_mobile);
            joint_holder_dob = NseUtils.checkParem(joint_holder_dob);
            joint_holder_name1 = NseUtils.checkParem(joint_holder_name1);
            joint_holder_pan1 = NseUtils.checkParem(joint_holder_pan1);
            joint_holder_dob1 = NseUtils.checkParem(joint_holder_dob1);
            joint_holder_email1 = NseUtils.checkParem(joint_holder_email1);
            joint_holder_mobile1 = NseUtils.checkParem(joint_holder_mobile1);
            joint_holder_email_relation = NseUtils.checkParem(joint_holder_email_relation);
            joint_holder_email_relation1 = NseUtils.checkParem(joint_holder_email_relation1);
            joint_holder_mobile_relation = NseUtils.checkParem(joint_holder_mobile_relation);
            joint_holder_mobile_relation1 = NseUtils.checkParem(joint_holder_mobile_relation1);
            nri_address1 = NseUtils.checkParem(nri_address1);
            nri_address2 = NseUtils.checkParem(nri_address2);
            nri_address3 = NseUtils.checkParem(nri_address3);
            nri_city = NseUtils.checkParem(nri_city);
            nri_state = NseUtils.checkParem(nri_state);
            nri_pincode = NseUtils.checkParem(nri_pincode);
            nri_country = NseUtils.checkParem(nri_country);
            address_type = NseUtils.checkParem(address_type);
            address_type_desc = NseUtils.checkParem(address_type_desc);
            //joint_holder_place_birth = NseUtils.checkParem(joint_holder_place_birth);
            joint_holder_country_birth = NseUtils.checkParem(joint_holder_country_birth);
            joint_holder_occupation = NseUtils.checkParem(joint_holder_occupation);
            joint_holder_income = NseUtils.checkParem(joint_holder_income);
            joint_holder_source_wealth = NseUtils.checkParem(joint_holder_source_wealth);
            joint_holder_address_type = NseUtils.checkParem(joint_holder_address_type);
            joint_holder_political = NseUtils.checkParem(joint_holder_political);
            //joint_holder_place_birth1 = NseUtils.checkParem(joint_holder_place_birth1);
            joint_holder_country_birth1 = NseUtils.checkParem(joint_holder_country_birth1);
            joint_holder_occupation1 = NseUtils.checkParem(joint_holder_occupation1);
            joint_holder_income1 = NseUtils.checkParem(joint_holder_income1);
            joint_holder_source_wealth1 = NseUtils.checkParem(joint_holder_source_wealth1);
            joint_holder_address_type1 = NseUtils.checkParem(joint_holder_address_type1);
            joint_holder_political1 = NseUtils.checkParem(joint_holder_political1);
            number_of_nominee = NseUtils.checkParem(number_of_nominee);
//            number_of_nominee_desc = NseUtils.checkParem(number_of_nominee_desc);
            nominee_type = NseUtils.checkParem(nominee_type);
//            nominee_type_desc = NseUtils.checkParem(nominee_type_desc);
            nominee1_name = NseUtils.checkParem(nominee1_name);
            nominee1_dob = NseUtils.checkParem(nominee1_dob);
            nominee1_address1 = NseUtils.checkParem(nominee1_address1);
            nominee1_address2 = NseUtils.checkParem(nominee1_address2);
            nominee1_address3 = NseUtils.checkParem(nominee1_address3);
            nominee1_pincode = NseUtils.checkParem(nominee1_pincode);
            nominee1_city = NseUtils.checkParem(nominee1_city);
            nominee1_state = NseUtils.checkParem(nominee1_state);
            nominee1_state_code = NseUtils.checkParem(nominee1_state_code);
            nominee1_country = NseUtils.checkParem(nominee1_country);
            nominee1_id_type = NseUtils.checkParem(nominee1_id_type);
            nominee1_id_no = NseUtils.checkParem(nominee1_id_no);
            nominee1_email = NseUtils.checkParem(nominee1_email);
            nominee1_mobile = NseUtils.checkParem(nominee1_mobile);
            nominee1_relation = NseUtils.checkParem(nominee1_relation);
            nominee1_guard_name = NseUtils.checkParem(nominee1_guard_name);
            nominee1_guard_pan = NseUtils.checkParem(nominee1_guard_pan);
            nominee1_guard_relationship = NseUtils.checkParem(nominee1_guard_relationship);
            nominee1_percentage = NseUtils.checkParem(nominee1_percentage);
            nominee2_type = NseUtils.checkParem(nominee2_type);
//            nominee2_type_desc = NseUtils.checkParem(nominee2_type_desc);
            nominee2_name = NseUtils.checkParem(nominee2_name);
            nominee2_dob = NseUtils.checkParem(nominee2_dob);
            nominee2_relation = NseUtils.checkParem(nominee2_relation);
            nominee2_percentage = NseUtils.checkParem(nominee2_percentage);
            nominee2_address1 = NseUtils.checkParem(nominee2_address1);
//            nominee2_address2 = NseUtils.checkParem(nominee2_address2);
//            nominee2_address3 = NseUtils.checkParem(nominee2_address3);
            nominee2_pincode = NseUtils.checkParem(nominee2_pincode);
            nominee2_city = NseUtils.checkParem(nominee2_city);
            nominee2_state = NseUtils.checkParem(nominee2_state);
            nominee2_state_code = NseUtils.checkParem(nominee2_state_code);
            nominee2_country = NseUtils.checkParem(nominee2_country);
            nominee2_id_type = NseUtils.checkParem(nominee2_id_type);
            nominee2_id_no = NseUtils.checkParem(nominee2_id_no);
            nominee2_email = NseUtils.checkParem(nominee2_email);
            nominee2_mobile = NseUtils.checkParem(nominee2_mobile);
            nominee2_guard_name = NseUtils.checkParem(nominee2_guard_name);
            nominee2_guard_pan = NseUtils.checkParem(nominee2_guard_pan);
            nominee2_guard_relationship = NseUtils.checkParem(nominee2_guard_relationship);
            nominee3_type = NseUtils.checkParem(nominee3_type);
//            nominee3_type_desc = NseUtils.checkParem(nominee3_type_desc);
            nominee3_name = NseUtils.checkParem(nominee3_name);
            nominee3_dob = NseUtils.checkParem(nominee3_dob);
            nominee3_relation = NseUtils.checkParem(nominee3_relation);
            nominee3_percentage = NseUtils.checkParem(nominee3_percentage);
            nominee3_address1 = NseUtils.checkParem(nominee3_address1);
//            nominee3_address2 = NseUtils.checkParem(nominee3_address2);
//            nominee3_address3 = NseUtils.checkParem(nominee3_address3);
            nominee3_pincode = NseUtils.checkParem(nominee3_pincode);
            nominee3_city = NseUtils.checkParem(nominee3_city);
            nominee3_state = NseUtils.checkParem(nominee3_state);
            nominee3_state_code = NseUtils.checkParem(nominee3_state_code);
            nominee3_country = NseUtils.checkParem(nominee3_country);
            nominee3_id_type = NseUtils.checkParem(nominee3_id_type);
            nominee3_id_no = NseUtils.checkParem(nominee3_id_no);
            nominee3_email = NseUtils.checkParem(nominee3_email);
            nominee3_mobile = NseUtils.checkParem(nominee3_mobile);
            nominee3_guard_name = NseUtils.checkParem(nominee3_guard_name);
            nominee3_guard_pan = NseUtils.checkParem(nominee3_guard_pan);
            nominee3_guard_relationship = NseUtils.checkParem(nominee3_guard_relationship);
            networth_dob = NseUtils.checkParem(networth_dob);
            networth_amount = NseUtils.checkParem(networth_amount);
            occupation_other = NseUtils.checkParem(occupation_other);
//            source_wealth_other = NseUtils.checkParem(source_wealth_other);
//            joint_holder_occupation_other = NseUtils.checkParem(joint_holder_occupation_other);
//            joint_source_wealth_other = NseUtils.checkParem(joint_source_wealth_other);
//            joint_holder_occupation_other1 = NseUtils.checkParem(joint_holder_occupation_other1);
//            joint_source_wealth_other1 = NseUtils.checkParem(joint_source_wealth_other1);
            alter_mobile = NseUtils.checkParem(alter_mobile);
            alter_email = NseUtils.checkParem(alter_email);
            inv_category = NseUtils.checkParem(inv_category);
            gaurd_relation_proof = NseUtils.checkParem(gaurd_relation_proof);
            residence_phone = NseUtils.checkParem(residence_phone);
            office_phone = NseUtils.checkParem(office_phone);
            bank_proof = NseUtils.checkParem(bank_proof);
            nominee1_guard_dob = NseUtils.checkParem(nominee1_guard_dob);
            nominee2_guard_dob = NseUtils.checkParem(nominee2_guard_dob);
            nominee3_guard_dob = NseUtils.checkParem(nominee3_guard_dob);
            mobile_isd_code = NseUtils.checkParem(mobile_isd_code);
            joint_holder_mobile1_isd_code = NseUtils.checkParem(joint_holder_mobile1_isd_code);
            joint_holder_mobile2_isd_code = NseUtils.checkParem(joint_holder_mobile2_isd_code);
            arn_number = NseUtils.checkParem(arn_number);
            gender = NseUtils.checkParem(gender);
            nominee_soa = NseUtils.checkParem(nominee_soa);

            if(nominee_soa.isEmpty())
            {
                nominee_soa = "N";
            }

            String euin = "";
            if (!arn_number.isEmpty()) {
                BseNseKeyDto nsekey = userServiceClient.getByClientName(client_name,token);
                String broker_code1 = nsekey.getBrokerCode();
                if (broker_code1 == null) {
                    broker_code1 = "";
                }

                euin = nsekey.getEuin();
                euin = euin.split(",")[0];
            }

            UserDto user = null;
            try {
                user =  userServiceClient.getUserDetailsByID(client_name, Integer.valueOf(userid),token);
            }catch (FeignException e)
            {
                return FeignErrorHandler.handle(e, "User Service", "User not found");
            }

            System.out.println("iin_number = " + iin_number);
            System.out.println("client_name = " + client_name);
            System.out.println("arn_number = " + arn_number);

            UserDto nse = null;
            try {
                nse = userServiceClient.getUserBseNseDetailsByNseIINNumberBrokerCode(client_name, iin_number, arn_number,token);
            }catch (FeignException e)
            {
                return FeignErrorHandler.handle(e, "User Service", "User not found");
            }

            System.out.println("-----------------------------------");
            System.out.println("nse = " + new Gson().toJson(nse));
            System.out.println("-----------------------------------");

            if (nse != null)
            {
                nse.setPan(pan);
                nse.setName(name.replaceAll("\\s+", " ").trim());
                nse.setEmail(email);
                nse.setMobile(mobile);
                nse.setMobile_isd_code(mobile_isd_code);
                nse.setMobile_relation(mobile_relation);
                nse.setEmail_relation(email_relation);
                nse.setAlter_email(alter_email);
                nse.setAlter_mobile(alter_mobile);
                nse.setDate_of_birth(dob);
                nse.setFather_name(father_name);
                nse.setPhone_office(office_phone);
                nse.setPhone_residence(residence_phone);
                //nse.setPlace_of_birth(place_birth);
                nse.setCountry_of_birth(country_birth);
                nse.setCountry_birth_code(country_birth_code);
                nse.setInv_category(inv_category);
                nse.setOccupation(occupation);
                nse.setOccupation_code(occupation_code);
                if (occupation_code.equalsIgnoreCase("99") && !occupation_other.isEmpty()) {
                    nse.setOccupation(occupation_other);
                }
//                nse.setAnnual_income(income);
//                nse.setAnnual_income_code(income_code);
//                nse.setSource_of_wealth(source_wealth);
//                nse.setSource_of_wealth_code(source_wealth_code);
//                if (source_wealth_code.equalsIgnoreCase("08") && !source_wealth_other.isEmpty()) {
//                    nse.setSource_of_wealth(source_wealth_other);
//                }
//                nse.setPolitical_code(political_status);
//                if (political_status.equalsIgnoreCase("Y") || political_status.equalsIgnoreCase("PEP")) {
//                    nse.setPolitical("I am Politically exposed person");
//                }
//                if (political_status.equalsIgnoreCase("R") || political_status.equalsIgnoreCase("RPEP")) {
//                    nse.setPolitical("I am related to Politically exposed person");
//                }
//                if (political_status.equalsIgnoreCase("N") || political_status.equalsIgnoreCase("NA")) {
//                    nse.setPolitical("Not Applicable");
//                }
                nse.setPincode(pincode);
                nse.setCity(city);
                nse.setState(state);
                System.out.println("updateUserDetailsByIIN::UserBseNseDetails::country: " + country);
                nse.setCountry(country);

                nse.setStreet_1(address1);
                nse.setStreet_2(address2);
                nse.setStreet_3(address3);
                nse.setState_code(state_code);
                nse.setBank_ifsc_code1(ifsc_code);
                nse.setBank_micr_code1(micr_code);
                nse.setBank_name1(bank_name);
                nse.setBank_branch1(branch_name);
                nse.setBank_address1(bank_address);
                nse.setBank_account_number1(account_number);
                nse.setBank_account_holder_name1(account_holder_name);
                nse.setBank_account_type1(account_type);
                nse.setDefault_bank1("Y");

                nse.setBank_ifsc_code1(ifsc_code);
                nse.setBank_micr_code1(micr_code);
                nse.setBank_name1(bank_name);
                nse.setBank_branch1(branch_name);
                nse.setBank_address1(bank_address);
                nse.setBank_account_number1(account_number);
                nse.setBank_account_holder_name1(account_holder_name);
                nse.setBank_account_type1(account_type);

                nse.setBank_ifsc_code2(ifsc_code2);
                nse.setBank_micr_code2(micr_code2);
                nse.setBank_name2(bank_name2);
                nse.setBank_branch2(branch_name2);
                nse.setBank_address2(bank_address2);
                nse.setBank_account_number2(account_number2);
                nse.setBank_account_holder_name2(account_holder_name2);
                nse.setBank_account_type2(account_type2);

                nse.setBank_ifsc_code3(ifsc_code3);
                nse.setBank_micr_code3(micr_code3);
                nse.setBank_name3(bank_name3);
                nse.setBank_branch3(branch_name3);
                nse.setBank_address3(bank_address3);
                nse.setBank_account_number3(account_number3);
                nse.setBank_account_holder_name3(account_holder_name3);
                nse.setBank_account_type3(account_type3);

                nse.setBank_ifsc_code4(ifsc_code4);
                nse.setBank_micr_code4(micr_code4);
                nse.setBank_name4(bank_name4);
                nse.setBank_branch4(branch_name4);
                nse.setBank_address4(bank_address4);
                nse.setBank_account_number4(account_number4);
                nse.setBank_account_holder_name4(account_holder_name4);
                nse.setBank_account_type4(account_type4);

                nse.setBank_ifsc_code5(ifsc_code5);
                nse.setBank_micr_code5(micr_code5);
                nse.setBank_name5(bank_name5);
                nse.setBank_branch5(branch_name5);
                nse.setBank_address5(bank_address5);
                nse.setBank_account_number5(account_number5);
                nse.setBank_account_holder_name5(account_holder_name5);
                nse.setBank_account_type5(account_type5);

                nse.setBank_proof1(bank_proof);
                nse.setGuard_name(guard_name);
                nse.setGuard_pan(guard_pan);
                nse.setGuard_dob(guard_dob);
                nse.setGuard_mobile(guard_mobile);
                nse.setGuard_email(guard_email);
                nse.setGuard_relationship(guard_relation);
                nse.setGuard_account_relation(guard_account_relation);
                nse.setGuard_relation_proof(gaurd_relation_proof);
                nse.setTax_status_code(tax_status);
                nse.setTax_status(tax_status_des);
                nse.setJoint_holder_name1(joint_holder_name);
                nse.setJoint_holder_name2(joint_holder_name1);
                nse.setJoint_holder_dob1(joint_holder_dob);
                nse.setJoint_holder_dob2(joint_holder_dob1);
                nse.setJoint_holder_email1(joint_holder_email);
                nse.setJoint_holder_email2(joint_holder_email1);
                nse.setJoint_holder_mobile1(joint_holder_mobile);
                nse.setJoint_holder_mobile1_isd_code(joint_holder_mobile1_isd_code);
                nse.setJoint_holder_mobile2(joint_holder_mobile1);
                nse.setJoint_holder_mobile2_isd_code(joint_holder_mobile2_isd_code);
                nse.setJoint_holder_email_relation1(joint_holder_email_relation);
                nse.setJoint_holder_email_relation2(joint_holder_email_relation1);
                nse.setJoint_holder_mobile_relation1(joint_holder_mobile_relation);
                nse.setJoint_holder_mobile_relation2(joint_holder_mobile_relation1);

                nse.setJoint_holder_pan1(joint_holder_pan);
                nse.setJoint_holder_pan2(joint_holder_pan1);
                nse.setHolding_nature_code(holding_nature);
                nse.setHolding_nature(holding_nature_desc);
                nse.setGender(gender);
                nse.setClient_name(client_name);
                nse.setNri_address1(nri_address1);
                nse.setNri_address2(nri_address2);
                nse.setNri_address3(nri_address3);
                nse.setNri_city(nri_city);
                nse.setNri_state(nri_state);
                nse.setNri_pincode(nri_pincode);
                nse.setNri_country(nri_country);
                nse.setAddress_type_code(address_type);
                nse.setAddress_type(address_type_desc);

                //nse.setJoint_holder_place_of_birth1(joint_holder_place_birth);
                //nse.setJoint_holder_place_of_birth2(joint_holder_place_birth1);
                nse.setJoint_holder_country_birth_code1(joint_holder_country_birth);
                nse.setJoint_holder_country_birth_code2(joint_holder_country_birth1);
                nse.setJoint_holder_occupation_code1(joint_holder_occupation);
//                if (joint_holder_occupation.equalsIgnoreCase("99") && !joint_holder_occupation_other.isEmpty()) {
//                    //nse.setJoint_holder_occupation_other1(joint_holder_occupation_other);
//                }
                nse.setJoint_holder_occupation_code2(joint_holder_occupation1);
//                if (joint_holder_occupation1.equalsIgnoreCase("99") && !joint_holder_occupation_other1.isEmpty()) {
//                    //nse.setJoint_holder_occupation_other2(joint_holder_occupation_other1);
//                }
                nse.setJoint_holder_source_of_wealth_code1(joint_holder_source_wealth);
//                if (joint_holder_source_wealth.equalsIgnoreCase("08") && !joint_source_wealth_other.isEmpty()) {
//                    //nse.setJoint_holder_source_of_wealth_other1(joint_source_wealth_other);
//                }
                nse.setJoint_holder_source_of_wealth_code2(joint_holder_source_wealth1);
//                if (joint_holder_source_wealth1.equalsIgnoreCase("08") && !joint_source_wealth_other1.isEmpty()) {
//                    //nse.setJoint_holder_source_of_wealth_other2(joint_source_wealth_other1);
//                }
                nse.setJoint_holder_annual_income_code1(joint_holder_income);
                nse.setJoint_holder_annual_income_code2(joint_holder_income1);
                nse.setJoint_holder_address_type_code1(joint_holder_address_type);
                nse.setJoint_holder_address_type_code2(joint_holder_address_type1);
                nse.setJoint_holder_political_code1(joint_holder_political);
                nse.setJoint_holder_political_code2(joint_holder_political1);
                nse.setNominee_soa(nominee_soa);

                nse.setNominee1_type(nominee_type);
                nse.setNominee1_guard_name(nominee1_guard_name);
                nse.setNominee1_guard_pan(nominee1_guard_pan);
                nse.setNominee2_type(nominee2_type);
                nse.setNominee2_guard_name(nominee2_guard_name);
                nse.setNominee2_guard_pan(nominee2_guard_pan);
                nse.setNominee3_type(nominee3_type);
                nse.setNominee3_guard_name(nominee3_guard_name);
                nse.setNominee3_guard_pan(nominee3_guard_pan);
                nse.setNumber_of_nominee(number_of_nominee);

                nse.setNominee1_name(nominee1_name);
                nse.setNominee1_dob(nominee1_dob);
                nse.setNominee1_address1(nominee1_address1);
                nse.setNominee1_address2(nominee1_address2);
                nse.setNominee1_address3(nominee1_address3);
                nse.setNominee1_pincode(nominee1_pincode);
                nse.setNominee1_city(nominee1_city);
                nse.setNominee1_state(nominee1_state);
                nse.setNominee1_state_code(nominee1_state_code);
                nse.setNominee1_country(nominee1_country);
                nse.setNominee1_email(nominee1_email);
                nse.setNominee1_mobile(nominee1_mobile);
                nse.setNominee1_id_no(nominee1_id_no);
                nse.setNominee1_id_type(nominee1_id_type);
                nse.setNominee1_relation(nominee1_relation);
                nse.setNominee1_percentage(nominee1_percentage);

                nse.setNominee2_name(nominee2_name);
                nse.setNominee2_dob(nominee2_dob);
                nse.setNominee2_percentage(nominee2_percentage);
                nse.setNominee2_relation(nominee2_relation);
                nse.setNominee2_address1(nominee2_address1);
                //nse.setNominee2_address2(nominee2_address2);
                //nse.setNominee2_address3(nominee2_address3);
                nse.setNominee2_pincode(nominee2_pincode);
                nse.setNominee2_city(nominee2_city);
                nse.setNominee2_state(nominee2_state);
                nse.setNominee2_state_code(nominee2_state_code);
                nse.setNominee2_country(nominee2_country);
                nse.setNominee2_email(nominee2_email);
                nse.setNominee2_mobile(nominee2_mobile);
                nse.setNominee2_id_no(nominee2_id_no);
                nse.setNominee2_id_type(nominee2_id_type);
                nse.setNominee3_name(nominee3_name);
                nse.setNominee3_dob(nominee3_dob);
                nse.setNominee3_percentage(nominee3_percentage);
                nse.setNominee3_relation(nominee3_relation);
                nse.setNominee3_address1(nominee3_address1);
                //nse.setNominee3_address2(nominee3_address2);
                //nse.setNominee3_address3(nominee3_address3);
                nse.setNominee3_pincode(nominee3_pincode);
                nse.setNominee3_city(nominee3_city);
                nse.setNominee3_state(nominee3_state);
                nse.setNominee3_state_code(nominee3_state_code);
                nse.setNominee3_country(nominee3_country);
                nse.setNominee3_email(nominee3_email);
                nse.setNominee3_mobile(nominee3_mobile);
                nse.setNominee3_id_no(nominee3_id_no);
                nse.setNominee3_id_type(nominee3_id_type);

                nse.setNetworth_amount(networth_amount);
                nse.setNetworth_dob(networth_dob);

                nse.setNominee1_guard_dob(nominee1_guard_dob);
                nse.setNominee2_guard_dob(nominee2_guard_dob);
                nse.setNominee3_guard_dob(nominee3_guard_dob);

                nse.setBroker_code(arn_number);
                nse.setEuin(euin);

                nse.setNominee1_guard_relationship(nominee1_guard_relationship);
                nse.setNominee2_guard_relationship(nominee2_guard_relationship);
                nse.setNominee3_guard_relationship(nominee3_guard_relationship);

                nse.setPan(pan);
                nse.setGender(gender);
                System.out.println("userServiceClient.saveUser::gender: " + nse.getGender());
                userServiceClient.updateUser(nse,token);

                return NseUtils.commonResponse(String.valueOf(user.getId()), HttpStatus.OK);
            }
            else
            {
                return NseUtils.commonResponse("User details not available. Please try again!", HttpStatus.BAD_REQUEST);
            }

        }
        catch(Exception ex)
        {
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }



    @Operation(
            summary = "Modify with NSE IIN Details",
            description = "Registers a user with NSE by collecting and transforming all mandatory user details, including nominee information, FATCA compliance, occupation details, holding nature, and joint holder relationships. The service fetches user data based on the 'multiple_reg' flag, enriches it with state codes from nominee pincodes, assigns default relation codes, and ensures FATCA fields are correctly populated before forwarding the data to NSE."
    )

    @ApiResponses(value =
            {
                    @ApiResponse(responseCode = "200", description = "Success Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SuccessResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Failure Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommonResponse.class))),
            })

    @GetMapping("/modifyNseIINDetails")
    public ResponseEntity<?> modifyNseIINDetails(
            HttpServletRequest request,
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String iin_number,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String multiple_reg)
    {

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        String userid = "";
        String client_name = "";
            try{
                userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);

                UserDto users = userServiceClient.getUserById(Integer.valueOf(userid), token);
                client_name = users.getClient_name();
                String usertypeid = String.valueOf(users.getType_id());
                String login_userid = userid;
                String login_pan = users.getPan();
                String login_name = users.getFirst_name();

                if(StringHelper.isEmpty(userid))
                {
                    return NseUtils.commonResponse("User details not available. Please try again!", HttpStatus.BAD_REQUEST);
                }

                Integer user_id = Integer.parseInt(userid);
                String broker_code = "";
                String name = "";
                String pan = "";
                String branch = "";
                String rm_name = "";
                String subbroker_name = "";
                String client_taxstatus = "";
                String client_acctype1 = "";
                String client_accno1 = "";
                String client_ifsccode1 = "";
                String client_micrcode1 = "";
                String client_bank_name = "";
                String client_branch_name = "";
                String client_branch_add1 = "";
                String client_city = "";
                String client_state_code = "";
                String client_pincode = "";
                String client_country = "";
                String client_place_of_birth = "";
                String client_country_birth_code = "";
                String client_email = "";
                String client_gender = "";
                String client_father_name = "";
                String client_occupation_code = "";
                String client_occupation_type = "";
                String client_source_of_wealth_code = "";
                String client_occupation = "";
                String client_address = "";
                String client_address1 = "";
                String client_address2 = "";
                String client_mobile_number = "";
                String client_dob = "";
                String client_date_of_birth = "";
                String applicable_income_code = "";
                String client_pep_code = "";
                String client_father_husband_guardian = "";
                String client_guardian_pan = "";
                String joint_holder_name = "";
                String joint_holder_dob = "";
                String joint_holder_email  = "";
                String joint_holder_email_relation  = "";
                String joint_holder_mobile  = "";
                String joint_holder_mobile_relation  = "";
                String joint_holder_pan = "";
                String joint_holder_name1 = "";
                String joint_holder_dob1 = "";
                String joint_holder_email1  = "";
                String joint_holder_email_relation1  = "";
                String joint_holder_mobile1  = "";
                String joint_holder_mobile_relation1  = "";
                String joint_holder_pan1 = "";
                String joint_dob ="";
                String joint_dob1 ="";
                String client_guardian_dob ="";
                String guard_date_of_birth ="";
                String holding_nature = "";
                String address_type = "";
                String networth_dob = "";
                String networth_amount = "";
                String nominee1_dob = "";
                String nominee2_dob = "";
                String nominee3_dob = "";
                String networth_date = "";
                String number_of_nominee = "";
                String nominee_soa = "";
                String nominee1_type = "";
                String nominee1_name = 	"";
                String nominee1_date_of_birth = "";
                String nominee1_address1 =  "";
                String nominee1_address2 = "";
                String nominee1_address3 = "";
                String nominee1_pincode = "";
                String nominee1_city = "";
                String nominee1_state = "";
                String nominee1_relation = "";
                String nominee1_percentage = "";
                String nominee1_id_type = "";
                String nominee1_id_no = "";
                String nominee1_email = "";
                String nominee1_mobile = "";
                String nominee1_country = "";
                String nominee1_guard_name = "";
                String nominee1_guard_pan = "";
                String nominee2_type = "";
                String nominee2_name = 	"";
                String nominee2_date_of_birth = "";
                String nominee2_relation = "";
                String nominee2_percentage = "";
                String nominee2_id_type = "";
                String nominee2_id_no = "";
                String nominee2_email = "";
                String nominee2_mobile = "";
                String nominee2_address1 = "";
                String nominee2_address2 = "";
                String nominee2_address3 = "";
                String nominee2_city = "";
                String nominee2_state = "";
                String nominee2_pincode = "";
                String nominee2_country = "";
                String nominee2_guard_name = "";
                String nominee2_guard_pan = "";
                String nominee3_type = "";
                String nominee3_name = 	"";
                String nominee3_date_of_birth = "";
                String nominee3_relation = "";
                String nominee3_percentage = "";
                String nominee3_id_type = "";
                String nominee3_id_no = "";
                String nominee3_email = "";
                String nominee3_mobile = "";
                String nominee3_address1 = "";
                String nominee3_address2 = "";
                String nominee3_address3 = "";
                String nominee3_city = "";
                String nominee3_state = "";
                String nominee3_pincode = "";
                String nominee3_country = "";
                String nominee3_guard_name = "";
                String nominee3_guard_pan = "";
                String nri_address1 = "";
                String nri_address2 = "";
                String nri_address3 = "";
                String nri_city = "";
                String nri_state = "";
                String nri_pincode = "";
                String nri_country = "";
                String mobile_relation = "";
                String email_relation = "";
                String tax_statuscode = "";
                String client_guardian_relation = "";
                String client_guardian_account_relation = "";
                String nominee1_guard_relationship = "";
                String nominee2_guard_relationship = "";
                String nominee3_guard_relationship = "";

                String nomination_opt = "";
                String nomination_authentication = "";

                String nominee1_pan = "";
                String nominee2_pan = "";
                String nominee3_pan = "";

                String primary_holder_kyc_type = "";
                String primary_holder_ckyc_number = "";
                String second_holder_kyc_type = "";
                String second_holder_ckyc_number = "";
                String third_holder_kyc_type = "";
                String third_holder_ckyc_number = "";
                String guardian_kyc_type = "";
                String guardian_ckyc_number = "";


                SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
                //SimpleDateFormat df1 = new SimpleDateFormat("dd-MMM-yyyy");
                SimpleDateFormat df1 = new SimpleDateFormat("dd/MM/yyyy");
                UserDto user = null;
                UserBseNseDto userBseNse = null;
                String nse_iin_number = "";

                try {
                    userBseNse = userServiceClient.getUserBseNseDetailsByIinNumber(
                            client_name, iin_number, token
                    );
                } catch (feign.FeignException.BadRequest e)
                {
                    System.out.println("No BSE/NSE mapping found for IIN: " + iin_number);
                } catch (feign.FeignException e)
                {
                    throw e;
                }
                if(userBseNse != null)
                {
                    nse_iin_number = userBseNse.getNse_iin_number();
                }

                if(userBseNse != null && nse_iin_number.equalsIgnoreCase(iin_number))
                {
                    iin_number = userBseNse.getNse_iin_number();
                    broker_code = userBseNse.getBroker_code();
                    user_id = userBseNse.getUser_id();
                    name = userBseNse.getName();
                    pan = userBseNse.getPan();
                    client_gender = userBseNse.getGender();
                    client_taxstatus = userBseNse.getTax_status_code().trim();
                    client_acctype1 = userBseNse.getBank_account_type1().trim();
                    client_accno1 = userBseNse.getBank_account_number1().trim();
                    client_ifsccode1 = userBseNse.getBank_ifsc_code1().trim();
                    client_micrcode1 = userBseNse.getBank_micr_code1();
                    client_bank_name = userBseNse.getBank_name1().trim();
                    client_branch_name = userBseNse.getBank_branch1().trim();
                    client_branch_add1 = userBseNse.getBank_address1().trim();
                    client_city = userBseNse.getCity().trim();
                    client_state_code = userBseNse.getState_code().trim();
                    client_pincode = userBseNse.getPincode().trim();
                    client_country = userBseNse.getCountry().trim();
                    client_place_of_birth = userBseNse.getPlace_of_birth().trim();
                    client_country_birth_code = userBseNse.getCountry_birth_code().trim();
                    client_email = userBseNse.getEmail().trim();
                    client_father_name = userBseNse.getFather_name().trim();
                    client_occupation_code = userBseNse.getOccupation_code().trim();
                    client_occupation_type = userBseNse.getOccupation();
                    client_source_of_wealth_code = userBseNse.getSource_of_wealth_code().trim();
                    client_occupation = userBseNse.getOccupation().trim();
                    client_address = userBseNse.getStreet_1().trim();
                    client_address1 = userBseNse.getStreet_2().trim();
                    client_address2 = userBseNse.getStreet_3().trim();
                    client_mobile_number = userBseNse.getMobile().trim();
                    client_dob = userBseNse.getDate_of_birth();
                    applicable_income_code = userBseNse.getAnnual_income_code().trim();
                    client_pep_code = userBseNse.getPolitical_code().trim();
                    holding_nature = userBseNse.getHolding_nature_code().trim();
                    address_type = userBseNse.getAddress_type_code().trim();
                    networth_dob = userBseNse.getNetworth_dob().trim();
                    networth_amount = userBseNse.getNetworth_amount().trim();
                    tax_statuscode = userBseNse.getTax_status_code().trim();
                    System.out.println("client_dob= " + client_dob);
                    if(client_dob != null && !client_dob.isEmpty())
                    {
                        Date dob = df.parse(client_dob);
                        System.out.println("dob= " + dob);
                        client_date_of_birth = df.format(dob);
                        System.out.println("client_date_of_birth= " + client_date_of_birth);
                    }

                    if(StringHelper.isNotEmpty(networth_dob))
                    {
                        Date networth_d = df.parse(networth_dob);
                        networth_date = df1.format(networth_d);
                    }

                    number_of_nominee = userBseNse.getNumber_of_nominee().trim();
                    if(StringHelper.isEmpty(number_of_nominee))
                    {
                        number_of_nominee = "0";
                    }
                    nominee_soa = userBseNse.getNominee_soa().trim();
                    nominee1_type = userBseNse.getNominee1_type().trim();
                    nominee1_name = 	userBseNse.getNominee1_name().trim();
                    nominee1_date_of_birth = userBseNse.getNominee1_dob().trim();
                    nominee1_address1 =  userBseNse.getNominee1_address1().trim();
                    nominee1_address2 =userBseNse.getNominee1_address2().trim();
                    nominee1_address3 = userBseNse.getNominee1_address3().trim();
                    nominee1_pincode = userBseNse.getNominee1_pincode().trim();
                    nominee1_city = userBseNse.getNominee1_city().trim();
                    nominee1_state = userBseNse.getNominee1_state().trim();
                    nominee1_country = userBseNse.getNominee1_country().trim();
                    nominee1_id_type = userBseNse.getNominee1_id_type().trim();
                    nominee1_id_no = userBseNse.getNominee1_id_no().trim();
                    nominee1_email = userBseNse.getNominee1_email().trim();
                    nominee1_mobile = userBseNse.getNominee1_mobile().trim();
                    nominee1_relation = userBseNse.getNominee1_relation().trim();
                    nominee1_percentage = userBseNse.getNominee1_percentage().trim();
                    nominee1_guard_name = userBseNse.getNominee1_guard_name().trim();
                    nominee1_guard_pan = userBseNse.getNominee1_guard_pan().trim();
                    nominee1_guard_relationship = userBseNse.getNominee1_guard_relationship().trim();
                    nominee1_pan = userBseNse.getNominee1_pan();

                    if(nominee1_percentage.contains("%") || nominee1_percentage.equalsIgnoreCase("percentage"))
                    {
                        nominee1_percentage = nominee1_percentage.replaceAll("%", "");
                        nominee1_percentage = nominee1_percentage.replaceAll("percentage", "");
                    }

                    nominee2_type = safeTrim(userBseNse.getNominee2_type());
                    nominee2_name = safeTrim(userBseNse.getNominee2_name());
                    nominee2_date_of_birth = safeTrim(userBseNse.getNominee2_dob());
                    nominee2_relation = safeTrim(userBseNse.getNominee2_relation());
                    nominee2_percentage = safeTrim(userBseNse.getNominee2_percentage());

                    nominee2_address1 = safeTrim(userBseNse.getNominee2_address1());
                    nominee2_address2 = safeTrim(userBseNse.getNominee2_address2());
                    nominee2_address3 = safeTrim(userBseNse.getNominee2_address3());
                    nominee2_pincode  = safeTrim(userBseNse.getNominee2_pincode());
                    nominee2_city     = safeTrim(userBseNse.getNominee2_city());
                    nominee2_state    = safeTrim(userBseNse.getNominee2_state());
                    nominee2_country  = safeTrim(userBseNse.getNominee2_country());
                    nominee2_id_type  = safeTrim(userBseNse.getNominee2_id_type());
                    nominee2_id_no    = safeTrim(userBseNse.getNominee2_id_no());
                    nominee2_email    = safeTrim(userBseNse.getNominee2_email());
                    nominee2_mobile   = safeTrim(userBseNse.getNominee2_mobile());

                    nominee2_guard_name = safeTrim(userBseNse.getNominee2_guard_name());
                    nominee2_guard_pan  = safeTrim(userBseNse.getNominee2_guard_pan());
                    nominee2_guard_relationship = safeTrim(userBseNse.getNominee2_guard_relationship());
                    nominee2_pan = safeTrim(userBseNse.getNominee2_pan());


                    if(nominee2_percentage.contains("%") || nominee2_percentage.equalsIgnoreCase("percentage"))
                    {
                        nominee2_percentage = nominee2_percentage.replaceAll("%", "");
                        nominee2_percentage = nominee2_percentage.replaceAll("percentage", "");
                    }

                    nominee3_type = safeTrim(userBseNse.getNominee3_type());
                    nominee3_name = safeTrim(userBseNse.getNominee3_name());
                    nominee3_date_of_birth = safeTrim(userBseNse.getNominee3_dob());
                    nominee3_relation = safeTrim(userBseNse.getNominee3_relation());

                    nominee3_address1 =  safeTrim(userBseNse.getNominee3_address1());
                    nominee3_address2 = safeTrim(userBseNse.getNominee3_address2());
                    nominee3_address3 = safeTrim(userBseNse.getNominee3_address3());
                    nominee3_pincode = safeTrim(userBseNse.getNominee3_pincode());
                    nominee3_city = safeTrim(userBseNse.getNominee3_city());
                    nominee3_state = safeTrim(userBseNse.getNominee3_state());
                    nominee3_country = safeTrim(userBseNse.getNominee3_country());
                    nominee3_id_type = safeTrim(userBseNse.getNominee3_id_type());
                    nominee3_id_no = safeTrim(userBseNse.getNominee3_id_no());
                    nominee3_email = safeTrim(userBseNse.getNominee3_email());
                    nominee3_mobile = safeTrim(userBseNse.getNominee3_mobile());

                    nominee3_percentage = safeTrim(userBseNse.getNominee3_percentage());
                    nominee3_guard_name = safeTrim(userBseNse.getNominee3_guard_name());
                    nominee3_guard_pan = safeTrim(userBseNse.getNominee3_guard_pan());
                    nominee3_guard_relationship = safeTrim(userBseNse.getNominee3_guard_relationship());
                    nominee3_pan = safeTrim(userBseNse.getNominee3_pan());

                    if(nominee3_percentage.contains("%") || nominee3_percentage.equalsIgnoreCase("percentage"))
                    {
                        nominee3_percentage = nominee3_percentage.replaceAll("%", "");
                        nominee3_percentage = nominee3_percentage.replaceAll("percentage", "");
                    }

                    if(StringHelper.isNotEmpty(nominee1_date_of_birth))
                    {
                        Date nominee1_dob1 = df.parse(nominee1_date_of_birth);
                        nominee1_dob = df1.format(nominee1_dob1);
                    }
                    if(StringHelper.isNotEmpty(nominee2_date_of_birth))
                    {
                        Date nominee2_dob1 = df.parse(nominee2_date_of_birth);
                        nominee2_dob = df1.format(nominee2_dob1);
                    }
                    if(StringHelper.isNotEmpty(nominee3_date_of_birth))
                    {
                        Date nominee3_dob1 = df.parse(nominee3_date_of_birth);
                        nominee3_dob = df1.format(nominee3_dob1);
                    }

                    if(userBseNse.getTax_status_code().equalsIgnoreCase("02") || userBseNse.getTax_status_code().equalsIgnoreCase("26") || userBseNse.getTax_status_code().equalsIgnoreCase("28"))
                    {
                        client_father_husband_guardian = userBseNse.getGuard_name().trim();
                        client_guardian_pan = userBseNse.getGuard_pan().trim();
                        client_guardian_dob = userBseNse.getGuard_dob().trim();
                        client_guardian_relation = userBseNse.getGuard_relationship();
                        client_guardian_account_relation = userBseNse.getGuard_account_relation();

                        Date guard_dob = df.parse(client_guardian_dob);
                        guard_date_of_birth = df1.format(guard_dob);
                    }
                    if(userBseNse.getTax_status_code().equalsIgnoreCase("11") || userBseNse.getTax_status_code().equalsIgnoreCase("21") || userBseNse.getTax_status_code().equalsIgnoreCase("26") || userBseNse.getTax_status_code().equalsIgnoreCase("28") || userBseNse.getTax_status_code().equalsIgnoreCase("61") || userBseNse.getTax_status_code().equalsIgnoreCase("62"))
                    {
                        if(userBseNse != null)
                        {
                            nri_address1 = userBseNse.getNri_address1();
                            nri_address2 = userBseNse.getNri_address2();
                            nri_address3 = userBseNse.getNri_address3();
                            nri_city = userBseNse.getNri_city();
                            nri_state = userBseNse.getNri_state();
                            nri_pincode = userBseNse.getNri_pincode();
                            nri_country = userBseNse.getNri_country();

                            String countryCode = NseUtils.getCountrycode(userBseNse.getNri_country());
                            nri_country = countryCode;
                        }else{
                            nri_address1 = user.getNri_address1();
                            nri_address2 = user.getNri_address2();
                            nri_address3 = user.getNri_address3();
                            nri_city = user.getNri_city();
                            nri_state = user.getNri_state();
                            nri_pincode = user.getNri_pincode();
                            nri_country = user.getNri_country();

                            String countryCode = NseUtils.getCountrycode(user.getNri_country());
                            nri_country = countryCode;
                        }

                    }
                    if(userBseNse.getHolding_nature_code().equalsIgnoreCase("JO") || userBseNse.getHolding_nature_code().equalsIgnoreCase("ES") || userBseNse.getHolding_nature_code().equalsIgnoreCase("AS"))
                    {
                        joint_holder_name = userBseNse.getJoint_holder_name1().trim();
                        joint_holder_dob = userBseNse.getJoint_holder_dob1().trim();
                        joint_holder_email = userBseNse.getJoint_holder_email1().trim();
                        joint_holder_email_relation = userBseNse.getJoint_holder_email_relation1().trim();
                        joint_holder_mobile = userBseNse.getJoint_holder_mobile1().trim();
                        joint_holder_mobile_relation = userBseNse.getJoint_holder_mobile_relation1().trim();
                        joint_holder_pan = userBseNse.getJoint_holder_pan1().trim();
                        joint_holder_name1 = userBseNse.getJoint_holder_name2().trim();
                        joint_holder_dob1 = userBseNse.getJoint_holder_dob2().trim();
                        joint_holder_email1 = userBseNse.getJoint_holder_email2().trim();
                        joint_holder_email_relation1 = userBseNse.getJoint_holder_email_relation2().trim();
                        joint_holder_mobile1 = userBseNse.getJoint_holder_mobile2().trim();
                        joint_holder_mobile_relation1 = userBseNse.getJoint_holder_mobile_relation2().trim();
                        joint_holder_pan1 = userBseNse.getJoint_holder_pan2().trim();

                        if(joint_holder_dob.isEmpty() || joint_holder_dob == null || joint_holder_dob.equalsIgnoreCase("undefined"))
                        {

                        }else
                        {
                            Date jointdob = df.parse(joint_holder_dob);
                            joint_dob = df1.format(jointdob);
                        }
                        if(joint_holder_dob1.isEmpty() || joint_holder_dob1 == null || joint_holder_dob1.equalsIgnoreCase("undefined"))
                        {

                        }else
                        {
                            Date jointdob1 = df.parse(joint_holder_dob1);
                            joint_dob1 = df1.format(jointdob1);
                        }
                    }
                    mobile_relation = userBseNse.getMobile_relation();
                    email_relation = userBseNse.getEmail_relation();

                }else
                {

                    user = userServiceClient.getUserDetailsByID(client_name, user_id,token);

                    if(user == null)
                    {
                        return NseUtils.commonResponse("User details not available. Please try again!", HttpStatus.BAD_REQUEST);
                    }

                    iin_number = user.getNse_iin_number();
                    broker_code = user.getBroker_code();
                    name = user.getName();
                    pan = user.getPan();
                    branch = user.getBranch();
                    rm_name = user.getRm_name();
                    subbroker_name = user.getSubbroker_name();
                    client_gender = user.getGender();
                    client_taxstatus = user.getTax_status_code().trim(); //01/02/03/04/05/06/07/08/10/11/12/21/23/24/47
                    client_acctype1 = user.getBank_account_type1().trim(); //SB/CB/NE/NO
                    client_accno1 = user.getBank_account_number1().trim();
                    client_ifsccode1 = user.getBank_ifsc_code1().trim();
                    client_micrcode1 = user.getBank_micr_code1();
                    client_bank_name = user.getBank_name1().trim();
                    client_branch_name = user.getBank_branch1().trim();
                    client_branch_add1 = user.getBank_address1().trim();
                    client_city = user.getCity().trim();
                    client_state_code = user.getState_code().trim();
                    client_pincode = user.getPincode().trim();
                    client_country = user.getCountry().trim();
                    client_place_of_birth = user.getPlace_of_birth().trim();
                    client_country_birth_code = user.getCountry_birth_code().trim();
                    client_email = user.getEmail().trim();
                    client_father_name = user.getFather_name().trim();
                    client_occupation_code = user.getOccupation_code().trim();
                    client_occupation_type = user.getOccupation();
                    client_source_of_wealth_code = user.getSource_of_wealth_code().trim();
                    client_occupation = user.getOccupation().trim();
                    client_address = user.getStreet_1().trim();
                    client_address1 = user.getStreet_2().trim();
                    client_address2 = user.getStreet_3().trim();
                    client_mobile_number = user.getMobile().trim();
                    client_dob = user.getDate_of_birth();
                    applicable_income_code = user.getAnnual_income_code().trim();
                    client_pep_code = user.getPolitical_code().trim();
                    holding_nature = user.getHolding_nature_code().trim();
                    address_type = user.getAddress_type_code().trim();
                    networth_dob = user.getNetworth_dob().trim();
                    networth_amount = user.getNetworth_amount().trim();

                    System.out.println("client_dob= " + client_dob);

                    System.out.println("client_dob= " + client_dob);

                    try {
                        client_date_of_birth = NseUtils.normalizeDob(client_dob);
                        System.out.println("client_date_of_birth= " + client_date_of_birth);
                    } catch (ParseException e) {
                        System.err.println("Invalid DOB received: " + client_dob);
                        throw e;
                    }


                    if(StringHelper.isNotEmpty(networth_dob))
                    {
                        Date networth_d = df.parse(networth_dob);
                        networth_date = df1.format(networth_d);
                    }

                    number_of_nominee = user.getNumber_of_nominee().trim();
                    nominee_soa = user.getNominee_soa().trim();
                    if(StringHelper.isEmpty(number_of_nominee))
                    {
                        number_of_nominee = "0";
                    }
                    nominee1_type = user.getNominee1_type().trim();
                    nominee1_name = user.getNominee1_name().trim();
                    nominee1_date_of_birth = user.getNominee1_dob().trim();
                    nominee1_address1 =  user.getNominee1_address1().trim();
                    nominee1_address2 =user.getNominee1_address2().trim();
                    nominee1_address3 = user.getNominee1_address3().trim();
                    nominee1_pincode = user.getNominee1_pincode().trim();
                    nominee1_city = user.getNominee1_city().trim();
                    nominee1_state = user.getNominee1_state().trim();
                    nominee1_country = user.getNominee1_country().trim();
                    nominee1_id_type = user.getNominee1_id_type().trim();
                    nominee1_id_no = user.getNominee1_id_no().trim();
                    nominee1_email = user.getNominee1_email().trim();
                    nominee1_mobile = user.getNominee1_mobile().trim();
                    nominee1_relation = user.getNominee1_relation().trim();
                    nominee1_percentage = user.getNominee1_percentage().trim();
                    nominee1_guard_name = user.getNominee1_guard_name().trim();
                    nominee1_guard_pan = user.getNominee1_guard_pan().trim();
                    nominee1_guard_relationship = user.getNominee1_guard_relationship().trim();
                    nominee1_pan = user.getNominee1_pan();

                    if(nominee1_percentage.contains("%") || nominee1_percentage.equalsIgnoreCase("percentage"))
                    {
                        nominee1_percentage = nominee1_percentage.replaceAll("%", "");
                        nominee1_percentage = nominee1_percentage.replaceAll("percentage", "");
                    }

                    nominee2_type = user.getNominee2_type().trim();
                    nominee2_name = user.getNominee2_name().trim();
                    nominee2_date_of_birth = user.getNominee2_dob().trim();
                    nominee2_relation = user.getNominee2_relation().trim();
                    nominee2_percentage = user.getNominee2_percentage().trim();
                    nominee2_address1 =  user.getNominee2_address1().trim();
                    //nominee2_address2 =user.getNominee2_address2().trim();
                    //nominee2_address3 = user.getNominee2_address3().trim();
                    nominee2_pincode = user.getNominee2_pincode().trim();
                    nominee2_city = user.getNominee2_city().trim();
                    nominee2_state = user.getNominee2_state().trim();
                    nominee2_country = user.getNominee2_country().trim();
                    nominee2_id_type = user.getNominee2_id_type().trim();
                    nominee2_id_no = user.getNominee2_id_no().trim();
                    nominee2_email = user.getNominee2_email().trim();
                    nominee2_mobile = user.getNominee2_mobile().trim();
                    nominee2_guard_name = user.getNominee2_guard_name().trim();
                    nominee2_guard_pan = user.getNominee2_guard_pan();
                    nominee2_guard_relationship = user.getNominee2_guard_relationship().trim();
                    nominee2_pan = user.getNominee2_pan();

                    if(nominee2_percentage.contains("%") || nominee2_percentage.equalsIgnoreCase("percentage"))
                    {
                        nominee2_percentage = nominee2_percentage.replaceAll("%", "");
                        nominee2_percentage = nominee2_percentage.replaceAll("percentage", "");
                    }

                    nominee3_type = user.getNominee3_type().trim();
                    nominee3_name = 	user.getNominee3_name().trim();
                    nominee3_date_of_birth = user.getNominee3_dob().trim();
                    nominee3_relation = user.getNominee3_relation().trim();
                    nominee3_percentage = user.getNominee3_percentage().trim();
                    nominee3_address1 =  user.getNominee3_address1().trim();
                    nominee3_pincode = user.getNominee3_pincode().trim();
                    nominee3_city = user.getNominee3_city().trim();
                    nominee3_state = user.getNominee3_state().trim();
                    nominee3_country = user.getNominee3_country().trim();
                    nominee3_id_type = user.getNominee3_id_type().trim();
                    nominee3_id_no = user.getNominee3_id_no().trim();
                    nominee3_email = user.getNominee3_email().trim();
                    nominee3_mobile = user.getNominee3_mobile().trim();
                    nominee3_guard_name = user.getNominee3_guard_name().trim();
                    nominee3_guard_pan = user.getNominee3_guard_pan().trim();
                    nominee3_guard_relationship = user.getNominee3_guard_relationship().trim();
                    nominee3_pan = user.getNominee3_pan();

                    if(nominee3_percentage.contains("%") || nominee3_percentage.equalsIgnoreCase("percentage"))
                    {
                        nominee3_percentage = nominee3_percentage.replaceAll("%", "");
                        nominee3_percentage = nominee3_percentage.replaceAll("percentage", "");
                    }

                    if(StringHelper.isNotEmpty(nominee1_date_of_birth))
                    {
                        nominee1_dob = NseUtils.normalizeDateToDdMmYyyy(nominee1_date_of_birth);
                        System.out.println("nominee1_dob= " + nominee1_dob);
                    }

                    if(StringHelper.isNotEmpty(nominee2_date_of_birth))
                    {
//                        Date nominee2_dob1 = df.parse(nominee2_date_of_birth);
                        nominee2_dob = NseUtils.normalizeDateToDdMmYyyy(nominee2_date_of_birth);
//                        nominee2_dob = df1.format(nominee2_dob1);
                    }
                    if(StringHelper.isNotEmpty(nominee3_date_of_birth))
                    {
//                        Date nominee3_dob1 = df.parse(nominee3_date_of_birth);
                        nominee3_dob = NseUtils.normalizeDateToDdMmYyyy(nominee3_date_of_birth);
//                        nominee3_dob = df1.format(nominee3_dob1);
                    }
                    if(user.getTax_status_code().equalsIgnoreCase("02") || user.getTax_status_code().equalsIgnoreCase("26") || user.getTax_status_code().equalsIgnoreCase("28"))
                    {
                        client_father_husband_guardian = user.getGuard_name().trim();
                        client_guardian_pan = user.getGuard_pan().trim();
                        client_guardian_dob = user.getGuard_dob().trim();
                        client_guardian_relation = user.getGuard_relationship();
                        client_guardian_account_relation = user.getGuard_account_relation();

                        Date guard_dob = df.parse(client_guardian_dob);
                        guard_date_of_birth = df1.format(guard_dob);
                    }

                    if(user.getTax_status_code().equalsIgnoreCase("11") || user.getTax_status_code().equalsIgnoreCase("21") || user.getTax_status_code().equalsIgnoreCase("26") || user.getTax_status_code().equalsIgnoreCase("28") || user.getTax_status_code().equalsIgnoreCase("61") || user.getTax_status_code().equalsIgnoreCase("62"))
                    {
                        nri_address1 = user.getNri_address1();
                        nri_address2 = user.getNri_address2();
                        nri_address3 = user.getNri_address3();
                        nri_city = user.getNri_city();
                        nri_state = user.getNri_state();
                        nri_pincode = user.getNri_pincode();
                        nri_country = user.getNri_country();

                        String countryCode = NseUtils.getCountrycode(user.getNri_country());
                        nri_country = countryCode;
                    }
                    if(user.getHolding_nature_code().equalsIgnoreCase("JO") || user.getHolding_nature_code().equalsIgnoreCase("ES") || user.getHolding_nature_code().equalsIgnoreCase("AS"))
                    {
                        joint_holder_name = user.getJoint_holder_name1().trim();
                        joint_holder_dob = user.getJoint_holder_dob1().trim();
                        joint_holder_email = user.getJoint_holder_email1().trim();
                        joint_holder_email_relation = user.getJoint_holder_email_relation1().trim();
                        joint_holder_mobile = user.getJoint_holder_mobile1().trim();
                        joint_holder_mobile_relation = user.getJoint_holder_mobile_relation1().trim();
                        joint_holder_pan = user.getJoint_holder_pan1().trim();
                        joint_holder_name1 = user.getJoint_holder_name2().trim();
                        joint_holder_dob1 = user.getJoint_holder_dob2().trim();
                        joint_holder_email1 = user.getJoint_holder_email2().trim();
                        joint_holder_email_relation1 = user.getJoint_holder_email_relation2().trim();
                        joint_holder_mobile1 = user.getJoint_holder_mobile2().trim();
                        joint_holder_mobile_relation1 = user.getJoint_holder_mobile_relation2().trim();
                        joint_holder_pan1 = user.getJoint_holder_pan2().trim();

                        if(joint_holder_dob.isEmpty() || joint_holder_dob == null || joint_holder_dob.equalsIgnoreCase("undefined"))
                        {

                        }else
                        {
                            Date jointdob = df.parse(joint_holder_dob);
                            joint_dob = df1.format(jointdob);
                        }
                        if(joint_holder_dob1.isEmpty() || joint_holder_dob1 == null || joint_holder_dob1.equalsIgnoreCase("undefined"))
                        {

                        }else
                        {
                            Date jointdob1 = df.parse(joint_holder_dob1);
                            joint_dob1 = df1.format(jointdob1);
                        }
                    }
                    mobile_relation = user.getMobile_relation();
                    email_relation = user.getEmail_relation();

                }

                if(user == null)
                {
                    user = userServiceClient.getUserDetailsByID(client_name, user_id,token);
                    branch = user.getBranch();
                    rm_name = user.getRm_name();
                    subbroker_name = user.getSubbroker_name();
                }

                if(!nominee1_pincode.isEmpty()) 
                {
                    Optional<NsePincode> pin = nsePincodeService.getPincodeDetails(nominee1_pincode);
                    if(pin.isPresent())
                    {
                        nominee1_state = pin.get().getState_code();
                    }
                }
                if(!nominee2_pincode.isEmpty()) {
                    Optional<NsePincode> pin = nsePincodeService.getPincodeDetails(nominee2_pincode);
                    if(pin.isPresent()) {
                        nominee2_state = pin.get().getState_code();
                    }
                }

                if(!nominee3_pincode.isEmpty()) {
                    Optional<NsePincode> pin = nsePincodeService.getPincodeDetails(nominee3_pincode);
                    if(pin.isPresent()) {
                        nominee3_state = pin.get().getState_code();
                    }
                }
                nominee_soa = NseUtils.checkParem(nominee_soa);

                if(!nominee_soa.isEmpty())
                {
                    nominee_soa = nominee_soa;
                }else
                {
                    nominee_soa = "N";
                }

                if(joint_holder_email_relation == null) {joint_holder_email_relation = "";}
                if(joint_holder_email_relation1 == null) {joint_holder_email_relation1 = "";}
                if(joint_holder_mobile_relation == null) {joint_holder_mobile_relation = "";}
                if(joint_holder_mobile_relation1 == null) {joint_holder_mobile_relation1 = "";}

                if(mobile_relation == null || mobile_relation.isEmpty()) {mobile_relation = "";}
                if(email_relation == null || email_relation.isEmpty()) {email_relation = "";}

                mobile_relation = mobile_relation.trim();
                email_relation = email_relation.trim();

                if (client_taxstatus.equalsIgnoreCase("01") || client_taxstatus.equalsIgnoreCase("02") || client_taxstatus.equalsIgnoreCase("11") ||
                        client_taxstatus.equalsIgnoreCase("21") || client_taxstatus.equalsIgnoreCase("61") || client_taxstatus.equalsIgnoreCase("62") || user.getTax_status_code().equalsIgnoreCase("26") || user.getTax_status_code().equalsIgnoreCase("28"))
                {
                    if(mobile_relation.isEmpty()) {mobile_relation = "SE";}
                    if(email_relation.isEmpty()) {email_relation = "SE";}
                }else
                {
                    mobile_relation = "";
                    email_relation = "";
                }

                nomination_authentication= "O";

                if(StringHelper.isNotEmpty(nominee1_name))
                {
                    nomination_opt = "Y";
                }
                else
                {
                    nomination_opt = "N";
                }

                primary_holder_kyc_type = "K";
                primary_holder_ckyc_number ="";
                if(StringHelper.isNotEmpty(joint_holder_name))
                {
                    second_holder_kyc_type="K";
                    second_holder_ckyc_number="";
                }
                if(StringHelper.isNotEmpty(joint_holder_name1))
                {
                    third_holder_kyc_type="K";
                    third_holder_ckyc_number="";
                }
                if(StringHelper.isNotEmpty(client_father_husband_guardian))
                {
                    guardian_kyc_type="K";
                    guardian_ckyc_number="";
                }

                String appln_id = "";
                String password = "";
                String euin = "";
                String host = "";
                String mail_support_name = "";
                String mail_support_email = "";

                BseNseKeyDto nsekey = userServiceClient.getByClientName(client_name,token);
                mail_support_name = nsekey.getMail_support_name();
                mail_support_email = nsekey.getMail_support_email();
                host = nsekey.getDomain_url();

                String broker_code1 = nsekey.getBrokerCode();

                if(broker_code1 == null) {broker_code1 = "";}


                if(broker_code.isEmpty())
                {
                    broker_code = broker_code1;
                }


                    broker_code = broker_code1;
                    appln_id = nsekey.getNse_appln_id();
                    password = nsekey.getNse_password();
                    euin = nsekey.getEuin();
                    euin = euin.split(",")[0];


                JSONArray regDetailsArray = new JSONArray();
                JSONObject regObject = new JSONObject();

                System.out.println("SingleRegistration::client_gender: " + client_gender);
                regObject.put("client_code", iin_number);
                regObject.put("primary_holder_first_name", name);
                regObject.put("primary_holder_middle_name", "");
                regObject.put("primary_holder_last_name", "");
                regObject.put("tax_status", client_taxstatus);
//                regObject.put("gender", client_gender);
                List<String> taxStatusList = Arrays.asList ("03", "04", "06", "07", "08", "13", "10", "47");
                if(taxStatusList.contains(tax_statuscode)){
                    regObject.put("gender", "O");
                }else{
                    regObject.put("gender", StringUtils.defaultString(client_gender));
                }
                System.out.println("client_date_of_birth11 = " + client_date_of_birth);
                regObject.put("primary_holder_dob_incorporation", client_date_of_birth);
                regObject.put("occupation_code", client_occupation_code);
                regObject.put("holding_nature", holding_nature);
                regObject.put("second_holder_first_name", joint_holder_name);
                regObject.put("second_holder_middle_name", "");
                regObject.put("second_holder_last_name", "");
                regObject.put("third_holder_first_name", joint_holder_name1);
                regObject.put("third_holder_middle_name", "");
                regObject.put("third_holder_last_name", "");
                regObject.put("second_holder_dob", joint_holder_dob);
                regObject.put("third_holder_dob", joint_holder_dob1);
                regObject.put("guardian_first_name", client_father_husband_guardian);
                regObject.put("guardian_middle_name", "");
                regObject.put("guardian_last_name", "");
                regObject.put("guardian_dob", client_guardian_dob);
                regObject.put("primary_holder_pan_exempt", "");
                regObject.put("second_holder_pan_exempt", "");
                regObject.put("third_holder_pan_exempt", "");
                regObject.put("guardian_pan_exempt", "");
                regObject.put("primary_holder_pan", pan);
                regObject.put("second_holder_pan", joint_holder_pan);
                regObject.put("third_holder_pan", joint_holder_pan1);
                regObject.put("guardian_pan", client_guardian_pan);
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
                regObject.put("account_type_1", client_acctype1);
                regObject.put("account_no_1", client_accno1);
                regObject.put("micr_no_1", client_micrcode1);
                regObject.put("ifsc_code_1", client_ifsccode1);
                regObject.put("default_bank_flag_1", "Y");
                regObject.put("account_type_2", "");
                regObject.put("account_no_2", "");
                regObject.put("micr_no_2", "");
                regObject.put("ifsc_code_2", "");
                regObject.put("default_bank_flag_2", "");
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
                regObject.put("address_1", client_address);
                regObject.put("address_2", client_address1);
                regObject.put("address_3", client_address2);
                regObject.put("city", client_city);
                regObject.put("state", client_state_code);
                regObject.put("pincode", client_pincode);
                regObject.put("country", client_country);
                regObject.put("resi_phone", "");
                regObject.put("resi_fax", "");
                regObject.put("office_phone", "");
                regObject.put("office_fax", "");
                regObject.put("email", client_email);
                regObject.put("communication_mode", "E");


                if(client_taxstatus.equalsIgnoreCase("24") || client_taxstatus.equalsIgnoreCase("21") || client_taxstatus.equalsIgnoreCase("26") || client_taxstatus.equalsIgnoreCase("28") || client_taxstatus.equalsIgnoreCase("61") || client_taxstatus.equalsIgnoreCase("62")) {
                    if (nri_address1 != null && nri_address1.length() > 40) {
                        nri_address1 = nri_address2.substring(0, 40);
                    }else if (nri_address2 != null && nri_address2.length() > 40) {
                        nri_address2 = nri_address2.substring(0, 40);
                    }else if (nri_address3 != null && nri_address3.length() > 40) {
                        nri_address2 = nri_address2.substring(0, 40);
                    }

                    regObject.put("foreign_address_1", nri_address1);
                    regObject.put("foreign_address_2", nri_address2);
                    regObject.put("foreign_address_3", nri_address3);
                    regObject.put("foreign_address_city", nri_city);
                    regObject.put("foreign_address_pincode", nri_pincode);
                    regObject.put("foreign_address_state", nri_pincode);
                    regObject.put("foreign_address_country", nri_country);
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


                regObject.put("indian_mobile_no", client_mobile_number);
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
                regObject.put("mobile_declaration_flag", mobile_relation);
                regObject.put("email_declaration_flag", email_relation);
                regObject.put("second_holder_email", joint_holder_email);
                regObject.put("second_holder_email_declaration", joint_holder_email_relation);
                regObject.put("second_holder_mobile", joint_holder_mobile);
                regObject.put("second_holder_mobile_declaration", joint_holder_mobile_relation);
                regObject.put("third_holder_email", joint_holder_email1);
                regObject.put("third_holder_email_declaration", joint_holder_email_relation1);
                regObject.put("third_holder_mobile", joint_holder_mobile1);
                regObject.put("third_holder_mobile_declaration", joint_holder_mobile_relation1);
                regObject.put("guardian_relation", client_guardian_relation);
                System.out.println("client_taxstatus = " + client_taxstatus);
                List<String> nominationOptTaxStatusList = Arrays.asList ("02","03", "04", "06", "07", "08", "13", "10", "47", "26", "28");
                if(nominationOptTaxStatusList.contains(client_taxstatus))
                {
                    regObject.put("nomination_opt", "");
                    regObject.put("nomination_authentication", "");
                    regObject.put("nominee_opt_out_ref_no", "");
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
                    regObject.put("nominee_2_address2", "");
                    regObject.put("nominee_2_address3", "");
                    regObject.put("nominee_2_city", "");
                    regObject.put("nominee_2_pin", "");
                    regObject.put("nominee_2_country", "");
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
                    regObject.put("nominee_3_address2", "");
                    regObject.put("nominee_3_address3", "");
                    regObject.put("nominee_3_city", "");
                    regObject.put("nominee_3_pin", "");
                    regObject.put("nominee_3_country", "");
                    regObject.put("nominee_soa", "");
                }else
                {
                    regObject.put("nomination_opt", nomination_opt);
                    if(nomination_opt.equalsIgnoreCase("N"))
                    {
                        int refNo = 1000 + new Random().nextInt(9000);
                        regObject.put("nominee_opt_out_ref_no", String.valueOf(refNo));
                    }else{
                        regObject.put("nominee_opt_out_ref_no", "");
                    }

                    if(nomination_opt.equalsIgnoreCase("N"))
                    {
                        regObject.put("nomination_authentication", "");
                    }else{
                        regObject.put("nomination_authentication", "O");
                    }


                    regObject.put("nominee_1_name", nominee1_name);
                    regObject.put("nominee_1_relationship", nominee1_relation);
                    regObject.put("nominee_1_applicable", nominee1_percentage);
                    regObject.put("nominee_1_minor_flag", nominee1_type);
                    regObject.put("nominee_1_dob", nominee1_date_of_birth);
                    regObject.put("nominee_1_guardian", nominee1_guard_name);
                    regObject.put("nominee_1_guardian_pan", nominee1_guard_pan);
                    regObject.put("nominee_1_identity_type", nominee1_id_type);
                    regObject.put("nominee_1_identity_number", nominee1_id_no);
                    regObject.put("nominee_1_email", nominee1_email);
                    regObject.put("nominee_1_mobile", nominee1_mobile);
                    regObject.put("nominee_1_address1", nominee1_address1);
                    regObject.put("nominee_1_address2", nominee1_address2);
                    regObject.put("nominee_1_address3", nominee1_address3);
                    regObject.put("nominee_1_city", nominee1_city);
                    regObject.put("nominee_1_pin", nominee1_pincode);
                    regObject.put("nominee_1_country", nominee1_country);
                    regObject.put("nominee_2_name", nominee2_name);
                    regObject.put("nominee_2_relationship", nominee2_relation);
                    regObject.put("nominee_2_applicable", nominee2_percentage);
                    regObject.put("nominee_2_dob", nominee2_date_of_birth);
                    regObject.put("nominee_2_minor_flag", nominee2_type);
                    regObject.put("nominee_2_guardian", nominee2_guard_name);
                    regObject.put("nominee_2_guardian_pan", nominee2_guard_pan);
                    regObject.put("nominee_2_identity_type", nominee2_id_type);
                    regObject.put("nominee_2_identity_number", nominee2_id_no);
                    regObject.put("nominee_2_email", nominee2_email);
                    regObject.put("nominee_2_mobile", nominee2_mobile);
                    regObject.put("nominee_2_address1", nominee2_address1);
                    regObject.put("nominee_2_address2", nominee2_address2);
                    regObject.put("nominee_2_address3", nominee2_address3);
                    regObject.put("nominee_2_city", nominee2_city);
                    regObject.put("nominee_2_pin", nominee2_pincode);
                    regObject.put("nominee_2_country", nominee2_country);
                    regObject.put("nominee_3_name", nominee3_name);
                    regObject.put("nominee_3_relationship", nominee3_relation);
                    regObject.put("nominee_3_applicable", nominee3_percentage);
                    regObject.put("nominee_3_dob", nominee3_date_of_birth);
                    regObject.put("nominee_3_minor_flag", nominee3_type);
                    regObject.put("nominee_3_guardian", nominee3_guard_name);
                    regObject.put("nominee_3_guardian_pan", nominee3_guard_pan);
                    regObject.put("nominee_3_identity_type", nominee3_id_type);
                    regObject.put("nominee_3_identity_number", nominee3_id_no);
                    regObject.put("nominee_3_email", nominee3_email);
                    regObject.put("nominee_3_mobile", nominee3_mobile);
                    regObject.put("nominee_3_address1", nominee3_address1);
                    regObject.put("nominee_3_address2", nominee3_address2);
                    regObject.put("nominee_3_address3", nominee3_address3);
                    regObject.put("nominee_3_city", nominee3_city);
                    regObject.put("nominee_3_pin", nominee3_pincode);
                    regObject.put("nominee_3_country", nominee3_country);
                    regObject.put("nominee_soa", nominee_soa);
                }

//                regObject.put("nomination_opt", nomination_opt);
//                regObject.put("nomination_authentication", nomination_authentication);
//                regObject.put("nominee_1_name", nominee1_name);
//                regObject.put("nominee_1_relationship", nominee1_relation);
//                regObject.put("nominee_1_applicable", nominee1_percentage);
//                regObject.put("nominee_1_minor_flag", nominee1_type);
//                regObject.put("nominee_1_dob", nominee1_date_of_birth);
//                regObject.put("nominee_1_guardian", nominee1_guard_name);
//                regObject.put("nominee_1_guardian_pan", nominee1_guard_pan);
//                regObject.put("nominee_1_identity_type", nominee1_id_type);
//                regObject.put("nominee_1_identity_number", nominee1_id_no);
//                regObject.put("nominee_1_email", nominee1_email);
//                regObject.put("nominee_1_mobile", nominee1_mobile);
//                regObject.put("nominee_1_address1", nominee1_address1);
//                regObject.put("nominee_1_address2", nominee1_address2);
//                regObject.put("nominee_1_address3", nominee1_address3);
//                regObject.put("nominee_1_city", nominee1_city);
//                regObject.put("nominee_1_pin", nominee1_pincode);
//                regObject.put("nominee_1_country", nominee1_country);
//                regObject.put("nominee_2_name", nominee2_name);
//                regObject.put("nominee_2_relationship", nominee2_relation);
//                regObject.put("nominee_2_applicable", nominee2_percentage);
//                regObject.put("nominee_2_dob", nominee2_date_of_birth);
//                regObject.put("nominee_2_minor_flag", nominee2_type);
//                regObject.put("nominee_2_guardian", nominee2_guard_name);
//                regObject.put("nominee_2_guardian_pan", nominee2_guard_pan);
//                regObject.put("nominee_2_identity_type", nominee2_id_type);
//                regObject.put("nominee_2_identity_number", nominee2_id_no);
//                regObject.put("nominee_2_email", nominee2_email);
//                regObject.put("nominee_2_mobile", nominee2_mobile);
//                regObject.put("nominee_2_address1", nominee2_address1);
//                regObject.put("nominee_2_address2", nominee2_address2);
//                regObject.put("nominee_2_address3", nominee2_address3);
//                regObject.put("nominee_2_city", nominee2_city);
//                regObject.put("nominee_2_pin", nominee2_pincode);
//                regObject.put("nominee_2_country", nominee2_country);
//                regObject.put("nominee_3_name", nominee3_name);
//                regObject.put("nominee_3_relationship", nominee3_relation);
//                regObject.put("nominee_3_applicable", nominee3_percentage);
//                regObject.put("nominee_3_dob", nominee3_date_of_birth);
//                regObject.put("nominee_3_minor_flag", nominee3_type);
//                regObject.put("nominee_3_guardian", nominee3_guard_name);
//                regObject.put("nominee_3_guardian_pan", nominee3_guard_pan);
//                regObject.put("nominee_3_identity_type", nominee3_id_type);
//                regObject.put("nominee_3_identity_number", nominee3_id_no);
//                regObject.put("nominee_3_email", nominee3_email);
//                regObject.put("nominee_3_mobile", nominee3_mobile);
//                regObject.put("nominee_3_address1", nominee3_address1);
//                regObject.put("nominee_3_address2", nominee3_address2);
//                regObject.put("nominee_3_address3", nominee3_address3);
//                regObject.put("nominee_3_city", nominee3_city);
//                regObject.put("nominee_3_pin", nominee3_pincode);
//                regObject.put("nominee_3_country", nominee3_country);
//                regObject.put("nominee_soa", "Y");
                regObject.put("reg_id", "");
                regObject.put("reg_status", "");
                regObject.put("reg_remark", "");
                regDetailsArray.put(regObject);

                JSONObject requestBody = new JSONObject();
                requestBody.put("reg_details", regDetailsArray);
                System.out.println("SingleRegistration::requestBody = " + requestBody);

                BseNseOnlineAccessDto online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, broker_code,token);
                if(online_access == null)
                {
                   return ResponseEntity.ok("Online Access Key is Not Present");
                }

                String nse_userid = NseUtils.trimOrEmpty(online_access.getNse_userid());
                String nse_memberid = NseUtils.trimOrEmpty(online_access.getNse_memberid());
                String nse_secret_key = NseUtils.trimOrEmpty(online_access.getNse_secret_key());
                String nse_license_key = NseUtils.trimOrEmpty(online_access.getNse_license_key());

                String base64Encoded = AESEncryptionUtilV2.base64EncodedAuth(nse_secret_key, nse_license_key, nse_userid);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("memberId", nse_memberid);
                headers.set("Authorization", "Basic "+base64Encoded);
                headers.set("User-Agent", "PostmanRuntime/7.43.3");
                headers.set("Accept-Encoding", "gzip, deflate, br");
                headers.set("Accept-Language", "en-US");
                headers.set("Connection", "keep-alive");
                headers.set("Referer", "");

                RestTemplate restTemplate = RestTemplateFactory.createRestTemplate();

                HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

                String requestUrl = NseApiUrls.CREATECUSTOMER;
                System.out.println("requestUrl = " + requestUrl);

                try
                {
                    ResponseEntity<String> uccResult = restTemplate.postForEntity(requestUrl, entity, String.class);
                    String statusCode = uccResult.getStatusCode().toString();
                    String responseBody = uccResult.getBody().toString();
                    System.out.println("SingleRegistration::responseBody = " + responseBody);

                    JSONObject jsonObject = new JSONObject(responseBody);
                    JSONArray jsonRegArray = jsonObject.getJSONArray("reg_details");

                    String reg_id = "";
                    String reg_status = "";
                    String reg_remark = "";

                    for (int i = 0; i < jsonRegArray.length(); i++)
                    {
                        JSONObject regDetail = jsonRegArray.getJSONObject(i);
                        reg_id = regDetail.optString("reg_id");
                        reg_status = regDetail.optString("reg_status");
                        reg_remark = regDetail.optString("reg_remark");
                    }

                    System.out.println("SingleRegistration::reg_id: " + reg_id);
                    System.out.println("SingleRegistration::reg_status: " + reg_status);
                    System.out.println("SingleRegistration::reg_remark: " + reg_remark);

                    NseTransactions nsetrans = new NseTransactions();
                    nsetrans.setUrl(requestUrl);
                    nsetrans.setNse_request(responseBody.toString());
                    nsetrans.setNse_response(responseBody);
                    nsetrans.setReturn_msg(reg_status);
                    nsetrans.setService_return_code(statusCode);
                    nsetrans.setService_msg(reg_status);
                    nsetrans.setReg_id(reg_id);
                    nsetrans.setPayment_link("");
                    nsetrans.setPan(pan);
                    nsetrans.setName(name);
                    nsetrans.setBranch(user.getBranch());
                    nsetrans.setRm_name(user.getRm_name());
                    nsetrans.setSubbroker_name(user.getSubbroker_name());
                    nsetrans.setClient_name(client_name);
                    nsetrans.setIin_number(iin_number);
                    nsetrans.setScheme_name("");
                    nsetrans.setScheme_code("");
                    nsetrans.setFolio_no("");
                    nsetrans.setAmount_units("");
                    nsetrans.setFrequency("");
                    nsetrans.setPeriod_day("");
                    nsetrans.setUmrn_no("");
                    nsetrans.setPurchase_type("");
                    nsetrans.setPayment_ref_no("");
                    nsetrans.setUnique_number("");
                    nsetrans.setAuto_trxn_no("");
                    nsetrans.setSip_reg_no("");
                    nsetrans.setPayment_mode("");
                    nsetrans.setTopup_amount(0.0);
                    nsetrans.setBank_acc_no("");
                    nsetrans.setTransaction_number("");
                    nsetrans.setApplication_number("");
                    nsetrans.setTo_scheme_code("");
                    nsetrans.setTo_scheme_name("");
                    nsetrans.setTransaction_type("Update UCC Registration");
                    nsetrans.setTransaction_status("");
                    nsetrans.setPayment_status("");
                    nsetrans.setActive_ceased_status("");
                    nsetrans.setRemarks(reg_remark);
                    nsetrans.setMandate_id("");
                    nsetrans.setMandate_status("");
                    nsetrans.setEmandate_auth_flag("");
                    nsetrans.setApp_received_flag("");
                    nsetrans.setTransaction_date(new Date());
                    nsetrans.setUser_id(user_id);
                    if(source.equalsIgnoreCase("Mobile"))
                    {
                        nsetrans.setRegister_source("Mobile App");
                    }else
                    {
                        nsetrans.setRegister_source("Website");
                    }
                    nsetrans.setBroker_code(broker_code);
                    nsetrans.setEuin_number(euin);
                    nsetrans.setCc_received("");
                    nsetrans.setFund_trans_to_amc("");
                    nsetrans.setRefund_status("");
                    nsetrans.setRefund_amount("");
                    nseTransactionService.save(nsetrans);
                    System.out.println("reg_status= " + reg_status);
                    if(reg_status.equalsIgnoreCase("REG_FAILED"))
                    {
                        return ResponseEntity.ok(reg_remark);
                    }

                    System.out.println("multiple_reg = " + multiple_reg);
                    System.out.println("multiple_reg is null? " + (multiple_reg == null));

                    if(reg_status.equalsIgnoreCase("REG_SUCCESS"))
                    {
                        System.out.println("multiple_reg =  " + multiple_reg);
                        System.out.println("userBseNSe =  " + userBseNse);

                        if (multiple_reg != null && "1".equals(multiple_reg.trim()))
                        {
                            if(userBseNse != null)
                            {
                                userBseNse.setNse_customer(1);
                                userBseNse.setNse_iin_number(iin_number);
                                userBseNse.setBroker_code(broker_code);
                                userBseNse.setEuin(euin);
                                if (client_name.equalsIgnoreCase("vbuildwealth")) {
                                    userBseNse.setNse_active(1);
                                    userBseNse.setOnline_flag("NSE");
                                }
                                System.out.println("Save User Bse Nse");
                                userServiceClient.saveUserBseNseDetail(userBseNse, token);
                            }else
                            {
                                user.setNse_customer(1);
                                user.setNse_iin_number(iin_number);
                                user.setBroker_code(broker_code);
                                user.setEuin(euin);
                                if(client_name.equalsIgnoreCase("vbuildwealth"))
                                {
                                    user.setNse_active(1);
                                    user.setOnline_flag("NSE");
                                }
                                System.out.println("Save User");
                                userServiceClient.saveUser(user,token);
                            }
                        }
                        else
                        {
                            user.setNse_customer(1);
                            user.setNse_iin_number(iin_number);
                            user.setBroker_code(broker_code);
                            user.setEuin(euin);
                            if(client_name.equalsIgnoreCase("vbuildwealth"))
                            {
                                user.setNse_active(1);
                                user.setOnline_flag("NSE");
                            }
                            System.out.println("Save User");
                            userServiceClient.saveUser(user,token);
                        }

                    }

                    // Final response page
                    if(reg_status.equalsIgnoreCase("REG_SUCCESS"))
                    {
                        return NseUtils.commonResponse(reg_status + " UCC registered successfully...! RegID: " + reg_id, HttpStatus.OK);
                    }
                    else
                    {
                        return NseUtils.commonResponse(reg_status + " : " + reg_remark, HttpStatus.BAD_REQUEST);
                    }

                }
                catch(Exception ex)
                {
                    return NseUtils.commonResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
                }

            }
            catch (Exception ex)
            {
                System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
                ex.printStackTrace();
                return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
            }

    }


}
