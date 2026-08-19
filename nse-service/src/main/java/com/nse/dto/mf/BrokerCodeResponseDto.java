package com.nse.dto.mf;

import lombok.Data;

import java.util.List;

@Data
public class BrokerCodeResponseDto
{
    private String broker_code;
    private String euin;
    private List<String> brokerCodeList;
    private int brokerCodeList_size;
    private List<String> euinList;
    private int euinList_size;

    // Constructor
    public BrokerCodeResponseDto(String broker_code, String euin,
                                 List<String> brokerCodeList, List<String> euinList)
    {
        this.broker_code = broker_code;
        this.euin = euin;
        this.brokerCodeList = brokerCodeList;
        this.brokerCodeList_size = brokerCodeList.size();
        this.euinList = euinList;
        this.euinList_size = euinList.size();
    }
}
