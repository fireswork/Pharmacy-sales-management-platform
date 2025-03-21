package com.drugstore.api.repository;

import com.drugstore.api.model.StoreInventory;
import com.drugstore.api.model.Store;
import com.drugstore.api.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreInventoryRepository extends JpaRepository<StoreInventory, Long> {
    
    // 根据门店查询库存
    List<StoreInventory> findByStore(Store store);
    
    // 根据门店和药品查询库存
    Optional<StoreInventory> findByStoreAndProduct(Store store, Product product);
    
    // 根据门店ID查询库存
    List<StoreInventory> findByStoreId(Long storeId);
    
    // 根据门店ID和药品ID查询库存
    Optional<StoreInventory> findByStoreIdAndProductId(Long storeId, Long productId);
} 