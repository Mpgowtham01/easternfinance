package com.nse.controller;

import com.nse.client.UserServiceClient;
import com.nse.config.TokenInterceptor;
import com.nse.dto.mf.MymfboxOnboardingDto;
import com.nse.dto.mf.UserDto;
import com.nse.model.*;
import com.nse.pojo.BankDetails;
import com.nse.pojo.CommonPojo;
import com.nse.repository.*;

import com.nse.response.CommonResponse;
import com.nse.response.StatusMessage;
import com.nse.response.TransactionCommonResponse;
import com.nse.services.NsePincodeService;
import com.nse.utils.NseUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang.StringUtils;
import org.hibernate.internal.util.StringHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "NSE Utils Controller",
        description = "APIs related to NSE utilities like tax status, holding nature, countries, nominee relationships, etc."
)
public class NseUtilsController {
    private final NseAccountTypeRepository accountTypeRepo;
    private final NseApplicableIncomeRepository incomeRepo;
    private final NseBankRepository bankRepo;
    private final NseCountryRepository countryRepo;
    private final NseOccupationRepository occupationRepo;
    private final NseSourceWealthRepository wealthRepo;
    private final NsePincodeRepository pincodeRepo;

    @Autowired
    UserServiceClient userServiceClient;

    @Value("${jwt.secret-key}")
    private String secretKey;
    @Autowired
    private NsePincodeService nsePincodeService;

    public NseUtilsController(
            NseAccountTypeRepository accountTypeRepo,
            NseApplicableIncomeRepository incomeRepo,
            NseBankRepository bankRepo,
            NseCountryRepository countryRepo,
            NseOccupationRepository occupationRepo,
            NseSourceWealthRepository wealthRepo,
            NsePincodeRepository pincodeRepo
    ) {
        this.accountTypeRepo = accountTypeRepo;
        this.incomeRepo = incomeRepo;
        this.bankRepo = bankRepo;
        this.countryRepo = countryRepo;
        this.occupationRepo = occupationRepo;
        this.wealthRepo = wealthRepo;
        this.pincodeRepo = pincodeRepo;
    }

    @Operation(
            summary = "Get Tax Status",
            description = "Returns a predefined list of tax status options with code and description.\n\n" +
                    "Used during onboarding and KYC to help classify the investor.\n\n" +
                    "Handles errors gracefully with detailed messaging."
    )

    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = CommonPojo.class)))),
            @ApiResponse(responseCode = "400", description = "Failure Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommonResponse.class)))
    })
    @GetMapping("/getTaxStatus")
    public ResponseEntity<?> getTaxStatus(@RequestParam(required = false) Integer multiple_reg_flag) {
        try {

            List<CommonPojo> taxList = new ArrayList<>();

            if (multiple_reg_flag == null) {
                multiple_reg_flag = 0;
            }

            if(multiple_reg_flag == 1) {
                taxList.add(NseUtils.createCommonData("01", "Individual"));
                taxList.add(NseUtils.createCommonData("24", "NRO"));
                taxList.add(NseUtils.createCommonData("21", "NRE"));
            }else {
                taxList.add(NseUtils.createCommonData("01", "Individual"));
                taxList.add(NseUtils.createCommonData("02", "On behalf of minor"));
                taxList.add(NseUtils.createCommonData("03", "HUF"));
                taxList.add(NseUtils.createCommonData("04", "Company"));
                taxList.add(NseUtils.createCommonData("06", "Partnership Firm"));
                taxList.add(NseUtils.createCommonData("07", "Body Corporate"));
                taxList.add(NseUtils.createCommonData("08", "Trust"));
                taxList.add(NseUtils.createCommonData("13", "Proprietorship"));
                taxList.add(NseUtils.createCommonData("24", "NRO"));
                taxList.add(NseUtils.createCommonData("21", "NRE"));
                taxList.add(NseUtils.createCommonData("26", "NRI - Minor (NRE)"));
                taxList.add(NseUtils.createCommonData("28", "NRI - Minor (NRO)"));
                taxList.add(NseUtils.createCommonData("47", "Limited Liability Partnership (LLP)"));
                taxList.add(NseUtils.createCommonData("61", "OCI - Repatriation"));
                taxList.add(NseUtils.createCommonData("62", "OCI - Non-Repatriation"));
            }

            return ResponseEntity.ok(taxList);
        } catch (Exception ex) {
            ex.printStackTrace();
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.BAD_REQUEST );
        }
    }

    @Operation(
            summary = "Get Holding Nature",
            description = "Returns holding nature options (Single, Joint, etc.) based on provided tax_status code.\n\n" +
                    "Some tax statuses support multiple holding types, others only “Single”.\n\n" +
                    "Validates input and returns an appropriate error for missing or invalid values.",
            parameters = {
                    @Parameter(
                            name = "tax_status",
                            description = "Tax status code (e.g., 01 for Individual, 04 for Company, etc.)",
                            required = true,
                            in = ParameterIn.QUERY,
                            example = "01"
                    )
            }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = CommonPojo.class)))),
            @ApiResponse(responseCode = "400", description = "Failure Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommonResponse.class)))
    })
    @GetMapping({"/getHoldingNature"})
    public ResponseEntity<?> getHoldingNature(@RequestParam String tax_status)
    {
        try
        {
            tax_status = NseUtils.checkParem(tax_status);

            if(StringUtils.isEmpty(tax_status))
            {
                return NseUtils.commonResponse("Please provide the Tax Status", HttpStatus.BAD_REQUEST);
            }

            List<CommonPojo> holdingNatureList = new ArrayList<>();

            if(tax_status.equalsIgnoreCase("01") || tax_status.equalsIgnoreCase("11") || tax_status.equalsIgnoreCase("21") || tax_status.equalsIgnoreCase("24") || tax_status.equalsIgnoreCase("61") || tax_status.equalsIgnoreCase("62"))
            {
                holdingNatureList.add(NseUtils.createCommonData("SI", "SINGLE"));
                holdingNatureList.add(NseUtils.createCommonData("AS", "ANYONE / SURVIVOR"));
                holdingNatureList.add(NseUtils.createCommonData("JO", "JOINT"));
            }else
            {
                holdingNatureList.add(NseUtils.createCommonData("SI", "SINGLE"));
            }
            return ResponseEntity.ok(holdingNatureList);
        } catch (Exception ex)
        {
            ex.printStackTrace();
            return NseUtils.commonResponse("Something went wrong, We have taken note of the issue. Be rest assured we will fix it as soon as possible", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get Gender",
            description = "Returns a static list of available gender options (Male, Female).\n\n" +
                    "Meant for investor registration or profile setup purposes.\n\n" +
                    "Returns an error message in case of failure."
    )

    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = CommonPojo.class)))),
            @ApiResponse(responseCode = "400", description = "Failure Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommonResponse.class)))
    })
    @GetMapping({"/getGender"})
    public ResponseEntity<?> getGender()
    {
        try
        {
            List<CommonPojo> genderList = new ArrayList<>();
            genderList.add(NseUtils.createCommonData("M", "Male"));
            genderList.add(NseUtils.createCommonData("F", "FeMale"));
            return ResponseEntity.ok(genderList);
        } catch (Exception ex)
        {
            ex.printStackTrace();
            return NseUtils.commonResponse("Something went wrong, We have taken note of the issue. Be rest assured we will fix it as soon as possible", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get Email or Mobile Relation",
            description = "Returns predefined relationship types like Self, Spouse, Guardian, POA, etc.\n\n" +
                    "Used to capture who owns the mobile or email during onboarding/KYC.\n\n" +
                    "Returns a list of CommonPojo or an internal server error on failure."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = CommonPojo.class)))),
            @ApiResponse(responseCode = "400", description = "Failure Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommonResponse.class)))
    })
    @GetMapping({"/getEmailOrMobileRelation"})
    public ResponseEntity<?> getEmailOrMobileRelation()
    {
        try
        {
            List<CommonPojo> emailOrMobileRelation = NseUtils.getEmailOrMobileRelationList();
            return ResponseEntity.ok(emailOrMobileRelation);
        } catch (Exception ex)
        {
            ex.printStackTrace();
            return NseUtils.commonResponse("Something went wrong, We have taken note of the issue. Be rest assured we will fix it as soon as possible", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get Countries",
            description = "Fetches a list of countries with their respective codes and names.\n\n" +
                    "Useful for address, nationality, or residency details in KYC.\n"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Success Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CommonPojo.class)))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Failure Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CommonResponse.class))
            )
    })
    @GetMapping("/getCountries")
    public ResponseEntity<?> getCountries() {
        try {
            List<NseCountry> countries = countryRepo.findAll(); // blocking method
            List<CommonPojo> countryList = new ArrayList<>();
            for (NseCountry country : countries) {
                countryList.add(new CommonPojo(country.getCountry_name(), country.getCountry_code()));
            }
            // Sort by country name (desc)
            countryList.sort(Comparator.comparing(CommonPojo::getDesc));
            return ResponseEntity.ok(countryList);
        } catch (Exception ex) {
            ex.printStackTrace();
            return NseUtils.commonResponse("Something went wrong. We will fix it ASAP.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Operation(
            summary = "Get Employment Status",
            description = "Retrieves employment/occupation types for user profiling or regulatory purposes.\n\n" +
                    "Data is pulled from the occupation repository and sorted by name."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CommonPojo.class)))),
            @ApiResponse(responseCode = "400", description = "Failure Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CommonResponse.class)))
    })
    @GetMapping("/getEmploymentStatus")
    public ResponseEntity<?> getEmploymentStatus() {
        try {
            List<NseOccupation> occupations = occupationRepo.findAll(); // blocking
            List<CommonPojo> employmentStatusList = new ArrayList<>();
            for (NseOccupation occ : occupations) {
                employmentStatusList.add(new CommonPojo(occ.getOccupation_desc(), occ.getOccupation_code()));
            }
            employmentStatusList.sort(Comparator.comparing(CommonPojo::getDesc));
            return ResponseEntity.ok(employmentStatusList);
        } catch (Exception ex) {
            ex.printStackTrace();
            return NseUtils.commonResponse("Something went wrong. We will fix it ASAP.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Operation(
            summary = "Get Annual Income Slabs",
            description = "Returns available annual income slabs for investor declaration.\n\n" +
                    "Typically used in FATCA, KYC, or investment profiling.\n\n" +
                    "Pulled from the income repository and sorted alphabetically.\n" +
                    "\n"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CommonPojo.class)))),
            @ApiResponse(responseCode = "400", description = "Failure Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CommonResponse.class)))
    })
    @GetMapping("/getAnnualIncome")
    public ResponseEntity<?> getAnnualSalary() {
        try {
            List<NseApplicableIncome> incomes = incomeRepo.findAll(); // blocking
            List<CommonPojo> annualSalaryList = new ArrayList<>();
            for (NseApplicableIncome income : incomes) {
                annualSalaryList.add(new CommonPojo(income.getApp_income_desc(), income.getApp_income_code()));
            }
            annualSalaryList.sort(Comparator.comparing(CommonPojo::getDesc));
            return ResponseEntity.ok(annualSalaryList);
        } catch (Exception ex) {
            ex.printStackTrace();
            return NseUtils.commonResponse("Something went wrong. We will fix it ASAP.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Operation(
            summary = "Get Source Wealth",
            description = "Provides source of wealth/income options like Salary, Business, etc.\n\n" +
                    "Used in compliance checks and onboarding forms."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CommonPojo.class)))),
            @ApiResponse(responseCode = "400", description = "Failure Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CommonResponse.class)))
    })
    @GetMapping("/getSourceWealth")
    public ResponseEntity<?> getSourceIncome() {
        try {
            List<NseSourceWealth> allSources = wealthRepo.findAll();
            List<CommonPojo> sourceList = new ArrayList<>();
            for (NseSourceWealth source : allSources) {
                sourceList.add(new CommonPojo(source.getSource_of_wealth(), source.getCode()));
            }
            sourceList.sort(Comparator.comparing(CommonPojo::getDesc));
            return ResponseEntity.ok(sourceList);
        } catch (Exception ex) {
            ex.printStackTrace();
            return NseUtils.commonResponse("Something went wrong, We have taken note of the issue. Be rest assured we will fix it as soon as possible", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get Address Type",
            description = "Returns a list of address types such as Residential, Business, or Registered Office.\n\n" +
                    "Helps categorize user address during onboarding or KYC processes.\n\n" +
                    "Useful for validating or selecting the appropriate address type."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CommonPojo.class)))),
            @ApiResponse(responseCode = "400", description = "Failure Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CommonResponse.class)))
    })
    @GetMapping("/getAddressType")
    public ResponseEntity<?> getAddressType() {
        try {
            List<CommonPojo> list = Arrays.asList(
                    NseUtils.createCommonData("1", "Residential or Business"),
                    NseUtils.createCommonData("2", "Residential"),
                    NseUtils.createCommonData("3", "Business"),
                    NseUtils.createCommonData("4", "Registered office"),
                    NseUtils.createCommonData("5", "Unspecified")
            );
            return ResponseEntity.ok(list);
        } catch (Exception ex) {
            ex.printStackTrace();
            return NseUtils.commonResponse("Something went wrong, We have taken note of the issue. Be rest assured we will fix it as soon as possible", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get Political Relationship",
            description = "Provides political relationship values like Self, Related, or Not Applicable.\n\n" +
                    "Used to identify politically exposed persons (PEPs) during KYC.\n\n" +
                    "Ensures compliance with financial regulations."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CommonPojo.class)))),
            @ApiResponse(responseCode = "400", description = "Failure Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CommonResponse.class)))
    })
    @GetMapping("/getPoliticalRelationship")
    public ResponseEntity<?> getPoliticalRelationship() {
        try {
            List<CommonPojo> list = Arrays.asList(
                    NseUtils.createCommonData("Y", "I am politically exposed person"),
                    NseUtils.createCommonData("R", "I am related to politically exposed person"),
                    NseUtils.createCommonData("N", "Not applicable")
            );
            return ResponseEntity.ok(list);
        } catch (Exception ex) {
            ex.printStackTrace();
            return NseUtils.commonResponse("Something went wrong, We have taken note of the issue. Be rest assured we will fix it as soon as possible", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get City State By Pincode",
            description = "Fetches city and state information based on the provided pincode.\n\n" +
                    "Auto-fills location details during user registration or address update.\n\n" +
                    "Returns location details if pincode exists, else throws an error.",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = {
                    @Parameter(
                            name = "pincode",
                            description = "Pincode (e.g., 560078, etc.)",
                            required = true,
                            in = ParameterIn.QUERY,
                            example = "560078"
                    )
            }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NsePincode.class))),
            @ApiResponse(responseCode = "400", description = "Failure Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CommonResponse.class)))
    })
    @GetMapping("/getCityStateByPincode")
    public ResponseEntity<?> getCityStateByPincode(@RequestParam String pincode) {
        try {
            Optional<NsePincode> optionalPin = pincodeRepo.findByPincode(pincode);
            if (optionalPin.isPresent()) {
                return ResponseEntity.ok(optionalPin.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Pincode not found");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return NseUtils.commonResponse("Something went wrong, We have taken note of the issue. Be rest assured we will fix it as soon as possible", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get Nominee Type",
            description = "Returns nominee types such as Major and Minor.\n\n" +
                    "Used to categorize the nominee’s legal status in investment accounts.\n\n" +
                    "Helps enforce nomination rules based on age group."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CommonPojo.class)))),
            @ApiResponse(responseCode = "400", description = "Failure Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CommonResponse.class)))
    })
    @GetMapping("/getNomineeType")
    public ResponseEntity<?> getNomineeType() {
        try {
            List<CommonPojo> list = Arrays.asList(
                    NseUtils.createCommonData("N", "Major"),
                    NseUtils.createCommonData("Y", "Minor")
            );
            return ResponseEntity.ok(list);
        } catch (Exception ex) {
            ex.printStackTrace();
            return NseUtils.commonResponse("Something went wrong, We have taken note of the issue. Be rest assured we will fix it as soon as possible", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Operation(
            summary = "Get Nominee Id Type",
            description = "Returns nominee identification types such as PAN, Aadhaar, Driving Licence, and Passport.\n\n" +
                    "Used for verifying nominee identity during onboarding or KYC.\n\n" +
                    "Ensures compliance with regulatory requirements."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CommonPojo.class)))),
            @ApiResponse(responseCode = "400", description = "Failure Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CommonResponse.class)))
    })
    @GetMapping("/getNomineeIdType")
    public ResponseEntity<?> getNomineeIdType() {
        try {
            List<CommonPojo> list = Arrays.asList(
                    NseUtils.createCommonData("1", "PAN"),
                    NseUtils.createCommonData("2", "Adhaar"),
                    NseUtils.createCommonData("3", "Driving licence"),
                    NseUtils.createCommonData("4", "Passport")
            );
            return ResponseEntity.ok(list);
        } catch (Exception ex) {
            ex.printStackTrace();
            return NseUtils.commonResponse("Something went wrong, We have taken note of the issue. Be rest assured we will fix it as soon as possible", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get Nominee Relationship",
            description = "Provides a list of valid relationships between investor and nominee (e.g., Father, Spouse, Son).\n\n" +
                    "Helps capture accurate nominee details for legal clarity.\n\n" +
                    "Covers both familial and \"Others\" relationship types."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CommonPojo.class)))),
            @ApiResponse(responseCode = "400", description = "Failure Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CommonResponse.class)))
    })
    @GetMapping("/getNomineeRelationship")
    public ResponseEntity<?> getNomineeRelationship() {
        try {
            List<CommonPojo> list = Arrays.asList(
                    NseUtils.createCommonData("01", "AUNT"),
                    NseUtils.createCommonData("02", "BROTHER-IN-LAW"),
                    NseUtils.createCommonData("03", "BROTHER"),
                    NseUtils.createCommonData("04", "DAUGHTER"),
                    NseUtils.createCommonData("05", "DAUGHTER-IN-LAW"),
                    NseUtils.createCommonData("06", "FATHER"),
                    NseUtils.createCommonData("07", "FATHER-IN-LAW"),
                    NseUtils.createCommonData("08", "GRAND DAUGHTER"),
                    NseUtils.createCommonData("09", "GRAND FATHER"),
                    NseUtils.createCommonData("10", "GRAND MOTHER"),
                    NseUtils.createCommonData("11", "GRAND SON"),
                    NseUtils.createCommonData("12", "MOTHER-IN-LAW"),
                    NseUtils.createCommonData("13", "MOTHER"),
                    NseUtils.createCommonData("14", "NEPHEW"),
                    NseUtils.createCommonData("15", "NIECE"),
                    NseUtils.createCommonData("16", "SISTER"),
                    NseUtils.createCommonData("17", "SISTER-IN-LAW"),
                    NseUtils.createCommonData("18", "SON"),
                    NseUtils.createCommonData("19", "SON-IN-LAW"),
                    NseUtils.createCommonData("20", "SPOUSE"),
                    NseUtils.createCommonData("21", "UNCLE"),
                    NseUtils.createCommonData("22", "OTHERS")
            );
            return ResponseEntity.ok(list);
        } catch (Exception ex) {
            ex.printStackTrace();
            return NseUtils.commonResponse("Something went wrong, We have taken note of the issue. Be rest assured we will fix it as soon as possible", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get Nominee Guardian Relationship",
            description = "Returns guardian relationship types for minor nominees such as Father, Mother, or Legal Guardian.\n\n" +
                    "Essential for nominee validation when the nominee is underage.\n\n" +
                    "Helps ensure proper guardian details are recorded."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CommonPojo.class)))),
            @ApiResponse(responseCode = "400", description = "Failure Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CommonResponse.class)))
    })
    @GetMapping("/getNomineeGuardianRelationship")
    public ResponseEntity<?> getNomineeGuardianRelationship() {
        try {
            List<CommonPojo> list = Arrays.asList(
                    NseUtils.createCommonData("06", "FATHER"),
                    NseUtils.createCommonData("13", "MOTHER"),
                    NseUtils.createCommonData("23", "COURT APPOINTED LEGAL GUARDIAN")
            );
            return ResponseEntity.ok(list);
        } catch (Exception ex) {
            ex.printStackTrace();
            return NseUtils.commonResponse("Something went wrong, We have taken note of the issue. Be rest assured we will fix it as soon as possible", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get Banks",
            description = "Returns a list of banks with their codes and names.\n\n" +
                    "Used for capturing or validating the investor’s bank details.\n\n" +
                    "Sorted alphabetically for ease of selection in forms."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CommonPojo.class)))),
            @ApiResponse(responseCode = "400", description = "Failure Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CommonResponse.class)))
    })
    @GetMapping("/getBanks")
    public ResponseEntity<?> getBanks() {
        try {
            List<NseBank> allBanks = bankRepo.findAll();
            List<CommonPojo> bankList = new ArrayList<>();
            for (NseBank bank : allBanks) {
                bankList.add(new CommonPojo(bank.getBank_name(), bank.getBank_code()));
            }
            bankList.sort(Comparator.comparing(CommonPojo::getDesc));
            return ResponseEntity.ok(bankList);
        } catch (Exception ex) {
            ex.printStackTrace();
            return NseUtils.commonResponse("Something went wrong, We have taken note of the issue. Be rest assured we will fix it as soon as possible", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get Bank Details By IfscCode",
            description = "Fetches detailed bank information for a given IFSC code using Razorpay’s public IFSC API.\n\n" +
                    "Useful for validating user-provided bank details during onboarding or KYC.\n\n" +
                    "Returns a BankDetails object on success or an error if the IFSC is invalid.\n\n" +
                    "\n",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = {
                    @Parameter(
                            name = "ifsc_code",
                            description = "IFSC Code (e.g., HDFC002850, etc.)",
                            required = true,
                            in = ParameterIn.QUERY
                    )
            }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BankDetails.class))),
            @ApiResponse(responseCode = "400", description = "Failure Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommonResponse.class)))
    })
    @GetMapping("/getBankDetailsByIfscCode")
    public ResponseEntity<?> getBankDetailsByIfscCode(@RequestParam String ifsc_code) {
        try {

            RestTemplate restTemplate = new RestTemplate();
            String url = "https://ifsc.razorpay.com/" + ifsc_code;
            BankDetails bankDetails = restTemplate.getForObject(url, BankDetails.class);

            if(bankDetails != null && bankDetails.getMicr() == null)
            {
                bankDetails.setMicr("");
            }
            return ResponseEntity.ok(bankDetails);
        } catch (Exception ex) {
            ex.printStackTrace();
            return NseUtils.commonResponse("Invalid IFSC code or unable to fetch details",
                    HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(
            summary = "Get Account Types",
            description = "Returns a sorted list of available account types used during investor registration.\n\n" +
                    "Data is fetched from the local database and formatted for client-side dropdowns.\n\n" +
                    "Returns a list of account types or an internal error message on failure."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CommonPojo.class)))),
            @ApiResponse(responseCode = "400", description = "Failure Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CommonResponse.class)))
    })
    @GetMapping("/getAccountTypes")
    public ResponseEntity<?> getAccountTypes(@RequestHeader("Authorization") String token)
    {
        String tax_status = "";
        try
        {
            String userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            String client_name = TokenInterceptor.extractClientNamedFromToken(token, secretKey);

            MymfboxOnboardingDto user = userServiceClient.getOrCreateOnboarding(client_name,Integer.valueOf(userid), token);

            if (user != null)
            {
                tax_status = user.getTax_status();
            }

            List<CommonPojo> accountTypeList = new ArrayList<>();
            List<NseAccountType> allTypes = accountTypeRepo.findAll();

            if(StringHelper.isNotEmpty(tax_status))
            {
                if(tax_status.equalsIgnoreCase("24") || tax_status.equalsIgnoreCase("28"))
                {
                    allTypes = allTypes.stream().filter(accountType -> accountType.getAccount_type().equalsIgnoreCase("NO")).collect(Collectors.toList());
                }else if(tax_status.equalsIgnoreCase("21") || tax_status.equalsIgnoreCase("26"))
                {
                    allTypes = allTypes.stream().filter(accountType -> accountType.getAccount_type().equalsIgnoreCase("NE")).collect(Collectors.toList());
                }else if(tax_status.equalsIgnoreCase("61") || tax_status.equalsIgnoreCase("62"))
                {
                    allTypes = allTypes.stream()
                            .filter(accountType -> accountType.getAccount_type().equalsIgnoreCase("NE")
                                    || accountType.getAccount_type().equalsIgnoreCase("NO"))
                            .collect(Collectors.toList());
                }else
                {
                    allTypes = allTypes.stream()
                            .filter(accountType -> accountType.getAccount_type().equalsIgnoreCase("SB")
                                    || accountType.getAccount_type().equalsIgnoreCase("CB"))
                            .collect(Collectors.toList());
                }
            }

            if(allTypes != null && allTypes.size() > 0)
            {
                for (NseAccountType account : allTypes)
                {
                    accountTypeList.add(new CommonPojo(account.getDescription(), account.getAccount_type()));
                }

                accountTypeList.sort(Comparator.comparing(CommonPojo::getDesc));
            }

            return ResponseEntity.ok(accountTypeList);


        } catch (Exception ex)
        {
            ex.printStackTrace();
            return NseUtils.commonResponse("Something went wrong, We have taken note of the issue. Be rest assured we will fix it as soon as possible", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get Account Relationship",
            description = "Retrieves a sorted list of account relationship types used during investor registration.\n\n" +
                    "Each item contains a description and a corresponding code representing the relationship type.\n\n" +
                    "Sample Response:\n" +
                    "[\n" +
                    "  { \"desc\": \"Guardian Only Account\", \"code\": \"G\" },\n" +
                    "  { \"desc\": \"Minor Only Account\", \"code\": \"M\" },\n" +
                    "  { \"desc\": \"Minor Under the Guardian Account\", \"code\": \"U\" }\n" +
                    "]\n\n" +
                    "This data is sourced from the local database and intended for use in client-side dropdowns or selection menus.\n\n" +
                    "Returns the list on success or an error message on failure."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CommonPojo.class)))),
            @ApiResponse(responseCode = "400", description = "Failure Response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CommonResponse.class)))
    })

    @GetMapping({"/getAccountRelationship"})
    public ResponseEntity<?> getAccountRelationship()
    {
        try
        {
            List<CommonPojo> holdingNatureList = new ArrayList<>();
                holdingNatureList.add(NseUtils.createCommonData("G", "Guardian Only Account"));
                holdingNatureList.add(NseUtils.createCommonData("M", "Minor Only Account"));
                holdingNatureList.add(NseUtils.createCommonData("U", "Minor Under the Guardian Account"));

            return ResponseEntity.ok(holdingNatureList);
        } catch (Exception ex)
        {
            ex.printStackTrace();
            return NseUtils.commonResponse("Something went wrong, We have taken note of the issue. Be rest assured we will fix it as soon as possible", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getCityStateForPincode")
    public ResponseEntity<?> getCityStateForPincode(@RequestHeader("Authorization") String token, @RequestParam String pinCode)
    {
        try
        {
            if(!TokenInterceptor.isValidToken(token, secretKey))
            {
                return NseUtils.commonResponse("token not valid or empty!", HttpStatus.UNAUTHORIZED);
            }
            pinCode = NseUtils.checkParem(pinCode);
            if(pinCode.isEmpty()){
                return NseUtils.commonResponse("pincode empty!", HttpStatus.BAD_REQUEST);
            }
            NsePincode pin = nsePincodeService.getPincodeDetails(pinCode).orElse(null);
            return ResponseEntity.ok(pin);

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getBankMandateTypes")
    public ResponseEntity<?> getBankMandateTypes(@RequestHeader("Authorization") String token)
    {
        List<CommonPojo> masterList = new ArrayList<CommonPojo>();
        CommonPojo pojo = null;
        try
        {
            pojo = new CommonPojo();
            pojo.setCode("P");
            pojo.setDesc("Physical");
            masterList.add(pojo);

            pojo = new CommonPojo();
            pojo.setCode("EMANDATE");
            pojo.setDesc("E-Mandate");
            masterList.add(pojo);

            TransactionCommonResponse apiResponse = new TransactionCommonResponse();
            apiResponse.setStatus(StatusMessage.SuccessCode);
            apiResponse.setStatus_msg(StatusMessage.SuccessMessage);
            apiResponse.setMsg(StatusMessage.SuccessMessage);
            apiResponse.setList(masterList);
            return  ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            e.printStackTrace();
            return NseUtils.commonResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


}