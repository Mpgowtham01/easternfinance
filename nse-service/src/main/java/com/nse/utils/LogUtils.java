package com.nse.utils;

import jakarta.servlet.http.HttpServletRequest;

public class LogUtils
{
    public static String getApiRequest(HttpServletRequest request)
    {
        if (request == null) return "Unknown Request";

        StringBuilder url = new StringBuilder();
        try
        {
            String scheme = request.getScheme(); // http / https
            String serverName = request.getServerName();
            int serverPort = request.getServerPort();
            String requestURI = request.getRequestURI();
            String queryString = request.getQueryString();

            // Build full URL safely
            url.append(scheme != null ? scheme : "https").append("://");
            url.append(serverName != null ? serverName : "localhost");

            // include port if not standard
            if ((scheme != null && scheme.equals("http") && serverPort != 80) || (scheme != null && scheme.equals("https") && serverPort != 443))
            {
                url.append(":").append(serverPort);
            }

            url.append(requestURI != null ? requestURI : "");

            if (queryString != null && !queryString.isEmpty())
            {
                url.append("?").append(queryString);
            }

        } catch (Exception e)
        {
            return "Failed to get request URL: " + e.getMessage();
        }

        return url.toString();
    }

    public String getBearerToken(HttpServletRequest request)
    {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer "))
        {
            return authHeader.substring(7);
        }

        return "";
    }

    public static String getIpAddr(HttpServletRequest request)
    {
        if (request == null) return "";

        //is client behind something?
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null)
        {
            ipAddress = request.getRemoteAddr();
        }
        ipAddress = checkParem(ipAddress);
        return ipAddress;
    }

    public static String checkParem(String param)
    {
        if (param == null || param.trim().equalsIgnoreCase("null") || param.trim().equalsIgnoreCase("undefined"))
        {
            return "";
        }

        return param.trim();
    }
}
