package com.auth.controller;

import com.auth.model.BseNseKey;
import com.auth.model.User;
import com.auth.repository.BseNseKeyRepository;
import com.auth.repository.UserRepository;
import com.auth.response.SuccessResponse;
import com.auth.response.AuthUtils;
import com.auth.response.ErrorResponse;
import com.auth.service.LoginService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hibernate.internal.util.StringHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@SecurityScheme(
		name = "bearerAuth",
		type = SecuritySchemeType.HTTP,
		scheme = "bearer",
		bearerFormat = "JWT"
)
@RestController
@Tag(name = "Auth Utils", description = "APIs for Auth operations")
public class AuthController {

	@Value("${jwt.secret-key}")
	private String secretKey;

	private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private BseNseKeyRepository bseNseKeyRepository;

    @Autowired
    LoginService loginService;

	@Operation(
			summary = "User Login API",
			description = """
        This API authenticates a user based on `userid`, `password`, and (if required) `ARN code` sent in the request paremeters.

        - `userid` (String, required): PAN number or registered mobile number.  
        - `password` (String, required): User’s password.  
        - `arn_no` (String, optional): ARN code (only required when multiple accounts share the same PAN/mobile).

        On successful authentication, the API returns two tokens:
        - `access_token`: Valid for 1 hour, used to call all protected APIs.
        - `refresh_token`: Valid for 7 days, used only to generate a new access token when the original one expires.

        ✅ **Token Usage Flow**:
        - Always use the `access_token` in the `Authorization` header when calling APIs.
        - If the `access_token` has expired, use the `refresh_token` with the `/refresh` API to get a new access token.
        - The `/refresh` API will return both a new `access_token` and `refresh_token`.

        🔁 **Admin-Investor Scenario**:
        If the admin selects a particular investor from a list, you must call this login API again with the selected `investor_id`.  
        In that case, a new token pair will be generated, and you must start using that token for all subsequent requests.
        """
	)

	@ApiResponses(value =
	{
			@ApiResponse(responseCode = "200", description = "Success Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SuccessResponse.class))),
			@ApiResponse(responseCode = "400", description = "Failure Response", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
	})

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String userid, @RequestParam String password, @RequestParam(required = false) String key, @RequestParam(required = false) String arn_no, @RequestParam(required = false) Integer investor_id)
    {
        try
        {
            userid = AuthUtils.checkParem(userid);
            password = AuthUtils.checkParem(password);
            arn_no = AuthUtils.checkParem(arn_no);

            if (investor_id == null) {investor_id = 0;}

            if (StringHelper.isEmpty(userid))
            {
                return AuthUtils.errorResponse("User ID cannot be empty", HttpStatus.BAD_REQUEST);
            }

            if (StringHelper.isEmpty(password))
            {
                return AuthUtils.errorResponse("Password cannot be empty.", HttpStatus.BAD_REQUEST);
            }

            String clientName = "";


            if(StringHelper.isEmpty(clientName))
            {
                if (!arn_no.contains("-"))
                {
                    if (!arn_no.toUpperCase().contains("ARN"))
                    {
                        arn_no = "ARN-" + arn_no;
                    } else
                    {
                        if (arn_no.toUpperCase().startsWith("ARN"))
                        {
                            arn_no = arn_no.substring(3);  // Removes the first 3 characters (i.e., "ARN")
                            arn_no = "ARN-" + arn_no;
                        }
                    }
                }

                Optional<BseNseKey> mfdArnDetailsOpt = bseNseKeyRepository.findClientNameByBrokerCode(arn_no);

                if (mfdArnDetailsOpt.isPresent())
                {
                    BseNseKey bseNseKey = mfdArnDetailsOpt.get();
                    clientName = bseNseKey.getClientName();
                }
            }

            User user = loginService.validateLogin(userid, password, clientName);

            if(user == null)
            {
                return AuthUtils.errorResponse("The PAN or mobile number is not registered with us. Please check your credentials and try again.", HttpStatus.BAD_REQUEST);
            }

            Integer userId = user.getId();
            Integer active = user.getActive();
            Integer typeId = user.getType_id();
            clientName = user.getClient_name();

            if (typeId.equals(1) || typeId.equals(3))
            {
                investor_id = userId;
            }


            if (active.equals(0))
            {
                return AuthUtils.errorResponse("Your account has been deactivated. Kindly contact us and we will set it right for you! Thanking you.", HttpStatus.BAD_REQUEST);
            }

            user.setLanguage("0");
            userRepository.save(user);

            // 1-Hour Access Token Expiry
            Date accessExpiryTime = new Date(System.currentTimeMillis() + 1000 * 60 * 60); // 1 hour
            long accessExpiresInSeconds = (accessExpiryTime.getTime() - System.currentTimeMillis()) / 1000;

            // 7-Day Refresh Token Expiry
            Date refreshExpiryTime = new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7); // 7 days
            long refreshExpiresInSeconds = (refreshExpiryTime.getTime() - System.currentTimeMillis()) / 1000;

            String refreshToken = Jwts.builder()
                    .setSubject("")
                    .setIssuedAt(new Date())
                    .setExpiration(refreshExpiryTime)
                    .claim("user_id", userId)
                    .claim("type_id", typeId)
                    .claim("investor_id", investor_id)
                    .claim("client_name", clientName)
                    .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()), SignatureAlgorithm.HS256)
                    .compact();

            String token = Jwts.builder()
                    .setSubject("")
                    .setIssuedAt(new Date())
                    .setExpiration(accessExpiryTime)
                    .claim("user_id", userId)
                    .claim("type_id", typeId)
                    .claim("investor_id", investor_id)
                    .claim("client_name", clientName)
                    .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()), SignatureAlgorithm.HS256)
                    .compact();

            return AuthUtils.successResponse(token, user.getClient_name(), accessExpiresInSeconds, HttpStatus.OK, refreshToken, refreshExpiresInSeconds);
        } catch (Exception ex) {
            ex.printStackTrace();
            return AuthUtils.errorResponse("Something went wrong, We have taken note of the issue. Be rest assured we will fix it as soon as possible", HttpStatus.BAD_REQUEST);
        }
    }

	@Operation(
			summary = "Refresh access token using refresh token",
			description = """
        This endpoint issues a new access token and refresh token based on a valid `refresh_token`
        provided in the `Authorization` header in the format `Bearer <token>`.

        The response includes:
        - `access_token`: valid for 1 hour
        - `refresh_token`: valid for 7 days

        🔒 This method uses Bearer Authentication (JWT) but only requires a valid refresh token.
        """
	)
	@SecurityRequirement(name = "bearerAuth")  // ✅ Only for this method
	@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "New tokens generated successfully",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SuccessResponse.class))
			),
			@ApiResponse(
					responseCode = "401",
					description = "Your given refresh token has expired",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
			),
			@ApiResponse(
					responseCode = "500",
					description = "Something went wrong, We have taken note of the issue. Be rest assured we will fix it as soon as possible.",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
			)
	})
	@PostMapping("/refresh")
	public ResponseEntity<?> refreshAccessToken(@RequestHeader("Authorization") String refreshToken)
	{
		try
		{
			if (refreshToken == null || refreshToken.isEmpty())
			{
				return AuthUtils.errorResponse("Refresh token is missing", HttpStatus.BAD_REQUEST);
			}

			if (refreshToken.startsWith("Bearer "))
			{
				refreshToken = refreshToken.substring(7);
			}

			// Parse and validate refresh token
			Claims claims = Jwts.parserBuilder()
					.setSigningKey(secretKey.getBytes())
					.build()
					.parseClaimsJws(refreshToken)
					.getBody();

			// Check expiry
			if (claims.getExpiration().before(new Date()))
			{
				return AuthUtils.errorResponse("Your given refresh token has expired.", HttpStatus.UNAUTHORIZED);
			}

			// Extract custom claims
			String userId = String.valueOf(claims.get("user_id"));
			String typeId = String.valueOf(claims.get("type_id"));
			String investorId = String.valueOf(claims.get("investor_id"));
			String clientName = String.valueOf(claims.get("client_name"));

			System.out.println("userId = " + userId);

			// 1-Hour Access Token Expiry
			Date accessExpiryTime = new Date(System.currentTimeMillis() + 1000 * 60 * 60); // 1 hour
			long accessExpiresInSeconds = (accessExpiryTime.getTime() - System.currentTimeMillis()) / 1000;

			// 7-Day Refresh Token Expiry
			Date refreshExpiryTime = new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7); // 7 days
			long refreshExpiresInSeconds = (refreshExpiryTime.getTime() - System.currentTimeMillis()) / 1000;

			String newRefreshToken = Jwts.builder()
					.setSubject("")
					.setIssuedAt(new Date())
					.setExpiration(refreshExpiryTime)
					.claim("user_id", userId)
					.claim("type_id", typeId)
					.claim("investor_id", investorId)
					.claim("client_name", clientName)
					.signWith(Keys.hmacShaKeyFor(secretKey.getBytes()), SignatureAlgorithm.HS256)
					.compact();

			String token = Jwts.builder()
					.setSubject("")
					.setIssuedAt(new Date())
					.setExpiration(accessExpiryTime)
					.claim("user_id", userId)
					.claim("type_id", typeId)
					.claim("investor_id", investorId)
					.claim("client_name", clientName)
					.signWith(Keys.hmacShaKeyFor(secretKey.getBytes()), SignatureAlgorithm.HS256)
					.compact();

			// 5) Return token + clientName
			return AuthUtils.successResponse(token, clientName, accessExpiresInSeconds, HttpStatus.OK, newRefreshToken, refreshExpiresInSeconds);
		} catch (Exception ex)
		{
			return AuthUtils.errorResponse("Something went wrong, We have taken note of the issue. Be rest assured we will fix it as soon as possible.", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Operation(
			summary = "Refresh access token using refresh token",
			description = """
        This endpoint issues a new access token and refresh token based on a valid `refresh_token`
        provided in the `Authorization` header in the format `Bearer <token>`.

        The response includes:
        - `access_token`: valid for 1 hour
        - `refresh_token`: valid for 7 days

        🔒 This method uses Bearer Authentication (JWT) but only requires a valid refresh token.
        """
	)
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "New tokens generated successfully",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SuccessResponse.class))
			),
			@ApiResponse(
					responseCode = "401",
					description = "Your given refresh token has expired",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
			),
			@ApiResponse(
					responseCode = "500",
					description = "Something went wrong, We have taken note of the issue. Be rest assured we will fix it as soon as possible.",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
			)
	})
	@PostMapping("/getTokenForAdminWithInvestorId")
	public ResponseEntity<?> getTokenForAdminWithInvestorId(@RequestHeader("Authorization") String token, @RequestParam(required = true) Integer investor_id)
	{
		try
		{
			if (token == null || token.isEmpty())
			{
				return AuthUtils.errorResponse("Your given token is missing", HttpStatus.BAD_REQUEST);
			}

			if (investor_id == null)
			{
				return AuthUtils.errorResponse("Investor id is missing", HttpStatus.BAD_REQUEST);
			}

			if (token.startsWith("Bearer "))
			{
				token = token.substring(7);
			}

			// Parse and validate refresh token
			Claims claims = Jwts.parserBuilder()
					.setSigningKey(secretKey.getBytes())
					.build()
					.parseClaimsJws(token)
					.getBody();

			// Check expiry
			if (claims.getExpiration().before(new Date()))
			{
				return AuthUtils.errorResponse("Your given refresh token has expired.", HttpStatus.UNAUTHORIZED);
			}

			// Extract custom claims
			String userId = String.valueOf(claims.get("user_id"));
			String typeId = String.valueOf(claims.get("type_id"));
			String clientName = String.valueOf(claims.get("client_name"));

			System.out.println("userId = " + userId);

			// 1-Hour Access Token Expiry
			Date accessExpiryTime = new Date(System.currentTimeMillis() + 1000 * 60 * 60); // 1 hour
			long accessExpiresInSeconds = (accessExpiryTime.getTime() - System.currentTimeMillis()) / 1000;

			// 7-Day Refresh Token Expiry
			Date refreshExpiryTime = new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7); // 7 days
			long refreshExpiresInSeconds = (refreshExpiryTime.getTime() - System.currentTimeMillis()) / 1000;

			String newRefreshToken = Jwts.builder()
					.setSubject("")
					.setIssuedAt(new Date())
					.setExpiration(refreshExpiryTime)
					.claim("user_id", userId)
					.claim("type_id", typeId)
					.claim("investor_id", investor_id)
					.claim("client_name", clientName)
					.signWith(Keys.hmacShaKeyFor(secretKey.getBytes()), SignatureAlgorithm.HS256)
					.compact();

			String newAccessToken = Jwts.builder()
					.setSubject("")
					.setIssuedAt(new Date())
					.setExpiration(accessExpiryTime)
					.claim("user_id", userId)
					.claim("type_id", typeId)
					.claim("investor_id", investor_id)
					.claim("client_name", clientName)
					.signWith(Keys.hmacShaKeyFor(secretKey.getBytes()), SignatureAlgorithm.HS256)
					.compact();

			// 5) Return token + clientName
			return AuthUtils.successResponse(newAccessToken, clientName, accessExpiresInSeconds, HttpStatus.OK, newRefreshToken, refreshExpiresInSeconds);
		} catch (Exception ex)
		{
			return AuthUtils.errorResponse("Something went wrong, We have taken note of the issue. Be rest assured we will fix it as soon as possible.", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

    @Hidden
    @Operation(
            summary = "Refresh access token using refresh token",
            description = """
       This endpoint issues a new access token and refresh token based on a valid `refresh_token`
       provided in the `Authorization` header in the format `Bearer <token>`.

       The response includes:
       - `access_token`: valid for 1 hour
       - `refresh_token`: valid for 7 days

       🔒 This method uses Bearer Authentication (JWT) but only requires a valid refresh token.
       """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "New tokens generated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SuccessResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Your given refresh token has expired",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Something went wrong, We have taken note of the issue. Be rest assured we will fix it as soon as possible.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/getTokenForAdminIdWithInvestorId")
    public ResponseEntity<?> getTokenForAdminIdWithInvestorId(@RequestParam(required = true) Integer admin_id,@RequestParam(required = true) Integer investor_id)
    {
        try
        {
            if (admin_id == null)
            {
                return AuthUtils.errorResponse("Admin id is missing", HttpStatus.BAD_REQUEST);
            }

            if (investor_id == null)
            {
                return AuthUtils.errorResponse("Investor id is missing", HttpStatus.BAD_REQUEST);
            }

            Optional<User> adminUserOpt = userRepository.findByIds(admin_id);
            Optional<User> investorUserOpt = userRepository.findByIds(investor_id);

            if(!adminUserOpt.isPresent() && !investorUserOpt.isPresent())
            {
                return AuthUtils.errorResponse("User Not Valid", HttpStatus.BAD_REQUEST);
            }

            User adminUser = adminUserOpt.get();
            User investorUser = investorUserOpt.get();

            Date accessExpiryTime = new Date(System.currentTimeMillis() + 1000 * 60 * 60); // 1 hour
            long accessExpiresInSeconds = (accessExpiryTime.getTime() - System.currentTimeMillis()) / 1000;

            // 7-Day Refresh Token Expiry
            Date refreshExpiryTime = new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7); // 7 days
            long refreshExpiresInSeconds = (refreshExpiryTime.getTime() - System.currentTimeMillis()) / 1000;

            String newRefreshToken = Jwts.builder()
                    .setSubject("")
                    .setIssuedAt(new Date())
                    .setExpiration(refreshExpiryTime)
                    .claim("user_id", adminUser.getId())
                    .claim("type_id", adminUser.getType_id())
                    .claim("investor_id", investorUser.getId())
                    .claim("client_name", investorUser.getClient_name())
                    .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()), SignatureAlgorithm.HS256)
                    .compact();

            String newAccessToken = Jwts.builder()
                    .setSubject("")
                    .setIssuedAt(new Date())
                    .setExpiration(accessExpiryTime)
                    .claim("user_id", adminUser.getId())
                    .claim("type_id", adminUser.getType_id())
                    .claim("investor_id", investorUser.getId())
                    .claim("client_name", investorUser.getClient_name())
                    .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()), SignatureAlgorithm.HS256)
                    .compact();

            // 5) Return token + clientName
            return AuthUtils.successResponse(newAccessToken, investorUser.getClient_name(), accessExpiresInSeconds, HttpStatus.OK, newRefreshToken, refreshExpiresInSeconds);
        } catch (Exception ex)
        {
            return AuthUtils.errorResponse("Something went wrong, We have taken note of the issue. Be rest assured we will fix it as soon as possible.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}