package com.user.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name="log")
public class Log
{
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    private Integer userid;
    private String username;
    private String mobile;
    private String title;
    private String description;
    private String content;
    private Date logtime;
    private String ip;
    private String client_name;
}
