package com.nse.dto.mf;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
public class MymfboxOnboardingDto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")

    private Integer id;
    private Integer user_id = 0;
    private Integer status = 0;
    private String client_name = "";
    private String vendor  = "";
    private String tax_status  = "";
    private String holding_nature  = "";
    private String inv_category  = "";
    private Boolean investor_info = false;
    private Boolean personal_info = false;
    private Boolean contact_info = false;
    private Boolean nri_info = false;
    private Boolean joint_holder_info = false;
    private Boolean nomiee_info = false;
    private Boolean bank_info = false;
    private Boolean signature_info = false;
    private Boolean has_nominee = false;
    private Boolean has_nri = false;
    private Boolean has_joint_holder = false;
    private Boolean is_all_steps_completed = false;
    private Boolean is_registration_completed = false;
    private Boolean is_multiple_registration = false;
    private Boolean nse_already_reg_diff_arn = false;
}
