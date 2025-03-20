package com.drugstore.api.model;

public class EmployeeInfoResponse {
    private String employeeId;
    private String name;
    private String phoneNumber;
    private String email;
    private StoreInfoResponse store;
    private String hireDate;
    private String status;

    public EmployeeInfoResponse(String employeeId, String name, String phoneNumber, String email, 
                               StoreInfoResponse store, String hireDate, String status) {
        this.employeeId = employeeId;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.store = store;
        this.hireDate = hireDate;
        this.status = status;
    }

    // Getters and Setters
    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
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