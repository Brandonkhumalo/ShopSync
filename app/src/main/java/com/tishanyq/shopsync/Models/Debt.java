package com.tishanyq.shopsync.Models;

public class Debt {
    private String id;
    private String localId;
    private String customerName;
    private double amountUSD;
    private double amountZWG;
    private String type; // "CHANGE_OWED" or "CREDIT_USED"
    private String notes;
    private long createdAt;
    private boolean cleared;
    private long clearedAt;
    private boolean synced;

    public Debt() {
        this.localId = "DEBT_" + System.currentTimeMillis();
        this.createdAt = System.currentTimeMillis();
        this.cleared = false;
        this.synced = false;
    }

    public Debt(String customerName, double amountUSD, double amountZWG, String type, String notes) {
        this();
        this.customerName = customerName;
        this.amountUSD = amountUSD;
        this.amountZWG = amountZWG;
        this.type = type;
        this.notes = notes;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLocalId() { return localId; }
    public void setLocalId(String localId) { this.localId = localId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public double getAmountUSD() { return amountUSD; }
    public void setAmountUSD(double amountUSD) { this.amountUSD = amountUSD; }
    public double getAmountZWG() { return amountZWG; }
    public void setAmountZWG(double amountZWG) { this.amountZWG = amountZWG; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public boolean isCleared() { return cleared; }
    public void setCleared(boolean cleared) { this.cleared = cleared; }
    public long getClearedAt() { return clearedAt; }
    public void setClearedAt(long clearedAt) { this.clearedAt = clearedAt; }
    public boolean isSynced() { return synced; }
    public void setSynced(boolean synced) { this.synced = synced; }
}