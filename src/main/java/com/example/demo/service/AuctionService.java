package com.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.demo.entity.Applications;
import com.example.demo.repository.ApplicationsRepository;

@Service
public class AuctionService {

    private final ApplicationsRepository applicationsRepository;

    public AuctionService(ApplicationsRepository applicationsRepository) {
        this.applicationsRepository = applicationsRepository;
    }

    public Optional<Applications> findHighestBid(Integer itemId) {
        return applicationsRepository.findTopByItemIdOrderByBidPriceDescCreatedAtAsc(itemId);
    }

    public Applications bid(Integer itemId, Integer userId, BigDecimal bidPrice) {
        Applications bid = new Applications();
        bid.setItemId(itemId);
        bid.setUserId(userId);
        bid.setBidPrice(bidPrice);
        bid.setStatus(0);
        bid.setCreatedAt(LocalDateTime.now());
        return applicationsRepository.save(bid);
    }
}
