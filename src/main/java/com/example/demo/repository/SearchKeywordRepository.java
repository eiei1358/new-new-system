package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.SearchKeyword;

public interface SearchKeywordRepository extends JpaRepository<SearchKeyword, Integer> {

    Optional<SearchKeyword> findByKeyword(String keyword);

    List<SearchKeyword> findAllByOrderByCreatedAtDesc();
}
