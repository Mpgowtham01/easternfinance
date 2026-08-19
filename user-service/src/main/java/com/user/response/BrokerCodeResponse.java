package com.user.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BrokerCodeResponse {
    @Schema(description = "HTTP status code with reason", example = "200")
    private Integer status= 0;

    @Schema(description = "Success Message", example = "Success")
    private String success = "";

    @Schema(description = "Detailed success message", example = "Investor information saved successfully.")
    private String message = "";

    public String client_name = "";
    public List<String> broker_code_list;

    private String broker_code;
    private String euin;
    private List<String> brokerCodeList;
    private int brokerCodeList_size;
    private List<String> euinList;
    private int euinList_size;

    // Constructor
    public void BrokerCodeResponseDto(String broker_code, String euin,
                                      List<String> brokerCodeList, List<String> euinList) {
        this.broker_code = broker_code;
        this.euin = euin;
        this.brokerCodeList = brokerCodeList;
        this.brokerCodeList_size = brokerCodeList.size();
        this.euinList = euinList;
        this.euinList_size = euinList.size();
    }

}
