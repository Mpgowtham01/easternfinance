package com.user.response;

import com.user.dto.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User Success Response")
public class UserResponse
{
    @Schema(description = "HTTP status code with reason", example = "200")
    private Integer status;

    @Schema(description = "Success Message", example = "Success")
    private String success;

    @Schema(description = "Detailed success message", example = "Investor information saved successfully.")
    private String message;

    private InvestorInfoDTO invest_info;
    private PersonalInfoDTO personal_info;
    private NriInfoDTO nri_info;
    private ContactInfoDTO contact_info;
    private List<NomineeInfoDTO> nominee_info;
    private List<JointHolderInfoDTO> joint_holder_info;
    private BankInfoDTO bank_info;
}
