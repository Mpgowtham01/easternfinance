package com.user.response;

public class StatusMessage
{
    public static String EFAPIKEY = "";

    public static String EFCLIENTNAME = "easternfinance";

    public static Integer SuccessCode = 200;
    public static String SuccessMessage = "Success";

    public static Integer FailureCode = 400;
    public static String FailureMessage = "Failure";

    public static Integer ExceptionCode = 500;
    public static String ExceptionMessage = "Exception";

    public static String Error = "Error";
    public static String Success = "Success";

    public static Integer NseSuccessCode = 0;
    public static String NseSuccessMessage = "Success";

    public static Integer NseFailureCode = 1;
    public static String NseFailureMessage = "Failure";

    public static Integer MfuSuccessCode = 0;
    public static String MfuSuccessMessage = "Success";

    public static Integer MfuFailureCode = 1;
    public static String MfuFailureMessage = "Failure";

    public static String ApiInvalidMessage = "Please provide the Valid API Key";
    public static String ClientNameInvalidMessage = "Please provide the Valid Client Name";
    public static String ClientNameEmptydMessage = "Please provide the Client Name";
    public static String EmptyUserId = "Please provide the user id";
    public static String InvalidUserId = "Please provide the Valid User Id";
    public static String UserNotAvailable = "User details not available.";
    public static String ExceptionAPIMessage = "Something went wrong, We have taken note of the issue. Be rest assured we will fix it as soon as possible";

    /*** Transaction Types ***/

    public static String FreshPurchase = "Fresh Purchase";
    public static String FreshPurchaseSystematic = "Fresh Purchase Systematic";
    public static String AdditionalPurchase = "Additional Purchase";
    public static String AdditionalPurchaseSystematic = "Additional Purchase Systematic";
    public static String SwitchIn = "Switch In";
    public static String DividendPayout = "Dividend Payout";

    public static String PartialSwitchOut = "Partial Switch Out";
    public static String FullSwitchOut = "Full Switch Out";
    public static String PartialRedemption = "Partial Redemption";
    public static String FullRedemption = "Full Redemption";
}
