package com.nse.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "nse_country")
public class NseCountry {
    @Id
    private Integer id;
    private String country_code;
    private String country_name;
}
