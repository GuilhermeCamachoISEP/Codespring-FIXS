package com.example.demo.service;

import com.example.demo.domain.Item;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ItemService {

    private final List<Item> items = new ArrayList<>();
    private Long id = 1L;

    public List<Item> getAll() {
        return items;
    }

    public Item add(Item item) {
        item.setId(id++);
        items.add(item);
        return item;
    }
}