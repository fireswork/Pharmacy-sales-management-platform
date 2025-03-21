package com.drugstore.api.model;

import java.math.BigDecimal;

public class FavoriteResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productCode;
    private String specification;
    private String manufacturer;
    private BigDecimal price;
    private String image;
    private Boolean available;
    private Boolean inStock;
    private Integer quantity;
    private String storeId;
    private String storeName;

    public FavoriteResponse(Long id, Long productId, String productName, 
                          String productCode, String specification, 
                          String manufacturer, BigDecimal price, String image, 
                          Boolean available, Boolean inStock, Integer quantity,
                          String storeId, String storeName) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.productCode = productCode;
        this.specification = specification;
        this.manufacturer = manufacturer;
        this.price = price;
        this.image = image;
        this.available = available;
        this.inStock = inStock;
        this.quantity = quantity;
        this.storeId = storeId;
        this.storeName = storeName;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getSpecification() {
        return specification;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getImage() {
        return image;
    }

    public Boolean getAvailable() {
        return available;
    }

    public Boolean getInStock() {
        return inStock;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getStoreId() {
        return storeId;
    }

    public String getStoreName() {
        return storeName;
    }
} 