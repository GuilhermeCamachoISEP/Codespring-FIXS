package com.example.demo.dto;

import com.example.demo.domain.WardrobeItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OutfitSuggestion {
    private String name;
    private String description;
    private List<WardrobeItem> items;
}
