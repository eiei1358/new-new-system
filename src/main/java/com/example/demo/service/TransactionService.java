package com.example.demo.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.demo.entity.Transaction;
import com.example.demo.repository.TransactionRepository;

@Service
public class TransactionService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Optional<Transaction> findByTransferCode(String transferCode) {
        return transactionRepository.findByTransferCode(transferCode);
    }

    public Transaction createTransfer(Integer itemId, Integer sellerId, Integer buyerId) {
        Transaction transaction = new Transaction();
        transaction.setItemId(itemId);
        transaction.setSellerId(sellerId);
        transaction.setBuyerId(buyerId);
        transaction.setStatus(1);
        transaction.setSellerCompleted(0);
        transaction.setBuyerCompleted(0);
        transaction.setTransferCode(generateTransferCode());
        return transactionRepository.save(transaction);
    }

    public Transaction completeIfBothDone(Transaction transaction) {
        if (Integer.valueOf(1).equals(transaction.getSellerCompleted())
                && Integer.valueOf(1).equals(transaction.getBuyerCompleted())) {
            transaction.setStatus(2);
            transaction.setCompletedAt(LocalDateTime.now());
        }
        return transactionRepository.save(transaction);
    }

    private String generateTransferCode() {
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
