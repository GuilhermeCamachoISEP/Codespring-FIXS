package com.example.demo.dto;

import com.example.demo.domain.WardrobeItem;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class OutfitSuggestion {
    private String name;
    private String description;
    private List<WardrobeItem> items;
    private String weatherNote;

    public OutfitSuggestion(String name, String description, List<WardrobeItem> items) {
        this.name = name;
        this.description = description;
        this.items = items;
    }

    public OutfitSuggestion(String name, String description, List<WardrobeItem> items, String weatherNote) {
        this.name = name;
        this.description = description;
        this.items = items;
        this.weatherNote = weatherNote;
    }
}
