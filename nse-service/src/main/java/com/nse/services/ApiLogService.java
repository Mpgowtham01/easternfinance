package com.nse.services;

import com.nse.client.UserServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Fire-and-forget wrapper around the user-service /saveApiLog endpoint.
 *
 * API logging must never change the outcome of a transaction or onboarding call. Calling
 * userServiceClient.saveApiLog(..) directly is unsafe: the Feign call throws when user-service
 * is down, slow or returns a non-2xx, and because the log lines sit just before the controller's
 * return statements that exception would be swallowed by the controller's outer catch block -
 * turning an already-placed NSE order into a "Transaction failed. Please try again!" response
 * and inviting the investor to place a duplicate order.
 *
 * Every log failure is therefore absorbed here.
 */
@Service
public class ApiLogService
{
    @Autowired
    private UserServiceClient userServiceClient;

    public void saveApiLog(String token, String api_request_url, String api_request_body, Integer api_status,
                           String api_failure_message, String ip, String source)
    {
        try
        {
            userServiceClient.saveApiLog(token, api_request_url, api_request_body, api_status, api_failure_message, ip, source);
        }
        catch (Exception ex)
        {
            // never propagate: logging is not allowed to break the caller's response
            System.out.println("saveApiLog failed for url = " + api_request_url + " & ERROR = " + ex.getMessage());
        }
    }
}
