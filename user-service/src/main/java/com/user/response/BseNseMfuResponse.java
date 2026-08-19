package com.user.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "BseNseMfu Success Response")
public class BseNseMfuResponse {
    @Schema(description = "HTTP status code with reason", example = "200")
    private Integer status= 0;

    @Schema(description = "Success Message", example = "Success")
    private String success = "";

    @Schema(description = "Detailed success message", example = "Investor information saved successfully.")
    private String message = "";

    public String bse_nse_mfu = "";
    public String logo = "";
    public String tax_status = "";
    public String tax_status_code = "";
    public String title = "";
    public boolean completed = false;
    public boolean enabled = false;
    public boolean checkRequiredOrNot = false;
}
