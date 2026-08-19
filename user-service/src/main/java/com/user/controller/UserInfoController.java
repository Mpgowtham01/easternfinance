package com.user.controller;


import com.mashape.unirest.http.exceptions.UnirestException;
import com.user.config.TokenInterceptor;
import com.user.model.*;
import com.user.pojo.InvestorClientCodePojo;
import com.user.pojo.MandateDetailsPojo;
import com.user.repository.*;
import com.user.response.IfscCodeResponse;
import com.user.response.InvestorClientCodeResponse;
import com.user.utils.UserUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.hibernate.internal.util.StringHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor

@Tag(
        name = "User Info Controller",
        description = "APIs related to User Information"
)
public class UserInfoController {


    @Autowired
    UserBseNseDetailsRespository userBseNseDetailsRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UsersPortfolioSchemewiseRepository usersPortfolioSchemewiseRepository;

    @Autowired
    BseNseKeyRepository bseNseKeyRepository;

    @Autowired
    UsersMandateDetailsRespository  UsersMandateDetailsRepository;

    @Autowired
    UserOnlineRegDetailsRespository userOnlineRegDetailsRespository;

    @Autowired
    UsersBankDetailsRepository usersBankDetailsRepository;


    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${vendor.logo.url}")
    private String vendorLogoPath;

    @Operation(
            summary = "Get User by ID",
            description = "Retrieves User Details available in the repository.\n\n" +
                    "Performs a distinct filter on the AMC code list before returning.\n\n" +
                    "Returns a list of strings or an error message on failure.\n" +
                    "\n"
    )

    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @GetMapping("/getUserDetailsByUserId")
    public ResponseEntity<?> getUserById(@RequestHeader("Authorization") String token) {
        try
        {

            String userId = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            List<UsersOnlineRegDetails> useOptional = userOnlineRegDetailsRespository.findNseUserByUserId(Integer.valueOf(userId));

            if (!useOptional.isEmpty()) {
                return ResponseEntity.ok(useOptional.get(0));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", HttpStatus.BAD_REQUEST, "status_msg", "User not found"));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR, "status_msg", "Error occurred while fetching user"));
        }
    }

    @Operation(
            summary = "Get User Details by UserBseNseDetials",
            description = "Retrieves User Details available in the repository.\n\n" +
                    "Performs a distinct filter on the AMC code list before returning.\n\n" +
                    "Returns a list of strings or an error message on failure.\n" +
                    "\n"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User Details",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = UsersOnlineRegDetails.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request â€“ Required parameters are missing or invalid",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "not found"))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @GetMapping("/getNseActiveUserBseNseDetailsByUserId")
    public ResponseEntity<?> getUserByIdAndClientNameAndNseActive(@RequestHeader("Authorization") String token, @RequestParam String clientName) {
        try {

            String userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);

            List<UsersOnlineRegDetails> key = userOnlineRegDetailsRespository.findUserByIdAndClientNameAndNseActives(Integer.valueOf(userid), clientName);
            if (key != null && key.size() > 0) {

                return ResponseEntity.ok(key);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", 404, "status_msg", "User not found"));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", 500, "status_msg", "Error occurred while fetching user details"));
        }
    }

    @Operation(
            summary = "Get Broker Code List by User Details",
            description = "Retrieves a list of distinct broker codes associated with a user.\n\n" +
                    "Filters based on optional parameters: client name, user ID, and folio number.\n\n" +
                    "Returns a list of broker codes (e.g., ARN codes) as strings."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved list of broker codes",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(
                                    schema = @Schema(type = "string", example = "ARN-77441")
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request â€“ Required parameters are missing or invalid",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "string", example = "Bad Request: Missing user ID")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error â€“ Something went wrong on the server side",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "string", example = "Error fetching SIP frequencies")
                    )
            )
    })
    @GetMapping("/getFolioBrokercode")
    public ResponseEntity<?> getFolioBrokercode(@RequestParam(required = false) String client_name,
                                                @RequestParam(required = false) Integer userid,
                                                @RequestParam(required = false) String folio) {
        try {
            if (client_name == null) {
                client_name = "";
            }
            ;
            if (folio == null) {
                folio = "";
            }
            ;

            List<String> frequencies = usersPortfolioSchemewiseRepository.findDistinctBrokerCodeByUserIdAndClientNameAndFolioNo(userid, client_name, folio);

            return ResponseEntity.ok(frequencies);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching SIP frequencies");
        }
    }

    @Operation(
            summary = "Get BSE/NSE Key by Client Name",
            description = "Retrieves the BSE/NSE key associated with the given client name. Returns the key object if found, otherwise returns an error message."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Key found for the given client name",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = BseNseKey.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Key not found for the client name",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "{\"status\": 404, \"status_msg\": \"Key not found\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error - while fetching key",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "{\"status\": 500, \"status_msg\": \"Error occurred while fetching key\"}")
                    )
            )
    })
    @GetMapping("/getClientNameByBseNseKey")
    public ResponseEntity<?> getClientNameByBseNseKey(@RequestParam String clientName)
    {
        try
        {
            BseNseKey key = bseNseKeyRepository.findByClientName(clientName);
            if (key != null)
            {
                return ResponseEntity.ok(key);
            } else
            {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", 404, "status_msg", "Key not found"));
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", 500, "status_msg", "Error occurred while fetching key"));
        }
    }

    @Operation(
            summary = "Get User Details by IIN Number and Client Name",
            description = "Fetches user BSE/NSE details based on the provided IIN number and client name"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User details fetched successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UsersOnlineRegDetails.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "{\"status\": 404, \"status_msg\": \"Key not found\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "{\"status\": 500, \"status_msg\": \"Error occurred while fetching key\"}")
                    )
            )
    })
    @GetMapping("/getUserDetialsByIinNumberAndClientName")
    public ResponseEntity<?> getUserDetialsByIinNumberAndClientName(@RequestParam String iin_number,@RequestParam String clientName)
    {
        try
        {
           Optional<UsersOnlineRegDetails> key = userOnlineRegDetailsRespository.findNseByIinNumberAndClientName(iin_number,clientName);
            if (key.isPresent())
            {
                return ResponseEntity.ok(key);
            } else
            {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", 404, "status_msg", "Key not found"));
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", 500, "status_msg", "Error occurred while fetching key"));
        }
    }

    @Operation(summary = "getUsersMandateDetailsByIIN", description = "Fetches user BSE/NSE details based on the provided IIN number and client name")
    @ApiResponses(value =
            {
                    @ApiResponse(responseCode = "200", description = "User details fetched successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UsersOnlineRegDetails.class))),
                    @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(example = "{\"status\": 404, \"status_msg\": \"Key not found\"}"))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(example = "{\"status\": 500, \"status_msg\": \"Error occurred while fetching key\"}")))
            })
    @GetMapping("/getMandateInfo")
    public ResponseEntity<?> getMandateInfo(
            @RequestHeader("Authorization") String token,
            @RequestParam String investor_code,
            @RequestParam(required = false) String account_number,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String mandate_flag)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
        try
        {
            String userId = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            String client_name = TokenInterceptor.extractClientNamedFromToken(token, secretKey);

            List<UsersOnlineRegDetails> regList = userOnlineRegDetailsRespository
                    .findByUserIdAndBseClientCodeAndClientName(Integer.valueOf(userId), investor_code, client_name);

            if (regList == null || regList.isEmpty())
            {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", 404, "status_msg", "User not found"));
            }

            UsersOnlineRegDetails reg = regList.get(0);

            Integer nse_customer = reg.getNse_customer();
            Integer nse_active = reg.getNse_active();

            if (nse_customer == null || nse_active == null || !nse_customer.equals(1) || !nse_active.equals(1))
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("status", 400, "status_msg", "NSE account is not active"));
            }

            Integer online_id = reg.getId();

            List<UsersBankDetails> bank_list = usersBankDetailsRepository.findByOnlineId(online_id);
            List<UsersMandateDetails> registered_mandate_list = UsersMandateDetailsRepository.findByOnlineId(online_id);

            if (bank_list == null)
            {
                bank_list = new ArrayList<UsersBankDetails>();
            }
            if (registered_mandate_list == null)
            {
                registered_mandate_list = new ArrayList<UsersMandateDetails>();
            }

            boolean include_pending = mandate_flag != null && mandate_flag.equalsIgnoreCase("Y");

            List<MandateDetailsPojo> mandate_list = new ArrayList<MandateDetailsPojo>();

            for (UsersBankDetails bank : bank_list)
            {
                String bank_account_number = nvl(bank.getBank_account_number());

                if (bank_account_number.isEmpty())
                {
                    continue;
                }

                List<UsersMandateDetails> bank_mandate_list = new ArrayList<UsersMandateDetails>();

                for (UsersMandateDetails mandate : registered_mandate_list)
                {
                    if (bank_account_number.equalsIgnoreCase(nvl(mandate.getBank_account_number())))
                    {
                        bank_mandate_list.add(mandate);
                    }
                }

                if (bank_mandate_list.isEmpty())
                {
                    if (include_pending)
                    {
                        mandate_list.add(buildMandateDetails(bank, null, sdf));
                    }
                    continue;
                }

                for (UsersMandateDetails mandate : bank_mandate_list)
                {
                    MandateDetailsPojo pojo = buildMandateDetails(bank, mandate, sdf);

                    if (include_pending || "Approved".equals(pojo.getMandate_status()))
                    {
                        mandate_list.add(pojo);
                    }
                }
            }

            if (account_number != null && !account_number.isEmpty())
            {
                mandate_list = mandate_list.stream()
                        .filter(mandate -> mandate.getBank_account_number().equalsIgnoreCase(account_number))
                        .collect(Collectors.toList());
            }

            return ResponseEntity.ok(mandate_list);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", 500, "status_msg", "Error occurred while fetching key"));
        }
    }

    /**
     * Builds one mandate row out of a users_bank_details record and the users_mandate_details
     * record registered against it. A null mandate means the bank has no mandate yet.
     */
    private MandateDetailsPojo buildMandateDetails(UsersBankDetails bank, UsersMandateDetails mandate, SimpleDateFormat sdf) throws UnirestException
    {
        String bank_name = nvl(bank.getBank_name());
        String bank_ifsc_code = nvl(bank.getBank_ifsc_code());
        String bank_micr_code = nvl(bank.getBank_micr_code());

        if (bank_micr_code.isEmpty() && !bank_name.isEmpty() && !bank_ifsc_code.isEmpty())
        {
            IfscCodeResponse apiResponse = UserUtils.getBankDetailsByIfsc(bank_ifsc_code, bank_name);

            if (apiResponse != null && apiResponse.getStatus() == 200 && apiResponse.getResult() != null)
            {
                bank_micr_code = nvl(apiResponse.getResult().getMicr_code());
            }
        }

        String default_bank = nvl(bank.getDefault_bank());
        if (default_bank.isEmpty())
        {
            default_bank = "N";
        }

        Integer nse_ach_flag = mandate == null || mandate.getNse_ach_flag() == null ? 0 : mandate.getNse_ach_flag();
        Integer nse_ach_approved = mandate == null || mandate.getNse_ach_approved() == null ? 0 : mandate.getNse_ach_approved();
        String nse_ach = mandate == null ? "" : nvl(mandate.getNse_ach());
        String nse_ach_amount = mandate == null ? "" : nvl(mandate.getNse_ach_amount());
        String nse_umrn_no = mandate == null ? "" : nvl(mandate.getNse_umrn_no());
        Date mandate_date = mandate == null ? null : mandate.getNse_ach_created_date();
        Date mandate_end_date = mandate == null ? null : mandate.getNse_ach_end_date();

        String mandate_status = resolveMandateStatus(nse_ach_flag, nse_ach, nse_ach_approved);

        MandateDetailsPojo pojo = new MandateDetailsPojo();
        pojo.setBank_name(bank_name);
        pojo.setBank_account_number(nvl(bank.getBank_account_number()));
        pojo.setBank_account_holder_name(nvl(bank.getBank_account_holder_name()));
        pojo.setBank_ifsc_code(bank_ifsc_code);
        pojo.setBank_micr_code(bank_micr_code);
        pojo.setBank_code(nvl(bank.getBank_code()));
        pojo.setBank_branch(nvl(bank.getBank_branch()));
        pojo.setAccount_type(nvl(bank.getBank_account_type()));
        pojo.setDefault_bank(default_bank);
        pojo.setMandate_type("ACH Mandate");
        pojo.setMandate_flag(nse_ach_flag);
        pojo.setMandate_id(nse_ach);
        pojo.setMandate_amount(nse_ach_amount);
        pojo.setMandate_approved(nse_ach_approved);
        pojo.setMmrn_number(nse_umrn_no);
        pojo.setMandate_status(mandate_status);
        pojo.setMandate_desc(resolveMandateDesc(mandate_status));
        pojo.setMandate_date(mandate_date == null ? "" : sdf.format(mandate_date));
        pojo.setMandate_end_date(mandate_end_date == null ? "" : sdf.format(mandate_end_date));

        return pojo;
    }

    private String resolveMandateStatus(Integer nse_ach_flag, String nse_ach, Integer nse_ach_approved)
    {
        if (nse_ach_flag.equals(0) && nse_ach.isEmpty() && nse_ach_approved.equals(0))
        {
            return "Generate";
        }
        if (nse_ach_flag.equals(1) && nse_ach_approved.equals(0))
        {
            return "Pending";
        }
        if (nse_ach_flag.equals(1) && !nse_ach.isEmpty() && nse_ach_approved.equals(1))
        {
            return "Approved";
        }
        return "";
    }

    private String resolveMandateDesc(String mandate_status)
    {
        if ("Generate".equals(mandate_status))
        {
            return "Mandate not generated.";
        }
        if ("Pending".equals(mandate_status))
        {
            return "Mandate generated & not approved.";
        }
        if ("Approved".equals(mandate_status))
        {
            return "Mandate generated & approved.";
        }
        return "";
    }

    private String nvl(String value)
    {
        return value == null ? "" : value;
    }

    @GetMapping("/getInvestorCode")
    public ResponseEntity<?> getInvestorCode(
        @RequestHeader("Authorization") String token)
    {
        System.out.println("--------------------------------------------------------------");
        System.out.println("date " + new Date());
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
        try {
            String userId = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            System.out.println("userId = " + userId);

            Optional<UsersOnlineRegDetails> userOptional = userOnlineRegDetailsRespository.findUSerByIdAndActive(Integer.valueOf(userId));
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("status", HttpStatus.BAD_REQUEST, "status_msg", "User not found"));
            }
            UsersOnlineRegDetails user = userOptional.get();
            String client_name = user.getClient_name();

            System.out.println("userList = " + user);

            Integer nse_active = user.getNse_active();
            String tax_status = user.getTax_status();
            String tax_status_code = user.getTax_status_code();
            String holding_nature = user.getHolding_nature();
            String holding_nature_code = user.getHolding_nature_code();
            String broker_code = user.getBroker_code();
            String inv_name = user.getName();

            String investor_code = "";
            String path = "";

            List<String> investorCodeArray = new ArrayList<String>();
            List<String> pathArray = new ArrayList<String>();
            List<String> vendorArray = new ArrayList<String>();

            investor_code = user.getNse_iin_number();
            path = UserUtils.getVendorImage("NSE");

            investorCodeArray.add(investor_code);
            pathArray.add(vendorLogoPath + path);
            vendorArray.add("NSE");

            List<InvestorClientCodePojo> list = new ArrayList<InvestorClientCodePojo>();
            InvestorClientCodePojo code = null;

            if(!investorCodeArray.isEmpty())
            {
                for (int i = 0; i < investorCodeArray.size(); i++)
                {
                    String invCode = investorCodeArray.get(i);
                    String pathCode = pathArray.get(i);
                    String vendorCode = vendorArray.get(i);

                    code = new InvestorClientCodePojo();
                    code.setInv_name(inv_name);
                    code.setTax_status(tax_status);
                    code.setTax_status_code(tax_status_code);
                    code.setHolding_nature(holding_nature);
                    code.setHolding_nature_code(holding_nature_code);
                    code.setBroker_code(broker_code);
                    code.setInvestor_code(invCode);
                    code.setLogo(pathCode);
                    code.setBse_nse_mfu_flag(vendorCode);
                    list.add(code);
                }
            }

            List<UsersOnlineRegDetails> user_list = userOnlineRegDetailsRespository.findUserByIdAndClientNameAndNseActives(Integer.parseInt(userId), client_name);
            if(user_list != null && user_list.size() > 0) {
                for (UsersOnlineRegDetails nse : user_list) {

                    investorCodeArray = new ArrayList<String>();
                    pathArray = new ArrayList<String>();
                    vendorArray = new ArrayList<String>();

                    inv_name = nse.getName();
                    nse_active = nse.getNse_active();
                    tax_status = nse.getTax_status();
                    tax_status_code = nse.getTax_status_code();
                    holding_nature = nse.getHolding_nature();
                    holding_nature_code = nse.getHolding_nature_code();
                    broker_code = nse.getBroker_code();
                    path = "";

                    if(nse_active.equals(1))
                    {
                        investor_code = nse.getNse_iin_number();
                        path = UserUtils.getVendorImage("NSE");

                        investorCodeArray.add(investor_code);
                        pathArray.add(vendorLogoPath + path);
                        vendorArray.add("NSE");
                    }


                    if(investorCodeArray.size() > 0)
                    {
                        for (int i = 0; i < investorCodeArray.size(); i++)
                        {
                            String invCode = investorCodeArray.get(i);
                            String pathCode = pathArray.get(i);
                            String vendorCode = vendorArray.get(i);

                            code = new InvestorClientCodePojo();
                            code.setInv_name(inv_name);
                            code.setTax_status(tax_status);
                            code.setTax_status_code(tax_status_code);
                            code.setHolding_nature(holding_nature);
                            code.setHolding_nature_code(holding_nature_code);
                            code.setBroker_code(broker_code);
                            code.setInvestor_code(invCode);
                            code.setLogo(pathCode);
                            code.setBse_nse_mfu_flag(vendorCode);
                            list.add(code);
                        }
                    }
                }

            }
            InvestorClientCodeResponse apiResponse = new InvestorClientCodeResponse();
            apiResponse.setStatus(200);
            apiResponse.setStatus_msg("Sucess");
            apiResponse.setMsg("");
            apiResponse.setClient_code_list(list);
            System.out.println("--------------END---------");

            return new ResponseEntity<InvestorClientCodeResponse>(apiResponse, HttpStatus.OK);


        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", 500, "status_msg", "Error occurred while fetching key"));
        }
    }


}
