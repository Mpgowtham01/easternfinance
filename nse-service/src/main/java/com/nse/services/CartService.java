package com.nse.services;

import com.nse.client.UserServiceClient;
import com.nse.dto.mf.CartDto;
import com.nse.dto.mf.UsersPortfolioSchemewiseDto;
import com.nse.model.NseOnlineSchemeMaster;
import com.nse.model.NseOnlineSipStpSwpMaster;
import com.nse.pojo.SchemeHoldingUnitsPojo;
import com.nse.repository.NseOnlineSchemeMasterRepository;
import com.nse.repository.NseOnlineSipStpSwpMasterRepository;
import com.nse.utils.NseUtils;
import org.hibernate.internal.util.StringHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestHeader;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

@Service
public class CartService {

    @Autowired
    NseOnlineSchemeMasterRepository nseOnlineSchemeMasterRepository;

    @Autowired
    NseOnlineSipStpSwpMasterRepository nseOnlineSipStpSwpMasterRepository;

    @Autowired
    UserServiceClient userServiceClient;

    @Value("${amc.logo.url}")
    private String amcLogoPath;

    public Double getNSELumpsumMinAmountBySchemeName(String schemeName, String purchaseType, String reinvestTag) {
        Double minAmount = 0.0;

        try {
            // Fetch valid schemes using the repository
            List<NseOnlineSchemeMaster> list = nseOnlineSchemeMasterRepository.findValidSchemesBySchemeCode(schemeName);

            if (list != null && !list.isEmpty()) {
                // Take the first scheme and get its min purchase amount
                NseOnlineSchemeMaster schemeMasterLimit = list.get(0);

                if (schemeMasterLimit.getNewPurchaseMinAmount() != null) {
                    minAmount = schemeMasterLimit.getNewPurchaseMinAmount();
                }
            } else {
                System.out.println("Scheme Code not available");
            }

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date());
            ex.printStackTrace();
        }

        return minAmount;
    }

    public Double validateSipamount(String scheme_code, String amc_name, String sip_frequency)
    {

        List<NseOnlineSipStpSwpMaster> list = null;
        NseOnlineSipStpSwpMaster schemeMasterLimit = null;

        Double minimum_amount = 0.0;

        try
        {
            list = nseOnlineSipStpSwpMasterRepository.findByAmcNameAndSchemeCodeAndFrequency(amc_name,scheme_code,sip_frequency);

            if(list != null && list.size() > 0)
            {
                schemeMasterLimit = list.get(0);
            }

            if (list != null && !list.isEmpty()) {

                schemeMasterLimit = list.get(0);

                if (schemeMasterLimit.getSip_minimum_installment_amount() != null) {
                    minimum_amount = schemeMasterLimit.getSip_minimum_installment_numbers();
                }
            } else {
                System.out.println("Scheme Code not available");
            }

        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
        return minimum_amount;
    }

    public CartDto getPurchaseCart(Integer user_id, String investor_code, String folio_no, String scheme_name, String scheme_reinvest_tag, String client_name, String purchase_type,@RequestHeader("Authorization") String token)
    {
        CartDto cart = null;
        try
        {

            List<CartDto> list = userServiceClient.getPurchaseCartUser(user_id,investor_code,folio_no,purchase_type,scheme_name,scheme_reinvest_tag,client_name,token);

            if(list != null && list.size() > 0)
            {
                cart = list.get(0);
            }
        }
        catch(Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date());
            ex.printStackTrace();
        }
        return cart;
    }


    public SchemeHoldingUnitsPojo getSchemeHoldingUnits(String client_name, String folio, String scheme_name, Integer user_id,@RequestHeader("Authorization") String token)
    {
        DecimalFormat unit_decimal1 = new DecimalFormat("0.00");
        SchemeHoldingUnitsPojo values = new SchemeHoldingUnitsPojo();
        try
        {
            String scheme_category = "";
            Double total_units = 0.0;
            Double current_value = 0.0;
            Double latest_nav = 0.0;


            List<UsersPortfolioSchemewiseDto> scheme_list = userServiceClient.getSchemeHoldingUnitsUser(user_id,client_name,scheme_name,folio,token);
            //System.out.println("scheme_list------>"+scheme_list.size());
            if(scheme_list != null && scheme_list.size() > 0)
            {
                scheme_category = scheme_list.get(0).getScheme_category();

                if(scheme_category.equalsIgnoreCase("Equity: ELSS"))
                {
                    latest_nav = scheme_list.get(0).getLatest_nav();

                    Double load_free_units = scheme_list.get(0).getLoad_free_units();

                    current_value = load_free_units * latest_nav;
                    current_value = Double.parseDouble(unit_decimal1.format(current_value));

                    values.setCurrent_value(current_value);
                    values.setTotal_units(total_units);
                }else
                {
                    total_units = scheme_list.get(0).getTotal_units();
                    current_value = scheme_list.get(0).getCurrent_value();

                    values.setCurrent_value(current_value);
                    values.setTotal_units(total_units);
                }
            }
        }
        catch(Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date());
            ex.printStackTrace();
        }
        return values;
    }

    public NseOnlineSchemeMaster getNSENFOLumpsumSchemecode(String product_name)
    {
        List<NseOnlineSchemeMaster> list = null;
        NseOnlineSchemeMaster schemeMaster = null;
        try
        {
            list = nseOnlineSchemeMasterRepository.findBySchemeNameIfActiveAndNormalPlanAndPurchaseAllowed(product_name);
            if(list != null && list.size() > 0)
            {
                schemeMaster = list.get(0);
            }
        }
        catch(Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date()); ex.printStackTrace();
        }
        return schemeMaster;
    }

    public NseOnlineSchemeMaster getNSENFOSipSchemecode(String product_name)
    {
        List<NseOnlineSchemeMaster> list = null;
        NseOnlineSchemeMaster schemeMaster = null;
        try
        {

            list = nseOnlineSchemeMasterRepository.findBySchemeNameForSipPurchaseAllowedAndActive(product_name);
            if(list != null && list.size() > 0)
            {
                schemeMaster = list.get(0);
            }
        }
        catch(Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date()); ex.printStackTrace();
        }
        return schemeMaster;
    }

    public NseOnlineSchemeMaster getNSESipSchemecode(String scheme_name, String dividend_code)
    {
        List<NseOnlineSchemeMaster> list = null;
        NseOnlineSchemeMaster schemeMaster = null;

        System.out.println("schemeName = " + scheme_name);
        System.out.println("dividendCode = " + dividend_code);

        try
        {
            scheme_name = NseUtils.checkParem(scheme_name);
            dividend_code = NseUtils.checkParem(dividend_code);

            if(StringHelper.isEmpty(dividend_code) || dividend_code.equalsIgnoreCase("Z"))
            {
                String dividend_type1 = "Z";
                list = nseOnlineSchemeMasterRepository.findEligibleSipSchemes(scheme_name,dividend_type1);

                if(list != null && list.size() > 0)
                {
                    schemeMaster = list.get(0);
                }
            } else
            {
                list = nseOnlineSchemeMasterRepository.findEligibleSipSchemes(scheme_name,dividend_code);

                if(list != null && list.size() > 0)
                {
                    schemeMaster = list.get(0);
                }else
                {
                    String dividend_type1 = "X";
                    list = nseOnlineSchemeMasterRepository.findEligibleSipSchemes(scheme_name,dividend_type1);

                    if(list != null && list.size() > 0)
                    {
                        schemeMaster = list.get(0);
                    }
                }

            }
        }
        catch(Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date()); ex.printStackTrace();
        }
        return schemeMaster;
    }

    public NseOnlineSchemeMaster getNSESwitchSchemecode(String scheme_name, String dividend_code)
    {
        NseOnlineSchemeMaster schemeMaster = null;
        try
        {
            if(NseUtils.isUrlEncoded(scheme_name))
            {
                scheme_name = URLDecoder.decode(scheme_name, StandardCharsets.UTF_8);
            }

            List<NseOnlineSchemeMaster> list = nseOnlineSchemeMasterRepository.findEligibleSchemesForSwitchAndRedemption(scheme_name,dividend_code);

            if(list != null && list.size() > 0)
            {
                schemeMaster = list.get(0);
            }

        } catch(Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date()); ex.printStackTrace();
        }
        return schemeMaster;
    }

    public NseOnlineSchemeMaster getNSERedemSchemeCode(String scheme, String dividend_code)
    {
        NseOnlineSchemeMaster schemeMaster = null;
        try
        {

            if(NseUtils.isUrlEncoded(scheme))
            {
                scheme = URLDecoder.decode(scheme, StandardCharsets.UTF_8);
            }

            List<NseOnlineSchemeMaster> list = nseOnlineSchemeMasterRepository.findEligibleSchemesForSwitchAndRedemption(scheme,dividend_code);

            if(list != null && list.size() > 0)
            {
                schemeMaster = list.get(0);
            }
        }
        catch(Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date()); ex.printStackTrace();
        }
        return schemeMaster;
    }

    public NseOnlineSchemeMaster getNSEStpSchemecode(String scheme, String dividend_code)
    {
        NseOnlineSchemeMaster schemeMaster = null;
        try
        {

            if(NseUtils.isUrlEncoded(scheme))
            {
                scheme = URLDecoder.decode(scheme, StandardCharsets.UTF_8);
            }

            List<NseOnlineSchemeMaster> list = nseOnlineSchemeMasterRepository.findSTPEnabledSchemes(scheme,dividend_code);

            if(list != null && list.size() > 0)
            {
                schemeMaster = list.get(0);
            }
        }
        catch(Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date()); ex.printStackTrace();
        }
        return schemeMaster;
    }

    public NseOnlineSchemeMaster getNSESwpSchemecode(String scheme, String dividend_code)
    {
        NseOnlineSchemeMaster schemeMaster = null;
        try
        {

            if(NseUtils.isUrlEncoded(scheme))
            {
                scheme = URLDecoder.decode(scheme, StandardCharsets.UTF_8);
            }

            List<NseOnlineSchemeMaster> list = nseOnlineSchemeMasterRepository.findSTPEnabledSchemes(scheme,dividend_code);

            if(list != null && list.size() > 0)
            {
                schemeMaster = list.get(0);
            }
        }
        catch(Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date()); ex.printStackTrace();
        }
        return schemeMaster;
    }

    public List<CartDto> getCartListByUserId(Integer user_id, String client_name, String payment_status, String purchase_type,@RequestHeader("Authorization") String token)
    {

        List<CartDto> list = null;
        try
        {
            if(!payment_status.isEmpty() && payment_status.equalsIgnoreCase("SUCCESS"))
            {
                list = userServiceClient.getInactiveSuccessfulCarts(user_id,purchase_type, client_name,token);
            }else
            {
                list = userServiceClient.getActiveCartsByUserIdAndClientNameAndPurchaseType(user_id,purchase_type, client_name,token);
            }

            if(list != null && list.size() > 0)
            {
                for (CartDto cart : list)
                {
                    String amc_name = cart.getScheme_company();

                    if(amc_name == null){amc_name = "";}

                    String logoName = amcLogoPath + NseUtils.getLogoByAmcNameOrSchemeName(amc_name);
                    String logo = logoName;

                    cart.setScheme_logo(logo);;

                    if(!cart.getTo_scheme_company().isEmpty())
                    {
                        logoName = amcLogoPath + NseUtils.getLogoByAmcNameOrSchemeName(cart.getTo_scheme_company());
                        logo = logoName;

                        cart.setTo_scheme_logo(logo);;
                    }
                }
            }

        }
        catch(Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date()); ex.printStackTrace();
        }
        return list;
    }

    public List<CartDto> getCartCount(Integer user_id, String client_name,@RequestHeader("Authorization") String token)
    {
        List<CartDto> list = null;
        try
        {
            list = userServiceClient.getAllActiveCartsByUserId(user_id,client_name, "NSE",token);
        }
        catch(Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date()); ex.printStackTrace();
        }
        return list;
    }

    public List<CartDto> getCartCountForPurchaseType(Integer user_id, String client_name,String purchase_type,String broker_code,@RequestHeader("Authorization") String token)
    {
        List<CartDto> list = null;
        try
        {
            list = userServiceClient.getAllActiveCartsByUserIdAndClientCode(user_id,client_name, "NSE",purchase_type,broker_code,token);
        }
        catch(Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date());
            ex.printStackTrace();
        }
        return list;
    }

    public CartDto getPurchaseCartForBse(Integer user_id, String investor_code, String folio_no, String scheme_name, String scheme_reinvest_tag,String to_scheme_name, String client_name, String purchase_type,@RequestHeader("Authorization") String token)
    {
        CartDto cart = null;
        try
        {

            List<CartDto> list = userServiceClient.getPurchaseCartUserForNse(user_id,investor_code,folio_no,purchase_type,scheme_name,scheme_reinvest_tag,to_scheme_name,client_name,token);

            if(list != null && list.size() > 0)
            {
                cart = list.get(0);
            }
        }
        catch(Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date());
            ex.printStackTrace();
        }
        return cart;
    }

    public List<CartDto> getCartListById(Integer cart_id, @RequestHeader("Authorization") String token)
    {
        List<CartDto> list = null;
        try
        {

            list = userServiceClient.getActiveCartsById(cart_id,token);

            if(list != null && list.size() > 0)
            {
                for (CartDto cart : list)
                {
                    String amc_name = cart.getScheme_company();

                    if(amc_name == null){amc_name = "";}

                    String logoName = amcLogoPath + NseUtils.getLogoByAmcNameOrSchemeName(amc_name);
                    String logo = logoName;

                    cart.setScheme_logo(logo);;

                    if(!cart.getTo_scheme_company().isEmpty())
                    {
                        logoName = amcLogoPath + NseUtils.getLogoByAmcNameOrSchemeName(cart.getTo_scheme_company());
                        logo = logoName;

                        cart.setTo_scheme_logo(logo);;
                    }
                }
            }

        }
        catch(Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date());
            ex.printStackTrace();
        }
        return list;
    }

}
