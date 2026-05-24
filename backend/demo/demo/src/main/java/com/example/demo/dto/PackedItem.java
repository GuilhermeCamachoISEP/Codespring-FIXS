package com.example.demo.dto;

public class PackedItem {
    private Long itemId;
    private String itemName;
    private String imageUrl;
    private int versatilityScore;
    private String category;

    public PackedItem() {}

    public PackedItem(Long itemId, String itemName, String imageUrl, int versatilityScore, String category) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.imageUrl = imageUrl;
        this.versatilityScore = versatilityScore;
        this.category = category;
    }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public int getVersatilityScore() { return versatilityScore; }
    public void setVersatilityScore(int versatilityScore) { this.versatilityScore = versatilityScore; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
