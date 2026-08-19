package com.nse.dto.mf;


import jakarta.persistence.Id;
import lombok.Data;

@Data
public class TransactionTypeDTO {

    @Id
    private int id;

    private String registrar;

    private String positive_transaction;

    private String negative_transaction;

    private String neutral_transaction;

}
