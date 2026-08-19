package com.nse.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "nse_source_wealth")
public class NseSourceWealth {
    @Id
    private Integer id;
    private String code;
    private String source_of_wealth;
}
