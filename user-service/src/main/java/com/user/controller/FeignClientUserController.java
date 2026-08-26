package com.user.controller;

import com.user.config.TokenInterceptor;
import com.user.dao.UserDAO;
import com.user.dto.BankDto;
import com.user.dto.UserDto;
import com.user.dto.UserMandateDetailsDto;
import com.user.dto.UsersNseRegReportDto;
import com.user.mapper.BankInfoMapper;
import com.user.mapper.UserMapper;
import com.user.model.*;
import com.user.repository.*;
import com.user.service.*;
import com.user.utils.UserUtils;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.hibernate.internal.util.StringHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.query.Param;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;



@RestController
//@Tag(
//		name = "User Controller",
//		description = ""
//)
//@Hidden
public class FeignClientUserController
{

    @Value("${jwt.secret-key}")
    private String secretKey;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	UserService userService;

	@Autowired
	UserBseNseDetailsService userBseNseDetailsService;

	@Autowired
	UserMappingService userMappingService;

	@Autowired
	AdvisorEmailSignatureRepository advisorEmailSignatureRepository;

	@Autowired
	private BseNseKeyRepository bseNseKeyRepository;

	@Autowired
	private BseNseOnlineAccessRepository bseNseOnlineAccessRepository;

	@Autowired
	UsersPortfolioSchemewiseRepository usersPortfolioSchemewiseRepository;

	@Autowired
	InvestorMasterCamsRepository investorMasterCamsRepository;

	@Autowired
	InvestorMasterKarvyRepository investorMasterKarvyRepository;

	@Autowired
	PortfolioTransactionsRepository portfolioTransactionsRepository;

	@Autowired
	TransactionTypeRepository transactionTypeRepository;

	@Autowired
	InvestorTransactionCamsRepository investorTransactionCamsRepository;

	@Autowired
	InvestorTransactionKarvyRepository investorTransactionKarvyRepository;

    @Autowired
    BasketDetailsRepository basketDetailsRepository;

	@Autowired
	private NseTransactionService nseTransactionService;

	@Autowired
	private UsersNseRegReportService usersNseRegReportService;

	@Autowired
	UsersNseRegReportRepository usersNseRegReportRepository;

	@Autowired
	UserMandateDetailsService userMandateDetailsService;

	@Autowired
	UserDAO userDAO;

    @Autowired
    OnboardingService onboardingService;

	@Autowired
	UserOnlineRegDetailsRespository userOnlineRegDetailsRespository;
    @Autowired
    private UsersBankDetailsRepository usersBankDetailsRepository;
    @Autowired
    private UsersNomineeDetailsRepository usersNomineeDetailsRepository;

	@Autowired
	UsersMandateDetailsRespository usersMandateDetailsRespository;

	@Operation(
			summary = "Get inactive NSE UserBseNseDetails by userId",
			description = "Fetches the inactive NSE UserBseNseDetails record for the given user ID. " +
					"Validates input and returns the first matching inactive record if available. " +
					"Responds with 400 for invalid input or no data, and 500 for internal errors."
	)

	@ApiResponses(value =
	{
		@ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UsersOnlineRegDetails.class))),
		@ApiResponse(responseCode = "400", description = "Invalid User ID or No data found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
		@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
	})
	@Hidden
	@GetMapping("/getNseInactiveUserBseNseDetailsByUserId")
	public ResponseEntity<?> getNseInactiveUserBseNseDetailsByUserId(@RequestParam Integer userId)
	{
		try
		{
			if (userId == null || userId <= 0)
			{
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", HttpStatus.BAD_REQUEST, "status_msg", "Invalid User ID"));
			}

			List<UsersOnlineRegDetails> list = userOnlineRegDetailsRespository.findInactiveNseByUserId(userId);

			if (list != null && !list.isEmpty())
			{
				return ResponseEntity.ok(Map.of("status", HttpStatus.OK, "status_msg", "success", "data", list.get(0)));
			} else
			{
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", 400, "status_msg", "failure"));
			}
		}catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", 400, "status_msg", "Error occurred while fetching data"));
		}
	}


	@Operation
	(
		summary = "Get inactive NSE UserBseNseDetails by IIN Number and Client Name",
		description = "Fetches inactive NSE user details based on the provided IIN number and client name.\n" +
				"Returns user data if a match is found; otherwise, returns an appropriate error message.\n" +
				"Useful for retrieving deactivated NSE profiles during reconciliation or validation.",
		parameters =
		{
			@Parameter(name = "client_name", description = "Client name associated with the user", required = true, in = ParameterIn.QUERY),
			@Parameter(name = "iin_number", description = "NSE IIN Number of the user", required = true, in = ParameterIn.QUERY)
		}
	)
	@ApiResponses(value =
	{
		@ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UsersOnlineRegDetails.class))),
		@ApiResponse(responseCode = "400", description = "No record found for the given IIN Number and Client Name", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
		@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
	})

	@Hidden
	@GetMapping("/getUserBseNseDetailsByNseIINNumber")
	public ResponseEntity<?> getUserBseNseDetailsByIinNumber(@RequestParam String client_name, @RequestParam String iin_number)
	{
		try
		{
			System.out.println("aaaa = " + client_name + iin_number);

			Optional<UsersOnlineRegDetails> key = userOnlineRegDetailsRespository.findInactiveNseByIinNumberAndClientName(iin_number, client_name);

			if (key.isPresent())
			{
				UsersOnlineRegDetails userDetail = key.get();
				List<UsersBankDetails> bankDetails = usersBankDetailsRepository.findByUseridAndClientName(userDetail.getUser_id(), userDetail.getClient_name(), String.valueOf(userDetail.getId()));
				Optional<UsersNomineeDetails> nomineeDetails = usersNomineeDetailsRepository.findByUseridAndClientName(userDetail.getUser_id(), userDetail.getClient_name(), String.valueOf(userDetail.getId()), "NSE");
				List<UsersMandateDetails> mandateDetails = usersMandateDetailsRespository.findByUseridAndClientName(userDetail.getUser_id(), userDetail.getClient_name(), String.valueOf(userDetail.getId()));

				UserDto userDto = UserMapper.mapToUserDtoMappers(userDetail, bankDetails, mandateDetails, nomineeDetails.isPresent() ? nomineeDetails.get() : null);
				return ResponseEntity.ok(userDto);
			}
			else {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", HttpStatus.BAD_REQUEST, "status_msg", "No record found for the given IIN Number and Client Name."));
			}
		}catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR, "status_msg", "Error occurred while fetching data"));
		}
	}


	// FeignClient
	@Operation(
			summary = "Get User by ID",
			description = "Retrieves user information by their unique user ID.\n" +
					"Returns full user details if the ID is valid, or a not found error if not.\n" +
					"Typically used in user management or profile viewing functionalities.",
			parameters = {
					@Parameter(
							name = "id",
							description = "User ID",
							required = true,
							in = ParameterIn.PATH
					)
			}
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = User.class))),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
	})
	@Hidden
	@GetMapping("/getUserByIdAndActive")
	public ResponseEntity<?> getUserById(@RequestParam Integer userId) {
		try
		{
			List<UsersOnlineRegDetails> userDetails = userOnlineRegDetailsRespository.findNseUserByUserId(userId);

			if(userDetails.size() > 0)
			{
				return ResponseEntity.ok(userDetails.get(0));
			}else
			{
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", HttpStatus.BAD_REQUEST, "status_msg", "User not found"));
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR, "status_msg", "Error occurred while fetching user"));
		}
	}


	@Operation(
			summary = "Get User Details by ID and Client Name",
			description = "Fetches user details for the given user ID and client name. " +
					"Returns user information if an active record exists in the database. " +
					"Responds with 404 if not found, or 500 in case of server error.",
	parameters = {
					@Parameter(name = "clientName", description = "Client name", required = true, in = ParameterIn.QUERY),
					@Parameter(name = "userId", description = "User ID", required = true, in = ParameterIn.QUERY)
			}
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = User.class))),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
	})
	@Hidden
	@GetMapping("/getUserByIdAndClientName")
	public ResponseEntity<?> getUserByIdAndClientName(@RequestParam String clientName, @RequestParam Integer userid) {
		try
		{
			Optional<UsersOnlineRegDetails> key = userOnlineRegDetailsRespository.findNseUserByUserIdAndClientName(userid,clientName);

			if (key.isPresent())
			{
				UsersOnlineRegDetails userDetail = key.get();
				List<UsersBankDetails> bankDetails = usersBankDetailsRepository.findByUseridAndClientName(userDetail.getUser_id(), userDetail.getClient_name(), String.valueOf(userDetail.getId()));
				Optional<UsersNomineeDetails> nomineeDetails = usersNomineeDetailsRepository.findByUseridAndClientName(userDetail.getUser_id(), userDetail.getClient_name(), String.valueOf(userDetail.getId()), "NSE");
				List<UsersMandateDetails> mandateDetails = usersMandateDetailsRespository.findByUseridAndClientName(userDetail.getUser_id(), userDetail.getClient_name(), String.valueOf(userDetail.getId()));

				UserDto userDto = UserMapper.mapToUserDtoMappers(userDetail, bankDetails, mandateDetails, nomineeDetails.isPresent() ? nomineeDetails.get() : null);
				return ResponseEntity.ok(userDto);
			}
			 else {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", HttpStatus.BAD_REQUEST, "status_msg", "No record found for the given IIN Number and Client Name."));
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", 500, "status_msg", "Error occurred while fetching user details"));
		}
	}

	@Hidden
	@GetMapping("/getUserByClientNameAndId")
	public ResponseEntity<?> getUserByClientNameAndId(@RequestParam String clientName, @RequestParam Integer userid) {
		try
		{
            System.out.println("userid = " + userid);
            System.out.println("username = " + clientName);
			Optional<UsersOnlineRegDetails> key = userOnlineRegDetailsRespository.findNseUserByUserIdAndClientName(userid,clientName);
            System.out.println("key = " + key.get());
			return ResponseEntity.ok(key.get());

		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", 500, "status_msg", "Error occurred while fetching user details"));
		}
	}

	@Operation(
			summary = "Get User Details by UserBseNseDetials",
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
							array = @ArraySchema(schema = @Schema(implementation = UsersOnlineRegDetails.class))
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
	@Hidden
	@GetMapping("/getUserByIdAndClientNameAndNseActive")
	public ResponseEntity<?> getUserByIdAndClientNameAndNseActive(@RequestParam String clientName, @RequestParam Integer userid) {
		try
		{
			Optional<UsersOnlineRegDetails> key = userOnlineRegDetailsRespository.findNseUserByUserIdAndClientName(userid,clientName);
			if (key != null && key.isPresent())
			{
				return ResponseEntity.ok(key.get());
			}
			else
			{
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", 404, "status_msg", "User not found"));
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", 500, "status_msg", "Error occurred while fetching user details"));
		}
	}

	@Operation(
			summary = "Get BseNseKey by Client Name",
			description = "Fetches BseNseKey details based on the provided client name. " +
					"Looks for the key using both domain and client name fields. " +
					"Returns 404 if no match is found, or 500 on failure.",
	parameters = {
					@Parameter(name = "clientName", description = "Client name", required = true, in = ParameterIn.QUERY)
			}
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BseNseKey.class))),
			@ApiResponse(responseCode = "404", description = "Key not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
	})
	@Hidden
	@GetMapping("/getClientName")
	public ResponseEntity<?> getByClientName(@RequestParam String clientName)
	{
		try
		{
			System.out.println("Client Name: " + clientName);
			BseNseKey key = bseNseKeyRepository.findByClientName(clientName);

			System.out.println("key: " + key);
			return ResponseEntity.ok(key);

		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", 500, "status_msg", "Error occurred while fetching key"));
		}
	}

    @Hidden
    @GetMapping("/getOrCreateOnboarding")
    public ResponseEntity<?> getOrCreateOnboarding(@RequestParam Integer userid,@RequestParam String clientName)
    {
        try
        {
            System.out.println("Client Name: " + clientName);
            MymfboxOnboarding key = onboardingService.getOrCreateOnboarding(userid,clientName);

            System.out.println("key: " + key);
            return ResponseEntity.ok(key);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", 500, "status_msg", "Error occurred while fetching key"));
        }
    }

    @Hidden
    @PostMapping("/saveUserRegStatus")
    public ResponseEntity<?> saveUserRegStatus(@RequestHeader("Authorization") String token) {
        UsersOnlineRegDetails user = null;
        try {
            String userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);

            Optional<UsersOnlineRegDetails> userOpt = userService.getUserById(Integer.parseInt(userid));

            if (userOpt.isPresent()) {
                user = userOpt.get();
            }
            System.out.println("user id = " + userid);
            System.out.println("userOpt  = " + userOpt);
            System.out.println("user  = " + user);

            MymfboxOnboarding onboarding = onboardingService.getOrCreateOnboarding(user.getId(), user.getClient_name());
            System.out.println("onboarding: " + onboarding);
            if (onboarding != null) {
                onboarding.setIs_registration_completed(true);
                onboardingService.saveOnboarding(onboarding);
                return UserUtils.successResponse("Updated Successfully.", HttpStatus.OK);
            } else {
                return UserUtils.errorResponse("Error occurred while fetching user details", HttpStatus.BAD_REQUEST);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return UserUtils.errorResponse("Error occurred while fetching user details.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

	@Operation(
			summary = "Get BseNseOnlineAccess by Client Name and Broker Code",
			description = "Retrieves BseNseOnlineAccess configuration for the specified client name and broker code. " +
					"Returns the corresponding access details if present. " +
					"Responds with 404 for missing records, and 500 for internal errors.",
	parameters = {
					@Parameter(name = "clientName", description = "Client name", required = true, in = ParameterIn.QUERY),
					@Parameter(name = "brokerCode", description = "Broker code", required = true, in = ParameterIn.QUERY)
			}
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BseNseOnlineAccess.class))),
			@ApiResponse(responseCode = "404", description = "Record not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
	})
	@Hidden
	@GetMapping("/getBseNseOnlineAccess")
	public ResponseEntity<?> getBseNseOnlineAccess(@RequestParam String clientName, @RequestParam String brokerCode) {
		try
		{
			BseNseKey key = bseNseKeyRepository.findByClientNameAndBrokerCode(clientName, brokerCode);
			if (key != null)
			{
				return ResponseEntity.ok(key);
			} else {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(Map.of("status", 404, "status_msg", "Record not found"));
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("status", 500, "status_msg", "Error occurred while fetching data"));
		}
	}

    @Hidden
    @PostMapping(value = "/saveUserNseSuccessResponse", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> saveUserNseSuccessResponse(@RequestBody UserDto userDto)
    {
        try
        {
            Optional<UsersOnlineRegDetails> onlineRegDetailsList = userOnlineRegDetailsRespository.findNseUserByUserIdAndClientName(userDto.getId(), userDto.getClient_name());

			UsersOnlineRegDetails users = null;
			if(onlineRegDetailsList.isPresent())
			{
				users = onlineRegDetailsList.get();

                users.setId(userDto.getId());
                users.setNse_customer(1);
                users.setNse_iin_number(userDto.getNse_iin_number());
                users.setBroker_code(userDto.getBroker_code());
                users.setEuin(userDto.getEuin());
                users.setNse_active(1);
            }
            assert users != null;
            userOnlineRegDetailsRespository.save(users);

            return ResponseEntity.ok("User saved successfully");
        } catch (Exception e) {

            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving user: " + e.getMessage());
        }
    }

    @Hidden
    @PostMapping(value = "/saveBseNseDetails", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> saveBseNseDetails(@RequestBody UserDto userDto)
    {
        try
        {
            Optional<UsersOnlineRegDetails> existing = userOnlineRegDetailsRespository.findByAllFields(userDto.getId(),userDto.getNse_iin_number(),userDto.getClient_name());
			UsersOnlineRegDetails users = existing.get();
            System.out.println("users ====500 " + users);
            if(users!= null)
            {
                users.setUser_id(userDto.getId());
                users.setNse_customer(1);
                users.setNse_iin_number(userDto.getNse_iin_number());
                users.setBroker_code(userDto.getBroker_code());
                users.setEuin(userDto.getEuin());
                users.setNse_active(1);
            }
            System.out.println("users ====510 " + users);
			userOnlineRegDetailsRespository.save(users);

            return ResponseEntity.ok("User saved successfully");
        } catch (Exception e) {

            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving user: " + e.getMessage());
        }
    }


	@Operation(summary = "Save NSE Registration Report", description = "Saves a user's NSE registration report.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Registration report saved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
	})
	@Hidden
	@PostMapping("/saveNseRegReport")
	public ResponseEntity<?> saveNseRegReport(@RequestBody UsersNseRegReportDto dto) {
		try
		{
			usersNseRegReportService.save(dto);
			return ResponseEntity.ok(Map.of("status", HttpStatus.OK, "status_msg", "Saved successfully"));
		} catch (Exception e)
		{
			e.printStackTrace();
			return ResponseEntity.status(500).body("Failed to save registration report");
		}
	}

	@Operation(summary = "Get NSE Registration Report", description = "Fetches the latest NSE registration report based on the IIN number and client name provided.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Registration report retrieved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UsersNseRegReportDto.class))),
			@ApiResponse(responseCode = "404", description = "No report found for given IIN and client name", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
	})
	@Hidden
	@GetMapping("/getRegReport")
	public ResponseEntity<?> getRegReport(
			@RequestParam String iin_number,
			@RequestParam String client_name) {
		try
		{
			return usersNseRegReportRepository.findFirstByIin_numberAndClient_name(iin_number, client_name)
					.map(ResponseEntity::ok)
					.orElseGet(() -> ResponseEntity.notFound().build());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return ResponseEntity.status(500).body("Error retrieving report");
		}
	}


	//User Portfolio Schemewise
	@Operation(
			summary = "Get User Portfolio Scheme-wise",
			description = "Fetches distinct mutual fund scheme codes for a specific user from their portfolio.\n" +
					"Takes user_id, client_name, and amc_code as query parameters.\n" +
					"Returns a list of scheme codes if found, or a 404 error if none exist."
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Scheme codes retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "No schemes found"),
			@ApiResponse(responseCode = "500", description = "Internal Server Error")
	})
	@Hidden
	@GetMapping("/getUsersPortfolioSchemewise")
	public ResponseEntity<?> getUsersPortfolioSchemewise(
			@RequestParam Integer user_id,
			@RequestParam String client_name,
			@RequestParam String amc_code) {
		try
		{
			List<String> schemeCodes = usersPortfolioSchemewiseRepository.findDistinctSchemeCode(user_id, client_name, amc_code);
			if (schemeCodes.isEmpty())
			{
				Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("status", "error");
				errorResponse.put("message", "No schemes found for the given parameters in Users Portfolio");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
			}
			else
			{
				return ResponseEntity.ok(schemeCodes);
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving portfolio schemes");
		}
	}



	//InvestorMasterCams
	@Operation(
			summary = "Get Investor Master CAMS Data",
			description = "Retrieves investor master data from CAMS for the given user ID, client name, and AMC code.\n" +
					"Useful for fetching CAMS-linked mutual fund records associated with an investor.\n" +
					"Returns matching records or a 404 error if no data is available."
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "CAMS records retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "No CAMS records found"),
			@ApiResponse(responseCode = "500", description = "Internal Server Error")
	})
	@Hidden
	@GetMapping("/getinvestorMasterCams")
	public ResponseEntity<?> getinvestorMasterCams(
			@RequestParam Integer user_id,
			@RequestParam String client_name,
			@RequestParam String amc_code) {
		try
		{
			List<InvestorMasterCams> records = investorMasterCamsRepository.findInvestorMasterCams(user_id, client_name, amc_code);

			if (records.isEmpty())
			{
				Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("status", "error");
				errorResponse.put("message", "No schemes found for the given parameters in CAMS data");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
			}

			List<InvestorMasterCams> dtoList = records.stream().map(entity -> {
				InvestorMasterCams dto = new InvestorMasterCams();
				BeanUtils.copyProperties(entity, dto);
				return dto;
			}).collect(Collectors.toList());
			return ResponseEntity.ok(dtoList);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving CAMS data");
		}
	}

	//InvestorMasterKarvy
	@Operation(
			summary = "Get Investor Master Karvy Data",
			description = "Retrieves distinct scheme or product codes from Karvy data for a specific user.\n" +
					"Takes user_id, client_name, and amc_code as query parameters.\n" +
					"Returns a list of codes or a 404 error if none are found."
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Scheme codes retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "No schemes found"),
			@ApiResponse(responseCode = "500", description = "Internal Server Error")
	})
	@Hidden
	@GetMapping("/getinvestorMasterKarvy")
	public ResponseEntity<?> getInvestorMasterKarvy(
			@RequestParam Integer user_id,
			@RequestParam String client_name,
			@RequestParam String amc_code) {
		try
		{

			System.out.println("amc code = "+  amc_code);
			List<InvestorMasterKarvy> schemeCodes = investorMasterKarvyRepository.findByUserIdAndClientNameAndFund(user_id, client_name, amc_code);
			System.out.println("scheme = " + schemeCodes);

			if (schemeCodes.isEmpty())
			{
				Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("status", "error");
				errorResponse.put("message", "No schemes found for the given parameters in Karvy data");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
			}
			else
			{
				return ResponseEntity.ok(schemeCodes);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving Karvy data");
		}
	}



	@Operation(
			summary = "Get BSE/NSE Details by IIN and User ID",
			description = "Fetches user mandate details from BSE/NSE based on IIN number, user ID, and client name.\n" +
					"Helps validate or retrieve investment mandates linked to a specific IIN.\n" +
					"Returns mandate details or a 404 if no matching records exist."
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "User mandate details retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "No mandate details found"),
			@ApiResponse(responseCode = "500", description = "Internal Server Error")
	})
	@Hidden
	@GetMapping("/getUserBseNseDetailsByIinNumberAndUserId")
	public ResponseEntity<?> getUserBseNseDetailsByIinNumberAndUserId(
			@RequestParam String client_name,
			@RequestParam String iin_number,
			@RequestParam Integer userid) {
		try
		{
			Optional<UsersOnlineRegDetails> detailsOptional = userOnlineRegDetailsRespository.findByAllFields(
					userid, iin_number, client_name
			);
            System.out.println("detailsOptional = " + detailsOptional);

			if (detailsOptional.isPresent())
			{
				return ResponseEntity.ok(detailsOptional.get());
			}
			else
			{
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body("No mandate details found for the given parameters.");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error retrieving user mandate details");
		}
	}

	@Operation(
			summary = "Get Broker Code Linked to SIP Folio",
			description = "Retrieves the list of unique broker codes associated with a user's SIP investment folios.\n\n" +
					"### Parameters:\n" +
					"- `client_name`: The client name associated with the user.\n" +
					"- `userid`: The ID of the user (typically a numeric value).\n" +
					"- `folio` : The folio number for which broker code is to be fetched.\n\n" +
					"### Functionality:\n" +
					"- This endpoint queries the user's SIP folio and returns distinct broker codes related to that folio.\n" +
					"- Useful for displaying broker code options in SIP configuration or validation.\n" +
					"- Returns an empty list if no matching broker code is found.\n\n" +
					"### Notes for UI Developers:\n" +
					"- Use this endpoint to dynamically populate dropdowns or display broker information in the user's SIP section.\n" +
					"- All parameters are optional. If no `userid` is provided, the result may be an empty list."
	)
	@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "Successfully fetched broker code list",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							array = @ArraySchema(schema = @Schema(type = "string", example = "ARN-77441"))
					)
			),
			@ApiResponse(
					responseCode = "400",
					description = "Bad Request – Required parameters are missing or invalid",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(example = "scheme_code, amc_code, and sip_frequency are required"))
			),
			@ApiResponse(
					responseCode = "500",
					description = "Internal server error occurred while fetching data",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(example = "Error fetching SIP frequencies"))
			)
	})
	@Hidden
	@GetMapping("/getSipFolioBrokerCodeUser")
	public ResponseEntity<?> getSipFolioBrokercodeuser(@RequestParam(required = false) String client_name,
													   @RequestParam(required = false) Integer userid,
													   @RequestParam(required = false) String folio) {
		try
		{
			if(client_name == null){client_name = "";};
			if(folio == null){folio = "";};

			List<String> frequencies = usersPortfolioSchemewiseRepository.findDistinctBrokerCodeByUserIdAndClientNameAndFolioNo(userid,client_name,folio);

			return ResponseEntity.ok(frequencies);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error fetching SIP frequencies");
		}
	}

	@Operation(
			summary = "Get BSE/NSE Details by IIN and User ID",
			description = "Fetches user mandate details from BSE/NSE based on IIN number, user ID, and client name.\n" +
					"Helps validate or retrieve investment mandates linked to a specific IIN.\n" +
					"Returns mandate details or a 404 if no matching records exist."
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "User mandate details retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "No mandate details found"),
			@ApiResponse(responseCode = "500", description = "Internal Server Error")
	})
	@Hidden
	@GetMapping("/getNseUserMandateDetailsByUmrn")
	public ResponseEntity<?> getNseUserMandateDetailsByUmrn(
			@RequestParam String client_name,
			@RequestParam String iin_number,
			@RequestParam String umrn_code,
			@RequestParam Integer userid)
	{
		try
		{
			Optional<UsersMandateDetails> detailsOptional = userMandateDetailsService.getNseUserMandateDetailsByUmrn(userid, iin_number, umrn_code, client_name);

			if (detailsOptional.isPresent())
			{
				return ResponseEntity.ok(detailsOptional.get());
			} else
			{
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No mandate details found for the given parameters.");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error retrieving user mandate details");
		}
	}


	@Hidden
	@GetMapping("/getByAllFields")
	public ResponseEntity<?> getByAllFields(
			@RequestParam String clientName,
			@RequestParam String onlineFlag,
			@RequestParam String onlineCode,
			@RequestParam String bankAccountNumber,
			@RequestParam Integer userid)
	{
		try {
			List<UsersMandateDetails> detailsOptional = userMandateDetailsService.getByAllFields(
					userid, onlineFlag, onlineCode, bankAccountNumber, clientName);

			return ResponseEntity.ok(detailsOptional);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error retrieving user mandate details");
		}
	}



	@Hidden
	@GetMapping("/getAllAMCDetails")
	public ResponseEntity<?> getAllAMCDetails(
			@RequestParam String client_name,
			@RequestParam Integer userid) {
		try
		{
			List<UsersPortfolioSchemewise> details = usersPortfolioSchemewiseRepository
					.findByUserIdAndClientNameWithPositiveUnitsGroupByAmcCode(userid, client_name);
			if (!details.isEmpty())
			{
				return ResponseEntity.ok(details);
			} else
			{
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body("No AMC details found for the given parameters.");
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error retrieving AMC details");
		}
	}

	@Hidden
	@GetMapping("/getRedemptionSchemesNews")
	public ResponseEntity<?> getRedemptionSchemesNews(
			@RequestParam String clientName,
			@RequestParam Integer userid,@RequestParam String amcCode) {
		try
		{
			List<String> details = usersPortfolioSchemewiseRepository.findDistinctSchemeCodeByUserIdAndClientNameAndAmcCodeAndRegistrarNotManual(userid,clientName,amcCode);

			if (!details.isEmpty())
			{
				return ResponseEntity.ok(details);
			} else
			{
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body("No AMC details found for the given parameters.");
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error retrieving AMC details");
		}
	}

	@Hidden
	@GetMapping("/getProductCode")
	public ResponseEntity<?> GetProductCode(
			@RequestParam String clientName,
			@RequestParam Integer userid,@RequestParam String product) {
		try
		{
			List<InvestorMasterCams> details = investorMasterCamsRepository.findByUserIdClientNameAndProductIn(userid,clientName,product);

			if (!details.isEmpty())
			{
				return ResponseEntity.ok(details);
			}
			else
			{
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body("No AMC details found for the given parameters.");
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error retrieving AMC details");
		}
	}

	@Hidden
	@GetMapping("/getRedemptionKarvy")
	public ResponseEntity<?> getRedemptionKarvy(
			@RequestParam Integer userid,
			@RequestParam String clientName,
			@RequestParam String fund) {
		try
		{
			List<InvestorMasterKarvy> schemeCodes = investorMasterKarvyRepository.findByUserIdClientNameAndFund(userid, clientName, fund);

			if (schemeCodes.isEmpty())
			{
				Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("status", "error");
				errorResponse.put("message", "No schemes found for the given parameters in Karvy data");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
			} else
			{
				return ResponseEntity.ok(schemeCodes);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving Karvy data");
		}
	}

	@Hidden
	@GetMapping("/getRedemptionPortfolio")
	public ResponseEntity<?> getRedemptionPortfolio(
			@RequestParam Integer userid,
			@RequestParam String clientName,
			@RequestParam String amcCode,
			@RequestParam List schemeCodes) {
		try
		{
			List<String> schemeCode = usersPortfolioSchemewiseRepository.findDistinctAmfiCodesByConditions(amcCode,userid, clientName, schemeCodes);
			if (schemeCode.isEmpty())
			{
				Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("status", "error");
				errorResponse.put("message", "No schemes found for the given parameters in Karvy data");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
			} else
			{
				return ResponseEntity.ok(schemeCode);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving Karvy data");
		}
	}

	@Hidden
	@GetMapping("/getRedemptionHoldingUnits")
	public ResponseEntity<?> getRedemptionHoldingUnits(
			@RequestParam Integer userid,
			@RequestParam String clientName,
			@RequestParam String folio_no,
			@RequestParam String scheme_name) {
		try
		{
			List<UsersPortfolioSchemewise> schemeCode = usersPortfolioSchemewiseRepository.findByUserIdAndClientNameAndSchemeNameAndFolioNoAndRegistrarNotManual(userid, clientName,scheme_name, folio_no);

			if (schemeCode.isEmpty())
			{
				Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("status", "error");
				errorResponse.put("message", "No schemes found for the given parameters in Karvy data");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
			} else
			{
				return ResponseEntity.ok(schemeCode);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving Karvy data");
		}
	}

	@Hidden
	@GetMapping("/getUsersPortfolioSchemewiseAll")
	public ResponseEntity<?> getUsersPortfolioSchemewiseAll(
			@RequestParam Integer user_id,
			@RequestParam String client_name,
			@RequestParam String scheme_name) {
		try
		{
			System.out.println("Fetching all schemes for user_id: " + user_id + ", client_name: " + client_name + ", scheme_name: " + scheme_name);
			List<UsersPortfolioSchemewise> schemeCode = usersPortfolioSchemewiseRepository.findByUserIdAndClientNameAndSchemeNameAndRegistrarNotManual(user_id, client_name,scheme_name);

			if (schemeCode.isEmpty())
			{
				Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("status", "error");
				errorResponse.put("message", "No schemes found for the given parameters in Karvy data");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
			} else
			{
				return ResponseEntity.ok(schemeCode);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving Karvy data");
		}
	}

	@Hidden
	@GetMapping("/getinvestorMasterKarvyScheme")
	public ResponseEntity<?> getinvestorMasterKarvyScheme(
			@RequestParam Integer user_id,
			@RequestParam String client_name,
			@RequestParam String scheme_name) {
		try
		{
			List<InvestorMasterKarvy> schemeCodes = investorMasterKarvyRepository.findByUserIdAndClientNameAndProductCodeStartsWith(user_id, client_name, scheme_name);

			if (schemeCodes.isEmpty())
			{
				Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("status", "error");
				errorResponse.put("message", "No schemes found for the given parameters in Karvy data");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
			}
			else
			{
				return ResponseEntity.ok(schemeCodes);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving Karvy data");
		}
	}

	@Hidden
	@GetMapping("/getinvestorMasterKarvySchemes")
	public ResponseEntity<?> getinvestorMasterKarvySchemes(
			@RequestParam Integer user_id,
			@RequestParam String client_name,
			@RequestParam List<String> productList) {
		try
		{
			List<InvestorMasterKarvy> schemeCodes = investorMasterKarvyRepository.findByUserIdAndClientNameAndProductCode(user_id, client_name, productList);

			if (schemeCodes.isEmpty())
			{
				Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("status", "error");
				errorResponse.put("message", "No schemes found for the given parameters in Karvy data");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
			}
			else
			{
				return ResponseEntity.ok(schemeCodes);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving Karvy data");
		}
	}

	@Hidden
	@GetMapping("/getAllTransactionType")
	public ResponseEntity<?> getAllTransactionType() {
		try
		{
			List<TransactionType> schemeCodes = transactionTypeRepository.findAll();

			if (schemeCodes.isEmpty())
			{
				Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("status", "error");
				errorResponse.put("message", "No schemes found for the given parameters in Karvy data");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
			}
			else
			{
				return ResponseEntity.ok(schemeCodes);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving Karvy data");
		}
	}

	@Hidden
	@GetMapping("/getAllCamsTransaction")
	public ResponseEntity<?> getAllCamsTransaction(
			@RequestParam Integer user_id,
			@RequestParam String client_name,
			@RequestParam List<String> prodcode,
			@RequestParam String folio_no
	) {
		try
		{
			List<InvestorTransactionCams> schemeCodes = investorTransactionCamsRepository.findByUserIdClientNameProdcodeAndFolioNo(user_id, client_name, prodcode, folio_no);

			if (schemeCodes.isEmpty())
			{
				Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("status", "error");
				errorResponse.put("message", "No schemes found for the given parameters in Karvy data");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
			}
			else
			{
				return ResponseEntity.ok(schemeCodes);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving Karvy data");
		}
	}

	@Hidden
	@GetMapping("/getGroupedByProdcodeAndFolioNo")
	public ResponseEntity<?> getGroupedByProdcodeAndFolioNo(
			@RequestParam Integer user_id,
			@RequestParam String client_name
	) {
		try
		{
			List<InvestorTransactionCams> schemeCodes = investorTransactionCamsRepository.findGroupedByProdcodeAndFolioNo(user_id, client_name);

			if (schemeCodes.isEmpty())
			{
				return ResponseEntity.ok(new ArrayList<>());
			}
			else
			{
				return ResponseEntity.ok(schemeCodes);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving Karvy data");
		}
	}

	@Hidden
	@GetMapping("/getByFolioNoAndProdcodeAndUserIdAndClientName")
	public ResponseEntity<?> getByFolioNoAndProdcodeAndUserIdAndClientName(
			@RequestParam Integer user_id,
			@RequestParam String client_name,
			@RequestParam String folioNo,
			@RequestParam String prodcode
	) {
		try
		{
			List<InvestorTransactionCams> schemeCodes = investorTransactionCamsRepository.findByFolioNoAndProdcodeAndUserIdAndClientName(folioNo,prodcode,user_id, client_name);

			if (schemeCodes.isEmpty())
			{
                return ResponseEntity.ok(new ArrayList<>());
			}
			else
			{
				return ResponseEntity.ok(schemeCodes);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving Karvy data");
		}
	}


	@Hidden
	@GetMapping("/getAllKarvyTransaction")
	public ResponseEntity<?> getAllKarvyTransaction(
			@RequestParam Integer user_id,
			@RequestParam String client_name,
			@RequestParam List<String> prodcode,
			@RequestParam String folio_no
	) {
		try
		{
			List<InvestorTransactionKarvy> schemeCodes = investorTransactionKarvyRepository.findByUserIdClientNameAndProductCodesAndFolio(user_id, client_name, prodcode, folio_no);

			if (schemeCodes.isEmpty())
			{
				Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("status", "error");
				errorResponse.put("message", "No schemes found for the given parameters in Karvy data");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
			}
			else
			{
				return ResponseEntity.ok(schemeCodes);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving Karvy data");
		}
	}

	@Hidden
	@GetMapping("/getGroupedTransactions")
	public ResponseEntity<?> getGroupedTransactions(
			@RequestParam Integer user_id,
			@RequestParam String client_name
	) {
		try
		{
            System.out.println("user_id = " + user_id);
            System.out.println("client_name = " + client_name);
			List<InvestorTransactionKarvy> schemeCodes = investorTransactionKarvyRepository.findGroupedTransactions(user_id, client_name);

			if (schemeCodes.isEmpty())
			{
				Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("status", "error");
				errorResponse.put("message", "No schemes found for the given parameters in Karvy data");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
			}
			else
			{
				return ResponseEntity.ok(schemeCodes);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving Karvy data");
		}
	}

	@Hidden
	@GetMapping("/getTransactionsByFolioAndFund")
	public ResponseEntity<?> getTransactionsByFolioAndFund(
			@RequestParam Integer user_id,
			@RequestParam String client_name,
			@RequestParam String folioNumber,
			@RequestParam String fund,
			@RequestParam String schemeCode
	) {
		try
		{
			List<InvestorTransactionKarvy> schemeCodes = investorTransactionKarvyRepository.findTransactionsByFolioAndFund(folioNumber,fund,schemeCode,user_id, client_name);

			if (schemeCodes.isEmpty())
			{
				Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("status", "error");
				errorResponse.put("message", "No schemes found for the given parameters in Karvy data");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
			}
			else
			{
				return ResponseEntity.ok(schemeCodes);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving Karvy data");
		}
	}

	@Hidden
	@GetMapping("/getClientNameAndIinNumber")
	public ResponseEntity<?> getClientNameAndIinNumber(
			@RequestParam String clientName,
			@RequestParam String nseIinNumber
	) {
		try
		{

			System.out.println("fsdfasf" + clientName + nseIinNumber);
			List<UsersOnlineRegDetails> schemeCodes = userOnlineRegDetailsRespository.findByClientNameAndNseIinNumberList(clientName, nseIinNumber);

			System.out.println("schemecode" +  schemeCodes);

			if (schemeCodes.isEmpty())
			{
				Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("status", "error");
				errorResponse.put("message", "No schemes found for the given parameters in Karvy data");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
			}
			else
			{
				return ResponseEntity.ok(schemeCodes);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving Karvy data");
		}
	}
	@Hidden
	@GetMapping("/getRTADetails")
	public ResponseEntity<?> getRTADetails(@RequestParam String scheme_name)
	{
		try
		{
			Optional<UsersPortfolioSchemewise> usersPortfolioSchemewiseOpt = usersPortfolioSchemewiseRepository.findFirstBySchemeName(scheme_name);

			if (usersPortfolioSchemewiseOpt.isPresent())
			{
				return ResponseEntity.ok(usersPortfolioSchemewiseOpt.get());
			} else
			{
				Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("status", "error");
				errorResponse.put("message", "No schemes found for the given parameters in Karvy data");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving Karvy data");
		}
	}

	@Hidden
	@PostMapping("/saveUser")
	public ResponseEntity<String> saveUserFromNse(@RequestBody UsersOnlineRegDetails user) {
        System.out.println("user = " + user);
		userService.saveOrUpdateUser(user);
		return ResponseEntity.ok("User saved successfully");
	}

	@Hidden
	@PostMapping("/saveUserBseNseDetail")
	public ResponseEntity<String> saveUserBseNseDetail(@RequestBody UsersOnlineRegDetails userBseNseDetails) {
		userBseNseDetailsService.saveOrUpdateUserOnlineReg(userBseNseDetails);
		return ResponseEntity.ok("User Bse Nse Details saved successfully");
	}

	@Hidden
	@PostMapping("/saveRegDetails")
	public ResponseEntity<String> saveUserBseNseDetail(@RequestBody UsersNseRegReport usersNseRegReport) {
		usersNseRegReportService.saveorupdateRegReport(usersNseRegReport);
		return ResponseEntity.ok("User Bse Nse Details saved successfully");
	}

	@Hidden
	@GetMapping("/getInactiveNseByUserIdAndClientName")
	public ResponseEntity<?> getInactiveNseByUserIdAndClientName(
			@RequestParam(required = false) Integer userid,
			@RequestParam(required = false) String clientName) {
		try {
			Optional<UsersOnlineRegDetails> users = userOnlineRegDetailsRespository.findInactiveNseByUserIdAndClientNameInActive(userid ,clientName);
            System.out.println("user ======1379 = " + userid);
            System.out.println("user ======1379 = " + clientName);
            System.out.println("user ======1379 = " + users);
			if (users.isPresent())
            {
				return ResponseEntity.ok(users.get());
			} else {
				Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("status", "error");
				errorResponse.put("message", "No user found");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving user data");
		}
	}

    @Hidden
    @GetMapping("/getUserDetailsByPanName")
    public ResponseEntity<?> getUserDetailsByPanName(
            @RequestParam(required = false) String pan,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String clientName) {
        try {
            List<User> users = userRepository.findByPanAndNameAndClientName(pan, name, clientName);

            if (!users.isEmpty()) {
                return ResponseEntity.ok(users);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "No user found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving user data");
        }
    }

	@Hidden
	@GetMapping("/getActiveUsersByPanName")
	public ResponseEntity<?> getActiveUsersByPanName(
			@RequestParam(required = false) String pan,
			@RequestParam(required = false) String name,
			@RequestParam(required = false) String clientName) {
		try {
			List<User> users = userRepository.findByPanAndNameAndActiveSourceAndClientName(pan, name, clientName);

			if (!users.isEmpty()) {
				return ResponseEntity.ok(users);
			} else {
				Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("status", "error");
				errorResponse.put("message", "No active user found");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving active user data");
		}
	}

	@Hidden
	@GetMapping("/getDistinctSchemeAmfiCodeByUserAndAmc")
	public ResponseEntity<?> getDistinctSchemeAmfiCodeByUserAndAmc(@RequestParam String amcCode, @RequestParam Integer userid,@RequestParam String clientName)
	{
		try
		{
			List<String> detailsOptional = usersPortfolioSchemewiseRepository.findDistinctSchemeAmfiCodeByUserAndAmc(amcCode, userid,clientName);

			if (detailsOptional.size() > 0)
			{
				return ResponseEntity.ok(detailsOptional);
			} else
			{
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", HttpStatus.BAD_REQUEST, "status_msg", "No record found for the given IIN Number and Client Name."));
			}
		}catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR, "status_msg", "Error occurred while fetching data"));
		}
	}

	@Hidden
	@GetMapping("/getActiveSchemesByUserAndClient")
	public ResponseEntity<?> getActiveSchemesByUserAndClient(@RequestParam Integer userid,@RequestParam String clientName)
	{
		try
		{
			List<UsersPortfolioSchemewise> detailsOptional = usersPortfolioSchemewiseRepository.findActiveSchemesByUserAndClient(userid,clientName);

			if (detailsOptional.size() > 0)
			{
				return ResponseEntity.ok(detailsOptional);
			} else
			{
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", HttpStatus.BAD_REQUEST, "status_msg", "No record found for the given IIN Number and Client Name."));
			}
		}catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR, "status_msg", "Error occurred while fetching data"));
		}
	}

	@Hidden
	@GetMapping("/getSchemeHoldingUnitsUser")
	public ResponseEntity<?>getSchemeHoldingUnitsUser(@RequestParam Integer userid,@RequestParam String clientName,@RequestParam String scheme_name, @RequestParam String folio)
	{
		try
		{
			List<UsersPortfolioSchemewise> detailsOptional = usersPortfolioSchemewiseRepository.findByUserIdAndClientNameAndSchemeNameOrAmfiCodeAndFolioNoAndRegistrarNotManual(userid,clientName,scheme_name,folio);

			if (detailsOptional.size() > 0)
			{
				return ResponseEntity.ok(detailsOptional);
			} else
			{
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", HttpStatus.BAD_REQUEST, "status_msg", "No record found for the given IIN Number and Client Name."));
			}
		}catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR, "status_msg", "Error occurred while fetching data"));
		}
	}

	@Hidden
	@GetMapping("/getByClientNameAndFolioNoAndSchemeCode")
	public ResponseEntity<?>getByClientNameAndFolioNoAndSchemeCode(@RequestParam String clientName, @RequestParam List<String> folio)
	{
		try
		{
			List<UsersPortfolioSchemewise> detailsOptional = usersPortfolioSchemewiseRepository.findByClientNameAndFolioNosAndRegistrarNotManualOrderByFolioNoAndSchemeCode(clientName,folio);

			if (detailsOptional.size() > 0)
			{
				return ResponseEntity.ok(detailsOptional);
			} else
			{
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", HttpStatus.BAD_REQUEST, "status_msg", "No record found for the given IIN Number and Client Name."));
			}
		}catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR, "status_msg", "Error occurred while fetching data"));
		}
	}

	@Hidden
	@GetMapping("/getByCamsUserIdAndClientName")
	public ResponseEntity<?> getByCamsUserIdAndClientName(@RequestParam Integer userid,@RequestParam String clientName)
	{
		try
		{
			List<InvestorMasterCams> detailsOptional = investorMasterCamsRepository.findByCamsUserIdAndClientName(userid,clientName);

			if (detailsOptional.size() > 0)
			{
				return ResponseEntity.ok(detailsOptional);
			} else
			{
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", HttpStatus.BAD_REQUEST, "status_msg", "No record found for the given IIN Number and Client Name."));
			}
		}catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR, "status_msg", "Error occurred while fetching data"));
		}
	}

	@Hidden
	@GetMapping("/findByUserIdAndClientNameAndProductIn")
	public ResponseEntity<?> findByUserIdAndClientNameAndProductIn(@RequestParam Integer userId,@RequestParam String clientName,@RequestParam List<String> products)
	{
		try
		{
			List<InvestorMasterCams> detailsOptional = investorMasterCamsRepository.findByUserIdAndClientNameAndProductIn(userId,clientName,products);

			if (detailsOptional.size() > 0)
			{
				return ResponseEntity.ok(detailsOptional);
			} else
			{
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", HttpStatus.BAD_REQUEST, "status_msg", "No record found for the given IIN Number and Client Name."));
			}
		}catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR, "status_msg", "Error occurred while fetching data"));
		}
	}

    @Hidden
    @GetMapping("/getByuserIdAndClientnameAndTaxstatus")
    public ResponseEntity<?> getByuserIdAndClientnameAndTaxstatus(@RequestParam("userId") Integer userId,
                                                                   @RequestParam("clientName") String clientName,
                                                                   @RequestParam("tax_status") String tax_status,
                                                                   @RequestParam("holding_nature") String holding_na,
                                                                   @RequestParam("joint1_pan") String joint1_pan,
                                                                   @RequestParam("broker_code") String broker_cod)
    {
        try
        {
            List<InvestorMasterCams> detailsOptional = investorMasterCamsRepository.findByuserIdAndClientnameAndTaxstatus(userId,clientName,tax_status,holding_na,joint1_pan,broker_cod);

            if (detailsOptional.size() > 0)
            {
                return ResponseEntity.ok(detailsOptional);
            } else
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", HttpStatus.BAD_REQUEST, "status_msg", "No record found for the given IIN Number and Client Name."));
            }
        }catch (Exception ex)
        {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR, "status_msg", "Error occurred while fetching data"));
        }
    }

	@Hidden
	@GetMapping("/getByKarvyUserIdAndClientName")
	public ResponseEntity<?> getByKarvyUserIdAndClientName(@RequestParam Integer userid,@RequestParam String clientName)
	{
		try
		{
			List<InvestorMasterKarvy> detailsOptional = investorMasterKarvyRepository.findByKarvyUserIdAndClientName(userid,clientName);

			if (detailsOptional.size() > 0)
			{
				return ResponseEntity.ok(detailsOptional);
			} else
			{
                return ResponseEntity.ok(new ArrayList<>());
			}
		}catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR, "status_msg", "Error occurred while fetching data"));
		}
	}

    @Hidden
    @GetMapping("/getByKarvyUserIdAndClientNameAndTaxstatus")
    public ResponseEntity<?> getByKarvyUserIdAndClientNameAndTaxstatus(@RequestParam Integer userid,@RequestParam String clientName,@RequestParam("tax_status") String tax_status,
                                                                       @RequestParam("holding_nature") String holding_na,
                                                                       @RequestParam("joint1_pan") String joint1_pan,
                                                                       @RequestParam("broker_code") String broker_cod)
    {
        try
        {

            List<String> taxStatusList = new ArrayList<>();

            if ("INDIVIDUAL".equalsIgnoreCase(tax_status)) {
                taxStatusList.add("I");
                taxStatusList.add("1");
            } else {
                taxStatusList.add(tax_status);
            }

            List<InvestorMasterKarvy> detailsOptional = investorMasterKarvyRepository.findByKarvyUserIdAndClientNameAndTaxstatus(userid,clientName,taxStatusList,holding_na,broker_cod);

            if (detailsOptional.size() > 0)
            {
                return ResponseEntity.ok(detailsOptional);
            } else
            {
                return ResponseEntity.ok(new ArrayList<>());
            }
        }catch (Exception ex)
        {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR, "status_msg", "Error occurred while fetching data"));
        }
    }

	@Hidden
	@GetMapping("/getNseGroupedTransactions")
	public ResponseEntity<?> getNseGroupedTransactions(@RequestParam Integer userid,@RequestParam String clientName)
	{
		try
		{
			List<PortfolioTransactions> detailsOptional = portfolioTransactionsRepository.findGroupedBySchemeAndFolio(userid,clientName);

			if (detailsOptional.size() > 0)
			{
				return ResponseEntity.ok(detailsOptional);
			} else
			{
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", HttpStatus.BAD_REQUEST, "status_msg", "No record found for the given IIN Number and Client Name."));
			}
		}catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR, "status_msg", "Error occurred while fetching data"));
		}
	}

	@Hidden
	@GetMapping("/getByFolioSchemeAndUser")
	public ResponseEntity<?> getByFolioSchemeAndUser(@RequestParam Integer userid,@RequestParam String clientName,@RequestParam String folioNo,@RequestParam String schemeCode)
	{
		try
		{
			List<PortfolioTransactions> detailsOptional = portfolioTransactionsRepository.findByFolioSchemeAndUser(folioNo,schemeCode,userid,clientName);

			if (detailsOptional.size() > 0)
			{
				return ResponseEntity.ok(detailsOptional);
			} else
			{
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", HttpStatus.BAD_REQUEST, "status_msg", "No record found for the given IIN Number and Client Name."));
			}
		}catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR, "status_msg", "Error occurred while fetching data"));
		}
	}

	@Hidden
	@GetMapping("/familyMapUserList")
	public ResponseEntity<?> familyMapUserList(
			@RequestParam String clientName)
	{
		try
		{
			List<Integer> detailsOptional = userMappingService.getFamilyMembersCount(clientName);

			if (!detailsOptional.isEmpty())
			{
				return ResponseEntity.ok(detailsOptional);
			} else
			{
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No mandate details found for the given parameters.");
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR, "status_msg", "Error occurred while fetching data"));
		}
	}
	@Hidden
	@GetMapping("/getFilteredUsersByClientName")
	public ResponseEntity<?> getFilteredUsersByClientName(
			@RequestParam String clientName)
	{
		try
		{
			List<UsersMapping> detailsOptional = userMappingService.findFilteredUsersByClientName(clientName);

			if (!detailsOptional.isEmpty())
			{
				return ResponseEntity.ok(detailsOptional);
			} else
			{
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No mandate details found for the given parameters.");
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR, "status_msg", "Error occurred while fetching data"));
		}
	}

	@Hidden
	@GetMapping("/getByUserIdAndClientName")
	public ResponseEntity<?> getByUserIdAndClientName(
			@RequestParam Integer userid,
			@RequestParam String clientName)
	{
		try
		{
			List<UsersMapping> detailsOptional = userMappingService.getByUserIdAndClientName(userid,clientName);

			if (!detailsOptional.isEmpty())
			{
				return ResponseEntity.ok(detailsOptional);
			} else
			{
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No mandate details found for the given parameters.");
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR, "status_msg", "Error occurred while fetching data"));
		}
	}

	@Hidden
	@GetMapping("/aumUsersList")
	public ResponseEntity<?> aumUsersList(
			@RequestParam String clientName,
			@RequestParam List<Integer> excludedIds)
	{
		try
		{
			List<Integer> detailsOptional = userRepository.findDistinctUserIdsByClientNameAndTypeIdNotInExcludedIds(clientName,excludedIds);

			if (!detailsOptional.isEmpty())
			{
				return ResponseEntity.ok(detailsOptional);
			} else
			{
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No mandate details found for the given parameters.");
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR, "status_msg", "Error occurred while fetching data"));
		}
	}

	@Hidden
	@GetMapping("/getByIdInAndClientNameNative")
	public ResponseEntity<?> getByIdInAndClientNameNative(
			@RequestParam String clientName,
			@RequestParam List<Integer> ids)
	{
		try
		{
			List<User> detailsOptional = userRepository.findByIdInAndClientNameNative(ids,clientName);

			if (!detailsOptional.isEmpty())
			{
				return ResponseEntity.ok(detailsOptional);
			} else
			{
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No mandate details found for the given parameters.");
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR, "status_msg", "Error occurred while fetching data"));
		}
	}


	@Hidden
	@GetMapping("/getUserListByDynamicQuery")
	public ResponseEntity<?> getUserListByDynamicQuery(@RequestParam String query)
	{
		try
		{
			List<User> userList = userDAO.getUserListByDynamicQuery(query);
			if(userList != null &&!userList.isEmpty())
			{
				return ResponseEntity.ok(userList);
			}else {
				return ResponseEntity.ok(new ArrayList<>());
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR, "status_msg", "Error occurred while fetching data"));
		}
	}

	@Hidden
	@GetMapping("/getByUserIdAndBseClientCodeAndClientName")
	public ResponseEntity<?> getByUserIdAndBseClientCodeAndClientName(@RequestParam Integer userid,@RequestParam String bseClientCode,@RequestParam String clientName)
	{
		try
		{
			List<UsersOnlineRegDetails> userList = userBseNseDetailsService.findByUserIdAndBseClientCodeAndClientName(userid,bseClientCode,clientName);
			if(userList != null &&!userList.isEmpty())
			{
				return ResponseEntity.ok(userList);
			}else {
				return ResponseEntity.ok(new ArrayList<>());
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR, "status_msg", "Error occurred while fetching data"));
		}
	}

	@Hidden
	@GetMapping("/getByUserIdAndBseClientCodeAndBrokerCodeAndClientName")
	public ResponseEntity<?> getByUserIdAndBseClientCodeAndBrokerCodeAndClientName(@RequestParam Integer userid,@RequestParam String bseClientCode,@RequestParam String brokerCode,@RequestParam String clientName)
	{
		try
		{
			List<UsersOnlineRegDetails> userList = userBseNseDetailsService.findByUserIdAndBseClientCodeAndBrokerCodeAndClientName(userid,bseClientCode,brokerCode,clientName);
			if(userList != null &&!userList.isEmpty())
			{
				return ResponseEntity.ok(userList);
			}else {
				return ResponseEntity.ok(new ArrayList<>());
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR, "status_msg", "Error occurred while fetching data"));
		}
	}

	@Hidden
	@GetMapping("/findActiveBseByBseClientCodeAndClientName")
	public ResponseEntity<?> findActiveBseByBseClientCodeAndClientName(@RequestParam String bseClientCode,@RequestParam String clientName)
	{
		try
		{
			List<UsersOnlineRegDetails> userList = userBseNseDetailsService.findActiveBseByBseClientCodeAndClientName(bseClientCode,clientName);
			if(userList != null &&!userList.isEmpty())
			{
				return ResponseEntity.ok(userList);
			}else {
				return ResponseEntity.ok(new ArrayList<>());
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR, "status_msg", "Error occurred while fetching data"));
		}
	}

	@Hidden
	@GetMapping("/getByClientNameByAdvisorEmailSignature")
	public ResponseEntity<?> getByClientNameByAdvisorEmailSignature(@RequestParam String clientName)
	{
		try
		{
			AdvisorEmailSignature userList = advisorEmailSignatureRepository.findByClientName(clientName);
			if(userList != null)
			{
				return ResponseEntity.ok(userList);
			}else {
				return ResponseEntity.ok(new ArrayList<>());
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR, "status_msg", "Error occurred while fetching data"));
		}
	}

	@Hidden
	@GetMapping("/getNseActiveUserBseNseDetailsByUserIdAndClientname")
	public ResponseEntity<?> getNseActiveUserBseNseDetailsByUserIdAndClientname(@RequestParam String clientName, @RequestParam Integer userid) {
		try
		{
			List<UsersOnlineRegDetails> userList = userBseNseDetailsService.getUserBseNseDetailsByUserIdAndClientname(userid,clientName);
			if(userList != null &&!userList.isEmpty())
			{
				return ResponseEntity.ok(userList);
			}else
			{
				return ResponseEntity.ok(new ArrayList<>());
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", 500, "status_msg", "Error occurred while fetching user details"));
		}
	}

    @Hidden
    @GetMapping("/getUserByIdAndClientNameActiveNse")
    public ResponseEntity<?> getUserByIdAndClientNameActiveNse(@RequestParam String clientName, @RequestParam Integer userid) {
        try {
            List<UsersOnlineRegDetails> key = userOnlineRegDetailsRespository.findListOfNseUserByUserIdAndClientName(userid, clientName);
            System.out.println("key "+key);
            if (key != null) {
                return ResponseEntity.ok(key);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", 404, "status_msg", "User not found"));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", 500, "status_msg", "Error occurred while fetching user details"));
        }
    }

    @Hidden
    @GetMapping("/getLatestByUserIdAndClientName")
    public ResponseEntity<?> getLatestByUserIdAndClientName(@RequestParam String clientName, @RequestParam Integer id,@RequestParam String basket_name)
    {
        BasketDetails showBasketDetails = null;
        try
        {
            List<BasketDetails> key = basketDetailsRepository.findLatestByUserIdAndClientName(id,basket_name, clientName);

            if(key != null && key.size() > 0)
            {
                showBasketDetails = key.get(0);
            }

            System.out.println("key "+showBasketDetails);

            if (showBasketDetails != null) {
                return ResponseEntity.ok(showBasketDetails);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", 404, "status_msg", "User not found"));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", 500, "status_msg", "Error occurred while fetching user details"));
        }
    }

    @Hidden
    @GetMapping("/getLatestByClientName")
    public ResponseEntity<?> getLatestByClientName(@RequestParam String clientName)
    {
        try
        {
            List<BasketDetails> key = basketDetailsRepository.findLatestByClientName(clientName);

            if (key != null) {
                return ResponseEntity.ok(key);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", 404, "status_msg", "User not found"));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", 500, "status_msg", "Error occurred while fetching user details"));
        }
    }

    @Hidden
    @PostMapping("/saveBasketDetails")
    public ResponseEntity<?> saveBasketDetails(@RequestBody BasketDetails basketDetails){

        try
        {

            List<BasketDetails> detailsOptional  = Collections.singletonList(basketDetailsRepository.save(basketDetails));

            if (detailsOptional.size() > 0)
            {
                return ResponseEntity.ok(detailsOptional);
            } else
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", HttpStatus.BAD_REQUEST, "status_msg", "Data Not saved"));
            }
        }catch (Exception ex)
        {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR, "status_msg", "Error occurred while fetching data"));
        }
    }

    @Hidden
    @GetMapping("/getUserDetailsByIinNumberAndBrokercode")
    public ResponseEntity<?> getUserDetailsByIinNumberAndBrokercode(@RequestParam String broker_code,@RequestParam String iin_number,@RequestParam String client_name,@RequestParam String user_id)
    {

        System.out.println("broker_code = " + broker_code);
        System.out.println("iin_number = " + iin_number);
        System.out.println("client_name = " + client_name);
        System.out.println("user_id = " + user_id);

        try
		{
            Optional<UsersOnlineRegDetails> detailsOptional =
					userOnlineRegDetailsRespository.findNseByIinNumberAndBrokercode(iin_number, broker_code,client_name,user_id);

            if (detailsOptional.isPresent()) {
                return ResponseEntity.ok(detailsOptional.get());
            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No user details found for the given broker code and IIN number.");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving user details");
        }
    }

	@Hidden
	@GetMapping("/getUserBseNseDetailsByNseIINNumberBrokerCode")
	public ResponseEntity<?> getUserBseNseDetailsByNseIINNumberBrokerCode(@RequestParam String client_name, @RequestParam String iin_number, @RequestParam String broker_code, @RequestHeader("Authorization") String token) {
		try {

			String userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);

			if (StringHelper.isEmpty(iin_number) || StringHelper.isEmpty(client_name) || StringHelper.isEmpty(broker_code)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", HttpStatus.BAD_REQUEST, "status_msg", "Broker code or Client name or UCC code is Empty...! Please check user master...!"));
			}

			List<UsersOnlineRegDetails> detailsOptional = userOnlineRegDetailsRespository.findNseByIinNumberAndClientNameBrokerCode(iin_number, client_name, broker_code, userid);
			if (!detailsOptional.isEmpty() && detailsOptional.size() > 0) {
				UsersOnlineRegDetails userDetail = detailsOptional.get(0);
				List<UsersBankDetails> bankDetails = usersBankDetailsRepository.findByUseridAndClientName(userDetail.getUser_id(), userDetail.getClient_name(), String.valueOf(userDetail.getId()));
				Optional<UsersNomineeDetails> nomineeDetails = usersNomineeDetailsRepository.findByUseridAndClientName(userDetail.getUser_id(), userDetail.getClient_name(), String.valueOf(userDetail.getId()), "NSE");
				List<UsersMandateDetails> mandateDetails = usersMandateDetailsRespository.findByUseridAndClientName(userDetail.getUser_id(), userDetail.getClient_name(), String.valueOf(userDetail.getId()));

				UserDto userDto = UserMapper.mapToUserDtoMappers(userDetail, bankDetails, mandateDetails, nomineeDetails.isPresent() ? nomineeDetails.get() : null);

				return ResponseEntity.ok(userDto);
			} else {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", HttpStatus.BAD_REQUEST, "status_msg", "No record found for the given IIN Number and Client Name."));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR, "status_msg", "Error occurred while fetching data"));
		}
	}

	@Hidden
	@GetMapping("/getMandateDetailsByBrokerCode")
	public ResponseEntity<?> getMandateDetailsByBrokerCode(
			@RequestParam("user_id") Integer userId,
			@RequestParam("client_name") String clientName,
			@RequestParam("online_code") String onlineCode,
			@RequestParam("broker_code") String broker_code) {
		try
		{
			List<UsersMandateDetails> detailsOptional =
					usersMandateDetailsRespository.getMandateDetailsByClientCode(
							userId, clientName, onlineCode, broker_code);

			// Convert entity -> DTO
			List<UserMandateDetailsDto> dtoList = detailsOptional.stream()
					.map(d -> {
						UserMandateDetailsDto dto = new UserMandateDetailsDto();
						dto.setId(d.getId());
						dto.setUser_id(d.getUser_id());
						dto.setOnline_id(d.getOnline_id());
						dto.setOnline_flag(d.getOnline_flag());
						dto.setOnline_code(d.getOnline_code());
						dto.setBroker_code(d.getBroker_code());
						dto.setBank_account_number(d.getBank_account_number());

						dto.setXsip_otm_flag(d.getXsip_otm_flag());
						dto.setXsip_otm(d.getXsip_otm());
						dto.setXsip_otm_amount(d.getXsip_otm_amount());
						dto.setXsip_otm_approved(d.getXsip_otm_approved());
						dto.setXsip_otm_rej_reason(d.getXsip_otm_rej_reason());
//                        dto.setXsip_otm_created_date(d.getXsip_otm_created_date() != null
//                                ? d.getXsip_otm_created_date().toLocalDate() : null);

						dto.setEmandate_otm_flag(d.getEmandate_otm_flag());
						dto.setEmandate_otm(d.getEmandate_otm());
						dto.setEmandate_otm_amount(d.getEmandate_otm_amount());
						dto.setEmandate_otm_approved(d.getEmandate_otm_approved());
						dto.setEmandate_otm_rej_reason(d.getEmandate_otm_rej_reason());
//                        dto.setEmandate_otm_created_date(d.getEmandate_otm_created_date() != null
//                                ? d.getEmandate_otm_created_date().toLocalDate() : null);

						// NSE ACH
						dto.setNse_ach_flag(d.getNse_ach_flag());
						dto.setNse_ach(d.getNse_ach());
						dto.setNse_ach_amount(d.getNse_ach_amount());
						dto.setNse_ach_approved(d.getNse_ach_approved());
						dto.setNse_ach_rej_reason(d.getNse_ach_rej_reason());

						dto.setClient_name(d.getClient_name());
						dto.setCreated_date(d.getCreated_date());

						dto.setNse_ach_end_date(d.getNse_ach_end_date());
						dto.setNse_ach_start_date(d.getNse_ach_start_date());

						return dto;
					})
					.toList();

			return ResponseEntity.ok(dtoList);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error retrieving user mandate details");
		}
	}

	@Hidden
	@GetMapping("/getBankDetailsByBrokerCode")
	public ResponseEntity<?> getBankDetailsByBrokerCode(
			@RequestParam("user_id") Integer userId,
			@RequestParam("client_name") String clientName,
			@RequestParam("online_code") String onlineCode,
			@RequestParam("broker_code") String broker_code) {
		try
		{
			List<UsersBankDetails> detailsOptional =
					usersBankDetailsRepository.getNseUserBankDetail(userId, onlineCode, clientName, broker_code);

			List<UsersBankDetails> dtoList = detailsOptional.stream()
					.map(d -> {
						UsersBankDetails dto = new UsersBankDetails();
						dto.setId(d.getId());
						dto.setUser_id(d.getUser_id());
						dto.setOnline_id(d.getOnline_id());
						dto.setOnline_flag(d.getOnline_flag());
						dto.setOnline_code(d.getOnline_code());
						dto.setBroker_code(d.getBroker_code());
						dto.setClient_name(d.getClient_name());

						dto.setBank_name(d.getBank_name());
						dto.setBank_branch(d.getBank_branch());
						dto.setBank_address(d.getBank_address());
						dto.setBank_account_number(d.getBank_account_number());
						dto.setBank_account_holder_name(d.getBank_account_holder_name());
						dto.setBank_account_type(d.getBank_account_type());
						dto.setBank_ifsc_code(d.getBank_ifsc_code());
						dto.setBank_micr_code(d.getBank_micr_code());
						dto.setBank_proof(d.getBank_proof());

						dto.setCreated_date(d.getCreated_date());

						return dto;
					})
					.toList();

			return ResponseEntity.ok(dtoList);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error retrieving user bank details");
		}
	}

	@Hidden
	@PostMapping("/saveBankMandateDetails")
	public ResponseEntity<?> saveBankMandateDetails(@RequestBody List<BankDto> bankDtos) {
		try
		{
			List<UsersBankDetails> entities = bankDtos.stream()
					.map(BankInfoMapper::toEntity)
					.collect(Collectors.toList());

			List<UsersBankDetails> savedEntities = usersBankDetailsRepository.saveAll(entities);

			List<BankDto> savedDtos = savedEntities.stream()
					.map(BankInfoMapper::toDto)
					.collect(Collectors.toList());

			return ResponseEntity.ok(savedDtos);

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error saving Bank details: " + e.getMessage());
		}
	}

	@Hidden
	@GetMapping("/getUserRegDetailsForCartByUserIdTaxStatus")
	public ResponseEntity<?> getUserRegDetailsForCartByUserIdTaxStatus(@RequestParam String clientName, @RequestParam Integer userid, @RequestParam String tax_status_code, @RequestParam String holding_nature_code) {
		try {
			List<UsersOnlineRegDetails> key = userOnlineRegDetailsRespository.getUserRegDetailsForCartByUserIdTaxStatus(userid, clientName,tax_status_code,holding_nature_code);
			if (key != null) {
				return ResponseEntity.ok(key.get(0));
			} else {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", 404, "status_msg", "User not found"));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", 500, "status_msg", "Error occurred while fetching user details"));
		}
	}

	@Hidden
	@GetMapping("/getUserByIdAndClientNameAndiinnumber")
	public ResponseEntity<?> getUserByIdAndClientNameAndiinnumber(@RequestParam String clientName, @RequestParam Integer userid,@RequestParam String iin_number) {
		try {
			List<UsersOnlineRegDetails> key = userOnlineRegDetailsRespository.findUserByIdAndClientNameAndiin_number(userid, clientName,iin_number);
			if (key != null) {
				return ResponseEntity.ok(key.get(0));
			} else {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", 404, "status_msg", "User not found"));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", 500, "status_msg", "Error occurred while fetching user details"));
		}
	}

	@GetMapping("/getRegisterByAmcCode")
	public ResponseEntity<?> getRegisterByAmcCode(@RequestParam String amc_code) {
		try
		{
			String userList = usersPortfolioSchemewiseRepository.findRegisterByAmcCode(amc_code);

			if (userList != null && !userList.isEmpty())
			{
				return ResponseEntity.ok(userList);
			} else {
				return ResponseEntity.ok(new User());
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR, "status_msg", "Error occurred while fetching data"));
		}
	}

	@Hidden
	@GetMapping("/getAmcCodeByAmcName")
	public ResponseEntity<?> getAmcCodeByAmcName(@RequestParam String amc_name) {
		try
		{
			String userList = usersPortfolioSchemewiseRepository.findAmcCodeByAmcName(amc_name);

			if (userList != null && !userList.isEmpty())
			{
				return ResponseEntity.ok(userList);
			} else {
				return ResponseEntity.ok(new User());
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR, "status_msg", "Error occurred while fetching data"));
		}
	}

}
