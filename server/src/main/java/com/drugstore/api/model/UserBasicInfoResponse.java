package com.drugstore.api.model;

public class UserBasicInfoResponse {
    private String username;
    private String role;

    public UserBasicInfoResponse(String username, String role) {
        this.username = username;
        this.role = role;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
} 