package com.drugstore.api.model;

public class StatusRequest {
    private String status;
    private String comment;
    
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
} 