package com.drugstore.api.model.response;

import java.math.BigDecimal;

public class CartResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productCode;
    private String specification;
    private String manufacturer;
    private BigDecimal price;
    private String image;
    private Integer quantity;
    private Boolean selected;
    private Boolean available;
    private Boolean inStock;
    private Boolean prescription;
    private Integer stockQuantity;
    private Boolean isMember;
    private BigDecimal memberPrice;

    public CartResponse(Long id, Long productId, String productName, String productCode,
                       String specification, String manufacturer, BigDecimal price,
                       String image, Integer quantity, Boolean selected,
                       Boolean available, Boolean inStock, Boolean prescription,
                       Integer stockQuantity, Boolean isMember, BigDecimal memberPrice) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.productCode = productCode;
        this.specification = specification;
        this.manufacturer = manufacturer;
        this.price = price;
        this.image = image;
        this.quantity = quantity;
        this.selected = selected;
        this.available = available;
        this.inStock = inStock;
        this.prescription = prescription;
        this.stockQuantity = stockQuantity;
        this.isMember = isMember;
        this.memberPrice = memberPrice;
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

    public Integer getQuantity() {
        return quantity;
    }

    public Boolean getSelected() {
        return selected;
    }

    public Boolean getAvailable() {
        return available;
    }

    public Boolean getInStock() {
        return inStock;
    }

    public Boolean getPrescription() {
        return prescription;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }
    
    public Boolean getIsMember() {
        return isMember;
    }
    
    public BigDecimal getMemberPrice() {
        return memberPrice;
    }
} 