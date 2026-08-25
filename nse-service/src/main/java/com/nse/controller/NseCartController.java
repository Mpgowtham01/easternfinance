package com.nse.controller;


import com.nse.client.AmfiServiceClient;
import com.nse.client.UserServiceClient;
import com.nse.config.TokenInterceptor;
import com.nse.dto.amfi.AmfiSchemeMasterDTO;
import com.nse.dto.mf.BasketDetailsDto;
import com.nse.dto.mf.CartDto;
import com.nse.dto.mf.UserDto;
import com.nse.model.NseOnlineSchemeMaster;
import com.nse.pojo.*;
import com.nse.repository.NseOnlineSchemeMasterRepository;
import com.nse.response.*;
import com.nse.services.CartService;
import com.nse.services.NseServiceDAO;
import com.nse.utils.FeignErrorHandler;
import com.nse.utils.NseUtils;
import com.nse.utils.NumberUtils;
import feign.FeignException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.internal.util.StringHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController

@Tag(
        name = "NSE Cart Controller",
        description = "APIs related to NSE Cart operations"
)

public class NseCartController {

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${amc.logo.url}")
    private String amcLogoPath;

    @Value("${vendor.logo.url}")
    private String vendorLogoPath;

    @Autowired
    UserServiceClient userServiceClient;

    @Autowired
    CartService cartService;

    @Autowired
    NseServiceDAO nseService;

    @Autowired
    NseOnlineSchemeMasterRepository nseOnlineSchemeMasterRepository;

    @Autowired
    AmfiServiceClient amfiServiceClient;

    @Operation(
            summary = "Save or Update Cart by User ID",
            description = "Saves or updates the user's cart. If investor details are missing, returns a 400 error."
    )
    @Parameters({
            @Parameter(name = "cart_id", in = ParameterIn.QUERY, description = "Cart ID", required = false),
            @Parameter(name = "broker_code", in = ParameterIn.QUERY, description = "Broker Code", required = false),
            @Parameter(name = "purchase_type", in = ParameterIn.QUERY, description = "Purchase type", required = false),
            @Parameter(name = "investor_code", in = ParameterIn.QUERY, description = "Investor code", required = false),
            @Parameter(name = "scheme_name", in = ParameterIn.QUERY, description = "Scheme name", required = false),
            @Parameter(name = "scheme_reinvest_tag", in = ParameterIn.QUERY, description = "Scheme reinvest tag", required = false),
            @Parameter(name = "to_scheme_name", in = ParameterIn.QUERY, description = "To scheme name", required = false),
            @Parameter(name = "to_scheme_reinvest_tag", in = ParameterIn.QUERY, description = "To scheme reinvest tag", required = false),
            @Parameter(name = "folio_no", in = ParameterIn.QUERY, description = "Folio number", required = false),
            @Parameter(name = "amount_type", in = ParameterIn.QUERY, description = "Amount type", required = false),
            @Parameter(name = "amount", in = ParameterIn.QUERY, description = "Amount", required = false),
            @Parameter(name = "units", in = ParameterIn.QUERY, description = "Units", required = false),
            @Parameter(name = "until_cancel", in = ParameterIn.QUERY, description = "Until cancel", required = false),
            @Parameter(name = "frequency", in = ParameterIn.QUERY, description = "Frequency", required = false),
            @Parameter(name = "sip_date", in = ParameterIn.QUERY, description = "SIP date", required = false),
            @Parameter(name = "stp_date", in = ParameterIn.QUERY, description = "STP date", required = false),
            @Parameter(name = "start_date", in = ParameterIn.QUERY, description = "Start date", required = false),
            @Parameter(name = "end_date", in = ParameterIn.QUERY, description = "End date", required = false),
            @Parameter(name = "trnx_type", in = ParameterIn.QUERY, description = "Transaction type", required = false),
            @Parameter(name = "total_amount", in = ParameterIn.QUERY, description = "Total amount", required = false),
            @Parameter(name = "total_units", in = ParameterIn.QUERY, description = "Total units", required = false),
            @Parameter(name = "installment", in = ParameterIn.QUERY, description = "Installment count", required = false),
            @Parameter(name = "sip_first_date", in = ParameterIn.QUERY, description = "SIP first date", required = false),
            @Parameter(name = "sip_second_date", in = ParameterIn.QUERY, description = "SIP second date", required = false),
            @Parameter(name = "nfo_flag", in = ParameterIn.QUERY, description = "NFO flag", required = false),
            @Parameter(name = "sip_tenure", in = ParameterIn.QUERY, description = "SIP tenure", required = false)
    })

    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Success Response",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SuccessResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Cart Updated",
                                            summary = "Cart Updated Successfully",
                                            value = "{\n" +
                                                    "  \"status\": 200,\n" +
                                                    "  \"status_msg\": \"Cart Details Updated Successfully.\",\n" +
                                                    "  \"message\": \"\"\n" +
                                                    "}"
                                    ),
                                    @ExampleObject(
                                            name = "Cart Saved",
                                            summary = "Cart Saved Successfully",
                                            value = "{\n" +
                                                    "  \"status\": 200,\n" +
                                                    "  \"status_msg\": \"Cart Details Saved Successfully.\",\n" +
                                                    "  \"message\": \"\"\n" +
                                                    "}"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Missing or Invalid Investor Details",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Investor Missing",
                                    value = "{\n" +
                                            "  \"status\": 400,\n" +
                                            "  \"status_msg\": \"Investor details not available.\",\n" +
                                            "  \"message\": \"\"\n" +
                                            "}"
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
                                    name = "Server Error",
                                    value = "{\n" +
                                            "  \"status\": 500,\n" +
                                            "  \"status_msg\": \"Something went wrong while saving the cart.\",\n" +
                                            "  \"message\": \"Internal Server Error\"\n" +
                                            "}"
                            )
                    )
            )
    })

    @PostMapping("/saveOrUpdateCartByUserId")
    public ResponseEntity<?> saveOrUpdateCartByUserId(HttpServletRequest request,
                                                      @RequestParam String cart_id,
                                                      @RequestParam String broker_code,
                                                      @RequestParam String purchase_type,
                                                      @RequestParam String investor_code,
                                                      @RequestParam String scheme_name,
                                                      @RequestParam String scheme_reinvest_tag,
                                                      @RequestParam String to_scheme_name,
                                                      @RequestParam String to_scheme_reinvest_tag,
                                                      @RequestParam String folio_no,
                                                      @RequestParam String amount_type,
                                                      @RequestParam String amount,
                                                      @RequestParam String units,
                                                      @RequestParam String until_cancel,
                                                      @RequestParam String frequency,
                                                      @RequestParam String sip_date,
                                                      @RequestParam String stp_date,
                                                      @RequestParam String start_date,
                                                      @RequestParam String end_date,
                                                      @RequestParam String trnx_type,
                                                      @RequestParam String total_amount,
                                                      @RequestParam String total_units,
                                                      @RequestParam String  installment,
                                                      @RequestParam String sip_first_date,
                                                      @RequestParam String sip_second_date,
                                                      @RequestParam(required = false) String tax_status,
                                                      @RequestParam(required = false) String tax_status_code,
                                                      @RequestParam(required = false) String holding_nature,
                                                      @RequestParam(required = false) String holding_nature_code,
                                                      @RequestParam(required = false) String inv_name,
                                                      @RequestParam(required = false) String step_up_flag,
                                                      @RequestParam(required = false) String step_up_frequency,
                                                      @RequestParam(required = false) String step_up_start_date,
                                                      @RequestParam(required = false) String step_up_end_date,
                                                      @RequestParam(required = false) String step_up_amount,
                                                      @RequestParam(required = false) String mandate_id,
                                                      @RequestParam(required = false) String first_order_flag,
                                                      @RequestParam String nfo_flag,
                                                      @RequestParam String sip_tenure,
                                                      @RequestParam(required = false) String source,
                                                      @RequestParam(required = false) String otherArn,
                                                      @RequestHeader("Authorization") String token)
    {
        StopWatch watch = new StopWatch();
        watch.start();
        UserDto user = null;
        CartDto cart = null;
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

        try
        {
            String userid = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);

            String client_name= TokenInterceptor.extractClientNamedFromToken(token, secretKey);
            String bse_nse_mfu_flag = NseUtils.checkParem("NSE");

            cart_id = NseUtils.checkParem(cart_id);
            broker_code = NseUtils.checkParem(broker_code);
            purchase_type = NseUtils.checkParem(purchase_type);
            investor_code = NseUtils.checkParem(investor_code);
            scheme_name = NseUtils.checkParem(scheme_name);
            scheme_reinvest_tag = NseUtils.checkParem(scheme_reinvest_tag);
            to_scheme_name = NseUtils.checkParem(to_scheme_name);
            to_scheme_reinvest_tag = NseUtils.checkParem(to_scheme_reinvest_tag);
            folio_no = NseUtils.checkParem(folio_no);
            amount_type = NseUtils.checkParem(amount_type);
            frequency = NseUtils.checkParem(frequency);
            sip_date = NseUtils.checkParem(sip_date);
            stp_date = NseUtils.checkParem(stp_date);
            start_date = NseUtils.checkParem(start_date);
            end_date = NseUtils.checkParem(end_date);
            trnx_type = NseUtils.checkParem(trnx_type);
            sip_first_date = NseUtils.checkParem(sip_first_date);
            sip_second_date = NseUtils.checkParem(sip_second_date);
            nfo_flag = NseUtils.checkParem(nfo_flag);
            inv_name = NseUtils.checkParem(inv_name);
            tax_status = NseUtils.checkParem(tax_status);
            tax_status_code = NseUtils.checkParem(tax_status_code);
            holding_nature = NseUtils.checkParem(holding_nature);
            holding_nature_code = NseUtils.checkParem(holding_nature_code);
            mandate_id = NseUtils.checkParem(mandate_id);
            first_order_flag = NseUtils.checkParem(first_order_flag);
            otherArn = NseUtils.checkParem(otherArn);

            if(first_order_flag.isEmpty())
            {
                first_order_flag = "N";
            }
            step_up_flag = NseUtils.checkParem(step_up_flag);
            step_up_frequency = NseUtils.checkParem(step_up_frequency);
            step_up_start_date = NseUtils.checkParem(step_up_start_date);
            step_up_end_date = NseUtils.checkParem(step_up_end_date);
            step_up_amount = NseUtils.checkParem(step_up_amount);
            source = NseUtils.checkParem(source);

            if(StringHelper.isEmpty(step_up_flag)) {step_up_flag = "N";}

            System.out.println("sip_date = " + sip_date);
            System.out.println("frequency = " + frequency);

            String start_day = "";
            String start_month = "";
            String start_year = "";

            String end_day = "";
            String end_month = "";
            String end_year = "";

            if(StringHelper.isEmpty(purchase_type))
            {
                return NseUtils.commonResponse("Please provide the Purchase Type", HttpStatus.BAD_REQUEST);
            }

            if(StringHelper.isEmpty(investor_code))
            {
                return NseUtils.commonResponse("Please provide the Investor Code",  HttpStatus.BAD_REQUEST);
            }

            if(StringHelper.isEmpty(scheme_reinvest_tag)) {
                return NseUtils.commonResponse("Please provide the scheme_reinvest_tag",  HttpStatus.BAD_REQUEST);
            }

            System.out.println("GET USER ID = " + userid);
            System.out.println("GET USER client_name = " + client_name);

            if(StringHelper.isNotEmpty(tax_status_code) && StringHelper.isNotEmpty(holding_nature_code))
            {
                user = userServiceClient.getUserRegDetailsForCartByUserIdTaxStatus(client_name, Integer.valueOf(userid),tax_status_code,holding_nature_code,token);
            }else
            {
                try
                {
                    user =  userServiceClient.getUserByIdAndClientNameAndiinnumber(client_name, Integer.valueOf(userid),investor_code,token);
                }catch (FeignException e)
                {
                    return FeignErrorHandler.handle(e, "User Service", "User not found");
                }
            }

            if(user != null)
            {
                if(StringHelper.isEmpty(inv_name)){inv_name = user.getName();}
                if(StringHelper.isEmpty(tax_status)){tax_status = user.getTax_status();}
                if(StringHelper.isEmpty(tax_status_code)){tax_status_code = user.getTax_status_code();}
                if(StringHelper.isEmpty(holding_nature)){holding_nature = user.getHolding_nature();}
                if(StringHelper.isEmpty(holding_nature_code)){holding_nature_code = user.getHolding_nature_code();}
                if(StringHelper.isEmpty(broker_code)){broker_code = user.getBroker_code();}

                System.out.println("second users name & Pan  = " + user.getName() + " &  PAN = " + user.getPan());
                Boolean cart_status = false;

                if (StringHelper.isNotEmpty(cart_id))
                {
                    cart = (CartDto) userServiceClient.getCartDetails(Integer.valueOf(cart_id), client_name, token);
                    System.out.println("CART DETAILS By NSE = " + cart);
                }

                if (cart == null)
                {
                    System.out.println("CAME HERE FOR CREATE NEW CART");
                    cart = new CartDto();
                    cart_status = false;
                } else {
                    cart_status = true;
                }

                System.out.println("user = "+ user);

                if (purchase_type.equalsIgnoreCase("Lumpsum Purchase"))
                {
                    if (StringHelper.isEmpty(scheme_name)) {
                        return NseUtils.commonResponse("Please provide the Scheme Name", HttpStatus.BAD_REQUEST);
                    }

                    if (nfo_flag.equalsIgnoreCase("N") && StringHelper.isEmpty(scheme_reinvest_tag)) {
                        return NseUtils.commonResponse("Please provide the Scheme Reinvest Tag", HttpStatus.BAD_REQUEST);
                    }

                    if (StringHelper.isEmpty(amount)) {
                        return NseUtils.commonResponse("Please provide the Amount", HttpStatus.BAD_REQUEST);
                    }

                    if (nfo_flag.equalsIgnoreCase("N")) {
                        Double minAmount = 0.0;

                        if (bse_nse_mfu_flag.equalsIgnoreCase("NSE")) {
                            minAmount = cartService.getNSELumpsumMinAmountBySchemeName(scheme_name, trnx_type, scheme_reinvest_tag);
                        }

                        Double minAmt = minAmount;
                        Double amt = Double.parseDouble(amount);

                        if (minAmt > amt) {
                            return NseUtils.commonResponse("Entered amount less than min product limits for " + scheme_name + ", (Min amount shoud be Rs." + minAmt + ")", HttpStatus.BAD_REQUEST);
                        }
                    }

                    if (StringHelper.isEmpty(folio_no)) {
                        return NseUtils.commonResponse("Please provide the Folio Number", HttpStatus.BAD_REQUEST);
                    }

                    if (StringHelper.isEmpty(trnx_type)) {
                        return NseUtils.commonResponse("Please provide Trasaction Type", HttpStatus.BAD_REQUEST);
                    }
                }
                else if (purchase_type.equalsIgnoreCase("SIP Purchase"))
                {

                    if (StringHelper.isEmpty(scheme_name)) {
                        return NseUtils.commonResponse("Please provide the Scheme Name", HttpStatus.BAD_REQUEST);
                    }

                    if (nfo_flag.equalsIgnoreCase("N") && StringHelper.isEmpty(scheme_reinvest_tag)) {
                        return NseUtils.commonResponse("Please provide the Scheme Reinvest Tag", HttpStatus.BAD_REQUEST);
                    }

                    if (StringHelper.isEmpty(amount)) {
                        return NseUtils.commonResponse("Please provide the Amount", HttpStatus.BAD_REQUEST);
                    }
                    System.out.println("228 + " + nfo_flag);
                    if (nfo_flag.equalsIgnoreCase("N")) {
                        double minAmount = 0.0;

                        if (bse_nse_mfu_flag.equalsIgnoreCase("NSE")) {
                            minAmount = cartService.validateSipamount(scheme_name, trnx_type, scheme_reinvest_tag);
                        }


                        Double minAmt = minAmount;
                        Double amt = Double.parseDouble(amount);

                        if (minAmt > amt) {
                            return NseUtils.commonResponse("Entered amount less than min product limits for " + scheme_name + ", (Min amount shoud be Rs." + minAmt + ")", HttpStatus.BAD_REQUEST);
                        }

                        System.out.println("min amount = " +  minAmount);
                    }

                    if (StringHelper.isEmpty(folio_no)) {
                        return NseUtils.commonResponse("Please provide the Folio Number", HttpStatus.BAD_REQUEST);
                    }

                    if (StringHelper.isEmpty(trnx_type)) {
                        return NseUtils.commonResponse("Please provide Trasaction Type", HttpStatus.BAD_REQUEST);
                    }

                    if (StringHelper.isEmpty(frequency)) {
                        return NseUtils.commonResponse("Please provide Frequency", HttpStatus.BAD_REQUEST);
                    }

                    if (StringHelper.isEmpty(start_date)) {
                        return NseUtils.commonResponse("Please provide Start Date", HttpStatus.BAD_REQUEST);
                    }

                    if (bse_nse_mfu_flag.equalsIgnoreCase("NSE")) {
                        if (frequency.equalsIgnoreCase("OW") && StringHelper.isEmpty(sip_date)) {
                            return NseUtils.commonResponse("Please provide SIP Date", HttpStatus.BAD_REQUEST);
                        }

                        if (frequency.equalsIgnoreCase("TM")) {
                            if (StringHelper.isEmpty(sip_first_date)) {
                                return NseUtils.commonResponse("Please provide SIP First Date", HttpStatus.BAD_REQUEST);
                            }

                            if (StringHelper.isEmpty(sip_second_date)) {
                                return NseUtils.commonResponse("Please provide SIP Second Date", HttpStatus.BAD_REQUEST);
                            }
                        }
                    }

                    if(step_up_flag.equalsIgnoreCase("Y"))
                    {
                        if (StringHelper.isEmpty(step_up_frequency))
                        {
                            return NseUtils.commonResponse("Please provide SIP Step Up Frequency", HttpStatus.BAD_REQUEST);
                        }

                        if (StringHelper.isEmpty(step_up_start_date))
                        {
                            return NseUtils.commonResponse("Please provide SIP Step Up Start Date", HttpStatus.BAD_REQUEST);
                        }

                        if (StringHelper.isEmpty(step_up_end_date))
                        {
                            return NseUtils.commonResponse("Please provide SIP Step Up End Date", HttpStatus.BAD_REQUEST);
                        }

                        if (StringHelper.isEmpty(step_up_amount))
                        {
                            return NseUtils.commonResponse("Please provide SIP Step Up Amount", HttpStatus.BAD_REQUEST);
                        }
                    }

                } else if (purchase_type.equalsIgnoreCase("Switch Purchase"))
                {
                    if (StringHelper.isEmpty(folio_no)) {
                        return NseUtils.commonResponse("Please provide the Folio Number", HttpStatus.BAD_REQUEST);
                    }

                    if (StringHelper.isEmpty(scheme_name)) {
                        return NseUtils.commonResponse("Please provide the From Scheme Name", HttpStatus.BAD_REQUEST);
                    }

                    if (StringHelper.isEmpty(scheme_reinvest_tag)) {
                        return NseUtils.commonResponse("Please provide the From Scheme Reinvest Tag", HttpStatus.BAD_REQUEST);
                    }

                    if (StringHelper.isEmpty(to_scheme_name)) {
                        return NseUtils.commonResponse("Please provide the To Scheme Name", HttpStatus.BAD_REQUEST);
                    }

                    if (StringHelper.isEmpty(to_scheme_reinvest_tag)) {
                        return NseUtils.commonResponse("Please provide the TO Scheme Reinvest Tag", HttpStatus.BAD_REQUEST);
                    }

                    if (StringHelper.isEmpty(amount_type)) {
                        return NseUtils.commonResponse("Please provide the Switch Type", HttpStatus.BAD_REQUEST);
                    }

                    if (StringHelper.isEmpty(cart_id)) {
                        if(otherArn.equalsIgnoreCase("T"))
                        {
                            cart = null;
                        }else{
                            cart = cartService.getPurchaseCartForBse(Integer.parseInt(userid), investor_code, folio_no, scheme_name, scheme_reinvest_tag,to_scheme_name, client_name, purchase_type,token);
                        }

                        System.out.println("cart = " + cart);
                        if(cart != null)
                        {
                            return NseUtils.commonResponse("This scheme is already in your cart. Please review your cart before proceeding.", HttpStatus.BAD_REQUEST);
                        }else
                        {
                            cart = new CartDto();
                            cart_status = false;
                        }
                    }

                    if(!amount_type.isEmpty() && amount_type.equalsIgnoreCase("Amount") && amount.isEmpty())
                    {
                        return NseUtils.commonResponse("Please provide the Switch Amount",HttpStatus.BAD_REQUEST);
                    }

                    if(!amount_type.isEmpty() && amount_type.equalsIgnoreCase("Amount") && amount.equalsIgnoreCase("0.0"))
                    {
                        return NseUtils.commonResponse("Please provide the Valid Switch Amount",HttpStatus.BAD_REQUEST);
                    }

                    if(!amount_type.isEmpty() && amount_type.equalsIgnoreCase("Units") && units.isEmpty())
                    {
                        return NseUtils.commonResponse("Please provide the Switch Amount",HttpStatus.BAD_REQUEST);
                    }

                    if(!amount_type.isEmpty() && amount_type.equalsIgnoreCase("Units") && units.equalsIgnoreCase("0.0"))
                    {
                        return NseUtils.commonResponse("Please provide the Valid Switch Amount",HttpStatus.BAD_REQUEST);
                    }

//                        if(!amount_type.isEmpty() && amount_type.equalsIgnoreCase("All Units") && units.isEmpty())
//                        {
//                            return NseUtils.commonResponse("Please provide the Switch Amount",HttpStatus.BAD_REQUEST);
//                        }
//
//                        if(!amount_type.isEmpty() && amount_type.equalsIgnoreCase("All Units") && units.equalsIgnoreCase("0.0"))
//                        {
//                            return NseUtils.commonResponse("Please provide the Valid Switch Amount",HttpStatus.BAD_REQUEST);
//                        }

                    if(!otherArn.equalsIgnoreCase("T"))
                    {
                        SchemeHoldingUnitsPojo values = cartService.getSchemeHoldingUnits(client_name, folio_no, scheme_name, Integer.parseInt(userid), token);

                        Double total_units_val = values.getTotal_units();

                        if (total_units_val == null) {
                            total_units_val = 0.0;
                        }

                        if (total_units_val.equals(0.0)) {
                            return NseUtils.commonResponse("Your current value is zero. You can't give the request right now", HttpStatus.BAD_REQUEST);
                        }

                    }

                    if(!amount_type.isEmpty() && amount_type.equalsIgnoreCase("Units") && units.isEmpty())
                    {
                        return NseUtils.commonResponse("Please provide the Switch Units",HttpStatus.BAD_REQUEST);
                    }

                    if(!amount_type.isEmpty() && amount_type.equalsIgnoreCase("Units") && units.equalsIgnoreCase("0"))
                    {
                        return NseUtils.commonResponse("Please provide the Valid Switch Units",HttpStatus.BAD_REQUEST);
                    }

//                        if(!amount_type.isEmpty() && amount_type.equalsIgnoreCase("All Units") && total_units.isEmpty())
//                        {
//                            return NseUtils.commonResponse("Please provide the Switch All Units",HttpStatus.BAD_REQUEST);
//                        }
//
//                        if(!amount_type.isEmpty() && amount_type.equalsIgnoreCase("All Units") && total_units.equalsIgnoreCase("0"))
//                        {
//                            return NseUtils.commonResponse("Please provide the Valid Switch All Units",HttpStatus.BAD_REQUEST);
//                        }
                }

                else if(purchase_type.equalsIgnoreCase("Redemption Purchase"))
                {
                    if(StringHelper.isEmpty(scheme_name))
                    {
                        return NseUtils.commonResponse("Please provide the Scheme Name",HttpStatus.BAD_REQUEST);
                    }

                    if(StringHelper.isEmpty(folio_no))
                    {
                        return NseUtils.commonResponse("Please provide the Folio Number",HttpStatus.BAD_REQUEST);
                    }

                    if(StringHelper.isEmpty(amount_type))
                    {
                        return NseUtils.commonResponse("Please provide the Redemption Type",HttpStatus.BAD_REQUEST);
                    }

                    if(!amount_type.isEmpty() && amount_type.equalsIgnoreCase("Amount") && amount.isEmpty())
                    {
                        return NseUtils.commonResponse("Please provide the Redemption Amount",HttpStatus.BAD_REQUEST);
                    }

                    if(!amount_type.isEmpty() && amount_type.equalsIgnoreCase("Units") && units.isEmpty())
                    {
                        return NseUtils.commonResponse("Please provide the Redemption Units",HttpStatus.BAD_REQUEST);
                    }

//                        if(!amount_type.isEmpty() && amount_type.equalsIgnoreCase("All Units") && total_units.isEmpty())
//                        {
//                            return NseUtils.commonResponse("Please provide the Redemption All Units",HttpStatus.BAD_REQUEST);
//                        }

                    if(StringHelper.isEmpty(cart_id))
                    {
                        cart = cartService.getPurchaseCart(Integer.parseInt(userid), investor_code, folio_no, scheme_name, scheme_reinvest_tag, client_name, purchase_type,token);

                        if(cart != null)
                        {
                            return NseUtils.commonResponse("This scheme is already in your cart. Please review your cart before proceeding.",HttpStatus.BAD_REQUEST);
                        }else
                        {
                            cart = new CartDto();
                            cart_status = false;
                        }
                    }

                    if(!otherArn.equalsIgnoreCase("T"))
                    {
                        SchemeHoldingUnitsPojo values = cartService.getSchemeHoldingUnits(client_name, folio_no, scheme_name, Integer.parseInt(userid), token);
                        Double total_units_val = values.getTotal_units();

                        if (total_units_val == null) {
                            total_units_val = 0.0;
                        }
                        if (total_units_val.equals(0.0)) {
                            return NseUtils.commonResponse("You cannot redeem your units or amount as your ELSS scheme does not have any free units available.", HttpStatus.BAD_REQUEST);
                        }
                    }
                }

                else if(purchase_type.equalsIgnoreCase("STP Purchase"))
                {
                    if(StringHelper.isEmpty(folio_no))
                    {
                        return NseUtils.commonResponse("Please provide the Folio Number",HttpStatus.BAD_REQUEST);
                    }

                    if(StringHelper.isEmpty(scheme_name))
                    {
                        return NseUtils.commonResponse("Please provide the From Scheme Name",HttpStatus.BAD_REQUEST);
                    }

                    if(StringHelper.isEmpty(scheme_reinvest_tag))
                    {
                        return NseUtils.commonResponse("Please provide the From Scheme Reinvest Tag",HttpStatus.BAD_REQUEST);
                    }

                    if(StringHelper.isEmpty(to_scheme_name))
                    {
                        return NseUtils.commonResponse("Please provide the To Scheme Name",HttpStatus.BAD_REQUEST);
                    }

                    if(StringHelper.isEmpty(to_scheme_reinvest_tag))
                    {
                        return NseUtils.commonResponse("Please provide the TO Scheme Reinvest Tag",HttpStatus.BAD_REQUEST);
                    }

                    if(StringHelper.isEmpty(amount_type))
                    {
                        return NseUtils.commonResponse("Please provide the STP Type",HttpStatus.BAD_REQUEST);
                    }

                    if(!amount_type.isEmpty() && amount_type.equalsIgnoreCase("Amount") && amount.isEmpty())
                    {
                        return NseUtils.commonResponse("Please provide the STP Amount",HttpStatus.BAD_REQUEST);
                    }

                    if(!amount_type.isEmpty() && amount_type.equalsIgnoreCase("Amount") && amount.equalsIgnoreCase("0"))
                    {
                        return NseUtils.commonResponse("Please provide the Valid STP Amount",HttpStatus.BAD_REQUEST);
                    }

                    if(!amount_type.isEmpty() && amount_type.equalsIgnoreCase("Units") && units.isEmpty())
                    {
                        return NseUtils.commonResponse("Please provide the STP Units",HttpStatus.BAD_REQUEST);
                    }

                    if(!amount_type.isEmpty() && amount_type.equalsIgnoreCase("Units") && units.equalsIgnoreCase("0"))
                    {
                        return NseUtils.commonResponse("Please provide the Valid STP Units",HttpStatus.BAD_REQUEST);
                    }

                    if(!amount_type.isEmpty() && amount_type.equalsIgnoreCase("All Units") && total_units.isEmpty())
                    {
                        return NseUtils.commonResponse("Please provide the STP All Units",HttpStatus.BAD_REQUEST);
                    }

                    if(!amount_type.isEmpty() && amount_type.equalsIgnoreCase("All Units") && total_units.equalsIgnoreCase("0"))
                    {
                        return NseUtils.commonResponse("Please provide the Valid STP All Units",HttpStatus.BAD_REQUEST);
                    }

                    if(!amount_type.isEmpty() && amount_type.equalsIgnoreCase("Amount") && !NumberUtils.isParsable(amount))
                    {
                        return NseUtils.commonResponse("Please provide the Valid STP Amount",HttpStatus.BAD_REQUEST);
                    }

                    if(!amount_type.isEmpty() && amount_type.equalsIgnoreCase("Units") && !NumberUtils.isParsable(units))
                    {
                        return NseUtils.commonResponse("Please provide the STP Units",HttpStatus.BAD_REQUEST);
                    }

                    if(!amount_type.isEmpty() && amount_type.equalsIgnoreCase("All Units") && !NumberUtils.isParsable(total_units))
                    {
                        return NseUtils.commonResponse("Please provide the STP All Units",HttpStatus.BAD_REQUEST);
                    }

                    if(StringHelper.isEmpty(start_date))
                    {
                        return NseUtils.commonResponse("Please provide the STP Start Date",HttpStatus.BAD_REQUEST);
                    }

                    if(StringHelper.isEmpty(frequency))
                    {
                        return NseUtils.commonResponse("Please provide the STP Frequency",HttpStatus.BAD_REQUEST);
                    }

                    if(bse_nse_mfu_flag.equalsIgnoreCase("NSE"))
                    {
//                            if(StringHelper.isEmpty(end_date))
//                            {
//                                return NseUtils.commonResponse("Please provide the STP End Date",HttpStatus.BAD_REQUEST);
//                            }

                        if(frequency.equalsIgnoreCase("OW") && stp_date.isEmpty())
                        {
                            return NseUtils.commonResponse("Please provide the STP Date",HttpStatus.BAD_REQUEST);
                        }else
                        {
                            sip_date = stp_date;
                        }
                    }
                }

                else if(purchase_type.equalsIgnoreCase("SWP Purchase"))
                {
                    if(StringHelper.isEmpty(folio_no))
                    {
                        return NseUtils.commonResponse("Please provide the Folio Number",HttpStatus.BAD_REQUEST);
                    }

                    if(StringHelper.isEmpty(scheme_name))
                    {
                        return NseUtils.commonResponse("Please provide the Scheme Name",HttpStatus.BAD_REQUEST);
                    }

                    if(StringHelper.isEmpty(scheme_reinvest_tag))
                    {
                        return NseUtils.commonResponse("Please provide the Scheme Reinvest Tag",HttpStatus.BAD_REQUEST);
                    }

                    if(StringHelper.isEmpty(amount_type))
                    {
                        return NseUtils.commonResponse("Please provide the SWP Type",HttpStatus.BAD_REQUEST);
                    }

                    if(!amount_type.isEmpty() && amount_type.equalsIgnoreCase("Amount") && amount.isEmpty())
                    {
                        return NseUtils.commonResponse("Please provide the SWP Amount",HttpStatus.BAD_REQUEST);
                    }

                    if(!amount_type.isEmpty() && amount_type.equalsIgnoreCase("Amount") && amount.equalsIgnoreCase("0"))
                    {
                        return NseUtils.commonResponse("Please provide the Valid SWP Amount",HttpStatus.BAD_REQUEST);
                    }

                    if(!amount_type.isEmpty() && amount_type.equalsIgnoreCase("Amount") && !NumberUtils.isParsable(amount))
                    {
                        return NseUtils.commonResponse("Please provide the Valid SWP Amount",HttpStatus.BAD_REQUEST);
                    }

                    if(StringHelper.isEmpty(start_date))
                    {
                        return NseUtils.commonResponse("Please provide the SWP Start Date",HttpStatus.BAD_REQUEST);
                    }

                    if(StringHelper.isEmpty(frequency))
                    {
                        return NseUtils.commonResponse("Please provide the SWP Frequency",HttpStatus.BAD_REQUEST);
                    }

//                        if(!cart_id.isEmpty())
//                        {
//
//                        }else{
//                            List<CartDto> cartList = userServiceClient.getCartDetailsByUserID(Integer.valueOf(userid),"NSE",investor_code,purchase_type,token);
//                            if (cartList != null && !cartList.isEmpty())
//                            {
//                                return NseUtils.commonResponse(
//                                        "You can add only one SWP Fund in Cart.",
//                                        HttpStatus.BAD_REQUEST
//                                );
//                            }
//                        }

                }
                NseOnlineSchemeMaster nseSchemeMaster = null;
                NseOnlineSchemeMaster nseOnlineSchemeMaster = null;

                String scheme_amfi = "";
                String scheme_amfi_short_name = "";
                String scheme_product_code = "";
                String scheme_company = "";
                String scheme_company_code = "";
                String to_product_name = "";
                String to_scheme_amfi = "";
                String to_scheme_amfi_short_name = "";
                String to_scheme_product_code = "";
                String to_scheme_company = "";
                String to_scheme_company_code = "";

                System.out.println("bse_nse_mfu_flag = " + bse_nse_mfu_flag);

                System.out.println("purchase_type = " + purchase_type);

                if(bse_nse_mfu_flag.equalsIgnoreCase("NSE"))
                {
                    if(purchase_type.equalsIgnoreCase("Lumpsum Purchase"))
                    {
                        if(nfo_flag.equalsIgnoreCase("Y"))
                        {
                            nseOnlineSchemeMaster = cartService.getNSENFOLumpsumSchemecode(scheme_name);
                        }else
                        {
                            nseOnlineSchemeMaster = nseService.getLumpsumSchemecodeService(scheme_name, scheme_reinvest_tag, amount);
                        }

                        if(nseOnlineSchemeMaster != null)
                        {
                            scheme_amfi = nseOnlineSchemeMaster.getSchemeName();
                            scheme_amfi_short_name = nseOnlineSchemeMaster.getSchemeAmfiShortName();
                            scheme_product_code = nseOnlineSchemeMaster.getSchemeCode();
                            scheme_company = nseOnlineSchemeMaster.getAmcName();
                            scheme_company_code = nseOnlineSchemeMaster.getAmcCode();

                            if(scheme_reinvest_tag.isEmpty() && nfo_flag.equalsIgnoreCase("Y"))
                            {
                                scheme_reinvest_tag = nseOnlineSchemeMaster.getDivReinvestFlag();
                            }
                        }else
                        {
                            if(nfo_flag.equalsIgnoreCase("Y"))
                            {
                                if(!scheme_reinvest_tag.isEmpty() && !scheme_reinvest_tag.equalsIgnoreCase("Z"))
                                {
                                    String dividend_type = "";

                                    if(scheme_reinvest_tag.equalsIgnoreCase("N"))
                                    {
                                        dividend_type = "Dividend Payout";
                                    }else if(scheme_reinvest_tag.equalsIgnoreCase("Y"))
                                    {
                                        dividend_type = "Dividend Reinvest";
                                    }

                                    return NseUtils.commonResponse("Selected scheme does't have "+dividend_type+" option. please select another option!", HttpStatus.BAD_REQUEST);
                                }else
                                {
                                    return NseUtils.commonResponse("This scheme not allowed purchase. Please select other scheme!", HttpStatus.BAD_REQUEST);
                                }
                            }else
                            {
                                if(!scheme_reinvest_tag.isEmpty() && !scheme_reinvest_tag.equalsIgnoreCase("Z"))
                                {
                                    String dividend_type = "";

                                    if(scheme_reinvest_tag.equalsIgnoreCase("N"))
                                    {
                                        dividend_type = "Dividend Payout";
                                    }else if(scheme_reinvest_tag.equalsIgnoreCase("Y"))
                                    {
                                        dividend_type = "Dividend Reinvest";
                                    }
                                    return NseUtils.commonResponse("Selected scheme does't have "+dividend_type+" option. please select another option!", HttpStatus.BAD_REQUEST);
                                }else
                                {
                                    return NseUtils.commonResponse("This scheme not allowed purchase. Please select other scheme!", HttpStatus.BAD_REQUEST);
                                }
                            }
                        }

                    }else if(purchase_type.equalsIgnoreCase("SIP Purchase"))
                    {

                        System.out.println("schemeName " + scheme_name);

                        if(nfo_flag.equalsIgnoreCase("Y"))
                        {
                            nseOnlineSchemeMaster = cartService.getNSENFOSipSchemecode(scheme_name);
                        }else
                        {
                            nseOnlineSchemeMaster = cartService.getNSESipSchemecode(scheme_name, scheme_reinvest_tag);
                        }

                        if(nseOnlineSchemeMaster != null)
                        {
                            scheme_amfi = nseOnlineSchemeMaster.getSchemeName();
                            scheme_amfi_short_name = nseOnlineSchemeMaster.getSchemeAmfiShortName();
                            scheme_product_code = nseOnlineSchemeMaster.getSchemeCode();
                            scheme_company = nseOnlineSchemeMaster.getAmcName();
                            scheme_company_code = nseOnlineSchemeMaster.getAmcCode();

                            if(scheme_reinvest_tag.isEmpty() && nfo_flag.equalsIgnoreCase("Y"))
                            {
                                scheme_reinvest_tag = nseOnlineSchemeMaster.getDivReinvestFlag();
                            }
                        }else
                        {
                            if(nfo_flag.equalsIgnoreCase("Y"))
                            {
                                if(!scheme_reinvest_tag.isEmpty() && !scheme_reinvest_tag.equalsIgnoreCase("Z"))
                                {
                                    String dividend_type = "";

                                    if(scheme_reinvest_tag.equalsIgnoreCase("N"))
                                    {
                                        dividend_type = "Dividend Payout";
                                    }else if(scheme_reinvest_tag.equalsIgnoreCase("Y"))
                                    {
                                        dividend_type = "Dividend Reinvest";
                                    }
                                    return NseUtils.commonResponse("Selected scheme does't have "+dividend_type+" option. please select another option!", HttpStatus.BAD_REQUEST);
                                }else
                                {
                                    return NseUtils.commonResponse(""+scheme_name+" not accept the SIP Purchase. Please choose other scheme.", HttpStatus.BAD_REQUEST);
                                }
                            }else
                            {
                                if(!scheme_reinvest_tag.isEmpty() && !scheme_reinvest_tag.equalsIgnoreCase("Z"))
                                {
                                    String dividend_type = "";

                                    if(scheme_reinvest_tag.equalsIgnoreCase("N"))
                                    {
                                        dividend_type = "Dividend Payout";
                                    }else if(scheme_reinvest_tag.equalsIgnoreCase("Y"))
                                    {
                                        dividend_type = "Dividend Reinvest";
                                    }
                                    return NseUtils.commonResponse("Selected scheme does't have "+dividend_type+" option. please select another option!", HttpStatus.BAD_REQUEST);
                                }else
                                {
                                    return NseUtils.commonResponse(""+scheme_name+" not accept the SIP Purchase. Please choose other scheme.", HttpStatus.BAD_REQUEST);
                                }
                            }
                        }
                    }else if(purchase_type.equalsIgnoreCase("Switch Purchase"))
                    {

                        nseOnlineSchemeMaster = cartService.getNSESwitchSchemecode(scheme_name, scheme_reinvest_tag);

                        if(nseOnlineSchemeMaster != null)
                        {
                            scheme_amfi = nseOnlineSchemeMaster.getSchemeName();
                            scheme_amfi_short_name = nseOnlineSchemeMaster.getSchemeAmfiShortName();
                            scheme_product_code = nseOnlineSchemeMaster.getSchemeCode();
                            scheme_company = nseOnlineSchemeMaster.getAmcCode();
                            scheme_company_code = nseOnlineSchemeMaster.getAmcName();
                        }else
                        {
                            if(scheme_reinvest_tag.isEmpty() && scheme_reinvest_tag.equalsIgnoreCase("Z"))
                            {
                                return NseUtils.commonResponse("Selected scheme switch not allowed. Please contact the admin.", HttpStatus.BAD_REQUEST);
                            }else
                            {
                                String dividend_type = "";

                                if(scheme_reinvest_tag.equalsIgnoreCase("N"))
                                {
                                    dividend_type = "Dividend Payout";
                                }else if(scheme_reinvest_tag.equalsIgnoreCase("Y"))
                                {
                                    dividend_type = "Dividend Reinvest";
                                }
                                return NseUtils.commonResponse("Selected scheme does't have " +dividend_type+ " option. please select another option!", HttpStatus.BAD_REQUEST);
                            }
                        }

                        if(!to_scheme_name.isEmpty())
                        {
                            nseOnlineSchemeMaster = cartService.getNSESwitchSchemecode(to_scheme_name, to_scheme_reinvest_tag);

                            if(nseOnlineSchemeMaster != null)
                            {
                                to_scheme_amfi = nseOnlineSchemeMaster.getSchemeName();
                                to_scheme_amfi_short_name = nseOnlineSchemeMaster.getSchemeAmfiShortName();
                                to_scheme_product_code = nseOnlineSchemeMaster.getSchemeCode();
                                to_scheme_company = nseOnlineSchemeMaster.getAmcCode();
                                to_scheme_company_code = nseOnlineSchemeMaster.getAmcName();
                            }else
                            {
                                if(to_scheme_reinvest_tag.isEmpty() && to_scheme_reinvest_tag.equalsIgnoreCase("Z"))
                                {
                                    return NseUtils.commonResponse("Selected scheme not allowed for the fresh purchase. Please contact the admin.", HttpStatus.BAD_REQUEST);
                                }else
                                {
                                    String dividend_type = "";

                                    if(to_scheme_reinvest_tag.equalsIgnoreCase("N"))
                                    {
                                        dividend_type = "Dividend Payout";
                                    }else if(to_scheme_reinvest_tag.equalsIgnoreCase("Y"))
                                    {
                                        dividend_type = "Dividend Reinvest";
                                    }
                                    return NseUtils.commonResponse(""+dividend_type+" Option is not available in " + to_scheme_name + ". Please choose another option.", HttpStatus.BAD_REQUEST);
                                }
                            }
                        }

                        if(!scheme_company_code.isEmpty() && !to_scheme_company_code.isEmpty() && !scheme_company_code.equalsIgnoreCase(to_scheme_company_code))
                        {
                            return NseUtils.commonResponse("Your From Scheme AMC Name and To Scheme Not Matching, Please choose the correct Scheme.", HttpStatus.BAD_REQUEST);
                        }

                    }else if(purchase_type.equalsIgnoreCase("Redemption Purchase"))
                    {
                        nseOnlineSchemeMaster = cartService.getNSERedemSchemeCode(scheme_name, scheme_reinvest_tag);

                        if(nseOnlineSchemeMaster != null)
                        {
                            scheme_amfi = nseOnlineSchemeMaster.getSchemeName();
                            scheme_amfi_short_name = nseOnlineSchemeMaster.getSchemeAmfiShortName();
                            scheme_product_code = nseOnlineSchemeMaster.getSchemeCode();
                            scheme_company = nseOnlineSchemeMaster.getAmcCode();
                            scheme_company_code = nseOnlineSchemeMaster.getAmcName();
                        }else
                        {
                            if(!scheme_reinvest_tag.isEmpty() && scheme_reinvest_tag.equalsIgnoreCase("Z"))
                            {
                                return NseUtils.commonResponse("Selected scheme redemption not allowed. Please select some other scheme", HttpStatus.BAD_REQUEST);
                            }
                            else if(!scheme_reinvest_tag.isEmpty() && !scheme_reinvest_tag.equalsIgnoreCase("Z"))
                            {
                                String dividend_type = "";

                                if(scheme_reinvest_tag.equalsIgnoreCase("N"))
                                {
                                    dividend_type = "Dividend Payout";
                                }else if(scheme_reinvest_tag.equalsIgnoreCase("Y"))
                                {
                                    dividend_type = "Dividend Reinvest";
                                }
                                return NseUtils.commonResponse("Selected scheme does't have "+dividend_type+" option. please select another option!", HttpStatus.BAD_REQUEST);
                            }else
                            {
                                return NseUtils.commonResponse(""+scheme_name+" not accept the Redemption. Please choose other scheme.", HttpStatus.BAD_REQUEST);
                            }
                        }

                    }else if(purchase_type.equalsIgnoreCase("STP Purchase"))
                    {
                        System.out.println("FINDING SCHEME NAME AND SCHEME CODE FROM");

                        List<NseOnlineSchemeMaster> nseSchemeMasterList = nseOnlineSchemeMasterRepository.findSTPEnabledSchemesForMobile(scheme_name, scheme_reinvest_tag);

                        if(!nseSchemeMasterList.isEmpty())
                        {
                            nseSchemeMaster = nseSchemeMasterList.get(0);
                        }

                        System.out.println("FINDING SCHEME NAME AND SCHEME CODE END");

                        if(nseSchemeMaster != null)
                        {
                            scheme_amfi = nseSchemeMaster.getSchemeName();
                            scheme_product_code = nseSchemeMaster.getSchemeCode();
                            scheme_company = nseSchemeMaster.getAmcName();
                            scheme_company_code = nseSchemeMaster.getAmcCode();
                        }else
                        {
                            if(!scheme_reinvest_tag.isEmpty() && scheme_reinvest_tag.equalsIgnoreCase("Z"))
                            {
                                return NseUtils.commonResponse("Selected scheme STP not allowed. Please select some other scheme", HttpStatus.BAD_REQUEST);
                            }
                            else if(!scheme_reinvest_tag.isEmpty() && !scheme_reinvest_tag.equalsIgnoreCase("Z"))
                            {
                                String dividend_type = "";

                                if(scheme_reinvest_tag.equalsIgnoreCase("N"))
                                {
                                    dividend_type = "Dividend Payout";
                                }else if(scheme_reinvest_tag.equalsIgnoreCase("Y"))
                                {
                                    dividend_type = "Dividend Reinvest";
                                }
                                return NseUtils.commonResponse("Selected scheme does't have "+dividend_type+" option. please select another option!", HttpStatus.BAD_REQUEST);
                            }else
                            {
                                return NseUtils.commonResponse(""+scheme_name+" not accept the STP. Please choose other scheme.", HttpStatus.BAD_REQUEST);
                            }
                        }
                        if(!to_scheme_name.isEmpty())
                        {

                            System.out.println("FINDING SCHEME NAME AND SCHEME CODE 1 FROM");

                            System.out.println("to_scheme_name = " + to_scheme_name);
                            System.out.println("to_scheme_reinvest_tag = " + to_scheme_reinvest_tag);

                            nseSchemeMasterList = nseOnlineSchemeMasterRepository.findSTPEnabledSchemesForMobile(to_scheme_name, to_scheme_reinvest_tag);

                            if(!nseSchemeMasterList.isEmpty())
                            {
                                nseSchemeMaster = nseSchemeMasterList.get(0);
                            }

                            System.out.println("FINDING SCHEME NAME AND SCHEME CODE 2 END");

                            if(nseSchemeMaster != null)
                            {
                                to_scheme_amfi = nseSchemeMaster.getSchemeName();
                                to_scheme_product_code = nseSchemeMaster.getSchemeCode();
                                to_scheme_company = nseSchemeMaster.getAmcName();
                                to_scheme_company_code = nseSchemeMaster.getAmcCode();
                            }else
                            {
                                if(to_scheme_reinvest_tag.isEmpty() && to_scheme_reinvest_tag.equalsIgnoreCase("Z"))
                                {
                                    return NseUtils.commonResponse("Selected scheme not allowed for the fresh purchase. Please contact the admin.", HttpStatus.BAD_REQUEST);
                                }else
                                {
                                    String dividend_type = "";

                                    if(to_scheme_reinvest_tag.equalsIgnoreCase("N"))
                                    {
                                        dividend_type = "Dividend Payout";
                                    }else if(to_scheme_reinvest_tag.equalsIgnoreCase("Y"))
                                    {
                                        dividend_type = "Dividend Reinvest";
                                    }
                                    return NseUtils.commonResponse(""+dividend_type+" Option is not available in " + to_scheme_amfi + ". Please choose another option.", HttpStatus.BAD_REQUEST);
                                }
                            }
                        }

                        if(!scheme_company_code.isEmpty() && !to_scheme_company_code.isEmpty() && !scheme_company_code.equalsIgnoreCase(to_scheme_company_code))
                        {
                            return NseUtils.commonResponse("Your From Scheme AMC Name and To Scheme Not Matching, Please choose the correct Scheme.", HttpStatus.BAD_REQUEST);
                        }

                    }else if(purchase_type.equalsIgnoreCase("SWP Purchase"))
                    {
                        Date startDate = sdf.parse(start_date);
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(startDate);

                        start_day = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH));
                        start_month = String.format("%02d", calendar.get(Calendar.MONTH) + 1);
                        start_year = String.valueOf(calendar.get(Calendar.YEAR));
                        if(StringHelper.isNotEmpty(end_date))
                        {
                            Date endDate = sdf.parse(end_date);
                            calendar = Calendar.getInstance();
                            calendar.setTime(endDate);
                        }


                        end_day = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH));
                        end_month = String.format("%02d", calendar.get(Calendar.MONTH) + 1);
                        end_year = String.valueOf(calendar.get(Calendar.YEAR));

                        nseSchemeMaster = nseOnlineSchemeMasterRepository.findSWPEnabledSchemesForMobile(scheme_name, scheme_reinvest_tag);

                        System.out.println("nseSchemeMaster = " + nseSchemeMaster);

                        System.out.println("scheme_reinvest_tag = " + scheme_reinvest_tag);

                        if(nseSchemeMaster != null)
                        {
                            scheme_amfi = nseSchemeMaster.getSchemeName();
                            scheme_product_code = nseSchemeMaster.getSchemeCode();
                            scheme_company = nseSchemeMaster.getAmcName();
                            scheme_company_code = nseSchemeMaster.getAmcCode();
                        }else
                        {
                            if(!scheme_reinvest_tag.isEmpty() && scheme_reinvest_tag.equalsIgnoreCase("Z"))
                            {
                                return NseUtils.commonResponse("Selected scheme SWP not allowed. Please select some other scheme", HttpStatus.BAD_REQUEST);
                            }
                            else if(!scheme_reinvest_tag.isEmpty() || !scheme_reinvest_tag.equalsIgnoreCase("Z"))
                            {
                                String dividend_type = "";

                                if(scheme_reinvest_tag.equalsIgnoreCase("N"))
                                {
                                    dividend_type = "Dividend Payout";
                                }else if(scheme_reinvest_tag.equalsIgnoreCase("Y"))
                                {
                                    dividend_type = "Dividend Reinvest";
                                }
                                return NseUtils.commonResponse("Selected scheme does't have "+dividend_type+" option. please select another option!", HttpStatus.BAD_REQUEST);
                            }else
                            {
                                return NseUtils.commonResponse(""+scheme_name+" not accept the SWP. Please choose other scheme.", HttpStatus.BAD_REQUEST);
                            }
                        }
                    }
                }

                System.out.println("sip dates = " + sip_date);

                if(StringHelper.isNotEmpty(cart_id))
                {
                    cart.setId(Integer.parseInt(cart_id));
                }

                cart.setUser_id(user.getUser_id());
                cart.setName(inv_name);
                cart.setTax_status_desc(tax_status);
                cart.setTax_status_code(tax_status_code);
                cart.setHolding_nature_code(holding_nature_code);
                cart.setHolding_nature_desc(holding_nature);
                cart.setPurchase_type(purchase_type);
                cart.setTrnx_type(trnx_type);
                cart.setVendor(bse_nse_mfu_flag);
                cart.setProduct_name("");

                if(nfo_flag.equalsIgnoreCase("Y"))
                {
                    cart.setNfo_flag(true);
                }else
                {
                    cart.setNfo_flag(false);
                }

                cart.setScheme_name(scheme_amfi);
                cart.setScheme_amfi_short_name(scheme_amfi_short_name);
                cart.setScheme_product_code(scheme_product_code);
                cart.setScheme_company(scheme_company);
                cart.setScheme_company_code(scheme_company_code);
                cart.setScheme_reinvest_tag(scheme_reinvest_tag);
                cart.setTo_product_name(to_product_name);
                cart.setTo_scheme_name(to_scheme_amfi);
                cart.setTo_scheme_amfi_short_name(to_scheme_amfi_short_name);
                cart.setTo_scheme_product_code(to_scheme_product_code);
                cart.setTo_scheme_company(to_scheme_company);
                cart.setTo_scheme_company_code(to_scheme_company_code);
                cart.setTo_scheme_reinvest_tag(to_scheme_reinvest_tag);
                cart.setFolio_no(folio_no);
                cart.setAmount_type(amount_type);
                cart.setAmount(amount);
                cart.setTotal_amount(total_amount);
                cart.setUnits(units);
                cart.setTotal_units(total_units);
                cart.setFrequency(frequency);
                cart.setSip_date(sip_date);
                cart.setStart_date(start_date);
                cart.setEnd_date(end_date);
                cart.setBroker_code(broker_code);
                cart.setInvestor_code(investor_code);
                cart.setEuin_code("");
                cart.setBank_account_number("");
                cart.setBank_ifsc("");
                cart.setBank_name("");
                cart.setPayment_mode("");
                cart.setBank_mandate(mandate_id);
                System.out.println("first_order_flag = " + first_order_flag);
                if(first_order_flag.equalsIgnoreCase("Y") || first_order_flag.equalsIgnoreCase("1"))
                {
                    cart.setFirst_order_flag(true);
                }else {
                    cart.setFirst_order_flag(false);
                }
                cart.setInstallment(installment);
                cart.setStart_day(start_day);
                cart.setStart_month(start_month);
                cart.setStart_year(start_year);
                cart.setEnd_day(end_day);
                cart.setEnd_month(end_month);
                cart.setEnd_year(end_year);
                cart.setTenure(sip_tenure);
                cart.setFirst_date(sip_first_date);
                cart.setSecond_date(sip_second_date);

                if(until_cancel.equalsIgnoreCase("1"))
                {
                    cart.setUntil_cancel(true);
                }else
                {
                    cart.setUntil_cancel(false);
                }

                cart.setStatus("");
                cart.setStatus_date(new Date());
                cart.setActive(true);
                cart.setClient_name(client_name);

                if(step_up_flag.equalsIgnoreCase("Y"))
                {
                    cart.setIs_step_up(true);
                    cart.setStep_up_frequency(step_up_frequency);
                    cart.setStep_up_start_date(step_up_start_date);
                    cart.setStep_up_end_date(step_up_end_date);
                    cart.setStep_up_amount(step_up_amount);
                }else
                {
                    cart.setIs_step_up(false);
                    cart.setStep_up_frequency("");
                    cart.setStep_up_start_date("");
                    cart.setStep_up_end_date("");
                    cart.setStep_up_amount("");
                }

                cart.setRegister_source(source);

                System.out.println("cart = " + cart);

                userServiceClient.saveOrUpdateCart(cart,token);

                if(cart_status)
                {
                    return NseUtils.commonResponse("Cart Details Updated Successfully.", HttpStatus.OK);
                }else
                {
                    return NseUtils.commonResponse("Cart Details Saved Successfully.", HttpStatus.OK);
                }

            }else
            {
                return NseUtils.commonResponse("Investor details not available.", HttpStatus.BAD_REQUEST);
            }
        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get Cart Details by User ID",
            description = "Fetch cart details for a given user ID (from token), vendor, investor code, and purchase type"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully fetched cart details",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CartDto.class),
                            examples = @ExampleObject(value = """
                                [
                                    {
                                        "inv_name": "Gowtham M P",
                                        "tax_status": "Individual",
                                        "tax_status_code": "01",
                                        "holding_nature": "SINGLE",
                                        "holding_nature_code": "SI",
                                        "broker_code": "ARN-175151",
                                        "investor_code": "ARN-175151",
                                        "logo": "http://localhost:8084/images/amc-logo//images/vendors/nse.png",
                                        "bse_nse_mfu_flag": "NSE",
                                        "scheme_list": [
                                            {
                                                "id": 23894,
                                                "scheme_name": "Axis Liquid Fund - Regular Plan - Growth Option",
                                                "scheme_product_code": "CFGPGGR",
                                                "amount": "1000",
                                                "start_date": "08-08-2025",
                                                "end_date": "01-08-2055",
                                                "frequency": "Q",
                                                "vendor": "NSE",
                                                "folio_no": "910182691781",
                                                "scheme_logo": "http://localhost:8084/images/amc-logo/axis.png"
                                            }
                                        ]
                                    }
                                ]
                                """)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "User not found or No Cart found",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
//    @GetMapping("/getCartDetailsByUserID")
//    public ResponseEntity<?> getCartDetailsByUserID(@RequestParam String vendor,
//                                                    @RequestParam String investorCode,
//                                                    @RequestParam String purchaseType,
//                                                    @RequestHeader("Authorization") String token)
//    {
//        String userId = "";
//        try {
//            userId = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
//            UserDto userOpt = userServiceClient.getUserById(Integer.valueOf(userId), token);
//
//            List<CartPojo> cartPojoList = new ArrayList<CartPojo>();
//            if (userOpt != null)
//            {
//                List<CartDto> cartList;
//                try
//                {
//                    System.out.println("userid = " + userId);
//                    cartList = userServiceClient.getCartDetailsByUserID(
//                            Integer.valueOf(userId),
//                            vendor,
//                            investorCode,
//                            purchaseType,
//                            token
//                    );
//
//                    for (CartDto cartItem : cartList)
//                    {
//                        if (cartItem.getScheme_name() != null && !cartItem.getScheme_name().isEmpty())
//                        {
//                            String logoPath = amcLogoPath + NseUtils.getLogoByAmcNameOrSchemeName(cartItem.getScheme_name());
//                            cartItem.setScheme_logo(logoPath);
//                        }
//                    }
//                    CartPojo cartPojo = new CartPojo();
//                    cartPojo.setInv_name(userOpt.getName());
//                    cartPojo.setTax_status(userOpt.getTax_status());
//                    cartPojo.setTax_status_code(userOpt.getTax_status_code());
//                    cartPojo.setHolding_nature(userOpt.getHolding_nature());
//                    cartPojo.setHolding_nature_code(userOpt.getHolding_nature_code());
//                    cartPojo.setBroker_code(userOpt.getBroker_code());
//                    cartPojo.setInvestor_code(investorCode);
//                    cartPojo.setLogo(vendorLogoPath + NseUtils.getVendorImage(vendor));
//                    cartPojo.setBse_nse_mfu_flag(vendor);
//                    cartPojo.setScheme_list(cartList);
//                    cartPojoList.add(cartPojo);
//
//                } catch (FeignException e)
//                {
//                    if (e.status() == 400)
//                    {
//                        return ResponseEntity.ok(cartPojoList);
//                    } else if (e.status() == 404)
//                    {
//                        return ResponseEntity.ok(cartPojoList);
//                    } else
//                    {
//                        return NseUtils.commonResponse("Downstream service error.", HttpStatus.INTERNAL_SERVER_ERROR);
//                    }
//                }
//
//                if (!cartList.isEmpty())
//                {
//                    return ResponseEntity.ok(cartPojoList);
//                } else
//                {
//                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
//                            Map.of("status", HttpStatus.BAD_REQUEST, "status_msg", "No Cart found")
//                    );
//                }
//            } else {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
//                        Map.of("status", 404, "status_msg", "User not found")
//                );
//            }
//
//        } catch (Exception ex)
//        {
//            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
//            ex.printStackTrace();
//            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//
//    }

    @GetMapping("/getCartDetailByUserID")
    public ResponseEntity<?> getCartDetailsByUserID(@RequestParam String payment_status,
                                                    @RequestParam String purchase_type,
                                                    @RequestHeader("Authorization") String token) {
        String userId = "";
        UserDto user = null;

        try
        {
            userId = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            UserDto userOpt = userServiceClient.getUserById(Integer.valueOf(userId), token);

            String client_name = userOpt.getClient_name();

            user = userServiceClient.getUserDetailsByID( client_name,Integer.parseInt(userId),token);

            System.out.println("User ID = " + userId + ", Client Name = " + client_name + ", Payment Status = " + payment_status + ", Purchase Type = " + purchase_type);

            System.out.println("User Details = " + user.getId());

            if(user != null)
            {

                List<CartDto> cartList = cartService.getCartListByUserId(Integer.valueOf(userId), client_name, payment_status, purchase_type,token);

                System.out.println("Cart List = " + cartList);

                if(cartList == null || cartList.size() == 0) {
                    return NseUtils.commonResponse("No Cart Found", HttpStatus.OK);
                }

                if(cartList != null && cartList.size() > 0)
                {
                    List<String> distinctInvestorCodes = cartList.stream().map(CartDto::getInvestor_code).distinct().collect(Collectors.toList());

                    List<CartPojo> cartPojoList = new ArrayList<CartPojo>();

                    for (String investorCode : distinctInvestorCodes)
                    {
                        List<CartDto> filteredList = cartList.stream().filter(cart -> cart.getInvestor_code().equalsIgnoreCase(investorCode)).sorted(Comparator.comparingInt(CartDto::getId).reversed()).collect(Collectors.toList());
                        System.out.println("filteredList = " + filteredList);
                        if(filteredList != null && filteredList.size() > 0)
                        {
                            CartDto cart = filteredList.get(0);

                            for (CartDto filtercart : filteredList)
                            {

                                String logoName = null;
                                if(filtercart.getPurchase_type().equalsIgnoreCase("Redemption Purchase"))
                                {
                                    logoName = NseUtils.getLogoByAmcNameOrSchemeName(filtercart.getScheme_company());
                                }else if(filtercart.getPurchase_type().equalsIgnoreCase("Switch Purchase"))
                                {
                                    logoName = NseUtils.getLogoByAmcNameOrSchemeName(filtercart.getScheme_company());
                                }else{
                                    logoName = NseUtils.getLogoByAmcNameOrSchemeName(filtercart.getScheme_company_code());
                                }

                                String logo = amcLogoPath + logoName;

                                filtercart.setScheme_logo(logo);

                                logoName = NseUtils.getLogoByAmcNameOrSchemeName(filtercart.getTo_scheme_name());
                                logo = amcLogoPath + logoName;

                                filtercart.setTo_scheme_logo(logo);
                            }

                            CartPojo cartPojo = new CartPojo();
                            cartPojo.setInv_name(cart.getName());
                            cartPojo.setTax_status(cart.getTax_status_desc());
                            cartPojo.setTax_status_code(cart.getTax_status_code());
                            cartPojo.setHolding_nature(cart.getHolding_nature_desc());
                            cartPojo.setHolding_nature_code(cart.getHolding_nature_code());
                            cartPojo.setBroker_code(cart.getBroker_code());
                            cartPojo.setInvestor_code(cart.getInvestor_code());
                            cartPojo.setLogo(vendorLogoPath +  NseUtils.getVendorImage(cart.getVendor()));
                            cartPojo.setBse_nse_mfu_flag(cart.getVendor());
                            cartPojo.setScheme_list(filteredList);
                            cartPojoList.add(cartPojo);
                        }
                    }

                    CartResponse apiResponse = new CartResponse();
                    apiResponse.setStatus(200);
                    apiResponse.setStatus_msg("Success Cart Fetched Successfully.");
                    apiResponse.setMsg("Cart Fetched Successfully.");
                    apiResponse.setResult(cartPojoList);
                    return new ResponseEntity<CartResponse>(apiResponse,HttpStatus.OK);
                }

            }else
            {
                return NseUtils.commonResponse("Investor details not available.", HttpStatus.BAD_REQUEST);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Operation(
            summary = "Get Cart Status By User ID",
            description = "Fetches cart status based on user ID and payment ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cart status retrieved successfully", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CartDto.class)
            )),
            @ApiResponse(responseCode = "400", description = "Missing or invalid parameters", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = "{\"status\": 400, \"status_msg\": \"Please provide the Payment Id\", \"message\": \"\"}")
            )),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
    })
    @PostMapping("/getCartStatusByUserId")
    public ResponseEntity<?> getCartStatusByUserId(HttpServletRequest request,
                                                   @RequestHeader("Authorization") String token,
                                                   @RequestParam String payment_id)
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
            if(StringHelper.isEmpty(userid))
            {
                 return NseUtils.commonResponse("Please provide the User Id", HttpStatus.BAD_REQUEST);
            }
            if(StringHelper.isEmpty(payment_id))
            {
                return NseUtils.commonResponse("Please provide the Payment Id", HttpStatus.BAD_REQUEST);
            }
            user = userServiceClient.getUserDetailsByID(client_name,Integer.parseInt(userid),token);

            if(user != null)
            {
                List<CartDto> cartList = userServiceClient.getSuccessCartListByUserIdAndPaymentType(user.getId(), client_name, payment_id,token);

                if(cartList != null && cartList.size() > 0)
                {
                    List<String> distinctInvestorCodes = cartList.stream().map(CartDto::getInvestor_code).distinct().collect(Collectors.toList());

                    List<CartPojo> cartPojoList = new ArrayList<CartPojo>();


                    String path = amcLogoPath;

                    for (String investorCode : distinctInvestorCodes)
                    {
                        List<CartDto> filteredList = cartList.stream().filter(cart -> cart.getInvestor_code().equalsIgnoreCase(investorCode)).sorted(Comparator.comparingInt(CartDto::getId)).collect(Collectors.toList());

                        if(filteredList != null && filteredList.size() > 0)
                        {
                            CartDto cart = filteredList.get(0);

                            for (CartDto filtercart : filteredList)
                            {
                                String logoName = NseUtils.getLogoByAmcNameOrSchemeName(filtercart.getScheme_name());
                                String logo = path + logoName;

                                filtercart.setScheme_logo(logo);

                                logoName = NseUtils.getLogoByAmcNameOrSchemeName(filtercart.getTo_scheme_name());
                                logo = path + logoName;

                                filtercart.setTo_scheme_logo(logo);
                            }

                            CartPojo cartPojo = new CartPojo();
                            cartPojo.setInv_name(cart.getName());
                            cartPojo.setTax_status(cart.getTax_status_desc());
                            cartPojo.setTax_status_code(cart.getTax_status_code());
                            cartPojo.setHolding_nature(cart.getHolding_nature_desc());
                            cartPojo.setHolding_nature_code(cart.getHolding_nature_code());
                            cartPojo.setBroker_code(cart.getBroker_code());
                            cartPojo.setInvestor_code(cart.getInvestor_code());
                            cartPojo.setLogo(vendorLogoPath + NseUtils.getVendorImage(cart.getVendor()));
                            cartPojo.setBse_nse_mfu_flag(cart.getVendor());
                            cartPojo.setScheme_list(filteredList);
                            cartPojoList.add(cartPojo);
                        }
                    }

                    CartResponse apiResponse = new CartResponse();
                    apiResponse.setStatus(200);
                    apiResponse.setMsg("Success Cart Fetched Successfully.");
                    apiResponse.setResult(cartPojoList);

                    return ResponseEntity.ok(apiResponse);
                } else
                {
                    return NseUtils.commonResponse("Your Cart is Empty, Please add the Schemes", HttpStatus.BAD_REQUEST);
                }
            } else
            {
                return NseUtils.commonResponse("Investor details not available.", HttpStatus.BAD_REQUEST);
            }

        }catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Delete Cart by Cart ID",
            description = "Deletes a specific cart entry based on cart ID and user authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cart deleted successfully", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = "{\"status\": 200, \"message\": \"Cart deleted successfully\"}")
            )),
            @ApiResponse(responseCode = "400", description = "Invalid user or cart ID", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = "{\"status\": 400, \"message\": \"User not found\"}")
            )),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PostMapping("/deleteCartById")
    public ResponseEntity<?> deleteCartById(HttpServletRequest request, @RequestHeader("Authorization") String token,@RequestParam String cart_id)
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
            if(StringHelper.isEmpty(client_name))
            {
                return NseUtils.commonResponse("Please provide the Client Name", HttpStatus.BAD_REQUEST);
            }

            if(StringHelper.isEmpty(userid))
            {
                return NseUtils.commonResponse("Please provide the User Id", HttpStatus.BAD_REQUEST);
            }

            if(StringHelper.isEmpty(cart_id))
            {
                return NseUtils.commonResponse("Please provide the Card Id", HttpStatus.BAD_REQUEST);
            }

            user = userServiceClient.getUserDetailsByID(client_name,Integer.parseInt(userid),token);

            if(user != null)
            {
                boolean isDeleted = userServiceClient.DeleteCartUserById(Integer.parseInt(userid),cart_id,client_name,token);

                if(isDeleted)
                {
                    return NseUtils.commonResponse("Cart deleted successfully.", HttpStatus.OK);
                }else
                {
                    return NseUtils.commonResponse("Failed to delete the cart.", HttpStatus.BAD_REQUEST);
                }
            }
            else
            {
               return NseUtils.commonResponse("Investor details not available.", HttpStatus.BAD_REQUEST);
            }

        }catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Delete All Cart Items",
            description = "Deletes all cart items for the logged-in user based on cart type"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cart deleted successfully", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = "{\"status\": 200, \"status_msg\": \"Cart deleted successfully.\"}")
            )),
            @ApiResponse(responseCode = "400", description = "User not found or invalid parameters", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = "{\"status\": 400, \"status_msg\": \"User not found\"}")
            )),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
    })
    @PostMapping("/deleteAllCart")
    public ResponseEntity<?> deleteAllCart(HttpServletRequest request,
                                           @RequestHeader("Authorization") String token,
                                           @RequestParam String cart_type)
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

            if(StringHelper.isEmpty(client_name))
            {
                return NseUtils.commonResponse("Please provide the Client Name", HttpStatus.BAD_REQUEST);
            }

            if(StringHelper.isEmpty(userid))
            {
                return NseUtils.commonResponse("Please provide the User Id", HttpStatus.BAD_REQUEST);
            }

            if(StringHelper.isEmpty(cart_type))
            {
                return NseUtils.commonResponse("Please provide the Card Type", HttpStatus.BAD_REQUEST);
            }

            user = userServiceClient.getUserDetailsByID(client_name,Integer.parseInt(userid),token);

            System.out.println("user = " + user);

            if(user != null)
            {
                boolean isDeleted = userServiceClient.deleteAllCart(Integer.parseInt(userid),cart_type,client_name,"NSE",token);

                if(isDeleted)
                {
                    return NseUtils.commonResponse("Cart deleted successfully.", HttpStatus.OK);
                }else
                {
                    return NseUtils.commonResponse("Failed to delete the cart.", HttpStatus.BAD_REQUEST);
                }
            }
            else
            {
                return NseUtils.commonResponse("Investor details not available.", HttpStatus.BAD_REQUEST);
            }

        }catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value="/saveBasketSchemesInCart")
    public ResponseEntity<?> saveBasketSchemesInCart(@RequestParam("Authorization") String token,
                                                     @RequestBody SipCartRequestBodyPojo sipRequest,
                                                     @RequestParam String user_id,
                                                     @RequestParam String inv_name,
                                                     @RequestParam String tax_status,
                                                     @RequestParam String tax_status_code,
                                                     @RequestParam String holding_nature,
                                                     @RequestParam String holding_nature_code,
                                                     @RequestParam String broker_code,
                                                     @RequestParam String euin_code,
                                                     @RequestParam String investor_code,
                                                     @RequestParam String purchase_type,
                                                     @RequestParam String bse_nse_mfu_flag) throws Exception
    {
        UserDto user = null;
        CartDto cart = null;
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        NseOnlineSchemeMaster nseSchemeMaster = null;
        try
        {
            String client_name = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);

            List<SipCartRequestPojo> sipRequestList = sipRequest.getList();

            System.out.println("sipRequestList = " + sipRequestList.size());

            if(StringHelper.isEmpty(bse_nse_mfu_flag))
            {
                return NseUtils.commonResponse("Please provide the bse_nse_mfu_flag", HttpStatus.BAD_REQUEST);
            }

            if(StringHelper.isEmpty(inv_name))
            {
                return NseUtils.commonResponse("Please provide the Investor Name", HttpStatus.BAD_REQUEST);
            }

            if(StringHelper.isEmpty(tax_status))
            {
                return NseUtils.commonResponse("Please provide the Tax Status", HttpStatus.BAD_REQUEST);
            }

            if(StringHelper.isEmpty(tax_status_code))
            {
                return NseUtils.commonResponse("Please provide the Tax Status Code", HttpStatus.BAD_REQUEST);
            }

            if(StringHelper.isEmpty(holding_nature))
            {
                return NseUtils.commonResponse("Please provide the Holding Nature", HttpStatus.BAD_REQUEST);
            }

            if(StringHelper.isEmpty(holding_nature_code))
            {
                return NseUtils.commonResponse("Please provide the Holding Nature Code", HttpStatus.BAD_REQUEST);
            }

            if(StringHelper.isEmpty(broker_code))
            {
                return NseUtils.commonResponse("Please provide the Broker Code", HttpStatus.BAD_REQUEST);
            }

            if(StringHelper.isEmpty(euin_code))
            {
                return NseUtils.commonResponse("Please provide the Euin Code", HttpStatus.BAD_REQUEST);
            }

            user = userServiceClient.getUserDetailsByID( client_name,Integer.parseInt(user_id),token);

            if(user != null)
            {
                if(purchase_type.equalsIgnoreCase("Lumpsum Purchase"))
                {
                    String start_day = "";
                    String start_month = "";
                    String start_year = "";

                    String end_day = "";
                    String end_month = "";
                    String end_year = "";

                    for (SipCartRequestPojo sipCartRequestPojo : sipRequestList)
                    {
                        String scheme_name = NseUtils.checkParem(sipCartRequestPojo.getScheme_name());
                        String scheme_reinvest_tag = NseUtils.checkParem(sipCartRequestPojo.getScheme_reinvest_tag());
                        String amount = NseUtils.checkParem(sipCartRequestPojo.getAmount());

                        if(StringHelper.isEmpty(scheme_name))
                        {
                            return NseUtils.commonResponse("Please provide the Scheme Name", HttpStatus.BAD_REQUEST);
                        }

                        if(StringHelper.isEmpty(scheme_reinvest_tag))
                        {
                            return NseUtils.commonResponse("Please provide the Scheme Reinvest Tag", HttpStatus.BAD_REQUEST);
                        }

                        if(StringHelper.isEmpty(amount))
                        {
                            return NseUtils.commonResponse("Please provide the Amount", HttpStatus.BAD_REQUEST);
                        }

                        if(bse_nse_mfu_flag.equalsIgnoreCase("NSE"))
                        {
                            nseSchemeMaster = nseService.getNSELumpsumSchemecode(scheme_name, scheme_reinvest_tag);

                            if(nseSchemeMaster == null)
                            {
                                return NseUtils.commonResponse("This scheme not allowed purchase. Please select other scheme!", HttpStatus.BAD_REQUEST);
                            }
                        }else
                        {
                            return NseUtils.commonResponse("Please provide the Valid bse_nse_mfu_flag", HttpStatus.BAD_REQUEST);
                        }

                        String minAmount = "";

                        if(bse_nse_mfu_flag.equalsIgnoreCase("NSE"))
                        {
                            minAmount = nseService.getNSELumpsumMinAmountBySchemeName(scheme_name, purchase_type, scheme_reinvest_tag);
                        }

                        if(StringHelper.isNotEmpty(minAmount) && !minAmount.equalsIgnoreCase("Scheme Code not available"))
                        {
                            int minAmt = Integer.parseInt(minAmount);
                            int amt = Integer.parseInt(amount);

                            if(minAmt > amt)
                            {
                                return NseUtils.commonResponse("Entered amount less than min product limits for "+scheme_name+", (Min amount shoud be Rs."+minAmt+")", HttpStatus.BAD_REQUEST);
                            }
                        }
                    }

                    for (SipCartRequestPojo sipCartRequestPojo : sipRequestList)
                    {
                        String scheme_name = NseUtils.checkParem(sipCartRequestPojo.getScheme_name());
                        String scheme_reinvest_tag = NseUtils.checkParem(sipCartRequestPojo.getScheme_reinvest_tag());
                        String amount = NseUtils.checkParem(sipCartRequestPojo.getAmount());
                        String sip_date = NseUtils.checkParem(sipCartRequestPojo.getSip_date());
                        String start_date = NseUtils.checkParem(sipCartRequestPojo.getStart_date());
                        String end_date = NseUtils.checkParem(sipCartRequestPojo.getEnd_date());
                        String installment = NseUtils.checkParem(sipCartRequestPojo.getInstallment());
                        String sip_tenure = NseUtils.checkParem(sipCartRequestPojo.getSip_tenure());

                        String frequency = "";
                        String trnx_type = "FP";
                        String folio_no = "New Folio";

                        String scheme_amfi = "";
                        String scheme_amfi_short_name = "";
                        String scheme_product_code = "";
                        String scheme_company = "";
                        String scheme_company_code = "";
                        String to_product_name = "";
                        String to_scheme_amfi = "";
                        String to_scheme_amfi_short_name = "";
                        String to_scheme_product_code = "";
                        String to_scheme_company = "";
                        String to_scheme_company_code = "";
                        String to_scheme_reinvest_tag = "";

                        if(bse_nse_mfu_flag.equalsIgnoreCase("NSE"))
                        {
                            nseSchemeMaster = nseService.getNSELumpsumSchemecode(scheme_name, scheme_reinvest_tag);

                            if(nseSchemeMaster != null)
                            {
                                scheme_amfi = nseSchemeMaster.getSchemeAmfiCode();
                                scheme_amfi_short_name = nseSchemeMaster.getSchemeAmfiShortName();
                                scheme_product_code = nseSchemeMaster.getSchemeCode();
                                scheme_company = nseSchemeMaster.getAmcName();
                                scheme_company_code = nseSchemeMaster.getAmcCode();

                                if(scheme_reinvest_tag.isEmpty())
                                {
                                    scheme_reinvest_tag = nseSchemeMaster.getDivReinvestFlag();
                                }
                            }

                        }

                        cart = new CartDto();
                        cart.setUser_id(user.getId());
                        cart.setName(inv_name);
                        cart.setTax_status_desc(tax_status);
                        cart.setTax_status_code(tax_status_code);
                        cart.setHolding_nature_code(holding_nature_code);
                        cart.setHolding_nature_desc(holding_nature);
                        cart.setPurchase_type("Lumpsum Purchase");
                        cart.setTrnx_type(trnx_type);
                        cart.setVendor(bse_nse_mfu_flag);
                        cart.setProduct_name(scheme_amfi);
                        cart.setScheme_name(scheme_amfi);
                        cart.setScheme_amfi_short_name(scheme_amfi_short_name);
                        cart.setScheme_product_code(scheme_product_code);
                        cart.setScheme_company(scheme_company);
                        cart.setScheme_company_code(scheme_company_code);
                        cart.setScheme_reinvest_tag(scheme_reinvest_tag);
                        cart.setTo_product_name(to_product_name);
                        cart.setTo_scheme_name(to_scheme_amfi);
                        cart.setTo_scheme_amfi_short_name(to_scheme_amfi_short_name);
                        cart.setTo_scheme_product_code(to_scheme_product_code);
                        cart.setTo_scheme_company(to_scheme_company);
                        cart.setTo_scheme_company_code(to_scheme_company_code);
                        cart.setTo_scheme_reinvest_tag(to_scheme_reinvest_tag);
                        cart.setFolio_no(folio_no);
                        cart.setAmount_type("");
                        cart.setAmount(amount);
                        cart.setTotal_amount(amount);
                        cart.setUnits("");
                        cart.setTotal_units("");
                        cart.setFrequency(frequency);
                        cart.setSip_date(sip_date);
                        cart.setStart_date(start_date);
                        cart.setEnd_date(end_date);
                        cart.setBroker_code(broker_code);
                        cart.setInvestor_code(investor_code);
                        cart.setEuin_code(euin_code);
                        cart.setBank_account_number("");
                        cart.setBank_ifsc("");
                        cart.setBank_name("");
                        cart.setPayment_mode("");
                        cart.setInstallment(installment);
                        cart.setStart_day(start_day);
                        cart.setStart_month(start_month);
                        cart.setStart_year(start_year);
                        cart.setEnd_day(end_day);
                        cart.setEnd_month(end_month);
                        cart.setEnd_year(end_year);
                        cart.setTenure(sip_tenure);
                        cart.setUntil_cancel(true);
                        cart.setStatus("");
                        cart.setStatus_date(new Date());
                        cart.setActive(true);
                        cart.setClient_name(client_name);
                        userServiceClient.saveOrUpdateCart(cart,token);
                    }
                }else if(purchase_type.equalsIgnoreCase("SIP Purchase"))
                {
                    String start_day = "";
                    String start_month = "";
                    String start_year = "";

                    String end_day = "";
                    String end_month = "";
                    String end_year = "";

                    for (SipCartRequestPojo sipCartRequestPojo : sipRequestList)
                    {
                        String scheme_name = NseUtils.checkParem(sipCartRequestPojo.getScheme_name());
                        String scheme_reinvest_tag = NseUtils.checkParem(sipCartRequestPojo.getScheme_reinvest_tag());
                        String amount = NseUtils.checkParem(sipCartRequestPojo.getAmount());
                        String start_date = NseUtils.checkParem(sipCartRequestPojo.getStart_date());
                        String end_date = NseUtils.checkParem(sipCartRequestPojo.getEnd_date());
                        String installment = NseUtils.checkParem(sipCartRequestPojo.getInstallment());
                        String sip_tenure = NseUtils.checkParem(sipCartRequestPojo.getSip_tenure());

                        if(StringHelper.isEmpty(scheme_name))
                        {
                            return NseUtils.commonResponse("Please provide the Scheme Name", HttpStatus.BAD_REQUEST);
                        }

                        if(StringHelper.isEmpty(scheme_reinvest_tag))
                        {
                            return NseUtils.commonResponse("Please provide the Scheme Reinvest Tag", HttpStatus.BAD_REQUEST);
                        }

                        if(StringHelper.isEmpty(amount))
                        {
                            return NseUtils.commonResponse("Please provide the Amount", HttpStatus.BAD_REQUEST);
                        }

                        String minAmount = "";
                        String frequency = "";
                        String trnx_type = "FP";

                        if(bse_nse_mfu_flag.equalsIgnoreCase("NSE"))
                        {
                            frequency = "OM";
                        }

                        if(bse_nse_mfu_flag.equalsIgnoreCase("NSE"))
                        {
                            minAmount = nseService.getNSESipMinimumAmount(scheme_name, scheme_reinvest_tag, trnx_type);
                        }

                        if(StringHelper.isNotEmpty(minAmount) && !minAmount.equalsIgnoreCase("Scheme Code not available"))
                        {
                            int minAmt = Integer.parseInt(minAmount);
                            int amt = Integer.parseInt(amount);

                            if(minAmt > amt)
                            {
                                return NseUtils.commonResponse("Entered amount less than min product limits for "+scheme_name+", (Min amount shoud be Rs."+minAmt+")", HttpStatus.BAD_REQUEST);
                            }
                        }

                        if(StringHelper.isEmpty(start_date))
                        {
                            return NseUtils.commonResponse("Please provide Start Date", HttpStatus.BAD_REQUEST);
                        }

                        if(StringHelper.isEmpty(end_date))
                        {
                            return NseUtils.commonResponse("Please provide End Date", HttpStatus.BAD_REQUEST);
                        }

                        if(bse_nse_mfu_flag.equalsIgnoreCase("NSE"))
                        {
                            nseSchemeMaster = nseService.getNSESipSchemecode(scheme_name, scheme_reinvest_tag);

                            if(nseSchemeMaster == null)
                            {
                                return NseUtils.commonResponse(""+scheme_name+" not accept the SIP Purchase. Please choose other scheme.", HttpStatus.BAD_REQUEST);
                            }
                        }else
                        {
                            return NseUtils.commonResponse("Please provide the Valid bse_nse_mfu_flag", HttpStatus.BAD_REQUEST);
                        }
                    }

                    for (SipCartRequestPojo sipCartRequestPojo : sipRequestList)
                    {
                        String scheme_name = NseUtils.checkParem(sipCartRequestPojo.getScheme_name());
                        String scheme_reinvest_tag = NseUtils.checkParem(sipCartRequestPojo.getScheme_reinvest_tag());
                        String amount = NseUtils.checkParem(sipCartRequestPojo.getAmount());
                        String sip_date = NseUtils.checkParem(sipCartRequestPojo.getSip_date());
                        String start_date = NseUtils.checkParem(sipCartRequestPojo.getStart_date());
                        String end_date = NseUtils.checkParem(sipCartRequestPojo.getEnd_date());
                        String installment = NseUtils.checkParem(sipCartRequestPojo.getInstallment());
                        String sip_tenure = NseUtils.checkParem(sipCartRequestPojo.getSip_tenure());

                        String frequency = "";
                        String trnx_type = "FP";
                        String folio_no = "New Folio";

                        if(bse_nse_mfu_flag.equalsIgnoreCase("NSE"))
                        {
                            frequency = "OM";
                        }else if(bse_nse_mfu_flag.equalsIgnoreCase("BSE"))
                        {
                            frequency = "MONTHLY";
                        }else if(bse_nse_mfu_flag.equalsIgnoreCase("MFU"))
                        {
                            frequency = "M";
                        }

                        String scheme_amfi = "";
                        String scheme_amfi_short_name = "";
                        String scheme_product_code = "";
                        String scheme_company = "";
                        String scheme_company_code = "";
                        String to_product_name = "";
                        String to_scheme_amfi = "";
                        String to_scheme_amfi_short_name = "";
                        String to_scheme_product_code = "";
                        String to_scheme_company = "";
                        String to_scheme_company_code = "";
                        String to_scheme_reinvest_tag = "";

                        if(bse_nse_mfu_flag.equalsIgnoreCase("NSE"))
                        {
                            nseSchemeMaster = nseService.getNSESipSchemecode(scheme_name, scheme_reinvest_tag);

                            if(nseSchemeMaster != null)
                            {
                                scheme_amfi = nseSchemeMaster.getSchemeAmfiCode();
                                scheme_amfi_short_name = nseSchemeMaster.getSchemeAmfiShortName();
                                scheme_product_code = nseSchemeMaster.getSchemeCode();
                                scheme_company = nseSchemeMaster.getAmcName();
                                scheme_company_code = nseSchemeMaster.getAmcCode();

                                if(scheme_reinvest_tag.isEmpty())
                                {
                                    scheme_reinvest_tag = nseSchemeMaster.getDivReinvestFlag();
                                }
                            }
                        }

                        cart = new CartDto();
                        cart.setUser_id(user.getId());
                        cart.setName(inv_name);
                        cart.setTax_status_desc(tax_status);
                        cart.setTax_status_code(tax_status_code);
                        cart.setHolding_nature_code(holding_nature_code);
                        cart.setHolding_nature_desc(holding_nature);
                        cart.setPurchase_type("SIP Purchase");
                        cart.setTrnx_type(trnx_type);
                        cart.setVendor(bse_nse_mfu_flag);
                        cart.setProduct_name(scheme_amfi);
                        cart.setScheme_name(scheme_amfi);
                        cart.setScheme_amfi_short_name(scheme_amfi_short_name);
                        cart.setScheme_product_code(scheme_product_code);
                        cart.setScheme_company(scheme_company);
                        cart.setScheme_company_code(scheme_company_code);
                        cart.setScheme_reinvest_tag(scheme_reinvest_tag);
                        cart.setTo_product_name(to_product_name);
                        cart.setTo_scheme_name(to_scheme_amfi);
                        cart.setTo_scheme_amfi_short_name(to_scheme_amfi_short_name);
                        cart.setTo_scheme_product_code(to_scheme_product_code);
                        cart.setTo_scheme_company(to_scheme_company);
                        cart.setTo_scheme_company_code(to_scheme_company_code);
                        cart.setTo_scheme_reinvest_tag(to_scheme_reinvest_tag);
                        cart.setFolio_no(folio_no);
                        cart.setAmount_type("");
                        cart.setAmount(amount);
                        cart.setTotal_amount(amount);
                        cart.setUnits("");
                        cart.setTotal_units("");
                        cart.setFrequency(frequency);
                        cart.setSip_date(sip_date);
                        cart.setStart_date(start_date);
                        cart.setEnd_date(end_date);
                        cart.setBroker_code(broker_code);
                        cart.setInvestor_code(investor_code);
                        cart.setEuin_code(euin_code);
                        cart.setBank_account_number("");
                        cart.setBank_ifsc("");
                        cart.setBank_name("");
                        cart.setPayment_mode("");
                        cart.setInstallment(installment);
                        cart.setStart_day(start_day);
                        cart.setStart_month(start_month);
                        cart.setStart_year(start_year);
                        cart.setEnd_day(end_day);
                        cart.setEnd_month(end_month);
                        cart.setEnd_year(end_year);
                        cart.setTenure(sip_tenure);
                        cart.setUntil_cancel(true);
                        cart.setStatus("");
                        cart.setStatus_date(new Date());
                        cart.setActive(true);
                        cart.setClient_name(client_name);
                        userServiceClient.saveOrUpdateCart(cart,token);
                    }
                }

                    return NseUtils.commonResponse("Cart Details Saved Successfully.", HttpStatus.OK);

                }else
                {
                    return NseUtils.commonResponse("Investor details not available.", HttpStatus.BAD_REQUEST);
                }

        }
        catch(Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date()); ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value="/addorUpdateBasket")
    public ResponseEntity<?> addorUpdateBasket(@RequestHeader("Authorization") String token,@RequestParam String user_id,
                                               @RequestParam String basket_name,@RequestParam String id,@RequestParam String scheme_name,@RequestParam String amount) throws Exception
    {
        CommonResponses commonResponse = new CommonResponses();
        UserDto user = null;
        Integer log_id = null;
        try
        {
            String client_name = TokenInterceptor.extractClientNamedFromToken(token, secretKey);

            if(StringHelper.isEmpty(user_id))
            {
                return NseUtils.commonResponse("Please provide the User Id", HttpStatus.BAD_REQUEST);
            }
            if(StringHelper.isEmpty(basket_name))
            {
                return NseUtils.commonResponse("Please provide basket name", HttpStatus.BAD_REQUEST);
            }

            if (StringHelper.isEmpty(scheme_name))
            {
                return NseUtils.commonResponse("Please provide scheme name", HttpStatus.BAD_REQUEST);
            }
            if (StringHelper.isEmpty(amount))
            {
                return NseUtils.commonResponse("Please provide amount", HttpStatus.BAD_REQUEST);
            }

            if(!NumberUtils.isParsable(amount))
            {
                return NseUtils.commonResponse("Invalid Amount", HttpStatus.BAD_REQUEST);
            }

            if(StringHelper.isNotEmpty(id) && !NumberUtils.isParsable(id))
            {
                return NseUtils.commonResponse("Please provide the valid Id", HttpStatus.BAD_REQUEST);
            }

                if(!NumberUtils.isParsable(user_id))
                {
                    return NseUtils.commonResponse("Please provide the valid user Id", HttpStatus.BAD_REQUEST);
                }

                user = userServiceClient.getUserDetailsByID(client_name,Integer.parseInt(user_id),token);

                BasketDetailsDto basketDetails = null;

                if(user != null)
                {
                    Boolean status = false;

                    if(StringHelper.isNotEmpty(id))
                    {
                        basketDetails = userServiceClient.getLatestByUserIdAndClientName(client_name, Integer.parseInt(id),basket_name,token);
                    }
                    AmfiSchemeMasterDTO schemeMappings = null;
                    String amfi_code = "";

                    List<AmfiSchemeMasterDTO> schemeMapping = amfiServiceClient.findSchemeAmfiMaster(scheme_name, token);

                    System.out.println("schemeMapping = " + schemeMapping);

                    if (schemeMapping != null && !schemeMapping.isEmpty()) {
                        schemeMappings = schemeMapping.get(0);
                        amfi_code = schemeMappings.getScheme_amfi_code();
                    } else {
                        return NseUtils.commonResponse("Please provide a valid Scheme Name", HttpStatus.BAD_REQUEST);
                    }


                    if(basketDetails == null)
                    {
                        basketDetails = new BasketDetailsDto();
                        basketDetails.setClient_name(client_name);
                        basketDetails.setBasket_name(basket_name);
                        status = false;
                    }else
                    {
                        status = true;
                    }

                    basketDetails.setScheme_amfi(scheme_name);
                    basketDetails.setScheme_amfi_code(amfi_code);
                    basketDetails.setAmount(Integer.parseInt(amount));
                    userServiceClient.saveBasketDetails(basketDetails,token);

                    commonResponse.setStatus(StatusMessage.SuccessCode);
                    commonResponse.setStatus_msg(StatusMessage.SuccessMessage);

                    if(status)
                    {
                        commonResponse.setMsg("The Basket Details are Updated Successfully.");
                    }else
                    {
                        commonResponse.setMsg("The Basket Details are Created Successfully.");
                    }

                    return ResponseEntity.ok(commonResponse);
                }
                else
                {
                    return NseUtils.commonResponse(StatusMessage.UserNotAvailable, HttpStatus.BAD_REQUEST);
                }
        }
        catch(Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date()); ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value="/getBasketDetails")
    public ResponseEntity<?> getBasketDetails(@RequestHeader("Authorization") String token,@RequestParam String user_id) throws Exception
    {
        UserDto user = null;
        Integer log_id = null;
        try
        {
            String client_name = TokenInterceptor.extractClientNamedFromToken(token, secretKey);

                if(StringHelper.isEmpty(client_name))
                {
                    return NseUtils.commonResponse(StatusMessage.ClientNameInvalidMessage, HttpStatus.BAD_REQUEST);
                }

                if(StringHelper.isEmpty(user_id))
                {
                    return NseUtils.commonResponse(StatusMessage.EmptyUserId, HttpStatus.BAD_REQUEST);
                }

                user = userServiceClient.getUserDetailsByID(client_name,Integer.parseInt(user_id),token);

                if(user != null)
                {
                    List<BasketDetailsDto> basketDetails = userServiceClient.getLatestByClientName(client_name,token);

                    List<String> basketNames = basketDetails.stream().map(BasketDetailsDto::getBasket_name).distinct().collect(Collectors.toList());

                    List<BasketDetailsPojo> master_list = new ArrayList<BasketDetailsPojo>();

                    if(basketNames != null && basketNames.size() >0 )
                    {
                        for (String basket : basketNames)
                        {
                            List<BasketDetailsDto> filteredList = basketDetails.stream().filter(b -> basket.equals(b.getBasket_name())).collect(Collectors.toList());

                            BasketDetailsPojo pojo = new BasketDetailsPojo();
                            pojo.setBasket_name(basket);
                            pojo.setList(filteredList);
                            master_list.add(pojo);
                        }
                    }

                    if(master_list != null && master_list.size() > 0)
                    {
                        Collections.reverse(master_list);
                    }

                    BasketDetailsResponse apiResponse = new BasketDetailsResponse();
                    apiResponse.setStatus(StatusMessage.SuccessCode);
                    apiResponse.setStatus_msg(StatusMessage.SuccessMessage);
                    apiResponse.setMsg(StatusMessage.SuccessMessage);
                    apiResponse.setResult(master_list);
                    return new ResponseEntity<BasketDetailsResponse>(apiResponse,HttpStatus.OK);
                }
                else
                {
                    return NseUtils.commonResponse(StatusMessage.UserNotAvailable, HttpStatus.OK);
                }
        }
        catch(Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date()); ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value="/autoSuggestAllMfSchemes")
    public ResponseEntity<?> autoSuggestAllMfSchemes(HttpServletRequest request,@RequestHeader("Authorization") String token,@RequestParam String user_id,
                                                     @RequestParam String query) throws Exception
    {
        CommonResponse commonResponse = new CommonResponse();
        String ipAddr = "";
        UserDto user = null;
        Integer log_id = null;
        try
        {
            ipAddr = NseUtils.getIpAddr(request);
            if(ipAddr == null){ipAddr="";}
            
            String client_name = TokenInterceptor.extractClientNamedFromToken(token, secretKey);
            String keyword = NseUtils.checkParem(query);

            if(user_id == null || StringHelper.isEmpty(user_id)){user_id = "";}
            if(client_name == null || StringHelper.isEmpty(client_name)){client_name = "";}
            if(keyword == null || StringHelper.isEmpty(keyword)){keyword = "Ax";}
            
            user_id = user_id.trim();
            client_name = client_name.trim();
            keyword = keyword.trim();

            if(user_id.equalsIgnoreCase(""))
            {
                return NseUtils.commonResponse("Please provide the User Id", HttpStatus.BAD_REQUEST);
            }
                if(StringHelper.isEmpty(client_name))
                {
                    return NseUtils.commonResponse(StatusMessage.ClientNameInvalidMessage, HttpStatus.BAD_REQUEST);
                }

                if(!NumberUtils.isParsable(user_id))
                {
                    return NseUtils.commonResponse("Please provide the valid user Id", HttpStatus.BAD_REQUEST);
                }

                user = userServiceClient.getUserDetailsByID(client_name,Integer.parseInt(user_id),token);

                if(user != null)
                {
                    String[][] param_and_pattern = {{keyword,"Scheme"}};

//                    int invalid_index_para = ESAPIValidator.isValidParameters(param_and_pattern);
//                    if(invalid_index_para > -1)
//                    {
//                        String[] parameters_name = {"query","category","amc"};
//                        String validationErrorMsg = "Invalid "+parameters_name[invalid_index_para];
//                        return NseUtils.commonResponse(validationErrorMsg, HttpStatus.BAD_REQUEST);
//                    }
                    /*ESAPI VALIDATION*/

                    List<Object[]> schemeList = amfiServiceClient.autoSuggestAllMfSchemes(keyword, "All", "All",token);

                    List<AutoSuggestSchemePojo> master_list = new ArrayList<AutoSuggestSchemePojo>();

                    if (schemeList != null)
                    {
                        schemeList.stream().forEach(object ->
                        {
                            String scheme_amfi = String.valueOf(object[0]);
                            String scheme_amfi_short_name = String.valueOf(object[1]);
                            String scheme_amfi_code = String.valueOf(object[2]);
                            String scheme_company = String.valueOf(object[3]);

                            String logoName = NseUtils.getLogoByAmcNameOrSchemeName(scheme_company);
                            String logo = amcLogoPath + logoName;

                            AutoSuggestSchemePojo obj = new AutoSuggestSchemePojo();
                            obj.setScheme_amfi(scheme_amfi);
                            obj.setScheme_amfi_short_name(scheme_amfi_short_name);
                            obj.setScheme_amfi_code(scheme_amfi_code);
                            obj.setScheme_company(scheme_company);
                            obj.setLogo(logo);

                            master_list.add(obj);
                        });
                    }

                    master_list.sort(Comparator.comparing(AutoSuggestSchemePojo::getScheme_amfi_short_name));

                    AutoSuggestSchemeResponse apiResponse = new AutoSuggestSchemeResponse();
                    apiResponse.setStatus(StatusMessage.SuccessCode);
                    apiResponse.setStatus_msg(StatusMessage.SuccessMessage);
                    apiResponse.setMsg(StatusMessage.SuccessMessage);
                    apiResponse.setList(master_list);
                    return new ResponseEntity<AutoSuggestSchemeResponse>(apiResponse,HttpStatus.OK);
                }
                else
                {
                    return NseUtils.commonResponse(StatusMessage.UserNotAvailable, HttpStatus.BAD_REQUEST);
                }
        }
        catch(Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date()); ex.printStackTrace();
            return NseUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getCartCount")
    public ResponseEntity<?> getCartCount(@RequestHeader("Authorization") String token)
    {
        String userId = "";
        UserDto user = null;
        try
        {
            userId = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            String client_name = TokenInterceptor.extractClientNamedFromToken(token, secretKey);
            System.out.println("userId  = " + userId + " client_name = " + client_name);
            try {
                user =  userServiceClient.getUserDetailsByID(client_name, Integer.valueOf(userId),token);
            }catch (FeignException e)
            {
                return FeignErrorHandler.handle(e, "User Service", "User not found");
            }

            System.out.println("User ID = " + userId + ", Client Name = " + client_name);

            List<CartDto> cartList = null;

            if(user != null)
            {
                cartList = cartService.getCartCount(Integer.valueOf(userId), client_name,token);

                return ResponseEntity.ok(cartList.size());
            }else
            {
                return NseUtils.commonResponse("Investor details not available.", HttpStatus.BAD_REQUEST);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @GetMapping("/getCartCountForPurchaseType")
    public ResponseEntity<?> getCartCountForPurchaseType(
            @RequestHeader("Authorization") String token,@RequestParam String purchase_type,@RequestParam String broker_code)
    {
        String userId = "";
        UserDto user = null;
        try
        {

            userId = TokenInterceptor.extractInvestorIdFromToken(token, secretKey);
            UserDto userOpt = userServiceClient.getUserById(Integer.valueOf(userId), token);

            String client_name = userOpt.getClient_name();
            user = userServiceClient.getUserDetailsByID( client_name,Integer.parseInt(userId),token);

            if(user != null)
            {
                List<CartDto> cartList = cartService.getCartCountForPurchaseType(Integer.valueOf(userId), client_name,purchase_type,broker_code, token);

                return ResponseEntity.ok(cartList.size());

            }else
            {
                return NseUtils.commonResponse("Investor details not available.", HttpStatus.BAD_REQUEST);
            }

        } catch (Exception e)
        {
            e.printStackTrace();
            return NseUtils.commonResponse("Investor details not available.", HttpStatus.BAD_REQUEST);
        }
    }

}
