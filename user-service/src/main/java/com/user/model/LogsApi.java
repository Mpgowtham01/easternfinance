package com.user.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Data
@Table(name = "microservice_api_logs")
public class LogsApi
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer origin_user_id;
    private Integer user_id;
    private Integer user_type_id;
    private String client_name;
    private String api_request;
    private String api_request_body;
    private Integer api_response_status;
    private String api_failure_message;
    private Date api_request_time;
    private String ip;
    private String source;
}
