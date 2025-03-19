package com.drugstore.api.model;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "users") // 表名
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 主键

    @Column(nullable = false, unique = true)
    private String username; // 用户名

    @Column(nullable = false)
    private String password; // 密码

    @Column(nullable = false, unique = true)
    private String phoneNumber; // 手机号

    @Column(nullable = false, unique = true)
    private String email; // 邮箱

    private LocalDate birthday; // 生日

    private String gender; // 性别

    private String role; // 权限

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
} 