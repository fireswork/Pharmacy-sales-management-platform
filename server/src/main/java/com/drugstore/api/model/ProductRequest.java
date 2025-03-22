package com.drugstore.api.model;

import java.math.BigDecimal;

public class ProductRequest {
    private String name;
    private String code;
    private String category;
    private String specification;
    private String manufacturer;
    private String supplier;
    private String approvalNumber;
    private BigDecimal retailPrice;
    private BigDecimal costPrice;
    private String image;
    private String description;
    private String usage;
    private String status;
    private Integer stock;
    private Long storeId;
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getSpecification() {
        return specification;
    }
    
    public void setSpecification(String specification) {
        this.specification = specification;
    }
    
    public String getManufacturer() {
        return manufacturer;
    }
    
    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }
    
    public String getSupplier() {
        return supplier;
    }
    
    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }
    
    public String getApprovalNumber() {
        return approvalNumber;
    }
    
    public void setApprovalNumber(String approvalNumber) {
        this.approvalNumber = approvalNumber;
    }
    
    public BigDecimal getRetailPrice() {
        return retailPrice;
    }
    
    public void setRetailPrice(BigDecimal retailPrice) {
        this.retailPrice = retailPrice;
    }
    
    public BigDecimal getCostPrice() {
        return costPrice;
    }
    
    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }
    
    public String getImage() {
        return image;
    }
    
    public void setImage(String image) {
        this.image = image;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getUsage() {
        return usage;
    }
    
    public void setUsage(String usage) {
        this.usage = usage;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Integer getStock() {
        return stock;
    }
    
    public void setStock(Integer stock) {
        this.stock = stock;
    }
    
    public Long getStoreId() {
        return storeId;
    }
    
    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }
} 