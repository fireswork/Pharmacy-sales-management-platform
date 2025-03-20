package com.drugstore.api.model;

public class StatusRequest {
    private String status;
    
    public StatusRequest() {
        // 默认构造函数
    }
    
    public StatusRequest(String status) {
        this.status = status;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
} 