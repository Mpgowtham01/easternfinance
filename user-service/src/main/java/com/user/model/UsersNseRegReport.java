package com.user.model;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "users_nse_reg_report")
public class UsersNseRegReport {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "id")

    Integer id;
    Integer user_id;
    String name;
    String pan;
    String branch;
    String rm_name;
    String subbroker_name;
    String iin_number;
    String iin_created_date;
    String form_updated_date;
    String cheque_updated_date;
    String iin_status;
    Integer iin_active;
    Integer mandate_active;
    String transaction_date;
    Integer multiple_reg;
    String client_name;
}
