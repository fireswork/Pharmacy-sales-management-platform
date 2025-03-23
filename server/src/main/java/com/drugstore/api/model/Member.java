package com.drugstore.api.model;

import javax.persistence.*;
import java.util.Date;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", unique = true)
    private String memberId; // 会员编号

    @Column(nullable = false)
    private String name; // 会员姓名

    @Column(name = "phone_number")
    private String phoneNumber; // 手机号

    @Column
    private String email; // 邮箱

    @Column
    private Date birthday; // 生日

    @Column
    private String gender; // 性别

    @Column(name = "member_level")
    private String memberLevel; // 会员等级

    @Column
    private Integer points; // 当前积分

    @Column(name = "total_spending")
    private Double totalSpending; // 累计消费

    @CreationTimestamp
    @Column(name = "registration_time", updatable = false)
    private Date registrationTime; // 注册时间

    @Column
    private String status; // 状态

    @Column(name = "is_registered")
    private Boolean isRegistered; // 是否已经注册

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user; // 关联的用户

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
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

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getMemberLevel() {
        return memberLevel;
    }

    public void setMemberLevel(String memberLevel) {
        this.memberLevel = memberLevel;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public Double getTotalSpending() {
        return totalSpending;
    }

    public void setTotalSpending(Double totalSpending) {
        this.totalSpending = totalSpending;
    }

    public Date getRegistrationTime() {
        return registrationTime;
    }

    public void setRegistrationTime(Date registrationTime) {
        this.registrationTime = registrationTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getIsRegistered() {
        return isRegistered;
    }

    public void setIsRegistered(Boolean isRegistered) {
        this.isRegistered = isRegistered;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
} 