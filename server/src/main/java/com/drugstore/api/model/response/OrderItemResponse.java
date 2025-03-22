package com.drugstore.api.model.response;

import java.math.BigDecimal;

public class OrderItemResponse {
    private Long productId;
    private String productName;
    private String image;
    private BigDecimal price;
    private Integer quantity;

    public OrderItemResponse(Long productId, String productName, String image,
                           BigDecimal price, Integer quantity) {
        this.productId = productId;
        this.productName = productName;
        this.image = image;
        this.price = price;
        this.quantity = quantity;
    }

    // Getters
    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getImage() {
        return image;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getQuantity() {
        return quantity;
    }
} 