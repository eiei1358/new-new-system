package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    Optional<Transaction> findByTransferCode(String transferCode);

    Optional<Transaction> findByItemId(Integer itemId);

    List<Transaction> findBySellerIdOrBuyerId(Integer sellerId, Integer buyerId);
}
