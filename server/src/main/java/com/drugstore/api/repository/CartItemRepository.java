package com.drugstore.api.repository;

import com.drugstore.api.model.CartItem;
import com.drugstore.api.model.Product;
import com.drugstore.api.model.Store;
import com.drugstore.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUserAndStoreOrderByCreateTimeDesc(User user, Store store);
    Optional<CartItem> findByUserAndStoreAndProduct(User user, Store store, Product product);
    List<CartItem> findByUserAndStoreAndSelectedTrue(User user, Store store);
    
    @Modifying
    @Transactional
    void deleteByUserAndStore(User user, Store store);
    List<CartItem> findByUserAndSelected(User user, boolean selected);
} 