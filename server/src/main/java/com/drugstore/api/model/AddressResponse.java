package com.drugstore.api.model;

public class AddressResponse {
    private Long id;
    private String receiver;
    private String phoneNumber;
    private String address;
    private boolean isDefault;
    
    public AddressResponse(Long id, String receiver, String phoneNumber, String address, boolean isDefault) {
        this.id = id;
        this.receiver = receiver;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.isDefault = isDefault;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getReceiver() {
        return receiver;
    }
    
    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public boolean isDefault() {
        return isDefault;
    }
    
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
} 