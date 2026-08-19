package com.nse.utils;

import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class FeignErrorHandler {

    public static ResponseEntity<?> handle(FeignException e, String serviceName, String notFoundMessage) {
        String body = e.contentUTF8();
        String msg = (body != null && !body.isEmpty()) ? body : e.getMessage();

        System.err.println("❌ Feign error from " + serviceName + " service: " + msg);

        if (e.status() == 404) {
            return NseUtils.commonResponse(notFoundMessage, HttpStatus.NOT_FOUND);
        } else if (e.status() == 503) {
            return NseUtils.commonResponse(serviceName + " is Unavailable.", HttpStatus.SERVICE_UNAVAILABLE);
        } else if (e.status() == 401) {
            return NseUtils.commonResponse("Unauthorized access to " + serviceName + ".", HttpStatus.UNAUTHORIZED);
        } else {
            return NseUtils.commonResponse("Unexpected error from " + serviceName + ": " + msg, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



}
