package com.nse.controller;

import com.google.gson.Gson;
import com.nse.client.UserServiceClient;
import com.nse.config.TokenInterceptor;
import com.nse.dto.mf.*;
import com.nse.model.NseLogModel;
import com.nse.model.NseOnlineSchemeMaster;
import com.nse.model.NseTransactions;
import com.nse.repository.NseLogRepository;
import com.nse.repository.NseOnlineSchemeMasterRepository;
import com.nse.repository.NseTransactionRepository;
import com.nse.response.CommonResponse;
import com.nse.response.StatusMessage;
import com.nse.response.TransactionResponse;
import com.nse.services.NseTransactionService;
import com.nse.utils.*;
import feign.FeignException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.internal.util.StringHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@Tag(name = "NSE Transactions", description = "NSE Lumpsum Purchase APIs")
public class NseTransactionController {

    @Autowired
    UserServiceClient userServiceClient;

    @Autowired
    NseOnlineSchemeMasterRepository nseOnlineSchemeMasterRepository;

    @Autowired
    NseLogRepository nseLogRepository;

    final static String nseUrl = "https://www.nseinvest.com";

    @Autowired
    private NseTransactionService nseTransactionService;

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Autowired
    private NseTransactionRepository nseTransactionRepository;

    /**
     * Cart ids are only collected when the order originates from a cart (Mobile source, or
     * Website with a cartid). For direct Website orders the list stays empty, and the NSE
     * response can also carry more transaction_details rows than we sent carts for, so the
     * response index cannot be used blindly.
     */
    private static String cartIdAt(List<String> cart_id_array, int index)
    {
        if (cart_id_array == null || index < 0 || index >= cart_id_array.size())
        {
            return "";
        }
        String cart_id = cart_id_array.get(index);
        return cart_id == null ? "" : cart_id;
    }


    @Operation(
            summary = "Save Lumpsum Purchase",
            description = """
        Triggers a lumpsum mutual fund purchase transaction on the NSE platform.
        
        🔹 If `source = web`: All transaction and investor details are expected.
        🔹 If `source = mobile`: Only essential payment and investor info is needed.
        """,
            parameters = {
                    // --- WEB ONLY PARAMETERS ---
                    @Parameter(name = "amc_code", in = ParameterIn.QUERY, required = false, description = "Asset Management Company code (web only)."),
                    @Parameter(name = "scheme_name", in = ParameterIn.QUERY, required = false, description = "Mutual fund scheme name (web only)."),
                    @Parameter(name = "scheme_code", in = ParameterIn.QUERY, required = false, description = "Scheme code of the mutual fund (web only)."),
                    @Parameter(name = "reinvest_tag", in = ParameterIn.QUERY, required = false, description = "Reinvestment tag - Y or N (web only)."),
                    @Parameter(name = "amount", in = ParameterIn.QUERY, required = false, description = "Amount to invest in INR (web only)."),
                    @Parameter(name = "folio", in = ParameterIn.QUERY, required = false, description = "Investor's folio number (web only)."),
                    @Parameter(name = "count", in = ParameterIn.QUERY, required = false, description = "Transaction count (web only)."),
                    @Parameter(name = "total_amount", in = ParameterIn.QUERY, required = false, description = "Total amount for multi-scheme order (web only)."),
                    @Parameter(name = "goal_id", in = ParameterIn.QUERY, required = false, description = "Linked goal ID (web only)."),
                    @Parameter(name = "risk_profile", in = ParameterIn.QUERY, required = false, description = "Investor risk profile (web only)."),
                    @Parameter(name = "purchase_type", in = ParameterIn.QUERY, required = false, description = "Purchase type: Lumpsum, Additional, etc. (web only)."),
                    @Parameter(name = "sub_trxn_type", in = ParameterIn.QUERY, required = false, description = "Sub-transaction type as per NSE (web only)."),
                    @Parameter(name = "buy_sell_type", in = ParameterIn.QUERY, required = false, description = "Buy/Sell indicator (web only)."),

                    // --- COMMON PARAMETERS FOR BOTH WEB & MOBILE ---
                    @Parameter(name = "payment_type", in = ParameterIn.QUERY, required = false, description = "Payment type (e.g., UPI, NEFT) (web & mobile)."),
                    @Parameter(name = "payment_mode", in = ParameterIn.QUERY, required = false, description = "Payment mode (online/offline) (web & mobile)."),
                    @Parameter(name = "umrn_code", in = ParameterIn.QUERY, required = false, description = "UMRN mandate reference code (web & mobile)."),
                    @Parameter(name = "bank_account_number", in = ParameterIn.QUERY, required = false, description = "Bank account number used (web & mobile)."),
                    @Parameter(name = "iin_number", in = ParameterIn.QUERY, required = false, description = "Investor Identification Number (IIN) (web & mobile)."),
                    @Parameter(name = "cheque_no", in = ParameterIn.QUERY, required = false, description = "Cheque number if cheque used (web & mobile)."),
                    @Parameter(name = "cheque_date", in = ParameterIn.QUERY, required = false, description = "Cheque date in YYYY-MM-DD (web & mobile)."),
                    @Parameter(name = "dd_charge", in = ParameterIn.QUERY, required = false, description = "Demand draft charges (web & mobile)."),
                    @Parameter(name = "broker_code", in = ParameterIn.QUERY, required = false, description = "Broker/Distributor code (web & mobile)."),
                    @Parameter(name = "euin_code", in = ParameterIn.QUERY, required = false, description = "EUIN (Employee Unique Identification Number) (web & mobile)."),

                    // --- MANDATORY FOR BOTH ---
                    @Parameter(name = "source", in = ParameterIn.QUERY, required = true, description = "Request source: 'web' or 'mobile'. Determines required parameters."),
                    @Parameter(name = "Authorization", in = ParameterIn.HEADER, required = true, description = "Bearer token for authentication.")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Order successfully triggered"),
                    @ApiResponse(
                            responseCode = "200",
                            description = "Order successfully triggered",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TransactionResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request or missing data",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CommonResponse.class),
                                    examples = @ExampleObject(
                                            value = "{ \"status\": 400, \"error\": \"Bad Request\", \"message\": \"User not found\" }"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CommonResponse.class),
                                    examples = @ExampleObject(
                                            value = "{ \"status\": 500, \"error\": \"Internal Server Error\", \"message\": \"Something went wrong on the server\" }"
                                    )
                            )
                    )
            }
    )

    @PostMapping("/saveLumpsum")
    public ResponseEntity<?> saveLumpsum(
            HttpServletRequest request,
            @RequestParam(required = false) String amc_code,
            @RequestParam(required = false) String scheme_name,
            @RequestParam(required = false) String scheme_code,
            @RequestParam(required = false) String reinvest_tag,
            @RequestParam(required = false) String amount,
            @RequestParam(required = false) String folio,
            @RequestParam(required = false) String payment_type,
            @RequestParam(required = false) String payment_mode,
            @RequestParam(required = false) String umrn_code,
            @RequestParam(required = false) String count,
            @RequestParam(required = false) String total_amount,
            @RequestParam(required = false) String goal_id,
            @RequestParam(required = false) String risk_profile,
            @RequestParam(required = false) String bank_account_number,
            @RequestParam(required = false) String iin_number,
            @RequestParam(required = false) String purchase_type,
            @RequestParam(required = false) String sub_trxn_type,
            @RequestParam(required = false) String cheque_no,
            @RequestParam(required = false) String cheque_date,
            @RequestParam(required = false) String dd_charge,
//            @RequestParam(required = false) String broker_code,
            @RequestParam(required = false) String euin_code,
            @RequestParam(required = false) String buy_sell_type,
            @RequestParam(required = false) String ip_address,
            @RequestParam(required = false) String origin_user_id,
            @RequestParam(required = false) String origin_first_name,
            @RequestParam(required = false) String subbroker_arn,
            @RequestParam(required = false) String subbroker_code,
            @RequestParam(required = false) String subbroker_name,
            @RequestParam(required = false) String cartid,
            @RequestParam(required = false) String source,
            @RequestHeader("Authorization") String token)
    {

        String ipAddr = "";
        List<CartDto> cartList = null;
        long currentTimeMillis = System.currentTimeMillis();
        try
        {
            amc_code = NseUtils.checkParem(amc_code);
            scheme_name = NseUtils.checkParem(scheme_name);
            scheme_code = NseUtils.checkParem(scheme_code);
            reinvest_tag = NseUtils.checkParem(reinvest_tag);
            amount = NseUtils.checkParem(amount);
            folio = NseUtils.checkParem(folio);
            payment_type = NseUtils.checkParem(payment_type);
            payment_mode = NseUtils.checkParem(payment_mode);
            umrn_code = NseUtils.checkParem(umrn_code);
            count = NseUtils.checkParem(count);
            total_amount = NseUtils.checkParem(total_amount);
            goal_id = NseUtils.checkParem(goal_id);
            risk_profile = NseUtils.checkParem(risk_profile);
            bank_account_number = NseUtils.checkParem(bank_account_number);
            iin_number = NseUtils.checkParem(iin_number);
            purchase_type = NseUtils.checkParem(purchase_type);
            sub_trxn_type = NseUtils.checkParem(sub_trxn_type);
            cheque_no = NseUtils.checkParem(cheque_no);
            cheque_date = NseUtils.checkParem(cheque_date);
            dd_charge = NseUtils.checkParem(dd_charge);
//            broker_code = NseUtils.checkParem();
            euin_code = NseUtils.checkParem(euin_code);
            source = NseUtils.checkParem(source);
            buy_sell_type = NseUtils.checkParem(buy_sell_type);
            cartid = NseUtils.checkParem(cartid);

            if(StringHelper.isEmpty(payment_type)){payment_type = "EMAIL";};
            if(StringHelper.isEmpty(payment_mode)){payment_mode = "Net Banking";};

            String broker_code = "ARN-0030";
            String login_name = "";
            String login_userid = "";
            String login_mobile = "";
            String client_name = "";

            String userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            System.out.println("User ID from token: " + userid);

            UserDto user = userServiceClient.getUserById(Integer.valueOf(userid), token);

            if (user == null)
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "User not found"));
            }
            System.out.println("usre = " + user);

            client_name = user.getClient_name();
            login_name = user.getFirst_name();
            login_userid = String.valueOf(user.getId());

            if(iin_number.isEmpty())
            {
                iin_number = user.getNse_iin_number();
            }
            if(bank_account_number.isEmpty())
            {
                bank_account_number = user.getBank_account_number1();
            }

            String appln_id = "";
            String password = "";
            String euin = "";
            String host = "";
            String mail_support_name = "";
            String mail_support_email = "";

            BseNseKeyDto nsekey = userServiceClient.getByClientName(client_name,token);
            System.out.println("nsekey = " + nsekey);
            System.out.println("NSE KEY = " + nsekey.getBrokerCode());

            host = nsekey.getDomain_url();
            mail_support_name = nsekey.getMail_support_name();
            mail_support_email = nsekey.getMail_support_email();


                broker_code = broker_code;
                appln_id = nsekey.getNse_appln_id();
                password = nsekey.getNse_password();
                if(!euin_code.isEmpty())
                {
                    euin = euin_code;
                }else{
                    euin = nsekey.getEuin();
                }

            euin = euin.split(",")[0];

            if(broker_code.isEmpty())
            {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), broker_code + " does not have the NSE credentials. Kindly update.sdsds"));
            }

            List<String> scheme_name_array = new ArrayList<String>();
            List<String> scheme_code_array = new ArrayList<String>();
            List<String> reinvest_tag_array = new ArrayList<String>();
            List<String> amc_code_array = new ArrayList<String>();
            List<String> amount_array = new ArrayList<String>();
            List<String> folio_array = new ArrayList<String>();
            List<String> cart_id_array = new ArrayList<String>();
            List<String> amc_name_array = new ArrayList<String>();
            List<String> trnx_type_array = new ArrayList<String>();
            List<String> purchase_type_array = new ArrayList<String>();

            if(source.equalsIgnoreCase("Mobile"))
            {
                scheme_name_array = new ArrayList<String>();
                scheme_code_array = new ArrayList<String>();
                reinvest_tag_array = new ArrayList<String>();
                amc_code_array = new ArrayList<String>();
                amount_array = new ArrayList<String>();
                folio_array = new ArrayList<String>();


                try {
                    cartList = userServiceClient.getCartDetailsByUserID(Integer.parseInt(userid),"NSE",iin_number, "Lumpsum Purchase",token);
                } catch (FeignException e) {
                    if (e.status() == 400) {
                        return NseUtils.commonResponse("No Cart found for the user.", HttpStatus.OK);
                    } else if (e.status() == 404) {
                        return NseUtils.commonResponse("No Cart found for the user.", HttpStatus.OK);
                    } else {
                        return NseUtils.commonResponse("Error occurred while fetching AMC details.", HttpStatus.BAD_REQUEST);
                    }
                }

                if(cartList.isEmpty())
                {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "No Cart found for the user."));
                }

                count = String.valueOf(cartList.size());

                total_amount = String.valueOf(cartList.stream().mapToInt(cart -> Integer.parseInt(cart.getAmount())).sum());

                for (CartDto cart : cartList)
                {
                    cart_id_array.add(String.valueOf(cart.getId()));
                    amc_code_array.add(cart.getScheme_company_code());
                    scheme_name_array.add(cart.getScheme_name());
                    scheme_code_array.add(cart.getScheme_product_code());
                    amount_array.add(cart.getAmount());
                    reinvest_tag_array.add(cart.getScheme_reinvest_tag());
                    trnx_type_array.add(cart.getTrnx_type());
                    purchase_type_array.add(cart.getTrnx_type());
                    amc_name_array.add(cart.getScheme_company());

                    if(StringHelper.isNotEmpty(cart.getFolio_no()))
                    {
                        List<String> folio_arrayArr = new ArrayList<String>(Arrays.asList(cart.getFolio_no().split(",")));
                        folio_array.addAll(folio_arrayArr);
                    }
                }
            }else
            {
                if(!cartid.isEmpty())
                {
                    //scheme_name_array = new ArrayList<String>();
                    scheme_code_array = new ArrayList<String>();
                    //reinvest_tag_array = new ArrayList<String>();
                    //amc_code_array = new ArrayList<String>();
                    amount_array = new ArrayList<String>();
                    folio_array = new ArrayList<String>();

                    List<Integer> ids = Arrays.stream(cartid.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());
                    try {
                        cartList = userServiceClient.getCartDetailsByIds(ids,token);
                    } catch (FeignException e) {
                        if (e.status() == 400) {
                            return NseUtils.commonResponse("No Cart found for the user.", HttpStatus.BAD_REQUEST);
                        } else if (e.status() == 404) {
                            return NseUtils.commonResponse("No Cart found for the user.", HttpStatus.BAD_REQUEST);
                        } else {
                            return NseUtils.commonResponse("Error occurred while fetching AMC details.", HttpStatus.BAD_REQUEST);
                        }
                    }

                    if(cartList.isEmpty())
                    {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "No Cart found for the user."));
                    }

                    count = String.valueOf(cartList.size());

                    total_amount = String.valueOf(cartList.stream().mapToInt(cart -> Integer.parseInt(cart.getAmount())).sum());

                    for (CartDto cart : cartList)
                    {
                        cart_id_array.add(String.valueOf(cart.getId()));
                        //amc_code_array.add(cart.getScheme_company_code());
                        //scheme_name_array.add(cart.getScheme_name());
                        scheme_code_array.add(cart.getScheme_product_code());
                        amount_array.add(cart.getAmount());
                        //reinvest_tag_array.add(cart.getScheme_reinvest_tag());
                        //trnx_type_array.add(cart.getTrnx_type());
                        purchase_type_array.add(cart.getTrnx_type());
                        //amc_name_array.add(cart.getScheme_company());

                        if(StringHelper.isNotEmpty(cart.getFolio_no()))
                        {
                            List<String> folio_arrayArr = new ArrayList<String>(Arrays.asList(cart.getFolio_no().split(",")));
                            folio_array.addAll(folio_arrayArr);
                        }
                    }
                }else
                {
                    //scheme_name_array = new ArrayList<String>(Arrays.asList(scheme_name.split(",")));
                    scheme_code_array = new ArrayList<String>(Arrays.asList(scheme_code.split(",")));
                    //reinvest_tag_array = new ArrayList<String>(Arrays.asList(reinvest_tag.split(",")));
                    //amc_code_array = new ArrayList<String>(Arrays.asList(amc_code.split(",")));
                    amount_array = new ArrayList<String>(Arrays.asList(amount.split(",")));
                    folio_array = new ArrayList<String>();
                    if(!purchase_type.isEmpty()){
                        purchase_type_array = new ArrayList<>(Arrays.asList(purchase_type.split(",")));
                    }
                    if(StringHelper.isNotEmpty(folio))
                    {
                        List<String> folio_arrayArr = new ArrayList<String>(Arrays.asList(folio.split(",")));
                        folio_array.addAll(folio_arrayArr);
                    }
                }
            }

            System.out.println("scheme_code_array = " + scheme_code_array.size());
            System.out.println("folio_array = " + folio_array.toString());

            String nse_iin = user.getNse_iin_number();
            String pan = "";
            String name = "";
            String selected_name = "";
            String client_acctype1 = "";
            String client_accno1 = "";
            String client_micrno1 = "";
            String client_ifsccode1 = "";
            String client_bank_name = "";
            String bank_holder_name = "";
            String client_branch_name = "";
            String bank_code = "";
            int otmflag = 0;
            String acc1 = "";
            String acc2 = "";
            String acc3 = "";

            String mobile  = "";
            String email = "";

            SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
            SimpleDateFormat df1 = new SimpleDateFormat("dd-MMM-yyyy");
            SimpleDateFormat df2 = new SimpleDateFormat("ddMMyyyyhhmmss");
            String unique_transaction_number = df2.format(new Date());

            if (!nse_iin.equalsIgnoreCase(iin_number))
            {

                UserBseNseDto nse = null;
                try {
                    nse = userServiceClient.getUserBseNseDetailsByIinNumber(client_name,iin_number,token);
                } catch (FeignException e) {
                    if (e.status() == 400) {
                        return NseUtils.commonResponse("No record found for the given IIN Number and Client Name.", HttpStatus.BAD_REQUEST);
                    } else if (e.status() == 404) {
                        return NseUtils.commonResponse("User not found.", HttpStatus.NOT_FOUND);
                    } else {
                        return NseUtils.commonResponse("Downstream service error.", HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                if(nse == null)
                {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "No User Found"));
                }
                pan = nse.getPan();
                name = nse.getName();
                mobile = nse.getMobile();
                email = nse.getEmail();
            }

            String sub_broker_code = "";
            String sub_code = "";

            if(!subbroker_code.isEmpty())
            {
                sub_code = subbroker_code;
            }else{
                sub_code = "";
            }

            if(!subbroker_arn.isEmpty())
            {
                sub_broker_code = subbroker_arn;
            }else{
                sub_broker_code = "";
            }

            String otm_flag = Integer.toString(otmflag);

            JSONArray regDetailsArray = new JSONArray();

            for(int i = 0; i < scheme_code_array.size(); i++)
            {

                String memberUniqueId = "P" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + UniqueIDProvider.generateUniquePin(3);
                JSONObject regObject = new JSONObject();

                regObject.put("order_ref_number","");
                regObject.put("scheme_code",scheme_code_array.get(i)); //scheme_code
                regObject.put("trxn_type","P");

                if(source.equalsIgnoreCase("Mobile"))
                {
                    if(purchase_type_array.get(i).equalsIgnoreCase("FP"))
                    {
                        regObject.put("buy_sell_type", "FRESH");
                    }else if(purchase_type_array.get(i).equalsIgnoreCase("AP"))
                    {
                        regObject.put("buy_sell_type", "ADDITIONAL");
                    }
                }else
                {
                    if(!cartid.isEmpty())
                    {
                        if(purchase_type_array.get(i).equalsIgnoreCase("FP"))
                        {
                            regObject.put("buy_sell_type", "FRESH");
                        }else if(purchase_type_array.get(i).equalsIgnoreCase("AP"))
                        {
                            regObject.put("buy_sell_type", "ADDITIONAL");
                        }
                    }else{
                        if(purchase_type_array.size() == scheme_code_array.size())
                        {
                            String purchaseType = purchase_type_array.get(i);
                            regObject.put(
                                    "buy_sell_type",
                                    purchaseType == null
                                            ? ""
                                            : purchaseType.toUpperCase(Locale.ROOT)
                            );
                        }else{
                            regObject.put("buy_sell_type", buy_sell_type);
                        }
                    }
                }

                regObject.put("client_code", iin_number);
                regObject.put("demat_physical", "P");
                regObject.put("order_amount", amount_array.get(i));

                if(folio_array!=null && folio_array.size()>i)
                {
                    if(!folio_array.get(i).equalsIgnoreCase("NEW") && !folio_array.get(i).equalsIgnoreCase("NEW FOLIO") && !folio_array.get(i).equalsIgnoreCase("0"))
                    {
                        regObject.put("folio_no",folio_array.get(i));
                    }else
                    {
                        regObject.put("folio_no","");
                    }
                }else{
                    regObject.put("folio_no","");
                }

                regObject.put("remarks", "");
                regObject.put("kyc_flag","Y");
                if(!sub_code.isEmpty())
                {
                    regObject.put("sub_broker_code",sub_code);
                }else{
                    regObject.put("sub_broker_code","");
                }
                regObject.put("euin_number", euin);
                regObject.put("euin_declaration", "Y");
                regObject.put("min_redemption_flag", "N");
                regObject.put("dpc_flag","Y");
                regObject.put("all_units","N");
                regObject.put("redemption_units","");
                regObject.put("sub_broker_arn","");
                regObject.put("bank_ref_no",""); //4651555
                regObject.put("account_no", bank_account_number);
                regObject.put("mobile_no",mobile);
                regObject.put("email", email);
                regObject.put("mandate_id", umrn_code);
                regObject.put("member_unique_id", memberUniqueId);
                regDetailsArray.put(regObject);
            }

            JSONObject requestBody = new JSONObject();
            requestBody.put("transaction_details", regDetailsArray);

            BseNseOnlineAccessDto online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, broker_code,token);

            if (online_access == null)
            {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new CommonResponse(StatusMessage.NseFailureCode, StatusMessage.NseFailureMessage, "NSE Online Credentials Not available. Please contact your RM"));
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
            headers.set("Accept-Language", "en-US");
            headers.set("Connection", "keep-alive");
            headers.set("Referer", "");
            headers.set("Accept-Encoding", "gzip");

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            String normalOrderUrl = NseApiUrls.normalOrderUrl;

            System.out.println("requestBody: " + requestBody.toString());

            String trxn_order_id = "";
            String trxn_status = "";
            String trxn_remark = "";

            String res_scheme_name = "";

            Map<String, String> resMap = new HashMap<>();
            String member_unique_id = "";

            int successCount = 0;
            int failureCount = 0;
            List<String> failedSchemes = new ArrayList<>();
            String lastSuccessRegId = "";

            Set<String> orderIdList = new HashSet<>();

            try
            {
                List<NseOnlineSchemeMaster> nseOnlineSchemeMasters = nseOnlineSchemeMasterRepository.getSchemeBySchemeCode(scheme_code_array);

                ResponseEntity<String> result = RestTemplateFactory.createRestTemplate().postForEntity(normalOrderUrl, entity, String.class);
                String status_code = result.getStatusCode().toString();
                String responseBody = result.getBody().toString();

                JSONObject jsonObject = new JSONObject(responseBody);
                System.out.println("jsonObject: " + jsonObject);
                JSONArray jsonRegArray = jsonObject.getJSONArray("transaction_details");

                Map<String, String> resultMap = new HashMap<>();
                boolean hasFailure = false;

                for (int i = 0; i < jsonRegArray.length(); i++) {
                    JSONObject regDetail = jsonRegArray.getJSONObject(i);

                    trxn_order_id = regDetail.optString("trxn_order_id");
                    trxn_status = regDetail.optString("trxn_status");
                    trxn_remark = regDetail.optString("trxn_remark");

                    System.out.println("trxn_order_id: " + trxn_order_id);
                    System.out.println("trxn_status: " + trxn_status);
                    System.out.println("trxn_remark: " + trxn_remark);
                }

                for (int i = 0; i < jsonRegArray.length(); i++)
                {
                    JSONObject regDetail = jsonRegArray.getJSONObject(i);

                    trxn_order_id = regDetail.optString("trxn_order_id");
                    trxn_status = regDetail.optString("trxn_status");
                    trxn_remark = regDetail.optString("trxn_remark");
                    member_unique_id = regDetail.optString("member_unique_id");

                    String res_order_ref_number    = NseUtils.checkParem(regDetail.optString("order_ref_number"));
                    String res_scheme_code         = NseUtils.checkParem(regDetail.optString("scheme_code"));
                    String res_trxn_type           = NseUtils.checkParem(regDetail.optString("trxn_type"));
                    String res_buy_sell_type       = NseUtils.checkParem(regDetail.optString("buy_sell_type"));
                    String res_client_code         = NseUtils.checkParem(regDetail.optString("client_code"));
                    String res_demat_physical      = NseUtils.checkParem(regDetail.optString("demat_physical"));
                    String res_order_amount        = NseUtils.checkParem(regDetail.optString("order_amount"));
                    String res_folio_no            = NseUtils.checkParem(regDetail.optString("folio_no"));
                    String res_remarks             = NseUtils.checkParem(regDetail.optString("remarks"));
                    String res_kyc_flag            = NseUtils.checkParem(regDetail.optString("kyc_flag"));
                    String res_sub_broker_code     = NseUtils.checkParem(regDetail.optString("sub_broker_code"));
                    String res_euin_number         = NseUtils.checkParem(regDetail.optString("euin_number"));
                    String res_euin_declaration    = NseUtils.checkParem(regDetail.optString("euin_declaration"));
                    String res_min_redemption_flag = NseUtils.checkParem(regDetail.optString("min_redemption_flag"));
                    String res_dpc_flag            = NseUtils.checkParem(regDetail.optString("dpc_flag"));
                    String res_all_units           = NseUtils.checkParem(regDetail.optString("all_units"));
                    String res_redemption_units    = NseUtils.checkParem(regDetail.optString("redemption_units"));
                    String res_sub_broker_arn      = NseUtils.checkParem(regDetail.optString("sub_broker_arn"));
                    String res_bank_ref_no         = NseUtils.checkParem(regDetail.optString("bank_ref_no"));
                    String res_account_no          = NseUtils.checkParem(regDetail.optString("account_no"));
                    String res_mobile_no           = NseUtils.checkParem(regDetail.optString("mobile_no"));
                    String res_email               = NseUtils.checkParem(regDetail.optString("email"));
                    String res_mandate_id          = NseUtils.checkParem(regDetail.optString("mandate_id"));
                    String res_filler1             = NseUtils.checkParem(regDetail.optString("filler1"));
                    String res_trxn_order_id       = NseUtils.checkParem(regDetail.optString("trxn_order_id"));
                    String res_trxn_status         = NseUtils.checkParem(regDetail.optString("trxn_status"));
                    String res_trxn_remark         = NseUtils.checkParem(regDetail.optString("trxn_remark"));
                    String res_member_unique_id    = NseUtils.checkParem(regDetail.optString("member_unique_id"));
                    String res_reg_id               = NseUtils.checkParem(regDetail.optString("reg_id"));
                    String res_reg_status           = NseUtils.checkParem(regDetail.optString("reg_status"));
                    String res_reg_remark           = NseUtils.checkParem(regDetail.optString("reg_remark"));

                    orderIdList.add(res_trxn_order_id);

                    System.out.println("res_order_ref_number    = " + res_order_ref_number);
                    System.out.println("res_scheme_code         = " + res_scheme_code);
                    System.out.println("res_trxn_type           = " + res_trxn_type);
                    System.out.println("res_buy_sell_type       = " + res_buy_sell_type);
                    System.out.println("res_client_code         = " + res_client_code);
                    System.out.println("res_demat_physical      = " + res_demat_physical);
                    System.out.println("res_order_amount        = " + res_order_amount);
                    System.out.println("res_folio_no            = " + res_folio_no);
                    System.out.println("res_remarks             = " + res_remarks);
                    System.out.println("res_kyc_flag            = " + res_kyc_flag);
                    System.out.println("res_sub_broker_code     = " + res_sub_broker_code);
                    System.out.println("res_euin_number         = " + res_euin_number);
                    System.out.println("res_euin_declaration    = " + res_euin_declaration);
                    System.out.println("res_min_redemption_flag = " + res_min_redemption_flag);
                    System.out.println("res_dpc_flag            = " + res_dpc_flag);
                    System.out.println("res_all_units           = " + res_all_units);
                    System.out.println("res_redemption_units    = " + res_redemption_units);
                    System.out.println("res_sub_broker_arn      = " + res_sub_broker_arn);
                    System.out.println("res_bank_ref_no         = " + res_bank_ref_no);
                    System.out.println("res_account_no          = " + res_account_no);
                    System.out.println("res_mobile_no           = " + res_mobile_no);
                    System.out.println("res_email               = " + res_email);
                    System.out.println("res_mandate_id          = " + res_mandate_id);
                    System.out.println("res_filler1             = " + res_filler1);
                    System.out.println("res_trxn_order_id       = " + res_trxn_order_id);
                    System.out.println("res_trxn_status         = " + res_trxn_status);
                    System.out.println("res_trxn_remark         = " + res_trxn_remark);
                    System.out.println("res_member_unique_id    = " + res_member_unique_id);

                    NseOnlineSchemeMaster nseOnlineSchemeMaster = nseOnlineSchemeMasters.stream().filter(obj -> obj.getSchemeCode().equalsIgnoreCase(res_scheme_code)).findFirst().orElse(new NseOnlineSchemeMaster());

                    if(trxn_status.equalsIgnoreCase("TRXN SUCCESS"))
                    {
                        resMap.put(nseOnlineSchemeMaster.getSchemeName(), trxn_status);
                    }else
                    {
                        resMap.put(nseOnlineSchemeMaster.getSchemeName(), trxn_remark);
                    }

                    if (name == null || name.isEmpty()) {
                        name = user.getName();
                    }

                    if (pan == null || pan.isEmpty()) {
                        pan = user.getPan();
                    }

                    NseTransactions nsetrans = new NseTransactions();
                    nsetrans.setUrl(normalOrderUrl);
                    nsetrans.setNse_request(requestBody.toString());
                    nsetrans.setNse_response(responseBody);
                    nsetrans.setReg_id(res_trxn_order_id);
                    nsetrans.setPayment_link("");
                    nsetrans.setPan(pan);
                    nsetrans.setName(name);
                    nsetrans.setBranch(user.getBranch());
                    nsetrans.setRm_name(user.getRm_name());
                    if(source.equalsIgnoreCase("Website"))
                    {
                        nsetrans.setSubbroker_name(subbroker_name);
                    }else{
                        nsetrans.setSubbroker_name(user.getSubbroker_name());
                    }
                    if(!subbroker_code.isEmpty())
                    {
                        nsetrans.setSubbroker_code(subbroker_code);
                    }
                    if(!subbroker_arn.isEmpty())
                    {
                        nsetrans.setSubbroker_arn(subbroker_arn);
                    }
                    nsetrans.setClient_name(client_name);
                    nsetrans.setIin_number(iin_number);
                    nsetrans.setScheme_name(nseOnlineSchemeMaster.getSchemeName());
                    nsetrans.setScheme_code(nseOnlineSchemeMaster.getSchemeCode());
                    nsetrans.setFolio_no(res_folio_no);
                    nsetrans.setAmount_units(res_order_amount);
                    nsetrans.setFrequency("");
                    nsetrans.setPeriod_day("");
                    nsetrans.setUmrn_no(res_mandate_id);
                    nsetrans.setService_return_code(status_code);
                    nsetrans.setService_msg(trxn_status);
                    if(StringHelper.isNotEmpty(res_buy_sell_type))
                    {
                        nsetrans.setPurchase_type(res_buy_sell_type);
                    }else
                    {
                        nsetrans.setPurchase_type("FRESH");
                    }
                    nsetrans.setReturn_msg(res_trxn_status);
                    nsetrans.setPayment_ref_no("");
                    nsetrans.setAuto_trxn_no(res_order_ref_number);
                    nsetrans.setUnique_number(member_unique_id);
                    nsetrans.setAuto_trxn_no("");
                    nsetrans.setSip_reg_no("");
                    nsetrans.setPayment_mode("");
                    nsetrans.setTopup_amount(0.0);
                    nsetrans.setBank_acc_no("");
                    if(StringHelper.isNotEmpty(res_trxn_order_id))
                    {
                        nsetrans.setTransaction_number(res_trxn_order_id);
                    }
                    nsetrans.setApplication_number("");
                    nsetrans.setTo_scheme_code("");
                    nsetrans.setTo_scheme_name("");
                    nsetrans.setTransaction_type("Lumpsum Purchase");
                    nsetrans.setTransaction_status(res_trxn_status);
                    nsetrans.setPayment_status("PENDING");
                    nsetrans.setActive_ceased_status("");
                    nsetrans.setRemarks(trxn_remark);
                    nsetrans.setMandate_id("");
                    nsetrans.setMandate_status("");
                    nsetrans.setEmandate_auth_flag("");
                    nsetrans.setApp_received_flag("");
                    nsetrans.setTransaction_date(new Date());
                    nsetrans.setUser_id(Integer.parseInt(userid));
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
//                    nsetrans.setIp_address(ip_address);
//                    nsetrans.setOrigin_user_id(origin_user_id);
//                    nsetrans.setOrigin_first_name(origin_first_name);
                    nsetrans.setCart_id(cartIdAt(cart_id_array, i));

                    try {
                        nseTransactionService.save(nsetrans);
                    } catch (PessimisticLockingFailureException ex) {

                        hasFailure = true;
                        resultMap.put(
                                nseOnlineSchemeMaster.getSchemeName(),
                                "System busy. Please retry"
                        );

                        // optional: log for retry job
                        System.out.println("Lock timeout for txn " +  nsetrans.getTransaction_number() + " - " + ex);

                    } catch (Exception ex) {
                        hasFailure = true;
                        resultMap.put(
                                nseOnlineSchemeMaster.getSchemeName(),
                                "Failed to save transaction"
                        );

                        System.out.println("Lock timeout for txn " +  nsetrans.getTransaction_number() + " - " + ex);
                    }


                    if(trxn_status.equalsIgnoreCase("TRXN SUCCESS"))
                    {
                        successCount++;
                        lastSuccessRegId = res_reg_id;
                        resMap.put(nseOnlineSchemeMaster.getSchemeName(), "TRXN SUCCESS");
                    }
                    else
                    {
                        failureCount++;
                        failedSchemes.add(nseOnlineSchemeMaster.getSchemeName() + ": " + res_reg_remark);
                        resMap.put(nseOnlineSchemeMaster.getSchemeName(), res_reg_remark);
                    }
                }
            } catch (HttpClientErrorException | HttpServerErrorException ex) {

                System.out.println("orderEntryFreshPurchase::Status Code: " + ex.getStatusCode());
                System.out.println("orderEntryFreshPurchase::Response Body: " + ex.getResponseBodyAsString());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), ex.getResponseBodyAsString()));
            }

            if (source.equalsIgnoreCase("Mobile"))
            {
                for (CartDto cart : cartList)
                {
                    cart.setPayment_type(payment_type);
                    cart.setPayment_mode(payment_mode);
                    cart.setBank_name(client_bank_name);
                    cart.setBank_account_number(client_accno1);
                    cart.setBank_ifsc(client_ifsccode1);
                    cart.setBroker_code(broker_code);
                    cart.setEuin_code(euin);

                    if (trxn_status.equalsIgnoreCase("TRXN SUCCESS"))
                    {
                        cart.setStatus("SUCCESS");
                        cart.setActive(false);
                        cart.setPayment_id(String.valueOf(currentTimeMillis));
                    }
                }

                userServiceClient.updateCartByCartId(cartList, token);
            }
            if (!cartid.isEmpty())
            {
                for (CartDto cart : cartList)
                {
                    cart.setPayment_type("");
                    cart.setPayment_mode("");
                    cart.setBank_name("");
                    cart.setBank_account_number("");
                    cart.setBank_ifsc("");
                    cart.setBroker_code(broker_code);
                    cart.setEuin_code(euin);

                    if (trxn_status.equalsIgnoreCase("TRXN SUCCESS"))
                    {
                        cart.setStatus("SUCCESS");
                        cart.setActive(false);
                        cart.setPayment_id(String.valueOf(currentTimeMillis));
                    }
                }

                userServiceClient.updateCartByCartId(cartList, token);
            }

            System.out.println("resMap = " + new Gson().toJson(resMap));

            if (successCount > 0 && failureCount > 0)
            {
                String message = String.format("%d out of %d Purchase transactions succeeded. Please go to MyOrders Page check the details.",successCount, (successCount + failureCount));
                message += "Failed transactions: " + String.join(", ", failedSchemes);
                return NseUtils.transactionResponse(HttpStatus.BAD_REQUEST, message, resMap);

            }
            else if (successCount > 0)
            {
                if(source.equalsIgnoreCase("Mobile"))
                {
                    return NseUtils.transactionMobileResponse(HttpStatus.OK, trxn_status + " Your Order is successfully triggered...! orderID: " + trxn_order_id, resMap);
                }else{
                    return NseUtils.purchaseTransactionResponse(HttpStatus.OK, trxn_status + " Your Order is successfully triggered...! orderID: " + trxn_order_id, resMap, orderIdList);
                }
            }
            else
            {
                return NseUtils.commonResponse(trxn_remark, HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), ""));
        }
    }



    @Operation(
            summary = "Save SIP Purchase",
            description = """
        Triggers a SIP (Systematic Investment Plan) purchase transaction for NSE. Parameters differ for Web and Mobile based on the 'source' field..
        
        🔹 If `source = web`: All transaction and investor details are expected.
        🔹 If `source = mobile`: Only essential payment and investor info is needed.
        """,
            parameters = {
                    // Common
                    @Parameter(name = "source", description = "Transaction source (web/mobile)", required = false),
                    @Parameter(name = "Authorization", description = "Bearer token for authentication", required = true, in = ParameterIn.HEADER),

                    // ✅ Website Parameters
                    @Parameter(name = "amc_code", description = "AMC code (Web)", required = false),
                    @Parameter(name = "scheme_name", description = "Scheme name (Web)", required = false),
                    @Parameter(name = "scheme_code", description = "Scheme code (Web)", required = false),
                    @Parameter(name = "amount", description = "Investment amount (Web)", required = false),
                    @Parameter(name = "folio", description = "Folio number (Web)", required = false),
                    @Parameter(name = "reinvest_tag", description = "Reinvest tag (Web)", required = false),
                    @Parameter(name = "frequency", description = "SIP frequency (e.g., Monthly) (Web)", required = false),
                    @Parameter(name = "start_date", description = "SIP start date (Web)", required = false),
                    @Parameter(name = "end_date", description = "SIP end date (Web)", required = false),
                    @Parameter(name = "until_cancelled", description = "Flag for until cancelled (Web)", required = false),
                    @Parameter(name = "total_amount", description = "Total SIP amount (Web)", required = false),
                    @Parameter(name = "multiple_count", description = "Number of SIPs (Web)", required = false),
                    @Parameter(name = "iin_number", description = "Investor Identification Number (Web)", required = false),
                    @Parameter(name = "umrn_code", description = "UMRN code (Web)", required = false),
                    @Parameter(name = "payment_type", description = "Payment type (Web)", required = false),
                    @Parameter(name = "broker_code", description = "Broker code (Web)", required = false),
                    @Parameter(name = "cheque_no", description = "Cheque number (Web)", required = false),
                    @Parameter(name = "cheque_date", description = "Cheque date (Web)", required = false),
                    @Parameter(name = "dd_charge", description = "Demand draft charges (Web)", required = false),
                    @Parameter(name = "euin_code", description = "EUIN code (Web)", required = false),
                    @Parameter(name = "sip_day", description = "SIP day (Web)", required = false),
                    @Parameter(name = "installment", description = "Number of installments (Web)", required = false),
                    @Parameter(name = "sip_first_date", description = "First SIP payment date (Web)", required = false),
                    @Parameter(name = "sip_second_date", description = "Second SIP payment date (Web)", required = false),
                    @Parameter(name = "first_payment_option", description = "First payment option (Web)", required = false),

                    // ✅ Mobile Parameters
                    @Parameter(name = "iin_number", description = "Investor Identification Number (Mobile)", required = false),
                    @Parameter(name = "umrn_code", description = "UMRN code (Mobile)", required = false),
                    @Parameter(name = "payment_type", description = "Payment type (Mobile)", required = false),
                    @Parameter(name = "broker_code", description = "Broker code (Mobile)", required = false),
                    @Parameter(name = "cheque_no", description = "Cheque number (Mobile)", required = false),
                    @Parameter(name = "cheque_date", description = "Cheque date (Mobile)", required = false),
                    @Parameter(name = "dd_charge", description = "Demand draft charges (Mobile)", required = false),
                    @Parameter(name = "euin_code", description = "EUIN code (Mobile)", required = false),
                    @Parameter(name = "first_payment_option", description = "First payment option (Mobile)", required = false)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "SIP Order successfully triggered",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TransactionResponse.class),
                                    examples = @ExampleObject(
                                            name = "SuccessResponseExample",
                                            summary = "SIP Order success example",
                                            value = """
                        {
                          "status": 200,
                          "status_msg": "SUCCESS",
                          "msg": "SIP Order successfully triggered",
                          "return_msg": "OrderID: TXN123456",
                          "transaction_status": {
                            "HDFC Overnight Fund - Growth": "Successfully Triggered"
                          }
                        }
                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request or missing data",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "BadRequestExample",
                                            summary = "Invalid SIP request",
                                            value = "{ \"status\": 400, \"error\": \"Bad Request\", \"message\": \"User not found\" }"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal Server Error",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "ServerErrorExample",
                                            summary = "Unexpected server error",
                                            value = "{ \"status\": 500, \"error\": \"Internal Server Error\", \"message\": \"Something went wrong on the server\" }"
                                    )
                            )
                    )
            }
    )
    @PostMapping("/saveSip")
    public ResponseEntity<?> saveSip(
            HttpServletRequest request,
            @RequestParam(required = false) String amc_code,
            @RequestParam(required = false) String scheme_name,
            @RequestParam(required = false) String scheme_code,
            @RequestParam(required = false) String amount,
            @RequestParam(required = false) String folio,
            @RequestParam(required = false) String reinvest_tag,
            @RequestParam(required = false) String frequency,
            @RequestParam(required = false) String start_date,
            @RequestParam(required = false) String end_date,
            @RequestParam(required = false) String until_cancelled,
            @RequestParam(required = false) String total_amount,
            @RequestParam(required = false) String multiple_count,
            @RequestParam(required = false) String iin_number,
            @RequestParam(required = false) String umrn_code,
            @RequestParam(required = false) String payment_type,
            @RequestParam(required = false) String broker_code,
            @RequestParam(required = false) String cheque_no,
            @RequestParam(required = false) String cheque_date,
            @RequestParam(required = false) String dd_charge,
            @RequestParam(required = false) String euin_code,
            @RequestParam(required = false) String sip_day,
            @RequestParam(required = false) String installment,
            @RequestParam(required = false) String sip_first_date,
            @RequestParam(required = false) String sip_second_date,
            @RequestParam(required = false) String bank_account_number,
            @RequestParam(required = false) String first_payment_option,
            @RequestParam(required = false) String subbroker_arn,
            @RequestParam(required = false) String subbroker_code,
            @RequestParam(required = false) String subbroker_euin,
            @RequestParam(required = false) String sip_stepup_start_date,
            @RequestParam(required = false) String sip_stepup_required,
            @RequestParam(required = false) String sip_stepup_end_date,
            @RequestParam(required = false) String sip_stepup_frequency,
            @RequestParam(required = false) String stepup_amount,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String cartid,
            @RequestParam(required = false) String subbroker_name,
            @RequestParam(required = false) String ip_address,
            @RequestParam(required = false) String origin_user_id,
            @RequestParam(required = false) String origin_first_name,
            @RequestHeader("Authorization") String token)
    {

        String ipAddr = "";
        List<CartDto> cartList = null;
        long currentTimeMillis = System.currentTimeMillis();
        try
        {
            String userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            String client_name = TokenInterceptor.extractClientNamedFromToken(token,secretKey);
            System.out.println("User ID from token: " + userid);

            amc_code = NseUtils.checkParem(amc_code);
            scheme_name = NseUtils.checkParem(scheme_name);
            scheme_code = NseUtils.checkParem(scheme_code);
            amount = NseUtils.checkParem(amount);
            folio = NseUtils.checkParem(folio);
            reinvest_tag = NseUtils.checkParem(reinvest_tag);
            frequency = NseUtils.checkParem(frequency);
            start_date = NseUtils.checkParem(start_date);
            end_date = NseUtils.checkParem(end_date);
            until_cancelled = NseUtils.checkParem(until_cancelled);
            total_amount = NseUtils.checkParem(total_amount);
            multiple_count = NseUtils.checkParem(multiple_count);
            iin_number = NseUtils.checkParem(iin_number);
            umrn_code = NseUtils.checkParem(umrn_code);
            payment_type = NseUtils.checkParem(payment_type);
            broker_code = NseUtils.checkParem(broker_code);
            cheque_no = NseUtils.checkParem(cheque_no);
            cheque_date = NseUtils.checkParem(cheque_date);
            dd_charge = NseUtils.checkParem(dd_charge);
            euin_code = NseUtils.checkParem(euin_code);
            sip_day = NseUtils.checkParem(sip_day);
            sip_first_date = NseUtils.checkParem(sip_first_date);
            sip_second_date = NseUtils.checkParem(sip_second_date);
            first_payment_option = NseUtils.checkParem(first_payment_option);
            source = NseUtils.checkParem(source);
            subbroker_arn = NseUtils.checkParem(subbroker_arn);
            subbroker_code = NseUtils.checkParem(subbroker_code);
            subbroker_euin = NseUtils.checkParem(subbroker_euin);

            List<String> amc_code_array = new ArrayList<String>();
            List<String> scheme_name_array = new ArrayList<String>();
            List<String> scheme_code_array = new ArrayList<String>();
            List<String> amount_array = new ArrayList<String>();
            List<String> folio_array = new ArrayList<String>();
            List<String> start_date_array = new ArrayList<String>();
            List<String> end_date_array = new ArrayList<String>();
            List<String> until_cancelled_array = new ArrayList<String>();
            List<String> reinvest_tag_array = new ArrayList<String>();
            List<String> frequency_array = new ArrayList<String>();
            List<String> sip_day_array = new ArrayList<String>();
            List<String> sip_first_date_array = new ArrayList<String>();
            List<String> sip_second_date_array = new ArrayList<String>();

            List<String> cart_id_array = new ArrayList<String>();
            List<String> trnx_type_array = new ArrayList<String>();
            List<String> purchase_type_array = new ArrayList<String>();
            List<String> sip_installment_array = new ArrayList<String>();
            List<String> date_array = new ArrayList<String>();

            List<String> sip_stepup_required_array = new ArrayList<>();
            List<String> sip_stepup_start_date_array = new ArrayList<>();
            List<String> sip_stepup_end_date_array = new ArrayList<>();
            List<String> sip_stepup_frequency_array = new ArrayList<>();
            List<String> stepup_amount_array = new ArrayList<>();

            if(source.equalsIgnoreCase("Mobile"))
            {
                amc_code_array = new ArrayList<String>();
                scheme_name_array = new ArrayList<String>();
                scheme_code_array = new ArrayList<String>();
                amount_array = new ArrayList<String>();
                folio_array = new ArrayList<String>();
                start_date_array = new ArrayList<String>();
                end_date_array = new ArrayList<String>();
                until_cancelled_array = new ArrayList<String>();
                reinvest_tag_array = new ArrayList<String>();
                frequency_array = new ArrayList<String>();
                sip_day_array = new ArrayList<String>();
                sip_first_date_array = new ArrayList<String>();
                sip_second_date_array = new ArrayList<String>();
                sip_installment_array = new ArrayList<String>();
                cart_id_array = new ArrayList<String>();
                trnx_type_array = new ArrayList<String>();
                purchase_type_array = new ArrayList<String>();
                date_array = new ArrayList<String>();
                System.out.println("user    " + userid + iin_number );

                try {
                    cartList = userServiceClient.getCartDetailsByUserID(Integer.parseInt(userid),"NSE",iin_number, "SIP Purchase",token);
                } catch (FeignException e) {
                    if (e.status() == 400) {
                        return NseUtils.commonResponse("No cart found", HttpStatus.BAD_REQUEST);
                    }
                }

                if(cartList.isEmpty())
                {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "No Cart found for the user."));
                }

                System.out.println("cartList = " + cartList);
                System.out.println("ccc = " + cartList.get(0).getInstallment());
                for (CartDto cart : cartList)
                {
                    cart_id_array.add(String.valueOf(cart.getId()));
                    amc_code_array.add(cart.getScheme_company());
                    scheme_name_array.add(cart.getScheme_name());
                    scheme_code_array.add(cart.getScheme_product_code());
                    amount_array.add(cart.getAmount());
                    trnx_type_array.add(cart.getTrnx_type());
                    purchase_type_array.add(cart.getTrnx_type());
                    start_date_array.add(cart.getStart_date());

                    frequency_array.add(cart.getFrequency());

                    String until_cancelledStr = cart.getUntil_cancel().equals(true) ? "Y" : "N";

                    if(!cart.getInstallment().isEmpty())
                    {
                        sip_installment_array.add(cart.getInstallment());
                    }

                    if(!cart.getFolio_no().isEmpty())
                    {
                        List<String> folio_arrayArr = new ArrayList<String>(Arrays.asList(cart.getFolio_no().split(",")));
                        folio_array.addAll(folio_arrayArr);
                    }

                    if(!cart.getEnd_date().isEmpty())
                    {
                        end_date_array.add(cart.getEnd_date());
                    }

                    if(!until_cancelledStr.isEmpty())
                    {
                        until_cancelled_array.add(until_cancelledStr);
                    }

                    if(!reinvest_tag_array.isEmpty())
                    {
                        reinvest_tag_array.add(cart.getScheme_reinvest_tag());
                    }

                    if(!cart.getSip_date().isEmpty())
                    {
                        sip_day_array.add(cart.getSip_date());
                    }

                    if(!cart.getStart_date().isEmpty())
                    {
                        sip_day_array.add(cart.getStart_date());
                    }

                    if(!cart.getFirst_date().isEmpty())
                    {
                        sip_first_date_array.add(cart.getFirst_date());
                    }

                    if(!cart.getSecond_date().isEmpty())
                    {
                        sip_first_date_array.add(cart.getSecond_date());
                    }
                }
            }else
            {
                if(!cartid.isEmpty())
                {
                    amc_code_array = new ArrayList<String>();
//                    scheme_name_array = new ArrayList<String>();
                    scheme_code_array = new ArrayList<String>();
                    amount_array = new ArrayList<String>();
                    folio_array = new ArrayList<String>();
                    start_date_array = new ArrayList<String>();
                    end_date_array = new ArrayList<String>();
//                    until_cancelled_array = new ArrayList<String>();
                    reinvest_tag_array = new ArrayList<String>();
                    frequency_array = new ArrayList<String>();
//                    sip_day_array = new ArrayList<String>();
//                    sip_first_date_array = new ArrayList<String>();
                    sip_second_date_array = new ArrayList<String>();
                    sip_installment_array = new ArrayList<String>();
                    cart_id_array = new ArrayList<String>();
//                    trnx_type_array = new ArrayList<String>();
//                    purchase_type_array = new ArrayList<String>();
                    date_array = new ArrayList<String>();

                    sip_stepup_required_array = new ArrayList<>();
                    sip_stepup_start_date_array = new ArrayList<>();
                    sip_stepup_end_date_array = new ArrayList<>();
                    sip_stepup_frequency_array = new ArrayList<>();
                    stepup_amount_array = new ArrayList<>();

                    List<Integer> ids = Arrays.stream(cartid.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());
                    System.out.println("ids = " + ids);

                    try {
                        cartList = userServiceClient.getCartDetailsByIds(ids,token);
                    } catch (FeignException e)
                    {
                        if (e.status() == 400)
                        {
                            return NseUtils.commonResponse("No cart found", HttpStatus.BAD_REQUEST);
                        }else
                        {
                            return NseUtils.commonResponse( e.getMessage(), HttpStatus.BAD_REQUEST);
                        }
                    }

                    if(cartList.isEmpty())
                    {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "No Cart found for the user."));
                    }

                    for (CartDto cart : cartList)
                    {
                        cart_id_array.add(String.valueOf(cart.getId()));
                        amc_code_array.add(cart.getScheme_company());
//                        scheme_name_array.add(cart.getScheme_name());
                        scheme_code_array.add(cart.getScheme_product_code());
                        amount_array.add(cart.getAmount());
//                        trnx_type_array.add(cart.getTrnx_type());
//                        purchase_type_array.add(cart.getTrnx_type());

                        String startDateStr = cart.getStart_date();

                        if(startDateStr != null && !startDateStr.isEmpty())
                        {
                            startDateStr = startDateStr.replaceAll("-","/");
                        }

                        start_date_array.add(startDateStr);

                        frequency_array.add(cart.getFrequency());

                        String until_cancelledStr = cart.getUntil_cancel().equals(true) ? "Y" : "N";

                        if(!cart.getInstallment().isEmpty())
                        {
                            sip_installment_array.add(cart.getInstallment());
                        }

                        if(!cart.getFolio_no().isEmpty())
                        {
                            List<String> folio_arrayArr = new ArrayList<String>(Arrays.asList(cart.getFolio_no().split(",")));
                            folio_array.addAll(folio_arrayArr);
                        }
                        if (cart.getEnd_date() != null && !cart.getEnd_date().isEmpty()) {
                            LocalDate date = LocalDate.parse(cart.getEnd_date(), DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                            String formattedEndDate = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                            end_date_array.add(formattedEndDate);
                        } else {
                            end_date_array.add("");
                        }

//                        if(!until_cancelledStr.isEmpty())
//                        {
//                            until_cancelled_array.add(until_cancelledStr);
//                        }

                        if(!reinvest_tag_array.isEmpty())
                        {
                            reinvest_tag_array.add(cart.getScheme_reinvest_tag());
                        }

//                        if(!cart.getSip_date().isEmpty())
//                        {
//                            sip_day_array.add(cart.getSip_date());
//                        }

//                        if(!cart.getStart_date().isEmpty())
//                        {
//                            sip_day_array.add(cart.getStart_date());
//                        }

//                        if(!cart.getFirst_date().isEmpty())
//                        {
//                            sip_first_date_array.add(cart.getFirst_date());
//                        }

//                        if(!cart.getSecond_date().isEmpty())
//                        {
//                            sip_first_date_array.add(cart.getSecond_date());
//                        }

                        if(cart.getIs_step_up())
                        {
                            sip_stepup_required_array.add("Y");

                            if(StringHelper.isNotEmpty(cart.getStep_up_start_date()))
                            {
                                sip_stepup_start_date_array.add(cart.getStep_up_start_date());
                            }

                            if(StringHelper.isNotEmpty(cart.getStep_up_end_date()))
                            {
                                sip_stepup_end_date_array.add(cart.getStep_up_end_date());
                            }

                            if(StringHelper.isNotEmpty(cart.getStep_up_frequency()))
                            {
                                sip_stepup_frequency_array.add(cart.getStep_up_frequency());
                            }

                            if(StringHelper.isNotEmpty(cart.getStep_up_amount()))
                            {
                                stepup_amount_array.add(cart.getStep_up_amount());
                            }
                        }
                    }
                }else
                {
                    amc_code_array = new ArrayList<String>(Arrays.asList(amc_code.split(",")));
//                    scheme_name_array = new ArrayList<String>(Arrays.asList(scheme_name.split(",")));
                    scheme_code_array = new ArrayList<String>(Arrays.asList(scheme_code.split(",")));
                    amount_array = new ArrayList<String>(Arrays.asList(amount.split(",")));
                    folio_array = new ArrayList<String>();
                    start_date_array = new ArrayList<String>(Arrays.asList(start_date.split(",")));
                    end_date_array = new ArrayList<String>();
//                    until_cancelled_array = new ArrayList<String>();
                    reinvest_tag_array = new ArrayList<String>();
                    frequency_array = new ArrayList<String>(Arrays.asList(frequency.split(",")));
//                    sip_day_array = new ArrayList<String>();
//                    sip_first_date_array = new ArrayList<String>();
                    sip_second_date_array = new ArrayList<String>();

                    sip_stepup_required_array = new ArrayList<>(Arrays.asList(sip_stepup_required.split(",")));
                    sip_stepup_start_date_array = new ArrayList<>(Arrays.asList(sip_stepup_start_date.split(",")));
                    sip_stepup_end_date_array = new ArrayList<>(Arrays.asList(sip_stepup_end_date.split(",")));
                    sip_stepup_frequency_array = new ArrayList<>(Arrays.asList(sip_stepup_frequency.split(",")));
                    stepup_amount_array = new ArrayList<>(Arrays.asList(stepup_amount.split(",")));

                    if(!folio.isEmpty())
                    {
                        folio_array = new ArrayList<String>(Arrays.asList(folio.split(",")));
                    }

                    if(!end_date.isEmpty())
                    {
                        end_date_array = new ArrayList<String>(Arrays.asList(end_date.split(",")));
                    }

//                    if(!until_cancelled.isEmpty())
//                    {
//                        until_cancelled_array = new ArrayList<String>(Arrays.asList(until_cancelled.split(",")));
//                    }

                    if(!reinvest_tag.isEmpty())
                    {
                        reinvest_tag_array = new ArrayList<String>(Arrays.asList(reinvest_tag.split(",")));
                    }

//                if(!sip_day.isEmpty())
//                {
//                    sip_day_array = new ArrayList<String>(Arrays.asList(sip_day.split(",")));
//                }
//
//                if(!sip_first_date.isEmpty())
//                {
//                    sip_first_date_array = new ArrayList<String>(Arrays.asList(sip_first_date.split(",")));
//                }
//
//                if(!sip_second_date.isEmpty())
//                {
//                    sip_second_date_array = new ArrayList<String>(Arrays.asList(sip_second_date.split(",")));
//                }

                    if(!installment.isEmpty())
                    {
                        sip_installment_array = new ArrayList<String>(Arrays.asList(installment.split(",")));
                    }
                }
            }

            if(StringHelper.isEmpty(umrn_code))
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "ACH MANDATE Code is empty. Please contact admin."));
            }

            UserDto nse = null;

            try {
                nse = userServiceClient.getUserBseNseDetailsByNseIINNumberBrokerCode(client_name, iin_number,broker_code,token);
            }catch (FeignException e)
            {
                return FeignErrorHandler.handle(e, "User Service", "User not found");
            }

            if(nse == null)
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "No User Found"));
            }

            String appln_id = "";
            String password = "";
            String euin = "";

            BseNseKeyDto nsekey = userServiceClient.getByClientName(client_name,token);
            String broker_code1 = nsekey.getBrokerCode();

            if(broker_code1 == null){broker_code1 = "";};


                broker_code = broker_code1;
                appln_id = nsekey.getNse_appln_id();
                password = nsekey.getNse_password();

                if(!subbroker_euin.isEmpty())
                {
                    euin = subbroker_euin;
                }else
                {
                    if(!euin_code.isEmpty())
                    {
                        euin = euin_code;
                    }else{
                        euin = nsekey.getEuin();
                    }
                }

                if(!subbroker_arn.isEmpty())
                {
                   subbroker_arn = subbroker_arn;
                }else{
                    subbroker_arn = "";
                }

                if(!subbroker_code.isEmpty())
                {
                    subbroker_code = subbroker_code;
                }else{
                    subbroker_code = "";
                }

            if(broker_code.isEmpty())
            {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), broker_code + " does not have the NSE credentials. Kindly update."));
            }

            SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
            SimpleDateFormat df1 = new SimpleDateFormat("dd-MMM-yyyy");
            SimpleDateFormat df2 = new SimpleDateFormat("ddMMyyyyhhmmss");

            String nse_iin = nse.getNse_iin_number();
            String pan = "";
            String name = "";
            String selected_name = "";
            String bank_code ="";
            String umrn_number = "";
            Integer otm_approved1 = 0;
            Integer otm_approved2 = 0;
            Integer otm_approved3 = 0;
            String bank_holder_name = "";
            String bank_name = "";
            String client_accno = "";
            String client_ifsccode = "";
            String client_acc_type = "";
            String client_bank_branch = "";

            String mobile = "";
            String email = "";

            System.out.println("nse_iin = " + nse_iin);
            System.out.println("iin_number = " + iin_number);

            UserDto user =null;

            try {
                user = userServiceClient.getUserById(Integer.valueOf(userid), token);
            } catch (FeignException e) {
                return FeignErrorHandler.handle(e, "User Service", "User not found");
            }
          
            System.out.println("aaaaa = " + client_name +   broker_code);
            BseNseOnlineAccessDto online_access = null;
            try {
                online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, broker_code,token);
            } catch (FeignException e) {
                if (e.status() == 400) {
                   return NseUtils.commonResponse("No record found in Bse Online Access Table", HttpStatus.BAD_REQUEST);
                } else if (e.status() == 404) {
                    return NseUtils.commonResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
                } else {
                    return NseUtils.commonResponse( e.getMessage(), HttpStatus.BAD_REQUEST);
                }
            }
            if (online_access == null)
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "NSE Online Credentials Not available. Please contact your RM"));
            }

            String nse_userid = NseUtils.trimOrEmpty(online_access.getNse_userid());
            String nse_memberid = NseUtils.trimOrEmpty(online_access.getNse_memberid());
            String nse_secret_key = NseUtils.trimOrEmpty(online_access.getNse_secret_key());
            String nse_license_key = NseUtils.trimOrEmpty(online_access.getNse_license_key());

            JSONArray regDetailsArray = new JSONArray();
            JSONObject regObject = null;

            for(int i=0; i<scheme_code_array.size(); i++)
            {
                String memberUniqueId = "SIP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + UniqueIDProvider.generateUniquePin(3);

                String start_date_str = start_date_array.get(i);

                if(start_date_str != null)
                {
                    start_date_str = start_date_str.replace("-", "/");
                }
                String sip_install = "";
                if(sip_installment_array.size() > 0){
                    sip_install = sip_installment_array.get(i);
                }

                regObject = new JSONObject();
                regObject.put("amc_code", amc_code_array.get(i));
                regObject.put("sch_code", scheme_code_array.get(i));
                regObject.put("client_code", iin_number);
                regObject.put("bank_ref_no", "");
                regObject.put("internal_ref_no", "");
                regObject.put("trans_mode", "P");
                regObject.put("dp_txn_mode", "P");
                regObject.put("start_date", start_date_str);
                regObject.put("frequency_type", frequency_array.get(i));
                regObject.put("frequency_allowed", "1");
                regObject.put("status", "1");
                regObject.put("member_code", nse_memberid);
                if(folio_array!= null && folio_array.size()> i)
                {
                    if(!folio_array.get(i).equalsIgnoreCase("NEW") && !folio_array.get(i).equalsIgnoreCase("NEW FOLIO") && !folio_array.get(i).equalsIgnoreCase("0"))
                    {
                        regObject.put("folio_no",folio_array.get(i));
                    }else
                    {
                        regObject.put("folio_no","");
                    }

                }else{
                    regObject.put("folio_no", "");
                }
                regObject.put("sip_remarks", "");

                if(sip_installment_array!= null && sip_installment_array.size()> i){
                    if ("DAILY".equalsIgnoreCase(frequency_array.get(i)))
                    {
                        regObject.put("installment_no", "");
                    }else
                    {
                        regObject.put("installment_no", sip_installment_array.get(i));
                    }
                }else{
                    regObject.put("installment_no", "");
                }

                if ("DAILY".equalsIgnoreCase(frequency_array.get(i)))
                {
                    String endDate = end_date_array.get(i);

                    if (endDate != null && !endDate.trim().isEmpty())
                    {
                        try
                        {

//                          LocalDate date = LocalDate.parse(endDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
//                          String formattedEndDate = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                            regObject.put("end_date", endDate);
//                          regObject.put("installment_amount", "");
                        } catch (Exception e)
                        {
                            e.printStackTrace();
                            regObject.put("end_date", "");
//                            regObject.put("installment_amount", amount_array.get(i));
                        }
                    } else {
                        regObject.put("end_date", "");
//                        regObject.put("installment_amount", amount_array.get(i));
                    }
                } else {
                    regObject.put("end_date", "");
                }
                regObject.put("installment_amount", amount_array.get(i));

                regObject.put("convenience_fee", "0");
                regObject.put("xsip_mandate_id", umrn_number);
                regObject.put("sub_broker_code", subbroker_code);
                regObject.put("euin_number", euin);
                regObject.put("euin_declaration", "Y");
                regObject.put("dpc_flag", "Y");
                if(first_payment_option.equalsIgnoreCase("SIP with first payment")) {
                    regObject.put("first_order_today", "Y");
                }else {
                    regObject.put("first_order_today", "N");
                }

                regObject.put("isip_mandate", "");
                regObject.put("sub_broker_arn", subbroker_arn);

                regObject.put("primary_holder_mobile", mobile);
                regObject.put("primary_holder_email", email);

                if(sip_stepup_required_array.size() > i && StringHelper.isNotEmpty(sip_stepup_required_array.get(i)) && sip_stepup_required_array.get(i).trim().equalsIgnoreCase("Y")){
                    regObject.put("step_up_required", sip_stepup_required_array.get(i));
                    regObject.put("step_up_start_date", sip_stepup_start_date_array.get(i));
                    regObject.put("step_up_end_date", sip_stepup_end_date_array.get(i));
                    regObject.put("step_up_frequency", sip_stepup_frequency_array.get(i));
                    regObject.put("step_up_amount", stepup_amount_array.get(i));
                }else{
                    regObject.put("step_up_required", "N");
                    regObject.put("step_up_start_date", "");
                    regObject.put("step_up_end_date", "");
                    regObject.put("step_up_frequency", "");
                    regObject.put("step_up_amount", "");
                }

                regObject.put("filler_1", "");
                regObject.put("filler_2", "");
                regObject.put("filler_3", "");
                regObject.put("filler_4", "");
                regObject.put("filler_5", "");
                regObject.put("member_unique_id", memberUniqueId);
                regDetailsArray.put(regObject);
            }

            JSONObject requestBody = new JSONObject();
            requestBody.put("reg_data", regDetailsArray);

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

            System.out.println("requestBody: " + requestBody.toString());
            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            String xSipRegistrationServiceApi_url= NseApiUrls.xSipRegistrationServiceApi_url;

            String reg_id = "";
            String reg_status = "";
            String reg_remark = "";

            Map<String, String> resMap = new HashMap<>();
            try
            {
                ResponseEntity<String> result = RestTemplateFactory.createRestTemplate().postForEntity(xSipRegistrationServiceApi_url, entity, String.class);
                String status_code = result.getStatusCode().toString();
                String responseBody = result.getBody().toString();

                System.out.println("status_code: " + status_code);
                System.out.println("responseBody: " + responseBody);

                JSONObject jsonObject = new JSONObject(responseBody);
                JSONArray jsonRegArray = jsonObject.getJSONArray("reg_data");

                List<NseOnlineSchemeMaster> nseOnlineSchemeMasters = nseOnlineSchemeMasterRepository.getSchemeBySchemeCode(scheme_code_array);

                int successCount = 0;
                int failureCount = 0;
                List<String> failedSchemes = new ArrayList<>();
                String lastSuccessRegId = "";

                String res_scheme_name = "";
                List<CartDto> master_cart_list = new ArrayList<>();
                for (int i = 0; i < jsonRegArray.length(); i++)
                {
                    JSONObject regDetail = jsonRegArray.getJSONObject(i);
                    reg_id = regDetail.optString("reg_id");
                    reg_status = regDetail.optString("reg_status");
                    reg_remark = regDetail.optString("reg_remark");

                    System.out.println("reg_id: " + reg_id);
                    System.out.println("reg_status: " + reg_status);
                    System.out.println("reg_remark: " + reg_remark);

                    // Extract values from regDetail JSONObject
                    String res_amc_code              = NseUtils.checkParem(regDetail.optString("amc_code"));
                    String res_sch_code              = NseUtils.checkParem(regDetail.optString("sch_code"));
                    String res_client_code           = NseUtils.checkParem(regDetail.optString("client_code"));
                    String res_bank_ref_no           = NseUtils.checkParem(regDetail.optString("bank_ref_no"));
                    String res_trans_mode            = NseUtils.checkParem(regDetail.optString("trans_mode"));
                    String res_dp_txn_mode           = NseUtils.checkParem(regDetail.optString("dp_txn_mode"));
                    String res_start_date            = NseUtils.checkParem(regDetail.optString("start_date"));
                    String res_frequency_type        = NseUtils.checkParem(regDetail.optString("frequency_type"));
                    String res_frequency_allowed     = NseUtils.checkParem(regDetail.optString("frequency_allowed"));
                    String res_installment_amount    = NseUtils.checkParem(regDetail.optString("installment_amount"));
                    String res_status                = NseUtils.checkParem(regDetail.optString("status"));
                    String res_member_code           = NseUtils.checkParem(regDetail.optString("member_code"));
                    String res_folio_no              = NseUtils.checkParem(regDetail.optString("folio_no"));
                    String res_sip_remarks           = NseUtils.checkParem(regDetail.optString("sip_remarks"));
                    String res_installment_no        = NseUtils.checkParem(regDetail.optString("installment_no"));
                    String res_convenience_fee       = NseUtils.checkParem(regDetail.optString("convenience_fee"));
                    String res_xsip_mandate_id       = NseUtils.checkParem(regDetail.optString("xsip_mandate_id"));
                    String res_sub_broker_code       = NseUtils.checkParem(regDetail.optString("sub_broker_code"));
                    String res_euin_number           = NseUtils.checkParem(regDetail.optString("euin_number"));
                    String res_euin_declaration      = NseUtils.checkParem(regDetail.optString("euin_declaration"));
                    String res_dpc_flag              = NseUtils.checkParem(regDetail.optString("dpc_flag"));
                    String res_first_order_today     = NseUtils.checkParem(regDetail.optString("first_order_today"));
                    String res_isip_mandate          = NseUtils.checkParem(regDetail.optString("isip_mandate"));
                    String res_sub_broker_arn        = NseUtils.checkParem(regDetail.optString("sub_broker_arn"));
                    String res_end_date              = NseUtils.checkParem(regDetail.optString("end_date"));
                    String res_primary_holder_mobile = NseUtils.checkParem(regDetail.optString("primary_holder_mobile"));
                    String res_primary_holder_email  = NseUtils.checkParem(regDetail.optString("primary_holder_email"));
                    String res_step_up_required      = NseUtils.checkParem(regDetail.optString("step_up_required"));
                    String res_step_up_start_date    = NseUtils.checkParem(regDetail.optString("step_up_start_date"));
                    String res_step_up_end_date      = NseUtils.checkParem(regDetail.optString("step_up_end_date"));
                    String res_step_up_frequency     = NseUtils.checkParem(regDetail.optString("step_up_frequency"));
                    String res_step_up_amout         = NseUtils.checkParem(regDetail.optString("step_up_amout"));
                    String res_filler_1              = NseUtils.checkParem(regDetail.optString("filler_1"));
                    String res_filler_2              = NseUtils.checkParem(regDetail.optString("filler_2"));
                    String res_filler_3              = NseUtils.checkParem(regDetail.optString("filler_3"));
                    String res_filler_4              = NseUtils.checkParem(regDetail.optString("filler_4"));
                    String res_filler_5              = NseUtils.checkParem(regDetail.optString("filler_5"));
                    String res_reg_id                = NseUtils.checkParem(regDetail.optString("reg_id"));
                    String res_reg_status            = NseUtils.checkParem(regDetail.optString("reg_status"));
                    String res_reg_remark            = NseUtils.checkParem(regDetail.optString("reg_remark"));
                    String res_member_unique_id      = NseUtils.checkParem(regDetail.optString("member_unique_id"));

                    // Print all values
                    System.out.println("res_amc_code              : " + res_amc_code);
                    System.out.println("res_sch_code              : " + res_sch_code);
                    System.out.println("res_client_code           : " + res_client_code);
                    System.out.println("res_bank_ref_no           : " + res_bank_ref_no);
                    System.out.println("res_trans_mode            : " + res_trans_mode);
                    System.out.println("res_dp_txn_mode           : " + res_dp_txn_mode);
                    System.out.println("res_start_date            : " + res_start_date);
                    System.out.println("res_frequency_type        : " + res_frequency_type);
                    System.out.println("res_frequency_allowed     : " + res_frequency_allowed);
                    System.out.println("res_installment_amount    : " + res_installment_amount);
                    System.out.println("res_status                : " + res_status);
                    System.out.println("res_member_code           : " + res_member_code);
                    System.out.println("res_folio_no              : " + res_folio_no);
                    System.out.println("res_sip_remarks           : " + res_sip_remarks);
                    System.out.println("res_installment_no        : " + res_installment_no);
                    System.out.println("res_convenience_fee       : " + res_convenience_fee);
                    System.out.println("res_xsip_mandate_id       : " + res_xsip_mandate_id);
                    System.out.println("res_sub_broker_code       : " + res_sub_broker_code);
                    System.out.println("res_euin_number           : " + res_euin_number);
                    System.out.println("res_euin_declaration      : " + res_euin_declaration);
                    System.out.println("res_dpc_flag              : " + res_dpc_flag);
                    System.out.println("res_first_order_today     : " + res_first_order_today);
                    System.out.println("res_isip_mandate          : " + res_isip_mandate);
                    System.out.println("res_sub_broker_arn        : " + res_sub_broker_arn);
                    System.out.println("res_end_date              : " + res_end_date);
                    System.out.println("res_primary_holder_mobile : " + res_primary_holder_mobile);
                    System.out.println("res_primary_holder_email  : " + res_primary_holder_email);
                    System.out.println("res_step_up_required      : " + res_step_up_required);
                    System.out.println("res_step_up_start_date    : " + res_step_up_start_date);
                    System.out.println("res_step_up_end_date      : " + res_step_up_end_date);
                    System.out.println("res_step_up_frequency     : " + res_step_up_frequency);
                    System.out.println("res_step_up_amout         : " + res_step_up_amout);
                    System.out.println("res_filler_1              : " + res_filler_1);
                    System.out.println("res_filler_2              : " + res_filler_2);
                    System.out.println("res_filler_3              : " + res_filler_3);
                    System.out.println("res_filler_4              : " + res_filler_4);
                    System.out.println("res_filler_5              : " + res_filler_5);
                    System.out.println("res_reg_id                : " + res_reg_id);
                    System.out.println("res_reg_status            : " + res_reg_status);
                    System.out.println("res_reg_remark            : " + res_reg_remark);
                    System.out.println("res_member_unique_id      : " + res_member_unique_id);

                    NseOnlineSchemeMaster nseOnlineSchemeMaster = nseOnlineSchemeMasters.stream().filter(obj -> obj.getSchemeCode().equalsIgnoreCase(res_sch_code)).findFirst().orElse(null);
                    System.out.println("nseOnlineSchemeMaster = " + nseOnlineSchemeMaster);
                    if(reg_status.equalsIgnoreCase("REG_SUCCESS"))
                    {
                        resMap.put(nseOnlineSchemeMaster.getSchemeName(), "REG_SUCCESS");
                    }else
                    {
                        resMap.put(nseOnlineSchemeMaster.getSchemeName(), reg_remark);
                    }

                    SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy");
                    Date res_start_dateDt = inputFormat.parse(res_start_date);
                    Date res_end_dateDt = null;

                    if(StringHelper.isNotEmpty(res_end_date))
                    {
                        res_end_dateDt = inputFormat.parse(res_end_date);
                    }

                    NseTransactions nsetrans = new NseTransactions();
                    nsetrans.setUrl(xSipRegistrationServiceApi_url);
                    nsetrans.setNse_request(requestBody.toString());
                    nsetrans.setNse_response(responseBody.toString());
                    nsetrans.setReg_id(res_reg_id);
                    nsetrans.setPayment_link("");
                    nsetrans.setPan(pan);
                    nsetrans.setName(name);
                    nsetrans.setBranch(user.getBranch());
                    nsetrans.setRm_name(user.getRm_name());
                    nsetrans.setSubbroker_name(user.getSubbroker_name());
                    nsetrans.setClient_name(client_name);
                    nsetrans.setIin_number(iin_number);
                    nsetrans.setScheme_name(nseOnlineSchemeMaster.getSchemeName());
                    nsetrans.setScheme_code(nseOnlineSchemeMaster.getSchemeCode());
                    nsetrans.setFolio_no(res_folio_no);
                    nsetrans.setAmount_units(res_installment_amount);
                    nsetrans.setFrequency(res_frequency_type);
                    nsetrans.setPeriod_day("");
                    nsetrans.setUmrn_no(res_xsip_mandate_id);
                    nsetrans.setPurchase_type("FRESH");
                    nsetrans.setPayment_ref_no("");
                    if(StringHelper.isNotEmpty(res_member_unique_id))
                    {
                        nsetrans.setUnique_number(res_member_unique_id);
                    }
                    nsetrans.setAuto_trxn_no("");
                    nsetrans.setSip_reg_no(res_reg_id);
                    nsetrans.setPayment_mode("");
                    nsetrans.setTopup_amount(0.0);
                    nsetrans.setBank_acc_no(bank_account_number);
                    nsetrans.setTransaction_number(res_reg_id);
                    nsetrans.setApplication_number("");
                    nsetrans.setTo_scheme_code("");
                    nsetrans.setTo_scheme_name("");
                    nsetrans.setTransaction_type("SIP Purchase");
                    nsetrans.setStart_date(res_start_dateDt);
                    nsetrans.setEnd_date(res_end_dateDt);

                    if(first_payment_option.equalsIgnoreCase("SIP with first payment"))
                    {
                        nsetrans.setFirst_order_today(1);
                    }else
                    {
                        nsetrans.setFirst_order_today(0);
                    }
                    nsetrans.setReturn_msg(res_reg_status);
                    nsetrans.setTransaction_status(res_reg_status);
                    nsetrans.setPayment_status("PENDING");
                    nsetrans.setActive_ceased_status("");
                    nsetrans.setRemarks(res_reg_remark);
                    nsetrans.setMandate_id(res_xsip_mandate_id);
                    nsetrans.setMandate_status("");
                    nsetrans.setEmandate_auth_flag("");
                    nsetrans.setApp_received_flag("");
                    nsetrans.setTransaction_date(new Date());
                    nsetrans.setUser_id(Integer.valueOf(userid));
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

                    if(source.equalsIgnoreCase("Mobile"))
                    {
                        CartDto cart = cartList.stream().filter(schemeData -> schemeData.getScheme_product_code().equalsIgnoreCase(nseOnlineSchemeMaster.getSchemeCode())).findAny().orElse(null);

                        if(cart != null)
                        {
                            cart.setPayment_type("");
                            cart.setPayment_mode("");
                            cart.setBank_name(bank_name);
                            cart.setBank_account_number(client_accno);
                            cart.setBank_ifsc(client_ifsccode);
                            cart.setBroker_code(broker_code);
                            cart.setEuin_code(euin);

                            if(reg_status.equalsIgnoreCase("REG_SUCCESS"))
                            {
                                cart.setStatus("SUCCESS");
                                cart.setActive(false);
                                cart.setStatus_date(new Date());
                                cart.setPayment_id(String.valueOf(currentTimeMillis));
                            }

                            master_cart_list.add(cart);
                        }
                    }

                    if(!cartid.isEmpty())
                    {
                        CartDto cart = cartList.stream().filter(schemeData -> schemeData.getScheme_product_code().equalsIgnoreCase(nseOnlineSchemeMaster.getSchemeCode())).findAny().orElse(null);

                        if(cart != null)
                        {
                            cart.setPayment_type("");
                            cart.setPayment_mode("");
                            cart.setBank_name(bank_name);
                            cart.setBank_account_number(client_accno);
                            cart.setBank_ifsc(client_ifsccode);
                            cart.setBroker_code(broker_code);
                            cart.setEuin_code(euin);

                            if(reg_status.equalsIgnoreCase("REG_SUCCESS"))
                            {
                                cart.setStatus("SUCCESS");
                                cart.setActive(false);
                                cart.setStatus_date(new Date());
                                cart.setPayment_id(String.valueOf(currentTimeMillis));
                            }

                            master_cart_list.add(cart);
                        }
                    }

                    if(reg_status.equalsIgnoreCase("REG_SUCCESS"))
                    {
                        successCount++;
                        lastSuccessRegId = res_reg_id;
                        resMap.put(nseOnlineSchemeMaster.getSchemeName(), "REG_SUCCESS");
                    }
                    else
                    {
                        failureCount++;
                        failedSchemes.add(nseOnlineSchemeMaster.getSchemeName() + ": " + res_reg_remark);
                        resMap.put(nseOnlineSchemeMaster.getSchemeName(), res_reg_remark);
                    }
                }

                System.out.println("cart = " +cartList);

                System.out.println("fasdfa = " + source);
                if(source.equalsIgnoreCase("Mobile"))
                {
                    System.out.println("cart = " +cartList);
                    for (CartDto cart : cartList)
                    {
                        System.out.println("cart = " +cartList);
                        cart.setPayment_type(payment_type);
                        cart.setPayment_mode("");
                        cart.setBank_name(bank_name);
                        cart.setBank_account_number(client_accno);
                        cart.setBank_ifsc(client_ifsccode);
                        cart.setBroker_code(broker_code);
                        cart.setEuin_code(euin);
                        System.out.println("cart = " +reg_status);

                        if (reg_status.equalsIgnoreCase("REG_SUCCESS"))
                        {
                            cart.setStatus("SUCCESS");
                            cart.setActive(false);
                            cart.setPayment_id(String.valueOf(currentTimeMillis));
                        }
                    }
                }
                if(master_cart_list != null && !master_cart_list.isEmpty())
                {
                    userServiceClient.updateCartByCartId(master_cart_list, token);
                }

                if (successCount > 0 && failureCount > 0)
                {
                    String message = String.format("%d out of %d SIP transactions succeeded. Please go to MyOrders Page check the details.",successCount, (successCount + failureCount));
                    message += "Failed transactions: " + String.join(", ", failedSchemes);
                    return NseUtils.transactionResponse(HttpStatus.BAD_REQUEST, message, resMap);
                }
                else if (successCount > 0)
                {
                    return NseUtils.transactionResponse(HttpStatus.OK,"Your SIP Orders successfully triggered! Last orderID: " + lastSuccessRegId,resMap);
                }
                else
                {
                    return NseUtils.commonResponse(reg_remark, HttpStatus.BAD_REQUEST);
                }

            } catch (HttpClientErrorException | HttpServerErrorException ex) {

                // For 4xx and 5xx responses
                System.out.println("sipRegistrationServiceApi::Status Code: " + ex.getStatusCode());
                System.out.println("sipRegistrationServiceApi::Response Body: " + ex.getResponseBodyAsString());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "Transaction failed. Please try again!"));
            } catch (Exception ex) {

                // Other exceptions (e.g., connection issues)
                ex.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "Transaction failed. Please try again!"));
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "Transaction failed. Please try again!"));
        }
    }


    @Operation(
            summary = "Redeem Investment",
            description = "Initiates a redemption transaction for an investor through NSE. Parameters vary for Web and Mobile based on 'source'.",
            parameters = {
                    // Common Parameters
                    @Parameter(name = "source", description = "Transaction source (web/mobile)", required = false),
                    @Parameter(name = "Authorization", description = "Bearer token for authentication", required = true, in = ParameterIn.HEADER),

                    // ✅ Web Parameters (all)
                    @Parameter(name = "amc_code", description = "AMC code (Web)", required = false),
                    @Parameter(name = "scheme_name", description = "Scheme name (Web)", required = false),
                    @Parameter(name = "scheme_code", description = "Scheme code (Web)", required = false),
                    @Parameter(name = "amount", description = "Redemption amount (Web)", required = false),
                    @Parameter(name = "folio", description = "Folio number (Web)", required = false),
                    @Parameter(name = "units", description = "Number of units to redeem (Web)", required = false),
                    @Parameter(name = "all_units", description = "Flag to redeem all units (Y/N) (Web)", required = false),
                    @Parameter(name = "redem_type", description = "Redemption type (Amount/Unit/All) (Web)", required = false),
                    @Parameter(name = "iin_number", description = "Investor Identification Number (Web)", required = false),
                    @Parameter(name = "reinvest_tag", description = "Reinvest tag (Web)", required = false),
                    @Parameter(name = "multiple_count", description = "Number of redemptions (Web)", required = false),
                    @Parameter(name = "broker_code", description = "Broker code (Web)", required = false),
                    @Parameter(name = "euin_code", description = "EUIN code (Web)", required = false),

                    // ✅ Mobile Parameters (limited)
                    @Parameter(name = "iin_number", description = "Investor Identification Number (Mobile)", required = false),
                    @Parameter(name = "broker_code", description = "Broker code (Mobile)", required = false),
                    @Parameter(name = "euin_code", description = "EUIN code (Mobile)", required = false)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Redemption successfully processed",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TransactionResponse.class),
                                    examples = @ExampleObject(
                                            name = "SuccessResponseExample",
                                            summary = "Redemption successful",
                                            value = """
                                            {
                                              "status": 200,
                                              "status_msg": "SUCCESS",
                                              "msg": "Redemption successfully processed",
                                              "return_msg": "OrderID: RED123456",
                                              "transaction_status": {
                                                "SBI Bluechip Fund - Growth": "Successfully Redeemed"
                                              }
                                            }
                                        """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request or missing data",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "BadRequestExample",
                                            summary = "Invalid redemption request",
                                            value = "{ \"status\": 400, \"error\": \"Bad Request\", \"message\": \"Invalid or missing parameters\" }"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal Server Error",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "ServerErrorExample",
                                            summary = "Unexpected server error",
                                            value = "{ \"status\": 500, \"error\": \"Internal Server Error\", \"message\": \"Something went wrong on the server\" }"
                                    )
                            )
                    )
            }
    )
    @PostMapping("/saveRedemption")
    public ResponseEntity<?> saveRedemption(
            HttpServletRequest request,
            @RequestParam(required = false) String amc_code,
            @RequestParam(required = false) String scheme_name,
            @RequestParam(required = false) String scheme_code,
            @RequestParam(required = false) String amount,
            @RequestParam(required = false) String folio,
            @RequestParam(required = false) String units,
            @RequestParam(required = false) String all_units,
            @RequestParam(required = false) String redem_type,
            @RequestParam(required = false) String iin_number,
            @RequestParam(required = false) String reinvest_tag,
//            @RequestParam(required = false) String multiple_count,
            @RequestParam(required = false) String broker_code,
            @RequestParam(required = false) String euin_code,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String cartid,
            @RequestParam(required = false) String subbroker_arn,
            @RequestParam(required = false) String subbroker_code,
            @RequestParam(required = false) String subbroker_name,
            @RequestParam(required = false) String ip_address,
            @RequestParam(required = false) String origin_user_id,
            @RequestParam(required = false) String origin_first_name,
            @RequestHeader("Authorization") String token)
    {

        String ipAddr = "";
        List<CartDto> cartList = null;
        long currentTimeMillis = System.currentTimeMillis();
        try
        {
            String userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            System.out.println("User ID from token: " + userid);

            amc_code = NseUtils.checkParem(amc_code);
            scheme_name = NseUtils.checkParem(scheme_name);
            scheme_code = NseUtils.checkParem(scheme_code);
            amount = NseUtils.checkParem(amount);
            folio = NseUtils.checkParem(folio);
            units = NseUtils.checkParem(units);
            all_units = NseUtils.checkParem(all_units);
            redem_type = NseUtils.checkParem(redem_type);
            iin_number = NseUtils.checkParem(iin_number);
            reinvest_tag = NseUtils.checkParem(reinvest_tag);
//            multiple_count = NseUtils.checkParem(multiple_count);
            broker_code = NseUtils.checkParem(broker_code);
            euin_code = NseUtils.checkParem(euin_code);
            source = NseUtils.checkParem(source);

            if(StringHelper.isEmpty(all_units)){all_units = "N";};

            List<String> amc_code_array = new ArrayList<String>();
            List<String> scheme_name_array = new ArrayList<String>();
            List<String> scheme_code_array = new ArrayList<String>();
            List<String> reinvest_tag_array = new ArrayList<String>();
            List<String> amount_array = new ArrayList<String>();
            List<String> units_array = new ArrayList<String>();
            List<String> all_units_array = new ArrayList<String>();
            List<String> redem_type_array = new ArrayList<String>();
            List<String> folio_array = new ArrayList<String>();
            List<String> cart_id_array = new ArrayList<String>();
            List<String> amc_name_array = new ArrayList<String>();
            List<String> trnx_type_array = new ArrayList<String>();
            List<String> purchase_type_array = new ArrayList<String>();

            UserDto user = userServiceClient.getUserById(Integer.valueOf(userid), token);

            if (user == null)
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "User not found"));
            }

            if(source.equalsIgnoreCase("Mobile"))
            {
                amc_code_array = new ArrayList<String>();
                scheme_name_array = new ArrayList<String>();
                scheme_code_array = new ArrayList<String>();
                reinvest_tag_array = new ArrayList<String>();
                amount_array = new ArrayList<String>();
                units_array = new ArrayList<String>();
                all_units_array = new ArrayList<String>();
                redem_type_array = new ArrayList<String>();
                folio_array = new ArrayList<String>();

                cartList = userServiceClient.getCartDetailsByUserID(Integer.parseInt(userid), "NSE", iin_number, "Redemption Purchase",token);

                if (cartList.isEmpty())
                {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "No Cart found for the user."));
                }

                for (CartDto cart : cartList)
                {
                    cart_id_array.add(String.valueOf(cart.getId()));
                    amc_code_array.add(cart.getScheme_company_code());
                    scheme_name_array.add(cart.getScheme_name());
                    scheme_code_array.add(cart.getScheme_product_code());
                    amount_array.add(cart.getAmount());

                    List<String> folio_arrayArr = new ArrayList<String>(Arrays.asList(cart.getFolio_no().split(",")));
                    folio_array.addAll(folio_arrayArr);

                    reinvest_tag_array.add(cart.getScheme_reinvest_tag());
                    trnx_type_array.add(cart.getTrnx_type());
                    purchase_type_array.add(cart.getTrnx_type());
                    amc_name_array.add(cart.getScheme_company());
                    redem_type_array.add(cart.getAmount_type());
                    units_array.add(cart.getUnits());

                    if(cart.getAmount_type().equalsIgnoreCase("Amount"))
                    {
                        all_units_array.add("N");

                    }else if(cart.getAmount_type().equalsIgnoreCase("Units"))
                    {
                        all_units_array.add("N");
                    }else if(cart.getAmount_type().equalsIgnoreCase("All Units"))
                    {
                        all_units_array.add("Y");
                    }
                }

            }else
            {
                if(!cartid.isEmpty())
                {
//                    amc_code_array = new ArrayList<String>();
//                    scheme_name_array = new ArrayList<String>();
                    scheme_code_array = new ArrayList<String>();
                    //reinvest_tag_array = new ArrayList<String>();
                    amount_array = new ArrayList<String>();
                    units_array = new ArrayList<String>();
                    all_units_array = new ArrayList<String>();
                    //redem_type_array = new ArrayList<String>();
                    folio_array = new ArrayList<String>();

                    List<Integer> ids = Arrays.stream(cartid.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());
                    System.out.println("ids = " + ids);
                    cartList = userServiceClient.getCartDetailsByIds(ids,token);

                    if (cartList.isEmpty())
                    {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "No Cart found for the user."));
                    }

                    for (CartDto cart : cartList)
                    {
                        cart_id_array.add(String.valueOf(cart.getId()));
//                        amc_code_array.add(cart.getScheme_company_code());
//                        scheme_name_array.add(cart.getScheme_name());
                        scheme_code_array.add(cart.getScheme_product_code());
                        amount_array.add(cart.getAmount());

                        List<String> folio_arrayArr = new ArrayList<String>(Arrays.asList(cart.getFolio_no().split(",")));
                        folio_array.addAll(folio_arrayArr);

//                        reinvest_tag_array.add(cart.getScheme_reinvest_tag());
//                        trnx_type_array.add(cart.getTrnx_type());
//                        purchase_type_array.add(cart.getTrnx_type());
//                        amc_name_array.add(cart.getScheme_company());
//                        redem_type_array.add(cart.getAmount_type());
                        units_array.add(cart.getUnits());

                        if(cart.getAmount_type().equalsIgnoreCase("Amount"))
                        {
                            all_units_array.add("N");

                        }else if(cart.getAmount_type().equalsIgnoreCase("Units"))
                        {
                            all_units_array.add("N");
                        }else if(cart.getAmount_type().equalsIgnoreCase("All Units"))
                        {
                            all_units_array.add("Y");
                        }
                    }
                }else{
//                    amc_code_array = new ArrayList<String>(Arrays.asList(amc_code.split(",")));
//                    scheme_name_array = new ArrayList<String>(Arrays.asList(scheme_name.split(",")));
                    scheme_code_array = new ArrayList<String>(Arrays.asList(scheme_code.split(",")));
//                    reinvest_tag_array = new ArrayList<String>(Arrays.asList(reinvest_tag.split(",")));
                    amount_array = new ArrayList<String>(Arrays.asList(amount.split(",")));
                    units_array = new ArrayList<String>(Arrays.asList(units.split(",")));
                    all_units_array = new ArrayList<String>(Arrays.asList(all_units.split(",")));
//                    redem_type_array = new ArrayList<String>(Arrays.asList(redem_type.split(",")));
                    folio_array = new ArrayList<String>(Arrays.asList(folio.split(",")));
                }
            }

            String appln_id = "";
            String password = "";
            String nse_iin = user.getNse_iin_number().trim();
            String client_name = user.getClient_name().trim();
            String name = "";
            String pan = "";
            String selected_name = "";
            String login_name = user.getName().trim();

            String mobile = "";
            String email = "";

            if(!nse_iin.equalsIgnoreCase(iin_number))
            {
                UserBseNseDto nse = null;

                try {
                    nse = userServiceClient.getUserBseNseDetailsByIinNumber(client_name, iin_number,token);
                } catch (FeignException e) {
                    if (e.status() == 400) {
                        System.out.println("Bad Request: " + e.getMessage());
                    } else if (e.status() == 404) {
                        System.out.println("User not found: " + e.getMessage());
                    } else {
                        System.out.println("Feign error: " + e.status() + " - " + e.getMessage());
                    }
                }

                if(nse == null)
                {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "No User Found"));
                }

                pan = nse.getPan();
                name = nse.getName();
                selected_name = name + " (" + userid + ")";

                mobile = nse.getMobile();
                email = nse.getEmail();

            }else
            {
                pan = user.getPan();
                name = user.getName();
                selected_name = name + " (" + userid + ")";

                mobile = user.getMobile();
                email = user.getEmail();
            }

            BseNseKeyDto nsekey = userServiceClient.getByClientName(client_name,token);
            String host = nsekey.getDomain_url();
            String broker_code1 = nsekey.getBrokerCode();

            broker_code = broker_code1;
            appln_id = nsekey.getNse_appln_id();
            password = nsekey.getNse_password();


            if(broker_code.isEmpty())
            {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), broker_code + " does not have the NSE credentials. Kindly update."));
            }

            JSONArray regDetailsArray = new JSONArray();

            for (int i = 0; i < scheme_code_array.size(); i++)
            {
                String memberUniqueId = "REM" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + UniqueIDProvider.generateUniquePin(3);
                JSONObject regObject = new JSONObject();

                regObject.put("order_ref_number", "");
                regObject.put("scheme_code", scheme_code_array.get(i));
                regObject.put("trxn_type", "R");
                regObject.put("buy_sell_type", "");
                regObject.put("client_code", iin_number);
                regObject.put("demat_physical", "P");
                regObject.put("folio_no", folio_array.get(i));
                regObject.put("remarks", "");
                regObject.put("kyc_flag", "Y");
                regObject.put("sub_broker_code", "");
                regObject.put("euin_number", euin_code);
                regObject.put("euin_declaration", "Y");
                regObject.put("min_redemption_flag", "N");
                regObject.put("dpc_flag", "Y");

                String all_uni = all_units_array.get(i);

                if (all_uni.equalsIgnoreCase("Y"))
                {
                    regObject.put("all_units", "Y");
                    regObject.put("order_amount", "");
                    regObject.put("redemption_units", "");
                } else
                {
                    regObject.put("all_units", "N");

                    if (amount_array.get(i) != null && !amount_array.get(i).isEmpty() && !amount_array.get(i).equalsIgnoreCase("0"))
                    {
                        regObject.put("order_amount", amount_array.get(i));
                        regObject.put("redemption_units", "");
                    }
                    else if (units_array.get(i) != null && !units_array.get(i).isEmpty())
                    {
                        regObject.put("order_amount", "");
                        regObject.put("redemption_units", units_array.get(i));
                    }
                }

                regObject.put("sub_broker_arn", subbroker_code);
                regObject.put("bank_ref_no", "");
                regObject.put("account_no", user.getBank_account_number1());
                regObject.put("mobile_no", mobile);
                regObject.put("email", email);
                regObject.put("mandate_id", "");
                regObject.put("filler1", "");
                regObject.put("member_unique_id", memberUniqueId);

                regDetailsArray.put(regObject);
            }
            System.out.println("regDetailsArray = " + regDetailsArray);

            JSONObject requestBody = new JSONObject();
            requestBody.put("transaction_details", regDetailsArray);

            BseNseOnlineAccessDto online_access = null;

            try
            {
                online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, broker_code,token);
            }catch(FeignException ex)
            {
                ex.printStackTrace();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new CommonResponse(StatusMessage.NseFailureCode, StatusMessage.NseFailureMessage, "NSE Online Credentials Not available. Please contact your RM"));
            }

            if (online_access == null)
            {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new CommonResponse(StatusMessage.NseFailureCode, StatusMessage.NseFailureMessage, "NSE Online Credentials Not available. Please contact your RM"));
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
            headers.set("Accept-Language", "en-US");
            headers.set("Connection", "keep-alive");
            headers.set("Referer", "");
            headers.set("Accept-Encoding", "gzip");

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            String sipLumpsumInvestment_url= NseApiUrls.sipLumpsumInvestment_url;

            String trxn_order_id = "";
            String trxn_status = "";
            String trxn_remark = "";
            String res_scheme_name = "";

            int successCount = 0;
            int failureCount = 0;
            List<String> failedSchemes = new ArrayList<>();
            String lastSuccessRegId = "";

            Map<String, String> resMap = new HashMap<String, String>();
            List<NseOnlineSchemeMaster> nseOnlineSchemeMasters = nseOnlineSchemeMasterRepository.getSchemeBySchemeCode(scheme_code_array);
            try
            {
                ResponseEntity<String> result = RestTemplateFactory.createRestTemplate().postForEntity(sipLumpsumInvestment_url, entity, String.class);
                String status_code = result.getStatusCode().toString();
                String responseBody = result.getBody().toString();

                JSONObject jsonObject = new JSONObject(responseBody);
                JSONArray jsonRegArray = jsonObject.getJSONArray("transaction_details");

                for (int i = 0; i < jsonRegArray.length(); i++)
                {
                    JSONObject regDetail = jsonRegArray.getJSONObject(i);
                    trxn_order_id = regDetail.optString("trxn_order_id");
                    trxn_status = regDetail.optString("trxn_status");
                    trxn_remark = regDetail.optString("trxn_remark");
                }

                for (int i = 0; i < jsonRegArray.length(); i++)
                {
                    JSONObject regDetail = jsonRegArray.getJSONObject(i);

                    trxn_order_id = regDetail.optString("trxn_order_id");
                    trxn_status = regDetail.optString("trxn_status");
                    trxn_remark = regDetail.optString("trxn_remark");

                    System.out.println((i+1)+" :trxn_order_id: " + trxn_order_id);
                    System.out.println((i+1)+" :trxn_status: " + trxn_status);
                    System.out.println((i+1)+" :trxn_remark: " + trxn_remark);



                    String res_order_ref_number    = NseUtils.checkParem(regDetail.optString("order_ref_number"));
                    String res_scheme_code         = NseUtils.checkParem(regDetail.optString("scheme_code"));
                    String res_trxn_type           = NseUtils.checkParem(regDetail.optString("trxn_type"));
                    String res_buy_sell_type       = NseUtils.checkParem(regDetail.optString("buy_sell_type"));
                    String res_client_code         = NseUtils.checkParem(regDetail.optString("client_code"));
                    String res_demat_physical      = NseUtils.checkParem(regDetail.optString("demat_physical"));
                    String res_order_amount        = NseUtils.checkParem(regDetail.optString("order_amount"));
                    String res_folio_no            = NseUtils.checkParem(regDetail.optString("folio_no"));
                    String res_remarks             = NseUtils.checkParem(regDetail.optString("remarks"));
                    String res_kyc_flag            = NseUtils.checkParem(regDetail.optString("kyc_flag"));
                    String res_sub_broker_code     = NseUtils.checkParem(regDetail.optString("sub_broker_code"));
                    String res_euin_number         = NseUtils.checkParem(regDetail.optString("euin_number"));
                    String res_euin_declaration    = NseUtils.checkParem(regDetail.optString("euin_declaration"));
                    String res_min_redemption_flag = NseUtils.checkParem(regDetail.optString("min_redemption_flag"));
                    String res_dpc_flag            = NseUtils.checkParem(regDetail.optString("dpc_flag"));
                    String res_all_units           = NseUtils.checkParem(regDetail.optString("all_units"));
                    String res_redemption_units    = NseUtils.checkParem(regDetail.optString("redemption_units"));
                    String res_sub_broker_arn      = NseUtils.checkParem(regDetail.optString("sub_broker_arn"));
                    String res_bank_ref_no         = NseUtils.checkParem(regDetail.optString("bank_ref_no"));
                    String res_account_no          = NseUtils.checkParem(regDetail.optString("account_no"));
                    String res_mobile_no           = NseUtils.checkParem(regDetail.optString("mobile_no"));
                    String res_email               = NseUtils.checkParem(regDetail.optString("email"));
                    String res_mandate_id          = NseUtils.checkParem(regDetail.optString("mandate_id"));
                    String res_filler1             = NseUtils.checkParem(regDetail.optString("filler1"));
                    String res_trxn_order_id       = NseUtils.checkParem(regDetail.optString("trxn_order_id"));
                    String res_trxn_status         = NseUtils.checkParem(regDetail.optString("trxn_status"));
                    String res_trxn_remark         = NseUtils.checkParem(regDetail.optString("trxn_remark"));
                    String res_member_unique_id    = NseUtils.checkParem(regDetail.optString("member_unique_id"));
                    String res_reg_remark           = NseUtils.checkParem(regDetail.optString("reg_remark"));

                    System.out.println("res_order_ref_number    = " + res_order_ref_number);
                    System.out.println("res_scheme_code         = " + res_scheme_code);
                    System.out.println("res_trxn_type           = " + res_trxn_type);
                    System.out.println("res_buy_sell_type       = " + res_buy_sell_type);
                    System.out.println("res_client_code         = " + res_client_code);
                    System.out.println("res_demat_physical      = " + res_demat_physical);
                    System.out.println("res_order_amount        = " + res_order_amount);
                    System.out.println("res_folio_no            = " + res_folio_no);
                    System.out.println("res_remarks             = " + res_remarks);
                    System.out.println("res_kyc_flag            = " + res_kyc_flag);
                    System.out.println("res_sub_broker_code     = " + res_sub_broker_code);
                    System.out.println("res_euin_number         = " + res_euin_number);
                    System.out.println("res_euin_declaration    = " + res_euin_declaration);
                    System.out.println("res_min_redemption_flag = " + res_min_redemption_flag);
                    System.out.println("res_dpc_flag            = " + res_dpc_flag);
                    System.out.println("res_all_units           = " + res_all_units);
                    System.out.println("res_redemption_units    = " + res_redemption_units);
                    System.out.println("res_sub_broker_arn      = " + res_sub_broker_arn);
                    System.out.println("res_bank_ref_no         = " + res_bank_ref_no);
                    System.out.println("res_account_no          = " + res_account_no);
                    System.out.println("res_mobile_no           = " + res_mobile_no);
                    System.out.println("res_email               = " + res_email);
                    System.out.println("res_mandate_id          = " + res_mandate_id);
                    System.out.println("res_filler1             = " + res_filler1);
                    System.out.println("res_trxn_order_id       = " + res_trxn_order_id);
                    System.out.println("res_trxn_status         = " + res_trxn_status);
                    System.out.println("res_trxn_remark         = " + res_trxn_remark);
                    System.out.println("res_member_unique_id    = " + res_member_unique_id);

                    NseOnlineSchemeMaster nseOnlineSchemeMaster = nseOnlineSchemeMasters.stream().filter(obj -> obj.getSchemeCode().equalsIgnoreCase(res_scheme_code)).findFirst().orElse(null);

                    if(trxn_status.equalsIgnoreCase("TRXN SUCCESS"))
                    {
                        resMap.put(nseOnlineSchemeMaster.getSchemeName(), trxn_status);
                    }else
                    {
                        resMap.put(nseOnlineSchemeMaster.getSchemeName(), trxn_remark);
                    }

                    NseTransactions nsetrans = new NseTransactions();
                    nsetrans.setUrl(sipLumpsumInvestment_url);
                    nsetrans.setNse_request(requestBody.toString());
                    nsetrans.setNse_response(responseBody.toString());
                    nsetrans.setReg_id(res_trxn_order_id);
                    nsetrans.setPayment_link("");
                    nsetrans.setPan(pan);
                    nsetrans.setName(name);
                    nsetrans.setBranch(user.getBranch());
                    nsetrans.setRm_name(user.getRm_name());
                    nsetrans.setSubbroker_name(user.getSubbroker_name());
                    nsetrans.setClient_name(client_name);
                    nsetrans.setIin_number(iin_number);
                    nsetrans.setScheme_name(nseOnlineSchemeMaster.getSchemeName());
                    nsetrans.setScheme_code(nseOnlineSchemeMaster.getSchemeCode());
                    nsetrans.setFolio_no(res_folio_no);
                    if(!res_order_amount.isEmpty())
                    {
                        nsetrans.setAmount_units(res_order_amount);
                    }else if(!res_redemption_units.isEmpty()){
                        nsetrans.setAmount_units(res_redemption_units);
                    }else if(res_all_units.equalsIgnoreCase("Y"))
                    {
                        nsetrans.setAmount_units("all units");
                    }else {
                        nsetrans.setAmount_units("");
                    }
                    nsetrans.setFrequency("");
                    nsetrans.setPeriod_day("");
                    nsetrans.setUmrn_no(res_mandate_id);
                    if(StringHelper.isNotEmpty(res_buy_sell_type))
                    {
                        nsetrans.setPurchase_type(res_buy_sell_type);
                    }else
                    {
                        nsetrans.setPurchase_type("FRESH");
                    }
                    nsetrans.setPayment_ref_no("");
                    if(StringHelper.isNotEmpty(res_member_unique_id))
                    {
                        nsetrans.setUnique_number(res_member_unique_id);
                    }
                    nsetrans.setAuto_trxn_no("");
                    nsetrans.setSip_reg_no("");
                    nsetrans.setPayment_mode("");
                    nsetrans.setTopup_amount(0.0);
                    nsetrans.setBank_acc_no(res_account_no);
                    if(StringHelper.isNotEmpty(res_trxn_order_id))
                    {
                        nsetrans.setTransaction_number(res_trxn_order_id);
                    }
                    nsetrans.setApplication_number("");
                    nsetrans.setTo_scheme_code("");
                    nsetrans.setTo_scheme_name("");
                    nsetrans.setTransaction_type("Redemption Transaction");
                    nsetrans.setReturn_msg(res_trxn_status);
                    nsetrans.setTransaction_status(res_trxn_status);
                    nsetrans.setPayment_status("PENDING");
                    nsetrans.setActive_ceased_status("");
                    nsetrans.setRemarks(trxn_remark);
                    nsetrans.setMandate_id("");
                    nsetrans.setMandate_status("");
                    nsetrans.setEmandate_auth_flag("");
                    nsetrans.setApp_received_flag("");
                    nsetrans.setTransaction_date(new Date());
                    nsetrans.setUser_id(Integer.parseInt(userid));
                    if(source.equalsIgnoreCase("Mobile"))
                    {
                        nsetrans.setRegister_source("Mobile App");
                    }else
                    {
                        nsetrans.setRegister_source("Website");
                    }
                    nsetrans.setBroker_code(broker_code);
                    nsetrans.setEuin_number(res_euin_number);
                    nsetrans.setCc_received("");
                    nsetrans.setFund_trans_to_amc("");
                    nsetrans.setRefund_status("");
                    nsetrans.setRefund_amount("");
                    System.out.println("nseTrans = " + nsetrans);
                    nseTransactionService.save(nsetrans);

                    if(trxn_status.equalsIgnoreCase("TRXN SUCCESS"))
                    {
                        successCount++;
                        lastSuccessRegId = res_trxn_order_id;
                        resMap.put(nseOnlineSchemeMaster.getSchemeName(), "TRXN SUCCESS");
                    }
                    else
                    {
                        failureCount++;
                        failedSchemes.add(nseOnlineSchemeMaster.getSchemeName() + ": " + res_reg_remark);
                        resMap.put(nseOnlineSchemeMaster.getSchemeName(), res_reg_remark);
                    }
                }

                if(source.equalsIgnoreCase("Mobile"))
                {
                    for (CartDto cart : cartList)
                    {
                        cart.setPayment_type("");
                        cart.setPayment_mode("Redemption");
                        cart.setBank_name("");
                        cart.setBank_account_number("");
                        cart.setBank_ifsc("");
                        cart.setBroker_code(broker_code);
                        cart.setEuin_code(euin_code);

                        if(trxn_status.equalsIgnoreCase("TRXN SUCCESS"))
                        {
                            cart.setStatus("SUCCESS");
                            cart.setActive(false);
                            cart.setPayment_id(String.valueOf(currentTimeMillis));
                        }
                    }

                    userServiceClient.updateCartByCartId(cartList, token);

                    System.out.println("CART SAVED SUCCESSFULLY.");
                }

                if(!cartid.isEmpty())
                {
                    for (CartDto cart : cartList)
                    {
                        cart.setPayment_type("");
                        cart.setPayment_mode("Redemption");
                        cart.setBank_name("");
                        cart.setBank_account_number("");
                        cart.setBank_ifsc("");
                        cart.setBroker_code(broker_code);
                        cart.setEuin_code(euin_code);

                        if(trxn_status.equalsIgnoreCase("TRXN SUCCESS"))
                        {
                            cart.setStatus("SUCCESS");
                            cart.setActive(false);
                            cart.setPayment_id(String.valueOf(currentTimeMillis));
                        }
                    }

                    userServiceClient.updateCartByCartId(cartList, token);

                    System.out.println("CART SAVED SUCCESSFULLY.");
                }

                if (successCount > 0 && failureCount > 0)
                {
                    String message = String.format("%d out of %d Redemption transactions succeeded. Please go to MyOrders Page check the details.",successCount, (successCount + failureCount));
                    message += "Failed transactions: " + String.join(", ", failedSchemes);
                    return NseUtils.transactionResponse(HttpStatus.BAD_REQUEST, message, resMap);
                }
                else if (successCount > 0)
                {
                    return NseUtils.transactionResponse(HttpStatus.OK,"All Redemption Orders successfully triggered! Last orderID: " + lastSuccessRegId,resMap);
                }
                else
                {
                    return NseUtils.commonResponse(trxn_remark, HttpStatus.BAD_REQUEST);
                }
            } catch (HttpClientErrorException | HttpServerErrorException ex) {

                // For 4xx and 5xx responses
                System.out.println("orderEntryRedemtion::Status Code: " + ex.getStatusCode());
                System.out.println("orderEntryRedemtion::Response Body: " + ex.getResponseBodyAsString());

                if(trxn_status.equalsIgnoreCase("TRXN FAILED"))
                {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), ex.getResponseBodyAsString()));
                }
            } catch (Exception ex) {

                // Other exceptions (e.g., connection issues)
                ex.printStackTrace();
                if(trxn_status.equalsIgnoreCase("TRXN FAILED"))
                {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), ex.getMessage()));
                }
            }

            if(source.equalsIgnoreCase("Mobile"))
            {
                for (CartDto cart : cartList)
                {
                    cart.setPayment_type("");
                    cart.setPayment_mode("Redemption");
                    cart.setBank_name("");
                    cart.setBank_account_number("");
                    cart.setBank_ifsc("");
                    cart.setBroker_code(broker_code);
                    cart.setEuin_code(euin_code);

                    if(trxn_status.equalsIgnoreCase("TRXN SUCCESS"))
                    {
                        cart.setStatus("SUCCESS");
                        cart.setActive(false);
                        cart.setPayment_id(String.valueOf(currentTimeMillis));
                    }
                }

                userServiceClient.updateCartByCartId(cartList, token);
            }

            if (trxn_status.equalsIgnoreCase("TRXN SUCCESS"))
            {
                return NseUtils.transactionResponse(HttpStatus.OK, trxn_status + " Your Order is successfully triggered...! orderID: " + trxn_order_id, resMap);
            }else{
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), trxn_remark));
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "Transaction failed. Please try again!"));
        }
    }


    @Operation(
            summary = "Switch Investment",
            description = "Initiates a switch transaction from one mutual fund scheme to another via NSE. Parameters vary for Web and Mobile based on 'source'.",
            parameters = {
                    // Common Parameters
                    @Parameter(name = "source", description = "Transaction source (web/mobile)", required = false),
                    @Parameter(
                            name = "Authorization",
                            description = "Bearer token for authentication",
                            required = true,
                            in = ParameterIn.HEADER
                    ),

                    // ✅ Web Parameters (full set)
                    @Parameter(name = "amc_code", description = "AMC code (Web)", required = false),
                    @Parameter(name = "from_scheme_code", description = "Source scheme code (Web)", required = false),
                    @Parameter(name = "from_scheme_name", description = "Source scheme name (Web)", required = false),
                    @Parameter(name = "to_scheme_code", description = "Target scheme code (Web)", required = false),
                    @Parameter(name = "to_scheme_name", description = "Target scheme name (Web)", required = false),
                    @Parameter(name = "folio", description = "Folio number (Web)", required = false),
                    @Parameter(name = "amount", description = "Amount to switch (Web)", required = false),
                    @Parameter(name = "units", description = "Units to switch (Web)", required = false),
                    @Parameter(name = "switch_type", description = "Switch type (Amount/Unit/All) (Web)", required = false),
                    @Parameter(name = "all_units", description = "Flag to switch all units (Y/N) (Web)", required = false),
                    @Parameter(name = "iin_number", description = "Investor Identification Number (Web)", required = false),
                    @Parameter(name = "reinvest_tag", description = "Reinvest tag (Web)", required = false),
                    @Parameter(name = "multiple_count", description = "Number of switches (Web)", required = false),
                    @Parameter(name = "broker_code", description = "Broker code (Web)", required = false),
                    @Parameter(name = "euin_code", description = "EUIN code (Web)", required = false),

                    // ✅ Mobile Parameters (only 3)
                    @Parameter(name = "iin_number", description = "Investor Identification Number (Mobile)", required = false),
                    @Parameter(name = "broker_code", description = "Broker code (Mobile)", required = false),
                    @Parameter(name = "euin_code", description = "EUIN code (Mobile)", required = false)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Switch transaction successfully processed",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TransactionResponse.class),
                                    examples = @ExampleObject(
                                            name = "SuccessResponseExample",
                                            summary = "Switch successful",
                                            value = """
                                            {
                                              "status": 200,
                                              "status_msg": "SUCCESS",
                                              "msg": "Switch successfully processed",
                                              "return_msg": "OrderID: SW123456",
                                              "transaction_status": {
                                                "HDFC Flexi Cap Fund - Growth → HDFC Mid Cap Opportunities": "Successfully Switched"
                                              }
                                            }
                                        """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request or missing data",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "BadRequestExample",
                                            summary = "Invalid redemption request",
                                            value = "{ \"status\": 400, \"error\": \"Bad Request\", \"message\": \"Invalid or missing parameters\" }"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal Server Error",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "ServerErrorExample",
                                            summary = "Unexpected server error",
                                            value = "{ \"status\": 500, \"error\": \"Internal Server Error\", \"message\": \"Something went wrong on the server\" }"
                                    )
                            )
                    )
            }
    )
    @PostMapping("/saveSwitch")
    public ResponseEntity<?> saveSwitch(
            HttpServletRequest request,
            @RequestParam(required = false) String iin_number,
            @RequestParam(required = false) String count,
            @RequestParam(required = false) String amc_code,
            @RequestParam(required = false) String from_scheme_name,
            @RequestParam(required = false) String from_scheme_code,
            @RequestParam(required = false) String to_scheme_name,
            @RequestParam(required = false) String to_scheme_code,
            @RequestParam(required = false) String folio,
            @RequestParam(required = false) String redem_type,
            @RequestParam(required = false) String amount,
            @RequestParam(required = false) String units,
            @RequestParam(required = false) String all_units,
            @RequestParam(required = false) String from_dividend_code,
            @RequestParam(required = false) String to_dividend_code,
            @RequestParam(required = false) String broker_code,
            @RequestParam(required = false) String euin_code,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String cartid,
            @RequestParam(required = false) String subbroker_arn,
            @RequestParam(required = false) String subbroker_code,
            @RequestParam(required = false) String subbroker_name,
            @RequestParam(required = false) String ip_address,
            @RequestParam(required = false) String origin_user_id,
            @RequestParam(required = false) String origin_first_name,
            @RequestHeader("Authorization") String token)
    {

        String ipAddr = "";
        List<CartDto> cartList = null;
        long currentTimeMillis = System.currentTimeMillis();
        try
        {
            String userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            System.out.println("User ID from token: " + userid);

            amc_code = NseUtils.checkParem(amc_code);
            from_scheme_name = NseUtils.checkParem(from_scheme_name);
            from_scheme_code = NseUtils.checkParem(from_scheme_code);
            to_scheme_name = NseUtils.checkParem(to_scheme_name);
            to_scheme_code = NseUtils.checkParem(to_scheme_code);
            folio = NseUtils.checkParem(folio);
            redem_type = NseUtils.checkParem(redem_type);
            amount = NseUtils.checkParem(amount);
            units = NseUtils.checkParem(units);
            all_units = NseUtils.checkParem(all_units);
            from_dividend_code = NseUtils.checkParem(from_dividend_code);
            to_dividend_code = NseUtils.checkParem(to_dividend_code);
            broker_code = NseUtils.checkParem(broker_code);
            euin_code = NseUtils.checkParem(euin_code);
            cartid = NseUtils.checkParem(cartid);
            subbroker_code = NseUtils.checkParem(subbroker_code);
            subbroker_name = NseUtils.checkParem(subbroker_name);
            subbroker_arn = NseUtils.checkParem(subbroker_arn);
            ip_address = NseUtils.checkParem(ip_address);
            origin_user_id = NseUtils.checkParem(origin_user_id);
            origin_first_name = NseUtils.checkParem(origin_first_name);

            if(StringHelper.isEmpty(all_units)){all_units = "N";};

            List<String> amc_code_array = new ArrayList<String>();
            List<String> from_scheme_name_array = new ArrayList<String>();
            List<String> from_scheme_code_array = new ArrayList<String>();
            List<String> from_dividend_code_array = new ArrayList<String>();
            List<String> to_scheme_name_array = new ArrayList<String>();
            List<String> to_scheme_code_array = new ArrayList<String>();
            List<String> to_dividend_code_array = new ArrayList<String>();
            List<String> amount_array = new ArrayList<String>();
            List<String> units_array = new ArrayList<String>();
            List<String> all_units_array = new ArrayList<String>();
            List<String> redem_type_array = new ArrayList<String>();
            List<String> folio_array = new ArrayList<String>();
            List<String> cart_id_array = new ArrayList<String>();
            List<String> from_amc_code_array = new ArrayList<String>();
            List<String> from_amc_name_array = new ArrayList<String>();
            List<String> from_reinvest_tag_array = new ArrayList<String>();
            List<String> to_amc_code_array = new ArrayList<String>();
            List<String> to_amc_name_array = new ArrayList<String>();
            List<String> to_reinvest_tag_array = new ArrayList<String>();

            UserDto user = userServiceClient.getUserById(Integer.valueOf(userid), token);

            if (user == null)
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "User not found"));
            }

            if(source.equalsIgnoreCase("Mobile"))
            {
                amc_code_array = new ArrayList<String>();
                from_scheme_name_array = new ArrayList<String>();
                from_scheme_code_array = new ArrayList<String>();
                from_dividend_code_array = new ArrayList<String>();
                to_scheme_name_array = new ArrayList<String>();
                to_scheme_code_array = new ArrayList<String>();
                to_dividend_code_array = new ArrayList<String>();
                amount_array = new ArrayList<String>();
                units_array = new ArrayList<String>();
                all_units_array = new ArrayList<String>();
                redem_type_array = new ArrayList<String>();
                folio_array = new ArrayList<String>();

                cartList = userServiceClient.getCartDetailsByUserID(Integer.parseInt(userid), "NSE", iin_number, "Switch Purchase",token);

                if (cartList.isEmpty())
                {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "No Cart found for the user."));
                }
                System.out.println("cartList = " + cartList);
                System.out.println("cartList = " + cartList.get(0).getAmount_type());
                for (CartDto cart : cartList)
                {
                    cart_id_array.add(String.valueOf(cart.getId()));
                    from_amc_name_array.add(cart.getScheme_company());
                    from_amc_code_array.add(cart.getScheme_company_code());
                    from_scheme_name_array.add(cart.getScheme_name());
                    from_scheme_code_array.add(cart.getScheme_product_code());
                    from_reinvest_tag_array.add(cart.getScheme_reinvest_tag());
                    to_amc_name_array.add(cart.getTo_scheme_company());
                    to_amc_code_array.add(cart.getTo_scheme_company_code());
                    to_scheme_name_array.add(cart.getTo_scheme_name());
                    to_scheme_code_array.add(cart.getTo_scheme_product_code());
                    to_reinvest_tag_array.add(cart.getTo_scheme_reinvest_tag());
                    List<String> folio_arrayArr = new ArrayList<String>(Arrays.asList(cart.getFolio_no().split(",")));
                    folio_array.addAll(folio_arrayArr);
                    amount_array.add(cart.getAmount());
                    units_array.add(cart.getUnits());
                    redem_type_array.add(cart.getAmount_type());

                    if(cart.getAmount_type().equalsIgnoreCase("Amount"))
                    {
                        all_units_array.add("N");

                    }else if(cart.getAmount_type().equalsIgnoreCase("Units"))
                    {
                        all_units_array.add("N");
                    }else if(cart.getAmount_type().equalsIgnoreCase("All Units"))
                    {
                        all_units_array.add("Y");
                    }
                }

            }else
            {
                if(!cartid.isEmpty())
                {

                    //amc_code_array = new ArrayList<String>();
                    //from_scheme_name_array = new ArrayList<String>();
                    from_scheme_code_array = new ArrayList<String>();
                    //from_dividend_code_array = new ArrayList<String>();
                    //to_scheme_name_array = new ArrayList<String>();
                    to_scheme_code_array = new ArrayList<String>();
                    //to_dividend_code_array = new ArrayList<String>();
                    amount_array = new ArrayList<String>();
                    units_array = new ArrayList<String>();
                    all_units_array = new ArrayList<String>();
                    //redem_type_array = new ArrayList<String>();
                    folio_array = new ArrayList<String>();

                    List<Integer> ids = Arrays.stream(cartid.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());
                    System.out.println("ids = " + ids);
                    cartList = userServiceClient.getCartDetailsByIds(ids,token);

                    if (cartList.isEmpty())
                    {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "No Cart found for the user."));
                    }

                    for (CartDto cart : cartList)
                    {
                        System.out.println("cart = " + cart);
                        cart_id_array.add(String.valueOf(cart.getId()));
                        //from_amc_name_array.add(cart.getScheme_company());
                        //from_amc_code_array.add(cart.getScheme_company_code());
                        //from_scheme_name_array.add(cart.getScheme_name());
                        from_scheme_code_array.add(cart.getScheme_product_code());
                        //from_reinvest_tag_array.add(cart.getScheme_reinvest_tag());
                        //to_amc_name_array.add(cart.getTo_scheme_company());
                        //to_amc_code_array.add(cart.getTo_scheme_company_code());
                        //to_scheme_name_array.add(cart.getTo_scheme_name());
                        to_scheme_code_array.add(cart.getTo_scheme_product_code());
                        //to_reinvest_tag_array.add(cart.getTo_scheme_reinvest_tag());
                        List<String> folio_arrayArr = new ArrayList<String>(Arrays.asList(cart.getFolio_no().split(",")));
                        folio_array.addAll(folio_arrayArr);
                        amount_array.add(cart.getAmount());
                        units_array.add(cart.getUnits());
                        //redem_type_array.add(cart.getAmount_type());

                        System.out.println("cart.getScheme_name(): " + cart.getAmount());

                        System.out.println("cart.getTotal_units(): " + cart.getTotal_units());

                        if(cart.getAmount_type().equalsIgnoreCase("Amount"))
                        {
                            all_units_array.add("N");

                        }else if(cart.getAmount_type().equalsIgnoreCase("Units"))
                        {
                            all_units_array.add("N");
                        }else if(cart.getAmount_type().equalsIgnoreCase("All Units"))
                        {
                            all_units_array.add("Y");
                        }
                    }
                }else {
                    //amc_code_array = new ArrayList<String>(Arrays.asList(amc_code.split(",")));
                    //from_scheme_name_array = new ArrayList<String>(Arrays.asList(from_scheme_name.split(",")));
                    from_scheme_code_array = new ArrayList<String>(Arrays.asList(from_scheme_code.split(",")));
                    //from_dividend_code_array = new ArrayList<String>(Arrays.asList(from_dividend_code.split(",")));
                    //to_scheme_name_array = new ArrayList<String>(Arrays.asList(to_scheme_name.split(",")));
                    to_scheme_code_array = new ArrayList<String>(Arrays.asList(to_scheme_code.split(",")));
                    //to_dividend_code_array = new ArrayList<String>(Arrays.asList(to_dividend_code.split(",")));
                    amount_array = new ArrayList<String>(Arrays.asList(amount.split(",")));
                    units_array = new ArrayList<String>(Arrays.asList(units.split(",")));
                    all_units_array = new ArrayList<String>(Arrays.asList(all_units.split(",")));
                    //redem_type_array = new ArrayList<String>(Arrays.asList(redem_type.split(",")));
                    folio_array = new ArrayList<String>(Arrays.asList(folio.split(",")));
                }
            }

            String nse_iin = user.getNse_iin_number();
            String pan = "";
            String name = "";
            String selected_name = "";
            String mobile = "";
            String email = "";
            String client_name = user.getClient_name();
            String login_name = user.getName();

            if(!nse_iin.equalsIgnoreCase(iin_number))
            {
                UserBseNseDto nse = null;

                try {
                    nse = userServiceClient.getUserBseNseDetailsByIinNumber(client_name, iin_number,token);
                } catch (FeignException e) {
                    if (e.status() == 400) {
                        System.out.println("Bad Request: " + e.getMessage());
                    } else if (e.status() == 404) {
                        System.out.println("User not found: " + e.getMessage());
                    } else {
                        System.out.println("Feign error: " + e.status() + " - " + e.getMessage());
                    }
                }

                if(nse == null)
                {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "No User Found"));
                }

                pan = nse.getPan();
                name = nse.getName();
                selected_name = name + " (" + userid + ")";

                mobile = nse.getMobile();
                email = nse.getEmail();

            }else
            {
                pan = user.getPan();
                name = user.getName();
                selected_name = name + " (" + userid + ")";

                mobile = user.getMobile();
                email = user.getEmail();
            }

            String appln_id = "";
            String password = "";
            String euin = "";
            String host = "";

            BseNseKeyDto nsekey = userServiceClient.getByClientName(client_name,token);
            host = nsekey.getDomain_url();
            String broker_code1 = nsekey.getBrokerCode();

            if(broker_code1 == null){broker_code1 = "";};
            broker_code = broker_code1;
            appln_id = nsekey.getNse_appln_id();
            password = nsekey.getNse_password();

            if(!euin_code.isEmpty())
            {
                euin = euin_code;
            }else
            {
                euin = nsekey.getEuin();
            }

            euin = euin.split(",")[0];

            if(broker_code.isEmpty())
            {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), broker_code + " does not have the NSE credentials. Kindly update."));
            }

            JSONArray regDetailsArray = new JSONArray();

            System.out.println("all utints array = " + all_units_array);
            System.out.println("amount = " + amount_array);

            for(int i=0; i<from_scheme_code_array.size(); i++)
            {
                String memberUniqueId = "SWI" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + UniqueIDProvider.generateUniquePin(3);
                JSONObject regObject = new JSONObject();
                regObject.put("order_ref_number", "");
                regObject.put("from_scheme_code", from_scheme_code_array.get(i));
                regObject.put("to_scheme_code", to_scheme_code_array.get(i));
                regObject.put("buy_sell_type", "");
                regObject.put("client_code", iin_number);
                regObject.put("demat_physical", "P");

                if(all_units_array.get(i).equalsIgnoreCase("Y"))
                {
                    regObject.put("amount", "");
                    regObject.put("units", "");
                    regObject.put("all_units", "Y");
                }else
                {
                    if(amount_array.get(i).equalsIgnoreCase("0") ||amount_array.get(i).isEmpty() ){
                        regObject.put("amount", "");
                        regObject.put("units", units_array.get(i));
                    }else{
                        regObject.put("amount", Integer.parseInt(amount_array.get(i)));
                        regObject.put("units", "");
                    }

                    regObject.put("all_units", "N");
                }

                regObject.put("folio_no", folio_array.get(i));
                regObject.put("remarks", "");
                regObject.put("kyc_flag", "Y");
                regObject.put("sub_broker_code", subbroker_code);
                regObject.put("euin_number", euin);
                regObject.put("euin_declaration", "Y");
                regObject.put("sub_broker_arn", subbroker_arn);
                regObject.put("mobile_no", mobile);
                regObject.put("email", email);
                regObject.put("filler1", "");
                regObject.put("filler2", "");
                regObject.put("filler3", "");
                regObject.put("trxn_so_order_id","");
                regObject.put("trxn_si_order_id","");
                regObject.put("member_unique_id",memberUniqueId);

                regDetailsArray.put(regObject);
            }

            JSONObject requestBody = new JSONObject();
            requestBody.put("transaction_details", regDetailsArray);

            BseNseOnlineAccessDto online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, broker_code,token);

            if (online_access == null)
            {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new CommonResponse(StatusMessage.NseFailureCode, StatusMessage.NseFailureMessage, "NSE Online Credentials Not available. Please contact your RM"));
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
            headers.set("Accept-Language", "en-US");
            headers.set("Connection", "keep-alive");
            headers.set("Referer", "");
            headers.set("Accept-Encoding", "gzip");
            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            String switchOrderEntryService_url= NseApiUrls.switchOrderEntryService_url;

            System.out.println("requestBody: " + requestBody.toString());

            String trxn_order_id = "";
            String trxn_status = "";
            String trxn_remark = "";

            int successCount = 0;
            int failureCount = 0;
            List<String> failedSchemes = new ArrayList<>();
            String lastSuccessRegId = "";

            Map<String, String> resMap = new HashMap<>();
            List<NseOnlineSchemeMaster> fromSchemeOnlineMasterList = nseOnlineSchemeMasterRepository.getNFOSchemeBySchemeCode(from_scheme_code_array);
            List<NseOnlineSchemeMaster> toSchemeOnlineMasterList = nseOnlineSchemeMasterRepository.getNFOSchemeBySchemeCode(to_scheme_code_array);
            try
            {
                ResponseEntity<String> result = RestTemplateFactory.createRestTemplate().postForEntity(switchOrderEntryService_url, entity, String.class);
                String status_code = result.getStatusCode().toString();
                String responseBody = result.getBody().toString();

                JSONObject jsonObject = new JSONObject(responseBody);
                System.out.println("jsonObject: " + jsonObject);
                JSONArray jsonRegArray = jsonObject.getJSONArray("transaction_details");

                String res_scheme_name= "";
                for (int i = 0; i < jsonRegArray.length(); i++)
                {

                        JSONObject regDetail = jsonRegArray.getJSONObject(i);

                        trxn_status = regDetail.optString("trxn_status");
                        trxn_remark = regDetail.optString("trxn_remark");

                        System.out.println("trxn_status: " + trxn_status);
                        System.out.println("trxn_remark: " + trxn_remark);

                        // Extract values from JSON
                        String res_order_ref_number   = NseUtils.checkParem(regDetail.optString("order_ref_number"));
                        String res_from_scheme_code   = NseUtils.checkParem(regDetail.optString("from_scheme_code"));
                        String res_to_scheme_code     = NseUtils.checkParem(regDetail.optString("to_scheme_code"));
                        String res_buy_sell_type      = NseUtils.checkParem(regDetail.optString("buy_sell_type"));
                        String res_client_code        = NseUtils.checkParem(regDetail.optString("client_code"));
                        String res_demat_physical     = NseUtils.checkParem(regDetail.optString("demat_physical"));
                        String res_amount             = NseUtils.checkParem(regDetail.optString("amount"));
                        String res_units              = NseUtils.checkParem(regDetail.optString("units"));
                        String res_all_units          = NseUtils.checkParem(regDetail.optString("all_units"));
                        String res_folio_no           = NseUtils.checkParem(regDetail.optString("folio_no"));
                        String res_remarks            = NseUtils.checkParem(regDetail.optString("remarks"));
                        String res_kyc_flag           = NseUtils.checkParem(regDetail.optString("kyc_flag"));
                        String res_sub_broker_code    = NseUtils.checkParem(regDetail.optString("sub_broker_code"));
                        String res_euin_number        = NseUtils.checkParem(regDetail.optString("euin_number"));
                        String res_euin_declaration   = NseUtils.checkParem(regDetail.optString("euin_declaration"));
                        String res_sub_broker_arn     = NseUtils.checkParem(regDetail.optString("sub_broker_arn"));
                        String res_mobile_no          = NseUtils.checkParem(regDetail.optString("mobile_no"));
                        String res_email              = NseUtils.checkParem(regDetail.optString("email"));
                        String res_filler1            = NseUtils.checkParem(regDetail.optString("filler1"));
                        String res_filler2            = NseUtils.checkParem(regDetail.optString("filler2"));
                        String res_filler3            = NseUtils.checkParem(regDetail.optString("filler3"));
                        String res_trxn_so_order_id   = NseUtils.checkParem(regDetail.optString("trxn_so_order_id"));
                        String res_trxn_si_order_id   = NseUtils.checkParem(regDetail.optString("trxn_si_order_id"));
                        String res_trxn_status        = NseUtils.checkParem(regDetail.optString("trxn_status"));
                        String res_trxn_remark        = NseUtils.checkParem(regDetail.optString("trxn_remark"));
                        String res_member_unique_id   = NseUtils.checkParem(regDetail.optString("member_unique_id"));
                    String res_reg_remark           = NseUtils.checkParem(regDetail.optString("reg_remark"));

                        // Print values
                        System.out.println("res_order_ref_number   : " + res_order_ref_number);
                        System.out.println("res_from_scheme_code   : " + res_from_scheme_code);
                        System.out.println("res_to_scheme_code     : " + res_to_scheme_code);
                        System.out.println("res_buy_sell_type      : " + res_buy_sell_type);
                        System.out.println("res_client_code        : " + res_client_code);
                        System.out.println("res_demat_physical     : " + res_demat_physical);
                        System.out.println("res_amount             : " + res_amount);
                        System.out.println("res_units              : " + res_units);
                        System.out.println("res_all_units          : " + res_all_units);
                        System.out.println("res_folio_no           : " + res_folio_no);
                        System.out.println("res_remarks            : " + res_remarks);
                        System.out.println("res_kyc_flag           : " + res_kyc_flag);
                        System.out.println("res_sub_broker_code    : " + res_sub_broker_code);
                        System.out.println("res_euin_number        : " + res_euin_number);
                        System.out.println("res_euin_declaration   : " + res_euin_declaration);
                        System.out.println("res_sub_broker_arn     : " + res_sub_broker_arn);
                        System.out.println("res_mobile_no          : " + res_mobile_no);
                        System.out.println("res_email              : " + res_email);
                        System.out.println("res_filler1            : " + res_filler1);
                        System.out.println("res_filler2            : " + res_filler2);
                        System.out.println("res_filler3            : " + res_filler3);
                        System.out.println("res_trxn_so_order_id   : " + res_trxn_so_order_id);
                        System.out.println("res_trxn_si_order_id   : " + res_trxn_si_order_id);
                        System.out.println("res_trxn_status        : " + res_trxn_status);
                        System.out.println("res_trxn_remark        : " + res_trxn_remark);
                        System.out.println("res_member_unique_id   : " + res_member_unique_id);

                        NseOnlineSchemeMaster fromSchemeMaster = fromSchemeOnlineMasterList.stream().filter(obj -> obj.getSchemeCode().equalsIgnoreCase(res_from_scheme_code)).findFirst().orElse(null);
                        NseOnlineSchemeMaster toSchemeMaster = toSchemeOnlineMasterList.stream().filter(obj -> obj.getSchemeCode().equalsIgnoreCase(res_to_scheme_code)).findFirst().orElse(null);

                        if(trxn_status.equalsIgnoreCase("TRXN SUCCESS"))
                        {
                            resMap.put(fromSchemeMaster.getSchemeName(), "TRXN SUCCESS");
                        }else
                        {
                            resMap.put(fromSchemeMaster.getSchemeName(), trxn_remark);
                        }

                        NseTransactions nsetrans = new NseTransactions();
                        nsetrans.setUrl(switchOrderEntryService_url);
                        nsetrans.setNse_request(requestBody.toString());
                        nsetrans.setNse_response(responseBody.toString());
                        nsetrans.setReg_id(res_order_ref_number);
                        nsetrans.setPayment_link("");
                        nsetrans.setPan(pan);
                        nsetrans.setName(name);
                        nsetrans.setBranch(user.getBranch());
                        nsetrans.setRm_name(user.getRm_name());
                        nsetrans.setSubbroker_name(user.getSubbroker_name());
                        nsetrans.setClient_name(client_name);
                        nsetrans.setIin_number(iin_number);
                        nsetrans.setScheme_name(fromSchemeMaster.getSchemeName());
                        nsetrans.setScheme_code(fromSchemeMaster.getSchemeCode());
                        nsetrans.setFolio_no(res_folio_no);

                        if(res_all_units.equalsIgnoreCase("Y"))
                        {
                            nsetrans.setAmount_units("All Units");
                        }else
                        {
                            if(StringHelper.isNotEmpty(res_units))
                            {
                                nsetrans.setAmount_units(res_units);
                            }else if(StringHelper.isNotEmpty(res_amount))
                            {
                                nsetrans.setAmount_units(res_amount);
                            }else{
                                nsetrans.setAmount_units("0");
                            }
                        }
                        nsetrans.setFrequency("");
                        nsetrans.setPeriod_day("");
                        nsetrans.setUmrn_no("");
                        nsetrans.setFirst_order_today(0);
                        if(StringHelper.isNotEmpty(res_buy_sell_type))
                        {
                            nsetrans.setPurchase_type(res_buy_sell_type);
                        }else
                        {
                            nsetrans.setPurchase_type("FRESH");
                        }
                        nsetrans.setPayment_ref_no("");
                        if(StringHelper.isNotEmpty(res_member_unique_id))
                        {
                            nsetrans.setUnique_number(res_member_unique_id);
                        }

                        nsetrans.setAuto_trxn_no("");
                        nsetrans.setSip_reg_no("");
                        nsetrans.setPayment_mode("");
                        nsetrans.setTopup_amount(0.0);
                        nsetrans.setBank_acc_no("");
                        nsetrans.setTransaction_number(res_trxn_so_order_id);
                        nsetrans.setApplication_number("");
                        nsetrans.setTo_scheme_code(toSchemeMaster.getSchemeName());
                        nsetrans.setTo_scheme_name(toSchemeMaster.getSchemeCode());
                        nsetrans.setTransaction_type("SWITCH Transaction");
                        nsetrans.setReturn_msg(res_trxn_status);
                        nsetrans.setTransaction_status(res_trxn_status);
                        nsetrans.setPayment_status("PENDING");
                        nsetrans.setActive_ceased_status("");
                        nsetrans.setRemarks(trxn_remark);
                        nsetrans.setMandate_id("");
                        nsetrans.setMandate_status("");
                        nsetrans.setEmandate_auth_flag("");
                        nsetrans.setApp_received_flag("");
                        nsetrans.setTransaction_date(new Date());
                        nsetrans.setUser_id(Integer.parseInt(userid));
                        if(source.equalsIgnoreCase("Mobile"))
                        {
                            nsetrans.setRegister_source("Mobile App");
                        }else
                        {
                            nsetrans.setRegister_source("Website");
                        }
                        nsetrans.setBroker_code(broker_code);
                        nsetrans.setEuin_number(res_euin_number);
                        nsetrans.setCc_received("");
                        nsetrans.setFund_trans_to_amc("");
                        nsetrans.setRefund_status("");
                        nsetrans.setRefund_amount("");
                        nseTransactionService.save(nsetrans);

                    if(trxn_status.equalsIgnoreCase("TRXN SUCCESS"))
                    {
                        successCount++;
                        lastSuccessRegId = res_trxn_so_order_id;
                        resMap.put(fromSchemeMaster.getSchemeName(), "TRXN SUCCESS");
                    }
                    else
                    {
                        failureCount++;
                        failedSchemes.add(fromSchemeMaster.getSchemeName() + ": " + res_reg_remark);
                        resMap.put(fromSchemeMaster.getSchemeName(), res_reg_remark);
                    }

                }
            } catch (HttpClientErrorException | HttpServerErrorException ex) {

                // For 4xx and 5xx responses
                System.out.println("orderEntryRedemtion::Status Code: " + ex.getStatusCode());
                System.out.println("orderEntryRedemtion::Response Body: " + ex.getResponseBodyAsString());

                if(trxn_status.equalsIgnoreCase("TRXN FAILED"))
                {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), ex.getResponseBodyAsString()));
                }
            } catch (Exception ex) {

                // Other exceptions (e.g., connection issues)
                ex.printStackTrace();
                if(trxn_status.equalsIgnoreCase("TRXN FAILED"))
                {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), ex.getMessage()));
                }
            }
            System.out.println("reg = " +  trxn_status);
            if(source.equalsIgnoreCase("Mobile"))
            {
                for (CartDto cart : cartList)
                {
                    cart.setPayment_type("");
                    cart.setPayment_mode("");
                    cart.setBank_name("");
                    cart.setBank_account_number("");
                    cart.setBank_ifsc("");
                    cart.setBroker_code(broker_code);
                    cart.setEuin_code(euin);
                    System.out.println("cartList = "+ cartList);

                    if(trxn_status.equalsIgnoreCase("TRXN SUCCESS"))
                    {
                        cart.setStatus("SUCCESS");
                        cart.setActive(false);
                        cart.setPayment_id(String.valueOf(currentTimeMillis));
                    }
                }

                userServiceClient.updateCartByCartId(cartList, token);
            }

            if(!cartid.isEmpty())
            {
                for (CartDto cart : cartList)
                {
                    cart.setPayment_type("");
                    cart.setPayment_mode("");
                    cart.setBank_name("");
                    cart.setBank_account_number("");
                    cart.setBank_ifsc("");
                    cart.setBroker_code(broker_code);
                    cart.setEuin_code(euin);

                    if(trxn_status.equalsIgnoreCase("TRXN SUCCESS"))
                    {
                        cart.setStatus("SUCCESS");
                        cart.setActive(false);
                        cart.setPayment_id(String.valueOf(currentTimeMillis));
                    }
                }

                userServiceClient.updateCartByCartId(cartList, token);
            }

            if (successCount > 0 && failureCount > 0)
            {
                String message = String.format("%d out of %d Switch transactions succeeded. Please go to MyOrders Page check the details.",successCount, (successCount + failureCount));
                message += "Failed transactions: " + String.join(", ", failedSchemes);
                return NseUtils.transactionResponse(HttpStatus.BAD_REQUEST, message, resMap);
            }
            else if (successCount > 0)
            {
                return NseUtils.transactionResponse(HttpStatus.OK,"All Switch Orders successfully triggered! Last orderID: " + lastSuccessRegId,resMap);
            }
            else
            {
                return NseUtils.commonResponse(trxn_remark, HttpStatus.BAD_REQUEST);
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "Transaction failed. Please try again!"));
        }
    }

    @Operation(
            summary = "Initiate STP Transaction",
            description = "Triggers a Systematic Transfer Plan (STP) transaction for an investor through NSE. Parameters vary based on source (web or mobile).",
            parameters = {
                    // Common Parameters
                    @Parameter(name = "source", description = "Transaction source (web/mobile)", required = false),
                    @Parameter(
                            name = "Authorization",
                            description = "Bearer token for authentication",
                            required = true,
                            in = ParameterIn.HEADER
                    ),

                    // ✅ Web Parameters (all)
                    @Parameter(name = "iin_number", description = "Investor Identification Number (Web)", required = false),
                    @Parameter(name = "multiple_count", description = "Number of STP instructions (Web)", required = false),
                    @Parameter(name = "amc_code", description = "AMC code (Web)", required = false),
                    @Parameter(name = "folio", description = "Folio number (Web)", required = false),
                    @Parameter(name = "amount", description = "Transfer amount per installment (Web)", required = false),
                    @Parameter(name = "start_date", description = "STP start date (yyyy-MM-dd) (Web)", required = false),
                    @Parameter(name = "end_date", description = "STP end date (yyyy-MM-dd) (Web)", required = false),
                    @Parameter(name = "frequency", description = "STP frequency (Monthly/Quarterly) (Web)", required = false),
                    @Parameter(name = "to_dividend_code", description = "Target scheme dividend code (Web)", required = false),
                    @Parameter(name = "from_dividend_code", description = "Source scheme dividend code (Web)", required = false),
                    @Parameter(name = "from_scheme_code", description = "Source scheme code (Web)", required = false),
                    @Parameter(name = "from_scheme_name", description = "Source scheme name (Web)", required = false),
                    @Parameter(name = "to_scheme_code", description = "Target scheme code (Web)", required = false),
                    @Parameter(name = "to_scheme_name", description = "Target scheme name (Web)", required = false),
                    @Parameter(name = "redem_type", description = "STP redemption type (Amount/Units) (Web)", required = false),
                    @Parameter(name = "split_start_date", description = "Split STP start date (Web)", required = false),
                    @Parameter(name = "stp_day", description = "Day of month for STP (Web)", required = false),
                    @Parameter(name = "broker_code", description = "Broker code (Web)", required = false),
                    @Parameter(name = "euin_code", description = "EUIN code (Web)", required = false),

                    // ✅ Mobile Parameters (limited)
                    @Parameter(name = "iin_number", description = "Investor Identification Number (Mobile)", required = false),
                    @Parameter(name = "broker_code", description = "Broker code (Mobile)", required = false),
                    @Parameter(name = "euin_code", description = "EUIN code (Mobile)", required = false)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "STP transaction successfully processed",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TransactionResponse.class),
                                    examples = @ExampleObject(
                                            name = "SuccessResponseExample",
                                            summary = "STP success example",
                                            value = """
                        {
                          "status": 200,
                          "status_msg": "SUCCESS",
                          "msg": "STP transaction successfully processed",
                          "return_msg": "OrderID: STP20250701",
                          "transaction_status": {
                            "HDFC Liquid Fund → HDFC Flexi Cap Fund": "STP successfully triggered"
                          }
                        }
                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request or missing data",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "BadRequestExample",
                                            summary = "Invalid STP request",
                                            value = "{ \"status\": 400, \"error\": \"Bad Request\", \"message\": \"Invalid or missing parameters\" }"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal Server Error",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "ServerErrorExample",
                                            summary = "Unexpected server error",
                                            value = "{ \"status\": 500, \"error\": \"Internal Server Error\", \"message\": \"Something went wrong on the server\" }"
                                    )
                            )
                    )
            }
    )
    @PostMapping("/saveStp")
    public ResponseEntity<?> saveStp(
            HttpServletRequest request,
            @RequestParam(required = false) String iin_number,
            @RequestParam(required = false) String multiple_count,
            @RequestParam(required = false) String amc_code,
            @RequestParam(required = false) String folio,
            @RequestParam(required = false) String amount,
            @RequestParam(required = false) String start_date,
            @RequestParam(required = false) String end_date,
            @RequestParam(required = false) String frequency,
            @RequestParam(required = false) String to_dividend_code,
            @RequestParam(required = false) String from_dividend_code,
            @RequestParam(required = false) String from_scheme_code,
            @RequestParam(required = false) String from_scheme_name,
            @RequestParam(required = false) String to_scheme_code,
            @RequestParam(required = false) String to_scheme_name,
            @RequestParam(required = false) String redem_type,
            @RequestParam(required = false) String split_start_date,
            @RequestParam(required = false) String stp_day,
            @RequestParam(required = false) String broker_code,
            @RequestParam(required = false) String euin_code,
            @RequestParam(required = false) String installment,
            @RequestParam(required = false) String first_order_today,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String cartid,
            @RequestParam(required = false) String subbroker_arn,
            @RequestParam(required = false) String subbroker_code,
            @RequestParam(required = false) String subbroker_name,
            @RequestParam(required = false) String ip_address,
            @RequestParam(required = false) String origin_user_id,
            @RequestParam(required = false) String origin_first_name,
            @RequestHeader("Authorization") String token)
    {

        String ipAddr = "";
        List<CartDto> cartList = null;
        long currentTimeMillis = System.currentTimeMillis();
        try
        {
            String userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            System.out.println("User ID from token: " + userid);

            iin_number = NseUtils.checkParem(iin_number);
            multiple_count = NseUtils.checkParem(multiple_count);
            amc_code = NseUtils.checkParem(amc_code);
            folio = NseUtils.checkParem(folio);
            amount = NseUtils.checkParem(amount);
            start_date = NseUtils.checkParem(start_date);
            end_date = NseUtils.checkParem(end_date);
            frequency = NseUtils.checkParem(frequency);
            to_dividend_code = NseUtils.checkParem(to_dividend_code);
            from_dividend_code = NseUtils.checkParem(from_dividend_code);
            from_scheme_code = NseUtils.checkParem(from_scheme_code);
            from_scheme_name = NseUtils.checkParem(from_scheme_name);
            to_scheme_code = NseUtils.checkParem(to_scheme_code);
            to_scheme_name = NseUtils.checkParem(to_scheme_name);
            redem_type = NseUtils.checkParem(redem_type);
            split_start_date = NseUtils.checkParem(split_start_date);
            stp_day = NseUtils.checkParem(stp_day);
            broker_code = NseUtils.checkParem(broker_code);
            euin_code = NseUtils.checkParem(euin_code);
            source = NseUtils.checkParem(source);
            first_order_today = NseUtils.checkParem(first_order_today);
            cartid = NseUtils.checkParem(cartid);
            subbroker_code = NseUtils.checkParem(subbroker_code);
            subbroker_name = NseUtils.checkParem(subbroker_name);
            subbroker_arn = NseUtils.checkParem(subbroker_arn);
            ip_address = NseUtils.checkParem(ip_address);
            origin_user_id = NseUtils.checkParem(origin_user_id);
            origin_first_name = NseUtils.checkParem(origin_first_name);

            if(first_order_today.isEmpty())
            {
                first_order_today = "N";
            }

            List<String> amc_code_array = new ArrayList<String>();
            List<String> from_scheme_name_array = new ArrayList<>();
            List<String> to_scheme_name_array = new ArrayList<String>();
            List<String> from_scheme_code_array = new ArrayList<String>();
            List<String> to_scheme_code_array = new ArrayList<String>();
            List<String> amount_array = new ArrayList<String>();
            List<String> folio_array = new ArrayList<String>();
            List<String> start_date_array = new ArrayList<String>();
            List<String> end_date_array = new ArrayList<String>();
            List<String> from_reinvest_tag_array = new ArrayList<String>();
            List<String> to_reinvest_tag_array = new ArrayList<String>();
            List<String> frequency_array = new ArrayList<String>();
            List<String> stp_day_array = new ArrayList<String>();
            List<String> split_start_date_array = new ArrayList<String>();
            List<String> cart_id_array = new ArrayList<String>();
            List<String> installment_array = new ArrayList<String>();
            List<Boolean> first_order_flag_array = new ArrayList<>();
            
            UserDto user = userServiceClient.getUserById(Integer.valueOf(userid), token);

            if (user == null)
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "User not found"));
            }

            if(source.equalsIgnoreCase("Mobile"))
            {
                amc_code_array = new ArrayList<String>();
                from_scheme_name_array = new ArrayList<>();
                to_scheme_name_array = new ArrayList<String>();
                from_scheme_code_array = new ArrayList<String>();
                to_scheme_code_array = new ArrayList<String>();
                amount_array = new ArrayList<String>();
                folio_array = new ArrayList<String>();
                start_date_array = new ArrayList<String>();
                end_date_array = new ArrayList<String>();
                from_reinvest_tag_array = new ArrayList<String>();
                to_reinvest_tag_array = new ArrayList<String>();
                frequency_array = new ArrayList<String>();
                stp_day_array = new ArrayList<String>();
                split_start_date_array = new ArrayList<String>();
                cart_id_array = new ArrayList<String>();

                try{
                    cartList = userServiceClient.getCartDetailsByUserID(Integer.parseInt(userid), "NSE", iin_number, "STP Purchase",token);
                }catch (FeignException e) {
                    return FeignErrorHandler.handle(e, "User Service", "No Cart found for the user.");
                }


                if (cartList.isEmpty())
                {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "No Cart found for the user."));
                }

                for (CartDto cart : cartList)
                {
                    cart_id_array.add(String.valueOf(cart.getId()));
                    amc_code_array.add(cart.getScheme_company_code());
                    from_scheme_name_array.add(cart.getScheme_name());
                    to_scheme_name_array.add(cart.getTo_scheme_name());
                    from_scheme_code_array.add(cart.getScheme_product_code());
                    to_scheme_code_array.add(cart.getTo_scheme_product_code());
                    amount_array.add(cart.getAmount());
                    List<String> folio_arrayArr = new ArrayList<String>(Arrays.asList(cart.getFolio_no().split(",")));
                    folio_array.addAll(folio_arrayArr);
                    start_date_array.add(cart.getStart_date());
                    end_date_array.add(cart.getEnd_date());
                    from_reinvest_tag_array.add(cart.getScheme_reinvest_tag());
                    to_reinvest_tag_array.add(cart.getTo_scheme_reinvest_tag());
                    frequency_array.add(cart.getFrequency());
                    stp_day_array.add(cart.getSip_date());
                    installment_array.add(cart.getInstallment());

                    String[] parts = cart.getStart_date().split("-");
                    split_start_date = parts[0];
                    split_start_date_array.add(split_start_date);
                }

            }else
            {
                if(!cartid.isEmpty())
                {
//                    amc_code_array = new ArrayList<String>();
                    from_scheme_name_array = new ArrayList<>();
                    to_scheme_name_array = new ArrayList<String>();
                    from_scheme_code_array = new ArrayList<String>();
                    to_scheme_code_array = new ArrayList<String>();
                    amount_array = new ArrayList<String>();
                    folio_array = new ArrayList<String>();
                    start_date_array = new ArrayList<String>();
                    end_date_array = new ArrayList<String>();
//                    from_reinvest_tag_array = new ArrayList<String>();
//                    to_reinvest_tag_array = new ArrayList<String>();
                    frequency_array = new ArrayList<String>();
//                    stp_day_array = new ArrayList<String>();
//                    split_start_date_array = new ArrayList<String>();
                    cart_id_array = new ArrayList<String>();

                    List<Integer> ids = Arrays.stream(cartid.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());
                    System.out.println("ids = " + ids);
                    cartList = userServiceClient.getCartDetailsByIds(ids,token);

                    try {
                        cartList = userServiceClient.getCartDetailsByIds(ids, token);
                    }catch (FeignException e) {
                        return FeignErrorHandler.handle(e, "User Service", "No Cart found for the user.");
                    }
                    if (cartList.isEmpty())
                    {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "No Cart found for the user."));
                    }

                    for (CartDto cart : cartList)
                    {
                        cart_id_array.add(String.valueOf(cart.getId()));
//                        amc_code_array.add(cart.getScheme_company_code());
                        from_scheme_name_array.add(cart.getScheme_name());
                        to_scheme_name_array.add(cart.getTo_scheme_name());
                        from_scheme_code_array.add(cart.getScheme_product_code());
                        to_scheme_code_array.add(cart.getTo_scheme_product_code());
                        amount_array.add(cart.getAmount());
                        List<String> folio_arrayArr = new ArrayList<String>(Arrays.asList(cart.getFolio_no().split(",")));
                        folio_array.addAll(folio_arrayArr);
                        start_date_array.add(cart.getStart_date());
                        end_date_array.add(cart.getEnd_date());
//                        from_reinvest_tag_array.add(cart.getScheme_reinvest_tag());
//                        to_reinvest_tag_array.add(cart.getTo_scheme_reinvest_tag());
                        frequency_array.add(cart.getFrequency());
//                        stp_day_array.add(cart.getSip_date());
                        installment_array.add(cart.getInstallment());

                        String[] parts = cart.getStart_date().split("-");
                        split_start_date = parts[0];
//                        split_start_date_array.add(split_start_date);
                        first_order_flag_array.add(cart.getFirst_order_flag());
                    }
                }else
                {
//                    amc_code_array = new ArrayList<String>(Arrays.asList(amc_code.split(",")));
//                    from_scheme_name_array = new ArrayList<String>(Arrays.asList(from_scheme_name.split(",")));
//                    to_scheme_name_array = new ArrayList<String>(Arrays.asList(to_scheme_name.split(",")));
                    from_scheme_code_array = new ArrayList<String>(Arrays.asList(from_scheme_code.split(",")));
                    to_scheme_code_array = new ArrayList<String>(Arrays.asList(to_scheme_code.split(",")));
                    amount_array = new ArrayList<String>(Arrays.asList(amount.split(",")));
                    folio_array = new ArrayList<String>(Arrays.asList(folio.split(",")));
                    start_date_array = new ArrayList<String>(Arrays.asList(start_date.split(",")));
                    end_date_array = new ArrayList<String>(Arrays.asList(end_date.split(",")));
//                    from_reinvest_tag_array = new ArrayList<String>(Arrays.asList(from_dividend_code.split(",")));
//                    to_reinvest_tag_array = new ArrayList<String>(Arrays.asList(to_dividend_code.split(",")));
                    frequency_array = new ArrayList<String>(Arrays.asList(frequency.split(",")));
//                    stp_day_array = new ArrayList<String>(Arrays.asList(stp_day.split(",")));
//                    split_start_date_array = new ArrayList<String>(Arrays.asList(split_start_date.split(",")));
                    System.out.println("installment = " + installment);
                    installment_array = new ArrayList<String>(Arrays.asList(installment.split(",")));
                    first_order_flag_array = Arrays.stream(first_order_today.split(",")) .map(value -> "Y".equalsIgnoreCase(value.trim())) .collect(Collectors.toList());
                }
            }

            String nse_iin = user.getNse_iin_number().trim();
            String pan = "";
            String name = "";
            String selected_name = "";
            String mobile = "";
            String email = "";
            String client_name = user.getClient_name();
            String login_name = user.getName();

            if(!nse_iin.equalsIgnoreCase(iin_number))
            {
                UserBseNseDto nse = null;

                try {
                    nse = userServiceClient.getUserBseNseDetailsByIinNumber(client_name, iin_number,token);
                } catch (FeignException e) {
                    if (e.status() == 400) {
                        System.out.println("Bad Request: " + e.getMessage());
                    } else if (e.status() == 404) {
                        System.out.println("User not found: " + e.getMessage());
                    } else {
                        System.out.println("Feign error: " + e.status() + " - " + e.getMessage());
                    }
                }

                if(nse == null)
                {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "No User Found"));
                }

                pan = nse.getPan();
                name = nse.getName();
                selected_name = name + " (" + userid + ")";

                mobile = nse.getMobile();
                email = nse.getEmail();

            }else
            {
                pan = user.getPan();
                name = user.getName();
                selected_name = name + " (" + userid + ")";

                mobile = user.getMobile();
                email = user.getEmail();
            }

            String appln_id = "";
            String password = "";
            String euin = "";
            String host = "";

            BseNseKeyDto nsekey = userServiceClient.getByClientName(client_name,token);
            host = nsekey.getDomain_url();
            String broker_code1 = nsekey.getBrokerCode();


            if(broker_code1 == null){broker_code1 = "";};



                broker_code = broker_code1;
                appln_id = nsekey.getNse_appln_id();
                password = nsekey.getNse_password();
                if(!euin_code.isEmpty())
                {
                    euin = euin_code;
                }else{
                    euin = nsekey.getEuin();
                }

            euin = euin.split(",")[0];

            if(broker_code.isEmpty())
            {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), broker_code + " does not have the NSE credentials. Kindly update."));
            }

            JSONArray regDetailsArray = new JSONArray();

            JSONObject regObject = null;

            Map<String, String> schemeMap = new HashMap<>();
            
            String fromSchemeName = "";
            String toSchemeName = "";

            for(int i=0; i< from_scheme_code_array.size(); i++)
            {
                String memberUniqueId = "STP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + UniqueIDProvider.generateUniquePin(3);
                String ref_number = "ADVSTP" + UniqueIDProvider.generateUniquePin(4);

                String scheme_code = from_scheme_code_array.get(i);
                String scheme_name = from_scheme_name_array.get(i);
                String toScheme_code = to_scheme_code_array.get(i);
                String toScheme_name = to_scheme_name_array.get(i);

                schemeMap.put(scheme_code, scheme_name);
                schemeMap.put(toScheme_code, toScheme_name);

                String start_date_str = start_date_array.get(i);

                if(start_date_str != null)
                {
                    start_date_str = start_date_str.replace("-", "/");
                }
                String end_date_str = "";

                if(end_date_array.size() > 0)
                {
                    end_date_str = end_date_array.get(i);

                    if(end_date_str != null)
                    {
                        end_date_str = end_date_str.replace("-", "/");
                    }
                    System.out.println("end_date_str = " + end_date_str);
                }

                regObject = new JSONObject();
                regObject.put("client_code", iin_number);
                regObject.put("from_scheme_code", from_scheme_code_array.get(i));
                regObject.put("to_scheme_code",to_scheme_code_array.get(i));
                regObject.put("buy_sell_type", "ADDITIONAL");
                regObject.put("transaction_mode","P");
                regObject.put("folio_no", folio_array.get(i));
                regObject.put("internal_ref_number",ref_number);
                regObject.put("start_date", start_date_str);
                regObject.put("frequency_type",frequency_array.get(i));

                String[] frequencyArray = frequency.split(",");
                for (int j = 0; j < frequencyArray.length; j++)
                {
                    if (frequencyArray[j].equalsIgnoreCase("Daily"))
                    {
                        regObject.put("no_of_transfers", "");
                    } else
                    {
                        if (j < installment_array.size()) {
                            regObject.put("no_of_transfers", installment_array.get(i));
                        } else {
                            regObject.put("no_of_transfers", "default_value_or_error_message");
                        }
                    }
                }

                if(!end_date_str.isEmpty())
                {
                    regObject.put("to_date", end_date_str);
                }else {
                    regObject.put("to_date", "");
                }

                regObject.put("installment_amount",amount_array.get(i));
                regObject.put("installment_units", "");

                String firstOrder = first_order_flag_array.get(i).equals(true) ? "Y" : "N";

                System.out.println("firstOrder = "+ firstOrder);

                if(!first_order_flag_array.isEmpty())
                {
                    regObject.put("first_order_today",firstOrder);
                }else
                {
                    regObject.put("first_order_today",first_order_today);
                }

                if(!cartid.isEmpty())
                {
                    first_order_today = first_order_flag_array.get(i).equals(true) ? "Y" : "N";
                }
                
                regObject.put("sub_broker_code", "");
                regObject.put("euin_declaration","Y");
                regObject.put("euin_number", euin);
                regObject.put("remarks","");
                regObject.put("sub_broker_arn_code","");
                regObject.put("mobile", mobile);
                regObject.put("email", email);
                regObject.put("member_unique_id", memberUniqueId);
                regDetailsArray.put(regObject);
            }

            JSONObject requestBody = new JSONObject();
            requestBody.put("reg_data", regDetailsArray);

            BseNseOnlineAccessDto online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, broker_code,token);

            if (online_access == null)
            {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new CommonResponse(StatusMessage.NseFailureCode, StatusMessage.NseFailureMessage, "NSE Online Credentials Not available. Please contact your RM"));
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
            headers.set("Accept-Language", "en-US");
            headers.set("Connection", "keep-alive");
            headers.set("Referer", "");
            headers.set("Accept-Encoding", "gzip");

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            String stpRegistrationService_url= NseApiUrls.stpRegistrationService_url;

            System.out.println("requestBody: " + requestBody.toString());

            String reg_id = "";
            String reg_status = "";
            String reg_remark = "";

            int successCount = 0;
            int failureCount = 0;
            List<String> failedSchemes = new ArrayList<>();
            String lastSuccessRegId = "";

            Map<String, String> resMap = new HashMap<>();
            List<NseOnlineSchemeMaster> fromSchemeOnlineMasterList = nseOnlineSchemeMasterRepository.getSchemeBySchemeCode(from_scheme_code_array);
            List<NseOnlineSchemeMaster> toSchemeOnlineMasterList = nseOnlineSchemeMasterRepository.getSchemeBySchemeCode(to_scheme_code_array);
            try
            {
                ResponseEntity<String> result = RestTemplateFactory.createRestTemplate().postForEntity(stpRegistrationService_url, entity, String.class);
                String status_code = result.getStatusCode().toString();
                String responseBody = result.getBody().toString();

                JSONObject jsonObject = new JSONObject(responseBody);
                System.out.println("jsonObject: " + jsonObject);
                JSONArray jsonRegArray = jsonObject.getJSONArray("reg_data");

                System.out.println("stpRegistrationService::responseBody: " + responseBody);

                for (int i = 0; i < jsonRegArray.length(); i++)
                {
                    JSONObject regDetail = jsonRegArray.getJSONObject(i);

                    reg_id = regDetail.optString("reg_id");
                    reg_status = regDetail.optString("reg_status");
                    reg_remark = regDetail.optString("reg_remark");

                    System.out.println("reg_id: " + reg_id);
                    System.out.println("reg_status" + reg_status);
                    System.out.println("reg_remark: " + reg_remark);

                    // Assuming you already have JSONObject regDetail

                    String res_client_code          = NseUtils.checkParem(regDetail.optString("client_code"));
                    String res_from_scheme_code     = NseUtils.checkParem(regDetail.optString("from_scheme_code"));
                    String res_to_scheme_code       = NseUtils.checkParem(regDetail.optString("to_scheme_code"));
                    String res_buy_sell_type        = NseUtils.checkParem(regDetail.optString("buy_sell_type"));
                    String res_transaction_mode     = NseUtils.checkParem(regDetail.optString("transaction_mode"));
                    String res_folio_no             = NseUtils.checkParem(regDetail.optString("folio_no"));
                    String res_internal_ref_number  = NseUtils.checkParem(regDetail.optString("internal_ref_number"));
                    String res_start_date           = NseUtils.checkParem(regDetail.optString("start_date"));
                    String res_frequency_type       = NseUtils.checkParem(regDetail.optString("frequency_type"));
                    String res_no_of_transfers      = NseUtils.checkParem(regDetail.optString("no_of_transfers"));
                    String res_to_date              = NseUtils.checkParem(regDetail.optString("to_date"));
                    String res_installment_amount   = NseUtils.checkParem(regDetail.optString("installment_amount"));
                    String res_installment_units    = NseUtils.checkParem(regDetail.optString("installment_units"));
                    String res_first_order_today    = NseUtils.checkParem(regDetail.optString("first_order_today"));
                    String res_sub_broker_code      = NseUtils.checkParem(regDetail.optString("sub_broker_code"));
                    String res_euin_declaration     = NseUtils.checkParem(regDetail.optString("euin_declaration"));
                    String res_euin_number          = NseUtils.checkParem(regDetail.optString("euin_number"));
                    String res_remarks              = NseUtils.checkParem(regDetail.optString("remarks"));
                    String res_sub_broker_arn_code  = NseUtils.checkParem(regDetail.optString("sub_broker_arn_code"));
                    String res_mobile               = NseUtils.checkParem(regDetail.optString("mobile"));
                    String res_email                = NseUtils.checkParem(regDetail.optString("email"));
                    String res_reg_id               = NseUtils.checkParem(regDetail.optString("reg_id"));
                    String res_reg_status           = NseUtils.checkParem(regDetail.optString("reg_status"));
                    String res_reg_remark           = NseUtils.checkParem(regDetail.optString("reg_remark"));
                    String res_member_unique_id     = NseUtils.checkParem(regDetail.optString("member_unique_id"));

                    // Print all values
                    System.out.println("res_client_code         = " + res_client_code);
                    System.out.println("res_from_scheme_code    = " + res_from_scheme_code);
                    System.out.println("res_to_scheme_code      = " + res_to_scheme_code);
                    System.out.println("res_buy_sell_type       = " + res_buy_sell_type);
                    System.out.println("res_transaction_mode    = " + res_transaction_mode);
                    System.out.println("res_folio_no            = " + res_folio_no);
                    System.out.println("res_internal_ref_number = " + res_internal_ref_number);
                    System.out.println("res_start_date          = " + res_start_date);
                    System.out.println("res_frequency_type      = " + res_frequency_type);
                    System.out.println("res_no_of_transfers     = " + res_no_of_transfers);
                    System.out.println("res_to_date             = " + res_to_date);
                    System.out.println("res_installment_amount  = " + res_installment_amount);
                    System.out.println("res_installment_units   = " + res_installment_units);
                    System.out.println("res_first_order_today   = " + res_first_order_today);
                    System.out.println("res_sub_broker_code     = " + res_sub_broker_code);
                    System.out.println("res_euin_declaration    = " + res_euin_declaration);
                    System.out.println("res_euin_number         = " + res_euin_number);
                    System.out.println("res_remarks             = " + res_remarks);
                    System.out.println("res_sub_broker_arn_code = " + res_sub_broker_arn_code);
                    System.out.println("res_mobile              = " + res_mobile);
                    System.out.println("res_email               = " + res_email);
                    System.out.println("res_reg_id              = " + res_reg_id);
                    System.out.println("res_reg_status          = " + res_reg_status);
                    System.out.println("res_reg_remark          = " + res_reg_remark);
                    System.out.println("res_member_unique_id    = " + res_member_unique_id);


                    fromSchemeName =  schemeMap.get(res_from_scheme_code);
                    toSchemeName =  schemeMap.get(res_to_scheme_code);

                    SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy");
                    Date res_start_dateDt = inputFormat.parse(res_start_date);
                    Date res_end_dateDt = null;

                    if(StringHelper.isNotEmpty(res_to_date))
                    {
                        res_end_dateDt = inputFormat.parse(res_to_date);
                    }

                    NseTransactions nsetrans = new NseTransactions();
                    nsetrans.setUrl(stpRegistrationService_url);
                    nsetrans.setNse_request(requestBody.toString());
                    nsetrans.setNse_response(responseBody);
                    nsetrans.setReg_id(res_reg_id);
                    nsetrans.setPayment_link("");
                    nsetrans.setPan(pan);
                    nsetrans.setName(name);
                    nsetrans.setBranch(user.getBranch());
                    nsetrans.setRm_name(user.getRm_name());
                    if(source.equalsIgnoreCase("Website"))
                    {
                        nsetrans.setSubbroker_name(subbroker_name);
                    }else{
                        nsetrans.setSubbroker_name(user.getSubbroker_name());
                    }
                    if(!subbroker_code.isEmpty())
                    {
                        nsetrans.setSubbroker_code(subbroker_code);
                    }
                    if(!subbroker_arn.isEmpty())
                    {
                        nsetrans.setSubbroker_arn(subbroker_arn);
                    }
                    nsetrans.setClient_name(client_name);
                    nsetrans.setIin_number(iin_number);
                    nsetrans.setScheme_name(fromSchemeName);
                    nsetrans.setScheme_code(from_scheme_code_array.get(i));
                    nsetrans.setFolio_no(res_folio_no);
                    nsetrans.setStart_date(res_start_dateDt);
                    nsetrans.setEnd_date(res_end_dateDt);

                    if(StringHelper.isNotEmpty(res_installment_amount))
                    {
                        nsetrans.setAmount_units(res_installment_amount);
                    }else if(StringHelper.isNotEmpty(res_installment_units))
                    {
                        nsetrans.setAmount_units(res_installment_units);
                    }else{
                        nsetrans.setAmount_units("0");
                    }
                    nsetrans.setFrequency(res_frequency_type);
                    nsetrans.setPeriod_day("");
                    nsetrans.setUmrn_no("");
                    if(StringHelper.isNotEmpty(res_buy_sell_type))
                    {
                        nsetrans.setPurchase_type(res_buy_sell_type);
                    }else
                    {
                        nsetrans.setPurchase_type("FRESH");
                    }

                    if(res_first_order_today.equalsIgnoreCase("Y"))
                    {
                        nsetrans.setFirst_order_today(1);
                    }else
                    {
                        nsetrans.setFirst_order_today(0);
                    }
                    nsetrans.setPayment_ref_no("");
                    nsetrans.setUnique_number(res_member_unique_id);
                    nsetrans.setAuto_trxn_no("");
                    nsetrans.setSip_reg_no(res_reg_id);
                    nsetrans.setPayment_mode("");
                    nsetrans.setTopup_amount(0.0);
                    nsetrans.setBank_acc_no("");
                    nsetrans.setTransaction_number(res_reg_id);
                    nsetrans.setTransaction_status(reg_status);
                    nsetrans.setApplication_number("");
                    nsetrans.setTo_scheme_code(to_scheme_code_array.get(i));
                    nsetrans.setTo_scheme_name(toSchemeName);
                    nsetrans.setTransaction_type("STP Transaction");
                    nsetrans.setPayment_status("PENDING");
                    nsetrans.setActive_ceased_status("");
                    nsetrans.setReturn_msg(res_reg_status);
                    nsetrans.setRemarks(res_reg_remark);
                    nsetrans.setMandate_id("");
                    nsetrans.setMandate_status("");
                    nsetrans.setEmandate_auth_flag("");
                    nsetrans.setApp_received_flag("");
                    nsetrans.setTransaction_date(new Date());
                    nsetrans.setUser_id(Integer.parseInt(userid));
                    if(source.equalsIgnoreCase("Mobile"))
                    {
                        nsetrans.setRegister_source("Mobile App");
                    }else
                    {
                        nsetrans.setRegister_source("Website");
                    }
                    nsetrans.setBroker_code(broker_code);
                    nsetrans.setEuin_number(res_euin_number);
                    nsetrans.setCc_received("");
                    nsetrans.setFund_trans_to_amc("");
                    nsetrans.setRefund_status("");
                    nsetrans.setRefund_amount("");
//                    nsetrans.setIp_address(ip_address);
//                    nsetrans.setOrigin_user_id(origin_user_id);
//                    nsetrans.setOrigin_first_name(origin_first_name);
                    nsetrans.setCart_id(cartIdAt(cart_id_array, i));
                    nseTransactionService.save(nsetrans);

                    if(source.equalsIgnoreCase("Mobile"))
                    {
                        for (CartDto cart : cartList)
                        {
                            cart.setPayment_type("");
                            cart.setPayment_mode("");
                            cart.setBank_name("");
                            cart.setBank_account_number("");
                            cart.setBank_ifsc("");
                            cart.setBroker_code(broker_code);
                            cart.setEuin_code(euin);

                            if(reg_status.equalsIgnoreCase("REG_SUCCESS"))
                            {
                                cart.setStatus("SUCCESS");
                                cart.setActive(false);
                                cart.setPayment_id(String.valueOf(currentTimeMillis));
                            }
                        }

                        userServiceClient.updateCartByCartId(cartList, token);
                    }

                    if(!cartid.isEmpty())
                    {
                        for (CartDto cart : cartList)
                        {
                            cart.setPayment_type("");
                            cart.setPayment_mode("");
                            cart.setBank_name("");
                            cart.setBank_account_number("");
                            cart.setBank_ifsc("");
                            cart.setBroker_code(broker_code);
                            cart.setEuin_code(euin);

                            if(reg_status.equalsIgnoreCase("REG_SUCCESS"))
                            {
                                cart.setStatus("SUCCESS");
                                cart.setActive(false);
                                cart.setPayment_id(String.valueOf(currentTimeMillis));
                            }
                        }

                        userServiceClient.updateCartByCartId(cartList, token);
                    }

                    if(res_reg_status.equalsIgnoreCase("REG_SUCCESS"))
                    {
                        successCount++;
                        lastSuccessRegId = res_reg_id;
                        resMap.put(fromSchemeName, "REG_SUCCESS");
                    }
                    else
                    {
                        failureCount++;
                        failedSchemes.add(fromSchemeName + ": " + res_reg_remark);
                        resMap.put(fromSchemeName, res_reg_remark);
                    }
                }
            }
            catch (HttpClientErrorException | HttpServerErrorException ex)
            {
                System.out.println("stpRegistrationService::Status Code: " + ex.getStatusCode());
                System.out.println("stpRegistrationService::Response Body: " + ex.getResponseBodyAsString());
            } catch (Exception ex)
            {
                ex.printStackTrace();
            }

            if (successCount > 0 && failureCount > 0)
            {
                String message = String.format("%d out of %d STP transactions succeeded. Please go to MyOrders Page check the details.",successCount, (successCount + failureCount));
                message += "Failed transactions: " + String.join(", ", failedSchemes);
                return NseUtils.commonResponse(message,HttpStatus.BAD_REQUEST);
            }
            else if (successCount > 0)
            {
                return NseUtils.transactionResponse(HttpStatus.OK,"All STP Orders successfully triggered! Last orderID: " + lastSuccessRegId,resMap);
            }
            else
            {
                String failureMessage = !failedSchemes.isEmpty() ? String.join(", ", failedSchemes)
                        : (StringHelper.isNotEmpty(reg_remark) ? reg_remark
                        : "STP registration failed. Please try again.");
                return NseUtils.commonResponse(failureMessage, HttpStatus.BAD_REQUEST);
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "Transaction failed. Please try again!"));
        }
    }

    @Operation(
            summary = "Initiate SWP Transaction",
            description = "Triggers a Systematic Withdrawal Plan (SWP) transaction for an investor through NSE. Parameters vary depending on whether the source is web or mobile.",
            parameters = {
                    // Common
                    @Parameter(name = "source", description = "Transaction source (web/mobile)", required = false),
                    @Parameter(
                            name = "Authorization",
                            description = "Bearer token for authentication",
                            required = true,
                            in = ParameterIn.HEADER
                    ),

                    // ✅ Website Parameters
                    @Parameter(name = "iin_number", description = "Investor Identification Number (Web)", required = false),
                    @Parameter(name = "multiple_count", description = "Number of SWP instructions to process (Web)", required = false),
                    @Parameter(name = "amc_code", description = "AMC code (Web)", required = false),
                    @Parameter(name = "folio", description = "Folio number (Web)", required = false),
                    @Parameter(name = "amount", description = "Withdrawal amount per installment (Web)", required = false),
                    @Parameter(name = "start_date", description = "SWP start date (yyyy-MM-dd) (Web)", required = false),
                    @Parameter(name = "end_date", description = "SWP end date (yyyy-MM-dd) (Web)", required = false),
                    @Parameter(name = "frequency", description = "SWP frequency (e.g., Monthly, Quarterly) (Web)", required = false),
                    @Parameter(name = "from_scheme_code", description = "Scheme code to withdraw from (Web)", required = false),
                    @Parameter(name = "from_scheme_name", description = "Scheme name to withdraw from (Web)", required = false),
                    @Parameter(name = "redem_type", description = "SWP redemption type (Amount/Units) (Web)", required = false),
                    @Parameter(name = "split_start_date", description = "Split start date for SWP if applicable (Web)", required = false),
                    @Parameter(name = "swp_day", description = "Day of the month for SWP (e.g., 10, 15) (Web)", required = false),
                    @Parameter(name = "broker_code", description = "Broker code (Web)", required = false),
                    @Parameter(name = "euin_code", description = "EUIN code (Web)", required = false),

                    // ✅ Mobile App Parameters
                    @Parameter(name = "iin_number", description = "Investor Identification Number (Mobile)", required = false),
                    @Parameter(name = "broker_code", description = "Broker code (Mobile)", required = false),
                    @Parameter(name = "euin_code", description = "EUIN code (Mobile)", required = false)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "SWP transaction successfully processed",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TransactionResponse.class),
                                    examples = @ExampleObject(
                                            name = "SuccessResponseExample",
                                            summary = "SWP Success",
                                            value = """
                        {
                          "status": 200,
                          "status_msg": "SUCCESS",
                          "msg": "SWP transaction successfully processed",
                          "return_msg": "OrderID: SWP20250724",
                          "transaction_status": {
                            "HDFC Corporate Bond Fund - Growth": "SWP successfully triggered"
                          }
                        }
                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request or missing data",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "BadRequestExample",
                                            summary = "Invalid STP request",
                                            value = "{ \"status\": 400, \"error\": \"Bad Request\", \"message\": \"Invalid or missing parameters\" }"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal Server Error",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "ServerErrorExample",
                                            summary = "Unexpected server error",
                                            value = "{ \"status\": 500, \"error\": \"Internal Server Error\", \"message\": \"Something went wrong on the server\" }"
                                    )
                            )
                    )
            }
    )
    @PostMapping("/saveSwp")
    public ResponseEntity<?> saveSwp(
            HttpServletRequest request,
            @RequestParam(required = false) String amc,
            @RequestParam(required = false) String scheme,
            @RequestParam(required = false) String reinvest_tag,
            @RequestParam(required = false) String redem_type,
            @RequestParam(required = false) String end_date,
            @RequestParam(required = false) String split_start_date,
            @RequestParam(required = false) String iin_number,
            @RequestParam(required = false) String broker_code,
            @RequestParam(required = false) String euin_code,
            @RequestParam(required = false) String withdrawals,
            @RequestParam(required = false) String first_order_today,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String cartid,
            @RequestParam(required = false) String subbroker_arn,
            @RequestParam(required = false) String subbroker_code,
            @RequestParam(required = false) String subbroker_name,
            @RequestParam(required = false) String ip_address,
            @RequestParam(required = false) String origin_user_id,
            @RequestParam(required = false) String origin_first_name,
            @RequestHeader("Authorization") String token)
    {

        String ipAddr = "";
        List<CartDto> cartList = null;
        long currentTimeMillis = System.currentTimeMillis();
        try
        {
            String userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            System.out.println("User ID from token: " + userid);

            amc = NseUtils.checkParem(amc);
            scheme = NseUtils.checkParem(scheme);
            reinvest_tag = NseUtils.checkParem(reinvest_tag);
            redem_type = NseUtils.checkParem(redem_type);
            end_date = NseUtils.checkParem(end_date);
            split_start_date = NseUtils.checkParem(split_start_date);
            iin_number = NseUtils.checkParem(iin_number);
            broker_code = NseUtils.checkParem(broker_code);
            euin_code = NseUtils.checkParem(euin_code);
            source = NseUtils.checkParem(source);
            first_order_today = NseUtils.checkParem(first_order_today);
            cartid = NseUtils.checkParem(cartid);
            subbroker_code = NseUtils.checkParem(subbroker_code);
            subbroker_name = NseUtils.checkParem(subbroker_name);
            subbroker_arn = NseUtils.checkParem(subbroker_arn);
            ip_address = NseUtils.checkParem(ip_address);
            origin_user_id = NseUtils.checkParem(origin_user_id);
            origin_first_name = NseUtils.checkParem(origin_first_name);

            if(first_order_today.isEmpty())
            {
                first_order_today = "N";
            }


            UserDto user = userServiceClient.getUserById(Integer.valueOf(userid), token);

            if (user == null)
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "User not found"));
            }

            List<String> scheme_name_array;
            List<String> scheme_code_array;
            List<String> amount_array;
            List<String> folio_array;
            List<String> cart_id_array;
            List<String> start_date_array;
            List<String> frequency_array;
            List<String> install_count_array;
            List<Boolean> first_order_flag_array;

            if(source.equalsIgnoreCase("Mobile"))
            {
                scheme_name_array = new ArrayList<String>();
                scheme_code_array = new ArrayList<String>();
                amount_array = new ArrayList<String>();
                folio_array = new ArrayList<String>();
                cart_id_array = new ArrayList<String>();
                start_date_array = new ArrayList<>();
                frequency_array = new ArrayList<>();
                install_count_array = new ArrayList<>();
                first_order_flag_array = new ArrayList<>();

                cartList = userServiceClient.getCartDetailsByUserID(Integer.parseInt(userid), "NSE", iin_number, "SWP Purchase",token);

                if (cartList.isEmpty())
                {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "No Cart found for the user."));
                }

                for (CartDto cart : cartList) {
                    scheme_name_array.add(cart.getScheme_name());
                    scheme_code_array.add(cart.getScheme_product_code());
                    amount_array.add(cart.getAmount());
                    folio_array.add(cart.getFolio_no());
                    start_date_array.add(cart.getStart_date());
                    frequency_array.add(cart.getFrequency());
                    install_count_array.add(cart.getInstallment());
                    first_order_flag_array.add(cart.getFirst_order_flag());
                    cart_id_array.add(String.valueOf(cart.getId()));
                }
            }else
            {
                if(!cartid.isEmpty())
                {
                    scheme_name_array = new ArrayList<String>();
                    scheme_code_array = new ArrayList<String>();
                    amount_array = new ArrayList<String>();
                    folio_array = new ArrayList<String>();
                    cart_id_array = new ArrayList<String>();
                    start_date_array = new ArrayList<>();
                    frequency_array = new ArrayList<>();
                    install_count_array = new ArrayList<>();
                    first_order_flag_array = new ArrayList<>();

                    List<Integer> ids = Arrays.stream(cartid.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());
                    System.out.println("ids = " + ids);

                    cartList = userServiceClient.getCartDetailsByIds(ids,token);

                    if (cartList.isEmpty())
                    {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "No Cart found for the user."));
                    }
                    for (CartDto cart : cartList)
                    {
                        scheme_name_array.add(cart.getScheme_name());
                        scheme_code_array.add(cart.getScheme_product_code());
                        amount_array.add(cart.getAmount());
                        folio_array.add(cart.getFolio_no());
                        start_date_array.add(cart.getStart_date());
                        frequency_array.add(cart.getFrequency());
                        install_count_array.add(cart.getInstallment());
                        first_order_flag_array.add(cart.getFirst_order_flag());
                        cart_id_array.add(String.valueOf(cart.getId()));
                    }
                }else{
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "Cart ID missing."));
                }
            }

            String nse_iin = user.getNse_iin_number().trim();
            String pan = "";
            String name = "";
            String selected_name = "";
            String mobile = "";
            String email = "";
            String client_name = user.getClient_name();
            String login_name = user.getName();

            if(!nse_iin.equalsIgnoreCase(iin_number))
            {
                UserBseNseDto nse = null;

                try {
                    nse = userServiceClient.getUserBseNseDetailsByIinNumber(client_name, iin_number,token);
                } catch (FeignException e) {
                    if (e.status() == 400) {
                        System.out.println("Bad Request: " + e.getMessage());
                    } else if (e.status() == 404) {
                        System.out.println("User not found: " + e.getMessage());
                    } else {
                        System.out.println("Feign error: " + e.status() + " - " + e.getMessage());
                    }
                }

                if(nse == null)
                {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "No User Found"));
                }

                pan = nse.getPan();
                name = nse.getName();
                selected_name = name + " (" + userid + ")";

                mobile = nse.getMobile();
                email = nse.getEmail();

            }else
            {
                pan = user.getPan();
                name = user.getName();
                selected_name = name + " (" + userid + ")";

                mobile = user.getMobile();
                email = user.getEmail();
            }

            String appln_id = "";
            String password = "";
            String euin = "";
            String host = "";

            BseNseKeyDto nsekey = userServiceClient.getByClientName(client_name,token);
            host = nsekey.getDomain_url();
            String broker_code1 = nsekey.getBrokerCode();

            if(broker_code1 == null){broker_code1 = "";};

            broker_code = broker_code1;
            appln_id = nsekey.getNse_appln_id();
            password = nsekey.getNse_password();
            if(!euin_code.isEmpty())
            {
                euin = euin_code;
            }else{
                euin = nsekey.getEuin();
            }

            euin = euin.split(",")[0];

            if(broker_code.isEmpty())
            {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), broker_code + " does not have the NSE credentials. Kindly update."));
            }

            String first_order_flag = "";
            Map<String, String> resMap = new HashMap<>();

            int successCount = 0;
            int failureCount = 0;
            List<String> failedSchemes = new ArrayList<>();
            String lastSuccessRegId = "";

            String reg_id = "";
            String reg_status = "";
            String reg_remark = "";

            Map<String, String> schemeMap = new HashMap<>();

            String schemeName = "";

            if(!scheme_code_array.isEmpty())
            {
                for (int i = 0; i < scheme_code_array.size(); i++)
                {
                    String scheme_code = scheme_code_array.get(i);
                    String scheme_name = scheme_name_array.get(i);
                    String start_date = start_date_array.get(i);
                    String install_count = install_count_array.get(i);
                    String frequency = frequency_array.get(i);
                    String amount = amount_array.get(i);
                    String folio = folio_array.get(i);
                    Boolean firstOrderFlag = first_order_flag_array.get(i);

                    schemeMap.put(scheme_code, scheme_name);

                    if(firstOrderFlag)
                    {
                        first_order_flag = "Y";
                    }else{
                        first_order_flag = "N";
                    }

                    String  folio_no [] = null;
                    String folio_no1 = "";

                    if(folio.contains("/"))
                    {
                        folio_no = folio.split("/");
                        folio_no1 = folio_no[0];
                        folio_no1 = folio_no1.trim();
                    }else
                    {
                        folio_no1 = folio.trim();
                    }

                    JSONArray regDetailsArray = new JSONArray();
                    JSONObject regObject = new JSONObject();

                    String start_date_str = start_date;

                    if(start_date_str != null)
                    {
                        start_date_str = start_date_str.replace("-", "/");
                    }

                    String ref_number = "ADVSWP" + UniqueIDProvider.generateUniquePin(4);

                    String memberUniqueId = "SWP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + UniqueIDProvider.generateUniquePin(3);

                    regObject.put("client_code", iin_number);
                    regObject.put("scheme_code", scheme_code);
                    regObject.put("trans_mode", "P");
                    regObject.put("folio_no", folio_no1);
                    regObject.put("internal_ref_no", ref_number);
                    regObject.put("start_date", start_date_str);
                    regObject.put("no_of_withdrawals", install_count);
                    regObject.put("frequency_type", frequency);
                    regObject.put("installment_amount", amount);
                    regObject.put("installment_units", "");

                    if(first_order_flag.equalsIgnoreCase("Y"))
                    {
                        regObject.put("first_order_today", "Y");
                    }else{
                        regObject.put("first_order_today", "N");
                    }


                    if(!subbroker_code.isEmpty())
                    {
                        regObject.put("sub_broker_code", subbroker_code);
                    }else{
                        regObject.put("sub_broker_code", "");
                    }
                    regObject.put("euin_declaration", "Y");
                    regObject.put("euin_number", euin);
                    regObject.put("remarks", "");
                    regObject.put("sub_broker_arn", subbroker_arn);
                    regObject.put("mobile", mobile);
                    regObject.put("email", email);
                    regObject.put("account_no", "");
                    regObject.put("member_unique_id", memberUniqueId);

                    regDetailsArray.put(regObject);

                    JSONObject requestBody = new JSONObject();
                    requestBody.put("reg_data", regDetailsArray);

                    BseNseOnlineAccessDto online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, broker_code,token);

                    if (online_access == null)
                    {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new CommonResponse(StatusMessage.NseFailureCode, StatusMessage.NseFailureMessage, "NSE Online Credentials Not available. Please contact your RM"));
                    }

                    String nse_userid = NseUtils.trimOrEmpty(online_access.getNse_userid());
                    String nse_memberid = NseUtils.trimOrEmpty(online_access.getNse_memberid());
                    String nse_secret_key = NseUtils.trimOrEmpty(online_access.getNse_secret_key());
                    String nse_license_key = NseUtils.trimOrEmpty(online_access.getNse_license_key());
//

                    String base64Encoded = AESEncryptionUtilV2.base64EncodedAuth(nse_secret_key, nse_license_key, nse_userid);

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.set("memberId", nse_memberid);
                    headers.set("Authorization", "Basic "+base64Encoded);
                    headers.set("User-Agent", "PostmanRuntime/7.43.3");
                    headers.set("Accept-Language", "en-US");
                    headers.set("Connection", "keep-alive");
                    headers.set("Referer", "");
                    headers.set("Accept-Encoding", "gzip");

                    HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

                    String swpRegistrationService_url= NseApiUrls.swpRegistrationService_url;

                    System.out.println("requestBody: " + requestBody.toString());

                    String responseBody = "";
                    try
                    {
                        ResponseEntity<String> result = RestTemplateFactory.createRestTemplate().postForEntity(swpRegistrationService_url, entity, String.class);
                        ////String status_code = result.getStatusCode().toString();
                        responseBody = result.getBody();

                        JSONObject jsonObject = new JSONObject(responseBody);
                        System.out.println("jsonObject: " + jsonObject);
                        JSONArray jsonRegArray = jsonObject.getJSONArray("reg_data");

                        System.out.println("swpRegistrationService::responseBody: " + responseBody);
                        for (int j = 0; j < jsonRegArray.length(); j++)
                        {
                            JSONObject regDetail = jsonRegArray.getJSONObject(j);

                            reg_id = regDetail.optString("reg_id");
                            reg_status = regDetail.optString("reg_status");
                            reg_remark = regDetail.optString("reg_remark");

                            System.out.println("reg_id: " + reg_id);
                            System.out.println("reg_status" + reg_status);
                            System.out.println("reg_remark: " + reg_remark);

                            // Extracting SWP Registration details
                            String res_client_code        = NseUtils.checkParem(regDetail.optString("client_code"));
                            String res_scheme_code        = NseUtils.checkParem(regDetail.optString("scheme_code"));
                            String res_trans_mode         = NseUtils.checkParem(regDetail.optString("trans_mode"));
                            String res_folio_no           = NseUtils.checkParem(regDetail.optString("folio_no"));
                            String res_internal_ref_no    = NseUtils.checkParem(regDetail.optString("internal_ref_no"));
                            String res_start_date         = NseUtils.checkParem(regDetail.optString("start_date"));
                            String res_no_of_withdrawals  = NseUtils.checkParem(regDetail.optString("no_of_withdrawals"));
                            String res_frequency_type     = NseUtils.checkParem(regDetail.optString("frequency_type"));
                            String res_installmenamount   = NseUtils.checkParem(regDetail.optString("installment_amount"));
                            String res_installment_units  = NseUtils.checkParem(regDetail.optString("installment_units"));
                            String res_first_order_today  = NseUtils.checkParem(regDetail.optString("first_order_today"));
                            String res_sub_broker_code    = NseUtils.checkParem(regDetail.optString("sub_broker_code"));
                            String res_euin_declaration   = NseUtils.checkParem(regDetail.optString("euin_declaration"));
                            String res_euin_number        = NseUtils.checkParem(regDetail.optString("euin_number"));
                            String res_remarks            = NseUtils.checkParem(regDetail.optString("remarks"));
                            String res_sub_broker_arn     = NseUtils.checkParem(regDetail.optString("sub_broker_arn"));
                            String res_mobile             = NseUtils.checkParem(regDetail.optString("mobile"));
                            String res_email              = NseUtils.checkParem(regDetail.optString("email"));
                            String res_account_no         = NseUtils.checkParem(regDetail.optString("account_no"));
                            String res_reg_id             = NseUtils.checkParem(regDetail.optString("reg_id"));
                            String res_reg_status         = NseUtils.checkParem(regDetail.optString("reg_status"));
                            String res_reg_remark         = NseUtils.checkParem(regDetail.optString("reg_remark"));
                            String res_member_unique_id   = NseUtils.checkParem(regDetail.optString("member_unique_id"));

                            // Printing all details
                            System.out.println("SWP Registration Details:");
                            System.out.println("res_client_code       : " + res_client_code);
                            System.out.println("res_scheme_code       : " + res_scheme_code);
                            System.out.println("res_trans_mode        : " + res_trans_mode);
                            System.out.println("res_folio_no          : " + res_folio_no);
                            System.out.println("res_internal_ref_no   : " + res_internal_ref_no);
                            System.out.println("res_start_date        : " + res_start_date);
                            System.out.println("res_no_of_withdrawals : " + res_no_of_withdrawals);
                            System.out.println("res_frequency_type    : " + res_frequency_type);
                            System.out.println("res_installmenamount  : " + res_installmenamount);
                            System.out.println("res_installment_units : " + res_installment_units);
                            System.out.println("res_first_order_today : " + res_first_order_today);
                            System.out.println("res_sub_broker_code   : " + res_sub_broker_code);
                            System.out.println("res_euin_declaration  : " + res_euin_declaration);
                            System.out.println("res_euin_number       : " + res_euin_number);
                            System.out.println("res_remarks           : " + res_remarks);
                            System.out.println("res_sub_broker_arn    : " + res_sub_broker_arn);
                            System.out.println("res_mobile            : " + res_mobile);
                            System.out.println("res_email             : " + res_email);
                            System.out.println("res_account_no        : " + res_account_no);
                            System.out.println("res_reg_id            : " + res_reg_id);
                            System.out.println("res_reg_status        : " + res_reg_status);
                            System.out.println("res_reg_remark        : " + res_reg_remark);
                            System.out.println("res_member_unique_id  : " + res_member_unique_id);

                            schemeName =  schemeMap.get(res_scheme_code);

                            SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy");
                            Date res_start_dateDt = inputFormat.parse(res_start_date);

                            NseTransactions nsetrans = new NseTransactions();
                            nsetrans.setUrl(swpRegistrationService_url);
                            nsetrans.setNse_request(requestBody.toString());
                            nsetrans.setNse_response(responseBody);
                            nsetrans.setReg_id(res_reg_id);
                            nsetrans.setPayment_link("");
                            nsetrans.setPan(pan);
                            nsetrans.setName(name);
                            nsetrans.setBranch(user.getBranch());
                            nsetrans.setRm_name(user.getRm_name());
                            if(source.equalsIgnoreCase("Website"))
                            {
                                nsetrans.setSubbroker_name(subbroker_name);
                            }else{
                                nsetrans.setSubbroker_name(user.getSubbroker_name());
                            }
                            if(!subbroker_code.isEmpty())
                            {
                                nsetrans.setSubbroker_code(subbroker_code);
                            }
                            if(!subbroker_arn.isEmpty())
                            {
                                nsetrans.setSubbroker_arn(subbroker_arn);
                            }
                            nsetrans.setClient_name(client_name);
                            nsetrans.setIin_number(iin_number);
                            nsetrans.setScheme_name(schemeName);
                            nsetrans.setScheme_code(res_scheme_code);
                            nsetrans.setFolio_no(res_folio_no);
                            if(StringHelper.isNotEmpty(res_installmenamount))
                            {
                                nsetrans.setAmount_units(res_installmenamount);
                            }else if(StringHelper.isNotEmpty(res_installment_units))
                            {
                                nsetrans.setAmount_units(res_installment_units);
                            }else{
                                nsetrans.setAmount_units("0");
                            }
                            if(first_order_flag.equalsIgnoreCase("Y") || first_order_flag.equalsIgnoreCase("true"))
                            {
                                nsetrans.setFirst_order_today(1);
                            }else
                            {
                                nsetrans.setFirst_order_today(0);
                            }
                            nsetrans.setFrequency(res_frequency_type);
                            nsetrans.setPeriod_day("");
                            nsetrans.setUmrn_no("");
                            nsetrans.setPurchase_type("FRESH");
                            nsetrans.setPayment_ref_no("");
                            nsetrans.setUnique_number(res_member_unique_id);
                            nsetrans.setStart_date(res_start_dateDt);
                            nsetrans.setAuto_trxn_no("");
                            nsetrans.setSip_reg_no(res_reg_id);
                            nsetrans.setPayment_mode("");
                            nsetrans.setTopup_amount(0.0);
                            nsetrans.setBank_acc_no("");
                            nsetrans.setTransaction_number(res_reg_id);
                            nsetrans.setApplication_number("");
                            nsetrans.setTo_scheme_code("");
                            nsetrans.setTo_scheme_name("");
                            nsetrans.setTransaction_type("SWP Transaction");
                            nsetrans.setPayment_status("PENDING");
                            nsetrans.setActive_ceased_status("");
                            nsetrans.setRemarks(res_reg_remark);
                            nsetrans.setMandate_id("");
                            nsetrans.setMandate_status("");
                            nsetrans.setEmandate_auth_flag("");
                            nsetrans.setApp_received_flag("");
                            nsetrans.setTransaction_date(new Date());
                            nsetrans.setTransaction_status(reg_status);
                            nsetrans.setReturn_msg(res_reg_status);
                            nsetrans.setUser_id(Integer.parseInt(userid));
                            if(source.equalsIgnoreCase("Mobile"))
                            {
                                nsetrans.setRegister_source("Mobile App");
                            }else
                            {
                                nsetrans.setRegister_source("Website");
                            }
                            nsetrans.setBroker_code(broker_code);
                            nsetrans.setEuin_number(res_euin_number);
                            nsetrans.setCc_received("");
                            nsetrans.setFund_trans_to_amc("");
                            nsetrans.setRefund_status("");
                            nsetrans.setRefund_amount("");
//                            nsetrans.setIp_address(ip_address);
//                            nsetrans.setOrigin_user_id(origin_user_id);
//                            nsetrans.setOrigin_first_name(origin_first_name);
                            if(cartid.isEmpty())
                            {
                                cartid = "0";
                            }
                            nsetrans.setCart_id(cartIdAt(cart_id_array, i));
                            nseTransactionService.save(nsetrans);

                            for (CartDto cart : cartList)
                            {
                                cart.setPayment_type("");
                                cart.setPayment_mode("");
                                cart.setBank_name("");
                                cart.setBank_account_number("");
                                cart.setBank_ifsc("");
                                cart.setBroker_code(broker_code);
                                cart.setEuin_code(euin);

                                if(reg_status.equalsIgnoreCase("REG_SUCCESS"))
                                {
                                    cart.setStatus("SUCCESS");
                                    cart.setActive(false);
                                    cart.setPayment_id(String.valueOf(currentTimeMillis));
                                }
                            }

                            userServiceClient.updateCartByCartId(cartList, token);

                            if(res_reg_status.equalsIgnoreCase("REG_SUCCESS"))
                            {
                                successCount++;
                                lastSuccessRegId = res_reg_id;
                                resMap.put(schemeName, "REG_SUCCESS");
                            }
                            else
                            {
                                failureCount++;
                                failedSchemes.add(schemeName + ": " + res_reg_remark);
                                resMap.put(schemeName, res_reg_remark);
                            }
                        }
                    }
                    catch (HttpClientErrorException | HttpServerErrorException ex)
                    {
                        failureCount++;
                        failedSchemes.add(schemeName + ": " + ex.getStatusCode());
                        resMap.put(schemeName, "SWP registration failed. Please try again.");
                        System.out.println("swpRegistrationService::Status Code: " + ex.getStatusCode());
                        System.out.println("swpRegistrationService::Response Body: " + ex.getResponseBodyAsString());
                    } catch (Exception ex)
                    {
                        failureCount++;
                        failedSchemes.add(schemeName + ": " + ex.getMessage());
                        resMap.put(schemeName, "SWP registration failed. Please try again.");
                        ex.printStackTrace();
                    }
                }
            }
            System.out.println("successCount: " + successCount);
            System.out.println("failureCount: " + failureCount);
            if (successCount > 0 && failureCount > 0)
            {
                String message = String.format("%d out of %d SWP transactions succeeded. Please go to MyOrders Page check the details.",successCount, (successCount + failureCount));
                message += "Failed transactions: " + String.join(", ", failedSchemes);

                if(source.equalsIgnoreCase("Mobile"))
                {
                    return NseUtils.transactionMobileResponse(HttpStatus.BAD_REQUEST, message, resMap);
                }else{
                    return NseUtils.transactionResponse(HttpStatus.BAD_REQUEST, message, resMap);
                }
            }
            else if (successCount > 0)
            {
                if(source.equalsIgnoreCase("Mobile"))
                {
                    return NseUtils.transactionMobileResponse(HttpStatus.OK, "All SWP Orders successfully triggered! Last orderID: " + lastSuccessRegId,resMap);
                }else{
                    return NseUtils.transactionResponse(HttpStatus.OK, "All SWP Orders successfully triggered! Last orderID: " + lastSuccessRegId,resMap);
                }
            }
            else
            {
                String failureMessage = !failedSchemes.isEmpty() ? String.join(", ", failedSchemes)
                        : (StringHelper.isNotEmpty(reg_remark) ? reg_remark
                        : "SWP registration failed. Please try again.");
                return NseUtils.commonResponse(failureMessage, HttpStatus.BAD_REQUEST);
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "Transaction failed. Please try again!"));
        }
    }


    @Operation(
            summary = "Cancel SIP/STP/SWP",
            description = "Cancels an existing SIP, STP, or SWP transaction based on the provided option. Works for both Web and Mobile.",
            parameters = {
                    @Parameter(name = "option", description = "Transaction type to cancel (SIP/STP/SWP)", required = false),
                    @Parameter(name = "auto_trxn_no", description = "Auto transaction number", required = false),
                    @Parameter(name = "iin_number", description = "Investor Identification Number", required = false),
                    @Parameter(name = "scheme_name", description = "Scheme name", required = false),
                    @Parameter(name = "amount_units", description = "Amount or units set in the original transaction", required = false),
                    @Parameter(name = "folio_no", description = "Folio number", required = false),
                    @Parameter(name = "scheme_code", description = "Scheme code", required = false),
                    @Parameter(name = "unique_number", description = "Unique identification number", required = false),
                    @Parameter(name = "trxn_no", description = "Transaction number", required = false),
                    @Parameter(name = "sip_reg_no", description = "SIP/STP/SWP registration number", required = false),
                    @Parameter(name = "cancel_reason", description = "Cancel reason code", required = false),
                    @Parameter(name = "other_reason", description = "Other reason code (if applicable)", required = false),
                    @Parameter(name = "cancel_reason_text", description = "Text description of the cancel reason", required = false),
                    @Parameter(name = "sip_cancel_date", description = "Effective cancel date (yyyy-MM-dd)", required = false),
                    @Parameter(name = "source", description = "Transaction source (web/mobile)", required = false),
                    @Parameter(
                            name = "Authorization",
                            description = "Bearer token for authentication",
                            required = true,
                            in = ParameterIn.HEADER
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Cancellation request processed successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TransactionResponse.class),
                                    examples = @ExampleObject(
                                            name = "SuccessCancelExample",
                                            summary = "Cancellation Success",
                                            value = """
                        {
                          "status": 200,
                          "status_msg": "SUCCESS",
                          "msg": "SIP/STP/SWP cancellation processed",
                          "return_msg": "Auto transaction cancelled successfully",
                          "transaction_status": {
                            "HDFC Balanced Advantage Fund - Growth": "Cancelled successfully"
                          }
                        }
                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request or missing data",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "BadRequestExample",
                                            summary = "Invalid STP request",
                                            value = "{ \"status\": 400, \"error\": \"Bad Request\", \"message\": \"Invalid or missing parameters\" }"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal Server Error",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "ServerErrorExample",
                                            summary = "Unexpected server error",
                                            value = "{ \"status\": 500, \"error\": \"Internal Server Error\", \"message\": \"Something went wrong on the server\" }"
                                    )
                            )
                    )
            }
    )
    @PostMapping("/cancelSipStpSwp")
    public ResponseEntity<?> cancelSipStpSwp(
            HttpServletRequest request,
            @RequestParam(required = false) String option,
            @RequestParam(required = false) String auto_trxn_no,
            @RequestParam(required = false) String iin_number,
            @RequestParam(required = false) String scheme_name,
            @RequestParam(required = false) String amount_units,
            @RequestParam(required = false) String folio_no,
            @RequestParam(required = false) String scheme_code,
            @RequestParam(required = false) String unique_number,
            @RequestParam(required = false) String trxn_no,
            @RequestParam(required = false) String sip_reg_no,
            @RequestParam(required = false) String cancel_reason,
            @RequestParam(required = false) String other_reason,
            @RequestParam(required = false) String cancel_reason_text,
            @RequestParam(required = false) String sip_cancel_date,
            @RequestParam(required = false) String source,
            @RequestHeader("Authorization") String token)
    {

        String ipAddr = "";
        List<CartDto> cartList = null;
        long currentTimeMillis = System.currentTimeMillis();
        String name = "";
        String pan = "";
        String mobile = "";
        String client_name = "";
        try
        {
            String userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            System.out.println("User ID from token: " + userid);

            option = NseUtils.checkParem(option);
            auto_trxn_no = NseUtils.checkParem(auto_trxn_no);
            iin_number = NseUtils.checkParem(iin_number);
            scheme_name = NseUtils.checkParem(scheme_name);
            amount_units = NseUtils.checkParem(amount_units);
            folio_no = NseUtils.checkParem(folio_no);
            scheme_code = NseUtils.checkParem(scheme_code);
            unique_number = NseUtils.checkParem(unique_number);
            trxn_no = NseUtils.checkParem(trxn_no);
            sip_reg_no = NseUtils.checkParem(sip_reg_no);
            cancel_reason = NseUtils.checkParem(cancel_reason);
            other_reason = NseUtils.checkParem(other_reason);
            cancel_reason_text = NseUtils.checkParem(cancel_reason_text);
            sip_cancel_date = NseUtils.checkParem(sip_cancel_date);
            source = NseUtils.checkParem(source);

            if (auto_trxn_no.isEmpty() || unique_number.isEmpty() || iin_number.isEmpty())
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "Transaction number or IIN Number is empty."));
            }

            UserDto user = userServiceClient.getUserById(Integer.valueOf(userid), token);

            if (user == null)
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "User not found"));
            }

            name = user.getName();
            pan = user.getPan();
            mobile = user.getMobile();
            client_name = user.getClient_name();

            String login_userid = userid;
            String login_name = name;
            String login_mobile = mobile;
            String selected_name = "";

            SimpleDateFormat df1 = new SimpleDateFormat("dd-MMM-yyyy");
            String cease_req_date = df1.format(new Date());

            NseTransactions transaction = null;
            Optional<NseTransactions> transactionOpt = nseTransactionRepository.getNseTransactionDetails(unique_number, client_name);

            if(transactionOpt.isPresent())
            {
                transaction = transactionOpt.get();
            }else
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "SIP Registration Details not available."));
            }

            Date tranDate = transaction.getTransaction_date();
            String broker_code = transaction.getBroker_code();
            String euin_code = transaction.getEuin_number();
            if (broker_code == null) {
                broker_code = "";
            }
            if (euin_code == null) {
                euin_code = "";
            }

            BseNseKeyDto nsekey = userServiceClient.getByClientName(client_name,token);
            String broker_code1 = nsekey.getBrokerCode();

            String host = nsekey.getDomain_url();
            String appln_id = "";
            String password = "";
            String euin = "";

            if(broker_code1 == null){broker_code1 = "";};



                broker_code = broker_code1;
                appln_id = nsekey.getNse_appln_id();
                password = nsekey.getNse_password();
                if (!euin_code.isEmpty()) {
                    euin = euin_code;
                } else {
                    euin = nsekey.getEuin();
                }

            euin = euin.split(",")[0];

            String scheme_amfi = transaction.getScheme_name();
            UsersPortfolioSchemewiseDto usersPortfolioSchemewiseDto = userServiceClient.getRTADetails(scheme_amfi,token);
            String registrar = usersPortfolioSchemewiseDto.getRegistrar();

            if (!registrar.isEmpty() && registrar.equalsIgnoreCase("karvy"))
            {
                cancel_reason = cancel_reason.replace("SC", "");
            }

            JSONArray requestDetailsArray = new JSONArray();
            JSONObject requestDetails = new JSONObject();
            requestDetails.put("client_code", iin_number);
            requestDetails.put("xsip_reg_no", sip_reg_no);

            if(option.equalsIgnoreCase("DAILY SIP")) {
                requestDetails.put("xsip_cancel_date", sip_cancel_date);
            }

            String remark_str = "";
            if (cancel_reason.equals("13")) {
                remark_str = "13:(" + other_reason + ")";
                requestDetails.put("remarks", remark_str);
            } else {
                requestDetails.put("remarks", cancel_reason);
            }
            requestDetailsArray.put(requestDetails);

            BseNseOnlineAccessDto online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, broker_code,token);

            if(online_access == null)
            {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "NSE Online Credentials Not available. Please contact your RM"));
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
            requestBody.put("can_data", requestDetailsArray);
            System.out.println("requestBody = " + requestBody.toString());

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            String cancelation_api_url = "";
            if (option.equalsIgnoreCase("SIP")) {

                cancelation_api_url = NseApiUrls.cancelSip;

            } else if (option.equalsIgnoreCase("STP")) {

                cancelation_api_url = NseApiUrls.cancelStp;

            } else if (option.equalsIgnoreCase("SWP")) {

                cancelation_api_url = NseApiUrls.cancelStp;

            }else if(option.equalsIgnoreCase("DAILY SIP")) {

                cancelation_api_url = NseApiUrls.cacleDailySip;
            }
            String can_status = "";
            String can_remark = "";

            try {
                ResponseEntity<String> mandateResult = RestTemplateFactory.createRestTemplate()
                        .postForEntity(cancelation_api_url, entity, String.class);
                String statusCode = mandateResult.getStatusCode().toString();
                String responseBody = mandateResult.getBody().toString();

                System.out.println("statusCode = " + statusCode);
                System.out.println("responseBody = " + responseBody);

                JSONObject jsonObject = new JSONObject(responseBody);

                if (jsonObject.has("reg_data")) {
                    JSONArray regDataArray = jsonObject.getJSONArray("reg_data");

                    if (regDataArray.length() > 0) {

                        JSONObject regData = regDataArray.getJSONObject(0);

                        can_status = regData.optString("can_status");
                        can_remark = regData.optString("can_remark");
                    }
                }
                NseTransactions nsetrans = new NseTransactions();
                nsetrans.setUrl(cancelation_api_url);
                nsetrans.setNse_request(requestBody.toString());
                nsetrans.setNse_response(responseBody.toString());
                nsetrans.setReturn_msg(can_remark);
                nsetrans.setService_return_code(statusCode);
                nsetrans.setService_msg(can_remark);
                nsetrans.setReg_id(sip_reg_no);
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
                nsetrans.setTransaction_type(option + " Cancellation");
                nsetrans.setTransaction_status("");
                nsetrans.setPayment_status("");
                nsetrans.setActive_ceased_status("");
                nsetrans.setRemarks(can_remark);
                nsetrans.setMandate_id("");
                nsetrans.setMandate_status("");
                nsetrans.setEmandate_auth_flag("");
                nsetrans.setApp_received_flag("");
                nsetrans.setTransaction_date(new Date());
                nsetrans.setUser_id(Integer.parseInt(userid));
                if(source.equalsIgnoreCase("Mobile"))
                {
                    nsetrans.setRegister_source("Mobile");
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

                ipAddr = NseUtils.getIpAddr(request);
                if (ipAddr == null) {ipAddr = "";}

                String investor = selected_name != null && StringHelper.isNotEmpty(selected_name) ? " for " + selected_name : "";
                String logmsg = login_name + " did " + option + " cancelation " + investor + ". Details:";
                logmsg += "trxn_no: " + trxn_no + ",";
                logmsg += "scheme_name: " + scheme_name + ",";
                logmsg += "folio_no: " + folio_no;


                NseLogModel log = new NseLogModel();
                log.setUserid(Integer.parseInt(userid));
                log.setUsername(login_name);
                log.setUsername(login_name);
                log.setMobile("");
                log.setTitle(option + " Cancelation");
                log.setDescription(option + " Cancelation");
                log.setContent(logmsg);
                log.setLogtime(new Date());
                log.setIp(ipAddr);
                if(source.equalsIgnoreCase("Mobile"))
                {
                    log.setSource("Mobile");
                }else{
                    log.setSource("WEB");
                }
                log.setClientName(client_name);
                nseLogRepository.save(log);

                if (can_status.equalsIgnoreCase("REG_SUCCESS"))
                {
                    return NseUtils.transactionResponse(HttpStatus.OK, option + " Cancellation done Successfully " + sip_reg_no, new HashMap<>());
                } else
                {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "can_remark"));
                }
            } catch (Exception ex)
            {
                ex.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "Transaction failed. Please try again!"));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "Transaction failed. Please try again!"));
        }
    }


    @Operation(
            summary = "Pause SIP Registration",
            description = "Pauses an active SIP registration for the investor through NSE. Works for both Web and Mobile based on the 'source' parameter.",
            parameters = {
                    @Parameter(name = "iin_number", description = "Investor Identification Number", required = false),
                    @Parameter(name = "unique_number", description = "Unique transaction reference number", required = false),
                    @Parameter(name = "effective_date", description = "Pause effective date (Format: DD/MM/YYYY)", required = false),
                    @Parameter(name = "source", description = "Transaction source (web/mobile)", required = false),
                    @Parameter(
                            name = "Authorization",
                            description = "Bearer token for authentication",
                            required = true,
                            in = ParameterIn.HEADER
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "SIP pause request successfully processed",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TransactionResponse.class),
                                    examples = @ExampleObject(
                                            name = "PauseSuccessExample",
                                            summary = "Pause Success",
                                            value = """
                    {
                      "status": 200,
                      "status_msg": "SUCCESS",
                      "msg": "SIP pause successfully processed",
                      "return_msg": "Pause order submitted successfully",
                      "transaction_status": {
                        "HDFC Flexi Cap Fund - Growth": "Pause successful"
                      }
                    }
                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request or missing data",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "BadRequestExample",
                                            summary = "Invalid STP request",
                                            value = "{ \"status\": 400, \"error\": \"Bad Request\", \"message\": \"Invalid or missing parameters\" }"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal Server Error",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "ServerErrorExample",
                                            summary = "Unexpected server error",
                                            value = "{ \"status\": 500, \"error\": \"Internal Server Error\", \"message\": \"Something went wrong on the server\" }"
                                    )
                            )
                    )
            }
    )
    @PostMapping("/pauseSip")
    public ResponseEntity<?> pauseSip(
            HttpServletRequest request,
            @RequestParam(required = false) String iin_number,
            @RequestParam(required = false) String unique_number,
            @RequestParam(required = false) String effective_date,
            @RequestParam(required = false) String source,
            @RequestHeader("Authorization") String token)
    {

        String ipAddr = "";
        List<CartDto> cartList = null;
        long currentTimeMillis = System.currentTimeMillis();
        String name = "";
        String pan = "";
        String mobile = "";
        String client_name = "";
        try
        {
            String userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            System.out.println("User ID from token: " + userid);

            iin_number = NseUtils.checkParem(iin_number);
            unique_number = NseUtils.checkParem(unique_number);
            effective_date = NseUtils.checkParem(effective_date);
            source = NseUtils.checkParem(source);

            if(effective_date.isEmpty() || unique_number.isEmpty() || iin_number.isEmpty())
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "Transaction number or IIN Number is empty."));
            }

            if(effective_date.isEmpty() || Integer.parseInt(effective_date) <= 0)
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "Format : DD/MM/YYYY. A future date. effective date < next installment date."));
            }

            UserDto user = userServiceClient.getUserById(Integer.valueOf(userid), token);

            if (user == null)
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "User not found"));
            }

            name = user.getName();
            pan = user.getPan();
            mobile = user.getMobile();
            client_name = user.getClient_name();

            String login_userid = userid;
            String login_name = name;
            String login_mobile = mobile;
            String selected_name = "";

            String appln_id = "";
            String broker_code = "";
            String password = "";
            String euin = "";
            String host = "";

            BseNseKeyDto nsekey = userServiceClient.getByClientName(client_name,token);
            appln_id = nsekey.getNse_appln_id();
            broker_code = nsekey.getBrokerCode();
            password = nsekey.getNse_password();
            euin = nsekey.getEuin();
            euin = euin.split(",")[0];
            host = nsekey.getDomain_url();

            SimpleDateFormat df1 = new SimpleDateFormat("dd-MMM-yyyy");
            String cease_req_date = df1.format(new Date());

            NseTransactions transaction = null;
            Optional<NseTransactions> transactionOpt = nseTransactionRepository.getNseTransactionDetails(unique_number, client_name);

            if(transactionOpt.isPresent())
            {
                transaction = transactionOpt.get();
            }else
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "SIP Registration Details not available."));
            }

            Date tranDate = transaction.getTransaction_date();
            String sip_reg_number = transaction.getSip_reg_no();

            SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
            Calendar cal = Calendar.getInstance();
            Date today = cal.getTime();
            cal.setTime(tranDate);
            cal.add(Calendar.DATE, 30);
            Date endDate = cal.getTime();
            if(endDate.compareTo(today) > 0)
            {
                endDate = today;
            }

            String from_date = sdf.format(tranDate);
            String end_date = sdf.format(endDate);

            JSONArray regDetailsArray = new JSONArray();
            JSONObject regObject = new JSONObject();

            regObject.put("sip_reg_no", sip_reg_number);
            regObject.put("effective_date", effective_date);
            regObject.put("pause_flag", "Y");
            regObject.put("remarks", "");
            regDetailsArray.put(regObject);

            JSONObject requestBody = new JSONObject();
            requestBody.put("pause_data", regDetailsArray);

            BseNseOnlineAccessDto online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, broker_code,token);

            if(online_access == null)
            {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "NSE Online Credentials Not available. Please contact your RM"));
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
            headers.set("Accept-Language", "en-US");
            headers.set("Connection", "keep-alive");
            headers.set("Referer", "");
            headers.set("Accept-Encoding", "gzip");
            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            System.out.println("requestBody: " + requestBody.toString());

            String xsipPauseService_url= NseApiUrls.xsipPauseService_url;

            System.out.println("xsipPauseService_url: " + xsipPauseService_url);

            String reg_id = "";
            String reg_status = "";
            String reg_remark = "";

            try
            {
                ResponseEntity<String> result = RestTemplateFactory.createRestTemplate().postForEntity(xsipPauseService_url, entity, String.class);
                String status_code = result.getStatusCode().toString();
                String responseBody = result.getBody().toString();

                JSONObject jsonObject = new JSONObject(responseBody);
                System.out.println("jsonObject: " + jsonObject);
                JSONArray jsonRegArray = jsonObject.getJSONArray("reg_data");

                System.out.println("swpRegistrationService::responseBody: " + responseBody);
                for (int i = 0; i < jsonRegArray.length(); i++)
                {
                    JSONObject regDetail = jsonRegArray.getJSONObject(i);

                    reg_id = regDetail.optString("reg_id");
                    reg_status = regDetail.optString("reg_status");
                    reg_remark = regDetail.optString("reg_remark");

                    System.out.println("reg_id: " + reg_id);
                    System.out.println("reg_status" + reg_status);
                    System.out.println("reg_remark: " + reg_remark);
                }

                NseTransactions nsetrans = new NseTransactions();
                nsetrans.setUrl(xsipPauseService_url);
                nsetrans.setNse_request(requestBody.toString());
                nsetrans.setNse_response(responseBody.toString());
                nsetrans.setReturn_msg(reg_status);
                nsetrans.setService_return_code(status_code);
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
                nsetrans.setTransaction_number(reg_id);
                nsetrans.setApplication_number("");
                nsetrans.setTo_scheme_code("");
                nsetrans.setTo_scheme_name("");
                nsetrans.setTransaction_type("SIP Topup Registration Service");
                nsetrans.setTransaction_status("");
                nsetrans.setPayment_status("");
                nsetrans.setActive_ceased_status("");
                nsetrans.setRemarks(reg_remark);
                nsetrans.setMandate_id("");
                nsetrans.setMandate_status("");
                nsetrans.setEmandate_auth_flag("");
                nsetrans.setApp_received_flag("");
                nsetrans.setTransaction_date(new Date());
                nsetrans.setUser_id(Integer.parseInt(userid));
                if(source.equalsIgnoreCase("Mobile"))
                {
                    nsetrans.setRegister_source("Mobile");
                }else
                {
                    nsetrans.setRegister_source("Website");
                }
                nsetrans.setEuin_number(euin);
                nsetrans.setCc_received("");
                nsetrans.setFund_trans_to_amc("");
                nsetrans.setRefund_status("");
                nsetrans.setRefund_amount("");
                nseTransactionService.save(nsetrans);
            }
            catch (HttpClientErrorException | HttpServerErrorException ex) {

                // For 4xx and 5xx responses
                System.out.println("swpRegistrationService::Status Code: " + ex.getStatusCode());
                System.out.println("swpRegistrationService::Response Body: " + ex.getResponseBodyAsString());
            } catch (Exception ex) {

                // Other exceptions (e.g., connection issues)
                ex.printStackTrace();
            }

            ipAddr = NseUtils.getIpAddr(request);
            if(ipAddr == null){ipAddr="";}

            if(reg_status.equalsIgnoreCase("TRXN SUCCESS"))
            {
                return NseUtils.transactionResponse(HttpStatus.OK, reg_remark + " Your Order is successfully triggered...! orderID: " + reg_id, new HashMap<>());
            }else{
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), reg_remark));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "Transaction failed. Please try again!"));
        }
    }

    @Operation(
            summary = "Submit ACH Mandate",
            description = "Submits an ACH (Automated Clearing House) mandate for the investor through NSE. Works for both Web and Mobile based on the 'source' parameter.",
            parameters = {
                    @Parameter(name = "iin_number", description = "Investor Identification Number", required = false),
                    @Parameter(name = "ach_from_date", description = "ACH valid from date (Format: yyyy-MM-dd)", required = false),
                    @Parameter(name = "ach_to_date", description = "ACH valid to date (Format: yyyy-MM-dd)", required = false),
                    @Parameter(name = "amount", description = "Mandate amount", required = false),
                    @Parameter(name = "ifsc_code", description = "IFSC code of the bank", required = false),
                    @Parameter(name = "bank_code", description = "Bank code", required = false),
                    @Parameter(name = "branch_name", description = "Branch name", required = false),
                    @Parameter(name = "account_number", description = "Bank account number", required = false),
                    @Parameter(name = "account_holder_name", description = "Account holder name", required = false),
                    @Parameter(name = "account_type", description = "Account type (Savings/Current)", required = false),
                    @Parameter(name = "until_cancelled", description = "Is the mandate until cancelled? (Y/N)", required = false),
                    @Parameter(name = "mandate_type", description = "Type of mandate (Physical/Digital)", required = false),
                    @Parameter(name = "mandate_option", description = "Mandate option (One Time/Recurring)", required = false),
                    @Parameter(name = "broker_code", description = "Broker code", required = false),
                    @Parameter(name = "euin_code", description = "EUIN code", required = false),
                    @Parameter(name = "micr_code", description = "MICR code of the bank", required = false),
                    @Parameter(name = "source", description = "Transaction source (web/mobile)", required = false),
                    @Parameter(
                            name = "Authorization",
                            description = "Bearer token for authentication",
                            required = true,
                            in = ParameterIn.HEADER
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "ACH mandate successfully submitted",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TransactionResponse.class),
                                    examples = @ExampleObject(
                                            name = "SuccessACHSubmission",
                                            summary = "ACH Submitted",
                                            value = """
                                            {
                                              "status": 200,
                                              "status_msg": "SUCCESS",
                                              "msg": "ACH mandate submitted successfully",
                                              "return_msg": "ACH#MND123456 submitted",
                                              "transaction_status": {
                                                "ACH Mandate": "Submitted and under processing"
                                              }
                                            }
                                            """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request or missing data",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "BadRequestExample",
                                            summary = "Invalid STP request",
                                            value = "{ \"status\": 400, \"error\": \"Bad Request\", \"message\": \"Invalid or missing parameters\" }"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal Server Error",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "ServerErrorExample",
                                            summary = "Unexpected server error",
                                            value = "{ \"status\": 500, \"error\": \"Internal Server Error\", \"message\": \"Something went wrong on the server\" }"
                                    )
                            )
                    )
            }
    )
    @PostMapping("/submitACH")
    public ResponseEntity<?> submitACH(
            HttpServletRequest request,
            @RequestParam(required = false) String iin_number,
            @RequestParam(required = false) String ach_from_date,
            @RequestParam(required = false) String ach_to_date,
            @RequestParam(required = false) String amount,
            @RequestParam(required = false) String ifsc_code,
            @RequestParam(required = false) String bank_code,
            @RequestParam(required = false) String branch_name,
            @RequestParam(required = false) String account_number,
            @RequestParam(required = false) String account_holder_name,
            @RequestParam(required = false) String account_type,
            @RequestParam(required = false) String until_cancelled,
            @RequestParam(required = false) String mandate_type,
            @RequestParam(required = false) String mandate_option,
            @RequestParam(required = false) String broker_code,
            @RequestParam(required = false) String euin_code,
            @RequestParam(required = false) String micr_code,
            @RequestParam(required = false) String source,
            @RequestHeader("Authorization") String token)
    {

        String ipAddr = "";
        List<CartDto> cartList = null;
        long currentTimeMillis = System.currentTimeMillis();
        String name = "";
        String pan = "";
        String mobile = "";
        String client_name = "";
        try
        {
            String userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            System.out.println("User ID from token: " + userid);

            iin_number = NseUtils.checkParem(iin_number);
            ach_from_date = NseUtils.checkParem(ach_from_date);
            ach_to_date = NseUtils.checkParem(ach_to_date);
            amount = NseUtils.checkParem(amount);
            ifsc_code = NseUtils.checkParem(ifsc_code);
            bank_code = NseUtils.checkParem(bank_code);
            branch_name = NseUtils.checkParem(branch_name);
            account_number = NseUtils.checkParem(account_number);
            account_holder_name = NseUtils.checkParem(account_holder_name);
            account_type = NseUtils.checkParem(account_type);
            until_cancelled = NseUtils.checkParem(until_cancelled);
            mandate_type = NseUtils.checkParem(mandate_type);
            mandate_option = NseUtils.checkParem(mandate_option);
            broker_code = NseUtils.checkParem(broker_code);
            euin_code = NseUtils.checkParem(euin_code);
            micr_code = NseUtils.checkParem(micr_code);
            source = NseUtils.checkParem(source);

            if (mandate_type.isEmpty()) {mandate_type = "E";};
            if (mandate_option.isEmpty()) {mandate_option = "NET";}

            UserDto user = userServiceClient.getUserById(Integer.valueOf(userid), token);

            if (user == null)
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "User not found"));
            }

            String appln_id = "";
            String password = "";
            String euin = "";
            String host = "";
            client_name = user.getClient_name();

            BseNseKeyDto nsekey = userServiceClient.getByClientName(client_name,token);
            String broker_code1 = nsekey.getBrokerCode();

            host = nsekey.getDomain_url();

            if(broker_code1 == null){broker_code1 = "";};



                broker_code = broker_code1;
                appln_id = nsekey.getNse_appln_id();
                password = nsekey.getNse_password();
                if (!euin_code.isEmpty()) {
                    euin = euin_code;
                } else {
                    euin = nsekey.getEuin();
                }

            euin = euin.split(",")[0];

            String nse_iin = user.getNse_iin_number();
            String selected_name = "";
            UserDto nse = null;
            if (!nse_iin.equalsIgnoreCase(iin_number))
            {
                 nse = userServiceClient.getUserBseNseDetailsByIinnumber(client_name,iin_number,token);

                if(nse == null)
                {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "No User Found"));
                }
                pan = nse.getPan();
                name = nse.getName();
                selected_name = name + " (" + userid + ")";
            } else
            {
                pan = user.getPan();
                name = user.getName();
                selected_name = name + " (" + userid + ")";
            }

            SimpleDateFormat inputFormat = new SimpleDateFormat("dd-MM-yyyy");
            SimpleDateFormat slashFormat = new SimpleDateFormat("dd/MM/yyyy");

            ach_from_date = ach_from_date.replace("/", "-");
            ach_to_date = ach_to_date.replace("/", "-");

            Date achFromDateObj = inputFormat.parse(ach_from_date);
            ach_from_date = slashFormat.format(achFromDateObj);

            Date achToDateObj = null;
            if (ach_to_date != null && !ach_to_date.isEmpty()) {
                achToDateObj = inputFormat.parse(ach_to_date);
                ach_to_date = slashFormat.format(achToDateObj);
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate today = LocalDate.now();
            String todayStr = today.format(formatter);

            boolean processedByMember = true;
            String registrationDate = "";

            if (processedByMember) {
                try {
                    if (ach_from_date != null && !ach_from_date.isBlank()) {
                        LocalDate startDate = LocalDate.parse(ach_from_date, formatter);
                        if (!today.isAfter(startDate)) {
                            registrationDate = todayStr;
                        }
                    } else {
                        registrationDate = todayStr;
                    }
                } catch (DateTimeParseException e) {
                    e.printStackTrace();
                }
            }

            JSONArray mandateDetailsArray = new JSONArray();
            JSONObject mandateReg = new JSONObject();
            mandateReg.put("client_code", nse_iin);
            mandateReg.put("amount", amount);
            mandateReg.put("mandate_type", mandate_type);
            mandateReg.put("account_no", account_number);
            mandateReg.put("ac_type", account_type);
            mandateReg.put("ifsc_code", ifsc_code);
            mandateReg.put("micr_code", micr_code);
            mandateReg.put("start_date", ach_from_date);
            mandateReg.put("end_date", ach_to_date);
            mandateReg.put("registration_date", registrationDate);
            mandateReg.put("member_mandate_no", "");
            mandateDetailsArray.put(mandateReg);

            BseNseOnlineAccessDto online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, broker_code,token);

            if(online_access == null)
            {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "NSE Online Credentials Not available. Please contact your RM"));
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
            requestBody.put("reg_data", mandateDetailsArray);
            System.out.println("requestBody = " + requestBody);

            RestTemplate restTemplate = RestTemplateFactory.createRestTemplate();

            System.out.println("mandateres" + mandateReg.toString());

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            String mandate_url = NseApiUrls.mandate_url;

            String reg_id = "";
            String reg_status = "";
            String reg_remark = "";

            try {
                ResponseEntity<String> mandateResult = restTemplate.postForEntity(mandate_url, entity,
                        String.class);
                String statusCode = mandateResult.getStatusCode().toString();
                String responseBody = mandateResult.getBody().toString();

                System.out.println("statusCode = " + statusCode);
                System.out.println("responseBody = " + responseBody);

                JSONObject jsonObject = new JSONObject(responseBody);

                if (jsonObject.has("reg_data")) {
                    JSONArray regDataArray = jsonObject.getJSONArray("reg_data");

                    if (regDataArray.length() > 0) {
                        JSONObject regData = regDataArray.getJSONObject(0);

                        reg_id = regData.optString("reg_id", "");
                        reg_status = regData.optString("reg_status", "");
                        reg_remark = regData.optString("reg_remark", "");

                    }
                }

                NseTransactions nsetrans = new NseTransactions();
                nsetrans.setUrl(mandate_url);
                nsetrans.setNse_request(requestBody.toString());
                nsetrans.setNse_response(responseBody.toString());
                nsetrans.setReturn_msg(reg_remark);
                nsetrans.setService_return_code(statusCode);
                nsetrans.setService_msg(reg_remark);
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
                nsetrans.setTransaction_type("ACH Mandate Request");
                nsetrans.setTransaction_status("");
                nsetrans.setPayment_status("");
                nsetrans.setActive_ceased_status("");
                nsetrans.setRemarks(reg_remark);
                nsetrans.setMandate_id("");
                nsetrans.setMandate_status("");
                nsetrans.setEmandate_auth_flag("");
                nsetrans.setApp_received_flag("");
                nsetrans.setTransaction_date(new Date());
                nsetrans.setUser_id(Integer.parseInt(userid));

                if(source.equalsIgnoreCase("Mobile"))
                {
                    nsetrans.setRegister_source("Mobile");
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

                ipAddr = NseUtils.getIpAddr(request);
                if(ipAddr == null){ipAddr="";}

                if (reg_status.equalsIgnoreCase("REG_SUCCESS")) {

                    if (!nse_iin.equalsIgnoreCase(iin_number)) {
                        if (account_number.equalsIgnoreCase(nse.getBank_account_number1())) {
                            nse.setNse_ach_flag1(1);
                            nse.setNse_ach1(reg_id);
                            nse.setNse_ach_amount1(amount);
                            nse.setNse_ach_approved1(0);
                            nse.setNse_ach_rej_reason1("");
                            nse.setNse_ach_created_date1(new Date());
                            //nse.setNse_ach_mode1(mandate_type);
                            userServiceClient.saveBseNseDetails(nse,token);

                        } else if (account_number.equalsIgnoreCase(nse.getBank_account_number2())) {
                            nse.setNse_ach_flag2(1);
                            nse.setNse_ach2(reg_id);
                            nse.setNse_ach_amount2(amount);
                            nse.setNse_ach_approved2(0);
                            nse.setNse_ach_rej_reason2("");
                            nse.setNse_ach_created_date2(new Date());
                            //nse.setNse_ach_mode2(mandate_type);
                            userServiceClient.saveBseNseDetails(nse,token);

                        } else if (account_number.equalsIgnoreCase(nse.getBank_account_number3())) {
                            nse.setNse_ach_flag3(1);
                            nse.setNse_ach3(reg_id);
                            nse.setNse_ach_amount3(amount);
                            nse.setNse_ach_approved3(0);
                            nse.setNse_ach_rej_reason3("");
                            nse.setNse_ach_created_date3(new Date());
                            //.setNse_ach_mode3(mandate_type);
                            userServiceClient.saveBseNseDetails(nse, token);
                        }
                    } else {
                        if (account_number.equalsIgnoreCase(user.getBank_account_number1())) {
                            user.setNse_ach_flag1(1);
                            user.setNse_ach1(reg_id);
                            user.setNse_ach_amount1(amount);
                            user.setNse_ach_approved1(0);
                            user.setNse_ach_rej_reason1("");
                            user.setNse_ach_created_date1(new Date());
                            //user.setNse_ach_mode1(mandate_type);
                            userServiceClient.saveUser(user, token);

                        } else if (account_number.equalsIgnoreCase(user.getBank_account_number2())) {
                            user.setNse_ach_flag2(1);
                            user.setNse_ach2(reg_id);
                            user.setNse_ach_amount2(amount);
                            user.setNse_ach_approved2(0);
                            user.setNse_ach_rej_reason2("");
                            user.setNse_ach_created_date2(new Date());
                            //user.setNse_ach_mode2(mandate_type);
                            userServiceClient.saveUser(user, token);

                        } else if (account_number.equalsIgnoreCase(user.getBank_account_number3())) {
                            user.setNse_ach_flag3(1);
                            user.setNse_ach3(reg_id);
                            user.setNse_ach_amount3(amount);
                            user.setNse_ach_approved3(0);
                            user.setNse_ach_rej_reason3("");
                            user.setNse_ach_created_date3(new Date());
                            //user.setNse_ach_mode3(mandate_type);
                            userServiceClient.saveUser(user, token);
                        }
                    }
                }
                if(reg_status.equalsIgnoreCase("REG_SUCCESS"))
                {
                    return NseUtils.transactionResponse(HttpStatus.OK, "Bank Mandate Registration done Successfully " + reg_id, new HashMap<>());
                }else
                {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), reg_remark));
                }
            } catch (Exception ex)
            {
                ex.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "Transaction failed. Please try again!"));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CommonResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "Transaction failed. Please try again!"));
        }
    }
    @Operation(summary = "Save bank details", description = "Saves bank account details for a given investor based on the provided token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bank details saved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request or invalid data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/saveBankDetails")
    public ResponseEntity<?> saveBankDetails(
            HttpServletRequest request,
            @RequestParam(required = false) String iin_number,
            @RequestParam(required = false) String bank_ifsc_code,
            @RequestParam(required = false) String bank_micr_code,
            @RequestParam(required = false) String bank_name,
            @RequestParam(required = false) String bank_address,
            @RequestParam(required = false) String bank_branch,
            @RequestParam(required = false) String bank_account_number,
            @RequestParam(required = false) String bank_account_holder_name,
            @RequestParam(required = false) String bank_account_type,
            @RequestParam(required = false) String bank_code,
            @RequestParam(required = false) String process_mode,
            @RequestParam(required = false) String broker_code,
            @RequestParam(required = false) String euin_code,
            @RequestParam(required = false) String source,
            @RequestHeader("Authorization") String token) {

            try {
                String userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
                UserDto users = userServiceClient.getUserById(Integer.valueOf(userid), token);
                String client_name = users.getClient_name();

                iin_number = NseUtils.checkParem(iin_number);
                bank_ifsc_code = NseUtils.checkParem(bank_ifsc_code);
                bank_micr_code = NseUtils.checkParem(bank_micr_code);
                bank_name = NseUtils.checkParem(bank_name);
                bank_address = NseUtils.checkParem(bank_address);
                bank_code = NseUtils.checkParem(bank_code);
                bank_branch = NseUtils.checkParem(bank_branch);
                bank_account_number = NseUtils.checkParem(bank_account_number);
                bank_account_holder_name = NseUtils.checkParem(bank_account_holder_name);
                bank_account_type = NseUtils.checkParem(bank_account_type);
                bank_code = NseUtils.checkParem(bank_code);
                process_mode = NseUtils.checkParem(process_mode);
                broker_code = NseUtils.checkParem(broker_code);
                euin_code = NseUtils.checkParem(euin_code);
                source = NseUtils.checkParem(source);

                UserDto user = null;
                try {
                    user = userServiceClient.getUserDetailsByID(client_name, Integer.valueOf(userid),token);
                } catch (FeignException e) {
                    if (e.status() == 400) {
                        System.out.println("Bad Request: " + e.getMessage());
                    } else if (e.status() == 404) {
                        System.out.println("User not found: " + e.getMessage());
                    } else {
                        System.out.println("Feign error: " + e.status() + " - " + e.getMessage());
                    }
                }

                String appln_id = "";
                String password = "";
                String euin = "";
                String host = "";


                BseNseKeyDto nsekey = userServiceClient.getByClientName(client_name,token);
                String broker_code1 = nsekey.getBrokerCode();

                host = nsekey.getDomain_url();


                    broker_code = broker_code1;
                    appln_id = nsekey.getNse_appln_id();
                    password = nsekey.getNse_password();

                    if(!euin_code.isEmpty())
                    {
                        euin = euin_code;
                    }else{
                        euin = nsekey.getEuin();
                    }

                euin = euin.split(",")[0];

                String nse_iin = user.getNse_iin_number();
                String pan = "";
                String name = "";
                String selected_name = "";
                UserBseNseDto nse = null;
                boolean checking_flag = false;

                if(!nse_iin.equalsIgnoreCase(iin_number))
                {

                    nse = userServiceClient.getUserBseNseDetailsByIinNumber(client_name,iin_number,token);

                    pan = nse.getPan();
                    name = nse.getName();
                    selected_name = name + " (" + userid + ")";

                    if(process_mode.equalsIgnoreCase("I"))
                    {


                        String acc_no1 = nse.getBank_account_number1();
                        String acc_no2 = nse.getBank_account_number2();
                        String acc_no3 = nse.getBank_account_number3();

                        if(acc_no1 == null){acc_no1 = "";}
                        if(acc_no2 == null){acc_no2 = "";}
                        if(acc_no3 == null){acc_no3 = "";}

                        if(acc_no1.equalsIgnoreCase(bank_account_number))
                        {
                            checking_flag = true;
                        }else if(acc_no2.equalsIgnoreCase(bank_account_number))
                        {
                            checking_flag = true;
                        }else if(acc_no3.equalsIgnoreCase(bank_account_number))
                        {
                            checking_flag = true;
                        }
                    }
                } else
                {
                    pan = user.getPan();
                    name = user.getName();
                    selected_name = name + " (" + userid + ")";

                    if(process_mode.equalsIgnoreCase("I"))
                    {
                        String acc_no1 = user.getBank_account_number1();
                        String acc_no2 = user.getBank_account_number2();
                        String acc_no3 = user.getBank_account_number3();

                        if(acc_no1 == null){acc_no1 = "";}
                        if(acc_no2 == null){acc_no2 = "";}
                        if(acc_no3 == null){acc_no3 = "";}

                        if(acc_no1.equalsIgnoreCase(bank_account_number))
                        {
                            checking_flag = true;
                        }else if(acc_no2.equalsIgnoreCase(bank_account_number))
                        {
                            checking_flag = true;
                        }else if(acc_no3.equalsIgnoreCase(bank_account_number))
                        {
                            checking_flag = true;
                        }
                    }
                }

                if(checking_flag)
                {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("This account number already exists!");
                }

                JSONArray requestDetailsArray = new JSONArray();
                JSONObject requestDetails = new JSONObject();
                requestDetails.put("client_code", iin_number);
                requestDetails.put("action_type", "ADD");
                requestDetails.put("account_type", bank_account_type);
                requestDetails.put("account_no", bank_account_number);
                requestDetails.put("micr_no", bank_micr_code);
                requestDetails.put("ifsc_code", bank_ifsc_code);
                requestDetails.put("default_bank_flag", "N");
                requestDetailsArray.put(requestDetails);

                BseNseOnlineAccessDto online_access = null;
                try
                {
                    online_access = userServiceClient.getBseNseOnlineAccessByClientName(
                            client_name,
                            broker_code,token
                    );
                } catch (feign.FeignException.NotFound ex)
                {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "NSE Online Credentials Not available. Please contact your RM"));

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

                JSONObject requestBody = new JSONObject();
                requestBody.put("bank_dtl", requestDetailsArray);
                System.out.println("requestBody = " + requestBody.toString());

                HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

                String clientBankDetail_url = NseApiUrls.clientBankDetail_url;

                String status= "";
                String error_remark= "";

                try{

                    ResponseEntity<String> mandateResult = RestTemplateFactory.createRestTemplate().postForEntity(clientBankDetail_url, entity, String.class);
                    String statusCode = mandateResult.getStatusCode().toString();
                    String responseBody = mandateResult.getBody().toString();

                    System.out.println("statusCode = " + statusCode);
                    System.out.println("responseBody = " + responseBody);

                    JSONObject jsonObject = new JSONObject(responseBody);

                    if (jsonObject.has("bank_dtl")) {
                        JSONArray regDataArray = jsonObject.getJSONArray("bank_dtl");

                        if (regDataArray.length() > 0) {
                            JSONObject regData = regDataArray.getJSONObject(0);

                            status= regData.optString("status");
                            error_remark= regData.optString("error_remark");
                        }
                    }

                    System.out.println("status: " + status);
                    System.out.println("error_remark: " + error_remark);

                    if(status.equalsIgnoreCase("SUCCESS"))
                    {
                        System.out.println("process_mode = " + process_mode);
                        System.out.println("nse_iin = " + nse_iin);
                        System.out.println("iin_number = " + iin_number);
                        if(process_mode.equalsIgnoreCase("I"))
                        {
                            if(!nse_iin.equalsIgnoreCase(iin_number))
                            {
                                String bank1 = nse.getBank_account_number1();
                                String bank2 = nse.getBank_account_number2();
                                String bank3 = nse.getBank_account_number3();

                                if(bank1 == null){bank1 = "";}
                                if(bank2 == null){bank2 = "";}
                                if(bank3 == null){bank3 = "";}

                                if(StringHelper.isEmpty(bank1))
                                {
                                    nse.setBank_account_holder_name1(bank_account_holder_name);
                                    nse.setBank_account_number1(bank_account_number);
                                    nse.setBank_name1(bank_name);
                                    nse.setBank_account_type1(bank_account_type);
                                    nse.setBank_address1(bank_address);
                                    nse.setBank_branch1(bank_branch);
                                    nse.setBank_ifsc_code1(bank_ifsc_code);
                                    nse.setBank_micr_code1(bank_micr_code);
                                    nse.setDefault_bank1("Y");
                                    nse.setBank_code1(bank_code);

                                }else if(StringHelper.isNotEmpty(bank1) && StringHelper.isEmpty(bank2))
                                {
                                    nse.setBank_account_holder_name2(bank_account_holder_name);
                                    nse.setBank_account_number2(bank_account_number);
                                    nse.setBank_name2(bank_name);
                                    nse.setBank_account_type2(bank_account_type);
                                    nse.setBank_address2(bank_address);
                                    nse.setBank_branch2(bank_branch);
                                    nse.setBank_ifsc_code2(bank_ifsc_code);
                                    nse.setBank_micr_code2(bank_micr_code);
                                    nse.setDefault_bank2("N");
                                    nse.setBank_code2(bank_code);

                                }else if(StringHelper.isNotEmpty(bank2) && StringHelper.isEmpty(bank3))
                                {
                                    nse.setBank_account_holder_name3(bank_account_holder_name);
                                    nse.setBank_account_number3(bank_account_number);
                                    nse.setBank_name3(bank_name);
                                    nse.setBank_account_type3(bank_account_type);
                                    nse.setBank_address3(bank_address);
                                    nse.setBank_branch3(bank_branch);
                                    nse.setBank_ifsc_code3(bank_ifsc_code);
                                    nse.setBank_micr_code3(bank_micr_code);
                                    nse.setDefault_bank3("N");
                                    nse.setBank_code3(bank_code);

                                }
                                userServiceClient.saveUserBseNseDetail(nse,token);
                            }else
                            {
                                String bank1 = user.getBank_account_number1();
                                String bank2 = user.getBank_account_number2();
                                String bank3 = user.getBank_account_number3();

                                if(bank1 == null){bank1 = "";}
                                if(bank2 == null){bank2 = "";}
                                if(bank3 == null){bank3 = "";}

                                if(StringHelper.isEmpty(bank1))
                                {
                                    user.setBank_account_holder_name1(bank_account_holder_name);
                                    user.setBank_account_number1(bank_account_number);
                                    user.setBank_name1(bank_name);
                                    user.setBank_account_type1(bank_account_type);
                                    user.setBank_address1(bank_address);
                                    user.setBank_branch1(bank_branch);
                                    user.setBank_ifsc_code1(bank_ifsc_code);
                                    user.setBank_micr_code1(bank_micr_code);
                                    user.setDefault_bank1("Y");
                                    user.setBank_code1(bank_code);
                                    user.set_purchase_allowed(true);
                                    user.set_redeem_allowed(true);
                                    user.set_switch_allowed(true);
                                    user.set_stp_allowed(true);
                                    user.set_stp_allowed(true);

                                }else if(StringHelper.isNotEmpty(bank1) && StringHelper.isEmpty(bank2))
                                {
                                    user.setBank_account_holder_name2(bank_account_holder_name);
                                    user.setBank_account_number2(bank_account_number);
                                    user.setBank_name2(bank_name);
                                    user.setBank_account_type2(bank_account_type);
                                    user.setBank_address2(bank_address);
                                    user.setBank_branch2(bank_branch);
                                    user.setBank_ifsc_code2(bank_ifsc_code);
                                    user.setBank_micr_code2(bank_micr_code);
                                    user.setDefault_bank2("N");
                                    user.setBank_code2(bank_code);
                                    user.set_purchase_allowed(true);
                                    user.set_redeem_allowed(true);
                                    user.set_switch_allowed(true);
                                    user.set_stp_allowed(true);
                                    user.set_stp_allowed(true);

                                }else if(StringHelper.isNotEmpty(bank2) && StringHelper.isEmpty(bank3))
                                {
                                    user.setBank_account_holder_name3(bank_account_holder_name);
                                    user.setBank_account_number3(bank_account_number);
                                    user.setBank_name3(bank_name);
                                    user.setBank_account_type3(bank_account_type);
                                    user.setBank_address3(bank_address);
                                    user.setBank_branch3(bank_branch);
                                    user.setBank_ifsc_code3(bank_ifsc_code);
                                    user.setBank_micr_code3(bank_micr_code);
                                    user.setDefault_bank3("N");
                                    user.setBank_code3(bank_code);
                                    user.set_purchase_allowed(true);
                                    user.set_redeem_allowed(true);
                                    user.set_switch_allowed(true);
                                    user.set_stp_allowed(true);
                                    user.set_stp_allowed(true);

                                }
                                userServiceClient.saveUser(user,token);
                            }
                        }else {

                            if(!nse_iin.equalsIgnoreCase(iin_number))
                            {
                                String acc_no1 = nse.getBank_account_number1();
                                String acc_no2 = nse.getBank_account_number2();
                                String acc_no3 = nse.getBank_account_number3();

                                if(acc_no1 == null){acc_no1 = "";}
                                if(acc_no2 == null){acc_no2 = "";}
                                if(acc_no3 == null){acc_no3 = "";}

                                if(acc_no1.equalsIgnoreCase(bank_account_number))
                                {
                                    nse.setBank_account_holder_name1(bank_account_holder_name);
                                    nse.setBank_account_number1(bank_account_number);
                                    nse.setBank_name1(bank_name);
                                    nse.setBank_account_type1(bank_account_type);
                                    nse.setBank_address1(bank_address);
                                    nse.setBank_branch1(bank_branch);
                                    nse.setBank_ifsc_code1(bank_ifsc_code);
                                    nse.setBank_micr_code1(bank_micr_code);
                                    nse.setBank_code1(bank_code);
                                    user.set_purchase_allowed(true);
                                    user.set_redeem_allowed(true);
                                    user.set_switch_allowed(true);
                                    user.set_stp_allowed(true);
                                    user.set_stp_allowed(true);
                                }
                                if(acc_no2.equalsIgnoreCase(bank_account_number))
                                {
                                    nse.setBank_account_holder_name2(bank_account_holder_name);
                                    nse.setBank_account_number2(bank_account_number);
                                    nse.setBank_name2(bank_name);
                                    nse.setBank_account_type2(bank_account_type);
                                    nse.setBank_address2(bank_address);
                                    nse.setBank_branch2(bank_branch);
                                    nse.setBank_ifsc_code2(bank_ifsc_code);
                                    nse.setBank_micr_code2(bank_micr_code);
                                    nse.setBank_code2(bank_code);
                                    user.set_purchase_allowed(true);
                                    user.set_redeem_allowed(true);
                                    user.set_switch_allowed(true);
                                    user.set_stp_allowed(true);
                                    user.set_stp_allowed(true);
                                }
                                if(acc_no3.equalsIgnoreCase(bank_account_number))
                                {
                                    nse.setBank_account_holder_name3(bank_account_holder_name);
                                    nse.setBank_account_number3(bank_account_number);
                                    nse.setBank_name3(bank_name);
                                    nse.setBank_account_type3(bank_account_type);
                                    nse.setBank_address3(bank_address);
                                    nse.setBank_branch3(bank_branch);
                                    nse.setBank_ifsc_code3(bank_ifsc_code);
                                    nse.setBank_micr_code3(bank_micr_code);
                                    nse.setBank_code3(bank_code);
                                    user.set_purchase_allowed(true);
                                    user.set_redeem_allowed(true);
                                    user.set_switch_allowed(true);
                                    user.set_stp_allowed(true);
                                    user.set_stp_allowed(true);
                                }
                                userServiceClient.saveUserBseNseDetail(nse,token);
                            }else
                            {
                                String acc_no1 = user.getBank_account_number1();
                                String acc_no2 = user.getBank_account_number2();
                                String acc_no3 = user.getBank_account_number3();

                                if(acc_no1 == null){acc_no1 = "";}
                                if(acc_no2 == null){acc_no2 = "";}
                                if(acc_no3 == null){acc_no3 = "";}

                                if(acc_no1.equalsIgnoreCase(bank_account_number))
                                {
                                    user.setBank_account_holder_name1(bank_account_holder_name);
                                    user.setBank_account_number1(bank_account_number);
                                    user.setBank_name1(bank_name);
                                    user.setBank_account_type1(bank_account_type);
                                    user.setBank_address1(bank_address);
                                    user.setBank_branch1(bank_branch);
                                    user.setBank_ifsc_code1(bank_ifsc_code);
                                    user.setBank_micr_code1(bank_micr_code);
                                    user.setBank_code1(bank_code);
                                    user.set_purchase_allowed(true);
                                    user.set_redeem_allowed(true);
                                    user.set_switch_allowed(true);
                                    user.set_stp_allowed(true);
                                    user.set_stp_allowed(true);
                                }
                                if(acc_no2.equalsIgnoreCase(bank_account_number))
                                {
                                    user.setBank_account_holder_name2(bank_account_holder_name);
                                    user.setBank_account_number2(bank_account_number);
                                    user.setBank_name2(bank_name);
                                    user.setBank_account_type2(bank_account_type);
                                    user.setBank_address2(bank_address);
                                    user.setBank_branch2(bank_branch);
                                    user.setBank_ifsc_code2(bank_ifsc_code);
                                    user.setBank_micr_code2(bank_micr_code);
                                    user.setBank_code2(bank_code);
                                    user.set_purchase_allowed(true);
                                    user.set_redeem_allowed(true);
                                    user.set_switch_allowed(true);
                                    user.set_stp_allowed(true);
                                    user.set_stp_allowed(true);
                                }
                                if(acc_no3.equalsIgnoreCase(bank_account_number))
                                {
                                    user.setBank_account_holder_name3(bank_account_holder_name);
                                    user.setBank_account_number3(bank_account_number);
                                    user.setBank_name3(bank_name);
                                    user.setBank_account_type3(bank_account_type);
                                    user.setBank_address3(bank_address);
                                    user.setBank_branch3(bank_branch);
                                    user.setBank_ifsc_code3(bank_ifsc_code);
                                    user.setBank_micr_code3(bank_micr_code);
                                    user.setBank_code3(bank_code);
                                    user.set_purchase_allowed(true);
                                    user.set_redeem_allowed(true);
                                    user.set_switch_allowed(true);
                                    user.set_stp_allowed(true);
                                    user.set_stp_allowed(true);
                                }
                                System.out.println("user = " + user);
                                System.out.println("purchase allowed = " + user.isIs_purchase_allowed());
                                System.out.println("redeem allowed   = " + user.isIs_redeem_allowed());
                                System.out.println("switch allowed   = " + user.isIs_switch_allowed());
                                System.out.println("stp allowed      = " + user.isIs_stp_allowed());
                                System.out.println("swp allowed      = " + user.isIs_swp_allowed());


                                userServiceClient.saveUser(user,token);
                            }
                        }
                        return NseUtils.transactionResponse(HttpStatus.OK, "Saved Bank Details Successfully", new HashMap<>());

                    }else
                    {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "Failed"));
                    }

                }catch (Exception ex)
                {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), error_remark));
                }
            }
            catch (Exception ex)
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "Error fetching in Bank Details"));
            }

        }

    @Operation(
            summary = "Remove ACH Mandate Details",
            description = "Removes the ACH mandate information for the given IIN number and account details"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ACH mandate removed successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Required fields missing or invalid"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @PostMapping("/removeACHMandateDetails")
    public ResponseEntity<?> removeACHMandateDetails(
            HttpServletRequest request,
            @RequestParam(required = false) String iin_number,
            @RequestParam(required = false) String account_number,
            @RequestParam(required = false) String umrn_no,
            @RequestParam(required = false) String source,
            @RequestHeader("Authorization") String token) {

        try {

            String userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            System.out.println("User ID from token: " + userid);

            UserDto users = userServiceClient.getUserById(Integer.valueOf(userid), token);

            String client_name = users.getClient_name();

            iin_number = NseUtils.checkParem(iin_number);
            account_number = NseUtils.checkParem(account_number);
            umrn_no = NseUtils.checkParem(umrn_no);
            source = NseUtils.checkParem(source);

            if(StringHelper.isEmpty(userid) || StringHelper.isEmpty(iin_number) || StringHelper.isEmpty(account_number))
            {
                return NseUtils.commonResponse("All fields are mandatory" , HttpStatus.BAD_REQUEST);
            }

            UserDto user =  userServiceClient.getUserDetailsByID( client_name, Integer.valueOf(userid),token);
            String nse_iin = user.getNse_iin_number();
            String nse_ach_id = "";
            String nse_ach_amount = "";
            int nse_ach_approved = 0;
            String selected_name = "";

            if(!nse_iin.equalsIgnoreCase(iin_number))
            {
                UserBseNseDto nse = userServiceClient.getUserBseNseDetailsByIinNumber(client_name,iin_number,token );
                String pan = nse.getPan();
                String name = nse.getName();

                selected_name = name + " PAN:" + pan + " (" + userid + ")";
                String bank_account_number1 = nse.getBank_account_number1();
                String bank_account_number2 = nse.getBank_account_number2();
                String bank_account_number3 = nse.getBank_account_number3();
                String ach1 = nse.getNse_ach1();
                String ach2 = nse.getNse_ach2();
                String ach3 = nse.getNse_ach3();
                if(bank_account_number1 == null) {bank_account_number1 = "";}
                if(bank_account_number2 == null) {bank_account_number2 = "";}
                if(bank_account_number3 == null) {bank_account_number3 = "";}
                if(ach1 == null) {ach1 = "";}
                if(ach2 == null) {ach2 = "";}
                if(ach3 == null) {ach3 = "";}

                if(ach1.equalsIgnoreCase(umrn_no) && account_number.equalsIgnoreCase(bank_account_number1))
                {
                    nse_ach_id = nse.getNse_ach1();
                    nse_ach_amount = nse.getNse_ach_amount1();
                    nse_ach_approved = nse.getNse_ach_approved1();

                    nse.setNse_ach_flag1(0);
                    nse.setNse_ach1("");
                    nse.setNse_ach_approved1(0);
                    nse.setNse_ach_amount1("");
                    nse.setNse_ach_rej_reason1("");
                    nse.setNse_ach_created_date1(null);
                    userServiceClient.saveUserBseNseDetail(nse,token);

                }else if(ach2.equalsIgnoreCase(umrn_no) && account_number.equalsIgnoreCase(bank_account_number2))
                {
                    nse_ach_id = nse.getNse_ach2();
                    nse_ach_amount = nse.getNse_ach_amount2();
                    nse_ach_approved = nse.getNse_ach_approved2();

                    nse.setNse_ach_flag2(0);
                    nse.setNse_ach2("");
                    nse.setNse_ach_approved2(0);
                    nse.setNse_ach_amount2("");
                    nse.setNse_ach_rej_reason2("");
                    nse.setNse_ach_created_date2(null);
                    userServiceClient.saveUserBseNseDetail(nse,token);

                }else if(ach3.equalsIgnoreCase(umrn_no) && account_number.equalsIgnoreCase(bank_account_number3))
                {
                    nse_ach_id = nse.getNse_ach3();
                    nse_ach_amount = nse.getNse_ach_amount3();
                    nse_ach_approved = nse.getNse_ach_approved3();

                    nse.setNse_ach_flag3(0);
                    nse.setNse_ach3("");
                    nse.setNse_ach_approved3(0);
                    nse.setNse_ach_amount3("");
                    nse.setNse_ach_rej_reason3("");
                    nse.setNse_ach_created_date3(null);
                    userServiceClient.saveUserBseNseDetail(nse,token);

                }else {

                }
            }else{


                String pan = user.getPan();
                String name = user.getName();
                selected_name = name + " PAN:" + pan + " (" + userid + ")";

                String bank_account_number1 = user.getBank_account_number1();
                String bank_account_number2 = user.getBank_account_number2();
                String bank_account_number3 = user.getBank_account_number3();
                String ach1 = user.getNse_ach1();
                String ach2 = user.getNse_ach2();
                String ach3 = user.getNse_ach3();
                if(bank_account_number1 == null) {bank_account_number1 = "";}
                if(bank_account_number2 == null) {bank_account_number2 = "";}
                if(bank_account_number3 == null) {bank_account_number3 = "";}
                if(ach1 == null) {ach1 = "";}
                if(ach2 == null) {ach2 = "";}
                if(ach3 == null) {ach3 = "";}

                if(ach1.equalsIgnoreCase(umrn_no) && account_number.equalsIgnoreCase(bank_account_number1))
                {
                    nse_ach_id = user.getNse_ach1();
                    nse_ach_amount = user.getNse_ach_amount1();
                    nse_ach_approved = user.getNse_ach_approved1();

                    user.setNse_ach_flag1(0);
                    user.setNse_ach1("");
                    user.setNse_ach_approved1(0);
                    user.setNse_ach_amount1("");
                    user.setNse_ach_rej_reason1("");
                    user.setNse_ach_created_date1(null);
                    userServiceClient.saveUser(user,token);

                }else if(ach2.equalsIgnoreCase(umrn_no) && account_number.equalsIgnoreCase(bank_account_number2))
                {
                    nse_ach_id = user.getNse_ach2();
                    nse_ach_amount = user.getNse_ach_amount2();
                    nse_ach_approved = user.getNse_ach_approved2();

                    user.setNse_ach_flag2(0);
                    user.setNse_ach2("");
                    user.setNse_ach_approved2(0);
                    user.setNse_ach_amount2("");
                    user.setNse_ach_rej_reason2("");
                    user.setNse_ach_created_date2(null);
                    userServiceClient.saveUser(user,token);

                }else if(ach3.equalsIgnoreCase(umrn_no) && account_number.equalsIgnoreCase(bank_account_number3))
                {
                    nse_ach_id = user.getNse_ach3();
                    nse_ach_amount = user.getNse_ach_amount3();
                    nse_ach_approved = user.getNse_ach_approved3();

                    user.setNse_ach_flag3(0);
                    user.setNse_ach3("");
                    user.setNse_ach_approved3(0);
                    user.setNse_ach_amount3("");
                    user.setNse_ach_rej_reason3("");
                    user.setNse_ach_created_date3(null);
                    userServiceClient.saveUser(user,token);

                }else {

                }
            }
//            String ipAddr = NseUtils.getIpAddr(request);
//            if(ipAddr == null){ipAddr="";}
//
//            String investor = selected_name != null && StringHelper.isNotEmpty(selected_name) ? " for " + selected_name : "";
//            String logmsg = users.getFirst_name() +" did NSE ACH Mandate Remove "+investor+". Details:";
//            logmsg += "client_name: "+client_name+",";
//            logmsg += "userid: "+userid+",";
//            logmsg += "iin_number: "+iin_number+",";
//            logmsg += "bank account number: "+account_number+",";
//            logmsg += "umrn_no: "+umrn_no+",";
//            logmsg += "nse_ach_amount: "+nse_ach_amount+",";
//            logmsg += "nse_ach_approved: "+nse_ach_approved+",";
//            logmsg += "nse_ach: "+nse_ach_id;
//
//            NseLogModel log = new NseLogModel();
//            log.setUserid(users.getId());
//            log.setUsername(users.getFirst_name());
//            log.setMobile(users.getMobile());
//            log.setTitle("NSE ACH Mandate Remove");
//            log.setDescription("NSE ACH Mandate Remove");
//            log.setContent(logmsg);
//            log.setLogtime(new Date());
//            log.setIp(ipAddr);
//
//            if(source.equalsIgnoreCase("Mobile"))
//            {
//                log.setSource("Mobile App");
//            }else
//            {
//                log.setSource("Website");
//            }
//
//            log.setClientName(client_name);
//
//            nseLogRepository.save(log);


            return NseUtils.commonResponse("ACH Mandate deatails removed successfully.", HttpStatus.OK);

        } catch(Exception ex)
        {
            ex.printStackTrace();
            return NseUtils.commonResponse("Error ", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Delete Bank Details",
            description = "Delete Bank information for the given IIN number and account details"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ACH mandate removed successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Required fields missing or invalid"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @PostMapping("/deleteBankDetails")
    public ResponseEntity<?> deleteBankDetails(
            HttpServletRequest request,
            @RequestParam(required = false) String iin_number,
            @RequestParam(required = false) String bank_code,
            @RequestParam(required = false) String bank_account_number,
            @RequestParam(required = false) String bank_account_type,
            @RequestParam(required = false) String bank_branch,
            @RequestParam(required = false) String bank_ifsc_code,
            @RequestParam(required = false) String source,
            @RequestHeader("Authorization") String token) {

        try{
            String userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            System.out.println("User ID from token: " + userid);

            UserDto users = userServiceClient.getUserById(Integer.valueOf(userid), token);

            String client_name = users.getClient_name();
            String login_userid =userid;
            String login_name = users.getFirst_name();
            String login_mobile = users.getMobile();


            iin_number = NseUtils.checkParem(iin_number);
            bank_code = NseUtils.checkParem(bank_code);
            bank_account_number = NseUtils.checkParem(bank_account_number);
            bank_account_type = NseUtils.checkParem(bank_account_type);
            bank_branch = NseUtils.checkParem(bank_branch);
            bank_ifsc_code = NseUtils.checkParem(bank_ifsc_code);
            source = NseUtils.checkParem(source);


            UserDto user = userServiceClient.getUserDetailsByID( client_name,Integer.parseInt(userid),token);
            if (user == null) {
                return NseUtils.commonResponse("User Details Not Exits, Please Contact Admin!",HttpStatus.BAD_REQUEST);
            }

            String appln_id = "";
            String broker_code = "";
            String password = "";

            BseNseKeyDto nsekey = userServiceClient.getByClientName(client_name,token);
            appln_id = nsekey.getNse_appln_id();
            broker_code = nsekey.getBrokerCode();
            password = nsekey.getNse_password();

            String nse_iin = user.getNse_iin_number();
            String name = "";
            String pan = "";
            String selected_name = "";
            UserBseNseDto nse = null;

            if (!nse_iin.equalsIgnoreCase(iin_number)) {
                nse = userServiceClient.getUserBseNseDetailsByIinNumber(iin_number, client_name,token);
                pan = nse.getPan();
                name = nse.getName();
                selected_name = name + " (" + userid + ")";
            } else {
                pan = user.getPan();
                name = user.getName();
                selected_name = name + " (" + userid + ")";
            }

            JSONArray requestDetailsArray = new JSONArray();
            JSONObject requestDetails = new JSONObject();
            requestDetails.put("client_code", iin_number);
            requestDetails.put("action_type", "DEL");
            requestDetails.put("account_type", bank_account_type);
            requestDetails.put("account_no", bank_account_number);
            requestDetails.put("micr_no", "");
            requestDetails.put("ifsc_code", bank_ifsc_code);
            requestDetails.put("default_bank_flag", "N");
            requestDetailsArray.put(requestDetails);

            //Get NSE API Credentials
            BseNseOnlineAccessDto online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, broker_code,token);
            if(online_access == null)
            {
                return NseUtils.commonResponse("NSE Online Credentials Not available. Please contact your RM",HttpStatus.BAD_REQUEST);
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
            requestBody.put("bank_dtl", requestDetailsArray);
            System.out.println("requestBody = " + requestBody.toString());

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            String clientBankDetail_url = nseUrl + "/nsemfdesk/api/v2/registration/CLIENTBANKDTL";

            String status = "";
            String error_remark = "";

            ResponseEntity<String> mandateResult = RestTemplateFactory.createRestTemplate()
                    .postForEntity(clientBankDetail_url, entity, String.class);
            String statusCode = mandateResult.getStatusCode().toString();
            String responseBody = mandateResult.getBody().toString();

            System.out.println("statusCode = " + statusCode);
            System.out.println("responseBody = " + responseBody);

            JSONObject jsonObject = new JSONObject(responseBody);

            if (jsonObject.has("bank_dtl")) {
                JSONArray regDataArray = jsonObject.getJSONArray("bank_dtl");

                if (regDataArray.length() > 0) {
                    JSONObject regData = regDataArray.getJSONObject(0);

                    status = regData.optString("status");
                    error_remark = regData.optString("error_remark");
                }
            }
            System.out.println("status: " + status);
            System.out.println("error_remark: " + error_remark);
//
//            NseTransactions nsetrans = new NseTransactions();
//            nsetrans.setUrl(clientBankDetail_url);
//            nsetrans.setNse_request(requestBody.toString());
//            nsetrans.setNse_response(responseBody);
//            nsetrans.setReturn_msg(status);
//            nsetrans.setService_return_code(status);
//            nsetrans.setService_msg(error_remark);
//            nsetrans.setPan(pan);
//            nsetrans.setName(name);
//            nsetrans.setBranch(user.getBranch());
//            nsetrans.setRm_name(user.getRm_name());
//            nsetrans.setSubbroker_name(user.getSubbroker_name());
//            nsetrans.setClient_name(client_name);
//            nsetrans.setIin_number(iin_number);
//            nsetrans.setScheme_code("");
//            nsetrans.setScheme_name("");
//            nsetrans.setFolio_no("");
//            nsetrans.setAmount_units("");
//            nsetrans.setPurchase_type("");
//            nsetrans.setTransaction_type("DELETE Bank Details");
//            nsetrans.setTransaction_status(status);
//            nsetrans.setUnique_number("");
//            nsetrans.setTransaction_number("");
//            nsetrans.setPayment_link("");
//            nsetrans.setTo_scheme_code("");
//            nsetrans.setTo_scheme_name("");
//            nsetrans.setTransaction_date(new Date());
//            nsetrans.setUser_id(Integer.parseInt(userid));
//            nsetrans.setRegister_source("Website");
//            nsetrans.setBroker_code(broker_code);
//            nsetrans.setEuin_number("");
//            nseTransactionRepository.save(nsetrans);

//            System.out.println("status = " + status);

            if (status.equalsIgnoreCase("SUCCESS"))
            {
                System.out.println("nse_iin = " + nse_iin);
                System.out.println("iin_number = " + iin_number);

                if (!nse_iin.equalsIgnoreCase(iin_number))
                {
                    String acc_no1 = nse.getBank_account_number1();
                    String acc_no2 = nse.getBank_account_number2();
                    String acc_no3 = nse.getBank_account_number3();

                    if (acc_no1 == null) {
                        acc_no1 = "";
                    }
                    if (acc_no2 == null) {
                        acc_no2 = "";
                    }
                    if (acc_no3 == null) {
                        acc_no3 = "";
                    }

                    if (acc_no1.equalsIgnoreCase(bank_account_number))
                    {
                        if (StringHelper.isNotEmpty(acc_no3))
                        {
                            nse.setBank_account_holder_name1(nse.getBank_account_holder_name3());
                            nse.setBank_account_number1(nse.getBank_account_number3());
                            nse.setBank_name1(nse.getBank_name3());
                            nse.setBank_code1(nse.getBank_code3());
                            nse.setBank_account_type1(nse.getBank_account_type3());
                            nse.setBank_address1(nse.getBank_address3());
                            nse.setBank_branch1(nse.getBank_branch3());
                            nse.setBank_ifsc_code1(nse.getBank_ifsc_code3());
                            nse.setBank_micr_code1(nse.getBank_micr_code3());
                            nse.setDefault_bank1("");

                            nse.setNse_ach_flag1(nse.getNse_ach_flag3());
                            nse.setNse_ach1(nse.getNse_ach3());
                            nse.setNse_ach_amount1(nse.getNse_ach_amount3());
                            nse.setNse_ach_approved1(nse.getNse_ach_approved3());
                            nse.setNse_ach_rej_reason1(nse.getNse_ach_rej_reason3());
                            nse.setNse_ach_created_date1(nse.getNse_ach_created_date3());

                            nse.setBank_account_holder_name3("");
                            nse.setBank_account_number3("");
                            nse.setBank_name3("");
                            nse.setBank_code3("");
                            nse.setBank_account_type3("");
                            nse.setBank_address3("");
                            nse.setBank_branch3("");
                            nse.setBank_ifsc_code3("");
                            nse.setBank_micr_code3("");
                            nse.setDefault_bank3("");

                            nse.setNse_ach_flag3(0);
                            nse.setNse_ach3("");
                            nse.setNse_ach_amount3("");
                            nse.setNse_ach_approved3(0);
                            nse.setNse_ach_rej_reason3("");
                            nse.setNse_ach_created_date3(null);

                        } else if (StringHelper.isNotEmpty(acc_no2))
                        {
                            nse.setBank_account_holder_name1(nse.getBank_account_holder_name2());
                            nse.setBank_account_number1(nse.getBank_account_number2());
                            nse.setBank_name1(nse.getBank_name2());
                            nse.setBank_code1(nse.getBank_code2());
                            nse.setBank_account_type1(nse.getBank_account_type2());
                            nse.setBank_address1(nse.getBank_address2());
                            nse.setBank_branch1(nse.getBank_branch2());
                            nse.setBank_ifsc_code1(nse.getBank_ifsc_code2());
                            nse.setBank_micr_code1(nse.getBank_micr_code2());
                            nse.setDefault_bank1("");

                            nse.setNse_ach_flag1(nse.getNse_ach_flag2());
                            nse.setNse_ach1(nse.getNse_ach2());
                            nse.setNse_ach_amount1(nse.getNse_ach_amount2());
                            nse.setNse_ach_approved1(nse.getNse_ach_approved2());
                            nse.setNse_ach_rej_reason1(nse.getNse_ach_rej_reason2());
                            nse.setNse_ach_created_date1(nse.getNse_ach_created_date2());

                            nse.setBank_account_holder_name2("");
                            nse.setBank_account_number2("");
                            nse.setBank_name2("");
                            nse.setBank_code2("");
                            nse.setBank_account_type2("");
                            nse.setBank_address2("");
                            nse.setBank_branch2("");
                            nse.setBank_ifsc_code2("");
                            nse.setBank_micr_code2("");
                            nse.setDefault_bank2("");

                            nse.setNse_ach_flag2(0);
                            nse.setNse_ach2("");
                            nse.setNse_ach_amount2("");
                            nse.setNse_ach_approved2(0);
                            nse.setNse_ach_rej_reason2("");
                            nse.setNse_ach_created_date2(null);
                        }
                    }
                    if (acc_no2.equalsIgnoreCase(bank_account_number))
                    {
                        nse.setBank_account_holder_name2("");
                        nse.setBank_account_number2("");
                        nse.setBank_name2("");
                        nse.setBank_code2("");
                        nse.setBank_account_type2("");
                        nse.setBank_address2("");
                        nse.setBank_branch2("");
                        nse.setBank_ifsc_code2("");
                        nse.setBank_micr_code2("");
                        nse.setDefault_bank2("");

                        nse.setNse_ach_flag2(0);
                        nse.setNse_ach2("");
                        nse.setNse_ach_amount2("");
                        nse.setNse_ach_approved2(0);
                        nse.setNse_ach_rej_reason2("");
                        nse.setNse_ach_created_date2(null);
                    }
                    if (acc_no3.equalsIgnoreCase(bank_account_number)) {
                        nse.setBank_account_holder_name3("");
                        nse.setBank_account_number3("");
                        nse.setBank_name3("");
                        nse.setBank_code3("");
                        nse.setBank_account_type3("");
                        nse.setBank_address3("");
                        nse.setBank_branch3("");
                        nse.setBank_ifsc_code3("");
                        nse.setBank_micr_code3("");
                        nse.setDefault_bank3("");

                        nse.setNse_ach_flag3(0);
                        nse.setNse_ach3("");
                        nse.setNse_ach_amount3("");
                        nse.setNse_ach_approved3(0);
                        nse.setNse_ach_rej_reason3("");
                        nse.setNse_ach_created_date3(null);
                    }
                    userServiceClient.saveUserBseNseDetail(nse,token);
                } else
                {
                    String acc_no1 = user.getBank_account_number1();
                    String acc_no2 = user.getBank_account_number2();
                    String acc_no3 = user.getBank_account_number3();

                    if (acc_no1 == null) {
                        acc_no1 = "";
                    }
                    if (acc_no2 == null) {
                        acc_no2 = "";
                    }
                    if (acc_no3 == null) {
                        acc_no3 = "";
                    }

                    if (acc_no1.equalsIgnoreCase(bank_account_number)) {
                        if (StringHelper.isNotEmpty(acc_no3))
                        {
                            user.setBank_account_holder_name1(user.getBank_account_holder_name3());
                            user.setBank_account_number1(user.getBank_account_number3());
                            user.setBank_name1(user.getBank_name3());
                            user.setBank_code1(user.getBank_code3());
                            user.setBank_account_type1(user.getBank_account_type3());
                            user.setBank_address1(user.getBank_address3());
                            user.setBank_branch1(user.getBank_branch3());
                            user.setBank_ifsc_code1(user.getBank_ifsc_code3());
                            user.setBank_micr_code1(user.getBank_micr_code3());
                            user.setDefault_bank1("");

                            user.setNse_ach_flag1(user.getNse_ach_flag3());
                            user.setNse_ach1(user.getNse_ach3());
                            user.setNse_ach_amount1(nse.getNse_ach_amount3());
                            user.setNse_ach_approved1(user.getNse_ach_approved3());
                            user.setNse_ach_rej_reason1(user.getNse_ach_rej_reason3());
                            user.setNse_ach_created_date1(user.getNse_ach_created_date3());

                            user.setBank_account_holder_name3("");
                            user.setBank_account_number3("");
                            user.setBank_name3("");
                            user.setBank_code3("");
                            user.setBank_account_type3("");
                            user.setBank_address3("");
                            user.setBank_branch3("");
                            user.setBank_ifsc_code3("");
                            user.setBank_micr_code3("");
                            user.setDefault_bank3("");

                            user.setNse_ach_flag3(0);
                            user.setNse_ach3("");
                            user.setNse_ach_amount3("");
                            user.setNse_ach_approved3(0);
                            user.setNse_ach_rej_reason3("");
                            user.setNse_ach_created_date3(null);
                            user.set_purchase_allowed(true);
                            user.set_redeem_allowed(true);
                            user.set_switch_allowed(true);
                            user.set_stp_allowed(true);
                            user.set_stp_allowed(true);
                        }
                        else if (StringHelper.isNotEmpty(acc_no2))
                        {
                            user.setBank_account_holder_name1(user.getBank_account_holder_name2());
                            user.setBank_account_number1(user.getBank_account_number2());
                            user.setBank_name1(user.getBank_name2());
                            user.setBank_code1(user.getBank_code2());
                            user.setBank_account_type1(user.getBank_account_type2());
                            user.setBank_address1(user.getBank_address2());
                            user.setBank_branch1(user.getBank_branch2());
                            user.setBank_ifsc_code1(user.getBank_ifsc_code2());
                            user.setBank_micr_code1(user.getBank_micr_code2());
                            user.setDefault_bank1("");

                            user.setNse_ach_flag1(user.getNse_ach_flag2());
                            user.setNse_ach1(user.getNse_ach2());
                            user.setNse_ach_amount1(user.getNse_ach_amount2());
                            user.setNse_ach_approved1(user.getNse_ach_approved2());
                            user.setNse_ach_rej_reason1(user.getNse_ach_rej_reason2());
                            user.setNse_ach_created_date1(user.getNse_ach_created_date2());

                            user.setBank_account_holder_name2("");
                            user.setBank_account_number2("");
                            user.setBank_name2("");
                            user.setBank_code2("");
                            user.setBank_account_type2("");
                            user.setBank_address2("");
                            user.setBank_branch2("");
                            user.setBank_ifsc_code2("");
                            user.setBank_micr_code2("");
                            user.setDefault_bank2("");

                            user.setNse_ach_flag2(0);
                            user.setNse_ach2("");
                            user.setNse_ach_amount2("");
                            user.setNse_ach_approved2(0);
                            user.setNse_ach_rej_reason2("");
                            user.setNse_ach_created_date2(null);
                            user.set_purchase_allowed(true);
                            user.set_redeem_allowed(true);
                            user.set_switch_allowed(true);
                            user.set_stp_allowed(true);
                            user.set_stp_allowed(true);
                        }
                    }
                    if (acc_no2.equalsIgnoreCase(bank_account_number))
                    {
                        user.setBank_account_holder_name2("");
                        user.setBank_account_number2("");
                        user.setBank_name2("");
                        user.setBank_code2("");
                        user.setBank_account_type2("");
                        user.setBank_address2("");
                        user.setBank_branch2("");
                        user.setBank_ifsc_code2("");
                        user.setBank_micr_code2("");
                        user.setDefault_bank2("");

                        user.setNse_ach_flag2(0);
                        user.setNse_ach2("");
                        user.setNse_ach_amount2("");
                        user.setNse_ach_approved2(0);
                        user.setNse_ach_rej_reason2("");
                        user.setNse_ach_created_date2(null);
                        user.set_purchase_allowed(true);
                        user.set_redeem_allowed(true);
                        user.set_switch_allowed(true);
                        user.set_stp_allowed(true);
                        user.set_stp_allowed(true);
                    }
                    if (acc_no3.equalsIgnoreCase(bank_account_number))
                    {
                        user.setBank_account_holder_name3("");
                        user.setBank_account_number3("");
                        user.setBank_name3("");
                        user.setBank_code3("");
                        user.setBank_account_type3("");
                        user.setBank_address3("");
                        user.setBank_branch3("");
                        user.setBank_ifsc_code3("");
                        user.setBank_micr_code3("");
                        user.setDefault_bank3("");

                        user.setNse_ach_flag3(0);
                        user.setNse_ach3("");
                        user.setNse_ach_amount3("");
                        user.setNse_ach_approved3(0);
                        user.setNse_ach_rej_reason3("");
                        user.setNse_ach_created_date3(null);
                        user.set_purchase_allowed(true);
                        user.set_redeem_allowed(true);
                        user.set_switch_allowed(true);
                        user.set_stp_allowed(true);
                        user.set_stp_allowed(true);
                    }
                    userServiceClient.saveUser(user,token);
                }
                return NseUtils.commonResponse("Deleted Bank Details successfully!", HttpStatus.OK);
            }else{
                return NseUtils.commonResponse(error_remark, HttpStatus.BAD_REQUEST);
            }
        }catch(Exception ex){
            ex.printStackTrace();
            return NseUtils.commonResponse("Error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Upload Nse Details",
            description = "Upload NSE details"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ACH mandate removed successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Required fields missing or invalid"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @PostMapping("/uploadNseImage")
    public ResponseEntity<?> uploadNseImage(
            HttpServletRequest request,
            @RequestParam(required = false) String iin_no,
            @RequestParam(required = false) String mandate_id,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String base64EncodedFile,
            @RequestParam(required = false) String broker_code,
            @RequestParam(required = false) String source,
            @RequestHeader("Authorization") String token) {


        String img_type = "";
        String service_return_code = "";
        String service_msg = "";

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

        try {

            String userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            System.out.println("User ID from token: " + userid);

            UserDto users = userServiceClient.getUserById(Integer.valueOf(userid), token);

            String client_name = users.getClient_name();
            String login_userid =userid;
            String login_name = users.getFirst_name();
            String login_mobile = users.getMobile();

            iin_no = NseUtils.checkParem(iin_no);
            mandate_id = NseUtils.checkParem(mandate_id);
            fileName = NseUtils.checkParem(fileName);
            base64EncodedFile = NseUtils.checkParem(base64EncodedFile);
            source = NseUtils.checkParem(source);

            JSONObject scanReq = new JSONObject();
            scanReq.put("client_code", iin_no);
            scanReq.put("mandate_id", mandate_id);
            scanReq.put("file_name", fileName);
            scanReq.put("file_data", base64EncodedFile);

            BseNseOnlineAccessDto online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, broker_code,token);
            if(online_access == null)
            {
                return NseUtils.commonResponse("NSE Online Credentials Not available. Please contact your RM", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            String nse_userid = NseUtils.trimOrEmpty(online_access.getNse_userid());
            String nse_memberid = NseUtils.trimOrEmpty(online_access.getNse_memberid());
            String nse_secret_key = NseUtils.trimOrEmpty(online_access.getNse_secret_key());
            String nse_license_key = NseUtils.trimOrEmpty(online_access.getNse_license_key());

            String base64Encoded = AESEncryptionUtilV2.base64EncodedAuth(nse_secret_key, nse_license_key, nse_userid);
            System.out.println("requestBody: " + scanReq.toString());
            System.out.println("authorization: " + base64Encoded);

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

            HttpEntity<String> entity = new HttpEntity<>(scanReq.toString(), headers);

            String scanMandate_url= nseUrl+"/nsemfdesk/api/v2/fileupload/MANDATEIMG";

            try
            {


                ResponseEntity<String> ScanMandateResult = restTemplate.postForEntity(scanMandate_url, entity, String.class);
                String statusCode = ScanMandateResult.getStatusCode().toString();
                String responseBody = ScanMandateResult.getBody();

                System.out.println("statusCode = " + statusCode);
                System.out.println("responseBody = " + responseBody);

                JSONObject jsonObject = new JSONObject(responseBody);


                String scanMandate_status = jsonObject.getString("status");
                String scanMandate_status_message = jsonObject.getString("message");

                NseTransactions nsetrans = new NseTransactions();
                nsetrans.setUrl(scanMandate_url);
                nsetrans.setNse_request(responseBody.toString());
                nsetrans.setNse_response(responseBody);
                nsetrans.setReturn_msg(scanMandate_status_message);
                nsetrans.setService_return_code(scanMandate_status);
                nsetrans.setService_msg(scanMandate_status_message);
                nsetrans.setReg_id("");
                nsetrans.setPayment_link("");
                nsetrans.setPan(users.getPan());
                nsetrans.setName(users.getFirst_name());
                nsetrans.setBranch(users.getBranch());
                nsetrans.setRm_name(users.getRm_name());
                nsetrans.setSubbroker_name(users.getSubbroker_name());
                nsetrans.setClient_name(client_name);
                nsetrans.setIin_number(iin_no);
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
                nsetrans.setTransaction_type("SCAN MANDATE");
                nsetrans.setTransaction_status("");
                nsetrans.setPayment_status("");
                nsetrans.setActive_ceased_status("");
                nsetrans.setRemarks(scanMandate_status_message);
                nsetrans.setMandate_id(mandate_id);
                nsetrans.setMandate_status("");
                nsetrans.setEmandate_auth_flag("");
                nsetrans.setApp_received_flag("");
                nsetrans.setTransaction_date(new Date());
                nsetrans.setUser_id(Integer.parseInt(userid));
                nsetrans.setRegister_source("Website");
                nsetrans.setBroker_code(broker_code);
                nsetrans.setEuin_number(users.getEuin());
                nsetrans.setCc_received("");
                nsetrans.setFund_trans_to_amc("");
                nsetrans.setRefund_status("");
                nsetrans.setRefund_amount("");
                nseTransactionRepository.save(nsetrans);

                if(scanMandate_status.equalsIgnoreCase("100"))
                {
                    return NseUtils.commonResponse(scanMandate_status_message,HttpStatus.OK);
                }
                else
                {
                    return NseUtils.commonResponse(scanMandate_status_message, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
            }

            if(service_return_code.equalsIgnoreCase("0"))
            {
                UsersNseRegReportDto reg = userServiceClient.getUserNseRegDetails(iin_no, client_name,token);
                if(reg != null)
                {
                    if(img_type.equalsIgnoreCase("IP")){
                        reg.setForm_updated_date(sdf.format(new Date()));
                    }

                    reg.setIin_status("Document uploaded. Waiting for IIN Activation");
                    userServiceClient.saveNseRegReport(reg,token);

                }

                return NseUtils.commonResponse("Document uploaded", HttpStatus.OK);
            }else
            {
                return NseUtils.commonResponse(service_msg, HttpStatus.INTERNAL_SERVER_ERROR);
            }

        }catch(Exception ex){
            ex.printStackTrace();
            return NseUtils.commonResponse("Error",HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    }