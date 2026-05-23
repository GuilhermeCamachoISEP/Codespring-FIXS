package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WeatherData {
    private double temperature;
    private String description;
    private String city;
    private int weatherCode;
    private double windSpeed;
    private String tempCategory;
    private String icon;
}
