package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.NgKeyword;

public interface NgKeywordRepository extends JpaRepository<NgKeyword, Integer> {

    List<NgKeyword> findByStatusOrderByCreatedAtDesc(Integer status);

    List<NgKeyword> findAllByOrderByCreatedAtDesc();
}
