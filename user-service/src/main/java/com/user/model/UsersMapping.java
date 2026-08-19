package com.user.model;

import jakarta.persistence.*;
import lombok.Data;

import java.beans.Transient;

@Entity
@Data
@Table(name = "users_mapping")
public class UsersMapping
{

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "id")

    Integer id;
    String mapping_name;
    String investor_name;
    String pan;
    String client_name;
    Integer user_id;
    Integer investor_id;
    String relation;
    Double aum;
    String mobile;
    String email;
    String rm_name;

    @Transient
    public String getMobile() {
        return mobile;
    }
    @Transient
    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
    @Transient
    public String getEmail() {
        return email;
    }
    @Transient
    public void setEmail(String email) {
        this.email = email;
    }
    @Transient
    public Double getAum() {
        return aum;
    }
    @Transient
    public void setAum(Double aum) {
        this.aum = aum;
    }
    @Transient
    public String getRm_name() {
        return rm_name;
    }
    @Transient
    public void setRm_name(String rm_name) {
        this.rm_name = rm_name;
    }


}