package com.user.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "advisor_email_signature")
public class AdvisorEmailSignature
{
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "id")

    private Integer id;
    private String name;
    private String designation;
    private String company_name;
    private String mobile;
    private String email;
    private String website;
    private String address_1;
    private String address_2;
    private String logo;
    private String html_code;
    private String client_name;
    private Date created_date;
    private Date updated_date;
    private Boolean active;


}
