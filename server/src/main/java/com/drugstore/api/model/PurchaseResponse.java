package com.drugstore.api.model;

import java.util.List;

public class PurchaseResponse {
    private Long id;
    private String code;
    private String name;
    private Long supplierId;
    private String applicant;
    private String createTime;
    private String status;
    private String reason;
    private String comment;
    private List<PurchaseItemResponse> products;

    public PurchaseResponse(Long id, String code, String name, Long supplierId, String applicant,
                           String createTime, String status, String reason, String comment,
                           List<PurchaseItemResponse> products) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.supplierId = supplierId;
        this.applicant = applicant;
        this.createTime = createTime;
        this.status = status;
        this.reason = reason;
        this.comment = comment;
        this.products = products;
    }

    public PurchaseResponse() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

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

    public String getApplicant() {
        return applicant;
    }

    public void setApplicant(String applicant) {
        this.applicant = applicant;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<PurchaseItemResponse> getProducts() {
        return products;
    }

    public void setProducts(List<PurchaseItemResponse> products) {
        this.products = products;
    }
} 