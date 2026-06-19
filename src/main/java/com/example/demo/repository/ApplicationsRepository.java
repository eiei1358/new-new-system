package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Applications;

public interface ApplicationsRepository extends JpaRepository<Applications, Integer> {

    List<Applications> findByItemIdOrderByCreatedAtDesc(Integer itemId);

    boolean existsByItemIdAndUserId(Integer itemId, Integer userId);

    Optional<Applications> findTopByItemIdOrderByBidPriceDescCreatedAtAsc(Integer itemId);
}
