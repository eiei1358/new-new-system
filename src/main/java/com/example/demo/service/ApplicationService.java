package com.example.demo.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.example.demo.entity.Applications;
import com.example.demo.repository.ApplicationsRepository;

@Service
public class ApplicationService {

    private final ApplicationsRepository applicationsRepository;

    public ApplicationService(ApplicationsRepository applicationsRepository) {
        this.applicationsRepository = applicationsRepository;
    }

    public Applications apply(Integer itemId, Integer userId) {
        Applications application = new Applications();
        application.setItemId(itemId);
        application.setUserId(userId);
        application.setStatus(0);
        application.setCreatedAt(LocalDateTime.now());
        return applicationsRepository.save(application);
    }
}
