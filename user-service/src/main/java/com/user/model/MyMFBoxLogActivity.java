package com.user.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Data
@Table(name = "mymfbox_log_activity")
public class MyMFBoxLogActivity {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "id")

    private Integer id;
    private Integer user_id;
    private Integer user_type_id;
    private String user_type;
    private String name;
    private String mobile;
    private String title;
    private String content;
    private Date activity_time;
    private String ip;
    private String api_request;
    private String api_response;
    private Integer api_status;
    private String api_process_time;
    private String source;
    private String client_name;



}
