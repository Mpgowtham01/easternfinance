package com.nse.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.nse.client.AmfiServiceClient;
import com.nse.client.UserServiceClient;
import com.nse.config.TokenInterceptor;
import com.nse.dto.amfi.AmfiLatestNavDto;
import com.nse.dto.amfi.AmfiSchemeMasterDTO;
import com.nse.dto.mf.*;
import com.nse.model.NseOnlineSchemeMaster;
import com.nse.model.NseOnlineSipStpSwpMaster;
import com.nse.model.NseTransactions;
import com.nse.pojo.*;
import com.nse.repository.NseOnlineSchemeMasterRepository;
import com.nse.repository.NseOnlineSipStpSwpMasterRepository;
import com.nse.repository.NseTransactionRepository;
import com.nse.response.*;
import com.nse.services.LogExceptionService;
import com.nse.services.NseLogService;
import com.nse.services.NseServiceDAO;
import com.nse.services.NseOnlineSchemeMasterService;
import com.nse.utils.*;
import feign.FeignException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.hibernate.internal.util.StringHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@RestController
@RequiredArgsConstructor

@Tag(
        name = "NSE Scheme Controller",
        description = "APIs related to NSE Scheme operations"
)
public class NseSchemeController {

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${vendor.logo.url}")
    private String vendorLogoPath;

    private final UserServiceClient userServiceClient;
    final static String nseUrl = "https://www.nseinvest.com";
    private final AmfiServiceClient amfiServiceClient;

    @Autowired
    private NseOnlineSchemeMasterRepository schemeRepository;

    @Autowired
    NseOnlineSipStpSwpMasterRepository nseOnlineSipStpSwpMasterRepository;

    @Autowired
    private NseServiceDAO nseService;

    @Autowired
    NseOnlineSchemeMasterRepository nseOnlineSchemeMasterRepository;

    @Autowired
    NseTransactionRepository nseTransactionRepository;

    @Autowired
    NseOnlineSchemeMasterService nseOnlineSchemeMasterService;

    @Autowired
    NseLogService nseLogService;

    @Autowired
    LogExceptionService logExceptionService;

    @Value("${amc.logo.url}")
    private String amcLogoPath;

    @Operation(
            summary = "Get all AMC codes",
            description = "Retrieves all unique AMC codes available in the repository.\n\n" +
                    "Performs a distinct filter on the AMC code list before returning.\n\n" +
                    "Returns a list of strings or an error message on failure.\n" +
                    "\n"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of AMC codes and names",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CommonPojo.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request – Required parameters are missing or invalid",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "not found"))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })

    @GetMapping("/getLumpsumAmc")
    public ResponseEntity<?> getLumpsumAmc(HttpServletRequest request,
                                           @RequestHeader("Authorization") String token)
    {
        String userid = "";
        String client_name = "";
        try
        {
            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);

            UserDto user = userServiceClient.getUserById(Integer.valueOf(userid), token);

            if (user == null)
            {
                return NseUtils.commonResponse("User not found", HttpStatus.BAD_REQUEST);
            }

            client_name = user.getClient_name();

            BseNseKeyDto nsekey = null;

            try {
                nsekey = userServiceClient.getByClientName(client_name,token);
            } catch (FeignException e) {
                if (e.status() == 400) {
                    return NseUtils.commonResponse("No record found for the given IIN Number and Client Name.", HttpStatus.BAD_REQUEST);
                } else if (e.status() == 404) {
                    return NseUtils.commonResponse("User not found.", HttpStatus.NOT_FOUND);
                } else {
                    return NseUtils.commonResponse("Downstream service error.", HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }

            List<String> amc_list;
            List<CommonPojo> amcList = null;
            String amc_string = nsekey.getAmc_names();

            if(!amc_string.isEmpty())
            {
                amc_list = new ArrayList<>(Arrays.asList(amc_string.split(",")));
            } else
            {
                amc_list = null;
            }

            List<Object[]> amc_sheme_master = nseOnlineSchemeMasterService.getLumpsumAmc(amc_list);

            if(amc_sheme_master != null && amc_sheme_master.size() > 0)
            {
                amcList = Stream.concat(Stream.of(new CommonPojo("All", "All")), amc_sheme_master.stream().map(s -> new CommonPojo((String) s[0], (String) s[1]))).collect(Collectors.toList());
            }else
            {
                return NseUtils.commonResponse("AMC not found", HttpStatus.BAD_REQUEST);
            }
            return ResponseEntity.ok(amcList);
        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Operation(
            summary = "Get scheme categories by AMC code",
            description = "Fetches a list of unique scheme categories for the specified AMC code.\n\n" +
                    "Filters data by matching AMC code (case-insensitive).\n\n" +
                    "**200: N/A** if no matching records are found.\n\n" +
                    "Returns a list of scheme category strings or an error message."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of scheme categories",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(type = "string", example = "Equity: Flexi Cap"))
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request – invalid or missing AMC code",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @GetMapping("/getLumpsumCategory")
    public ResponseEntity<?> getLumpsumCategory(@RequestParam String amcCode)
    {
        try
        {
            amcCode = NseUtils.checkParem(amcCode);

            if(StringHelper.isEmpty(amcCode)) {amcCode = "All";}

            List<String> categories = new ArrayList<>();

            categories = nseOnlineSchemeMasterService.getLumpsumCategories(amcCode);

            categories.add(0,"All");
            return ResponseEntity.ok(categories);
        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);}
    }

    @Operation(
            summary = "Get scheme names by AMC code and category",
            description = "Returns distinct scheme entries filtered by AMC code and scheme category.\n\n" +
                    "- Both parameters (`amcCode` and `schemeCategory`) must match repository data **case-insensitively**.\n" +
                    "- Returns all matching scheme details in full object form.\n\n" +
                    "**200: N/A** → If no matching records are found, an empty array is returned.\n"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful response with a list of scheme objects",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Scheme List Example",
                                    value = "[\n" +
                                            "  {\n" +
                                            "    \"scheme_name\": \"360 ONE ELSS Tax Saver Nifty 50 Index Fund - Regular Plan - IDCW Payout\",\n" +
                                            "    \"scheme_category\": \"Equity: ELSS\",\n" +
                                            "    \"amc_name\": \"360 ONE Mutual Fund\",\n" +
                                            "    \"amc_code\": \"360_ONE_MUTUALFUND_MF\",\n" +
                                            "    \"logo\": \"http://localhost:8084/images/amc-logo/360_one.png\"\n" +
                                            "  },\n" +
                                            "  {\n" +
                                            "    \"scheme_name\": \"360 ONE ELSS Tax Saver Nifty 50 Index Fund - Regular Plan - Growth\",\n" +
                                            "    \"scheme_category\": \"Equity: ELSS\",\n" +
                                            "    \"amc_name\": \"360 ONE Mutual Fund\",\n" +
                                            "    \"amc_code\": \"360_ONE_MUTUALFUND_MF\",\n" +
                                            "    \"logo\": \"http://localhost:8084/images/amc-logo/360_one.png\"\n" +
                                            "  }\n" +
                                            "]"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request – One or more query parameters are missing or invalid",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @GetMapping("/getLumpsumScheme")
    public ResponseEntity<?> getLumpsumScheme(@RequestParam String amcCode, @RequestParam String schemeCategory)
    {
        try
        {
            amcCode = NseUtils.checkParem(amcCode);
            schemeCategory = NseUtils.checkParem(schemeCategory);

            if (StringHelper.isEmpty(amcCode)) { amcCode = "All"; }
            if (StringHelper.isEmpty(schemeCategory)) { schemeCategory = "All"; }

            List<NewSchemePojo> schemeList = new ArrayList<>();
            List<Object[]> filteredSchemes = nseOnlineSchemeMasterService.getLumpsumSchemeNames(amcCode, schemeCategory);

            if (filteredSchemes != null && !filteredSchemes.isEmpty()) {
                schemeList = filteredSchemes.stream().map(row -> {
                            String schemeName = (String) row[0];
                            String scheme = (String) row[1];
                            String category = (String) row[2];
                            String amc_code = (String) row[3];
                            String amc_name = (String) row[4];
                            String logo = amcLogoPath + NseUtils.getLogoByAmcNameOrSchemeName(amc_code);
                            return new NewSchemePojo(schemeName,scheme, category, amc_code, amc_name, "", logo);
                        })
                        .sorted(Comparator.comparing(NewSchemePojo::getScheme_name, String.CASE_INSENSITIVE_ORDER))
                        .collect(Collectors.toList());
            }

            return ResponseEntity.ok(schemeList);

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get Lumpsum Scheme Options",
            description = "Returns distinct dividend reinvestment options (Growth / Dividend Payout / Reinvestment) for a given scheme name.\n\n" +
                    "- Filters based on matching `scheme` (case-insensitive).\n" +
                    "- Converts internal reinvestment tags (Z, N, Y, X) to descriptive labels.\n\n" +
                    "**200: N/A** → If no tags are found, defaults to 'Growth' only.\n"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved dividend reinvestment flags",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = String.class)),
                            examples = @ExampleObject(value = """
               [
                   {
                      "desc": "Growth",
                       "code": "Z"
                    }
                ]
            """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request – Missing or invalid scheme name",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error – Unable to fetch scheme options",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @GetMapping("/getLumpsumSchemeOptions")
    public ResponseEntity<?> getLumpsumSchemeOptions(@RequestParam String scheme)
    {
        CommonPojo pojo = null;
        List<CommonPojo> masterList = new ArrayList<>();
        try
        {
            scheme = NseUtils.checkParem(scheme);
            if(NseUtils.isUrlEncoded(scheme))
            {
                scheme = URLDecoder.decode(scheme, StandardCharsets.UTF_8);
            }
            List<String> schemeList = nseOnlineSchemeMasterRepository.findDistinctDivReinvestFlagBySchemeName(scheme);
            System.out.println("getLumpsumSchemeOptions = " + schemeList);

            if (schemeList != null && !schemeList.isEmpty())
            {
                for (String reinvest_tag : schemeList)
                {
                    if(reinvest_tag.equalsIgnoreCase("Z"))
                    {
                        pojo = new CommonPojo();
                        pojo.setCode(reinvest_tag);
                        pojo.setDesc("Growth");
                        masterList.add(pojo);
                    }else if(reinvest_tag.equalsIgnoreCase("N"))
                    {
                        pojo = new CommonPojo();
                        pojo.setCode(reinvest_tag);
                        pojo.setDesc("Dividend Payout");
                        masterList.add(pojo);
                    }else if(reinvest_tag.equalsIgnoreCase("Y"))
                    {
                        pojo = new CommonPojo();
                        pojo.setCode(reinvest_tag);
                        pojo.setDesc("Dividend Reinvestment");
                        masterList.add(pojo);
                    }else if(reinvest_tag.equalsIgnoreCase("X"))
                    {
                        pojo = new CommonPojo();
                        pojo.setCode("N");
                        pojo.setDesc("Dividend Payout");
                        masterList.add(pojo);

                        pojo = new CommonPojo();
                        pojo.setCode("Y");
                        pojo.setDesc("Dividend Reinvestment");
                        masterList.add(pojo);
                    }
                }

            } else
            {
                pojo = new CommonPojo();
                pojo.setCode("Z");
                pojo.setDesc("Growth");
                masterList.add(pojo);
            }
            return ResponseEntity.ok(masterList);
        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get Folio Numbers by AMC with Holdings",
            description = "Retrieves folio numbers for a given AMC and IIN (Investor Identification Number).\n\n" +
                    "It uses the authenticated user's client name and ID (from token) to retrieve matching records from NSE/BSE.\n\n" +
                    "If IIN is found and matches, tax status and holding nature are extracted.\n\n" +
                    "These are used to query folio numbers for the given AMC.\n\n" +
                    "**200: N/A** → Returns an empty list if no folios are found for the specified filters."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of folio numbers",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(type = "string", example = "910182691781"))
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request – Missing or invalid IIN, AMC name, or Authorization token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error – Could not retrieve folio numbers",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })

    @GetMapping("/getFolioNumberByAMCwithHoldings")
    public ResponseEntity<?> getFolioNumberByAMCwithHoldings(
            @RequestParam String iin_number,
            @RequestParam String amc_name,
            @RequestHeader("Authorization") String token)
    {
        String userid = "";
        String client_name = "";
        try {
             userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            UserDto users = userServiceClient.getUserById(Integer.valueOf(userid), token);
             client_name=users.getClient_name();

            client_name = client_name != null ? client_name.trim() : "";
            iin_number = iin_number != null ? iin_number.trim() : "";
            amc_name = amc_name != null ? amc_name.trim() : "";

            String tax_status_code = "";
            String holding_nature_code = "";
            String joint_holder_pan1 = "";
            String joint_holder_pan2 = "";

            UserDto user = null;
            try {
                user = userServiceClient.getUserDetailsByID(client_name, Integer.valueOf(userid),token);
            } catch (FeignException e) {
                if (e.status() == 400) {
                    return NseUtils.commonResponse("No record found for the given Client Name.", HttpStatus.BAD_REQUEST);
                }
            }
            System.out.println("user = " + user);

            if (user != null && user.getNse_iin_number().equalsIgnoreCase(iin_number))
            {
                tax_status_code = Optional.ofNullable(user.getTax_status_code()).orElse("");
                holding_nature_code = Optional.ofNullable(user.getHolding_nature_code()).orElse("");
                joint_holder_pan1 = Optional.ofNullable(user.getJoint_holder_pan1()).orElse("");
                joint_holder_pan2 = Optional.ofNullable(user.getJoint_holder_pan2()).orElse("");
            }
            else
            {
                UserBseNseDto nse = null;

                try
                {
                    nse = userServiceClient.getUserBseNseDetailsByIinNumber(client_name, iin_number,token);
                }
                catch (FeignException.BadRequest ex)
                {
                    nse = null;
                }
                if (nse != null) {
                    tax_status_code = Optional.ofNullable(nse.getTax_status_code()).orElse("");
                    holding_nature_code = Optional.ofNullable(nse.getHolding_nature_code()).orElse("");
                    joint_holder_pan1 = Optional.ofNullable(nse.getJoint_holder_pan1()).orElse("");
                    joint_holder_pan2 = Optional.ofNullable(nse.getJoint_holder_pan2()).orElse("");
                }
            }

            List<String> folioList = nseService.getFolioNumberByAMCwithHoldings(
                    client_name,
                    Integer.valueOf(userid),
                    amc_name,
                    holding_nature_code,
                    tax_status_code,
                    joint_holder_pan1,
                    joint_holder_pan2,token
            );
            return ResponseEntity.ok(folioList);

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getBankMandateOptions")
    public ResponseEntity<?> getBankMandateOptions(@RequestHeader ("Authorization") String token)
    {
        String userid = null;
        String client_name = null;
        CommonPojo pojo = null;
        List<CommonPojo> masterList = new ArrayList<CommonPojo>();
        try
        {
            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            UserDto user = userServiceClient.getUserById(Integer.valueOf(userid), token);

            if (user == null)
            {
                return NseUtils.commonResponse("User not found", HttpStatus.BAD_REQUEST);
            }

            client_name = user.getClient_name();

            if(StringHelper.isEmpty(userid))
            {
                return NseUtils.commonResponse("Please provide the user id", HttpStatus.BAD_REQUEST);
            }

            String bse_nse_mfu_flag = "NSE";

            BseNseKeyDto bseNseKey = userServiceClient.getByClientName(client_name,token);

            String vendors = NseUtils.checkParem(bseNseKey.getNse_bse());

            if(StringHelper.isNotEmpty(vendors) && !vendors.contains(bse_nse_mfu_flag.toLowerCase()))
            {
                return NseUtils.commonResponse("Client Not Available in "+bse_nse_mfu_flag.toUpperCase()+"", HttpStatus.BAD_REQUEST);
            }

            if(bse_nse_mfu_flag.equalsIgnoreCase("NSE"))
            {
                pojo = new CommonPojo();
                pojo.setCode("NET");
                pojo.setDesc("Net Banking");
                masterList.add(pojo);

                pojo = new CommonPojo();
                pojo.setCode("DC");
                pojo.setDesc("Debit Card");
                masterList.add(pojo);

                pojo = new CommonPojo();
                pojo.setCode("AA");
                pojo.setDesc("Aadhaar");
                masterList.add(pojo);
            }
            return ResponseEntity.ok(masterList);

        }catch (Exception ex)
        {
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @Operation(
            summary = "Get Payment Modes",
            description = "Fetches a predefined list of available payment modes such as Net Banking, UPI, and others based on the user's ACH approval status.\n\n" +
                    "Requires a valid Authorization token to determine user's eligibility for certain modes.\n\n" +
                    "Returns a list of payment modes as label-value pairs."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful retrieval of payment modes",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(
                                    schema = @Schema(
                                            implementation = CommonPojo.class,
                                            example = """
                    [
                        { "label": "Net Banking", "value": "Net Banking" },
                        { "label": "UPI", "value": "UPI" },
                        { "label": "Debit Mandate", "value": "Debit Mandate" },
                        { "label": "RTGS", "value": "RTGS/NEFT" },
                        { "label": "Cheque", "value": "Cheque" },
                        { "label": "DD", "value": "Demand Draft" }
                    ]
                    """
                                    )
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Invalid or missing Authorization token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error - Failed to fetch payment modes",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @GetMapping("/getPaymentMode")
    public ResponseEntity<?> getPaymentMode(@RequestHeader("Authorization") String token)
    {
        String userid = "";
        try
        {
            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            UserDto user = userServiceClient.getUserById(Integer.valueOf(userid), token);

            Boolean achFlag = false;

            if(user.getNse_ach_approved1().equals(1) || user.getNse_ach_approved2().equals(1) || user.getNse_ach_approved3().equals(1))
            {
                achFlag = true;
            }

            List<CommonPojo> modes = new ArrayList<CommonPojo>();
            modes.add(new CommonPojo("Cheque", "Cheque"));
            modes.add(new CommonPojo("UPI", "UPI"));
            modes.add(new CommonPojo("Net Banking", "Net Banking"));
            modes.add(new CommonPojo("RTGS", "RTGS/NEFT"));
            modes.add(new CommonPojo("Debit Mandate", "Debit Mandate"));
//            modes.add(new CommonPojo("DD", "Demand Draft"));
//            if (achFlag)
//            {
//                modes.add(new CommonPojo("Debit Mandate", "Debit Mandate"));
//            }
            return ResponseEntity.ok(modes);

        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Operation(
            summary = "Load Mandate Details",
            description = "Retrieves mandate details for a specific client and bank account using IIN and Authorization token.\n\n" +
                    "Performs validation and checks ACH approval status across multiple linked bank accounts.\n\n" +
                    "Returns a list of mandate detail objects (`MandateMasterResponse`) or an error message."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful retrieval of mandate details",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(
                                    schema = @Schema(
                                            implementation = MandateMasterResponse.class,
                                            example = """
                    [
                        {
                            "bank_name": "HDFC Bank",
                            "bank_account_number": "XXXXXXXX1234",
                            "bank_account_type": "Savings",
                            "nse_ach": "Approved",
                            "nse_ach_amount": 100000.00
                        },
                        {
                            "bank_name": "ICICI Bank",
                            "bank_account_number": "XXXXXXXX5678",
                            "bank_account_type": "Current",
                            "nse_ach": "Approved",
                            "nse_ach_amount": 200000.00
                        }
                    ]
                    """
                                    )
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Missing or invalid input parameters (IIN or bank account number)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error - Could not fetch mandate details",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @GetMapping("/getMandateDetails")
    public ResponseEntity<?> loadMandateDetails(
            @RequestParam String nse_iin_num,
            @RequestParam String bank_account_number,
            @RequestHeader("Authorization") String token)
    {
            String userid = "";
            String client_name ="";

            try
            {
                userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
                UserDto user = userServiceClient.getUserById(Integer.valueOf(userid), token);

                client_name = user.getClient_name();

                if (nse_iin_num == null)
                {
                    nse_iin_num = "";
                }
                if (bank_account_number == null)
                {
                    bank_account_number = "";
                }

                nse_iin_num = nse_iin_num.trim();
                bank_account_number = bank_account_number.trim();

                List<MandateMasterResponse> mandate_list = new ArrayList<>();

                if (user.getNse_iin_number().equalsIgnoreCase(nse_iin_num))
                {
                    String nse_iin = user.getNse_iin_number();
                    String acc1 = user.getBank_account_number1();
                    String acc2 = user.getBank_account_number2();
                    String acc3 = user.getBank_account_number3();
                    if (acc1 == null)
                    {
                        acc1 = "";
                    }
                    if (acc2 == null)
                    {
                        acc2 = "";
                    }
                    if (acc3 == null)
                    {
                        acc3 = "";
                    }
                    if (acc1.equalsIgnoreCase(bank_account_number))
                    {
                        Integer ach_approved1 = user.getNse_ach_approved1();
                        if (ach_approved1 == 1)
                        {
                            MandateMasterResponse mandate = new MandateMasterResponse();
                            mandate.setBank_name(user.getBank_name1());
                            mandate.setBank_account_number(user.getBank_account_number1());
                            mandate.setBank_account_type(user.getBank_account_type1());
                            mandate.setNse_ach(user.getNse_ach1());
                            mandate.setNse_ach_amount(user.getNse_ach_amount1());
                            mandate_list.add(mandate);

                            String onlineFlag = "NSE";
                            String onlineCode = user.getNse_iin_number();

                            List<UserMandateDetailsDto> additional_mandate_list = null;

                            try
                            {
                                additional_mandate_list = userServiceClient.getByAllFields(client_name,onlineFlag,onlineCode,bank_account_number, Integer.valueOf(userid),token);
                            } catch (FeignException e)
                            {
                                if (e.status() == 400)
                                {
                                    return NseUtils.commonResponse("No mandate details found for the given parameters.", HttpStatus.BAD_REQUEST);
                                } else if (e.status() == 404)
                                {
                                    return NseUtils.commonResponse("No mandate details found for the given parameters..", HttpStatus.NOT_FOUND);
                                } else
                                {
                                    return NseUtils.commonResponse("Downstream service error.", HttpStatus.INTERNAL_SERVER_ERROR);
                                }
                            }
                            if (additional_mandate_list != null && additional_mandate_list.size() > 0)
                            {
                                for (UserMandateDetailsDto userMandateDetailsDto : additional_mandate_list)
                                {
                                    mandate = new MandateMasterResponse();
                                    mandate.setBank_name(user.getBank_name1());
                                    mandate.setBank_account_number(user.getBank_account_number1());
                                    mandate.setBank_account_type(user.getBank_account_type1());
                                    mandate.setNse_ach(userMandateDetailsDto.getNse_ach());
                                    mandate.setNse_ach_amount(userMandateDetailsDto.getNse_ach_amount());
                                    mandate_list.add(mandate);
                                }
                            }
                        } else if (acc2.equalsIgnoreCase(bank_account_number))
                        {

                            Integer ach_approved2 = user.getNse_ach_approved2();
                            if (ach_approved2 == 1)
                            {
                                MandateMasterResponse mandate = new MandateMasterResponse();
                                mandate.setBank_name(user.getBank_name2());
                                mandate.setBank_account_number(user.getBank_account_number2());
                                mandate.setBank_account_type(user.getBank_account_type2());
                                mandate.setNse_ach(user.getNse_ach2());
                                mandate.setNse_ach_amount(user.getNse_ach_amount2());
                                mandate_list.add(mandate);
                                String onlineFlag = "NSE";
                                String onlineCode = user.getNse_iin_number();

                                List<UserMandateDetailsDto> additional_mandate_list = null;

                                additional_mandate_list = userServiceClient.getByAllFields(client_name,onlineFlag,onlineCode,bank_account_number, Integer.valueOf(userid),token);

                                if (additional_mandate_list != null && additional_mandate_list.size() > 0)
                                {
                                    for (UserMandateDetailsDto userMandateDetailsDto : additional_mandate_list)
                                    {
                                        mandate = new MandateMasterResponse();
                                        mandate.setBank_name(user.getBank_name2());
                                        mandate.setBank_account_number(user.getBank_account_number2());
                                        mandate.setBank_account_type(user.getBank_account_type2());
                                        mandate.setNse_ach(userMandateDetailsDto.getNse_ach());
                                        mandate.setNse_ach_amount(userMandateDetailsDto.getNse_ach_amount());
                                        mandate_list.add(mandate);
                                    }
                                }
                            }
                        } else
                        {

                            Integer ach_approved3 = user.getNse_ach_approved3();
                            if (ach_approved3 == 1)
                            {
                                MandateMasterResponse mandate = new MandateMasterResponse();
                                mandate.setBank_name(user.getBank_name3());
                                mandate.setBank_account_number(user.getBank_account_number3());
                                mandate.setBank_account_type(user.getBank_account_type3());
                                mandate.setNse_ach(user.getNse_ach3());
                                mandate.setNse_ach_amount(user.getNse_ach_amount3());
                                mandate_list.add(mandate);
                                String onlineFlag = "NSE";
                                String onlineCode = user.getNse_iin_number();

                                List<UserMandateDetailsDto> additional_mandate_list = null;

                                additional_mandate_list = userServiceClient.getByAllFields(client_name,onlineFlag,onlineCode,bank_account_number, Integer.valueOf(userid),token);

                                if (additional_mandate_list != null && additional_mandate_list.size() > 0)
                                {
                                    for (UserMandateDetailsDto userMandateDetailsDto : additional_mandate_list)
                                    {
                                        mandate = new MandateMasterResponse();
                                        mandate.setBank_name(user.getBank_name3());
                                        mandate.setBank_account_number(user.getBank_account_number3());
                                        mandate.setBank_account_type(user.getBank_account_type3());
                                        mandate.setNse_ach(userMandateDetailsDto.getNse_ach());
                                        mandate.setNse_ach_amount(userMandateDetailsDto.getNse_ach_amount());
                                        mandate_list.add(mandate);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    UserBseNseDto nse = null;
                    try
                    {
                        nse = userServiceClient.getUserBseNseDetailsByIinNumberAndUserId(Integer.valueOf(userid), nse_iin_num, client_name,token);
                    } catch (FeignException e)
                    {
                        if (e.status() == 400)
                        {
                            return NseUtils.commonResponse("No record found for the given IIN Number and Client Name.", HttpStatus.BAD_REQUEST);
                        } else if (e.status() == 404)
                        {
                            return NseUtils.commonResponse("User not found.", HttpStatus.NOT_FOUND);
                        } else
                        {
                            return NseUtils.commonResponse("Downstream service error.", HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    }

                    if (nse != null && nse.getNse_active() == 1)
                    {
                        String acc1 = nse.getBank_account_number1();
                        String acc2 = nse.getBank_account_number2();
                        String acc3 = nse.getBank_account_number3();
                        if (acc1 == null)
                        {
                            acc1 = "";
                        }
                        if (acc2 == null)
                        {
                            acc2 = "";
                        }
                        if (acc3 == null) {
                            acc3 = "";
                        }

                        if (acc1.equalsIgnoreCase(bank_account_number))
                        {
                            Integer ach_approved1 = nse.getNse_ach_approved1();
                            if (ach_approved1 == 1)
                            {
                                MandateMasterResponse mandate = new MandateMasterResponse();
                                mandate.setBank_name(nse.getBank_name1());
                                mandate.setBank_account_number(nse.getBank_account_number1());
                                mandate.setBank_account_type(nse.getBank_account_type1());
                                mandate.setNse_ach(nse.getNse_ach1());
                                mandate.setNse_ach_amount(nse.getNse_ach_amount1());
                                mandate_list.add(mandate);

                                String onlineFlag = "NSE";
                                String onlineCode = user.getNse_iin_number();

                                List<UserMandateDetailsDto> additional_mandate_list = null;


                                additional_mandate_list = userServiceClient.getByAllFields(client_name,onlineFlag,onlineCode,bank_account_number, Integer.valueOf(userid),token);


                                if (additional_mandate_list != null && additional_mandate_list.size() > 0)
                                {
                                    for (UserMandateDetailsDto userMandateDetailsDto : additional_mandate_list)
                                    {
                                        mandate = new MandateMasterResponse();
                                        mandate.setBank_name(nse.getBank_name1());
                                        mandate.setBank_account_number(nse.getBank_account_number1());
                                        mandate.setBank_account_type(nse.getBank_account_type1());
                                        mandate.setNse_ach(userMandateDetailsDto.getNse_ach());
                                        mandate.setNse_ach_amount(userMandateDetailsDto.getNse_ach_amount());
                                        mandate_list.add(mandate);
                                    }
                                }
                            }

                        } else if (acc2.equalsIgnoreCase(bank_account_number))
                        {

                            Integer ach_approved2 = nse.getNse_ach_approved2();
                            if (ach_approved2 == 1) {
                                MandateMasterResponse mandate = new MandateMasterResponse();
                                mandate.setBank_name(nse.getBank_name2());
                                mandate.setBank_account_number(nse.getBank_account_number2());
                                mandate.setBank_account_type(nse.getBank_account_type2());
                                mandate.setNse_ach(nse.getNse_ach2());
                                mandate.setNse_ach_amount(nse.getNse_ach_amount2());
                                mandate_list.add(mandate);

                                String onlineFlag = "NSE";
                                String onlineCode = user.getNse_iin_number();

                                List<UserMandateDetailsDto> additional_mandate_list = null;

                                    additional_mandate_list = userServiceClient.getByAllFields(client_name,onlineFlag,onlineCode,bank_account_number, Integer.valueOf(userid),token);


                                if (additional_mandate_list != null && additional_mandate_list.size() > 0)
                                {
                                    for (UserMandateDetailsDto userMandateDetailsDto : additional_mandate_list)
                                    {
                                        mandate = new MandateMasterResponse();
                                        mandate.setBank_name(nse.getBank_name2());
                                        mandate.setBank_account_number(nse.getBank_account_number2());
                                        mandate.setBank_account_type(nse.getBank_account_type2());
                                        mandate.setNse_ach(userMandateDetailsDto.getNse_ach());
                                        mandate.setNse_ach_amount(userMandateDetailsDto.getNse_ach_amount());
                                        mandate_list.add(mandate);
                                    }
                                }
                            }
                        } else
                        {

                            Integer ach_approved3 = nse.getNse_ach_approved3();
                            if (ach_approved3 == 1)
                            {
                                MandateMasterResponse mandate = new MandateMasterResponse();
                                mandate.setBank_name(nse.getBank_name3());
                                mandate.setBank_account_number(nse.getBank_account_number3());
                                mandate.setBank_account_type(nse.getBank_account_type3());
                                mandate.setNse_ach(nse.getNse_ach3());
                                mandate.setNse_ach_amount(nse.getNse_ach_amount3());
                                mandate_list.add(mandate);

                                String onlineFlag = "NSE";
                                String onlineCode = user.getNse_iin_number();

                                List<UserMandateDetailsDto> additional_mandate_list = null;


                                    additional_mandate_list = userServiceClient.getByAllFields(client_name,onlineFlag,onlineCode,bank_account_number, Integer.valueOf(userid),token);


                                if (additional_mandate_list != null && additional_mandate_list.size() > 0)
                                {
                                    for (UserMandateDetailsDto userMandateDetailsDto : additional_mandate_list)
                                    {
                                        mandate = new MandateMasterResponse();
                                        mandate.setBank_name(nse.getBank_name3());
                                        mandate.setBank_account_number(nse.getBank_account_number3());
                                        mandate.setBank_account_type(nse.getBank_account_type3());
                                        mandate.setNse_ach(userMandateDetailsDto.getNse_ach());
                                        mandate.setNse_ach_amount(userMandateDetailsDto.getNse_ach_amount());
                                        mandate_list.add(mandate);
                                    }
                                }
                            }
                        }

                    }
                }
                return ResponseEntity.ok(mandate_list);

            } catch (Exception ex) {
                System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
                ex.printStackTrace();
                return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

    @Operation(
            summary = "Get Lumpsum Scheme Code",
            description = """
        Fetches the lumpsum scheme code based on the provided `scheme`, `dividend_code`, and `amount`.

        - Validates that `scheme` is not empty.
        - Queries internal AMFI service for matching scheme.
        - Returns detailed scheme data, or an appropriate HTTP error.
        """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Scheme code found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NseOnlineSchemeMasterDto.class),
                            examples = @ExampleObject(
                                    name = "Success",
                                    value = """
                    {
                      "schemeCode": "123456",
                      "schemeName": "Axis Bluechip Fund",
                      "dividendCode": "G",
                      "amcName": "Axis Mutual Fund",
                      "schemeType": "Open Ended",
                      "schemeCategory": "Equity",
                      "purchaseAllowed": "Y",
                      "newPurchaseMinAmount": 5000,
                      "redemptionAllowed": "Y",
                      "redemptionMinQty": 1,
                      "exitLoadFlag": "Y",
                      "exitLoad": "1% if redeemed within 1 year",
                      "lockInPeriodFlag": "N",
                      "lockInPeriod": null
                    }
                """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request – scheme is required",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(
                                    name = "Missing Scheme",
                                    value = "\"Scheme is required.\""
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Scheme not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(
                                    name = "Not Found",
                                    value = "\"No matching scheme found.\""
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(
                                    name = "Server Error",
                                    value = "\"Error fetching scheme from AMFI: <detailed error message>\""
                            )
                    )
            )
    })

    @GetMapping("/getLumpsumSchemeCode")
        public ResponseEntity<?> getLumpsumSchemecode(
            @RequestParam(required = false) String scheme,
            @RequestParam(required = false) String dividend_code,
            @RequestParam(required = false) String amount) {
        try {
            // Ensure parameters are not null and trimmed
            scheme = (scheme != null) ? scheme.trim() : "";
            dividend_code = (dividend_code != null) ? dividend_code.trim() : "";
            amount = (amount != null) ? amount.trim() : "";

            if (StringHelper.isEmpty(scheme)) {
                return ResponseEntity.badRequest().body("Scheme is required.");
            }

            System.out.println("scheme = " + scheme + "---" +  dividend_code + "---" + amount);

            NseOnlineSchemeMaster dto = nseService.getLumpsumSchemecodeService(scheme, dividend_code, amount);

            if (dto != null) {
                return ResponseEntity.ok(dto);
            } else {
                return NseUtils.commonResponse("No Scheme Available.", HttpStatus.OK);

            }

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Validate Lumpsum Amount for Scheme",
            description = """
        Validates whether the entered lumpsum amount is valid based on the scheme code provided.

        - Looks up scheme details using the given `scheme_code`.
        - Returns scheme info if found.
        - Returns appropriate error if scheme is invalid or not found.
        """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Scheme found and amount is valid",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NseOnlineSchemeMaster.class),
                            examples = @ExampleObject(
                                    name = "Valid Scheme",
                                    value = """
                    {
                      "schemeCode": "AXIS123",
                      "schemeName": "Axis Long Term Equity Fund",
                      "newPurchaseMinAmount": 500.0,
                      "additionalPurchaseMinAmount": 100.0,
                      "additionalPurchaseMaxAmount": 1000000.0,
                      "purchaseAmountMultiplier": 100.0,
                      "purchaseAllowed": "Y"
                    }
                """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid scheme code provided",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(
                                    name = "Invalid Input",
                                    value = "\"Scheme code must not be empty or invalid.\""
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Scheme not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(
                                    name = "Not Found",
                                    value = "\"No valid scheme found for given scheme code.\""
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(
                                    name = "Server Error",
                                    value = "\"Error fetching scheme from AMFI: <error message>\""
                            )
                    )
            )
    })

    @GetMapping("/validateLumpsumAmount")
    public ResponseEntity<?> validateLumpsumAmount(@RequestParam String scheme_code)
    {
        try
        {
            List<NseOnlineSchemeMaster> schemes = schemeRepository.findValidSchemesBySchemeCode(scheme_code);

            if (schemes.isEmpty())
            {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No valid scheme found for given scheme code.");
            }

            NseOnlineSchemeMaster scheme = schemes.get(0);

            return ResponseEntity.ok(scheme);
        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get Broker Code",
            description = """
        Returns broker code and EUIN details based on the user's ARN flag.

        - Authenticates user from JWT token in the `Authorization` header.
        - Fetches client name, then resolves ARN flag logic.
        - Based on `arn_flag` value (1 to 5), determines:
            - Single or multiple broker codes
            - EUIN value(s) or empty list
        - Used in order processing and compliance mapping.
        
        ### ARN Flag Behavior:
        - 1 → Single Broker & EUIN
        - 2 → Multiple EUINs
        - 3/4 → Multiple Brokers
        - 5 → No broker/EUIN

        ### Response Codes:
        - `200`: Successfully fetched broker details
        - `200 (NA)`: No broker or EUIN available
        - `400`: Invalid or missing token
        - `500`: Internal error or downstream failure
        """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Broker code(s) and EUIN(s) fetched successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = BrokerCodeResponseDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Single Broker",
                                            value = """
                        {
                          "broker_code": "ARN1234",
                          "euin": "E123456",
                          "brokerCodeList": [],
                          "euinList": []
                        }
                    """
                                    ),
                                    @ExampleObject(
                                            name = "Multiple Brokers",
                                            value = """
                        {
                          "broker_code": "",
                          "euin": "",
                          "brokerCodeList": ["ARN1111", "ARN2222", "ARN3333"],
                          "euinList": []
                        }
                    """
                                    ),
                                    @ExampleObject(
                                            name = "Multiple EUINs",
                                            value = """
                        {
                          "broker_code": "",
                          "euin": "",
                          "brokerCodeList": [],
                          "euinList": ["E123", "E456", "E789"]
                        }
                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "200",
                    description = "No broker code or EUIN available (NA)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = BrokerCodeResponseDto.class),
                            examples = @ExampleObject(
                                    name = "No Broker Available",
                                    value = """
                    {
                      "broker_code": "",
                      "euin": "",
                      "brokerCodeList": [],
                      "euinList": []
                    }
                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing or invalid Authorization token",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(
                                    value = "\"Authorization token is missing or malformed.\""
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error during broker code fetch",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(
                                    value = "\"Error retrieving broker code details: <error message>\""
                            )
                    )
            )
    })

    @GetMapping(value = "/getBrokerCode", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getBrokerCode(@RequestHeader("Authorization") String token)
    {

        String userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
        UserDto users = userServiceClient.getUserById(Integer.valueOf(userid), token);
        String client_name=users.getClient_name();

        List<String> brokerCodeList = new ArrayList<String>();
        List<String> euinList = new ArrayList<String>();
        BseNseKeyDto bsekey = null;
        try
        {
            bsekey = userServiceClient.getByClientName(client_name,token);
        } catch (FeignException e)
        {
            if (e.status() == 400)
            {
                return NseUtils.commonResponse("No record found for the given IIN Number and Client Name.", HttpStatus.BAD_REQUEST);
            } else if (e.status() == 404)
            {
                return NseUtils.commonResponse("User not found.", HttpStatus.NOT_FOUND);
            } else
            {
                return NseUtils.commonResponse("Downstream service error.", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        int arn_flag = 0;

        String broker_code = "";
        String euin = "";
        if(arn_flag == 1)
        {
            broker_code = bsekey.getBrokerCode();
            euin = bsekey.getEuin();
        }
        if(arn_flag == 2)
        {
            String euin_code = bsekey.getEuin();
            if(euin_code == null){euin_code = "";};
            euin_code = euin_code.trim();
            euinList = new ArrayList<String>(Arrays.asList(euin_code.split(",")));
        }
        if(arn_flag == 3 || arn_flag == 4)
        {
            String broker_code1 = bsekey.getBrokerCode();

            String nse_appln_id1 = bsekey.getNse_appln_id();


            if(broker_code1 == null){broker_code1 = "";};

            if(nse_appln_id1 == null){nse_appln_id1 = "";};


            broker_code1 = broker_code1.trim();

            nse_appln_id1 = nse_appln_id1.trim();


            if(StringHelper.isNotEmpty(broker_code1) && StringHelper.isNotEmpty(nse_appln_id1))
            {
                brokerCodeList.add(broker_code1);
            }

        }
        if(arn_flag == 5)
        {
            broker_code = "";
            euin = "";
            brokerCodeList = new ArrayList<String>();
            euinList = new ArrayList<String>();
        }
        if(brokerCodeList.size() > 1)
        {
            euinList = new ArrayList<String>();
        }
        BrokerCodeResponseDto response = new BrokerCodeResponseDto(broker_code, euin, brokerCodeList, euinList);
        return ResponseEntity.ok(response);
    }
    //SIP

    @Operation(
            summary = "Get all SIP AMC codes",
            description = "Retrieves a list of unique AMC code-name pairs where SIP is allowed.\n\n" +
                    "If the provided client name is valid and linked to AMC names, only those AMC names are queried.\n" +
                    "Otherwise, fetches all valid AMC code-name pairs from the scheme repository."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved the list of AMC code-name pairs",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CommonPojo.class)))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or missing clientName parameter",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error while fetching AMC codes",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @GetMapping("/getSipAmc")
    public ResponseEntity<?> getAllSIPAmcCodes(@RequestHeader("Authorization") String token)
    {
        String userid = "";
        try
        {
            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            UserDto users = userServiceClient.getUserById(Integer.valueOf(userid), token);
            String clientName=users.getClient_name();
            List<CommonPojo> result;
            List<String> amcNames = null;

            BseNseKeyDto bseDto = null;
            try
            {
                bseDto = userServiceClient.getByClientName(clientName,token);
            } catch (FeignException e)
            {
                if (e.status() == 400)
                {
                    return NseUtils.commonResponse("No record found for the given IIN Number and Client Name.", HttpStatus.BAD_REQUEST);
                } else if (e.status() == 404)
                {
                    return NseUtils.commonResponse("User not found.", HttpStatus.NOT_FOUND);
                } else
                {
                    return NseUtils.commonResponse("Downstream service error.", HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }

            if (bseDto != null && bseDto.getAmc_names() != null && !bseDto.getAmc_names().isEmpty())
            {
                amcNames = Arrays.asList(bseDto.getAmc_names().split(","));
            }

            List<Object[]> amc_sheme_master = nseOnlineSchemeMasterService.getLumpsumAmc(amcNames);

            if(amc_sheme_master != null && amc_sheme_master.size() > 0)
            {
                result = Stream.concat(Stream.of(new CommonPojo("All", "All")), amc_sheme_master.stream().map(s -> new CommonPojo((String) s[0], (String) s[1]))).collect(Collectors.toList());
            }else
            {
                return NseUtils.commonResponse("AMC not found", HttpStatus.BAD_REQUEST);
            }

            return ResponseEntity.ok(result);

        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get SIP category names by AMC code",
            description = "Retrieves a list of unique SIP category names filtered by AMC code.\n\n" +
                    "If `amc_code` is 'All', it returns all distinct SIP category names.\n" +
                    "Otherwise, it filters the categories based on the provided AMC code.\n\n" +
                    "Returns a list of category name strings or an appropriate error message."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved category names",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(
                                    schema = @Schema(
                                            type = "string",
                                            example = "Equity: ELSS"
                                    )
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - missing or invalid AMC code",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @GetMapping("/getSipCategoryNamesByAMC")
    public ResponseEntity<?> getsipCategoryNamesByAMC(@RequestParam String amc_code)
    {
        try
        {
            if (amc_code == null || amc_code.trim().isEmpty())
            {
                return ResponseEntity.badRequest().body("amcCode is required");
            }
            amc_code = amc_code.trim();

            List<String> categoryNames = nseOnlineSchemeMasterService.getSipCategories(amc_code);

            if (categoryNames != null && !categoryNames.isEmpty())
            {
                categoryNames.add("All");
                return ResponseEntity.ok(categoryNames);
            }
            else
            {
                return NseUtils.commonResponse("No AMC code-name data found.", HttpStatus.BAD_REQUEST);
            }
        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get SIP scheme names by AMC code and category",
            description = "Fetches a list of unique scheme names for the specified AMC code and category.\n\n" +
                    "If category is 'All', returns all scheme names for the AMC code.\n\n" +
                    "Returns a list of scheme names or an error message in case of failure."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of schemes with name, category, AMC details, and logo",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Scheme List Example",
                                    value = "[\n" +
                                            "  {\n" +
                                            "    \"scheme_name\": \"360 ONE Balanced Hybrid Fund - Regular Plan - Growth\",\n" +
                                            "    \"scheme_category\": \"Hybrid: Balanced\",\n" +
                                            "    \"amc_name\": \"360 ONE Mutual Fund\",\n" +
                                            "    \"amc_code\": \"360_ONE_MUTUALFUND_MF\",\n" +
                                            "    \"logo\": \"http://localhost:8084/images/amc-logo/360_one.png\"\n" +
                                            "  },\n" +
                                            "  {\n" +
                                            "    \"scheme_name\": \"360 ONE Balanced Hybrid fund - Regular Plan - IDCW\",\n" +
                                            "    \"scheme_category\": \"Hybrid: Balanced\",\n" +
                                            "    \"amc_name\": \"360 ONE Mutual Fund\",\n" +
                                            "    \"amc_code\": \"360_ONE_MUTUALFUND_MF\",\n" +
                                            "    \"logo\": \"http://localhost:8084/images/amc-logo/360_one.png\"\n" +
                                            "  }\n" +
                                            "]"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Missing or invalid parameters",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })

    @GetMapping("/getSipSchemeByAmc")
    public ResponseEntity<?> getSipSchemeByAmc(@RequestParam String amcCode,
                                               @RequestParam String category)
    {
        try
        {
            amcCode = NseUtils.checkParem(amcCode);
            category = NseUtils.checkParem(category);

            if(StringHelper.isEmpty(amcCode)){amcCode = "All";}
            if(StringHelper.isEmpty(category)){category = "All";}

            List<SchemePojo> schemeList =  new ArrayList<>();
            List<Object[]> schemeNames = nseOnlineSchemeMasterService.getSipSchemeNames(amcCode, category);

            if (schemeNames != null && !schemeNames.isEmpty())
            {
                schemeList = schemeNames.stream().map(row ->
                {
                    String schemeName = (String) row[0];
                    String schemeCategory = (String) row[1];
                    String amc_code = (String) row[2];
                    String amc_name = (String) row[3];
                    String scheme_code = (String) row[4];
                    String logo = amcLogoPath + NseUtils.getLogoByAmcNameOrSchemeName(amc_code);
                    return new SchemePojo(schemeName, schemeCategory, amc_code, amc_name,scheme_code, logo);
                }).collect(Collectors.toList());
            }else
            {
                return NseUtils.commonResponse("No Scheme names data found.", HttpStatus.BAD_REQUEST);
            }

            return ResponseEntity.ok(schemeList);

        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get SIP Dates and Frequencies",
            description = "Fetches a list of SIP options (like frequency and dates) for the specified scheme name.\n\n" +
                    "Filters data where master_option is 'SIP', status is active (1), and matches the provided scheme name.\n\n" +
                    "Returns a list of SIP options or an internal server error on failure."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of SIP options",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NseOnlineSipStpSwpMaster.class))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid input",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @GetMapping("/getSipDatesAndFrequency")
    public ResponseEntity<?> getSipDatesAndFrequency(@RequestParam String scheme_name) {
        try {
            if (scheme_name == null || scheme_name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("scheme_name is required");
            }

            if (NseUtils.isUrlEncoded(scheme_name)) {
                scheme_name = URLDecoder.decode(scheme_name, StandardCharsets.UTF_8);
            }

            List<NseOnlineSipStpSwpMaster> frequencies = nseOnlineSipStpSwpMasterRepository.findGroupedSipBySchemeAmfi(scheme_name);

            for (NseOnlineSipStpSwpMaster item : frequencies) {
                if ("DAILY".equalsIgnoreCase(item.getSip_frequency()) && (item.getSip_dates() == null || item.getSip_dates().trim().isEmpty())) {
                    // Generate "1,2,3,...31" string
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i <= 31; i++) {
                        sb.append(i).append(",");
                    }
                    // Remove trailing comma
                    sb.setLength(sb.length() - 1);
                    item.setSip_dates(sb.toString());
                }
            }

            return ResponseEntity.ok(frequencies);

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Operation(
            summary = "Get SIP Scheme Options",
            description = "Retrieves a list of reinvestment options (like Growth, Dividend Payout, Dividend Reinvestment) " +
                    "based on the provided scheme AMFI code.\n\n" +
                    "- If the scheme value is empty or null, returns a 400 Bad Request.\n" +
                    "- Based on the `div_reinvest_flag`, maps values:\n" +
                    "  - 'Z' → Growth\n" +
                    "  - 'N' → Dividend Payout\n" +
                    "  - 'Y' → Dividend Reinvestment\n" +
                    "  - 'X' → Both Dividend Payout and Dividend Reinvestment\n\n" +
                    "Returns a list of objects containing `code` and `desc`."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully fetched SIP scheme options",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(
                                    type = "object",
                                    example = """
                {
                    "desc": "Dividend Reinvestment",
                    "code": "Y"
                }
                """
                            ))
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - scheme is required",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error while fetching SIP scheme options",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })

    @GetMapping("/getSipSchemeOptions")
    public ResponseEntity<?> getSIPSchemeOptions(@RequestParam String scheme)
    {
        CommonPojo pojo = null;
        List<CommonPojo> masterList = new ArrayList<>();

        try
        {
            scheme = NseUtils.checkParem(scheme);
            if(NseUtils.isUrlEncoded(scheme))
            {
                scheme = URLDecoder.decode(scheme, StandardCharsets.UTF_8);
            }

            if (scheme == null || scheme.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("scheme_name is required");
            }

            List<String> schemeList = nseOnlineSchemeMasterRepository.findDistinctDivReinvestFlagForSip(scheme);

            if (schemeList != null && !schemeList.isEmpty())
            {
                for (String reinvest_tag : schemeList)
                {
                    if(reinvest_tag.equalsIgnoreCase("Z"))
                    {
                        pojo = new CommonPojo();
                        pojo.setCode(reinvest_tag);
                        pojo.setDesc("Growth");
                        masterList.add(pojo);
                    }else if(reinvest_tag.equalsIgnoreCase("N"))
                    {
                        pojo = new CommonPojo();
                        pojo.setCode(reinvest_tag);
                        pojo.setDesc("Dividend Payout");
                        masterList.add(pojo);
                    }else if(reinvest_tag.equalsIgnoreCase("Y"))
                    {
                        pojo = new CommonPojo();
                        pojo.setCode(reinvest_tag);
                        pojo.setDesc("Dividend Reinvestment");
                        masterList.add(pojo);
                    }else if(reinvest_tag.equalsIgnoreCase("X"))
                    {
                        pojo = new CommonPojo();
                        pojo.setCode("N");
                        pojo.setDesc("Dividend Payout");
                        masterList.add(pojo);

                        pojo = new CommonPojo();
                        pojo.setCode("Y");
                        pojo.setDesc("Dividend Reinvestment");
                        masterList.add(pojo);
                    }
                }

            } else
            {
                pojo = new CommonPojo();
                pojo.setCode("Z");
                pojo.setDesc("Growth");
                masterList.add(pojo);
            }
            return ResponseEntity.ok(masterList);
        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Fetch SIP Broker Codes by Folio",
            description = "This endpoint retrieves a list of **distinct broker codes** associated with a specific **SIP folio** for a given user.\n\n" +
                    "It uses the `user_id`, `client_name`, and `folio` number to query the data.\n\n" +
                    "**Use Case:**\n" +
                    "UI can use this to display or filter broker codes linked to a user’s SIP folios.\n\n" +
                    "**Inputs:**\n" +
                    "- `client_name` (String) → Name of the client (e.g., 'NSE')\n" +
                    "- `userid` (Integer) → Unique user ID\n" +
                    "- `folio` (String) → Folio number of the investment\n\n" +
                    "**Output:**\n" +
                    "- A JSON list of unique broker codes (e.g., [\"ARN-1234\", \"ARN-5678\"])\n\n" +
                    "**Note:**\n" +
                    "- Returns empty list if no matching broker codes are found.\n" +
                    "- Broker codes are filtered only for SIP transactions (not STP/SWP/manual)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved list of broker codes",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(
                                    schema = @Schema(
                                            implementation = String.class,
                                            example = "ARN-123456"
                                    )
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input - `client_name`, `userid`, and `folio` are required",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "client_name, userid, and folio are required"))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error occurred while fetching broker codes",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Error fetching SIP frequencies"))
            )
    })
    @GetMapping("/getSipFolioByBrokercode")
    public ResponseEntity<?> getSipFolioBrokercode(@RequestHeader("Authorization") String token,
                                                   @RequestParam String folio)
    {
        String userid = "";
        String client_name = "";
        try {

            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            UserDto users = userServiceClient.getUserById(Integer.valueOf(userid), token);
            client_name=users.getClient_name();

            List<String> frequencies = userServiceClient.getSipBrokercodeuser(Integer.valueOf(userid),folio,client_name,token);

            return ResponseEntity.ok(frequencies);
        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Fetch SIP Scheme Code",
            description = """
        Retrieves a SIP scheme from the NSE Scheme Master based on the scheme name, dividend option code, and AMC code.

        ### Logic Used:
        1. **If no `dividend_code` is provided or it is 'Z'**:
           - Fetch scheme with reinvest flag 'Z'.
        2. **If `dividend_code` is provided but not matched in step 1**:
           - Fetch scheme with the given `dividend_code`.
        3. **If still not found**:
           - Try fetching with reinvest flag 'X' as fallback.

        ### Parameters:
        - `scheme`: Name of the mutual fund scheme.
        - `dividend_code`: Code representing dividend type (`Z`, `N`, `Y`, `X`).
        - `amc_code`: Code of the Asset Management Company.

        ### Response:
        - Returns a single matching scheme if found.
        - If no match is found, returns 404 with a message.
        - If an error occurs, returns 500 with a generic error message.

        ### Reinvestment Flag Legend:
        - `Z`: Growth
        - `N`: Dividend Payout
        - `Y`: Dividend Reinvestment
        - `X`: Any other/fallback
        """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully fetched matching SIP scheme",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NseOnlineSchemeMaster.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing or invalid parameters",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "scheme_name is required"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No matching SIP scheme found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "No matching SIP scheme found"))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error occurred while fetching SIP scheme",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Error fetching SIP scheme"))
            )
    })
    @GetMapping("/getSipSchemecode")
    public ResponseEntity<?> getSipSchemecode(@RequestParam String scheme,
                                              @RequestParam String dividend_code,
                                              @RequestParam String amc_name)
    {
        try
        {
            NseOnlineSchemeMaster schemeMaster = null;
            if(NseUtils.isUrlEncoded(scheme))
            {
                scheme = URLDecoder.decode(scheme, StandardCharsets.UTF_8);
            }

            scheme = NseUtils.checkParem(scheme);
            dividend_code = NseUtils.checkParem(dividend_code);
            amc_name = NseUtils.checkParem(amc_name);

            if(StringHelper.isEmpty(dividend_code))
            {
                dividend_code = "Z";
            }
            List<NseOnlineSchemeMaster> list = null;

            if(amc_name.equalsIgnoreCase("all"))
            {
                list =  nseOnlineSchemeMasterRepository.findBySchemeNameAndReinvestFlag(scheme, dividend_code);
            }else
            {
                list = nseOnlineSchemeMasterRepository.findBySchemeNameAndAmcNameAndReinvestFlag(scheme, amc_name, dividend_code);
            }

            if (!list.isEmpty())
            {
                schemeMaster = list.get(0);
            }

            if (schemeMaster == null)
            {
                dividend_code = "X";

                list = nseOnlineSchemeMasterRepository.findBySchemeNameAndAmcNameAndReinvestFlag(scheme, amc_name, dividend_code);

                if (!list.isEmpty())
                {
                    schemeMaster = list.get(0);
                }
            }

            if (schemeMaster == null)
            {
                return NseUtils.commonResponse("No matching SIP scheme found",HttpStatus.BAD_REQUEST);
            }

            return ResponseEntity.ok(schemeMaster);

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Validate SIP Amount Parameters",
            description = "This endpoint validates and retrieves SIP details from the NSE SIP master based on the following input parameters:\n\n" +
                    "- AMC Code (`amc_code`): The Asset Management Company code.\n" +
                    "- Scheme Code (`scheme_code`): The scheme identifier for the mutual fund.\n" +
                    "- SIP Frequency (`sip_frequency`): The frequency at which SIPs are to be executed (e.g., Monthly, Quarterly).\n\n" +
                    "Returns SIP constraints such as minimum amount, maximum amount, multiplier, etc., based on the given inputs."
    )

    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "SIP scheme data retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = NseOnlineSipStpSwpMaster.class)))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request – Required parameters are missing or invalid",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "scheme_code, amc_code, and sip_frequency are required"))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error – Could not fetch SIP scheme data",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Error fetching SIP scheme"))
            )
    })

    @GetMapping("/validateSipAmount")
    public ResponseEntity<?> validateSipAmount(@RequestParam String scheme_code,
                                               @RequestParam String sip_frequency,
                                               @RequestParam String amc_code) {
        try {

            if(scheme_code == null){scheme_code = "";}
            if(amc_code == null){amc_code = "";}
            if(sip_frequency == null){sip_frequency = "";}

            scheme_code = scheme_code.trim();
            amc_code = amc_code.trim();
            sip_frequency = sip_frequency.trim();

            List<NseOnlineSipStpSwpMaster> nse= nseOnlineSipStpSwpMasterRepository.findByAmcNameAndSchemeCodeAndFrequency(amc_code, scheme_code, sip_frequency);
            return ResponseEntity.ok(nse);

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

            //Redemption


    @Operation(
            summary = "Get all Redemption AMC details",
            description = """
        Retrieves all redemption-eligible AMC scheme details for the logged-in user.
        
        - Extracts user ID from the Authorization token.
        - Calls the User Service to fetch client details and AMC scheme data.
        - Returns a list of portfolio schemes filtered for redemption eligibility.
        
        Possible errors include invalid tokens, user not found, or no AMC details found.
        """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved list of AMC redemption schemes",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = UsersPortfolioSchemewiseDto.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request – Required parameters are missing or invalid",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Invalid request for AMC details.")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No AMC details found for the given user",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "No AMC details found for the given user.")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Error fetching AMC codes and names.")
                    )
            )
    })
    @GetMapping("/getRedemptionAmc")
    public ResponseEntity<?> getRedemptionAmc(@RequestHeader("Authorization") String token)
    {
        String userid = "";
        String client_name = "";
        try {
            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            UserDto users = userServiceClient.getUserById(Integer.valueOf(userid), token);
            client_name = users.getClient_name();

            List<UsersPortfolioSchemewiseDto> amcList = null;
            try
            {
                amcList = userServiceClient.getAllRedemptionAmcDetails(Integer.valueOf(userid), client_name,token);
            } catch (FeignException e)
            {
                if (e.status() == 400)
                {
                    return NseUtils.commonResponse("Invalid request for AMC details.", HttpStatus.BAD_REQUEST);
                } else if (e.status() == 404) {
                    return NseUtils.commonResponse("No AMC details found for the given user.", HttpStatus.BAD_REQUEST);
                } else {
                    return NseUtils.commonResponse("Error occurred while fetching AMC details.", HttpStatus.BAD_REQUEST);
                }
            }

            return ResponseEntity.ok(amcList);
        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Operation(
            summary = "Fetch distinct redemption schemes for user",
            description = "Returns a list of unique scheme codes associated with the user, client name, and AMC code, excluding manually registered ones."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Schemes retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Scheme Response Example",
                                    value = "[\n" +
                                            "  {\n" +
                                            "    \"scheme_name\": \"HDFC Overnight Fund - Growth Option\",\n" +
                                            "    \"scheme_category\": \"Debt: Overnight\",\n" +
                                            "    \"amc_name\": \"HDFC Mutual Fund\",\n" +
                                            "    \"amc_code\": \"HDFCMUTUALFUND_MF\",\n" +
                                            "    \"logo\": \"http://localhost:8084/images/amc-logo/hdfc.png\"\n" +
                                            "  }\n" +
                                            "]"
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "No AMC details found",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content)
    })
    @GetMapping("/getRedemptionFromScheme")
    public ResponseEntity<?> getRedemptionFromScheme(@RequestHeader("Authorization") String token,
                                                     @RequestParam String amc_code,
                                                     @RequestParam String iin_number)
    {
        String userid = "";
        String client_name = "";
        try {
            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            UserDto users = userServiceClient.getUserById(Integer.valueOf(userid), token);
            client_name = users.getClient_name();


            if(userid == null){userid = "";};
            if(client_name == null){client_name = "";};
            if(amc_code == null){amc_code = "";};
            if(iin_number == null){iin_number = "";};

            userid = userid.trim();
            client_name = client_name.trim();
            amc_code = amc_code.trim();
            iin_number = iin_number.trim();

            String tax_status_code = "";
            String holding_nature_code = "";
            String joint_holder_pan1 = "";
            String joint_holder_pan2 = "";

            UserDto user = null;
            try
            {
                user = userServiceClient.getUserDetailsByID(client_name, Integer.valueOf(userid),token);
            } catch (FeignException e)
            {
                if (e.status() == 400)
                {
                    return NseUtils.commonResponse("Invalid request for AMC details.", HttpStatus.BAD_REQUEST);
                } else if (e.status() == 404)
                {
                    return NseUtils.commonResponse("No AMC details found for the given user.", HttpStatus.BAD_REQUEST);
                } else
                {
                    return NseUtils.commonResponse("Error occurred while fetching AMC details.", HttpStatus.BAD_REQUEST);
                }
            }

            List<String> schemeCodeList = new ArrayList<>();
            List<SchemePojo> masterList = null;

            if(user != null)
            {
                String iin = user.getNse_iin_number();
                if(iin.equalsIgnoreCase(iin_number))
            {
                tax_status_code = user.getTax_status_code();
                holding_nature_code = user.getHolding_nature_code();
                joint_holder_pan1 = user.getJoint_holder_pan1();
                joint_holder_pan2 = user.getJoint_holder_pan2();
                if(tax_status_code == null){tax_status_code = "";}
                if(holding_nature_code == null){holding_nature_code = "";}
                if(joint_holder_pan1 == null){joint_holder_pan1 = "";}
                if(joint_holder_pan2 == null){joint_holder_pan2 = "";}
            } else
            {

                UserBseNseDto nse = null;
                try
                {
                    nse = userServiceClient.getUserBseNseDetailsByIinNumber(client_name,iin_number,token);
                } catch (FeignException e)
                {
                    if (e.status() == 400)
                    {
                        return NseUtils.commonResponse("Invalid request for AMC details.", HttpStatus.BAD_REQUEST);
                    } else if (e.status() == 404)
                    {
                        return NseUtils.commonResponse("No AMC details found for the given user.", HttpStatus.BAD_REQUEST);
                    } else {
                        return NseUtils.commonResponse("Error occurred while fetching AMC details.", HttpStatus.BAD_REQUEST);
                    }
                }
                tax_status_code = nse.getTax_status_code();
                holding_nature_code = nse.getHolding_nature_code();
                joint_holder_pan1 = nse.getJoint_holder_pan1();
                joint_holder_pan2 = nse.getJoint_holder_pan2();
                if(tax_status_code == null){tax_status_code = "";}
                if(holding_nature_code == null){holding_nature_code = "";}
                if(joint_holder_pan1 == null){joint_holder_pan1 = "";}
                if(joint_holder_pan2 == null){joint_holder_pan2 = "";}
            }
        }
            String rta_name = NseUtils.getRTAName(amc_code);

            if(StringHelper.isNotEmpty(rta_name) && rta_name.equalsIgnoreCase("CAMS"))
            {

               List<String>  dto = null;

                try {
                    dto = userServiceClient.getRedemptionSchemesNews(Integer.valueOf(userid),client_name,amc_code,token);
                } catch (FeignException e) {
                    if (e.status() == 400) {
                        return NseUtils.commonResponse("Invalid request for AMC details.", HttpStatus.BAD_REQUEST);
                    } else if (e.status() == 404) {
                        return NseUtils.commonResponse("No AMC details found for the given user.", HttpStatus.BAD_REQUEST);
                    } else {
                        return NseUtils.commonResponse("Error occurred while fetching AMC details.", HttpStatus.BAD_REQUEST);
                    }
                }


                List<String> prodcodeList = dto;

                if(prodcodeList == null || prodcodeList.size() == 0)
                {
                    List<String> amfi = amfiServiceClient.getschemeCamsProductCodesByCompanys(amc_code,token);

                    System.out.println("amfi = " + amfi);

                    prodcodeList = amfi;

                    if(prodcodeList != null && prodcodeList.size() > 0)
                    {
                        String prodcode = String.join("", prodcodeList);
                        prodcodeList = Arrays.asList(prodcode.split(","));

                        prodcodeList = prodcodeList.stream().filter(item-> !item.trim().isEmpty()).collect(Collectors.toList());
                        HashSet<Object> seen = new HashSet<>();
                        prodcodeList.removeIf(c -> !seen.add(Arrays.asList(c)));
                    }
                }

                List<InvestorMasterCamsDto> cams = null;

                try
                {
                    cams = userServiceClient.getProductCode(Integer.valueOf(userid),client_name,prodcodeList,token);
                } catch (FeignException e) {
                    if (e.status() == 400) {
                        return NseUtils.commonResponse("Invalid request for AMC details.", HttpStatus.BAD_REQUEST);
                    } else if (e.status() == 404) {
                        return NseUtils.commonResponse("No AMC details found for the given user.", HttpStatus.BAD_REQUEST);
                    } else {
                        return NseUtils.commonResponse("Error occurred while fetching AMC details.", HttpStatus.BAD_REQUEST);
                    }
                }
                List<InvestorMasterCamsDto> camsList = cams;
                if(camsList.size() > 0)
                {
                    for (InvestorMasterCamsDto camsScheme : camsList)
                    {
                        String holding = camsScheme.getHolding_na();
                        String joint1_pan = camsScheme.getJoint1_pan();
                        String joint2_pan = camsScheme.getJoint2_pan();
                        String bank_acc_type = camsScheme.getAc_type();
                        if(holding == null){holding = "";}
                        if(joint1_pan == null){joint1_pan = "";}
                        if(joint2_pan == null){joint2_pan = "";}
                        if(bank_acc_type == null){bank_acc_type = "";}

                        if(tax_status_code.equalsIgnoreCase("01"))
                        {
                            if(holding_nature_code.equalsIgnoreCase("SI"))
                            {
                                if(holding.equalsIgnoreCase("SI"))
                                {
                                    schemeCodeList.add(camsScheme.getProduct());
                                }
                            }else if(holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES"))
                            {
                                if(holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES"))
                                {
                                    if(joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan))
                                    {
                                        schemeCodeList.add(camsScheme.getProduct());
                                    }
                                }
                            }else if(holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO"))
                            {
                                if(joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan))
                                {
                                    schemeCodeList.add(camsScheme.getProduct());
                                }
                            }else
                            {

                            }
                        }
                        else if(tax_status_code.equalsIgnoreCase("11") || tax_status_code.equalsIgnoreCase("21"))
                        {
                            if(tax_status_code.equalsIgnoreCase("11") && bank_acc_type.equalsIgnoreCase("NRO"))
                            {
                                if(holding_nature_code.equalsIgnoreCase("SI"))
                                {
                                    if(holding.equalsIgnoreCase("SI"))
                                    {
                                        schemeCodeList.add(camsScheme.getProduct());
                                    }
                                }else if(holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES"))
                                {
                                    if(holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES"))
                                    {
                                        if(joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan))
                                        {
                                            schemeCodeList.add(camsScheme.getProduct());
                                        }
                                    }
                                }else if(holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO"))
                                {
                                    if(joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan))
                                    {
                                        schemeCodeList.add(camsScheme.getProduct());
                                    }
                                }else
                                {

                                }
                            }else if(tax_status_code.equalsIgnoreCase("21") && bank_acc_type.equalsIgnoreCase("NRE"))
                            {
                                if(holding_nature_code.equalsIgnoreCase("SI"))
                                {
                                    if(holding.equalsIgnoreCase("SI"))
                                    {
                                        schemeCodeList.add(camsScheme.getProduct());
                                    }
                                }else if(holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES"))
                                {
                                    if(holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES"))
                                    {
                                        if(joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan))
                                        {
                                            schemeCodeList.add(camsScheme.getProduct());
                                        }
                                    }
                                }else if(holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO"))
                                {
                                    if(joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan))
                                    {
                                        schemeCodeList.add(camsScheme.getProduct());
                                    }
                                }else
                                {

                                }
                            }else
                            {

                            }

                        }
                        else
                        {
                            schemeCodeList.add(camsScheme.getProduct());
                        }

                    }
                }
            }

            if(StringHelper.isNotEmpty(rta_name) && rta_name.equalsIgnoreCase("Karvy"))
            {
                List<InvestorMasterKarvyDto> karvy = null;

                try {
                    karvy = userServiceClient.getRedemptionKarvy(Integer.valueOf(userid),client_name,amc_code,token);
                } catch (FeignException e) {
                    if (e.status() == 400) {
                        return NseUtils.commonResponse("No record found for the given Client Name.", HttpStatus.BAD_REQUEST);
                    } else if (e.status() == 404) {
                        return NseUtils.commonResponse("User not found.", HttpStatus.NOT_FOUND);
                    } else {
                        return NseUtils.commonResponse("Downstream service error.", HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }

                List<InvestorMasterKarvyDto> karvyList = karvy;
                if(karvyList.size() > 0)
                {
                    for (InvestorMasterKarvyDto karvyScheme : karvyList)
                    {

                        String holding = karvyScheme.getMode_of_holding();
                        String pan2 = karvyScheme.getPan2();
                        String pan3 = karvyScheme.getPan3();
                        String bank_acc_type = karvyScheme.getAccount_type();
                        if(holding == null){holding = "";}
                        if(pan2 == null){pan2 = "";}
                        if(pan3 == null){pan3 = "";}
                        if(bank_acc_type == null){bank_acc_type = "";}

                        if(tax_status_code.equalsIgnoreCase("01"))
                        {
                            if(holding.equalsIgnoreCase("1"))
                            {
                                holding = "SI";
                            }else if(holding.equalsIgnoreCase("2") || holding.equalsIgnoreCase("J"))
                            {
                                holding = "JO";
                            }else if(holding.equalsIgnoreCase("3") || holding.equalsIgnoreCase("5"))
                            {
                                holding = "ES";
                            }else if(holding.equalsIgnoreCase("4") || holding.equalsIgnoreCase("7"))
                            {
                                holding = "AS";
                            }

                            if(holding.isEmpty())
                            {
                                String holding_des = karvyScheme.getMode_of_holding_description();
                                if(holding_des == null){holding_des = "";}

                                if(holding_des.equalsIgnoreCase("SINGLE") || holding_des.equalsIgnoreCase("SINGLY"))
                                {
                                    holding = "SI";
                                }else if(holding_des.equalsIgnoreCase("JOINT") || holding_des.equalsIgnoreCase("JOINTLY"))
                                {
                                    holding = "JO";
                                }else if(holding_des.equalsIgnoreCase("EITHER OR SURVIVOR"))
                                {
                                    holding = "ES";
                                }else if(holding_des.equalsIgnoreCase("ANYONE OR SURVIVOR"))
                                {
                                    holding = "AS";
                                }
                            }

                            if(holding_nature_code.equalsIgnoreCase("SI"))
                            {
                                if(holding.equalsIgnoreCase("SI"))
                                {
                                    schemeCodeList.add(karvyScheme.getProduct_code());
                                }
                            }else if(holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES"))
                            {
                                if(holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES"))
                                {
                                    if(joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3))
                                    {
                                        schemeCodeList.add(karvyScheme.getProduct_code());
                                    }
                                }
                            }else if(holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO"))
                            {
                                if(joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3))
                                {
                                    schemeCodeList.add(karvyScheme.getProduct_code());
                                }
                            }else
                            {

                            }
                        }
                        else if(tax_status_code.equalsIgnoreCase("11") || tax_status_code.equalsIgnoreCase("21"))
                        {
                            if(tax_status_code.equalsIgnoreCase("11") && bank_acc_type.equalsIgnoreCase("NRO"))
                            {
                                if(holding.equalsIgnoreCase("1"))
                                {
                                    holding = "SI";
                                }else if(holding.equalsIgnoreCase("2") || holding.equalsIgnoreCase("J"))
                                {
                                    holding = "JO";
                                }else if(holding.equalsIgnoreCase("3") || holding.equalsIgnoreCase("5"))
                                {
                                    holding = "ES";
                                }else if(holding.equalsIgnoreCase("4") || holding.equalsIgnoreCase("7"))
                                {
                                    holding = "AS";
                                }

                                if(holding.isEmpty())
                                {
                                    String holding_des = karvyScheme.getMode_of_holding_description();
                                    if(holding_des == null){holding_des = "";}

                                    if(holding_des.equalsIgnoreCase("SINGLE") || holding_des.equalsIgnoreCase("SINGLY"))
                                    {
                                        holding = "SI";
                                    }else if(holding_des.equalsIgnoreCase("JOINT") || holding_des.equalsIgnoreCase("JOINTLY"))
                                    {
                                        holding = "JO";
                                    }else if(holding_des.equalsIgnoreCase("EITHER OR SURVIVOR"))
                                    {
                                        holding = "ES";
                                    }else if(holding_des.equalsIgnoreCase("ANYONE OR SURVIVOR"))
                                    {
                                        holding = "AS";
                                    }
                                }

                                if(holding_nature_code.equalsIgnoreCase("SI"))
                                {
                                    if(holding.equalsIgnoreCase("SI"))
                                    {
                                        schemeCodeList.add(karvyScheme.getProduct_code());
                                    }
                                }else if(holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES"))
                                {
                                    if(holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES"))
                                    {
                                        if(joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3))
                                        {
                                            schemeCodeList.add(karvyScheme.getProduct_code());
                                        }
                                    }
                                }else if(holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO"))
                                {
                                    if(joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3))
                                    {
                                        schemeCodeList.add(karvyScheme.getProduct_code());
                                    }
                                }else
                                {

                                }
                            }else if(tax_status_code.equalsIgnoreCase("21") && bank_acc_type.equalsIgnoreCase("NRE"))
                            {
                                if(holding.equalsIgnoreCase("1"))
                                {
                                    holding = "SI";
                                }else if(holding.equalsIgnoreCase("2") || holding.equalsIgnoreCase("J"))
                                {
                                    holding = "JO";
                                }else if(holding.equalsIgnoreCase("3") || holding.equalsIgnoreCase("5"))
                                {
                                    holding = "ES";
                                }else if(holding.equalsIgnoreCase("4") || holding.equalsIgnoreCase("7"))
                                {
                                    holding = "AS";
                                }

                                if(holding.isEmpty())
                                {
                                    String holding_des = karvyScheme.getMode_of_holding_description();
                                    if(holding_des == null){holding_des = "";}

                                    if(holding_des.equalsIgnoreCase("SINGLE") || holding_des.equalsIgnoreCase("SINGLY"))
                                    {
                                        holding = "SI";
                                    }else if(holding_des.equalsIgnoreCase("JOINT") || holding_des.equalsIgnoreCase("JOINTLY"))
                                    {
                                        holding = "JO";
                                    }else if(holding_des.equalsIgnoreCase("EITHER OR SURVIVOR"))
                                    {
                                        holding = "ES";
                                    }else if(holding_des.equalsIgnoreCase("ANYONE OR SURVIVOR"))
                                    {
                                        holding = "AS";
                                    }
                                }

                                if(holding_nature_code.equalsIgnoreCase("SI"))
                                {
                                    if(holding.equalsIgnoreCase("SI"))
                                    {
                                        schemeCodeList.add(karvyScheme.getProduct_code());
                                    }
                                }else if(holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES"))
                                {
                                    if(holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES"))
                                    {
                                        if(joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3))
                                        {
                                            schemeCodeList.add(karvyScheme.getProduct_code());
                                        }
                                    }
                                }else if(holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO"))
                                {
                                    if(joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3))
                                    {
                                        schemeCodeList.add(karvyScheme.getProduct_code());
                                    }
                                }else
                                {

                                }
                            }else
                            {

                            }
                        }
                        else
                        {
                            schemeCodeList.add(karvyScheme.getProduct_code());
                        }

                    }
                }
            }

            List schemeCodeListNew = new ArrayList<String>(new LinkedHashSet<String>(schemeCodeList));

            if(schemeCodeListNew.size() > 0 && schemeCodeListNew !=null)
            {

                List<String> list = null;
                try {
                    list = userServiceClient.getRedemptionPortfolio(Integer.valueOf(userid), client_name, amc_code, schemeCodeListNew,token);
                } catch (FeignException e) {
                    if (e.status() == 400) {
                        return NseUtils.commonResponse("Invalid request for AMC details.", HttpStatus.BAD_REQUEST);
                    } else if (e.status() == 404) {
                        return NseUtils.commonResponse("No AMC details found for the given user.", HttpStatus.BAD_REQUEST);
                    } else {
                        return NseUtils.commonResponse("Error occurred while fetching AMC details.", HttpStatus.BAD_REQUEST);
                    }
                }

                List<String> list1 = list;

                if(list1 != null && list1.size() > 0)
                {
                    List<Object[]> userScheme = nseOnlineSchemeMasterRepository.findDistinctSchemeNamesForRedemption(list1);

                    if(userScheme != null && userScheme.size() > 0)
                    {
                        masterList = userScheme.stream().map(row ->
                        {
                            String scheme = (String) row[0];
                            String category = (String) row[1];
                            String amcCode = (String) row[2];
                            String amcName = (String) row[3];
                            String schemeCode = (String) row[4];
                            String logo = amcLogoPath + NseUtils.getLogoByAmcNameOrSchemeName(scheme);
                            return new SchemePojo(scheme, category, amcCode, amcName,schemeCode, logo);
                        }).collect(Collectors.toList());
                    }
                }
            }
            return ResponseEntity.ok(masterList);
        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Fetch eligible scheme holding units",
            description = """
        Retrieves AMC scheme holding details eligible for redemption for the logged-in user.
        
        Steps performed:
        - Extracts user ID from JWT token.
        - Calls User Service for client info.
        - Retrieves redemption-eligible scheme data based on folio and scheme name.
        
        Notes:
        - ELSS schemes calculate value from load-free units.
        - Other schemes return total units and current value directly.
        
        Errors handled:
        - Invalid token or missing user.
        - User not mapped with any AMC details.
        - Internal errors during data fetching.
        """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved scheme holding details",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NseSchemeDetailsResponse.class),
                            examples = @ExampleObject(value = """
            {
              "user_id": 655178,
              "amc_code": "H",
              "amc_name": "HDFC Mutual Fund",
              "folio_no": null,
              "registrar": "cams",
              "scheme_category": "Debt: Overnight",
              "scheme_code": "H57N",
              "dividend_option": "",
              "scheme_option_code": "",
              "scheme_amfi_code": "101996",
              "scheme_name": null,
              "total_units": 0.028,
              "load_free_units": null,
              "current_value": 107.0
            }
            """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request – Required parameters are missing or invalid",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Invalid request for AMC details.")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No AMC details found for the given user",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "No AMC details found for the given user.")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error occurred while processing",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Error fetching AMC codes and names.")
                    )
            )
    })

    @GetMapping("/getSchemeHoldingUnits")
    public ResponseEntity<?> getSchemeHoldingUnits(@RequestHeader("Authorization") String token,
                                                   @RequestParam String folio_no,
                                                   @RequestParam String scheme_name)
    {
        String userid = "";
        String client_name = "";
        try {
            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);

            UserDto users = userServiceClient.getUserById(Integer.valueOf(userid), token);
            client_name = users.getClient_name();

            if(NseUtils.isUrlEncoded(scheme_name))
            {
                scheme_name = URLDecoder.decode(scheme_name, StandardCharsets.UTF_8);
            }

            NseSchemeDetailsResponse result = new NseSchemeDetailsResponse();

            String scheme_registrar = "";
            String amc_code = "";
            String amc_name = "";
            String scheme_code = "";
            String scheme_amfi_code = "";
            String dividend_option = "";
            String scheme_option_code = "";
            String scheme_category = "";
            Double total_units = 0.0;
            Double current_value = 0.0;
            Double latest_nav = 0.0;
            DecimalFormat unit_decimal1 = new DecimalFormat("0.00");

            List<UsersPortfolioSchemewiseDto> scheme_list = userServiceClient.getRedemptionHoldingUnits(Integer.valueOf(userid), client_name, folio_no, scheme_name,token);
            List<UsersPortfolioSchemewiseDto> scheme_list1 = scheme_list;

            if(scheme_list != null && scheme_list.size() > 0)
            {
                scheme_category = scheme_list.get(0).getScheme_category();

                if(scheme_category.equalsIgnoreCase("Equity: ELSS"))
                {
                    scheme_registrar = scheme_list.get(0).getRegistrar();
                    scheme_code = scheme_list.get(0).getScheme_code();
                    latest_nav = scheme_list.get(0).getLatest_nav();

                    amc_code = scheme_list.get(0).getAmc_code();
                    amc_name = scheme_list.get(0).getAmc_name();
                    scheme_amfi_code = scheme_list.get(0).getScheme_amfi_code();
                    dividend_option = scheme_list.get(0).getDividend_option();
                    scheme_option_code = scheme_list.get(0).getScheme_option_code();
                    //total_units = scheme_list.get(0).getTotal_units();
                    //current_value = scheme_list.get(0).getCurrent_value();
                    Double load_free_units = scheme_list.get(0).getLoad_free_units();

                    current_value = load_free_units * latest_nav;
                    current_value = Double.parseDouble(unit_decimal1.format(current_value));

                    result.setTotal_units(load_free_units);
                    result.setCurrent_value(current_value);
                    result.setRegistrar(scheme_registrar);
                    result.setAmc_code(amc_code);
                    result.setAmc_name(amc_name);
                    result.setUser_id(Integer.valueOf(userid));
                    result.setScheme_code(scheme_code);
                    result.setScheme_category(scheme_category);
                    result.setScheme_amfi_code(scheme_amfi_code);
                    result.setDividend_option(dividend_option);
                    result.setScheme_option_code(scheme_option_code);
                    result.setLoad_free_units(load_free_units);

                }else
                {
                    scheme_registrar = scheme_list.get(0).getRegistrar();
                    amc_code = scheme_list.get(0).getAmc_code();
                    amc_name = scheme_list.get(0).getAmc_name();
                    scheme_amfi_code = scheme_list.get(0).getScheme_amfi_code();
                    scheme_code = scheme_list.get(0).getScheme_code();
                    dividend_option = scheme_list.get(0).getDividend_option();
                    scheme_option_code = scheme_list.get(0).getScheme_option_code();
                    total_units = scheme_list.get(0).getTotal_units();
                    current_value = scheme_list.get(0).getCurrent_value();

                    result.setTotal_units(total_units);
                    result.setCurrent_value(current_value);
                    result.setRegistrar(scheme_registrar);
                    result.setAmc_code(amc_code);
                    result.setAmc_name(amc_name);
                    result.setUser_id(Integer.valueOf(userid));
                    result.setScheme_code(scheme_code);
                    result.setScheme_category(scheme_category);
                    result.setScheme_amfi_code(scheme_amfi_code);
                    result.setDividend_option(dividend_option);
                    result.setScheme_option_code(scheme_option_code);
                }
            }


            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get Redemption Scheme Options",
            description ="   Retrieves a list of dividend reinvestment flags (DivReinvestFlag) for a given scheme name,\n" +
                    "        which are eligible for redemption for the logged-in user."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved dividend reinvestment flags",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = String.class)),
                            examples = @ExampleObject(value = """
               [
                   {
                      "desc": "Growth",
                       "code": "Z"
                    }
                ]
            """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request – Required parameters are missing or invalid",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Invalid request for AMC details.")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No AMC details found for the given user",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "No AMC details found for the given user.")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error occurred while processing",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Error fetching AMC codes and names.")
                    )
            )
    })
    @GetMapping("/getRedemptionSchemeOptions")
    public ResponseEntity<?> getRedemptionSchemeOptions(@RequestHeader("Authorization") String token,
                                                        @RequestParam String schemeName)
    {
        CommonPojo pojo = null;
        List<CommonPojo> masterList = new ArrayList<>();
        try {
            if(NseUtils.isUrlEncoded(schemeName))
            {
                schemeName = URLDecoder.decode(schemeName, StandardCharsets.UTF_8);
            }

                List<String> schemeList = nseOnlineSchemeMasterRepository.findDistinctDivReinvestFlagBySchemeNameForRedemption(schemeName);

                if (schemeList != null && !schemeList.isEmpty())
                {

                    for (String reinvest_tag : schemeList)
                    {
                        if (reinvest_tag.equalsIgnoreCase("Z"))
                        {
                            pojo = new CommonPojo();
                            pojo.setCode(reinvest_tag);
                            pojo.setDesc("Growth");
                            masterList.add(pojo);
                        } else if (reinvest_tag.equalsIgnoreCase("N"))
                        {
                            pojo = new CommonPojo();
                            pojo.setCode(reinvest_tag);
                            pojo.setDesc("Dividend Payout");
                            masterList.add(pojo);
                        } else if (reinvest_tag.equalsIgnoreCase("Y"))
                        {
                            pojo = new CommonPojo();
                            pojo.setCode(reinvest_tag);
                            pojo.setDesc("Dividend Reinvestment");
                            masterList.add(pojo);
                        } else if (reinvest_tag.equalsIgnoreCase("X"))
                        {
                            pojo = new CommonPojo();
                            pojo.setCode("N");
                            pojo.setDesc("Dividend Payout");
                            masterList.add(pojo);

                            pojo = new CommonPojo();
                            pojo.setCode("Y");
                            pojo.setDesc("Dividend Reinvestment");
                            masterList.add(pojo);
                        }
                    }

                } else
                {
                    pojo = new CommonPojo();
                    pojo.setCode("Z");
                    pojo.setDesc("Growth");
                    masterList.add(pojo);
                }

                return ResponseEntity.ok(masterList);

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    //SWITCH

    @Operation(
            summary = "Get Switchable Scheme Options by AMC",
            description = "Retrieves a list of scheme names under the specified AMC (Asset Management Company) code " +
                    "that are eligible for switch/redemption for the logged-in user."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved scheme options",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = String.class)),
                            examples = @ExampleObject(value = """
                [
                    "HDFC Value Fund - Growth Plan",
                    "HDFC Arbitrage Fund - Wholesale Growth Option",
                    "HDFC Balanced Advantage Fund - Growth Plan",
                    "HDFC Corporate Bond Fund - IDCW Option",
                    "HDFC Banking and Financial Services Fund - Growth Option"
                ]
            """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request – Required parameters are missing or invalid",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Invalid request for AMC details.")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No AMC details found for the given user",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "No AMC details found for the given user.")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error occurred while processing",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Error fetching AMC codes and names.")
                    )
            )
    })
    @GetMapping("/getSwitchSchemeByAmc")
    public ResponseEntity<?> getSwitchSchemeByAmc(@RequestHeader("Authorization") String token,
                                                  @RequestParam String amc)
    {
        try {

            List<SchemePojo> schemeList =  new ArrayList<>();
            List<Object[]> filteredSchemes = nseOnlineSchemeMasterRepository.findDistinctSchemeNameByAmcCodeAndMinAmount(amc);

            if (filteredSchemes != null && !filteredSchemes.isEmpty())
            {
                schemeList = filteredSchemes.stream().map(row ->
                {
                    String scheme = (String) row[0];
                    String category = (String) row[1];
                    String amc_code = (String) row[2];
                    String amc_name = (String) row[3];
                    String scheme_code = (String) row[4];
                    String logo = amcLogoPath + NseUtils.getLogoByAmcNameOrSchemeName(scheme);
                    return new SchemePojo(scheme, category, amc_code, amc_name,scheme_code, logo);
                }).collect(Collectors.toList());
            }

            return ResponseEntity.ok(schemeList);
        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get Switch Scheme Options",
            description = "Returns a list of DivReinvestFlag values for the specified scheme name. " +
                    "These flags indicate whether the scheme is eligible for switch/redemption for the logged-in user."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved dividend reinvestment flags",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = String.class)),
                            examples = @ExampleObject(value = """
               [
                   {
                      "desc": "Growth",
                       "code": "Z"
                    }
                ]
            """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request – Required parameters are missing or invalid",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Invalid request for AMC details.")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No AMC details found for the given user",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "No AMC details found for the given user.")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error occurred while processing",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Error fetching AMC codes and names.")
                    )
            )
    })
    @GetMapping("/getSwitchSchemeOptions")
    public ResponseEntity<?> getSwitchSchemeOptions(@RequestHeader("Authorization") String token,
                                                    @RequestParam String scheme)
    {
        CommonPojo pojo = null;
        List<CommonPojo> masterList = new ArrayList<>();
        try
        {
            if(NseUtils.isUrlEncoded(scheme))
            {
                scheme = URLDecoder.decode(scheme, StandardCharsets.UTF_8);
            }

            List<String> schemeList = nseOnlineSchemeMasterRepository.findDivReinvestFlagsForSwitchAllowed(scheme);

            if (schemeList != null && !schemeList.isEmpty())
            {

                for (String reinvest_tag : schemeList) {
                    if (reinvest_tag.equalsIgnoreCase("Z"))
                    {
                        pojo = new CommonPojo();
                        pojo.setCode(reinvest_tag);
                        pojo.setDesc("Growth");
                        masterList.add(pojo);
                    } else if (reinvest_tag.equalsIgnoreCase("N"))
                    {
                        pojo = new CommonPojo();
                        pojo.setCode(reinvest_tag);
                        pojo.setDesc("Dividend Payout");
                        masterList.add(pojo);
                    } else if (reinvest_tag.equalsIgnoreCase("Y"))
                    {
                        pojo = new CommonPojo();
                        pojo.setCode(reinvest_tag);
                        pojo.setDesc("Dividend Reinvestment");
                        masterList.add(pojo);
                    } else if (reinvest_tag.equalsIgnoreCase("X"))
                    {
                        pojo = new CommonPojo();
                        pojo.setCode("N");
                        pojo.setDesc("Dividend Payout");
                        masterList.add(pojo);

                        pojo = new CommonPojo();
                        pojo.setCode("Y");
                        pojo.setDesc("Dividend Reinvestment");
                        masterList.add(pojo);
                    }
                }

            } else {
                pojo = new CommonPojo();
                pojo.setCode("Z");
                pojo.setDesc("Growth");
                masterList.add(pojo);
            }
            return ResponseEntity.ok(masterList);

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Operation(
            summary = "Get Switchable Scheme Options by AMC",
            description = "Retrieves a list of scheme names under the specified AMC (Asset Management Company) code " +
                    "that are eligible for NFO switch for the logged-in user."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved scheme options",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = String.class)),
                            examples = @ExampleObject(value = """
                [
                    "HDFC Value Fund - Growth Plan",
                    "HDFC Arbitrage Fund - Wholesale Growth Option",
                    "HDFC Balanced Advantage Fund - Growth Plan",
                    "HDFC Corporate Bond Fund - IDCW Option",
                    "HDFC Banking and Financial Services Fund - Growth Option"
                ]
            """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request – Required parameters are missing or invalid",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Invalid request for AMC details.")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No AMC details found for the given user",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "No AMC details found for the given user.")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error occurred while processing",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Error fetching AMC codes and names.")
                    )
            )
    })
    @GetMapping("/getNFOSwitchSchemeByAmc")
    public ResponseEntity<?> getNFOSwitchSchemeByAmc(@RequestHeader("Authorization") String token,
                                                     @RequestParam String amc)
    {
        try {
        List<SchemePojo> schemeList =  new ArrayList<>();
            List<Object[]> filteredSchemes = nseOnlineSchemeMasterRepository.findSchemesByAmcCodeWithDateRangeAndSettlementMF(amc);

            System.out.println("filteredSchemes = " +  filteredSchemes);
            if (filteredSchemes != null && !filteredSchemes.isEmpty())
            {
                schemeList = filteredSchemes.stream().map(row ->
                {
                    String scheme = (String) row[0];
                    String category = (String) row[1];
                    String amc_code = (String) row[2];
                    String amc_name = (String) row[3];
                    String scheme_code = (String) row[4];
                    String logo = amcLogoPath + NseUtils.getLogoByAmcNameOrSchemeName(scheme);
                    return new SchemePojo(scheme, category, amc_code, amc_name,scheme_code, logo);
                }).collect(Collectors.toList());
            }

            return ResponseEntity.ok(schemeList);
        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get Scheme Folio Numbers",
            description = "Fetches a list of folio numbers associated with a given scheme for the logged-in user. " +
                    "This helps determine the folios under which investments have been made for the specified scheme and IIN number."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved folio numbers",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = String.class)),
                            examples = @ExampleObject(value = """
                [
                    "27947163/73",
                    "27947174/40",
                    "27947175/37",
                    "27966737/36",
                    "27966738/33",
                    "27966739/30",
                    "28647044/92",
                    "28647046/86",
                    "28676268/11",
                    "28676271/02"
                ]
            """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request – Required parameters are missing or invalid",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Invalid request. Please check scheme name and IIN number.")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User or folio details not found for the given parameters",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "No folio numbers found for the given scheme and IIN.")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error occurred while processing",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Error fetching folio numbers due to server error.")
                    )
            )
    })
    @GetMapping("/getSchemeFolioNumbers")
    public ResponseEntity<?> getSchemeFolioNumbers(@RequestHeader("Authorization") String token,
                                                   @RequestParam String scheme_name,
                                                   @RequestParam String iin_number)
    {
        try
        {
            if(NseUtils.isUrlEncoded(scheme_name))
            {
                scheme_name = URLDecoder.decode(scheme_name, StandardCharsets.UTF_8);
            }

            String userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            UserDto users = userServiceClient.getUserById(Integer.valueOf(userid), token);
            String client_name = users.getClient_name();
            String usertypeid = String.valueOf(users.getType_id());

            if(userid == null){userid = "";};
            if(client_name == null){client_name = "";};
            if(scheme_name == null){scheme_name = "";};
            if(iin_number == null){iin_number = "";};

            userid = userid.trim();
            client_name = client_name.trim();
            scheme_name = scheme_name.trim();
            iin_number = iin_number.trim();

            UserDto user = null;

            try
            {
                user = userServiceClient.getUserDetailsByID(client_name, Integer.valueOf(userid),token);
            }
            catch (FeignException e)
            {
                if (e.status() == 400) {
                    return NseUtils.commonResponse("Invalid request for AMC details.", HttpStatus.BAD_REQUEST);
                } else if (e.status() == 404) {
                    return NseUtils.commonResponse("No AMC details found for the given user.", HttpStatus.BAD_REQUEST);
                } else {
                    return NseUtils.commonResponse("Error occurred while fetching AMC details.", HttpStatus.BAD_REQUEST);
                }
            }
            String holding_nature_code = "";
            String tax_status_code = "";
            String joint_holder_pan1 = "";
            String joint_holder_pan2 = "";

            if(user.getNse_iin_number().equalsIgnoreCase(iin_number))
            {
                holding_nature_code = user.getHolding_nature_code();
                tax_status_code = user.getTax_status_code();
                joint_holder_pan1 = user.getJoint_holder_pan1();
                joint_holder_pan2 = user.getJoint_holder_pan2();
            }else
            {
                UserBseNseDto nse = null;

                try
                {
                    nse = userServiceClient.getUserBseNseDetailsByIinNumber(client_name, iin_number,token);
                }
                catch (FeignException.BadRequest ex)
                {
                    System.out.println("No record found for the given IIN and Client Name.");
                    nse = null;
                }

                if(nse != null){
                    holding_nature_code = nse.getHolding_nature_code();
                    tax_status_code = nse.getTax_status_code();
                    joint_holder_pan1 = nse.getJoint_holder_pan1();
                    joint_holder_pan2 = nse.getJoint_holder_pan2();
                }
            }
            if(tax_status_code == null){tax_status_code="";}
            if(holding_nature_code == null){holding_nature_code="";}
            if(joint_holder_pan1 == null){joint_holder_pan1 = "";}
            if(joint_holder_pan2 == null){joint_holder_pan2 = "";}

            List<String> folioList = nseService.getSchemeFolioNumbers(
                    client_name,
                    Integer.valueOf(userid),
                    scheme_name,
                    holding_nature_code,
                    tax_status_code,
                    joint_holder_pan1,
                    joint_holder_pan2,token
            );


            return ResponseEntity.ok(folioList);

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    //STP

    @Operation(
            summary = "Get STP Frequency Types",
            description = "Returns a list of distinct STP frequency types (e.g., MONTHLY, QUARTERLY, WEEKLY) for a given AMC and Scheme"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "STP Frequencies retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(
                                    schema = @Schema(
                                            type = "string",
                                            example = "MONTHLY"
                                    )
                            ),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = "[\"MONTHLY\", \"QUARTERLY\", \"WEEKLY\"]"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input parameters",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = "{\"error\": \"amcName or schemeName is missing or invalid\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = "{\"error\": \"An unexpected error occurred\"}"
                            )
                    )
            )
    })
    @GetMapping("/getStpSchemeFrequency")
    public ResponseEntity<?> getStpSchemeFrequency(@RequestHeader("Authorization") String token,
                                                   @RequestParam String scheme_name,
                                                   @RequestParam(required = false) String amc_name,
                                                   @RequestParam String dividend_code)
    {
        try {
            if(NseUtils.isUrlEncoded(scheme_name))
            {
                scheme_name = URLDecoder.decode(scheme_name, StandardCharsets.UTF_8);
            }

            if(scheme_name == null){scheme_name = "";}
            if(dividend_code == null){dividend_code = "";}

            scheme_name = scheme_name.trim();

            List<NseOnlineSipStpSwpMaster>  schemeList = nseOnlineSipStpSwpMasterRepository.findDistinctAstpFrequenciesByAmcNameAndSchemeName(scheme_name);

            return ResponseEntity.ok(schemeList);

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get STP Schemes by AMC",
            description = "Returns a list of scheme names eligible for Systematic Transfer Plan (STP) under the specified AMC code and category."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved list of STP-eligible scheme names",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = String.class)),
                            examples = @ExampleObject(value = """
                            [
                                "HDFC Value Fund - Growth Plan",
                                "HDFC Value Fund - IDCW Plan",
                                "HDFC Arbitrage Fund - Wholesale Growth Option",
                                "HDFC Arbitrage Fund - Wholesale Monthly IDCW Option",
                                "HDFC Arbitrage Fund - Wholesale IDCW Option",
                                "HDFC Balanced Advantage Fund - Growth Plan",
                                "HDFC Balanced Advantage Fund - IDCW Plan",
                                "HDFC Corporate Bond Fund - IDCW Option",
                                "HDFC Banking and Financial Services Fund - IDCW Option",
                                "HDFC Corporate Bond Fund - Quarterly IDCW Option",
                                "HDFC Banking and Financial Services Fund - Growth Option",
                                "HDFC Credit Risk Debt Fund - Quarterly IDCW Option",
                                "HDFC Credit Risk Debt Fund - IDCW Option"
                            ]
                        """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request – One or more required parameters (e.g., amc_code, category) are missing or invalid",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Invalid request parameters: amc_code and category are required.")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error – An error occurred while fetching the STP scheme list",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Error fetching AMC codes and names.")
                    )
            )
    })
    @GetMapping("/getSTPSchemeByAmc")
    public ResponseEntity<?> getSTPSchemeByAmc(@RequestHeader("Authorization") String token,
                                               @RequestParam String amc_code,
                                               @RequestParam String category)
    {
        try {
            List<String> schemeNames = null;
            if(category.equalsIgnoreCase("All"))
            {
               schemeNames = nseOnlineSchemeMasterRepository.findDistinctSchemeNamesForStpByAmcCode(amc_code);
            }else
            {
                schemeNames = nseOnlineSchemeMasterRepository.findDistinctSchemeNamesForStpByAmcCodeAndCategory(amc_code, category);
            }
            return ResponseEntity.ok(schemeNames);

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "getSTPSchemeOptions",
            description = "Returns a list of DivReinvestFlag values for the specified scheme name. " +
                    "These flags indicate whether the scheme is eligible for STP for the logged-in user."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved dividend reinvestment flags",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = String.class)),
                            examples = @ExampleObject(value = """
               [
                   {
                      "desc": "Growth",
                       "code": "Z"
                    }
                ]
            """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request – Required parameters are missing or invalid",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Invalid request for AMC details.")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No AMC details found for the given user",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "No AMC details found for the given user.")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error occurred while processing",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Error fetching AMC codes and names.")
                    )
            )
    })
    @GetMapping("/getSTPSchemeOptions")
    public ResponseEntity<?> getSTPSchemeOptions(@RequestHeader("Authorization") String token,
                                                 @RequestParam String scheme)
    {
        CommonPojo pojo = null;
        List<CommonPojo> masterList = new ArrayList<>();
        try {
            if(NseUtils.isUrlEncoded(scheme))
            {
                scheme = URLDecoder.decode(scheme, StandardCharsets.UTF_8);
            }

            List<String> schemeList = nseOnlineSchemeMasterRepository.findDistinctDivReinvestFlagForStp(scheme);

            if (schemeList != null && !schemeList.isEmpty()) {

                for (String reinvest_tag : schemeList)
                {
                    if (reinvest_tag.equalsIgnoreCase("Z"))
                    {
                        pojo = new CommonPojo();
                        pojo.setCode(reinvest_tag);
                        pojo.setDesc("Growth");
                        masterList.add(pojo);
                    } else if (reinvest_tag.equalsIgnoreCase("N"))
                    {
                        pojo = new CommonPojo();
                        pojo.setCode(reinvest_tag);
                        pojo.setDesc("Dividend Payout");
                        masterList.add(pojo);
                    } else if (reinvest_tag.equalsIgnoreCase("Y"))
                    {
                        pojo = new CommonPojo();
                        pojo.setCode(reinvest_tag);
                        pojo.setDesc("Dividend Reinvestment");
                        masterList.add(pojo);
                    } else if (reinvest_tag.equalsIgnoreCase("X"))
                    {
                        pojo = new CommonPojo();
                        pojo.setCode("N");
                        pojo.setDesc("Dividend Payout");
                        masterList.add(pojo);

                        pojo = new CommonPojo();
                        pojo.setCode("Y");
                        pojo.setDesc("Dividend Reinvestment");
                        masterList.add(pojo);
                    }
                }

            } else {
                pojo = new CommonPojo();
                pojo.setCode("Z");
                pojo.setDesc("Growth");
                masterList.add(pojo);
            }

            return ResponseEntity.ok(masterList);

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    //SWP

    @Operation(
            summary = "Get EUIN Codes by ARN",
            description = "Retrieves a list of EUIN (Employee Unique Identification Number) codes associated with the specified broker ARN (Advisor Registration Number). " +
                    "The EUIN is fetched for the logged-in user based on the matching broker code."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved EUIN code(s)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = String.class)),
                            examples = @ExampleObject(value = """
                [
                    "E114247"
                ]
            """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request – Missing or invalid broker code",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Invalid broker code or missing input.")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No EUIN code found for the specified broker ARN",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "No EUIN code found for the provided broker code.")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error – Failed to retrieve EUIN code",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "An error occurred while processing the request.")
                    )
            )
    })
    @GetMapping("/getEuinCodeByARN")
    public ResponseEntity<?> getEuinCodeByARN(@RequestHeader("Authorization") String token,
                                              @RequestParam String broker_code)
    {
        String userid = "";
        String clientName = "";
        try {

            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            UserDto users = userServiceClient.getUserById(Integer.valueOf(userid), token);
            clientName=users.getClient_name();

            if(broker_code == null || StringHelper.isEmpty(broker_code)){broker_code = "";}
            broker_code = broker_code.trim();

            BseNseKeyDto bsekey = userServiceClient.getByClientName(clientName,token);

            String broker_code1 = bsekey.getBrokerCode();


            if(broker_code1 == null){broker_code1 = "";};


            broker_code1 = broker_code1.trim();


            String euin_code = "";

            if(broker_code1.equalsIgnoreCase(broker_code))
            {
                euin_code = bsekey.getEuin();
            }

            List<String> euin_array = new ArrayList<String>();
            if(!euin_code.isEmpty())
            {
                euin_array = new ArrayList<String>(Arrays.asList(euin_code.split(",")));
            }

            return ResponseEntity.ok(euin_array);

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "getSTPSchemeOptions",
            description = "Returns a list of DivReinvestFlag values for the specified scheme name. " +
                    "These flags indicate whether the scheme is eligible for SWP for the logged-in user."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved dividend reinvestment flags",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = String.class)),
                            examples = @ExampleObject(value = """
               [
                   {
                      "desc": "Growth",
                       "code": "Z"
                    }
                ]
            """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request – Required parameters are missing or invalid",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Invalid request for AMC details.")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No AMC details found for the given user",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "No AMC details found for the given user.")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error occurred while processing",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Error fetching AMC codes and names.")
                    )
            )
    })
    @GetMapping("/getSwpSchemeOptions")
    public ResponseEntity<?> getSwpchemeOptions(@RequestHeader("Authorization") String token,
                                                @RequestParam String scheme)
    {
        CommonPojo pojo = null;
        List<CommonPojo> masterList = new ArrayList<>();
        try {
            if(NseUtils.isUrlEncoded(scheme))
            {
                scheme = URLDecoder.decode(scheme, StandardCharsets.UTF_8);
            }

            List<String> schemeList = nseOnlineSchemeMasterRepository.findDistinctDivReinvestFlagForSwp(scheme);

            if (schemeList != null && !schemeList.isEmpty())
            {

                for (String reinvest_tag : schemeList) {
                    if (reinvest_tag.equalsIgnoreCase("Z"))
                    {
                        pojo = new CommonPojo();
                        pojo.setCode(reinvest_tag);
                        pojo.setDesc("Growth");
                        masterList.add(pojo);
                    } else if (reinvest_tag.equalsIgnoreCase("N"))
                    {
                        pojo = new CommonPojo();
                        pojo.setCode(reinvest_tag);
                        pojo.setDesc("Dividend Payout");
                        masterList.add(pojo);
                    } else if (reinvest_tag.equalsIgnoreCase("Y"))
                    {
                        pojo = new CommonPojo();
                        pojo.setCode(reinvest_tag);
                        pojo.setDesc("Dividend Reinvestment");
                        masterList.add(pojo);
                    } else if (reinvest_tag.equalsIgnoreCase("X"))
                    {
                        pojo = new CommonPojo();
                        pojo.setCode("N");
                        pojo.setDesc("Dividend Payout");
                        masterList.add(pojo);

                        pojo = new CommonPojo();
                        pojo.setCode("Y");
                        pojo.setDesc("Dividend Reinvestment");
                        masterList.add(pojo);
                    }
                }

            } else {
                pojo = new CommonPojo();
                pojo.setCode("Z");
                pojo.setDesc("Growth");
                masterList.add(pojo);
            }

            return ResponseEntity.ok(masterList);

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR); }
    }

    @Operation(
            summary = "Get SwpSchemeHoldingUnits",
            description = "Returns portfolio metrics such as total units and current value for a given investor."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Portfolio metrics fetched successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Demo Response",
                                    summary = "Static example response",
                                    value = "[\n" +
                                            "  { \"desc\": \"total_units\", \"value\": 0.113 },\n" +
                                            "  { \"desc\": \"current_value\", \"value\": 431.16 }\n" +
                                            "]"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Invalid parameters",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"error\": \"Invalid request parameters\" }")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"error\": \"An unexpected error occurred\" }")
                    )
            )
    })
    @GetMapping("/getSwpSchemeHoldingUnits")
    public ResponseEntity<?> getSwpSchemeHoldingUnits(@RequestHeader("Authorization") String token,
                                                      @RequestParam String folio_no,
                                                      @RequestParam String scheme_name)
    {
        String userid = "";
        String client_name = "";
        String usertypeid = "";

        try {
            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);

            if(NseUtils.isUrlEncoded(scheme_name))
            {
                scheme_name = URLDecoder.decode(scheme_name, StandardCharsets.UTF_8);
            }

            UserDto users = userServiceClient.getUserById(Integer.valueOf(userid), token);
            client_name = users.getClient_name();
             usertypeid = String.valueOf(users.getType_id());

            String cams = "";
            String karvy = "";
            Double total_units = 0.0;
            Double current_value = 0.0;
            Double purprice = 0.0;
            Double latest_nav = 0.0;
            String scheme_category = "";
            AmfiSchemeMasterDTO schemeMapping = null;
            List<Double> result = new ArrayList<Double>();
            DecimalFormat unit_decimal = new DecimalFormat("0.0000");
            DecimalFormat unit_decimal1 = new DecimalFormat("0.00");
            Calendar cal = Calendar.getInstance();
            Date today = cal.getTime();



            List<AmfiSchemeMasterDTO> schemeMappingList = amfiServiceClient.findBySchemeAmfiAndActive(scheme_name,token);

            System.out.println("");

            if (schemeMappingList != null && schemeMappingList.size() > 0) {
                schemeMapping = schemeMappingList.get(0);
                scheme_category = schemeMapping.getScheme_advisorkhoj_category();
                cams = schemeMapping.getScheme_cams_productcode();
                karvy = schemeMapping.getScheme_karvy_productcode();
            }
            if (StringHelper.isNotEmpty(cams) || StringHelper.isNotEmpty(karvy)) {
                // CAMS Data Processing
                List<String> camsPositiveTransactionArrayList = new ArrayList<String>();
                List<String> camsNegativeTransactionArrayList = new ArrayList<String>();
                // Karvy Data Processing
                List<String> karvyPositiveTransactionArrayList = new ArrayList<String>();
                List<String> karvyNegativeTransactionArrayList = new ArrayList<String>();

                List<TransactionTypeDTO> transaction_type_list = userServiceClient.getAllTransactionType(token);

                if (transaction_type_list != null && transaction_type_list.size() > 0) {
                    for (TransactionTypeDTO transactionType : transaction_type_list) {
                        String registrar = transactionType.getRegistrar();
                        String positive_transaction = transactionType.getPositive_transaction();
                        String negative_transaction = transactionType.getNegative_transaction();
                        String netural_transaction = transactionType.getNeutral_transaction();

                        String[] positiveTransactionList = positive_transaction.split("[\\,]+");
                        String[] negativeTransactionList = negative_transaction.split("[\\,]+");
                        String[] neutralTransactionList = netural_transaction.split("[\\,]+");

                        if (registrar.equalsIgnoreCase("cams")) {
                            camsPositiveTransactionArrayList = Arrays.asList(positiveTransactionList);
                            camsNegativeTransactionArrayList = Arrays.asList(negativeTransactionList);
                        }
                        if (registrar.equalsIgnoreCase("karvy")) {
                            karvyPositiveTransactionArrayList = Arrays.asList(positiveTransactionList);
                            karvyNegativeTransactionArrayList = Arrays.asList(negativeTransactionList);
                        }
                    }
                }
                if (StringHelper.isNotEmpty(cams)) {
                    List<String> cams_list = new ArrayList<String>(Arrays.asList(cams.split(",")));
                    List<InvestorTransactionCamsDto> camsSchemeWiseInvestorTransactions = userServiceClient.getAllCamsTransaction(
                            Integer.valueOf(userid),
                            client_name,
                            folio_no,
                            cams_list,token
                    );

                    for (int i = 0; i < camsSchemeWiseInvestorTransactions.size(); i++) {
                        Date trxn_date = camsSchemeWiseInvestorTransactions.get(i).getTraddate();
                        String trxn_type_ = camsSchemeWiseInvestorTransactions.get(i).getTrxn_type_().trim();
                        purprice = camsSchemeWiseInvestorTransactions.get(i).getPurprice();

                        if (scheme_category.equalsIgnoreCase("Equity: ELSS")) {
                            cal = Calendar.getInstance();
                            cal.setTime(trxn_date);
                            cal.add(Calendar.YEAR, 3);
                            Date trxn_date_after3years = cal.getTime();

                            if (camsPositiveTransactionArrayList.contains(trxn_type_) && trxn_date_after3years.compareTo(today) < 0) {
                                if (total_units <= 0) {
                                    total_units = 0.0;
                                }
                                total_units = total_units + camsSchemeWiseInvestorTransactions.get(i).getUnits();
                            }
                            if (camsNegativeTransactionArrayList.contains(trxn_type_)) {
                                if (total_units <= 0) {
                                    total_units = 0.0;
                                }
                                total_units = total_units - camsSchemeWiseInvestorTransactions.get(i).getUnits();
                            }
                        } else {
                            if (camsPositiveTransactionArrayList.contains(trxn_type_)) {
                                if (total_units <= 0) {
                                    total_units = 0.0;
                                }
                                total_units = total_units + camsSchemeWiseInvestorTransactions.get(i).getUnits();
                            }
                            if (camsNegativeTransactionArrayList.contains(trxn_type_)) {
                                if (total_units <= 0) {
                                    total_units = 0.0;
                                }
                                total_units = total_units - camsSchemeWiseInvestorTransactions.get(i).getUnits();
                            }
                        }
                    }

                    total_units = Double.parseDouble(unit_decimal.format(total_units));

                    if (total_units <= 0) {
                        total_units = 0.0;
                    }
                    if (total_units > 0 && schemeMapping != null) {
                        List<AmfiLatestNavDto> latestNavList = amfiServiceClient.findByLatestNav(schemeMapping.getScheme_amfi_code(),token);
                        if (latestNavList != null && latestNavList.size() > 0) {
                            latest_nav = latestNavList.get(0).getNet_asset_value();
                        } else {
                            List<Double> nav_list = amfiServiceClient.findByMfNav(schemeMapping.getScheme_amfi_code(),token);
                        }

                        if (latest_nav > 0) {
                            current_value = total_units * latest_nav;
                            current_value = Double.parseDouble(unit_decimal1.format(current_value));
                        } else {
                            current_value = total_units * purprice;
                            current_value = Double.parseDouble(unit_decimal1.format(current_value));
                        }
                    }
                }

                if (StringHelper.isNotEmpty(karvy)) {
                    List<String> karvy_list = new ArrayList<String>(Arrays.asList(karvy.split(",")));
                    List<InvestorTransactionKarvyDto> schemeWiseInvestorTransactions = userServiceClient.getAllKarvyTransaction(Integer.valueOf(userid),
                            client_name,
                            folio_no,
                            karvy_list,token);
                    for (int i = 0; i < schemeWiseInvestorTransactions.size(); i++) {
                        Date trxn_date = schemeWiseInvestorTransactions.get(i).getTransaction_date();
                        String trxn_type_ = schemeWiseInvestorTransactions.get(i).getTransaction_description().trim();
                        purprice = schemeWiseInvestorTransactions.get(i).getPrice();

                        if (scheme_category.equalsIgnoreCase("Equity: ELSS")) {
                            cal = Calendar.getInstance();
                            cal.setTime(trxn_date);
                            cal.add(Calendar.YEAR, 3);
                            Date trxn_date_after3years = cal.getTime();

                            if (karvyPositiveTransactionArrayList.contains(trxn_type_) && trxn_date_after3years.compareTo(today) < 0) {
                                if (total_units <= 0) {
                                    total_units = 0.0;
                                }
                                total_units = total_units + schemeWiseInvestorTransactions.get(i).getUnits();
                            }
                            if (karvyNegativeTransactionArrayList.contains(trxn_type_)) {
                                if (total_units <= 0) {
                                    total_units = 0.0;
                                }
                                total_units = total_units - schemeWiseInvestorTransactions.get(i).getUnits();
                            }
                        } else {
                            if (karvyPositiveTransactionArrayList.contains(trxn_type_)) {
                                if (total_units <= 0) {
                                    total_units = 0.0;
                                }
                                total_units = total_units + schemeWiseInvestorTransactions.get(i).getUnits();
                            }
                            if (karvyNegativeTransactionArrayList.contains(trxn_type_)) {
                                if (total_units <= 0) {
                                    total_units = 0.0;
                                }
                                total_units = total_units - schemeWiseInvestorTransactions.get(i).getUnits();
                            }
                        }
                    }

                    total_units = Double.parseDouble(unit_decimal.format(total_units));

                    if (total_units <= 0) {
                        total_units = 0.0;
                    }

                    if (total_units > 0 && schemeMapping != null) {
                        List<AmfiLatestNavDto> latestNavList = amfiServiceClient.findByLatestNav(schemeMapping.getScheme_amfi_code(),token);
                        if (latestNavList != null && latestNavList.size() > 0)
                        {
                            latest_nav = latestNavList.get(0).getNet_asset_value();
                        } else
                        {
                            List<Double> nav_list = amfiServiceClient.findByMfNav(schemeMapping.getScheme_amfi_code(),token);
                        }
                    }

                    if (latest_nav > 0) {
                        current_value = total_units * latest_nav;
                        current_value = Double.parseDouble(unit_decimal1.format(current_value));
                    } else {
                        current_value = total_units * purprice;
                        current_value = Double.parseDouble(unit_decimal1.format(current_value));
                    }
                }
            }

            result.add(total_units);
            result.add(current_value);

            List<UnitsPojo> masterList = new ArrayList<>();

            UnitsPojo pojo = new UnitsPojo();
            pojo.setDesc("total_units");
            pojo.setValue(total_units);
            masterList.add(pojo);

            pojo = new UnitsPojo();
            pojo.setDesc("current_value");
            pojo.setValue(current_value);
            masterList.add(pojo);

            return ResponseEntity.ok(masterList);

        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get SWP Dates",
            description = "Returns a distinct list of SWP frequency options (e.g., MONTHLY, QUARTERLY, etc.) for the given AMC and scheme name. " +
                    "These options represent available withdrawal intervals for SWP (Systematic Withdrawal Plan)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved SWP frequencies",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = String.class)),
                            examples = @ExampleObject(value = """
                [
                    "QUARTERLY",
                    "MONTHLY",
                    "ANNUAL",
                    "SEMI-ANNUAL"
                ]
            """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request – Required parameters are missing or invalid",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Invalid request for AMC details.")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No SWP frequencies found for the given AMC and scheme",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "No SWP frequencies found for the given inputs.")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error occurred while processing",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Error fetching SWP frequency data.")
                    )
            )
    })
    @GetMapping("/SwpDates")
    public ResponseEntity<?> swpDates(@RequestHeader("Authorization") String token,@RequestParam String scheme)
    {
        try
        {

            if(NseUtils.isUrlEncoded(scheme))
            {
                scheme = URLDecoder.decode(scheme, StandardCharsets.UTF_8);
            }

            if(scheme == null){scheme = "";}

            scheme = scheme.trim();

            List<NseOnlineSipStpSwpMaster> schemeList = nseOnlineSipStpSwpMasterRepository.findDistinctAswpFrequenciesByAmcNameAndSchemeName(scheme);

            return ResponseEntity.ok(schemeList);

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



    //Cancel SIP/SWP/SWT

    @Operation(
            summary = "Get SIP/STP/SWP Cancel Schemes",
            description = "Returns a list of SIP/STP/SWP transactions eligible for cancellation for the logged-in user. " +
                    "This endpoint filters transactions based on the selected option (`SIP`, `STP`, or `SWP`) and user credentials."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully fetched cancellation eligible transactions",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NseTransactions.class),
                            examples = @ExampleObject(value = """
            {
                "message": "No transactions found for this option",
                "data": []
            }
            """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request – Token or option is invalid or missing",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = """
            {
                "status": "400",
                "message": "Invalid input parameters"
            }
            """)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error while fetching transactions",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = """
            {
                "status": "500",
                "message": "Error fetching AMC codes and names."
            }
            """)
                    )
            )
    })

    @GetMapping("/getSIPSTPSWPCancelSchemes")
    public ResponseEntity<?> getSIPSTPSWPCancelSchemes(
            @RequestHeader("Authorization") String token,
            @RequestParam String option,
            @RequestParam String iin_number,
            @RequestParam String arn_number,
            @RequestParam(required = false) String source
    )
    {
        SimpleDateFormat inputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd-MM-yyyy");

        String userid = "";
        String client_name = "";
        try {

            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            UserDto users =null;

            try
            {
                users = userServiceClient.getUserById(Integer.valueOf(userid), token);
            } catch (FeignException e)
            {
                if (e.status() == 400) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User Not Found");
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Downstream service error.");
                }
            }
            client_name = users.getClient_name();

            client_name = NseUtils.checkParem(client_name);

            JSONObject requestBody = new JSONObject();
            requestBody.put("client_code", iin_number);

            BseNseOnlineAccessDto online_access = userServiceClient.getBseNseOnlineAccessByClientName(client_name, arn_number,token);

            if(online_access == null)
            {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new CommonResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "NSE Online Credentials Not available. Please contact your RM"));
            }

            String nse_userid = NseUtils.trimOrEmpty(online_access.getNse_userid());
            String nse_memberid = NseUtils.trimOrEmpty(online_access.getNse_memberid());
            String nse_secret_key = NseUtils.trimOrEmpty(online_access.getNse_secret_key());
            String nse_license_key = NseUtils.trimOrEmpty(online_access.getNse_license_key());
            
            String base64Encoded = AESEncryptionUtilV2.base64EncodedAuth(nse_secret_key, nse_license_key, nse_userid);
            System.out.println("base64Encoded = " + base64Encoded);
            System.out.println("nse_memberid = " + nse_memberid);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("memberId", nse_memberid);
            headers.set("Authorization", "Basic "+base64Encoded);
            headers.set("User-Agent", "PostmanRuntime/7.43.3");
            headers.set("Accept-Encoding", "gzip, deflate, br");
            headers.set("Accept-Language", "en-US");
            headers.set("Connection", "keep-alive");
            headers.set("Referer", "");

            System.out.println("requestBody: " + requestBody);
            String cancelation_url = "";
            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
            if(option.equalsIgnoreCase("SIP") || option.equalsIgnoreCase("DAILY SIP"))
            {
                cancelation_url = nseUrl+"/nsemfdesk/api/v2/reports/XSIP_REG_REPORT";
            }else if(option.equalsIgnoreCase("STP")){
                cancelation_url = nseUrl+"/nsemfdesk/api/v2/reports/STP_REG_REPORT";
            }else if(option.equalsIgnoreCase("SWP")){
                cancelation_url = nseUrl+"/nsemfdesk/api/v2/reports/SWP_REG_REPORT";
            }else
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("please select option");
            }
            try
            {
                System.out.println("cancelation_url: " + cancelation_url);

                ResponseEntity<String> result = RestTemplateFactory.createRestTemplate().postForEntity(cancelation_url, entity, String.class);
                String responseBody = result.getBody();
                JSONObject jsonObject = new JSONObject(responseBody);
                System.out.println("responseBody: " + responseBody);
                System.out.println("result::status: " + result.getStatusCode());

                JSONArray jsonRegArray = jsonObject.getJSONArray("report_data");

                if (option.equalsIgnoreCase("DAILY SIP"))
                {
                    JSONArray filteredArray = new JSONArray();

                    IntStream.range(0, jsonRegArray.length())
                            .mapToObj(i -> jsonObject.getJSONArray("report_data").getJSONObject(i))
                            .filter(x -> "DAILY".equalsIgnoreCase(x.optString("frequency_type")))
                            .forEach(filteredArray::put);

                    jsonRegArray = filteredArray;  // ✅ assign filtered result back
                    option = "SIP";
                }

                String client_code="";
                String scheme_name2="";
                String folio_no2="";
                String scheme_code2="";
                String active_status="";
                String amount="";
                String start_date="";
                String end_date="";
                String registration_no="";
                String nse_mandate_id="";
                String unique_id="";
                String transaction_date="";
                String frequency_type="";
                String euin="";
                SipRegistrationResponse resp;

                String stp_registration_no="";
                String from_scheme_name="";
                String to_scheme_name="";
                String stp_registration_date="";
                String stp_start_date="";
                String stp_end_date="";
                String transfer_units="";
                String transfer_amount="";
                String to_scheme_code = "";
                String from_scheme_code = "";

                List<SipRegistrationResponse> nse_tran_list = new ArrayList<>();
                for (int i = 0; i < jsonRegArray.length(); i++)
                {
                    JSONObject report_data = jsonRegArray.getJSONObject(i);

                    active_status = report_data.optString("status");

                    if(active_status.equalsIgnoreCase("ACTIVE") || active_status.equalsIgnoreCase("PAUSE"))
                    {
                        frequency_type = report_data.optString("frequency_type");
                        unique_id = report_data.optString("member_unique_id");

                        resp = new SipRegistrationResponse();

                        if(option.equalsIgnoreCase("SIP"))
                        {
                            scheme_name2 = report_data.optString("scheme_name");
                            scheme_code2 =report_data.optString("rta_scheme_code");
                            start_date = report_data.optString("start_date");
                            end_date = report_data.optString("end_date");
                            registration_no = report_data.optString("xsip_registration_no");
                            nse_mandate_id = report_data.optString("nse_mandate_id");
                            transaction_date = report_data.optString("xsip_registration_date");
                            folio_no2 =report_data.optString("folio_number");
                            amount = report_data.optString("installments_amount");
                            euin = report_data.optString("euin_no");
                            client_code = report_data.optString("client_code");

                            start_date = outputFormat.format(inputFormat.parse(start_date));
                            end_date = outputFormat.format(inputFormat.parse(end_date));
                            transaction_date = outputFormat.format(inputFormat.parse(transaction_date));

                            resp.setFolioNo(folio_no2);
                            resp.setSchemeName(scheme_name2);
                            resp.setSchemeCode(scheme_code2);
                            resp.setAmount(amount);
                            resp.setStartDate(start_date);
                            resp.setEndDate(end_date);
                            resp.setRegistrationNo(registration_no);
                            resp.setNseMandateId(nse_mandate_id);
                            resp.setUniqueId(unique_id);
                            resp.setTransactionDate(transaction_date);
                            resp.setEuin(euin);

                        }else if((option.equalsIgnoreCase("STP"))){

                            euin = report_data.optString("euin_number");
                            folio_no2 =report_data.optString("folio_no");
                            stp_registration_no = report_data.optString("stp_registration_no");
                            from_scheme_name = report_data.optString("from_scheme_name");
                            to_scheme_name = report_data.optString("to_scheme_name");
                            stp_registration_date = report_data.optString("stp_registration_date");
                            stp_start_date = report_data.optString("stp_start_date");
                            stp_end_date = report_data.optString("stp_end_date");
                            transfer_units = report_data.optString("transfer_units");
                            transfer_amount = report_data.optString("transfer_amount");
                            from_scheme_code = report_data.optString("from_nse_scheme_code");
                            to_scheme_code = report_data.optString("to_nse_scheme_code");

                            stp_registration_date = outputFormat.format(inputFormat.parse(stp_registration_date));
                            stp_start_date = outputFormat.format(inputFormat.parse(stp_start_date));
                            stp_end_date = outputFormat.format(inputFormat.parse(stp_end_date));

                            resp.setEuin(euin);
                            resp.setFolioNo(folio_no2);
                            resp.setRegistrationNo(stp_registration_no);
                            resp.setTransactionDate(stp_registration_date);
                            resp.setStartDate(stp_start_date);
                            resp.setEndDate(stp_end_date);
                            resp.setFromSchemeName(from_scheme_name);
                            resp.setToSchemeName(to_scheme_name);
                            resp.setTransferUnits(transfer_units);
                            resp.setTransferAmount(transfer_amount);
                            resp.setSchemeName(to_scheme_name);
                            resp.setSchemeCode(to_scheme_code);

                            resp.setFromSchemeCode(from_scheme_code);
                            resp.setToSchemeCode(to_scheme_code);
                        }else if((option.equalsIgnoreCase("SWP"))){

                            euin = report_data.optString("euin_number");
                            scheme_name2 =report_data.optString("scheme_name");
                            scheme_code2 =report_data.optString("nse_scheme_code");
                            start_date = report_data.optString("swp_start_date");
                            end_date = report_data.optString("swp_end_date");
                            registration_no = report_data.optString("swp_registration_no");
                            transaction_date = report_data.optString("swp_registration_date");
                            folio_no2 =report_data.optString("folio_no");

                            start_date = outputFormat.format(inputFormat.parse(start_date));
                            end_date = outputFormat.format(inputFormat.parse(end_date));
                            transaction_date = outputFormat.format(inputFormat.parse(transaction_date));

                            resp.setEuin(euin);
                            resp.setSchemeName(scheme_name2);
                            resp.setSchemeCode(scheme_code2);
                            resp.setEndDate(end_date);
                            resp.setStartDate(start_date);
                            resp.setRegistrationNo(registration_no);
                            resp.setTransactionDate(transaction_date);
                            resp.setFolioNo(folio_no2);
                        }
                        resp.setClientCode(client_code);
                        resp.setActiveStatus(active_status);
                        resp.setFrequencyType(frequency_type);
                        resp.setUniqueId(unique_id);

                        nse_tran_list.add(resp);
                    }
                }

                Map<String, List<SipRegistrationResponse>> grouped = nse_tran_list.stream().collect(Collectors.groupingBy(SipRegistrationResponse::getClientCode));

                UserDto finalUsers = users;

                List<SIPSTPSWPCancelSchemesPojo> finalList = grouped.entrySet().stream().map(e -> convertGroup(e.getKey(), e.getValue(), finalUsers)).collect(Collectors.toList());

                if ("mobile".equalsIgnoreCase(source))
                {
                    SipRegistrationResponseWrapperPojo response = new SipRegistrationResponseWrapperPojo();
                    response.setStatus(200);
                    response.setStatusMsg("Success");
                    response.setMsg("Success");
                    response.setResult(finalList);

                    return ResponseEntity.ok(response);
                }
                return ResponseEntity.ok(nse_tran_list);
            }catch (Exception ex)
            {
                ex.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }

    private SIPSTPSWPCancelSchemesPojo convertGroup(
            String clientCode,
            List<SipRegistrationResponse> txList,
            UserDto users)
    {
        SIPSTPSWPCancelSchemesPojo pojo = new SIPSTPSWPCancelSchemesPojo();

        pojo.setUser_id(users.getUser_id());
        pojo.setInv_name(users.getName());
        pojo.setTax_status(users.getTax_status());
//        pojo.setTax_status_desc(Integer.parseInt(users.getTax_status_code()));
        pojo.setHolding_nature(users.getHolding_nature());
        pojo.setHolding_nature_desc(users.getHolding_nature_code());
        pojo.setBroker_code(users.getBroker_code());

        // The investor_code should show the client code
        pojo.setInvestor_code(clientCode);

        String logo = vendorLogoPath + NseUtils.getVendorImage("NSE");
        pojo.setLogo(logo);

        List<SipRegistrationResponseWrapper> schemeList =
                txList.stream().map(this::convertToSipResponse).collect(Collectors.toList());

        pojo.setScheme_list(schemeList);

        return pojo;
    }

    private SipRegistrationResponseWrapper convertToSipResponse(SipRegistrationResponse t)
    {
        SipRegistrationResponseWrapper resp = new SipRegistrationResponseWrapper();

        resp.setRegistration_no(t.getRegistrationNo());
        resp.setFolio_no(t.getFolioNo());
        resp.setScheme_name(t.getSchemeName());
        resp.setScheme_code(t.getSchemeCode());
        resp.setAmount(String.valueOf(t.getAmount()));
        resp.setStart_date(t.getStartDate());
        resp.setEnd_date(t.getEndDate());
        resp.setFrequency(t.getFrequencyType());

        // DEMO missing -> set null
        resp.setFrequency_code(null);

        resp.setEuin_number(t.getEuin());
        resp.setTransaction_status(t.getActiveStatus());
        resp.setPayment_status(null);     // DEMO sets null or "PENDING"

        resp.setRegister_source("Mobile App"); // or Website

        // New fields from demo
        resp.setUmrn_no(null);
        resp.setUnique_number(t.getUniqueId());
        resp.setFirst_order_flag(null);
        resp.setInstallment(null);
        resp.setTransaction_date(t.getTransactionDate());
        resp.setTo_scheme_name(t.getToSchemeName());
        resp.setTo_scheme_code(t.getToSchemeCode());
        resp.setAmc_code(null);
        resp.setExt_unique_ref_no(null);
        resp.setUnique_ref_no(null);
        resp.setTrxn_ref_no(null);
        resp.setGroup_order_no(null);
        resp.setSip_reg_no(t.getRegistrationNo());
        resp.setMandate_id(t.getNseMandateId());

        String logoUrl = amcLogoPath +
                NseUtils.getLogoByAmcNameOrSchemeName(t.getSchemeName());
        resp.setLogo(logoUrl);

        return resp;
    }

    @Operation(summary = "Get SIP Cancellation Reasons", description = "Fetches a list of predefined SIP cancellation reasons")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved reasons", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                          "status": 200,
                          "message": "Success",
                          "data": [
                            { "code": "01", "reason": "Non availability of Funds" },
                            { "code": "02", "reason": "Scheme not performing" },
                            { "code": "03", "reason": "Service issue" },
                            { "code": "04", "reason": "Load Revised" },
                            { "code": "05", "reason": "Wish to invest in other schemes" },
                            { "code": "06", "reason": "Change in Fund Manager" },
                            { "code": "07", "reason": "Goal Achieved" },
                            { "code": "08", "reason": "Not comfortable with market volatility" },
                            { "code": "09", "reason": "Will be restarting SIP after few months" },
                            { "code": "10", "reason": "Modifications in bank/mandate/date etc" },
                            { "code": "11", "reason": "I have decided to invest elsewhere" },
                            { "code": "12", "reason": "This is not the right time to invest" },
                            { "code": "13", "reason": "Others (pls specify the reason)" }
                          ]
                        }
                        """
                    ))),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/getSipCancellationReasons")
    public ResponseEntity<?> getSipCancellationReasons() {
        List<Map<String, String>> reasons = new ArrayList<>();

        reasons.add(Map.of("code", "01", "reason", "Non availability of Funds"));
        reasons.add(Map.of("code", "02", "reason", "Scheme not performing"));
        reasons.add(Map.of("code", "03", "reason", "Service issue"));
        reasons.add(Map.of("code", "04", "reason", "Load Revised"));
        reasons.add(Map.of("code", "05", "reason", "Wish to invest in other schemes"));
        reasons.add(Map.of("code", "06", "reason", "Change in Fund Manager"));
        reasons.add(Map.of("code", "07", "reason", "Goal Achieved"));
        reasons.add(Map.of("code", "08", "reason", "Not comfortable with market volatility"));
        reasons.add(Map.of("code", "09", "reason", "Will be restarting SIP after few months"));
        reasons.add(Map.of("code", "10", "reason", "Modifications in bank/mandate/date etc"));
        reasons.add(Map.of("code", "11", "reason", "I have decided to invest elsewhere"));
        reasons.add(Map.of("code", "12", "reason", "This is not the right time to invest"));
        reasons.add(Map.of("code", "13", "reason", "Others (pls specify the reason)"));

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Success");
        response.put("data", reasons);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get IIN Details", description = "Fetch IIN details from NSE or BSE using token and IIN number.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved IIN details",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input or user data",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Server failed to process the request",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/getIINDetails")
    public ResponseEntity<?> getIINDetails(@RequestHeader("Authorization") String token,
                                           @RequestParam String iin_number)
    {
        String userid = "";
        String client_name = "";
        try {
            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            UserDto users = userServiceClient.getUserById(Integer.valueOf(userid), token);
            client_name = users.getClient_name();

            UserDto user = null;
            try {
                user = userServiceClient.getUserDetailsByID(client_name, Integer.valueOf(userid),token);
            } catch (FeignException e) {
                if (e.status() == 400) {
                    return NseUtils.commonResponse("Invalid request for AMC details.", HttpStatus.BAD_REQUEST);
                } else if (e.status() == 404) {
                    return NseUtils.commonResponse("No AMC details found for the given user.", HttpStatus.BAD_REQUEST);
                } else {
                    return NseUtils.commonResponse("Error occurred while fetching AMC details.", HttpStatus.BAD_REQUEST);
                }
            }

            if (user != null)
            {
                String nse_iin_number = user.getNse_iin_number() != null ? user.getNse_iin_number() : "";

                if (iin_number.equalsIgnoreCase(nse_iin_number))
                {
                    return ResponseEntity.ok(user);
                } else
                {
                    UserBseNseDto bse_nse = null;
                    try
                    {
                        bse_nse = userServiceClient.getUserBseNseDetailsByIinNumber(client_name, iin_number,token);
                    } catch (FeignException.BadRequest ex)
                    {
                        System.out.println("No BSE/NSE details found for IIN: " + iin_number);
                    }

                    if (bse_nse != null)
                    {
                        return ResponseEntity.ok(bse_nse);
                    }
                }
            }
            return ResponseEntity.ok(Collections.emptyList());

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Fetch ACH Registered Bank Details",
            description = "Returns a list of ACH registered bank details (unique number | bank name (account number)) for a given IIN number. Searches both NSE and BSE data sources based on the investor.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of ACH registered bank details",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(
                                            schema = @Schema(
                                                    example = "[\"23812037|IDBI Bank ( 1140104000034982 )\",\"23892581|HDFC Bank Ltd ( 00581140015952 )\"]",
                                                    type = "string"
                                            )
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad Request - Invalid input or client name/IIN not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(
                                            example = "\"No BSE/NSE details found for IIN: 1234567890\""
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal Server Error - Failure while processing the request",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(
                                            example = "\"Error fetching IIN details.\""
                                    )
                            )
                    )
            }
    )
    @GetMapping("/getAchRegisteredBankDetails")
    public ResponseEntity<?> getAchRegisteredBankDetails(@RequestHeader("Authorization") String token,
                                                         @RequestParam String iin_number)
    {
        String userid = "";
        String client_name = "";
        try {
            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            UserDto users = userServiceClient.getUserById(Integer.valueOf(userid), token);
            client_name = users.getClient_name();
            String transaction_type= "ACH Mandate Request";
            List<String> master = new ArrayList<String>();
            List<NseTransactions> nse_transaction = nseTransactionRepository.findPhysicalTransactionsByIinAndClient(iin_number,client_name,transaction_type);

            System.out.println("nse" + nse_transaction);

            List<UserDto> list = null;
            try {
                list = userServiceClient.getClientNameAndIinNumber(client_name,iin_number,token);
            } catch (FeignException.BadRequest ex) {
                System.out.println("No BSE/NSE details found for IIN: " + iin_number);
            }

            if (list != null)
            {
                UserDto user = list.get(0);
                Integer ach1 = user.getNse_ach_flag1();
                Integer ach2 = user.getNse_ach_flag2();
                Integer ach3 = user.getNse_ach_flag3();
                if (ach1 == null)
                {
                    ach1 = 0;
                }
                if (ach2 == null)
                {
                    ach2 = 0;
                }
                if (ach3 == null)
                {
                    ach3 = 0;
                }

                if (ach1 == 1)
                {
                    String bank_name1 = user.getBank_name1();
                    String bank_acc1 = user.getBank_account_number1();
                    String unique_no = "";
                    if (nse_transaction != null)
                    {
                        NseTransactions nse = nse_transaction.stream()
                                .filter(x -> x.getBank_acc_no().equalsIgnoreCase(bank_acc1)).findAny().orElse(null);
                        if (nse != null)
                        {
                            unique_no = nse.getUnique_number();
                        }
                    }
                    String str = unique_no + "|" + bank_name1 + " ( " + bank_acc1 + " )";
                    master.add(str);
                }
                if (ach2 == 1)
                {
                    String bank_name2 = user.getBank_name2();
                    String bank_acc2 = user.getBank_account_number2();
                    String unique_no = "";
                    if (nse_transaction != null)
                    {
                        NseTransactions nse = nse_transaction.stream()
                                .filter(x -> x.getBank_acc_no().equalsIgnoreCase(bank_acc2)).findAny().orElse(null);
                        if (nse != null)
                        {
                            unique_no = nse.getUnique_number();
                        }
                    }
                    String str = unique_no + "|" + bank_name2 + " ( " + bank_acc2 + " )";
                    master.add(str);
                }
                if (ach3 == 1)
                {
                    String bank_name3 = user.getBank_name3();
                    String bank_acc3 = user.getBank_account_number3();
                    String unique_no = "";
                    if (nse_transaction != null)
                    {
                        NseTransactions nse = nse_transaction.stream()
                                .filter(x -> x.getBank_acc_no().equalsIgnoreCase(bank_acc3)).findAny().orElse(null);
                        if (nse != null)
                        {
                            unique_no = nse.getUnique_number();
                        }
                    }
                    String str = unique_no + "|" + bank_name3 + " ( " + bank_acc3 + " )";
                    master.add(str);
                }
            }else {
                List<UserBseNseDto> list2 = null;
                try
                {
                    list2 = userServiceClient.getUserBseNseDetailsByIinNumbers(client_name, iin_number,token);
                } catch (FeignException.BadRequest ex)
                {
                    System.out.println("No BSE/NSE details found for IIN: " + iin_number);
                }


                if (list2 != null && list2.size() > 0)
                {
                    UserBseNseDto user = list2.get(0);
                    Integer ach1 = user.getNse_ach_flag1();
                    Integer ach2 = user.getNse_ach_flag2();
                    Integer ach3 = user.getNse_ach_flag3();
                    if (ach1 == null)
                    {
                        ach1 = 0;
                    }
                    if (ach2 == null)
                    {
                        ach2 = 0;
                    }
                    if (ach3 == null)
                    {
                        ach3 = 0;
                    }
                    if (ach1 == 1)
                    {
                        String bank_name1 = user.getBank_name1();
                        String bank_acc1 = user.getBank_account_number1();
                        String unique_no = "";
                        if (nse_transaction != null)
                        {
                            NseTransactions nse = nse_transaction.stream()
                                    .filter(x -> x.getBank_acc_no().equalsIgnoreCase(bank_acc1)).findAny().orElse(null);
                            if (nse != null)
                            {
                                unique_no = nse.getUnique_number();
                            }
                        }
                        String str = unique_no + "|" + bank_name1 + " ( " + bank_acc1 + " )";
                        master.add(str);
                    }
                    if (ach2 == 1)
                    {
                        String bank_name2 = user.getBank_name2();
                        String bank_acc2 = user.getBank_account_number2();
                        String unique_no = "";
                        if (nse_transaction != null)
                        {
                            NseTransactions nse = nse_transaction.stream()
                                    .filter(x -> x.getBank_acc_no().equalsIgnoreCase(bank_acc2)).findAny().orElse(null);
                            if (nse != null)
                            {
                                unique_no = nse.getUnique_number();
                            }
                        }
                        String str = unique_no + "|" + bank_name2 + " ( " + bank_acc2 + " )";
                        master.add(str);
                    }
                    if (ach3 == 1)
                    {
                        String bank_name3 = user.getBank_name3();
                        String bank_acc3 = user.getBank_account_number3();
                        String unique_no = "";
                        if (nse_transaction != null)
                        {
                            NseTransactions nse = nse_transaction.stream()
                                    .filter(x -> x.getBank_acc_no().equalsIgnoreCase(bank_acc3)).findAny().orElse(null);
                            if (nse != null)
                            {
                                unique_no = nse.getUnique_number();
                            }
                        }
                        String str = unique_no + "|" + bank_name3 + " ( " + bank_acc3 + " )";
                        master.add(str);
                    }
                }
            }
            return ResponseEntity.ok(master);

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get NSE My Orders",
            description = "Fetches all NSE transactions excluding request types like UCC Request, FATCA Request etc. for a logged-in user."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved list of orders",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = NseTransactions.class)),
                            examples = {
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "Sample Response",
                                            value = "[\n" +
                                                    "  {\n" +
                                                    "    \"id\": 218851,\n" +
                                                    "    \"url\": \"https://www.nseinvest.com/nsemfdesk/api/v2/transaction/NORMAL\",\n" +
                                                    "    \"nse_request\": \"{...}\",\n" +
                                                    "    \"nse_response\": \"{...}\",\n" +
                                                    "    \"return_msg\": \"TRXN FAILED\",\n" +
                                                    "    \"service_return_code\": \"200 OK\",\n" +
                                                    "    \"service_msg\": \"TRXN FAILED\",\n" +
                                                    "    \"pan\": \"AAMPC1524D\",\n" +
                                                    "    \"name\": \"PRADIP  CHAKRABARTY\",\n" +
                                                    "    \"client_name\": \"milansamajder\",\n" +
                                                    "    \"transaction_type\": \"Redemtion-Order entry\",\n" +
                                                    "    \"remarks\": \"Scheme code - 57N not found / disabled.\",\n" +
                                                    "    \"transaction_date\": \"2025-07-23T16:00:12.000+00:00\"\n" +
                                                    "  },\n" +
                                                    "  {\n" +
                                                    "    \"id\": 218850,\n" +
                                                    "    \"url\": \"https://www.nseinvest.com/nsemfdesk/api/v2/transaction/SWITCH\",\n" +
                                                    "    \"nse_request\": \"{...}\",\n" +
                                                    "    \"nse_response\": \"{...}\",\n" +
                                                    "    \"return_msg\": \"TRXN FAILED\",\n" +
                                                    "    \"service_return_code\": \"200 OK\",\n" +
                                                    "    \"service_msg\": \"TRXN FAILED\",\n" +
                                                    "    \"pan\": \"AAMPC1524D\",\n" +
                                                    "    \"name\": \"PRADIP  CHAKRABARTY\",\n" +
                                                    "    \"client_name\": \"milansamajder\",\n" +
                                                    "    \"transaction_type\": \"Switch-Order entry\",\n" +
                                                    "    \"remarks\": \"FROM SCHEME CODE IS INVALID.\",\n" +
                                                    "    \"transaction_date\": \"2025-07-23T16:00:12.000+00:00\"\n" +
                                                    "  }\n" +
                                                    "]"
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid token or user not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error while fetching IIN details")
    })

//    @GetMapping("/getMyOrders")
//    public ResponseEntity<?> getMyOrders(@RequestHeader("Authorization") String token)
//    {
//        String userid = "";
//        String client_name ="";
//        try
//        {
//            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
//            UserDto users = userServiceClient.getUserById(Integer.valueOf(userid), token);
//            client_name = users.getClient_name();
//
//            List<NseTransactions> list = nseTransactionRepository.findNonRequestTransactionsOrderedByDate(Integer.valueOf(userid),client_name);
//            System.out.println("list" + list);
//            for(NseTransactions nseTransactions : list)
//            {
//                List<String> schemeCodeList = new ArrayList<>();
//                schemeCodeList.add(nseTransactions.getScheme_code());
//                List<NseOnlineSchemeMaster> schemeMasterList = nseOnlineSchemeMasterRepository.getSchemeBySchemeCode(schemeCodeList);
//                System.out.println("schemeMasterList = " + schemeMasterList);
//                if (schemeMasterList != null && !schemeMasterList.isEmpty()) {
//                    for (NseOnlineSchemeMaster schemeMaster : schemeMasterList) {
//                        String logo = amcLogoPath + NseUtils.getLogoByAmcNameOrSchemeName(schemeMaster.getSchemeName());
//                        nseTransactions.setLogo(logo);
//                    }
//            }else {
//                    nseTransactions.setLogo("");
//                }
//            }
//
//            return ResponseEntity.ok(list);
//
//        } catch (Exception ex)
//        {
//            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
//            ex.printStackTrace();
//            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }

    @GetMapping("/getMyOrders")
    public ResponseEntity<?> getMyOrders(@RequestHeader("Authorization") String token,@RequestParam(required = false) String source,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(required = false, defaultValue = "25") int size)
    {
        String userid;
        String client_name;
        try
        {
            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);

            UserDto users = userServiceClient.getUserById(Integer.valueOf(userid), token);
            client_name = users.getClient_name();

            List<NseTransactions> list;

            if ("MOBILE".equalsIgnoreCase(source))
            {
                Pageable pageable = PageRequest.of(page, size);

                Page<NseTransactions> transactionsPage =
                        nseTransactionRepository.findByUserIdAndClientNameOrderByTxnDateDesc(
                                Integer.valueOf(userid), client_name, pageable);

                list = transactionsPage.getContent();

                list.forEach(nse ->
                        nse.setLogo(amcLogoPath + NseUtils.getLogoByAmcNameOrSchemeName(nse.getScheme_name()))
                );

                System.out.println("NseTransactionsList size (mobile): " + list.size());
                return ResponseEntity.ok(list);

            } else
            {
                list = nseTransactionRepository.findNonRequestTransactionsOrderedByDate(
                        Integer.valueOf(userid), client_name);

                if (list != null && !list.isEmpty()) {
                    list.forEach(nse ->
                            nse.setLogo(amcLogoPath + NseUtils.getLogoByAmcNameOrSchemeName(nse.getScheme_name()))
                    );
                }
                System.out.println("NseTransactionsList size (website): " + list.size());
                return ResponseEntity.ok(list);
            }

            /*
            if(list != null && list.size() > 0)
            {
                schemeCodeList = list.stream().map(nse -> nse.getScheme_code()).collect(Collectors.toList());
            }

            List<NseOnlineSchemeMaster> schemeMasterList = nseOnlineSchemeMasterRepository.getSchemeBySchemeCode(schemeCodeList);
            System.out.println("second");
            if(list != null && list.size() > 0)
            {
                list.forEach(nseTransactions -> {
                    NseOnlineSchemeMaster schemeMaster = schemeMasterList.stream()
                            .filter(s -> s.getSchemeCode().equals(nseTransactions.getScheme_code()))
                            .findFirst()
                            .orElse(null);

                    if (schemeMaster != null) {
                        nseTransactions.setLogo(
                                amcLogoPath + NseUtils.getLogoByAmcNameOrSchemeName(schemeMaster.getAmcCode())
                        );
                    } else {
                        nseTransactions.setLogo("");
                    }
                });

                return ResponseEntity.ok(list);
            }else
            {
                return ResponseEntity.ok(new ArrayList<>());
            }*/
        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Operation(
            summary = "Update Payment Status for a Transaction",
            description = "Updates the payment status of a transaction using the payment reference number and customer ID (IIN number). This verifies the transaction and updates its status if found."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment status updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "string", example = "Transaction Saved Successfully")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Transaction not found or invalid input",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "string", example = "Transaction not found")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error while processing the request",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "string", example = "Error fetching SIP scheme names")
                    )
            )
    })
    @PostMapping("/updatePaymentStatus")
    public ResponseEntity<?> updatePaymentStatus(@RequestHeader("Authorization") String token,
                                                 @RequestParam String payment_ref_no,
                                                 @RequestParam String customer_id,
                                                 @RequestParam String PaymentStatus)
    {
        try
        {
            payment_ref_no = payment_ref_no.split("\\?")[0];
            List<NseTransactions> list = nseTransactionRepository.findByIinNumberAndPaymentRefNo(customer_id,payment_ref_no);

            if(list != null && list.size() > 0)
            {
                NseTransactions transactions = list.get(0);
                transactions.setPayment_status(PaymentStatus);
                nseTransactionRepository.save(transactions);
            }

            return NseUtils.commonResponse("Transaction Saved Succesfully",HttpStatus.OK);
        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Operation(
            summary = "Get Redemption Scheme Code",
            description = "Fetches the eligible scheme details for redemption based on AMC code, scheme name, and dividend code."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved redemption scheme details",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NseOnlineSchemeMaster.class),
                            examples = @ExampleObject(
                                    name = "Success Example",
                                    summary = "Sample Redemption Scheme Response",
                                    value = """
                    {
                        "id": 4937,
                        "uniqueSrNo": "22339",
                        "schemeCode": "HD57N-GR",
                        "rtaSchemeCode": "57N",
                        "amcSchemeCode": "57N",
                        "isin": "INF179KB1HS3",
                        "amcCode": "HDFC Mutual Fund",
                        "amcName": "HDFCMUTUALFUND_MF",
                        "schemeType": "OVERNIGHT",
                        "schemeCategory": "Debt: Overnight",
                        "planType": "NORMAL",
                        "schemeAmfiCode": "101996",
                        "schemeName": "HDFC Overnight Fund - Growth Option",
                        "schemeAmfiShortName": "HDFC Overnight Gr",
                        "scheme": "HDFC OVERNIGHT FUND - REGULAR PLAN -  GROWTH",
                        "purchaseAllowed": "Y",
                        "purchaseTransactionMode": "DP",
                        "newPurchaseMinAmount": 100.0,
                        "additionalPurchaseMinAmount": 100.0,
                        "additionalPurchaseMaxAmount": 9999999999.0,
                        "purchaseAmountMultiplier": 1.0,
                        "purchaseCutoffTime": "14:30:00",
                        "redemptionAllowed": "Y",
                        "redemptionTransactionMode": "DP",
                        "redemptionMinQty": 0.001,
                        "redemptionQtyMultiplier": 0.001,
                        "redemptionMaxQty": 999999999.0,
                        "redemptionMinAmount": 100.0,
                        "redemptionMaxAmount": 9999999999.0,
                        "redemptionAmountMultiplier": 1.0,
                        "redemptionCutoffTime": "19:00:00",
                        "rtaAgentCode": "CAMS",
                        "amcActiveFlag": "Y",
                        "divReinvestFlag": "Z",
                        "sipAllowed": "Y",
                        "stpEnabled": "Y",
                        "swpEnabled": "Y",
                        "switchAllowed": "Y",
                        "settlementType": "T1",
                        "faceValue": "0",
                        "schemeStartDate": "01-01-2010",
                        "maturityDate": "31-12-2099",
                        "exitLoadFlag": "N",
                        "lockInPeriodFlag": "N",
                        "channelPartnerCode": "H57N",
                        "createdDate": "2025-06-26"
                    }
                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = MediaType.TEXT_PLAIN_VALUE,
                            examples = @ExampleObject(value = "Error fetching SIP scheme names")
                    )
            )
    })
    @GetMapping("/getRedemSchemeCode")
    public ResponseEntity<?> getRedemSchemeCode(@RequestHeader("Authorization") String token,
                                                @RequestParam String scheme,
                                                @RequestParam String amc_code,
                                                @RequestParam String dividend_code)
    {
        try
        {

            NseOnlineSchemeMaster schemeMaster = null;
            if(NseUtils.isUrlEncoded(scheme))
            {
                scheme = URLDecoder.decode(scheme, StandardCharsets.UTF_8);
            }

            List<NseOnlineSchemeMaster> list = nseOnlineSchemeMasterRepository.findEligibleSchemeForSwitchAndRedemption(scheme,dividend_code);

            if(list.size() > 0)
            {
                schemeMaster = list.get(0);
            }

            return ResponseEntity.ok(schemeMaster);

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Operation(
            summary = "Get Switch Scheme Code",
            description = "Fetches the eligible scheme details for Switch based on AMC code, scheme name, and dividend code."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved Switch scheme details",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NseOnlineSchemeMaster.class),
                            examples = @ExampleObject(
                                    name = "Success Example",
                                    summary = "Sample Switch Scheme Response",
                                    value = """
                    {
                        "id": 4937,
                        "uniqueSrNo": "22339",
                        "schemeCode": "HD57N-GR",
                        "rtaSchemeCode": "57N",
                        "amcSchemeCode": "57N",
                        "isin": "INF179KB1HS3",
                        "amcCode": "HDFC Mutual Fund",
                        "amcName": "HDFCMUTUALFUND_MF",
                        "schemeType": "OVERNIGHT",
                        "schemeCategory": "Debt: Overnight",
                        "planType": "NORMAL",
                        "schemeAmfiCode": "101996",
                        "schemeName": "HDFC Overnight Fund - Growth Option",
                        "schemeAmfiShortName": "HDFC Overnight Gr",
                        "scheme": "HDFC OVERNIGHT FUND - REGULAR PLAN -  GROWTH",
                        "purchaseAllowed": "Y",
                        "purchaseTransactionMode": "DP",
                        "newPurchaseMinAmount": 100.0,
                        "additionalPurchaseMinAmount": 100.0,
                        "additionalPurchaseMaxAmount": 9999999999.0,
                        "purchaseAmountMultiplier": 1.0,
                        "purchaseCutoffTime": "14:30:00",
                        "redemptionAllowed": "Y",
                        "redemptionTransactionMode": "DP",
                        "redemptionMinQty": 0.001,
                        "redemptionQtyMultiplier": 0.001,
                        "redemptionMaxQty": 999999999.0,
                        "redemptionMinAmount": 100.0,
                        "redemptionMaxAmount": 9999999999.0,
                        "redemptionAmountMultiplier": 1.0,
                        "redemptionCutoffTime": "19:00:00",
                        "rtaAgentCode": "CAMS",
                        "amcActiveFlag": "Y",
                        "divReinvestFlag": "Z",
                        "sipAllowed": "Y",
                        "stpEnabled": "Y",
                        "swpEnabled": "Y",
                        "switchAllowed": "Y",
                        "settlementType": "T1",
                        "faceValue": "0",
                        "schemeStartDate": "01-01-2010",
                        "maturityDate": "31-12-2099",
                        "exitLoadFlag": "N",
                        "lockInPeriodFlag": "N",
                        "channelPartnerCode": "H57N",
                        "createdDate": "2025-06-26"
                    }
                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = MediaType.TEXT_PLAIN_VALUE,
                            examples = @ExampleObject(value = "Error fetching Switch scheme names")
                    )
            )
    })
    @GetMapping("/getSwitchSchemecode")
    public ResponseEntity<?> getSwitchSchemecode(@RequestHeader("Authorization") String token,
                                                 @RequestParam String scheme,
                                                 @RequestParam String dividend_code)
    {
        try
        {
            NseOnlineSchemeMaster schemeMaster = null;

            if(NseUtils.isUrlEncoded(scheme))
            {
                scheme = URLDecoder.decode(scheme, StandardCharsets.UTF_8);
            }
            System.out.println("schem = " + scheme);
            System.out.println("dividend_code" + dividend_code);
            List<NseOnlineSchemeMaster> list = nseOnlineSchemeMasterRepository.findEligibleSchemesForSwitchAndRedemptions(scheme,dividend_code);

            if(list.size() > 0)
            {
                schemeMaster = list.get(0);
            }

            return ResponseEntity.ok(schemeMaster);
        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getSwitchSchemeName")
    public ResponseEntity<?> getSwitchSchemeName(@RequestHeader("Authorization") String token,
                                                 @RequestParam String scheme,
                                                 @RequestParam String dividend_code)
    {
        try
        {
            NseOnlineSchemeMaster schemeMaster = null;

            if(NseUtils.isUrlEncoded(scheme))
            {
                scheme = URLDecoder.decode(scheme, StandardCharsets.UTF_8);
            }
            System.out.println("schem = " + scheme);
            System.out.println("dividend_code" + dividend_code);
            List<NseOnlineSchemeMaster> list = nseOnlineSchemeMasterRepository.findEligibleSchemeNameForSwitchAndRedemptions(scheme,dividend_code);

            if(list.size() > 0)
            {
                schemeMaster = list.get(0);
            }

            return ResponseEntity.ok(schemeMaster);
        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get Switch Scheme Details",
            description = "Fetches scheme details based on provided scheme code and AMC code. This is used for validating switch transactions in mutual fund processing."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved switch scheme details",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NseOnlineSchemeMaster.class),
                            examples = @ExampleObject(
                                    name = "Sample Response",
                                    value = "{\n" +
                                            "  \"id\": 6577,\n" +
                                            "  \"uniqueSrNo\": \"27596\",\n" +
                                            "  \"schemeCode\": \"ESEHGPG-GR\",\n" +
                                            "  \"rtaSchemeCode\": \"EHGPG\",\n" +
                                            "  \"amcSchemeCode\": \"EHGP\",\n" +
                                            "  \"isin\": \"INF959L01DI2\",\n" +
                                            "  \"amcCode\": \"Navi Mutual Fund\",\n" +
                                            "  \"amcName\": \"NAVIMUTUALFUND_MF\",\n" +
                                            "  \"schemeType\": \"EQUITY\",\n" +
                                            "  \"schemeCategory\": \"Hybrid: Aggressive\",\n" +
                                            "  \"planType\": \"NORMAL\",\n" +
                                            "  \"schemeAmfiCode\": \"143162\",\n" +
                                            "  \"schemeName\": \"Navi Aggressive Hybrid Fund - Regular Plan - Growth\",\n" +
                                            "  \"purchaseAllowed\": \"Y\",\n" +
                                            "  \"purchaseTransactionMode\": \"DP\",\n" +
                                            "  \"newPurchaseMinAmount\": 100.0,\n" +
                                            "  \"additionalPurchaseMinAmount\": 100.0,\n" +
                                            "  \"additionalPurchaseMaxAmount\": 199999.0,\n" +
                                            "  \"purchaseAmountMultiplier\": 1.0,\n" +
                                            "  \"purchaseCutoffTime\": \"14:30:00\",\n" +
                                            "  \"redemptionAllowed\": \"Y\",\n" +
                                            "  \"redemptionTransactionMode\": \"DP\",\n" +
                                            "  \"redemptionMinQty\": 0.001,\n" +
                                            "  \"redemptionQtyMultiplier\": 0.001,\n" +
                                            "  \"redemptionMaxQty\": 999999999.0,\n" +
                                            "  \"redemptionMinAmount\": 10.0,\n" +
                                            "  \"redemptionMaxAmount\": 9999999999.0,\n" +
                                            "  \"redemptionAmountMultiplier\": 1.0,\n" +
                                            "  \"redemptionCutoffTime\": \"15:00:00\",\n" +
                                            "  \"rtaAgentCode\": \"CAMS\",\n" +
                                            "  \"sipAllowed\": \"Y\",\n" +
                                            "  \"stpEnabled\": \"Y\",\n" +
                                            "  \"swpEnabled\": \"Y\",\n" +
                                            "  \"switchAllowed\": \"Y\",\n" +
                                            "  \"settlementType\": \"T1\",\n" +
                                            "  \"schemeStartDate\": \"01-01-2010\",\n" +
                                            "  \"maturityDate\": \"31-12-2099\",\n" +
                                            "  \"exitLoadFlag\": \"N\"\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @GetMapping("/getValidateSwitchAmount")
    public ResponseEntity<?> getValidateSwitchAmount(@RequestHeader("Authorization") String token,
                                                     @RequestParam String scheme_code,
                                                     @RequestParam String amc_code)
    {
        try
        {
            scheme_code = NseUtils.checkParem(scheme_code);
            amc_code = NseUtils.checkParem(amc_code);

            NseOnlineSchemeMaster schemeMasterLimit = null;

            List<NseOnlineSchemeMaster> list  = nseOnlineSchemeMasterRepository.findBySchemeAndAmcAndTransactionMode(scheme_code,amc_code);

            if(list != null && list.size() > 0)
            {
                schemeMasterLimit = list.get(0);
            }

            return ResponseEntity.ok(schemeMasterLimit);
        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get NFO Lumpsum Schemes",
            description = "Retrieves NFO Lumpsum Schemes based on the user's AMC eligibility and scheme start date.\n\n" +
                    "If the user's AMC list is available, only those AMC schemes are returned. " +
                    "Otherwise, all schemes with a start date from today onward are returned."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved list of NFO Lumpsum Schemes",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = NseOnlineSchemeMaster.class)),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                    [
                      {
                        "id": 123,
                        "scheme_name": "Navi ELSS Tax Saver Fund",
                        "amc_name": "NAVIMUTUALFUND_MF",
                        "plan_type": "NORMAL",
                        "purchase_allowed": "Y",
                        "amc_active_flag": "Y",
                        "scheme_start_date": "29-07-2025"
                      }
                    ]
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Invalid token or input",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                    {
                      "error": "Invalid request or missing data"
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error - Failed to fetch schemes",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                    {
                      "error": "Error fetching Switch scheme names"
                    }
                    """
                            )
                    )
            )
    })
    @GetMapping("/getNFOLumpsumSchemes")
    public ResponseEntity<?> getNFOLumpsumSchemes(@RequestHeader("Authorization") String token)
    {
        String userid = "";
        String client_name = "";
        try
        {
            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            UserDto users = userServiceClient.getUserById(Integer.valueOf(userid), token);
            client_name = users.getClient_name();

            List<String> amc_list = new ArrayList<String>();
            List<NseOnlineSchemeMaster> scheme_list = null;

            BseNseKeyDto bse_list = userServiceClient.getByClientName(client_name,token);
            System.out.println("bse_list = " + bse_list);
            if(bse_list != null)
            {
                String amc_string = bse_list.getAmc_names();

                if(!amc_string.isEmpty())
                {
                    amc_list = new ArrayList<String>(Arrays.asList(amc_string.split(",")));
                }
            }

            System.out.println("amList = " + amc_list);
            if (amc_list != null && !amc_list.isEmpty())
            {

                scheme_list = nseOnlineSchemeMasterRepository.findSchemesByAmcNameAndStartDateNative(amc_list);
                if (scheme_list != null && !scheme_list.isEmpty())
                {
                    for (NseOnlineSchemeMaster scheme : scheme_list) {
                        String logo = amcLogoPath + NseUtils.getLogoByAmcNameOrSchemeName(scheme.getAmcCode());
                        scheme.setLogo(logo);
                    }

                }

            } else {
                scheme_list = nseOnlineSchemeMasterRepository.findSchemesWithStartDateTodayOrLater();
                System.out.println("scheme_list = " + scheme_list);
                if (scheme_list != null && !scheme_list.isEmpty())
                {
                    for (NseOnlineSchemeMaster scheme : scheme_list) {
                        String logo = amcLogoPath + NseUtils.getLogoByAmcNameOrSchemeName(scheme.getAmcCode());
                        scheme.setLogo(logo);
                    }

                }
            }

            return ResponseEntity.ok(scheme_list);
        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getNFOSchemecode")
    public ResponseEntity<?> getNFOSchemecode(@RequestHeader("Authorization") String token,
                                              @RequestParam String scheme,
                                              @RequestParam String dividend_type)
    {
        String userid = "";
        String client_name = "";
        try
        {
            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            UserDto users = userServiceClient.getUserById(Integer.valueOf(userid), token);
            client_name = users.getClient_name();

            scheme = NseUtils.checkParem(scheme);
            dividend_type = NseUtils.checkParem(dividend_type);

            List<NseOnlineSchemeMaster> list = null;
            NseOnlineSchemeMaster schemeMaster = null;

            if(StringHelper.isEmpty(dividend_type) || dividend_type.equalsIgnoreCase("Z"))
            {
                String dividend_type1 = "Z";
                list = nseOnlineSchemeMasterRepository.findBySchemeNameAndDivReinvestFlag(scheme,dividend_type1);

                if(list != null && list.size() > 0)
                {
                    schemeMaster = list.get(0);
                }
            } else
            {
                list = nseOnlineSchemeMasterRepository.findBySchemeNameAndDivReinvestFlag(scheme,dividend_type);

                if(list != null && list.size() > 0)
                {
                    schemeMaster = list.get(0);
                }else
                {
                    String dividend_type1 = "X";
                    list = nseOnlineSchemeMasterRepository.findBySchemeNameAndDivReinvestFlag(scheme,dividend_type1);

                    if(list != null && list.size() > 0)
                    {
                        schemeMaster = list.get(0);
                    }
                }

            }
            return ResponseEntity.ok(schemeMaster);

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get NFO SIP Schemes",
            description = "Retrieves NFO SIP Schemes based on the user's AMC eligibility and scheme start date.\n\n" +
                    "If the user's AMC list is available, only those AMC schemes are returned. " +
                    "Otherwise, all schemes with a start date from today onward are returned."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved list of NFO SIP Schemes",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = NseOnlineSchemeMaster.class)),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                    [
                      {
                        "id": 123,
                        "scheme_name": "Navi ELSS Tax Saver Fund",
                        "amc_name": "NAVIMUTUALFUND_MF",
                        "plan_type": "NORMAL",
                        "purchase_allowed": "Y",
                        "amc_active_flag": "Y",
                        "scheme_start_date": "29-07-2025"
                      }
                    ]
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Invalid token or input",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                    {
                      "error": "Invalid request or missing data"
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error - Failed to fetch schemes",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                    {
                      "error": "Error fetching Switch scheme names"
                    }
                    """
                            )
                    )
            )
    })
    @GetMapping("/getNFOSipSchemes")
    public ResponseEntity<?> getNFOSipSchemes(@RequestHeader("Authorization") String token)
    {
        String userid = "";
        String client_name = "";
        try
        {
            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            UserDto users = userServiceClient.getUserById(Integer.valueOf(userid), token);
            client_name = users.getClient_name();

            List<String> amc_list = new ArrayList<String>();
            List<NseOnlineSchemeMaster> scheme_list = null;

            BseNseKeyDto bse_list = userServiceClient.getByClientName(client_name,token);

            if(bse_list != null)
            {
                String amc_string = bse_list.getAmc_names();

                if(!amc_string.isEmpty())
                {
                    amc_list = new ArrayList<String>(Arrays.asList(amc_string.split(",")));
                }
            }
            System.out.println("amcList = " + amc_list);
            if (amc_list != null && !amc_list.isEmpty())
            {
                scheme_list = nseOnlineSchemeMasterRepository.findSIPSchemesByAmcNameAndStartDateNative(amc_list);
                if (scheme_list != null && !scheme_list.isEmpty())
                {
                    if (scheme_list != null && !scheme_list.isEmpty())
                    {
                        for (NseOnlineSchemeMaster scheme : scheme_list) {
                            String logo = amcLogoPath + NseUtils.getLogoByAmcNameOrSchemeName(scheme.getAmcCode());
                            scheme.setLogo(logo);
                        }
                    }
                }
            } else {
                scheme_list = nseOnlineSchemeMasterRepository.findSIPSchemesWithStartDateTodayOrLater();

                if (scheme_list != null && !scheme_list.isEmpty())
                {
                    if (scheme_list != null && !scheme_list.isEmpty())
                    {
                        for (NseOnlineSchemeMaster scheme : scheme_list) {
                            String logo = amcLogoPath + NseUtils.getLogoByAmcNameOrSchemeName(scheme.getAmcCode());
                            scheme.setLogo(logo);
                        }
                    }
                }
            }

            return ResponseEntity.ok(scheme_list);

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getNFOSipSchemecode")
    public ResponseEntity<?> getNFOSipSchemecode(@RequestHeader("Authorization") String token,
                                                 @RequestParam String scheme,
                                                 @RequestParam String dividend_type)
    {
        String userid = "";
        String client_name = "";
        try
        {
            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            UserDto users = userServiceClient.getUserById(Integer.valueOf(userid), token);
            client_name = users.getClient_name();

            scheme = NseUtils.checkParem(scheme);
            dividend_type = NseUtils.checkParem(dividend_type);

            List<NseOnlineSchemeMaster> list = null;
            NseOnlineSchemeMaster schemeMaster = null;

            if(StringHelper.isEmpty(dividend_type) || dividend_type.equalsIgnoreCase("Z"))
            {
                String dividend_type1 = "Z";
                list = nseOnlineSchemeMasterRepository.findEligibleSipSchemes(scheme,dividend_type1);

                if(list != null && list.size() > 0)
                {
                    schemeMaster = list.get(0);
                }
            } else
            {
                list = nseOnlineSchemeMasterRepository.findEligibleSipSchemes(scheme,dividend_type);

                if(list != null && list.size() > 0)
                {
                    schemeMaster = list.get(0);
                }else
                {
                    String dividend_type1 = "X";
                    list = nseOnlineSchemeMasterRepository.findEligibleSipSchemes(scheme,dividend_type1);

                    if(list != null && list.size() > 0)
                    {
                        schemeMaster = list.get(0);
                    }
                }

            }
            return ResponseEntity.ok(schemeMaster);

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get Folio Numbers By Scheme Code",
            description = "Fetches a list of folio numbers associated with a given scheme for the logged-in user. " +
                    "This helps determine the folios under which investments have been made for the specified scheme and Scheme name."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved folio numbers",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = String.class)),
                            examples = @ExampleObject(value = """
                [
                    "27947163/73",
                    "27947174/40"
                ]
            """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request – Required parameters are missing or invalid",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Invalid request. Please check scheme name and IIN number.")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User or folio details not found for the given parameters",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "No folio numbers found for the given scheme and IIN.")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error occurred while processing",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Error fetching folio numbers due to server error.")
                    )
            )
    })

    @GetMapping("/getFolioNumberBySchemeCode")
    public ResponseEntity<?> getFolioNumberBySchemeCode(
            @RequestParam String iin_number,
            @RequestParam String scheme_code,
            @RequestParam String scheme_name,
            @RequestHeader("Authorization") String token)
    {
        String userid = "";
        String client_name = "";

        try {
            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            UserDto users = userServiceClient.getUserById(Integer.valueOf(userid), token);
            client_name=users.getClient_name();

            iin_number = NseUtils.checkParem(iin_number);
            scheme_code = NseUtils.checkParem(scheme_code);
            scheme_name = NseUtils.checkParem(scheme_name);

            String tax_status_code = "";
            String holding_nature_code = "";
            String joint_holder_pan1 = "";
            String joint_holder_pan2 = "";

            UserDto user = null;
            try {
                user = userServiceClient.getUserDetailsByID(client_name, Integer.valueOf(userid),token);
            } catch (FeignException e)
            {
                if (e.status() == 400)
                {
                    System.out.println("Bad Request: " + e.getMessage());
                } else if (e.status() == 404)
                {
                    System.out.println("User not found: " + e.getMessage());
                } else
                {
                    System.out.println("Feign error: " + e.status() + " - " + e.getMessage());
                }
            }

            if (user != null && user.getNse_iin_number().equalsIgnoreCase(iin_number))
            {
                tax_status_code = user.getTax_status_code();
                holding_nature_code = user.getHolding_nature_code();
                joint_holder_pan1 = user.getJoint_holder_pan1();
                joint_holder_pan2 = user.getJoint_holder_pan2();
                if(tax_status_code == null){tax_status_code = "";}
                if(holding_nature_code == null){holding_nature_code = "";}
                if(joint_holder_pan1 == null){joint_holder_pan1 = "";}
                if(joint_holder_pan2 == null){joint_holder_pan2 = "";}
            }
            else
            {
                UserBseNseDto nse = null;
                try
                {
                    nse = userServiceClient.getUserBseNseDetailsByIinNumber(client_name, iin_number,token);
                }
                catch (FeignException.BadRequest ex)
                {
                    nse = null;
                }
                if (nse != null)
                {
                    tax_status_code = nse.getTax_status_code();
                    holding_nature_code = nse.getHolding_nature_code();
                    joint_holder_pan1 = nse.getJoint_holder_pan1();
                    joint_holder_pan2 = nse.getJoint_holder_pan2();
                    if(tax_status_code == null){tax_status_code = "";}
                    if(holding_nature_code == null){holding_nature_code = "";}
                    if(joint_holder_pan1 == null){joint_holder_pan1 = "";}
                    if(joint_holder_pan2 == null){joint_holder_pan2 = "";}
                }
            }

            List<String> folioList = nseService.getFolioNumberBySchemeCode(
                    client_name,
                    Integer.valueOf(userid),
                    scheme_code,
                    scheme_name,
                    holding_nature_code,
                    tax_status_code,
                    joint_holder_pan1,
                    joint_holder_pan2,token
            );
            return ResponseEntity.ok(folioList);

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get Purchase Scheme Details",
            description = "Fetches eligible purchase schemes for the user based on AMC code."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = NseOnlineSchemeMaster.class),
                            examples = @ExampleObject(
                                    name = "SuccessResponse",
                                    value = """
                                        [
                                          {
                                          "id": 4931,
                                          "uniqueSrNo": "39080",
                                          "schemeCode": "HD57N-GR-L0",
                                          "rtaSchemeCode": "57N",
                                          "amcSchemeCode": "57N",
                                          "isin": "INF179KB1HS3",
                                          "amcCode": "HDFC Mutual Fund",
                                          "amcName": "HDFCMUTUALFUND_MF",
                                          "schemeType": "LIQUID",
                                          "schemeCategory": "Debt: Overnight",
                                          "planType": "NORMAL",
                                          "schemeAmfiCode": "101996",
                                          "schemeName": "HDFC Overnight Fund - Growth Option",
                                          "schemeAmfiShortName": "HDFC Overnight Gr",
                                          "scheme": "HDFC OVERNIGHT FUND - REGULAR PLAN -  GROWTH",
                                          "purchaseAllowed": "Y",
                                          "purchaseTransactionMode": "DP",
                                          "newPurchaseMinAmount": 100.0,
                                          "additionalPurchaseMinAmount": 100.0,
                                          "additionalPurchaseMaxAmount": 9.999999999E9,
                                          "purchaseAmountMultiplier": 1.0,
                                          "purchaseCutoffTime": "13:00:00",
                                          "redemptionAllowed": "N",
                                          "redemptionTransactionMode": "DP",
                                          "redemptionMinQty": 0.001,
                                          "redemptionQtyMultiplier": 0.001,
                                          "redemptionMaxQty": 9.99999999E8,
                                          "redemptionMinAmount": 0.01,
                                          "redemptionMaxAmount": 9.999999999E9,
                                          "redemptionAmountMultiplier": 1.0,
                                          "redemptionCutoffTime": "15:00:00",
                                          "rtaAgentCode": "CAMS",
                                          "amcActiveFlag": "Y",
                                          "divReinvestFlag": "Z",
                                          "sipAllowed": "Y",
                                          "stpEnabled": "N",
                                          "swpEnabled": "N",
                                          "switchAllowed": "N",
                                          "settlementType": "L0",
                                          "amcInd": "",
                                          "faceValue": "0",
                                          "schemeStartDate": "01-01-2010",
                                          "maturityDate": "31-12-2099",
                                          "exitLoadFlag": "N",
                                          "exitLoad": "",
                                          "lockInPeriodFlag": "N",
                                          "lockInPeriod": "0",
                                          "channelPartnerCode": "H57N",
                                          "reopeningDate": "",
                                          "openCloseEndedScheme": "",
                                          "createdDate": "2025-06-26",
                                          "logo": null
                                          }
                                        ]
                                        """
                            )
                    )),
            @ApiResponse(responseCode = "400", description = "Bad Request - No record found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "BadRequestExample",
                                    value = """
                                        {
                                          "status": "BAD_REQUEST",
                                          "status_msg": "No record found for the given IIN Number and Client Name."
                                        }
                                        """
                            )
                    )),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "InternalServerError",
                                    value = """
                                        {
                                          "status": "INTERNAL_SERVER_ERROR",
                                          "status_msg": "Something went wrong, please try again later."
                                        }
                                        """
                            )
                    ))
    })
    @GetMapping("/getPurchaseSchemeDetails")
    public ResponseEntity<?> getPurchaseSchemeDetails(
            @RequestParam String amc_code,
            @RequestHeader("Authorization") String token)
    {
        String userid = "";
        String client_name = "";

        try {
            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            UserDto users = userServiceClient.getUserById(Integer.valueOf(userid), token);
            client_name = users.getClient_name();
            amc_code = NseUtils.checkParem(amc_code);

            List<NseOnlineSchemeMaster> master_list = null;
            List<String> list1 = null;

            try
            {
                list1 = userServiceClient.getDistinctSchemeAmfiCodeByUserAndAmc(amc_code, userid, client_name, token);
            } catch (FeignException e)
            {
                if (e.status() == 400)
                {
                    return NseUtils.commonResponse("No record found for the given IIN Number and Client Name.", HttpStatus.BAD_REQUEST);
                } else if (e.status() == 404)
                {
                    return NseUtils.commonResponse("User not found.", HttpStatus.NOT_FOUND);
                } else {
                    return NseUtils.commonResponse("Downstream service error.", HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }

            if (list1 != null && !list1.isEmpty())
            {
                master_list = nseOnlineSchemeMasterRepository.findValidSchemesByAmfiCodes(list1);
            }

            return ResponseEntity.ok(master_list);
        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get Existing Scheme Holdings",
            description = "Fetches existing scheme holdings for an investor using the provided Authorization token. " +
                    "If no holdings are found initially, it fetches the investor's portfolio with live holdings " +
                    "and returns the updated scheme-wise portfolio."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully fetched scheme holdings",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NseOnlineSchemeMaster.class),
                            examples = @ExampleObject(
                                    name = "Success Example",
                                    summary = "Sample Scheme Holdings Response",
                                    value = """
                    {
                         "scheme": "BANDHAN SMALL CAP FUND - REGULAR PLAN GROWTH",
                         "scheme_amfi_short_name": null,
                         "scheme_class": null,
                         "scheme_code": "G340",
                         "scheme_registrar": "cams",
                         "scheme_amfi_common": null,
                         "amc_code": null,
                         "amc_name": null,
                         "foliono": "5377185/30",
                         "amc_logo": null,
                         "scheme_weight": null,
                         "scheme_rating": null,
                         "scheme_score": null,
                         "scheme_review": null,
                         "scheme_benchmark_name": null,
                         "scheme_benchmark_code": null,
                         "scheme_risk_profile": null,
                         "notes": null
                    }
                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "{\"message\": \"Something went wrong, We have taken note of the issue. Be rest assured we will fix it as soon as possible.\"}")
                    )
            )
    })
    @GetMapping("/getExistingSchemes")
    public ResponseEntity<?> getExistingSchemes(@RequestHeader("Authorization") String token,@RequestParam(required = false) String iin_number, @RequestParam(required = false) String broker_code)
    {
        String userid = "";
        String client_name = "";
        try
        {
            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            UserDto users = null;

            broker_code = NseUtils.checkParem(broker_code);
            iin_number = NseUtils.checkParem(iin_number);
            client_name = TokenInterceptor.extractClientNamedFromToken(token, secretKey);

            if(!broker_code.isEmpty() && !iin_number.isEmpty())
            {
                users = userServiceClient.getUserDetailsByIinNumberAndBrokercode(broker_code, iin_number,client_name,userid, token);
            }else
            {
                users = userServiceClient.getUserById(Integer.valueOf(userid), token);
            }

           List<InvestorSchemeWisePortfolioResponse> portfolio_list = nseService.getSchemeHoldings(client_name, Integer.valueOf(userid),users,broker_code,token);
            System.out.println("portfolio_list = " + portfolio_list.size());
            if(portfolio_list == null || portfolio_list.size() == 0)
            {
                InvestorPortfolioResponse portfolio = nseService.getInvestorPortfolioWithLiveHoldings(Integer.parseInt(userid), client_name,token);
                if(portfolio != null)
                {
                    List<InvestorSchemeWisePortfolioResponse> scheme_list = portfolio.getInvestorSchemeWisePortfolioResponses();
                    if(scheme_list != null && scheme_list.size() > 0)
                    {
                        portfolio_list =  nseService.getSchemeHolding(Integer.parseInt(userid), client_name, scheme_list,token);
                    }
                }
            }

            if(portfolio_list != null && !portfolio_list.isEmpty())
            {
                portfolio_list.forEach(scheme ->
                {
                    scheme.setAmc_logo(
                            amcLogoPath + NseUtils.getLogoByAmcNameOrSchemeName(scheme.getScheme())
                    );
                });
            }
            for(InvestorSchemeWisePortfolioResponse scheme : portfolio_list)
            {
                scheme.setOptionEnable(isOptionEnabled(scheme, users, broker_code));
            }

            return ResponseEntity.ok(portfolio_list);
        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isOptionEnabled(InvestorSchemeWisePortfolioResponse scheme, UserDto users, String broker_code)
    {
        if(users == null || scheme == null)
        {
            return false;
        }
//        System.out.println("scheme is Demant = " + scheme.getIsDeamtAccount());
//        System.out.println("scheme NAme = " + scheme.getScheme());
        if(Boolean.TRUE.equals(scheme.getIsDeamtAccount()))
        {
            return false;
        }

        String tax_status_code     = users.getTax_status_code();
        String holding_nature_code = users.getHolding_nature_code();
        String joint_holder_pan1   = users.getJoint_holder_pan1();
        String joint_holder_pan2   = users.getJoint_holder_pan2();

//        System.out.println("broker_code = " + broker_code);
//        System.out.println("tax_status_code = " + tax_status_code);
//        System.out.println("holding_nature_code = " + holding_nature_code);
//        System.out.println("joint_holder_pan1 = " + joint_holder_pan1);
//        System.out.println("joint_holder_pan2 = " + joint_holder_pan2);
//
//        System.out.println("scheme tax_status_code = " + scheme.getTax_status_code());
//        System.out.println("scheme holding_nature = " + scheme.getHolding_nature());
//        System.out.println("scheme joint1_pan = " + scheme.getJoint1_pan());
//        System.out.println("scheme joint2_pan = " + scheme.getJoint2_pan());
//        System.out.println("scheme.getBroker_code() + " + scheme.getBroker_code());

        if(!"01".equals(tax_status_code) && !"24".equals(scheme.getTax_status_code()) && !"21".equals(scheme.getTax_status_code()))
        {
            return true;
        }

//        System.out.println("mohan");

        if(("24".equals(tax_status_code) || "21".equals(tax_status_code)) && equalsSafe(scheme.getBroker_code(), broker_code) && tax_status_code.equals(scheme.getTax_status_code())
                && holdingNatureMatches(scheme.getHolding_nature(), holding_nature_code) && panMatches(scheme.getJoint1_pan(), joint_holder_pan1) && panMatches(scheme.getJoint2_pan(), joint_holder_pan2))
        {
            return true;
        }
//        System.out.println("gowtham");

//        System.out.println("01".equals(tax_status_code)
//                && !"24".equals(scheme.getTax_status_code())
//                && !"21".equals(scheme.getTax_status_code()));
//
//        System.out.println("01".equals(tax_status_code)
//                && !"24".equals(scheme.getTax_status_code())
//                && !"21".equals(scheme.getTax_status_code())
//                && equalsSafe(scheme.getBroker_code(), broker_code));
//
//        System.out.println("01".equals(tax_status_code)
//                && !"24".equals(scheme.getTax_status_code())
//                && !"21".equals(scheme.getTax_status_code())
//                && equalsSafe(scheme.getBroker_code(), broker_code)
//                && holdingNatureMatches(scheme.getHolding_nature(), holding_nature_code));
//
//        System.out.println("01".equals(tax_status_code)
//                && !"24".equals(scheme.getTax_status_code())
//                && !"21".equals(scheme.getTax_status_code())
//                && equalsSafe(scheme.getBroker_code(), broker_code)
//                && holdingNatureMatches(scheme.getHolding_nature(), holding_nature_code)
//                && panMatches(scheme.getJoint1_pan(), joint_holder_pan1));
//
//        System.out.println("01".equals(tax_status_code)
//                && !"24".equals(scheme.getTax_status_code())
//                && !"21".equals(scheme.getTax_status_code())
//                && equalsSafe(scheme.getBroker_code(), broker_code)
//                && holdingNatureMatches(scheme.getHolding_nature(), holding_nature_code)
//                && panMatches(scheme.getJoint1_pan(), joint_holder_pan1)
//                && panMatches(scheme.getJoint2_pan(), joint_holder_pan2));

        if("01".equals(tax_status_code)
                && !"24".equals(scheme.getTax_status_code())
                && !"21".equals(scheme.getTax_status_code())
                && equalsSafe(scheme.getBroker_code(), broker_code)
                && holdingNatureMatches(scheme.getHolding_nature(), holding_nature_code)
                && panMatches(scheme.getJoint1_pan(), joint_holder_pan1)
                && panMatches(scheme.getJoint2_pan(), joint_holder_pan2))
        {
            return true;
        }

        return false;
    }

    private boolean holdingNatureMatches(String schemeHoldingNature, String holdingNatureCode)
    {
        if(schemeHoldingNature == null || holdingNatureCode == null) return false;
        return schemeHoldingNature.equals(holdingNatureCode)
                || ("AS".equals(schemeHoldingNature) && "ES".equals(holdingNatureCode))
                || ("ES".equals(schemeHoldingNature) && "AS".equals(holdingNatureCode));
    }

    private boolean panMatches(String schemePan, String userPan)
    {
        String a = schemePan == null ? "" : schemePan.trim().toUpperCase();
        String b = userPan == null ? "" : userPan.trim().toUpperCase();
        return a.equals(b);
    }

    private boolean equalsSafe(String a, String b)
    {
        if(a == null || b == null) return false;
        return a.equals(b);
    }

    @Operation(
            summary = "Get STP Scheme Code",
            description = "Fetches the eligible scheme details for redemption based on AMC code, scheme name, and dividend code."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved STP scheme details",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NseOnlineSchemeMaster.class),
                            examples = @ExampleObject(
                                    name = "Success Example",
                                    summary = "Sample STP Scheme Response",
                                    value = """
                    {
                        "id": 4937,
                        "uniqueSrNo": "22339",
                        "schemeCode": "HD57N-GR",
                        "rtaSchemeCode": "57N",
                        "amcSchemeCode": "57N",
                        "isin": "INF179KB1HS3",
                        "amcCode": "HDFC Mutual Fund",
                        "amcName": "HDFCMUTUALFUND_MF",
                        "schemeType": "OVERNIGHT",
                        "schemeCategory": "Debt: Overnight",
                        "planType": "NORMAL",
                        "schemeAmfiCode": "101996",
                        "schemeName": "HDFC Overnight Fund - Growth Option",
                        "schemeAmfiShortName": "HDFC Overnight Gr",
                        "scheme": "HDFC OVERNIGHT FUND - REGULAR PLAN -  GROWTH",
                        "purchaseAllowed": "Y",
                        "purchaseTransactionMode": "DP",
                        "newPurchaseMinAmount": 100.0,
                        "additionalPurchaseMinAmount": 100.0,
                        "additionalPurchaseMaxAmount": 9999999999.0,
                        "purchaseAmountMultiplier": 1.0,
                        "purchaseCutoffTime": "14:30:00",
                        "redemptionAllowed": "Y",
                        "redemptionTransactionMode": "DP",
                        "redemptionMinQty": 0.001,
                        "redemptionQtyMultiplier": 0.001,
                        "redemptionMaxQty": 999999999.0,
                        "redemptionMinAmount": 100.0,
                        "redemptionMaxAmount": 9999999999.0,
                        "redemptionAmountMultiplier": 1.0,
                        "redemptionCutoffTime": "19:00:00",
                        "rtaAgentCode": "CAMS",
                        "amcActiveFlag": "Y",
                        "divReinvestFlag": "Z",
                        "sipAllowed": "Y",
                        "stpEnabled": "Y",
                        "swpEnabled": "Y",
                        "switchAllowed": "Y",
                        "settlementType": "T1",
                        "faceValue": "0",
                        "schemeStartDate": "01-01-2010",
                        "maturityDate": "31-12-2099",
                        "exitLoadFlag": "N",
                        "lockInPeriodFlag": "N",
                        "channelPartnerCode": "H57N",
                        "createdDate": "2025-06-26"
                    }
                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = MediaType.TEXT_PLAIN_VALUE,
                            examples = @ExampleObject(value = "Error fetching STP scheme names")
                    )
            )
    })
    @GetMapping("/getStpSchemecode")
    public ResponseEntity<?> getStpSchemecode(@RequestHeader("Authorization") String token,
                                              @RequestParam String scheme,
                                              @RequestParam(required = false) String amc_code,
                                              @RequestParam String dividend_code)
    {
        try
        {
            NseOnlineSchemeMaster schemeMaster = null;
            if(NseUtils.isUrlEncoded(scheme))
            {
                scheme = URLDecoder.decode(scheme, StandardCharsets.UTF_8);
            }
            List<NseOnlineSchemeMaster> list = nseOnlineSchemeMasterRepository.findSTPEnabledSchemes(scheme,dividend_code);
            if(list.size() > 0)
            {
                schemeMaster = list.get(0);
            }
            return ResponseEntity.ok(schemeMaster);
        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get SWP Scheme Code",
            description = "Fetches the eligible scheme details for redemption based on AMC code, scheme name, and dividend code."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved SWP scheme details",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NseOnlineSchemeMaster.class),
                            examples = @ExampleObject(
                                    name = "Success Example",
                                    summary = "Sample SWP Scheme Response",
                                    value = """
                    {
                        "id": 4937,
                        "uniqueSrNo": "22339",
                        "schemeCode": "HD57N-GR",
                        "rtaSchemeCode": "57N",
                        "amcSchemeCode": "57N",
                        "isin": "INF179KB1HS3",
                        "amcCode": "HDFC Mutual Fund",
                        "amcName": "HDFCMUTUALFUND_MF",
                        "schemeType": "OVERNIGHT",
                        "schemeCategory": "Debt: Overnight",
                        "planType": "NORMAL",
                        "schemeAmfiCode": "101996",
                        "schemeName": "HDFC Overnight Fund - Growth Option",
                        "schemeAmfiShortName": "HDFC Overnight Gr",
                        "scheme": "HDFC OVERNIGHT FUND - REGULAR PLAN -  GROWTH",
                        "purchaseAllowed": "Y",
                        "purchaseTransactionMode": "DP",
                        "newPurchaseMinAmount": 100.0,
                        "additionalPurchaseMinAmount": 100.0,
                        "additionalPurchaseMaxAmount": 9999999999.0,
                        "purchaseAmountMultiplier": 1.0,
                        "purchaseCutoffTime": "14:30:00",
                        "redemptionAllowed": "Y",
                        "redemptionTransactionMode": "DP",
                        "redemptionMinQty": 0.001,
                        "redemptionQtyMultiplier": 0.001,
                        "redemptionMaxQty": 999999999.0,
                        "redemptionMinAmount": 100.0,
                        "redemptionMaxAmount": 9999999999.0,
                        "redemptionAmountMultiplier": 1.0,
                        "redemptionCutoffTime": "19:00:00",
                        "rtaAgentCode": "CAMS",
                        "amcActiveFlag": "Y",
                        "divReinvestFlag": "Z",
                        "sipAllowed": "Y",
                        "stpEnabled": "Y",
                        "swpEnabled": "Y",
                        "switchAllowed": "Y",
                        "settlementType": "T1",
                        "faceValue": "0",
                        "schemeStartDate": "01-01-2010",
                        "maturityDate": "31-12-2099",
                        "exitLoadFlag": "N",
                        "lockInPeriodFlag": "N",
                        "channelPartnerCode": "H57N",
                        "createdDate": "2025-06-26"
                    }
                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = MediaType.TEXT_PLAIN_VALUE,
                            examples = @ExampleObject(value = "Error fetching SWP scheme names")
                    )
            )
    })
    @GetMapping("/getSwpSchemecode")
    public ResponseEntity<?> getSwpSchemecode(@RequestHeader("Authorization") String token,
                                              @RequestParam String scheme,
                                              @RequestParam(required = false) String amc_code,
                                              @RequestParam String dividend_code)
    {
        try
        {
            NseOnlineSchemeMaster schemeMaster = null;
            if(NseUtils.isUrlEncoded(scheme))
            {
                scheme = URLDecoder.decode(scheme, StandardCharsets.UTF_8);
            }
            List<NseOnlineSchemeMaster> list = nseOnlineSchemeMasterRepository.findSWPEnabledSchemes(scheme,dividend_code);
            if(list.size() > 0)
            {
                schemeMaster = list.get(0);
            }

            return ResponseEntity.ok(schemeMaster);
        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get NFO Scheme Reinvest Tag",
            description = "Returns the reinvest tag for a given AMC name and scheme code if available."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Reinvest tag fetched successfully",
                    content = @Content(
                            mediaType = MediaType.TEXT_PLAIN_VALUE,
                            examples = @ExampleObject(value = "REINVEST_TAG_SAMPLE")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input or missing parameters",
                    content = @Content(
                            mediaType = MediaType.TEXT_PLAIN_VALUE,
                            examples = @ExampleObject(value = "Bad request")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error while fetching reinvest tag",
                    content = @Content(
                            mediaType = MediaType.TEXT_PLAIN_VALUE,
                            examples = @ExampleObject(value = "Error fetching SIP scheme names")
                    )
            )
    })
    @GetMapping("/getNfoSchemeReinvestTag")
    public ResponseEntity<?> getNfoSchemeReinvestTag(@RequestHeader("Authorization") String token,
                                                     @RequestParam String amc_name,
                                                     @RequestParam String to_scheme_code)
    {
        String reinvest_tag = "";
        try
        {
            amc_name = NseUtils.checkParem(amc_name);
            to_scheme_code = NseUtils.checkParem(to_scheme_code);

            String nfo_list = nseOnlineSchemeMasterRepository.findReinvestTagByAmcNameAndSchemeCode(amc_name,to_scheme_code);

            if(nfo_list != null)
            {
                reinvest_tag = nfo_list;
            }

            return ResponseEntity.ok(reinvest_tag);
        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Validate STP Amount Parameters",
            description = "This endpoint validates and retrieves SIP details from the NSE STP master based on the following input parameters:\n\n" +
                    "- AMC Code (`amc_code`): The Asset Management Company code.\n" +
                    "- Scheme Code (`scheme_code`): The scheme identifier for the mutual fund.\n" +
                    "- SIP Frequency (`sip_frequency`): The frequency at which SIPs are to be executed (e.g., Monthly, Quarterly).\n\n" +
                    "Returns SIP constraints such as minimum amount, maximum amount, multiplier, etc., based on the given inputs."
    )

    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "SIP scheme data retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = NseOnlineSipStpSwpMaster.class)))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request – Required parameters are missing or invalid",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "scheme_code, amc_code, and sip_frequency are required"))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error – Could not fetch SIP scheme data",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "Error fetching STP scheme"))
            )
    })
    @GetMapping("/validateStpAmount")
    public ResponseEntity<?> validateStpAmount(@RequestParam String scheme_code,
                                               @RequestParam String amc_code)
    {

        List<NseOnlineSipStpSwpMaster> list = null;
        NseOnlineSipStpSwpMaster schemeMasterLimit = null;
        try
        {

            if(scheme_code == null){scheme_code = "";};
            if(amc_code == null){amc_code = "";};

            scheme_code = scheme_code.trim();
            amc_code = amc_code.trim();

             list= nseOnlineSipStpSwpMasterRepository.findFirstByAmcNameAndSchemeCodeForStp(amc_code, scheme_code);

            if(list != null && list.size() > 0)
            {
                schemeMasterLimit = list.get(0);
            }

            return ResponseEntity.ok(schemeMasterLimit);
        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getInvestorCode")
    public ResponseEntity<?> getInvestorCode(@RequestHeader("Authorization") String token)
    {
        String userid = "";
        String client_name = "";
        List<InvestorClientCodePojo> list = new ArrayList<InvestorClientCodePojo>();
        InvestorClientCodePojo code = null;
        String investor_code = "";
        String path = "";
        Integer reg_id = 0;
        try {
            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            UserDto users =null;
            System.out.println("userid = " + userid);
            try {
                users = userServiceClient.getUserById(Integer.valueOf(userid), token);
            } catch (FeignException e) {
                return FeignErrorHandler.handle(e, "User Service", "User not found");
            }
            client_name = users.getClient_name();
            System.out.println("usersss = " + users);
            UserDto user = null;
            try {
                user =  userServiceClient.getUserDetailsByID(client_name, Integer.valueOf(userid),token);
            }catch (FeignException e)
            {
                return FeignErrorHandler.handle(e, "User Service", "User not found");
            }

            if (user == null) {
                return NseUtils.commonResponse("User Details not available", HttpStatus.BAD_REQUEST);
            }
            System.out.println("user = " + user);
            Integer nse_active = user.getNse_active();
            String tax_status = user.getTax_status();
            String tax_status_code = user.getTax_status_code();
            String holding_nature = user.getHolding_nature();
            String holding_nature_code = user.getHolding_nature_code();
            String broker_code = user.getBroker_code();
            String inv_name = user.getName();
            reg_id = user.getId();

            investor_code = user.getNse_iin_number();
            path = NseUtils.getVendorImage("NSE");

            code = new InvestorClientCodePojo();
            code.setInv_name(inv_name);
            code.setTax_status(tax_status);
            code.setTax_status_code(tax_status_code);
            code.setHolding_nature(holding_nature);
            code.setHolding_nature_code(holding_nature_code);
            code.setBroker_code(broker_code);
            code.setInvestor_code(investor_code);
            code.setReg_id(reg_id);
            code.setLogo(vendorLogoPath + path);
            code.setBse_nse_mfu_flag("NSE");
            list.add(code);

            List<String> investorCodeArray = new ArrayList<String>();
            List<String> pathArray = new ArrayList<String>();
            List<String> vendorArray = new ArrayList<String>();

            List<UserDto> user_list = null;
            try {
                user_list = userServiceClient.getUserByIdAndClientNameActiveNse(client_name, Integer.valueOf(userid), token);
            }catch (FeignException e)
            {
                return FeignErrorHandler.handle(e, "User Service", "User not found");
            }
            System.out.println("userList = " + user_list);

            if (user_list != null && user_list.size() > 0)
            {
                for (UserDto nse : user_list)
                {
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
                    reg_id = nse.getId();

                    investor_code = nse.getNse_iin_number();
                    path = NseUtils.getVendorImage("NSE");

                    code = new InvestorClientCodePojo();
                    code.setInv_name(inv_name);
                    code.setTax_status(tax_status);
                    code.setTax_status_code(tax_status_code);
                    code.setHolding_nature(holding_nature);
                    code.setHolding_nature_code(holding_nature_code);
                    code.setBroker_code(broker_code);
                    code.setInvestor_code(investor_code);
                    code.setLogo(vendorLogoPath + path);
                    code.setReg_id(reg_id);
                    code.setBse_nse_mfu_flag("NSE");
                    list.add(code);
                }

            }
            InvestorClientCodeResponse apiResponse = new InvestorClientCodeResponse();
            apiResponse.setStatus(200);
            apiResponse.setStatus_msg("Success");
            apiResponse.setMsg("");
            apiResponse.setClient_code_list(list);
            return new ResponseEntity<InvestorClientCodeResponse>(apiResponse, HttpStatus.OK);

        } catch (Exception e) {
            System.out.println("getInvestorCode = " + e);
            e.printStackTrace();
            return NseUtils.commonResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/getLumpsumSchemeByAmc")
    public ResponseEntity<?> getLumpsumSchemeByAmc(@RequestHeader("Authorization") String token,@RequestParam String option)
    {
        try
        {
            option = NseUtils.checkParem(option);
            String client_name = TokenInterceptor.extractClientNamedFromToken(token,secretKey);
            if(option.isEmpty())
            {
                return NseUtils.commonResponse("Please enter a Option", HttpStatus.BAD_REQUEST);
            }
            String optionString = "";
            if(option.equalsIgnoreCase("growth"))
            {
                optionString = "Z";
            }else if(option.equalsIgnoreCase("dividend payout"))
            {
                optionString = "N";
            }else if(option.equalsIgnoreCase("dividend reinvestment"))
            {
                optionString = "Y";
            }else if(option.equalsIgnoreCase("SIF"))
            {
                optionString = "SIF";
            }

            List<NewSchemePojo> schemeList = new ArrayList<>();
            List<Object[]> filteredSchemes = null;
            List<String> amc_list = new ArrayList<String>();
            BseNseKeyDto nsekey = null;

            try {
                nsekey = userServiceClient.getByClientName(client_name,token);
            } catch (FeignException e) {
                if (e.status() == 400) {
                    return NseUtils.commonResponse("No record found for the given IIN Number and Client Name.", HttpStatus.BAD_REQUEST);
                } else if (e.status() == 404) {
                    return NseUtils.commonResponse("User not found.", HttpStatus.NOT_FOUND);
                } else {
                    return NseUtils.commonResponse("Downstream service error.", HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }

            if(nsekey != null)
            {
                String amc_string = nsekey.getAmc_names();

                if(!amc_string.isEmpty())
                {
                    amc_list = new ArrayList<String>(Arrays.asList(amc_string.split(",")));
                }
            }

            if(!amc_list.isEmpty())
            {
                if(optionString.equalsIgnoreCase("SIF"))
                {
                    filteredSchemes = nseOnlineSchemeMasterRepository.getAllLumpsumSchemesBySifWithAmc(amc_list);
                }else
                {
                    filteredSchemes = nseOnlineSchemeMasterRepository.getAllLumpsumSchemesByOptionWithAmc(optionString,amc_list);
                }
            }else
            {
                if(optionString.equalsIgnoreCase("SIF"))
                {
                    filteredSchemes = nseOnlineSchemeMasterRepository.getAllLumpsumSchemesBySif();
                }else
                {
                    filteredSchemes = nseOnlineSchemeMasterRepository.getAllLumpsumSchemesByOption(optionString);
                }
            }

            if (filteredSchemes != null && !filteredSchemes.isEmpty()) {
                schemeList = filteredSchemes.stream().map(row -> {
                            String schemeName = (String) row[0];
                            String scheme = (String) row[1];
                            String category = (String) row[2];
                            String amc_code = (String) row[3];
                            String amc_name = (String) row[4];
                            String logo = amcLogoPath + NseUtils.getLogoByAmcNameOrSchemeName(amc_code);
                            return new NewSchemePojo(schemeName,scheme, category, amc_code, amc_name, "", logo);
                        })
                        .sorted(Comparator.comparing(NewSchemePojo::getScheme_name, String.CASE_INSENSITIVE_ORDER))
                        .collect(Collectors.toList());
            }

            return ResponseEntity.ok(schemeList);

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getMandateDetailsByClientCode")
    public ResponseEntity<?> getMandateDetailsByClientCode(
            @RequestHeader("Authorization") String token,
            @RequestParam String client_code,
            @RequestParam String broker_code) {

        try
        {
            String userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            String client_name = TokenInterceptor.extractClientNamedFromToken(token, secretKey);

            UserDto user =null;

            try {
                user = userServiceClient.getUserById(Integer.valueOf(userid), token);
            } catch (FeignException e) {
                return FeignErrorHandler.handle(e, "User Service", "User not found");
            }

            List<UserMandateDetailsDto> mandateDetails =
                    userServiceClient.getMandateDetailsByBrokerCode(
                            Integer.valueOf(userid), client_name, client_code, broker_code, token);
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>)
                            (src, typeOfSrc, context) -> new JsonPrimitive(src.toString()))
                    .create();

            System.out.println("mandateDetails:1 " + gson.toJson(mandateDetails));

            List<UsersBankDetailsDTO>  bankDetails =
                    userServiceClient.getBankDetailsByBrokerCode(
                            Integer.valueOf(userid), client_name, client_code, broker_code, token);
            System.out.println("bankDetails " + gson.toJson(bankDetails));
            Set<String> mandateAccounts = mandateDetails.stream()
                    .map(UserMandateDetailsDto::getBank_account_number)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            System.out.println("mandateAccounts " + mandateAccounts);
            Set<String> bankAccounts = bankDetails.stream()
                    .map(UsersBankDetailsDTO::getBank_account_number)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            System.out.println("bankAccounts " + bankAccounts);
            Set<String> missingAccounts = new HashSet<>(mandateAccounts);
            missingAccounts.removeAll(bankAccounts);

            System.out.println("missingAccounts = " + missingAccounts);

            if (!missingAccounts.isEmpty()) {
                JSONObject requestDetails = new JSONObject();
                requestDetails.put("client_code", client_code);
                requestDetails.put("from_date", "");
                requestDetails.put("to_date", "");
                requestDetails.put("pan", "");

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

                HttpEntity<String> entity = new HttpEntity<>(requestDetails.toString(), headers);

                String clientMasterReportApi_url = NseApiUrls.clientMasterReport_url;

                ResponseEntity<String> mandateResult = RestTemplateFactory.createRestTemplate().postForEntity(clientMasterReportApi_url, entity, String.class);
                String responseBody = mandateResult.getBody();

                JSONObject jsonObject = new JSONObject(responseBody);
                JSONArray regDataArray = new JSONArray();

                if (jsonObject.has("report_data")) {
                    regDataArray = jsonObject.getJSONArray("report_data");
                }

                System.out.println("jsonObject = " + jsonObject);

                Map<String, Map<String, String>> missingBankAccountsMap = new HashMap<>();

                for (int i = 0; i < regDataArray.length(); i++) {
                    JSONObject jsonObjects = regDataArray.getJSONObject(i);

                    for (int j = 1; j <= 5; j++) {
                        String accNoKey = "account_no_" + j;
                        String accTypeKey = "account_type_" + j;
                        String micrKey = "micr_no_" + j;
                        String ifscKey = "ifsc_code_" + j;
                        String bankNameKey = "bank_name_" + j;
                        String branchKey = "bank_branch_" + j;
                        String defaultKey = "default_bank_flag_" + j;
                        String createdKey = "bank_" + j + "_created_at";
                        String modifiedKey = "bank_" + j + "_last_modified_at";
                        String statusKey = "bank_" + j + "_status";
                        String remarksKey = "bank_" + j + "_status_remarks";

                        if (jsonObjects.has(accNoKey)) {
                            String accountNo = jsonObjects.optString(accNoKey, "").trim();
                            if (!accountNo.isEmpty() && missingAccounts.contains(accountNo)) {
                                Map<String, String> bankDetail = new HashMap<>();
                                bankDetail.put("account_type", jsonObjects.optString(accTypeKey, ""));
                                bankDetail.put("account_no", accountNo);
                                bankDetail.put("micr_no", jsonObjects.optString(micrKey, ""));
                                bankDetail.put("ifsc_code", jsonObjects.optString(ifscKey, ""));
                                bankDetail.put("bank_name", jsonObjects.optString(bankNameKey, ""));
                                bankDetail.put("bank_branch", jsonObjects.optString(branchKey, ""));
                                bankDetail.put("default_bank_flag", jsonObjects.optString(defaultKey, ""));
                                bankDetail.put("created_at", jsonObjects.optString(createdKey, ""));
                                bankDetail.put("last_modified_at", jsonObjects.optString(modifiedKey, ""));
                                bankDetail.put("status", jsonObjects.optString(statusKey, ""));
                                bankDetail.put("status_remarks", jsonObjects.optString(remarksKey, ""));

                                missingBankAccountsMap.put(accountNo, bankDetail);
                            }
                        }
                    }
                }

                List<BankDto> bankDtos = new ArrayList<>();

                for (Map.Entry<String, Map<String, String>> entry : missingBankAccountsMap.entrySet()) {
                    String accountNo = entry.getKey();
                    Map<String, String> details = entry.getValue();

                    BankDto bankInfo = new BankDto();
                    bankInfo.setUser_id(Integer.valueOf(userid));
                    bankInfo.setOnline_id(user.getId());
                    bankInfo.setOnline_code(client_code);
                    bankInfo.setOnline_flag("NSE");
                    bankInfo.setBroker_code(broker_code);
                    bankInfo.setClient_name(client_name);

                    bankInfo.setBank_account_number(accountNo);
                    bankInfo.setBank_account_type(details.getOrDefault("account_type", ""));
                    bankInfo.setBank_name(details.getOrDefault("bank_name", ""));
                    bankInfo.setBank_branch(details.getOrDefault("bank_branch", ""));
                    bankInfo.setBank_ifsc_code(details.getOrDefault("ifsc_code", ""));
                    bankInfo.setBank_micr_code(details.getOrDefault("micr_no", ""));
                    bankInfo.setDefault_bank(details.getOrDefault("default_bank_flag", ""));
                    bankInfo.setCreated_date(new Date());


                    bankDtos.add(bankInfo);
                }

                userServiceClient.saveBankMandateDetails(bankDtos, token);

                bankDetails = userServiceClient.getBankDetailsByBrokerCode(
                        Integer.valueOf(userid), client_name, client_code, broker_code, token
                );
            }

            // Group mandates by bank account number
            Map<String, List<UserMandateDetailsDto>> mandatesByAccount = mandateDetails.stream()
                    .collect(Collectors.groupingBy(UserMandateDetailsDto::getBank_account_number));
            System.out.println("mandatesByAccount = " + mandatesByAccount);

            List<UserMandateDetailsResponse> responseList = bankDetails.stream()
                    .flatMap(b -> {
                        // Get mandates for this bank account
                        List<UserMandateDetailsDto> matchingMandates =
                                mandatesByAccount.getOrDefault(
                                        b.getBank_account_number(),
                                        Collections.emptyList()
                                );

                        // ✅ If NO mandate → return bank with default values
                        if (matchingMandates.isEmpty()) {
                            return Stream.of(createResponseWithDefaults(b));
                        }

                        // ✅ If mandate exists → map each mandate
                        return matchingMandates.stream()
                                .map(mandate -> createResponseWithMandate(b, mandate));
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responseList);

        } catch (GeneralSecurityException | RuntimeException ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private UserMandateDetailsResponse createResponseWithDefaults(UsersBankDetailsDTO b) {
        UserMandateDetailsResponse resp = new UserMandateDetailsResponse();
        mapBankDetails(resp, b);
        setDefaultMandateValues(resp);
        return resp;
    }

    private UserMandateDetailsResponse createResponseWithMandate(UsersBankDetailsDTO b, UserMandateDetailsDto m) {
        UserMandateDetailsResponse resp = new UserMandateDetailsResponse();
        mapBankDetails(resp, b);
        mapMandateDetails(resp, m);
        return resp;
    }

    private void mapBankDetails(UserMandateDetailsResponse resp, UsersBankDetailsDTO b) {
        resp.setId(b.getId());
        resp.setUser_id(b.getUser_id());
        resp.setOnline_id(b.getOnline_id());
        resp.setOnline_flag(b.getOnline_flag());
        resp.setOnline_code(b.getOnline_code());
        resp.setBroker_code(b.getBroker_code());
        resp.setBank_name(b.getBank_name());
        resp.setBank_branch(b.getBank_branch());
        resp.setBank_address(b.getBank_address());
        resp.setBank_account_holder_name(b.getBank_account_holder_name());
        resp.setBank_account_type(b.getBank_account_type());
        resp.setBank_ifsc_code(b.getBank_ifsc_code());
        resp.setBank_micr_code(b.getBank_micr_code());
        resp.setBank_proof(b.getBank_proof());
        resp.setBank_account_number(b.getBank_account_number());
    }

    private void setDefaultMandateValues(UserMandateDetailsResponse resp) {
        resp.setXsip_otm_flag(0);
        resp.setXsip_otm("");
        resp.setXsip_otm_amount("");
        resp.setXsip_otm_approved(0);
        resp.setXsip_otm_rej_reason("");

        resp.setEmandate_otm_flag(0);
        resp.setEmandate_otm("");
        resp.setEmandate_otm_amount("");
        resp.setEmandate_otm_approved(0);
        resp.setEmandate_otm_rej_reason("");

        resp.setNse_ach_flag(0);
        resp.setNse_ach("");
        resp.setNse_ach_amount("");
        resp.setNse_ach_approved(0);
        resp.setNse_ach_rej_reason("");

        resp.setMfu_mandate_flag(0);
        resp.setMfu_mandate("");
        resp.setMfu_mandate_mode("");
        resp.setMfu_mmrn_no("");
        resp.setMfu_mandate_amount("");
        resp.setMfu_mandate_approved(0);
        resp.setMfu_mandate_rej_reason("");

        resp.setClient_name("");
    }

    private void mapMandateDetails(UserMandateDetailsResponse resp, UserMandateDetailsDto m) {
        resp.setBank_account_number(m.getBank_account_number());

        resp.setXsip_otm_flag(m.getXsip_otm_flag());
        resp.setXsip_otm(m.getXsip_otm());
        resp.setXsip_otm_amount(m.getXsip_otm_amount());
        resp.setXsip_otm_approved(m.getXsip_otm_approved());
        resp.setXsip_otm_rej_reason(m.getXsip_otm_rej_reason());
//        resp.setXsip_otm_created_date(m.getXsip_otm_created_date());

        resp.setEmandate_otm_flag(m.getEmandate_otm_flag());
        resp.setEmandate_otm(m.getEmandate_otm());
        resp.setEmandate_otm_amount(m.getEmandate_otm_amount());
        resp.setEmandate_otm_approved(m.getEmandate_otm_approved());
        resp.setEmandate_otm_rej_reason(m.getEmandate_otm_rej_reason());
//        resp.setEmandate_otm_created_date(m.getEmandate_otm_created_date());

        resp.setNse_ach_flag(m.getNse_ach_flag());
        resp.setNse_ach(m.getNse_ach());
        resp.setNse_ach_amount(m.getNse_ach_amount());
        resp.setNse_ach_approved(m.getNse_ach_approved());
        resp.setNse_ach_rej_reason(m.getNse_ach_rej_reason());
//        resp.setNse_ach_created_date(m.getNse_ach_created_date());
//        resp.setMfu_mandate_created_date(m.getMfu_mandate_created_date());

        resp.setClient_name(m.getClient_name());
        resp.setCreated_date(m.getCreated_date());

//        resp.setNse_ach_start_date(m.getNse_ach_start_date());
//        resp.setNse_ach_end_date(m.getNse_ach_end_date());
    }

    @GetMapping("/getSipSchemeByAmcOnline")
    public ResponseEntity<?> getSipSchemeByAmcOnline(@RequestHeader("Authorization") String token,@RequestParam String option)
    {
        try {

            String client_name = TokenInterceptor.extractClientNamedFromToken(token, secretKey);
            option = NseUtils.checkParem(option);

            if(option.isEmpty())
            {
                return NseUtils.commonResponse("Please enter a Option", HttpStatus.BAD_REQUEST);
            }
            String optionString = "";
            if(option.equalsIgnoreCase("growth"))
            {
                optionString = "Z";
            }else if(option.equalsIgnoreCase("dividend payout"))
            {
                optionString = "N";
            }else if(option.equalsIgnoreCase("dividend reinvestment"))
            {
                optionString = "Y";
            }else if(option.equalsIgnoreCase("SIF"))
            {
                optionString = "SIF";
            }

            List<SchemePojo> schemeList;
            List<Object[]> schemeNames = null;
            List<String> amc_list = new ArrayList<String>();
            BseNseKeyDto bse_list = userServiceClient.getByClientName(client_name,token);
            if(bse_list != null)
            {
                String amc_string = bse_list.getAmc_names();

                if(amc_string != null && !amc_string.isEmpty())
                {
                    amc_list = new ArrayList<String>(Arrays.asList(amc_string.split(",")));
                }
            }

            if(!amc_list.isEmpty())
            {
                if(optionString.equalsIgnoreCase("SIF"))
                {
                    schemeNames = nseOnlineSchemeMasterRepository.getSipSifSchemesWithAmc(amc_list);
                }else
                {
                    schemeNames = nseOnlineSchemeMasterRepository.getSipSchemesByOptionWithAmc(optionString,amc_list);
                }
            }else
            {
                if(optionString.equalsIgnoreCase("SIF"))
                {
                    schemeNames = nseOnlineSchemeMasterRepository.getSipSifSchemes();
                }else
                {
                    schemeNames = nseOnlineSchemeMasterRepository.getSipSchemesByOption(optionString);
                }
            }

            if (schemeNames != null && !schemeNames.isEmpty()) {
                schemeList = schemeNames.stream().map(row -> {
                            String schemeName = (String) row[0];
                            String schemeCategory = (String) row[1];
                            String amc_code = (String) row[2];
                            String amc_name = (String) row[3];
                            String logo = amcLogoPath + NseUtils.getLogoByAmcNameOrSchemeName(amc_code);
                            return new SchemePojo(schemeName, schemeCategory, amc_code, amc_name, "", logo);
                        })
                        .sorted(Comparator.comparing(SchemePojo::getScheme_name, String.CASE_INSENSITIVE_ORDER))
                        .collect(Collectors.toList());
            } else {
                return NseUtils.commonResponse("No Scheme names data found.", HttpStatus.OK);
            }

            return ResponseEntity.ok(schemeList);

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getStpFromSchemeOnline")
    public ResponseEntity<?> getStpFromSchemeOnline(@RequestHeader("Authorization") String token,@RequestParam String iin_number, @RequestParam String broker_code)
    {
        String userid = "";
        String client_name = "";
        try {
            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            UserDto users =null;

            try {
                users = userServiceClient.getUserById(Integer.valueOf(userid), token);
            } catch (FeignException e) {
                return FeignErrorHandler.handle(e, "User Service", "User not found");
            }
            client_name = users.getClient_name();

            System.out.println("client_name  = " + client_name);

            List<UsersPortfolioSchemewiseDto> amcList = null;
            amcList = userServiceClient.getAllRedemptionAmcDetails(Integer.valueOf(userid), client_name,token);
            System.out.println("amcList = " + amcList.size());

            Set<String> amcCodeSet = amcList.stream()
                    .map(UsersPortfolioSchemewiseDto::getAmc_code)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            List<SchemePojo> masterList = new ArrayList<>();
            for(String amc_code: amcCodeSet)
            {
                if (client_name == null) {
                    client_name = "";
                }
                ;
                if (amc_code == null) {
                    amc_code = "";
                }
                ;
                if (iin_number == null) {
                    iin_number = "";
                }
                ;
                if (broker_code == null) {
                    broker_code = "";
                }
                ;

                userid = userid.trim();
                client_name = client_name.trim();
                amc_code = amc_code.trim();
                iin_number = iin_number.trim();
                broker_code = broker_code.trim();

                String tax_status_code = "";
                String holding_nature_code = "";
                String joint_holder_pan1 = "";
                String joint_holder_pan2 = "";

                if (broker_code.isEmpty()) {
                    return NseUtils.commonResponse("Broker code is empty...!", HttpStatus.BAD_REQUEST);
                }

                UserDto user = null;
                try {
                    try {
                        user = userServiceClient.getUserDetailsByID(client_name, Integer.valueOf(userid), token);
                    } catch (FeignException e)
                    {
                        return FeignErrorHandler.handle(e, "User Service", "User not found");
                    }
                } catch (FeignException e) {
                    if (e.status() == 400) {
                        return NseUtils.commonResponse("Invalid request for AMC details.", HttpStatus.BAD_REQUEST);
                    } else if (e.status() == 404) {
                        return NseUtils.commonResponse("No AMC details found for the given user.", HttpStatus.BAD_REQUEST);
                    } else {
                        return NseUtils.commonResponse("Error occurred while fetching AMC details.", HttpStatus.BAD_REQUEST);
                    }
                }

                System.out.println("user = " + user);

                List<String> schemeCodeList = new ArrayList<>();

                if (user != null)
                {
                    UserDto nse = null;
                    try {
                        nse = userServiceClient.getUserBseNseDetailsByNseIINNumberBrokerCode(client_name, iin_number, broker_code, token);
                    } catch (FeignException e)
                    {
                        if (e.status() == 400) {
                            return NseUtils.commonResponse("Invalid request for AMC details.", HttpStatus.BAD_REQUEST);
                        } else if (e.status() == 404) {
                            return NseUtils.commonResponse("No AMC details found for the given user.", HttpStatus.BAD_REQUEST);
                        } else {
                            return NseUtils.commonResponse("Error occurred while fetching AMC details.", HttpStatus.BAD_REQUEST);
                        }
                    }
                    System.out.println("nse = " + nse);
                    if (nse == null) {
                        return NseUtils.commonResponse("No AMC details found for the given user.", HttpStatus.BAD_REQUEST);
                    }
                    tax_status_code = nse.getTax_status_code();
                    holding_nature_code = nse.getHolding_nature_code();
                    joint_holder_pan1 = nse.getJoint_holder_pan1();
                    joint_holder_pan2 = nse.getJoint_holder_pan2();
                    if (tax_status_code == null) {
                        tax_status_code = "";
                    }
                    if (holding_nature_code == null) {
                        holding_nature_code = "";
                    }
                    if (joint_holder_pan1 == null) {
                        joint_holder_pan1 = "";
                    }
                    if (joint_holder_pan2 == null) {
                        joint_holder_pan2 = "";
                    }
                }

                System.out.println("amc_code = " + amc_code);
//                String rta_name = NseUtils.getRTAName(amc_code);
                String rta_name = userServiceClient.getRegisterByAmcCode(amc_code,token);
                System.out.println("rtaName = " + rta_name);

                if (StringHelper.isNotEmpty(rta_name) && rta_name.equalsIgnoreCase("CAMS"))
                {
                    List<String> dto = null;
                    try
                    {
                        dto = userServiceClient.getRedemptionSchemesNews(Integer.valueOf(userid), client_name, amc_code, token);
                    } catch (FeignException e)
                    {
                        System.out.println("exception: " + e.getMessage());
                    }

                    List<String> prodcodeList = dto;

                    if (prodcodeList == null || prodcodeList.size() == 0)
                    {
                        List<String> amfi = amfiServiceClient.getschemeCamsProductCodesByCompanys(amc_code, token);
                        prodcodeList = amfi;

                        if (prodcodeList != null && prodcodeList.size() > 0)
                        {
                            String prodcode = String.join("", prodcodeList);
                            prodcodeList = Arrays.asList(prodcode.split(","));

                            prodcodeList = prodcodeList.stream().filter(item -> !item.trim().isEmpty()).collect(Collectors.toList());
                            HashSet<Object> seen = new HashSet<>();
                            prodcodeList.removeIf(c -> !seen.add(Arrays.asList(c)));
                        }
                    }

                    List<InvestorMasterCamsDto> cams = new ArrayList<>();

                    try
                    {
                        cams = userServiceClient.getProductCode(Integer.valueOf(userid), client_name, prodcodeList, token);
                        // System.out.println("cams: " + new Gson().toJson(cams));
                    } catch (FeignException e) {
                        System.out.println("exception: " + e.getMessage());
//                        if (e.status() == 400) {
//                            return NseUtils.commonResponse("Invalid request for AMC details.", HttpStatus.BAD_REQUEST);
//                        } else if (e.status() == 404) {
//                            return NseUtils.commonResponse("No AMC details found for the given user.", HttpStatus.BAD_REQUEST);
//                        } else {
//                            return NseUtils.commonResponse("Error occurred while fetching AMC details.", HttpStatus.BAD_REQUEST);
//                        }
                    }

                    List<InvestorMasterCamsDto> camsList = cams;
                    if (camsList.size() > 0) {
                        for (InvestorMasterCamsDto camsScheme : camsList) {
                            String holding = camsScheme.getHolding_na();
                            String joint1_pan = camsScheme.getJoint1_pan();
                            String joint2_pan = camsScheme.getJoint2_pan();
                            String bank_acc_type = camsScheme.getAc_type();
                            if (holding == null) {
                                holding = "";
                            }
                            if (joint1_pan == null) {
                                joint1_pan = "";
                            }
                            if (joint2_pan == null) {
                                joint2_pan = "";
                            }
                            if (bank_acc_type == null) {
                                bank_acc_type = "";
                            }

                            if (tax_status_code.equalsIgnoreCase("01")) {
                                if (holding_nature_code.equalsIgnoreCase("SI")) {
                                    if (holding.equalsIgnoreCase("SI")) {
                                        schemeCodeList.add(camsScheme.getProduct());
                                    }
                                } else if (holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES")) {
                                    if ((holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES"))
                                            && joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan)) {
                                        schemeCodeList.add(camsScheme.getProduct());
                                    }
                                } else if (holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO")
                                        && joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan)) {
                                    schemeCodeList.add(camsScheme.getProduct());
                                }
                            } else if (tax_status_code.equalsIgnoreCase("24") || tax_status_code.equalsIgnoreCase("21")) {
                                if (tax_status_code.equalsIgnoreCase("24") && bank_acc_type.equalsIgnoreCase("NRO")) {
                                    if (holding_nature_code.equalsIgnoreCase("SI")) {
                                        if (holding.equalsIgnoreCase("SI")) {
                                            schemeCodeList.add(camsScheme.getProduct());
                                        }
                                    } else if (holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES")) {
                                        if ((holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES"))
                                                && joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan)) {
                                            schemeCodeList.add(camsScheme.getProduct());
                                        }
                                    } else if (holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO")
                                            && joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan)) {
                                        schemeCodeList.add(camsScheme.getProduct());
                                    }
                                } else if (tax_status_code.equalsIgnoreCase("21") && bank_acc_type.equalsIgnoreCase("NRE")) {
                                    if (holding_nature_code.equalsIgnoreCase("SI")) {
                                        if (holding.equalsIgnoreCase("SI")) {
                                            schemeCodeList.add(camsScheme.getProduct());
                                        }
                                    } else if (holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES")) {
                                        if ((holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES"))
                                                && joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan)) {
                                            schemeCodeList.add(camsScheme.getProduct());
                                        }
                                    } else if (holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO")
                                            && joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan)) {
                                        schemeCodeList.add(camsScheme.getProduct());
                                    }
                                }

                            } else {
                                schemeCodeList.add(camsScheme.getProduct());
                            }

                        }
                    }
                }

                if (StringHelper.isNotEmpty(rta_name) && rta_name.equalsIgnoreCase("Karvy")) {
                    List<InvestorMasterKarvyDto> karvy = null;

                    karvy = userServiceClient.getRedemptionKarvy(Integer.valueOf(userid), client_name, amc_code, token);
                    System.out.println("karvy = " + karvy);

                    List<InvestorMasterKarvyDto> karvyList = karvy;
                    System.out.println("karvyList = " + karvyList.size());
                    if (karvyList.size() > 0) {
                        for (InvestorMasterKarvyDto karvyScheme : karvyList) {

                            String holding = karvyScheme.getMode_of_holding();
                            String pan2 = karvyScheme.getPan2();
                            String pan3 = karvyScheme.getPan3();
                            String bank_acc_type = karvyScheme.getAccount_type();
                            if (holding == null) {
                                holding = "";
                            }
                            if (pan2 == null) {
                                pan2 = "";
                            }
                            if (pan3 == null) {
                                pan3 = "";
                            }
                            if (bank_acc_type == null) {
                                bank_acc_type = "";
                            }

                            System.out.println("tax_status_code = " + tax_status_code);
                            System.out.println("holding_nature_code = " + holding_nature_code);
                            System.out.println("holding = " + holding);
                            if (tax_status_code.equalsIgnoreCase("01")) {
                                if (holding.equalsIgnoreCase("1")) {
                                    holding = "SI";
                                } else if (holding.equalsIgnoreCase("2") || holding.equalsIgnoreCase("J")) {
                                    holding = "JO";
                                } else if (holding.equalsIgnoreCase("3") || holding.equalsIgnoreCase("5")) {
                                    holding = "ES";
                                } else if (holding.equalsIgnoreCase("4") || holding.equalsIgnoreCase("7")) {
                                    holding = "AS";
                                } else {
                                    holding = "";
                                }

                                if (holding.isEmpty()) {
                                    String holding_des = karvyScheme.getMode_of_holding_description();
                                    if (holding_des == null) {
                                        holding_des = "";
                                    }
                                    System.out.println("holding_des = " + holding_des);
                                    if (holding_des.equalsIgnoreCase("SINGLE") || holding_des.equalsIgnoreCase("SINGLY")) {
                                        holding = "SI";
                                    } else if (holding_des.equalsIgnoreCase("JOINT") || holding_des.equalsIgnoreCase("JOINTLY")) {
                                        holding = "JO";
                                    } else if (holding_des.equalsIgnoreCase("EITHER OR SURVIVOR")) {
                                        holding = "ES";
                                    } else if (holding_des.equalsIgnoreCase("ANYONE OR SURVIVOR")) {
                                        holding = "AS";
                                    }
                                }

                                if (holding_nature_code.equalsIgnoreCase("SI")) {
                                    if (holding.equalsIgnoreCase("SI")) {
                                        schemeCodeList.add(karvyScheme.getProduct_code());
                                    }
                                } else if (holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES")) {
                                    if ((holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES"))
                                            && joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3)) {
                                        schemeCodeList.add(karvyScheme.getProduct_code());
                                    }
                                } else if (holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO")
                                        && joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3)) {
                                    schemeCodeList.add(karvyScheme.getProduct_code());
                                }
                            } else if (tax_status_code.equalsIgnoreCase("24") || tax_status_code.equalsIgnoreCase("21")) {
                                System.out.println("tax_status_code = " + tax_status_code);
                                System.out.println("holding = " + holding);
                                System.out.println("bank_acc_type = " + bank_acc_type);

                                if (tax_status_code.equalsIgnoreCase("24") && bank_acc_type.equalsIgnoreCase("NRO")) {
                                    if (holding.equalsIgnoreCase("1")) {
                                        holding = "SI";
                                    } else if (holding.equalsIgnoreCase("2") || holding.equalsIgnoreCase("J")) {
                                        holding = "JO";
                                    } else if (holding.equalsIgnoreCase("3") || holding.equalsIgnoreCase("5")) {
                                        holding = "ES";
                                    } else if (holding.equalsIgnoreCase("4") || holding.equalsIgnoreCase("7")) {
                                        holding = "AS";
                                    }else {
                                        holding = "";
                                    }

                                    if (holding.isEmpty()) {
                                        String holding_des = karvyScheme.getMode_of_holding_description();
                                        if (holding_des == null) {
                                            holding_des = "";
                                        }

                                        if (holding_des.equalsIgnoreCase("SINGLE") || holding_des.equalsIgnoreCase("SINGLY")) {
                                            holding = "SI";
                                        } else if (holding_des.equalsIgnoreCase("JOINT") || holding_des.equalsIgnoreCase("JOINTLY")) {
                                            holding = "JO";
                                        } else if (holding_des.equalsIgnoreCase("EITHER OR SURVIVOR")) {
                                            holding = "ES";
                                        } else if (holding_des.equalsIgnoreCase("ANYONE OR SURVIVOR")) {
                                            holding = "AS";
                                        }
                                    }

                                    if (holding_nature_code.equalsIgnoreCase("SI")) {
                                        if (holding.equalsIgnoreCase("SI")) {
                                            schemeCodeList.add(karvyScheme.getProduct_code());
                                        }
                                    } else if (holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES")) {
                                        if ((holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES"))
                                                && joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3)) {
                                            schemeCodeList.add(karvyScheme.getProduct_code());
                                        }
                                    } else if (holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO")
                                            && joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3)) {
                                        schemeCodeList.add(karvyScheme.getProduct_code());
                                    }
                                } else if (tax_status_code.equalsIgnoreCase("21") && bank_acc_type.equalsIgnoreCase("NRE")) {
                                    if (holding.equalsIgnoreCase("1")) {
                                        holding = "SI";
                                    } else if (holding.equalsIgnoreCase("2") || holding.equalsIgnoreCase("J")) {
                                        holding = "JO";
                                    } else if (holding.equalsIgnoreCase("3") || holding.equalsIgnoreCase("5")) {
                                        holding = "ES";
                                    } else if (holding.equalsIgnoreCase("4") || holding.equalsIgnoreCase("7")) {
                                        holding = "AS";
                                    }else{
                                        holding = "";
                                    }
                                    System.out.println("new -----------------> " + holding);
                                    if (holding.isEmpty()) {
                                        String holding_des = karvyScheme.getMode_of_holding_description();
                                        System.out.println("new holding description -----------------> " + holding_des);
                                        if (holding_des == null) {
                                            holding_des = "";
                                        }
                                        System.out.println("new holding description -----------------> " + holding_des);
                                        if (holding_des.equalsIgnoreCase("SINGLE") || holding_des.equalsIgnoreCase("SINGLY")) {
                                            holding = "SI";
                                        } else if (holding_des.equalsIgnoreCase("JOINT") || holding_des.equalsIgnoreCase("JOINTLY")) {
                                            holding = "JO";
                                        } else if (holding_des.equalsIgnoreCase("EITHER OR SURVIVOR")) {
                                            holding = "ES";
                                        } else if (holding_des.equalsIgnoreCase("ANYONE OR SURVIVOR")) {
                                            holding = "AS";
                                        }
                                    }

                                    if (holding_nature_code.equalsIgnoreCase("SI")) {
                                        if (holding.equalsIgnoreCase("SI")) {
                                            schemeCodeList.add(karvyScheme.getProduct_code());
                                        }
                                    } else if (holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES")) {
                                        if ((holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES"))
                                                && joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3)) {
                                            schemeCodeList.add(karvyScheme.getProduct_code());
                                        }
                                    } else if (holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO")
                                            && joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3)) {
                                        schemeCodeList.add(karvyScheme.getProduct_code());
                                    }
                                }
                            } else {
                                schemeCodeList.add(karvyScheme.getProduct_code());
                            }

                        }
                    }
                }

                List schemeCodeListNew = new ArrayList<String>(new LinkedHashSet<String>(schemeCodeList));

                System.out.println("schemeCodeListNew = " + schemeCodeListNew.size());
                List<String> list = null;
                try {
                    list = userServiceClient.getRedemptionPortfolio(Integer.valueOf(userid), client_name, amc_code, schemeCodeListNew, token);
                } catch (FeignException e) {
                    System.out.println("exception: " + e.getMessage());
                }

                List<String> list1 = list;
                System.out.println("list 1  = " + list1);

                if (list1 != null && list1.size() > 0) {
                    List<Object[]> userScheme = nseOnlineSchemeMasterRepository.findDistinctSchemeNamesForStp(list1);

                    if (userScheme != null && userScheme.size() > 0) {
                        List<SchemePojo> tempList = userScheme.stream().map(row ->
                        {
                            String scheme = (String) row[0];
                            String category = (String) row[1];
                            String amcCode = (String) row[2];
                            String amcName = (String) row[3];
                            String schemeCode = (String) row[4];
                            String logo = amcLogoPath + NseUtils.getLogoByAmcNameOrSchemeName(scheme);
                            return new SchemePojo(scheme, category, amcCode, amcName, schemeCode, logo);
                        }).collect(Collectors.toList());
                        masterList.addAll(tempList);
                    }
                }

            }
            return ResponseEntity.ok(masterList);
        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getExistingSchemesOnline")
    public ResponseEntity<?> getExistingSchemesOnline(@RequestHeader("Authorization") String token,@RequestParam String iin_number,@RequestParam String brokercode)
    {
        String userid = "";
        String client_name = "";
        try
        {
            userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            UserDto users =null;
            client_name = TokenInterceptor.extractClientNamedFromToken(token, secretKey);
            try {
                users = userServiceClient.getUserBseNseDetailsByNseIINNumberBrokerCode(client_name,iin_number,brokercode,token);
            } catch (FeignException e) {
                return FeignErrorHandler.handle(e, "User Service", "User not found");
            }
            List<InvestorSchemeWisePortfolioResponse> portfolio_list = getSchemeHoldings(client_name, Integer.valueOf(userid),users,token);
            if(!portfolio_list.isEmpty())
            {
                portfolio_list.stream().forEach(scheme ->
                {
                    scheme.setAmc_logo(amcLogoPath + NseUtils.getLogoByAmcNameOrSchemeName(scheme.getAmc_name()));
                });
            }
            return ResponseEntity.ok(portfolio_list);
        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public List<InvestorSchemeWisePortfolioResponse> getSchemeHoldings(String client_name, Integer userid,UserDto users,@RequestHeader("Authorization") String token)
    {

        InvestorSchemeWisePortfolioResponse scheme = null;
        List<InvestorSchemeWisePortfolioResponse> master_list = new ArrayList<InvestorSchemeWisePortfolioResponse>();

        try
        {
            String registrar = "";
            String scheme_code = "";
            String folio_no = "";
            String company = "";
            String scheme_name = "";
            Double totalUnits;
            Double latestNav;
            Double totalCurrentValue;
            String broker_code = "";
            String euin = "";
            String amc_name = "";
            String amc_code = "";
            String scheme_amfi_short_name = "";

            String userTaxStatusCode = users.getTax_status_code();
            String userHoldingNature = users.getHolding_nature_code();
            String jointHolderpan1 = users.getJoint_holder_pan1();
            String jointHolderpan2 = users.getJoint_holder_pan2();
            String userBrokerCode = users.getBroker_code();

            List<UsersPortfolioSchemewiseDto> list = null;
            try {
                list = userServiceClient.getActiveSchemesByUserAndClient(userid,client_name,token);
            } catch (FeignException e) {
                if (e.status() == 400) {
                    return master_list;
                } else if (e.status() == 404) {
                    return master_list;
                } else {
                    return master_list;
                }
            }

            if(list != null && list.size() > 0)
            {
                List<InvestorMasterCamsDto> camsList = userServiceClient.getByCamsUserIdAndClientName(userid,client_name,token);

                List<InvestorMasterKarvyDto> karvyList = userServiceClient.getByKarvyUserIdAndClientName(userid,client_name,token);
                ////System.out.println("karvyList = " + karvyList);
                for (UsersPortfolioSchemewiseDto portfolio : list)
                {
                    scheme_name = portfolio.getScheme_name();
                    folio_no = portfolio.getFolio_no();
                    scheme_code = portfolio.getScheme_code();
                    company = portfolio.getAmc_name();
                    registrar = portfolio.getRegistrar();
                    totalUnits = portfolio.getTotal_units();
                    latestNav = portfolio.getLatest_nav();
                    totalCurrentValue = portfolio.getCurrent_value();
                    broker_code = portfolio.getBroker_code();
                    euin = portfolio.getEuin();
                    amc_name = portfolio.getAmc_name();
                    amc_code = portfolio.getAmc_code();
                    scheme_amfi_short_name = portfolio.getScheme_amfi_short_name();

                    scheme = new InvestorSchemeWisePortfolioResponse();
                    scheme.setScheme(scheme_name);
                    scheme.setScheme_code(scheme_code);
                    scheme.setScheme_registrar(registrar);
                    scheme.setScheme_company(company);
                    scheme.setFoliono(folio_no);
                    scheme.setTotalUnits(totalUnits);
                    scheme.setLatestNav(latestNav);
                    scheme.setTotalCurrentValue(totalCurrentValue);
                    scheme.setBroker_code(broker_code);
                    scheme.setEuin(euin);
                    scheme.setAmc_name(amc_name);
                    scheme.setAmc_code(amc_code);
                    scheme.setScheme_amfi_short_name(scheme_amfi_short_name);
                    ////System.out.println("registrar = " + registrar);

                    if(StringHelper.isNotEmpty(registrar) && registrar.equalsIgnoreCase("CAMS"))
                    {
                        InvestorMasterCamsDto camsScheme = null;
                        if(camsList != null && camsList.size() > 0)
                        {
                            String folio_no1 = folio_no;
                            String scheme_code1 = scheme_code;
                            camsScheme = camsList.stream().filter(x -> x.getFoliochk().equalsIgnoreCase(folio_no1) && x.getProduct().equalsIgnoreCase(scheme_code1)).findAny().orElse(null);
                        }
                        ////System.out.println("camsScheme = " + camsScheme);
                        if(camsScheme != null)
                        {
                            String holding = camsScheme.getHolding_na();
                            String joint1_pan = camsScheme.getJoint1_pan();
                            String joint2_pan = camsScheme.getJoint2_pan();
                            String joint1_name = camsScheme.getJnt_name1();
                            String joint2_name = camsScheme.getJnt_name2();
                            String demat = camsScheme.getDemat();
                            //String dp_id = camsScheme.getDp_id();
                            String nominee1_name = camsScheme.getNom_name();
                            String nominee1_relation = camsScheme.getRelation();
                            String nominee1_percentage = String.valueOf(camsScheme.getNom_percen());
                            String nominee2_name = camsScheme.getNom2_name();
                            String nominee2_relation = camsScheme.getNom2_relat();
                            String nominee2_percentage = String.valueOf(camsScheme.getNom2_perce());
                            String nominee3_name = camsScheme.getNom3_name();
                            String nominee3_relation = camsScheme.getNom3_relat();
                            String nominee3_percentage = String.valueOf(camsScheme.getNom3_perce());
                            String bank_acc_type = camsScheme.getAc_type();
                            String tax_status = camsScheme.getTax_status();

                            if(holding == null){holding = "";}
                            if(joint1_pan == null){joint1_pan = "";}
                            if(joint2_pan == null){joint2_pan = "";}
                            if(joint1_name == null){joint1_name = "";}
                            if(joint2_name == null){joint2_name = "";}
                            if(demat == null){demat = "";}
                            if(nominee1_name == null){nominee1_name = "";}
                            if(nominee1_relation == null){nominee1_relation = "";}
                            if(nominee2_name == null){nominee2_name = "";}
                            if(nominee2_relation == null){nominee2_relation = "";}
                            if(nominee3_name == null){nominee3_name = "";}
                            if(nominee3_relation == null){nominee3_relation = "";}
                            if(bank_acc_type == null){bank_acc_type = "";}


                            scheme.setHolding_nature(holding);
                            scheme.setJoint1_pan(joint1_pan);
                            scheme.setJoint2_pan(joint2_pan);
                            scheme.setJoint1_name(joint1_name);
                            scheme.setJoint2_name(joint2_name);
                            if(demat.equalsIgnoreCase("Y")){
                                scheme.setIsDeamtAccount(true);
                            }else{
                                scheme.setIsDeamtAccount(false);
                            }
                            scheme.setNominee1_name(nominee1_name);
                            scheme.setNominee1_relation(nominee1_relation);
                            scheme.setNominee1_percentage(nominee1_percentage);
                            scheme.setNominee2_name(nominee2_name);
                            scheme.setNominee2_relation(nominee2_relation);
                            scheme.setNominee2_percentage(nominee2_percentage);
                            scheme.setNominee3_name(nominee3_name);
                            scheme.setNominee3_relation(nominee3_relation);
                            scheme.setNominee3_percentage(nominee3_percentage);
                            scheme.setTax_status(tax_status);
                            if(bank_acc_type.equalsIgnoreCase("NRE")) {
                                scheme.setTax_status_code("21");
                            }else if(bank_acc_type.equalsIgnoreCase("NRO") && tax_status.contains("OCI"))
                            {
                                scheme.setTax_status_code("62");
                            }else if(bank_acc_type.equalsIgnoreCase("NRO"))
                            {
                                scheme.setTax_status_code("24");
                            }else
                            {
                                scheme.setTax_status_code("");
                            }
                        }else
                        {
                            scheme.setHolding_nature("");
                            scheme.setJoint1_pan("");
                            scheme.setJoint2_pan("");
                            scheme.setJoint1_name("");
                            scheme.setJoint2_name("");
                            scheme.setIsDeamtAccount(false);
                            scheme.setNominee1_name("");
                            scheme.setNominee1_relation("");
                            scheme.setNominee1_percentage("");
                            scheme.setNominee2_name("");
                            scheme.setNominee2_relation("");
                            scheme.setNominee2_percentage("");
                            scheme.setNominee3_name("");
                            scheme.setNominee3_relation("");
                            scheme.setNominee3_percentage("");
                            scheme.setTax_status_code("");

                        }
                    }
                    if(StringHelper.isNotEmpty(registrar) && registrar.equalsIgnoreCase("KARVY"))
                    {
                        InvestorMasterKarvyDto karvyScheme = null;
                        if(karvyList != null && karvyList.size() > 0)
                        {
                            String folio_no1 = folio_no;
                            String scheme_code1 = scheme_code;
                            karvyScheme = karvyList.stream().filter(x -> x.getFolio().equalsIgnoreCase(folio_no1) && x.getProduct_code().equalsIgnoreCase(scheme_code1)).findAny().orElse(null);
                        }

                        if(karvyScheme != null)
                        {
                            String holding = karvyScheme.getMode_of_holding();
                            String pan2 = karvyScheme.getPan2();
                            String pan3 = karvyScheme.getPan3();
                            String jnt_name1 = karvyScheme.getJoint_name1();
                            String jnt_name2 = karvyScheme.getJoint_name2();
                            String dp_id = karvyScheme.getDp_id();
                            String nominee1_name = karvyScheme.getNominee();
                            String nominee1_relation = karvyScheme.getNominee_rel();
                            String nominee1_percentage = karvyScheme.getNominee_ra5();
                            String nominee2_name = karvyScheme.getNominee2();
                            String nominee2_relation = karvyScheme.getNominee2_r3();
                            String nominee2_percentage = karvyScheme.getNominee2_r6();
                            String nominee3_name = karvyScheme.getNominee3();
                            String nominee3_relation = karvyScheme.getNominee3_r4();
                            String nominee3_percentage = karvyScheme.getNominee3_r7();
                            String bank_acc_type = karvyScheme.getAccount_type();
                            String tax_status = karvyScheme.getTax_status();
                            String category_d1 = karvyScheme.getCategory_d1();

                            if(holding == null){holding = "";}
                            if(pan2 == null){pan2 = "";}
                            if(pan3 == null){pan3 = "";}
                            if(jnt_name1 == null){jnt_name1 = "";}
                            if(jnt_name2 == null){jnt_name2 = "";}
                            if(nominee1_name == null){nominee1_name = "";}
                            if(nominee1_relation == null){nominee1_relation = "";}
                            if(nominee1_percentage == null){nominee1_percentage = "";}
                            if(nominee2_name == null){nominee2_name = "";}
                            if(nominee2_relation == null){nominee2_relation = "";}
                            if(nominee2_percentage == null){nominee2_percentage = "";}
                            if(nominee3_name == null){nominee3_name = "";}
                            if(nominee3_relation == null){nominee3_relation = "";}
                            if(nominee3_percentage == null){nominee3_percentage = "";}
                            if(dp_id == null || dp_id.equalsIgnoreCase("NOT PROVIDED")){dp_id = "";}
                            if(bank_acc_type == null){bank_acc_type = "";}

                            if(holding.equalsIgnoreCase("1"))
                            {
                                holding = "SI";
                            }else if(holding.equalsIgnoreCase("2") || holding.equalsIgnoreCase("J"))
                            {
                                holding = "JO";
                            }else if(holding.equalsIgnoreCase("3") || holding.equalsIgnoreCase("5"))
                            {
                                holding = "ES";
                            }else if(holding.equalsIgnoreCase("4") || holding.equalsIgnoreCase("7"))
                            {
                                holding = "AS";
                            }else{
                                holding = "";
                            }

                            if(holding == null || holding.isEmpty())
                            {
                                String holding_des = karvyScheme.getMode_of_holding_description();

                                if(holding_des.equalsIgnoreCase("SINGLE") || holding_des.equalsIgnoreCase("SINGLY"))
                                {
                                    holding = "SI";
                                }else if(holding_des.equalsIgnoreCase("JOINT") || holding_des.equalsIgnoreCase("JOINTLY"))
                                {
                                    holding = "JO";
                                }else if(holding.equalsIgnoreCase("EITHER OR SURVIVOR"))
                                {
                                    holding = "ES";
                                }else if(holding_des.equalsIgnoreCase("ANYONE OR SURVIVOR"))
                                {
                                    holding = "AS";
                                }
                            }

                            scheme.setHolding_nature(holding);
                            scheme.setJoint1_pan(pan2);
                            scheme.setJoint2_pan(pan3);
                            scheme.setJoint1_name(jnt_name1);
                            scheme.setJoint2_name(jnt_name2);
                            if(!dp_id.isEmpty()){
                                scheme.setIsDeamtAccount(true);
                            }else{
                                scheme.setIsDeamtAccount(false);
                            }
                            scheme.setNominee1_name(nominee1_name);
                            scheme.setNominee1_relation(nominee1_relation);
                            scheme.setNominee1_percentage(nominee1_percentage);
                            scheme.setNominee2_name(nominee2_name);
                            scheme.setNominee2_relation(nominee2_relation);
                            scheme.setNominee2_percentage(nominee2_percentage);
                            scheme.setNominee3_name(nominee3_name);
                            scheme.setNominee3_relation(nominee3_relation);
                            scheme.setNominee3_percentage(nominee3_percentage);
                            scheme.setTax_status(tax_status);
                            if(bank_acc_type.equalsIgnoreCase("NRE"))
                            {
                                scheme.setTax_status_code("21");
                            }else if(bank_acc_type.equalsIgnoreCase("NRE") && category_d1.contains("NRI"))
                            {
                                scheme.setTax_status_code("62");
                            }else if(bank_acc_type.equalsIgnoreCase("NRO"))
                            {
                                scheme.setTax_status_code("24");
                            }else
                            {
                                scheme.setTax_status_code("");
                            }
                        }else
                        {
                            scheme.setHolding_nature("");
                            scheme.setJoint1_pan("");
                            scheme.setJoint2_pan("");
                            scheme.setJoint1_name("");
                            scheme.setJoint2_name("");
                            scheme.setIsDeamtAccount(false);
                            scheme.setNominee1_name("");
                            scheme.setNominee1_relation("");
                            scheme.setNominee1_percentage("");
                            scheme.setNominee2_name("");
                            scheme.setNominee2_relation("");
                            scheme.setNominee2_percentage("");
                            scheme.setNominee3_name("");
                            scheme.setNominee3_relation("");
                            scheme.setNominee3_percentage("");
                            scheme.setTax_status_code("");

                        }
                    }


                    if(!userTaxStatusCode.equalsIgnoreCase("01")
                            && !userTaxStatusCode.equalsIgnoreCase("21")
                            && !userTaxStatusCode.equalsIgnoreCase("24")
                            && !userTaxStatusCode.equalsIgnoreCase("61")
                            && !userTaxStatusCode.equalsIgnoreCase("62"))
                    {
                        if(scheme.getBroker_code().equalsIgnoreCase(userBrokerCode))
                        {
                            master_list.add(scheme);
                        }
                    }
                    else if(userTaxStatusCode.equalsIgnoreCase("21")
                            || userTaxStatusCode.equalsIgnoreCase("24")
                            || userTaxStatusCode.equalsIgnoreCase("61")
                            || userTaxStatusCode.equalsIgnoreCase("62"))
                    {
                        if(scheme.getBroker_code().equalsIgnoreCase(userBrokerCode) && scheme.getTax_status_code().equalsIgnoreCase(userTaxStatusCode) && (scheme.getHolding_nature().equalsIgnoreCase(userHoldingNature) || ((scheme.getHolding_nature().equalsIgnoreCase("AS") && userHoldingNature.equalsIgnoreCase("ES")) || (scheme.getHolding_nature().equalsIgnoreCase("ES") && userHoldingNature.equalsIgnoreCase("AS")))) && scheme.getJoint1_pan().equalsIgnoreCase(jointHolderpan1) && scheme.getJoint2_pan().equalsIgnoreCase(jointHolderpan2))
                        {
                            System.out.println("entered second");
                            master_list.add(scheme);
                        }
                    }
                    else
                    {
                        if(scheme.getBroker_code().equalsIgnoreCase(userBrokerCode) && (scheme.getHolding_nature().equalsIgnoreCase(userHoldingNature) || ((scheme.getHolding_nature().equalsIgnoreCase("AS") && userHoldingNature.equalsIgnoreCase("ES")) || (scheme.getHolding_nature().equalsIgnoreCase("ES") && userHoldingNature.equalsIgnoreCase("AS")))) && scheme.getJoint1_pan().equalsIgnoreCase(jointHolderpan1) && scheme.getJoint2_pan().equalsIgnoreCase(jointHolderpan2))
                        {
                            System.out.println("entered");
                            master_list.add(scheme);
                        }
                    }
                }
            }
//            if (master_list.size() > 0)
//            {
//                Collections.sort(master_list, new Comparator<InvestorSchemeWisePortfolioResponse>()
//                {
//                    @Override
//                    public int compare(final InvestorSchemeWisePortfolioResponse object1, final InvestorSchemeWisePortfolioResponse object2) {
//                        return object1.getScheme().compareTo(object2.getScheme());
//                    }
//                });
//            }

            if (!master_list.isEmpty()) {
                master_list.sort(
                        Comparator.comparing(
                                InvestorSchemeWisePortfolioResponse::getScheme,
                                Comparator.nullsLast(String::compareToIgnoreCase)
                        )
                );
            }
        }
        catch (Exception ex)
        {
            System.err.println("Error fetching folio numbers: " + ex.getMessage());
        }
        return master_list;

    }

    @GetMapping("/getSwitchSchemeByAmcOnline")
    public ResponseEntity<?> getSwitchSchemeByAmcOnline(@RequestHeader("Authorization") String token,
                                                        @RequestParam String schemeName)
    {
        try {
            List<String> filteredScheme = nseOnlineSchemeMasterRepository.findDistinctSchemeNameByAmcCodeAndMinAmountschemeName(schemeName);

            String amcname =  filteredScheme.get(0);
            List<SchemePojo> schemeList =  new ArrayList<>();
            List<Object[]> filteredSchemes = nseOnlineSchemeMasterRepository.findDistinctSchemeNameByAmcCodeAndMinAmount(amcname);

            if (filteredSchemes != null && !filteredSchemes.isEmpty())
            {
                schemeList = filteredSchemes.stream().map(row ->
                {
                    String scheme = (String) row[0];
                    String category = (String) row[1];
                    String amc_code = (String) row[2];
                    String amc_name = (String) row[3];
                    String scheme_code = (String) row[4];
                    String logo = amcLogoPath + NseUtils.getLogoByAmcNameOrSchemeName(scheme);
                    return new SchemePojo(scheme, category, amc_code, amc_name,scheme_code, logo);
                }).collect(Collectors.toList());
            }

            return ResponseEntity.ok(schemeList);
        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getSIPActiveSchemeList")
    public ResponseEntity<?> getSIPActiveSchemeList(@RequestHeader("Authorization") String token,
                                                    @RequestParam String client_code,
                                                    @RequestParam String broker_code,
                                                    @RequestParam String client_name)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        List<NseTransactions> nseTransactionsList = new ArrayList<>();
        try {

            System.out.println("clientCode: " + client_code);
            System.out.println("brokerCode: " + broker_code);

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

            HttpHeaders headers = NseUtils.getHttpHeaders(nse_memberid, base64Encoded);

            JSONObject requestBody = new JSONObject();
            requestBody.put("xsip_reg_id", "");
            requestBody.put("client_code", client_code);
            requestBody.put("from_date", "");
            requestBody.put("to_date", "");

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            String provisionalReport_url= nseUrl+"/nsemfdesk/api/v2/reports/XSIP_REG_REPORT";

            try {
                ResponseEntity<String> result = RestTemplateFactory.createRestTemplate().postForEntity(provisionalReport_url, entity, String.class);

                String responseBody = result.getBody();

                JSONObject jsonObject = new JSONObject(responseBody);
                System.out.println("jsonObject: " + jsonObject);
                JSONArray jsonRegArray = jsonObject.getJSONArray("report_data");

                String xsip_registration_no = "";
                String installments_amount = "";
                String scheme_name = "";
                String start_date = "";
                String end_date = "";
                String primary_holder_mobile = "";
                String primary_holder_email = "";
                String euin_no = "";
                String rta_scheme_code = "";
                String folio_number = "";
                String nse_mandate_id = "";
                String status = "";
                String freq_type="";
                System.out.println("jsonRegArray:size: " + jsonRegArray.length());
                NseTransactions nse;
                for (int i = 0; i < jsonRegArray.length(); i++) {

                    JSONObject report_data = jsonRegArray.getJSONObject(i);
                    status = report_data.optString("status");
                    xsip_registration_no = report_data.optString("xsip_registration_no");

                    System.out.println("round: " + i  + " :xsip_registration_no: "+xsip_registration_no+" : " + status);

                    nse = new NseTransactions();
                    if(status.equalsIgnoreCase("ACTIVE")) {

                        installments_amount = report_data.optString("installments_amount");
                        scheme_name = report_data.optString("scheme_name");
                        start_date = report_data.optString("start_date");
                        end_date = report_data.optString("end_date");
                        primary_holder_mobile = report_data.optString("primary_holder_mobile");
                        primary_holder_email = report_data.optString("primary_holder_email");
                        euin_no = report_data.optString("euin_no");
                        rta_scheme_code = report_data.optString("rta_scheme_code");
                        folio_number = report_data.optString("folio_number");
                        nse_mandate_id = report_data.optString("nse_mandate_id");
                        freq_type = report_data.optString("frequency_type");

                        nse.setIin_number(client_code);
                        nse.setFolio_no(folio_number);
                        nse.setAmount_units(installments_amount);
                        nse.setSip_reg_no(xsip_registration_no);
                        nse.setTransaction_status(status);
                        nse.setScheme_name(scheme_name);
                        nse.setStart_date(sdf.parse(start_date));
                        nse.setEnd_date(sdf.parse(end_date));
                        nse.setEuin_number(euin_no);
                        nse.setScheme_code(rta_scheme_code);
                        nse.setMandate_id(nse_mandate_id);
//                        nse.setPrimary_holder_mobile(primary_holder_mobile);
//                        nse.setPrimary_holder_email(primary_holder_email);
                        nse.setBroker_code(broker_code);
                        nse.setFrequency(freq_type);

                        nseTransactionsList.add(nse);
                    }
                }
            }catch (Exception ex) {
                ex.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching AMC codes and names.");
            }
            System.out.println("nseTransactionsList size: " + nseTransactionsList.size());
            return ResponseEntity.ok(nseTransactionsList);

        } catch (Exception ex) {

            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);}
    }

    @GetMapping("/getSTPSchemeByAmcOnline")
    public ResponseEntity<?> getSTPSchemeByAmcOnline(@RequestHeader("Authorization") String token,@RequestParam String scheme_name)
    {
        try {
            List<String> filteredScheme = nseOnlineSchemeMasterRepository.findDistinctSchemeNameByAmcCodeAndMinAmountschemeName(scheme_name);

            String amcname =  filteredScheme.get(0);

            List<String> schemeNames = null;

            schemeNames = nseOnlineSchemeMasterRepository.findDistinctSchemeNamesForStpByAmcCode(amcname);


            System.out.println("schemName = " + schemeNames);
            return ResponseEntity.ok(schemeNames);

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getNewNFOSwitchSchemeByAmc")
    public ResponseEntity<?> getNewNFOSwitchSchemeByAmc(@RequestParam(required = false) String scheme_name,@RequestHeader("Authorization") String token)
    {
        try
        {
            List<NseOnlineSchemeMaster> filteredSchemes = null;
            if(StringHelper.isNotEmpty(scheme_name))
            {
                List<NseOnlineSchemeMaster> amcNames = nseOnlineSchemeMasterRepository.findBySchemeNameExcludeInsured(scheme_name);

                NseOnlineSchemeMaster amcList = amcNames.get(0);
                filteredSchemes = nseOnlineSchemeMasterRepository.findSchemesByAmc(amcList.getAmcName());

            }else{
                filteredSchemes = nseOnlineSchemeMasterRepository.findSchemesByAmcCodeAndStartDateWithSettlementCheckNew();
            }

            return ResponseEntity.ok(filteredSchemes);
        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getSwitchSchemeByOption")
    public ResponseEntity<?> getSwitchSchemeByOption(@RequestParam String option)
    {
        try {
            option = NseUtils.checkParem(option);

            if(option.isEmpty())
            {
                return NseUtils.commonResponse("Please enter a Option", HttpStatus.BAD_REQUEST);
            }
            String optionString = "";
            if(option.equalsIgnoreCase("growth"))
            {
                optionString = "Z";
            }else if(option.equalsIgnoreCase("dividend payout"))
            {
                optionString = "N";
            }else if(option.equalsIgnoreCase("dividend reinvestment"))
            {
                optionString = "Y";
            }else if(option.equalsIgnoreCase("SIF"))
            {
                optionString = "SIF";
            }

            List<NewSchemePojo> schemeList = new ArrayList<>();
            List<Object[]> filteredSchemes = null;

            if(optionString.equalsIgnoreCase("SIF"))
            {
                filteredSchemes = nseOnlineSchemeMasterRepository.getAllSwitchSchemesBySif();
            }else
            {
                filteredSchemes = nseOnlineSchemeMasterRepository.getAllSwitchSchemesByOption(optionString);
            }

            if (filteredSchemes != null && !filteredSchemes.isEmpty()) {
                schemeList = filteredSchemes.stream().map(row -> {
                            String schemeName = (String) row[0];
                            String scheme = (String) row[1];
                            String category = (String) row[2];
                            String amc_code = (String) row[3];
                            String amc_name = (String) row[4];
                            String logo = amcLogoPath + NseUtils.getLogoByAmcNameOrSchemeName(amc_code);
                            return new NewSchemePojo(schemeName,scheme, category, amc_code, amc_name, "", logo);
                        })
                        .sorted(Comparator.comparing(NewSchemePojo::getScheme_name, String.CASE_INSENSITIVE_ORDER))
                        .collect(Collectors.toList());
            }

            return ResponseEntity.ok(schemeList);

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getRedemptionSchemeByAmc")
    public ResponseEntity<?> getRedemptionSchemeByAmc(@RequestHeader("Authorization") String token,@RequestParam String option)
    {
        try
        {
            option = NseUtils.checkParem(option);
            String client_name = TokenInterceptor.extractClientNamedFromToken(token,secretKey);
            if(option.isEmpty())
            {
                return NseUtils.commonResponse("Please enter a Option", HttpStatus.BAD_REQUEST);
            }
            String optionString = "";
            if(option.equalsIgnoreCase("growth"))
            {
                optionString = "Z";
            }else if(option.equalsIgnoreCase("dividend payout"))
            {
                optionString = "N";
            }else if(option.equalsIgnoreCase("dividend reinvestment"))
            {
                optionString = "Y";
            }else if(option.equalsIgnoreCase("SIF"))
            {
                optionString = "SIF";
            }

            List<NewSchemePojo> schemeList = new ArrayList<>();
            List<Object[]> filteredSchemes = null;
            List<String> amc_list = new ArrayList<String>();
            BseNseKeyDto nsekey = null;

            try {
                nsekey = userServiceClient.getByClientName(client_name,token);
            } catch (FeignException e) {
                if (e.status() == 400) {
                    return NseUtils.commonResponse("No record found for the given IIN Number and Client Name.", HttpStatus.BAD_REQUEST);
                } else if (e.status() == 404) {
                    return NseUtils.commonResponse("User not found.", HttpStatus.NOT_FOUND);
                } else {
                    return NseUtils.commonResponse("Downstream service error.", HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }

            if(nsekey != null)
            {
                String amc_string = nsekey.getAmc_names();

                if(!amc_string.isEmpty())
                {
                    amc_list = new ArrayList<String>(Arrays.asList(amc_string.split(",")));
                }
            }

            if(!amc_list.isEmpty())
            {
                if(optionString.equalsIgnoreCase("SIF"))
                {
                    filteredSchemes = nseOnlineSchemeMasterRepository.getAllRedemptionSchemesBySifWithAmc(amc_list);
                }else
                {
                    filteredSchemes = nseOnlineSchemeMasterRepository.getAllRedemptionSchemesByOptionWithAmc(optionString,amc_list);
                }
            }else
            {
                if(optionString.equalsIgnoreCase("SIF"))
                {
                    filteredSchemes = nseOnlineSchemeMasterRepository.getAllRedemptionSchemesBySif();
                }else
                {
                    filteredSchemes = nseOnlineSchemeMasterRepository.getAllRedemptionSchemesByOption(optionString);
                }
            }

            if (filteredSchemes != null && !filteredSchemes.isEmpty())
            {
                schemeList = filteredSchemes.stream().map(row -> {
                            String schemeName = (String) row[0];
                            String scheme = (String) row[1];
                            String category = (String) row[2];
                            String amc_code = (String) row[3];
                            String amc_name = (String) row[4];
                            String logo = amcLogoPath + NseUtils.getLogoByAmcNameOrSchemeName(amc_code);
                            return new NewSchemePojo(schemeName,scheme, category, amc_code, amc_name, "", logo);
                        })
                        .sorted(Comparator.comparing(NewSchemePojo::getScheme_name, String.CASE_INSENSITIVE_ORDER))
                        .collect(Collectors.toList());
            }

            return ResponseEntity.ok(schemeList);

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
