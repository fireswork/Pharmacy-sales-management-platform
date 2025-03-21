package com.drugstore.api.model;

public class EmployeeInfoResponse {
    private Long id;
    private String code;
    private String name;
    private String phoneNumber;
    private String email;
    private StoreInfoResponse store;
    private String hireDate;
    private String status;

    public EmployeeInfoResponse(Long id, String code, String name, String phoneNumber, String email, 
                              StoreInfoResponse store, String hireDate, String status) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.store = store;
        this.hireDate = hireDate;
        this.status = status;
    }

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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public StoreInfoResponse getStore() {
        return store;
    }

    public void setStore(StoreInfoResponse store) {
        this.store = store;
    }

    public String getHireDate() {
        return hireDate;
    }

    public void setHireDate(String hireDate) {
        this.hireDate = hireDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
} 