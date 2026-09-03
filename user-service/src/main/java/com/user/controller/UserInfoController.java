package com.user.controller;


import com.mashape.unirest.http.exceptions.UnirestException;
import com.user.config.TokenInterceptor;
import com.user.dto.UserDto;
import com.user.mapper.UserMapper;
import com.user.model.*;
import com.user.pojo.CommonPojo;
import com.user.pojo.InvestorClientCodePojo;
import com.user.pojo.MandateDetailsPojo;
import com.user.repository.*;
import com.user.response.IfscCodeResponse;
import com.user.response.InvestorClientCodeResponse;
import com.user.response.StatusMessage;
import com.user.response.TransactionCommonResponse;
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
public class UserInfoController
{
    @Autowired
    UserRepository userRepository;

    @Autowired
    UsersPortfolioSchemewiseRepository usersPortfolioSchemewiseRepository;

    @Autowired
    BseNseKeyRepository bseNseKeyRepository;

    @Autowired
    UsersMandateDetailsRespository  usersMandateDetailsRepository;

    @Autowired
    UserOnlineRegDetailsRespository userOnlineRegDetailsRespository;

    @Autowired
    UsersBankDetailsRepository usersBankDetailsRepository;


    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${vendor.logo.url}")
    private String vendorLogoPath;
    @Autowired
    private UsersNomineeDetailsRepository usersNomineeDetailsRepository;

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
    public ResponseEntity<?> getFolioBrokercode(@RequestHeader("Authorization") String token,@RequestParam(required = false) String folio) {
        try
        {
            String userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            String client_name = TokenInterceptor.extractClientNamedFromToken(token, secretKey);
            List<String> frequencies = usersPortfolioSchemewiseRepository.findDistinctBrokerCodeByUserIdAndClientNameAndFolioNo(Integer.valueOf(userid), client_name, folio);

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
            List<UsersMandateDetails> registered_mandate_list = usersMandateDetailsRepository.findByOnlineId(online_id);

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
                        .filter(mandate -> account_number.equalsIgnoreCase(mandate.getBank_account_number()))
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

    @GetMapping("/getUserBseNseDetailsByUserID")
    public ResponseEntity<?> getUserBseNseDetailsByUserID(@RequestParam Integer userid, @RequestParam String clientName,@RequestParam(required = false) String online_flag) {
        try {

            online_flag = UserUtils.checkParem(online_flag);
            if(StringHelper.isEmpty(online_flag)) {online_flag = "NSE";}

            Optional<User> users = userRepository.findById(userid);

            List<UsersOnlineRegDetails> userDetailsList = null;
            if(online_flag.equalsIgnoreCase("BSE"))
            {
                userDetailsList = userOnlineRegDetailsRespository.getUserBseNseDetailsByUserIDAndOnlineFlagBse(userid, clientName,online_flag);
            }else
            {
                userDetailsList = userOnlineRegDetailsRespository.getUserBseNseDetailsByUserIDAndOnlineFlag(userid, clientName,online_flag);
            }

            if (userDetailsList.isEmpty())
            {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", 404, "status_msg", "User not found"));
            }

            List<UserDto> userDtos = new ArrayList<>();

            for (UsersOnlineRegDetails userDetails : userDetailsList) {
                List<UsersBankDetails> bankDetails =
                        usersBankDetailsRepository.findByUseridAndClientNameAndOnlineFlag(
                                userDetails.getUser_id(), clientName, String.valueOf(userDetails.getId()),online_flag);

                Optional<UsersNomineeDetails> nomineeDetails =
                        usersNomineeDetailsRepository.findByUseridAndClientName(
                                userDetails.getUser_id(), clientName, String.valueOf(userDetails.getId()), online_flag);

                List<UsersMandateDetails> mandateDetails =
                        usersMandateDetailsRepository.findByUseridAndClientNameAndOnlineFlag(
                                userDetails.getUser_id(), userDetails.getClient_name(), String.valueOf(userDetails.getId()),online_flag);

                UserDto userDto = UserMapper.mapToUserDtoMapper(
                        userDetails,
                        bankDetails,
                        mandateDetails,
                        nomineeDetails.orElse(new UsersNomineeDetails())
                );

                if (users.isPresent()) {
                    User user = users.get();

                    userDto.setSubbroker_name(user.getSubbroker_name());
                    userDto.setRm_name(user.getRm_name());
                    userDto.setSuper_subbroker_name(user.getSuper_subbroker_name());
                    userDto.setBranch(user.getBranch());
                }
                userDtos.add(userDto);
            }

            return ResponseEntity.ok(userDtos);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", 500, "status_msg", "Error occurred while fetching key"));
        }
    }

    @GetMapping("/getUsersBankDetailsByIIN")
    public ResponseEntity<?> getUsersBankDetailsByIIN(@RequestParam String onlineCode,@RequestParam String clientName,@RequestParam(required = false) String online_flag,
                                                      @RequestParam(required = false) String broker_code)
    {
        try
        {
            broker_code = UserUtils.checkParem(broker_code);
            online_flag = UserUtils.checkParem(online_flag);
            onlineCode = UserUtils.checkParem(onlineCode);
            clientName = UserUtils.checkParem(clientName);

            List<UsersBankDetails> key = null;
            if(broker_code.isEmpty())
            {
                key = usersBankDetailsRepository.findByonlineidAndClientName(online_flag,clientName,onlineCode);
            }else{
                key = usersBankDetailsRepository.findByonlineidAndClientNameAndBrokerCode(online_flag,clientName,onlineCode,broker_code);
            }

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

    @GetMapping("/getOnlineAccessValue")
    public ResponseEntity<?> getByClientName(@RequestParam String clientName,@RequestParam String brokercode) {
        try {
            BseNseKey response = bseNseKeyRepository.findByClientNameAndBrokerCode(clientName,brokercode);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", 500, "status_msg", "Error occurred while fetching key"));
        }
    }

    @GetMapping("/getUsersMandateDetailsByOnlineCode")
    public ResponseEntity<?> getUsersMandateDetailsByOnlineCode(
            @RequestHeader("Authorization") String token,
            @RequestParam String onlineCode,
            @RequestParam String broker_code,
            @RequestParam(required = false) String account_number,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String mandate_flag,
            @RequestParam(required = false) String bse_nse_mfu_flag)
    {
        try
        {

            String userId = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            String clientName = TokenInterceptor.extractClientNamedFromToken(token, secretKey);

            Optional<UsersOnlineRegDetails> userOptional = userOnlineRegDetailsRespository.findUSerByIdAndActive(Integer.valueOf(userId));
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("status", HttpStatus.BAD_REQUEST, "status_msg", "User not found"));
            }
            UsersOnlineRegDetails user = userOptional.get();

            source = UserUtils.checkParameter(source);
            mandate_flag = UserUtils.checkParameter(mandate_flag);

            if(StringHelper.isEmpty(mandate_flag)) {mandate_flag = "N";}
            if(StringHelper.isEmpty(source)) {mandate_flag = "Website";}

//            System.out.println("USER ID = " + user.getUser_id());
//            System.out.println("USER onlineCode = " + onlineCode);
//            System.out.println("USER clientName = " + clientName);
//            System.out.println("USER broker_code = " + broker_code);
//            System.out.println("USER bse_nse_mfu_flag = " + bse_nse_mfu_flag);

            List<UsersMandateDetails> mandateList = usersMandateDetailsRepository.findMandateDetailsAch(user.getUser_id(),clientName,onlineCode,bse_nse_mfu_flag,broker_code);

            System.out.println("masterList = " + mandateList.size());

            if (mandateList != null && mandateList.size() > 0)
            {
                if(source.equalsIgnoreCase("Mobile"))
                {
                    List<MandateDetailsPojo> mandate_list = new ArrayList<>();

                    List<UsersBankDetails> bankList = usersBankDetailsRepository.findBankDetails(user.getUser_id(),clientName,onlineCode,bse_nse_mfu_flag,broker_code);

                    for(UsersMandateDetails mandateDetails : mandateList)
                    {
                        UsersBankDetails bankDetails = bankList.stream().filter(bank -> bank.getBank_account_number().equals(mandateDetails.getBank_account_number())).findFirst().orElse(null);

                        if(bankDetails != null)
                        {
                            String mandate_status = "";
                            String mandate_desc = "";

                            if(mandate_flag.equalsIgnoreCase("Y"))
                            {
                                MandateDetailsPojo mandate = new MandateDetailsPojo();
                                mandate.setBank_name(bankDetails.getBank_name());
                                mandate.setBank_account_number(bankDetails.getBank_account_number());
                                mandate.setBank_ifsc_code(bankDetails.getBank_ifsc_code());
                                mandate.setBank_micr_code(bankDetails.getBank_micr_code());
                                mandate.setMandate_type("MANDATE");
                                mandate.setMandate_flag(mandateDetails.getNse_ach_flag());
                                mandate.setMandate_id(mandateDetails.getNse_ach());
                                mandate.setMandate_amount(mandateDetails.getNse_ach_amount());
                                mandate.setMandate_approved(mandateDetails.getNse_ach_approved());
                                mandate.setAccount_type(bankDetails.getBank_account_type());
                                if(mandateDetails.getNse_ach_flag().equals(0) && mandateDetails.getNse_ach().isEmpty() && mandateDetails.getNse_ach_approved().equals(0))
                                {
                                    mandate_status = "Generate";
                                    mandate_desc = "Mandate not generated.";
                                }else if(mandateDetails.getNse_ach_flag().equals(1) && mandateDetails.getNse_ach().isEmpty() && mandateDetails.getNse_ach_approved().equals(0))
                                {
                                    mandate_status = "Pending";
                                    mandate_desc = "Mandate generated & not approved.";
                                }else if(mandateDetails.getNse_ach_flag().equals(1) && !mandateDetails.getNse_ach().isEmpty() && mandateDetails.getNse_ach_approved().equals(0))
                                {
                                    mandate_status = "Pending";
                                    mandate_desc = "Mandate generated & not approved.";
                                }else if(mandateDetails.getNse_ach_flag().equals(1) && !mandateDetails.getNse_ach().isEmpty() && mandateDetails.getNse_ach_approved().equals(1))
                                {
                                    mandate_status = "Approved";
                                    mandate_desc = "Mandate generated & approved.";
                                }else
                                {
                                    mandate_status = "";
                                    mandate_desc = "";
                                }

                                SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
                                Date mandate_date = mandateDetails.getNse_ach_created_date();
                                String mandate_date_str = "";
                                if(mandate_date != null)
                                {
                                    mandate_date_str = sdf.format(mandate_date);
                                }

                                mandate.setMandate_desc(mandate_desc);
                                mandate.setMandate_date(mandate_date_str);
                                mandate.setMandate_status(mandate_status);
                                mandate.setBank_account_holder_name(bankDetails.getBank_account_holder_name());
                                mandate.setBank_branch(bankDetails.getBank_branch());
                                mandate_list.add(mandate);
                            }else
                            {
                                if(mandateDetails.getNse_ach_flag().equals(1) && StringHelper.isNotEmpty(mandateDetails.getNse_ach()))
                                {
                                    mandate_status = "Approved";
                                    mandate_desc = "Mandate generated & approved.";

                                    MandateDetailsPojo mandate = new MandateDetailsPojo();
                                    mandate.setBank_name(bankDetails.getBank_name());
                                    mandate.setBank_account_number(mandateDetails.getBank_account_number());
                                    mandate.setMandate_type("ACH Mandate");
                                    mandate.setMandate_flag(mandateDetails.getNse_ach_flag());
                                    mandate.setMandate_id(mandateDetails.getNse_ach());
                                    mandate.setMandate_amount(mandateDetails.getNse_ach_amount());
                                    mandate.setMandate_approved(mandateDetails.getNse_ach_approved());
                                    mandate.setMandate_status(mandate_status);

                                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
                                    Date mandate_date = mandateDetails.getNse_ach_created_date();
                                    String mandate_date_str = "";
                                    if(mandate_date != null)
                                    {
                                        mandate_date_str = sdf.format(mandate_date);
                                    }

                                    mandate.setMandate_date(mandate_date_str);
                                    mandate.setMandate_desc(mandate_desc);
                                    mandate_list.add(mandate);
                                }else
                                {
                                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", 400, "status_msg", "Account Details Not Found"));
                                }
                            }
                        }else
                        {
                            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", 400, "status_msg", "Bank Account Details Not Found"));
                        }
                    }

                    return ResponseEntity.ok(mandate_list);
                }else
                {
                    List<UsersBankDetails> bankList = usersBankDetailsRepository.findBankDetails(user.getUser_id(),clientName,onlineCode,bse_nse_mfu_flag,broker_code);
                    for(UsersMandateDetails mandateDetails : mandateList) {
                        UsersBankDetails bankDetails = bankList.stream().filter(bank -> bank.getBank_account_number().equals(mandateDetails.getBank_account_number())).findFirst().orElse(null);
                        if(bankDetails != null){
                            mandateDetails.setBank_name(bankDetails.getBank_name());
                            mandateDetails.setBank_account_type(bankDetails.getBank_account_type());
                        }
                    }
                    return ResponseEntity.ok(mandateList);
                }
            }
            else
            {
                List<MandateDetailsPojo> mandate_list = new ArrayList<>();

                List<UsersBankDetails> bankList = usersBankDetailsRepository.findBankDetails(user.getUser_id(),clientName,onlineCode,bse_nse_mfu_flag,broker_code);

                for(UsersBankDetails bankDetails : bankList)
                {
                    MandateDetailsPojo mandate = new MandateDetailsPojo();
                    mandate.setBank_name(bankDetails.getBank_name());
                    mandate.setBank_account_number(bankDetails.getBank_account_number());
                    mandate.setBank_ifsc_code(bankDetails.getBank_ifsc_code());
                    mandate.setBank_micr_code(bankDetails.getBank_micr_code());
                    mandate.setMandate_type("MANDATE");
                    mandate.setMandate_flag(0);
                    mandate.setMandate_id("");
                    mandate.setMandate_amount("0");
                    mandate.setMandate_approved(0);
                    mandate.setAccount_type(bankDetails.getBank_account_type());
                    mandate.setMandate_desc("");
                    mandate.setMandate_date("");
                    mandate.setMandate_status("");
                    mandate.setBank_account_holder_name(bankDetails.getBank_account_holder_name());
                    mandate.setBank_branch(bankDetails.getBank_branch());
                    mandate_list.add(mandate);
                }
                return ResponseEntity.ok(mandate_list);
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", 500, "status_msg", "Error occurred while fetching key"));
        }
    }

    @GetMapping(value="/getCancelSipReason")
    public ResponseEntity<?> getCancelSipReason(@RequestHeader("Authorization") String token) throws Exception
    {
        Optional<UsersOnlineRegDetails> user = null;
        List<CommonPojo> masterList = new ArrayList<CommonPojo>();
        CommonPojo pojo = null;
        Integer log_id = null;
        try
        {
            String user_id = TokenInterceptor.extractInvestorIdFromToken(token,secretKey);
            String client_name = TokenInterceptor.extractClientNamedFromToken(token,secretKey);

            if(StringHelper.isEmpty(client_name))
            {
                return UserUtils.getCommonResponse(StatusMessage.ClientNameInvalidMessage, StatusMessage.FailureCode);
            }

            if(StringHelper.isEmpty(user_id))
            {
                return UserUtils.getCommonResponse("Please provide the user id", StatusMessage.FailureCode);
            }

            user = userOnlineRegDetailsRespository.findInactiveNseByUserIdAndClientNameInActive(Integer.parseInt(user_id), client_name);

            if(user.isPresent())
            {
                pojo = new CommonPojo();
                pojo.setCode("SC1");
                pojo.setDesc("Non availability of Funds");
                masterList.add(pojo);

                pojo = new CommonPojo();
                pojo.setCode("SC2");
                pojo.setDesc("Scheme not performing");
                masterList.add(pojo);

                pojo = new CommonPojo();
                pojo.setCode("SC3");
                pojo.setDesc("Service issue");
                masterList.add(pojo);

                pojo = new CommonPojo();
                pojo.setCode("SC4");
                pojo.setDesc("Load Revised");
                masterList.add(pojo);

                pojo = new CommonPojo();
                pojo.setCode("SC5");
                pojo.setDesc("Wish to invest in other schemes");
                masterList.add(pojo);

                pojo = new CommonPojo();
                pojo.setCode("SC6");
                pojo.setDesc("Change in Fund Manager");
                masterList.add(pojo);

                pojo = new CommonPojo();
                pojo.setCode("SC7");
                pojo.setDesc("Goal Achieved");
                masterList.add(pojo);

                pojo = new CommonPojo();
                pojo.setCode("SC8");
                pojo.setDesc("Not comfortable with market volatility");
                masterList.add(pojo);

                pojo = new CommonPojo();
                pojo.setCode("SC9");
                pojo.setDesc("Will be restarting SIP after few months");
                masterList.add(pojo);

                pojo = new CommonPojo();
                pojo.setCode("SC10");
                pojo.setDesc("Modifications in bank/mandate/date etc");
                masterList.add(pojo);

                pojo = new CommonPojo();
                pojo.setCode("SC11");
                pojo.setDesc("I have decided to invest elsewhere");
                masterList.add(pojo);

                pojo = new CommonPojo();
                pojo.setCode("SC12");
                pojo.setDesc("This is not the right time to invest");
                masterList.add(pojo);

                pojo = new CommonPojo();
                pojo.setCode("SC13");
                pojo.setDesc("Others (pls specify the reason)");
                masterList.add(pojo);

                TransactionCommonResponse apiResponse = new TransactionCommonResponse();
                apiResponse.setStatus(StatusMessage.SuccessCode);
                apiResponse.setStatus_msg(StatusMessage.SuccessMessage);
                apiResponse.setMsg(StatusMessage.SuccessMessage);
                apiResponse.setList(masterList);
                return new ResponseEntity<TransactionCommonResponse>(apiResponse, HttpStatus.OK);
            }else
            {
                return UserUtils.getCommonResponse("User details not available.", StatusMessage.FailureCode);
            }
        }
        catch(Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date()); ex.printStackTrace();
            return UserUtils.getCommonResponse(StatusMessage.ExceptionAPIMessage, StatusMessage.ExceptionCode);
        }
    }

}
