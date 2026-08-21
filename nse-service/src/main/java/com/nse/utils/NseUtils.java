package com.nse.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.nse.dto.mf.InvestorTransactionCamsDto;
import com.nse.dto.mf.InvestorTransactionKarvyDto;
import com.nse.dto.mf.UserDto;
import com.nse.pojo.CommonPojo;
import com.nse.response.CommonResponse;
import com.nse.response.PurchaseTransactionResponse;
import com.nse.response.TransactionMobileResponse;
import com.nse.response.TransactionResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.internal.util.StringHelper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class NseUtils
{

    public static String checkParem(String param)
    {
        if (param == null || param.trim().equalsIgnoreCase("null") || param.trim().equalsIgnoreCase("undefined"))
        {
            return "";
        }

        return param.trim();
    }

    public static ResponseEntity<?> transactionResponse(HttpStatus status, String returnMsg, Map<String, String> transaction_status)
    {
        return ResponseEntity.status(status).body(new TransactionResponse(status.value(), status.getReasonPhrase(), returnMsg, transaction_status));
    }


    public static CommonPojo createCommonData(String code, String status)
    {
        return new CommonPojo(status, code);
    }

    public static ResponseEntity<?> transactionMobileResponse(HttpStatus status, String returnMsg, Map<String, String> transaction_status)
    {
        return ResponseEntity.status(status).body(new TransactionMobileResponse(status.value(), status.getReasonPhrase(), returnMsg, transaction_status));
    }

    public static ResponseEntity<?> purchaseTransactionResponse(HttpStatus status, String returnMsg, Map<String, String> transaction_status, Set<String> orderIdList)
    {
        return ResponseEntity.status(status).body(new PurchaseTransactionResponse(status.value(), status.getReasonPhrase(), returnMsg, transaction_status,orderIdList));
    }

    public static ResponseEntity<Object> commonResponse(String message, HttpStatus status) {
        // Use existing constructor with 3 args
        CommonResponse commonResponse = new CommonResponse(status.value(), message, "");
        return new ResponseEntity<>(commonResponse, status);
    }
    public static String trimOrEmpty(String value) {
        return value != null ? value.trim() : "";
    }

    public static String getIpAddr(HttpServletRequest request)
    {
        //is client behind something?
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null)
        {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    public static String buildLogMessage(String title, UserDto user, HttpServletRequest request)
    {
        StringBuilder logMsg = new StringBuilder();

        logMsg.append(user.getName())
                .append(" did ").append(title).append(". Details:");

        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String param = paramNames.nextElement();
            String value = request.getParameter(param);
            logMsg.append(param).append(": ").append(value).append(", ");
        }

        // Remove trailing comma if present
        if (logMsg.toString().endsWith(", ")) {
            logMsg.setLength(logMsg.length() - 2);
        }

        return logMsg.toString();
    }

    public static String getAMCCode(String amc_name)
    {
        HashMap<String,String> amc_name_map = new HashMap<String,String>();
        amc_name_map.put("Aditya Birla Sun Life Mutual Fund","B");
        amc_name_map.put("Axis Mutual Fund","128");
        amc_name_map.put("Baroda Mutual Fund","107");
        amc_name_map.put("BNP Paribas Mutual Fund","178");
        amc_name_map.put("Bank of India Mutual Fund","116");
        amc_name_map.put("Canara Robeco Mutual Fund","101");
        amc_name_map.put("Deutsche Mutual Fund","109");
        amc_name_map.put("PGIM India Mutual Fund","129");
        amc_name_map.put("DSP Mutual Fund","D");
        amc_name_map.put("Edelweiss Mutual Fund","118");
        amc_name_map.put("Franklin Templeton Mutual Fund","FTI");
        amc_name_map.put("Goldman Sachs Mutual Fund","110");
        amc_name_map.put("HDFC Mutual Fund","H");
        amc_name_map.put("HSBC Mutual Fund","O");
        amc_name_map.put("ICICI Prudential Mutual Fund","P");
        amc_name_map.put("IDBI Mutual Fund","135");
        amc_name_map.put("Bandhan Mutual Fund","G");
        amc_name_map.put("ITI Mutual Fund","152");
        /*amc_name_map.put("IIFCL Mutual Fund (IDF)","");*/
        amc_name_map.put("360 ONE Mutual Fund","IF");
        /*amc_name_map.put("IL&FS Mutual Fund (IDF)","");*/
        amc_name_map.put("Groww Mutual Fund","125");
        amc_name_map.put("ING Mutual Fund","I");
        amc_name_map.put("Invesco Mutual Fund","120");
        amc_name_map.put("JM Financial Mutual Fund","105");
        amc_name_map.put("JPMorgan Mutual Fund","J");
        amc_name_map.put("Kotak Mahindra Mutual Fund","K");
        amc_name_map.put("L&T Mutual Fund","F");
        amc_name_map.put("LIC Mutual Fund","102");
        amc_name_map.put("Mahindra Manulife Mutual Fund","MM");
        amc_name_map.put("Mirae Asset Mutual Fund","117");
        amc_name_map.put("Morgan Stanley Mutual Fund","115");
        amc_name_map.put("Motilal Oswal Mutual Fund","127");
        amc_name_map.put("PineBridge Mutual Fund","A");
        amc_name_map.put("PPFAS Mutual Fund","PP");
        amc_name_map.put("PRINCIPAL Mutual Fund","103");
        amc_name_map.put("Quant Mutual Fund","166");
        amc_name_map.put("Quantum Mutual Fund","123");
        amc_name_map.put("Nippon India Mutual Fund","RMF");
        amc_name_map.put("Sahara Mutual Fund","113");
        amc_name_map.put("SBI Mutual Fund","L");
        amc_name_map.put("Shriram Mutual Fund","SH");
        /*amc_name_map.put("SREI Mutual Fund (IDF)","");*/
        amc_name_map.put("Sundaram Mutual Fund","176");
        amc_name_map.put("Tata Mutual Fund","T");
        amc_name_map.put("Taurus Mutual Fund","104");
        amc_name_map.put("Union Mutual Fund","UK");
        amc_name_map.put("UTI Mutual Fund","108");
        amc_name_map.put("YES Mutual Fund","Y");
        amc_name_map.put("Navi Mutual Fund","130");
        amc_name_map.put("WhiteOak Capital Mutual Fund","Y");
        amc_name_map.put("Trust Mutual Fund","185");
        amc_name_map.put("NJ Mutual Fund","187");
        amc_name_map.put("Samco Mutual Fund","188");
        amc_name_map.put("Baroda BNP Paribas Mutual Fund","178");
        amc_name_map.put("Bajaj Finserv Mutual Fund","189");
        amc_name_map.put("Helios Mutual Fund","HLS");
        amc_name_map.put("Old Bridge Mutual Fund","139");
        amc_name_map.put("BNP PARIBAS Mutual Fund","178");

        String amc_code = amc_name_map.get(amc_name);

        return amc_code;
    }

    public static String getRTAName(String amc_code)
    {
        HashMap<String,String> amc_name_map = new HashMap<String,String>();
        amc_name_map.put("B","CAMS");
        amc_name_map.put("128","Karvy");
        amc_name_map.put("107","Karvy");
        amc_name_map.put("178","Karvy");
        amc_name_map.put("116","Karvy");
        amc_name_map.put("101","Karvy");
        amc_name_map.put("109","Karvy");
        amc_name_map.put("129","Karvy");
        amc_name_map.put("D","CAMS");
        amc_name_map.put("118","Karvy");
        amc_name_map.put("130","Karvy");
        amc_name_map.put("FTI","CAMS");
        amc_name_map.put("114","Karvy");
        amc_name_map.put("110","Karvy");
        amc_name_map.put("H","CAMS");
        amc_name_map.put("O","CAMS");
        amc_name_map.put("P","CAMS");
        amc_name_map.put("135","Karvy");
        amc_name_map.put("G","CAMS");
        /*amc_name_map.put("","IIFCL Mutual Fund (IDF)");*/
        amc_name_map.put("IF","CAMS");
        /*amc_name_map.put("","IL&FS Mutual Fund (IDF)");*/
        amc_name_map.put("125","Karvy");
        amc_name_map.put("152","Karvy");
        amc_name_map.put("I","CAMS");
        amc_name_map.put("120","Karvy");
        amc_name_map.put("105","Karvy");
        amc_name_map.put("J","CAMS");
        amc_name_map.put("K","CAMS");
        amc_name_map.put("F","CAMS");
        amc_name_map.put("102","Karvy");
        amc_name_map.put("MM","CAMS");
        amc_name_map.put("117","Karvy");
        amc_name_map.put("115","Karvy");
        amc_name_map.put("127","Karvy");
        amc_name_map.put("A","CAMS");
        amc_name_map.put("PP","CAMS");
        amc_name_map.put("103","Karvy");
        amc_name_map.put("166","Karvy");
        amc_name_map.put("123","Karvy");
        amc_name_map.put("RMF","Karvy");
        amc_name_map.put("113","Karvy");
        amc_name_map.put("L","CAMS");
        amc_name_map.put("SH","CAMS");
        /*amc_name_map.put("","SREI Mutual Fund (IDF)");*/
        amc_name_map.put("176","Karvy");
        amc_name_map.put("T","CAMS");
        amc_name_map.put("104","Karvy");
        amc_name_map.put("UK","CAMS");
        amc_name_map.put("108","Karvy");
        amc_name_map.put("130","Karvy");
        amc_name_map.put("Y","CAMS");
        amc_name_map.put("185","Karvy");
        amc_name_map.put("187","Karvy");
        amc_name_map.put("188","Karvy");
        amc_name_map.put("189","Karvy");
        amc_name_map.put("HLS","CAMS");
        amc_name_map.put("139","Karvy");

        String rta_name = amc_name_map.get(amc_code);

        return rta_name;
    }

    public static List<CommonPojo> getEmailOrMobileRelationList() {
        List<CommonPojo> list = new ArrayList<>();
        list.add(createCommonData("SE", "Self"));
        list.add(createCommonData("SP", "Spouse"));
        list.add(createCommonData("DC", "Dependent Children"));
        list.add(createCommonData("DS", "Dependent Siblings"));
        list.add(createCommonData("DP", "Dependent Parents"));
        list.add(createCommonData("GD", "Guardian"));
        list.add(createCommonData("PM", "PMS"));
        list.add(createCommonData("CD", "Custodian"));
        list.add(createCommonData("PO", "POA"));
        return list;
    }

    private static BiMap<String, String> clientTaxStatusCodeOrName(){

        BiMap<String, String> biMap = HashBiMap.create();

        biMap.put("01","INDIVIDUAL");
        biMap.put("02","ON BEHALF OF MINOR");
        biMap.put("03","HUF");
        biMap.put("04","COMPANY");
        biMap.put("05","AOP");
        biMap.put("06","PARTNERSHIP FIRM");
        biMap.put("07","BODY CORPORATE");
        biMap.put("08","TRUST");
        biMap.put("09","SOCIETY");
        biMap.put("10","OTHERS");
        biMap.put("11","NRI-OTHERS");
        biMap.put("12","DFI");
        biMap.put("13","SOLE PROPRIETORSHIP");
        biMap.put("21","NRI - REPATRIABLE (NRE)");
        biMap.put("22","OCB");
        biMap.put("23","FII");
        biMap.put("24","NRI - REPATRIABLE (NRO)");
        biMap.put("25","OVERSEAS CORP. BODY - OTHERS");
        biMap.put("26","NRI CHILD");
        biMap.put("27","NRI - HUF (NRO)");
        biMap.put("28","NRI - MINOR (NRO)");
        biMap.put("29","NRI - HUF (NRE)");
        biMap.put("31","PROVIDEND FUND");
        biMap.put("32","SUPER ANNUATION FUND");
        biMap.put("33","GRATUITY FUND");
        biMap.put("34","PENSION FUND");
        biMap.put("36","MUTUAL FUNDS FOF SCHEMES");
        biMap.put("37","NPS TRUST");
        biMap.put("38","GLOBAL DEVELOPMENT NETWORK");
        biMap.put("39","FCRA");
        biMap.put("41","QFI - INDIVIDUAL");
        biMap.put("42","QFI - MINORS");
        biMap.put("43","QFI - CORPORATE");
        biMap.put("44","QFI - PENSION FUNDS");
        biMap.put("45","QFI - HEDGE FUNDS");
        biMap.put("46","QFI - MUTUAL FUNDS");
        biMap.put("47","LLP");
        biMap.put("48","NON-PROFIT ORGANIZATION [NPO]");
        biMap.put("51","PUBLIC LIMITED COMPANY");
        biMap.put("52","PRIVATE LIMITED COMPANY");
        biMap.put("53","UNLISTED COMPANY");
        biMap.put("54","MUTUAL FUNDS");
        biMap.put("55","FPI - CATEGORY I");
        biMap.put("56","FPI - CATEGORY II");
        biMap.put("57","FPI - CATEGORY III");
        biMap.put("58","FINANCIAL INSTITUTIONS");
        biMap.put("59","BODY OF INDIVIDUALS");
        biMap.put("60","INSURANCE COMPANY");
        biMap.put("61","OCI - REPATRIATION");
        biMap.put("62","OCI - NON REPATRIATION");
        biMap.put("70","PERSON OF INDIAN ORIGIN");
        biMap.put("72","GOVERNMENT BODY");
        biMap.put("73","DEFENCE ESTABLISHMENT");
        biMap.put("74","NON - GOVERNMENT ORGANISATION");
        biMap.put("75","BANK/ CO-OPERATIVE BANK");
        biMap.put("76","ARTIFICIAL JURIDICAL PERSON");
        biMap.put("77","SEAFARER NRE");
        biMap.put("78","SEAFARER NRO");
        biMap.put("79","LOCAL AUTHORITY");

        return biMap;
    }

    public static String getOccupationTypeByCode(String code)
    {
        switch (code) {
            case "01": return "B"; // BUSINESS
            case "02": return "S"; // SERVICE
            case "03": return "S"; // PROFESSIONAL
            case "04": return "S"; // AGRICULTURIST
            case "05": return "O"; // RETIRED
            case "06": return "O"; // HOUSEWIFE
            case "07": return "O"; // STUDENT
            case "08": return "O"; // OTHERS
            case "09": return "S"; // DOCTOR
            case "41": return "S"; // PRIVATE SECTOR SERVICE
            case "42": return "S"; // PUBLIC SECTOR SERVICE
            case "43": return "B"; // FOREX DEALER
            case "44": return "S"; // GOVERNMENT SERVICE
            case "99": return "O"; // UNKNOWN / NOT APPLICABLE
            default: return "X";   // Not Categorized
        }
    }

    public static String getClientTaxStatusCode(String name) {
        return clientTaxStatusCodeOrName().inverse().get(name);
    }

    public static String getLogoByAmcNameOrSchemeName(String amc_or_scheme_name)
    {
        amc_or_scheme_name = checkParem(amc_or_scheme_name);
        if (StringHelper.isEmpty(amc_or_scheme_name)) return "empty.png";
//        System.out.println("bbbb" + amc_or_scheme_name);
        String firstWord = amc_or_scheme_name.trim().split("\\s+")[0].toLowerCase();

        Map<String, String> LOGO_MAP = Map.ofEntries(
                Map.entry("axis", "axis.png"),
                Map.entry("bandhan", "bandhan.png"),
                Map.entry("baroda", "bnp.png"),
                Map.entry("aditya", "birla.png"),
                Map.entry("absl", "birla.png"),
                Map.entry("bnp", "bnp.png"),
                Map.entry("boi", "boi.png"),
                Map.entry("bank", "boi.png"),
                Map.entry("canara", "canara.png"),
                Map.entry("dsp", "dsp.png"),
                Map.entry("franklin", "franklin.png"),
                Map.entry("templeton", "franklin.png"),
                Map.entry("hdfc", "hdfc.png"),
                Map.entry("icici", "icici.png"),
                Map.entry("idbi", "idbi.png"),
                Map.entry("jm", "jm.png"),
                Map.entry("kotak", "kotak.png"),
                Map.entry("lic", "lic.png"),
                Map.entry("principal", "principal.png"),
                Map.entry("nippon", "nippon.png"),
                Map.entry("cpse", "nippon.png"),
                Map.entry("sbi", "sbi.png"),
                Map.entry("sundaram", "sundaram.png"),
                Map.entry("tata", "tata.png"),
                Map.entry("uti", "uti.png"),
                Map.entry("pgim", "pgim.png"),
                Map.entry("edelweiss", "edelweiss.png"),
                Map.entry("hsbc", "hsbc.png"),
                Map.entry("invesco", "invesco.png"),
                Map.entry("l&t", "lt.png"),
                Map.entry("lt", "lt.png"),
                Map.entry("mahindra", "mahindra.png"),
                Map.entry("mirae", "mirae.png"),
                Map.entry("motilal", "motilal.png"),
                Map.entry("essel", "essel.png"),
                Map.entry("navi", "navi.png"),
                Map.entry("quantum", "quantum.png"),
                Map.entry("quant", "quant.png"),
                Map.entry("taurus", "taurus.png"),
                Map.entry("union", "union.png"),
                Map.entry("360", "360_one.png"),
                Map.entry("ppfas", "ppfas.png"),
                Map.entry("parag", "ppfas.png"),
                Map.entry("shriram", "shriram.png"),
                Map.entry("yes", "yes.png"),
                Map.entry("iti", "iti.png"),
                Map.entry("nj", "nj.png"),
                Map.entry("whiteoak", "whiteoak.png"),
                Map.entry("woc", "whiteoak.png"),
                Map.entry("samco", "samco.png"),
                Map.entry("helios", "helios.png"),
                Map.entry("angel", "angelone.png"),
                Map.entry("old", "old-bridge.png"),
                Map.entry("bajaj", "bajaj.png"),
                Map.entry("groww", "groww.png"),
                Map.entry("zerodha", "zerodha.png"),
                Map.entry("unifi", "unifi.png"),
                Map.entry("trust", "trust.png"),
                Map.entry("the", "wealth.png"),
                Map.entry("altiva", "edelweiss.png"),
                Map.entry("abakkus","abacus.png")
        );

        return LOGO_MAP.getOrDefault(firstWord, "empty.png");
    }

    private static final Map<String, String> stateCodeMap = new HashMap<>();

    public static String getFatcaCode(String stateMasterCode) {
        String mapped = stateCodeMap.get(stateMasterCode);
        return (mapped != null) ? mapped : stateMasterCode;
    }

    public static String getVendorImage(String vendor)
    {
        String path = "";

        if(vendor.equalsIgnoreCase("NSE"))
        {
            path = "nse.png";
        }else if(vendor.equalsIgnoreCase("BSE"))
        {
            path =  "bse.png";
        }else if(vendor.equalsIgnoreCase("MFU"))
        {
            path = "mfu.png";
        }else
        {
            path = "";
        }

        return path;
    }

    public static boolean isUrlEncoded(String value) {
        return value != null && value.matches(".*%[0-9a-fA-F]{2}.*");
    }

    public static List<String> KarvyNeutralDividendTransactionType()
    {
        List<String> list = new ArrayList<String>();
        list.add("Gross Dividend");
        list.add("Gross Dividend Rejection");
        list.add("Gross Dividend Rejection Reversal");
        list.add("Dividend Sweep Out");
        list.add("Dividend Sweep Out Rej");
        list.add("Dividend Sweep Out Rej.");
        return list;
    }

    public static List<InvestorTransactionCamsDto> removeCamsMinusTransaction(List<InvestorTransactionCamsDto> list)
    {
        try
        {


            Double unit = 0.0;

            List<InvestorTransactionCamsDto> units_minus_list = list.stream().filter(trxn -> trxn.getUnits().compareTo(unit) < 0).collect(Collectors.toList());


            for (InvestorTransactionCamsDto cams : units_minus_list)
            {
                List<InvestorTransactionCamsDto> remove_list = new ArrayList<InvestorTransactionCamsDto>();

                Date date = cams.getTraddate();
                Double units = cams.getUnits();
                String transaction_type = cams.getTrxn_type_();

                Double units2 = units * -1;

                if(transaction_type.equalsIgnoreCase("SIP Rejection"))
                {
                    InvestorTransactionCamsDto cams2 = list.stream().filter(trxn -> trxn.getTraddate().compareTo(date) == 0 && trxn.getUnits().compareTo(units2) == 0 && (transaction_type.equalsIgnoreCase("Fresh Purchase Systematic") || transaction_type.equalsIgnoreCase("Additional Purchase Systematic"))).findFirst().orElse(null);
                    if(cams2 == null)
                    {

                    }else
                    {
                        remove_list.add(cams);
                        remove_list.add(cams2);

                        list.removeIf(x -> remove_list.contains(x));


                    }
                }else
                {
                    InvestorTransactionCamsDto cams2 = list.stream().filter(trxn -> trxn.getTraddate().compareTo(date) == 0 && trxn.getUnits().compareTo(units2) == 0 && trxn.getTrxn_type_().equalsIgnoreCase(transaction_type)).findFirst().orElse(null);
                    if(cams2 == null)
                    {

                    }else
                    {
                        remove_list.add(cams);
                        remove_list.add(cams2);

                        list.removeIf(x -> remove_list.contains(x));


                    }
                }
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
        return list;
    }


    public static List<InvestorTransactionKarvyDto> removeKarvyMinusTransaction(List<InvestorTransactionKarvyDto> list)
    {
        try
        {


            Double unit = 0.0;

            List<InvestorTransactionKarvyDto> units_minus_list = list.stream().filter(trxn -> trxn.getUnits().compareTo(unit) < 0).collect(Collectors.toList());


            for (InvestorTransactionKarvyDto karvy : units_minus_list)
            {
                List<InvestorTransactionKarvyDto> remove_list = new ArrayList<InvestorTransactionKarvyDto>();

                Date date = karvy.getTransaction_date();
                Double units = karvy.getUnits();
                String transaction_type = karvy.getTransaction_description();

                Double units2 = units * -1;

                InvestorTransactionKarvyDto karvy2 = list.stream().filter(trxn -> trxn.getTransaction_date().compareTo(date) == 0 && trxn.getUnits().compareTo(units2) == 0).findFirst().orElse(null);

                if(karvy2 == null)
                {
                    String ihno = karvy.getIhno();

                    Double total_units = list.stream().filter(trxn -> trxn.getTransaction_date().compareTo(date) == 0 && trxn.getIhno().equalsIgnoreCase(ihno) && trxn.getUnits() > 0).mapToDouble(InvestorTransactionKarvyDto::getUnits).sum();

                    if(total_units.compareTo(units2) == 0) {
                        List<InvestorTransactionKarvyDto> karvy_sub_list = list.stream().filter(trxn -> trxn.getTransaction_date().compareTo(date) == 0 && trxn.getIhno().equalsIgnoreCase(ihno) && trxn.getUnits() > 0).collect(Collectors.toList());

                        remove_list.add(karvy);
                        remove_list.addAll(karvy_sub_list);

                        list.removeIf(x -> remove_list.contains(x));


                    }

                }else
                {
                    remove_list.add(karvy);
                    remove_list.add(karvy2);

                    list.removeIf(x -> remove_list.contains(x));


                }
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
        return list;
    }

    public static String getFullRequestUrl(HttpServletRequest request) {
        StringBuffer requestURL = request.getRequestURL(); // base URL
        String queryString = request.getQueryString();     // ?param1=value1&param2=value2

        if (queryString != null) {
            requestURL.append("?").append(queryString);
        }

        return requestURL.toString();
    }

    public static HttpHeaders getHttpHeaders(String nse_memberid, String base64Encoded) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("memberId", nse_memberid);
        headers.set("Authorization", "Basic "+ base64Encoded);
        headers.set("User-Agent", "PostmanRuntime/7.43.3");
        headers.set("Accept-Encoding", "gzip, deflate, br");
        headers.set("Accept-Language", "en-US");
        headers.set("Connection", "keep-alive");
        headers.set("Referer", "");
        return headers;
    }

    public static String getAMCNameByNSECompanyName(String companyName){

        HashMap<String,String> amc_name_map = new HashMap<String,String>();
        amc_name_map.put("360_ONE_MUTUALFUND_MF","360 ONE Mutual Fund");
        amc_name_map.put("AXISMUTUALFUND_MF","Axis Mutual Fund");
        amc_name_map.put("BAJAJFINSERV_MF","Bajaj Finserv Mutual Fund");
        amc_name_map.put("BANDHANMUTUALFUND_MF","Bandhan Mutual Fund");
        amc_name_map.put("BANKOFINDIAMUTUALFUND_MF","Bank of India Mutual Fund");
        amc_name_map.put("BARODABNPPARIBASMUTUALFUND_MF","Baroda BNP Paribas Mutual Fund");
        amc_name_map.put("BIRLASUNLIFEMUTUALFUND_MF","Aditya Birla Sun Life Mutual Fund");
        amc_name_map.put("CANARAROBECOMUTUALFUND_MF","Canara Robeco Mutual Fund");
        amc_name_map.put("DSP_MF","DSP Mutual Fund");
        amc_name_map.put("EDELWEISSMUTUALFUND_MF","Edelweiss Mutual Fund");
        amc_name_map.put("FRANKLINTEMPLETON","Franklin Templeton Mutual Fund");
        amc_name_map.put("GROWWMUTUALFUND_MF","Groww Mutual Fund");
        amc_name_map.put("HDFCMUTUALFUND_MF","HDFC Mutual Fund");
        amc_name_map.put("HSBCMUTUALFUND_MF","HSBC Mutual Fund");
        amc_name_map.put("ICICIPRUDENTIALMUTUALFUND_MF","ICICI Prudential Mutual Fund");
        amc_name_map.put("INVESCOMUTUALFUND_MF","Invesco Mutual Fund");
        amc_name_map.put("ITIMUTUALFUND_MF","ITI Mutual Fund");
        amc_name_map.put("JMFINANCIALMUTUALFUND_MF","JM Financial Mutual Fund");
        amc_name_map.put("KOTAKMAHINDRAMF","Kotak Mahindra Mutual Fund");
        amc_name_map.put("LICMUTUALFUND_MF","LIC Mutual Fund");
        amc_name_map.put("MAHINDRAMANULIFEMUTUALFUND_MF","Mahindra Manulife Mutual Fund");
        amc_name_map.put("MIRAEASSET","Mirae Asset Mutual Fund");
        amc_name_map.put("MOTILALOSWAL_MF","Motilal Oswal Mutual Fund");
        amc_name_map.put("NAVIMUTUALFUND_MF","Navi Mutual Fund");
        amc_name_map.put("NIPPONINDIAMUTUALFUND_MF","Nippon India Mutual Fund");
        amc_name_map.put("NJMUTUALFUND_MF","NJ Mutual Fund");
        amc_name_map.put("PGIMINDIAMUTUALFUND_MF","PGIM India Mutual Fund");
        amc_name_map.put("PPFAS_MF","PPFAS Mutual Fund");
        amc_name_map.put("QUANTMUTUALFUND_MF","Quant Mutual Fund");
        amc_name_map.put("QUANTUMMUTUALFUND_MF","Quantum Mutual Fund");
        amc_name_map.put("SAMCOMUTUALFUND_MF","Samco Mutual Fund");
        amc_name_map.put("SBIMUTUALFUND_MF","SBI Mutual Fund");
        amc_name_map.put("SHRIRAMMUTUALFUND_MF","Shriram Mutual Fund");
        amc_name_map.put("SUNDARAMMUTUALFUND_MF","Sundaram Mutual Fund");
        amc_name_map.put("TATAMUTUALFUND_MF","Tata Mutual Fund");
        amc_name_map.put("TAURUSMUTUALFUND_MF","Taurus Mutual Fund");
        amc_name_map.put("TRUSTMUTUALFUND_MF","Trust Mutual Fund");
        amc_name_map.put("UNIONMUTUALFUND_MF","Union Mutual Fund");
        amc_name_map.put("UTIMUTUALFUND_MF","UTI Mutual Fund");
        amc_name_map.put("WHITEOAKCAPITALMUTUALFUND_MF","WhiteOak Capital Mutual Fund");
        amc_name_map.put("YESMUTUALFUND_MF","YES Mutual Fund");
        amc_name_map.put("ZERODHAMUTUALFUND_MF","Zerodha Mutual Fund");
        amc_name_map.put("HELIOSMUTUALFUND_MF","Helios Mutual Fund");
        amc_name_map.put("OLDBRIDGEMUTUALFUND_MF","Old Bridge Mutual Fund");
        amc_name_map.put("ANGELONEMUTUALFUND_MF","Angel One Mutual Fund");
        amc_name_map.put("CAPITALMINDMUTUALFUND_MF","Capitalmind Mutual Fund");
        amc_name_map.put("CAPITALMIND_MF","Capitalmind Mutual Fund");
        amc_name_map.put("UNIFIMUTUALFUND_MF","Unifi Mutual Fund");
        amc_name_map.put("JIOBLACKROCKMUTUALFUND_MF","Jio BlackRock Mutual Fund");
        amc_name_map.put("WEALTHCOMPANY_MF","The Wealth Company Mutual Fund");
        amc_name_map.put("ABAKKUS_MF","Abakkus Mutual Fund");
        amc_name_map.put("ALPHAGREPMUTUALFUND_MF","AlphaGrep Mutual Fund");

        amc_name_map.put("EDELWEISSMMF_SIF","Edelweiss Mutual Fund SIF");
        amc_name_map.put("QSIF","Quant Mutual Fund SIF");
        amc_name_map.put("SBIMF_SIF","SBI Mutual Fund SIF");
        amc_name_map.put("TATAMF_SIF","Tata Mutual Fund SIF");
        amc_name_map.put("ITIMF_SIF","ITI Mutual Fund SIF");
        amc_name_map.put("BANDHANMF_SIF","Bandhan Mutual Fund SIF");
        amc_name_map.put("ICICIMF_SIF","ICICI Prudential Mutual Fund SIF");
        amc_name_map.put("360ONEMF_SIF","360 ONE Mutual Fund SIF");
        amc_name_map.put("BIRLAMF_SIF","Aditya Birla Sun Life Mutual Fund SIF");
        amc_name_map.put("CHOICEAMC_MF","Choice Mutual Fund");
        amc_name_map.put("FRANKLINMF_SIF","Franklin Templeton Mutual Fund SIF");
        amc_name_map.put("UNIONMF_SIF","Union Mutual Fund SIF");
        amc_name_map.put("WEALTHMF_SIF","The Wealth Company Mutual Fund SIF");
        amc_name_map.put("MIRAEMF_SIF","Mirae Asset Mutual Fund SIF");
        amc_name_map.put("JIOMF_SIF","Jio BlackRock Mutual Fund SIF");
        amc_name_map.put("INVESCOMF_SIF","Invesco Mutual Fund SIF");


        String amc_name = amc_name_map.get(companyName);

        return amc_name;
    }
    public static String getSIFAmcCode(String amcname) {

        HashMap<String, String> amc_name_map = new HashMap<String, String>();
        amc_name_map.put("Edelweiss Mutual Fund (SIF)", "EDELWEISSMMF_SIF");
        amc_name_map.put("Quant Mutual Fund (SIF)", "QSIF");
        amc_name_map.put("SBI Mutual Fund (SIF)", "SBIMF_SIF");
        amc_name_map.put("Bandhan Mutual Fund SIF","BANDHANMF_SIF");
        amc_name_map.put("ICICI Prudential Mutual Fund SIF","ICICIMF_SIF");

        String amc_name = amc_name_map.get(amcname);

        return amc_name;

    }

    public static String getCountrycode(String companyName) {

        HashMap<String, String> amc_name_map = new HashMap<String, String>();
        amc_name_map.put("AFGHANISTAN", "001");
        amc_name_map.put("ALAND ISLANDS", "002");
        amc_name_map.put("ALBANIA", "003");
        amc_name_map.put("ALGERIA", "004");
        amc_name_map.put("AMERICAN SAMOA", "005");
        amc_name_map.put("ANDORRA", "006");
        amc_name_map.put("ANGOLA", "007");
        amc_name_map.put("ANGUILLA", "008");
        amc_name_map.put("ANTARCTICA", "009");
        amc_name_map.put("ANTIGUA AND BARBUDA", "010");
        amc_name_map.put("ARGENTINA", "011");
        amc_name_map.put("ARMENIA", "012");
        amc_name_map.put("ARUBA", "013");
        amc_name_map.put("AUSTRALIA", "014");
        amc_name_map.put("AUSTRIA", "015");
        amc_name_map.put("AZERBAIJAN", "016");
        amc_name_map.put("BAHAMAS", "017");
        amc_name_map.put("BAHRAIN", "018");
        amc_name_map.put("BANGLADESH", "019");
        amc_name_map.put("BARBADOS", "020");
        amc_name_map.put("BELARUS", "021");
        amc_name_map.put("BELGIUM", "022");
        amc_name_map.put("BELIZE", "023");
        amc_name_map.put("BENIN", "024");
        amc_name_map.put("BERMUDA", "025");
        amc_name_map.put("BHUTAN", "026");
        amc_name_map.put("BOLIVIA", "027");
        amc_name_map.put("BOSNIA AND HERZEGOVINA", "028");
        amc_name_map.put("BOTSWANA", "029");
        amc_name_map.put("BOUVET ISLAND", "030");
        amc_name_map.put("BRAZIL", "031");
        amc_name_map.put("BRITISH INDIAN OCEAN TERRITORY", "032");
        amc_name_map.put("BRUNEI DARUSSALAM", "033");
        amc_name_map.put("BULGARIA", "034");
        amc_name_map.put("BURKINA FASO", "035");
        amc_name_map.put("BURUNDI", "036");
        amc_name_map.put("CAMBODIA", "037");
        amc_name_map.put("CAMEROON", "038");
        amc_name_map.put("CANADA", "039");
        amc_name_map.put("CAPE VERDE", "040");
        amc_name_map.put("CAYMAN ISLANDS", "041");
        amc_name_map.put("CENTRAL AFRICAN REPUBLIC", "042");
        amc_name_map.put("CHAD", "043");
        amc_name_map.put("CHILE", "044");
        amc_name_map.put("CHINA", "045");
        amc_name_map.put("CHRISTMAS ISLAND", "046");
        amc_name_map.put("COCOS (KEELING) ISLANDS", "047");
        amc_name_map.put("COLOMBIA", "048");
        amc_name_map.put("COMOROS", "049");
        amc_name_map.put("CONGO", "050");
        amc_name_map.put("CONGO, THE DEMOCRATIC REPUBLIC OF THE", "051");
        amc_name_map.put("COOK ISLANDS", "052");
        amc_name_map.put("COSTA RICA", "053");
        amc_name_map.put("COTE DIVOIRE", "054");
        amc_name_map.put("CROATIA", "055");
        amc_name_map.put("CUBA", "056");
        amc_name_map.put("CYPRUS", "057");
        amc_name_map.put("CZECH REPUBLIC", "058");
        amc_name_map.put("DENMARK", "059");
        amc_name_map.put("DJIBOUTI", "060");
        amc_name_map.put("DOMINICA", "061");
        amc_name_map.put("DOMINICAN REPUBLIC", "062");
        amc_name_map.put("ECUADOR", "063");
        amc_name_map.put("EGYPT", "064");
        amc_name_map.put("EL SALVADOR", "065");
        amc_name_map.put("EQUATORIAL GUINEA", "066");
        amc_name_map.put("ERITREA", "067");
        amc_name_map.put("ESTONIA", "068");
        amc_name_map.put("ETHIOPIA", "069");
        amc_name_map.put("FALKLAND ISLANDS (MALVINAS)", "070");
        amc_name_map.put("FAROE ISLANDS", "071");
        amc_name_map.put("FIJI", "072");
        amc_name_map.put("FINLAND", "073");
        amc_name_map.put("FRANCE", "074");
        amc_name_map.put("FRENCH GUIANA", "075");
        amc_name_map.put("FRENCH POLYNESIA", "076");
        amc_name_map.put("FRENCH SOUTHERN TERRITORIES", "077");
        amc_name_map.put("GABON", "078");
        amc_name_map.put("GAMBIA", "079");
        amc_name_map.put("GEORGIA", "080");
        amc_name_map.put("GERMANY", "081");
        amc_name_map.put("GHANA", "082");
        amc_name_map.put("GIBRALTAR", "083");
        amc_name_map.put("GREECE", "084");
        amc_name_map.put("GREENLAND", "085");
        amc_name_map.put("GRENADA", "086");
        amc_name_map.put("GUADELOUPE", "087");
        amc_name_map.put("GUAM", "088");
        amc_name_map.put("GUATEMALA", "089");
        amc_name_map.put("GUERNSEY", "090");
        amc_name_map.put("GUINEA", "091");
        amc_name_map.put("GUINEA-BISSAU", "092");
        amc_name_map.put("GUYANA", "093");
        amc_name_map.put("HAITI", "094");
        amc_name_map.put("HEARD ISLAND AND MCDONALD ISLANDS", "095");
        amc_name_map.put("HOLY SEE (VATICAN CITY STATE)", "096");
        amc_name_map.put("HONDURAS", "097");
        amc_name_map.put("HONG KONG", "098");
        amc_name_map.put("HUNGARY", "099");
        amc_name_map.put("ICELAND", "100");
        amc_name_map.put("INDIA", "101");
        amc_name_map.put("INDONESIA", "102");
        amc_name_map.put("IRAN, ISLAMIC REPUBLIC OF", "103");
        amc_name_map.put("IRAQ", "104");
        amc_name_map.put("IRELAND", "105");
        amc_name_map.put("ISLE OF MAN", "106");
        amc_name_map.put("ISRAEL", "107");
        amc_name_map.put("ITALY", "108");
        amc_name_map.put("JAMAICA", "109");
        amc_name_map.put("JAPAN", "110");
        amc_name_map.put("JERSEY", "111");
        amc_name_map.put("JORDAN", "112");
        amc_name_map.put("KAZAKHSTAN", "113");
        amc_name_map.put("KENYA", "114");
        amc_name_map.put("KIRIBATI", "115");
        amc_name_map.put("KOREA, DEMOCRATIC PEOPLES REPUBLIC OF", "116");
        amc_name_map.put("KOREA, REPUBLIC OF", "117");
        amc_name_map.put("KUWAIT", "118");
        amc_name_map.put("KYRGYZSTAN", "119");
        amc_name_map.put("LAO PEOPLES DEMOCRATIC REPUBLIC", "120");
        amc_name_map.put("LATVIA", "121");
        amc_name_map.put("LEBANON", "122");
        amc_name_map.put("LESOTHO", "123");
        amc_name_map.put("LIBERIA", "124");
        amc_name_map.put("LIBYAN ARAB JAMAHIRIYA", "125");
        amc_name_map.put("LIECHTENSTEIN", "126");
        amc_name_map.put("LITHUANIA", "127");
        amc_name_map.put("LUXEMBOURG", "128");
        amc_name_map.put("MACAO", "129");
        amc_name_map.put("MACEDONIA, THE FORMER YUGOSLAV REPUBLIC OF", "130");
        amc_name_map.put("MADAGASCAR", "131");
        amc_name_map.put("MALAWI", "132");
        amc_name_map.put("MALAYSIA", "133");
        amc_name_map.put("MALDIVES", "134");
        amc_name_map.put("MALI", "135");
        amc_name_map.put("MALTA", "136");
        amc_name_map.put("MARSHALL ISLANDS", "137");
        amc_name_map.put("MARTINIQUE", "138");
        amc_name_map.put("MAURITANIA", "139");
        amc_name_map.put("MAURITIUS", "140");
        amc_name_map.put("MAYOTTE", "141");
        amc_name_map.put("MEXICO", "142");
        amc_name_map.put("MICRONESIA, FEDERATED STATES OF", "143");
        amc_name_map.put("MOLDOVA, REPUBLIC OF", "144");
        amc_name_map.put("MONACO", "145");
        amc_name_map.put("MONGOLIA", "146");
        amc_name_map.put("MONTSERRAT", "147");
        amc_name_map.put("MOROCCO", "148");
        amc_name_map.put("MOZAMBIQUE", "149");
        amc_name_map.put("MYANMAR", "150");
        amc_name_map.put("NAMIBIA", "151");
        amc_name_map.put("NAURU", "152");
        amc_name_map.put("NEPAL", "153");
        amc_name_map.put("NETHERLANDS", "154");
        amc_name_map.put("NETHERLANDS ANTILLES", "155");
        amc_name_map.put("NEW CALEDONIA", "156");
        amc_name_map.put("NEW ZEALAND", "157");
        amc_name_map.put("NICARAGUA", "158");
        amc_name_map.put("NIGER", "159");
        amc_name_map.put("NIGERIA", "160");
        amc_name_map.put("NIUE", "161");
        amc_name_map.put("NORFOLK ISLAND", "162");
        amc_name_map.put("NORTHERN MARIANA ISLANDS", "163");
        amc_name_map.put("NORWAY", "164");
        amc_name_map.put("OMAN", "165");
        amc_name_map.put("PAKISTAN", "166");
        amc_name_map.put("PALAU", "167");
        amc_name_map.put("PALESTINIAN TERRITORY, OCCUPIED", "168");
        amc_name_map.put("PANAMA", "169");
        amc_name_map.put("PAPUA NEW GUINEA", "170");
        amc_name_map.put("PARAGUAY", "171");
        amc_name_map.put("PERU", "172");
        amc_name_map.put("PHILIPPINES", "173");
        amc_name_map.put("PITCAIRN", "174");
        amc_name_map.put("POLAND", "175");
        amc_name_map.put("PORTUGAL", "176");
        amc_name_map.put("PUERTO RICO", "177");
        amc_name_map.put("QATAR", "178");
        amc_name_map.put("REUNION", "179");
        amc_name_map.put("ROMANIA", "180");
        amc_name_map.put("RUSSIAN FEDERATION", "181");
        amc_name_map.put("RWANDA", "182");
        amc_name_map.put("SAINT HELENA", "183");
        amc_name_map.put("SAINT KITTS AND NEVIS", "184");
        amc_name_map.put("SAINT LUCIA", "185");
        amc_name_map.put("SAINT PIERRE AND MIQUELON", "186");
        amc_name_map.put("SAINT VINCENT AND THE GRENADINES", "187");
        amc_name_map.put("SAMOA", "188");
        amc_name_map.put("SAN MARINO", "189");
        amc_name_map.put("SAO TOME AND PRINCIPE", "190");
        amc_name_map.put("SAUDI ARABIA", "191");
        amc_name_map.put("SENEGAL", "192");
        amc_name_map.put("SERBIA AND MONTENEGRO", "193");
        amc_name_map.put("SEYCHELLES", "194");
        amc_name_map.put("SIERRA LEONE", "195");
        amc_name_map.put("SINGAPORE", "196");
        amc_name_map.put("SLOVAKIA", "197");
        amc_name_map.put("SLOVENIA", "198");
        amc_name_map.put("SOLOMON ISLANDS", "199");
        amc_name_map.put("SOMALIA", "200");
        amc_name_map.put("SOUTH AFRICA", "201");
        amc_name_map.put("SOUTH GEORGIA AND THE SOUTH SANDWICH ISLANDS", "202");
        amc_name_map.put("SPAIN", "203");
        amc_name_map.put("SRI LANKA", "204");
        amc_name_map.put("SUDAN", "205");
        amc_name_map.put("SURINAME", "206");
        amc_name_map.put("SVALBARD AND JAN MAYEN", "207");
        amc_name_map.put("SWAZILAND", "208");
        amc_name_map.put("SWEDEN", "209");
        amc_name_map.put("SWITZERLAND", "210");
        amc_name_map.put("SYRIAN ARAB REPUBLIC", "211");
        amc_name_map.put("TAIWAN, PROVINCE OF CHINA", "212");
        amc_name_map.put("TAJIKISTAN", "213");
        amc_name_map.put("TANZANIA, UNITED REPUBLIC OF", "214");
        amc_name_map.put("THAILAND", "215");
        amc_name_map.put("TIMOR-LESTE", "216");
        amc_name_map.put("TOGO", "217");
        amc_name_map.put("TOKELAU", "218");
        amc_name_map.put("TONGA", "219");
        amc_name_map.put("TRINIDAD AND TOBAGO", "220");
        amc_name_map.put("TUNISIA", "221");
        amc_name_map.put("TURKEY", "222");
        amc_name_map.put("TURKMENISTAN", "223");
        amc_name_map.put("TURKS AND CAICOS ISLANDS", "224");
        amc_name_map.put("TUVALU", "225");
        amc_name_map.put("UGANDA", "226");
        amc_name_map.put("UKRAINE", "227");
        amc_name_map.put("UNITED ARAB EMIRATES", "228");
        amc_name_map.put("UNITED KINGDOM", "229");
        amc_name_map.put("UNITED STATES OF AMERICA", "230");
        amc_name_map.put("UNITED STATES MINOR OUTLYING ISLANDS", "231");
        amc_name_map.put("URUGUAY", "232");
        amc_name_map.put("UZBEKISTAN", "233");
        amc_name_map.put("VANUATU", "234");
        amc_name_map.put("VENEZUELA", "235");
        amc_name_map.put("VIET NAM", "236");
        amc_name_map.put("VIRGIN ISLANDS, BRITISH", "237");
        amc_name_map.put("VIRGIN ISLANDS, U.S.", "238");
        amc_name_map.put("WALLIS AND FUTUNA", "239");
        amc_name_map.put("WESTERN SAHARA", "240");
        amc_name_map.put("YEMEN", "241");
        amc_name_map.put("ZAMBIA", "242");
        amc_name_map.put("ZIMBABWE", "243");

        String amc_name = amc_name_map.get(companyName);

        return amc_name;
    }

    private static final String[] DATE_PATTERNS = {
            "dd-MM-yyyy",
            "dd/MM/yyyy",
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "dd-MMM-yyyy"
    };

    public static String normalizeDob(String inputDate) throws ParseException {
        if (inputDate == null || inputDate.trim().isEmpty()) {
            return null;
        }

        Date parsedDate = null;

        for (String pattern : DATE_PATTERNS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                sdf.setLenient(false); // VERY IMPORTANT
                parsedDate = sdf.parse(inputDate);
                break; // stop once parsed
            } catch (ParseException ignored) {
            }
        }

        if (parsedDate == null) {
            throw new ParseException("Invalid DOB format: " + inputDate, 0);
        }

        SimpleDateFormat outputDf = new SimpleDateFormat("dd-MM-yyyy");
        return outputDf.format(parsedDate);
    }

    private static final String[] INPUT_FORMATS = {
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "dd-MMM-yyyy"
    };

    public static String normalizeDateToDdMmYyyy(String inputDate) throws ParseException {

        if (inputDate == null || inputDate.trim().isEmpty()) {
            return null;
        }

        Date parsedDate = null;

        for (String format : INPUT_FORMATS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                sdf.setLenient(false); // IMPORTANT
                parsedDate = sdf.parse(inputDate);
                break;
            } catch (ParseException ignored) {
            }
        }

        if (parsedDate == null) {
            throw new ParseException("Invalid date format: " + inputDate, 0);
        }

        SimpleDateFormat outputDf = new SimpleDateFormat("dd/MM/yyyy");
        return outputDf.format(parsedDate);
    }


}
