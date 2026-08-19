package com.nse.model;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "nse_log")
@Data
public class NseLogModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer userid;

    private String username;

    private String mobile;

    private String title;

    private String description;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Date logtime;

    private String ip;

    private String source;

    @Column(name = "client_name")
    private String clientName;

}
