package com.drugstore.api.model;

import java.util.List;

public class PurchaseRequest {
    private String name;
    private Long supplierId;
    private String reason;
    private List<PurchaseItemRequest> products;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<PurchaseItemRequest> getProducts() {
        return products;
    }

    public void setProducts(List<PurchaseItemRequest> products) {
        this.products = products;
    }
} 