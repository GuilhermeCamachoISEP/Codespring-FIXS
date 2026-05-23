package com.example.demo.dto;

public record InspirationPhoto(
        String id,
        String url,
        String thumbUrl,
        String unsplashUrl,
        String photographerName,
        String photographerUrl,
        String styleLabel
) {}
