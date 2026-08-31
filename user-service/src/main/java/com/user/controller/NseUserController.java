package com.user.controller;

import com.user.config.TokenInterceptor;
import com.user.dto.*;
import com.user.mapper.*;
import com.user.model.*;
import com.user.pojo.CommonPojo;
import com.user.pojo.UserRegStatusPojo;
import com.user.repository.*;
import com.user.response.*;
import com.user.service.*;
import com.user.utils.UserUtils;
import com.user.validate.UserValidate;
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
import org.apache.commons.lang.StringUtils;
import org.hibernate.internal.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@Tag(
        name = "NSE User Controller",
        description = "APIs related to NSE onboarding like saveInvestorInfo, savePersonalInfo, etc."
)
public class NseUserController
{
    private static final Logger logger = LoggerFactory.getLogger(FeignClientUserController.class);

    @Autowired
    UserService userService;

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Autowired
    UserBseNseDetailsService userBseNseDetailsService;

    @Autowired
    CheckNseIinNumber checkNseIinNumber;

    @Autowired
    BseNseKeyRepository bseNseKeyRepository;

    @Autowired
    OnboardingService onboardingService;

    @Autowired
    LogService logService;

    @Autowired
    UserRepository userRepository;

    @Value("${vendor.logo.url}")
    private String vendorLogoPath;

    @Value("custom.server.url")
    private String customServerUrl;

    @Autowired
    UserOnlineRegDetailsRespository userOnlineRegDetailsRespository;

    @Autowired
    UsersNomineeDetailsRepository usersNomineeDetailsRepository;

    @Autowired
    UsersBankDetailsRepository usersBankDetailsRepository;

    @Autowired
    UsersOnlineRegDetailsService usersOnlineRegDetailsService;

    @Operation
    (
        summary = "Save Investor Information, Specifically created for Mobile App",
            description =
                    "Saves investor information submitted via the mobile application.\n\n" +
                            "This endpoint captures essential investor details required for onboarding and KYC validation. " +
                            "It is intended specifically for mobile app users and stores information such as PAN number, " +
                            "broker code, investor code, tax status, and holding nature.\n\n" +
                            "Fields Collected:\n" +
                            "- **pan :** PAN number of the investor (e.g., ABCDE1234F)\n" +
                            "- **brokerCode :** AMFI-registered broker code (e.g., ARN-77441)\n" +
                            "- **investorCode :** NSE IIN Number (e.g., INV1001)\n" +
                            "- **taxStatusCode and taxStatusDesc :** Code and description of investor's tax status (e.g., 01, Individual)\n" +
                            "- **holdingNatureCode and holdingNatureDesc :** Code and description of the investment holding nature (e.g., 01, Single)\n\n" +
                            "A successful response indicates that the investor information has been saved.\n" +
                            "If validation fails or required data is missing, an appropriate error response is returned.",

        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Investor information to be saved", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = InvestorInfoDTO.class)))
    )
    @ApiResponses(value =
    {
        @ApiResponse(responseCode = "200", description = "Success Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SuccessResponse.class))),
        @ApiResponse(responseCode = "400", description = "Failure Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
    })

    @PostMapping("/saveInvestorInfo")
    public ResponseEntity<?> saveInvestorInfo(@RequestBody InvestorInfoDTO dto, @RequestHeader("Authorization") String token,@RequestParam(required = false) String is_MultiReg)
    {
        try {
            if (dto == null) {
                return UserUtils.errorResponse("Investor cannot be empty", HttpStatus.BAD_REQUEST);
            }

            String error = UserValidate.validateInvestorInfo(dto);

            if (error != null) {
                return UserUtils.errorResponse(error, HttpStatus.BAD_REQUEST);
            }

            PanKYCResponse panStatus = UserUtils.checkPanKycStatus(dto.getPan());

            if (panStatus != null && !panStatus.getKyc_status()) {
                return UserUtils.errorResponse(panStatus.getMsg(), HttpStatus.BAD_REQUEST);
            }
            is_MultiReg = UserUtils.checkParem(is_MultiReg);
            String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            Integer userId = Integer.parseInt(userIdFromToken);

            Optional<UsersOnlineRegDetails> userOpt = userOnlineRegDetailsRespository.findUSerByIdAndActive(userId);
            System.out.println("useropt = " + userOpt);
            if (userOpt.isEmpty()) {
                return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
            }

            UsersOnlineRegDetails user = userOpt.get();
            UsersOnlineRegDetails userDetails = null;

            Boolean isMultiReg = false;
            if(is_MultiReg.equalsIgnoreCase("1"))
            {
                isMultiReg = true;
            }
            System.out.println("isMultiReg = " + isMultiReg);

            MymfboxOnboarding onboarding = null;

            if(!is_MultiReg.isEmpty()) {
                onboarding = onboardingService.getOrCreateOnboardingbyMultireg(user.getId(), user.getClient_name(), isMultiReg);
            }else{
                onboarding = onboardingService.getOrCreateOnboarding(user.getId(), user.getClient_name());
            }

            System.out.println("onboardi  = " + onboarding);
            if (onboarding == null) {
                return UserUtils.errorResponse("Onboarding Details not found.", HttpStatus.NOT_FOUND);
            }

            boolean taxStatusFlag = false;

            if (!onboarding.getTax_status().equalsIgnoreCase(dto.getTaxStatusDesc())) {
                taxStatusFlag = true;
            } else if (!onboarding.getHolding_nature().equalsIgnoreCase(dto.getHoldingNatureDesc())) {
                taxStatusFlag = true;
            }

            onboarding.setVendor("NSE");
            onboarding.setTax_status(dto.getTaxStatusCode());
            onboarding.setHolding_nature(dto.getHoldingNatureCode());

            if (taxStatusFlag) {
                onboarding.setInvestor_info(true);
                onboarding.setPersonal_info(false);
                onboarding.setContact_info(false);
                onboarding.setNri_info(false);
                onboarding.setJoint_holder_info(false);
                onboarding.setNomiee_info(false);
                onboarding.setBank_info(false);
                onboarding.setSignature_info(false);
                onboarding.setIs_all_steps_completed(false);
                onboarding.setIs_registration_completed(false);
            } else {
                onboarding.setInvestor_info(true);
            }

            if (Arrays.asList("01", "24", "21", "61", "62").contains(dto.getTaxStatusCode())) {
                onboarding.setHas_nominee(true);
            } else {
                onboarding.setHas_nominee(false);
            }

            if (Arrays.asList("AS", "JO").contains(dto.getHoldingNatureCode())) {
                onboarding.setHas_joint_holder(true);
            } else {
                onboarding.setHas_joint_holder(false);
            }

            if (Arrays.asList("24", "21", "26", "28", "61", "62").contains(dto.getTaxStatusCode())) {
                onboarding.setHas_nri(true);
            } else {
                onboarding.setHas_nri(false);
            }

            if(user.getNse_customer().equals(1) && user.getNse_active().equals(1) && StringHelper.isNotEmpty(user.getNse_iin_number()))
            {
                List<UsersOnlineRegDetails> userDetailsOpt = userOnlineRegDetailsRespository.getNseInactiveUserRegDetailsByUserIdAndClientName(user.getId(), user.getClient_name());

                if (userDetailsOpt != null && !userDetailsOpt.isEmpty()) {
                    userDetails = userDetailsOpt.get(0);
                } else {
                    userDetails = new UsersOnlineRegDetails();
                    userDetails.setUser_id(user.getId());
                    userDetails.setClient_name(user.getClient_name());
                    userDetails.setName(user.getName());
                    userDetails.setEmail(user.getEmail());
                    userDetails.setPan(user.getPan());
                    userDetails.setMobile(user.getMobile());
                    userDetails.setCreated_date(new Date());
                    userDetails.setRegister_source("Mobile App");
                    userDetails.setBroker_code(user.getBroker_code());
                    userDetails.setNse_active(0);
                    userDetails.setNse_customer(0);
                }

                userDetails = InvestorInfoMapper.mapDtoToUserBseNseDetails(dto, userDetails);

                if(StringHelper.isNotEmpty(dto.getInvestorCode()))
                {
                    userDetails.setNse_iin_number(dto.getInvestorCode());
                }
                else if(StringHelper.isNotEmpty(dto.getPan()))
                {
                    List<UsersOnlineRegDetails> userList = userOnlineRegDetailsRespository.getUserDetailsByIinNumberAndClientName(dto.getPan().toUpperCase(), user.getClient_name());

                    if(userList != null && !userList.isEmpty())
                    {
                        String iin_number_new = checkNseIinNumber.CheckNseIinNumbers(user.getClient_name());
                        userDetails.setNse_iin_number(iin_number_new);
                    }else
                    {
                        userDetails.setNse_iin_number(dto.getPan().toUpperCase());
                    }
                }
                else
                {
                    String iin_number_new = checkNseIinNumber.CheckNseIinNumbers(user.getClient_name());
                    userDetails.setNse_iin_number(iin_number_new.toUpperCase());
                }

                onboarding.setIs_multiple_registration(onboarding.getIs_multiple_registration());

               userBseNseDetailsService.saveOrUpdateUserOnlineReg(userDetails);
            }else
            {
                user = InvestorInfoMapper.mapDtoToUser(dto, user);

                if (StringHelper.isNotEmpty(dto.getInvestorCode()))
                {
                    user.setNse_iin_number(dto.getInvestorCode());
                } else if (StringHelper.isNotEmpty(dto.getPan()))
                {
                    List<User> userList = userRepository.getUserDetailsByIinNumberAndClientName(dto.getPan().toUpperCase(), user.getClient_name());

                    if(userList != null && !userList.isEmpty())
                    {
                        String iin_number_new = checkNseIinNumber.CheckNseIinNumbers(user.getClient_name());
                        user.setNse_iin_number(iin_number_new);
                    }else
                    {
                        user.setNse_iin_number(dto.getPan().toUpperCase());
                    }
                } else
                {
                    String iin_number_new = checkNseIinNumber.CheckNseIinNumbers(user.getClient_name());
                    user.setNse_iin_number(iin_number_new.toUpperCase());
                }

                onboarding.setIs_multiple_registration(false);

                userService.saveOrUpdateUser(user);
            }

            return UserUtils.successResponse("Investor information saved successfully.", HttpStatus.OK);

        }catch(Throwable ex)
        {
            logger.error("Error while saving investor information", ex);
            return UserUtils.errorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isUnder18(String dobStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate dob = LocalDate.parse(dobStr, formatter);
        LocalDate today = LocalDate.now();
        Period age = Period.between(dob, today);
        return age.getYears() < 18;
    }

    @Operation(
            summary = "Save Personal Information",
            description =
                    "Stores the investor's personal and KYC-related information required for onboarding.\n\n" +
                            "This endpoint accepts detailed investor data, such as PAN, contact details, tax status, occupation, income, and guardian information (if applicable). It is typically used during the registration or account creation process in compliance with regulatory requirements.\n\n" +
                            "**Key Fields Captured:**\n" +
                            "- **pan:** Permanent Account Number (e.g., ABCDE1234F)\n" +
                            "- **name, fatherName, dob, gender:** Basic identity details\n" +
                            "- **email, mobile:** Contact information with relationship codes\n" +
                            "- **brokerCode:** AMFI-registered broker code (e.g., ARN-77441)\n" +
                            "- **investorCode:** Unique IIN code for the investor (e.g., INV1001)\n" +
                            "- **taxStatusCode, taxStatusDesc:** Income tax classification (e.g., 01 = Individual)\n" +
                            "- **holdingNatureCode, holdingNatureDesc:** Investment holding type (e.g., 01 = Single)\n" +
                            "- **occupationCode, incomeCode, sourceWealthCode:** Financial background\n" +
                            "- **politicalStatusCode:** Politically Exposed Person (PEP) status (e.g., N = Not exposed)\n" +
                            "- **guardian details:** Required if the investor is a minor (e.g., guardName, guardPan)\n" +
                            "- **addressTypeCode, networthAmount, source:** Address and financial declaration\n\n" +
                            "### Notes:\n" +
                            "- Fields like `multiple` flag if the investor holds multiple accounts.\n" +
                            "- Validation errors or missing required fields will result in error responses.\n" +
                            "- All dates should follow the YYYY-MM-DD format.\n\n" +
                            "A successful response confirms the data is stored successfully.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Personal Information object containing all required investor details",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PersonalInfoDTO.class)
                    )
            )
    )

    @ApiResponses(value =
            {
                    @ApiResponse(responseCode = "200", description = "Success Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SuccessResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Failure Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            })

//    @PostMapping("/savePersonalInfo")
//    public ResponseEntity<?> savePersonalInfo(@RequestBody PersonalInfoDTO dto, @RequestHeader("Authorization") String token)
//    {
//        try
//        {
//            // 1. Null check
//            if (dto == null)
//            {
//                return UserUtils.errorResponse("Personal details cannot be empty", HttpStatus.BAD_REQUEST);
//            }
//
//            if (StringUtils.isEmpty(dto.getSource()))
//            {
//                return UserUtils.errorResponse("Source Cannot be empty", HttpStatus.BAD_REQUEST);
//            }
//
//            if ("Individual".equalsIgnoreCase(dto.getTaxStatusDesc())) {
//                if (isUnder18(dto.getDob())) {
//                    Map<String, Object> errorResponse = new HashMap<>();
//                    errorResponse.put("status", 400);
//                    errorResponse.put("status_msg", "Bad Request");
//                    errorResponse.put("message", "For Individual tax status, DOB must be 18 years or above.");
//
//                    return ResponseEntity
//                            .status(HttpStatus.BAD_REQUEST)
//                            .body(errorResponse);
//                }
//            }
//
//            String error = UserValidate.validatePersonalInfo(dto);
//
//            if (error != null)
//            {
//                return UserUtils.errorResponse(error, HttpStatus.BAD_REQUEST);
//            }
//
//            String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
//            Integer userId = Integer.parseInt(userIdFromToken);
//
//            Optional<User> userOpt = userRepository.findUSerByIdAndActive(userId);
//
//            if (!userOpt.isPresent())
//            {
//                return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
//            }
//
//            User user = userOpt.get();
//            UserBseNseDetails userDetails = null;
//
//            MymfboxOnboarding onboarding = onboardingService.getOrCreateOnboarding(user.getId(), user.getClient_name());
//
//            if (onboarding == null)
//            {
//                return UserUtils.errorResponse("Could not create onboarding record", HttpStatus.BAD_REQUEST);
//            }
//
//            if(dto.getSource().equalsIgnoreCase("Website"))
//            {
//                boolean taxStatusFlag = false;
//
//                if(onboarding != null)
//                {
//                    if(!onboarding.getTax_status().equalsIgnoreCase(dto.getTaxStatusCode()))
//                    {
//                        taxStatusFlag = true;
//                    } else if(!onboarding.getHolding_nature().equalsIgnoreCase(dto.getHoldingNatureDesc()))
//                    {
//                        taxStatusFlag = true;
//                    }
//                }
//
//                if(dto.getMultiple().equals(1))
//                {
//                    onboarding.setIs_multiple_registration(true);
//                }
//
//                onboarding.setVendor("NSE");
//                onboarding.setTax_status(dto.getTaxStatusCode());
//                onboarding.setHolding_nature(dto.getHoldingNatureCode());
//                onboarding.setInv_category(dto.getInvestorCode());
//
//                if(taxStatusFlag)
//                {
//                    onboarding.setInvestor_info(true);
//                    onboarding.setPersonal_info(false);
//                    onboarding.setContact_info(false);
//                    onboarding.setNri_info(false);
//                    onboarding.setJoint_holder_info(false);
//                    onboarding.setNomiee_info(false);
//                    onboarding.setBank_info(false);
//                    onboarding.setSignature_info(false);
//                } else
//                {
//                    onboarding.setInvestor_info(true);
//                }
//
//                if(Arrays.asList("01","11","21","26","28","61","62").contains(dto.getTaxStatusCode()))
//                {
//                    onboarding.setHas_nominee(true);
//                } else
//                {
//                    onboarding.setHas_nominee(false);
//                }
//
//                if(Arrays.asList("AS","ES","JO").contains(dto.getHoldingNatureCode()))
//                {
//                    onboarding.setHas_joint_holder(true);
//                } else
//                {
//                    onboarding.setHas_joint_holder(false);
//                }
//
//                if(Arrays.asList("11","21","26","28","61","62").contains(dto.getTaxStatusCode()))
//                {
//                    onboarding.setHas_nri(true);
//                } else
//                {
//                    onboarding.setHas_nri(false);
//                }
//
//                onboardingService.saveOnboarding(onboarding);
//            }
//
//            if(user.getNse_active().equals(1))
//            {
//                Optional<UserBseNseDetails> userDetailsOpt = userBseNseDetailsService.getUserBseNseDetailsByUserIdAndClientName(user.getId(), user.getClient_name());
//
//                if(userDetailsOpt.isEmpty())
//                {
//                    return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
//                }else
//                {
//                    userDetails = userDetailsOpt.get();
//                }
//            }
//
//            if(userDetails != null)
//            {
//                userDetails = PersonalInfoMapper.dtoToUserBseNseDetails(dto, userDetails);
//
//                if(StringHelper.isNotEmpty(dto.getInvestorCode()))
//                {
//                    userDetails.setNse_iin_number(dto.getInvestorCode());
//                }
//                else if(StringHelper.isNotEmpty(dto.getPan()))
//                {
//                    userDetails.setNse_iin_number(dto.getPan().toUpperCase());
//                }
//                else
//                {
//                    String iin_number_new = checkNseIinNumber.CheckNseIinNumbers(user.getClient_name());
//                    userDetails.setNse_iin_number(iin_number_new.toUpperCase());
//                }
//                userBseNseDetailsService.saveOrUpdateUserBseNseDetails(userDetails);
//            }
//            else
//            {
//                user = PersonalInfoMapper.dtoToUser(dto, user);
//
//                if (StringHelper.isNotEmpty(dto.getInvestorCode()))
//                {
//                    user.setNse_iin_number(dto.getInvestorCode());
//                } else if (StringHelper.isNotEmpty(dto.getPan()))
//                {
//                    user.setNse_iin_number(dto.getPan().toUpperCase());
//                } else
//                {
//                    String iin_number_new = checkNseIinNumber.CheckNseIinNumbers(user.getClient_name());
//                    user.setNse_iin_number(iin_number_new.toUpperCase());
//                }
//
//                userService.saveOrUpdateUser(user);
//            }
//
//            onboarding.setPersonal_info(true);
//            onboardingService.saveOnboarding(onboarding);
//
//            return UserUtils.successResponse("Personal information saved successfully.", HttpStatus.OK);
//        }catch(Exception ex)
//        {
//            logger.error("Error while saving personal information", ex);
//            return UserUtils.errorResponse("Something went wrong. We have taken note of the issue. Rest assured it will be fixed ASAP.", HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
    @PostMapping("/savePersonalInfo")
    public ResponseEntity<?> savePersonalInfo(@RequestBody PersonalInfoDTO dto, @RequestHeader("Authorization") String token,@RequestParam(required = false) String is_MultiReg)
    {
        try
        {
            if (dto == null)
            {
                return UserUtils.errorResponse("Personal details cannot be empty", HttpStatus.BAD_REQUEST);
            }

            if (StringUtils.isEmpty(dto.getSource()))
            {
                return UserUtils.errorResponse("Source Cannot be empty", HttpStatus.BAD_REQUEST);
            }

            String error = UserValidate.validatePersonalInfo(dto);

            if (error != null)
            {
                return UserUtils.errorResponse(error, HttpStatus.BAD_REQUEST);
            }
            is_MultiReg = UserUtils.checkParem(is_MultiReg);
            String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            Integer userId = Integer.parseInt(userIdFromToken);

            Optional<UsersOnlineRegDetails> userOpt = userOnlineRegDetailsRespository.findUSerByIdAndActive(userId);

            if (userOpt.isEmpty())
            {
                return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
            }

            UsersOnlineRegDetails user = userOpt.get();
            UsersOnlineRegDetails userDetails = null;
            Boolean isMultiReg = false;
            if(is_MultiReg.equalsIgnoreCase("1"))
            {
                isMultiReg = true;
            }
            System.out.println("isMultiReg = " + isMultiReg);

            MymfboxOnboarding onboarding = null;

            if(!is_MultiReg.isEmpty()) {
                onboarding = onboardingService.getOrCreateOnboardingbyMultireg(user.getId(), user.getClient_name(), isMultiReg);
                System.out.println("isMultiReg1 = " + isMultiReg);
            }else{
                onboarding = onboardingService.getOrCreateOnboarding(user.getId(), user.getClient_name());
                System.out.println("isMultiReg2 = " + isMultiReg);
            }

            if (onboarding == null)
            {
                return UserUtils.errorResponse("Could not create onboarding record", HttpStatus.BAD_REQUEST);
            }


            if(user.getNse_customer().equals(1) && user.getNse_active().equals(1) && StringHelper.isNotEmpty(user.getNse_iin_number()))
            {
                List<UsersOnlineRegDetails> userDetailsOpt = userOnlineRegDetailsRespository.getNseInactiveUserRegDetailsByUserIdAndClientName(onboarding.getUser_id(), user.getClient_name());

                if (userDetailsOpt != null && !userDetailsOpt.isEmpty()) {
                    userDetails = userDetailsOpt.get(0);
                }

                if (userDetails != null) {
                    userDetails = PersonalInfoMapper.dtoToUserBseNseDetails(dto, userDetails);
                    userBseNseDetailsService.saveOrUpdateUserOnlineReg(userDetails);
                }
                else{
                    user = PersonalInfoMapper.dtoToUser(dto, user);
                    userService.saveOrUpdateUser(user);
                }
            }else{
                user = PersonalInfoMapper.dtoToUser(dto, user);
                userService.saveOrUpdateUser(user);
            }

            onboarding.setPersonal_info(true);
            onboardingService.saveOnboarding(onboarding);

            return UserUtils.successResponse("Personal information saved successfully.", HttpStatus.OK);
        }catch(Exception ex)
        {
            logger.error("Error while saving personal information", ex);
            return UserUtils.errorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Save NRI Information",
            description = """
        Saves Non-Resident Indian (NRI) investor address information submitted through the client application.

        This endpoint is used during onboarding and KYC verification to collect the NRI investor’s residential address details.

        **Fields Collected:**
        - **address1**: Address line 1 (e.g., 123, Palm Street)
        - **address2**: Address line 2 (e.g., Near Marina Bay)
        - **address3**: Address line 3 (e.g., Apartment 45B)
        - **city**: City of residence (e.g., Dubai)
        - **state**: State or province (e.g., Dubai)
        - **pincode**: Postal code (e.g., 00000)
        - **country**: Country of residence (e.g., United Arab Emirates)

        A successful response indicates that the NRI address information has been saved.
        If validation fails or required data is missing, an appropriate error response is returned.
    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "NRI address information to be saved",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NriInfoDTO.class)
                    )
            )
    )
    @ApiResponses(value =
            {
                    @ApiResponse(responseCode = "200", description = "Success Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SuccessResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Failure Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            })
    @PostMapping("/saveNriInfo")
    public ResponseEntity<?> saveNriInfo(@RequestBody NriInfoDTO dto, @RequestHeader("Authorization") String token,@RequestParam(required = false) String is_MultiReg)
    {
        try
        {
            if (dto == null)
            {
                return UserUtils.errorResponse("NRI details cannot be empty", HttpStatus.BAD_REQUEST);
            }
            String error = UserValidate.validateNriInfo(dto);

            if (error != null)
            {
                return UserUtils.errorResponse(error, HttpStatus.BAD_REQUEST);
            }

            is_MultiReg = UserUtils.checkParem(is_MultiReg);

            String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            Integer userId = Integer.parseInt(userIdFromToken);
            System.out.println("userid = " + userId);
            Optional<UsersOnlineRegDetails> userOpt = userOnlineRegDetailsRespository.findUSerByIdAndActive(userId);
            System.out.println("userOpt = " + userOpt);
            if (userOpt.isEmpty())
            {
                return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
            }

            UsersOnlineRegDetails user = userOpt.get();
            UsersOnlineRegDetails userDetails = null;

            Boolean isMultiReg = false;
            if(is_MultiReg.equalsIgnoreCase("1"))
            {
                isMultiReg = true;
            }
            System.out.println("isMultiReg = " + isMultiReg);

            MymfboxOnboarding onboarding = null;

            if(!is_MultiReg.isEmpty()) {
                onboarding = onboardingService.getOrCreateOnboardingbyMultireg(user.getId(), user.getClient_name(), isMultiReg);
            }else{
                onboarding = onboardingService.getOrCreateOnboarding(user.getId(), user.getClient_name());
            }

            if (onboarding == null)
            {
                return UserUtils.errorResponse("Could not create onboarding record", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            if(user.getNse_customer().equals(1) && user.getNse_active().equals(1) && StringHelper.isNotEmpty(user.getNse_iin_number()))
            {
                List<UsersOnlineRegDetails> userDetailsOpt = userOnlineRegDetailsRespository.getNseInactiveUserRegDetailsByUserIdAndClientName(onboarding.getUser_id(), user.getClient_name());

                if (userDetailsOpt != null && !userDetailsOpt.isEmpty()) {
                    userDetails = userDetailsOpt.get(0);
                }

                if (userDetails != null) {
                    userDetails = NriInfoMapper.dtoToUserBseNseDetails(dto, userDetails);
                    userBseNseDetailsService.saveOrUpdateUserOnlineReg(userDetails);
                }
                else{
                    user = NriInfoMapper.dtoToUser(dto, user);
                    userService.saveOrUpdateUser(user);
                }
            }else {
                user = NriInfoMapper.dtoToUser(dto, user);
                userService.saveOrUpdateUser(user);
            }
            onboarding.setNri_info(true);
            onboardingService.saveOnboarding(onboarding);

            return UserUtils.successResponse("NRI information saved successfully.", HttpStatus.OK);
        }catch(Exception ex)
        {
            logger.error("Error while saving personal information", ex);
            return UserUtils.errorResponse(
                    "Something went wrong. We have taken note of the issue. Rest assured it will be fixed ASAP.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }


    @Operation(
            summary = "Save Contact Information",
            description =
                    "Saves investor contact and residential address information submitted through the client application.\n\n" +
                            "This endpoint captures and stores detailed address information for the investor, which is required for regulatory compliance and communication purposes.\n\n" +
                            "**Supported Platforms:** Web and Mobile\n\n" +
                            "### Fields Collected:\n" +
                            "- **address1, address2, address3:** Full residential address (e.g., 123, Palm Street, Near Marina Bay, Apartment 45B)\n" +
                            "- **city:** Name of the city (e.g., Dubai)\n" +
                            "- **state and stateCode:** State name and corresponding code (e.g., Dubai, DU)\n" +
                            "- **pincode:** Postal or ZIP code (e.g., 00000)\n" +
                            "- **country:** Country of residence (e.g., United Arab Emirates)\n\n" +
                            "A successful response confirms that the contact information has been saved.\n" +
                            "If any validation fails or required fields are missing, an appropriate error message is returned.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Contact Information object containing residential address details",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContactInfoDTO.class)
                    )
            )
    )
    @ApiResponses(value =
            {
                    @ApiResponse(responseCode = "200", description = "Success Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SuccessResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Failure Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            })
//    @PostMapping("/saveContactInfo")
//    public ResponseEntity<?> saveContactInfo(@RequestBody ContactInfoDTO dto, @RequestHeader("Authorization") String token)
//    {
//        try
//        {
//            // 1. Null check
//            if (dto == null)
//            {
//                return UserUtils.errorResponse("NRI details cannot be empty", HttpStatus.BAD_REQUEST);
//            }
//
//            // 2. Field-level validation
//            String error = UserValidate.validateContactInfo(dto);
//
//            if (error != null)
//            {
//                return UserUtils.errorResponse(error, HttpStatus.BAD_REQUEST);
//            }
//
//            String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
//            Integer userId = Integer.parseInt(userIdFromToken);
//
//            // 3. Fetch user
//            Optional<User> userOpt = userRepository.findUSerByIdAndActive(userId);
//
//            if (!userOpt.isPresent())
//            {
//                return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
//            }
//
//            User user = userOpt.get();
//            UserBseNseDetails userDetails = null;
//
//            MymfboxOnboarding onboarding = onboardingService.getOrCreateOnboarding(user.getId(), user.getClient_name());
//
//            if (onboarding == null) {
//                return UserUtils.errorResponse("Could not create onboarding record", HttpStatus.INTERNAL_SERVER_ERROR);
//            }
//
//            if(user.getNse_active().equals(1))
//            {
//                Optional<UserBseNseDetails> userDetailsOpt = userBseNseDetailsService.getUserBseNseDetailsByUserIdAndClientName(user.getId(), user.getClient_name());
//
//                if(userDetailsOpt.isEmpty())
//                {
//                    return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
//                }else
//                {
//                    userDetails = userDetailsOpt.get();
//                }
//            }
//
//            // 4. Map and save
//            if(userDetails != null)
//            {
//                userDetails = ContactInfoMapper.dtoToUserBseNseDetails(dto, userDetails);
//                userBseNseDetailsService.saveOrUpdateUserBseNseDetails(userDetails);
//            }else
//            {
//                user = ContactInfoMapper.dtoToUser(dto, user);
//                userService.saveOrUpdateUser(user);
//            }
//            onboarding.setContact_info(true);
//            onboardingService.saveOnboarding(onboarding);
//            // 5. Success
//            return UserUtils.successResponse("Contact information saved successfully.", HttpStatus.OK);
//        }catch(Exception ex)
//        {
//            logger.error("Error while saving personal information", ex);
//            return UserUtils.errorResponse("Something went wrong. We have taken note of the issue. Rest assured it will be fixed ASAP.", HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
    @PostMapping("/saveContactInfo")
    public ResponseEntity<?> saveContactInfo(@RequestBody ContactInfoDTO dto, @RequestHeader("Authorization") String token,@RequestParam(required = false) String is_MultiReg)
    {
        try
        {
            if (dto == null)
            {
                return UserUtils.errorResponse("NRI details cannot be empty", HttpStatus.BAD_REQUEST);
            }

            String error = UserValidate.validateContactInfo(dto);

            if (error != null)
            {
                return UserUtils.errorResponse(error, HttpStatus.BAD_REQUEST);
            }

            String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            Integer userId = Integer.parseInt(userIdFromToken);
            is_MultiReg = UserUtils.checkParem(is_MultiReg);
            Optional<UsersOnlineRegDetails> userOpt = userOnlineRegDetailsRespository.findUSerByIdAndActive(userId);

            if (userOpt.isEmpty())
            {
                return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
            }

            UsersOnlineRegDetails user = userOpt.get();
            UsersOnlineRegDetails userDetails = null;

            Boolean isMultiReg = false;

            if(is_MultiReg.equalsIgnoreCase("1"))
            {
                isMultiReg = true;
            }
            System.out.println("isMultiReg = " + isMultiReg);

            MymfboxOnboarding onboarding = null;

            if(!is_MultiReg.isEmpty())
            {
                onboarding = onboardingService.getOrCreateOnboardingbyMultireg(user.getId(), user.getClient_name(), isMultiReg);
            }else
            {
                onboarding = onboardingService.getOrCreateOnboarding(user.getId(), user.getClient_name());
            }

            if (onboarding == null)
            {
                return UserUtils.errorResponse("Could not create onboarding record", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            if(user.getNse_customer().equals(1) && user.getNse_active().equals(1) && StringHelper.isNotEmpty(user.getNse_iin_number())) {
                List<UsersOnlineRegDetails> userDetailsOpt = userOnlineRegDetailsRespository.getNseInactiveUserRegDetailsByUserIdAndClientName(onboarding.getUser_id(), user.getClient_name());

//                if (userDetailsOpt.isEmpty()) {
//                    return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
//                }
                if (userDetailsOpt != null && !userDetailsOpt.isEmpty()) {
                    userDetails = userDetailsOpt.get(0);
                }

                if (userDetails != null) {
                    userDetails = ContactInfoMapper.dtoToUserBseNseDetails(dto, userDetails);
                    userBseNseDetailsService.saveOrUpdateUserOnlineReg(userDetails);
                }
                else{
                    user = ContactInfoMapper.dtoToUser(dto, user);
                    userService.saveOrUpdateUser(user);
                }
            }else{
                user = ContactInfoMapper.dtoToUser(dto, user);
                userService.saveOrUpdateUser(user);
            }
            onboarding.setContact_info(true);
            onboardingService.saveOnboarding(onboarding);

            return UserUtils.successResponse("Contact information saved successfully.", HttpStatus.OK);
        }catch(Exception ex)
        {
            logger.error("Error while saving personal information", ex);
            return UserUtils.errorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Operation(
            summary = "Save Nominee Information",
            description =
                    "Saves nominee details submitted by the investor through the client application.\n\n" +
                            "This endpoint collects nominee-related information for the investor as part of the onboarding and regulatory KYC process. It includes nominee identity, address, relationship to the investor, and guardian details if the nominee is a minor.\n\n" +
                            "**Supported Platforms:** Web and Mobile\n\n" +
                            "### Fields Collected:\n" +
                            "- **id:** Nominee entry identifier (e.g., 1)\n" +
                            "- **type and typeDesc:** Nominee type code and description (e.g., 01, Primary)\n" +
                            "- **name, dob:** Full name and date of birth of the nominee (e.g., Alice Doe, 2005-05-10)\n" +
                            "- **address1, address2, address3, city, state, stateCode, pincode, country:** Complete address of the nominee\n" +
                            "- **idType and idNo:** Type and number of identification document (e.g., PAN, ABCDE1234F)\n" +
                            "- **email, mobile:** Contact details of the nominee\n" +
                            "- **relation:** Relationship with the investor (e.g., Daughter)\n" +
                            "- **guardName, guardPan, guardRelation:** Guardian details (required if nominee is a minor)\n" +
                            "- **percentage:** Share of investment allocated to this nominee (e.g., 100 for full allocation)\n\n" +
                            "A successful response indicates that the nominee details have been saved.\n" +
                            "If validation fails or mandatory fields are missing, an appropriate error response is returned.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Nominee Information object containing all required nominee details",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NomineeInfoDTO.class)
                    )
            )
    )
    @ApiResponses(value =
            {
                    @ApiResponse(responseCode = "200", description = "Success Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SuccessResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Failure Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            })

//    @PostMapping("/saveNomineeInfo")
//    public ResponseEntity<?> saveNomineeInfo(@RequestBody List<NomineeInfoDTO> dtoList, @RequestHeader("Authorization") String token)
//    {
//        try
//        {
//            if (dtoList == null)
//            {
//                return UserUtils.errorResponse("Nominee details cannot be empty", HttpStatus.BAD_REQUEST);
//            }
//
//            String error = UserValidate.validateNomineeInfo(dtoList);
//
//            if (error != null)
//            {
//                return UserUtils.errorResponse(error, HttpStatus.BAD_REQUEST);
//            }
//
//            String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
//            int userId = Integer.parseInt(userIdFromToken);
//
//            Optional<User> userOpt = userRepository.findUSerByIdAndActive(userId);
//
//            if (userOpt.isEmpty())
//            {
//                return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
//            }
//
//            User user = userOpt.get();
//            UserBseNseDetails userDetails = null;
//
//            MymfboxOnboarding onboarding = onboardingService.getOrCreateOnboarding(user.getId(), user.getClient_name());
//
//            if (onboarding == null)
//            {
//                return UserUtils.errorResponse("Could not create onboarding record", HttpStatus.INTERNAL_SERVER_ERROR);
//            }
//
//            if(user.getNse_active().equals(1))
//            {
//                Optional<UserBseNseDetails> userDetailsOpt = userBseNseDetailsService.getUserBseNseDetailsByUserIdAndClientName(user.getId(), user.getClient_name());
//
//                if(userDetailsOpt.isEmpty())
//                {
//                    return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
//                }else
//                {
//                    userDetails = userDetailsOpt.get();
//                }
//            }
//
//            // 4. Map and save
//            if(userDetails != null)
//            {
//                userDetails = NomineeInfoMapper.dtoToUserBseNseDetails(dtoList, userDetails);
//                userBseNseDetailsService.saveOrUpdateUserBseNseDetails(userDetails);
//            }else
//            {
//                user = NomineeInfoMapper.dtoToUser(dtoList, user);
//                userService.saveOrUpdateUser(user);
//            }
//            onboarding.setNomiee_info(true);
//            onboardingService.saveOnboarding(onboarding);
//            // 5. Success
//            return UserUtils.successResponse("Nominee information saved successfully.", HttpStatus.OK);
//        }catch(Exception ex)
//        {
//            logger.error("Error while saving personal information", ex);
//            return UserUtils.errorResponse("Something went wrong. We have taken note of the issue. Rest assured it will be fixed ASAP.", HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }

    @PostMapping("/saveNomineeInfo")
    public ResponseEntity<?> saveNomineeInfo(@RequestBody List<NomineeInfoDTO> dtoList, @RequestHeader("Authorization") String token,@RequestParam(required = false) String is_MultiReg)
    {
        try
        {
            if (dtoList == null)
            {
                return UserUtils.errorResponse("Nominee details cannot be empty", HttpStatus.BAD_REQUEST);
            }

            String error = UserValidate.validateNomineeInfo(dtoList);

            if (error != null)
            {
                return UserUtils.errorResponse(error, HttpStatus.BAD_REQUEST);
            }

            String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            int userId = Integer.parseInt(userIdFromToken);
            is_MultiReg = UserUtils.checkParem(is_MultiReg);
            Optional<UsersOnlineRegDetails> userOpt = userOnlineRegDetailsRespository.findUSerByIdAndActive(userId);

            if (userOpt.isEmpty())
            {
                return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
            }

            UsersOnlineRegDetails user = userOpt.get();
            UsersNomineeDetails nomineeDetails = null;

            Boolean isMultiReg = false;
            if(is_MultiReg.equalsIgnoreCase("1"))
            {
                isMultiReg = true;
            }
            System.out.println("isMultiReg = " + isMultiReg);

            MymfboxOnboarding onboarding = null;

            if(!is_MultiReg.isEmpty())
            {
                onboarding = onboardingService.getOrCreateOnboardingbyMultireg(user.getId(), user.getClient_name(), isMultiReg);
            }else
            {
                onboarding = onboardingService.getOrCreateOnboarding(user.getId(), user.getClient_name());
            }

            if (onboarding == null)
            {
                return UserUtils.errorResponse("Could not create onboarding record", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            if(user.getNse_customer().equals(1) && user.getNse_active().equals(1) && StringHelper.isNotEmpty(user.getNse_iin_number()))
            {

                UsersNomineeDetails userDetailsOpts = usersNomineeDetailsRepository.findByUserIdAndClientName(onboarding.getUser_id(), user.getClient_name());

                if (userDetailsOpts != null) {
                    nomineeDetails = userDetailsOpts;
                }

                UsersNomineeDetails nomineeInfo = NomineeInfoMapper.dtoToUserBseNseDetails(dtoList, nomineeDetails);
                nomineeInfo.setUser_id(nomineeDetails.getUser_id());
                nomineeInfo.setOnline_code(nomineeDetails.getOnline_code());
                nomineeInfo.setBroker_code(nomineeDetails.getBroker_code());
                nomineeInfo.setNumber_of_nominee(String.valueOf(dtoList.size()));
                nomineeInfo.setCreated_date(new Date());
                nomineeInfo.setClient_name(nomineeDetails.getClient_name());

                usersNomineeDetailsRepository.save(nomineeInfo);
            }
            onboarding.setNomiee_info(true);
            onboardingService.saveOnboarding(onboarding);

            return UserUtils.successResponse("Nominee information saved successfully.", HttpStatus.OK);
        }catch(Exception ex)
        {
            logger.error("Error while saving personal information", ex);
            return UserUtils.errorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Save Joint Holder Information",
            description =
                    "Saves joint holder information submitted by the investor through the client application.\n\n" +
                            "This endpoint captures personal and KYC-related details of joint holders associated with an investor's account. It ensures regulatory compliance for accounts with multiple holders.\n\n" +
                            "**Supported Platforms:** Web and Mobile\n\n" +
                            "### Fields Collected:\n" +
                            "- **id:** Unique identifier for the joint holder (e.g., 1)\n" +
                            "- **name:** Full name of the joint holder (e.g., Jane Doe)\n" +
                            "- **pan:** PAN number of the joint holder (e.g., ABCDE1234F)\n" +
                            "- **dob:** Date of birth in YYYY-MM-DD format (e.g., 1985-06-15)\n" +
                            "- **email, emailRelation:** Email address and its relationship code (e.g., 01 = Self)\n" +
                            "- **mobile, mobileRelation:** Mobile number and its relationship code (e.g., 01 = Self)\n" +
                            "- **placeBirth:** Place of birth (e.g., Mumbai)\n" +
                            "- **countryBirth:** Country of birth (e.g., India)\n" +
                            "- **occupation:** Profession of the joint holder (e.g., Software Engineer)\n" +
                            "- **income:** Declared income range (e.g., 5–10 LPA)\n" +
                            "- **sourceWealth:** Source of wealth (e.g., Employment)\n" +
                            "- **addressType:** Type of address (e.g., Residential, Office)\n" +
                            "- **political:** Political exposure status (e.g., Not Politically Exposed)\n\n" +
                            "A successful response indicates the joint holder’s details have been saved.\n" +
                            "In case of missing or invalid data, an appropriate error response will be returned.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Joint Holder Information object containing relevant KYC details",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = JointHolderInfoDTO.class)
                    )
            )
    )
    @ApiResponses(value =
            {
                    @ApiResponse(responseCode = "200", description = "Success Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SuccessResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Failure Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            })
//    @PostMapping("/saveJointHolderInfo")
//    public ResponseEntity<?> saveJointHolderInfo(@RequestBody List<JointHolderInfoDTO> dtoList, @RequestHeader("Authorization") String token)
//    {
//        try
//        {
//            // 1. Null check
//            if (dtoList == null)
//            {
//                return UserUtils.errorResponse("Nominee details cannot be empty", HttpStatus.BAD_REQUEST);
//            }
//
//            // 2. Field-level validation
//            String error = UserValidate.validateJointHolderInfo(dtoList);
//
//            if (error != null)
//            {
//                return UserUtils.errorResponse(error, HttpStatus.BAD_REQUEST);
//            }
//            String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
//            Integer userId = Integer.parseInt(userIdFromToken);
//
//            // 3. Fetch user
//            Optional<User> userOpt = userRepository.findUSerByIdAndActive(userId);
//
//            if (!userOpt.isPresent())
//            {
//                return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
//            }
//
//            User user = userOpt.get();
//            UserBseNseDetails userDetails = null;
//
//            MymfboxOnboarding onboarding = onboardingService.getOrCreateOnboarding(user.getId(), user.getClient_name());
//
//            if (onboarding == null)
//            {
//                return UserUtils.errorResponse("Could not create onboarding record", HttpStatus.INTERNAL_SERVER_ERROR);
//            }
//
//            if(user.getNse_active().equals(1))
//            {
//                Optional<UserBseNseDetails> userDetailsOpt = userBseNseDetailsService.getUserBseNseDetailsByUserIdAndClientName(user.getId(), user.getClient_name());
//
//                if(userDetailsOpt.isEmpty())
//                {
//                    return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
//                }else
//                {
//                    userDetails = userDetailsOpt.get();
//                }
//            }
//
//            // 4. Map and save
//            if(userDetails != null)
//            {
//                userDetails = JoinHolderInfoMapper.dtoToUserBseNseDetails(dtoList, userDetails);
//                userBseNseDetailsService.saveOrUpdateUserBseNseDetails(userDetails);
//            }else
//            {
//                user = JoinHolderInfoMapper.dtoToUser(dtoList, user);
//                userService.saveOrUpdateUser(user);
//            }
//
//            onboarding.setJoint_holder_info(true);
//            onboardingService.saveOnboarding(onboarding);
//
//            // 5. Success
//            return UserUtils.successResponse("Joint Holder information saved successfully.", HttpStatus.OK);
//        }catch(Exception ex)
//        {
//            logger.error("Error while saving personal information", ex);
//            return UserUtils.errorResponse(
//                    "Something went wrong. We have taken note of the issue. Rest assured it will be fixed ASAP.",
//                    HttpStatus.INTERNAL_SERVER_ERROR
//            );
//        }
//    }
    @PostMapping("/saveJointHolderInfo")
    public ResponseEntity<?> saveJointHolderInfo(@RequestBody List<JointHolderInfoDTO> dtoList, @RequestHeader("Authorization") String token,@RequestParam(required = false) String is_MultiReg)
    {
        try
        {
            if (dtoList == null)
            {
                return UserUtils.errorResponse("Joint holder details cannot be empty", HttpStatus.BAD_REQUEST);
            }

            String error = UserValidate.validateJointHolderInfo(dtoList);

            if (error != null)
            {
                return UserUtils.errorResponse(error, HttpStatus.BAD_REQUEST);
            }
            String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            Integer userId = Integer.parseInt(userIdFromToken);
            is_MultiReg = UserUtils.checkParem(is_MultiReg);
            Optional<UsersOnlineRegDetails> userOpt = userService.getUserById(userId);

            if (userOpt.isEmpty())
            {
                return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
            }

            UsersOnlineRegDetails user = userOpt.get();
            UsersOnlineRegDetails userDetails = null;

            Boolean isMultiReg = false;
            if(is_MultiReg.equalsIgnoreCase("1"))
            {
                isMultiReg = true;
            }
            System.out.println("isMultiReg = " + isMultiReg);

            MymfboxOnboarding onboarding = null;

            if(!is_MultiReg.isEmpty()) {
                onboarding = onboardingService.getOrCreateOnboardingbyMultireg(user.getId(), user.getClient_name(), isMultiReg);
            }else{
                onboarding = onboardingService.getOrCreateOnboarding(user.getId(), user.getClient_name());
            }

            if (onboarding == null)
            {
                return UserUtils.errorResponse("Could not create onboarding record", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            if(user.getNse_customer().equals(1) && user.getNse_active().equals(1) && StringHelper.isNotEmpty(user.getNse_iin_number())) {
                List<UsersOnlineRegDetails> userDetailsOpt = userOnlineRegDetailsRespository.getNseInactiveUserRegDetailsByUserIdAndClientName(onboarding.getUser_id(), user.getClient_name());

                if (userDetailsOpt != null && !userDetailsOpt.isEmpty()) {
                    userDetails = userDetailsOpt.get(0);
                }

                if (userDetails != null) {
                    userDetails = JoinHolderInfoMapper.dtoToUserBseNseDetails(dtoList, userDetails);
                    userBseNseDetailsService.saveOrUpdateUserOnlineReg(userDetails);
                }
                else{
                    user = JoinHolderInfoMapper.dtoToUser(dtoList, user);
                    userService.saveOrUpdateUser(user);
                }
            }else{
                user = JoinHolderInfoMapper.dtoToUser(dtoList, user);
                userService.saveOrUpdateUser(user);
            }
            onboarding.setJoint_holder_info(true);
            onboardingService.saveOnboarding(onboarding);

            return UserUtils.successResponse("Joint Holder information saved successfully.", HttpStatus.OK);
        }catch(Exception ex)
        {
            logger.error("Error while saving personal information", ex);
            return UserUtils.errorResponse(
                    "Something went wrong. We have taken note of the issue. Rest assured it will be fixed ASAP.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }


    @Operation(
            summary = "Save Bank Information",
            description =
                    "Saves bank account details submitted by the investor through the client application.\n\n" +
                            "This endpoint captures bank-related information necessary for investment transactions, redemptions, and compliance with KYC norms. It stores data such as account number, IFSC, branch details, and account type.\n\n" +
                            "**Supported Platforms:** Web and Mobile\n\n" +
                            "### Fields Collected:\n" +
                            "- **ifscCode:** IFSC code of the bank branch (e.g., HDFC0001234)\n" +
                            "- **micrCode:** MICR code of the bank (e.g., 110240123)\n" +
                            "- **bankCode:** Internal bank code used by the system (e.g., HDFC)\n" +
                            "- **bankName:** Name of the bank (e.g., HDFC Bank)\n" +
                            "- **bankAddress:** Full address of the bank branch (e.g., HDFC Towers, MG Road, Bengaluru)\n" +
                            "- **branchName:** Name of the bank branch (e.g., MG Road Branch)\n" +
                            "- **accountNumber:** Investor’s bank account number (e.g., 123456789012)\n" +
                            "- **accountHolderName:** Name of the account holder as per bank records (e.g., John Doe)\n" +
                            "- **accountType:** Type of account (e.g., Savings, Current)\n" +
                            "- **accountDesc:** Description of the account type (e.g., Savings Bank Account)\n\n" +
                            "A successful response confirms that the bank details have been saved.\n" +
                            "If any validation fails or mandatory fields are missing, an appropriate error message will be returned.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Bank Information object containing account and branch details",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = BankInfoDTO.class)
                    )
            )
    )
    @ApiResponses(value =
            {
                    @ApiResponse(responseCode = "200", description = "Success Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SuccessResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Failure Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            })
    @PostMapping("/saveBankInfo")
    public ResponseEntity<?> saveBankInfo(@RequestBody BankInfoDTO dto, @RequestHeader("Authorization") String token,@RequestParam(required = false) String is_MultiReg)
    {
        try
        {
            if (dto == null)
            {
                return UserUtils.errorResponse("Bank details cannot be empty", HttpStatus.BAD_REQUEST);
            }

            String error = UserValidate.validateBankInfo(dto);

            if (error != null)
            {
                return UserUtils.errorResponse(error, HttpStatus.BAD_REQUEST);
            }

            String userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            is_MultiReg = UserUtils.checkParem(is_MultiReg);
            Optional<UsersOnlineRegDetails> userOpt = userService.getUserById(Integer.parseInt(userid));

            if (userOpt.isEmpty())
            {
                return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
            }


            UsersOnlineRegDetails user = userOpt.get();

            UsersOnlineRegDetails userDetails = null;

            Boolean isMultiReg = false;
            if(is_MultiReg.equalsIgnoreCase("1"))
            {
                isMultiReg = true;
            }
            System.out.println("isMultiReg = " + isMultiReg);

            MymfboxOnboarding onboarding = null;

            if(!is_MultiReg.isEmpty()) {
                onboarding = onboardingService.getOrCreateOnboardingbyMultireg(user.getId(), user.getClient_name(), isMultiReg);
            }else{
                onboarding = onboardingService.getOrCreateOnboarding(user.getId(), user.getClient_name());
            }

            if (onboarding == null)
            {
                return UserUtils.errorResponse("Could not create onboarding record", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            List<UsersOnlineRegDetails> userDetailsOpt = userOnlineRegDetailsRespository.findByUseridAndClientName(onboarding.getUser_id(), user.getClient_name());

            if(userDetailsOpt.isEmpty())
            {
                return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
            }

            userDetails = userDetailsOpt.get(0);

            List<UsersBankDetails> usersBankDetailsOpt = usersBankDetailsRepository.findByUserIdAndClientName(onboarding.getUser_id(), user.getClient_name());

            UsersBankDetails userBankDetails = null;
            if(!usersBankDetailsOpt.isEmpty())
            {
                userBankDetails = usersBankDetailsOpt.stream() .filter(bank -> bank.getBank_account_number().equals(dto.getAccountNumber())).findFirst() .orElse(null);
            }

            UsersBankDetails bankInfo = BankInfoMapper.dtoToUserBseNseDetails(dto, userBankDetails);

            bankInfo.setUser_id(userDetails.getUser_id());
            bankInfo.setOnline_flag("NSE");
            bankInfo.setOnline_code(userDetails.getNse_iin_number());
            bankInfo.setOnline_id(userDetails.getId());
            bankInfo.setBroker_code(userDetails.getBroker_code());
            bankInfo.setClient_name(userDetails.getClient_name());
            bankInfo.setCreated_date(new Date());

            usersBankDetailsRepository.save(bankInfo);

            onboarding.setBank_info(true);
            onboarding.setIs_all_steps_completed(true);
            onboardingService.saveOnboarding(onboarding);

            return UserUtils.successResponse("Bank information saved successfully.", HttpStatus.OK);
        }catch(Exception ex)
        {
            logger.error("Error while saving personal information", ex);
            return UserUtils.errorResponse(
                    "Something went wrong. We have taken note of the issue. Rest assured it will be fixed ASAP.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @Operation
    (
            summary = "Get All User Information",
            description =
                    "Retrieves the complete set of user-related information including investor details, personal info, NRI info, contact info, nominee details, joint holder info, and bank account information.\n\n" +
                            "**Supported Platforms:** Web and Mobile\n\n" +
                            "### Information Retrieved:\n" +
                            "- **Investor Information :** PAN number, tax status, holding nature, etc.\n" +
                            "- **Personal Information :** Name, gender, marital status, date of birth, etc.\n" +
                            "- **NRI Information :** Country of birth, citizenship, tax residency, etc.\n" +
                            "- **Contact Information :** Address, email, mobile number, communication preference, etc.\n" +
                            "- **Nominee Information :** List of nominees with their name, share percentage, relationship, etc.\n" +
                            "- **Joint Holder Information :** List of joint account holders with name, PAN, relationship, etc.\n" +
                            "- **Bank Information :** IFSC code, bank name, branch, account number, account type, etc.\n\n" +
                            "If the user is registered via NSE and has NSE details, those are prioritized. Otherwise, basic user profile data is returned.\n\n" +
                            "Returns a comprehensive JSON object containing all these sections on success.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Get All User Information", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = User.class)))
    )
    @ApiResponses(value =
    {
            @ApiResponse(responseCode = "200", description = "Success Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SuccessResponse.class))),
            @ApiResponse(responseCode = "400", description = "Failure Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping("/getUserInfo")
    public ResponseEntity<?> getUserInfo(@RequestHeader("Authorization") String token)
    {
        try
        {
            String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            Integer userId = Integer.parseInt(userIdFromToken);

            Optional<User> userOpt = userRepository.findUSerByIdAndActive(userId);

            if (!userOpt.isPresent())
            {
                return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
            }

            UsersOnlineRegDetails userDetails = null;

            // 3. Fetch user
            List<UsersOnlineRegDetails> userDetailsOpt = userOnlineRegDetailsRespository.findByUseridAndClientName(userId, userOpt.get().getClient_name());
            UsersOnlineRegDetails userDetail = userDetailsOpt.get(0);
            List<UsersBankDetails> userBank = usersBankDetailsRepository.findByUseridAndClientName(userId, userOpt.get().getClient_name(), String.valueOf(userDetail.getId()));
            Optional<UsersNomineeDetails> userNominee1 = usersNomineeDetailsRepository.findByUseridAndClientName(userId, userOpt.get().getClient_name(), String.valueOf(userDetail.getId()),"NSE");
            UsersNomineeDetails userNominee = null;

            if(userNominee1.isPresent())
            {
                userNominee = userNominee1.get();
            }

            if(userDetailsOpt == null)
            {
                return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
            }else
            {
                userDetails = userDetail;
            }

            InvestorInfoDTO invest_info = null;
            PersonalInfoDTO personal_info = null;
            NriInfoDTO nri_info = null;
            ContactInfoDTO contact_info = null;
            List<NomineeInfoDTO> nominee_info = null;
            List<JointHolderInfoDTO> joint_holder_info = null;
            List<BankInfoDTO> bank_info = null;

            invest_info = InvestorInfoMapper.mapBseNseDetailsToDto(userDetails);

            personal_info = PersonalInfoMapper.userBseNseDetailsToDto(userDetails);

            nri_info = NriInfoMapper.userBseNseDetailsToDto(userDetails);

            contact_info = ContactInfoMapper.userBseNseDetailsToDto(userDetails);

            nominee_info = NomineeInfoMapper.userBseNseDetailsToDto(userNominee);

            joint_holder_info = JoinHolderInfoMapper.userBseNseDetailsToDto(userDetails);

            bank_info = BankInfoMapper.userBseNseDetailsToDto(userBank);
            // 5. Succe
            return UserUtils.userSuccessResponse("Investor Information.", HttpStatus.OK, invest_info, personal_info, nri_info, nominee_info, joint_holder_info,contact_info,bank_info);
        }catch(Exception ex)
        {
            logger.error("Error while saving personal information", ex);
            return UserUtils.errorResponse("Something went wrong. We have taken note of the issue. Rest assured it will be fixed ASAP.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get Investor Info",
            description = "Fetches the Investor Details of the logged-in user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved Investor information",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InvestorInfoDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping("/getInvestorInfo")
    public ResponseEntity<?> getInvestorInfo(@RequestHeader("Authorization") String token,@RequestParam(required = false) String is_MultiReg)
    {
        try
        {
            String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            Integer userId = Integer.parseInt(userIdFromToken);
            is_MultiReg = UserUtils.checkParem(is_MultiReg);

            Optional<User> userOpt = userRepository.findUSerByIdAndActive(userId);

            if (!userOpt.isPresent())
            {
                return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
            }

            User user = userOpt.get();
            UsersOnlineRegDetails userDetails = null;

            Boolean isMultiReg = false;
            if(is_MultiReg.equalsIgnoreCase("1"))
            {
                isMultiReg = true;
            }
            System.out.println("isMultiReg = " + isMultiReg);

            MymfboxOnboarding onboarding = null;

            if(!is_MultiReg.isEmpty()) {
                onboarding = onboardingService.getOrCreateOnboardingbyMultireg(user.getId(), user.getClient_name(), isMultiReg);
            }else{
                onboarding = onboardingService.getOrCreateOnboarding(user.getId(), user.getClient_name());
            }

            if (onboarding == null)
            {
                return UserUtils.errorResponse("Onboarding details not found.", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            System.out.println("Onboarding UserId = " + onboarding.getUser_id());
            System.out.println("Onboarding OnlineId = " + onboarding.getUser_id());
            System.out.println("Onboarding clientName = " + onboarding.getClient_name());
            InvestorInfoDTO investor_info = null;

            List<UsersOnlineRegDetails> userDetailsOpt = userOnlineRegDetailsRespository.getNseInactiveUserRegDetailsByUserIdAndClientName(onboarding.getUser_id(), onboarding.getClient_name());

            if (!userDetailsOpt.isEmpty()) {
                userDetails = userDetailsOpt.get(0);
            } else {
                userDetails = new UsersOnlineRegDetails();
                userDetails.setPan(user.getPan());
                userDetails.setBroker_code(user.getBroker_code());
            }

            System.out.println("user = " + user);
            if (userDetails != null)
            {
                investor_info = InvestorInfoMapper.mapBseNseDetailsToDto(userDetails);
            }
            return ResponseEntity.ok(investor_info);
        }catch(Exception ex)
        {
            logger.error("Error while saving personal information", ex);
            return UserUtils.errorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get Personal Info",
            description = "Fetches the Personal Details of the logged-in user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved Personal information",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PersonalInfoDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping("/getPersonalInfo")
    public ResponseEntity<?> getPersonalInfo(@RequestHeader("Authorization") String token,@RequestParam(required = false) String reg_id,@RequestParam(required = false) String is_MultiReg)
    {
        try
        {
            String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            Integer userId = Integer.parseInt(userIdFromToken);
            is_MultiReg = UserUtils.checkParem(is_MultiReg);

            Optional<User> userOpt = userRepository.findUSerByIdAndActive(userId);

            if (!userOpt.isPresent())
            {
                return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
            }

            User user = userOpt.get();
            UsersOnlineRegDetails userDetails = null;
            PersonalInfoDTO personal_info = null;
            Boolean isMultiReg = false;

            if(is_MultiReg.equalsIgnoreCase("1"))
            {
                isMultiReg = true;
            }
            System.out.println("isMultiReg = " + isMultiReg);

            MymfboxOnboarding onboarding = null;

            if(!is_MultiReg.isEmpty())
            {
                onboarding = onboardingService.getOrCreateOnboardingbyMultireg(user.getId(), user.getClient_name(), isMultiReg);
            }else
            {
                onboarding = onboardingService.getOrCreateOnboarding(user.getId(), user.getClient_name());
            }

            if (onboarding == null)
            {
                return UserUtils.errorResponse("Onboarding details not found.", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            System.out.println("onboarding = " + onboarding);

            List<UsersOnlineRegDetails> userDetailsOpt = userOnlineRegDetailsRespository.getNseInactiveUserRegDetailsByUserIdAndClientName(onboarding.getUser_id(), onboarding.getClient_name());

            if(!userDetailsOpt.isEmpty())
            {
                userDetails = userDetailsOpt.get(0);
            }

            personal_info = PersonalInfoMapper.userBseNseDetailsToDto(userDetails);

            if(userDetails != null)
            {
                String dob = personal_info.getDob();

                if(StringHelper.isEmpty(dob))
                {
                    dob = user.getDate_of_birth();

                    if(dob.contains("/"))
                    {
                        dob = dob.replace("/", "-");
                    }

                    if(StringHelper.isNotEmpty(dob))
                    {
                        personal_info.setDob(dob);
                    }
                }
            }

            return ResponseEntity.ok(personal_info);
        }catch(Exception ex)
        {
            logger.error("Error while saving personal information", ex);
            return UserUtils.errorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get Nri Info",
            description = "Fetches the Nri details of the logged-in user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved Nri information",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = NriInfoDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping("/getNriInfo")
    public ResponseEntity<?> getNriInfo(@RequestHeader("Authorization") String token,@RequestParam(required = false) String reg_id,@RequestParam(required = false) String is_MultiReg)
    {
        try
        {
            String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            Integer userId = Integer.parseInt(userIdFromToken);
            is_MultiReg = UserUtils.checkParem(is_MultiReg);

            Optional<User> userOpt = userRepository.findUSerByIdAndActive(userId);

            if (!userOpt.isPresent())
            {
                return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
            }

            User user = userOpt.get();
            UsersOnlineRegDetails userDetails = null;

            Boolean isMultiReg = false;
            if(is_MultiReg.equalsIgnoreCase("1"))
            {
                isMultiReg = true;
            }
            System.out.println("isMultiReg = " + isMultiReg);

            MymfboxOnboarding onboarding = null;

            if(!is_MultiReg.isEmpty()) {
                onboarding = onboardingService.getOrCreateOnboardingbyMultireg(user.getId(), user.getClient_name(), isMultiReg);
            }else{
                onboarding = onboardingService.getOrCreateOnboarding(user.getId(), user.getClient_name());
            }

            if (onboarding == null)
            {
                return UserUtils.errorResponse("Onboarding details not found.", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            NriInfoDTO nri_info = null;

            List<UsersOnlineRegDetails> userDetailsOpt = userOnlineRegDetailsRespository.getNseInactiveUserRegDetailsByUserIdAndClientName(onboarding.getUser_id(), onboarding.getClient_name());
            if(!userDetailsOpt.isEmpty())
            {
                userDetails = userDetailsOpt.get(0);
            }

            if(userDetails != null)
            {
                nri_info = NriInfoMapper.userBseNseDetailsToDto(userDetails);
            }

            return ResponseEntity.ok(nri_info);
        }catch(Exception ex)
        {
            logger.error("Error while saving personal information", ex);
            return UserUtils.errorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get Contact Info",
            description = "Fetches the Contact details of the logged-in user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved Contact information",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ContactInfoDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping("/getContactInfo")
    public ResponseEntity<?> getContactInfo(@RequestHeader("Authorization") String token,@RequestParam(required = false) String reg_id,@RequestParam(required = false) String is_MultiReg)
    {
        try
        {
            String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            Integer userId = Integer.parseInt(userIdFromToken);
            is_MultiReg = UserUtils.checkParem(is_MultiReg);

            Optional<User> userOpt = userRepository.findUSerByIdAndActive(userId);

            if (userOpt.isEmpty())
            {
                return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
            }

            User user = userOpt.get();
            UsersOnlineRegDetails userDetails = null;

            Boolean isMultiReg = false;
            if(is_MultiReg.equalsIgnoreCase("1"))
            {
                isMultiReg = true;
            }
            System.out.println("isMultiReg = " + isMultiReg);

            MymfboxOnboarding onboarding = null;

            if(!is_MultiReg.isEmpty()) {
                onboarding = onboardingService.getOrCreateOnboardingbyMultireg(user.getId(), user.getClient_name(), isMultiReg);
            }else{
                onboarding = onboardingService.getOrCreateOnboarding(user.getId(), user.getClient_name());
            }


            if (onboarding == null)
            {
                return UserUtils.errorResponse("Onboarding details not found.", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            List<UsersOnlineRegDetails> userDetailsOpt = userOnlineRegDetailsRespository.getNseInactiveUserRegDetailsByUserIdAndClientName(onboarding.getUser_id(), onboarding.getClient_name());
            if(!userDetailsOpt.isEmpty())
            {
                userDetails = userDetailsOpt.get(0);
            }

            ContactInfoDTO contact_info = (userDetails != null) ? ContactInfoMapper.userBseNseDetailsToDto(userDetails) : null;

            if (contact_info == null || StringHelper.isEmpty(contact_info.getPincode()))
            {
                contact_info = ContactInfoMapper.userToDto(user);
            }
            return ResponseEntity.ok(contact_info);
        }catch(Exception ex)
        {
            logger.error("Error while saving personal information", ex);
            return UserUtils.errorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Operation(
            summary = "Get Nominee Info",
            description = "Fetches the Nominee details of the logged-in user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved Nominee information",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = NomineeInfoDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping("/getNomineeInfo")
    public ResponseEntity<?> getNomineeInfo(@RequestHeader("Authorization") String token,@RequestParam(required = false) String reg_id,@RequestParam(required = false) String is_MultiReg)
    {
        try
        {
            String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            Integer userId = Integer.parseInt(userIdFromToken);
            is_MultiReg = UserUtils.checkParem(is_MultiReg);
            Optional<User> userOpt = userRepository.findUSerByIdAndActive(userId);

            if (userOpt.isEmpty())
            {
                return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
            }

            User user = userOpt.get();

            Boolean isMultiReg = false;
            if(is_MultiReg.equalsIgnoreCase("1"))
            {
                isMultiReg = true;
            }
            System.out.println("isMultiReg = " + isMultiReg);

            MymfboxOnboarding onboarding = null;

            if(!is_MultiReg.isEmpty()) {
                onboarding = onboardingService.getOrCreateOnboardingbyMultireg(user.getId(), user.getClient_name(), isMultiReg);
            }else{
                onboarding = onboardingService.getOrCreateOnboarding(user.getId(), user.getClient_name());
            }

            if (onboarding == null)
            {
                return UserUtils.errorResponse("Onboarding details not found.", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            List<NomineeInfoDTO> nominee_info = null;

            UsersNomineeDetails userDetailsOpt = usersNomineeDetailsRepository.findByUserIdAndClientName(onboarding.getUser_id(), onboarding.getClient_name());

            if(userDetailsOpt != null)
            {
                nominee_info = NomineeInfoMapper.userBseNseDetailsToDto(userDetailsOpt);
            }

            return ResponseEntity.ok(nominee_info);
        }catch(Exception ex)
        {
            logger.error("Error while saving personal information", ex);
            return UserUtils.errorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @Operation(
            summary = "Get Joint Holder Info",
            description = "Fetches the Joint Holder details of the logged-in user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved Joint Holder information",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = JointHolderInfoDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping("/getJointHolderInfo")
    public ResponseEntity<?> getJointHolderInfo(@RequestHeader("Authorization") String token,@RequestParam(required = false) String reg_id,@RequestParam(required = false) String is_MultiReg)
    {
        try
        {
            String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            Integer userId = Integer.parseInt(userIdFromToken);
            is_MultiReg = UserUtils.checkParem(is_MultiReg);

            Optional<User> userOpt = userRepository.findUSerByIdAndActive(userId);

            if (userOpt.isEmpty())
            {
                return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
            }

            User user = userOpt.get();
            UsersOnlineRegDetails userDetails = null;

            Boolean isMultiReg = false;
            if(is_MultiReg.equalsIgnoreCase("1"))
            {
                isMultiReg = true;
            }
            System.out.println("isMultiReg = " + isMultiReg);

            MymfboxOnboarding onboarding = null;

            if(!is_MultiReg.isEmpty()) {
                onboarding = onboardingService.getOrCreateOnboardingbyMultireg(user.getId(), user.getClient_name(), isMultiReg);
            }else{
                onboarding = onboardingService.getOrCreateOnboarding(user.getId(), user.getClient_name());
            }

            if (onboarding == null)
            {
                return UserUtils.errorResponse("Onboarding details not found.", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            List<JointHolderInfoDTO> joint_info = null;

            List<UsersOnlineRegDetails> userDetailsOpt = userOnlineRegDetailsRespository.getNseInactiveUserRegDetailsByUserIdAndClientName(onboarding.getUser_id(), onboarding.getClient_name());
            if(!userDetailsOpt.isEmpty())
            {
                userDetails = userDetailsOpt.get(0);
            }

            if(userDetails != null)
            {
                joint_info = JoinHolderInfoMapper.userBseNseDetailsToDto(userDetails);
            }

            return ResponseEntity.ok(joint_info);
        }catch(Exception ex)
        {
            logger.error("Error while saving personal information", ex);
            return UserUtils.errorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get Bank Info",
            description = "Fetches the bank account details of the logged-in user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved bank information",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BankInfoDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })


    @GetMapping("/getBankInfo")
    public ResponseEntity<?> getBankInfo(@RequestHeader("Authorization") String token,@RequestParam(required = false) String reg_id,@RequestParam(required = false) String is_MultiReg)
    {
        try
        {
            String userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            Optional<User> userOpt = userRepository.findUSerByIdAndActive(Integer.parseInt(userid));
            is_MultiReg = UserUtils.checkParem(is_MultiReg);

            if (userOpt.isEmpty())
            {
                return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
            }

            User user = userOpt.get();
            UsersOnlineRegDetails userDetails = null;

            Boolean isMultiReg = false;
            if(is_MultiReg.equalsIgnoreCase("1"))
            {
                isMultiReg = true;
            }
            System.out.println("isMultiReg = " + isMultiReg);

            MymfboxOnboarding onboarding = null;

            if(!is_MultiReg.isEmpty()) {
                onboarding = onboardingService.getOrCreateOnboardingbyMultireg(user.getId(), user.getClient_name(), isMultiReg);
            }else{
                onboarding = onboardingService.getOrCreateOnboarding(user.getId(), user.getClient_name());
            }

            if (onboarding == null)
            {
                return UserUtils.errorResponse("Could not create onboarding record", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            List<BankInfoDTO> bank_info = null;
            System.out.println("ONLINE ID = " + onboarding.getUser_id());

            List<UsersBankDetails> usersBankDetailsOpt = usersBankDetailsRepository.findByUserIdAndClientName(onboarding.getUser_id(), onboarding.getClient_name());

            if(usersBankDetailsOpt != null)
            {
                bank_info = BankInfoMapper.userBseNseDetailsToDto(usersBankDetailsOpt);
            }
            
            return ResponseEntity.ok(bank_info);
        }catch(Exception ex)
        {
            logger.error("Error while saving personal information", ex);
            return UserUtils.errorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Check PAN KYC Status",
            description = "Checks the KYC (Know Your Customer) verification status of an investor based on their PAN.\n\n" +
                    "**Purpose:** Ensure PAN is KYC-compliant before allowing any transactions.\n\n" +
                    "**Use Cases:**\n" +
                    "- Validate PAN during user onboarding\n" +
                    "- Prevent further processing if KYC not completed\n" +
                    "- Enrich user profile using KYC data\n\n" +
                    "**Expected Input:**\n" +
                    "- PAN (as query param), token in header\n\n" +
                    "**Expected Output:**\n" +
                    "- KYC status (e.g., VERIFIED, NOT_VERIFIED, FAILED)\n" +
                    "- Metadata like timestamp, reason if failed"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "KYC status fetched successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PanKYCResponse.class),
                            examples = @ExampleObject(value = """
                {
                  "kyc_status": true,
                  "msg": "PAN is KYC compliant.",
                  "kyc_verified_at": "2025-07-22T15:04:05Z"
                }
                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid PAN or verification failed",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PanKYCResponse.class),
                            examples = @ExampleObject(value = """
                {
                  "kyc_status": false,
                  "msg": "Invalid PAN format."
                }
                """)
                    )
            )
    })
    @GetMapping("/checkPanKycStatus")
    public PanKYCResponse checkPanKycStatus(@RequestHeader("Authorization") String token, @RequestParam String pan) {
        try {
            String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            Integer userId = Integer.parseInt(userIdFromToken);

            if (!UserUtils.validatePan(pan)) {
                PanKYCResponse response = new PanKYCResponse();
                response.setMsg("Invalid PAN format.");
                response.setKyc_status(false);
                return response;
            }

            return UserUtils.checkPanKycStatus(pan.toUpperCase());

        } catch (Throwable ex) {
            logger.error("Error while checking PAN KYC status", ex);
            PanKYCResponse response = new PanKYCResponse();
            response.setMsg("Something went wrong. We have taken note of the issue. Rest assured it will be fixed ASAP.");
            response.setKyc_status(false);
            return response;
        }
    }

    @Operation(
            summary = "Get Vendor Platform Info",
            description = """
        Used by the mobile app to determine the investor's platform (BSE, NSE, or MFU) based on the provided Client Name.
        
        This helps in selecting the appropriate registration or transaction platform for the user automatically during onboarding.
        """,
            parameters = {
                    @Parameter(
                            name = "client_name",
                            description = "Client name used to identify the platform (BSE/NSE/MFU)",
                            required = true,
                            in = ParameterIn.QUERY,
                            schema = @Schema(type = "string", example = "reachyourgoals")
                    ),
                    @Parameter(
                            name = "Authorization",
                            description = "Bearer token for user authentication",
                            required = true,
                            in = ParameterIn.HEADER,
                            schema = @Schema(type = "string", example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6...")
                    )
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Platform list fetched successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "PlatformListResponse",
                                    summary = "Successful platform fetch response",
                                    value = """
                {
                  "status": 200,
                  "success": "Success",
                  "message": "Platform list fetched successfully.",
                  "bse_nse_mfu": "NSE",
                  "logo": "https://api.mymfbox.com/images/vendors/nse.png",
                  "tax_status": "",
                  "tax_status_code": "",
                  "title": "NSE NMF Platform",
                  "completed": false,
                  "enabled": false,
                  "checkRequiredOrNot": false
                }
                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Client not mapped to any platform",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "string", example = "NSE platform not linked for this client.")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "string", example = "Internal server error: <details>")
                    )
            )
    })

    @GetMapping("/getVendor")
    public ResponseEntity<?> getVendor(@RequestHeader("Authorization") String token,  @RequestParam String client_name)
    {
        try
        {
            BseNseKey bseNseKey = bseNseKeyRepository.findByClientName(client_name);

            if(bseNseKey != null)
            {
                List<BseNseMfuResponse> bseNseMfuList = new ArrayList<>();
                BseNseMfuResponse response = null;
                String bse_nse_mfu = "NSE";
                String path = "";
                String name = "";
                String title = "";

                if(bse_nse_mfu.contains(","))
                {
                    List<String> bse_nse_mfu_list = new ArrayList<String>(Arrays.asList(bse_nse_mfu.split(",")));
                    if(bse_nse_mfu_list != null && bse_nse_mfu_list.size() > 0)
                    {
                        for (String string : bse_nse_mfu_list)
                        {
                            name = "";
                            path = "";
                            if(string.equalsIgnoreCase("nse"))
                            {
                                name = "NSE";
                                path = UserUtils.getVendorImage("NSE");
                                title = "NSE NMF Platform";
                            }
                            response = new BseNseMfuResponse();
                            response.setBse_nse_mfu(name);
                            response.setLogo(vendorLogoPath + path);
                            response.setTitle(title);
                            bseNseMfuList.add(response);
                        }
                    }
                    else
                    {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body("NSE platform not linked for this client.");
                    }
                }
                else
                {
                    if(bse_nse_mfu.equalsIgnoreCase("nse"))
                    {
                        name = "NSE";
                        path = UserUtils.getVendorImage("NSE");
                        title = "NSE NMF Platform";

                    }
                    response = new BseNseMfuResponse();
                    response.setBse_nse_mfu(name);
                    response.setLogo(vendorLogoPath + path);
                    response.setTitle(title);
                    bseNseMfuList.add(response);
                }

                response.setStatus(200);
                response.setSuccess("Success");
                response.setMessage("Platform list fetched successfully.");
            return ResponseEntity.status(HttpStatus.OK).body(response);

            }
            else {

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("NSE platform not linked for this client.");
            }
        }
        catch (Throwable ex)
        {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Get OnBoarding Status",
            description = "Retrieves the current onboarding status of a user for a specified client.\n"
                    + "Provides step-wise completion data including investor, contact, bank, nominee, and joint holder info.\n"
                    + "Also returns the selected onboarding platform (NSE/BSE/MFU) to help web and mobile apps guide the user through the registration flow."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Success Response",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = OnboardingResponse.class),
                            examples = @ExampleObject(
                                    name = "SuccessExample",
                                    summary = "Successful onboarding status response",
                                    value = """
                        {
                          "status": 200,
                          "success": "Success",
                          "message": "Fetched onboarding details successfully",
                          "user_id": 1234,
                          "client_name": "JohnDoe",
                          "vendor": "NSE",
                          "title": "NSE NMF Platform",
                          "logo": "https://yourcdn.com/images/nse.png",
                          "tax_status": "Individual",
                          "holding_nature": "Single",
                          "investor_info": true,
                          "personal_info": true,
                          "contact_info": true,
                          "nri_info": false,
                          "joint_holder_info": false,
                          "nomiee_info": false,
                          "bank_info": true,
                          "signature_info": true,
                          "has_nominee": true,
                          "has_nri": false,
                          "has_joint_holder": false,
                          "is_all_steps_completed": false,
                          "is_all_registration_completed": false,
                          "menu_list": [
                            {
                              "title": "Investor Information",
                              "completed": true,
                              "enabled": true
                            },
                            {
                              "title": "Personal Info",
                              "completed": true,
                              "enabled": true
                            },
                            {
                              "title": "Contact Info",
                              "completed": true,
                              "enabled": true
                            },
                            {
                              "title": "Nominee Info",
                              "completed": false,
                              "enabled": true
                            },
                            {
                              "title": "Bank Details",
                              "completed": true,
                              "enabled": true
                            }
                          ]
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                        {
                          "status": 400,
                          "error": "Bad Request",
                          "message": "Invalid client name"
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                        {
                          "status": 500,
                          "error": "Internal Server Error",
                          "message": "Internal server error: NullPointerException"
                        }
                        """
                            )
                    )
            )
    })
    @GetMapping("/getOnBoardingStatus")
    public ResponseEntity<?> getOnBoardingStatus(@RequestHeader("Authorization") String token,  @RequestParam String client_name)
    {
        String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
        Integer userId = Integer.parseInt(userIdFromToken);
        OnboardingResponse pojo = new OnboardingResponse();
        List<BseNseMfuResponse> bseNseMfuList = new ArrayList<>();
        BseNseMfuResponse obj = null;
        MymfboxOnboarding onboarding = null;
        try
        {
            Optional<User> userOpt = userRepository.findUSerByIdAndActive(userId);

            if (userOpt.isPresent())
            {
                User user = userOpt.get();
                onboarding = onboardingService.getOrCreateOnboarding(user.getId(), user.getClient_name());

                if(onboarding != null)
                {
                    pojo.setTitle("NSE MF Invest Platform");
                    pojo.setLogo(vendorLogoPath + UserUtils.getVendorImage("NSE"));
                    pojo.setVendor("NSE");
                    pojo.setUser_id(user.getId());
                    pojo.setClient_name(user.getClient_name());
                    pojo.setTax_status(onboarding.getTax_status());
                    pojo.setHolding_nature(onboarding.getHolding_nature());
                    pojo.setInvestor_info(onboarding.getInvestor_info());
                    pojo.setPersonal_info(onboarding.getPersonal_info());
                    pojo.setContact_info(onboarding.getContact_info());
                    pojo.setNri_info(onboarding.getNri_info());
                    pojo.setJoint_holder_info(onboarding.getJoint_holder_info());
                    pojo.setNomiee_info(onboarding.getNomiee_info());
                    pojo.setBank_info(onboarding.getBank_info());
                    pojo.setSignature_info(onboarding.getSignature_info());
                    pojo.setHas_nominee(onboarding.getHas_nominee());
                    pojo.setHas_nri(onboarding.getHas_nri());
                    pojo.setHas_joint_holder(onboarding.getHas_joint_holder());
                    pojo.setIs_all_steps_completed(onboarding.getIs_all_steps_completed());
                    pojo.setIs_all_registration_completed(onboarding.getIs_registration_completed());

                    if(onboarding.getInvestor_info())
                    {
                        obj = new BseNseMfuResponse();
                        obj.setTitle("Investor Information");
                        obj.setCompleted(true);
                        obj.setEnabled(true);
                        bseNseMfuList.add(obj);
                    }else
                    {
                        obj = new BseNseMfuResponse();
                        obj.setTitle("Investor Information");
                        obj.setCompleted(false);
                        obj.setEnabled(true);
                        bseNseMfuList.add(obj);
                    }

                    if(onboarding.getPersonal_info())
                    {
                        obj = new BseNseMfuResponse();
                        obj.setTitle("Personal Info");
                        obj.setCompleted(true);
                        obj.setEnabled(true);
                        bseNseMfuList.add(obj);
                    }else
                    {
                        if(onboarding.getInvestor_info())
                        {
                            obj = new BseNseMfuResponse();
                            obj.setTitle("Personal Info");
                            obj.setCompleted(false);
                            obj.setEnabled(true);
                            bseNseMfuList.add(obj);
                        }else
                        {
                            obj = new BseNseMfuResponse();
                            obj.setTitle("Personal Info");
                            obj.setCompleted(false);
                            obj.setEnabled(false);
                            bseNseMfuList.add(obj);
                        }
                    }

                    if(onboarding.getContact_info())
                    {
                        obj = new BseNseMfuResponse();
                        obj.setTitle("Contact Info");
                        obj.setCompleted(true);
                        obj.setEnabled(true);
                        bseNseMfuList.add(obj);
                    }
                    else
                    {
                        if(onboarding.getPersonal_info())
                        {
                            obj = new BseNseMfuResponse();
                            obj.setTitle("Contact Info");
                            obj.setCompleted(false);
                            obj.setEnabled(true);
                            bseNseMfuList.add(obj);
                        }
                        else
                        {
                            obj = new BseNseMfuResponse();
                            obj.setTitle("Contact Info");
                            obj.setCompleted(false);
                            obj.setEnabled(false);
                            bseNseMfuList.add(obj);
                        }
                    }

                    if(onboarding.getHas_nominee())
                    {
                        if(onboarding.getNomiee_info())
                        {
                            obj = new BseNseMfuResponse();
                            obj.setTitle("Nominee Info");
                            obj.setCompleted(true);
                            obj.setEnabled(true);
                            bseNseMfuList.add(obj);
                        }
                        else
                        {
                            if(onboarding.getContact_info())
                            {
                                obj = new BseNseMfuResponse();
                                obj.setTitle("Nominee Info");
                                obj.setCompleted(false);
                                obj.setEnabled(true);
                                bseNseMfuList.add(obj);
                            }
                            /*else if(onboarding.getPersonal_info())
                            {
                                obj = new BseNseMfuResponse();
                                obj.setTitle("Nominee Info");
                                obj.setCompleted(false);
                                obj.setEnabled(true);
                                bseNseMfuList.add(obj);
                            }*/
                            else
                            {
                                obj = new BseNseMfuResponse();
                                obj.setTitle("Nominee Info");
                                obj.setCompleted(false);
                                obj.setEnabled(false);
                                bseNseMfuList.add(obj);
                            }

                        }
                    }
                    if(onboarding.getHas_nri())
                    {
                        if(onboarding.getNri_info())
                        {
                            obj = new BseNseMfuResponse();
                            obj.setTitle("NRI Info");
                            obj.setCompleted(true);
                            obj.setEnabled(true);
                            bseNseMfuList.add(obj);
                        }
                        else
                        {
                            if(onboarding.getHas_nominee())
                            {
                                if(onboarding.getNomiee_info())
                                {
                                    obj = new BseNseMfuResponse();
                                    obj.setTitle("NRI Info");
                                    obj.setCompleted(false);
                                    obj.setEnabled(true);
                                    bseNseMfuList.add(obj);
                                }
                                else
                                {
                                    obj = new BseNseMfuResponse();
                                    obj.setTitle("NRI Info");
                                    obj.setCompleted(false);
                                    obj.setEnabled(false);
                                    bseNseMfuList.add(obj);
                                }
                            }
                            else if(onboarding.getContact_info())
                            {
                                obj = new BseNseMfuResponse();
                                obj.setTitle("NRI Info");
                                obj.setCompleted(false);
                                obj.setEnabled(true);
                                bseNseMfuList.add(obj);
                            }
                            else
                            {
                                obj = new BseNseMfuResponse();
                                obj.setTitle("NRI Info");
                                obj.setCompleted(false);
                                obj.setEnabled(false);
                                bseNseMfuList.add(obj);
                            }
                        }
                    }

                    if(onboarding.getHas_joint_holder())
                    {
                        if(onboarding.getJoint_holder_info())
                        {
                            obj = new BseNseMfuResponse();
                            obj.setTitle("Joint Holder Info");
                            obj.setCompleted(true);
                            obj.setEnabled(true);
                            bseNseMfuList.add(obj);
                        }
                        else
                        {
                            if(onboarding.getHas_nominee())
                            {
                                if(onboarding.getNomiee_info())
                                {
                                    obj = new BseNseMfuResponse();
                                    obj.setTitle("Joint Holder Info");
                                    obj.setCompleted(false);
                                    obj.setEnabled(true);
                                    bseNseMfuList.add(obj);
                                }
                                else
                                {
                                    obj = new BseNseMfuResponse();
                                    obj.setTitle("Joint Holder Info");
                                    obj.setCompleted(false);
                                    obj.setEnabled(false);
                                    bseNseMfuList.add(obj);
                                }
                            }
                            else if(onboarding.getHas_nri())
                            {
                                if(onboarding.getNri_info())
                                {
                                    obj = new BseNseMfuResponse();
                                    obj.setTitle("Joint Holder Info");
                                    obj.setCompleted(false);
                                    obj.setEnabled(true);
                                    bseNseMfuList.add(obj);
                                }
                                else
                                {
                                    obj = new BseNseMfuResponse();
                                    obj.setTitle("Joint Holder Info");
                                    obj.setCompleted(false);
                                    obj.setEnabled(false);
                                    bseNseMfuList.add(obj);
                                }
                            }
                            else if(onboarding.getContact_info())
                            {
                                obj = new BseNseMfuResponse();
                                obj.setTitle("Joint Holder Info");
                                obj.setCompleted(false);
                                obj.setEnabled(true);
                                bseNseMfuList.add(obj);
                            }
                            else
                            {
                                obj = new BseNseMfuResponse();
                                obj.setTitle("Joint Holder Info");
                                obj.setCompleted(false);
                                obj.setEnabled(false);
                                bseNseMfuList.add(obj);
                            }
                        }
                    }
                    if(onboarding.getBank_info())
                    {
                        obj = new BseNseMfuResponse();
                        obj.setTitle("Bank Details");
                        obj.setCompleted(true);
                        obj.setEnabled(true);
                        bseNseMfuList.add(obj);
                    }
                    else
                    {

                        if(onboarding.getHas_joint_holder())
                        {
                            if(onboarding.getJoint_holder_info())
                            {
                                obj = new BseNseMfuResponse();
                                obj.setTitle("Bank Details");
                                obj.setCompleted(false);
                                obj.setEnabled(true);
                                bseNseMfuList.add(obj);
                            }
                            else
                            {
                                obj = new BseNseMfuResponse();
                                obj.setTitle("Bank Details");
                                obj.setCompleted(false);
                                obj.setEnabled(false);
                                bseNseMfuList.add(obj);
                            }
                        }
                        else if(onboarding.getHas_nri())
                        {
                            if(onboarding.getNri_info())
                            {
                                obj = new BseNseMfuResponse();
                                obj.setTitle("Bank Details");
                                obj.setCompleted(false);
                                obj.setEnabled(true);
                                bseNseMfuList.add(obj);
                            }
                            else
                            {
                                obj = new BseNseMfuResponse();
                                obj.setTitle("Bank Details");
                                obj.setCompleted(false);
                                obj.setEnabled(false);
                                bseNseMfuList.add(obj);
                            }
                        }
                        else if(onboarding.getHas_nominee())
                        {
                            if(onboarding.getNomiee_info())
                            {
                                obj = new BseNseMfuResponse();
                                obj.setTitle("Bank Details");
                                obj.setCompleted(false);
                                obj.setEnabled(true);
                                bseNseMfuList.add(obj);
                            }
                            else
                            {
                                obj = new BseNseMfuResponse();
                                obj.setTitle("Bank Details");
                                obj.setCompleted(false);
                                obj.setEnabled(false);
                                bseNseMfuList.add(obj);
                            }
                        }
                        else if(onboarding.getContact_info())
                        {
                            obj = new BseNseMfuResponse();
                            obj.setTitle("Bank Details");
                            obj.setCompleted(false);
                            obj.setEnabled(true);
                            bseNseMfuList.add(obj);
                        }
                        else if(onboarding.getPersonal_info())
                        {
                            obj = new BseNseMfuResponse();
                            obj.setTitle("Bank Details");
                            obj.setCompleted(false);
                            obj.setEnabled(true);
                            bseNseMfuList.add(obj);
                        }
                        else
                        {
                            obj = new BseNseMfuResponse();
                            obj.setTitle("Bank Details");
                            obj.setCompleted(false);
                            obj.setEnabled(false);
                            bseNseMfuList.add(obj);
                        }
                    }
                    pojo.setMenu_list(bseNseMfuList);
                }
                else
                {
                    onboarding = new MymfboxOnboarding();
                    onboarding.setUser_id(user.getId());
                    onboarding.setClient_name(user.getClient_name());
                    onboarding.setVendor("NSE");
                    onboarding.setNse_already_reg_diff_arn(false);
                    onboarding.setIs_multiple_registration(false);
                    onboardingService.saveOnboarding(onboarding);

                    pojo.setVendor("NSE");
                    pojo.setTitle("NSE MF Invest Platform");
                    pojo.setLogo(vendorLogoPath + UserUtils.getVendorImage("NSE"));
                    pojo.setUser_id(user.getId());
                    pojo.setClient_name(user.getClient_name());
                    pojo.setTax_status("");
                    pojo.setHolding_nature("");
                    pojo.setInvestor_info(false);
                    pojo.setPersonal_info(false);
                    pojo.setContact_info(false);
                    pojo.setNri_info(false);
                    pojo.setJoint_holder_info(false);
                    pojo.setNomiee_info(false);
                    pojo.setBank_info(false);
                    pojo.setSignature_info(false);
                    pojo.setHas_nominee(false);
                    pojo.setHas_nri(false);
                    pojo.setHas_joint_holder(false);
                    pojo.setIs_all_steps_completed(false);
                    pojo.setIs_all_registration_completed(false);

                    obj = new BseNseMfuResponse();
                    obj.setTitle("Investor Information");
                    obj.setCompleted(false);
                    obj.setEnabled(true);
                    bseNseMfuList.add(obj);

                    obj = new BseNseMfuResponse();
                    obj.setTitle("Personal Info");
                    obj.setCompleted(false);
                    obj.setEnabled(false);
                    bseNseMfuList.add(obj);

                    obj = new BseNseMfuResponse();
                    obj.setTitle("Contact Info");
                    obj.setCompleted(false);
                    obj.setEnabled(false);
                    bseNseMfuList.add(obj);

                    obj = new BseNseMfuResponse();
                    obj.setTitle("Nominee Info");
                    obj.setCompleted(false);
                    obj.setEnabled(false);
                    bseNseMfuList.add(obj);

                    obj = new BseNseMfuResponse();
                    obj.setTitle("Bank Details");
                    obj.setCompleted(false);
                    obj.setEnabled(false);
                    bseNseMfuList.add(obj);
                    pojo.setMenu_list(bseNseMfuList);
                }
            }

            pojo.setStatus(HttpStatus.OK.value());
            pojo.setSuccess("Success");
            pojo.setMessage("Fetched onboarding details successfully");
            return ResponseEntity.status(HttpStatus.OK).body(pojo);
        }
        catch (Throwable ex)
        {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Get ARN Code",
            description = "Fetches the ARN and NSE application ID(s) associated with the provided client name.\n" +
                    "This is used to retrieve broker codes required for onboarding and transaction processing.\n" +
                    "Supports multiple NSE application ID fields for flexible integration with different platforms.",
            parameters = {
                    @Parameter(name = "client_name", required = true, example = "milansamajder", description = "Client name used to identify the ARN and NSE codes."),
                    @Parameter(name = "Authorization", in = ParameterIn.HEADER, required = true, description = "JWT Token")
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Success Response",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = BrokerCodeResponse.class),
                            examples = @ExampleObject(value = "{\n" +
                                    "  \"status\": 200,\n" +
                                    "  \"success\": \"OK\",\n" +
                                    "  \"message\": \"Fetched ARN code successfully.\",\n" +
                                    "  \"broker_code_list\": [\n" +
                                    "    \"ARN-123456\",\n" +
                                    "    \"ARN-654321\"\n" +
                                    "  ]\n" +
                                    "}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Failure Response",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\n" +
                                    "  \"status\": 400,\n" +
                                    "  \"error\": \"Client name is missing or invalid\"\n" +
                                    "}")
                    )
            )
    })
    @GetMapping("/getArnCode")
    public ResponseEntity<?> getArnCode(@RequestHeader("Authorization") String token,  @RequestParam String client_name)
    {
        try
        {
            BseNseKey bsekey = bseNseKeyRepository.findByClientName(client_name);

            String broker_code = "";
            String euin = "";
            List<String> broker_code_list = new ArrayList<String>();
            List<String> euin_code_list = new ArrayList<String>();
            if(bsekey != null)
            {

                String broker_code1 = bsekey.getBrokerCode();

                String nse_appln_id1 = bsekey.getNse_appln_id();


                if(broker_code1 == null){broker_code1 = "";};


                if(nse_appln_id1 == null){nse_appln_id1 = "";};


                broker_code1 = broker_code1.trim();

                nse_appln_id1 = nse_appln_id1.trim();

                if(StringHelper.isNotEmpty(broker_code1) && StringHelper.isNotEmpty(nse_appln_id1))
                {
                    broker_code_list.add(broker_code1);
                }


                if(broker_code_list.size() > 1)
                {
                    euin_code_list = new ArrayList<String>();
                }
            }
            BrokerCodeResponse apiResponse = new BrokerCodeResponse();
            apiResponse.setStatus(HttpStatus.OK.value());
            apiResponse.setSuccess(HttpStatus.OK.getReasonPhrase());
            apiResponse.setMessage("Fetched ARN code successfully.");
            apiResponse.setBroker_code(broker_code);
            apiResponse.setEuin(euin);
            apiResponse.setBrokerCodeList(broker_code_list);
            apiResponse.setEuinList(euin_code_list);
            apiResponse.setBrokerCodeList_size(broker_code_list.size());
            apiResponse.setEuinList_size(euin_code_list.size());
            return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
        }
        catch (Throwable ex)
        {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Get Broker Code List For Create Customer",
            description = "Fetches the ARN and NSE application ID(s) associated with the provided client name.\n" +
                    "This is used to retrieve broker codes required for onboarding and transaction processing.\n" +
                    "Supports multiple NSE application ID fields for flexible integration with different platforms.",
            parameters = {
                    @Parameter(name = "Authorization", in = ParameterIn.HEADER, required = true, description = "JWT Token")
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Success Response",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = BrokerCodeResponse.class),
                            examples = @ExampleObject(
                                    name = "ARN List Example",
                                    value = "[\"ARN-123456\", \"ARN-654321\"]"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Failure Response",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\n" +
                                    "  \"status\": 400,\n" +
                                    "  \"error\": \"Client name is missing or invalid\"\n" +
                                    "}")
                    )
            )
    })
    @GetMapping("/getBrokerCodeListForCreateCustomer")
    public ResponseEntity<?> getBrokerCodeList(@RequestHeader("Authorization") String token)
    {
        List<String> brokerCodeList = new ArrayList<String>();

        String userId = TokenInterceptor.extractAdminIdFromToken(token, secretKey);
        Optional<User> useOptional = userRepository.findUSerByIdAndActive(Integer.valueOf(userId));
        String client_name = useOptional.get().getClient_name();

        BseNseKey bsekey = bseNseKeyRepository.findByClientName(client_name);

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

        return ResponseEntity.ok(brokerCodeList);

    }

    @Operation(
            summary = "Save User Details",
            description = "Registers user-specific ARN and NSE application IDs for a given client name.\n\n"
                    + "This is used to save or update broker registration details required for onboarding, transactions, "
                    + "and compliance mapping. Supports storing multiple ARN/NSE app ID pairs for flexible integration "
                    + "with various platforms.",
            parameters =
                    {
                            @Parameter(name = "Authorization", required = false, description = "Authorization"),
                            @Parameter(name = "iin_number", required = false, description = "Iin number"),
                            @Parameter(name = "pan", required = false, description = "Pan"),
                            @Parameter(name = "name", required = false, description = "Name"),
                            @Parameter(name = "email", required = false, description = "Email"),

                            @Parameter(name = "nominee1_guard_name", required = false, description = "Nominee1 guard name"),
                            @Parameter(name = "nominee1_guard_pan", required = false, description = "Nominee1 guard pan"),
                            @Parameter(name = "nominee1_guard_relationship", required = false, description = "Nominee1 guard relationship"),
                            @Parameter(name = "nominee1_percentage", required = false, description = "Nominee1 percentage"),
                            @Parameter(name = "arn_number", required = false, description = "Arn number"),
                    }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Success Response",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = BrokerCodeResponse.class),
                            examples = @ExampleObject(value = "{\n" +
                                    "  \"status\": 200,\n" +
                                    "  \"success\": \"OK\",\n" +
                                    "  \"message\": \"User Details saved successfully.\",\n" +
                                    "}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Failure Response",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\n" +
                                    "  \"status\": 400,\n" +
                                    "  \"error\": \"Client name is missing or invalid\"\n" +
                                    "}")
                    )
            )
    })


    @GetMapping("/saveUserDetails")
    public ResponseEntity<?> saveUserDetails(
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
            @RequestParam(required = false) String gender,
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
            @RequestParam(required = false) String place_birth,
            @RequestParam(required = false) String country_birth,
            @RequestParam(required = false) String country_birth_code,
            @RequestParam(required = false) String occupation,
            @RequestParam(required = false) String occupation_code,
            @RequestParam(required = false) String income,
            @RequestParam(required = false) String income_code,
            @RequestParam(required = false) String source_wealth,
            @RequestParam(required = false) String source_wealth_code,
            @RequestParam(required = false) String political_status,
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
            @RequestParam(required = false) String joint_holder_place_birth,
            @RequestParam(required = false) String joint_holder_country_birth,
            @RequestParam(required = false) String joint_holder_occupation,
            @RequestParam(required = false) String joint_holder_income,
            @RequestParam(required = false) String joint_holder_source_wealth,
            @RequestParam(required = false) String joint_holder_address_type,
            @RequestParam(required = false) String joint_holder_political,
            @RequestParam(required = false) String joint_holder_place_birth1,
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
            @RequestParam(required = false) String nominee_soa,
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
            @RequestParam(required = false) String nominee_opt_flag
    )
    {
        String client_name = "";
        try
        {
            String userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);

            iin_number = UserUtils.checkParem(iin_number);
            userid = UserUtils.checkParem(userid);
            pan = UserUtils.checkParem(pan);
            name = UserUtils.checkParem(name);
            email = UserUtils.checkParem(email);
            mobile = UserUtils.checkParem(mobile);
            email_relation = UserUtils.checkParem(email_relation);
            mobile_relation = UserUtils.checkParem(mobile_relation);
            dob = UserUtils.checkParem(dob);
            gender = UserUtils.checkParem(gender);
            tax_status = UserUtils.checkParem(tax_status);
            tax_status_des = UserUtils.checkParem(tax_status_des);
            holding_nature = UserUtils.checkParem(holding_nature);
            holding_nature_desc = UserUtils.checkParem(holding_nature_desc);
            guard_name = UserUtils.checkParem(guard_name);
            guard_pan = UserUtils.checkParem(guard_pan);
            guard_dob = UserUtils.checkParem(guard_dob);
            guard_mobile = UserUtils.checkParem(guard_mobile);
            guard_email = UserUtils.checkParem(guard_email);
            guard_relation = UserUtils.checkParem(guard_relation);
            guard_account_relation = UserUtils.checkParem(guard_account_relation);
            father_name = UserUtils.checkParem(father_name);
            place_birth = UserUtils.checkParem(place_birth);
            country_birth = UserUtils.checkParem(country_birth);
            country_birth_code = UserUtils.checkParem(country_birth_code);
            occupation = UserUtils.checkParem(occupation);
            occupation_code = UserUtils.checkParem(occupation_code);
            income = UserUtils.checkParem(income);
            income_code = UserUtils.checkParem(income_code);
            source_wealth = UserUtils.checkParem(source_wealth);
            source_wealth_code = UserUtils.checkParem(source_wealth_code);
            political_status = UserUtils.checkParem(political_status);
            address1 = UserUtils.checkParem(address1);
            address2 = UserUtils.checkParem(address2);
            address3 = UserUtils.checkParem(address3);
            pincode = UserUtils.checkParem(pincode);
            city = UserUtils.checkParem(city);
            state = UserUtils.checkParem(state);
            state_code = UserUtils.checkParem(state_code);
            country = UserUtils.checkParem(country);
            ifsc_code = UserUtils.checkParem(ifsc_code);
            micr_code = UserUtils.checkParem(micr_code);
            bank_name = UserUtils.checkParem(bank_name);
            bank_code = UserUtils.checkParem(bank_code);
            branch_name = UserUtils.checkParem(branch_name);
            bank_address = UserUtils.checkParem(bank_address);
            account_number = UserUtils.checkParem(account_number);
            account_holder_name = UserUtils.checkParem(account_holder_name);
            account_type = UserUtils.checkParem(account_type);
            account_desc = UserUtils.checkParem(account_desc);
            joint_holder_name = UserUtils.checkParem(joint_holder_name);
            joint_holder_pan = UserUtils.checkParem(joint_holder_pan);
            joint_holder_email = UserUtils.checkParem(joint_holder_email);
            joint_holder_mobile = UserUtils.checkParem(joint_holder_mobile);
            joint_holder_dob = UserUtils.checkParem(joint_holder_dob);
            joint_holder_name1 = UserUtils.checkParem(joint_holder_name1);
            joint_holder_pan1 = UserUtils.checkParem(joint_holder_pan1);
            joint_holder_dob1 = UserUtils.checkParem(joint_holder_dob1);
            joint_holder_email1 = UserUtils.checkParem(joint_holder_email1);
            joint_holder_mobile1 = UserUtils.checkParem(joint_holder_mobile1);
            joint_holder_email_relation = UserUtils.checkParem(joint_holder_email_relation);
            joint_holder_email_relation1 = UserUtils.checkParem(joint_holder_email_relation1);
            joint_holder_mobile_relation = UserUtils.checkParem(joint_holder_mobile_relation);
            joint_holder_mobile_relation1 = UserUtils.checkParem(joint_holder_mobile_relation1);
            nri_address1 = UserUtils.checkParem(nri_address1);
            nri_address2 = UserUtils.checkParem(nri_address2);
            nri_address3 = UserUtils.checkParem(nri_address3);
            nri_city = UserUtils.checkParem(nri_city);
            nri_state = UserUtils.checkParem(nri_state);
            nri_pincode = UserUtils.checkParem(nri_pincode);
            nri_country = UserUtils.checkParem(nri_country);
            address_type = UserUtils.checkParem(address_type);
            address_type_desc = UserUtils.checkParem(address_type_desc);
            joint_holder_place_birth = UserUtils.checkParem(joint_holder_place_birth);
            joint_holder_country_birth = UserUtils.checkParem(joint_holder_country_birth);
            joint_holder_occupation = UserUtils.checkParem(joint_holder_occupation);
            joint_holder_income = UserUtils.checkParem(joint_holder_income);
            joint_holder_source_wealth = UserUtils.checkParem(joint_holder_source_wealth);
            joint_holder_address_type = UserUtils.checkParem(joint_holder_address_type);
            joint_holder_political = UserUtils.checkParem(joint_holder_political);
            joint_holder_place_birth1 = UserUtils.checkParem(joint_holder_place_birth1);
            joint_holder_country_birth1 = UserUtils.checkParem(joint_holder_country_birth1);
            joint_holder_occupation1 = UserUtils.checkParem(joint_holder_occupation1);
            joint_holder_income1 = UserUtils.checkParem(joint_holder_income1);
            joint_holder_source_wealth1 = UserUtils.checkParem(joint_holder_source_wealth1);
            joint_holder_address_type1 = UserUtils.checkParem(joint_holder_address_type1);
            joint_holder_political1 = UserUtils.checkParem(joint_holder_political1);
            number_of_nominee = UserUtils.checkParem(number_of_nominee);
            number_of_nominee_desc = UserUtils.checkParem(number_of_nominee_desc);
            nominee_type = UserUtils.checkParem(nominee_type);
            nominee_type_desc = UserUtils.checkParem(nominee_type_desc);
            nominee1_name = UserUtils.checkParem(nominee1_name);
            nominee1_dob = UserUtils.checkParem(nominee1_dob);
            nominee1_address1 = UserUtils.checkParem(nominee1_address1);
            nominee1_address2 = UserUtils.checkParem(nominee1_address2);
            nominee1_address3 = UserUtils.checkParem(nominee1_address3);
            nominee1_pincode = UserUtils.checkParem(nominee1_pincode);
            nominee1_city = UserUtils.checkParem(nominee1_city);
            nominee1_state = UserUtils.checkParem(nominee1_state);
            nominee1_state_code = UserUtils.checkParem(nominee1_state_code);
            nominee1_country = UserUtils.checkParem(nominee1_country);
            nominee1_id_type = UserUtils.checkParem(nominee1_id_type);
            nominee1_id_no = UserUtils.checkParem(nominee1_id_no);
            nominee1_email = UserUtils.checkParem(nominee1_email);
            nominee1_mobile = UserUtils.checkParem(nominee1_mobile);
            nominee1_relation = UserUtils.checkParem(nominee1_relation);
            nominee1_guard_name = UserUtils.checkParem(nominee1_guard_name);
            nominee1_guard_pan = UserUtils.checkParem(nominee1_guard_pan);
            nominee1_guard_relationship = UserUtils.checkParem(nominee1_guard_relationship);
            nominee1_percentage = UserUtils.checkParem(nominee1_percentage);
            nominee2_type = UserUtils.checkParem(nominee2_type);
            nominee2_type_desc = UserUtils.checkParem(nominee2_type_desc);
            nominee2_name = UserUtils.checkParem(nominee2_name);
            nominee2_dob = UserUtils.checkParem(nominee2_dob);
            nominee2_relation = UserUtils.checkParem(nominee2_relation);
            nominee2_percentage = UserUtils.checkParem(nominee2_percentage);
            nominee2_address1 = UserUtils.checkParem(nominee2_address1);
            nominee2_address2 = UserUtils.checkParem(nominee2_address2);
            nominee2_address3 = UserUtils.checkParem(nominee2_address3);
            nominee2_pincode = UserUtils.checkParem(nominee2_pincode);
            nominee2_city = UserUtils.checkParem(nominee2_city);
            nominee2_state = UserUtils.checkParem(nominee2_state);
            nominee2_state_code = UserUtils.checkParem(nominee2_state_code);
            nominee2_country = UserUtils.checkParem(nominee2_country);
            nominee2_id_type = UserUtils.checkParem(nominee2_id_type);
            nominee2_id_no = UserUtils.checkParem(nominee2_id_no);
            nominee2_email = UserUtils.checkParem(nominee2_email);
            nominee2_mobile = UserUtils.checkParem(nominee2_mobile);
            nominee2_guard_name = UserUtils.checkParem(nominee2_guard_name);
            nominee2_guard_pan = UserUtils.checkParem(nominee2_guard_pan);
            nominee2_guard_relationship = UserUtils.checkParem(nominee2_guard_relationship);
            nominee3_type = UserUtils.checkParem(nominee3_type);
            nominee3_type_desc = UserUtils.checkParem(nominee3_type_desc);
            nominee3_name = UserUtils.checkParem(nominee3_name);
            nominee3_dob = UserUtils.checkParem(nominee3_dob);
            nominee3_relation = UserUtils.checkParem(nominee3_relation);
            nominee3_percentage = UserUtils.checkParem(nominee3_percentage);
            nominee3_address1 = UserUtils.checkParem(nominee3_address1);
            nominee3_address2 = UserUtils.checkParem(nominee3_address2);
            nominee3_address3 = UserUtils.checkParem(nominee3_address3);
            nominee3_pincode = UserUtils.checkParem(nominee3_pincode);
            nominee3_city = UserUtils.checkParem(nominee3_city);
            nominee3_state = UserUtils.checkParem(nominee3_state);
            nominee3_state_code = UserUtils.checkParem(nominee3_state_code);
            nominee3_country = UserUtils.checkParem(nominee3_country);
            nominee3_id_type = UserUtils.checkParem(nominee3_id_type);
            nominee3_id_no = UserUtils.checkParem(nominee3_id_no);
            nominee3_email = UserUtils.checkParem(nominee3_email);
            nominee3_mobile = UserUtils.checkParem(nominee3_mobile);
            nominee3_guard_name = UserUtils.checkParem(nominee3_guard_name);
            nominee3_guard_pan = UserUtils.checkParem(nominee3_guard_pan);
            nominee3_guard_relationship = UserUtils.checkParem(nominee3_guard_relationship);
            networth_dob = UserUtils.checkParem(networth_dob);
            networth_amount = UserUtils.checkParem(networth_amount);
            occupation_other = UserUtils.checkParem(occupation_other);
            source_wealth_other = UserUtils.checkParem(source_wealth_other);
            joint_holder_occupation_other = UserUtils.checkParem(joint_holder_occupation_other);
            joint_source_wealth_other = UserUtils.checkParem(joint_source_wealth_other);
            joint_holder_occupation_other1 = UserUtils.checkParem(joint_holder_occupation_other1);
            joint_source_wealth_other1 = UserUtils.checkParem(joint_source_wealth_other1);
            alter_mobile = UserUtils.checkParem(alter_mobile);
            alter_email = UserUtils.checkParem(alter_email);
            inv_category = UserUtils.checkParem(inv_category);
            gaurd_relation_proof = UserUtils.checkParem(gaurd_relation_proof);
            residence_phone = UserUtils.checkParem(residence_phone);
            office_phone = UserUtils.checkParem(office_phone);
            bank_proof = UserUtils.checkParem(bank_proof);
            nominee1_guard_dob = UserUtils.checkParem(nominee1_guard_dob);
            nominee2_guard_dob = UserUtils.checkParem(nominee2_guard_dob);
            nominee3_guard_dob = UserUtils.checkParem(nominee3_guard_dob);
            mobile_isd_code = UserUtils.checkParem(mobile_isd_code);
            joint_holder_mobile1_isd_code = UserUtils.checkParem(joint_holder_mobile1_isd_code);
            joint_holder_mobile2_isd_code = UserUtils.checkParem(joint_holder_mobile2_isd_code);
            arn_number = UserUtils.checkParem(arn_number);
            nominee_soa = UserUtils.checkParem(nominee_soa);
            nominee_opt_flag = UserUtils.checkParem(nominee_opt_flag);

            if(!nominee_soa.isEmpty())
            {
                nominee_soa = nominee_soa;
            }else{
                nominee_soa = "N";
            }

            User userMain = userRepository.findById(Integer.parseInt(userid)).orElse(null);

            if (userMain == null)
            {
                return UserUtils.errorResponse("User not found", HttpStatus.BAD_REQUEST);
            }

            if(StringHelper.isNotEmpty(iin_number))
            {
                UsersOnlineRegDetails userDetails = checkNseIinNumber.CheckNewIinNumber(userMain.getClient_name(),iin_number, arn_number);
                if(userDetails == null) {

                }else{
                    if(userDetails.getNse_active() == 1)
                    {
                        return UserUtils.errorResponse("This client code is already exist. Please create a new client code", HttpStatus.BAD_REQUEST);
                    }else{

                    }
                }
            }

            UsersOnlineRegDetails user = null;

            if(StringHelper.isNotEmpty(iin_number))
            {
                user = userOnlineRegDetailsRespository.findByUserIdAndOnlineCodeAndIinNumber(Integer.valueOf(userid), "NSE", arn_number,iin_number);
            }else
            {
                user = userOnlineRegDetailsRespository.findByUserIdAndOnlineCode(Integer.valueOf(userid), "NSE", arn_number);
            }

            if(user == null)
            {
                user= new UsersOnlineRegDetails();
            }

            user.setUser_id(Integer.parseInt(userid));
            user.setName(userMain.getName());
            user.setPan(userMain.getPan());
            user.setMobile(userMain.getMobile());
            user.setEmail(userMain.getEmail());
//            user.setAlter_email(userMain.getAlter_email());
//            user.setAlter_mobile(userMain.getAlter_mobile());
            user.setStreet_1(userMain.getStreet_1());
            user.setStreet_2(userMain.getStreet_2());
            user.setStreet_3(userMain.getStreet_3());
            user.setCity(userMain.getCity());
            user.setPincode(userMain.getPincode());
            user.setState(userMain.getState());
            user.setCountry(userMain.getCountry());
            user.setFather_name(userMain.getFather_name());
            user.setGender(userMain.getGender());
            user.setDate_of_birth(userMain.getDate_of_birth());
            user.setPhone_office(userMain.getPhone_office());
            user.setPhone_residence(userMain.getPhone_residence());
            user.setBroker_code(userMain.getBroker_code());
            user.setClient_name(userMain.getClient_name());

            client_name = userMain.getClient_name();


            String euin = "";
            if(!arn_number.isEmpty())
            {
                BseNseKey list = bseNseKeyRepository.findByClientName(client_name);

                String broker_code1 = list.getBrokerCode();

                if(broker_code1 == null){broker_code1 = "";};

                euin = list.getEuin();
                euin = euin.split(",")[0];
            }

            String iin_number_new = checkNseIinNumber.CheckNseIinNumbers(user.getClient_name());

            if(StringHelper.isNotEmpty(iin_number))
            {
                user.setNse_iin_number(iin_number);
            }
            else
            {
                if(StringHelper.isNotEmpty(pan))
                {
                    boolean iin_number_flag = checkNseIinNumber.CheckNewIinNumbers(userMain.getClient_name(),pan.toUpperCase());
                    if(!iin_number_flag)
                    {
                        user.setNse_iin_number(pan.toUpperCase());
                    }else
                    {
                        user.setNse_iin_number(iin_number_new.toUpperCase());
                    }
                }
                else
                {
                    user.setNse_iin_number(iin_number_new.toUpperCase());
                }
            }

            user.setPan(pan);
            user.setName(name.replaceAll("\\s+", " ").trim());
            user.setEmail(email);
            user.setMobile(mobile);
            user.setMobile_isd_code(mobile_isd_code);
            user.setMobile_relation(mobile_relation);
            user.setEmail_relation(email_relation);
            user.setAlter_email(alter_email);
            user.setAlter_mobile(alter_mobile);
            user.setDate_of_birth(dob);
            user.setFather_name(father_name);
            user.setPhone_office(office_phone);
            user.setPhone_residence(residence_phone);
            user.setPlace_of_birth(place_birth);
            user.setCountry_of_birth(country_birth);
            user.setCountry_birth_code(country_birth_code);
            user.setInv_category(inv_category);
            user.setOccupation(occupation);
            user.setOccupation_code(occupation_code);
            if(occupation_code.equalsIgnoreCase("99") && !occupation_other.isEmpty())
            {
                user.setOccupation(occupation_other);
            }
            user.setAnnual_income(income);
            user.setAnnual_income_code(income_code);
            user.setSource_of_wealth(source_wealth);
            user.setSource_of_wealth_code(source_wealth_code);
            if(source_wealth_code.equalsIgnoreCase("08") && !source_wealth_other.isEmpty())
            {
                user.setSource_of_wealth(source_wealth_other);
            }
            user.setPolitical_code(political_status);
            if(political_status.equalsIgnoreCase("Y") || political_status.equalsIgnoreCase("PEP"))
            {
                user.setPolitical("I am Politically exposed person");
            }
            if(political_status.equalsIgnoreCase("R") || political_status.equalsIgnoreCase("RPEP"))
            {
                user.setPolitical("I am related to Politically exposed person");
            }
            if(political_status.equalsIgnoreCase("N") || political_status.equalsIgnoreCase("NA"))
            {
                user.setPolitical("Not Applicable");
            }
            user.setOnline_flag("NSE");
            user.setPincode(pincode);
            user.setCity(city);
            user.setState(state);
            user.setCountry(country);
            user.setStreet_1(address1);
            user.setStreet_2(address2);
            user.setStreet_3(address3);
            user.setState_code(state_code);

            user.setGuard_name(guard_name);
            user.setGuard_pan(guard_pan);
            user.setGuard_dob(guard_dob);
            user.setGuard_mobile(guard_mobile);
            user.setGuard_email(guard_email);
            user.setGuard_relationship(guard_relation);
            user.setGuard_account_relation(guard_account_relation);
            user.setGuard_relation_proof(gaurd_relation_proof);
            user.setTax_status_code(tax_status);
            user.setTax_status(tax_status_des);
            user.setJoint_holder_name1(joint_holder_name);
            user.setJoint_holder_name2(joint_holder_name1);
            user.setJoint_holder_dob1(joint_holder_dob);
            user.setJoint_holder_dob2(joint_holder_dob1);
            user.setJoint_holder_email1(joint_holder_email);
            user.setJoint_holder_email2(joint_holder_email1);
            user.setJoint_holder_mobile1(joint_holder_mobile);
//            user.setJoint_holder_mobile1_isd_code(joint_holder_mobile1_isd_code);
            user.setJoint_holder_mobile2(joint_holder_mobile1);
            user.setJoint_holder_mobile2_isd_code(joint_holder_mobile2_isd_code);
            user.setJoint_holder_email_relation1(joint_holder_email_relation);
            user.setJoint_holder_email_relation2(joint_holder_email_relation1);
            user.setJoint_holder_mobile_relation1(joint_holder_mobile_relation);
            user.setJoint_holder_mobile_relation2(joint_holder_mobile_relation1);

            user.setJoint_holder_pan1(joint_holder_pan);
            user.setJoint_holder_pan2(joint_holder_pan1);
            user.setHolding_nature_code(holding_nature);
            user.setHolding_nature(holding_nature_desc);
            user.setGender(gender);
//            user.setMarital_status("");
            user.setNse_customer(0);
            user.setNse_active(0);
            user.setClient_name(client_name);
            user.setNri_address1(nri_address1);
            user.setNri_address2(nri_address2);
            user.setNri_address3(nri_address3);
            user.setNri_city(nri_city);
            user.setNri_state(nri_state);
            user.setNri_pincode(nri_pincode);
            user.setNri_country(nri_country);
            user.setAddress_type_code(address_type);
            user.setAddress_type(address_type_desc);

            user.setJoint_holder_place_of_birth1(joint_holder_place_birth);
            user.setJoint_holder_place_of_birth2(joint_holder_place_birth1);
            user.setJoint_holder_country_birth_code1(joint_holder_country_birth);
            user.setJoint_holder_country_birth_code2(joint_holder_country_birth1);
            user.setJoint_holder_occupation_code1(joint_holder_occupation);
            if(joint_holder_occupation.equalsIgnoreCase("99") && !joint_holder_occupation_other.isEmpty())
            {
                //user.setJoint_holder_occupation_other1(joint_holder_occupation_other);
            }
            user.setJoint_holder_occupation_code2(joint_holder_occupation1);
            if(joint_holder_occupation1.equalsIgnoreCase("99") && !joint_holder_occupation_other1.isEmpty())
            {
                //user.setJoint_holder_occupation_other2(joint_holder_occupation_other1);
            }
            user.setJoint_holder_source_of_wealth_code1(joint_holder_source_wealth);
            if(joint_holder_source_wealth.equalsIgnoreCase("08") && !joint_source_wealth_other.isEmpty())
            {
                //user.setJoint_holder_source_of_wealth_other1(joint_source_wealth_other);
            }
            user.setJoint_holder_source_of_wealth_code2(joint_holder_source_wealth1);
            if(joint_holder_source_wealth1.equalsIgnoreCase("08") && !joint_source_wealth_other1.isEmpty())
            {
                //user.setJoint_holder_source_of_wealth_other2(joint_source_wealth_other1);
            }
            user.setJoint_holder_annual_income_code1(joint_holder_income);
            user.setJoint_holder_annual_income_code2(joint_holder_income1);
            user.setJoint_holder_address_type_code1(joint_holder_address_type);
            user.setJoint_holder_address_type_code2(joint_holder_address_type1);
            user.setJoint_holder_political_code1(joint_holder_political);
            user.setJoint_holder_political_code2(joint_holder_political1);

            user.setNetworth_amount(networth_amount);
            user.setNetworth_dob(networth_dob);
            user.setBroker_code(arn_number);
            user.setEuin(euin);
            user.setCreated_date(new Date());

            user = userOnlineRegDetailsRespository.save(user);

            UsersBankDetails bank = usersBankDetailsRepository.getUsersBankDetailsByOnlineId(user.getId(), client_name);
            if(bank == null){
                bank = new UsersBankDetails();
            }
            bank.setUser_id(Integer.parseInt(userid));
            bank.setOnline_id(user.getId());
            bank.setOnline_flag("NSE");
            bank.setOnline_code(user.getNse_iin_number());
            bank.setBroker_code(arn_number);
            bank.setClient_name(client_name);
            bank.setBank_ifsc_code(ifsc_code);
            bank.setBank_micr_code(micr_code);
            bank.setBank_name(bank_name);
            bank.setBank_branch(branch_name);
            bank.setBank_address(bank_address);
            bank.setBank_account_number(account_number);
            bank.setBank_account_holder_name(account_holder_name);
            bank.setBank_account_type(account_type);
            bank.setBank_proof(bank_proof);
            bank.setCreated_date(new Date());
            usersBankDetailsRepository.save(bank);

            /*
            userBankDetailsRepository.save(bankDetails);
            UsersBankDetails bankDetails = new UsersBankDetails();

            bankDetails.setBank_ifsc_code(ifsc_code);
            bankDetails.setBank_micr_code(micr_code);
            bankDetails.setBank_name(bank_name);
            bankDetails.setBank_code(bank_code);
            bankDetails.setBank_branch(branch_name);
            bankDetails.setBank_address(bank_address);
            bankDetails.setBank_account_number(account_number);
            bankDetails.setBank_account_holder_name(account_holder_name);
            bankDetails.setBank_account_type(account_type);
            bankDetails.setDefault_bank("Y");
            bankDetails.setBank_proof(bank_proof);*/

            UsersNomineeDetails nominee = usersNomineeDetailsRepository.getUsersNomineeDetailsByOnlineId(user.getId(), client_name);
            if(nominee == null){
                nominee = new UsersNomineeDetails();
            }
            nominee.setUser_id(Integer.parseInt(userid));
            nominee.setOnline_id(user.getId());
            nominee.setOnline_flag("NSE");
            nominee.setOnline_code(user.getNse_iin_number());
            nominee.setBroker_code(arn_number);
            nominee.setClient_name(client_name);
            nominee.setNominee1_type(nominee_type);
            nominee.setNominee1_guard_name(nominee1_guard_name);
            nominee.setNominee1_guard_pan(nominee1_guard_pan);
            nominee.setNominee2_type(nominee2_type);
            nominee.setNominee2_guard_name(nominee2_guard_name);
            nominee.setNominee2_guard_pan(nominee2_guard_pan);
            nominee.setNominee3_type(nominee3_type);
            nominee.setNominee3_guard_name(nominee3_guard_name);
            nominee.setNominee3_guard_pan(nominee3_guard_pan);
            nominee.setNumber_of_nominee(number_of_nominee);
            nominee.setNominee_soa(nominee_soa);

            nominee.setNominee_opt(nominee_opt_flag);

            nominee.setNominee1_name(nominee1_name);
            nominee.setNominee1_dob(nominee1_dob);
            nominee.setNominee1_address1(nominee1_address1);
            nominee.setNominee1_address2(nominee1_address2);
            nominee.setNominee1_address3(nominee1_address3);
            nominee.setNominee1_pincode(nominee1_pincode);
            nominee.setNominee1_city(nominee1_city);
            nominee.setNominee1_state(nominee1_state);
            nominee.setNominee1_state_code(nominee1_state_code);
            nominee.setNominee1_country(nominee1_country);
            nominee.setNominee1_email(nominee1_email);
            nominee.setNominee1_mobile(nominee1_mobile);
            nominee.setNominee1_id_no(nominee1_id_no);
            nominee.setNominee1_id_type(nominee1_id_type);
            nominee.setNominee1_relation(nominee1_relation);
            nominee.setNominee1_percentage(nominee1_percentage);

            nominee.setNominee2_name(nominee2_name);
            nominee.setNominee2_dob(nominee2_dob);
            nominee.setNominee2_percentage(nominee2_percentage);
            nominee.setNominee2_relation(nominee2_relation);
            nominee.setNominee2_address1(nominee2_address1);
            nominee.setNominee2_address2(nominee2_address2);
            nominee.setNominee2_address3(nominee2_address3);
            nominee.setNominee2_pincode(nominee2_pincode);
            nominee.setNominee2_city(nominee2_city);
            nominee.setNominee2_state(nominee2_state);
            nominee.setNominee2_state_code(nominee2_state_code);
            nominee.setNominee2_country(nominee2_country);
            nominee.setNominee2_email(nominee2_email);
            nominee.setNominee2_mobile(nominee2_mobile);
            nominee.setNominee2_id_no(nominee2_id_no);
            nominee.setNominee2_id_type(nominee2_id_type);
            nominee.setNominee3_name(nominee3_name);
            nominee.setNominee3_dob(nominee3_dob);
            nominee.setNominee3_percentage(nominee3_percentage);
            nominee.setNominee3_relation(nominee3_relation);
            nominee.setNominee3_address1(nominee3_address1);
            nominee.setNominee3_address2(nominee3_address2);
            nominee.setNominee3_address3(nominee3_address3);
            nominee.setNominee3_pincode(nominee3_pincode);
            nominee.setNominee3_city(nominee3_city);
            nominee.setNominee3_state(nominee3_state);
            nominee.setNominee3_state_code(nominee3_state_code);
            nominee.setNominee3_country(nominee3_country);
            nominee.setNominee3_email(nominee3_email);
            nominee.setNominee3_mobile(nominee3_mobile);
            nominee.setNominee3_id_no(nominee3_id_no);
            nominee.setNominee3_id_type(nominee3_id_type);
            nominee.setNominee1_guard_relationship(nominee1_guard_relationship);
            nominee.setNominee2_guard_relationship(nominee2_guard_relationship);
            nominee.setNominee3_guard_relationship(nominee3_guard_relationship);
            nominee.setCreated_date(new Date());
            usersNomineeDetailsRepository.save(nominee);

/*
            nomineeDetails.setNominee1_type(nominee_type);
            nomineeDetails.setNominee1_guard_name(nominee1_guard_name);
            nomineeDetails.setNominee1_guard_pan(nominee1_guard_pan);
            nomineeDetails.setNominee2_type(nominee2_type);
            nomineeDetails.setNominee2_guard_name(nominee2_guard_name);
            nomineeDetails.setNominee2_guard_pan(nominee2_guard_pan);
            nomineeDetails.setNominee3_type(nominee3_type);
            nomineeDetails.setNominee3_guard_name(nominee3_guard_name);
            nomineeDetails.setNominee3_guard_pan(nominee3_guard_pan);
            nomineeDetails.setNumber_of_nominee(number_of_nominee);

            nomineeDetails.setNominee1_name(nominee1_name);
            nomineeDetails.setNominee1_dob(nominee1_dob);
            nomineeDetails.setNominee1_address1(nominee1_address1);
            nomineeDetails.setNominee1_address2(nominee1_address2);
            nomineeDetails.setNominee1_address3(nominee1_address3);
            nomineeDetails.setNominee1_pincode(nominee1_pincode);
            nomineeDetails.setNominee1_city(nominee1_city);
            nomineeDetails.setNominee1_state(nominee1_state);
            nomineeDetails.setNominee1_state_code(nominee1_state_code);
            nomineeDetails.setNominee1_country(nominee1_country);
            nomineeDetails.setNominee1_email(nominee1_email);
            nomineeDetails.setNominee1_mobile(nominee1_mobile);
            nomineeDetails.setNominee1_id_no(nominee1_id_no);
            nomineeDetails.setNominee1_id_type(nominee1_id_type);
            nomineeDetails.setNominee1_relation(nominee1_relation);
            nomineeDetails.setNominee1_percentage(nominee1_percentage);

            nomineeDetails.setNominee2_name(nominee2_name);
            nomineeDetails.setNominee2_dob(nominee2_dob);
            nomineeDetails.setNominee2_percentage(nominee2_percentage);
            nomineeDetails.setNominee2_relation(nominee2_relation);
            nomineeDetails.setNominee2_address1(nominee2_address1);
            //user.setNominee2_address2(nominee2_address2);
            //user.setNominee2_address3(nominee2_address3);
            nomineeDetails.setNominee2_pincode(nominee2_pincode);
            nomineeDetails.setNominee2_city(nominee2_city);
            nomineeDetails.setNominee2_state(nominee2_state);
            nomineeDetails.setNominee2_state_code(nominee2_state_code);
            nomineeDetails.setNominee2_country(nominee2_country);
            nomineeDetails.setNominee2_email(nominee2_email);
            nomineeDetails.setNominee2_mobile(nominee2_mobile);
            nomineeDetails.setNominee2_id_no(nominee2_id_no);
            nomineeDetails.setNominee2_id_type(nominee2_id_type);
            nomineeDetails.setNominee3_name(nominee3_name);
            nomineeDetails.setNominee3_dob(nominee3_dob);
            nomineeDetails.setNominee3_percentage(nominee3_percentage);
            nomineeDetails.setNominee3_relation(nominee3_relation);
            nomineeDetails.setNominee3_address1(nominee3_address1);
            //user.setNominee3_address2(nominee3_address2);
            //user.setNominee3_address3(nominee3_address3);
            nomineeDetails.setNominee3_pincode(nominee3_pincode);
            nomineeDetails.setNominee3_city(nominee3_city);
            nomineeDetails.setNominee3_state(nominee3_state);
            nomineeDetails.setNominee3_state_code(nominee3_state_code);
            nomineeDetails.setNominee3_country(nominee3_country);
            nomineeDetails.setNominee3_email(nominee3_email);
            nomineeDetails.setNominee3_mobile(nominee3_mobile);
            nomineeDetails.setNominee3_id_no(nominee3_id_no);
            nomineeDetails.setNominee3_id_type(nominee3_id_type);

            nomineeDetails.setNominee1_guard_dob(nominee1_guard_dob);
            nomineeDetails.setNominee2_guard_dob(nominee2_guard_dob);
            nomineeDetails.setNominee3_guard_dob(nominee3_guard_dob);
            nomineeDetails.setNominee1_guard_relationship(nominee1_guard_relationship);
            nomineeDetails.setNominee2_guard_relationship(nominee2_guard_relationship);
            nomineeDetails.setNominee3_guard_relationship(nominee3_guard_relationship);*/

            String ipAddr = UserUtils.getIpAddr(request);
            if(ipAddr == null){ipAddr="";}

            String logmsg = name+" did NSE Create Customer. Details:";
            logmsg += "userid: "+userid+",";
            logmsg += "pan: "+pan+",";
            logmsg += "name: "+name+",";
            logmsg += "email: "+email+",";
            logmsg += "mobile: "+mobile+",";
            logmsg += "tax_status: "+tax_status+",";
            logmsg += "holding_nature: "+holding_nature+",";
            logmsg += "bank_name: "+bank_name+",";
            logmsg += "account_number: "+account_number+",";
            logmsg += "nominee1_name: "+nominee1_name+",";
            logmsg += "nominee1_relation: "+nominee1_relation+"";

            logService.saveLog(client_name, Integer.parseInt(userid), name, mobile, "NSE Create Customer", "NSE Create Customer", logmsg , ipAddr);

            return UserUtils.successResponse(String.valueOf(user.getId()), HttpStatus.OK);

        }catch (Throwable ex)
        {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error: " + ex.getMessage());
        }
    }


    @Operation(
            summary = "Save Multiple RegistrationUser Details",
            description = "Registers user-specific ARN and NSE application IDs for a given client name.\n\n"
                    + "This is used to save or update broker registration details required for onboarding, transactions, "
                    + "and compliance mapping. Supports storing multiple ARN/NSE app ID pairs for flexible integration "
                    + "with various platforms.",
            parameters =
                    {
                            @Parameter(name = "Authorization", required = false, description = "Authorization"),
                            @Parameter(name = "iin_number", required = false, description = "Iin number"),
                            @Parameter(name = "pan", required = false, description = "Pan"),
                            @Parameter(name = "name", required = false, description = "Name"),
                            @Parameter(name = "email", required = false, description = "Email"),

                            @Parameter(name = "nominee1_relation", required = false, description = "Nominee1 relation"),
                            @Parameter(name = "nominee1_guard_name", required = false, description = "Nominee1 guard name"),
                            @Parameter(name = "nominee1_guard_pan", required = false, description = "Nominee1 guard pan"),
                            @Parameter(name = "nominee1_guard_relationship", required = false, description = "Nominee1 guard relationship"),
                            @Parameter(name = "nominee1_percentage", required = false, description = "Nominee1 percentage"),
                            @Parameter(name = "arn_number", required = false, description = "Arn number"),
                    }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Success Response",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = BrokerCodeResponse.class),
                            examples = @ExampleObject(value = "{\n" +
                                    "  \"status\": 200,\n" +
                                    "  \"success\": \"OK\",\n" +
                                    "  \"message\": \"User Details saved successfully.\",\n" +
                                    "}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Failure Response",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\n" +
                                    "  \"status\": 400,\n" +
                                    "  \"error\": \"Client name is missing or invalid\"\n" +
                                    "}")
                    )
            )
    })
    @PostMapping("/saveUserMultipleRegistrationDetails")
    public ResponseEntity<?> saveUserMultipleRegistrationDetails(
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
            @RequestParam(required = false) String gender,
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
            @RequestParam(required = false) String place_birth,
            @RequestParam(required = false) String country_birth,
            @RequestParam(required = false) String country_birth_code,
            @RequestParam(required = false) String occupation,
            @RequestParam(required = false) String occupation_code,
            @RequestParam(required = false) String income,
            @RequestParam(required = false) String income_code,
            @RequestParam(required = false) String source_wealth,
            @RequestParam(required = false) String source_wealth_code,
            @RequestParam(required = false) String political_status,
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
            @RequestParam(required = false) String joint_holder_place_birth,
            @RequestParam(required = false) String joint_holder_country_birth,
            @RequestParam(required = false) String joint_holder_occupation,
            @RequestParam(required = false) String joint_holder_income,
            @RequestParam(required = false) String joint_holder_source_wealth,
            @RequestParam(required = false) String joint_holder_address_type,
            @RequestParam(required = false) String joint_holder_political,
            @RequestParam(required = false) String joint_holder_place_birth1,
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
            @RequestParam(required = false) String gaurd_relation,
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
            @RequestParam(required = false) String nominee_opt_flag

    )
    {
        String client_name = "";
        try
        {
            String userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);

            iin_number = UserUtils.checkParem(iin_number);
            userid = UserUtils.checkParem(userid);
            pan = UserUtils.checkParem(pan);
            name = UserUtils.checkParem(name);
            email = UserUtils.checkParem(email);
            mobile = UserUtils.checkParem(mobile);
            email_relation = UserUtils.checkParem(email_relation);
            mobile_relation = UserUtils.checkParem(mobile_relation);
            dob = UserUtils.checkParem(dob);
            gender = UserUtils.checkParem(gender);
            tax_status = UserUtils.checkParem(tax_status);
            tax_status_des = UserUtils.checkParem(tax_status_des);
            holding_nature = UserUtils.checkParem(holding_nature);
            holding_nature_desc = UserUtils.checkParem(holding_nature_desc);
            guard_name = UserUtils.checkParem(guard_name);
            guard_pan = UserUtils.checkParem(guard_pan);
            guard_dob = UserUtils.checkParem(guard_dob);
            guard_mobile = UserUtils.checkParem(guard_mobile);
            guard_email = UserUtils.checkParem(guard_email);
            guard_relation = UserUtils.checkParem(guard_relation);
            guard_account_relation = UserUtils.checkParem(guard_account_relation);
            father_name = UserUtils.checkParem(father_name);
            place_birth = UserUtils.checkParem(place_birth);
            country_birth = UserUtils.checkParem(country_birth);
            country_birth_code = UserUtils.checkParem(country_birth_code);
            occupation = UserUtils.checkParem(occupation);
            occupation_code = UserUtils.checkParem(occupation_code);
            income = UserUtils.checkParem(income);
            income_code = UserUtils.checkParem(income_code);
            source_wealth = UserUtils.checkParem(source_wealth);
            source_wealth_code = UserUtils.checkParem(source_wealth_code);
            political_status = UserUtils.checkParem(political_status);
            address1 = UserUtils.checkParem(address1);
            address2 = UserUtils.checkParem(address2);
            address3 = UserUtils.checkParem(address3);
            pincode = UserUtils.checkParem(pincode);
            city = UserUtils.checkParem(city);
            state = UserUtils.checkParem(state);
            state_code = UserUtils.checkParem(state_code);
            country = UserUtils.checkParem(country);
            ifsc_code = UserUtils.checkParem(ifsc_code);
            micr_code = UserUtils.checkParem(micr_code);
            bank_name = UserUtils.checkParem(bank_name);
            bank_code = UserUtils.checkParem(bank_code);
            branch_name = UserUtils.checkParem(branch_name);
            bank_address = UserUtils.checkParem(bank_address);
            account_number = UserUtils.checkParem(account_number);
            account_holder_name = UserUtils.checkParem(account_holder_name);
            account_type = UserUtils.checkParem(account_type);
            account_desc = UserUtils.checkParem(account_desc);
            joint_holder_name = UserUtils.checkParem(joint_holder_name);
            joint_holder_pan = UserUtils.checkParem(joint_holder_pan);
            joint_holder_email = UserUtils.checkParem(joint_holder_email);
            joint_holder_mobile = UserUtils.checkParem(joint_holder_mobile);
            joint_holder_dob = UserUtils.checkParem(joint_holder_dob);
            joint_holder_name1 = UserUtils.checkParem(joint_holder_name1);
            joint_holder_pan1 = UserUtils.checkParem(joint_holder_pan1);
            joint_holder_dob1 = UserUtils.checkParem(joint_holder_dob1);
            joint_holder_email1 = UserUtils.checkParem(joint_holder_email1);
            joint_holder_mobile1 = UserUtils.checkParem(joint_holder_mobile1);
            joint_holder_email_relation = UserUtils.checkParem(joint_holder_email_relation);
            joint_holder_email_relation1 = UserUtils.checkParem(joint_holder_email_relation1);
            joint_holder_mobile_relation = UserUtils.checkParem(joint_holder_mobile_relation);
            joint_holder_mobile_relation1 = UserUtils.checkParem(joint_holder_mobile_relation1);
            nri_address1 = UserUtils.checkParem(nri_address1);
            nri_address2 = UserUtils.checkParem(nri_address2);
            nri_address3 = UserUtils.checkParem(nri_address3);
            nri_city = UserUtils.checkParem(nri_city);
            nri_state = UserUtils.checkParem(nri_state);
            nri_pincode = UserUtils.checkParem(nri_pincode);
            nri_country = UserUtils.checkParem(nri_country);
            address_type = UserUtils.checkParem(address_type);
            address_type_desc = UserUtils.checkParem(address_type_desc);
            joint_holder_place_birth = UserUtils.checkParem(joint_holder_place_birth);
            joint_holder_country_birth = UserUtils.checkParem(joint_holder_country_birth);
            joint_holder_occupation = UserUtils.checkParem(joint_holder_occupation);
            joint_holder_income = UserUtils.checkParem(joint_holder_income);
            joint_holder_source_wealth = UserUtils.checkParem(joint_holder_source_wealth);
            joint_holder_address_type = UserUtils.checkParem(joint_holder_address_type);
            joint_holder_political = UserUtils.checkParem(joint_holder_political);
            joint_holder_place_birth1 = UserUtils.checkParem(joint_holder_place_birth1);
            joint_holder_country_birth1 = UserUtils.checkParem(joint_holder_country_birth1);
            joint_holder_occupation1 = UserUtils.checkParem(joint_holder_occupation1);
            joint_holder_income1 = UserUtils.checkParem(joint_holder_income1);
            joint_holder_source_wealth1 = UserUtils.checkParem(joint_holder_source_wealth1);
            joint_holder_address_type1 = UserUtils.checkParem(joint_holder_address_type1);
            joint_holder_political1 = UserUtils.checkParem(joint_holder_political1);
            number_of_nominee = UserUtils.checkParem(number_of_nominee);
            number_of_nominee_desc = UserUtils.checkParem(number_of_nominee_desc);
            nominee_type = UserUtils.checkParem(nominee_type);
            nominee_type_desc = UserUtils.checkParem(nominee_type_desc);
            nominee1_name = UserUtils.checkParem(nominee1_name);
            nominee1_dob = UserUtils.checkParem(nominee1_dob);
            nominee1_address1 = UserUtils.checkParem(nominee1_address1);
            nominee1_address2 = UserUtils.checkParem(nominee1_address2);
            nominee1_address3 = UserUtils.checkParem(nominee1_address3);
            nominee1_pincode = UserUtils.checkParem(nominee1_pincode);
            nominee1_city = UserUtils.checkParem(nominee1_city);
            nominee1_state = UserUtils.checkParem(nominee1_state);
            nominee1_state_code = UserUtils.checkParem(nominee1_state_code);
            nominee1_country = UserUtils.checkParem(nominee1_country);
            nominee1_id_type = UserUtils.checkParem(nominee1_id_type);
            nominee1_id_no = UserUtils.checkParem(nominee1_id_no);
            nominee1_email = UserUtils.checkParem(nominee1_email);
            nominee1_mobile = UserUtils.checkParem(nominee1_mobile);
            nominee1_relation = UserUtils.checkParem(nominee1_relation);
            nominee1_guard_name = UserUtils.checkParem(nominee1_guard_name);
            nominee1_guard_pan = UserUtils.checkParem(nominee1_guard_pan);
            nominee1_guard_relationship = UserUtils.checkParem(nominee1_guard_relationship);
            nominee1_percentage = UserUtils.checkParem(nominee1_percentage);
            nominee2_type = UserUtils.checkParem(nominee2_type);
            nominee2_type_desc = UserUtils.checkParem(nominee2_type_desc);
            nominee2_name = UserUtils.checkParem(nominee2_name);
            nominee2_dob = UserUtils.checkParem(nominee2_dob);
            nominee2_relation = UserUtils.checkParem(nominee2_relation);
            nominee2_percentage = UserUtils.checkParem(nominee2_percentage);
            nominee2_address1 = UserUtils.checkParem(nominee2_address1);
            nominee2_address2 = UserUtils.checkParem(nominee2_address2);
            nominee2_address3 = UserUtils.checkParem(nominee2_address3);
            nominee2_pincode = UserUtils.checkParem(nominee2_pincode);
            nominee2_city = UserUtils.checkParem(nominee2_city);
            nominee2_state = UserUtils.checkParem(nominee2_state);
            nominee2_state_code = UserUtils.checkParem(nominee2_state_code);
            nominee2_country = UserUtils.checkParem(nominee2_country);
            nominee2_id_type = UserUtils.checkParem(nominee2_id_type);
            nominee2_id_no = UserUtils.checkParem(nominee2_id_no);
            nominee2_email = UserUtils.checkParem(nominee2_email);
            nominee2_mobile = UserUtils.checkParem(nominee2_mobile);
            nominee2_guard_name = UserUtils.checkParem(nominee2_guard_name);
            nominee2_guard_pan = UserUtils.checkParem(nominee2_guard_pan);
            nominee2_guard_relationship = UserUtils.checkParem(nominee2_guard_relationship);
            nominee3_type = UserUtils.checkParem(nominee3_type);
            nominee3_type_desc = UserUtils.checkParem(nominee3_type_desc);
            nominee3_name = UserUtils.checkParem(nominee3_name);
            nominee3_dob = UserUtils.checkParem(nominee3_dob);
            nominee3_relation = UserUtils.checkParem(nominee3_relation);
            nominee3_percentage = UserUtils.checkParem(nominee3_percentage);
            nominee3_address1 = UserUtils.checkParem(nominee3_address1);
            nominee3_address2 = UserUtils.checkParem(nominee3_address2);
            nominee3_address3 = UserUtils.checkParem(nominee3_address3);
            nominee3_pincode = UserUtils.checkParem(nominee3_pincode);
            nominee3_city = UserUtils.checkParem(nominee3_city);
            nominee3_state = UserUtils.checkParem(nominee3_state);
            nominee3_state_code = UserUtils.checkParem(nominee3_state_code);
            nominee3_country = UserUtils.checkParem(nominee3_country);
            nominee3_id_type = UserUtils.checkParem(nominee3_id_type);
            nominee3_id_no = UserUtils.checkParem(nominee3_id_no);
            nominee3_email = UserUtils.checkParem(nominee3_email);
            nominee3_mobile = UserUtils.checkParem(nominee3_mobile);
            nominee3_guard_name = UserUtils.checkParem(nominee3_guard_name);
            nominee3_guard_pan = UserUtils.checkParem(nominee3_guard_pan);
            nominee3_guard_relationship = UserUtils.checkParem(nominee3_guard_relationship);
            networth_dob = UserUtils.checkParem(networth_dob);
            networth_amount = UserUtils.checkParem(networth_amount);
            occupation_other = UserUtils.checkParem(occupation_other);
            source_wealth_other = UserUtils.checkParem(source_wealth_other);
            joint_holder_occupation_other = UserUtils.checkParem(joint_holder_occupation_other);
            joint_source_wealth_other = UserUtils.checkParem(joint_source_wealth_other);
            joint_holder_occupation_other1 = UserUtils.checkParem(joint_holder_occupation_other1);
            joint_source_wealth_other1 = UserUtils.checkParem(joint_source_wealth_other1);
            alter_mobile = UserUtils.checkParem(alter_mobile);
            alter_email = UserUtils.checkParem(alter_email);
            inv_category = UserUtils.checkParem(inv_category);
            gaurd_relation_proof = UserUtils.checkParem(gaurd_relation_proof);
            residence_phone = UserUtils.checkParem(residence_phone);
            office_phone = UserUtils.checkParem(office_phone);
            bank_proof = UserUtils.checkParem(bank_proof);
            nominee1_guard_dob = UserUtils.checkParem(nominee1_guard_dob);
            nominee2_guard_dob = UserUtils.checkParem(nominee2_guard_dob);
            nominee3_guard_dob = UserUtils.checkParem(nominee3_guard_dob);
            mobile_isd_code = UserUtils.checkParem(mobile_isd_code);
            joint_holder_mobile1_isd_code = UserUtils.checkParem(joint_holder_mobile1_isd_code);
            joint_holder_mobile2_isd_code = UserUtils.checkParem(joint_holder_mobile2_isd_code);
            arn_number = UserUtils.checkParem(arn_number);
            nominee_soa = UserUtils.checkParem(nominee_soa);
            nominee_opt_flag = UserUtils.checkParem(nominee_opt_flag);

            if(!nominee_soa.isEmpty())
            {
                nominee_soa = nominee_soa;
            }else{
                nominee_soa = "N";
            }

            User userMain = userRepository.findById(Integer.parseInt(userid)).orElse(null);

            if (userMain == null)
            {
                return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
            }

//            UsersOnlineRegDetails user2 = usersOnlineRegDetailsRepository.findByUserId(Integer.valueOf(userid)).orElse(null);
//
//            if (user2 == null)
//            {
//                return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
//            }
            client_name = userMain.getClient_name();

            String euin = "";
            if(!arn_number.isEmpty())
            {
                BseNseKey list = bseNseKeyRepository.findByClientName(client_name);

                String broker_code1 = list.getBrokerCode();
                if(broker_code1 == null){broker_code1 = "";};

                euin = list.getEuin();
                euin = euin.split(",")[0];
            }

//          Integer userId = user2.getUser_id();
//          String clientName = user2.getClient_name();
//          String taxStatusCode = user2.getTax_status_code();
//          String holdingNatureCode = user2.getHolding_nature_code();

            UsersOnlineRegDetails user = userOnlineRegDetailsRespository.getUserBseNseDetailsByAllFieldsFOrNse(Integer.valueOf(userid), client_name, tax_status, holding_nature, arn_number, "NSE").orElse(new UsersOnlineRegDetails());

            String iin_number_new = checkNseIinNumber.checkNseMultipleRegistrationIinNumbers(user.getClient_name(), arn_number);
            try{
                user.setOnline_flag("NSE");

                if(StringHelper.isNotEmpty(iin_number))
                {
                    user.setNse_iin_number(iin_number);
                }else{
                    user.setNse_iin_number(iin_number_new);
                }

                user.setUser_id(Integer.parseInt(userid));
                user.setPan(pan);
                user.setName(name);
                user.setEmail(email);
                user.setMobile(mobile);
                user.setAlter_email(alter_email);
                user.setAlter_mobile(alter_mobile);
                user.setMobile_relation(mobile_relation);
                user.setEmail_relation(email_relation);
                user.setDate_of_birth(dob);
                user.setFather_name(father_name);
                user.setPhone_office(office_phone);
                user.setPhone_residence(residence_phone);
                user.setInv_category(inv_category);
                user.setPlace_of_birth(place_birth);
                user.setCountry_of_birth(country_birth);
                user.setCountry_birth_code(country_birth_code);
                user.setOccupation(occupation);
                user.setOccupation_code(occupation_code);
                if(occupation_code.equalsIgnoreCase("99") && !occupation_other.isEmpty())
                {
                    user.setOccupation(occupation_other);
                }
                user.setAnnual_income(income);
                user.setAnnual_income_code(income_code);
                user.setSource_of_wealth(source_wealth);
                user.setSource_of_wealth_code(source_wealth_code);
                if(source_wealth_code.equalsIgnoreCase("08") && !source_wealth_other.isEmpty())
                {
                    user.setSource_of_wealth(source_wealth_other);
                }
                user.setPolitical_code(political_status);
                if(political_status.equalsIgnoreCase("Y") || political_status.equalsIgnoreCase("PEP"))
                {
                    user.setPolitical("I am Politically exposed person");
                }
                if(political_status.equalsIgnoreCase("R") || political_status.equalsIgnoreCase("RPEP"))
                {
                    user.setPolitical("I am related to Politically exposed person");
                }
                if(political_status.equalsIgnoreCase("N") || political_status.equalsIgnoreCase("NA"))
                {
                    user.setPolitical("Not Applicable");
                }

                user.setPincode(pincode);
                user.setCity(city);
                user.setState(state);
                user.setCountry(country);
                user.setStreet_1(address1);
                user.setStreet_2(address2);
                user.setStreet_3(address3);
                user.setState_code(state_code);


                user.setGuard_name(guard_name);
                user.setGuard_pan(guard_pan);
                user.setGuard_dob(guard_dob);
                user.setGuard_relationship(gaurd_relation);
                user.setGuard_relation_proof(gaurd_relation_proof);
                user.setTax_status_code(tax_status);
                user.setTax_status(tax_status_des);
                user.setJoint_holder_name1(joint_holder_name);
                user.setJoint_holder_name2(joint_holder_name1);
                user.setJoint_holder_dob1(joint_holder_dob);
                user.setJoint_holder_dob2(joint_holder_dob1);
                user.setJoint_holder_email1(joint_holder_email);
                user.setJoint_holder_email2(joint_holder_email1);
                user.setJoint_holder_email_relation1(joint_holder_email_relation);
                user.setJoint_holder_email_relation2(joint_holder_email_relation1);
                user.setJoint_holder_mobile1(joint_holder_mobile);
                user.setJoint_holder_mobile2(joint_holder_mobile1);
                user.setJoint_holder_mobile2_isd_code(joint_holder_mobile2_isd_code);
                user.setJoint_holder_mobile_relation1(joint_holder_mobile_relation);
                user.setJoint_holder_mobile_relation2(joint_holder_mobile_relation1);
                user.setJoint_holder_pan1(joint_holder_pan);
                user.setJoint_holder_pan2(joint_holder_pan1);
                user.setHolding_nature_code(holding_nature);
                user.setHolding_nature(holding_nature_desc);
                user.setGender(gender);
                user.setNri_address1(nri_address1);
                user.setNri_address2(nri_address2);
                user.setNri_address3(nri_address3);
                user.setNri_city(nri_city);
                user.setNri_state(nri_state);
                user.setNri_pincode(nri_pincode);
                user.setNri_country(nri_country);
                user.setAddress_type_code(address_type);
                user.setAddress_type(address_type_desc);

                user.setJoint_holder_place_of_birth1(joint_holder_place_birth);
                user.setJoint_holder_place_of_birth2(joint_holder_place_birth1);
                user.setJoint_holder_country_birth_code1(joint_holder_country_birth);
                user.setJoint_holder_country_birth_code2(joint_holder_country_birth1);
                user.setJoint_holder_occupation_code1(joint_holder_occupation);

                user.setJoint_holder_occupation_code2(joint_holder_occupation1);
                user.setJoint_holder_source_of_wealth_code1(joint_holder_source_wealth);
                user.setJoint_holder_source_of_wealth_code2(joint_holder_source_wealth1);
                user.setJoint_holder_annual_income_code1(joint_holder_income);
                user.setJoint_holder_annual_income_code2(joint_holder_income1);
                user.setJoint_holder_address_type_code1(joint_holder_address_type);
                user.setJoint_holder_address_type_code2(joint_holder_address_type1);
                user.setJoint_holder_political_code1(joint_holder_political);
                user.setJoint_holder_political_code2(joint_holder_political1);

                user.setJoint_holder_email_relation1(joint_holder_email_relation);
                user.setJoint_holder_email_relation2(joint_holder_email_relation1);
                user.setJoint_holder_mobile_relation1(joint_holder_mobile_relation);
                user.setJoint_holder_mobile_relation2(joint_holder_mobile_relation1);

                user.setBroker_code(arn_number);
                user.setEuin(euin);

                user.setNetworth_amount(networth_amount);
                user.setNetworth_dob(networth_dob);
                user.setNse_customer(0);
                user.setNse_active(0);
                user.setRegister_source("Website");
                user.setClient_name(client_name);
                user.setCreated_date(new Date());

                user = userOnlineRegDetailsRespository.save(user);
            }catch (DataIntegrityViolationException e)
            {
                String message = "Duplicate entry found";

                Throwable rootCause = e.getRootCause();

                if (rootCause != null && rootCause.getMessage() != null)
                {
                    String errorMessage = rootCause.getMessage();

                    if (errorMessage.contains("Duplicate entry"))
                    {
                        int endIndex = errorMessage.indexOf("for key");

                        if (endIndex > 0)
                        {
                            message = errorMessage.substring(0, endIndex).trim();
                        }
                        else
                        {
                            message = errorMessage;
                        }
                    }
                }
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(message);
            }

            UsersNomineeDetails nomineeDetails = usersNomineeDetailsRepository.getUsersNomineeDetailsByOnlineId(user.getId(), client_name);
            if(nomineeDetails == null){
                nomineeDetails = new UsersNomineeDetails();
            }
            nomineeDetails.setNominee_opt(nominee_opt_flag);
            nomineeDetails.setNominee_soa(nominee_soa);
            nomineeDetails.setNominee1_type(nominee_type);
            nomineeDetails.setNominee1_guard_name(nominee1_guard_name);
            nomineeDetails.setNominee1_guard_pan(nominee1_guard_pan);
            nomineeDetails.setNominee2_type(nominee2_type);
            nomineeDetails.setNominee2_guard_name(nominee2_guard_name);
            nomineeDetails.setNominee2_guard_pan(nominee2_guard_pan);
            nomineeDetails.setNominee3_type(nominee3_type);
            nomineeDetails.setNominee3_guard_name(nominee3_guard_name);
            nomineeDetails.setNominee3_guard_pan(nominee3_guard_pan);
            nomineeDetails.setNumber_of_nominee(number_of_nominee);
            nomineeDetails.setNominee1_name(nominee1_name);
            nomineeDetails.setNominee1_dob(nominee1_dob);
            nomineeDetails.setNominee1_address1(nominee1_address1);
            nomineeDetails.setNominee1_address2(nominee1_address2);
            nomineeDetails.setNominee1_address3(nominee1_address3);
            nomineeDetails.setNominee1_pincode(nominee1_pincode);
            nomineeDetails.setNominee1_city(nominee1_city);
            nomineeDetails.setNominee1_state(nominee1_state);
            nomineeDetails.setNominee1_state_code(nominee1_state_code);
            nomineeDetails.setNominee1_country(nominee1_country);
            nomineeDetails.setNominee1_email(nominee1_email);
            nomineeDetails.setNominee1_mobile(nominee1_mobile);
            nomineeDetails.setNominee1_id_no(nominee1_id_no);
            nomineeDetails.setNominee1_id_type(nominee1_id_type);
            nomineeDetails.setNominee1_relation(nominee1_relation);
            nomineeDetails.setNominee1_percentage(nominee1_percentage);
            nomineeDetails.setNominee2_name(nominee2_name);
            nomineeDetails.setNominee2_dob(nominee2_dob);
            nomineeDetails.setNominee2_percentage(nominee2_percentage);
            nomineeDetails.setNominee2_relation(nominee2_relation);
            nomineeDetails.setNominee2_address1(nominee2_address1);
            nomineeDetails.setNominee2_address2(nominee2_address2);
            nomineeDetails.setNominee2_address3(nominee2_address3);
            nomineeDetails.setNominee2_pincode(nominee2_pincode);
            nomineeDetails.setNominee2_city(nominee2_city);
            nomineeDetails.setNominee2_state(nominee2_state);
            nomineeDetails.setNominee2_state_code(nominee2_state_code);
            nomineeDetails.setNominee2_country(nominee2_country);
            nomineeDetails.setNominee2_email(nominee2_email);
            nomineeDetails.setNominee2_mobile(nominee2_mobile);
            nomineeDetails.setNominee2_id_no(nominee2_id_no);
            nomineeDetails.setNominee2_id_type(nominee2_id_type);
            nomineeDetails.setNominee3_name(nominee3_name);
            nomineeDetails.setNominee3_dob(nominee3_dob);
            nomineeDetails.setNominee3_percentage(nominee3_percentage);
            nomineeDetails.setNominee3_relation(nominee3_relation);
            nomineeDetails.setNominee3_address1(nominee3_address1);
            nomineeDetails.setNominee3_address2(nominee3_address2);
            nomineeDetails.setNominee3_address3(nominee3_address3);
            nomineeDetails.setNominee3_pincode(nominee3_pincode);
            nomineeDetails.setNominee3_city(nominee3_city);
            nomineeDetails.setNominee3_state(nominee3_state);
            nomineeDetails.setNominee3_state_code(nominee3_state_code);
            nomineeDetails.setNominee3_country(nominee3_country);
            nomineeDetails.setNominee3_email(nominee3_email);
            nomineeDetails.setNominee3_mobile(nominee3_mobile);
            nomineeDetails.setNominee3_id_no(nominee3_id_no);
            nomineeDetails.setNominee3_id_type(nominee3_id_type);
            nomineeDetails.setNominee1_guard_dob(nominee1_guard_dob);
            nomineeDetails.setNominee2_guard_dob(nominee2_guard_dob);
            nomineeDetails.setNominee3_guard_dob(nominee3_guard_dob);
            nomineeDetails.setUser_id(Integer.parseInt(userid));
            nomineeDetails.setOnline_id(user.getId());
            nomineeDetails.setOnline_flag("NSE");
            nomineeDetails.setOnline_code(user.getNse_iin_number());
            nomineeDetails.setBroker_code(arn_number);
            nomineeDetails.setClient_name(client_name);
            nomineeDetails.setCreated_date(new Date());
            usersNomineeDetailsRepository.save(nomineeDetails);

            UsersBankDetails bankDetails = usersBankDetailsRepository.getUsersBankDetailsByOnlineId(user.getId(), client_name);
            if(bankDetails == null){
                bankDetails = new UsersBankDetails();
            }
            bankDetails.setBank_ifsc_code(ifsc_code);
            bankDetails.setBank_micr_code(micr_code);
            bankDetails.setBank_name(bank_name);
            bankDetails.setBank_branch(branch_name);
            bankDetails.setBank_address(bank_address);
            bankDetails.setBank_account_number(account_number);
            bankDetails.setBank_account_holder_name(account_holder_name);
            bankDetails.setBank_account_type(account_type);
            bankDetails.setBank_proof(bank_proof);
            bankDetails.setUser_id(Integer.parseInt(userid));
            bankDetails.setOnline_id(user.getId());
            bankDetails.setOnline_flag("NSE");
            bankDetails.setOnline_code(user.getNse_iin_number());
            bankDetails.setBroker_code(arn_number);
            bankDetails.setClient_name(client_name);
            bankDetails.setCreated_date(new Date());
            usersBankDetailsRepository.save(bankDetails);

            String ipAddr = UserUtils.getIpAddr(request);
            if(ipAddr == null){ipAddr="";}

            String logmsg = name+" did NSE Create Customer. Details:";
            logmsg += "userid: "+userid+",";
            logmsg += "pan: "+pan+",";
            logmsg += "name: "+name+",";
            logmsg += "email: "+email+",";
            logmsg += "mobile: "+mobile+",";
            logmsg += "tax_status: "+tax_status+",";
            logmsg += "holding_nature: "+holding_nature+",";
            logmsg += "bank_name: "+bank_name+",";
            logmsg += "account_number: "+account_number+",";
            logmsg += "nominee1_name: "+nominee1_name+",";
            logmsg += "nominee1_relation: "+nominee1_relation;

            logService.saveLog(client_name, Integer.parseInt(userid), name, mobile, "NSE Create Multiple Customer", "NSE Create Customer", logmsg , ipAddr);

            return UserUtils.successResponse(String.valueOf(user.getId()), HttpStatus.OK);

        }catch (Throwable ex)
        {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }

    @Operation(
            summary = "Get OnBoarding Status",
            description = "Retrieves the current onboarding status of a user for a specified client.\n"
                    + "Provides step-wise completion data including investor, contact, bank, nominee, and joint holder info.\n"
                    + "Also returns the selected onboarding platform (NSE/BSE/MFU) to help web and mobile apps guide the user through the registration flow."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Success Response",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = OnboardingResponse.class),
                            examples = @ExampleObject(
                                    name = "SuccessExample",
                                    summary = "Successful onboarding status response",
                                    value = """
                        {
                          "status": 200,
                          "success": "Success",
                          "message": "Fetched onboarding details successfully",
                          "user_id": 1234,
                          "client_name": "JohnDoe",
                          "vendor": "NSE",
                          "title": "NSE MF Invest Platform",
                          "logo": "https://yourcdn.com/images/nse.png",
                          "tax_status": "Individual",
                          "holding_nature": "Single",
                          "investor_info": true,
                          "personal_info": true,
                          "contact_info": true,
                          "nri_info": false,
                          "joint_holder_info": false,
                          "nomiee_info": false,
                          "bank_info": true,
                          "signature_info": true,
                          "has_nominee": true,
                          "has_nri": false,
                          "has_joint_holder": false,
                          "is_all_steps_completed": false,
                          "is_all_registration_completed": false,
                          "menu_list": [
                            {
                              "title": "Investor Info",
                              "completed": true,
                              "enabled": true
                            },
                            {
                              "title": "Personal Info",
                              "completed": true,
                              "enabled": true
                            },
                            {
                              "title": "Contact Info",
                              "completed": true,
                              "enabled": true
                            },
                            {
                              "title": "Nominee Info",
                              "completed": false,
                              "enabled": true
                            },
                            {
                              "title": "Bank Info",
                              "completed": true,
                              "enabled": true
                            }
                          ]
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                        {
                          "status": 400,
                          "error": "Bad Request",
                          "message": "Invalid client name"
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                        {
                          "status": 500,
                          "error": "Internal Server Error",
                          "message": "Internal server error: NullPointerException"
                        }
                        """
                            )
                    )
            )
    })
    @GetMapping("/getOnBoardingStatusForModifyUcc")
    public ResponseEntity<?> getOnBoardingStatusForModifyUcc(@RequestHeader("Authorization") String token, @RequestParam String brokercode, @RequestParam String iin_number,@RequestParam String online_reg_id)
    {
        String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
        Integer userId = Integer.parseInt(userIdFromToken);
        OnboardingResponse pojo = new OnboardingResponse();
        List<BseNseMfuResponse> bseNseMfuList = new ArrayList<>();
        BseNseMfuResponse obj = null;
        MymfboxOnboarding existing = null;
        try
        {
            System.out.println("userId = " + userId);
            Optional<User> userOpt = userRepository.findUSerByIdAndActive(userId);
            System.out.println("userOpt = " + userOpt);

            if (userOpt.isPresent())
            {
                User user = userOpt.get();

                existing = onboardingService.getOrCreateOnboardingbyid(Integer.valueOf(online_reg_id),user.getClient_name());
                if(existing == null)
                {
                    existing = onboardingService.getOrCreateOnboardingbyidAndClientName(Integer.valueOf(online_reg_id),user.getClient_name());
                }
                System.out.println("existing = " + existing);
                UserDto userDetail = null;
                if(existing == null)
                {
                    System.out.println("brokercode = " + brokercode);
                    System.out.println("iin_number = " + iin_number);

                    UsersOnlineRegDetails userDetails = userOnlineRegDetailsRespository.findNseByIinNumberAndBrokercodeAndId(iin_number,brokercode,user.getClient_name(),online_reg_id);
                    if(userDetails != null)
                    {
                        UserDto dtos = new UserDto();
                        BeanUtils.copyProperties(userDetails, dtos);
                        userDetail = dtos;
                    }

                    System.out.println("userDetail = " + userDetail);
                    MymfboxOnboarding onboarding = new MymfboxOnboarding();
                    onboarding.setUser_id(userDetail.getId());
                    onboarding.setClient_name(userDetail.getClient_name());
                    onboarding.setVendor("NSE");
                    onboarding.setNse_already_reg_diff_arn(false);
                    onboarding.setTax_status(userDetail.getTax_status_code());
                    onboarding.setHolding_nature(userDetail.getHolding_nature_code());

                    if(Arrays.asList("01","24","21","61","62").contains(userDetail.getTax_status_code()))
                    {
                        onboarding.setHas_nominee(true);
                        onboarding.setNomiee_info(true);
                    } else
                    {
                        onboarding.setHas_nominee(false);
                        onboarding.setNomiee_info(false);
                    }

                    if(Arrays.asList("AS","JO").contains(userDetail.getHolding_nature_code()))
                    {
                        onboarding.setHas_joint_holder(true);
                        onboarding.setJoint_holder_info(true);
                    } else
                    {
                        onboarding.setHas_joint_holder(false);
                        onboarding.setJoint_holder_info(false);
                    }

                    if(Arrays.asList("24","21","26","28","61","62").contains(userDetail.getTax_status_code()))
                    {
                        onboarding.setHas_nri(true);
                        onboarding.setNri_info(true);
                    } else
                    {
                        onboarding.setHas_nri(false);
                        onboarding.setNri_info(false);
                    }

                    onboarding.setInvestor_info(true);
                    onboarding.setPersonal_info(true);
                    onboarding.setContact_info(true);
                    onboarding.setBank_info(true);
                    onboarding.setIs_multiple_registration(false);
                    onboardingService.saveOnboarding(onboarding);

                    pojo.setVendor("NSE");
                    pojo.setTitle("NSE MF Invest Platform");
                    pojo.setLogo(vendorLogoPath + UserUtils.getVendorImage("NSE"));
                    pojo.setUser_id(user.getId());
                    pojo.setClient_name(user.getClient_name());
                    pojo.setTax_status(onboarding.getTax_status());
                    pojo.setHolding_nature(onboarding.getHolding_nature());
                    pojo.setSignature_info(false);
                    pojo.setHas_nominee(onboarding.getHas_nominee());
                    pojo.setHas_nri(onboarding.getHas_nri());
                    pojo.setHas_joint_holder(onboarding.getHas_joint_holder());
                    pojo.setIs_all_steps_completed(true);
                    pojo.setIs_all_registration_completed(false);
                    pojo.setIs_multiple_registration(false);
                    pojo.setInvestor_info(onboarding.getInvestor_info());
                    pojo.setPersonal_info(onboarding.getPersonal_info());
                    pojo.setContact_info(onboarding.getContact_info());
                    pojo.setNri_info(onboarding.getNri_info());
                    pojo.setJoint_holder_info(onboarding.getJoint_holder_info());
                    pojo.setBank_info(onboarding.getBank_info());
                    pojo.setNomiee_info(onboarding.getNomiee_info());

                    obj = new BseNseMfuResponse();
                    obj.setTitle("Investor Information");
                    obj.setCompleted(onboarding.getInvestor_info());
                    obj.setEnabled(onboarding.getInvestor_info());
                    bseNseMfuList.add(obj);

                    obj = new BseNseMfuResponse();
                    obj.setTitle("Personal Info");
                    obj.setCompleted(onboarding.getPersonal_info());
                    obj.setEnabled(onboarding.getPersonal_info());
                    bseNseMfuList.add(obj);

                    obj = new BseNseMfuResponse();
                    obj.setTitle("Contact Info");
                    obj.setCompleted(onboarding.getContact_info());
                    obj.setEnabled(onboarding.getContact_info());
                    bseNseMfuList.add(obj);

                    obj = new BseNseMfuResponse();
                    obj.setTitle("Nominee Info");
                    obj.setCompleted(onboarding.getNomiee_info());
                    obj.setEnabled(onboarding.getNomiee_info());
                    bseNseMfuList.add(obj);

                    obj = new BseNseMfuResponse();
                    obj.setTitle("Bank Details");
                    obj.setCompleted(onboarding.getBank_info());
                    obj.setEnabled(onboarding.getBank_info());
                    bseNseMfuList.add(obj);
                    pojo.setMenu_list(bseNseMfuList);

                    if(onboarding.getNri_info() == true) {
                        obj = new BseNseMfuResponse();
                        obj.setTitle("NRI Info");
                        obj.setCompleted(onboarding.getNri_info());
                        obj.setEnabled(onboarding.getNri_info());
                        bseNseMfuList.add(obj);
                        pojo.setMenu_list(bseNseMfuList);
                    }

                    if(onboarding.getJoint_holder_info() == true) {
                        obj = new BseNseMfuResponse();
                        obj.setTitle("Joint Holder Info");
                        obj.setCompleted(onboarding.getJoint_holder_info());
                        obj.setEnabled(onboarding.getJoint_holder_info());
                        bseNseMfuList.add(obj);
                        pojo.setMenu_list(bseNseMfuList);
                    }
                }else
                {
                    pojo.setVendor("NSE");
                    pojo.setTitle("NSE MF Invest Platform");
                    pojo.setLogo(vendorLogoPath + UserUtils.getVendorImage("NSE"));
                    pojo.setUser_id(user.getId());
                    pojo.setClient_name(user.getClient_name());
                    pojo.setTax_status(existing.getTax_status());
                    pojo.setHolding_nature(existing.getHolding_nature());
                    pojo.setTax_status(existing.getTax_status());
                    pojo.setHolding_nature(existing.getHolding_nature());
                    pojo.setSignature_info(false);
                    pojo.setHas_nominee(existing.getHas_nominee());
                    pojo.setHas_nri(existing.getHas_nri());
                    pojo.setHas_joint_holder(existing.getHas_joint_holder());
                    pojo.setIs_all_steps_completed(true);
                    pojo.setIs_all_registration_completed(false);
                    pojo.setIs_multiple_registration(false);
                    pojo.setInvestor_info(existing.getInvestor_info());
                    pojo.setPersonal_info(existing.getPersonal_info());
                    pojo.setContact_info(existing.getContact_info());
                    pojo.setNri_info(existing.getNri_info());
                    pojo.setJoint_holder_info(existing.getJoint_holder_info());
                    pojo.setBank_info(existing.getBank_info());
                    pojo.setNomiee_info(existing.getNomiee_info());

                    obj = new BseNseMfuResponse();
                    obj.setTitle("Investor Information");
                    obj.setCompleted(existing.getInvestor_info());
                    obj.setEnabled(existing.getInvestor_info());
                    bseNseMfuList.add(obj);

                    obj = new BseNseMfuResponse();
                    obj.setTitle("Personal Info");
                    obj.setCompleted(existing.getPersonal_info());
                    obj.setEnabled(existing.getPersonal_info());
                    bseNseMfuList.add(obj);

                    obj = new BseNseMfuResponse();
                    obj.setTitle("Contact Info");
                    obj.setCompleted(existing.getContact_info());
                    obj.setEnabled(existing.getContact_info());
                    bseNseMfuList.add(obj);

                    obj = new BseNseMfuResponse();
                    obj.setTitle("Nominee Info");
                    obj.setCompleted(existing.getNomiee_info());
                    obj.setEnabled(existing.getNomiee_info());
                    bseNseMfuList.add(obj);

                    obj = new BseNseMfuResponse();
                    obj.setTitle("Bank Details");
                    obj.setCompleted(existing.getBank_info());
                    obj.setEnabled(existing.getBank_info());
                    bseNseMfuList.add(obj);
                    pojo.setMenu_list(bseNseMfuList);

                    if(existing.getNri_info() == true) {
                        obj = new BseNseMfuResponse();
                        obj.setTitle("NRI Info");
                        obj.setCompleted(existing.getNri_info());
                        obj.setEnabled(existing.getNri_info());
                        bseNseMfuList.add(obj);
                        pojo.setMenu_list(bseNseMfuList);
                    }

                    if(existing.getJoint_holder_info() == true) {
                        obj = new BseNseMfuResponse();
                        obj.setTitle("Joint Holder Info");
                        obj.setCompleted(existing.getJoint_holder_info());
                        obj.setEnabled(existing.getJoint_holder_info());
                        bseNseMfuList.add(obj);
                        pojo.setMenu_list(bseNseMfuList);
                    }
                }
            }

            pojo.setStatus(HttpStatus.OK.value());
            pojo.setSuccess("Success");
            pojo.setMessage("Fetched onboarding details successfully");
            return ResponseEntity.status(HttpStatus.OK).body(pojo);
        }
        catch (Throwable ex)
        {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error: " + ex.getMessage());
        }
    }

    @GetMapping("/getOnBoardingStatusForMultipleReg")
    public ResponseEntity<?> getOnBoardingStatusForMultipleReg(@RequestHeader("Authorization") String token)
    {
        String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
        Integer userId = Integer.parseInt(userIdFromToken);
        OnboardingResponse pojo = new OnboardingResponse();
        List<BseNseMfuResponse> bseNseMfuList = new ArrayList<>();
        BseNseMfuResponse obj = null;
        MymfboxOnboarding onboarding = null;
        try
        {
            System.out.println("userId = " + userId);
            Optional<User> userOpt = userRepository.findUSerByIdAndActive(userId);
            System.out.println("userOpt = " + userOpt);

            if (userOpt.isPresent())
            {
                User user = userOpt.get();

                onboardingService.deleteExistingOnboardings(user.getId(),user.getClient_name());

                onboarding = onboardingService.getOrCreateOnboardingbyidMulti(user.getId(),user.getClient_name());
                System.out.println("onboarding = " + onboarding);
                if(onboarding != null)
                {
                    pojo.setTitle("NSE MF Invest Platform");
                    pojo.setLogo(vendorLogoPath + UserUtils.getVendorImage("NSE"));
                    pojo.setVendor("NSE");
                    pojo.setUser_id(user.getId());
                    pojo.setClient_name(user.getClient_name());
                    pojo.setTax_status(onboarding.getTax_status());
                    pojo.setHolding_nature(onboarding.getHolding_nature());
                    pojo.setInvestor_info(onboarding.getInvestor_info());
                    pojo.setPersonal_info(onboarding.getPersonal_info());
                    pojo.setContact_info(onboarding.getContact_info());
                    pojo.setNri_info(onboarding.getNri_info());
                    pojo.setJoint_holder_info(onboarding.getJoint_holder_info());
                    pojo.setNomiee_info(onboarding.getNomiee_info());
                    pojo.setBank_info(onboarding.getBank_info());
                    pojo.setSignature_info(onboarding.getSignature_info());
                    pojo.setHas_nominee(onboarding.getHas_nominee());
                    pojo.setHas_nri(onboarding.getHas_nri());
                    pojo.setHas_joint_holder(onboarding.getHas_joint_holder());
                    pojo.setIs_all_steps_completed(onboarding.getIs_all_steps_completed());
                    pojo.setIs_all_registration_completed(onboarding.getIs_registration_completed());

                    if(onboarding.getInvestor_info())
                    {
                        obj = new BseNseMfuResponse();
                        obj.setTitle("Investor Information");
                        obj.setCompleted(true);
                        obj.setEnabled(true);
                        bseNseMfuList.add(obj);
                    }else
                    {
                        obj = new BseNseMfuResponse();
                        obj.setTitle("Investor Information");
                        obj.setCompleted(false);
                        obj.setEnabled(true);
                        bseNseMfuList.add(obj);
                    }

                    if(onboarding.getPersonal_info())
                    {
                        obj = new BseNseMfuResponse();
                        obj.setTitle("Personal Info");
                        obj.setCompleted(true);
                        obj.setEnabled(true);
                        bseNseMfuList.add(obj);
                    }else
                    {
                        if(onboarding.getInvestor_info())
                        {
                            obj = new BseNseMfuResponse();
                            obj.setTitle("Personal Info");
                            obj.setCompleted(false);
                            obj.setEnabled(true);
                            bseNseMfuList.add(obj);
                        }else
                        {
                            obj = new BseNseMfuResponse();
                            obj.setTitle("Personal Info");
                            obj.setCompleted(false);
                            obj.setEnabled(false);
                            bseNseMfuList.add(obj);
                        }
                    }

                    if(onboarding.getContact_info())
                    {
                        obj = new BseNseMfuResponse();
                        obj.setTitle("Contact Info");
                        obj.setCompleted(true);
                        obj.setEnabled(true);
                        bseNseMfuList.add(obj);
                    }
                    else
                    {
                        if(onboarding.getPersonal_info())
                        {
                            obj = new BseNseMfuResponse();
                            obj.setTitle("Contact Info");
                            obj.setCompleted(false);
                            obj.setEnabled(true);
                            bseNseMfuList.add(obj);
                        }
                        else
                        {
                            obj = new BseNseMfuResponse();
                            obj.setTitle("Contact Info");
                            obj.setCompleted(false);
                            obj.setEnabled(false);
                            bseNseMfuList.add(obj);
                        }
                    }

                    if(onboarding.getHas_nominee())
                    {
                        if(onboarding.getNomiee_info())
                        {
                            obj = new BseNseMfuResponse();
                            obj.setTitle("Nominee Info");
                            obj.setCompleted(true);
                            obj.setEnabled(true);
                            bseNseMfuList.add(obj);
                        }
                        else
                        {
                            if(onboarding.getContact_info())
                            {
                                obj = new BseNseMfuResponse();
                                obj.setTitle("Nominee Info");
                                obj.setCompleted(false);
                                obj.setEnabled(true);
                                bseNseMfuList.add(obj);
                            }
                            /*else if(onboarding.getPersonal_info())
                            {
                                obj = new BseNseMfuResponse();
                                obj.setTitle("Nominee Info");
                                obj.setCompleted(false);
                                obj.setEnabled(true);
                                bseNseMfuList.add(obj);
                            }*/
                            else
                            {
                                obj = new BseNseMfuResponse();
                                obj.setTitle("Nominee Info");
                                obj.setCompleted(false);
                                obj.setEnabled(false);
                                bseNseMfuList.add(obj);
                            }

                        }
                    }
                    if(onboarding.getHas_nri())
                    {
                        if(onboarding.getNri_info())
                        {
                            obj = new BseNseMfuResponse();
                            obj.setTitle("NRI Info");
                            obj.setCompleted(true);
                            obj.setEnabled(true);
                            bseNseMfuList.add(obj);
                        }
                        else
                        {
                            if(onboarding.getHas_nominee())
                            {
                                if(onboarding.getNomiee_info())
                                {
                                    obj = new BseNseMfuResponse();
                                    obj.setTitle("NRI Info");
                                    obj.setCompleted(false);
                                    obj.setEnabled(true);
                                    bseNseMfuList.add(obj);
                                }
                                else
                                {
                                    obj = new BseNseMfuResponse();
                                    obj.setTitle("NRI Info");
                                    obj.setCompleted(false);
                                    obj.setEnabled(false);
                                    bseNseMfuList.add(obj);
                                }
                            }
                            else if(onboarding.getContact_info())
                            {
                                obj = new BseNseMfuResponse();
                                obj.setTitle("NRI Info");
                                obj.setCompleted(false);
                                obj.setEnabled(true);
                                bseNseMfuList.add(obj);
                            }
                            else
                            {
                                obj = new BseNseMfuResponse();
                                obj.setTitle("NRI Info");
                                obj.setCompleted(false);
                                obj.setEnabled(false);
                                bseNseMfuList.add(obj);
                            }
                        }
                    }

                    if(onboarding.getHas_joint_holder())
                    {
                        if(onboarding.getJoint_holder_info())
                        {
                            obj = new BseNseMfuResponse();
                            obj.setTitle("Joint Holder Info");
                            obj.setCompleted(true);
                            obj.setEnabled(true);
                            bseNseMfuList.add(obj);
                        }
                        else
                        {
                            if(onboarding.getHas_nominee())
                            {
                                if(onboarding.getNomiee_info())
                                {
                                    obj = new BseNseMfuResponse();
                                    obj.setTitle("Joint Holder Info");
                                    obj.setCompleted(false);
                                    obj.setEnabled(true);
                                    bseNseMfuList.add(obj);
                                }
                                else
                                {
                                    obj = new BseNseMfuResponse();
                                    obj.setTitle("Joint Holder Info");
                                    obj.setCompleted(false);
                                    obj.setEnabled(false);
                                    bseNseMfuList.add(obj);
                                }
                            }
                            else if(onboarding.getHas_nri())
                            {
                                if(onboarding.getNri_info())
                                {
                                    obj = new BseNseMfuResponse();
                                    obj.setTitle("Joint Holder Info");
                                    obj.setCompleted(false);
                                    obj.setEnabled(true);
                                    bseNseMfuList.add(obj);
                                }
                                else
                                {
                                    obj = new BseNseMfuResponse();
                                    obj.setTitle("Joint Holder Info");
                                    obj.setCompleted(false);
                                    obj.setEnabled(false);
                                    bseNseMfuList.add(obj);
                                }
                            }
                            else if(onboarding.getContact_info())
                            {
                                obj = new BseNseMfuResponse();
                                obj.setTitle("Joint Holder Info");
                                obj.setCompleted(false);
                                obj.setEnabled(true);
                                bseNseMfuList.add(obj);
                            }
                            else
                            {
                                obj = new BseNseMfuResponse();
                                obj.setTitle("Joint Holder Info");
                                obj.setCompleted(false);
                                obj.setEnabled(false);
                                bseNseMfuList.add(obj);
                            }
                        }
                    }
                    if(onboarding.getBank_info())
                    {
                        obj = new BseNseMfuResponse();
                        obj.setTitle("Bank Details");
                        obj.setCompleted(true);
                        obj.setEnabled(true);
                        bseNseMfuList.add(obj);
                    }
                    else
                    {

                        if(onboarding.getHas_joint_holder())
                        {
                            if(onboarding.getJoint_holder_info())
                            {
                                obj = new BseNseMfuResponse();
                                obj.setTitle("Bank Details");
                                obj.setCompleted(false);
                                obj.setEnabled(true);
                                bseNseMfuList.add(obj);
                            }
                            else
                            {
                                obj = new BseNseMfuResponse();
                                obj.setTitle("Bank Details");
                                obj.setCompleted(false);
                                obj.setEnabled(false);
                                bseNseMfuList.add(obj);
                            }
                        }
                        else if(onboarding.getHas_nri())
                        {
                            if(onboarding.getNri_info())
                            {
                                obj = new BseNseMfuResponse();
                                obj.setTitle("Bank Details");
                                obj.setCompleted(false);
                                obj.setEnabled(true);
                                bseNseMfuList.add(obj);
                            }
                            else
                            {
                                obj = new BseNseMfuResponse();
                                obj.setTitle("Bank Details");
                                obj.setCompleted(false);
                                obj.setEnabled(false);
                                bseNseMfuList.add(obj);
                            }
                        }
                        else if(onboarding.getHas_nominee())
                        {
                            if(onboarding.getNomiee_info())
                            {
                                obj = new BseNseMfuResponse();
                                obj.setTitle("Bank Details");
                                obj.setCompleted(false);
                                obj.setEnabled(true);
                                bseNseMfuList.add(obj);
                            }
                            else
                            {
                                obj = new BseNseMfuResponse();
                                obj.setTitle("Bank Details");
                                obj.setCompleted(false);
                                obj.setEnabled(false);
                                bseNseMfuList.add(obj);
                            }
                        }
                        else if(onboarding.getContact_info())
                        {
                            obj = new BseNseMfuResponse();
                            obj.setTitle("Bank Details");
                            obj.setCompleted(false);
                            obj.setEnabled(true);
                            bseNseMfuList.add(obj);
                        }
                        else if(onboarding.getPersonal_info())
                        {
                            obj = new BseNseMfuResponse();
                            obj.setTitle("Bank Details");
                            obj.setCompleted(false);
                            obj.setEnabled(true);
                            bseNseMfuList.add(obj);
                        }
                        else
                        {
                            obj = new BseNseMfuResponse();
                            obj.setTitle("Bank Details");
                            obj.setCompleted(false);
                            obj.setEnabled(false);
                            bseNseMfuList.add(obj);
                        }
                    }
                    pojo.setMenu_list(bseNseMfuList);
                }
                else
                {
                    onboarding = new MymfboxOnboarding();
                    onboarding.setUser_id(user.getId());
                    onboarding.setClient_name(user.getClient_name());
                    onboarding.setVendor("NSE");
                    onboarding.setNse_already_reg_diff_arn(false);
                    onboarding.setIs_multiple_registration(true);
                    onboardingService.saveOnboarding(onboarding);

                    pojo.setVendor("NSE");
                    pojo.setTitle("NSE MF Invest Platform");
                    pojo.setLogo(vendorLogoPath + UserUtils.getVendorImage("NSE"));
                    pojo.setUser_id(user.getId());
                    pojo.setClient_name(user.getClient_name());
                    pojo.setTax_status("");
                    pojo.setHolding_nature("");
                    pojo.setInvestor_info(false);
                    pojo.setPersonal_info(false);
                    pojo.setContact_info(false);
                    pojo.setNri_info(false);
                    pojo.setJoint_holder_info(false);
                    pojo.setNomiee_info(false);
                    pojo.setBank_info(false);
                    pojo.setSignature_info(false);
                    pojo.setHas_nominee(false);
                    pojo.setHas_nri(false);
                    pojo.setHas_joint_holder(false);
                    pojo.setIs_all_steps_completed(false);
                    pojo.setIs_all_registration_completed(false);

                    obj = new BseNseMfuResponse();
                    obj.setTitle("Investor Information");
                    obj.setCompleted(false);
                    obj.setEnabled(true);
                    bseNseMfuList.add(obj);

                    obj = new BseNseMfuResponse();
                    obj.setTitle("Personal Info");
                    obj.setCompleted(false);
                    obj.setEnabled(false);
                    bseNseMfuList.add(obj);

                    obj = new BseNseMfuResponse();
                    obj.setTitle("Contact Info");
                    obj.setCompleted(false);
                    obj.setEnabled(false);
                    bseNseMfuList.add(obj);

                    obj = new BseNseMfuResponse();
                    obj.setTitle("Nominee Info");
                    obj.setCompleted(false);
                    obj.setEnabled(false);
                    bseNseMfuList.add(obj);

                    obj = new BseNseMfuResponse();
                    obj.setTitle("Bank Details");
                    obj.setCompleted(false);
                    obj.setEnabled(false);
                    bseNseMfuList.add(obj);
                    pojo.setMenu_list(bseNseMfuList);
                }
            }

            pojo.setStatus(HttpStatus.OK.value());
            pojo.setSuccess("Success");
            pojo.setMessage("Fetched onboarding details successfully");
            return ResponseEntity.status(HttpStatus.OK).body(pojo);
        }
        catch (Throwable ex)
        {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Get Investor Info",
            description = "Fetches the Investor Details of the logged-in user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved Investor information",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InvestorInfoDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping("/getInvestorInfoForModifyUcc")
    public ResponseEntity<?> getInvestorInfoForModifyUcc(@RequestHeader("Authorization") String token,@RequestParam String tax_status,@RequestParam String holding_nature,@RequestParam String broker_code,@RequestParam String investor_code)
    {
        try
        {
            String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            Integer userId = Integer.parseInt(userIdFromToken);

            String clientName = TokenInterceptor.extractClientNamedFromToken(token, secretKey);

            Optional<User> userOpt = userRepository.findUSerByIdAndActive(userId);

            if (userOpt.isEmpty())
            {
                return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
            }

            User user = userOpt.get();
            UsersOnlineRegDetails userDetails = null;

//            MymfboxOnboarding onboarding = onboardingService.getOrCreateOnboardingbyid(Integer.valueOf(online_reg_id), user.getClient_name());
//
//            if (onboarding == null)
//            {
//                return UserUtils.errorResponse("Onboarding details not found.", HttpStatus.INTERNAL_SERVER_ERROR);
//            }

            InvestorInfoDTO investorInfo = null;

            Optional<UsersOnlineRegDetails> userDetailsOpt =
                    userOnlineRegDetailsRespository.getUserBseNseDetailsByAllFieldsForNse(
                            userId,
                            clientName,
                            tax_status,
                            holding_nature,
                            broker_code,
                            investor_code
                    );

            if(userDetailsOpt.isPresent())
            {
                userDetails = userDetailsOpt.get();
            }else
            {
                userDetails = new UsersOnlineRegDetails();
                userDetails.setPan(user.getPan());
                userDetails.setBroker_code(user.getBroker_code());
            }

            investorInfo = InvestorInfoMapper.mapBseNseDetailsToDto(userDetails);

            return ResponseEntity.ok(investorInfo);
        }catch(Exception ex)
        {
            logger.error("Error while saving personal information", ex);
            return UserUtils.errorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation
            (
                    summary = "Save Investor Information, Specifically created for Mobile App",
                    description =
                            "Saves investor information submitted via the mobile application.\n\n" +
                                    "This endpoint captures essential investor details required for onboarding and KYC validation. " +
                                    "It is intended specifically for mobile app users and stores information such as PAN number, " +
                                    "broker code, investor code, tax status, and holding nature.\n\n" +
                                    "Fields Collected:\n" +
                                    "- **pan :** PAN number of the investor (e.g., ABCDE1234F)\n" +
                                    "- **brokerCode :** AMFI-registered broker code (e.g., ARN-77441)\n" +
                                    "- **investorCode :** NSE IIN Number (e.g., INV1001)\n" +
                                    "- **taxStatusCode and taxStatusDesc :** Code and description of investor's tax status (e.g., 01, Individual)\n" +
                                    "- **holdingNatureCode and holdingNatureDesc :** Code and description of the investment holding nature (e.g., 01, Single)\n\n" +
                                    "A successful response indicates that the investor information has been saved.\n" +
                                    "If validation fails or required data is missing, an appropriate error response is returned.",

                    requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Investor information to be saved", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = InvestorInfoDTO.class)))
            )
    @ApiResponses(value =
            {
                    @ApiResponse(responseCode = "200", description = "Success Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SuccessResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Failure Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            })

    @PostMapping("/saveInvestorInfoForModifyUcc")
    public ResponseEntity<?> saveInvestorInfoForModifyUcc(@RequestBody InvestorInfoDTO dto, @RequestHeader("Authorization") String token)
    {
        try
        {

            String client_name = TokenInterceptor.extractClientNamedFromToken(token, secretKey);
            if (dto == null)
            {
                return UserUtils.errorResponse("Investor cannot be empty", HttpStatus.BAD_REQUEST);
            }

            String error = UserValidate.validateInvestorInfo(dto);

            if (error != null)
            {
                return UserUtils.errorResponse(error, HttpStatus.BAD_REQUEST);
            }

            PanKYCResponse panStatus = UserUtils.checkPanKycStatus(dto.getPan());

            if(panStatus != null && !panStatus.getKyc_status())
            {
                return UserUtils.errorResponse(panStatus.getMsg(), HttpStatus.BAD_REQUEST);
            }

            String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            Integer userId = Integer.parseInt(userIdFromToken);

            Optional<UsersOnlineRegDetails> userOpt =
                    userOnlineRegDetailsRespository.getUserBseNseDetailsByAllFieldsForNse(
                            userId,
                            client_name,
                            dto.getTaxStatusCode(),
                            dto.getHoldingNatureCode(),
                            dto.getBrokerCode(),
                            dto.getInvestorCode()
                    );
            UsersOnlineRegDetails userDetails = null;
            if(userOpt != null && userOpt.isPresent())
            {
                userDetails = userOpt.get();
            }else
            {
                return UserUtils.errorResponse("Ucc Details not found.", HttpStatus.NOT_FOUND);
            }

            userDetails.setOnline_flag("NSE");
            userDetails = InvestorInfoMapper.mapDtoToUserBseNseDetails(dto, userDetails);

            usersOnlineRegDetailsService.saveOrUpdateUserOnlineReg(userDetails);
            return UserUtils.successResponse("Investor information saved successfully.", HttpStatus.OK);

        }catch(Throwable ex)
        {
            logger.error("Error while saving investor information", ex);
            return UserUtils.errorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/savePersonalInfoForModifyUcc")
    public ResponseEntity<?> savePersonalInfoForModifyUcc(@RequestBody PersonalInfoDTO dto, @RequestHeader("Authorization") String token)
    {
        try
        {

            String client_name = TokenInterceptor.extractClientNamedFromToken(token, secretKey);
            if (dto == null)
            {
                return UserUtils.errorResponse("Investor cannot be empty", HttpStatus.BAD_REQUEST);
            }

            String error = UserValidate.validatePersonalInfo(dto);

            if (error != null)
            {
                return UserUtils.errorResponse(error, HttpStatus.BAD_REQUEST);
            }


            String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            Integer userId = Integer.parseInt(userIdFromToken);

            Optional<UsersOnlineRegDetails> userOpt =
                    userOnlineRegDetailsRespository.getUserBseNseDetailsByAllFieldsForNse(
                            userId,
                            client_name,
                            dto.getTaxStatusCode(),
                            dto.getHoldingNatureCode(),
                            dto.getBrokerCode(),
                            dto.getInvestorCode()
                    );

            UsersOnlineRegDetails userEntity;

            if (userOpt.isPresent())
            {
                userEntity = userOpt.get();

                PersonalInfoMapper.dtoToUser(dto, userEntity);
                userOnlineRegDetailsRespository.save(userEntity);
            }

            return UserUtils.successResponse("Investor information saved successfully.", HttpStatus.OK);

        }catch(Throwable ex)
        {
            logger.error("Error while saving investor information", ex);
            return UserUtils.errorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/saveNriInfoForModifyUcc")
    public ResponseEntity<?> saveNriInfoForModifyUcc(@RequestBody NriInfoDTO dto,@RequestParam String tax_status_code,@RequestParam String holding_nature_code,@RequestParam String broker_code,@RequestParam String investor_code, @RequestHeader("Authorization") String token)
    {
        try
        {

            String client_name = TokenInterceptor.extractClientNamedFromToken(token, secretKey);
            if (dto == null)
            {
                return UserUtils.errorResponse("Investor cannot be empty", HttpStatus.BAD_REQUEST);
            }

            String error = UserValidate.validateNriInfo(dto);

            if (error != null)
            {
                return UserUtils.errorResponse(error, HttpStatus.BAD_REQUEST);
            }


            String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            Integer userId = Integer.parseInt(userIdFromToken);

            Optional<UsersOnlineRegDetails> userOpt =
                    userOnlineRegDetailsRespository.getUserBseNseDetailsByAllFieldsForNse(
                            userId,
                            client_name,
                            tax_status_code,
                            holding_nature_code,
                            broker_code,
                            investor_code
                    );

            UsersOnlineRegDetails userEntity;

            if (userOpt.isPresent())
            {
                userEntity = userOpt.get();

                NriInfoMapper.dtoToUser(dto, userEntity);
                userOnlineRegDetailsRespository.save(userEntity);
            }

            return UserUtils.successResponse("Investor information saved successfully.", HttpStatus.OK);

        }catch(Throwable ex)
        {
            logger.error("Error while saving investor information", ex);
            return UserUtils.errorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/saveContactInfoForModifyUcc")
    public ResponseEntity<?> saveContactInfoForModifyUcc(@RequestBody ContactInfoDTO dto,@RequestParam String tax_status_code,@RequestParam String holding_nature_code,@RequestParam String broker_code,@RequestParam String investor_code, @RequestHeader("Authorization") String token)
    {
        try
        {

            String client_name = TokenInterceptor.extractClientNamedFromToken(token, secretKey);
            if (dto == null)
            {
                return UserUtils.errorResponse("Investor cannot be empty", HttpStatus.BAD_REQUEST);
            }

            String error = UserValidate.validateContactInfo(dto);

            if (error != null)
            {
                return UserUtils.errorResponse(error, HttpStatus.BAD_REQUEST);
            }


            String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            Integer userId = Integer.parseInt(userIdFromToken);

            Optional<UsersOnlineRegDetails> userOpt =
                    userOnlineRegDetailsRespository.getUserBseNseDetailsByAllFieldsForNse(
                            userId,
                            client_name,
                            tax_status_code,
                            holding_nature_code,
                            broker_code,
                            investor_code
                    );

            UsersOnlineRegDetails userEntity;

            if (userOpt.isPresent())
            {
                userEntity = userOpt.get();

                ContactInfoMapper.dtoToUser(dto, userEntity);
                userOnlineRegDetailsRespository.save(userEntity);

            }

            return UserUtils.successResponse("Investor information saved successfully.", HttpStatus.OK);

        }catch(Throwable ex)
        {
            logger.error("Error while saving investor information", ex);
            return UserUtils.errorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/saveNomineeInfoForModifyUcc")
    public ResponseEntity<?> saveNomineeInfoForModifyUcc(@RequestBody List<NomineeInfoDTO> dto,@RequestParam String tax_status_code,@RequestParam String holding_nature_code,@RequestParam String broker_code,@RequestParam String investor_code, @RequestHeader("Authorization") String token)
    {
        try
        {

            String client_name = TokenInterceptor.extractClientNamedFromToken(token, secretKey);
            if (dto == null)
            {
                return UserUtils.errorResponse("Investor cannot be empty", HttpStatus.BAD_REQUEST);
            }

            String error = UserValidate.validateNomineeInfo(dto);

            if (error != null)
            {
                return UserUtils.errorResponse(error, HttpStatus.BAD_REQUEST);
            }


            String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            Integer userId = Integer.parseInt(userIdFromToken);

            Optional<UsersNomineeDetails> userOpt =
                    usersNomineeDetailsRepository.findByUseridAndClientNameAndClientCode(
                            userId,
                            client_name,
                            investor_code,
                            broker_code
                    );

            UsersNomineeDetails userEntity;

            if (userOpt.isPresent())
            {
                userEntity = userOpt.get();

                NomineeInfoMapper.dtoToUser(dto, userEntity);
                usersNomineeDetailsRepository.save(userEntity);
            }

            return UserUtils.successResponse("Investor information saved successfully.", HttpStatus.OK);

        }catch(Throwable ex)
        {
            logger.error("Error while saving investor information", ex);
            return UserUtils.errorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/saveJointHolderInfoForModifyUcc")
    public ResponseEntity<?> saveJointHolderInfoForModifyUcc(@RequestBody List<JointHolderInfoDTO> dto,@RequestParam String tax_status_code,@RequestParam String holding_nature_code,@RequestParam String broker_code,@RequestParam String investor_code, @RequestHeader("Authorization") String token)
    {
        try
        {

            String client_name = TokenInterceptor.extractClientNamedFromToken(token, secretKey);
            if (dto == null)
            {
                return UserUtils.errorResponse("Investor cannot be empty", HttpStatus.BAD_REQUEST);
            }

            String error = UserValidate.validateJointHolderInfo(dto);
            if (error != null)
            {
                return UserUtils.errorResponse(error, HttpStatus.BAD_REQUEST);
            }


            String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            Integer userId = Integer.parseInt(userIdFromToken);

            Optional<UsersOnlineRegDetails> userOpt =
                    userOnlineRegDetailsRespository.getUserBseNseDetailsByAllFieldsForNse(
                            userId,
                            client_name,
                            tax_status_code,
                            holding_nature_code,
                            broker_code,
                            investor_code
                    );

            UsersOnlineRegDetails userEntity;

            if (userOpt.isPresent())
            {
                userEntity = userOpt.get();

                JoinHolderInfoMapper.dtoToUser(dto, userEntity);
                userOnlineRegDetailsRespository.save(userEntity);

            }

            return UserUtils.successResponse("Investor information saved successfully.", HttpStatus.OK);

        }catch(Throwable ex)
        {
            logger.error("Error while saving investor information", ex);
            return UserUtils.errorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/saveBankInfoForModifyUcc")
    public ResponseEntity<?> saveBankInfoForModifyUcc(@RequestBody BankInfoDTO dto,@RequestParam String tax_status_code,@RequestParam String holding_nature_code,@RequestParam String broker_code,@RequestParam String investor_code, @RequestHeader("Authorization") String token)
    {
        try
        {

            String client_name = TokenInterceptor.extractClientNamedFromToken(token, secretKey);
            if (dto == null)
            {
                return UserUtils.errorResponse("Investor cannot be empty", HttpStatus.BAD_REQUEST);
            }

            String error = UserValidate.validateBankInfo(dto);

            if (error != null)
            {
                return UserUtils.errorResponse(error, HttpStatus.BAD_REQUEST);
            }


            String userIdFromToken = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            Integer userId = Integer.parseInt(userIdFromToken);
            UsersOnlineRegDetails userDetails = null;
            List<UsersOnlineRegDetails> userDetailsOpt = userOnlineRegDetailsRespository.findByUseridAndClientName(userId, client_name);

            if(userDetailsOpt.isEmpty())
            {
                return UserUtils.errorResponse("User not found", HttpStatus.NOT_FOUND);
            }

            userDetails = userDetailsOpt.get(0);

            List<UsersBankDetails> usersBankDetailsOpt = usersBankDetailsRepository.findByUserIdAndClientName(userId, client_name);

            UsersBankDetails userBankDetails = null;
            if(!usersBankDetailsOpt.isEmpty())
            {
                userBankDetails = usersBankDetailsOpt.stream() .filter(bank -> bank.getBank_account_number().equals(dto.getAccountNumber())).findFirst() .orElse(null);
            }

            UsersBankDetails bankInfo = BankInfoMapper.dtoToUserBseNseDetails(dto, userBankDetails);

            bankInfo.setUser_id(userDetails.getUser_id());
            bankInfo.setOnline_flag("NSE");
            bankInfo.setOnline_code(userDetails.getNse_iin_number());
            bankInfo.setOnline_id(userDetails.getId());
            bankInfo.setBroker_code(userDetails.getBroker_code());
            bankInfo.setClient_name(userDetails.getClient_name());
            bankInfo.setCreated_date(new Date());

            usersBankDetailsRepository.save(bankInfo);

            return UserUtils.successResponse("Investor information saved successfully.", HttpStatus.OK);

        }catch(Throwable ex)
        {
            logger.error("Error while saving investor information", ex);
            return UserUtils.errorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getBankDetailsByIINNumber")
    public ResponseEntity<?> getBankDetailsByIINNumber(@RequestHeader("Authorization") String token, String client_name, String iin_number,String broker_code)
    {
        List<UsersBankDetails> list = usersBankDetailsRepository.getBankDetailsByIINNumber(broker_code,iin_number,client_name,"NSE");
        return ResponseEntity.ok(list);
    }

    @GetMapping("/getNomineeInfoByClientCodeAndBrokerCode")
    public ResponseEntity<?> getNomineeInfoByClientCodeAndBrokerCode( @RequestParam String clientCode, @RequestParam String brokerCode, @RequestParam String clientName, @RequestHeader("Authorization") String token)
    {
        try
        {
            UsersNomineeDetails nomineeDetails = usersNomineeDetailsRepository.getNomineeInfoByClientCodeAndBrokerCode(clientCode, brokerCode, "NSE", clientName);
            if(nomineeDetails == null){
                return UserUtils.errorResponse("No nominee information found.", HttpStatus.NOT_FOUND);
            }
            return ResponseEntity.ok(nomineeDetails);
        }catch(Exception ex)
        {
            logger.error("Error while saving personal information", ex);
            return UserUtils.errorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getUserRegStatus")
    public ResponseEntity<?> getUserRegStatus(@RequestHeader("Authorization") String token)
    {
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
                return UserUtils.getCommonResponse("Please provide the User Id", StatusMessage.FailureCode);
            }

            Optional<UsersOnlineRegDetails> user = userOnlineRegDetailsRespository.findNseUserByUserIdAndClientName(Integer.parseInt(user_id), client_name);

            if(user.isPresent())
            {
                Boolean isKycComplaint = null;
                Boolean showCard = false;
                String status = "";
                String title = "";
                String description = "";
                String button_text = "";
                String call_back_url = "";

                String host = customServerUrl;

                System.out.println("CAME HERE 1");

                Integer nse_customer = user.get().getNse_customer();
                Integer bse_customer = 0;
                Integer mfu_customer = 0;
                Integer nse_active = user.get().getNse_active();
                Integer bse_active = 0;
                Integer mfu_active = 0;
                String nse_iin_number = user.get().getNse_iin_number();
                String bse_client_code = "";
                String mfu_can_number = "";

                if(nse_customer == null){nse_customer = 0;}
                if(bse_customer == null){bse_customer = 0;}
                if(mfu_customer == null){mfu_customer = 0;}
                if(nse_active == null){nse_active = 0;}
                if(bse_active == null){bse_active = 0;}
                if(mfu_active == null){mfu_active = 0;}
                if(nse_iin_number == null){nse_iin_number = "";}
                if(bse_client_code == null){bse_client_code = "";}
                if(mfu_can_number == null){mfu_can_number = "";}

                MymfboxOnboarding onboarding = onboardingService.getOnboardingByUserId(user.get().getUser_id(), client_name);

                if(onboarding == null && nse_customer.equals(0))
                {
                    onboarding = new MymfboxOnboarding();
                    onboarding.setUser_id(user.get().getUser_id());
                    onboarding.setStatus(0);
                    onboarding.setClient_name(client_name);
                    onboarding.setVendor("NSE");
                    onboarding.setTax_status("");
                    onboarding.setHolding_nature("");
                    onboarding.setInv_category("");
                    onboarding.setInvestor_info(false);
                    onboarding.setPersonal_info(false);
                    onboarding.setContact_info(false);
                    onboarding.setNri_info(false);
                    onboarding.setJoint_holder_info(false);
                    onboarding.setNomiee_info(false);
                    onboarding.setBank_info(false);
                    onboarding.setSignature_info(false);
                    onboarding.setHas_nominee(false);
                    onboarding.setHas_nri(false);
                    onboarding.setHas_joint_holder(false);
                    onboarding.setIs_all_steps_completed(false);
                    onboarding.setIs_registration_completed(false);
                    onboarding.setIs_multiple_registration(false);
                    onboarding.setNse_already_reg_diff_arn(false);

                    onboardingService.saveOnboarding(onboarding);
                }

                Integer onboardingStatus = 0;

                if(onboarding != null)
                {
                    onboardingStatus = onboarding.getStatus();
                }

                if(onboardingStatus == null){onboardingStatus = 0;}

                if(nse_customer.equals(0) && bse_customer.equals(0) && mfu_customer.equals(0))
                {
                    showCard = true;
                    status = "Open";
                    title = "Open Mutual Fund Account";
                    description = "Start investing by Opening an account with us in less than 15 minutes. We will help you make sure everything hassle free and secure.";
                    button_text = "Open Now";
                    call_back_url = "";

                }else if(nse_customer.equals(1) && nse_active.equals(0) && nse_iin_number.isEmpty() && bse_customer.equals(0) && mfu_customer.equals(0))
                {
                    showCard = true;
                    status = "Progress";
                    title = "Continue Mutual Fund Account";
                    description = "Continue to Open an account with us in less than 15 minutes. We will help you make sure everything hassle free and secure.";
                    button_text = "Continue";
                    call_back_url = "";
                }else if(nse_customer.equals(0) && bse_customer.equals(1) && bse_active.equals(0) && bse_client_code.isEmpty() && mfu_customer.equals(0))
                {
                    showCard = true;
                    status = "Progress";
                    title = "Continue Mutual Fund Account";
                    description = "Continue to Open an account with us in less than 15 minutes. We will help you make sure everything hassle free and secure.";
                    button_text = "Continue";
                    call_back_url = "";
                }else if(nse_customer.equals(0) && bse_customer.equals(1) && bse_active.equals(0) && !bse_client_code.isEmpty() && onboarding != null && !onboarding.getIs_registration_completed() && mfu_customer.equals(0))
                {
                    showCard = true;
                    status = "Progress";
                    title = "Continue Mutual Fund Account";
                    description = "Continue to Open an account with us in less than 15 minutes. We will help you make sure everything hassle free and secure.";
                    button_text = "Continue";
                    call_back_url = "";
                }else if(nse_customer.equals(0) && bse_customer.equals(0) && mfu_customer.equals(1) && mfu_active.equals(0) && mfu_can_number.isEmpty())
                {
                    showCard = true;
                    status = "Progress";
                    title = "Continue Mutual Fund Account";
                    description = "Continue to Open an account with us in less than 15 minutes. We will help you make sure everything hassle free and secure.";
                    button_text = "Continue";
                    call_back_url = "";
                }else if(nse_customer.equals(1) && nse_active.equals(0) && !nse_iin_number.isEmpty() && bse_customer.equals(0) && mfu_customer.equals(0) && onboarding != null && onboarding.getIs_registration_completed())
                {
                    showCard = true;
                    status = "Pending";
                    title = "Pending Mutual Fund Account";
                    description = "You have successfully done the online registration. Please complete the authentication through the email, and your account will be activated within 1 day.";
                    button_text = "";
                    call_back_url = "";

                }else if(nse_customer.equals(1) && nse_active.equals(0) && !nse_iin_number.isEmpty() && bse_customer.equals(0) && mfu_customer.equals(0) && onboarding != null &&  !onboarding.getIs_registration_completed() && onboarding.getNse_already_reg_diff_arn())
                {
                    showCard = true;
                    status = "Pending";
                    title = "Pending Mutual Fund Account";
                    description = "You have successfully done the online registration. Please complete the authentication through the email, and your account will be activated within 1 day.";
                    button_text = "";
                    call_back_url = "";

                }else if(nse_customer.equals(0) && bse_customer.equals(1) && bse_active.equals(0) && !bse_client_code.isEmpty() && onboarding != null &&  onboarding.getIs_registration_completed() && mfu_customer.equals(0))
                {
                    showCard = true;
                    status = "Pending";
                    title = "Pending Mutual Fund Account";

                    String tax_status = user.get().getTax_status_code();

                    if(!tax_status.equalsIgnoreCase("01") && !tax_status.equalsIgnoreCase("02") && !tax_status.equalsIgnoreCase("21") && !tax_status.equalsIgnoreCase("24"))
                    {
                        description = "You have successfully done the online registration. Please complete the authentication through the email, and your account will be activated within 1 day.";
                        button_text = "";
                        call_back_url = "";
                    }else
                    {
                        description = "Your mutual fund account has been successfully created, Please click here to activate your mutual fund account.";
                        button_text = "Activate Account";
                        call_back_url = host + "/onboard/uploadBseAOF?key="+""+"&user_id="+user.get().getId()+"&bse_nse_mfu_flag=BSE&multiple_reg=0&client_name="+client_name;
                    }
                }else if(nse_customer.equals(0) && bse_customer.equals(0) && mfu_customer.equals(1) && mfu_active.equals(0) && !mfu_can_number.isEmpty() && onboarding != null &&  onboarding.getIs_registration_completed())
                {
                    showCard = true;
                    status = "Pending";
                    title = "Pending Mutual Fund Account";
                    description = "Your mutual fund account has been successfully created, Please click here to activate your mutual fund account.";
                    button_text = "Activate Account";
                    call_back_url = host + "/onboard/activateMfuCanNumber?key="+""+"&user_id="+user.get().getId()+"&bse_nse_mfu_flag=MFU&multiple_reg=0&client_name="+client_name;
                }else if(nse_customer.equals(1) && nse_active.equals(1) && !nse_iin_number.isEmpty() && bse_customer.equals(0) && mfu_customer.equals(0))
                {
                    showCard = false;
                    status = "Registered";
                    title = "";
                    description = ".";
                    button_text = "";
                    call_back_url = "";

                }else if(nse_customer.equals(0) && bse_customer.equals(1) && bse_active.equals(1) && !bse_client_code.isEmpty() && mfu_customer.equals(0))
                {
                    showCard = false;
                    status = "Registered";
                    title = "";
                    description = ".";
                    button_text = "";
                    call_back_url = "";
                }else if(nse_customer.equals(0) && bse_customer.equals(0) && mfu_customer.equals(1) && mfu_active.equals(1) && !mfu_can_number.isEmpty())
                {
                    showCard = false;
                    status = "Registered";
                    title = "";
                    description = ".";
                    button_text = "";
                    call_back_url = "";
                }else
                {
                    showCard = false;
                    status = "Registered";
                    title = "";
                    description = ".";
                    button_text = "";
                    call_back_url = "";
                }


                UserRegStatusPojo pojo = new UserRegStatusPojo();
                pojo.setShowCard(showCard);
                pojo.setStatus(status);
                pojo.setTitle(title);
                pojo.setDescription(description);
                pojo.setButton_text(button_text);
                pojo.setCall_back_url(call_back_url);

                UserRegStatusResponse apiResponse = new UserRegStatusResponse();
                apiResponse.setStatus(StatusMessage.SuccessCode);
                apiResponse.setStatus_msg(StatusMessage.SuccessMessage);
                apiResponse.setMsg(StatusMessage.SuccessMessage);
                apiResponse.setResult(pojo);
                return new ResponseEntity<UserRegStatusResponse>(apiResponse, HttpStatus.OK);

            }else
            {
                return UserUtils.getCommonResponse("User details not available.", StatusMessage.FailureCode);
            }

        }catch(Exception ex)
        {
            logger.error("Error while saving personal information", ex);
            return UserUtils.errorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getSipDays")
    public ResponseEntity<?> getSipDays(@RequestHeader("Authorization") String token,@RequestParam String start_date,@RequestParam String bse_nse_mfu_flag)
    {
        Optional<UsersOnlineRegDetails> user = null;
        List<CommonPojo> masterList = new ArrayList<CommonPojo>();
        CommonPojo pojo = null;
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

            if(StringHelper.isEmpty(bse_nse_mfu_flag))
            {
                return UserUtils.getCommonResponse("Please provide the bse_nse_mfu_flag", StatusMessage.FailureCode);
            }

            user = userOnlineRegDetailsRespository.findNseUserByUserIdAndClientName(Integer.parseInt(user_id), client_name);

            if(user.isPresent())
            {
                BseNseKey bseNseKey = bseNseKeyRepository.findByClientName(client_name);

                String vendors = UserUtils.checkParameter(bseNseKey.getNse_bse());

                if(StringHelper.isNotEmpty(vendors) && !vendors.contains(bse_nse_mfu_flag.toLowerCase()))
                {
                    return UserUtils.getCommonResponse("Client Not Available in "+bse_nse_mfu_flag.toUpperCase()+"", StatusMessage.FailureCode);
                }

                if(StringHelper.isEmpty(start_date))
                {
                    pojo = new CommonPojo();
                    pojo.setCode("02");
                    pojo.setDesc("Monday");
                    masterList.add(pojo);

                    pojo = new CommonPojo();
                    pojo.setCode("03");
                    pojo.setDesc("Tuesday");
                    masterList.add(pojo);

                    pojo = new CommonPojo();
                    pojo.setCode("04");
                    pojo.setDesc("Wednesday");
                    masterList.add(pojo);

                    pojo = new CommonPojo();
                    pojo.setCode("05");
                    pojo.setDesc("Thursday");
                    masterList.add(pojo);

                    pojo = new CommonPojo();
                    pojo.setCode("06");
                    pojo.setDesc("Friday");
                    masterList.add(pojo);
                }else
                {
                    LocalDate date = LocalDate.parse(start_date);
                    DayOfWeek dayOfWeek = date.getDayOfWeek();


                    if(String.valueOf(dayOfWeek).toLowerCase().equalsIgnoreCase("monday"))
                    {
                        pojo = new CommonPojo();
                        pojo.setCode("02");
                        pojo.setDesc("Monday");
                        masterList.add(pojo);
                    }else if(String.valueOf(dayOfWeek).toLowerCase().equalsIgnoreCase("tuesday"))
                    {
                        pojo = new CommonPojo();
                        pojo.setCode("03");
                        pojo.setDesc("Tuesday");
                        masterList.add(pojo);
                    }else if(String.valueOf(dayOfWeek).toLowerCase().equalsIgnoreCase("wednesday"))
                    {
                        pojo = new CommonPojo();
                        pojo.setCode("04");
                        pojo.setDesc("Wednesday");
                        masterList.add(pojo);
                    }else if(String.valueOf(dayOfWeek).toLowerCase().equalsIgnoreCase("thursday"))
                    {
                        pojo = new CommonPojo();
                        pojo.setCode("05");
                        pojo.setDesc("Thursday");
                        masterList.add(pojo);
                    }else if(String.valueOf(dayOfWeek).toLowerCase().equalsIgnoreCase("friday"))
                    {
                        pojo = new CommonPojo();
                        pojo.setCode("06");
                        pojo.setDesc("Friday");
                        masterList.add(pojo);
                    }
                }

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