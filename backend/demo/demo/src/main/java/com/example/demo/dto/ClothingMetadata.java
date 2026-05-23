package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClothingMetadata {
    private String category;
    private String subcategory;
    private String color;
    private String fit;
    private String material;
    private String brand;
    private List<String> season;
    private List<String> style_tags;
}
