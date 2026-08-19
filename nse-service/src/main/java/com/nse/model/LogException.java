package com.nse.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "nse_log_exception")
public class LogException
{
    @Id
    private Integer id;
    private Integer user_id;
    private String client_name;
    private String api_request;
    private String exception_message;
    private String http_method;
    private String ip_address;
    private String source;
    private Date created_at;
    private Date updated_at;
    private Boolean active;
}
