package com.tishanyq.shopsync.Models;

public class Sale {
    private String id;
    private String localId;
    private String itemId;
    private String itemName;
    private int quantity;
    private double totalUSD;
    private double totalZWG;
    private String paymentMethod = "CASH";
    private double debtUsedUSD = 0;
    private double debtUsedZWG = 0;
    private String debtId; // Link to debt record if used
    private long saleDate;
    private boolean synced;

    public Sale() {
        this.localId = "SALE_" + System.currentTimeMillis();
        this.saleDate = System.currentTimeMillis();
        this.synced = false;
    }

    public Sale(String itemId, String itemName, int quantity, double totalUSD, double totalZWG) {
        this();
        this.itemId = itemId;
        this.itemName = itemName;
        this.quantity = quantity;
        this.totalUSD = totalUSD;
        this.totalZWG = totalZWG;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLocalId() { return localId; }
    public void setLocalId(String localId) { this.localId = localId; }
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getTotalUSD() { return totalUSD; }
    public void setTotalUSD(double totalUSD) { this.totalUSD = totalUSD; }
    public double getTotalZWG() { return totalZWG; }
    public void setTotalZWG(double totalZWG) { this.totalZWG = totalZWG; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public double getDebtUsedUSD() { return debtUsedUSD; }
    public void setDebtUsedUSD(double debtUsedUSD) { this.debtUsedUSD = debtUsedUSD; }
    public double getDebtUsedZWG() { return debtUsedZWG; }
    public void setDebtUsedZWG(double debtUsedZWG) { this.debtUsedZWG = debtUsedZWG; }
    public String getDebtId() { return debtId; }
    public void setDebtId(String debtId) { this.debtId = debtId; }
    public long getSaleDate() { return saleDate; }
    public void setSaleDate(long saleDate) { this.saleDate = saleDate; }
    public boolean isSynced() { return synced; }
    public void setSynced(boolean synced) { this.synced = synced; }
}
