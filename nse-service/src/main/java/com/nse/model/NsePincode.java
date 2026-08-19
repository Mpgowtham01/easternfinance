package com.nse.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "nse_pincode")
@Data
public class NsePincode {
    @Id
    private Integer id;

    private String state_code;
    private String state_name;
    private String city;
    private String pincode;
}
