package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.entity.Message;

public interface MessageRepository extends JpaRepository<Message, Integer> {

    List<Message> findByItemIdOrderByCreatedAtAsc(Integer itemId);

    @Query("""
            SELECT m FROM Message m
            WHERE m.itemId = :itemId
              AND ((m.senderId = :userId AND m.receiverId = :otherUserId)
                OR (m.senderId = :otherUserId AND m.receiverId = :userId))
            ORDER BY m.createdAt ASC
            """)
    List<Message> findConversation(
            @Param("itemId") Integer itemId,
            @Param("userId") Integer userId,
            @Param("otherUserId") Integer otherUserId);
}
