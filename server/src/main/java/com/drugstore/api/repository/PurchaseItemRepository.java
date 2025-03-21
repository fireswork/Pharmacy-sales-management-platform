package com.drugstore.api.repository;

import com.drugstore.api.model.PurchaseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, Long> {
    // 可以添加自定义查询方法
} 