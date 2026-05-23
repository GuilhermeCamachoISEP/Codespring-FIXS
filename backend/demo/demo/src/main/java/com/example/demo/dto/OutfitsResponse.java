package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OutfitsResponse {
    private List<OutfitSuggestion> outfits;
    private WeatherData weather;
    private WeatherAdvisory advisory;
}
