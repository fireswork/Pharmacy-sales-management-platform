package com.drugstore.api.controller;

import com.drugstore.api.model.*;
import com.drugstore.api.repository.*;
import com.drugstore.api.model.request.ReviewRequest;
import com.drugstore.api.model.response.ReviewResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    // 创建评价
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(@RequestBody ReviewRequest request) {
        // 获取当前用户
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username);

        // 检查订单是否存在
        Order order = orderRepository.findById(request.getOrderId())
            .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 检查订单状态是否为已发货或已完成
        if (!order.getStatus().equals("DELIVERING") && !order.getStatus().equals("COMPLETED")) {
            throw new RuntimeException("只有发货后的订单才能评价");
        }

        // 检查订单是否属于当前用户
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("无法评价不属于您的订单");
        }

        // 检查商品是否存在
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new RuntimeException("商品不存在"));

        // 检查商品是否在订单中
        boolean productInOrder = order.getItems().stream()
            .anyMatch(item -> item.getProduct().getId().equals(product.getId()));
        
        if (!productInOrder) {
            throw new RuntimeException("该商品不在订单中");
        }

        // 检查是否已经评价过
        if (reviewRepository.existsByOrderIdAndProductId(request.getOrderId(), request.getProductId())) {
            throw new RuntimeException("已经评价过该商品");
        }

        // 验证评分
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new RuntimeException("评分必须在1-5之间");
        }

        // 创建评价
        Review review = new Review();
        review.setUser(user);
        review.setOrder(order);
        review.setProduct(product);
        review.setRating(request.getRating());
        review.setContent(request.getContent());
        review.setImages(request.getImages());
        review.setCreateTime(new Date());

        // 保存评价
        Review savedReview = reviewRepository.save(review);

        // 返回评价信息
        ReviewResponse response = convertToResponse(savedReview);
        return ResponseEntity.ok(new ApiResponse<>(response, 200, "评价成功"));
    }

    // 获取商品评价
    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getProductReviews(@PathVariable Long productId) {
        List<Review> reviews = reviewRepository.findByProductId(productId);
        List<ReviewResponse> responseList = reviews.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(new ApiResponse<>(responseList, 200, "获取评价成功"));
    }

    // 获取用户评价
    @GetMapping("/user")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getUserReviews() {
        // 获取当前用户
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username);
        
        List<Review> reviews = reviewRepository.findByUserId(user.getId());
        List<ReviewResponse> responseList = reviews.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(new ApiResponse<>(responseList, 200, "获取评价成功"));
    }

    // 获取订单评价
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getOrderReviews(@PathVariable Long orderId) {
        // 获取当前用户
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username);
        boolean isAdmin = user.getRole().toUpperCase().equals("ADMIN");
        
        // 检查订单是否存在
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("订单不存在"));
            
        // 如果不是管理员，检查订单是否属于当前用户
        if (!isAdmin && !order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("无法查看不属于您的订单评价");
        }
        
        List<Review> reviews = reviewRepository.findByOrderId(orderId);
        List<ReviewResponse> responseList = reviews.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(new ApiResponse<>(responseList, 200, "获取评价成功"));
    }

    // 修改评价
    @PutMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Long reviewId,
            @RequestBody ReviewRequest request) {
        // 获取当前用户
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username);
        
        // 检查评价是否存在
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("评价不存在"));
            
        // 检查评价是否属于当前用户
        if (!review.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("无法修改不属于您的评价");
        }
        
        // 验证评分
        if (request.getRating() != null) {
            if (request.getRating() < 1 || request.getRating() > 5) {
                throw new RuntimeException("评分必须在1-5之间");
            }
            review.setRating(request.getRating());
        }
        
        // 更新评价
        if (request.getContent() != null) {
            review.setContent(request.getContent());
        }
        
        if (request.getImages() != null) {
            review.setImages(request.getImages());
        }
        
        Review updatedReview = reviewRepository.save(review);
        ReviewResponse response = convertToResponse(updatedReview);
        
        return ResponseEntity.ok(new ApiResponse<>(response, 200, "修改评价成功"));
    }

    // 删除评价
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long reviewId) {
        // 获取当前用户
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username);
        
        // 检查评价是否存在
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("评价不存在"));
            
        // 检查评价是否属于当前用户
        if (!review.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("无法删除不属于您的评价");
        }
        
        // 删除评价
        reviewRepository.delete(review);
        
        return ResponseEntity.ok(new ApiResponse<>(null, 200, "删除评价成功"));
    }

    // 检查是否已评价
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Boolean>> checkReviewed(
            @RequestParam Long orderId,
            @RequestParam Long productId) {
        boolean reviewed = reviewRepository.existsByOrderIdAndProductId(orderId, productId);
        return ResponseEntity.ok(new ApiResponse<>(reviewed, 200, "检查成功"));
    }

    // 将Review转换为ReviewResponse
    private ReviewResponse convertToResponse(Review review) {
        return new ReviewResponse(
            review.getId(),
            review.getUser().getId(),
            review.getUser().getName(),
            review.getProduct().getId(),
            review.getProduct().getName(),
            review.getProduct().getImage(),
            review.getRating(),
            review.getContent(),
            review.getImages(),
            review.getCreateTime()
        );
    }
} 