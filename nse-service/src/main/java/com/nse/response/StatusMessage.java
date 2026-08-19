package com.nse.response;

public class StatusMessage
{
    public static final Integer NseFailureCode = 400;
    public static final String NseFailureMessage = "Bad Request";

    public static Integer SuccessCode = 200;
    public static String SuccessMessage = "Success";

    public static String UserNotAvailable = "User details not available.";

    public static String ClientNameInvalidMessage = "Please provide the Valid Client Name";

    public static String EmptyUserId = "Please provide the user id";

    public static String ExceptionAPIMessage = "Something went wrong, We have taken note of the issue. Be rest assured we will fix it as soon as possible";
}
