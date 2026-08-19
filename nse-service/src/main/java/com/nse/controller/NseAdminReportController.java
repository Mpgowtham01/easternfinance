package com.nse.controller;

import com.google.gson.Gson;
import com.nse.client.UserServiceClient;
import com.nse.config.TokenInterceptor;
import com.nse.dto.mf.BseNseKeyDto;
import com.nse.dto.mf.BseNseOnlineAccessDto;
import com.nse.dto.mf.UserDto;
import com.nse.model.NseOnlineStepUpSchemeMaster;
import com.nse.model.NseTransactions;
import com.nse.repository.NseLogRepository;
import com.nse.response.CommonResponse;
import com.nse.response.StatusMessage;
import com.nse.response.SuccessResponse;
import com.nse.services.*;
import com.nse.utils.AESEncryptionUtilV2;
import com.nse.utils.NseApiUrls;
import com.nse.utils.NseUtils;
import com.nse.utils.RestTemplateFactory;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.internal.util.StringHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.LocalDate;

import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "NSE Admin Report Controller",
        description = "APIs related to NSE Admin Reports"
)
public class NseAdminReportController
{

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Autowired
    UserServiceClient userServiceClient;

    @Autowired
    NseTransactionService nseTransactionService;

    @Autowired
    NseServiceDAO nseAmfiService;

    @Autowired
    NseLogService nseLogService;

    @Autowired
    LogExceptionService logExceptionService;

    @Autowired
    NseAmfiOnlineSchemeMaster nseAmfiOnlineSchemeMaster;

    @Operation(
            summary = "Update UTR Number for Purchase Order",
            description = "Updates the UTR number for a specific purchase order by collecting bank details and user credentials. The service verifies user identity via token, fetches user data, validates required fields like client name and IIN number, and constructs a request to the NSE API with the appropriate authentication headers. This endpoint is critical for confirming fund transfers against investment orders."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "UTR number successfully updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SuccessResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Missing or invalid input, or user/NSE details not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommonResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error - Unexpected error while processing the request",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommonResponse.class))
            )
    })
    @GetMapping("/utrNoUpdateOnPurchaseOrderApi")
    public ResponseEntity<?> utrNoUpdateOnPurchaseOrderApi(
            HttpServletRequest request,
            @RequestHeader("Authorization") String token,
            @RequestParam String name_pan_id,
            @RequestParam String bank_name,
            @RequestParam String bank_ifsc_code,
            @RequestParam String bank_acc_no,
            @RequestParam String order_id,
            @RequestParam String transfer_date,
            @RequestParam String utr_no,
            @RequestParam String iin_number,
            @RequestParam String broker_code,
            @RequestParam String source) throws Exception
    {
        String userid = "";
        String client_name = "";
        try
        {
            name_pan_id = NseUtils.checkParem(name_pan_id);
            bank_name = NseUtils.checkParem(bank_name);
            bank_ifsc_code = NseUtils.checkParem(bank_ifsc_code);
            bank_acc_no = NseUtils.checkParem(bank_acc_no);
            order_id = NseUtils.checkParem(order_id);
            transfer_date = NseUtils.checkParem(transfer_date);
            utr_no = NseUtils.checkParem(utr_no);
            iin_number = NseUtils.checkParem(iin_number);
            broker_code = NseUtils.checkParem(broker_code);
            source = NseUtils.checkParem(source);

            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);

            UserDto users = userServiceClient.getUserById(Integer.valueOf(userid), token);

            if (users == null)
            {
                return NseUtils.commonResponse("User not found", HttpStatus.BAD_REQUEST);
            }

            client_name = users.getClient_name();

            JSONArray requestDetailsArray = new JSONArray();
            JSONObject requestDetails = new JSONObject();
            requestDetails.put("client_code", iin_number);
            requestDetails.put("bank_name", bank_name);
            requestDetails.put("account_no", bank_acc_no);
            requestDetails.put("ifsc", bank_ifsc_code);
            requestDetails.put("utr_no", utr_no);
            requestDetails.put("transfer_date", transfer_date);
            requestDetails.put("order_id", order_id);
            requestDetailsArray.put(requestDetails);

            BseNseKeyDto nsekey = userServiceClient.getByClientName(client_name,token);

            if (nsekey == null)
            {
                return NseUtils.commonResponse("Client Not Found, Please contact your RM.", HttpStatus.BAD_REQUEST);
            }

            String broker_code1 = nsekey.getBrokerCode();


            if(broker_code1 == null) {broker_code1 = "";}


            if(broker_code.isEmpty())
            {
                broker_code = broker_code1;
            }

            BseNseOnlineAccessDto online_access = null;

            try
            {
                online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, broker_code,token);

            } catch (feign.FeignException.NotFound ex)
            {
                return NseUtils.commonResponse("NSE Online Credentials Not available. Please contact your RM", HttpStatus.BAD_REQUEST);
            }

            String nse_userid = NseUtils.trimOrEmpty(online_access.getNse_userid());
            String nse_memberid = NseUtils.trimOrEmpty(online_access.getNse_memberid());
            String nse_secret_key = NseUtils.trimOrEmpty(online_access.getNse_secret_key());
            String nse_license_key = NseUtils.trimOrEmpty(online_access.getNse_license_key());

            String base64Encoded = AESEncryptionUtilV2.base64EncodedAuth(nse_secret_key, nse_license_key, nse_userid);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("memberId", nse_memberid);
            headers.set("Authorization", "Basic " + base64Encoded);
            headers.set("User-Agent", "PostmanRuntime/7.43.3");
            headers.set("Accept-Encoding", "gzip, deflate, br");
            headers.set("Accept-Language", "en-US");
            headers.set("Connection", "keep-alive");
            headers.set("Referer", "");

            JSONObject requestBody = new JSONObject();
            requestBody.put("utr_details", requestDetailsArray);

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            String utrNoUpdateOnPurchaseOrder_url = NseApiUrls.UTRUPDATE;

            try
            {
                ResponseEntity<String> mandateResult = RestTemplateFactory.createRestTemplate().postForEntity(utrNoUpdateOnPurchaseOrder_url, entity, String.class);
                String statusCode = mandateResult.getStatusCode().toString();
                String responseBody = mandateResult.getBody().toString();

                JSONArray regDataArray = new JSONArray(responseBody);

                String status = "";
                String message = "";

                for (int i = 0; i < regDataArray.length(); i++)
                {
                    JSONObject item = regDataArray.getJSONObject(i);
                    status = item.getString("status");
                    message = item.getString("message");
                }


                NseTransactions nsetrans = new NseTransactions();
                nsetrans.setUrl(utrNoUpdateOnPurchaseOrder_url);
                nsetrans.setNse_request(requestBody.toString());
                nsetrans.setNse_response(responseBody.toString());
                nsetrans.setReturn_msg(message);
                nsetrans.setService_return_code(statusCode);
                nsetrans.setService_msg(message);
                nsetrans.setReg_id("");
                nsetrans.setPayment_link("");
                nsetrans.setPan("");
                nsetrans.setName("");
                nsetrans.setBranch(users.getBranch());
                nsetrans.setRm_name(users.getRm_name());
                nsetrans.setSubbroker_name(users.getSubbroker_name());
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
                nsetrans.setTransaction_type("UTR No Update On purchase order");
                nsetrans.setTransaction_status("");
                nsetrans.setPayment_status("");
                nsetrans.setActive_ceased_status("");
                nsetrans.setRemarks(message);
                nsetrans.setMandate_id("");
                nsetrans.setMandate_status("");
                nsetrans.setEmandate_auth_flag("");
                nsetrans.setApp_received_flag("");
                nsetrans.setTransaction_date(new Date());
                nsetrans.setUser_id(Integer.parseInt(userid));
                if(source.equalsIgnoreCase("Mobile"))
                {
                    nsetrans.setRegister_source("Mobile App");
                }else {
                    nsetrans.setRegister_source("Website");
                }

                nsetrans.setBroker_code("");
                nsetrans.setEuin_number("");
                nsetrans.setCc_received("");
                nsetrans.setFund_trans_to_amc("");
                nsetrans.setRefund_status("");
                nsetrans.setRefund_amount("");
                nseTransactionService.save(nsetrans);

                nseLogService.saveLog("UTR No Update On purchase order", "UTR No Update On purchase order", NseUtils.buildLogMessage("UTR No Update On purchase order", users, request), NseUtils.getIpAddr(request), source, client_name, users);

                if (status.equalsIgnoreCase("100")) {

                    return NseUtils.commonResponse(message, HttpStatus.OK);
                } else
                {
                    return NseUtils.commonResponse(message, HttpStatus.BAD_REQUEST);
                }

            } catch (Exception ex)
            {
                logExceptionService.save(Integer.parseInt(userid), client_name, NseUtils.getFullRequestUrl(request), ex.getMessage(), request.getMethod(), NseUtils.getIpAddr(request), source);
                System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
                ex.printStackTrace();
                return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }catch (Exception ex)
        {
            logExceptionService.save(Integer.parseInt(userid), client_name, NseUtils.getFullRequestUrl(request), ex.getMessage(), request.getMethod(), NseUtils.getIpAddr(request), source);
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Operation(
            summary = "Fetch NSE Online 2FA Report",
            description = "Retrieves a report of NSE Online 2FA activities for a given client and broker within a specified date range and product type. The service authenticates the user via token, extracts user information, and communicates with the external NSE API to fetch 2FA status reports."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "2FA report fetched successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SuccessResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Missing or invalid input, or user/client information not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommonResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error - Failed to fetch data from NSE or process the request",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommonResponse.class))
            )
    })
    @GetMapping("/getNseOnline2faReport")
    public ResponseEntity<?> nseOnline2faReport(
                HttpServletRequest request,
                @RequestHeader("Authorization") String token,
                @RequestParam String from_date,
                @RequestParam String to_date,
                @RequestParam String product_type,
                @RequestParam String client_code,
                @RequestParam String source,
                @RequestParam String broker_code) throws Exception
    {

        String userid = "";
        String client_name = "";
        try
        {
            from_date = NseUtils.checkParem(from_date);
            to_date = NseUtils.checkParem(to_date);
            product_type = NseUtils.checkParem(product_type);
            client_code = NseUtils.checkParem(client_code);
            source = NseUtils.checkParem(source);
            broker_code = NseUtils.checkParem(broker_code);

            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);

            UserDto users = userServiceClient.getUserById(Integer.valueOf(userid), token);

            if(users == null)
            {
                return NseUtils.commonResponse("User not found, Please login again", HttpStatus.BAD_REQUEST);
            }

            client_name = users.getClient_name();

            RestTemplate restTemplate = RestTemplateFactory.createRestTemplate();

            JSONObject online2faRequest = new JSONObject();
            online2faRequest.put("from_date", from_date);
            online2faRequest.put("to_date", to_date);
            online2faRequest.put("product_type", product_type);
            online2faRequest.put("product_id", "");
            online2faRequest.put("client_code", client_code);

            BseNseKeyDto nsekey = userServiceClient.getByClientName(client_name,token);

            String broker_code1 = nsekey.getBrokerCode();


            if (broker_code1 == null) {broker_code1 = "";}


            if (broker_code.isEmpty()) {broker_code = broker_code1;}

            BseNseOnlineAccessDto online_access = null;

            try
            {
                online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, broker_code,token);

            } catch (feign.FeignException.NotFound ex)
            {
                return NseUtils.commonResponse("NSE Online Credentials Not available. Please contact your RM", HttpStatus.BAD_REQUEST);
            }

            String nse_userid = NseUtils.trimOrEmpty(online_access.getNse_userid());
            String nse_memberid = NseUtils.trimOrEmpty(online_access.getNse_memberid());
            String nse_secret_key = NseUtils.trimOrEmpty(online_access.getNse_secret_key());
            String nse_license_key = NseUtils.trimOrEmpty(online_access.getNse_license_key());

            String base64Encoded = AESEncryptionUtilV2.base64EncodedAuth(nse_secret_key, nse_license_key, nse_userid);
            System.out.println("2FAReportApi::requestBody: " + online2faRequest.toString());
            System.out.println("2FAReportApi::authorization: " + base64Encoded);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("memberId", nse_memberid);
            headers.set("Authorization", "Basic " + base64Encoded);
            headers.set("User-Agent", "PostmanRuntime/7.43.3");
            headers.set("Accept-Encoding", "gzip, deflate, br");
            headers.set("Accept-Language", "en-US");
            headers.set("Connection", "keep-alive");
            headers.set("Referer", "");

            HttpEntity<String> entity = new HttpEntity<>(online2faRequest.toString(), headers);

            String Online2faReport_url = NseApiUrls.Online2faReport_url;

            ResponseEntity<String> online2faResponse = restTemplate.postForEntity(Online2faReport_url, entity, String.class);
            System.out.println("2FAReportApi::Response Code: " + online2faResponse.getStatusCode());
            System.out.println("2FAReportApi::Response Body: " + online2faResponse.getBody());

            JSONObject jsonResponse = new JSONObject(online2faResponse.getBody());
            String responseStatus = jsonResponse.optString("response_status");

            String report_data_json = null;

            if ("S".equalsIgnoreCase(responseStatus))
            {
                JSONArray reportDataArray = jsonResponse.optJSONArray("report_data");

                if (reportDataArray != null)
                {
                    report_data_json = reportDataArray.toString().replace("'", "\\'");
                } else
                {
                    report_data_json = "[]";
                }
            } else
            {
                report_data_json = "[]";
            }

            NseTransactions nsetrans = new NseTransactions();
            nsetrans.setUrl(Online2faReport_url);
            nsetrans.setNse_request(online2faRequest.toString());
            nsetrans.setNse_response(online2faResponse.getBody().toString());
            nsetrans.setReturn_msg(report_data_json);
            nsetrans.setService_return_code(responseStatus);
            nsetrans.setService_msg(report_data_json);
            nsetrans.setReg_id("");
            nsetrans.setPayment_link("");
            nsetrans.setPan("");
            nsetrans.setName("");
            nsetrans.setBranch(users.getBranch());
            nsetrans.setRm_name(users.getRm_name());
            nsetrans.setSubbroker_name(users.getSubbroker_name());
            nsetrans.setClient_name(client_name);
            nsetrans.setIin_number("");
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
            nsetrans.setTransaction_type("Fetch NSE Online 2FA Report");
            nsetrans.setTransaction_status("");
            nsetrans.setPayment_status("");
            nsetrans.setActive_ceased_status("");
            nsetrans.setRemarks(report_data_json);
            nsetrans.setMandate_id("");
            nsetrans.setMandate_status("");
            nsetrans.setEmandate_auth_flag("");
            nsetrans.setApp_received_flag("");
            nsetrans.setTransaction_date(new Date());
            nsetrans.setUser_id(Integer.parseInt(userid));
            if(source.equalsIgnoreCase("Mobile"))
            {
                nsetrans.setRegister_source("Mobile App");
            }else {
                nsetrans.setRegister_source("Website");
            }

            nsetrans.setBroker_code("");
            nsetrans.setEuin_number("");
            nsetrans.setCc_received("");
            nsetrans.setFund_trans_to_amc("");
            nsetrans.setRefund_status("");
            nsetrans.setRefund_amount("");
            nseTransactionService.save(nsetrans);

            nseLogService.saveLog("Fetch NSE Online 2FA Report", "Fetch NSE Online 2FA Report", NseUtils.buildLogMessage("Fetch NSE Online 2FA Report", users, request), NseUtils.getIpAddr(request), source, client_name, users);

            return ResponseEntity.ok(report_data_json);

        }catch (Exception ex)
        {
            logExceptionService.save(Integer.parseInt(userid), client_name, NseUtils.getFullRequestUrl(request), ex.getMessage(), request.getMethod(), NseUtils.getIpAddr(request), source);
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Resend Email for Transaction Confirmation",
            description = "Triggers a resend of the transaction confirmation email for a specified client and transaction. This API validates the user based on the provided token, checks the client details, and communicates with backend services to resend the email related to the given transaction number and product type."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Email resend initiated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SuccessResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Invalid input or user not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommonResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error - Email resend process failed",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommonResponse.class))
            )
    })
    @GetMapping("/resendEmailApi")
    public ResponseEntity<?> resendEmailApi(
            HttpServletRequest request,
            @RequestHeader("Authorization") String token,
            @RequestParam String client_code,
            @RequestParam String product_type,
            @RequestParam String transaction_number,
            @RequestParam String source,
            @RequestParam String broker_code) throws Exception
    {
        String userid = "";
        String client_name = "";
        try
        {
            client_code = NseUtils.checkParem(client_code);
            product_type = NseUtils.checkParem(product_type);
            transaction_number = NseUtils.checkParem(transaction_number);
            broker_code = NseUtils.checkParem(broker_code);

            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);

            UserDto user = userServiceClient.getUserById(Integer.valueOf(userid), token);

            if(user == null)
            {
                return NseUtils.commonResponse("User not found, Please login again", HttpStatus.BAD_REQUEST);
            }

            client_name = user.getClient_name();

            client_code = NseUtils.checkParem(client_code);
            product_type = NseUtils.checkParem(product_type);
            transaction_number = NseUtils.checkParem(transaction_number);
            broker_code = NseUtils.checkParem(broker_code);

            JSONObject requestDetails = new JSONObject();
            requestDetails.put("productType", product_type);
            requestDetails.put("productRefId", transaction_number);

            BseNseKeyDto nsekey = userServiceClient.getByClientName(client_name,token);
            String broker_code1 = nsekey.getBrokerCode();


            if(broker_code1 == null) {broker_code1 = "";}


            if(broker_code.isEmpty())
            {
                broker_code = broker_code1;
            }

            BseNseOnlineAccessDto online_access = null;

            try
            {
                online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, broker_code,token);

            } catch (feign.FeignException.NotFound ex)
            {
                return NseUtils.commonResponse("NSE Online Credentials Not available. Please contact your RM", HttpStatus.BAD_REQUEST);
            }

            String nse_userid = NseUtils.trimOrEmpty(online_access.getNse_userid());
            String nse_memberid = NseUtils.trimOrEmpty(online_access.getNse_memberid());
            String nse_secret_key = NseUtils.trimOrEmpty(online_access.getNse_secret_key());
            String nse_license_key = NseUtils.trimOrEmpty(online_access.getNse_license_key());

            String base64Encoded = AESEncryptionUtilV2.base64EncodedAuth(nse_secret_key, nse_license_key, nse_userid);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("memberId", nse_memberid);
            headers.set("Authorization", "Basic " + base64Encoded);
            headers.set("User-Agent", "PostmanRuntime/7.43.3");
            headers.set("Accept-Encoding", "gzip, deflate, br");
            headers.set("Accept-Language", "en-US");
            headers.set("Connection", "keep-alive");
            headers.set("Referer", "");

            System.out.println("resendEmailApi::requestBody: "  +requestDetails.toString());
            HttpEntity<String> entity = new HttpEntity<>(requestDetails.toString(), headers);

            String resendEmail_url = NseApiUrls.resendEmail_url;

            try
            {

                ResponseEntity<String> resendResult = RestTemplateFactory.createRestTemplate().postForEntity(resendEmail_url, entity, String.class);
                String statusCode = resendResult.getStatusCode().toString();
                String responseBody = resendResult.getBody().toString();

                System.out.println("statusCode = " + statusCode);
                System.out.println("responseBody = " + responseBody);

                JSONObject jsonObject = new JSONObject(responseBody);

                String response_status = jsonObject.getString("response_status");
                String error_remark = jsonObject.getString("error_remark");

                NseTransactions nsetrans = new NseTransactions();
                nsetrans.setUrl(resendEmail_url);
                nsetrans.setNse_request(requestDetails.toString());
                nsetrans.setNse_response(resendResult.getBody().toString());
                nsetrans.setReturn_msg(error_remark);
                nsetrans.setService_return_code(response_status);
                nsetrans.setService_msg(error_remark);
                nsetrans.setReg_id("");
                nsetrans.setPayment_link("");
                nsetrans.setPan("");
                nsetrans.setName("");
                nsetrans.setBranch(user.getBranch());
                nsetrans.setRm_name(user.getRm_name());
                nsetrans.setSubbroker_name(user.getSubbroker_name());
                nsetrans.setClient_name(client_name);
                nsetrans.setIin_number("");
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
                nsetrans.setTransaction_type("Resend Email for Transaction Confirmation");
                nsetrans.setTransaction_status("");
                nsetrans.setPayment_status("");
                nsetrans.setActive_ceased_status("");
                nsetrans.setRemarks(error_remark);
                nsetrans.setMandate_id("");
                nsetrans.setMandate_status("");
                nsetrans.setEmandate_auth_flag("");
                nsetrans.setApp_received_flag("");
                nsetrans.setTransaction_date(new Date());
                nsetrans.setUser_id(Integer.parseInt(userid));
                if(source.equalsIgnoreCase("Mobile"))
                {
                    nsetrans.setRegister_source("Mobile App");
                }else {
                    nsetrans.setRegister_source("Website");
                }

                nsetrans.setBroker_code("");
                nsetrans.setEuin_number("");
                nsetrans.setCc_received("");
                nsetrans.setFund_trans_to_amc("");
                nsetrans.setRefund_status("");
                nsetrans.setRefund_amount("");
                nseTransactionService.save(nsetrans);

                nseLogService.saveLog("Resend Email for Transaction Confirmation", "Resend Email for Transaction Confirmation", NseUtils.buildLogMessage("Resend Email for Transaction Confirmation", user, request), NseUtils.getIpAddr(request), source, client_name, user);

                if(!response_status.equalsIgnoreCase("S"))
                {
                    return NseUtils.commonResponse(error_remark, HttpStatus.BAD_REQUEST);
                }

                return NseUtils.commonResponse("Email sent Successfully",HttpStatus.OK);

            }catch (Exception ex)
            {
                logExceptionService.save(Integer.parseInt(userid), client_name, NseUtils.getFullRequestUrl(request), ex.getMessage(), request.getMethod(), NseUtils.getIpAddr(request), source);
                System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
                ex.printStackTrace();
                return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }catch (Exception ex)
        {
            logExceptionService.save(Integer.parseInt(userid), client_name, NseUtils.getFullRequestUrl(request), ex.getMessage(), request.getMethod(), NseUtils.getIpAddr(request), source);
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Generate Client Master Report",
            description = "Fetches a client master report based on specified filters such as date range, client code, PAN number, and broker code. The API validates input based on the selected search option (either by client code or PAN), authenticates the user using the provided token, and returns client details accordingly."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Client master report generated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SuccessResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Invalid input, missing parameters, or user not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommonResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error - Error occurred during report generation",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommonResponse.class))
            )
    })
    @GetMapping("/clientMasterReportApi")
    public ResponseEntity<?> clientMasterReportApi(
            HttpServletRequest request,
            @RequestHeader("Authorization") String token,
            @RequestParam String from_date,
            @RequestParam String to_date,
            @RequestParam String client_code,
            @RequestParam String broker_code,
            @RequestParam String pan_no,
            @RequestParam String source,
            @RequestParam String option) throws Exception
    {

        String client_name = "";
        String userid = "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        try
        {
            from_date = NseUtils.checkParem(from_date);
            to_date = NseUtils.checkParem(to_date);
            client_code = NseUtils.checkParem(client_code);
            pan_no = NseUtils.checkParem(pan_no);
            option = NseUtils.checkParem(option);
            source = NseUtils.checkParem(source);
            broker_code = NseUtils.checkParem(broker_code);

            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);

            UserDto user = userServiceClient.getUserById(Integer.valueOf(userid), token);

            if (user == null)
            {
                return NseUtils.commonResponse("User not found, please try again", HttpStatus.BAD_REQUEST);
            }

            client_name = user.getClient_name();

            broker_code= NseUtils.checkParem(broker_code);
            client_name = NseUtils.checkParem(client_name);
            from_date = NseUtils.checkParem(from_date);
            to_date = NseUtils.checkParem(to_date);
            client_code = NseUtils.checkParem(client_code);
            pan_no = NseUtils.checkParem(pan_no);
            option = NseUtils.checkParem(option);

            if(StringHelper.isEmpty(broker_code))
            {
                return NseUtils.commonResponse("Broker code must be selected!", HttpStatus.BAD_REQUEST);
            }

            if(option.equalsIgnoreCase("client code"))
            {
                if (StringHelper.isEmpty(client_code))
                {
                    return NseUtils.commonResponse("Please provide client code!", HttpStatus.BAD_REQUEST);
                }
            }else if(option.equalsIgnoreCase("pan"))
            {
                if (StringHelper.isEmpty(pan_no))
                {
                    return NseUtils.commonResponse("Please provide pan number!", HttpStatus.BAD_REQUEST);
                }

            }else if(option.equalsIgnoreCase("date"))
            {
                if(from_date.isEmpty() && to_date.isEmpty())
                {
                    Calendar cal = Calendar.getInstance();
                    Date today = cal.getTime();

                    cal = Calendar.getInstance();
                    cal.add(Calendar.DATE, -7);
                    Date fromDate = cal.getTime();

                    from_date = sdf.format(fromDate);
                    to_date = sdf.format(today);

                }else
                {
                    Date toDate = sdf.parse(to_date);
                    Date fromDate = sdf.parse(from_date);

                    if(toDate.before(fromDate)){
                        return ResponseEntity.internalServerError().body("from_date cannot be future date.");
                    }

                    long diffInMillis = toDate.getTime() - fromDate.getTime();

                    long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);

                    if(diffInDays > 7)
                    {
                        return NseUtils.commonResponse("FromDate and ToDate must be between & 7 days!", HttpStatus.BAD_REQUEST);
                    }

                    to_date = sdf.format(toDate);
                    from_date = sdf.format(fromDate);
                }
            }

            JSONObject requestDetails = new JSONObject();
            requestDetails.put("client_code", client_code);
            requestDetails.put("from_date", from_date);
            requestDetails.put("to_date", to_date);
            requestDetails.put("pan", pan_no);

            BseNseOnlineAccessDto online_access = null;

            try
            {
                online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, broker_code,token);

            } catch (feign.FeignException.NotFound ex)
            {
                return NseUtils.commonResponse("NSE Online Credentials Not available. Please contact your RM", HttpStatus.BAD_REQUEST);
            }

            String nse_userid = NseUtils.trimOrEmpty(online_access.getNse_userid());
            String nse_memberid = NseUtils.trimOrEmpty(online_access.getNse_memberid());
            String nse_secret_key = NseUtils.trimOrEmpty(online_access.getNse_secret_key());
            String nse_license_key = NseUtils.trimOrEmpty(online_access.getNse_license_key());

            String base64Encoded = AESEncryptionUtilV2.base64EncodedAuth(nse_secret_key, nse_license_key, nse_userid);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("memberId", nse_memberid);
            headers.set("Authorization", "Basic " + base64Encoded);
            headers.set("User-Agent", "PostmanRuntime/7.43.3");
            headers.set("Accept-Encoding", "gzip, deflate, br");
            headers.set("Accept-Language", "en-US");
            headers.set("Connection", "keep-alive");
            headers.set("Referer", "");

            HttpEntity<String> entity = new HttpEntity<>(requestDetails.toString(), headers);

            String clientMasterReportApi_url = NseApiUrls.clientMasterReport_url;

            try
            {

                ResponseEntity<String> mandateResult = RestTemplateFactory.createRestTemplate().postForEntity(clientMasterReportApi_url, entity, String.class);
                String responseBody = mandateResult.getBody().toString();

                JSONObject jsonObject = new JSONObject(responseBody);
                JSONArray regDataArray = new JSONArray();

                String status = jsonObject.getString("response_status");
                String error_remark = jsonObject.getString("error_remark");

                if (jsonObject.has("report_data"))
                {
                    regDataArray = jsonObject.getJSONArray("report_data");
                }

                NseTransactions nsetrans = new NseTransactions();
                nsetrans.setUrl(clientMasterReportApi_url);
                nsetrans.setNse_request(requestDetails.toString());
                nsetrans.setNse_response(mandateResult.getBody().toString());
                nsetrans.setReturn_msg(error_remark);
                nsetrans.setService_return_code(status);
                nsetrans.setService_msg(error_remark);
                nsetrans.setReg_id("");
                nsetrans.setPayment_link("");
                nsetrans.setPan("");
                nsetrans.setName("");
                nsetrans.setBranch(user.getBranch());
                nsetrans.setRm_name(user.getRm_name());
                nsetrans.setSubbroker_name(user.getSubbroker_name());
                nsetrans.setClient_name(client_name);
                nsetrans.setIin_number("");
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
                nsetrans.setTransaction_type("Generate Client Master Report");
                nsetrans.setTransaction_status("");
                nsetrans.setPayment_status("");
                nsetrans.setActive_ceased_status("");
                nsetrans.setRemarks(error_remark);
                nsetrans.setMandate_id("");
                nsetrans.setMandate_status("");
                nsetrans.setEmandate_auth_flag("");
                nsetrans.setApp_received_flag("");
                nsetrans.setTransaction_date(new Date());
                nsetrans.setUser_id(Integer.parseInt(userid));
                if(source.equalsIgnoreCase("Mobile"))
                {
                    nsetrans.setRegister_source("Mobile App");
                }else {
                    nsetrans.setRegister_source("Website");
                }

                nsetrans.setBroker_code("");
                nsetrans.setEuin_number("");
                nsetrans.setCc_received("");
                nsetrans.setFund_trans_to_amc("");
                nsetrans.setRefund_status("");
                nsetrans.setRefund_amount("");
                nseTransactionService.save(nsetrans);

                nseLogService.saveLog("Generate Client Master Report", "Generate Client Master Report", NseUtils.buildLogMessage("Generate Client Master Report", user, request), NseUtils.getIpAddr(request), source, client_name, user);

                if (status.equalsIgnoreCase("0") || status.equalsIgnoreCase(""))
                {
                    return NseUtils.commonResponse(error_remark, HttpStatus.BAD_REQUEST);
                }

                nseAmfiService.insertClientMasterData(regDataArray,broker_code,client_name,token);

                return NseUtils.commonResponse("Client Master Updated!", HttpStatus.OK);

            } catch (Exception ex)
            {
                logExceptionService.save(Integer.parseInt(userid), client_name, NseUtils.getFullRequestUrl(request), ex.getMessage(), request.getMethod(), NseUtils.getIpAddr(request), source);
                System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
                ex.printStackTrace();
                return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }catch (Exception ex)
        {
            logExceptionService.save(Integer.parseInt(userid), client_name, NseUtils.getFullRequestUrl(request), ex.getMessage(), request.getMethod(), NseUtils.getIpAddr(request), source);
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Fetch Redemption Statement Report",
            description = "Retrieves the redemption statement report for a given date range and broker code. The API extracts user information from the authorization token, sets default values for optional fields, and communicates with the external NSE system to fetch the required data."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Redemption statement report fetched successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SuccessResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Invalid or missing user data, or input validation failure",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommonResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error - Unable to process the redemption report request",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommonResponse.class))
            )
    })
    @GetMapping("/getRedemptionStatementReport")
    public ResponseEntity<?> utrNoUpdateOnPurchaseOrderApi(
            HttpServletRequest request,
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String from_date,
            @RequestParam(required = false) String to_date,
            @RequestParam(required = false) String broker_code,
            @RequestParam(required = false) String source) throws Exception
    {
        String userid = "";
        String client_name = "";
        String iin_number = "";
        try
        {
            from_date = NseUtils.trimOrEmpty(from_date);
            to_date = NseUtils.trimOrEmpty(to_date);
            broker_code = NseUtils.trimOrEmpty(broker_code);
            source = NseUtils.trimOrEmpty(source);

            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);

            UserDto user = userServiceClient.getUserById(Integer.valueOf(userid), token);

            if(user == null)
            {
                return NseUtils.commonResponse("User not found, please login again.", HttpStatus.BAD_REQUEST);
            }

            client_name = user.getClient_name();
            iin_number = user.getNse_iin_number();

            RestTemplate restTemplate = RestTemplateFactory.createRestTemplate();

            try
            {
                JSONObject redemptionstatement = new JSONObject();
                redemptionstatement.put("from_date", from_date);
                redemptionstatement.put("to_date", to_date);
                redemptionstatement.put("order_type", "");
                redemptionstatement.put("sub_order_type", "");
                redemptionstatement.put("order_ids", "");
                redemptionstatement.put("client_code", iin_number);
                redemptionstatement.put("transaction_type", "P");
                redemptionstatement.put("order_status", "All");
                redemptionstatement.put("settlement_type", "ALL");
                redemptionstatement.put("member_unique_ids", "");


                BseNseKeyDto nsekey = userServiceClient.getByClientName(client_name, token);
                String broker_code1 = nsekey.getBrokerCode();

                if(broker_code1 == null) {broker_code1 = "";}


                if(broker_code.isEmpty())
                {
                    broker_code = broker_code1;
                }

                BseNseOnlineAccessDto online_access = null;

                try
                {
                    online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, broker_code,token);

                } catch (feign.FeignException.NotFound ex)
                {
                    return NseUtils.commonResponse("NSE Online Credentials Not available. Please contact your RM", HttpStatus.BAD_REQUEST);
                }

                String nse_userid = NseUtils.trimOrEmpty(online_access.getNse_userid());
                String nse_memberid = NseUtils.trimOrEmpty(online_access.getNse_memberid());
                String nse_secret_key = NseUtils.trimOrEmpty(online_access.getNse_secret_key());
                String nse_license_key = NseUtils.trimOrEmpty(online_access.getNse_license_key());

                String base64Encoded = AESEncryptionUtilV2.base64EncodedAuth(nse_secret_key, nse_license_key, nse_userid);
                System.out.println("redemptionstatementApi::requestBody: " + redemptionstatement.toString());
                System.out.println("redemptionstatementApi::authorization: " + base64Encoded);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("memberId", nse_memberid);
                headers.set("Authorization", "Basic "+base64Encoded);
                headers.set("User-Agent", "PostmanRuntime/7.43.3");
                headers.set("Accept-Encoding", "gzip, deflate, br");
                headers.set("Accept-Language", "en-US");
                headers.set("Connection", "keep-alive");
                headers.set("Referer", "");

                HttpEntity<String> entity = new HttpEntity<>(redemptionstatement.toString(), headers);

                String redemptionstatementReport_url = NseApiUrls.redemptionstatementReport_url;

                ResponseEntity<String> redemptionstatementResponse = restTemplate.postForEntity(redemptionstatementReport_url, entity, String.class);
                System.out.println("redemptionstatementApi::Response Code: " + redemptionstatementResponse.getStatusCode());
                System.out.println("redemptionstatementApi::Response Body: " + redemptionstatementResponse.getBody());

                JSONObject jsonResponse = new JSONObject(redemptionstatementResponse.getBody());
                String responseStatus = jsonResponse.optString("response_status");
                String error_remark = jsonResponse.getString("error_remark");

                NseTransactions nsetrans = new NseTransactions();
                nsetrans.setUrl(redemptionstatementReport_url);
                nsetrans.setNse_request(redemptionstatement.toString());
                nsetrans.setNse_response(redemptionstatementResponse.getBody());
                nsetrans.setReturn_msg(responseStatus);
                nsetrans.setService_return_code(responseStatus);
                nsetrans.setService_msg(responseStatus);
                nsetrans.setReg_id("");
                nsetrans.setPayment_link("");
                nsetrans.setPan("");
                nsetrans.setName("");
                nsetrans.setBranch(user.getBranch());
                nsetrans.setRm_name(user.getRm_name());
                nsetrans.setSubbroker_name(user.getSubbroker_name());
                nsetrans.setClient_name(client_name);
                nsetrans.setIin_number("");
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
                nsetrans.setTransaction_type("Fetch Redemption Statement Report");
                nsetrans.setTransaction_status("");
                nsetrans.setPayment_status("");
                nsetrans.setActive_ceased_status("");
                nsetrans.setRemarks(responseStatus);
                nsetrans.setMandate_id("");
                nsetrans.setMandate_status("");
                nsetrans.setEmandate_auth_flag("");
                nsetrans.setApp_received_flag("");
                nsetrans.setTransaction_date(new Date());
                nsetrans.setUser_id(Integer.parseInt(userid));
                if(source.equalsIgnoreCase("Mobile"))
                {
                    nsetrans.setRegister_source("Mobile App");
                }else{
                    nsetrans.setRegister_source("Website");
                }

                nsetrans.setBroker_code("");
                nsetrans.setEuin_number("");
                nsetrans.setCc_received("");
                nsetrans.setFund_trans_to_amc("");
                nsetrans.setRefund_status("");
                nsetrans.setRefund_amount("");
                nseTransactionService.save(nsetrans);

                nseLogService.saveLog("Fetch Redemption Statement Report", "Fetch Redemption Statement Report", NseUtils.buildLogMessage("Fetch Redemption Statement Report", user, request), NseUtils.getIpAddr(request), source, client_name, user);

                String report_data_json = "[]";

                if (!responseStatus.equalsIgnoreCase("S"))
                {
                    return NseUtils.commonResponse(error_remark, HttpStatus.BAD_REQUEST);
                }

                JSONArray reportDataArray = jsonResponse.optJSONArray("report_data");

                if (reportDataArray != null)
                {
                    report_data_json = reportDataArray.toString().replace("'", "\\'");
                }

                return ResponseEntity.ok(report_data_json);

            }catch (Exception ex)
            {
                logExceptionService.save(Integer.parseInt(userid), client_name, NseUtils.getFullRequestUrl(request), ex.getMessage(), request.getMethod(), NseUtils.getIpAddr(request), source);
                System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
                ex.printStackTrace();
                return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }catch (Exception ex)
        {
            logExceptionService.save(Integer.parseInt(userid), client_name, NseUtils.getFullRequestUrl(request), ex.getMessage(), request.getMethod(), NseUtils.getIpAddr(request), source);
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Fetch Allotment Statement Report",
            description = "Retrieves the allotment statement report based on optional filters such as date range and broker code. The API extracts user details using the provided token, prepares the request payload, and interacts with the NSE system to fetch the allotment report data."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Allotment statement report retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SuccessResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Missing or invalid user data or required parameters",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommonResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error - Error occurred while processing the allotment report",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommonResponse.class))
            )
    })
    @GetMapping("/getAllotmentStatementReport")
    public ResponseEntity<?> getAllotmentStatementReport(
            HttpServletRequest request,
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String from_date,
            @RequestParam(required = false) String to_date,
            @RequestParam(required = false) String broker_code,
            @RequestParam(required = false) String source) throws Exception
    {
        String userid = "";
        String client_name = "";
        String iin_number = "";
        try
        {
            from_date = NseUtils.trimOrEmpty(from_date);
            to_date = NseUtils.trimOrEmpty(to_date);
            broker_code = NseUtils.trimOrEmpty(broker_code);
            source = NseUtils.trimOrEmpty(source);

            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);

            UserDto user = userServiceClient.getUserById(Integer.valueOf(userid), token);

            if(user == null)
            {
                return NseUtils.commonResponse("User not found, please login again", HttpStatus.BAD_REQUEST);
            }

            client_name = user.getClient_name();
            iin_number = user.getNse_iin_number();

            RestTemplate restTemplate = RestTemplateFactory.createRestTemplate();
            try
            {
                JSONObject allotmentstatement = new JSONObject();
                allotmentstatement.put("from_date", from_date);
                allotmentstatement.put("to_date", to_date);
                allotmentstatement.put("order_type", "");
                allotmentstatement.put("sub_order_type", "");
                allotmentstatement.put("order_ids", "");
                allotmentstatement.put("client_code", iin_number);
                allotmentstatement.put("transaction_type", "P");
                allotmentstatement.put("order_status", "All");
                allotmentstatement.put("settlement_type", "ALL");
                allotmentstatement.put("member_unique_ids", "");

                BseNseKeyDto nsekey = userServiceClient.getByClientName(client_name, token);
                String broker_code1 = nsekey.getBrokerCode();

                if(broker_code1 == null) {broker_code1 = "";}

                if(broker_code.isEmpty())
                {
                    broker_code = broker_code1;
                }

                BseNseOnlineAccessDto online_access = null;

                try
                {
                    online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, broker_code,token);

                } catch (feign.FeignException.NotFound ex)
                {
                    return NseUtils.commonResponse("NSE Online Credentials Not available. Please contact your RM", HttpStatus.BAD_REQUEST);
                }

                String nse_userid = NseUtils.trimOrEmpty(online_access.getNse_userid());
                String nse_memberid = NseUtils.trimOrEmpty(online_access.getNse_memberid());
                String nse_secret_key = NseUtils.trimOrEmpty(online_access.getNse_secret_key());
                String nse_license_key = NseUtils.trimOrEmpty(online_access.getNse_license_key());

                String base64Encoded = AESEncryptionUtilV2.base64EncodedAuth(nse_secret_key, nse_license_key, nse_userid);
                System.out.println("allotmentstatementApi::requestBody: " + allotmentstatement.toString());
                System.out.println("allotmentstatementApi::authorization: " + base64Encoded);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("memberId", nse_memberid);
                headers.set("Authorization", "Basic "+base64Encoded);
                headers.set("User-Agent", "PostmanRuntime/7.43.3");
                headers.set("Accept-Encoding", "gzip, deflate, br");
                headers.set("Accept-Language", "en-US");
                headers.set("Connection", "keep-alive");
                headers.set("Referer", "");

                HttpEntity<String> entity = new HttpEntity<>(allotmentstatement.toString(), headers);

                String allotmentstatementReport_url= NseApiUrls.allotmentstatementReport_url;

                ResponseEntity<String> allotmentstatementResponse = restTemplate.postForEntity(allotmentstatementReport_url, entity, String.class);
                System.out.println("allotmentstatementApi::Response Code: " + allotmentstatementResponse.getStatusCode());
                System.out.println("allotmentstatementApi::Response Body: " + allotmentstatementResponse.getBody());

                JSONObject jsonResponse = new JSONObject(allotmentstatementResponse.getBody());
                String responseStatus = jsonResponse.optString("response_status");
                String error_remark = jsonResponse.optString("error_remark");

                NseTransactions nsetrans = new NseTransactions();
                nsetrans.setUrl(allotmentstatementReport_url);
                nsetrans.setNse_request(allotmentstatement.toString());
                nsetrans.setNse_response(allotmentstatementResponse.getBody());
                nsetrans.setReturn_msg(responseStatus);
                nsetrans.setService_return_code(responseStatus);
                nsetrans.setService_msg(responseStatus);
                nsetrans.setReg_id("");
                nsetrans.setPayment_link("");
                nsetrans.setPan("");
                nsetrans.setName("");
                nsetrans.setBranch(user.getBranch());
                nsetrans.setRm_name(user.getRm_name());
                nsetrans.setSubbroker_name(user.getSubbroker_name());
                nsetrans.setClient_name(client_name);
                nsetrans.setIin_number("");
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
                nsetrans.setTransaction_type("Allotment Statement Report");
                nsetrans.setTransaction_status("");
                nsetrans.setPayment_status("");
                nsetrans.setActive_ceased_status("");
                nsetrans.setRemarks(responseStatus);
                nsetrans.setMandate_id("");
                nsetrans.setMandate_status("");
                nsetrans.setEmandate_auth_flag("");
                nsetrans.setApp_received_flag("");
                nsetrans.setTransaction_date(new Date());
                nsetrans.setUser_id(Integer.parseInt(userid));
                if(source.equalsIgnoreCase("Mobile"))
                {
                    nsetrans.setRegister_source("Mobile App");
                }else{
                    nsetrans.setRegister_source("Website");
                }

                nsetrans.setBroker_code("");
                nsetrans.setEuin_number("");
                nsetrans.setCc_received("");
                nsetrans.setFund_trans_to_amc("");
                nsetrans.setRefund_status("");
                nsetrans.setRefund_amount("");
                nseTransactionService.save(nsetrans);

                nseLogService.saveLog("Fetch Allotment Statement Report", "Fetch Allotment Statement Report", NseUtils.buildLogMessage("Fetch Allotment Statement Report", user, request), NseUtils.getIpAddr(request), source, client_name, user);

                String report_data_json = "[]";

                if (!responseStatus.equalsIgnoreCase("S"))
                {
                    return NseUtils.commonResponse(error_remark, HttpStatus.BAD_REQUEST);
                }

                JSONArray reportDataArray = jsonResponse.optJSONArray("report_data");

                if (reportDataArray != null)
                {
                    report_data_json = reportDataArray.toString().replace("'", "\\'");
                }

                return ResponseEntity.ok(report_data_json);

            }catch (Exception ex)
            {
                logExceptionService.save(Integer.parseInt(userid), client_name, NseUtils.getFullRequestUrl(request), ex.getMessage(), request.getMethod(), NseUtils.getIpAddr(request), source);
                System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
                ex.printStackTrace();
                return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }catch (Exception ex)
        {
            logExceptionService.save(Integer.parseInt(userid), client_name, NseUtils.getFullRequestUrl(request), ex.getMessage(), request.getMethod(), NseUtils.getIpAddr(request), source);
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Fetch Client Authorization Report",
            description = "Retrieves the client authorization report based on optional filters such as IIN number, date range, broker code, and source. If no dates are provided, it defaults to the last 7 days. The endpoint authenticates the user via token and fetches data from the NSE system."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Client authorization report retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SuccessResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Missing user or required information",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommonResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error - Failed to fetch or process client authorization report",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommonResponse.class))
            )
    })
    @GetMapping("/getClientAuthorizationReport")
    public ResponseEntity<?> getClientAuthorizationReport(
            HttpServletRequest request,
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String iin_number,
            @RequestParam(required = false) String from_date,
            @RequestParam(required = false) String to_date,
            @RequestParam(required = false) String broker_code,
            @RequestParam(required = false) String source) throws Exception
    {
        String userid = "";
        String client_name = "";
        try
        {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            SimpleDateFormat sdf1 = new SimpleDateFormat("MM/dd/yyyy");

            iin_number = NseUtils.trimOrEmpty(iin_number);
            from_date = NseUtils.trimOrEmpty(from_date);
            to_date = NseUtils.trimOrEmpty(to_date);
            broker_code = NseUtils.trimOrEmpty(broker_code);
            source = NseUtils.trimOrEmpty(source);

            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);

            UserDto user = userServiceClient.getUserById(Integer.valueOf(userid), token);

            if(user == null)
            {
                return NseUtils.commonResponse("User not found, please login again", HttpStatus.BAD_REQUEST);
            }

            client_name = user.getClient_name();

            if(from_date == "" && to_date =="")
            {
                Calendar cal = Calendar.getInstance();
                Date today = cal.getTime();

                cal = Calendar.getInstance();
                cal.add(Calendar.DATE, -7);
                Date fromDate = cal.getTime();

                from_date = sdf.format(fromDate);
                to_date = sdf.format(today);

            }else
            {
                Date toDate = sdf.parse(to_date);
                Date fromDate = sdf.parse(from_date);

                if(toDate.before(fromDate))
                {
                    return NseUtils.commonResponse("From date cannot be future date", HttpStatus.BAD_REQUEST);
                }

                long diffInMillis = toDate.getTime() - fromDate.getTime();

                long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);

                if(diffInDays > 7)
                {
                    return NseUtils.commonResponse("FromDate and ToDate must be between & 7 days!", HttpStatus.BAD_REQUEST);
                }
                to_date = sdf.format(toDate);
                from_date = sdf.format(fromDate);
            }

            RestTemplate restTemplate = RestTemplateFactory.createRestTemplate();

            try
            {
                JSONObject requestDetails = new JSONObject();
                requestDetails.put("client_code", "");
                requestDetails.put("from_date", from_date);
                requestDetails.put("to_date", to_date);
                requestDetails.put("auth_status", "");

                BseNseKeyDto nsekey = userServiceClient.getByClientName(client_name, token);
                String broker_code1 = nsekey.getBrokerCode();

                if(broker_code1 == null) {broker_code1 = "";}

                if(broker_code.isEmpty())
                {
                    broker_code = broker_code1;
                }

                BseNseOnlineAccessDto online_access = null;

                try
                {
                    online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, broker_code,token);

                } catch (feign.FeignException.NotFound ex)
                {
                    return NseUtils.commonResponse("NSE Online Credentials Not available. Please contact your RM", HttpStatus.BAD_REQUEST);
                }

                String nse_userid = NseUtils.trimOrEmpty(online_access.getNse_userid());
                String nse_memberid = NseUtils.trimOrEmpty(online_access.getNse_memberid());
                String nse_secret_key = NseUtils.trimOrEmpty(online_access.getNse_secret_key());
                String nse_license_key = NseUtils.trimOrEmpty(online_access.getNse_license_key());

                String base64Encoded = AESEncryptionUtilV2.base64EncodedAuth(nse_secret_key, nse_license_key, nse_userid);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("memberId", nse_memberid);
                headers.set("Authorization", "Basic " + base64Encoded);
                headers.set("User-Agent", "PostmanRuntime/7.43.3");
                headers.set("Accept-Encoding", "gzip, deflate, br");
                headers.set("Accept-Language", "en-US");
                headers.set("Connection", "keep-alive");
                headers.set("Referer", "");

                HttpEntity<String> entity = new HttpEntity<>(requestDetails.toString(), headers);

                String clientAuthorizationReportApi_url = NseApiUrls.clientAuthorizationReportApi_url;

                ResponseEntity<String> mandateResult = RestTemplateFactory.createRestTemplate().postForEntity(clientAuthorizationReportApi_url, entity, String.class);
                String statusCode = mandateResult.getStatusCode().toString();
                String responseBody = mandateResult.getBody().toString();

                JSONObject jsonObject = new JSONObject(responseBody);
                JSONArray regDataArray = new JSONArray();

                String status = jsonObject.getString("report_data_total");
                String error_remark = jsonObject.getString("error_remark");

                if (jsonObject.has("report_data"))
                {
                    regDataArray = jsonObject.getJSONArray("report_data");
                }

                NseTransactions nsetrans = new NseTransactions();
                nsetrans.setUrl(clientAuthorizationReportApi_url);
                nsetrans.setNse_request(requestDetails.toString());
                nsetrans.setNse_response(responseBody);
                nsetrans.setReturn_msg(status);
                nsetrans.setService_return_code(status);
                nsetrans.setService_msg(status);
                nsetrans.setReg_id("");
                nsetrans.setPayment_link("");
                nsetrans.setPan("");
                nsetrans.setName("");
                nsetrans.setBranch(user.getBranch());
                nsetrans.setRm_name(user.getRm_name());
                nsetrans.setSubbroker_name(user.getSubbroker_name());
                nsetrans.setClient_name(client_name);
                nsetrans.setIin_number("");
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
                nsetrans.setTransaction_type("Client Authorization report");
                nsetrans.setTransaction_status("");
                nsetrans.setPayment_status("");
                nsetrans.setActive_ceased_status("");
                nsetrans.setRemarks(status);
                nsetrans.setMandate_id("");
                nsetrans.setMandate_status("");
                nsetrans.setEmandate_auth_flag("");
                nsetrans.setApp_received_flag("");
                nsetrans.setTransaction_date(new Date());
                nsetrans.setUser_id(Integer.parseInt(userid));
                if(source.equalsIgnoreCase("Mobile"))
                {
                    nsetrans.setRegister_source("Mobile App");
                }else{
                    nsetrans.setRegister_source("Website");
                }

                nsetrans.setBroker_code("");
                nsetrans.setEuin_number("");
                nsetrans.setCc_received("");
                nsetrans.setFund_trans_to_amc("");
                nsetrans.setRefund_status("");
                nsetrans.setRefund_amount("");
                nseTransactionService.save(nsetrans);

                nseLogService.saveLog("Fetch Client Authorization report", "Fetch Client Authorization report", NseUtils.buildLogMessage("Fetch Client Authorization report", user, request), NseUtils.getIpAddr(request), source, client_name, user);

                if(status.equalsIgnoreCase("0") || status.equalsIgnoreCase(""))
                {
                    return NseUtils.commonResponse(error_remark, HttpStatus.BAD_REQUEST);
                }

                return ResponseEntity.ok(regDataArray.toString());

            }catch (Exception ex)
            {
                logExceptionService.save(Integer.parseInt(userid), client_name, NseUtils.getFullRequestUrl(request), ex.getMessage(), request.getMethod(), NseUtils.getIpAddr(request), source);
                System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
                ex.printStackTrace();
                return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }catch (Exception ex)
        {
            logExceptionService.save(Integer.parseInt(userid), client_name, NseUtils.getFullRequestUrl(request), ex.getMessage(), request.getMethod(), NseUtils.getIpAddr(request), source);
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Initiate or Fetch Purchase Order Payment Details",
            description = "Retrieves or processes payment details for a specific purchase order. Accepts optional filters such as payment mode, client code, bank details, and UPI/VPA information. The API authenticates the user from the provided token and interacts with backend services to fetch or prepare payment instructions."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment information retrieved or processed successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SuccessResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Invalid or missing input, or user not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommonResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error - Failed to process the payment details",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommonResponse.class))
            )
    })
    @GetMapping("/getPurchaseOrdersPayment")
    public ResponseEntity<?> getPurchaseOrdersPayment(
            HttpServletRequest request,
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String payment_mode,
            @RequestParam(required = false) String client_code,
            @RequestParam(required = false) String order_id,
            @RequestParam(required = false) String mandate_id,
            @RequestParam(required = false) String bank_account_no,
            @RequestParam(required = false) String ifsc,
            @RequestParam(required = false) String cheque_no,
            @RequestParam(required = false) String cheque_date,
            @RequestParam(required = false) String vpa,
            @RequestParam(required = false) String callback_url,
            @RequestParam(required = false) String broker_code,
            @RequestParam(required = false) String source) throws Exception
    {
        String userid = "";
        String client_name = "";

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat sdf1 = new SimpleDateFormat("MM/dd/yyyy");

        try
        {
            payment_mode = NseUtils.checkParem(payment_mode);
            client_code = NseUtils.checkParem(client_code);
            order_id = NseUtils.checkParem(order_id);
            mandate_id = NseUtils.checkParem(mandate_id);
            bank_account_no = NseUtils.checkParem(bank_account_no);
            ifsc = NseUtils.checkParem(ifsc);
            cheque_no = NseUtils.checkParem(cheque_no);
            cheque_date = NseUtils.checkParem(cheque_date);
            vpa = NseUtils.checkParem(vpa);
            callback_url = NseUtils.checkParem(callback_url);
            broker_code = NseUtils.checkParem(broker_code);
            source = NseUtils.checkParem(source);

            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);

            UserDto user = userServiceClient.getUserById(Integer.valueOf(userid), token);

            if(user == null)
            {
                return NseUtils.commonResponse("User not found, please login again.", HttpStatus.BAD_REQUEST);
            }

            client_name = user.getClient_name();

            JSONObject requestDetails = new JSONObject();
            requestDetails.put("payment_mode", payment_mode);
            requestDetails.put("client_code", client_code);
            requestDetails.put("order_ids", order_id);

            if (payment_mode.equalsIgnoreCase("MANDATE"))
            {
                requestDetails.put("mandate_id", mandate_id);

            } else if (payment_mode.equalsIgnoreCase("CHEQUE"))
            {
                requestDetails.put("bank_account_no", bank_account_no);
                requestDetails.put("ifsc", ifsc);
                requestDetails.put("cheque_no", cheque_no);
                requestDetails.put("cheque_date", cheque_date);
            } else if (payment_mode.equalsIgnoreCase("UPI"))
            {
                requestDetails.put("bank_account_no", bank_account_no);
                requestDetails.put("ifsc", ifsc);
                requestDetails.put("vpa", vpa);
            } else if (payment_mode.equalsIgnoreCase("NETBANKING"))
            {
                requestDetails.put("bank_account_no", bank_account_no);
                requestDetails.put("ifsc", ifsc);
                requestDetails.put("callback_url", callback_url);
            }

            BseNseKeyDto nsekey = userServiceClient.getByClientName(client_name, token);
            String broker_code1 = nsekey.getBrokerCode();


            if (broker_code1 == null) {broker_code1 = "";}


            if (broker_code.isEmpty())
            {
                broker_code = broker_code1;
            }

            BseNseOnlineAccessDto online_access = null;

            try
            {
                online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, broker_code,token);

            } catch (feign.FeignException.NotFound ex)
            {
                return NseUtils.commonResponse("NSE Online Credentials Not available. Please contact your RM", HttpStatus.BAD_REQUEST);
            }

            String nse_userid = NseUtils.trimOrEmpty(online_access.getNse_userid());
            String nse_memberid = NseUtils.trimOrEmpty(online_access.getNse_memberid());
            String nse_secret_key = NseUtils.trimOrEmpty(online_access.getNse_secret_key());
            String nse_license_key = NseUtils.trimOrEmpty(online_access.getNse_license_key());

            String base64Encoded = AESEncryptionUtilV2.base64EncodedAuth(nse_secret_key, nse_license_key, nse_userid);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("memberId", nse_memberid);
            headers.set("Authorization", "Basic " + base64Encoded);
            headers.set("User-Agent", "PostmanRuntime/7.43.3");
            headers.set("Accept-Encoding", "gzip, deflate, br");
            headers.set("Accept-Language", "en-US");
            headers.set("Connection", "keep-alive");
            headers.set("Referer", "");

            HttpEntity<String> entity = new HttpEntity<>(requestDetails.toString(), headers);

            String PurchaseOrdersPaymentApi_url = NseApiUrls.PurchaseOrdersPaymentApi_url;

            try
            {
                ResponseEntity<String> mandateResult = RestTemplateFactory.createRestTemplate().postForEntity(PurchaseOrdersPaymentApi_url, entity, String.class);
                String statusCode = mandateResult.getStatusCode().toString();
                String responseBody = mandateResult.getBody().toString();

                System.out.println("purchaseOrdersPaymentApi::statusCode: " + statusCode);
                System.out.println("purchaseOrdersPaymentApi::responseBody: " + responseBody);

                JSONObject jsonObject = new JSONObject(responseBody);

                NseTransactions nsetrans = new NseTransactions();
                nsetrans.setUrl(PurchaseOrdersPaymentApi_url);
                nsetrans.setNse_request(requestDetails.toString());
                nsetrans.setNse_response(responseBody);
                nsetrans.setReturn_msg(statusCode);
                nsetrans.setService_return_code(statusCode);
                nsetrans.setService_msg(statusCode);
                nsetrans.setReg_id("");
                nsetrans.setPayment_link("");
                nsetrans.setPan("");
                nsetrans.setName("");
                nsetrans.setBranch(user.getBranch());
                nsetrans.setRm_name(user.getRm_name());
                nsetrans.setSubbroker_name(user.getSubbroker_name());
                nsetrans.setClient_name(client_name);
                nsetrans.setIin_number("");
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
                nsetrans.setTransaction_type("Purchase Order Payment Report");
                nsetrans.setTransaction_status("");
                nsetrans.setPayment_status("");
                nsetrans.setActive_ceased_status("");
                nsetrans.setRemarks(statusCode);
                nsetrans.setMandate_id("");
                nsetrans.setMandate_status("");
                nsetrans.setEmandate_auth_flag("");
                nsetrans.setApp_received_flag("");
                nsetrans.setTransaction_date(new Date());
                nsetrans.setUser_id(Integer.parseInt(userid));
                if (source.equalsIgnoreCase("Mobile")) {
                    nsetrans.setRegister_source("Mobile App");
                } else {
                    nsetrans.setRegister_source("Website");
                }

                nsetrans.setBroker_code("");
                nsetrans.setEuin_number("");
                nsetrans.setCc_received("");
                nsetrans.setFund_trans_to_amc("");
                nsetrans.setRefund_status("");
                nsetrans.setRefund_amount("");
                nseTransactionService.save(nsetrans);

                nseLogService.saveLog("Fetch Purchase Order Payment Report", "Fetch Purchase Order Payment Report", NseUtils.buildLogMessage("Fetch Purchase Order Payment Report", user, request), NseUtils.getIpAddr(request), source, client_name, user);

                if (!statusCode.equalsIgnoreCase("200"))
                {
                    return NseUtils.commonResponse("Error occurred while fetching user details", HttpStatus.BAD_REQUEST);
                }

                return ResponseEntity.ok(jsonObject.toString());

            } catch (Exception ex)
            {
                logExceptionService.save(Integer.parseInt(userid), client_name, NseUtils.getFullRequestUrl(request), ex.getMessage(), request.getMethod(), NseUtils.getIpAddr(request), source);
                System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
                ex.printStackTrace();
                return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception ex)
        {
            logExceptionService.save(Integer.parseInt(userid), client_name, NseUtils.getFullRequestUrl(request), ex.getMessage(), request.getMethod(), NseUtils.getIpAddr(request), source);
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get Order Status Report",
            description = "Fetches the order status report for the given filters such as transaction type, report status type, date range, broker code, and source. If no date is provided, defaults to the last 7 days."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully fetched the order status report",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - missing or invalid parameters",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @GetMapping("/getOrderStatusReport")
    public ResponseEntity<?> getOrderStatusReport(
            HttpServletRequest request,
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String trans_type,
            @RequestParam(required = false) String report_status_type,
            @RequestParam(required = false) String from_date,
            @RequestParam(required = false) String to_date,
            @RequestParam(required = false) String broker_code,
            @RequestParam(required = false) String source) throws Exception {

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat sdf1 = new SimpleDateFormat("MM/dd/yyyy");
        String userid = "";
        String client_name = "";
        try
        {
            trans_type = NseUtils.checkParem(trans_type);
            report_status_type = NseUtils.checkParem(report_status_type);
            from_date = NseUtils.checkParem(from_date);
            to_date = NseUtils.checkParem(to_date);
            broker_code = NseUtils.checkParem(broker_code);
            source = NseUtils.checkParem(source);

            if(report_status_type.isEmpty()){report_status_type = "Order Status Report";}
            if(trans_type.isEmpty()){trans_type = "ALL";}

            if(from_date.isEmpty() || to_date.isEmpty())
            {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                LocalDate today = LocalDate.now();
                LocalDate fromDate = today.minusDays(6);

                from_date = fromDate.format(formatter);
                to_date = today.format(formatter);
            }

            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);

            UserDto user = userServiceClient.getUserById(Integer.valueOf(userid), token);

            if(user == null)
            {
                return NseUtils.commonResponse("User not found, please login again.", HttpStatus.BAD_REQUEST);
            }

            client_name = user.getClient_name();

            String login_userid = String.valueOf(user.getId());
            String login_name = user.getFirst_name();
            String login_mobile = user.getMobile();

            RestTemplate restTemplate = RestTemplateFactory.createRestTemplate();
            try
            {
                JSONObject orderRequest = new JSONObject();
                orderRequest.put("from_date", from_date);
                orderRequest.put("to_date", to_date);
                orderRequest.put("trans_type", trans_type);
                orderRequest.put("order_type", "ALL");
                orderRequest.put("sub_order_type", "ALL");
                orderRequest.put("client_code", "");
                orderRequest.put("order_status", "All");
                orderRequest.put("settlement_type", "ALL");
                orderRequest.put("order_ids", "");
                orderRequest.put("member_unique_ids", "");

                System.out.println(orderRequest.getString("from_date"));
                System.out.println(orderRequest.getString("to_date"));

                BseNseKeyDto nsekey = userServiceClient.getByClientName(client_name, token);
                String broker_code1 = nsekey.getBrokerCode();

                if(broker_code1 == null) {broker_code1 = "";}

                if(broker_code.isEmpty())
                {
                    broker_code = broker_code1;
                }

                BseNseOnlineAccessDto online_access = null;

                try
                {
                    online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, broker_code,token);

                } catch (feign.FeignException.NotFound ex)
                {
                    return NseUtils.commonResponse("NSE Online Credentials Not available. Please contact your RM", HttpStatus.BAD_REQUEST);
                }

                String nse_userid = NseUtils.trimOrEmpty(online_access.getNse_userid());
                String nse_memberid = NseUtils.trimOrEmpty(online_access.getNse_memberid());
                String nse_secret_key = NseUtils.trimOrEmpty(online_access.getNse_secret_key());
                String nse_license_key = NseUtils.trimOrEmpty(online_access.getNse_license_key());

                String base64Encoded = AESEncryptionUtilV2.base64EncodedAuth(nse_secret_key, nse_license_key, nse_userid);
                System.out.println("orderStatusReportApi::requestBody: " + orderRequest.toString());
                System.out.println("orderStatusReportApi::authorization: " + base64Encoded);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("memberId", nse_memberid);
                headers.set("Authorization", "Basic "+base64Encoded);
                headers.set("User-Agent", "PostmanRuntime/7.43.3");
                headers.set("Accept-Encoding", "gzip, deflate, br");
                headers.set("Accept-Language", "en-US");
                headers.set("Connection", "keep-alive");
                headers.set("Referer", "");

                HttpEntity<String> entity = new HttpEntity<>(orderRequest.toString(), headers);

                System.out.println("report_status_type = " + report_status_type);
                String orderStatus_url= "";
                if(report_status_type.equalsIgnoreCase("Order Status Report"))
                {
                    orderStatus_url= NseApiUrls.nse_order_status;
                }
                else
                {
                    orderStatus_url= NseApiUrls.nse_preview_order;
                }

                ResponseEntity<String> orderStatusresponse = restTemplate.postForEntity(orderStatus_url, entity, String.class);
                System.out.println("orderStatusReportApi::Response Code: " + orderStatusresponse.getStatusCode());
                System.out.println("orderStatusReportApi::Response Body: " + orderStatusresponse.getBody());

                JSONObject jsonResponse = new JSONObject(orderStatusresponse.getBody());
                String responseStatus = jsonResponse.optString("response_status");
                String error_remark = jsonResponse.optString("error_remark");

                NseTransactions nsetrans = new NseTransactions();
                nsetrans.setUrl(orderStatus_url);
                nsetrans.setNse_request(orderRequest.toString());
                nsetrans.setNse_response(jsonResponse.toString());
                nsetrans.setReturn_msg(responseStatus);
                nsetrans.setService_return_code(responseStatus);
                nsetrans.setService_msg(responseStatus);
                nsetrans.setReg_id("");
                nsetrans.setPayment_link("");
                nsetrans.setPan("");
                nsetrans.setName("");
                nsetrans.setBranch(user.getBranch());
                nsetrans.setRm_name(user.getRm_name());
                nsetrans.setSubbroker_name(user.getSubbroker_name());
                nsetrans.setClient_name(client_name);
                nsetrans.setIin_number("");
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
                nsetrans.setTransaction_type("Get Order Status Report");
                nsetrans.setTransaction_status("");
                nsetrans.setPayment_status("");
                nsetrans.setActive_ceased_status("");
                nsetrans.setRemarks(responseStatus);
                nsetrans.setMandate_id("");
                nsetrans.setMandate_status("");
                nsetrans.setEmandate_auth_flag("");
                nsetrans.setApp_received_flag("");
                nsetrans.setTransaction_date(new Date());
                nsetrans.setUser_id(Integer.parseInt(userid));
                if (source.equalsIgnoreCase("Mobile")) {
                    nsetrans.setRegister_source("Mobile App");
                } else {
                    nsetrans.setRegister_source("Website");
                }

                nsetrans.setBroker_code("");
                nsetrans.setEuin_number("");
                nsetrans.setCc_received("");
                nsetrans.setFund_trans_to_amc("");
                nsetrans.setRefund_status("");
                nsetrans.setRefund_amount("");
                nseTransactionService.save(nsetrans);

                nseLogService.saveLog("Get Order Status Report", "Get Order Status Report", NseUtils.buildLogMessage("Get Order Status Report", user, request), NseUtils.getIpAddr(request), source, client_name, user);

                String report_data_json = "[]";

                if (!responseStatus.equalsIgnoreCase("S"))
                {
                    return NseUtils.commonResponse(error_remark, HttpStatus.BAD_REQUEST);
                }

                JSONArray reportDataArray = jsonResponse.optJSONArray("report_data");

                if (reportDataArray != null)
                {
                    report_data_json = reportDataArray.toString().replace("'", "\\'");
                }

                return ResponseEntity.ok(report_data_json);

            }catch (Exception ex)
            {
                logExceptionService.save(Integer.parseInt(userid), client_name, NseUtils.getFullRequestUrl(request), ex.getMessage(), request.getMethod(), NseUtils.getIpAddr(request), source);
                System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
                ex.printStackTrace();
                return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception ex)
        {
            logExceptionService.save(Integer.parseInt(userid), client_name, NseUtils.getFullRequestUrl(request), ex.getMessage(), request.getMethod(), NseUtils.getIpAddr(request), source);
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Resend Communication Email",
            description = "Resends the communication email for a given transaction number, client code, and product type. If broker code or source is not provided, default values are used. Requires a valid authorization token."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Email successfully resent",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(value = "{\"statusCode\":200,\"message\":\"Success\",\"data\":\"Email resent successfully\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or missing user",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(value = "{\"statusCode\":400,\"message\":\"Bad Request\",\"data\":\"User not found or invalid input\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(value = "{\"statusCode\":500,\"message\":\"Internal Server Error\",\"data\":\"An error occurred while processing the request\"}")
                    )
            )
    })
    @GetMapping("/resendCommunicationEmail")
    public ResponseEntity<?> resendCommunicationEmail(
            HttpServletRequest request,
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String client_code,
            @RequestParam(required = false) String product_type,
            @RequestParam(required = false) String transaction_number,
            @RequestParam(required = false) String broker_code,
            @RequestParam(required = false) String source) throws Exception {

        String userid = "";
        String client_name = "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat sdf1 = new SimpleDateFormat("MM/dd/yyyy");

        try
        {
            client_code = NseUtils.checkParem(client_code);
            product_type = NseUtils.checkParem(product_type);
            transaction_number = NseUtils.checkParem(transaction_number);
            broker_code = NseUtils.checkParem(broker_code);
            source = NseUtils.checkParem(source);

            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);

            UserDto user = userServiceClient.getUserById(Integer.valueOf(userid), token);

            if(user == null)
            {
                return NseUtils.commonResponse("User not found, Please try again.", HttpStatus.BAD_REQUEST);
            }

            client_name = user.getClient_name();

            String login_userid = String.valueOf(user.getId());
            String login_name = user.getFirst_name();
            String login_mobile = user.getMobile();

            RestTemplate restTemplate = RestTemplateFactory.createRestTemplate();
            try
            {
                JSONObject requestDetails = new JSONObject();
                requestDetails.put("productType", product_type);
                requestDetails.put("productRefId", transaction_number);

                BseNseKeyDto nsekey = userServiceClient.getByClientName(client_name, token);
                String broker_code1 = nsekey.getBrokerCode();

                if(broker_code1 == null) {broker_code1 = "";}

                if(broker_code.isEmpty())
                {
                    broker_code = broker_code1;
                }

                BseNseOnlineAccessDto online_access = null;

                try
                {
                    online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, broker_code,token);

                } catch (feign.FeignException.NotFound ex)
                {
                    return NseUtils.commonResponse("NSE Online Credentials Not available. Please contact your RM", HttpStatus.BAD_REQUEST);
                }

                String nse_userid = NseUtils.trimOrEmpty(online_access.getNse_userid());
                String nse_memberid = NseUtils.trimOrEmpty(online_access.getNse_memberid());
                String nse_secret_key = NseUtils.trimOrEmpty(online_access.getNse_secret_key());
                String nse_license_key = NseUtils.trimOrEmpty(online_access.getNse_license_key());

                String base64Encoded = AESEncryptionUtilV2.base64EncodedAuth(nse_secret_key, nse_license_key, nse_userid);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("memberId", nse_memberid);
                headers.set("Authorization", "Basic " + base64Encoded);
                headers.set("User-Agent", "PostmanRuntime/7.43.3");
                headers.set("Accept-Encoding", "gzip, deflate, br");
                headers.set("Accept-Language", "en-US");
                headers.set("Connection", "keep-alive");
                headers.set("Referer", "");

                HttpEntity<String> entity = new HttpEntity<>(requestDetails.toString(), headers);

                String resendEmail_url = NseApiUrls.resendEmail_url;

                ResponseEntity<String> resendResult = RestTemplateFactory.createRestTemplate().postForEntity(resendEmail_url, entity, String.class);
                String statusCode = resendResult.getStatusCode().toString();
                String responseBody = resendResult.getBody().toString();

                System.out.println("statusCode = " + statusCode);
                System.out.println("responseBody = " + responseBody);

                JSONObject jsonObject = new JSONObject(responseBody);

                String response_status = jsonObject.getString("response_status");
                String error_remark = jsonObject.getString("error_remark");

                NseTransactions nsetrans = new NseTransactions();
                nsetrans.setUrl(resendEmail_url);
                nsetrans.setNse_request(requestDetails.toString());
                nsetrans.setNse_response(responseBody);
                nsetrans.setReturn_msg(statusCode);
                nsetrans.setService_return_code(statusCode);
                nsetrans.setService_msg(statusCode);
                nsetrans.setReg_id("");
                nsetrans.setPayment_link("");
                nsetrans.setPan("");
                nsetrans.setName("");
                nsetrans.setBranch(user.getBranch());
                nsetrans.setRm_name(user.getRm_name());
                nsetrans.setSubbroker_name(user.getSubbroker_name());
                nsetrans.setClient_name(client_name);
                nsetrans.setIin_number("");
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
                nsetrans.setTransaction_type("Resend Communication Email");
                nsetrans.setTransaction_status("");
                nsetrans.setPayment_status("");
                nsetrans.setActive_ceased_status("");
                nsetrans.setRemarks(statusCode);
                nsetrans.setMandate_id("");
                nsetrans.setMandate_status("");
                nsetrans.setEmandate_auth_flag("");
                nsetrans.setApp_received_flag("");
                nsetrans.setTransaction_date(new Date());
                nsetrans.setUser_id(Integer.parseInt(userid));
                if (source.equalsIgnoreCase("Mobile")) {
                    nsetrans.setRegister_source("Mobile App");
                } else {
                    nsetrans.setRegister_source("Website");
                }

                nsetrans.setBroker_code("");
                nsetrans.setEuin_number("");
                nsetrans.setCc_received("");
                nsetrans.setFund_trans_to_amc("");
                nsetrans.setRefund_status("");
                nsetrans.setRefund_amount("");
                nseTransactionService.save(nsetrans);

                nseLogService.saveLog("Resend Communication Email", "Resend Communication Email", NseUtils.buildLogMessage("Resend Communication Email", user, request), NseUtils.getIpAddr(request), source, client_name, user);

                if(!response_status.equalsIgnoreCase("S"))
                {
                    return NseUtils.commonResponse(error_remark, HttpStatus.BAD_REQUEST);
                }

                return NseUtils.commonResponse("Email sent Successfully", HttpStatus.OK);
            }catch (Exception ex)
            {
                logExceptionService.save(Integer.parseInt(userid), client_name, NseUtils.getFullRequestUrl(request), ex.getMessage(), request.getMethod(), NseUtils.getIpAddr(request), source);
                System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
                ex.printStackTrace();
                return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception ex)
        {
            logExceptionService.save(Integer.parseInt(userid), client_name, NseUtils.getFullRequestUrl(request), ex.getMessage(), request.getMethod(), NseUtils.getIpAddr(request), source);
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Fetch Mandate Report",
            description = "Fetches the mandate report based on PAN/IIN, date range, and broker code. This API is secured and requires a valid Authorization token."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Mandate report fetched successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(value = "{\"status\":200,\"message\":\"OK\",\"data\":{...}}"))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request due to missing/invalid parameters or user not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = "{\"status\":400,\"message\":\"Bad Request\",\"data\":\"User not found\"}"))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = "{\"status\":500,\"message\":\"Internal Server Error\",\"data\":\"An unexpected error occurred\"}"))
            )
    })
    @GetMapping("/getMandateReport")
    public ResponseEntity<?> getMandateReport(
            HttpServletRequest request,
            @RequestHeader("Authorization") String token,
            @RequestParam String name_pan_id,
            @RequestParam String from_date,
            @RequestParam String to_date,
            @RequestParam String iin_number,
            @RequestParam String source,
            @RequestParam String broker_code) throws Exception
    {

        String userid = "";
        String client_name = "";
        try
        {
            name_pan_id = NseUtils.checkParem(name_pan_id);
            from_date = NseUtils.checkParem(from_date);
            to_date = NseUtils.checkParem(to_date);
            iin_number = NseUtils.checkParem(iin_number);
            source = NseUtils.checkParem(source);
            broker_code = NseUtils.checkParem(broker_code);

            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);

            UserDto user = userServiceClient.getUserById(Integer.valueOf(userid), token);

            if (user == null)
            {
                return NseUtils.commonResponse("User not found, please login again.", HttpStatus.BAD_REQUEST);
            }

            client_name = user.getClient_name();

            RestTemplate restTemplate = RestTemplateFactory.createRestTemplate();

            JSONObject requestDetails = new JSONObject();
            requestDetails.put("client_code", iin_number);
            requestDetails.put("from_date", from_date);
            requestDetails.put("to_date", to_date);
            requestDetails.put("mandate_id", "");
            requestDetails.put("memberMandateIds", "");

            BseNseKeyDto nsekey = userServiceClient.getByClientName(client_name,token);
            String broker_code1 = nsekey.getBrokerCode();


            if(broker_code1 == null) {broker_code1 = "";}

            if(broker_code.isEmpty())
            {
                broker_code = broker_code1;
            }

            BseNseOnlineAccessDto online_access = null;

            try
            {
                online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, broker_code,token);

            } catch (feign.FeignException.NotFound ex)
            {
                return NseUtils.commonResponse("NSE Online Credentials Not available. Please contact your RM", HttpStatus.BAD_REQUEST);
            }

            String nse_userid = NseUtils.trimOrEmpty(online_access.getNse_userid());
            String nse_memberid = NseUtils.trimOrEmpty(online_access.getNse_memberid());
            String nse_secret_key = NseUtils.trimOrEmpty(online_access.getNse_secret_key());
            String nse_license_key = NseUtils.trimOrEmpty(online_access.getNse_license_key());

            String base64Encoded = AESEncryptionUtilV2.base64EncodedAuth(nse_secret_key, nse_license_key, nse_userid);
            System.out.println("MandateStatusApi::requestBody: " + requestDetails.toString());
            System.out.println("MandateStatusApi::authorization: " + base64Encoded);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("memberId", nse_memberid);
            headers.set("Authorization", "Basic "+base64Encoded);
            headers.set("User-Agent", "PostmanRuntime/7.43.3");
            headers.set("Accept-Encoding", "gzip, deflate, br");
            headers.set("Accept-Language", "en-US");
            headers.set("Connection", "keep-alive");
            headers.set("Referer", "");

            HttpEntity<String> entity = new HttpEntity<>(requestDetails.toString(), headers);

            String mandateStatusReport_url = NseApiUrls.mandateStatusReport_url;
            ResponseEntity<String> mandateStatusResponse = restTemplate.postForEntity(mandateStatusReport_url, entity, String.class);
            System.out.println("MandateStatusApi::Response Code: " + mandateStatusResponse.getStatusCode());
            System.out.println("MandateStatusApi::Response Body: " + mandateStatusResponse.getBody());

            JSONObject jsonResponse = new JSONObject(mandateStatusResponse.getBody());
            String responseStatus = jsonResponse.optString("response_status");
            String error_remark = jsonResponse.optString("error_remark");

            NseTransactions nsetrans = new NseTransactions();
            nsetrans.setUrl(mandateStatusReport_url);
            nsetrans.setNse_request(requestDetails.toString());
            nsetrans.setNse_response(mandateStatusResponse.toString());
            nsetrans.setReturn_msg(responseStatus);
            nsetrans.setService_return_code(responseStatus);
            nsetrans.setService_msg(jsonResponse.toString());
            nsetrans.setReg_id("");
            nsetrans.setPayment_link("");
            nsetrans.setPan("");
            nsetrans.setName("");
            nsetrans.setBranch(user.getBranch());
            nsetrans.setRm_name(user.getRm_name());
            nsetrans.setSubbroker_name(user.getSubbroker_name());
            nsetrans.setClient_name(client_name);
            nsetrans.setIin_number("");
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
            nsetrans.setTransaction_type("Fetch Mandate Report");
            nsetrans.setTransaction_status("");
            nsetrans.setPayment_status("");
            nsetrans.setActive_ceased_status("");
            nsetrans.setRemarks(responseStatus);
            nsetrans.setMandate_id("");
            nsetrans.setMandate_status("");
            nsetrans.setEmandate_auth_flag("");
            nsetrans.setApp_received_flag("");
            nsetrans.setTransaction_date(new Date());
            nsetrans.setUser_id(Integer.parseInt(userid));

            if (source.equalsIgnoreCase("Mobile"))
            {
                nsetrans.setRegister_source("Mobile App");
            } else {
                nsetrans.setRegister_source("Website");
            }

            nsetrans.setBroker_code("");
            nsetrans.setEuin_number("");
            nsetrans.setCc_received("");
            nsetrans.setFund_trans_to_amc("");
            nsetrans.setRefund_status("");
            nsetrans.setRefund_amount("");
            nseTransactionService.save(nsetrans);

            nseLogService.saveLog("Fetch Mandate Report", "Fetch Mandate Report", NseUtils.buildLogMessage("Fetch Mandate Report", user, request), NseUtils.getIpAddr(request), source, client_name, user);

            String report_data_json = "[]";

            if (!responseStatus.equalsIgnoreCase("S"))
            {
                return NseUtils.commonResponse(error_remark, HttpStatus.BAD_REQUEST);
            }

            JSONArray reportDataArray = jsonResponse.optJSONArray("report_data");

            if (reportDataArray != null)
            {
                report_data_json = reportDataArray.toString().replace("'", "\\'");
            }

            return ResponseEntity.ok(report_data_json);

        } catch (Exception ex)
        {
            logExceptionService.save(Integer.parseInt(userid), client_name, NseUtils.getFullRequestUrl(request), ex.getMessage(), request.getMethod(), NseUtils.getIpAddr(request), source);
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Scheme Master & NAV Download API",
            description = "Scheme Master & NAV Download API Possible Values : \n" +
                    "SCH = Consolidated Scheme master \n" +
                    "SIP = SIP Scheme Master, \n" +
                    "STP = STP Scheme Master, \n" +
                    "SWP = SWP Scheme Master, \n" +
                    "NAV = NAV Download"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "uploaded successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Exception Return",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "{ \"status\": 400, \"message\": \"Invalid parameters\" }"))
            )
    })
    @GetMapping("/masterDownloadApi")
    public ResponseEntity<?> masterDownload(@RequestHeader("Authorization") String token, @RequestParam(required = false) String fileType ) throws Exception {
        try
        {
            if(!TokenInterceptor.isValidToken(token, secretKey)){
                return NseUtils.commonResponse("Token not valid or expired!", HttpStatus.UNAUTHORIZED);
            }
            String userid = TokenInterceptor.extractAdminIdFromToken(token, secretKey);

            UserDto user = userServiceClient.getUserById(Integer.valueOf(userid), token);

            if(user == null)
            {
                return NseUtils.commonResponse("User not found, please login again.", HttpStatus.BAD_REQUEST);
            }
            System.out.println("user = " + user);
            String client_name = user.getClient_name();

            RestTemplate restTemplate = RestTemplateFactory.createRestTemplate();
            try
            {
                JSONObject downloadMaster = new JSONObject();
                downloadMaster.put("file_type", fileType);
                BseNseOnlineAccessDto online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, user.getBroker_code(),token);
                if(online_access == null)
                {
                    return NseUtils.commonResponse("NSE Online Credentials Not available. Please contact your RM", HttpStatus.BAD_REQUEST);
                }
                String nse_userid = NseUtils.trimOrEmpty(online_access.getNse_userid());
                String nse_memberid = NseUtils.trimOrEmpty(online_access.getNse_memberid());
                String nse_secret_key = NseUtils.trimOrEmpty(online_access.getNse_secret_key());
                String nse_license_key = NseUtils.trimOrEmpty(online_access.getNse_license_key());

                String base64Encoded = AESEncryptionUtilV2.base64EncodedAuth(nse_secret_key, nse_license_key, nse_userid);
                System.out.println("masterDownload::requestBody: " + downloadMaster);
                System.out.println("masterDownload::authorization: " + base64Encoded);

                HttpHeaders headers = NseUtils.getHttpHeaders(nse_memberid, base64Encoded);

                HttpEntity<String> entity = new HttpEntity<>(downloadMaster.toString(), headers);

                String MASTER_DOWNLOAD_URL= NseApiUrls.MASTER_DOWNLOAD_URL;

                ResponseEntity<byte[]> masterDownloadResponse = restTemplate.postForEntity(MASTER_DOWNLOAD_URL, entity, byte[].class);
                String fileName;
                String contentDisposition = masterDownloadResponse.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);

                if (contentDisposition != null && contentDisposition.contains("filename="))
                {
                    fileName = contentDisposition.split("filename=")[1].replace("\"", "").trim();
                    if(fileName.endsWith(";")) {
                        fileName = fileName.substring(0, fileName.length() - 1);
                    }
                } else {
                    fileName = "NSE_NSEINVEST_" + fileType + "_" + LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")) + ".txt";
                }
                File file = new File(fileName);
                Files.write(file.toPath(), Objects.requireNonNull(masterDownloadResponse.getBody()));

                System.out.println("Downloaded file: " + file.getAbsolutePath());

                List<String> error_list = new ArrayList<>();

                if(fileType.equalsIgnoreCase("SCH"))
                {
                    error_list = nseAmfiOnlineSchemeMaster.uploadNSEOnlineSchemeMasterPhy(file,token);
                }
                else if(fileType.equalsIgnoreCase("SIP"))
                {
                    error_list = nseAmfiOnlineSchemeMaster.uploadNSEOnlineSchemeMasterSip(file,token);
                }
                else if(fileType.equalsIgnoreCase("STP"))
                {
                    error_list = nseAmfiOnlineSchemeMaster.uploadNSEOnlineSchemeMasterStp(file,token);
                }else if(fileType.equalsIgnoreCase("SWP"))
                {
                    error_list = nseAmfiOnlineSchemeMaster.uploadNSEOnlineSchemeMasterSwp(file,token);
                }
                return NseUtils.commonResponse(error_list.toString(), HttpStatus.OK);

            }catch (Exception ex)
            {
                ex.printStackTrace();
                return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getShortURLLinkAPI")
    public ResponseEntity<?> getShortURLLinkAPI(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = true) String broker_code,
            @RequestParam(required = false) String productRefId,
            @RequestParam(required = false) String productType,
            @RequestParam(required = false) String source) throws Exception
    {
        CommonResponse commonResponse = new CommonResponse();
        try
        {
            String client_name = TokenInterceptor.extractClientNamedFromToken(token,secretKey);
            productRefId = NseUtils.checkParem(productRefId);
            productType = NseUtils.checkParem(productType);
            broker_code = NseUtils.checkParem(broker_code);


            BseNseOnlineAccessDto online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, broker_code,token);
            if(online_access == null)
            {
                return NseUtils.commonResponse("NSE Online Credentials Not available. Please contact your RM", HttpStatus.BAD_REQUEST);
            }
            String nse_userid = NseUtils.trimOrEmpty(online_access.getNse_userid());
            String nse_memberid = NseUtils.trimOrEmpty(online_access.getNse_memberid());
            String nse_secret_key = NseUtils.trimOrEmpty(online_access.getNse_secret_key());
            String nse_license_key = NseUtils.trimOrEmpty(online_access.getNse_license_key());

            JSONObject requestBody = new JSONObject();
            requestBody.put("productType", productType);
            requestBody.put("productRefId", productRefId);

            String base64Encoded = AESEncryptionUtilV2.base64EncodedAuth(nse_secret_key, nse_license_key, nse_userid);
            System.out.println("orderStatusReportApi::requestBody: " + requestBody.toString());
            System.out.println("orderStatusReportApi::authorization: " + base64Encoded);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("memberId", nse_memberid);
            headers.set("Authorization", "Basic " + base64Encoded);
            headers.set("User-Agent", "PostmanRuntime/7.43.3");
            headers.set("Accept-Encoding", "gzip, deflate, br");
            headers.set("Accept-Language", "en-US");
            headers.set("Connection", "keep-alive");
            headers.set("Referer", "");

            RestTemplate restTemplate = RestTemplateFactory.createRestTemplate();

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            String mandate_url = "https://www.nseinvest.com/nsemfdesk/api/v2/reports/GET_LINK";

            String firstHolderLink = "";
            String secondHolderLink = "";
            String thirdHolderLink = "";
            String errorMessage = "";

            try
            {
                ResponseEntity<String> mandateResult = restTemplate.postForEntity(mandate_url, entity,
                        String.class);
                String statusCode = mandateResult.getStatusCode().toString();
                String responseBody = mandateResult.getBody().toString();

                System.out.println("statusCode = " + statusCode);
                System.out.println("responseBody = " + responseBody);

                JSONObject jsonObject = new JSONObject(responseBody);
                firstHolderLink = jsonObject.optString("firstHolderLink", "");
                secondHolderLink = jsonObject.optString("secondHolderLink", "");
                thirdHolderLink = jsonObject.optString("thirdHolderLink", "");
                errorMessage = jsonObject.optString("errorMessage", "");

                System.out.println("firstHolderLink: " + firstHolderLink);
                System.out.println("secondHolderLink: " + secondHolderLink);
                System.out.println("thirdHolderLink: " + thirdHolderLink);
                System.out.println("errorMessage: " + errorMessage);

                if (errorMessage.isEmpty())
                {
                    commonResponse.setStatus(200);
                    commonResponse.setStatus_msg("Succcess");
                    commonResponse.setMessage(firstHolderLink);
                    return ResponseEntity.ok(commonResponse);
                } else
                {
                    commonResponse.setStatus(400);
                    commonResponse.setStatus_msg("Failure");
                    commonResponse.setMessage(errorMessage);
                    return ResponseEntity.ok(commonResponse);
                }
            }catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }catch (Exception ex)
        {
            ex.printStackTrace();
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return null;
    }

    @Operation(
            summary = "Order Lifecycle Report",
            description = "Fetches the order status report for the given filters such as transaction type, report status type, date range, broker code, and source. "
                    + "If no date is provided, defaults to the last 7 days."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully fetched the order status report",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - missing or invalid parameters",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "{ \"status\": 400, \"message\": \"Invalid parameters\" }"))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "{ \"status\": 500, \"message\": \"Something went wrong\" }"))
            )
    })

    @GetMapping("/order-lifecycle-report")
    public ResponseEntity<?> orderLifecycleReport(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String client_code,
            @RequestParam(required = false) String from_date,
            @RequestParam(required = false) String to_date,
            @RequestParam(required = true) String broker_code,
            @RequestParam(required = false) String product_id,
            @RequestParam(required = false) String product_type,
            @RequestParam(required = false) String source) throws Exception
    {

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat sdf1 = new SimpleDateFormat("MM/dd/yyyy");
        String userid = "";
        String client_name = "";
        try
        {
            System.out.println("clientName = " + client_name + "broker_code = " + broker_code);
            from_date = NseUtils.checkParem(from_date);
            to_date = NseUtils.checkParem(to_date);
            broker_code = NseUtils.checkParem(broker_code);
            client_code = NseUtils.checkParem(client_code);
            source = NseUtils.checkParem(source);

            if(from_date.isEmpty() || to_date.isEmpty())
            {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                LocalDate today = LocalDate.now();
                LocalDate fromDate = today.minusDays(2);

                from_date = fromDate.format(formatter);
                to_date = today.format(formatter);
            }

            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            client_name = TokenInterceptor.extractClientNamedFromToken(token, secretKey);

            RestTemplate restTemplate = RestTemplateFactory.createRestTemplate();
            try
            {
                JSONObject orderlifecycle = new JSONObject();

                orderlifecycle.put("from_date", from_date);
                orderlifecycle.put("to_date", to_date);
                if(!client_code.isEmpty())
                {
                    orderlifecycle.put("client_code", client_code);
                }else{
                    orderlifecycle.put("client_code", "");
                }
                if(!product_type.isEmpty())
                {
                    orderlifecycle.put("Product_type", product_type);
                }else{
                    orderlifecycle.put("Product_type", "");
                }
                if(!product_id.isEmpty())
                {
                    orderlifecycle.put("product_id", product_id);
                }else{
                    orderlifecycle.put("product_id", "");
                }


                System.out.println("clientName = " + client_name + "broker_code = " + broker_code);

                BseNseOnlineAccessDto online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, broker_code,token);
                if(online_access == null)
                {
                    return NseUtils.commonResponse("NSE Online Credentials Not available. Please contact your RM", HttpStatus.BAD_REQUEST);
                }
                String nse_userid = NseUtils.trimOrEmpty(online_access.getNse_userid());
                String nse_memberid = NseUtils.trimOrEmpty(online_access.getNse_memberid());
                String nse_secret_key = NseUtils.trimOrEmpty(online_access.getNse_secret_key());
                String nse_license_key = NseUtils.trimOrEmpty(online_access.getNse_license_key());

                String base64Encoded = AESEncryptionUtilV2.base64EncodedAuth(nse_secret_key, nse_license_key, nse_userid);
                System.out.println("orderStatusReportApi::requestBody: " + orderlifecycle.toString());
                System.out.println("orderStatusReportApi::authorization: " + base64Encoded);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("memberId", nse_memberid);
                headers.set("Authorization", "Basic "+base64Encoded);
                headers.set("User-Agent", "PostmanRuntime/7.43.3");
                headers.set("Accept-Encoding", "gzip, deflate, br");
                headers.set("Accept-Language", "en-US");
                headers.set("Connection", "keep-alive");
                headers.set("Referer", "");

                HttpEntity<String> entity = new HttpEntity<>(orderlifecycle.toString(), headers);

                String orderStatus_url= NseApiUrls.order_lifecycle;

                ResponseEntity<String> orderStatusresponse = restTemplate.postForEntity(orderStatus_url, entity, String.class);
                System.out.println("orderStatusReportApi::Response Code: " + orderStatusresponse.getStatusCode());

                JSONObject jsonResponse = new JSONObject(orderStatusresponse.getBody());
                String responseStatus = jsonResponse.optString("response_status");
                String error_remark = jsonResponse.optString("error_remark");

                JSONArray ordersArray = jsonResponse.optJSONArray("report_data");
                System.out.println("ordersArray = " + ordersArray);

                String report_data_json = "[]";

                if (!responseStatus.equalsIgnoreCase("S"))
                {
                    return NseUtils.commonResponse(error_remark, HttpStatus.BAD_REQUEST);
                }

                JSONArray reportDataArray = jsonResponse.optJSONArray("report_data");

                if (reportDataArray != null)
                {
                    report_data_json = reportDataArray.toString().replace("'", "\\'");
                }

                return ResponseEntity.ok(report_data_json);

            }catch (Exception ex)
            {
                System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
                ex.printStackTrace();
                return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @PostMapping(value = "/sendTransactionEmailRetrigger")
    public ResponseEntity<Object> sendTransactionEmailRetrigger(@RequestHeader("Authorization") String token,
                                                                @RequestParam String productType,
                                                                @RequestParam String productRefId,
                                                                @RequestParam String broker_code) throws Exception
    {
        try
        {
            String client_name = TokenInterceptor.extractClientNamedFromToken(token, secretKey);

            if(productRefId.isEmpty() || productType.isEmpty() || broker_code.isEmpty())
            {
                return NseUtils.commonResponse("Missing request", HttpStatus.BAD_REQUEST);
            }

            if (productType == null) {
                productType = "";
            }
            if (productRefId == null) {
                productRefId = "";
            }
            if (broker_code == null) {
                broker_code = "";
            }

            productType = productType.trim();
            productRefId = productRefId.trim();
            broker_code = broker_code.trim();

            String appln_id = "";
            String password = "";

            BseNseOnlineAccessDto online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, broker_code,token);
            if(online_access == null)
            {
                return NseUtils.commonResponse("NSE Online Credentials Not available. Please contact your RM", HttpStatus.BAD_REQUEST);
            }
            String nse_userid = NseUtils.trimOrEmpty(online_access.getNse_userid());
            String nse_memberid = NseUtils.trimOrEmpty(online_access.getNse_memberid());
            String nse_secret_key = NseUtils.trimOrEmpty(online_access.getNse_secret_key());
            String nse_license_key = NseUtils.trimOrEmpty(online_access.getNse_license_key());


            JSONObject requestBody = new JSONObject();
            requestBody.put("productType", productType);
            requestBody.put("productRefId", productRefId);

            String base64Encoded = AESEncryptionUtilV2.base64EncodedAuth(nse_secret_key, nse_license_key, nse_userid);
            System.out.println("orderStatusReportApi::requestBody: " + requestBody.toString());
            System.out.println("orderStatusReportApi::authorization: " + base64Encoded);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("memberId", nse_memberid);
            headers.set("Authorization", "Basic " + base64Encoded);
            headers.set("User-Agent", "PostmanRuntime/7.43.3");
            headers.set("Accept-Encoding", "gzip, deflate, br");
            headers.set("Accept-Language", "en-US");
            headers.set("Connection", "keep-alive");
            headers.set("Referer", "");

            System.out.println("headers = "+headers.toString());

            RestTemplate restTemplate = RestTemplateFactory.createRestTemplate();

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            String mandate_url = "https://www.nseinvest.com/nsemfdesk/api/v2/registration/RESEND_COMM";

            System.out.println("mandate_url: " + mandate_url);
            String firstHolderLink = "";
            String secondHolderLink = "";
            String thirdHolderLink = "";
            String errorMessage = "";

            try {
                ResponseEntity<String> Result = restTemplate.postForEntity(mandate_url, entity, String.class);
                String statusCode = Result.getStatusCode().toString();
                String responseBody = Result.getBody().toString();

                System.out.println("statusCode = " + statusCode);
                System.out.println("responseBody = " + responseBody);

                JSONObject json = new JSONObject(responseBody);
                String responseStatus = json.getString("responseStatus");
                String responseRemark = json.getString("responseRemark");

                System.out.println("responseStatus = "+responseStatus);
                System.out.println("responseRemark = "+responseRemark);

                if (responseStatus.equalsIgnoreCase("Success"))
                {
                    return NseUtils.commonResponse(responseRemark,HttpStatus.OK);

                } else
                {
                    return NseUtils.commonResponse(responseRemark,HttpStatus.BAD_REQUEST);
                }
            } catch (Exception ex) {

                ex.printStackTrace();
            }
        } catch (Exception ex)
        {
            ex.printStackTrace();
            return NseUtils.commonResponse("Something went wrong, please try again.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return null;
    }


    @GetMapping("/isSchemeValidForSipStepUp")
    public ResponseEntity<?> isSchemeValidForSipStepUp(@RequestHeader("Authorization") String token,  @RequestParam("scheme_name") String scheme_name,@RequestParam(required = false) String source)
    {
        try
        {
            if(!TokenInterceptor.isValidToken(token, secretKey)){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid token!");
            }

            source = NseUtils.checkParem(source);
            NseOnlineStepUpSchemeMaster nseOnlineStepUpSchemeMaster= nseAmfiOnlineSchemeMaster.isSchemeValidForSipStepUp(scheme_name);
            System.out.println("nseOnlineStepUpSchemeMaster size: " + new Gson().toJson(nseOnlineStepUpSchemeMaster));

            if(source.equalsIgnoreCase("mobile")) {
                if (nseOnlineStepUpSchemeMaster != null) {
                    return NseUtils.commonResponse("Y", HttpStatus.OK);
                } else {
                    return NseUtils.commonResponse("N", HttpStatus.OK);
                }
            }else{
                if(nseOnlineStepUpSchemeMaster!= null){
                    return ResponseEntity.ok(true);
                }else{
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(false);
                }
            }


        } catch (Exception ex){

            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }
}