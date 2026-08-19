package com.nse.utils;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class UniqueIDProvider {

    private static long _index = Long.MIN_VALUE;
    //private static final int DIGIT = 4;
    private static final String ALPHA_NUM = "0123456789";

    public static String generateID() {
        return Long.toHexString(System.nanoTime()) + Long.toHexString(_index++);
    }

    public static String generateUniquePin(int DIGIT) {
        StringBuffer sb = new StringBuffer(DIGIT);
        for (int i = 0; i < DIGIT; i++) {
            int ndx = (int) (Math.random() * ALPHA_NUM.length());
            sb.append(ALPHA_NUM.charAt(ndx));
        }
        return sb.toString();
    }

    public static String generateAuthCode(){
        String auth_code = UUID.randomUUID().toString();
        return auth_code;
    }

    public static double getDiffYears(Date date1, Date date2)
    {
        long diff = date2.getTime() - date1.getTime();
        long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
        double years = (double)days / (double)365;
        return years;
    }

}
