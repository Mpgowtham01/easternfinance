package com.user.model;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "transaction_type")
@Data
public class TransactionType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(length = 45)
    private String registrar;

    @Column(columnDefinition = "TEXT")
    private String positive_transaction;

    @Column(columnDefinition = "TEXT")
    private String negative_transaction;

    @Column(columnDefinition = "TEXT")
    private String neutral_transaction;
}
