package com.nse.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "nse_occupation")
public class NseOccupation {
    @Id
    private Integer id;
    private String occupation_code;
    private String occupation_desc;
}
