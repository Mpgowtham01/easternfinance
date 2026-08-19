package com.user.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Onboarding Success Response")
public class OnboardingResponse {
    @Schema(description = "HTTP status code with reason", example = "200")
    private Integer status;

    @Schema(description = "Success Message", example = "Success")
    private String success;

    @Schema(description = "Detailed success message", example = "Investor information saved successfully.")
    private String message;


    private Integer user_id;
    private String client_name;
    private String vendor;
    private String title;
    private String logo;
    private String tax_status;
    private String holding_nature;
    private Boolean investor_info;
    private Boolean personal_info;
    private Boolean contact_info;
    private Boolean nri_info;
    private Boolean joint_holder_info;
    private Boolean nomiee_info;
    private Boolean bank_info;
    private Boolean signature_info;
    private Boolean has_nominee;
    private Boolean has_nri;
    private Boolean has_joint_holder;
    private Boolean is_all_steps_completed;
    private Boolean is_all_registration_completed;
    private Boolean is_multiple_registration;
    private List<BseNseMfuResponse> menu_list;

}
