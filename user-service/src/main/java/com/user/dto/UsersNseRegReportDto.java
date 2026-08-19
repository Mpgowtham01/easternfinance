package com.user.dto;

import lombok.Data;

@Data
public class UsersNseRegReportDto {
    private Integer id;
    private Integer user_id;
    private String name;
    private String pan;
    private String branch;
    private String rm_name;
    private String subbroker_name;
    private String iin_number;
    private String iin_created_date;
    private String form_updated_date;
    private String cheque_updated_date;
    private String iin_status;
    private Integer iin_active;
    private Integer mandate_active;
    private String transaction_date;
    private Integer multiple_reg;
    private String client_name;
}

