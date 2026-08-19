package com.nse.utils;

public class NseApiUrls
{
    public static String NSE_MAIN_DOMAIN = "https://www.nseinvest.com";

    public static String NSE_API_BASE_URL = NSE_MAIN_DOMAIN + "/nsemfdesk/api/v2";

    public static String CREATECUSTOMER = NSE_API_BASE_URL + "/registration/CLIENTCOMMON183";

    public static String UTRUPDATE = NSE_API_BASE_URL + "/transaction/UTRUPDATE";

    public static String Online2faReport_url = NSE_API_BASE_URL + "/reports/2FA";

    public static String clientMasterReport_url = NSE_API_BASE_URL + "/reports/client_master_report";

    public static String resendEmail_url = NSE_API_BASE_URL + "/registration/RESEND_COMM";

    public static String redemptionstatementReport_url = NSE_API_BASE_URL + "/reports/REDEMPTION_STATEMENT";

    public static String allotmentstatementReport_url = NSE_API_BASE_URL + "/reports/ALLOTMENT_STATEMENT";

    public static String clientAuthorizationReportApi_url = NSE_API_BASE_URL + "/reports/client_authorization";

    public static String PurchaseOrdersPaymentApi_url = NSE_API_BASE_URL + "/payments/purchase_payment";

    public static String nse_order_status = NSE_API_BASE_URL + "/reports/ORDER_STATUS";

    public static String nse_preview_order = NSE_API_BASE_URL + "/reports/PROV_ORDERS";

    public static String mandateStatusReport_url = NSE_API_BASE_URL + "/reports/MANDATE_STATUS";

    public static String xSipRegistrationServiceApi_url = NSE_API_BASE_URL + "/registration/product/XSIP";

    public static String sipLumpsumInvestment_url = NSE_API_BASE_URL + "/transaction/NORMAL";

    public static String switchOrderEntryService_url = NSE_API_BASE_URL + "/transaction/SWITCH";

    public static String stpRegistrationService_url = NSE_API_BASE_URL + "/registration/product/STP";

    public static String swpRegistrationService_url = NSE_API_BASE_URL + "/registration/product/SWP";

    public static String cancelSip = NSE_API_BASE_URL + "/cancellation/XSIP_CAN";

    public static String cancelStp = NSE_API_BASE_URL + "/cancellation/STP_CAN";

    public static String cancelSwp = NSE_API_BASE_URL + "/cancellation/SWP_CAN";

    public static String cacleDailySip = NSE_API_BASE_URL + "/cancellation/DAILY_XSIP_CAN";

    public static String xsipPauseService_url = NSE_API_BASE_URL + "/registration/XSIP_PAUSE";

    public static String mandate_url = NSE_API_BASE_URL + "/registration/product/MANDATE";

    public static String clientBankDetail_url = NSE_API_BASE_URL + "/registration/CLIENTBANKDTL";

    public static String normalOrderUrl = NSE_API_BASE_URL + "/transaction/NORMAL";

    public static final String MASTER_DOWNLOAD_URL = NSE_API_BASE_URL + "/reports/MASTER_DOWNLOAD";

    public static String order_lifecycle = NSE_API_BASE_URL + "/reports/order_lifecycle";

}
