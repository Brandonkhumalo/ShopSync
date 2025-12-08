package com.tishanyq.shopsync.Models;

public class Item {
    private String id;
    private String localId;
    private String name;
    private String category;
    private double priceUSD;
    private double priceZWG;
    private int quantity;
    private boolean synced;
    private long createdAt;

    public Item() {
        this.localId = "LOCAL_" + System.currentTimeMillis();
        this.createdAt = System.currentTimeMillis();
        this.synced = false;
    }

    public Item(String name, String category, double priceUSD, double priceZWG, int quantity) {
        this();
        this.name = name;
        this.category = category;
        this.priceUSD = priceUSD;
        this.priceZWG = priceZWG;
        this.quantity = quantity;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLocalId() { return localId; }
    public void setLocalId(String localId) { this.localId = localId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getPriceUSD() { return priceUSD; }
    public void setPriceUSD(double priceUSD) { this.priceUSD = priceUSD; }
    public double getPriceZWG() { return priceZWG; }
    public void setPriceZWG(double priceZWG) { this.priceZWG = priceZWG; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public boolean isSynced() { return synced; }
    public void setSynced(boolean synced) { this.synced = synced; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}

