package com.drugstore.api.model;

public class PurchaseItemResponse {
    private Long id;
    private String name;
    private Integer quantity;
    private String remark;

    public PurchaseItemResponse(Long id, String name, Integer quantity, String remark) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.remark = remark;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
} 