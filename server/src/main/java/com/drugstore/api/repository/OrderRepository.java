package com.drugstore.api.repository;

import com.drugstore.api.model.Order;
import com.drugstore.api.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    List<Order> findByUserOrderByCreateTimeDesc(User user);
    
    // 根据状态查询订单
    List<Order> findByStatus(String status);
    
    // 根据时间范围和状态查询订单
    List<Order> findByCreateTimeBetweenAndStatus(Date startDate, Date endDate, String status);
    
    // 根据店铺ID和状态查询订单
    List<Order> findByStoreIdAndStatus(Long storeId, String status);
    
    // 根据店铺ID、时间范围和状态查询订单
    List<Order> findByStoreIdAndCreateTimeBetweenAndStatus(Long storeId, Date startDate, Date endDate, String status);
    
    // 分页查询 - 根据状态
    Page<Order> findByStatus(String status, Pageable pageable);
    
    // 分页查询 - 根据时间范围和状态
    Page<Order> findByCreateTimeBetweenAndStatus(Date startDate, Date endDate, String status, Pageable pageable);
    
    // 分页查询 - 根据店铺ID和状态
    Page<Order> findByStoreIdAndStatus(Long storeId, String status, Pageable pageable);
    
    // 分页查询 - 根据店铺ID、时间范围和状态
    Page<Order> findByStoreIdAndCreateTimeBetweenAndStatus(Long storeId, Date startDate, Date endDate, String status, Pageable pageable);
} 