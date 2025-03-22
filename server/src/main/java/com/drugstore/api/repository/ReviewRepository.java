package com.drugstore.api.repository;

import com.drugstore.api.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductId(Long productId);
    List<Review> findByUserId(Long userId);
    List<Review> findByOrderId(Long orderId);
    Optional<Review> findByOrderIdAndProductId(Long orderId, Long productId);
    boolean existsByOrderIdAndProductId(Long orderId, Long productId);
} 