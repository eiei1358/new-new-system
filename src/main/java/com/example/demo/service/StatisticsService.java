package com.example.demo.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.repository.ApplicationsRepository;
import com.example.demo.repository.ItemRepository;
import com.example.demo.repository.MessageRepository;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.repository.UserRepository;

@Service
public class StatisticsService {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ApplicationsRepository applicationsRepository;
    private final TransactionRepository transactionRepository;
    private final MessageRepository messageRepository;

    public StatisticsService(UserRepository userRepository,
            ItemRepository itemRepository,
            ApplicationsRepository applicationsRepository,
            TransactionRepository transactionRepository,
            MessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.applicationsRepository = applicationsRepository;
        this.transactionRepository = transactionRepository;
        this.messageRepository = messageRepository;
    }

    public Map<String, Long> summary() {
        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("users", userRepository.count());
        summary.put("items", itemRepository.count());
        summary.put("applications", applicationsRepository.count());
        summary.put("transactions", transactionRepository.count());
        summary.put("messages", messageRepository.count());
        return summary;
    }
}
