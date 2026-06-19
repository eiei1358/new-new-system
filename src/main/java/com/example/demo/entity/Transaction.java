package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Integer transactionId;

    @Column(name = "item_id", nullable = false)
    private Integer itemId;

    @Column(name = "seller_id", nullable = false)
    private Integer sellerId;

    @Column(name = "buyer_id", nullable = false)
    private Integer buyerId;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "seller_completed", nullable = false)
    private Integer sellerCompleted;

    @Column(name = "buyer_completed", nullable = false)
    private Integer buyerCompleted;

    @Column(name = "transfer_code", unique = true, length = 50)
    private String transferCode;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
