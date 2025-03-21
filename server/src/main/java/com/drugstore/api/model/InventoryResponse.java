package com.drugstore.api.model;

import java.math.BigDecimal;

public class InventoryResponse {
    private Long id;
    private Long storeId;
    private String storeName;
    private Long productId;
    private String productName;
    private String productCode;
    private String category;
    private String specification;
    private String manufacturer;
    private BigDecimal price;
    private String description;
    private String image;
    private Integer quantity;
    private String lastUpdateTime;
    
    public InventoryResponse() {
    }
    
    public InventoryResponse(Long id, Long storeId, String storeName, Long productId, 
                            String productName, String productCode, String category, 
                            String specification, String manufacturer, BigDecimal price,
                            String description, String image, Integer quantity, 
                            String lastUpdateTime) {
        this.id = id;
        this.storeId = storeId;
        this.storeName = storeName;
        this.productId = productId;
        this.productName = productName;
        this.productCode = productCode;
        this.category = category;
        this.specification = specification;
        this.manufacturer = manufacturer;
        this.price = price;
        this.description = description;
        this.image = image;
        this.quantity = quantity;
        this.lastUpdateTime = lastUpdateTime;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getStoreId() {
        return storeId;
    }
    
    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }
    
    public String getStoreName() {
        return storeName;
    }
    
    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }
    
    public Long getProductId() {
        return productId;
    }
    
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public String getProductCode() {
        return productCode;
    }
    
    public void setProductCode(String productCode) {
        this.productCode = productCode;
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
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getImage() {
        return image;
    }
    
    public void setImage(String image) {
        this.image = image;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public String getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    public void setLastUpdateTime(String lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
} 