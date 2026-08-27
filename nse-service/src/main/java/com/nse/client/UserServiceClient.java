package com.nse.client;

import com.nse.dto.mf.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@FeignClient(name = "user-service", url = "${user.service.base-url}")
public interface UserServiceClient {

    // old: GET /{id}
    @GetMapping("/getUserByIdAndActive")
    UserDto getUserById(@RequestParam Integer userId, @RequestHeader("Authorization") String token);

    // old: GET /nse-user/{id}
    @GetMapping("/nse-user/{id}")
    UserBseNseDto getUserNseDetailsById(@PathVariable("id") String userId);

    @GetMapping("/getClientName")
    BseNseKeyDto getByClientName(@RequestParam("clientName") String clientName, @RequestHeader("Authorization") String token);

    @PostMapping("/saveUserNseSuccessResponse")
    ResponseEntity<String> saveUserNseSuccessResponse(@RequestBody UserDto userDto, @RequestHeader("Authorization") String token);

    @PostMapping("/saveBseNseDetails")
    ResponseEntity<String> saveBseNseDetails(@RequestBody UserDto userDto, @RequestHeader("Authorization") String token);

    @GetMapping("/getUserByClientNameAndId")
    UserDto getUserByClientNameAndId(
            @RequestParam("userid") Integer user_id,
            @RequestParam("clientName") String client_name,
            @RequestHeader("Authorization") String token
    );

    @GetMapping("/getCartDetailsByIds")
    List<CartDto> getCartDetailsByIds(@RequestParam List<Integer> ids, @RequestHeader("Authorization") String token);

    @GetMapping("/getUserBseNseDetailsByNseIINNumberBrokerCode")
    UserDto getUserBseNseDetailsByNseIINNumberBrokerCode(
            @RequestParam("client_name") String clientName,
            @RequestParam("iin_number") String iin_number,
            @RequestParam("broker_code") String broker_code,
            @RequestHeader("Authorization") String token
    );

    @GetMapping("/getUserByIdAndClientName")
    UserDto getUserDetailsByID(@RequestParam("clientName") String clientName,@RequestParam("userid") Integer userid, @RequestHeader("Authorization") String token);

    @GetMapping("/getUserByIdAndClientNameAndNseActive")
    List<UserBseNseDto> getUserByIdAndClientNameAndNseActive(@RequestParam("clientName") String clientName,@RequestParam("userid") Integer userid, @RequestHeader("Authorization") String token);


    @GetMapping("/getUserBseNseDetailsByNseIINNumber")
    UserBseNseDto getUserBseNseDetailsByIinNumber(
            @RequestParam("client_name") String clientName,
            @RequestParam("iin_number") String iin_number, @RequestHeader("Authorization") String token
    );

    @GetMapping("/getUserBseNseDetailsByNseIINNumber")
    UserDto getUserBseNseDetailsByIinnumber(
            @RequestParam("client_name") String clientName,
            @RequestParam("iin_number") String iin_number, @RequestHeader("Authorization") String token
    );


    @GetMapping("/getOrCreateOnboarding")
    MymfboxOnboardingDto getOrCreateOnboarding(
            @RequestParam("clientName") String clientName,
            @RequestParam("userid") Integer userid, @RequestHeader("Authorization") String token
    );

    @GetMapping("/getBseNseOnlineAccess")
    BseNseOnlineAccessDto getBseNseOnlineAccessByClientName(
            @RequestParam("clientName") String clientName,
            @RequestParam("brokerCode") String brokerCode, @RequestHeader("Authorization") String token
    );

    @PostMapping("/saveNseTransaction")
    void saveNseTransaction(@RequestBody NseTransactionsDto dto, @RequestHeader("Authorization") String token);

    @PostMapping("/saveNseRegReport")
    void saveNseRegReport(@RequestBody UsersNseRegReportDto dto, @RequestHeader("Authorization") String token);

    @GetMapping("/getRegReport")
    UsersNseRegReportDto getUserNseRegDetails(
            @RequestParam("iin_number") String iin_number,
            @RequestParam("client_name") String client_name, @RequestHeader("Authorization") String token

    );

    @GetMapping("/getUsersPortfolioSchemewise")
    List<String> getUsersPortfolioSchemewise(
            @RequestParam("user_id") Integer userId,
            @RequestParam("client_name") String clientName,
            @RequestParam("amc_code") String amcCode, @RequestHeader("Authorization") String token
    );

    @GetMapping("/getUsersPortfolioSchemewiseAll")
    List<UsersPortfolioSchemewiseDto> getUsersPortfolioSchemewiseUser(
            @RequestParam("user_id") Integer userId,
            @RequestParam("client_name") String client_name,
            @RequestParam("scheme_name") String scheme_name, @RequestHeader("Authorization") String token
    );

    @GetMapping("/getinvestorMasterCams")
    List<InvestorMasterCamsDto> getinvestorMasterCams(
            @RequestParam("user_id") Integer user_id,
            @RequestParam("client_name") String client_name,
            @RequestParam("amc_code") String amc_code, @RequestHeader("Authorization") String token
    );


    @GetMapping("/getinvestorMasterKarvy")
   List<InvestorMasterKarvyDto> getinvestorMasterKarvy(
            @RequestParam("user_id") Integer user_id,
            @RequestParam("client_name") String client_name,
            @RequestParam("amc_code") String amc_code, @RequestHeader("Authorization") String token
    );


    @GetMapping("/getUserBseNseDetailsByIinNumberAndUserId")
    UserBseNseDto getUserBseNseDetailsByIinNumberAndUserId(
            @RequestParam("userid") Integer user_id,
            @RequestParam("iin_number") String nse_iin_num,
            @RequestParam("client_name") String client_name, @RequestHeader("Authorization") String token
    );

    @GetMapping("/getUserDetailsByIinNumberAndBrokercode")
    UserDto getUserDetailsByIinNumberAndBrokercode(@RequestParam("broker_code") String broker_code,@RequestParam("iin_number") String nse_iin_num,
                                                   @RequestParam("client_name") String client_name,@RequestParam("user_id") String userid,@RequestHeader("Authorization") String token);

    @GetMapping("/getSipFolioBrokerCodeUser")
    List<String> getSipBrokercodeuser(
            @RequestParam("userid") Integer userid,
            @RequestParam("folio") String folio,
            @RequestParam("client_name") String client_name, @RequestHeader("Authorization") String token
    );


    @GetMapping("/getCartDetailsByUserID")
    List<CartDto> getCartDetailsByUserID(@RequestParam Integer userId, @RequestParam String vendor, @RequestParam String investorCode, @RequestParam String purchaseType, @RequestHeader("Authorization") String token);

    @PostMapping("/updateCartByCartId")
    void updateCartByCartId(List<CartDto> cartList, @RequestHeader("Authorization") String token);

    @RequestMapping("/getNseUserMandateDetailsByUmrn")
    UserMandateDetailsDto getNseUserMandateDetailsByUmrn(@RequestParam String client_name, @RequestParam String iin_number, @RequestParam String umrn_code, @RequestParam Integer userid, @RequestHeader("Authorization") String token);

    @GetMapping("/getCartDetails")
    CartDto getCartDetails(
            @RequestParam("userid") Integer userid,
            @RequestParam("clientName") String client_name, @RequestHeader("Authorization") String token
    );

    @GetMapping("/getSuccessCartListByUserIdAndPaymentType")
    List<CartDto> getSuccessCartListByUserIdAndPaymentType(
            @RequestParam("userid") Integer userid,
            @RequestParam("clientName") String client_name,@RequestParam("payment_id") String payment_id, @RequestHeader("Authorization") String token
    );

    @PostMapping("/DeleteCartUserById")
    boolean DeleteCartUserById(
            @RequestParam("userid") Integer userid,
            @RequestParam("id") String id,@RequestParam("clientName") String clientName, @RequestHeader("Authorization") String token
    );

    @PostMapping("/deleteAllCart")
    boolean deleteAllCart(
            @RequestParam("userid") Integer userid,
            @RequestParam("purchaseType") String purchaseType,@RequestParam("clientName") String clientName,
            @RequestParam("vendor") String vendor,
            @RequestHeader("Authorization") String token
    );


    //Redemption

    @GetMapping("/getAllAMCDetails")
    List<UsersPortfolioSchemewiseDto> getAllRedemptionAmcDetails(
            @RequestParam("userid") Integer userid,
            @RequestParam("client_name") String client_name, @RequestHeader("Authorization") String token
    );

    @GetMapping("/getRedemptionSchemesNews")
    List<String> getRedemptionSchemesNews(
            @RequestParam("userid") Integer userid,
            @RequestParam("clientName") String client_name,
            @RequestParam("amcCode") String amc_code, @RequestHeader("Authorization") String token
    );

    @GetMapping("/getProductCode")
    List<InvestorMasterCamsDto> getProductCode(
            @RequestParam("userid") Integer userid,
            @RequestParam("clientName") String client_name,
            @RequestParam("product") List<String> product, @RequestHeader("Authorization") String token
    );
    @GetMapping("/getProductCodes")
    List<InvestorMasterKarvyDto> getProductCodes(
            @RequestParam("userid") Integer userid,
            @RequestParam("clientName") String client_name,
            @RequestParam("product") List<String> product, @RequestHeader("Authorization") String token
    );
    @GetMapping("/getRedemptionKarvy")
    List<InvestorMasterKarvyDto> getRedemptionKarvy(
            @RequestParam("userid") Integer userid,
            @RequestParam("clientName") String client_name,
            @RequestParam("fund") String amc_code, @RequestHeader("Authorization") String token
    );

    @GetMapping("/getRedemptionPortfolio")
    List<String> getRedemptionPortfolio(
            @RequestParam("userid") Integer userid,
            @RequestParam("clientName") String client_name,
            @RequestParam("amcCode") String amc_code,
            @RequestParam("schemeCodes") List<String> schemeCodes, @RequestHeader("Authorization") String token
    );
    @GetMapping("/getByAllFields")
    List<UserMandateDetailsDto> getByAllFields(@RequestParam("clientName") String clientName,
                                               @RequestParam("onlineFlag") String onlineFlag,
                                               @RequestParam("onlineCode") String onlineCode,
                                               @RequestParam("bankAccountNumber") String bankAccountNumber,
                                               @RequestParam("userid") Integer userid,
                                               @RequestHeader("Authorization") String token);
    @GetMapping("/getRedemptionHoldingUnits")
    List<UsersPortfolioSchemewiseDto> getRedemptionHoldingUnits(
            @RequestParam("userid") Integer userid,
            @RequestParam("clientName") String client_name,
            @RequestParam("folio_no") String folio_no,
            @RequestParam("scheme_name") String scheme_name, @RequestHeader("Authorization") String token
    );

    @GetMapping("/getinvestorMasterKarvyScheme")
    List<InvestorMasterKarvyDto> getinvestorMasterKarvyScheme(
            @RequestParam("user_id") Integer user_id,
            @RequestParam("client_name") String client_name,
            @RequestParam("scheme_name") String scheme_name, @RequestHeader("Authorization") String token
    );

    @GetMapping("/getinvestorMasterKarvySchemes")
    List<InvestorMasterKarvyDto> getinvestorMasterKarvySchemes(
            @RequestParam("user_id") Integer user_id,
            @RequestParam("client_name") String client_name,
            @RequestParam("productList") List<String> productList, @RequestHeader("Authorization") String token
    );

    @GetMapping("/getAllTransactionType")
    List<TransactionTypeDTO> getAllTransactionType(@RequestHeader("Authorization") String token);

    @GetMapping("/getAllCamsTransaction")
    List<InvestorTransactionCamsDto> getAllCamsTransaction(
            @RequestParam("user_id") Integer userid,
            @RequestParam("client_name") String client_name,
            @RequestParam("folio_no") String folio_no,
            @RequestParam("prodcode") List<String> prodcode, @RequestHeader("Authorization") String token
    );

    @GetMapping("/getAllKarvyTransaction")
    List<InvestorTransactionKarvyDto> getAllKarvyTransaction(
            @RequestParam("user_id") Integer userid,
            @RequestParam("client_name") String client_name,
            @RequestParam("folio_no") String folio_no,
            @RequestParam("prodcode") List<String> prodcode, @RequestHeader("Authorization") String token
    );

    @GetMapping("/getClientNameAndIinNumber")
    List<UserDto> getClientNameAndIinNumber(@RequestParam("clientName") String clientName,
                                                @RequestParam("nseIinNumber") String IinNumber, @RequestHeader("Authorization") String token);

    @GetMapping("/getUserBseNseDetailsByNseIINNumber")
    List<UserBseNseDto> getUserBseNseDetailsByIinNumbers(
            @RequestParam("client_name") String clientName,
            @RequestParam("iin_number") String iin_number, @RequestHeader("Authorization") String token
    );

    @GetMapping("/getRTADetails")
    UsersPortfolioSchemewiseDto getRTADetails(@RequestParam("scheme_name") String scheme_name, @RequestHeader("Authorization") String token);

    @PostMapping("/saveUser")
    String saveUser(@RequestBody UserDto userDto, @RequestHeader("Authorization") String token);

    @PostMapping("/saveUserBseNseDetail")
    UserBseNseDto saveUserBseNseDetail(@RequestBody UserBseNseDto userBseNseDto, @RequestHeader("Authorization") String token);

    @PostMapping("/saveRegDetails")
    UsersNseRegReportDto saveRegDetails(@RequestParam UsersNseRegReportDto usersNseRegReportDto, @RequestHeader("Authorization") String token);

    @GetMapping("/getUserDetailsByPanName")
    List<UserDto> getUserDetailsByPanName(
            @RequestParam("pan") String pan,
            @RequestParam("name") String name,
            @RequestParam("clientName") String client_name, @RequestHeader("Authorization") String token
    );

    @GetMapping("/getActiveUsersByPanName")
    List<UserDto> getActiveUsersByPanName(
            @RequestParam("pan") String pan,
            @RequestParam("name") String name,
            @RequestParam("clientName") String client_name, @RequestHeader("Authorization") String token
    );

    @GetMapping("/getDistinctSchemeAmfiCodeByUserAndAmc")
    List<String> getDistinctSchemeAmfiCodeByUserAndAmc(
            @RequestParam("amcCode") String amc_code,
            @RequestParam("userid") String userid,
            @RequestParam("clientName") String client_name, @RequestHeader("Authorization") String token
    );

    @GetMapping("/getActiveSchemesByUserAndClient")
    List<UsersPortfolioSchemewiseDto> getActiveSchemesByUserAndClient(
            @RequestParam("userid") Integer userid,
            @RequestParam("clientName") String client_name, @RequestHeader("Authorization") String token
    );

    @GetMapping("/getByCamsUserIdAndClientName")
    List<InvestorMasterCamsDto> getByCamsUserIdAndClientName(
            @RequestParam("userid") Integer userid,
            @RequestParam("clientName") String client_name, @RequestHeader("Authorization") String token
    );

    @GetMapping("/getByuserIdAndClientnameAndTaxstatus")
    List<InvestorMasterCamsDto> getByuserIdAndClientnameAndTaxstatus(@RequestParam("userId") Integer userId,
                                                                      @RequestParam("clientName") String clientName,
                                                                      @RequestParam("tax_status") String tax_status,
                                                                      @RequestParam("holding_nature") String holding_na,
                                                                      @RequestParam("joint1_pan") String joint1_pan,
                                                                      @RequestParam("broker_code") String broker_cod,
                                                                     @RequestHeader("Authorization") String token);

    @GetMapping("/getByKarvyUserIdAndClientNameAndTaxstatus")
    List<InvestorMasterKarvyDto> getByKarvyUserIdAndClientNameAndTaxstatus(@RequestParam Integer userid,
                                                                       @RequestParam String clientName,
                                                                       @RequestParam("tax_status") String tax_status,
                                                                       @RequestParam("holding_nature") String holding_na,
                                                                       @RequestParam("joint1_pan") String joint1_pan,
                                                                       @RequestParam("broker_code") String broker_cod,
                                                                       @RequestHeader("Authorization") String token);

    @GetMapping("/getByKarvyUserIdAndClientName")
    List<InvestorMasterKarvyDto> getByKarvyUserIdAndClientName(
            @RequestParam("userid") Integer userid,
            @RequestParam("clientName") String client_name, @RequestHeader("Authorization") String token
    );

    @GetMapping("/getGroupedByProdcodeAndFolioNo")
    List<InvestorTransactionCamsDto> getGroupedByProdcodeAndFolioNo(
            @RequestParam("user_id") Integer userid,
            @RequestParam("client_name") String client_name, @RequestHeader("Authorization") String token
    );

    @GetMapping("/getByFolioNoAndProdcodeAndUserIdAndClientName")
    List<InvestorTransactionCamsDto> getByFolioNoAndProdcodeAndUserIdAndClientName(
            @RequestParam("user_id") Integer userid,
            @RequestParam("client_name") String client_name,
            @RequestParam("folioNo") String folioNo,
            @RequestParam("prodcode") String  prodcode,
            @RequestHeader("Authorization") String token
    );

    @GetMapping("/getGroupedTransactions")
    List<InvestorTransactionKarvyDto> getGroupedTransactions(
            @RequestParam("user_id") Integer userid,
            @RequestParam("client_name") String client_name,
            @RequestHeader("Authorization") String token
    );
    @GetMapping("/getTransactionsByFolioAndFund")
    List<InvestorTransactionKarvyDto> getTransactionsByFolioAndFund(
            @RequestParam("user_id") Integer userid,
            @RequestParam("client_name") String client_name,
            @RequestParam("folioNumber") String folioNo,
            @RequestParam("fund") String  prodcode,
            @RequestParam("schemeCode") String  schemeCode,
            @RequestHeader("Authorization") String token
    );

    @GetMapping("/getByFolioSchemeAndUser")
    List<PortfolioTransactionsDto> getByFolioSchemeAndUser(
            @RequestParam("userid") Integer userid,
            @RequestParam("clientName") String client_name,
            @RequestParam("folioNo") String folioNo,
            @RequestParam("schemeCode") String  schemeCode,
            @RequestHeader("Authorization") String token
    );

    @GetMapping("/getNseGroupedTransactions")
    List<PortfolioTransactionsDto> getNseGroupedTransactions(
            @RequestParam("userid") Integer userid,
            @RequestParam("clientName") String client_name,
            @RequestHeader("Authorization") String token
    );

    @GetMapping("/getPurchaseCartUser")
    List<CartDto> getPurchaseCartUser(
            @RequestParam("userid") Integer userid,
            @RequestParam("investorCode") String investorCode,
            @RequestParam("folioNo") String folioNo,
            @RequestParam("purchaseType") String purchaseType,
            @RequestParam("schemeName") String schemeName,
            @RequestParam("schemeReinvestTag") String schemeReinvestTag,
            @RequestParam("clientName") String clientName,
            @RequestHeader("Authorization") String token
    );
    @GetMapping("/getSchemeHoldingUnitsUser")
    List<UsersPortfolioSchemewiseDto> getSchemeHoldingUnitsUser(
            @RequestParam("userid") Integer userid,
            @RequestParam("clientName") String clientName,
            @RequestParam("scheme_name") String scheme_name,
            @RequestParam("folio") String folioNo,
            @RequestHeader("Authorization") String token
    );

    @PostMapping("/saveUserRegStatus")
    void saveUserRegStatus(@RequestHeader("Authorization") String token);

    @PostMapping("/saveOrUpdateCart")
    void saveOrUpdateCart(@RequestBody CartDto dto, @RequestHeader("Authorization") String token);

    @PostMapping("/saveBasketDetails")
    void saveBasketDetails(@RequestBody BasketDetailsDto basketDetails, @RequestHeader("Authorization") String token);

    @GetMapping("/getInactiveSuccessfulCarts")
    List<CartDto> getInactiveSuccessfulCarts(@RequestParam("userid") Integer userId, @RequestParam("purchaseType") String purchaseType, @RequestParam("clientName") String clientName, @RequestHeader("Authorization") String token);

    @GetMapping("/getUserByIdAndClientNameActiveNse")
    List<UserDto> getUserByIdAndClientNameActiveNse(@RequestParam("clientName") String clientName,@RequestParam("userid") Integer userid, @RequestHeader("Authorization") String token);

    @GetMapping("/getActiveCartsByUserIdAndClientNameAndPurchaseType")
    List<CartDto> getActiveCartsByUserIdAndClientNameAndPurchaseType(@RequestParam("userid") Integer userId, @RequestParam("purchaseType") String purchaseType, @RequestParam("clientName") String clientName, @RequestHeader("Authorization") String token);

    @GetMapping("/getInactiveNseByUserIdAndClientName")
    UserDto getInactiveNseByUserIdAndClientName(@RequestParam("clientName") String clientName, @RequestParam("userid") Integer userid, @RequestHeader("Authorization") String token);

    @GetMapping("/getLatestByUserIdAndClientName")
    BasketDetailsDto getLatestByUserIdAndClientName(@RequestParam("clientName") String clientName, @RequestParam("id") Integer id,@RequestParam("basket_name") String basket_name, @RequestHeader("Authorization") String token);

    @GetMapping("/getLatestByClientName")
    List<BasketDetailsDto> getLatestByClientName(@RequestParam("clientName") String clientName, @RequestHeader("Authorization") String token);

    @GetMapping("/getAllActiveCartsByUserId")
    List<CartDto> getAllActiveCartsByUserId(@RequestParam("userid") Integer userId,@RequestParam("clientName") String clientName,@RequestParam("vendor") String vendor, @RequestHeader("Authorization") String token);

    @GetMapping("/getMandateDetailsByBrokerCode")
    List<UserMandateDetailsDto> getMandateDetailsByBrokerCode (
            @RequestParam("user_id") Integer userId,
            @RequestParam("client_name") String clientName,
            @RequestParam("online_code") String client_code,
            @RequestParam("broker_code") String broker_code,
            @RequestHeader("Authorization") String token);

    @GetMapping("/getBankDetailsByBrokerCode")
    List<UsersBankDetailsDTO> getBankDetailsByBrokerCode (
            @RequestParam("user_id") Integer userId,
            @RequestParam("client_name") String clientName,
            @RequestParam("online_code") String client_code,
            @RequestParam("broker_code") String broker_code,
            @RequestHeader("Authorization") String token);

    @PostMapping("/saveBankMandateDetails")
    ResponseEntity<String>  saveBankMandateDetails(@RequestBody List<BankDto> bankDetails, @RequestHeader("Authorization") String token);

    @GetMapping("/getAllActiveCartsByUserIdAndClientCode")
    List<CartDto> getAllActiveCartsByUserIdAndClientCode(@RequestParam("userid") Integer userId,@RequestParam("clientName") String clientName,@RequestParam("vendor") String vendor,@RequestParam("purchase_type") String purchase_type,@RequestParam("broker_code") String broker_code, @RequestHeader("Authorization") String token);

    @GetMapping("/getUserRegDetailsForCartByUserIdTaxStatus")
    UserDto getUserRegDetailsForCartByUserIdTaxStatus(@RequestParam("clientName") String clientName,@RequestParam("userid") Integer userid,@RequestParam("tax_status_code") String tax_status_code,@RequestParam("holding_nature_code") String holding_nature_code, @RequestHeader("Authorization") String token);

    @GetMapping("/getUserByIdAndClientNameAndiinnumber")
    UserDto getUserByIdAndClientNameAndiinnumber(@RequestParam("clientName") String clientName,@RequestParam("userid") Integer userid,@RequestParam("iin_number") String iin_number, @RequestHeader("Authorization") String token);

    @GetMapping("/getPurchaseCartUserForNse")
    List<CartDto> getPurchaseCartUserForNse(
            @RequestParam("userid") Integer userid,
            @RequestParam("investorCode") String investorCode,
            @RequestParam("folioNo") String folioNo,
            @RequestParam("purchaseType") String purchaseType,
            @RequestParam("schemeName") String schemeName,
            @RequestParam("schemeReinvestTag") String schemeReinvestTag,
            @RequestParam("to_scheme_name") String to_scheme_name,
            @RequestParam("clientName") String clientName,
            @RequestHeader("Authorization") String token
    );

    @GetMapping("/getAmcCodeByAmcName")
    String getAmcCodeByAmcName(@RequestParam("amc_name") String amc_name, @RequestHeader("Authorization") String token);

    @GetMapping("/getRegisterByAmcCode")
    String getRegisterByAmcCode(@RequestParam("amc_code") String amc_code, @RequestHeader("Authorization") String token);

    @GetMapping("/getMobileAppUserDetailsByOnlineId")
    UserDto getMobileAppUserDetailsByOnlineId(@RequestParam Integer userId, @RequestHeader("Authorization") String token);

        @GetMapping("/getUserByOnlineIdAndActive")
    UserDto getUserByOnlineIdAndActive(@RequestParam Integer onlineId, @RequestParam Integer userId, @RequestHeader("Authorization") String token);

}
