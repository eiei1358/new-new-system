package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.entity.NgKeyword;
import com.example.demo.repository.NgKeywordRepository;

@Service
public class AdminWatchService {

    private final NgKeywordRepository ngKeywordRepository;

    public AdminWatchService(NgKeywordRepository ngKeywordRepository) {
        this.ngKeywordRepository = ngKeywordRepository;
    }

    public List<NgKeyword> findActiveNgKeywords() {
        return ngKeywordRepository.findByStatusOrderByCreatedAtDesc(0);
    }

    public boolean containsNgKeyword(String text) {
        if (text == null) {
            return false;
        }
        String lowerText = text.toLowerCase();
        return findActiveNgKeywords().stream()
                .map(NgKeyword::getKeyword)
                .filter(keyword -> keyword != null && !keyword.isBlank())
                .anyMatch(keyword -> lowerText.contains(keyword.toLowerCase()));
    }
}
