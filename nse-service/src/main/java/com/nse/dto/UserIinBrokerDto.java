package com.nse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserIinBrokerDto {
    private String iin;
    private String brokerCode;
}
