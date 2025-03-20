package com.drugstore.api.model;

public class UserInfoResponse {
    private String username;
    private String role;
    private MemberInfoResponse member;

    public UserInfoResponse(String username, String role, MemberInfoResponse member) {
        this.username = username;
        this.role = role;
        this.member = member;
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

    public MemberInfoResponse getMember() {
        return member;
    }

    public void setMember(MemberInfoResponse member) {
        this.member = member;
    }
} 