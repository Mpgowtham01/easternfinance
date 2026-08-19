package com.nse.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "nse_bank")
public class NseBank {

    @Id
    private Integer id;
    private String bank_code;
    private String bank_name;
    private String last_modified_date;
    private String api_bank_name;
}
