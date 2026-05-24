package com.example.demo.dto;

import java.util.List;

public class PackingResponse {
    private List<PackedItem> packingList;
    private List<DayPlan> dayPlans;
    private int totalItems;
    private String aiReasoning;

    public PackingResponse() {}

    public PackingResponse(List<PackedItem> packingList, List<DayPlan> dayPlans, int totalItems, String aiReasoning) {
        this.packingList = packingList;
        this.dayPlans = dayPlans;
        this.totalItems = totalItems;
        this.aiReasoning = aiReasoning;
    }

    public List<PackedItem> getPackingList() {
        return packingList;
    }

    public void setPackingList(List<PackedItem> packingList) {
        this.packingList = packingList;
    }

    public List<DayPlan> getDayPlans() {
        return dayPlans;
    }

    public void setDayPlans(List<DayPlan> dayPlans) {
        this.dayPlans = dayPlans;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public String getAiReasoning() {
        return aiReasoning;
    }

    public void setAiReasoning(String aiReasoning) {
        this.aiReasoning = aiReasoning;
    }
}
