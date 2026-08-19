package com.user.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "basket_details")
public class BasketDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")

    private Integer id;
    private String basket_name;
    private String scheme_amfi;
    private String scheme_amfi_code;
    private Integer amount;
    private String client_name;

}
