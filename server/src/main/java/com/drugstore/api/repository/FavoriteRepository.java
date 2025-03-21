package com.drugstore.api.repository;

import com.drugstore.api.model.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserIdAndStoreId(Long userId, Long storeId);
    Optional<Favorite> findByUserIdAndStoreIdAndProductId(Long userId, Long storeId, Long productId);
    boolean existsByUserIdAndStoreIdAndProductId(Long userId, Long storeId, Long productId);
} 