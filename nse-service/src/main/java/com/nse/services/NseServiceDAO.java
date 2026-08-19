package com.nse.services;

import com.nse.client.AmfiServiceClient;
import com.nse.client.UserServiceClient;
import com.nse.dto.amfi.AmfiLatestNavDto;
import com.nse.dto.amfi.AmfiSchemeMasterDTO;
import com.nse.dto.mf.*;
import com.nse.model.NseBank;
import com.nse.model.NseOnlineSchemeMaster;
import com.nse.repository.NseBankRepository;
import com.nse.repository.NseOnlineSchemeMasterRepository;
import com.nse.repository.NseTransactionRepository;
import com.nse.response.*;
import com.nse.utils.NseUtils;
import feign.FeignException;
import org.hibernate.internal.util.StringHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestHeader;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NseServiceDAO {
    @Autowired
    UserServiceClient userServiceClient;

    @Autowired
    AmfiServiceClient amfiServiceClient;

    @Autowired
    NseOnlineSchemeMasterRepository nseOnlineSchemeMasterRepository;

    @Autowired
    NseBankRepository nseBankRepository;

    @Autowired
    NseTransactionRepository nseTransactionRepository;


    public List<String> getFolioNumberByAMCwithHoldings(String client_name, Integer user_id, String amc_name,
                                                        String holding_nature_code, String tax_status_code,
                                                        String joint_holder_pan1, String joint_holder_pan2,@RequestHeader("Authorization") String token) {
        List<String> result = new ArrayList<>();
        System.out.println("Fetching folio numbers for AMC: " + amc_name + ", Client: " + client_name + ", User ID: " + user_id);
        try {

            String amc_code = NseUtils.getAMCCode(amc_name);
            String rta_name = NseUtils.getRTAName(amc_code);

            System.out.println("rta_name = " + rta_name);
            System.out.println("amc_code = " + amc_code);

            if (StringHelper.isNotEmpty(rta_name) && rta_name.equalsIgnoreCase("CAMS")) {

                List<String> schemeCodes = userServiceClient.getUsersPortfolioSchemewise(user_id, client_name, amc_code, token);

                System.out.println("Scheme Codes: " + schemeCodes);

                if (schemeCodes == null) {
                    List<AmfiSchemeMasterDTO> camsCodesRaw = amfiServiceClient.getschemeCamsProductCodesByCompany(amc_name, token);

                    List<String> flatCodes = new ArrayList<>();
                    for (AmfiSchemeMasterDTO dto : camsCodesRaw) {
                        String codeGroup = dto.getScheme_cams_productcode();
                        if (StringHelper.isNotEmpty(codeGroup)) {
                            String[] splitCodes = codeGroup.split(",");
                            for (String code : splitCodes) {
                                if (StringHelper.isNotEmpty(code.trim())) {
                                    flatCodes.add(code.trim());
                                }
                            }
                        }
                    }

                    result = flatCodes.stream().distinct().collect(Collectors.toList());
                }

                List<InvestorMasterCamsDto> camsList = null;
                try {
                    camsList = userServiceClient.getinvestorMasterCams(user_id, client_name, amc_code, token);
                    System.out.println("CAMS List: " + camsList);
                } catch (FeignException e) {
                    String response = e.contentUTF8();
                    if (response.contains("No schemes found")) {
                        System.out.println("No CAMS schemes found, skipping to next method.");
                        camsList = null;
                    } else {
                        System.out.println("Unexpected error from CAMS: " + response);
                        throw e;
                    }
                }


                System.out.println("CAMS List: " + camsList);


                if (camsList != null && !camsList.isEmpty()) {
                    for (InvestorMasterCamsDto camsScheme : camsList) {
                        String holding = camsScheme.getHolding_na();
                        String joint1_pan = camsScheme.getJoint1_pan();
                        String joint2_pan = camsScheme.getJoint2_pan();
                        String bank_acc_type = camsScheme.getAc_type();

                        if (holding == null) holding = "";
                        if (joint1_pan == null) joint1_pan = "";
                        if (joint2_pan == null) joint2_pan = "";
                        if (bank_acc_type == null) bank_acc_type = "";

                        holding = holding.trim();
                        joint1_pan = joint1_pan.trim();
                        joint2_pan = joint2_pan.trim();
                        bank_acc_type = bank_acc_type.trim();
                        tax_status_code = tax_status_code.trim();
                        holding_nature_code = holding_nature_code.trim();
                        joint_holder_pan1 = joint_holder_pan1.trim();
                        joint_holder_pan2 = joint_holder_pan2.trim();

                        if (tax_status_code.equalsIgnoreCase("01")) {
                            if (holding_nature_code.equalsIgnoreCase("SI")) {
                                if (holding.equalsIgnoreCase("SI")) {
                                    result.add(camsScheme.getFoliochk());
                                }
                            } else if (holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES")) {
                                if (holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES")) {
                                    if (joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan)) {
                                        result.add(camsScheme.getFoliochk());
                                    }
                                }
                            } else if (holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO")) {
                                if (joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan)) {
                                    result.add(camsScheme.getFoliochk());
                                }
                            } else {

                            }
                        } else if (tax_status_code.equalsIgnoreCase("11") || tax_status_code.equalsIgnoreCase("21")) {
                            if (tax_status_code.equalsIgnoreCase("11") && bank_acc_type.equalsIgnoreCase("NRO")) {
                                if (holding_nature_code.equalsIgnoreCase("SI")) {
                                    if (holding.equalsIgnoreCase("SI")) {
                                        result.add(camsScheme.getFoliochk());
                                    }
                                } else if (holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES")) {
                                    if (holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES")) {
                                        if (joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan)) {
                                            result.add(camsScheme.getFoliochk());
                                        }
                                    }
                                } else if (holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO")) {
                                    if (joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan)) {
                                        result.add(camsScheme.getFoliochk());
                                    }
                                } else {

                                }
                            } else if (tax_status_code.equalsIgnoreCase("21") && bank_acc_type.equalsIgnoreCase("NRE")) {
                                if (holding_nature_code.equalsIgnoreCase("SI")) {
                                    if (holding.equalsIgnoreCase("SI")) {
                                        result.add(camsScheme.getFoliochk());
                                    }
                                } else if (holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES")) {
                                    if (holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES")) {
                                        if (joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan)) {
                                            result.add(camsScheme.getFoliochk());
                                        }
                                    }
                                } else if (holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO")) {
                                    if (joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan)) {
                                        result.add(camsScheme.getFoliochk());
                                    }
                                } else {

                                }
                            } else {

                            }

                        } else {
                            result.add(camsScheme.getFoliochk());
                        }
                    }
                }
            }

            System.out.println("result: " + result);

                if (StringHelper.isNotEmpty(rta_name) && rta_name.equalsIgnoreCase("Karvy")) {

                    List<InvestorMasterKarvyDto> karvyList = userServiceClient.getinvestorMasterKarvy(user_id, client_name, amc_code, token);
                    System.out.println("karvyList =" + karvyList);

                    if (karvyList != null && !karvyList.isEmpty()) {
                        for (InvestorMasterKarvyDto karvyScheme : karvyList) {
                            String holding = karvyScheme.getMode_of_holding();
                            String pan2 = karvyScheme.getPan2();
                            String pan3 = karvyScheme.getPan3();
                            String bank_acc_type = karvyScheme.getAccount_type();

                            if (holding == null) holding = "";
                            if (pan2 == null) pan2 = "";
                            if (pan3 == null) pan3 = "";
                            if (bank_acc_type == null) bank_acc_type = "";

                            holding = holding.trim();
                            pan2 = pan2.trim();
                            pan3 = pan3.trim();
                            bank_acc_type = bank_acc_type.trim();
                            tax_status_code = tax_status_code.trim();
                            holding_nature_code = holding_nature_code.trim();
                            joint_holder_pan1 = joint_holder_pan1.trim();
                            joint_holder_pan2 = joint_holder_pan2.trim();

                            if (tax_status_code.equalsIgnoreCase("01")) {
                                if (holding.equalsIgnoreCase("1")) {
                                    holding = "SI";
                                } else if (holding.equalsIgnoreCase("2") || holding.equalsIgnoreCase("J")) {
                                    holding = "JO";
                                } else if (holding.equalsIgnoreCase("3") || holding.equalsIgnoreCase("5")) {
                                    holding = "ES";
                                } else if (holding.equalsIgnoreCase("4") || holding.equalsIgnoreCase("7")) {
                                    holding = "AS";
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
                                        result.add(karvyScheme.getFolio());
                                    }
                                } else if (holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES")) {
                                    if (holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES")) {
                                        if (joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3)) {
                                            result.add(karvyScheme.getFolio());
                                        }
                                    }
                                } else if (holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO")) {
                                    if (joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3)) {
                                        result.add(karvyScheme.getFolio());
                                    }
                                } else {

                                }
                            } else if (tax_status_code.equalsIgnoreCase("11") || tax_status_code.equalsIgnoreCase("21")) {
                                if (tax_status_code.equalsIgnoreCase("11") && bank_acc_type.equalsIgnoreCase("NRO")) {
                                    if (holding.equalsIgnoreCase("1")) {
                                        holding = "SI";
                                    } else if (holding.equalsIgnoreCase("2") || holding.equalsIgnoreCase("J")) {
                                        holding = "JO";
                                    } else if (holding.equalsIgnoreCase("3") || holding.equalsIgnoreCase("5")) {
                                        holding = "ES";
                                    } else if (holding.equalsIgnoreCase("4") || holding.equalsIgnoreCase("7")) {
                                        holding = "AS";
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
                                            result.add(karvyScheme.getFolio());
                                        }
                                    } else if (holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES")) {
                                        if (holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES")) {
                                            if (joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3)) {
                                                result.add(karvyScheme.getFolio());
                                            }
                                        }
                                    } else if (holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO")) {
                                        if (joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3)) {
                                            result.add(karvyScheme.getFolio());
                                        }
                                    } else {

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
                                            result.add(karvyScheme.getFolio());
                                        }
                                    } else if (holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES")) {
                                        if (holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES")) {
                                            if (joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3)) {
                                                result.add(karvyScheme.getFolio());
                                            }
                                        }
                                    } else if (holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO")) {
                                        if (joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3)) {
                                            result.add(karvyScheme.getFolio());
                                        }
                                    } else {

                                    }
                                } else {

                                }
                            } else {
                                result.add(karvyScheme.getFolio());
                            }
                        }
                    }
                }

                result = new ArrayList<String>(new LinkedHashSet<String>(result));




        } catch (Exception ex) {
            System.err.println("Error fetching folio numbers: " + ex.getMessage());
        }

        return result;
    }



    public NseOnlineSchemeMaster getLumpsumSchemecodeService(String schemeName, String dividendCode, String amount) {
        if (StringHelper.isEmpty(amount)) {
            amount = "100000";
        }

        System.out.println("schemeName = " + schemeName + dividendCode + amount);

        try {
            Double lumpsumAmount = Double.parseDouble(amount);
            List<NseOnlineSchemeMaster> schemeList;

            System.out.println(lumpsumAmount);

            if (lumpsumAmount >= 200000) {
                System.out.println("lumpsumAmount");
                schemeList = nseOnlineSchemeMasterRepository.findWithoutAmountCondition(schemeName, dividendCode);
                if (schemeList.isEmpty()) {
                    schemeList = nseOnlineSchemeMasterRepository.findWithMinAmountGTE(schemeName, dividendCode);
                    if (schemeList.isEmpty()) {
                        schemeList = nseOnlineSchemeMasterRepository.findWithMinAmountLT(schemeName, dividendCode);
                    }
                }
            } else {
                System.out.println("lumpsumAmount123");
                schemeList = nseOnlineSchemeMasterRepository.findWithMinAmountLT(schemeName, dividendCode);
            }
            System.out.println("schemeList = "  + schemeList);
            if (!schemeList.isEmpty()) {
                NseOnlineSchemeMaster selectedScheme = schemeList.get(0);
                return selectedScheme;
            } else {
                System.out.println("No matching scheme found.");
                return null;
            }

        } catch (FeignException.NotFound e) {
            System.out.println("No matching scheme found in AMFI service.");
            return null;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }




    //getSchemeFolioNumbers

    public List<String> getSchemeFolioNumbers(String client_name, Integer user_id, String scheme_name,
                                                        String holding_nature_code, String tax_status_code,
                                                        String joint_holder_pan1, String joint_holder_pan2,@RequestHeader("Authorization") String token) {
        List<String> result = new ArrayList<>();
        System.out.println("Fetching folio numbers for AMC: " + scheme_name + ", Client: " + client_name + ", User ID: " + user_id);

        String registrar = "";
        String amc_code = "";
        String scheme_code = "";
        String cams = "";
        String karvy = "";

        List<String> list = new ArrayList<String>();
        try {

            List<UsersPortfolioSchemewiseDto> schemeCodes = userServiceClient.getUsersPortfolioSchemewiseUser(user_id, client_name, scheme_name,token);

            List<UsersPortfolioSchemewiseDto> scheme_list = schemeCodes;

            System.out.println("Scheme Codes: " + scheme_list);


            if(scheme_list != null && scheme_list.size() > 0) {
                scheme_code = scheme_list.get(0).getScheme_code();
                registrar = scheme_list.get(0).getRegistrar();
                amc_code = scheme_list.get(0).getAmc_code();

                List<String> productList = new ArrayList<>();
                productList.add(scheme_code);


                System.out.println("registrar Code: " + registrar);

                if (StringHelper.isNotEmpty(registrar) && registrar.equalsIgnoreCase("CAMS")) {
                    List<InvestorMasterCamsDto> camsList = userServiceClient.getProductCode(user_id, client_name, productList,token);
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
                                        list.add(camsScheme.getFoliochk());
                                    }
                                } else if (holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES")) {
                                    if (holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES")) {
                                        if (joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan)) {
                                            list.add(camsScheme.getFoliochk());
                                        }
                                    }
                                } else if (holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO")) {
                                    if (joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan)) {
                                        list.add(camsScheme.getFoliochk());
                                    }
                                } else {

                                }
                            } else if (tax_status_code.equalsIgnoreCase("11") || tax_status_code.equalsIgnoreCase("21")) {
                                if (tax_status_code.equalsIgnoreCase("11") && bank_acc_type.equalsIgnoreCase("NRO")) {
                                    if (holding_nature_code.equalsIgnoreCase("SI")) {
                                        if (holding.equalsIgnoreCase("SI")) {
                                            list.add(camsScheme.getFoliochk());
                                        }
                                    } else if (holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES")) {
                                        if (holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES")) {
                                            if (joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan)) {
                                                list.add(camsScheme.getFoliochk());
                                            }
                                        }
                                    } else if (holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO")) {
                                        if (joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan)) {
                                            list.add(camsScheme.getFoliochk());
                                        }
                                    } else {

                                    }
                                } else if (tax_status_code.equalsIgnoreCase("21") && bank_acc_type.equalsIgnoreCase("NRE")) {
                                    if (holding_nature_code.equalsIgnoreCase("SI")) {
                                        if (holding.equalsIgnoreCase("SI")) {
                                            list.add(camsScheme.getFoliochk());
                                        }
                                    } else if (holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES")) {
                                        if (holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES")) {
                                            if (joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan)) {
                                                list.add(camsScheme.getFoliochk());
                                            }
                                        }
                                    } else if (holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO")) {
                                        if (joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan)) {
                                            list.add(camsScheme.getFoliochk());
                                        }
                                    } else {

                                    }
                                } else {

                                }

                            } else {
                                list.add(camsScheme.getFoliochk());
                            }
                        }
                    }
                }

                if (StringHelper.isNotEmpty(registrar) && registrar.equalsIgnoreCase("KARVY")) {
                    List<InvestorMasterKarvyDto> karvyList = null;

                    if (amc_code.equalsIgnoreCase("103")) {
                        karvyList = userServiceClient.getinvestorMasterKarvyScheme(user_id, client_name, scheme_name,token);
                    } else {
                        karvyList = userServiceClient.getinvestorMasterKarvySchemes(user_id, client_name, scheme_name,token);
                    }

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

                            if (tax_status_code.equalsIgnoreCase("01")) {
                                if (holding.equalsIgnoreCase("1")) {
                                    holding = "SI";
                                } else if (holding.equalsIgnoreCase("2") || holding.equalsIgnoreCase("J")) {
                                    holding = "JO";
                                } else if (holding.equalsIgnoreCase("3") || holding.equalsIgnoreCase("5")) {
                                    holding = "ES";
                                } else if (holding.equalsIgnoreCase("4") || holding.equalsIgnoreCase("7")) {
                                    holding = "AS";
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
                                        list.add(karvyScheme.getFolio());
                                    }
                                } else if (holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES")) {
                                    if (holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES")) {
                                        if (joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3)) {
                                            list.add(karvyScheme.getFolio());
                                        }
                                    }
                                } else if (holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO")) {
                                    if (joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3)) {
                                        list.add(karvyScheme.getFolio());
                                    }
                                } else {

                                }
                            } else if (tax_status_code.equalsIgnoreCase("11") || tax_status_code.equalsIgnoreCase("21")) {
                                if (tax_status_code.equalsIgnoreCase("11") && bank_acc_type.equalsIgnoreCase("NRO")) {
                                    if (holding.equalsIgnoreCase("1")) {
                                        holding = "SI";
                                    } else if (holding.equalsIgnoreCase("2") || holding.equalsIgnoreCase("J")) {
                                        holding = "JO";
                                    } else if (holding.equalsIgnoreCase("3") || holding.equalsIgnoreCase("5")) {
                                        holding = "ES";
                                    } else if (holding.equalsIgnoreCase("4") || holding.equalsIgnoreCase("7")) {
                                        holding = "AS";
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
                                            list.add(karvyScheme.getFolio());
                                        }
                                    } else if (holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES")) {
                                        if (holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES")) {
                                            if (joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3)) {
                                                list.add(karvyScheme.getFolio());
                                            }
                                        }
                                    } else if (holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO")) {
                                        if (joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3)) {
                                            list.add(karvyScheme.getFolio());
                                        }
                                    } else {

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
                                            list.add(karvyScheme.getFolio());
                                        }
                                    } else if (holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES")) {
                                        if (holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES")) {
                                            if (joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3)) {
                                                list.add(karvyScheme.getFolio());
                                            }
                                        }
                                    } else if (holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO")) {
                                        if (joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3)) {
                                            list.add(karvyScheme.getFolio());
                                        }
                                    } else {

                                    }
                                } else {

                                }
                            } else {
                                list.add(karvyScheme.getFolio());
                            }
                        }
                    }
                }
            }else
            {
                List<AmfiSchemeMasterDTO> schemeMappingList = amfiServiceClient.findBySchemeAmfiAndActive(scheme_name,token);

                if(schemeMappingList != null && schemeMappingList.size() > 0)
                {
                    cams = schemeMappingList.get(0).getScheme_cams_productcode();
                    karvy = schemeMappingList.get(0).getScheme_karvy_productcode();
                }

                List<String> prodcodeList = new ArrayList<String>();

                if(StringHelper.isNotEmpty(cams))
                {
                    prodcodeList = Arrays.asList(cams.split(","));
                    prodcodeList = prodcodeList.stream().filter(item-> !item.trim().isEmpty()).collect(Collectors.toList());
                    HashSet<Object> seen = new HashSet<>();
                    prodcodeList.removeIf(c -> !seen.add(Arrays.asList(c)));

                    List<InvestorMasterCamsDto> camsList = userServiceClient.getProductCode(user_id, client_name, prodcodeList,token);

                    if(camsList.size() > 0)
                    {
                        for (InvestorMasterCamsDto camsScheme : camsList)
                        {
                            if(tax_status_code.equalsIgnoreCase("01") || tax_status_code.equalsIgnoreCase("11") || tax_status_code.equalsIgnoreCase("21"))
                            {
                                String holding = camsScheme.getHolding_na();
                                String joint1_pan = camsScheme.getJoint1_pan();
                                String joint2_pan = camsScheme.getJoint2_pan();
                                if(holding == null){holding = "";}
                                if(joint1_pan == null){joint1_pan = "";}
                                if(joint2_pan == null){joint2_pan = "";}

                                if(holding_nature_code.equalsIgnoreCase("SI"))
                                {
                                    if(holding.equalsIgnoreCase("SI"))
                                    {
                                        list.add(camsScheme.getFoliochk());
                                    }
                                }else if(holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES"))
                                {
                                    if(holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES"))
                                    {
                                        if(joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan))
                                        {
                                            list.add(camsScheme.getFoliochk());
                                        }
                                    }
                                }else if(holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO"))
                                {
                                    if(joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan))
                                    {
                                        list.add(camsScheme.getFoliochk());
                                    }
                                }else
                                {

                                }
                            }else
                            {
                                list.add(camsScheme.getFoliochk());
                            }
                        }
                    }
                }

                if(StringHelper.isNotEmpty(karvy))
                {
                    prodcodeList = Arrays.asList(karvy.split(","));
                    prodcodeList = prodcodeList.stream().filter(item-> !item.trim().isEmpty()).collect(Collectors.toList());
                    HashSet<Object> seen = new HashSet<>();
                    prodcodeList.removeIf(c -> !seen.add(Arrays.asList(c)));


                    List<InvestorMasterKarvyDto> karvyList = userServiceClient.getProductCodes(user_id, client_name, prodcodeList,token);

                    if(karvyList.size() > 0)
                    {
                        for (InvestorMasterKarvyDto karvyScheme : karvyList)
                        {
                            if(tax_status_code.equalsIgnoreCase("01") || tax_status_code.equalsIgnoreCase("11") || tax_status_code.equalsIgnoreCase("21"))
                            {
                                String holding = karvyScheme.getMode_of_holding();
                                String pan2 = karvyScheme.getPan2();
                                String pan3 = karvyScheme.getPan3();
                                if(holding == null){holding = "";}
                                if(pan2 == null){pan2 = "";}
                                if(pan3 == null){pan3 = "";}

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


                                if(holding_nature_code.equalsIgnoreCase("SI"))
                                {
                                    if(holding.equalsIgnoreCase("SI"))
                                    {
                                        list.add(karvyScheme.getFolio());
                                    }
                                }else if(holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES"))
                                {
                                    if(holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES"))
                                    {
                                        if(joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3))
                                        {
                                            list.add(karvyScheme.getFolio());
                                        }
                                    }
                                }else if(holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO"))
                                {
                                    if(joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3))
                                    {
                                        list.add(karvyScheme.getFolio());
                                    }
                                }else
                                {

                                }
                            }else
                            {
                                list.add(karvyScheme.getFolio());
                            }
                        }
                    }
                }
            }

            list = new ArrayList<String>(new LinkedHashSet<String>(list));



        } catch (Exception ex) {
            System.err.println("Error fetching folio numbers: " + ex.getMessage());
        }

        return list;
    }

    public void insertClientMasterData(JSONArray regDataArray, String broker_code, String client_name,String token) {
        try
        {
            String service_return_code = "";
            String service_msg = "";

            String title = "";
            String name = "";
            String pan = "";
            String mobile = "";
            String email = "";
            String type_id = "1";
            String branch = "";
            String rm_name = "";
            String subbroker_name = "";
            String active = "1";
            String user_password = "$2a$09$mLfc3oJMTmFmUe8IDxLS0upxrrq8b8PRHmbS7oO/KOK/N0XokdGtC";
            String user_pass = "123456";
            String street_1 = "";
            String street_2 = "";
            String street_3 = "";
            String city = "";
            String pincode = "";
            String state = "";
            String state_code = "";
            String country = "";
            String phone_office = "";
            String phone_residence = "";

            String father_name = "";
            String gender = "";
            String date_of_birth = "";
            String marital_status = "";
            String place_of_birth = "";
            String country_of_birth = "";
            String country_birth_code = "";

            String source_of_wealth = "";
            String source_of_wealth_code = "";
            String annual_income = "";
            String annual_income_code = "";
            String occupation = "";
            String occupation_code = "";
            String networth_amount = "";
            String networth_dob = "";
            String political = "";
            String political_code = "";
            String address_type = "";
            String address_type_desc = "";

            String pan_tax_status_code = "";
            String pan_tax_status_desc = "";
            String holding_nature = "";
            String holding_nature_desc = "";

            String guard_name = "";
            String guard_pan = "";
            String guard_dob = "";
            String guard_mobile = "";
            String guard_relationship = "";

            String email_verified = "0";
            String email_authcode = "";
            String mobile_verified = "0";
            String mobile_otp = "";
            String bse_customer = "0";
            String bse_active = "0";

            String bank_name = "";
            String bank_code = "";
            String bank_branch = "";
            String bank_address = "";
            String bank_address1 = "";
            String bank_address2 = "";
            String bank_address3 = "";
            String bank_account_number = "";
            String bank_account_holder_name = "";
            String bank_account_type = "";
            String bank_ifsc_code = "";
            String bank_micr_code = "";
            String default_bank = "Y";

            String nominee_required = "0";

            String otm_flag = "0";
            String otm = "";
            String otm_approved = "0";

            String joint_holder_name = "";
            String joint_holder_pan = "";
            String joint_holder_dob = "";
            String joint_holder_email = "";
            String joint_holder_mobile = "";

            String joint_holder_name1 = "";
            String joint_holder_pan1 = "";
            String joint_holder_dob1 = "";
            String joint_holder_email1 = "";
            String joint_holder_mobile1 = "";

            String nri_address1 = "";
            String nri_address2 = "";
            String nri_address3 = "";
            String nri_city = "";
            String nri_state = "";
            String nri_pincode = "";
            String nri_country = "";

            String number_of_nominee = "";
            String nominee_type = "";
            String nominee1_name = "";
            String nominee1_dob = "";
            String nominee1_address1 = "";
            String nominee1_address2 = "";
            String nominee1_address3 = "";
            String nominee1_pincode = "";
            String nominee1_city = "";
            String nominee1_state = "";
            String nominee1_relation = "";
            String nominee1_guard_name = "";
            String nominee1_guard_pan = "";

            String nominee2_type = "";
            String nominee2_name = "";
            String nominee2_dob = "";
            String nominee2_relation = "";
            String nominee2_percentage = "";
            String nominee2_guard_name = "";
            String nominee2_guard_pan = "";

            String nominee3_type = "";
            String nominee3_name = "";
            String nominee3_dob = "";
            String nominee3_relation = "";
            String nominee3_percentage = "";
            String nominee3_guard_name = "";
            String nominee3_guard_pan = "";

            String nse_customer = "1";
            String nse_active_user = "";
            String iin_number = "";
            String created_date = "";
            Date dob1 = null;
            String euin = "";

            String nom1_pan = "";
            String nom1_percentage = "";
            String nom1_country = "";
            String nom1_guard_rel = "";

            String nom2type = "";
            String nom2_guard_rel = "";
            String nom2_pan = "";

            String nom3type = "";
            String nom3_guard_rel = "";
            String nom3_pan = "";

            String account_type_2 = "";
            String account_no_2 = "";
            String micr_no_2 = "";
            String ifsc_code_2 = "";
            String bank_name_2 = "";
            String bank_branch_2 = "";
            String default_bank_flag_2 = "";
            String bank_2_created_at = "";
            String bank_2_last_modified_at = "";
            String bank_2_status = "";
            String bank_2_status_remarks = "";

            String account_type_3 = "";
            String account_no_3 = "";
            String micr_no_3 = "";
            String ifsc_code_3 = "";
            String bank_name_3 = "";
            String bank_branch_3 = "";
            String default_bank_flag_3 = "";
            String bank_3_created_at = "";
            String bank_3_last_modified_at = "";
            String bank_3_status = "";
            String bank_3_status_remarks = "";

            SimpleDateFormat df3 = new SimpleDateFormat("dd-MMM-yyyy");
            SimpleDateFormat df2 = new SimpleDateFormat("dd-MM-yyyy");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

            SimpleDateFormat df4 = new SimpleDateFormat("dd/MM/yyyy");


            try{

                List<NseBank> bank_list = nseBankRepository.findAll();

                BseNseKeyDto nsekey = userServiceClient.getByClientName(client_name,token);
                String default_branch = nsekey.getDefault_branch();
                String default_rm = nsekey.getDefault_rm();
                String broker_code1 = nsekey.getBrokerCode();
                String euin1 = nsekey.getEuin();


                for (int i= 0; i<regDataArray.length(); i++) {
                    title = "";
                    name = "";
                    pan = "";
                    mobile = "";
                    email = "";
                    subbroker_name = "";
                    street_1 = "";
                    street_2 = "";
                    street_3 = "";
                    city = "";
                    pincode = "";
                    state = "";
                    state_code = "";
                    country = "";
                    phone_office = "";
                    phone_residence = "";

                    father_name = "";
                    gender = "";
                    date_of_birth = "";
                    marital_status = "";
                    place_of_birth = "";
                    country_of_birth = "";
                    country_birth_code = "";

                    source_of_wealth = "";
                    source_of_wealth_code = "";
                    annual_income = "";
                    annual_income_code = "";
                    occupation = "";
                    occupation_code = "";
                    networth_amount = "";
                    networth_dob = "";
                    political = "";
                    political_code = "";
                    address_type = "";
                    address_type_desc = "";

                    pan_tax_status_code = "";
                    pan_tax_status_desc = "";
                    holding_nature = "";
                    holding_nature_desc = "";

                    guard_name = "";
                    guard_pan = "";
                    guard_dob = "";
                    guard_mobile = "";
                    guard_relationship = "";

                    email_verified = "0";
                    email_authcode = "";
                    mobile_verified = "0";
                    mobile_otp = "";
                    bse_customer = "0";
                    bse_active = "0";

                    bank_name = "";
                    bank_code = "";
                    bank_branch = "";
                    bank_address = "";
                    bank_address1 = "";
                    bank_address2 = "";
                    bank_address3 = "";
                    bank_account_number = "";
                    bank_account_holder_name = "";
                    bank_account_type = "";
                    bank_ifsc_code = "";
                    bank_micr_code = "";
                    default_bank = "Y";

                    nominee_required = "0";

                    otm_flag = "0";
                    otm = "";
                    otm_approved = "0";

                    joint_holder_name = "";
                    joint_holder_pan = "";
                    joint_holder_dob = "";
                    joint_holder_email = "";
                    joint_holder_mobile = "";

                    joint_holder_name1 = "";
                    joint_holder_pan1 = "";
                    joint_holder_dob1 = "";
                    joint_holder_email1 = "";
                    joint_holder_mobile1 = "";

                    nri_address1 = "";
                    nri_address2 = "";
                    nri_address3 = "";
                    nri_city = "";
                    nri_state = "";
                    nri_pincode = "";
                    nri_country = "";

                    number_of_nominee = "";
                    nominee_type = "";
                    nominee1_name = "";
                    nominee1_dob = "";
                    nominee1_address1 = "";
                    nominee1_address2 = "";
                    nominee1_address3 = "";
                    nominee1_pincode = "";
                    nominee1_city = "";
                    nominee1_state = "";
                    nominee1_relation = "";
                    nominee1_guard_name = "";
                    nominee1_guard_pan = "";

                    nominee2_type = "";
                    nominee2_name = "";
                    nominee2_dob = "";
                    nominee2_relation = "";
                    nominee2_percentage = "";
                    nominee2_guard_name = "";
                    nominee2_guard_pan = "";

                    nominee3_type = "";
                    nominee3_name = "";
                    nominee3_dob = "";
                    nominee3_relation = "";
                    nominee3_percentage = "";
                    nominee3_guard_name = "";
                    nominee3_guard_pan = "";

                    nse_customer = "1";
                    nse_active_user = "";
                    created_date = "";
                    dob1 = null;

                    nom1_pan = "";
                    nom1_percentage = "";
                    nom1_country = "";
                    nom1_guard_rel = "";

                    nom2type = "";
                    nom2_guard_rel = "";
                    nom2_pan = "";

                    nom3type = "";
                    nom3_guard_rel = "";
                    nom3_pan = "";

                    JSONObject jsonObject = regDataArray.getJSONObject(i);

                    String member_code = jsonObject.getString("member_code");
                    String client_code = jsonObject.getString("client_code");
                    String primary_holder_first_name = jsonObject.getString("primary_holder_first_name");
                    String primary_holder_middle_name = jsonObject.getString("primary_holder_middle_name");
                    String primary_holder_last_name = jsonObject.getString("primary_holder_last_name");
                    String tax_status = jsonObject.getString("tax_status");
                    gender = jsonObject.getString("gender");

                    String primary_holder_dob_incorporation = jsonObject.getString("primary_holder_dob_incorporation");

                    occupation_code = jsonObject.getString("occupation_code");
                    holding_nature = jsonObject.getString("holding_nature");

                    String second_holder_first_name = jsonObject.getString("second_holder_first_name");
                    String second_holder_middle_name = jsonObject.getString("second_holder_middle_name");
                    String second_holder_last_name = jsonObject.getString("second_holder_last_name");
                    String third_holder_first_name = jsonObject.getString("third_holder_first_name");
                    String third_holder_middle_name = jsonObject.getString("third_holder_middle_name");
                    String third_holder_last_name = jsonObject.getString("third_holder_last_name");
                    String second_holder_dob = jsonObject.getString("second_holder_dob");
                    String third_holder_dob = jsonObject.getString("third_holder_dob");
                    String guardian_first_name = jsonObject.getString("guardian_first_name");
                    String guardian_middle_name = jsonObject.getString("guardian_middle_name");
                    String guardian_last_name = jsonObject.getString("guardian_last_name");
                    String guardian_dob = jsonObject.getString("guardian_dob");
                    String primary_holder_pan_exempt = jsonObject.getString("primary_holder_pan_exempt");
                    String second_holder_pan_exempt = jsonObject.getString("second_holder_pan_exempt");
                    String third_holder_pan_exempt = jsonObject.getString("third_holder_pan_exempt");
                    String guardian_pan_exempt = jsonObject.getString("guardian_pan_exempt");
                    String primary_holder_pan = jsonObject.getString("primary_holder_pan");
                    String second_holder_pan = jsonObject.getString("second_holder_pan");
                    String third_holder_pan = jsonObject.getString("third_holder_pan");
                    String guardian_pan = jsonObject.getString("guardian_pan");
                    String primary_holder_exempt_category = jsonObject.getString("primary_holder_exempt_category");
                    String second_holder_exempt_category = jsonObject.getString("second_holder_exempt_category");
                    String third_holder_exempt_category = jsonObject.getString("third_holder_exempt_category");
                    String guardian_exempt_category = jsonObject.getString("guardian_exempt_category");
                    String client_type = jsonObject.getString("client_type");
                    String dp_status = jsonObject.getString("dp_status");
                    String dp_status_remarks = jsonObject.getString("dp_status_remarks");
                    String pms = jsonObject.getString("pms");
                    String default_dp = jsonObject.getString("default_dp");
                    String cdsl_dpid = jsonObject.getString("cdsl_dpid");
                    String cdslcltid = jsonObject.getString("cdslcltid");
                    String cmbp_id = jsonObject.getString("cmbp_id");
                    String nsdldpid = jsonObject.getString("nsdldpid");
                    String nsdlcltid = jsonObject.getString("nsdlcltid");
                    String account_type_1 = jsonObject.getString("account_type_1");
                    String account_no_1 = jsonObject.getString("account_no_1");
                    String micr_no_1 = jsonObject.getString("micr_no_1");
                    String ifsc_code_1 = jsonObject.getString("ifsc_code_1");
                    String bank_name_1 = jsonObject.getString("bank_name_1");
                    String bank_branch_1 = jsonObject.getString("bank_branch_1");
                    String default_bank_flag_1 = jsonObject.getString("default_bank_flag_1");
                    String bank_1_created_at = jsonObject.getString("bank_1_created_at");
                    String bank_1_last_modified_at = jsonObject.getString("bank_1_last_modified_at");
                    String bank_1_status = jsonObject.getString("bank_1_status");
                    String bank_1_status_remarks = jsonObject.getString("bank_1_status_remarks");

                    account_type_2 = jsonObject.getString("account_type_2");
                    account_no_2 = jsonObject.getString("account_no_2");
                    micr_no_2 = jsonObject.getString("micr_no_2");
                    ifsc_code_2 = jsonObject.getString("ifsc_code_2");
                    bank_name_2 = jsonObject.getString("bank_name_2");
                    bank_branch_2 = jsonObject.getString("bank_branch_2");
                    default_bank_flag_2 = jsonObject.getString("default_bank_flag_2");
                    bank_2_created_at = jsonObject.getString("bank_2_created_at");
                    bank_2_last_modified_at = jsonObject.getString("bank_2_last_modified_at");
                    bank_2_status = jsonObject.getString("bank_2_status");
                    bank_2_status_remarks = jsonObject.getString("bank_2_status_remarks");

                    account_type_3 = jsonObject.getString("account_type_3");
                    account_no_3 = jsonObject.getString("account_no_3");
                    micr_no_3 = jsonObject.getString("micr_no_3");
                    ifsc_code_3 = jsonObject.getString("ifsc_code_3");
                    bank_name_3 = jsonObject.getString("bank_name_3");
                    bank_branch_3 = jsonObject.getString("bank_branch_3");
                    default_bank_flag_3 = jsonObject.getString("default_bank_flag_3");
                    bank_3_created_at = jsonObject.getString("bank_3_created_at");
                    bank_3_last_modified_at = jsonObject.getString("bank_3_last_modified_at");
                    bank_3_status = jsonObject.getString("bank_3_status");
                    bank_3_status_remarks = jsonObject.getString("bank_3_status_remarks");

                    String account_type_4 = jsonObject.getString("account_type_4");
                    String account_no_4 = jsonObject.getString("account_no_4");
                    String micr_no_4 = jsonObject.getString("micr_no_4");
                    String ifsc_code_4 = jsonObject.getString("ifsc_code_4");
                    String bank_name_4 = jsonObject.getString("bank_name_4");
                    String bank_branch_4 = jsonObject.getString("bank_branch_4");
                    String default_bank_flag_4 = jsonObject.getString("default_bank_flag_4");
                    String bank_4_created_at = jsonObject.getString("bank_4_created_at");
                    String bank_4_last_modified_at = jsonObject.getString("bank_4_last_modified_at");
                    String bank_4_status = jsonObject.getString("bank_4_status");
                    String bank_4_status_remarks = jsonObject.getString("bank_4_status_remarks");
                    String account_type_5 = jsonObject.getString("account_type_5");
                    String account_no_5 = jsonObject.getString("account_no_5");
                    String micr_no_5 = jsonObject.getString("micr_no_5");
                    String ifsc_code_5 = jsonObject.getString("ifsc_code_5");
                    String bank_name_5 = jsonObject.getString("bank_name_5");
                    String bank_branch_5 = jsonObject.getString("bank_branch_5");
                    String default_bank_flag_5 = jsonObject.getString("default_bank_flag_5");
                    String bank_5_created_at = jsonObject.getString("bank_5_created_at");
                    String bank_5_last_modified_at = jsonObject.getString("bank_5_last_modified_at");
                    String bank_5_status = jsonObject.getString("bank_5_status");
                    String bank_5_status_remarks = jsonObject.getString("bank_5_status_remarks");
                    String cheque_name = jsonObject.getString("cheque_name");
                    String div_pay_mode = jsonObject.getString("div_pay_mode");
                    String address_1 = jsonObject.getString("address_1");
                    String address_2 = jsonObject.getString("address_2");
                    String address_3 = jsonObject.getString("address_3");
                    city = jsonObject.getString("city");
                    pincode = jsonObject.getString("pincode");
                    state = jsonObject.getString("state");
                    country = jsonObject.getString("country");
                    String resi_phone = jsonObject.getString("resi._phone");
                    String resi_fax = jsonObject.getString("resi._fax");
                    String office_phone = jsonObject.getString("office_phone");
                    String office_fax = jsonObject.getString("office_fax");
                    email = jsonObject.getString("email");
                    String communication_mode = jsonObject.getString("communication_mode");
                    String foreign_address_1 = jsonObject.getString("foreign_address_1");
                    String foreign_address_2 = jsonObject.getString("foreign_address_2");
                    String foreign_address_3 = jsonObject.getString("foreign_address_3");
                    String foreign_address_city = jsonObject.getString("foreign_address_city");
                    String foreign_address_pincode = jsonObject.getString("foreign_address_pincode");
                    String foreign_address_state = jsonObject.getString("foreign_address_state");
                    String foreign_address_country = jsonObject.getString("foreign_address_country");
                    String foreign_address_resi_phone = jsonObject.getString("foreign_address_resi_phone");
                    String foreign_address_fax = jsonObject.getString("foreign_address_fax");
                    String foreign_address_off_phone = jsonObject.getString("foreign_address_off._phone");
                    String foreign_address_off_fax = jsonObject.getString("foreign_address_off._fax");
                    String indian_mobile_no = jsonObject.getString("indian_mobile_no");
                    String primary_holder_kyc_type = jsonObject.getString("primary_holder_kyc_type");
                    String primary_holder_ckyc_number = jsonObject.getString("primary_holder_ckyc_number");
                    String second_holder_kyc_type = jsonObject.getString("second_holder_kyc_type");
                    String second_holder_ckyc_number = jsonObject.getString("second_holder_ckyc_number");
                    String third_holder_kyc_type = jsonObject.getString("third_holder_kyc_type");
                    String third_holder_kyc_number = jsonObject.getString("third_holder_kyc_number");
                    String guardian_kyc_type = jsonObject.getString("guardian_kyc_type");
                    String guardian_ckyc_number = jsonObject.getString("guardian_ckyc_number");
                    String primary_holder_kra_exempt_ref_no = jsonObject.getString("primary_holder_kra_exempt_ref._no.");
                    String second_holder_kra_exempt_ref_no = jsonObject.getString("second_holder_kra_exempt_ref._no.");
                    String third_holder_kra_exempt_ref_no = jsonObject.getString("third_holder_kra_exempt_ref._no");
                    String guardian_exempt_ref_no = jsonObject.getString("guardian_exempt_ref._no");
                    String aadhaar_updated = jsonObject.getString("aadhaar_updated");
                    String mapin_id = jsonObject.getString("mapin_id");
                    String paperless_flag = jsonObject.getString("paperless_flag");
                    String lei_no = jsonObject.getString("lei_no");
                    String lei_validity = jsonObject.getString("lei_validity");
                    String email_declaration_flag = jsonObject.getString("email_declaration_flag");
                    String mobile_declaration_flag = jsonObject.getString("mobile_declaration_flag");
                    String second_holder_email = jsonObject.getString("second_holder_email");
                    String second_holder_email_declaration = jsonObject.getString("second_holder_email_declaration");
                    String second_holder_mobile = jsonObject.getString("second_holder_mobile");
                    String second_holder_mobile_declaration = jsonObject.getString("second_holder_mobile_declaration");
                    String third_holder_email = jsonObject.getString("third_holder_email");
                    String third_holder_email_declaration = jsonObject.getString("third_holder_email_declaration");
                    String third_holder_mobile = jsonObject.getString("third_holder_mobile");
                    String third_holder_mobile_declaration = jsonObject.getString("third_holder_mobile_declaration");
                    String guardian_relationship = jsonObject.getString("guardian_relationship");
                    String nomination_opt = jsonObject.getString("nomination_opt");
                    String nomination_authentication_mode = jsonObject.getString("nomination_authentication_mode");
                    String nominee_1_name = jsonObject.getString("nominee_1_name");
                    String nominee_1_relationship = jsonObject.getString("nominee_1_relationship");
                    String nominee1_applicable = jsonObject.getString("nominee1_applicable");
                    String nominee_1_minor_flag = jsonObject.getString("nominee_1_minor_flag");
                    String nominee_1_dob = jsonObject.getString("nominee_1_dob");
                    String nominee_1_guardian = jsonObject.getString("nominee_1_guardian");
                    String nominee_1_guardian_pan = jsonObject.getString("nominee_1_guardian_pan");
                    String nominee_1_identity_type = jsonObject.getString("nominee_1_identity_type");
                    String nominee_1_id_number = jsonObject.getString("nominee_1_id_number");
                    String nominee_1_email = jsonObject.getString("nominee_1_email");
                    String nominee_1_mobile = jsonObject.getString("nominee_1_mobile");
                    String nominee_1_address1 = jsonObject.getString("nominee_1_address1");
                    String nominee_1_address2 = jsonObject.getString("nominee_1_address2");
                    String nominee_1_address3 = jsonObject.getString("nominee_1_address3");
                    String nominee_1_city = jsonObject.getString("nominee_1_city");
                    String nominee1_pin = jsonObject.getString("nominee1_pin");
                    String nominee_1_country = jsonObject.getString("nominee_1_country");
                    String nominee_2_name = jsonObject.getString("nominee_2_name");
                    String nominee_2_relationship = jsonObject.getString("nominee_2_relationship");
                    String nominee_2_applicable = jsonObject.getString("nominee_2_applicable");
                    String nominee_2_dob = jsonObject.getString("nominee_2_dob");
                    String nominee2_minor_flag = jsonObject.getString("nominee2_minor_flag");
                    String nominee2_guardian = jsonObject.getString("nominee2_guardian");
                    String nominee_2_guardian_pan = jsonObject.getString("nominee_2_guardian_pan");
                    String nominee_2_identity_type = jsonObject.getString("nominee_2_identity_type");
                    String nominee_2_id_number = jsonObject.getString("nominee_2_id_number");
                    String nominee_2_email = jsonObject.getString("nominee_2_email");
                    String nominee_2_mobile = jsonObject.getString("nominee_2_mobile");
                    String nominee_2_address1 = jsonObject.getString("nominee_2_address1");
                    String nominee_2_address2 = jsonObject.getString("nominee_2_address2");
                    String nominee_2_address3 = jsonObject.getString("nominee_2_address3");

                    String nominee_2_city = jsonObject.getString("nominee_2_city");
                    String nominee_2_pin = jsonObject.getString("nominee_2_pin");
                    String nominee_2_country = jsonObject.getString("nominee_2_country");
                    String nominee_3_name = jsonObject.getString("nominee_3_name");
                    String nominee_3_relationship = jsonObject.getString("nominee_3_relationship");
                    String nominee_3_applicable = jsonObject.getString("nominee_3_applicable");
                    String nominee_3_dob = jsonObject.getString("nominee_3_dob");
                    String nominee_3_minor_flag = jsonObject.getString("nominee_3_minor_flag");
                    String nominee_3_guardian = jsonObject.getString("nominee_3_guardian");
                    String nominee_3_guardian_pan = jsonObject.getString("nominee_3_guardian_pan");
                    String nominee_3_identity_type = jsonObject.getString("nominee_3_identity_type");
                    String nominee_3_id_number = jsonObject.getString("nominee_3_id_number");
                    String nominee_3_email = jsonObject.getString("nominee_3_email");
                    String nominee_3_mobile = jsonObject.getString("nominee_3_mobile");
                    String nominee_3_address1 = jsonObject.getString("nominee_3_address1");
                    String nominee_3_address2 = jsonObject.getString("nominee_3_address2");
                    String nominee_3_address3 = jsonObject.getString("nominee_3_address3");
                    String nominee_3_city = jsonObject.getString("nominee_3_city");
                    String nominee_3_pin = jsonObject.getString("nominee_3_pin");
                    String nominee_3_country = jsonObject.getString("nominee_3_country");
                    String nominee_names_registration_status_in_soa = jsonObject.getString("nominee_names_registration_status_in_soa");

                    String created_by = jsonObject.getString("created_by");
                    String created_at = jsonObject.getString("created_at");
                    String last_modified_by = jsonObject.getString("last_modified_by");

                    String last_modified_at = jsonObject.getString("last_modified_at");
                    iin_number = client_code;
                    title = "";
                    List<String> nameParts = Arrays.asList(primary_holder_first_name, primary_holder_middle_name, primary_holder_last_name);
                    name = nameParts.stream().filter(part -> part != null && !part.trim().isEmpty()).collect(Collectors.joining(" "));
                    pan = primary_holder_pan;
                    mobile = indian_mobile_no;
                    email = email;
                    subbroker_name = "";
                    street_1 = address_1; // need to clarify
                    street_2 = address_2; // need to clarify
                    street_3 = address_3; // need to clarify
                    city = city;
                    pincode = pincode;
                    state = state;
                    state_code = "";
                    country = country;
                    phone_office = office_phone;
                    phone_residence = resi_phone;

                    father_name = "";
                    gender = gender;
                    date_of_birth = primary_holder_dob_incorporation;
                    marital_status = "";
                    place_of_birth = "";
                    country_of_birth = "";
                    country_birth_code = "";

                    source_of_wealth = "";
                    source_of_wealth_code = "";
                    annual_income = "";
                    annual_income_code = "";
                    occupation = "";
                    occupation_code = occupation_code;
                    networth_amount = "";
                    networth_dob = "";
                    political = "";
                    political_code = "";
                    address_type = "";
                    address_type_desc = "";

                    pan_tax_status_code = tax_status;
                    pan_tax_status_desc = "";
                    holding_nature = holding_nature;
                    holding_nature_desc = "";

                    List<String> guardNameParts = Arrays.asList(guardian_first_name, guardian_middle_name, guardian_last_name);
                    guard_name = nameParts.stream().filter(part -> part != null && !part.trim().isEmpty()).collect(Collectors.joining(" "));
                    guard_pan = guardian_pan;
                    guard_dob = guardian_dob;
                    guard_mobile = "";
                    guard_relationship = guardian_relationship;

                    email_verified = email_declaration_flag;
                    email_authcode = "";
                    mobile_verified = mobile_declaration_flag;
                    mobile_otp = "";
                    bse_customer = "0";
                    bse_active = "0";

                    bank_name = bank_name_1;
                    bank_code = "";
                    bank_branch = bank_branch_1;
                    bank_address = "";
                    bank_address1 = "";
                    bank_address2 = "";
                    bank_address3 = "";
                    bank_account_number = account_no_1;
                    bank_account_holder_name = "";
                    bank_account_type = account_type_1;
                    bank_ifsc_code = ifsc_code_1;
                    bank_micr_code = micr_no_1;
                    default_bank = default_bank_flag_1;


                    nominee_required = "0";

                    otm_flag = "0";
                    otm = "";
                    otm_approved = "0";

                    List<String> joint_holderParts = Arrays.asList(second_holder_first_name, second_holder_middle_name, second_holder_last_name);
                    joint_holder_name = joint_holderParts.stream().filter(part -> part != null && !part.trim().isEmpty()).collect(Collectors.joining(" "));
                    joint_holder_pan = second_holder_pan;
                    joint_holder_dob = second_holder_dob;
                    joint_holder_email = second_holder_email;
                    joint_holder_mobile = second_holder_mobile;

                    List<String> joint_holderParts1 = Arrays.asList(third_holder_first_name, third_holder_middle_name, third_holder_last_name);
                    joint_holder_name1 = joint_holderParts1.stream().filter(part -> part != null && !part.trim().isEmpty()).collect(Collectors.joining(" "));
                    joint_holder_pan1 = third_holder_pan;
                    joint_holder_dob1 = third_holder_dob;
                    joint_holder_email1 = third_holder_email;
                    joint_holder_mobile1 = third_holder_mobile;

                    nri_address1 = foreign_address_1;
                    nri_address2 = foreign_address_2;
                    nri_address3 = foreign_address_3;
                    nri_city = foreign_address_city;
                    nri_state = foreign_address_state;
                    nri_pincode = foreign_address_pincode;
                    nri_country = foreign_address_country;

                    number_of_nominee = "";
                    nominee_type = nominee_1_minor_flag;
                    nominee1_name = nominee_1_name;
                    nominee1_dob = nominee_1_dob;
                    nominee1_address1 = nominee_1_address1;
                    nominee1_address2 = nominee_1_address2;
                    nominee1_address3 = nominee_1_address3;
                    nominee1_pincode = nominee1_pin;
                    nominee1_city = nominee_1_city;
                    nominee1_state = "";
                    nominee1_relation = nominee_1_relationship;
                    nominee1_guard_name = nominee_1_guardian;
                    nominee1_guard_pan = nominee_1_guardian_pan;

                    nominee2_type = nominee2_minor_flag;
                    nominee2_name = nominee_2_name;
                    nominee2_dob = nominee_2_dob;
                    nominee2_relation = nominee_2_relationship;
                    nominee2_percentage = nominee1_applicable;
                    nominee2_guard_name = nominee2_guardian;
                    nominee2_guard_pan = nominee_2_guardian_pan;

                    nominee3_type = nominee_3_minor_flag;
                    nominee3_name = nominee_3_name;
                    nominee3_dob = nominee_3_dob;
                    nominee3_relation = nominee_3_relationship;
                    nominee3_percentage = nominee1_applicable;
                    nominee3_guard_name = nominee_3_guardian;
                    nominee3_guard_pan = nominee_3_guardian_pan;

                    nse_customer = "1";
                    nse_active_user = "YES";
                    // broker_code= "";
                    created_date = "";
                    dob1 = null;

                    nom1_pan = "";
                    nom1_percentage = nominee1_applicable;
                    nom1_country = nominee_1_country;
                    nom1_guard_rel = "";

                    nom2type = nominee2_minor_flag;
                    nom2_guard_rel = "";
                    nom2_pan = nominee_2_id_number;

                    nom3type = nominee_3_minor_flag;
                    nom3_guard_rel = "";
                    nom3_pan = nominee_2_id_number;

                    euin = "";

                    if (!broker_code.isEmpty()) {
                        if (broker_code.equalsIgnoreCase(broker_code1)) {
                            euin = euin1;
                        }
                        if (euin.indexOf(",") != -1) {
                            euin = "";
                        }
                    }

                    Integer nse_active = 0;

                    if (mobile.contains("+91")) {
                        mobile = mobile.replace("+91", "");
                    }

                    if (nse_active_user.equalsIgnoreCase("YES")) {
                        nse_active = 1;
                    } else {
                        nse_active = 0;
                    }

                    name = name.trim();
                    name = name.replaceAll("'", "");

                    bank_account_holder_name = name;

                    if (StringHelper.isNotEmpty(date_of_birth.trim())) {
                        dob1 = df4.parse(date_of_birth);
                        date_of_birth = df2.format(dob1);
                    }

                    if (StringHelper.isNotEmpty(nominee1_dob.trim())) {
                        dob1 = df4.parse(nominee1_dob);
                        nominee1_dob = df2.format(dob1);
                    }
                    if (StringHelper.isNotEmpty(nominee2_dob.trim())) {
                        dob1 = df4.parse(nominee2_dob);
                        nominee2_dob = df2.format(dob1);
                    }
                    if (StringHelper.isNotEmpty(nominee3_dob.trim())) {
                        dob1 = df4.parse(nominee3_dob);
                        nominee3_dob = df2.format(dob1);
                    }
                    if (StringHelper.isNotEmpty(guard_dob.trim())) {
                        Date guard1_dob1 = df4.parse(guard_dob);
                        guard_dob = df2.format(guard1_dob1);
                    }

                    Date date = Calendar.getInstance().getTime();
                    created_date = dateFormat.format(date);


                    street_1 = street_1.replaceAll("'", "");
                    street_1 = street_1.replaceAll("\"", "");
                    street_2 = street_2.replaceAll("'", "");
                    street_2 = street_2.replaceAll("\"", "");
                    street_3 = street_3.replaceAll("'", "");
                    street_3 = street_3.replaceAll("\"", "");
                    city = city.replaceAll("'", "");
                    city = city.replaceAll("\"", "");
                    state = state.replaceAll("'", "");
                    state = state.replaceAll("\"", "");

                    if (state_code.length() > 5) {
                        state_code = "";
                    }

                    bank_code = bank_code.trim();
                    String nse_bank_code = bank_code;

                    NseBank nseBank = bank_list.stream()
                            .filter(x -> x.getBank_code().equalsIgnoreCase(nse_bank_code)).findAny()
                            .orElse(null);
                    if (nseBank != null) {
                        bank_name = nseBank.getBank_name();
                    }

                    if(holding_nature.contentEquals("SINGLE")) {

                        holding_nature_desc = holding_nature;
                        holding_nature = "SI";

                    }else if(holding_nature.contentEquals("JOINT")){
                        holding_nature_desc = holding_nature;
                        holding_nature = "JO";

                    }else if(holding_nature.contains("ANYONE") ||holding_nature.contains("SURVIVOR")) {
                        holding_nature_desc = holding_nature;
                        holding_nature = "AS";
                    }

                    if(occupation_code.contentEquals("BUSINESS")) {
                        occupation = occupation_code;
                        occupation_code = "01";
                    }else if(occupation_code.contentEquals("SERVICES")) {
                        occupation = occupation_code;
                        occupation_code = "02";
                    }else if(occupation_code.contentEquals("PROFESSIONAL")) {
                        occupation = occupation_code;
                        occupation_code = "03";
                    }else if(occupation_code.contentEquals("AGRICULTURE")) {
                        occupation = occupation_code;
                        occupation_code = "04";
                    }else if(occupation_code.contentEquals("RETIRED")) {
                        occupation = occupation_code;
                        occupation_code = "05";
                    }else if(occupation_code.contentEquals("HOUSEWIFE")) {
                        occupation = occupation_code;
                        occupation_code = "06";
                    }else if(occupation_code.contentEquals("STUDENT")) {
                        occupation = occupation_code;
                        occupation_code = "07";
                    }else if(occupation_code.contentEquals("OTHERS")) {
                        occupation = occupation_code;
                        occupation_code = "08";
                    }


                    pan_tax_status_desc = pan_tax_status_code;
                    pan_tax_status_code = NseUtils.getClientTaxStatusCode(pan_tax_status_code);

                    if (!pan.isEmpty()) {

                        List<UserDto> pan_list = userServiceClient.getUserDetailsByPanName(pan,name,client_name,token);

                        if (pan_list.size() > 0) {

                            List<UserDto> user_list = userServiceClient.getActiveUsersByPanName(pan,name,client_name,token);

                            if (user_list.size() > 0) {

                                UserDto obj2 = user_list.get(0);

                                if(obj2.getNse_iin_number().equalsIgnoreCase(iin_number) && obj2.getBroker_code().equalsIgnoreCase(broker_code) && obj2.getName().equalsIgnoreCase(name)){
                                    continue;
                                }

                                UserBseNseDto obj = new UserBseNseDto();
                                obj.setUser_id(obj2.getId());
                                obj.setBroker_code(broker_code);
                                obj.setEuin(euin);
                                obj.setSalutation(title);
                                obj.setName(name);
                                obj.setPan(pan);
                                obj.setDate_of_birth(date_of_birth);
                                obj.setHolding_nature_code(holding_nature);
                                obj.setHolding_nature(holding_nature_desc);
                                obj.setTax_status_code(pan_tax_status_code);
                                obj.setTax_status(pan_tax_status_desc);
                                obj.setOccupation_code(occupation_code);
                                obj.setOccupation(occupation);
                                obj.setFather_name(father_name);
                                obj.setStreet_1(street_1);
                                obj.setStreet_2(street_2);
                                obj.setStreet_3(street_3);
                                obj.setCity(city);
                                obj.setState(state);
                                obj.setState_code(state_code);
                                obj.setPincode(pincode);
                                obj.setCountry(country);
                                obj.setMobile(mobile);
                                obj.setEmail(email);
                                obj.setGender(gender);
                                obj.setPlace_of_birth(place_of_birth);
                                obj.setCountry_of_birth(country_of_birth);
                                obj.setCountry_birth_code(country_birth_code);

                                obj.setNri_address1(nri_address1);
                                obj.setNri_address2(nri_address2);
                                obj.setNri_address3(nri_address3);
                                obj.setNri_city(nri_city);
                                obj.setNri_state(nri_state);
                                obj.setNri_country(nri_country);
                                obj.setNri_pincode(nri_pincode);

                                obj.setBank_name1(bank_name);
                                obj.setBank_code1(bank_code);
                                obj.setBank_account_number1(bank_account_number);
                                obj.setBank_account_type1(bank_account_type);
                                obj.setBank_ifsc_code1(bank_ifsc_code);
                                obj.setBank_micr_code1(bank_micr_code);
                                obj.setBank_account_holder_name1(bank_account_holder_name);
                                obj.setBank_branch1(bank_branch);
                                obj.setBank_address1(bank_address);
                                obj.setDefault_bank1(default_bank);

                                obj.setJoint_holder_name1(joint_holder_name);
                                obj.setJoint_holder_dob1(joint_holder_dob);
                                obj.setJoint_holder_pan1(joint_holder_pan);
                                obj.setJoint_holder_email1(joint_holder_email);
                                obj.setJoint_holder_mobile1(joint_holder_mobile);
                                obj.setJoint_holder_name2(joint_holder_name1);
                                obj.setJoint_holder_dob2(joint_holder_dob1);
                                obj.setJoint_holder_pan2(joint_holder_pan1);
                                obj.setJoint_holder_email2(joint_holder_email1);
                                obj.setJoint_holder_mobile2(joint_holder_mobile1);

                                obj.setNumber_of_nominee(number_of_nominee);
                                obj.setNominee1_type(nominee_type);
                                obj.setNominee1_name(nominee1_name);
                                obj.setNominee1_dob(nominee1_dob);
                                obj.setNominee1_relation(nominee1_relation);
                                obj.setNominee1_address1(nominee1_address1);
                                obj.setNominee1_address2(nominee1_address2);
                                obj.setNominee1_address3(nominee1_address3);
                                obj.setNominee1_city(nominee1_city);
                                obj.setNominee1_pincode(nominee1_pincode);
                                obj.setNominee1_state(nominee1_state);
                                obj.setNominee1_guard_name(nominee1_guard_name);
                                obj.setNominee1_guard_pan(nominee1_guard_pan);

                                obj.setNominee2_name(nominee2_name);
                                obj.setNominee2_type(nominee2_type);
                                obj.setNominee2_dob(nominee2_dob);
                                obj.setNominee2_relation(nominee2_relation);
                                obj.setNominee2_guard_name(nominee2_guard_name);
                                obj.setNominee2_guard_pan(nominee2_guard_pan);
                                obj.setNominee2_percentage(nominee2_percentage);

                                obj.setNominee3_name(nominee3_name);
                                obj.setNominee3_type(nominee3_type);
                                obj.setNominee3_dob(nominee3_dob);
                                obj.setNominee3_relation(nominee3_relation);
                                obj.setNominee3_guard_name(nominee3_guard_name);
                                obj.setNominee3_guard_pan(nominee3_guard_pan);
                                obj.setNominee3_percentage(nominee3_percentage);

                                obj.setGuard_name(guard_name);
                                obj.setGuard_pan(guard_pan);
                                obj.setGuard_dob(guard_dob);
                                obj.setGuard_mobile(guard_mobile);
                                obj.setGuard_relationship(guard_relationship);

                                obj.setNse_customer(1);
                                obj.setNse_iin_number(iin_number);
                                obj.setNse_active(nse_active);
                                obj.setAddress_type(address_type_desc);
                                obj.setAddress_type_code(address_type);
                                obj.setSource_of_wealth(source_of_wealth);
                                obj.setSource_of_wealth_code(source_of_wealth_code);
                                obj.setAnnual_income_code(annual_income_code);
                                obj.setAnnual_income(annual_income);

                                obj.setNetworth_amount(networth_amount);
                                obj.setNetworth_dob(networth_dob);
                                obj.setPolitical(political);
                                obj.setPolitical_code(political_code);
                                obj.setNse_ach1("");
                                obj.setNse_ach_flag1(0);
                                obj.setNse_ach_approved1(0);

                                obj.setClient_name(client_name);
                                obj.setCreated_date(new Date());
                                obj.setRegister_source("IIN Update");
                                obj.setOnline_flag("NSE");

                                obj.setNominee1_pan(nom1_pan);
                                obj.setNominee1_percentage(nom1_percentage);
                                obj.setNominee1_country(nom1_country);
                                obj.setNominee1_guard_relationship(nom1_guard_rel);
                                obj.setNominee2_type(nom2type);
                                obj.setNominee2_guard_relationship(nom2_guard_rel);
                                obj.setNominee2_pan(nom2_pan);
                                obj.setNominee3_type(nom3type);
                                obj.setNominee3_guard_relationship(nom3_guard_rel);
                                obj.setNominee3_pan(nom3_pan);

                                userServiceClient.saveUserBseNseDetail(obj,token);

                            }else{

                                UserDto obj = pan_list.get(0);

                                obj.setBroker_code(broker_code);
                                obj.setEuin(euin);
                                obj.setSalutation(title);
                                obj.setName(name.replaceAll("\\s+", " ").trim());
                                obj.setPan(pan);
                                obj.setDate_of_birth(date_of_birth);
                                obj.setDate_of_birth_greeting(date_of_birth);
                                obj.setHolding_nature_code(holding_nature);
                                obj.setHolding_nature(holding_nature_desc);
                                obj.setTax_status_code(pan_tax_status_code);
                                obj.setTax_status(pan_tax_status_desc);
                                obj.setOccupation_code(occupation_code);
                                obj.setOccupation(occupation);
                                obj.setFather_name(father_name);
                                obj.setStreet_1(street_1);
                                obj.setStreet_2(street_2);
                                obj.setStreet_3(street_3);
                                obj.setCity(city);
                                obj.setState(state);
                                obj.setState_code(state_code);
                                obj.setPincode(pincode);
                                obj.setCountry(country);
                                obj.setMobile(mobile);
                                obj.setEmail(email);
                                obj.setGender(gender);
                                obj.setPlace_of_birth(place_of_birth);
                                obj.setCountry_of_birth(country_of_birth);
                                obj.setCountry_birth_code(country_birth_code);

                                obj.setNri_address1(nri_address1);
                                obj.setNri_address2(nri_address2);
                                obj.setNri_address3(nri_address3);
                                obj.setNri_city(nri_city);
                                obj.setNri_state(nri_state);
                                obj.setNri_country(nri_country);
                                obj.setNri_pincode(nri_pincode);

                                obj.setBank_name1(bank_name);
                                obj.setBank_code1(bank_code);
                                obj.setBank_account_number1(bank_account_number);
                                obj.setBank_account_type1(bank_account_type);
                                obj.setBank_ifsc_code1(bank_ifsc_code);
                                obj.setBank_micr_code1(bank_micr_code);
                                obj.setBank_account_holder_name1(bank_account_holder_name);
                                obj.setBank_branch1(bank_branch);
                                obj.setBank_address1(bank_address);
                                obj.setDefault_bank1(default_bank);

                                obj.setJoint_holder_name1(joint_holder_name);
                                obj.setJoint_holder_dob1(joint_holder_dob);
                                obj.setJoint_holder_pan1(joint_holder_pan);
                                obj.setJoint_holder_email1(joint_holder_email);
                                obj.setJoint_holder_mobile1(joint_holder_mobile);
                                obj.setJoint_holder_name2(joint_holder_name1);
                                obj.setJoint_holder_dob2(joint_holder_dob1);
                                obj.setJoint_holder_pan2(joint_holder_pan1);
                                obj.setJoint_holder_email2(joint_holder_email1);
                                obj.setJoint_holder_mobile2(joint_holder_mobile1);

                                obj.setNumber_of_nominee(number_of_nominee);
                                obj.setNominee1_type(nominee_type);
                                obj.setNominee1_name(nominee1_name);
                                obj.setNominee1_dob(nominee1_dob);
                                obj.setNominee1_relation(nominee1_relation);
                                obj.setNominee1_address1(nominee1_address1);
                                obj.setNominee1_address2(nominee1_address2);
                                obj.setNominee1_address3(nominee1_address3);
                                obj.setNominee1_city(nominee1_city);
                                obj.setNominee1_pincode(nominee1_pincode);
                                obj.setNominee1_state(nominee1_state);
                                obj.setNominee1_guard_name(nominee1_guard_name);
                                obj.setNominee1_guard_pan(nominee1_guard_pan);

                                obj.setNominee2_name(nominee2_name);
                                obj.setNominee2_type(nominee2_type);
                                obj.setNominee2_dob(nominee2_dob);
                                obj.setNominee2_relation(nominee2_relation);
                                obj.setNominee2_guard_name(nominee2_guard_name);
                                obj.setNominee2_guard_pan(nominee2_guard_pan);
                                obj.setNominee2_percentage(nominee2_percentage);

                                obj.setNominee3_name(nominee3_name);
                                obj.setNominee3_type(nominee3_type);
                                obj.setNominee3_dob(nominee3_dob);
                                obj.setNominee3_relation(nominee3_relation);
                                obj.setNominee3_guard_name(nominee3_guard_name);
                                obj.setNominee3_guard_pan(nominee3_guard_pan);
                                obj.setNominee3_percentage(nominee3_percentage);

                                obj.setGuard_name(guard_name);
                                obj.setGuard_pan(guard_pan);
                                obj.setGuard_dob(guard_dob);
                                obj.setGuard_mobile(guard_mobile);
                                obj.setGuard_relationship(guard_relationship);

                                obj.setNse_customer(1);
                                obj.setNse_iin_number(iin_number);
                                obj.setNse_active(nse_active);
                                obj.setAddress_type(address_type_desc);
                                obj.setAddress_type_code(address_type);
                                obj.setSource_of_wealth(source_of_wealth);
                                obj.setSource_of_wealth_code(source_of_wealth_code);
                                obj.setAnnual_income_code(annual_income_code);
                                obj.setAnnual_income(annual_income);

                                obj.setNetworth_amount(networth_amount);
                                obj.setNetworth_dob(networth_dob);
                                obj.setPolitical(political);
                                obj.setPolitical_code(political_code);
                                obj.setNse_ach1("");
                                obj.setNse_ach_flag1(0);
                                obj.setNse_ach_approved1(0);

                                obj.setBse_customer(0);
                                obj.setBse_active(0);
                                obj.setClient_name(client_name);
                                obj.setCreated_date(new Date());
                                obj.setRegister_source("IIN Update");
                                obj.setOnline_flag("NSE");

                                obj.setNominee1_pan(nom1_pan);
                                obj.setNominee1_percentage(nom1_percentage);
                                obj.setNominee1_country(nom1_country);
                                obj.setNominee1_guard_relationship(nom1_guard_rel);
                                obj.setNominee2_type(nom2type);
                                obj.setNominee2_guard_relationship(nom2_guard_rel);
                                obj.setNominee2_pan(nom2_pan);
                                obj.setNominee3_type(nom3type);
                                obj.setNominee3_guard_relationship(nom3_guard_rel);
                                obj.setNominee3_pan(nom3_pan);

                                userServiceClient.saveUser(obj,token);

                            }
                        }else{

                            UserDto obj = new UserDto();

                            obj.setBroker_code(broker_code);
                            obj.setEuin(euin);
                            obj.setSalutation(title);
                            obj.setName(name.replaceAll("\\s+", " ").trim());
                            obj.setPan(pan);
                            obj.setBranch(default_branch);
                            obj.setRm_name(default_rm);
                            obj.setSuper_subbroker_name("");
                            obj.setSubbroker_name("");
                            obj.setDate_of_birth(date_of_birth);
                            obj.setDate_of_birth_greeting(date_of_birth);
                            obj.setHolding_nature_code(holding_nature);
                            obj.setHolding_nature(holding_nature_desc);
                            obj.setTax_status_code(pan_tax_status_code);
                            obj.setTax_status(pan_tax_status_desc);
                            obj.setOccupation_code(occupation_code);
                            obj.setOccupation(occupation);
                            obj.setFather_name(father_name);
                            obj.setStreet_1(street_1);
                            obj.setStreet_2(street_2);
                            obj.setStreet_3(street_3);
                            obj.setCity(city);
                            obj.setState(state);
                            obj.setState_code(state_code);
                            obj.setPincode(pincode);
                            obj.setCountry(country);
                            obj.setMobile(mobile);
                            obj.setEmail(email);
                            obj.setGender(gender);
                            obj.setPlace_of_birth(place_of_birth);
                            obj.setCountry_of_birth(country_of_birth);
                            obj.setCountry_birth_code(country_birth_code);

                            obj.setNri_address1(nri_address1);
                            obj.setNri_address2(nri_address2);
                            obj.setNri_address3(nri_address3);
                            obj.setNri_city(nri_city);
                            obj.setNri_state(nri_state);
                            obj.setNri_country(nri_country);
                            obj.setNri_pincode(nri_pincode);

                            obj.setBank_name1(bank_name);
                            obj.setBank_code1(bank_code);
                            obj.setBank_account_number1(bank_account_number);
                            obj.setBank_account_type1(bank_account_type);
                            obj.setBank_ifsc_code1(bank_ifsc_code);
                            obj.setBank_micr_code1(bank_micr_code);
                            obj.setBank_account_holder_name1(bank_account_holder_name);
                            obj.setBank_branch1(bank_branch);
                            obj.setBank_address1(bank_address);
                            obj.setDefault_bank1(default_bank);

                            obj.setJoint_holder_name1(joint_holder_name);
                            obj.setJoint_holder_dob1(joint_holder_dob);
                            obj.setJoint_holder_pan1(joint_holder_pan);
                            obj.setJoint_holder_email1(joint_holder_email);
                            obj.setJoint_holder_mobile1(joint_holder_mobile);
                            obj.setJoint_holder_name2(joint_holder_name1);
                            obj.setJoint_holder_dob2(joint_holder_dob1);
                            obj.setJoint_holder_pan2(joint_holder_pan1);
                            obj.setJoint_holder_email2(joint_holder_email1);
                            obj.setJoint_holder_mobile2(joint_holder_mobile1);

                            obj.setNumber_of_nominee(number_of_nominee);
                            obj.setNominee1_type(nominee_type);
                            obj.setNominee1_name(nominee1_name);
                            obj.setNominee1_dob(nominee1_dob);
                            obj.setNominee1_relation(nominee1_relation);
                            obj.setNominee1_address1(nominee1_address1);
                            obj.setNominee1_address2(nominee1_address2);
                            obj.setNominee1_address3(nominee1_address3);
                            obj.setNominee1_city(nominee1_city);
                            obj.setNominee1_pincode(nominee1_pincode);
                            obj.setNominee1_state(nominee1_state);
                            obj.setNominee1_guard_name(nominee1_guard_name);
                            obj.setNominee1_guard_pan(nominee1_guard_pan);

                            obj.setNominee2_name(nominee2_name);
                            obj.setNominee2_type(nominee2_type);
                            obj.setNominee2_dob(nominee2_dob);
                            obj.setNominee2_relation(nominee2_relation);
                            obj.setNominee2_guard_name(nominee2_guard_name);
                            obj.setNominee2_guard_pan(nominee2_guard_pan);
                            obj.setNominee2_percentage(nominee2_percentage);

                            obj.setNominee3_name(nominee3_name);
                            obj.setNominee3_type(nominee3_type);
                            obj.setNominee3_dob(nominee3_dob);
                            obj.setNominee3_relation(nominee3_relation);
                            obj.setNominee3_guard_name(nominee3_guard_name);
                            obj.setNominee3_guard_pan(nominee3_guard_pan);
                            obj.setNominee3_percentage(nominee3_percentage);

                            obj.setGuard_name(guard_name);
                            obj.setGuard_pan(guard_pan);
                            obj.setGuard_dob(guard_dob);
                            obj.setGuard_mobile(guard_mobile);
                            obj.setGuard_relationship(guard_relationship);

                            obj.setNse_customer(1);
                            obj.setNse_iin_number(iin_number);
                            obj.setNse_active(nse_active);
                            obj.setAddress_type(address_type_desc);
                            obj.setAddress_type_code(address_type);
                            obj.setSource_of_wealth(source_of_wealth);
                            obj.setSource_of_wealth_code(source_of_wealth_code);
                            obj.setAnnual_income_code(annual_income_code);
                            obj.setAnnual_income(annual_income);

                            obj.setNetworth_amount(networth_amount);
                            obj.setNetworth_dob(networth_dob);
                            obj.setPolitical(political);
                            obj.setPolitical_code(political_code);
                            obj.setNse_ach1("");
                            obj.setNse_ach_flag1(0);
                            obj.setNse_ach_approved1(0);

                            obj.setPhone_office(phone_office);
                            obj.setPhone_residence(phone_residence);
                            obj.setType_id(1);
                            obj.setUser_pass(user_pass);
                            obj.setUser_password(user_password);
                            obj.setActive(1);

                            obj.setBse_customer(0);
                            obj.setBse_active(0);
                            obj.setClient_name(client_name);
                            obj.setCreated_date(new Date());
                            obj.set_purchase_allowed(false);
                            obj.set_redeem_allowed(false);
                            obj.set_switch_allowed(false);
                            obj.set_stp_allowed(false);
                            obj.set_swp_allowed(false);

                            obj.setMf_oneday_change(1);
                            obj.setRegister_source("IIN Update");
                            obj.setOnline_flag("NSE");
                            obj.setOnline_kyc_flag(0);

                            obj.setNominee1_pan(nom1_pan);
                            obj.setNominee1_percentage(nom1_percentage);
                            obj.setNominee1_country(nom1_country);
                            obj.setNominee1_guard_relationship(nom1_guard_rel);
                            obj.setNominee2_type(nom2type);
                            obj.setNominee2_guard_relationship(nom2_guard_rel);
                            obj.setNominee2_pan(nom2_pan);
                            obj.setNominee3_type(nom3type);
                            obj.setNominee3_guard_relationship(nom3_guard_rel);
                            obj.setNominee3_pan(nom3_pan);

                            userServiceClient.saveUser(obj,token);
                        }
                    }else{

                        List<UserDto> name_list = userServiceClient.getUserDetailsByPanName(pan,name,client_name,token);

                        if (name_list.size() > 0) {
                            List<UserDto> user_list = userServiceClient.getActiveUsersByPanName("",name,client_name,token);

                            if (user_list.size() > 0) {

                                UserDto obj2 = user_list.get(0);

                                if(obj2.getNse_iin_number().equalsIgnoreCase(iin_number) && obj2.getBroker_code().equalsIgnoreCase(broker_code) && obj2.getName().equalsIgnoreCase(name)){
                                    continue;
                                }

                                UserBseNseDto obj = new UserBseNseDto();

                                obj.setUser_id(obj2.getId());
                                obj.setBroker_code(broker_code);
                                obj.setEuin(euin);
                                obj.setSalutation(title);
                                obj.setName(name);
                                obj.setPan(pan);
                                obj.setDate_of_birth(date_of_birth);
                                obj.setHolding_nature_code(holding_nature);
                                obj.setHolding_nature(holding_nature_desc);
                                obj.setTax_status_code(pan_tax_status_code);
                                obj.setTax_status(pan_tax_status_desc);
                                obj.setOccupation_code(occupation_code);
                                obj.setOccupation(occupation);
                                obj.setFather_name(father_name);
                                obj.setStreet_1(street_1);
                                obj.setStreet_2(street_2);
                                obj.setStreet_3(street_3);
                                obj.setCity(city);
                                obj.setState(state);
                                obj.setState_code(state_code);
                                obj.setPincode(pincode);
                                obj.setCountry(country);
                                obj.setMobile(mobile);
                                obj.setEmail(email);
                                obj.setGender(gender);
                                obj.setPlace_of_birth(place_of_birth);
                                obj.setCountry_of_birth(country_of_birth);
                                obj.setCountry_birth_code(country_birth_code);

                                obj.setNri_address1(nri_address1);
                                obj.setNri_address2(nri_address2);
                                obj.setNri_address3(nri_address3);
                                obj.setNri_city(nri_city);
                                obj.setNri_state(nri_state);
                                obj.setNri_country(nri_country);
                                obj.setNri_pincode(nri_pincode);

                                obj.setBank_name1(bank_name);
                                obj.setBank_code1(bank_code);
                                obj.setBank_account_number1(bank_account_number);
                                obj.setBank_account_type1(bank_account_type);
                                obj.setBank_ifsc_code1(bank_ifsc_code);
                                obj.setBank_micr_code1(bank_micr_code);
                                obj.setBank_account_holder_name1(bank_account_holder_name);
                                obj.setBank_branch1(bank_branch);
                                obj.setBank_address1(bank_address);
                                obj.setDefault_bank1(default_bank);

                                obj.setJoint_holder_name1(joint_holder_name);
                                obj.setJoint_holder_dob1(joint_holder_dob);
                                obj.setJoint_holder_pan1(joint_holder_pan);
                                obj.setJoint_holder_email1(joint_holder_email);
                                obj.setJoint_holder_mobile1(joint_holder_mobile);
                                obj.setJoint_holder_name2(joint_holder_name1);
                                obj.setJoint_holder_dob2(joint_holder_dob1);
                                obj.setJoint_holder_pan2(joint_holder_pan1);
                                obj.setJoint_holder_email2(joint_holder_email1);
                                obj.setJoint_holder_mobile2(joint_holder_mobile1);

                                obj.setNumber_of_nominee(number_of_nominee);
                                obj.setNominee1_type(nominee_type);
                                obj.setNominee1_name(nominee1_name);
                                obj.setNominee1_dob(nominee1_dob);
                                obj.setNominee1_relation(nominee1_relation);
                                obj.setNominee1_address1(nominee1_address1);
                                obj.setNominee1_address2(nominee1_address2);
                                obj.setNominee1_address3(nominee1_address3);
                                obj.setNominee1_city(nominee1_city);
                                obj.setNominee1_pincode(nominee1_pincode);
                                obj.setNominee1_state(nominee1_state);
                                obj.setNominee1_guard_name(nominee1_guard_name);
                                obj.setNominee1_guard_pan(nominee1_guard_pan);

                                obj.setNominee2_name(nominee2_name);
                                obj.setNominee2_type(nominee2_type);
                                obj.setNominee2_dob(nominee2_dob);
                                obj.setNominee2_relation(nominee2_relation);
                                obj.setNominee2_guard_name(nominee2_guard_name);
                                obj.setNominee2_guard_pan(nominee2_guard_pan);
                                obj.setNominee2_percentage(nominee2_percentage);

                                obj.setNominee3_name(nominee3_name);
                                obj.setNominee3_type(nominee3_type);
                                obj.setNominee3_dob(nominee3_dob);
                                obj.setNominee3_relation(nominee3_relation);
                                obj.setNominee3_guard_name(nominee3_guard_name);
                                obj.setNominee3_guard_pan(nominee3_guard_pan);
                                obj.setNominee3_percentage(nominee3_percentage);

                                obj.setGuard_name(guard_name);
                                obj.setGuard_pan(guard_pan);
                                obj.setGuard_dob(guard_dob);
                                obj.setGuard_mobile(guard_mobile);
                                obj.setGuard_relationship(guard_relationship);

                                obj.setNse_customer(1);
                                obj.setNse_iin_number(iin_number);
                                obj.setNse_active(nse_active);
                                obj.setAddress_type(address_type_desc);
                                obj.setAddress_type_code(address_type);
                                obj.setSource_of_wealth(source_of_wealth);
                                obj.setSource_of_wealth_code(source_of_wealth_code);
                                obj.setAnnual_income_code(annual_income_code);
                                obj.setAnnual_income(annual_income);

                                obj.setNetworth_amount(networth_amount);
                                obj.setNetworth_dob(networth_dob);
                                obj.setPolitical(political);
                                obj.setPolitical_code(political_code);
                                obj.setNse_ach1("");
                                obj.setNse_ach_flag1(0);
                                obj.setNse_ach_approved1(0);

                                obj.setClient_name(client_name);
                                obj.setCreated_date(new Date());
                                obj.setRegister_source("IIN Update");
                                obj.setOnline_flag("NSE");

                                obj.setNominee1_pan(nom1_pan);
                                obj.setNominee1_percentage(nom1_percentage);
                                obj.setNominee1_country(nom1_country);
                                obj.setNominee1_guard_relationship(nom1_guard_rel);
                                obj.setNominee2_type(nom2type);
                                obj.setNominee2_guard_relationship(nom2_guard_rel);
                                obj.setNominee2_pan(nom2_pan);
                                obj.setNominee3_type(nom3type);
                                obj.setNominee3_guard_relationship(nom3_guard_rel);
                                obj.setNominee3_pan(nom3_pan);

                                userServiceClient.saveUserBseNseDetail(obj,token);
                            }else{
                                UserDto obj = name_list.get(0);
                                obj.setBroker_code(broker_code);
                                obj.setEuin(euin);
                                obj.setSalutation(title);
                                obj.setName(name.replaceAll("\\s+", " ").trim());
                                obj.setPan(pan);
                                obj.setDate_of_birth(date_of_birth);
                                obj.setDate_of_birth_greeting(date_of_birth);
                                obj.setHolding_nature_code(holding_nature);
                                obj.setHolding_nature(holding_nature_desc);
                                obj.setTax_status_code(pan_tax_status_code);
                                obj.setTax_status(pan_tax_status_desc);
                                obj.setOccupation_code(occupation_code);
                                obj.setOccupation(occupation);
                                obj.setFather_name(father_name);
                                obj.setStreet_1(street_1);
                                obj.setStreet_2(street_2);
                                obj.setStreet_3(street_3);
                                obj.setCity(city);
                                obj.setState(state);
                                obj.setState_code(state_code);
                                obj.setPincode(pincode);
                                obj.setCountry(country);
                                obj.setMobile(mobile);
                                obj.setEmail(email);
                                obj.setGender(gender);
                                obj.setPlace_of_birth(place_of_birth);
                                obj.setCountry_of_birth(country_of_birth);
                                obj.setCountry_birth_code(country_birth_code);

                                obj.setNri_address1(nri_address1);
                                obj.setNri_address2(nri_address2);
                                obj.setNri_address3(nri_address3);
                                obj.setNri_city(nri_city);
                                obj.setNri_state(nri_state);
                                obj.setNri_country(nri_country);
                                obj.setNri_pincode(nri_pincode);

                                obj.setBank_name1(bank_name);
                                obj.setBank_code1(bank_code);
                                obj.setBank_account_number1(bank_account_number);
                                obj.setBank_account_type1(bank_account_type);
                                obj.setBank_ifsc_code1(bank_ifsc_code);
                                obj.setBank_micr_code1(bank_micr_code);
                                obj.setBank_account_holder_name1(bank_account_holder_name);
                                obj.setBank_branch1(bank_branch);
                                obj.setBank_address1(bank_address);
                                obj.setDefault_bank1(default_bank);

                                obj.setJoint_holder_name1(joint_holder_name);
                                obj.setJoint_holder_dob1(joint_holder_dob);
                                obj.setJoint_holder_pan1(joint_holder_pan);
                                obj.setJoint_holder_email1(joint_holder_email);
                                obj.setJoint_holder_mobile1(joint_holder_mobile);
                                obj.setJoint_holder_name2(joint_holder_name1);
                                obj.setJoint_holder_dob2(joint_holder_dob1);
                                obj.setJoint_holder_pan2(joint_holder_pan1);
                                obj.setJoint_holder_email2(joint_holder_email1);
                                obj.setJoint_holder_mobile2(joint_holder_mobile1);

                                obj.setNumber_of_nominee(number_of_nominee);
                                obj.setNominee1_type(nominee_type);
                                obj.setNominee1_name(nominee1_name);
                                obj.setNominee1_dob(nominee1_dob);
                                obj.setNominee1_relation(nominee1_relation);
                                obj.setNominee1_address1(nominee1_address1);
                                obj.setNominee1_address2(nominee1_address2);
                                obj.setNominee1_address3(nominee1_address3);
                                obj.setNominee1_city(nominee1_city);
                                obj.setNominee1_pincode(nominee1_pincode);
                                obj.setNominee1_state(nominee1_state);
                                obj.setNominee1_guard_name(nominee1_guard_name);
                                obj.setNominee1_guard_pan(nominee1_guard_pan);

                                obj.setNominee2_name(nominee2_name);
                                obj.setNominee2_type(nominee2_type);
                                obj.setNominee2_dob(nominee2_dob);
                                obj.setNominee2_relation(nominee2_relation);
                                obj.setNominee2_guard_name(nominee2_guard_name);
                                obj.setNominee2_guard_pan(nominee2_guard_pan);
                                obj.setNominee2_percentage(nominee2_percentage);

                                obj.setNominee3_name(nominee3_name);
                                obj.setNominee3_type(nominee3_type);
                                obj.setNominee3_dob(nominee3_dob);
                                obj.setNominee3_relation(nominee3_relation);
                                obj.setNominee3_guard_name(nominee3_guard_name);
                                obj.setNominee3_guard_pan(nominee3_guard_pan);
                                obj.setNominee3_percentage(nominee3_percentage);

                                obj.setGuard_name(guard_name);
                                obj.setGuard_pan(guard_pan);
                                obj.setGuard_dob(guard_dob);
                                obj.setGuard_mobile(guard_mobile);
                                obj.setGuard_relationship(guard_relationship);

                                obj.setNse_customer(1);
                                obj.setNse_iin_number(iin_number);
                                obj.setNse_active(nse_active);
                                obj.setAddress_type(address_type_desc);
                                obj.setAddress_type_code(address_type);
                                obj.setSource_of_wealth(source_of_wealth);
                                obj.setSource_of_wealth_code(source_of_wealth_code);
                                obj.setAnnual_income_code(annual_income_code);
                                obj.setAnnual_income(annual_income);

                                obj.setNetworth_amount(networth_amount);
                                obj.setNetworth_dob(networth_dob);
                                obj.setPolitical(political);
                                obj.setPolitical_code(political_code);
                                obj.setNse_ach1("");
                                obj.setNse_ach_flag1(0);
                                obj.setNse_ach_approved1(0);

                                obj.setBse_customer(0);
                                obj.setBse_active(0);
                                obj.setClient_name(client_name);
                                // obj.setCreated_date(new Date());
                                obj.setRegister_source("IIN Update");
                                obj.setOnline_flag("NSE");

                                obj.setNominee1_pan(nom1_pan);
                                obj.setNominee1_percentage(nom1_percentage);
                                obj.setNominee1_country(nom1_country);
                                obj.setNominee1_guard_relationship(nom1_guard_rel);
                                obj.setNominee2_type(nom2type);
                                obj.setNominee2_guard_relationship(nom2_guard_rel);
                                obj.setNominee2_pan(nom2_pan);
                                obj.setNominee3_type(nom3type);
                                obj.setNominee3_guard_relationship(nom3_guard_rel);
                                obj.setNominee3_pan(nom3_pan);

                                userServiceClient.saveUser(obj,token);
                            }
                        }else{

                            UserDto obj = new UserDto();

                            obj.setBroker_code(broker_code);
                            obj.setEuin(euin);
                            obj.setSalutation(title);
                            obj.setName(name.replaceAll("\\s+", " ").trim());
                            obj.setPan(pan);
                            obj.setBranch(default_branch);
                            obj.setRm_name(default_rm);
                            obj.setSuper_subbroker_name("");
                            obj.setSubbroker_name("");
                            obj.setDate_of_birth(date_of_birth);
                            obj.setDate_of_birth_greeting(date_of_birth);
                            obj.setHolding_nature_code(holding_nature);
                            obj.setHolding_nature(holding_nature_desc);
                            obj.setTax_status_code(pan_tax_status_code);
                            obj.setTax_status(pan_tax_status_desc);
                            obj.setOccupation_code(occupation_code);
                            obj.setOccupation(occupation);
                            obj.setFather_name(father_name);
                            obj.setStreet_1(street_1);
                            obj.setStreet_2(street_2);
                            obj.setStreet_3(street_3);
                            obj.setCity(city);
                            obj.setState(state);
                            obj.setState_code(state_code);
                            obj.setPincode(pincode);
                            obj.setCountry(country);
                            obj.setMobile(mobile);
                            obj.setEmail(email);
                            obj.setGender(gender);
                            obj.setPlace_of_birth(place_of_birth);
                            obj.setCountry_of_birth(country_of_birth);
                            obj.setCountry_birth_code(country_birth_code);

                            obj.setNri_address1(nri_address1);
                            obj.setNri_address2(nri_address2);
                            obj.setNri_address3(nri_address3);
                            obj.setNri_city(nri_city);
                            obj.setNri_state(nri_state);
                            obj.setNri_country(nri_country);
                            obj.setNri_pincode(nri_pincode);

                            obj.setBank_name1(bank_name);
                            obj.setBank_code1(bank_code);
                            obj.setBank_account_number1(bank_account_number);
                            obj.setBank_account_type1(bank_account_type);
                            obj.setBank_ifsc_code1(bank_ifsc_code);
                            obj.setBank_micr_code1(bank_micr_code);
                            obj.setBank_account_holder_name1(bank_account_holder_name);
                            obj.setBank_branch1(bank_branch);
                            obj.setBank_address1(bank_address);
                            obj.setDefault_bank1(default_bank);

                            obj.setJoint_holder_name1(joint_holder_name);
                            obj.setJoint_holder_dob1(joint_holder_dob);
                            obj.setJoint_holder_pan1(joint_holder_pan);
                            obj.setJoint_holder_email1(joint_holder_email);
                            obj.setJoint_holder_mobile1(joint_holder_mobile);
                            obj.setJoint_holder_name2(joint_holder_name1);
                            obj.setJoint_holder_dob2(joint_holder_dob1);
                            obj.setJoint_holder_pan2(joint_holder_pan1);
                            obj.setJoint_holder_email2(joint_holder_email1);
                            obj.setJoint_holder_mobile2(joint_holder_mobile1);

                            obj.setNumber_of_nominee(number_of_nominee);
                            obj.setNominee1_type(nominee_type);
                            obj.setNominee1_name(nominee1_name);
                            obj.setNominee1_dob(nominee1_dob);
                            obj.setNominee1_relation(nominee1_relation);
                            obj.setNominee1_address1(nominee1_address1);
                            obj.setNominee1_address2(nominee1_address2);
                            obj.setNominee1_address3(nominee1_address3);
                            obj.setNominee1_city(nominee1_city);
                            obj.setNominee1_pincode(nominee1_pincode);
                            obj.setNominee1_state(nominee1_state);
                            obj.setNominee1_guard_name(nominee1_guard_name);
                            obj.setNominee1_guard_pan(nominee1_guard_pan);

                            obj.setNominee2_name(nominee2_name);
                            obj.setNominee2_type(nominee2_type);
                            obj.setNominee2_dob(nominee2_dob);
                            obj.setNominee2_relation(nominee2_relation);
                            obj.setNominee2_guard_name(nominee2_guard_name);
                            obj.setNominee2_guard_pan(nominee2_guard_pan);
                            obj.setNominee2_percentage(nominee2_percentage);

                            obj.setNominee3_name(nominee3_name);
                            obj.setNominee3_type(nominee3_type);
                            obj.setNominee3_dob(nominee3_dob);
                            obj.setNominee3_relation(nominee3_relation);
                            obj.setNominee3_guard_name(nominee3_guard_name);
                            obj.setNominee3_guard_pan(nominee3_guard_pan);
                            obj.setNominee3_percentage(nominee3_percentage);

                            obj.setGuard_name(guard_name);
                            obj.setGuard_pan(guard_pan);
                            obj.setGuard_dob(guard_dob);
                            obj.setGuard_mobile(guard_mobile);
                            obj.setGuard_relationship(guard_relationship);

                            obj.setNse_customer(1);
                            obj.setNse_iin_number(iin_number);
                            obj.setNse_active(nse_active);
                            obj.setAddress_type(address_type_desc);
                            obj.setAddress_type_code(address_type);
                            obj.setSource_of_wealth(source_of_wealth);
                            obj.setSource_of_wealth_code(source_of_wealth_code);
                            obj.setAnnual_income_code(annual_income_code);
                            obj.setAnnual_income(annual_income);

                            obj.setNetworth_amount(networth_amount);
                            obj.setNetworth_dob(networth_dob);
                            obj.setPolitical(political);
                            obj.setPolitical_code(political_code);
                            obj.setNse_ach1("");
                            obj.setNse_ach_flag1(0);
                            obj.setNse_ach_approved1(0);

                            obj.setPhone_office(phone_office);
                            obj.setPhone_residence(phone_residence);
                            obj.setType_id(1);
                            obj.setUser_pass(user_pass);
                            obj.setUser_password(user_password);
                            obj.setActive(1);

                            obj.setBse_customer(0);
                            obj.setBse_active(0);
                            obj.setClient_name(client_name);
                            obj.setCreated_date(new Date());

                            obj.set_purchase_allowed(false);
                            obj.set_redeem_allowed(false);
                            obj.set_switch_allowed(false);
                            obj.set_stp_allowed(false);
                            obj.set_swp_allowed(false);

                            obj.setMf_oneday_change(1);
                            obj.setRegister_source("IIN Update");
                            obj.setOnline_flag("NSE");
                            obj.setOnline_kyc_flag(0);

                            obj.setNominee1_pan(nom1_pan);
                            obj.setNominee1_percentage(nom1_percentage);
                            obj.setNominee1_country(nom1_country);
                            obj.setNominee1_guard_relationship(nom1_guard_rel);
                            obj.setNominee2_type(nom2type);
                            obj.setNominee2_guard_relationship(nom2_guard_rel);
                            obj.setNominee2_pan(nom2_pan);
                            obj.setNominee3_type(nom3type);
                            obj.setNominee3_guard_relationship(nom3_guard_rel);
                            obj.setNominee3_pan(nom3_pan);

                            userServiceClient.saveUser(obj,token);
                        }
                    }
                }

            } catch (Exception ex){
                ex.printStackTrace();
            }

        } catch (Exception ex)
        {
            ex.printStackTrace();
        }

    }


    //getFolioNumberBySchemeCode

    public List<String> getFolioNumberBySchemeCode(String client_name, Integer user_id, String scheme_code,String scheme_name,
                                                        String holding_nature_code, String tax_status_code,
                                                        String joint_holder_pan1, String joint_holder_pan2,@RequestHeader("Authorization") String token) {
        System.out.println("Fetching folio numbers for AMC: " + scheme_name + ", Client: " + client_name + ", User ID: " + user_id);

        List<String> list = null;

        try {
            String amc_name = "";
            String amc_code = "";

            List<NseOnlineSchemeMaster> scheme_list = null;

            scheme_list = nseOnlineSchemeMasterRepository.findBySchemeCodeAndSchemeName(scheme_code,scheme_name);

            if(scheme_list != null && scheme_list.size() > 0){
                amc_name = scheme_list.get(0).getAmcName();
                amc_code = scheme_list.get(0).getAmcCode();
            }

            String rta_name = NseUtils.getRTAName(amc_code);

            list = new ArrayList<String>();

            if (StringHelper.isNotEmpty(rta_name) && rta_name.equalsIgnoreCase("CAMS")) {

                List<InvestorMasterCamsDto> camsList = null;
                try {
                    camsList = userServiceClient.getinvestorMasterCams(user_id, client_name, amc_code, token);
                    System.out.println("CAMS List: " + camsList);
                } catch (FeignException e) {
                    String response = e.contentUTF8();
                    if (response.contains("No schemes found")) {
                        System.out.println("No CAMS schemes found, skipping to next method.");
                        camsList = null;
                    } else {
                        System.out.println("Unexpected error from CAMS: " + response);
                        throw e;
                    }
                }


                System.out.println("CAMS List: " + camsList);
                if (camsList != null && !camsList.isEmpty()) {
                    for (InvestorMasterCamsDto camsScheme : camsList) {
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
                                    list.add(camsScheme.getFoliochk());
                                }
                            }else if(holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES"))
                            {
                                if(holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES"))
                                {
                                    if(joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan))
                                    {
                                        list.add(camsScheme.getFoliochk());
                                    }
                                }
                            }else if(holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO"))
                            {
                                if(joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan))
                                {
                                    list.add(camsScheme.getFoliochk());
                                }
                            }else
                            {

                            }
                        }else if(tax_status_code.equalsIgnoreCase("11") || tax_status_code.equalsIgnoreCase("21"))
                        {
                            if(tax_status_code.equalsIgnoreCase("11") && bank_acc_type.equalsIgnoreCase("NRO"))
                            {
                                if(holding_nature_code.equalsIgnoreCase("SI"))
                                {
                                    if(holding.equalsIgnoreCase("SI"))
                                    {
                                        list.add(camsScheme.getFoliochk());
                                    }
                                }else if(holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES"))
                                {
                                    if(holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES"))
                                    {
                                        if(joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan))
                                        {
                                            list.add(camsScheme.getFoliochk());
                                        }
                                    }
                                }else if(holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO"))
                                {
                                    if(joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan))
                                    {
                                        list.add(camsScheme.getFoliochk());
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
                                        list.add(camsScheme.getFoliochk());
                                    }
                                }else if(holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES"))
                                {
                                    if(holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES"))
                                    {
                                        if(joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan))
                                        {
                                            list.add(camsScheme.getFoliochk());
                                        }
                                    }
                                }else if(holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO"))
                                {
                                    if(joint_holder_pan1.equalsIgnoreCase(joint1_pan) && joint_holder_pan2.equalsIgnoreCase(joint2_pan))
                                    {
                                        list.add(camsScheme.getFoliochk());
                                    }
                                }else
                                {

                                }
                            }else
                            {

                            }

                        }else
                        {
                            list.add(camsScheme.getFoliochk());
                        }
                    }
                }
            }

            System.out.println("rta_name: " + rta_name);

            System.out.println("amc_code: " + amc_code);

            if (StringHelper.isNotEmpty(rta_name) && rta_name.equalsIgnoreCase("Karvy")) {

                List<InvestorMasterKarvyDto> karvyList = userServiceClient.getinvestorMasterKarvy(user_id, client_name, amc_code, token);
                System.out.println("karvyList =" + karvyList);

                if (karvyList != null && !karvyList.isEmpty()) {
                    for (InvestorMasterKarvyDto karvyScheme : karvyList) {
                        String holding = karvyScheme.getMode_of_holding();
                        String pan2 = karvyScheme.getPan2();
                        String pan3 = karvyScheme.getPan3();
                        String bank_acc_type = karvyScheme.getAccount_type();
                        if(holding == null){holding = "";}
                        if(pan2 == null){pan2 = "";}
                        if(pan3 == null){pan3 = "";}

                        if (holding == null) holding = "";
                        if (pan2 == null) pan2 = "";
                        if (pan3 == null) pan3 = "";
                        if (bank_acc_type == null) bank_acc_type = "";

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
                                    list.add(karvyScheme.getFolio());
                                }
                            }else if(holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES"))
                            {
                                if(holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES"))
                                {
                                    if(joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3))
                                    {
                                        list.add(karvyScheme.getFolio());
                                    }
                                }
                            }else if(holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO"))
                            {
                                if(joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3))
                                {
                                    list.add(karvyScheme.getFolio());
                                }
                            }else
                            {

                            }
                        }else if(tax_status_code.equalsIgnoreCase("11") || tax_status_code.equalsIgnoreCase("21"))
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
                                        list.add(karvyScheme.getFolio());
                                    }
                                }else if(holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES"))
                                {
                                    if(holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES"))
                                    {
                                        if(joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3))
                                        {
                                            list.add(karvyScheme.getFolio());
                                        }
                                    }
                                }else if(holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO"))
                                {
                                    if(joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3))
                                    {
                                        list.add(karvyScheme.getFolio());
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
                                        list.add(karvyScheme.getFolio());
                                    }
                                }else if(holding_nature_code.equalsIgnoreCase("AS") || holding_nature_code.equalsIgnoreCase("ES"))
                                {
                                    if(holding.equalsIgnoreCase("AS") || holding.equalsIgnoreCase("ES"))
                                    {
                                        if(joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3))
                                        {
                                            list.add(karvyScheme.getFolio());
                                        }
                                    }
                                }else if(holding_nature_code.equalsIgnoreCase("JO") && holding.equalsIgnoreCase("JO"))
                                {
                                    if(joint_holder_pan1.equalsIgnoreCase(pan2) && joint_holder_pan2.equalsIgnoreCase(pan3))
                                    {
                                        list.add(karvyScheme.getFolio());
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
                            list.add(karvyScheme.getFolio());
                        }
                    }
                }
            }

            list = new ArrayList<String>(new LinkedHashSet<String>(list));

        } catch (Exception ex) {
            System.err.println("Error fetching folio numbers: " + ex.getMessage());
        }

        return list;
    }


    //getSchemeHoldings

    public List<InvestorSchemeWisePortfolioResponse> getSchemeHoldings(String client_name, Integer userid,UserDto users,String brokercode,@RequestHeader("Authorization") String token)
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
        Double totalUnits = 0.0;
        Double latestNav = 0.0;
        Double totalCurrentValue = 0.0;
        String broker_code = "";
        String euin = "";
        String amc_name = "";
        String amc_code = "";
        String scheme_short_name = "";

        String userTaxStatusCode = users.getTax_status_code();
        String userTaxStatus = users.getTax_status();
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
            List<InvestorMasterCamsDto> camsList  = null;

            List<InvestorMasterKarvyDto> karvyList  = null;

            camsList  = userServiceClient.getByCamsUserIdAndClientName(userid,client_name,token);

            karvyList  = userServiceClient.getByKarvyUserIdAndClientName(userid,client_name,token);

            for (UsersPortfolioSchemewiseDto portfolio : list)
            {
                broker_code = "";
                euin = "";
                registrar = "";
                scheme_code = "";
                folio_no = "";
                scheme_name = "";
                company = "";
                totalUnits = 0.0;
                latestNav = 0.0;
                totalCurrentValue = 0.0;
                amc_name = "";
                amc_code = "";

                scheme_name = portfolio.getScheme_name();
                scheme_short_name = portfolio.getScheme_amfi_short_name();
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

                scheme = new InvestorSchemeWisePortfolioResponse();
                scheme.setScheme(scheme_name);
                scheme.setScheme_amfi_short_name(scheme_short_name);
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

                if(StringHelper.isNotEmpty(registrar) && registrar.equalsIgnoreCase("CAMS"))
                {
                    InvestorMasterCamsDto camsScheme = null;
                    if(camsList != null && camsList.size() > 0)
                    {
                        String folio_no1 = folio_no;
                        String scheme_code1 = scheme_code;
                        camsScheme = camsList.stream().filter(x -> x.getFoliochk().equalsIgnoreCase(folio_no1) && x.getProduct().equalsIgnoreCase(scheme_code1)).findAny().orElse(null);
                    }

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
                        //if(dp_id == null || dp_id.equalsIgnoreCase("NOT PROVIDED")){dp_id = "";}

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
                        if(bank_acc_type.equalsIgnoreCase("NRE")) {
                            scheme.setTax_status_code("21");
                        }else if(bank_acc_type.equalsIgnoreCase("NRO")){
                            scheme.setTax_status_code("11");
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
                        if(bank_acc_type.equalsIgnoreCase("NRE")) {
                            scheme.setTax_status_code("21");
                        }else if(bank_acc_type.equalsIgnoreCase("NRO")){
                            scheme.setTax_status_code("11");
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

                System.out.println("brokercode = " + brokercode);

                System.out.println("userbrokercode = " + userBrokerCode);
                System.out.println("brokercode = " + scheme.getBroker_code());
                System.out.println("tax_status = " + scheme.getTax_status());
                System.out.println("userTaxStatus = " + userTaxStatus);

                System.out.println("tax_status_code = " + scheme.getTax_status_code());
                System.out.println("userTaxStatusCode = " + userTaxStatusCode);

                System.out.println("holding_nature = " + scheme.getHolding_nature());
                System.out.println("userHoldingNature = " + userHoldingNature);
                System.out.println("joint1_pan = " + scheme.getJoint1_pan());
                System.out.println("jointHolderpan1 = " + jointHolderpan1);
                System.out.println("joint2_pan = " + scheme.getJoint2_pan());
                System.out.println("jointHolderpan2 = " + jointHolderpan2);

                if(brokercode.isEmpty())
                {
                    master_list.add(scheme);
                }else
                {
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
                        if(scheme.getBroker_code().equalsIgnoreCase(userBrokerCode) && (scheme.getHolding_nature().equalsIgnoreCase(userHoldingNature) || ((scheme.getHolding_nature().equalsIgnoreCase("AS") && userHoldingNature.equalsIgnoreCase("ES")) || (scheme.getHolding_nature().equalsIgnoreCase("ES") && userHoldingNature.equalsIgnoreCase("AS"))))
//                                && scheme.getJoint1_pan().equalsIgnoreCase(jointHolderpan1) && scheme.getJoint2_pan().equalsIgnoreCase(jointHolderpan2)
                        )
                        {
                            System.out.println("entered second");
                            master_list.add(scheme);
                        }
                    }
                    else
                    {
                        if(scheme.getBroker_code().equalsIgnoreCase(userBrokerCode) && (scheme.getHolding_nature().equalsIgnoreCase(userHoldingNature) || ((scheme.getHolding_nature().equalsIgnoreCase("AS") && userHoldingNature.equalsIgnoreCase("ES")) || (scheme.getHolding_nature().equalsIgnoreCase("ES") && userHoldingNature.equalsIgnoreCase("AS"))))
//                                && scheme.getJoint1_pan().equalsIgnoreCase(jointHolderpan1) && scheme.getJoint2_pan().equalsIgnoreCase(jointHolderpan2)
                        )
                        {
                            System.out.println("entered");
                            master_list.add(scheme);
                        }
                    }
                }
            }
        }
        if (master_list.size() > 0)
        {
            Collections.sort(master_list, new Comparator<InvestorSchemeWisePortfolioResponse>()
            {
                @Override
                public int compare(final InvestorSchemeWisePortfolioResponse object1, final InvestorSchemeWisePortfolioResponse object2) {
                    return object1.getScheme().compareTo(object2.getScheme());
                }
            });
        }
    }
        catch (Exception ex)
        {
        System.err.println("Error fetching folio numbers: " + ex.getMessage());
    }
        return master_list;

        }


    public InvestorPortfolioResponse getInvestorPortfolioWithLiveHoldings(Integer user_id, String client_name,@RequestHeader("Authorization") String token)
    {
        Calendar cal = null;
        InvestorPortfolioResponse investorPortfolioResponse = null;

        List<XirrResponse> final_cagr_list = new ArrayList<XirrResponse>();
        List<XirrResponse> xirr_list = null;
        XirrResponse xirrRes = null;

        List<InvestorTransactionCamsDto> cams_scheme_list = null;
        List<InvestorTransactionKarvyDto> karvy_scheme_list = null;
        List<PortfolioTransactionsDto> portfolio_scheme_list = null;

        List<AmfiLatestNavDto> latestNavList = null;

        InvestorSchemeWisePortfolioResponse investorSchemeWisePortfolioResponse = null;
        List<InvestorSchemeWisePortfolioResponse> investorSchemeWisePortfolioResponseList = null;

        InvestorSchemeWiseTransactionResponse investorSchemeWiseTransactionResponse = null;
        List<InvestorSchemeWiseTransactionResponse> investorSchemeWiseTransactionResponseList = null;

        List<InvestorTransactionCamsDto> camsSchemeWiseInvestorTransactions = null;
        List<InvestorTransactionKarvyDto> karvySchemeWiseInvestorTransactions = null;
        List<PortfolioTransactionsDto> portfolioSchemeWiseInvestorTransactions = null;

        DecimalFormat cost_dcf = new DecimalFormat("0.00");
        DecimalFormat unit_decimal = new DecimalFormat("0.0000");
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf2 = new SimpleDateFormat("dd-MM-yyyy");

        // CAMS Data Processing
        List<String> camsPositiveTransactionArrayList = new ArrayList<String>();
        List<String> camsNegativeTransactionArrayList = new ArrayList<String>();
        List<String> camsNeutralTransactionArrayList = new ArrayList<String>();

        // Karvy Data Processing
        List<String> karvyPositiveTransactionArrayList = new ArrayList<String>();
        List<String> karvyNegativeTransactionArrayList = new ArrayList<String>();
        List<String> karvyNeutralTransactionArrayList = new ArrayList<String>();

        // Portfolio Data Processing
        List<String> mf_manualPositiveTransactionArrayList = new ArrayList<String>();
        List<String> mf_manualNegativeTransactionArrayList = new ArrayList<String>();
        List<String> mf_manualNeutralTransactionArrayList = new ArrayList<String>();

        List<String> karvy_neutral_dividend_transaction_type_list = NseUtils.KarvyNeutralDividendTransactionType();

        try
        {
            // Loop the scheme and get schemewise portfolio details
            investorPortfolioResponse = new InvestorPortfolioResponse();
            investorSchemeWisePortfolioResponseList = new ArrayList<InvestorSchemeWisePortfolioResponse>();

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
                        camsNeutralTransactionArrayList = Arrays.asList(neutralTransactionList);
                    }
                    if (registrar.equalsIgnoreCase("karvy")) {
                        karvyPositiveTransactionArrayList = Arrays.asList(positiveTransactionList);
                        karvyNegativeTransactionArrayList = Arrays.asList(negativeTransactionList);
                        karvyNeutralTransactionArrayList = Arrays.asList(neutralTransactionList);
                    }
                    if (registrar.equalsIgnoreCase("mf_manual")) {
                        mf_manualPositiveTransactionArrayList = Arrays.asList(positiveTransactionList);
                        mf_manualNegativeTransactionArrayList = Arrays.asList(negativeTransactionList);
                        mf_manualNeutralTransactionArrayList = Arrays.asList(neutralTransactionList);
                    }
                }
            }

            // Get investor details

            UserDto users_list = userServiceClient.getUserByClientNameAndId(user_id,client_name,token);

//            if (users_list.size() > 0) {
                UserDto user = users_list;
                investorPortfolioResponse.setId(user.getId());
                investorPortfolioResponse.setInvestorName(user.getName());
                investorPortfolioResponse.setInvestorPan(user.getPan());
                investorPortfolioResponse.setBse_client_code(user.getBse_client_code());
                investorPortfolioResponse.setAddress1(user.getStreet_1());
                investorPortfolioResponse.setAddress2(user.getStreet_2());
                investorPortfolioResponse.setAddress3(user.getStreet_3());
                investorPortfolioResponse.setCity(user.getCity());
                investorPortfolioResponse.setPincode(user.getPincode());
                investorPortfolioResponse.setState(user.getState());
                investorPortfolioResponse.setGuardian_pan(user.getGuard_pan());
                investorPortfolioResponse.setPhone_office(user.getPhone_office());
                investorPortfolioResponse.setPhone_residence(user.getPhone_residence());
                investorPortfolioResponse.setEmail(user.getEmail());
                investorPortfolioResponse.setDate_of_birth(user.getDate_of_birth());
                investorPortfolioResponse.setMobile(user.getMobile());
                investorPortfolioResponse.setOccupation(user.getOccupation());
                investorPortfolioResponse.setSalutation(user.getSalutation());
                if(user.getBse_active() != null)
                {
                    investorPortfolioResponse.setBse_active(user.getBse_active());
                }
                if(user.getBse_client_code() != null)
                {
                    investorPortfolioResponse.setBse_client_code(user.getBse_client_code());
                }
                if(user.getNse_active() != null)
                {
                    investorPortfolioResponse.setNse_active(user.getNse_active());
                }
                if(user.getNse_iin_number() != null)
                {
                    investorPortfolioResponse.setNse_iin_number(user.getNse_iin_number());
                }
//            }

            // Get distinct schemes of the transaction

            cams_scheme_list = userServiceClient.getGroupedByProdcodeAndFolioNo(user_id,client_name,token);

            for (int s = 0; s < cams_scheme_list.size(); s++)
            {
                camsSchemeWiseInvestorTransactions = userServiceClient.getByFolioNoAndProdcodeAndUserIdAndClientName(user_id,client_name,cams_scheme_list.get(s).getFolio_no(),cams_scheme_list.get(s).getProdcode(),token);

                if(camsSchemeWiseInvestorTransactions.size() > 0)
                {
                    camsSchemeWiseInvestorTransactions = NseUtils.removeCamsMinusTransaction(camsSchemeWiseInvestorTransactions);
                }

                if(camsSchemeWiseInvestorTransactions.size() == 0)
                {
                    continue;
                }

                investorSchemeWisePortfolioResponse = new InvestorSchemeWisePortfolioResponse();
                investorSchemeWiseTransactionResponseList = new ArrayList<InvestorSchemeWiseTransactionResponse>();

                xirr_list = new ArrayList<XirrResponse>();
                xirrRes = null;
                List<Double> check_units_array = new ArrayList<Double>();
                Date last_tran_date = null;
                Double last_tran_nav = 0.0;
                Date traddate_prev = null;
                Double units_prev = 0.0;
                Date traddate_next = null;
                Double units_next = 0.0;

                for (int i = 0; i < camsSchemeWiseInvestorTransactions.size(); i++)
                {
                    String trxn_type_ = camsSchemeWiseInvestorTransactions.get(i).getTrxn_type_().trim();
                    String scheme = camsSchemeWiseInvestorTransactions.get(i).getScheme().trim();
                    String prodcode = camsSchemeWiseInvestorTransactions.get(i).getProdcode().trim();
                    Date traddate = camsSchemeWiseInvestorTransactions.get(i).getTraddate();
                    Double price = camsSchemeWiseInvestorTransactions.get(i).getPurprice();
                    Double units = camsSchemeWiseInvestorTransactions.get(i).getUnits();
                    Double amount = camsSchemeWiseInvestorTransactions.get(i).getAmount();
                    last_tran_date = traddate;
                    last_tran_nav = price;

                    investorSchemeWiseTransactionResponse = new InvestorSchemeWiseTransactionResponse();
                    investorSchemeWiseTransactionResponse.setTRADDATE(traddate);
                    investorSchemeWiseTransactionResponse.setPURPRICE(price);
                    investorSchemeWiseTransactionResponse.setUNITS(units);
                    investorSchemeWiseTransactionResponse.setTOTAL_TAX(camsSchemeWiseInvestorTransactions.get(i).getTotal_tax());
                    investorSchemeWiseTransactionResponse.setTRXN_TYPE_(trxn_type_);
                    investorSchemeWiseTransactionResponse.setAMOUNT(amount);
                    //investorSchemeWiseTransactionResponseList.add(investorSchemeWiseTransactionResponse);

                    if (i == 0)
                    {
                        investorSchemeWisePortfolioResponse.setScheme(scheme);
                        investorSchemeWisePortfolioResponse.setScheme_amfi_short_name(scheme);
                        investorSchemeWisePortfolioResponse.setScheme_code(prodcode);
                        investorSchemeWisePortfolioResponse.setFoliono(camsSchemeWiseInvestorTransactions.get(i).getFolio_no());
                        investorSchemeWisePortfolioResponse.setAmc_code(camsSchemeWiseInvestorTransactions.get(i).getAmc_code());
                        investorSchemeWisePortfolioResponse.setInvestmentStartNav(price);
                        investorSchemeWisePortfolioResponse.setInvestmentStartDate(traddate);
                        investorSchemeWisePortfolioResponse.setInvestmentStartValue(amount);
                        investorSchemeWisePortfolioResponse.setPurchaseNav(price);
						/*String dividend_flag = camsSchemeWiseInvestorTransactions.get(i).getReinvest_f().trim();
						if (dividend_flag.equalsIgnoreCase("N") || dividend_flag.equalsIgnoreCase("Y")) {
							investorSchemeWisePortfolioResponse.setIsDividendScheme(true);
							if (dividend_flag.equalsIgnoreCase("Y")) {
								investorSchemeWisePortfolioResponse.setIsDividendReinvest(true);
							}
						}*/
                        investorSchemeWisePortfolioResponse.setScheme_registrar("cams");
                        investorSchemeWisePortfolioResponse.setBroker_code(camsSchemeWiseInvestorTransactions.get(i).getBrokcode());
                        investorSchemeWisePortfolioResponse.setEuin(camsSchemeWiseInvestorTransactions.get(i).getEuin());
                    }

                    if (camsPositiveTransactionArrayList.contains(trxn_type_))
                    {
                        if (investorSchemeWisePortfolioResponse.getTotalUnits() == 0) {
                            investorSchemeWisePortfolioResponse.setIsNegativeTransaction(false);
                            investorSchemeWisePortfolioResponse.setTotalInflow(0);
                            investorSchemeWisePortfolioResponse.setTotalOutflow(0);
                            investorSchemeWisePortfolioResponse.setTotalUnits(0);
                            investorSchemeWisePortfolioResponse.setDividendReinvestment(0);
                            investorSchemeWisePortfolioResponse.setDividendPaid(0);
                            investorSchemeWisePortfolioResponse.setTotal_dividend_reinvest(0);
                            investorSchemeWisePortfolioResponse.setTotal_dividend_paid(0);
                            investorSchemeWisePortfolioResponse.setLastInvestmentDate(traddate);
                            investorSchemeWisePortfolioResponse.setInvestmentStartNav(price);
                            investorSchemeWisePortfolioResponse.setInvestmentStartDate(traddate);
                            investorSchemeWisePortfolioResponse.setInvestmentStartValue(amount);
                            investorSchemeWisePortfolioResponse.setPurchaseNav(0);
                            xirr_list = new ArrayList<XirrResponse>();
                            xirrRes = null;
                        }

                        Double added_units = investorSchemeWisePortfolioResponse.getTotalUnits() + units;
                        added_units = Double.parseDouble(unit_decimal.format(added_units));
                        investorSchemeWisePortfolioResponse.setTotalUnits(added_units);

						/*double total_inflow_amount = investorSchemeWisePortfolioResponse.getTotalInflow() + amount;
						total_inflow_amount = Double.parseDouble(cost_dcf.format(total_inflow_amount));
						investorSchemeWisePortfolioResponse.setTotalInflow(total_inflow_amount);*/

                        if (trxn_type_.equalsIgnoreCase("Dividend Reinvest"))
                        {
                            investorSchemeWisePortfolioResponse.setDividendReinvestment(investorSchemeWisePortfolioResponse.getDividendReinvestment() + amount);
                            investorSchemeWisePortfolioResponse.setIsDividendDeclared(true);
                            investorSchemeWisePortfolioResponse.setIsDividendReinvest(true);
                            investorSchemeWisePortfolioResponse.setTotal_dividend_reinvest(investorSchemeWisePortfolioResponse.getTotal_dividend_reinvest() + amount);
                        }
                        check_units_array.add(units);

                    } else if (camsNeutralTransactionArrayList.contains(trxn_type_)) {
                        investorSchemeWisePortfolioResponse.setDividendPaid(investorSchemeWisePortfolioResponse.getDividendPaid() + amount);
                        investorSchemeWisePortfolioResponse.setIsDividendDeclared(true);
                        investorSchemeWisePortfolioResponse.setTotal_dividend_paid(investorSchemeWisePortfolioResponse.getTotal_dividend_paid() + amount);
                    } else {
                        investorSchemeWisePortfolioResponse.setIsNegativeTransaction(true);
                        investorSchemeWisePortfolioResponse.setLastTransactionDate(traddate);
                        //investorSchemeWisePortfolioResponse.setTotalOutflow(investorSchemeWisePortfolioResponse.getTotalOutflow() + amount + camsSchemeWiseInvestorTransactions.get(i).getTotal_tax());
                        Double added_units = investorSchemeWisePortfolioResponse.getTotalUnits() - units;
                        added_units = Double.parseDouble(unit_decimal.format(added_units));
                        investorSchemeWisePortfolioResponse.setTotalUnits(added_units);
                    }

                    if (camsNegativeTransactionArrayList.contains(trxn_type_)) {

                        Double purchase_units = 0.0;
                        Double sold_units = units;


                        for (int k = 0; k < check_units_array.size(); k++) {
                            purchase_units = check_units_array.get(k);


                            if (purchase_units > 0) {
                                Double remain_units = purchase_units - sold_units;
                                remain_units = Double.parseDouble(unit_decimal.format(remain_units));


                                if (remain_units >= 0) {

                                    check_units_array.set(k, remain_units);
                                    InvestorSchemeWiseTransactionResponse result = investorSchemeWiseTransactionResponseList.get(k);

                                    if (remain_units <= 0) {
                                        result.setUNITS(0.0);
                                        result.setTOTAL_UNITS(0.0);
                                    } else {
                                        result.setUNITS(remain_units);
                                        result.setTOTAL_UNITS(remain_units);
                                    }

                                    investorSchemeWiseTransactionResponseList.set(k, result);
                                    break;
                                } else {

                                    Double current_units = check_units_array.get(k);
                                    sold_units = sold_units - current_units;
                                    sold_units = Double.parseDouble(unit_decimal.format(sold_units));
                                    check_units_array.set(k, 0.0);
                                    InvestorSchemeWiseTransactionResponse result = investorSchemeWiseTransactionResponseList.get(k);
                                    result.setUNITS(0.0);
                                    result.setTOTAL_UNITS(0.0);
                                    investorSchemeWiseTransactionResponseList.set(k, result);
                                }
                            }
                        }
                    }
                    if (camsPositiveTransactionArrayList.contains(trxn_type_)) {
                        investorSchemeWiseTransactionResponseList.add(investorSchemeWiseTransactionResponse);
                    }
                }

                if (investorSchemeWisePortfolioResponse.getTotalUnits() <= 0)
                {
                    continue;
                }
                Double total_value = 0.0;
                boolean first_flag = true;
                for (InvestorSchemeWiseTransactionResponse res : investorSchemeWiseTransactionResponseList)
                {
                    Double unit = res.getUNITS();
                    if(unit <= 0){
                        continue;
                    }
                    total_value = total_value + unit;
                    String trxn_ty = res.getTRXN_TYPE_();
                    Date trad = res.getTRADDATE();
                    Double nav = res.getPURPRICE();
                    Double amt = unit * nav;
                    if(first_flag){
                        investorSchemeWisePortfolioResponse.setInvestmentStartDate(trad);
                        first_flag = false;
                    }
                    xirrRes = new XirrResponse();
                    xirrRes.setTrxn_date(trad);
                    xirrRes.setAmount(-amt);
                    xirr_list.add(xirrRes);

                    double total_inflow_amount = investorSchemeWisePortfolioResponse.getTotalInflow() + amt;
                    total_inflow_amount = Double.parseDouble(cost_dcf.format(total_inflow_amount));
                    investorSchemeWisePortfolioResponse.setTotalInflow(total_inflow_amount);
                }


                investorSchemeWisePortfolioResponse.setInvestorSchemeWiseTransactionResponses(investorSchemeWiseTransactionResponseList);
                investorSchemeWisePortfolioResponse.setRealisedProfitLoss(0);


                AmfiSchemeMasterDTO schemeMappingList = amfiServiceClient.getBySchemeCamsProductcode(investorSchemeWisePortfolioResponse.getScheme_code(),token);
                if (schemeMappingList != null)
                {
                    String scheme_company = schemeMappingList.getScheme_company();
                    String scheme_advisorkhoj_category = schemeMappingList.getScheme_advisorkhoj_category();
                    String scheme_class = schemeMappingList.getScheme_broad_category();
                    String scheme_name = schemeMappingList.getScheme_amfi();
                    String scheme_amfi_code = schemeMappingList.getScheme_amfi_code();
                    String scheme_amfi_short_name = schemeMappingList.getScheme_amfi_short_name();

                    investorSchemeWisePortfolioResponse.setScheme(scheme_name);
                    investorSchemeWisePortfolioResponse.setScheme_amfi_short_name(scheme_amfi_short_name);
                    investorSchemeWisePortfolioResponse.setScheme_class(scheme_class);
                    investorSchemeWisePortfolioResponse.setScheme_company(scheme_company);
                    investorSchemeWisePortfolioResponse.setScheme_advisorkhoj_category(scheme_advisorkhoj_category);
                    investorSchemeWisePortfolioResponse.setScheme_amfi_code(scheme_amfi_code);


                    latestNavList = amfiServiceClient.findByLatestNav(schemeMappingList.getScheme_amfi_code(),token);
                    if (latestNavList.size() > 0) {
                        investorSchemeWisePortfolioResponse.setLatestNav(latestNavList.get(0).getNet_asset_value());
                        investorSchemeWisePortfolioResponse.setLatestNavDate(latestNavList.get(0).getNav_date());
                    } else
                    {
                        Date nav_date = null;
                        double nav_value = 0;


                        List nav_list = amfiServiceClient.getTopBySchemeCodeOrderByNavDateDesc(schemeMappingList.getScheme_amfi_code(),token);
                        for (Iterator It = nav_list.iterator(); It.hasNext();) {
                            Object[] row = (Object[]) It.next();
                            nav_date = sdf1.parse(String.valueOf(row[0]));
                            nav_value = Double.parseDouble(String.valueOf(row[1]));
                        }
                        if (nav_date != null) {
                            investorSchemeWisePortfolioResponse.setLatestNav(nav_value);
                            investorSchemeWisePortfolioResponse.setLatestNavDate(nav_date);
                        } else {
                            investorSchemeWisePortfolioResponse.setLatestNav(last_tran_nav);
                            investorSchemeWisePortfolioResponse.setLatestNavDate(last_tran_date);
                        }
                    }
                }else
                {
                    investorSchemeWisePortfolioResponse.setLatestNav(last_tran_nav);
                    investorSchemeWisePortfolioResponse.setLatestNavDate(last_tran_date);
                }

                Double current_cost = investorSchemeWisePortfolioResponse.getTotalInflow();
                if(current_cost <= 0)
                {
                    current_cost = 0.0;
                }
                current_cost = Double.parseDouble(cost_dcf.format(current_cost));
                investorSchemeWisePortfolioResponse.setCurrentCostOfInvestment(current_cost);

                Double purchase_nav = current_cost / investorSchemeWisePortfolioResponse.getTotalUnits();
                purchase_nav = Double.parseDouble(unit_decimal.format(purchase_nav));
                if (purchase_nav > 0) {
                    investorSchemeWisePortfolioResponse.setPurchaseNav(purchase_nav);
                }
                investorSchemeWisePortfolioResponse.setTotalCurrentValue(investorSchemeWisePortfolioResponse.getTotalUnits() * investorSchemeWisePortfolioResponse.getLatestNav());

                xirrRes = new XirrResponse();
                xirrRes.setTrxn_date(investorSchemeWisePortfolioResponse.getLatestNavDate());
                xirrRes.setAmount(investorSchemeWisePortfolioResponse.getTotalCurrentValue());
                xirr_list.add(xirrRes);

                investorSchemeWisePortfolioResponse.setUnrealisedProfitLoss(investorSchemeWisePortfolioResponse.getTotalCurrentValue() - investorSchemeWisePortfolioResponse.getCurrentCostOfInvestment());

                Double gain = investorSchemeWisePortfolioResponse.getUnrealisedProfitLoss();
                Double invested_value = investorSchemeWisePortfolioResponse.getCurrentCostOfInvestment();
                if (invested_value > 0) {
                    Double absolute_return = gain / invested_value;
                    absolute_return = absolute_return * 100;
                    absolute_return = Double.parseDouble(cost_dcf.format(absolute_return));
                    investorSchemeWisePortfolioResponse.setAbsolute_return(absolute_return);
                } else {
                    investorSchemeWisePortfolioResponse.setAbsolute_return(0.0);
                }

                final_cagr_list.addAll(xirr_list);

                int array_size = xirr_list.size();

                double[] values = new double[array_size];
                double[] dates = new double[array_size];

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(0);

                int i = 0;
                for (XirrResponse xir : xirr_list)
                {
                    values[i] = xir.getAmount();
                    cal = Calendar.getInstance();
                    cal.setTime(xir.getTrxn_date());
                    dates[i] = (double) XIRR.getDateDiff(cal, calendar);
                    i++;
                }
                double xirrValue = XIRR.Newtons_method(values, dates, 0.1);
                if (xirrValue == 0) {
                    xirrValue = XIRR.Bisection_method(values, dates, 0.1);
                }
                if (array_size == 1 && xirr_list.get(0).getAmount() == 0) {
                    xirrValue = 0;
                }
                if (array_size == 2) {
                    if (xirr_list.get(0).getAmount() + xirr_list.get(1).getAmount() == 0) {
                        xirrValue = 0;
                    }
                }
                xirrValue = xirrValue * 100;

                investorSchemeWisePortfolioResponse.setCagr(Double.parseDouble(cost_dcf.format(xirrValue)));

                investorPortfolioResponse.setTotalInflow(investorPortfolioResponse.getTotalInflow() + investorSchemeWisePortfolioResponse.getTotalInflow());
                investorPortfolioResponse.setTotalOutflow(investorPortfolioResponse.getTotalOutflow() + investorSchemeWisePortfolioResponse.getTotalOutflow());
                investorPortfolioResponse.setTotalUnReliasedGain(investorPortfolioResponse.getTotalUnReliasedGain() + investorSchemeWisePortfolioResponse.getUnrealisedProfitLoss());
                investorPortfolioResponse.setTotalReliasedGain(investorPortfolioResponse.getTotalReliasedGain() + investorSchemeWisePortfolioResponse.getRealisedProfitLoss());
                investorPortfolioResponse.setTotalDividendPaid(investorPortfolioResponse.getTotalDividendPaid() + investorSchemeWisePortfolioResponse.getTotal_dividend_paid());
                investorPortfolioResponse.setTotalDividendReinvestment(investorPortfolioResponse.getTotalDividendReinvestment() + investorSchemeWisePortfolioResponse.getTotal_dividend_reinvest());
                investorPortfolioResponse.setTotalCurrentValue(investorPortfolioResponse.getTotalCurrentValue() + investorSchemeWisePortfolioResponse.getTotalCurrentValue());
                investorPortfolioResponse.setTotalCurrentcost(investorPortfolioResponse.getTotalCurrentcost() + investorSchemeWisePortfolioResponse.getCurrentCostOfInvestment());
                investorPortfolioResponse.setTotalCAGR(investorPortfolioResponse.getTotalCAGR() + investorSchemeWisePortfolioResponse.getCagr());



                investorSchemeWisePortfolioResponseList.add(investorSchemeWisePortfolioResponse);
            }

            // Get distinct schemes of the karvy transactions

            try {
                karvy_scheme_list = userServiceClient.getGroupedTransactions(user_id, client_name, token);
            } catch (FeignException.NotFound e)
            {
                karvy_scheme_list = new ArrayList<>();
            } catch (FeignException e)
            {
                karvy_scheme_list = new ArrayList<>();
            }
            System.out.println("karvy_scheme_list = " + karvy_scheme_list.size());
            for (int s = 0; s < karvy_scheme_list.size(); s++) {


                karvySchemeWiseInvestorTransactions = userServiceClient.getTransactionsByFolioAndFund(user_id,client_name,karvy_scheme_list.get(s).getFolio_number(),karvy_scheme_list.get(s).getFund(),karvy_scheme_list.get(s).getScheme_code(),token);

                if(karvySchemeWiseInvestorTransactions.size() > 0)
                {
                    karvySchemeWiseInvestorTransactions = NseUtils.removeKarvyMinusTransaction(karvySchemeWiseInvestorTransactions);
                }

                if(karvySchemeWiseInvestorTransactions.size() == 0)
                {
                    continue;
                }

                investorSchemeWisePortfolioResponse = new InvestorSchemeWisePortfolioResponse();
                investorSchemeWiseTransactionResponseList = new ArrayList<InvestorSchemeWiseTransactionResponse>();

                xirr_list = new ArrayList<XirrResponse>();
                xirrRes = null;
                List<Double> check_units_array = new ArrayList<Double>();
                Date last_tran_date = null;
                Double last_tran_nav = 0.0;
                Date traddate_prev = null;
                Double units_prev = 0.0;
                Date traddate_next = null;
                Double units_next = 0.0;
                boolean transfer_flag = false;

                for (int i = 0; i < karvySchemeWiseInvestorTransactions.size(); i++) {
                    Date transaction_date = karvySchemeWiseInvestorTransactions.get(i).getTransaction_date();
                    Double purchase_price = 0.0;
                    if(karvySchemeWiseInvestorTransactions.get(i).getPrice() != null){
                        purchase_price = karvySchemeWiseInvestorTransactions.get(i).getPrice();
                    }
                    Double units = 0.0;
                    if(karvySchemeWiseInvestorTransactions.get(i).getUnits() != null){
                        units = karvySchemeWiseInvestorTransactions.get(i).getUnits();
                    }
                    String transaction_description = karvySchemeWiseInvestorTransactions.get(i).getTransaction_description().trim();
                    Double amount = karvySchemeWiseInvestorTransactions.get(i).getAmount();
                    String remarks = karvySchemeWiseInvestorTransactions.get(i).getRemarks().trim();
                    String fund_description = karvySchemeWiseInvestorTransactions.get(i).getFund_description().trim();
                    String prodcode = karvySchemeWiseInvestorTransactions.get(i).getProduct_code().trim();
                    Double tds_amount = 0.0;
                    if (karvySchemeWiseInvestorTransactions.get(i).getTdsamount() != null) {
                        tds_amount = karvySchemeWiseInvestorTransactions.get(i).getTdsamount();
                    } else {
                        tds_amount = 0.0;
                    }
                    last_tran_date = transaction_date;
                    last_tran_nav = purchase_price;

                    investorSchemeWiseTransactionResponse = new InvestorSchemeWiseTransactionResponse();
                    investorSchemeWiseTransactionResponse.setTRADDATE(transaction_date);
                    investorSchemeWiseTransactionResponse.setPURPRICE(purchase_price);
                    investorSchemeWiseTransactionResponse.setUNITS(units);
                    investorSchemeWiseTransactionResponse.setTOTAL_TAX(tds_amount);
                    investorSchemeWiseTransactionResponse.setTRXN_TYPE_(transaction_description);
                    investorSchemeWiseTransactionResponse.setTRXN_SUFFI(remarks);
                    investorSchemeWiseTransactionResponse.setAMOUNT(amount);
                    //investorSchemeWiseTransactionResponseList.add(investorSchemeWiseTransactionResponse);

                    if (i == 0) {
                        investorSchemeWisePortfolioResponse.setScheme(fund_description);
                        investorSchemeWisePortfolioResponse.setScheme_amfi_short_name(fund_description);
                        investorSchemeWisePortfolioResponse.setScheme_code(karvySchemeWiseInvestorTransactions.get(i).getFund() + karvySchemeWiseInvestorTransactions.get(i).getScheme_code());
                        investorSchemeWisePortfolioResponse.setFoliono(karvySchemeWiseInvestorTransactions.get(i).getFolio_number().trim());
                        investorSchemeWisePortfolioResponse.setAmc_code(karvySchemeWiseInvestorTransactions.get(i).getFund());
						/*String scheme_dividend = karvySchemeWiseInvestorTransactions.get(i).getDividend_option().trim();
						if (scheme_dividend.equalsIgnoreCase("D") || scheme_dividend.equalsIgnoreCase("R")) {
							investorSchemeWisePortfolioResponse.setIsDividendScheme(true);
							if (scheme_dividend.equalsIgnoreCase("R")) {
								investorSchemeWisePortfolioResponse.setIsDividendReinvest(true);
							}
						}*/
                        investorSchemeWisePortfolioResponse.setInvestmentStartNav(purchase_price);
                        investorSchemeWisePortfolioResponse.setInvestmentStartDate(transaction_date);
                        investorSchemeWisePortfolioResponse.setInvestmentStartValue(amount);
                        investorSchemeWisePortfolioResponse.setPurchaseNav(purchase_price);
                        investorSchemeWisePortfolioResponse.setScheme_registrar("karvy");
                        investorSchemeWisePortfolioResponse.setBroker_code(karvySchemeWiseInvestorTransactions.get(i).getAgent_code());
                        investorSchemeWisePortfolioResponse.setEuin(karvySchemeWiseInvestorTransactions.get(i).getEuin());
                    }

                    if (karvyPositiveTransactionArrayList.contains(transaction_description)) {
                        if (investorSchemeWisePortfolioResponse.getTotalUnits() == 0) {
                            investorSchemeWisePortfolioResponse.setIsNegativeTransaction(false);
                            investorSchemeWisePortfolioResponse.setTotalInflow(0);
                            investorSchemeWisePortfolioResponse.setTotalOutflow(0);
                            investorSchemeWisePortfolioResponse.setTotalUnits(0);
                            investorSchemeWisePortfolioResponse.setDividendReinvestment(0);
                            investorSchemeWisePortfolioResponse.setDividendPaid(0);
                            investorSchemeWisePortfolioResponse.setTotal_dividend_reinvest(0);
                            investorSchemeWisePortfolioResponse.setTotal_dividend_paid(0);
                            investorSchemeWisePortfolioResponse.setInvestmentStartNav(purchase_price);
                            investorSchemeWisePortfolioResponse.setInvestmentStartDate(transaction_date);
                            investorSchemeWisePortfolioResponse.setInvestmentStartValue(amount);
                            investorSchemeWisePortfolioResponse.setPurchaseNav(0);
                            xirr_list = new ArrayList<XirrResponse>();
                            xirrRes = null;
                        }

                        Double current_units = investorSchemeWisePortfolioResponse.getTotalUnits() + units;
                        current_units = Double.parseDouble(unit_decimal.format(current_units));
                        investorSchemeWisePortfolioResponse.setTotalUnits(current_units);

						/*double total_inflow_amount = investorSchemeWisePortfolioResponse.getTotalInflow() + amount;
						total_inflow_amount = Double.parseDouble(cost_dcf.format(total_inflow_amount));
						investorSchemeWisePortfolioResponse.setTotalInflow(total_inflow_amount);*/

                        if (transaction_description.equalsIgnoreCase("Div. Reinvestment") || transaction_description.equalsIgnoreCase("ReInvestment Rejection") || transaction_description.equalsIgnoreCase("Bonus Units"))
                        {
                            investorSchemeWisePortfolioResponse.setDividendReinvestment(investorSchemeWisePortfolioResponse.getDividendReinvestment() + amount);
                            investorSchemeWisePortfolioResponse.setIsDividendReinvest(true);
                            investorSchemeWisePortfolioResponse.setIsDividendDeclared(true);
                            investorSchemeWisePortfolioResponse.setTotal_dividend_reinvest(investorSchemeWisePortfolioResponse.getTotal_dividend_reinvest() + amount);
                        }

                        check_units_array.add(units);

                    } else if (karvyNeutralTransactionArrayList.contains(transaction_description)) {
                        if (transaction_description.equalsIgnoreCase("Consolidation Out") || transaction_description.contains("Transmission")) {
                            investorSchemeWisePortfolioResponse.setIsNegativeTransaction(false);
                            investorSchemeWisePortfolioResponse.setTotalInflow(0);
                            investorSchemeWisePortfolioResponse.setTotalOutflow(0);
                            investorSchemeWisePortfolioResponse.setTotalUnits(0);
                            investorSchemeWisePortfolioResponse.setDividendReinvestment(0);
                            investorSchemeWisePortfolioResponse.setDividendPaid(0);
                            investorSchemeWisePortfolioResponse.setTotal_dividend_reinvest(0);
                            investorSchemeWisePortfolioResponse.setTotal_dividend_paid(0);
                            investorSchemeWisePortfolioResponse.setInvestmentStartNav(purchase_price);
                            investorSchemeWisePortfolioResponse.setInvestmentStartDate(transaction_date);
                            investorSchemeWisePortfolioResponse.setInvestmentStartValue(amount);
                            investorSchemeWisePortfolioResponse.setPurchaseNav(0);
                            xirr_list = new ArrayList<XirrResponse>();
                            xirrRes = null;
                            transfer_flag = true;
                        }else if (karvy_neutral_dividend_transaction_type_list.contains(transaction_description)) {
                            investorSchemeWisePortfolioResponse.setDividendPaid(investorSchemeWisePortfolioResponse.getDividendPaid() + amount);
                            investorSchemeWisePortfolioResponse.setIsDividendDeclared(true);
                            investorSchemeWisePortfolioResponse.setTotal_dividend_paid(investorSchemeWisePortfolioResponse.getTotal_dividend_paid() + amount);

                        }else{

                        }
                    } else {
                        investorSchemeWisePortfolioResponse.setIsNegativeTransaction(true);
                        investorSchemeWisePortfolioResponse.setLastTransactionDate(transaction_date);
                        //investorSchemeWisePortfolioResponse.setTotalOutflow(investorSchemeWisePortfolioResponse.getTotalOutflow() + amount);
                        Double current_units = investorSchemeWisePortfolioResponse.getTotalUnits() - units;
                        current_units = Double.parseDouble(unit_decimal.format(current_units));
                        investorSchemeWisePortfolioResponse.setTotalUnits(current_units);
                    }

                    if (transfer_flag)
                    {
                        investorSchemeWisePortfolioResponse.setIsNegativeTransaction(false);
                        investorSchemeWisePortfolioResponse.setTotalInflow(0);
                        investorSchemeWisePortfolioResponse.setTotalOutflow(0);
                        investorSchemeWisePortfolioResponse.setTotalUnits(0);
                        investorSchemeWisePortfolioResponse.setDividendReinvestment(0);
                        investorSchemeWisePortfolioResponse.setDividendPaid(0);
                        investorSchemeWisePortfolioResponse.setTotal_dividend_reinvest(0);
                        investorSchemeWisePortfolioResponse.setTotal_dividend_paid(0);
                        investorSchemeWisePortfolioResponse.setInvestmentStartNav(purchase_price);
                        investorSchemeWisePortfolioResponse.setInvestmentStartDate(transaction_date);
                        investorSchemeWisePortfolioResponse.setInvestmentStartValue(amount);
                        investorSchemeWisePortfolioResponse.setPurchaseNav(0);
                        xirr_list = new ArrayList<XirrResponse>();
                        xirrRes = null;
                    }

                    if (karvyNegativeTransactionArrayList.contains(transaction_description)) {

                        Double purchase_units = 0.0;
                        Double sold_units = units;


                        for (int k = 0; k < check_units_array.size(); k++) {
                            purchase_units = check_units_array.get(k);


                            if (purchase_units > 0) {
                                Double remain_units = purchase_units - sold_units;
                                remain_units = Double.parseDouble(unit_decimal.format(remain_units));


                                if (remain_units >= 0) {

                                    check_units_array.set(k, remain_units);
                                    InvestorSchemeWiseTransactionResponse result = investorSchemeWiseTransactionResponseList.get(k);

                                    if (remain_units <= 0) {
                                        result.setUNITS(0.0);
                                        result.setTOTAL_UNITS(0.0);
                                    } else {
                                        result.setUNITS(remain_units);
                                        result.setTOTAL_UNITS(remain_units);
                                    }

                                    investorSchemeWiseTransactionResponseList.set(k, result);
                                    break;
                                } else {

                                    Double current_units = check_units_array.get(k);
                                    sold_units = sold_units - current_units;
                                    sold_units = Double.parseDouble(unit_decimal.format(sold_units));
                                    check_units_array.set(k, 0.0);
                                    InvestorSchemeWiseTransactionResponse result = investorSchemeWiseTransactionResponseList.get(k);
                                    result.setUNITS(0.0);
                                    result.setTOTAL_UNITS(0.0);
                                    investorSchemeWiseTransactionResponseList.set(k, result);
                                }
                            }
                        }
                    }
                    if (karvyPositiveTransactionArrayList.contains(transaction_description)) {
                        investorSchemeWiseTransactionResponseList.add(investorSchemeWiseTransactionResponse);
                    }
                }

                if (investorSchemeWisePortfolioResponse.getTotalUnits() <= 0)
                {
                    continue;
                }

                Double total_value = 0.0;
                boolean first_flag = true;
                for (InvestorSchemeWiseTransactionResponse res : investorSchemeWiseTransactionResponseList)
                {
                    Double unit = res.getUNITS();
                    if(unit <= 0){
                        continue;
                    }
                    total_value = total_value + unit;
                    String trxn_ty = res.getTRXN_TYPE_();
                    Date trad = res.getTRADDATE();
                    Double nav = res.getPURPRICE();
                    Double amt = unit * nav;
                    if(first_flag){
                        investorSchemeWisePortfolioResponse.setInvestmentStartDate(trad);
                        first_flag = false;
                    }
                    xirrRes = new XirrResponse();
                    xirrRes.setTrxn_date(trad);
                    xirrRes.setAmount(-amt);
                    xirr_list.add(xirrRes);

                    double total_inflow_amount = investorSchemeWisePortfolioResponse.getTotalInflow() + amt;
                    total_inflow_amount = Double.parseDouble(cost_dcf.format(total_inflow_amount));
                    investorSchemeWisePortfolioResponse.setTotalInflow(total_inflow_amount);
                }

                investorSchemeWisePortfolioResponse.setInvestorSchemeWiseTransactionResponses(investorSchemeWiseTransactionResponseList);
                investorSchemeWisePortfolioResponse.setRealisedProfitLoss(0);


                AmfiSchemeMasterDTO schemeMappingList = amfiServiceClient.getFirstBySchemeKarvyProductcode(investorSchemeWisePortfolioResponse.getScheme_code(),token);

                if (schemeMappingList != null) {
                    String scheme_company = schemeMappingList.getScheme_company();
                    String scheme_advisorkhoj_category = schemeMappingList.getScheme_advisorkhoj_category();
                    String scheme_class = schemeMappingList.getScheme_broad_category();
                    String scheme_name = schemeMappingList.getScheme_amfi();
                    String scheme_amfi_code = schemeMappingList.getScheme_amfi_code();
                    String scheme_amfi_short_name = schemeMappingList.getScheme_amfi_short_name();

                    investorSchemeWisePortfolioResponse.setScheme(scheme_name);
                    investorSchemeWisePortfolioResponse.setScheme_amfi_short_name(scheme_amfi_short_name);
                    investorSchemeWisePortfolioResponse.setScheme_class(scheme_class);
                    investorSchemeWisePortfolioResponse.setScheme_company(scheme_company);
                    investorSchemeWisePortfolioResponse.setScheme_advisorkhoj_category(scheme_advisorkhoj_category);
                    investorSchemeWisePortfolioResponse.setScheme_amfi_code(scheme_amfi_code);


                    latestNavList = amfiServiceClient.findByLatestNav(schemeMappingList.getScheme_amfi_code(),token);

                    if (latestNavList.size() > 0) {
                        investorSchemeWisePortfolioResponse.setLatestNav(latestNavList.get(0).getNet_asset_value());
                        investorSchemeWisePortfolioResponse.setLatestNavDate(latestNavList.get(0).getNav_date());
                    } else {
                        Date nav_date = null;
                        double nav_value = 0;

                        List nav_list = amfiServiceClient.getTopBySchemeCodeOrderByNavDateDesc(schemeMappingList.getScheme_amfi_code(),token);

                        for (Iterator It = nav_list.iterator(); It.hasNext();) {
                            Object[] row = (Object[]) It.next();
                            nav_date = sdf1.parse(String.valueOf(row[0]));
                            nav_value = Double.parseDouble(String.valueOf(row[1]));
                        }
                        if (nav_date != null) {
                            investorSchemeWisePortfolioResponse.setLatestNav(nav_value);
                            investorSchemeWisePortfolioResponse.setLatestNavDate(nav_date);
                        } else {
                            investorSchemeWisePortfolioResponse.setLatestNav(last_tran_nav);
                            investorSchemeWisePortfolioResponse.setLatestNavDate(last_tran_date);
                        }
                    }
                } else {
                    investorSchemeWisePortfolioResponse.setLatestNav(last_tran_nav);
                    investorSchemeWisePortfolioResponse.setLatestNavDate(last_tran_date);
                }

                Double current_cost = investorSchemeWisePortfolioResponse.getTotalInflow();
                if(current_cost <= 0)
                {
                    current_cost = 0.0;
                }
                current_cost = Double.parseDouble(cost_dcf.format(current_cost));
                investorSchemeWisePortfolioResponse.setCurrentCostOfInvestment(current_cost);

                Double purchase_nav = current_cost / investorSchemeWisePortfolioResponse.getTotalUnits();
                purchase_nav = Double.parseDouble(unit_decimal.format(purchase_nav));
                if (purchase_nav > 0) {
                    investorSchemeWisePortfolioResponse.setPurchaseNav(purchase_nav);
                }

                Double current_value = investorSchemeWisePortfolioResponse.getTotalUnits() * investorSchemeWisePortfolioResponse.getLatestNav();
                current_value = Double.parseDouble(cost_dcf.format(current_value));
                investorSchemeWisePortfolioResponse.setTotalCurrentValue(current_value);

                xirrRes = new XirrResponse();
                xirrRes.setTrxn_date(investorSchemeWisePortfolioResponse.getLatestNavDate());
                xirrRes.setAmount(investorSchemeWisePortfolioResponse.getTotalCurrentValue());
                xirr_list.add(xirrRes);

                investorSchemeWisePortfolioResponse.setUnrealisedProfitLoss(investorSchemeWisePortfolioResponse.getTotalCurrentValue() - investorSchemeWisePortfolioResponse.getCurrentCostOfInvestment());

                Double gain = investorSchemeWisePortfolioResponse.getUnrealisedProfitLoss();
                Double invested_value = investorSchemeWisePortfolioResponse.getCurrentCostOfInvestment();
                if (invested_value > 0) {
                    Double absolute_return = gain / invested_value;
                    absolute_return = absolute_return * 100;
                    absolute_return = Double.parseDouble(cost_dcf.format(absolute_return));
                    investorSchemeWisePortfolioResponse.setAbsolute_return(absolute_return);
                } else {
                    investorSchemeWisePortfolioResponse.setAbsolute_return(0.0);
                }

                final_cagr_list.addAll(xirr_list);

                int array_size = xirr_list.size();

                double[] values = new double[array_size];
                double[] dates = new double[array_size];

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(0);

                int i = 0;
                for (XirrResponse xir : xirr_list)
                {
                    values[i] = xir.getAmount();
                    cal = Calendar.getInstance();
                    cal.setTime(xir.getTrxn_date());
                    dates[i] = (double) XIRR.getDateDiff(cal, calendar);
                    i++;
                }
                double xirrValue = XIRR.Newtons_method(values, dates, 0.1);
                if (xirrValue == 0) {
                    xirrValue = XIRR.Bisection_method(values, dates, 0.1);
                }
                if (array_size == 1 && xirr_list.get(0).getAmount() == 0) {
                    xirrValue = 0;
                }
                if (array_size == 2) {
                    if (xirr_list.get(0).getAmount() + xirr_list.get(1).getAmount() == 0) {
                        xirrValue = 0;
                    }
                }
                xirrValue = xirrValue * 100;

                investorSchemeWisePortfolioResponse.setCagr(Double.parseDouble(cost_dcf.format(xirrValue)));

                investorPortfolioResponse.setTotalInflow(investorPortfolioResponse.getTotalInflow() + investorSchemeWisePortfolioResponse.getTotalInflow());
                investorPortfolioResponse.setTotalOutflow(investorPortfolioResponse.getTotalOutflow() + investorSchemeWisePortfolioResponse.getTotalOutflow());
                investorPortfolioResponse.setTotalUnReliasedGain(investorPortfolioResponse.getTotalUnReliasedGain() + investorSchemeWisePortfolioResponse.getUnrealisedProfitLoss());
                investorPortfolioResponse.setTotalReliasedGain(investorPortfolioResponse.getTotalReliasedGain() + investorSchemeWisePortfolioResponse.getRealisedProfitLoss());
                investorPortfolioResponse.setTotalDividendPaid(investorPortfolioResponse.getTotalDividendPaid() + investorSchemeWisePortfolioResponse.getTotal_dividend_paid());
                investorPortfolioResponse.setTotalDividendReinvestment(investorPortfolioResponse.getTotalDividendReinvestment() + investorSchemeWisePortfolioResponse.getTotal_dividend_reinvest());
                investorPortfolioResponse.setTotalCurrentValue(investorPortfolioResponse.getTotalCurrentValue() + investorSchemeWisePortfolioResponse.getTotalCurrentValue());
                investorPortfolioResponse.setTotalCurrentcost(investorPortfolioResponse.getTotalCurrentcost() + investorSchemeWisePortfolioResponse.getCurrentCostOfInvestment());
                investorPortfolioResponse.setTotalCAGR(investorPortfolioResponse.getTotalCAGR() + investorSchemeWisePortfolioResponse.getCagr());
                investorSchemeWisePortfolioResponseList.add(investorSchemeWisePortfolioResponse);
            }


            // Get distinct schemes of the transaction
            try {
                portfolio_scheme_list = userServiceClient.getNseGroupedTransactions(user_id,client_name,token);
            } catch (FeignException.NotFound e)
            {
                portfolio_scheme_list = new ArrayList<>();
            } catch (FeignException e)
            {
                portfolio_scheme_list = new ArrayList<>();
            }

            System.out.println("portfolio_scheme_list = " + portfolio_scheme_list.size());
            for (int s = 0; s < portfolio_scheme_list.size(); s++) {

                portfolioSchemeWiseInvestorTransactions = userServiceClient.getByFolioSchemeAndUser(user_id,client_name,portfolio_scheme_list.get(s).getFolio_no(),portfolio_scheme_list.get(s).getScheme_code(),token);

                investorSchemeWisePortfolioResponse = new InvestorSchemeWisePortfolioResponse();
                investorSchemeWiseTransactionResponseList = new ArrayList<InvestorSchemeWiseTransactionResponse>();

                xirr_list = new ArrayList<XirrResponse>();
                xirrRes = null;
                List<Double> check_units_array = new ArrayList<Double>();
                Date last_tran_date = null;
                Double last_tran_nav = 0.0;

                for (int i = 0; i < portfolioSchemeWiseInvestorTransactions.size(); i++) {
                    String trxn_type_ = portfolioSchemeWiseInvestorTransactions.get(i).getTrxn_type_().trim();
                    Date traddate = portfolioSchemeWiseInvestorTransactions.get(i).getTraddate();
                    Double price = portfolioSchemeWiseInvestorTransactions.get(i).getPurprice();
                    Double units = portfolioSchemeWiseInvestorTransactions.get(i).getUnits();
                    Double amount = portfolioSchemeWiseInvestorTransactions.get(i).getAmount();
                    last_tran_date = traddate;
                    last_tran_nav = price;

                    investorSchemeWiseTransactionResponse = new InvestorSchemeWiseTransactionResponse();
                    investorSchemeWiseTransactionResponse.setTRADDATE(traddate);
                    investorSchemeWiseTransactionResponse.setPURPRICE(price);
                    investorSchemeWiseTransactionResponse.setUNITS(units);
                    investorSchemeWiseTransactionResponse.setTOTAL_TAX(0.0);
                    investorSchemeWiseTransactionResponse.setTRXN_TYPE_(trxn_type_);
                    investorSchemeWiseTransactionResponse.setTRXN_SUFFI(portfolioSchemeWiseInvestorTransactions.get(i).getTrxn_suffi());
                    investorSchemeWiseTransactionResponse.setAMOUNT(amount);
                    //investorSchemeWiseTransactionResponseList.add(investorSchemeWiseTransactionResponse);

                    if (i == 0) {
                        String scheme_name = portfolioSchemeWiseInvestorTransactions.get(i).getScheme().trim();
                        investorSchemeWisePortfolioResponse.setScheme(scheme_name);
                        investorSchemeWisePortfolioResponse.setScheme_amfi_short_name(scheme_name);
                        investorSchemeWisePortfolioResponse.setScheme_code(portfolioSchemeWiseInvestorTransactions.get(i).getScheme_code());
                        investorSchemeWisePortfolioResponse.setFoliono(portfolioSchemeWiseInvestorTransactions.get(i).getFolio_no());
                        boolean dividend_flag = portfolioSchemeWiseInvestorTransactions.get(i).isDividend_trxn();
                        if (dividend_flag) {
                            investorSchemeWisePortfolioResponse.setIsDividendScheme(true);
                        }
                        investorSchemeWisePortfolioResponse.setInvestmentStartNav(price);
                        investorSchemeWisePortfolioResponse.setInvestmentStartDate(traddate);
                        investorSchemeWisePortfolioResponse.setInvestmentStartValue(amount);
                        investorSchemeWisePortfolioResponse.setPurchaseNav(price);
                        investorSchemeWisePortfolioResponse.setScheme_registrar("mfmanual");
                        investorSchemeWisePortfolioResponse.setBroker_code(portfolioSchemeWiseInvestorTransactions.get(i).getBroker_code());
                        investorSchemeWisePortfolioResponse.setEuin("");
                    }

                    if (mf_manualPositiveTransactionArrayList.contains(trxn_type_)) {
                        if (investorSchemeWisePortfolioResponse.getTotalUnits() == 0) {
                            investorSchemeWisePortfolioResponse.setIsNegativeTransaction(false);
                            investorSchemeWisePortfolioResponse.setTotalInflow(0);
                            investorSchemeWisePortfolioResponse.setTotalOutflow(0);
                            investorSchemeWisePortfolioResponse.setTotalUnits(0);
                            investorSchemeWisePortfolioResponse.setDividendReinvestment(0);
                            investorSchemeWisePortfolioResponse.setDividendPaid(0);
                            investorSchemeWisePortfolioResponse.setTotal_dividend_reinvest(0);
                            investorSchemeWisePortfolioResponse.setTotal_dividend_paid(0);
                            investorSchemeWisePortfolioResponse.setInvestmentStartNav(price);
                            investorSchemeWisePortfolioResponse.setInvestmentStartDate(traddate);
                            investorSchemeWisePortfolioResponse.setInvestmentStartValue(amount);
                            investorSchemeWisePortfolioResponse.setPurchaseNav(0);
                            xirr_list = new ArrayList<XirrResponse>();
                            xirrRes = null;
                        }

                        Double added_units = investorSchemeWisePortfolioResponse.getTotalUnits() + units;
                        added_units = Double.parseDouble(unit_decimal.format(added_units));
                        investorSchemeWisePortfolioResponse.setTotalUnits(added_units);

						/*double total_inflow_amount = investorSchemeWisePortfolioResponse.getTotalInflow() + amount;
						total_inflow_amount = Double.parseDouble(cost_dcf.format(total_inflow_amount));
						investorSchemeWisePortfolioResponse.setTotalInflow(total_inflow_amount);*/

                        if (trxn_type_.equalsIgnoreCase("Dividend Reinvest"))
                        {
                            investorSchemeWisePortfolioResponse.setDividendReinvestment(investorSchemeWisePortfolioResponse.getDividendReinvestment() + amount);
                            investorSchemeWisePortfolioResponse.setIsDividendReinvest(true);
                            investorSchemeWisePortfolioResponse.setIsDividendDeclared(true);
                            investorSchemeWisePortfolioResponse.setTotal_dividend_reinvest(investorSchemeWisePortfolioResponse.getTotal_dividend_reinvest()+ amount);
                        }

                        check_units_array.add(units);

                    } else if (mf_manualNeutralTransactionArrayList.contains(trxn_type_)) {
                        investorSchemeWisePortfolioResponse.setDividendPaid(investorSchemeWisePortfolioResponse.getDividendPaid() + amount);
                        investorSchemeWisePortfolioResponse.setIsDividendDeclared(true);
                        investorSchemeWisePortfolioResponse.setTotal_dividend_paid(investorSchemeWisePortfolioResponse.getTotal_dividend_paid() + amount);
                    } else {
                        investorSchemeWisePortfolioResponse.setIsNegativeTransaction(true);
                        investorSchemeWisePortfolioResponse.setLastTransactionDate(traddate);
                        //investorSchemeWisePortfolioResponse.setTotalOutflow(investorSchemeWisePortfolioResponse.getTotalOutflow() + amount);
                        Double added_units = investorSchemeWisePortfolioResponse.getTotalUnits() - units;
                        added_units = Double.parseDouble(unit_decimal.format(added_units));
                        investorSchemeWisePortfolioResponse.setTotalUnits(added_units);
                    }

                    if (mf_manualNegativeTransactionArrayList.contains(trxn_type_)) {

                        Double purchase_units = 0.0;
                        Double sold_units = units;


                        for (int k = 0; k < check_units_array.size(); k++) {
                            purchase_units = check_units_array.get(k);


                            if (purchase_units > 0) {
                                Double remain_units = purchase_units - sold_units;
                                remain_units = Double.parseDouble(unit_decimal.format(remain_units));


                                if (remain_units >= 0) {

                                    check_units_array.set(k, remain_units);
                                    InvestorSchemeWiseTransactionResponse result = investorSchemeWiseTransactionResponseList.get(k);

                                    if (remain_units <= 0) {
                                        result.setUNITS(0.0);
                                        result.setTOTAL_UNITS(0.0);
                                    } else {
                                        result.setUNITS(remain_units);
                                        result.setTOTAL_UNITS(remain_units);
                                    }

                                    investorSchemeWiseTransactionResponseList.set(k, result);
                                    break;
                                } else {

                                    Double current_units = check_units_array.get(k);
                                    sold_units = sold_units - current_units;
                                    sold_units = Double.parseDouble(unit_decimal.format(sold_units));
                                    check_units_array.set(k, 0.0);
                                    InvestorSchemeWiseTransactionResponse result = investorSchemeWiseTransactionResponseList.get(k);
                                    result.setUNITS(0.0);
                                    result.setTOTAL_UNITS(0.0);
                                    investorSchemeWiseTransactionResponseList.set(k, result);
                                }
                            }
                        }
                    }
                    if (mf_manualPositiveTransactionArrayList.contains(trxn_type_)) {
                        investorSchemeWiseTransactionResponseList.add(investorSchemeWiseTransactionResponse);
                    }
                }

                if (investorSchemeWisePortfolioResponse.getTotalUnits() <= 0)
                {
                    continue;
                }

                Double total_value = 0.0;
                boolean first_flag = true;
                for (InvestorSchemeWiseTransactionResponse res : investorSchemeWiseTransactionResponseList)
                {
                    Double unit = res.getUNITS();
                    if(unit <= 0){
                        continue;
                    }
                    total_value = total_value + unit;
                    String trxn_ty = res.getTRXN_TYPE_();
                    Date trad = res.getTRADDATE();
                    Double nav = res.getPURPRICE();
                    Double amt = unit * nav;
                    if(first_flag){
                        investorSchemeWisePortfolioResponse.setInvestmentStartDate(trad);
                        first_flag = false;
                    }

                    xirrRes = new XirrResponse();
                    xirrRes.setTrxn_date(trad);
                    xirrRes.setAmount(-amt);
                    xirr_list.add(xirrRes);

                    double total_inflow_amount = investorSchemeWisePortfolioResponse.getTotalInflow() + amt;
                    total_inflow_amount = Double.parseDouble(cost_dcf.format(total_inflow_amount));
                    investorSchemeWisePortfolioResponse.setTotalInflow(total_inflow_amount);
                }

                investorSchemeWisePortfolioResponse.setInvestorSchemeWiseTransactionResponses(investorSchemeWiseTransactionResponseList);
                investorSchemeWisePortfolioResponse.setRealisedProfitLoss(0);

                List<AmfiSchemeMasterDTO> schemeMappingList = null;

                schemeMappingList = amfiServiceClient.getBySchemeAmfiCodeAndActive(investorSchemeWisePortfolioResponse.getScheme_code(),token);
                if (schemeMappingList.size() > 0) {
                    String scheme_company = schemeMappingList.get(0).getScheme_company();
                    String scheme_advisorkhoj_category = schemeMappingList.get(0).getScheme_advisorkhoj_category();
                    String scheme_class = schemeMappingList.get(0).getScheme_broad_category();
                    String scheme_name = schemeMappingList.get(0).getScheme_amfi();
                    String scheme_amfi_code = schemeMappingList.get(0).getScheme_amfi_code();
                    String scheme_amfi_short_name = schemeMappingList.get(0).getScheme_amfi_short_name();

                    investorSchemeWisePortfolioResponse.setScheme(scheme_name);
                    investorSchemeWisePortfolioResponse.setScheme_amfi_short_name(scheme_amfi_short_name);
                    investorSchemeWisePortfolioResponse.setScheme_class(scheme_class);
                    investorSchemeWisePortfolioResponse.setScheme_company(scheme_company);
                    investorSchemeWisePortfolioResponse.setScheme_advisorkhoj_category(scheme_advisorkhoj_category);
                    investorSchemeWisePortfolioResponse.setScheme_amfi_code(scheme_amfi_code);

                    latestNavList = amfiServiceClient.findByLatestNav(schemeMappingList.get(0).getScheme_amfi_code(),token);

                    if (latestNavList.size() > 0) {
                        investorSchemeWisePortfolioResponse.setLatestNav(latestNavList.get(0).getNet_asset_value());
                        investorSchemeWisePortfolioResponse.setLatestNavDate(latestNavList.get(0).getNav_date());
                    } else {
                        Date nav_date = null;
                        double nav_value = 0;

                        List nav_list = amfiServiceClient.getTopBySchemeCodeOrderByNavDateDesc(schemeMappingList.get(0).getScheme_amfi_code(),token);
                        for (Iterator It = nav_list.iterator(); It.hasNext();) {
                            Object[] row = (Object[]) It.next();
                            nav_date = sdf1.parse(String.valueOf(row[0]));
                            nav_value = Double.parseDouble(String.valueOf(row[1]));
                        }
                        if (nav_date != null) {
                            investorSchemeWisePortfolioResponse.setLatestNav(nav_value);
                            investorSchemeWisePortfolioResponse.setLatestNavDate(nav_date);
                        } else {
                            investorSchemeWisePortfolioResponse.setLatestNav(last_tran_nav);
                            investorSchemeWisePortfolioResponse.setLatestNavDate(last_tran_date);
                        }
                    }
                } else {
                    investorSchemeWisePortfolioResponse.setLatestNav(last_tran_nav);
                    investorSchemeWisePortfolioResponse.setLatestNavDate(last_tran_date);
                }

                Double current_cost = investorSchemeWisePortfolioResponse.getTotalInflow();
                if(current_cost <= 0)
                {
                    current_cost = 0.0;
                }
                current_cost = Double.parseDouble(cost_dcf.format(current_cost));
                investorSchemeWisePortfolioResponse.setCurrentCostOfInvestment(current_cost);

                Double purchase_nav = current_cost / investorSchemeWisePortfolioResponse.getTotalUnits();
                purchase_nav = Double.parseDouble(unit_decimal.format(purchase_nav));
                if (purchase_nav > 0) {
                    investorSchemeWisePortfolioResponse.setPurchaseNav(purchase_nav);
                }

                investorSchemeWisePortfolioResponse.setTotalCurrentValue(investorSchemeWisePortfolioResponse.getTotalUnits() * investorSchemeWisePortfolioResponse.getLatestNav());

                xirrRes = new XirrResponse();
                xirrRes.setTrxn_date(investorSchemeWisePortfolioResponse.getLatestNavDate());
                xirrRes.setAmount(investorSchemeWisePortfolioResponse.getTotalCurrentValue());
                xirr_list.add(xirrRes);

                investorSchemeWisePortfolioResponse.setUnrealisedProfitLoss(investorSchemeWisePortfolioResponse.getTotalCurrentValue() - investorSchemeWisePortfolioResponse.getCurrentCostOfInvestment());

                Double gain = investorSchemeWisePortfolioResponse.getUnrealisedProfitLoss();
                Double invested_value = investorSchemeWisePortfolioResponse.getCurrentCostOfInvestment();
                if (invested_value > 0) {
                    Double absolute_return = gain / invested_value;
                    absolute_return = absolute_return * 100;
                    absolute_return = Double.parseDouble(cost_dcf.format(absolute_return));
                    investorSchemeWisePortfolioResponse.setAbsolute_return(absolute_return);
                } else {
                    investorSchemeWisePortfolioResponse.setAbsolute_return(0.0);
                }

                final_cagr_list.addAll(xirr_list);

                int array_size = xirr_list.size();

                double[] values = new double[array_size];
                double[] dates = new double[array_size];

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(0);

                int i = 0;
                for (XirrResponse xir : xirr_list)
                {
                    values[i] = xir.getAmount();
                    cal = Calendar.getInstance();
                    cal.setTime(xir.getTrxn_date());
                    dates[i] = (double) XIRR.getDateDiff(cal, calendar);
                    i++;
                }
                double xirrValue = XIRR.Newtons_method(values, dates, 0.1);
                if (xirrValue == 0) {
                    xirrValue = XIRR.Bisection_method(values, dates, 0.1);
                }
                if (array_size == 1 && xirr_list.get(0).getAmount() == 0) {
                    xirrValue = 0;
                }
                if (array_size == 2) {
                    if (xirr_list.get(0).getAmount() + xirr_list.get(1).getAmount() == 0) {
                        xirrValue = 0;
                    }
                }
                xirrValue = xirrValue * 100;

                investorSchemeWisePortfolioResponse.setCagr(Double.parseDouble(cost_dcf.format(xirrValue)));

                investorPortfolioResponse.setTotalInflow(investorPortfolioResponse.getTotalInflow() + investorSchemeWisePortfolioResponse.getTotalInflow());
                investorPortfolioResponse.setTotalOutflow(investorPortfolioResponse.getTotalOutflow() + investorSchemeWisePortfolioResponse.getTotalOutflow());
                investorPortfolioResponse.setTotalUnReliasedGain(investorPortfolioResponse.getTotalUnReliasedGain() + investorSchemeWisePortfolioResponse.getUnrealisedProfitLoss());
                investorPortfolioResponse.setTotalReliasedGain(investorPortfolioResponse.getTotalReliasedGain() + investorSchemeWisePortfolioResponse.getRealisedProfitLoss());
                investorPortfolioResponse.setTotalDividendPaid(investorPortfolioResponse.getTotalDividendPaid() + investorSchemeWisePortfolioResponse.getTotal_dividend_paid());
                investorPortfolioResponse.setTotalDividendReinvestment(investorPortfolioResponse.getTotalDividendReinvestment() + investorSchemeWisePortfolioResponse.getTotal_dividend_reinvest());
                investorPortfolioResponse.setTotalCurrentValue(investorPortfolioResponse.getTotalCurrentValue() + investorSchemeWisePortfolioResponse.getTotalCurrentValue());
                investorPortfolioResponse.setTotalCurrentcost(investorPortfolioResponse.getTotalCurrentcost() + investorSchemeWisePortfolioResponse.getCurrentCostOfInvestment());
                investorPortfolioResponse.setTotalCAGR(investorPortfolioResponse.getTotalCAGR() + investorSchemeWisePortfolioResponse.getCagr());
                investorSchemeWisePortfolioResponseList.add(investorSchemeWisePortfolioResponse);
            }



            Double total_gain = investorPortfolioResponse.getTotalUnReliasedGain();
            Double total_invested_value = investorPortfolioResponse.getTotalCurrentcost();
            if (total_invested_value > 0) {
                Double total_absolute_return = total_gain / total_invested_value;
                total_absolute_return = total_absolute_return * 100;
                total_absolute_return = Double.parseDouble(cost_dcf.format(total_absolute_return));
                investorPortfolioResponse.setTotalAbsoluteReturn(total_absolute_return);
            } else {
                investorPortfolioResponse.setTotalAbsoluteReturn(0.0);
            }

            double xirrValue = 0;
            if (final_cagr_list.size() > 0) {

                Collections.sort(final_cagr_list, new Comparator<XirrResponse>() {
                    @Override
                    public int compare(final XirrResponse object1, final XirrResponse object2) {
                        return object1.getTrxn_date().compareTo(object2.getTrxn_date());
                    }
                });

                List<XirrResponse> cagr_list = new ArrayList<XirrResponse>();
                cagr_list.addAll(final_cagr_list);

                investorPortfolioResponse.setCagr_list(cagr_list);

                int array_size = final_cagr_list.size();


                double[] values = new double[array_size];
                double[] dates = new double[array_size];

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(0);

                int i = 0;
                for (XirrResponse xir : final_cagr_list)
                {
                    values[i] = xir.getAmount();
                    cal = Calendar.getInstance();
                    cal.setTime(xir.getTrxn_date());
                    dates[i] = (double) XIRR.getDateDiff(cal, calendar);
                    i++;


                }

                xirrValue = XIRR.Newtons_method(values, dates, 0.1);

                if (xirrValue == 0) {
                    xirrValue = XIRR.Bisection_method(values, dates, 0.1);

                }
                xirrValue = xirrValue * 100;

            } else {
                List<XirrResponse> cagr_list = new ArrayList<XirrResponse>();

                investorPortfolioResponse.setCagr_list(cagr_list);
            }
            investorPortfolioResponse.setTotalCAGR(Double.parseDouble(cost_dcf.format(xirrValue)));
            investorPortfolioResponse.setTotalDividend(investorPortfolioResponse.getTotalDividendPaid() + investorPortfolioResponse.getTotalDividendReinvestment());

            if (investorSchemeWisePortfolioResponseList != null && investorSchemeWisePortfolioResponseList.size() > 0) {
                Collections.sort(investorSchemeWisePortfolioResponseList, new Comparator<InvestorSchemeWisePortfolioResponse>() {
                    @Override
                    public int compare(final InvestorSchemeWisePortfolioResponse object1, final InvestorSchemeWisePortfolioResponse object2) {
                        return object1.getScheme().compareTo(object2.getScheme());
                    }
                });
            }

            investorPortfolioResponse.setInvestorSchemeWisePortfolioResponses(investorSchemeWisePortfolioResponseList);

        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return investorPortfolioResponse;
    }


    public List<InvestorSchemeWisePortfolioResponse> getSchemeHolding(Integer userid, String client_name, List<InvestorSchemeWisePortfolioResponse> list,@RequestHeader("Authorization") String token)
    {
        String registrar = "";
        String scheme_code = "";
        String folio_no = "";
        String company = "";
        String scheme_name = "";
        Double totalUnits = 0.0;
        Double latestNav = 0.0;
        Double totalCurrentValue = 0.0;
        String broker_code = "";
        String euin = "";
        String amc_name = "";
        String amc_code = "";
        String scheme_short_name = "";

        InvestorSchemeWisePortfolioResponse scheme = null;
        List<InvestorSchemeWisePortfolioResponse> master_list = new ArrayList<InvestorSchemeWisePortfolioResponse>();

    try {

        List<InvestorMasterCamsDto> camsList = new ArrayList<>();

        try {
            camsList = userServiceClient.getByCamsUserIdAndClientName(userid,client_name,token);
        } catch (FeignException.NotFound e) {
            // Specific handling for 404 error
            System.out.println("No schemes found for user: " + userid + " (" + client_name + ")");
            camsList = new ArrayList<>(); // empty list to avoid null issues
        } catch (FeignException e) {
            // General Feign exception handling
            System.out.println("Feign error while fetching grouped transactions: " + e.getMessage());
            camsList = new ArrayList<>();
        }

        List<InvestorMasterKarvyDto> karvyList = new ArrayList<>();

        try
        {
            karvyList = userServiceClient.getByKarvyUserIdAndClientName(userid,client_name,token);
        } catch (FeignException.NotFound e)
        {
            System.out.println("No schemes found for user: " + userid + " (" + client_name + ")");
            karvyList = new ArrayList<>();
        } catch (FeignException e)
        {
            System.out.println("Feign error while fetching grouped transactions: " + e.getMessage());
            karvyList = new ArrayList<>();
        }

        for (InvestorSchemeWisePortfolioResponse portfolio : list)
        {
            broker_code = "";
            euin = "";
            registrar = "";
            scheme_code = "";
            folio_no = "";
            scheme_name = "";
            company = "";
            totalUnits = 0.0;
            latestNav = 0.0;
            totalCurrentValue = 0.0;
            amc_name = "";
            amc_code = "";

            scheme_name = portfolio.getScheme();
            folio_no = portfolio.getFoliono();
            scheme_code = portfolio.getScheme_code();
            company = portfolio.getScheme_company();
            registrar = portfolio.getScheme_registrar();
            totalUnits = portfolio.getTotalUnits();
            latestNav = portfolio.getLatestNav();
            totalCurrentValue = portfolio.getTotalCurrentValue();
            broker_code = portfolio.getBroker_code();
            euin = portfolio.getEuin();
            amc_name = portfolio.getAmc_name();
            amc_code = portfolio.getAmc_code();
            scheme_short_name = portfolio.getScheme_amfi_short_name();

            scheme = new InvestorSchemeWisePortfolioResponse();
            scheme.setScheme(scheme_name);
            scheme.setScheme_amfi_short_name(scheme_short_name);
            scheme.setScheme_code(scheme_code);
            scheme.setScheme_registrar(registrar);
            scheme.setScheme_company(company);
            scheme.setFoliono(folio_no);
            scheme.setTotalUnits(totalUnits);
            scheme.setLatestNav(latestNav);
            scheme.setTotalCurrentValue(totalCurrentValue);
            scheme.setBroker_code(broker_code);
            scheme.setEuin(euin);
            scheme.setAmc_code(amc_code);
            scheme.setAmc_name(amc_name);


            if(StringHelper.isNotEmpty(registrar) && registrar.equalsIgnoreCase("CAMS"))
            {
                InvestorMasterCamsDto camsScheme = null;
                if(camsList != null && camsList.size() > 0)
                {
                    String folio_no1 = folio_no;
                    String scheme_code1 = scheme_code;
                    camsScheme = camsList.stream().filter(x -> x.getFoliochk().equalsIgnoreCase(folio_no1) && x.getProduct().equalsIgnoreCase(scheme_code1)).findAny().orElse(null);
                }
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
                    //if(dp_id == null || dp_id.equalsIgnoreCase("NOT PROVIDED")){dp_id = "";}
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
                    if(bank_acc_type.equalsIgnoreCase("NRE")) {
                        scheme.setTax_status_code("21");
                    }else if(bank_acc_type.equalsIgnoreCase("NRO")){
                        scheme.setTax_status_code("11");
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
                    if(bank_acc_type.equalsIgnoreCase("NRE")) {
                        scheme.setTax_status_code("21");
                    }else if(bank_acc_type.equalsIgnoreCase("NRO")){
                        scheme.setTax_status_code("11");
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
            master_list.add(scheme);
        }
    } catch (Exception ex)
    {
        System.out.println(ex);
    }
        return master_list;
    }


    public NseOnlineSchemeMaster getNSELumpsumSchemecode(String scheme, String dividend_code)
    {
        List<NseOnlineSchemeMaster> list = null;
        NseOnlineSchemeMaster schemeMaster = null;
        try
        {
            if(StringHelper.isEmpty(dividend_code) || dividend_code.equalsIgnoreCase("Z"))
            {
                list = nseOnlineSchemeMasterRepository.findBySchemeAmfiOrShortNameAndReinvestTag(scheme,"Z");
                if(list != null && list.size() > 0)
                {
                    schemeMaster = list.get(0);
                }
            }else
            {
                list = nseOnlineSchemeMasterRepository.findBySchemeAmfiOrShortNameAndReinvestTag(scheme,dividend_code);

                if(list != null && list.size() > 0)
                {
                    schemeMaster = list.get(0);
                }else
                {
                    list = nseOnlineSchemeMasterRepository.findBySchemeAmfiOrShortNameAndReinvestTag(scheme,"X");
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

    public String getNSELumpsumMinAmountBySchemeName(String scheme_name, String purchase_type, String reinvest_tag)
    {
        String minAmount = "";

        try
        {
            NseOnlineSchemeMaster schemeMaster = null;

            if(StringHelper.isEmpty(reinvest_tag) || reinvest_tag.equalsIgnoreCase("Z"))
            {
                List<NseOnlineSchemeMaster> list = nseOnlineSchemeMasterRepository.findBySchemeAmfiOrShortNameAndReinvestTag(scheme_name,"Z");

                if(list != null && list.size() > 0)
                {
                    schemeMaster = list.get(0);
                }
            }else
            {
                List<NseOnlineSchemeMaster> list = nseOnlineSchemeMasterRepository.findBySchemeAmfiOrShortNameAndReinvestTag(scheme_name,reinvest_tag);

                if(list != null && list.size() > 0)
                {
                    schemeMaster = list.get(0);
                }else
                {
                    list = nseOnlineSchemeMasterRepository.findBySchemeAmfiOrShortNameAndReinvestTag(scheme_name,"X");

                    if(list != null && list.size() > 0)
                    {
                        schemeMaster = list.get(0);
                    }
                }
            }

//            if(schemeMaster != null)
//            {
//                String scheme_code = schemeMaster.getSchemeCode();
//                String amc_code = schemeMaster.getAmcCode();
//
//                query = amfisession.createQuery("FROM NseSchemeMasterLimit where product_code = :product_code and amc_code =:amc_code and sub_trxn_type ='N' and trxn_type =:trxn_type");
//                query.setParameter("product_code", scheme_code);
//                query.setParameter("amc_code", amc_code);
//                query.setParameter("trxn_type", purchase_type);
//
//                List<NseOnlineSchemeMaster> list1 = query.getResultList();
//
//                if(list1 != null && list1.size() > 0)
//                {
//                    NseSchemeMasterLimit nse = list1.get(0);
//                    minAmount = nse.getMinimum_amount();
//                }else
//                {
//                    minAmount = "Scheme Code not available";
//                }
//            }else
//            {
//                minAmount = "Scheme Code not available";
//            }
        }
        catch(Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date()); ex.printStackTrace();
        }
        return minAmount;
    }
    public String getNSESipMinimumAmount(String scheme_name, String dividend_code, String purchase_type_code)
    {
        List<NseOnlineSchemeMaster> list = null;
        NseOnlineSchemeMaster schemeMaster = null;
        String minimum_amount = "0.0";
        try
        {
            if(StringHelper.isEmpty(dividend_code) || dividend_code.equalsIgnoreCase("Z"))
            {
                list = nseOnlineSchemeMasterRepository.findBySchemeAmfiOrShortNameAndReinvestTagSip(scheme_name,"Z");
                if(list != null && list.size() > 0)
                {
                    schemeMaster = list.get(0);
                }
            }else
            {
                list = nseOnlineSchemeMasterRepository.findBySchemeAmfiOrShortNameAndReinvestTagSip(scheme_name,dividend_code);
                if(list != null && list.size() > 0)
                {
                    schemeMaster = list.get(0);
                }else
                {
                    list = nseOnlineSchemeMasterRepository.findBySchemeAmfiOrShortNameAndReinvestTagSip(scheme_name,"X");
                    if(list != null && list.size() > 0)
                    {
                        schemeMaster = list.get(0);
                    }
                }
            }

            if(schemeMaster != null)
            {
                String scheme_code = schemeMaster.getSchemeCode();
                String amc_code = schemeMaster.getAmcCode();

                //////System.out.println("scheme_code = " + scheme_code);
                //////System.out.println("amc_code = " + amc_code);
                //////System.out.println("purchase_type_code = " + purchase_type_code);

//                query = amfisession.createQuery("FROM NseSchemeMasterLimit where product_code = :product_code and amc_code =:amc_code and sub_trxn_type ='S' and trxn_type =:trxn_type");
//                query.setParameter("product_code", scheme_code);
//                query.setParameter("amc_code", amc_code);
//                query.setParameter("trxn_type", purchase_type_code);
//                List<NseSchemeMasterLimit> list2 = query.getResultList();
//                if(list2 != null && list2.size() > 0)
//                {
//                    minimum_amount = list2.get(0).getMinimum_amount();
//                }
            }
        }
        catch(Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date()); ex.printStackTrace();
        }
        return minimum_amount;
    }

    public NseOnlineSchemeMaster getNSESipSchemecode(String scheme_name, String dividend_code)
    {
        List<NseOnlineSchemeMaster> list = null;
        NseOnlineSchemeMaster schemeMaster = null;
        try
        {
            if(StringHelper.isEmpty(dividend_code) || dividend_code.equalsIgnoreCase("Z"))
            {
                list = nseOnlineSchemeMasterRepository.findBySchemeAmfiOrShortNameAndReinvestTagSip(scheme_name,"Z");
                if(list != null && list.size() > 0)
                {
                    schemeMaster = list.get(0);
                }
            }else
            {
                list = nseOnlineSchemeMasterRepository.findBySchemeAmfiOrShortNameAndReinvestTagSip(scheme_name,dividend_code);
                if(list != null && list.size() > 0)
                {
                    schemeMaster = list.get(0);
                }else
                {
                    list = nseOnlineSchemeMasterRepository.findBySchemeAmfiOrShortNameAndReinvestTagSip(scheme_name,"X");
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

}
