package com.example.demo.dto;

import java.time.LocalDate;
import java.util.List;

public class ReserveOutfitRequest {
    private String eventName;
    private LocalDate eventDate;
    private List<Long> itemIds;

    public ReserveOutfitRequest() {}

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
    public List<Long> getItemIds() { return itemIds; }
    public void setItemIds(List<Long> itemIds) { this.itemIds = itemIds; }
}
