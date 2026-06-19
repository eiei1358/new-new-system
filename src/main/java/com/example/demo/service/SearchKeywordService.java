package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.entity.SearchKeyword;
import com.example.demo.repository.SearchKeywordRepository;

@Service
public class SearchKeywordService {

    private final SearchKeywordRepository searchKeywordRepository;

    public SearchKeywordService(SearchKeywordRepository searchKeywordRepository) {
        this.searchKeywordRepository = searchKeywordRepository;
    }

    public List<SearchKeyword> findAll() {
        return searchKeywordRepository.findAllByOrderByCreatedAtDesc();
    }

    public SearchKeyword create(String keyword) {
        SearchKeyword searchKeyword = new SearchKeyword();
        searchKeyword.setKeyword(keyword);
        searchKeyword.setStatus(0);
        searchKeyword.setCreatedAt(LocalDateTime.now());
        return searchKeywordRepository.save(searchKeyword);
    }
}
