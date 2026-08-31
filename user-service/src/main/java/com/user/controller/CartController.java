package com.user.controller;

import com.user.config.TokenInterceptor;
import com.user.model.Cart;
import com.user.model.User;
import com.user.model.UsersOnlineRegDetails;
import com.user.pojo.CartCountPojo;
import com.user.repository.BseNseKeyRepository;
import com.user.repository.UserOnlineRegDetailsRespository;
import com.user.repository.UserRepository;
import com.user.response.CartCountResponse;
import com.user.response.StatusMessage;
import com.user.service.CartService;
import com.user.utils.UserUtils;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hibernate.internal.util.StringHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;


@RestController
@Tag(
        name = "Mobile App Cart APIs",
        description = "APIs related to Product Cart."
)
public class CartController
{
    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BseNseKeyRepository bseNseKeyRepository;

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Autowired
    UserOnlineRegDetailsRespository userOnlineRegDetailsRespository;


    @Hidden
    @GetMapping("/getCartDetailsByUserID")
    public ResponseEntity<?> getCartDetailsByUserID(@RequestParam Integer userId,@RequestParam String vendor,@RequestParam String investorCode, @RequestParam String purchaseType)
    {
        try
        {
            Optional<User> userOpt = userRepository.findById(userId);

            System.out.println("aaa = " + userId + vendor + investorCode + purchaseType);

            if (userOpt.isPresent())
            {
                User user = userOpt.get();
                String clientName = user.getClient_name();
                System.out.println("client = " + clientName);
                List<Cart> cartList = cartService.getActiveCarts(userId, clientName, purchaseType, investorCode, vendor);

                if(!cartList.isEmpty())
                {
                    return ResponseEntity.ok(cartList);
                }else
                {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", HttpStatus.BAD_REQUEST, "status_msg", "No Cart found"));
                }
            }
            else
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", 404, "status_msg", "User not found"));
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", 500, "status_msg", "Error occurred while fetching user details"));
        }
    }

    @Hidden
    @PostMapping("/updateCartByCartId")
    public ResponseEntity<?> updateCartByCartId(@RequestBody List<Cart> cartList)
    {
        try
        {
            cartService.updateCartByCartId(cartList);
            return ResponseEntity.ok(Map.of("status", HttpStatus.OK, "status_msg", "success"));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", 500, "status_msg", "Error occurred while fetching user details"));
        }
    }
    @Hidden
    @GetMapping("/getCartDetails")
    public ResponseEntity<?> getCartDetails(@RequestParam Integer userid,@RequestParam String clientName)
    {
        try
        {
            Optional<Cart> detailsOptional  = cartService.getCartById(userid, clientName);

            if (detailsOptional != null)
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
    @GetMapping("/getPurchaseCartUser")
    public ResponseEntity<?> getPurchaseCartUser(@RequestParam Integer userid,
                                             @RequestParam String investorCode,
                                             @RequestParam String folioNo,
                                             @RequestParam String purchaseType,
                                             @RequestParam String schemeName,
                                             @RequestParam String schemeReinvestTag,
                                             @RequestParam String clientName)
    {
        try
        {
            List<Cart> detailsOptional  = Collections.singletonList(cartService.getPurchaseCart(userid, clientName,schemeName,investorCode,folioNo,purchaseType,schemeReinvestTag));

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
    @GetMapping("/getSuccessCartListByUserIdAndPaymentType")
    public ResponseEntity<?> getSuccessCartListByUserIdAndPaymentType(@RequestParam Integer userid,
                                                 @RequestParam String payment_id,
                                                 @RequestParam String clientName)
    {
        try
        {
            List<Cart> detailsOptional  = Collections.singletonList(cartService.getInactiveCartsByUserIdAndClientNameAndPaymentId(userid, clientName,payment_id));

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

    @PostMapping("/DeleteCartUserById")
    public boolean deleteCartUserById(
            @RequestParam Integer userid,
            @RequestParam Integer id,
            @RequestParam String clientName
    ) {
        try {
            return cartService.deleteCartUserById(id, userid, clientName);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }


    @Hidden
    @PostMapping("/deleteAllCart")
    public boolean deleteAllCart(
            @RequestParam Integer userid,
            @RequestParam String purchaseType,
            @RequestParam String clientName,
            @RequestParam String vendor
    ) {
        System.out.println("userid = " + userid + " purchaseType = " + purchaseType + " clientName = " + clientName);
        try {
            return cartService.deleteAllCart(userid,purchaseType, clientName,vendor);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }


    @Hidden
    @PostMapping("/saveOrUpdateCart")
    public ResponseEntity<?> saveOrUpdateCart(@RequestBody Cart cart){

        try
        {

            List<Cart> detailsOptional  = Collections.singletonList(cartService.saveOrUpdateUser(cart));

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
    @GetMapping("/getInactiveSuccessfulCarts")
    public ResponseEntity<List<Cart>> getInactiveSuccessfulCarts(
            @RequestParam Integer userid,
            @RequestParam String purchaseType,
            @RequestParam String clientName
    ) {
        try {
            System.out.println("userid = " + userid + " purchaseType = " + purchaseType + " clientName = " + clientName);
            List<Cart> list = cartService.getInactiveSuccessfulCarts(userid, purchaseType, clientName);
            return ResponseEntity.ok(list);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Hidden
    @GetMapping("/getActiveCartsByUserIdAndClientNameAndPurchaseType")
    public ResponseEntity<List<Cart>> getActiveCartsByUserIdAndClientNameAndPurchaseType(
            @RequestParam Integer userid,
            @RequestParam String purchaseType,
            @RequestParam String clientName
    ) {
        try {

            List<Cart> list = cartService.getActiveCartsByUserIdAndClientNameAndPurchaseType(userid, purchaseType, clientName);
            return ResponseEntity.ok(list);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Hidden
    @GetMapping("/getCartDetailsByIds")
    public ResponseEntity<?> getCartDetailsByIds(@RequestParam List<Integer> ids)
    {
        try
        {
            List<Cart> cartList = cartService.getAllCartsBasedOnIds(ids);

            if(!cartList.isEmpty())
            {
                return ResponseEntity.ok(cartList);
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
    @GetMapping("/getAllActiveCartsByUserId")
    public ResponseEntity<List<Cart>> getAllActiveCartsByUserId(@RequestParam Integer userid,@RequestParam String clientName,@RequestParam String vendor)
    {
        try
        {
            List<Cart> list = cartService.findAllActiveCartsByUserId(userid, clientName,vendor);
            return ResponseEntity.ok(list);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Hidden
    @GetMapping("/getAllActiveCartsByUserIdAndClientCode")
    public ResponseEntity<List<Cart>> getAllActiveCartsByUserIdAndClientCode(@RequestParam Integer userid,
                                                                             @RequestParam String clientName,
                                                                             @RequestParam String vendor,
                                                                             @RequestParam String purchase_type,
                                                                             @RequestParam String broker_code)
    {
        try
        {
            List<Cart> list = cartService.findAllActiveCartsByUserIdAndClientCode(userid, clientName,vendor,purchase_type,broker_code);
            return ResponseEntity.ok(list);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Hidden
    @GetMapping("/getPurchaseCartUserForNse")
    public ResponseEntity<?> getPurchaseCartUserForNse(@RequestParam Integer userid,
                                                       @RequestParam String investorCode,
                                                       @RequestParam String folioNo,
                                                       @RequestParam String purchaseType,
                                                       @RequestParam String schemeName,
                                                       @RequestParam String schemeReinvestTag,
                                                       @RequestParam String to_scheme_name,
                                                       @RequestParam String clientName)
    {
        try
        {
            List<Cart> detailsOptional  = Collections.singletonList(cartService.getPurchaseCartForNse(userid, clientName,schemeName,investorCode,folioNo,purchaseType,schemeReinvestTag,to_scheme_name));

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
    @GetMapping("/getActiveCartsById")
    public ResponseEntity<List<Cart>> getActiveCartsById(@RequestParam Integer cart_id)
    {
        try
        {
            List<Cart> list = cartService.getActiveCartsById(cart_id);
            return ResponseEntity.ok(list);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/getCartCounts")
    public ResponseEntity<?> getCartCounts(@RequestHeader("Authorization") String token)
    {
        Optional<UsersOnlineRegDetails> user = null;
        Integer log_id = null;
        try
        {
            String user_id = TokenInterceptor.extractInvestorIdFromToken(token,secretKey);
            String client_name = TokenInterceptor.extractClientNamedFromToken(token,secretKey);

            if(StringHelper.isEmpty(user_id))
            {
                return UserUtils.getCommonResponse("Please provide the user id", StatusMessage.FailureCode);
            }

            user = userOnlineRegDetailsRespository.findNseUserByUserIdAndClientName(Integer.parseInt(user_id), client_name);

            if(user.isPresent())
            {
                CartCountPojo pojo = new CartCountPojo();

                Map<String, Integer> purchaseCounts = cartService.getCartCount(Integer.parseInt(user_id));

                if(purchaseCounts != null && !purchaseCounts.isEmpty())
                {
                    for (Map.Entry<String, Integer> entry : purchaseCounts.entrySet())
                    {
                        String purchaseType = entry.getKey();
                        Integer count = entry.getValue();

                        if(count == null){count = 0;}

                        if(purchaseType.equalsIgnoreCase("Lumpsum Purchase"))
                        {
                            pojo.setLumpsum_count(count);
                        }else if(purchaseType.equalsIgnoreCase("SIP Purchase"))
                        {
                            pojo.setSip_count(count);
                        }else if(purchaseType.equalsIgnoreCase("Redemption Purchase"))
                        {
                            pojo.setRedeem_count(count);
                        }else if(purchaseType.equalsIgnoreCase("Switch Purchase"))
                        {
                            pojo.setSwitch_count(count);
                        }else if(purchaseType.equalsIgnoreCase("STP Purchase"))
                        {
                            pojo.setStp_count(count);
                        }else if(purchaseType.equalsIgnoreCase("SWP Purchase"))
                        {
                            pojo.setSwp_count(count);
                        }else if(purchaseType.equalsIgnoreCase("Total Count"))
                        {
                            pojo.setTotal_count(count);
                        }
                    }
                }

                CartCountResponse apiResponse = new CartCountResponse();
                apiResponse.setStatus(StatusMessage.SuccessCode);
                apiResponse.setStatus_msg(StatusMessage.SuccessMessage);
                apiResponse.setMsg(StatusMessage.SuccessMessage);
                apiResponse.setResult(pojo);
                return new ResponseEntity<CartCountResponse>(apiResponse, HttpStatus.OK);

            }else
            {
                return UserUtils.getCommonResponse("User details not available.", StatusMessage.FailureCode);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
