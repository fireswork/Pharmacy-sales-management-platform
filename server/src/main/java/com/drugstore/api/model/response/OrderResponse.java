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
    private String memberName;
    private String memberLevel;
    private Long storeId;
    private String storeName;
    private List<OrderItemResponse> items;

    public OrderResponse(Long id, String orderNumber, BigDecimal totalAmount,
                        String deliveryMethod, String paymentMethod, String status,
                        Date createTime, String memberName, String memberLevel,
                        Long storeId, String storeName, List<OrderItemResponse> items) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.deliveryMethod = deliveryMethod;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.createTime = createTime;
        this.memberName = memberName;
        this.memberLevel = memberLevel;
        this.storeId = storeId;
        this.storeName = storeName;
        this.items = items;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getMemberLevel() {
        return memberLevel;
    }

    public void setMemberLevel(String memberLevel) {
        this.memberLevel = memberLevel;
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

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public void setItems(List<OrderItemResponse> items) {
        this.items = items;
    }
} 