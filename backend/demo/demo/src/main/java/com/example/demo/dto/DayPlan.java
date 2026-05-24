package com.example.demo.dto;

import java.util.List;

public class DayPlan {
    private int dayNumber;
    private String context;
    private List<Long> items;
    private String outfitDescription;

    public DayPlan() {}

    public DayPlan(int dayNumber, String context, List<Long> items, String outfitDescription) {
        this.dayNumber = dayNumber;
        this.context = context;
        this.items = items;
        this.outfitDescription = outfitDescription;
    }

    public int getDayNumber() { return dayNumber; }
    public void setDayNumber(int dayNumber) { this.dayNumber = dayNumber; }

    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }

    public List<Long> getItems() { return items; }
    public void setItems(List<Long> items) { this.items = items; }

    public String getOutfitDescription() { return outfitDescription; }
    public void setOutfitDescription(String outfitDescription) { this.outfitDescription = outfitDescription; }
}
