package com.drugstore.api.model;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "purchases")
public class Purchase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // 采购编号

    @Column(nullable = false)
    private String name; // 采购名称

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier; // 供应商

    @Column(length = 2000)
    private String reason; // 采购原因

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee applicant; // 申请人(员工)

    @ManyToOne
    @JoinColumn(name = "store_id")
    private Store store; // 所属门店

    @Column(nullable = false)
    private String status = "pending"; // 状态：pending-待审核，approved-已通过，rejected-已拒绝

    @Column(length = 1000)
    private String comment; // 审核意见

    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime; // 申请时间

    @Temporal(TemporalType.TIMESTAMP)
    private Date updateTime; // 更新时间

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PurchaseItem> items = new ArrayList<>(); // 初始化列表

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

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Employee getApplicant() {
        return applicant;
    }

    public void setApplicant(Employee applicant) {
        this.applicant = applicant;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public List<PurchaseItem> getItems() {
        return items;
    }

    public void setItems(List<PurchaseItem> items) {
        this.items.clear();
        if (items != null) {
            this.items.addAll(items);
            // 设置双向关系
            for (PurchaseItem item : items) {
                item.setPurchase(this);
            }
        }
    }
} 