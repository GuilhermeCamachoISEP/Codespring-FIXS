package com.example.demo.dto;

import lombok.Data;
import java.util.Map;

@Data
public class StyleWeightsRequest {
    private Map<String, Double> styles;
    private String gender;
    private String ageRange;
    private String budgetRange;
}
