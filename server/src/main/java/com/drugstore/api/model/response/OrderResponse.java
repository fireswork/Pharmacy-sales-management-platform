package com.drugstore.api.model.response;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class OrderResponse {
    private Long id;
    private String orderNumber;
    private BigDecimal totalAmount;
    private String deliveryMethod;
    private String paymentMethod;
    private String status;
    private Date createTime;
    private List<OrderItemResponse> items;

    public OrderResponse(Long id, String orderNumber, BigDecimal totalAmount,
                        String deliveryMethod, String paymentMethod, String status,
                        Date createTime, List<OrderItemResponse> items) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.deliveryMethod = deliveryMethod;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.createTime = createTime;
        this.items = items;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }
} 