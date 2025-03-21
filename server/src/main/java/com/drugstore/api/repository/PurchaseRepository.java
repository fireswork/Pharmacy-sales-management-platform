package com.drugstore.api.repository;

import com.drugstore.api.model.Purchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    
    // 根据关键字查询
    @Query("SELECT p FROM Purchase p WHERE p.name LIKE %:keyword% OR p.code LIKE %:keyword%")
    Page<Purchase> findByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    // 根据状态查询
    Page<Purchase> findByStatus(String status, Pageable pageable);
    
    // 根据日期查询
    Page<Purchase> findByCreateTimeBetween(Date startDate, Date endDate, Pageable pageable);
    
    // 组合查询
    @Query("SELECT p FROM Purchase p WHERE (p.name LIKE %:keyword% OR p.code LIKE %:keyword%) AND p.status = :status")
    Page<Purchase> findByKeywordAndStatus(@Param("keyword") String keyword, @Param("status") String status, Pageable pageable);
    
    @Query("SELECT p FROM Purchase p WHERE (p.name LIKE %:keyword% OR p.code LIKE %:keyword%) AND p.createTime BETWEEN :startDate AND :endDate")
    Page<Purchase> findByKeywordAndDate(@Param("keyword") String keyword, @Param("startDate") Date startDate, @Param("endDate") Date endDate, Pageable pageable);
    
    Page<Purchase> findByStatusAndCreateTimeBetween(String status, Date startDate, Date endDate, Pageable pageable);
    
    @Query("SELECT p FROM Purchase p WHERE (p.name LIKE %:keyword% OR p.code LIKE %:keyword%) AND p.status = :status AND p.createTime BETWEEN :startDate AND :endDate")
    Page<Purchase> findByKeywordAndStatusAndDate(@Param("keyword") String keyword, @Param("status") String status, @Param("startDate") Date startDate, @Param("endDate") Date endDate, Pageable pageable);

    @Query("SELECT p FROM Purchase p LEFT JOIN FETCH p.items WHERE p.id = :id")
    Optional<Purchase> findByIdWithItems(@Param("id") Long id);

    @Query("SELECT p FROM Purchase p LEFT JOIN FETCH p.items")
    List<Purchase> findAllWithItems();
} 