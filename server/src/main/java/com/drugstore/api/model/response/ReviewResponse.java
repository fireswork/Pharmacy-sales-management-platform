package com.drugstore.api.model.response;

import java.util.Date;

public class ReviewResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long productId;
    private String productName;
    private String productImage;
    private Integer rating;
    private String content;
    private String images;
    private Date createTime;
    
    public ReviewResponse(Long id, Long userId, String userName, 
                        Long productId, String productName, String productImage,
                        Integer rating, String content, String images, Date createTime) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.productId = productId;
        this.productName = productName;
        this.productImage = productImage;
        this.rating = rating;
        this.content = content;
        this.images = images;
        this.createTime = createTime;
    }
    
    // getters
    public Long getId() {
        return id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public Long getProductId() {
        return productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public String getProductImage() {
        return productImage;
    }
    
    public Integer getRating() {
        return rating;
    }
    
    public String getContent() {
        return content;
    }
    
    public String getImages() {
        return images;
    }
    
    public Date getCreateTime() {
        return createTime;
    }
} 