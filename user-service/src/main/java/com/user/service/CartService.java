package com.user.service;

import com.user.model.BseNseKey;
import com.user.model.Cart;
import com.user.model.MyMFBoxLogActivity;
import com.user.model.User;
import com.user.repository.BseNseKeyRepository;
import com.user.repository.CartRepository;
import com.user.repository.MyMfBoxLogRepository;
import com.user.repository.UserRepository;
import com.user.response.MyMFBoxApiValidityResponse;
import com.user.utils.UserUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.internal.util.StringHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class CartService
{

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    BseNseKeyRepository bseNseKeyRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    MyMfBoxLogRepository myMfBoxLogRepository;

    public Cart saveOrUpdateUser(Cart cart)
    {
        return cartRepository.save(cart);
    }

    public List<Cart> getActiveCarts(Integer userId, String clientName, String purchaseType, String investorCode, String vendor)
    {
        return cartRepository.findActiveCarts(userId, clientName, purchaseType, investorCode, vendor);
    }

    public List<Cart> updateCartByCartId(List<Cart> cartList)
    {
        return cartRepository.saveAll(cartList);
    }


    public Optional<Cart> getCartById(Integer cart_id, String client_name)
    {
        Optional<Cart> cart = null;
        try
        {
            Optional<Cart> list = cartRepository.findByIdAndClientNameOrderByIdDesc(cart_id,client_name);

            if(list != null)
            {
                cart = list;
            }
        }
        catch(Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date()); ex.printStackTrace();
        }
        return cart;
    }

    public Cart getPurchaseCart(Integer userid,String clientName,String schemeName,String investorCode,String folioNo,String purchaseType,String schemeReinvestTag)
    {
        Cart cart = null;
        try
        {
            List<Cart> list = cartRepository.findCartByAllParams(userid,investorCode,folioNo,purchaseType,schemeName,schemeReinvestTag,clientName);

            if(list != null && list.size() > 0)
            {
                cart = list.get(0);
            }
        }
        catch(Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date()); ex.printStackTrace();
        }
        return cart;
    }

    public Cart getInactiveCartsByUserIdAndClientNameAndPaymentId(Integer userid,String clientName,String payment_id)
    {
        Cart cart = null;
        try
        {
            List<Cart> list = cartRepository.findInactiveCartsByUserIdAndClientNameAndPaymentId(userid,clientName,payment_id);

            if(list != null && list.size() > 0)
            {
                cart = list.get(0);
            }
        }
        catch(Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date()); ex.printStackTrace();
        }
        return cart;
    }

    public boolean deleteCartUserById(Integer id, Integer userId, String clientName) {
        try {
            cartRepository.deleteByIdAndUserIdAndClientName(id, userId, clientName);
            return true;
        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date());
            ex.printStackTrace();
            return false;
        }
    }

    public boolean deleteAllCart(Integer userId,String purchaseType, String clientName,String vendor) {
        try {
            cartRepository.deleteActiveCartsByUserIdAndPurchaseTypeAndClientName(userId, purchaseType,clientName,vendor);
            return true;
        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date());
            ex.printStackTrace();
            return false;
        }
    }

    public List<Cart> getInactiveSuccessfulCarts(Integer userId,String purchaseType, String clientName) {
        try {
           return cartRepository.findInactiveSuccessfulCarts(userId, clientName,purchaseType,"NSE");
        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date());
            ex.printStackTrace();
            return Collections.emptyList();
        }
    }


    public List<Cart> getActiveCartsByUserIdAndClientNameAndPurchaseType(Integer userId,String purchaseType, String clientName) {
        try {
          return   cartRepository.findActiveCartsByUserIdAndClientNameAndPurchaseType(userId, clientName,purchaseType,"NSE");
        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date());
            ex.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<Cart> getAllCartsBasedOnIds(List<Integer> Ids)
    {
        return cartRepository.findAllCartsBasedOnIds(Ids);
    }

    public List<Cart> findAllActiveCartsByUserId(Integer userId, String clientName,String vendor)
    {
        try
        {
            return cartRepository.findAllActiveCartsByUserId(userId, clientName,vendor);
        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date());
            ex.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<Cart> findAllActiveCartsByUserIdAndClientCode(Integer userId, String clientName,String vendor,String purchase_type,String broker_code)
    {
        try
        {
            broker_code = UserUtils.checkParem(broker_code);
            System.out.println("broker_code " + broker_code);

            if(broker_code == null || broker_code.isEmpty())
            {
                return cartRepository.findAllActiveCartsByUserIdAndClientCodeWithoutBrokerCode(userId, clientName,vendor,purchase_type);
            }else{
                return cartRepository.findAllActiveCartsByUserIdAndClientCode(userId, clientName,vendor,purchase_type,broker_code);
            }
        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date());
            ex.printStackTrace();
            return Collections.emptyList();
        }
    }

    public Cart getPurchaseCartForNse(Integer userid,String clientName,String schemeName,String investorCode,String folioNo,String purchaseType,String schemeReinvestTag,String to_scheme_name)
    {
        Cart cart = null;
        try
        {
            List<Cart> list = cartRepository.findCartByAllParamsForNse(userid,investorCode,folioNo,purchaseType,schemeName,schemeReinvestTag,to_scheme_name,clientName);

            if(list != null && list.size() > 0)
            {
                cart = list.get(0);
            }
        }
        catch(Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date()); ex.printStackTrace();
        }
        return cart;
    }

    public List<Cart> getActiveCartsById(Integer cart_id) {
        try {
            return   cartRepository.findActiveCartsById(cart_id);
        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date());
            ex.printStackTrace();
            return Collections.emptyList();
        }
    }

}
