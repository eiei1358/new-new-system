package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.entity.Message;
import com.example.demo.repository.MessageRepository;

@Service
public class MessageService {

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public List<Message> findConversation(Integer itemId, Integer userId, Integer otherUserId) {
        return messageRepository.findConversation(itemId, userId, otherUserId);
    }

    public Message send(Integer senderId, Integer receiverId, Integer itemId, String content) {
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setItemId(itemId);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        return messageRepository.save(message);
    }
}
