package com.nse.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "nse_applicable_income")
public class NseApplicableIncome {

    @Id
    private Integer id;
    private String app_income_code;
    private String app_income_desc;
}
