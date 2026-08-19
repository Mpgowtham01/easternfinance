package com.nse.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "nse_account_type")
public class NseAccountType {

    @Id
    private Integer id;
    private String account_type;
    private String description;
}
