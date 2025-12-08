package com.tishanyq.shopsync.Models;

public class Shop {
    private String id;
    private String name;
    private String ownerName;
    private String ownerSurname;
    private String phoneNumber;
    private String services;
    private String address;
    private boolean synced;

    public Shop() {}

    public Shop(String name, String ownerName, String ownerSurname,
                String phoneNumber, String services, String address) {
        this.name = name;
        this.ownerName = ownerName;
        this.ownerSurname = ownerSurname;
        this.phoneNumber = phoneNumber;
        this.services = services;
        this.address = address;
        this.synced = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getOwnerSurname() { return ownerSurname; }
    public void setOwnerSurname(String ownerSurname) { this.ownerSurname = ownerSurname; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getServices() { return services; }
    public void setServices(String services) { this.services = services; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public boolean isSynced() { return synced; }
    public void setSynced(boolean synced) { this.synced = synced; }
}