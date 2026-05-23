package com.example.demo.dto;

import java.util.List;

public record InspirationSection(
        String styleId,
        String styleLabel,
        List<InspirationPhoto> photos
) {}
