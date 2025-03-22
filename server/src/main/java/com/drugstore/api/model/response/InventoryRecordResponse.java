package com.drugstore.api.model.response;

import java.util.Date;

public class InventoryRecordResponse {
    private Long id;
    private Date recordTime;
    private String recordType;
    private int quantity;
    private String sourceType;
    private Long sourceId;
    private String batchNumber;
    private Date productionDate;
    private Date expirationDate;
    private String remark;
    
    // 产品信息
    private Long productId;
    private String productName;
    private String productCode;
    
    // 门店信息
    private Long storeId;
    private String storeName;
    
    // 操作人信息
    private String operatorName;
    // 如果需要 operatorId，请确保类型是正确的
    // private Long operatorId; 或 private String operatorId;
    
    // 构造函数、getter 和 setter 方法
    public InventoryRecordResponse() {}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Date getRecordTime() {
        return recordTime;
    }
    
    public void setRecordTime(Date recordTime) {
        this.recordTime = recordTime;
    }
    
    public String getRecordType() {
        return recordType;
    }
    
    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    public String getSourceType() {
        return sourceType;
    }
    
    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }
    
    public Long getSourceId() {
        return sourceId;
    }
    
    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }
    
    public String getBatchNumber() {
        return batchNumber;
    }
    
    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }
    
    public Date getProductionDate() {
        return productionDate;
    }
    
    public void setProductionDate(Date productionDate) {
        this.productionDate = productionDate;
    }
    
    public Date getExpirationDate() {
        return expirationDate;
    }
    
    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
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
    
    public String getOperatorName() {
        return operatorName;
    }
    
    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }
    
    // 如果需要 operatorId，请添加适当类型的 getter 和 setter
} 