package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.demo.entity.Item;
import com.example.demo.repository.ItemRepository;

@Service
public class ItemService {

    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public Optional<Item> findItem(Integer itemId) {
        return itemRepository.findById(itemId);
    }

    public List<Item> findByOwner(Integer userId) {
        return itemRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }
}
